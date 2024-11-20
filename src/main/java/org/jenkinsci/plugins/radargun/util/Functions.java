package org.jenkinsci.plugins.radargun.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.jenkinsci.plugins.radargun.RadarGunBuilder;
import org.jenkinsci.plugins.radargun.RadarGunInstallation;
import org.jenkinsci.plugins.radargun.RemoteLoginProgram;
import org.jenkinsci.plugins.radargun.RgBuild;
import org.jenkinsci.plugins.radargun.config.RadarGunCustomInstallation;
import org.jenkinsci.plugins.radargun.config.RadarGunInstallationWrapper;
import org.jenkinsci.plugins.radargun.config.RadarGunInstance;
import org.jenkinsci.plugins.radargun.model.impl.Node;
import org.jenkinsci.plugins.radargun.model.impl.NodeList;

import hudson.FilePath;
import hudson.Launcher.ProcStarter;
import hudson.model.BuildListener;
import hudson.model.StreamBuildListener;
import jenkins.model.Jenkins;

/**
 * 
 * @author vjuranek
 * 
 */
public class Functions {

    public static String convertWsToCanonicalPath(FilePath workspace) {
        String workspacePath = "";
        try {
            workspacePath = (new File(workspace.toURI())).getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return workspacePath;
    }

    public static void makeExecutable(String filePath) {
        File msf = new File(filePath);
        msf.setExecutable(true);
    }
    
    public static void checkDeprecatedConfigs(NodeList nodes, ConsoleLogger console) {
        for (Node node : nodes.asList()) {
            if (node.getJvmOptions() != null && !node.isMain()) {
                console.logAnnot("[RadarGun] WARN: Setting up JVM options on RG worker node via RG jenkins plugin is deprecated. Please use RG 3 or higher and set up JVM options direcly in RG!");
            }
        }
    }
    
    public static String[] userCmdsToArray(List<String> userCmds, char cmdSep, boolean sepCharBefore) {
        String cmdSepStr = Character.toString(cmdSep);
        String[] cmds = sepCharBefore ? new String[] {cmdSepStr} : new String[] {};
        for (String cmd : userCmds) {
            cmds = (String[])ArrayUtils.addAll(cmds, cmd.split(" "));
            cmds = (String[])ArrayUtils.add(cmds, cmdSepStr);
        }
        cmds = sepCharBefore ? (String[])ArrayUtils.remove(cmds, cmds.length - 1) : cmds; 
        return cmds;
    }
    
    public static ProcStarter buildProcStarter(RgBuild rgBuild, String[] cmdLine, FileOutputStream logStream)
            throws IOException, InterruptedException {
        FilePath workspace = getRemoteWorkspace(rgBuild);
        BuildListener logListener = new StreamBuildListener(logStream, Charset.defaultCharset());
        ProcStarter procStarter = rgBuild.getLauncher().launch().cmds(cmdLine).envs(rgBuild.getBuild().getEnvironment(logListener))
                .pwd(workspace).stdout(logListener);
        return procStarter;
    }
    
    public static FilePath getRemoteWorkspace(RgBuild rgBuild) throws IOException, InterruptedException {
        if (rgBuild == null || rgBuild.getBuild() == null || rgBuild.getBuild().getBuiltOn() == null) {
            throw new IllegalArgumentException("RgBuild or node there it was built is null");
        }
        String wsPath = rgBuild.getRgBuilder().getWorkspacePath();
        hudson.model.Node n = rgBuild.getBuild().getBuiltOn();
        FilePath workspace = isNullOrEmpty(wsPath) ? 
                rgBuild.getBuild().getWorkspace()
                : n.createPath(Resolver.buildVar(rgBuild.getBuild(), wsPath));
        if (workspace != null && !workspace.exists()) {
            throw new IOException(String.format("Workspace path '%s' doesn't exists! Check your job configuration!", workspace.getRemote()));
        }
        return workspace;
    }
    
    public static String[] buildRemoteCmd(RgBuild rgBuild, String nodeHostname, String[] localCmd) {
        String[] remoteLoginCmd = RemoteLoginProgram.valueOf(rgBuild.getRgBuilder().getRemoteLoginProgram()).getCmd();
        String remoteLoginCfg = rgBuild.getRgBuilder().getRemoteLogin();
        String remoteLoginPrivateKeyPath = rgBuild.getRgBuilder().getPrivateKeyPath();
        String remoteLogin = isNullOrEmpty(remoteLoginCfg) ? "" : remoteLoginCfg + "@";

        if (!isNullOrEmpty(remoteLoginPrivateKeyPath)) {
            remoteLoginCmd = (String[]) ArrayUtils.addAll(remoteLoginCmd, new String[] { "-i", remoteLoginPrivateKeyPath });
        }
        remoteLoginCmd = (String[]) ArrayUtils.addAll(remoteLoginCmd, new String[] {remoteLogin + nodeHostname});
        String[] cmd = (String[]) ArrayUtils.addAll(remoteLoginCmd, localCmd);
        return cmd;
    }

    public static boolean isNullOrEmpty(String str) {
        return ((str == null) || (str.trim().isEmpty()));
    }
    
    public static RadarGunInstallation getRgInstallation(RadarGunInstance radarGunInstance) {
        return radarGunInstance instanceof RadarGunInstallationWrapper  
        ? Jenkins.getInstance().getDescriptorByType(RadarGunBuilder.DescriptorImpl.class).getInstallation(((RadarGunInstallationWrapper)radarGunInstance).getRadarGunName())
        : new RadarGunInstallation(Resolver.doResolve(radarGunInstance.getName()), Resolver.doResolve(((RadarGunCustomInstallation)radarGunInstance).getHome()),  null);
    }
}
