package org.jenkinsci.plugins.radargun.model.impl;

import java.io.FileOutputStream;
import java.io.IOException;

import org.jenkinsci.plugins.radargun.NodeRunner;
import org.jenkinsci.plugins.radargun.RadarGunNodeAction;
import org.jenkinsci.plugins.radargun.RgBuild;
import org.jenkinsci.plugins.radargun.model.RgWorkerProcess;
import org.jenkinsci.plugins.radargun.model.WorkerScriptConfig;
import org.jenkinsci.plugins.radargun.util.Functions;
import org.jenkinsci.plugins.radargun.util.Resolver;

import hudson.Launcher.ProcStarter;

public class RgWorkerProcessImpl extends AbstractRgProcess implements RgWorkerProcess {
    
    private final RgBuild rgBuild;
    private final int workerId;
    
    public RgWorkerProcessImpl(RgBuild rgBuild, int workerId) {
        this.rgBuild = rgBuild;
        this.workerId = workerId;
    }

    @Override
    public NodeRunner createRunner() throws IOException, InterruptedException {
        Node worker = rgBuild.getNodes().getWorkers().get(workerId);
        RadarGunNodeAction workerAction = new RadarGunNodeAction(rgBuild.getBuild(), worker.getName());
        rgBuild.getBuild().addAction(workerAction);

        String[] workerCmdLine = getWorkerCmdLine();
        ProcStarter workerProcStarter = Functions.buildProcStarter(rgBuild, workerCmdLine, new FileOutputStream(workerAction.getLogFile()));
        return new NodeRunner(workerProcStarter, workerAction);
    }

    public String[] getWorkerCmdLine() throws InterruptedException, IOException {
        WorkerScriptConfig workerScriptConfig = new WorkerShellScript();
        workerScriptConfig.withWorkerIndex(workerId).withMainHost(rgBuild.getNodes().getMain().getHostname())
                .withScriptPath(rgBuild.getRgInstall().getExecutable(workerScriptConfig, rgBuild.getLauncher().getChannel()));
        
        String pluginPath = rgBuild.getRgBuilder().getPluginPath();
        String pluginConfigPath = rgBuild.getRgBuilder().getPluginConfigPath();
        if (pluginPath != null && !pluginPath.isEmpty()) {
            workerScriptConfig.withPlugin(pluginPath);
            if (pluginConfigPath != null && !pluginConfigPath.isEmpty()) {
                workerScriptConfig.withPluginConfig(pluginConfigPath);
            }
        }

        Node worker = rgBuild.getNodes().getWorkers().get(workerId);
        String javaOpts = Resolver.buildVar(rgBuild.getBuild(), worker.getAllJavaOpts());
        workerScriptConfig.withJavaOpts(javaOpts);
        
        String[] workerCmdLine = rgBuild.getRgBuilder().getScriptSource().getWorkerCmdLine(workerId, rgBuild, workerScriptConfig);
        return workerCmdLine;
    }

}
