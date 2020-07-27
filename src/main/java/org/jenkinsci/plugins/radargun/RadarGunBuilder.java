package org.jenkinsci.plugins.radargun;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.radargun.config.NodeConfigSource;
import org.jenkinsci.plugins.radargun.config.RadarGunInstallationWrapper;
import org.jenkinsci.plugins.radargun.config.RadarGunInstance;
import org.jenkinsci.plugins.radargun.config.ScenarioSource;
import org.jenkinsci.plugins.radargun.config.ScriptSource;
import org.jenkinsci.plugins.radargun.model.RgMainProcess;
import org.jenkinsci.plugins.radargun.model.RgProcess;
import org.jenkinsci.plugins.radargun.model.impl.Node;
import org.jenkinsci.plugins.radargun.model.impl.NodeList;
import org.jenkinsci.plugins.radargun.model.impl.RgMainProcessImpl;
import org.jenkinsci.plugins.radargun.model.impl.RgWorkerProcessImpl;
import org.jenkinsci.plugins.radargun.util.ConsoleLogger;
import org.jenkinsci.plugins.radargun.util.Functions;
import org.jenkinsci.plugins.radargun.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.AbortException;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;

public class RadarGunBuilder extends Builder {

    private static Logger LOGGER = Logger.getLogger(RadarGunBuilder.class.getName());

    private String radarGunName;
    private RadarGunInstance radarGunInstance;
    private final ScenarioSource scenarioSource;
    private final NodeConfigSource nodeSource;
    private final ScriptSource scriptSource;
    private String remoteLoginProgram; //cannot be final as we re-assign it in readResolve() it it's null for backward compatibility reasons
    private final String remoteLogin;
    private final String workspacePath;
    private final String pluginPath;
    private final String pluginConfigPath;
    private final String reporterPath;

    @DataBoundConstructor
    public RadarGunBuilder(RadarGunInstance radarGunInstance, ScenarioSource scenarioSource, NodeConfigSource nodeSource,
            ScriptSource scriptSource, String remoteLoginProgram, String remoteLogin, String workspacePath, String pluginPath, String pluginConfigPath,
            String reporterPath) {
        this.radarGunInstance = radarGunInstance;
        this.scenarioSource = scenarioSource;
        this.nodeSource = nodeSource;
        this.scriptSource = scriptSource;
        this.remoteLoginProgram = remoteLoginProgram;
        this.remoteLogin = remoteLogin;
        this.workspacePath = Util.fixEmpty(workspacePath);
        this.pluginPath = pluginPath;
        this.pluginConfigPath = pluginConfigPath;
        this.reporterPath = reporterPath;
    }
    
    /**
     * For keeping backward compatibility defaults in ssh as a remote login program and converts RG name into RG installation wrapper
     */
    public RadarGunBuilder readResolve() {
        if ((radarGunInstance == null) && (radarGunName != null)) {
            radarGunInstance = new RadarGunInstallationWrapper(radarGunName);
            radarGunName = null;
        }
        if (this.remoteLoginProgram == null) {
            this.remoteLoginProgram = RemoteLoginProgram.SSH.getName().toUpperCase();
        }
        return this;
    }

    public RadarGunInstance getRadarGunInstance() {
        return radarGunInstance;
    }

    public ScenarioSource getScenarioSource() {
        return scenarioSource;
    }

    public NodeConfigSource getNodeSource() {
        return nodeSource;
    }

    public ScriptSource getScriptSource() {
        return scriptSource;
    }
    
    public String getRemoteLoginProgram() {
        return remoteLoginProgram;
    }

    public String getRemoteLogin() {
        return remoteLogin;
    }
    
    public String getWorkspacePath() {
        return workspacePath;
    }

    public String getPluginPath() {
        return pluginPath;
    }

    public String getPluginConfigPath() {
        return pluginConfigPath;
    }

    public String getReporterPath() {
        return reporterPath;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        Resolver.init(build);
        ConsoleLogger console = new ConsoleLogger(listener);
        
        RadarGunInstallation rgInstall = Functions.getRgInstallation(radarGunInstance);
            
        build.addAction(new RadarGunInvisibleAction(rgInstall.getHome()));

        NodeList nodes = nodeSource.getNodesList(launcher.getChannel());

        console.log(nodes.toString());

        //check deprecated options
        console.logAnnot("[RadarGun] WARN: As of RG plugin release 1.3, \"Start script\" config will be replaced by \"Remote login program\" "
                + "options with limited config. options. Please make sure you don't use anything special in your start scripts (besides launching "
                + "the RG main/workers). If so, please move it into \"Node list\" section, where you can use \"beforeCmds:\" to execute "
                + "custom commands on remote machines.");
        Functions.checkDeprecatedConfigs(nodes, console);
        
        RgBuild rgBuild = new RgBuild(this, build, launcher, nodes, rgInstall);
        List<RgProcess> rgProcesses = null;
        ExecutorService executorService = null;
        try {
            rgProcesses = prepareRgProcesses(rgBuild);
            executorService = Executors.newFixedThreadPool(rgProcesses.size());
            for (RgProcess process : rgProcesses) {
                process.start(executorService);
            }
            return waitForRgMain(rgProcesses.get(0));
        } catch (Exception e) {
            console.logAnnot("[RadarGun] ERROR: something went wrong, caught exception: " + e.getMessage());
            e.printStackTrace(console.getLogger());
            return false;
        } finally {
            cleanup(rgProcesses, executorService);
        }
    }

    private List<RgProcess> prepareRgProcesses(RgBuild rgBuild) {
        List<RgProcess> rgProcesses = new ArrayList<RgProcess>(rgBuild.getNodes().getNodeCount());
        rgProcesses.add(new RgMainProcessImpl(rgBuild));
        List<Node> workers = rgBuild.getNodes().getWorkers();
        for (int i = 0; i < workers.size(); i++) {
            rgProcesses.add(new RgWorkerProcessImpl(rgBuild, i));
        } 
        return rgProcesses;
    }
    
    private boolean waitForRgMain(RgProcess mainProc) throws AbortException {
        boolean isSuccess = false;
        try {
            // wait for main process to be finished, failure of the worker process should be detected by RG main
            isSuccess = mainProc.waitForResult() == 0;
        } catch (InterruptedException e) {
            //TODO actually shouln't fail the build but set it to canceled
            LOGGER.log(Level.INFO, "Stopping the build - build interrupted", e);
            //throw new AbortException(e.getMessage());
        } catch (ExecutionException e) {
            LOGGER.log(Level.INFO, "Failing the build - getting main result has failed", e);
            throw new AbortException(e.getMessage());
        } 
        return isSuccess;
    }
    
    private void cleanup(List<RgProcess> rgProcesses, ExecutorService executorService) {
        if (executorService != null) {
            List<Runnable> notStarted = executorService.shutdownNow();
            LOGGER.log(Level.FINE, "Number of tasks that weren't started: " + notStarted.size());
        }
        
        try {
            scriptSource.cleanup();
            scenarioSource.cleanup();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Removing temporal files failed", e);
        }
        
        if (rgProcesses != null) {
            try {
                ((RgMainProcess)rgProcesses.get(0)).kill();
            } catch(Exception e) {
                LOGGER.log(Level.WARNING, "Killing RG main failed", e);
            }
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private volatile List<RadarGunInstallation> installations = new ArrayList<>();

        public DescriptorImpl() {
            load();
        }

        public List<RadarGunInstallation> getInstallations() {
            return installations;
        }
        
        public List<RadarGunInstallationWrapper> getInstallationWrappers() {
            List<RadarGunInstallationWrapper> wrappers = new ArrayList<>();
            for (RadarGunInstallation inst : installations) {
                wrappers.add(new RadarGunInstallationWrapper(inst.getName()));
            }
            return wrappers;
        }
        
        public void setInstallations(RadarGunInstallation... installations) {
            List<RadarGunInstallation> installs = new ArrayList<>();
            for (RadarGunInstallation installation : installations) {
                installs.add(installation);
            }
            this.installations = installs;
            save();
        }
        
        public RadarGunInstallation getInstallation(String installationName) {
            if (installationName == null || installationName.isEmpty())
                return null;

            for (RadarGunInstallation i : installations) {
                if (i.getName().equals(installationName))
                    return i;
            }
            return null;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Run RadarGun";
        }

        public ListBoxModel doFillRadarGunNameItems() {
            ListBoxModel lb = new ListBoxModel();
            for (RadarGunInstallation rgi : installations) {
                lb.add(rgi.getName(), rgi.getName());
            }
            return lb;
        }

        public static DescriptorExtensionList<RadarGunInstance, Descriptor<RadarGunInstance>> getRgInstances() {
            return RadarGunInstance.all();
        }
        
        public static DescriptorExtensionList<ScenarioSource, Descriptor<ScenarioSource>> getScenarioSources() {
            return ScenarioSource.all();
        }

        public static DescriptorExtensionList<NodeConfigSource, Descriptor<NodeConfigSource>> getNodeSources() {
            return NodeConfigSource.all();
        }

        public static ListBoxModel doFillRemoteLoginProgramItems() {
            ListBoxModel lb = new ListBoxModel();
            for (RemoteLoginProgram remote : RemoteLoginProgram.values()) {
                lb.add(remote.getName(), remote.getName().toUpperCase());
            }
            return lb;
        }
        
        public static DescriptorExtensionList<ScriptSource, Descriptor<ScriptSource>> getScriptSources() {
            return ScriptSource.all();
        }

    }
}
