package org.jenkinsci.plugins.radargun.model.impl;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.radargun.NodeRunner;
import org.jenkinsci.plugins.radargun.RadarGunNodeAction;
import org.jenkinsci.plugins.radargun.RgBuild;
import org.jenkinsci.plugins.radargun.model.MainScriptConfig;
import org.jenkinsci.plugins.radargun.model.RgMainProcess;
import org.jenkinsci.plugins.radargun.util.Functions;
import org.jenkinsci.plugins.radargun.util.Resolver;

import hudson.Launcher.ProcStarter;

public class RgMainProcessImpl extends AbstractRgProcess implements RgMainProcess {

    private static Logger LOGGER = Logger.getLogger(RgMainProcessImpl.class.getName());

    private static final int MAX_PID_FILE_RETRIES = 5; // how many times should we try if RG main.pid file ws created
    private static final int RETRY_DELAY = 5000; // how long (in milliseconds) should we wait before we try next time

    private final RgBuild rgBuild;
    private Integer rgMainPid = null;
    private RadarGunNodeAction mainAction;

    public RgMainProcessImpl(RgBuild rgBuild) {
        this.rgBuild = rgBuild;
    }

    @Override
    public NodeRunner createRunner() throws IOException, InterruptedException {
        mainAction = new RadarGunNodeAction(rgBuild.getBuild(), rgBuild.getNodes().getMain().getName(),
                "RadarGun main ");
        rgBuild.getBuild().addAction(mainAction);
        String[] mainCmdLine = getMainCmdLine();
        ProcStarter mainProcStarter = Functions.buildProcStarter(rgBuild, mainCmdLine, new FileOutputStream(mainAction.getLogFile()));
        return new NodeRunner(mainProcStarter, mainAction);
    }

    public String[] getMainCmdLine() throws InterruptedException, IOException {
        MainNode main = rgBuild.getNodes().getMain();
        MainScriptConfig mainScriptConfig = new MainShellScript();
        mainScriptConfig.withNumberOfWorkers(rgBuild.getNodes().getWorkerCount())
                .withConfigPath(rgBuild.getRgBuilder().getScenarioSource().getTmpScenarioPath(rgBuild.getBuild()))
                .withMainHost(main.getHostname()).withScriptPath(
                        rgBuild.getRgInstall().getExecutable(mainScriptConfig, rgBuild.getLauncher().getChannel()));

        String pluginPath = rgBuild.getRgBuilder().getPluginPath();
        String pluginConfigPath = rgBuild.getRgBuilder().getPluginConfigPath();
        String reporterPath = rgBuild.getRgBuilder().getReporterPath();
        if (pluginPath != null && !pluginPath.isEmpty()) {
            mainScriptConfig.withPlugin(pluginPath);
            if (pluginConfigPath != null && !pluginConfigPath.isEmpty()) {
                mainScriptConfig.withPluginConfig(pluginConfigPath);
            }
        }
        if (reporterPath != null && !reporterPath.isEmpty())
            mainScriptConfig.withReporter(reporterPath);

        String javaOpts = Resolver.buildVar(rgBuild.getBuild(), main.getAllJavaOpts());
        mainScriptConfig.withJavaOpts(javaOpts);

        String[] mainCmdLine = rgBuild.getRgBuilder().getScriptSource().getMainCmdLine(rgBuild, mainScriptConfig);
        return mainCmdLine;
    }

    @Override
    public Integer getProcessId() {
        if (rgMainPid == null) {
            try {
                rgMainPid = readRemotePid();
            } catch (IOException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Cannot read remote RG main PID", e);
            }
        }
        return rgMainPid;
    }

    @Override
    public boolean kill() throws IOException, InterruptedException {
        Integer pid = getProcessId();
        if (pid == null) {
            LOGGER.log(Level.WARNING, "Remote RG main PID in null, skipping RG main kill");
            return false;
        }
        
        String[] cmd = Functions.buildRemoteCmd(rgBuild, rgBuild.getNodes().getMain().getHostname(),
                new String[] { "/usr/bin/kill", "-9", pid.toString() });
        ProcStarter killPidProc = Functions.buildProcStarter(rgBuild, cmd, new FileOutputStream(mainAction.getLogFile(), true));
        killPidProc.start();
        return killPidProc.join() == 0;
    }

    private Integer readRemotePid() throws IOException, InterruptedException {
        String workspace = Functions.getRemoteWorkspace(rgBuild).getRemote();

        // check if RG main.pid file exists and eventually wait for it to be created
        String[] cmd = Functions.buildRemoteCmd(rgBuild, rgBuild.getNodes().getMain().getHostname(),
                new String[] { "/usr/bin/test", "-f", workspace + "/main.pid" });
        ProcStarter mainPidFileProc = Functions.buildProcStarter(rgBuild, cmd, new FileOutputStream(mainAction.getLogFile(), true));
        int retCode = 1;
        int retries = 0;
        do {
            if (retries != 0) {
                Thread.sleep(RETRY_DELAY);
            }
            mainPidFileProc.start();
            retCode = mainPidFileProc.join();
            retries++;
        } while (retCode != 0 && retries <= MAX_PID_FILE_RETRIES);
        if (retCode != 0) {
            throw new IllegalStateException("RG main.pid was not created during wating period, exiting!");
        }

        // main.pid exists, read the file content
        cmd = Functions.buildRemoteCmd(rgBuild, rgBuild.getNodes().getMain().getHostname(),
                new String[] { "/usr/bin/cat", workspace + "/main.pid" });
        ProcStarter mainPidProc = Functions.buildProcStarter(rgBuild, cmd, new FileOutputStream(mainAction.getLogFile(), true));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mainPidProc.stdout(baos);
        retCode = mainPidProc.join();
        if (retCode != 0) {
            throw new IOException("Unable to read main.pid file!");
        }
        String pid = new String(baos.toByteArray(), Charset.defaultCharset());
        return Integer.valueOf(pid.substring(0, pid.length() - 1));
    }

}
