package org.jenkinsci.plugins.radargun.config;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang.ArrayUtils;
import org.jenkinsci.plugins.radargun.RgBuild;
import org.jenkinsci.plugins.radargun.model.MainScriptConfig;
import org.jenkinsci.plugins.radargun.model.NodeScriptConfig;
import org.jenkinsci.plugins.radargun.model.WorkerScriptConfig;
import org.jenkinsci.plugins.radargun.model.impl.Node;
import org.jenkinsci.plugins.radargun.util.Functions;

import hudson.DescriptorExtensionList;
import hudson.FilePath;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * Init scripts used for launching RG on remote nodes, typically via ssh. This seems to be redundant and will be removed
 * soon - actually probably only removed as it contains important stuff like construcing cmd line.
 * 
 * TODO replace by more convenient construct or script should be completely generated based on used configuration.
 * 
 * @author vjuranek
 * 
 */
public abstract class ScriptSource implements Describable<ScriptSource> {

    public static final String CD_CMD = "cd";
    public static final String ENV_CMD = "env";
    public static final String EXPORT_CMD = "export";
    public static final String RG_SUFFIX_ENV_VAR = "RG_LOG_ID";
    public static final String JAVA_PROP_PREFIX = "-D";
    public static final char ENV_KEY_VAL_SEPARATOR = '=';
    public static final char ENV_VAR_QUOTE = '"';
    public static final char VAR_SEPARATOR = ' ';
    public static final char CMD_SEPARATOR = ';';

    public abstract String getMainScriptPath(FilePath workspace) throws InterruptedException, IOException;

    public abstract String getWorkerScriptPath(FilePath workspace) throws InterruptedException, IOException;

    public abstract void cleanup() throws InterruptedException, IOException;

    /*package*/ String[] getNodeCmdLine(String nodeScriptPath, Node node, NodeScriptConfig nodeScriptConfig, String workspace, int buildId)
            throws InterruptedException, IOException {
        Functions.makeExecutable(nodeScriptPath);
        //path to init script (typically ssh) and hostname of the machine where subsequent commands should be executed
        //also changes pwd to workspace 
        //and export RG_LOG_ID env. var. to adjust RG log suffix to Jenkins build ID
        String[] cmd = new String[] { nodeScriptPath, node.getHostname(), CD_CMD, workspace + CMD_SEPARATOR, EXPORT_CMD, RG_SUFFIX_ENV_VAR + ENV_KEY_VAL_SEPARATOR + String.valueOf(buildId) + CMD_SEPARATOR };
        
        //set up user init commands
        cmd = node.getBeforeCmds() == null ? cmd : (String[])ArrayUtils.addAll(cmd, Functions.userCmdsToArray(node.getBeforeCmds(), CMD_SEPARATOR, false));
        
        //eventually setup environment where RG scripts should be executed
        //env cmd takes as a parameter command which needs to be executed in given env, so this needs to be right before RG script is added 
        //into cmd array
        cmd = node.getEnvVars() == null ? cmd : (String[]) ArrayUtils.addAll(cmd, new String[] { ENV_CMD, prepareEnvVars(node.getEnvVars()) });
        
        // Check whether to gather logs, i.e. run with "tail" option ("-t") 
        if (node.getGatherLogs()) {
            nodeScriptConfig.withTailFollow();
        }
        // Always run with wait ("-w") option not to finish immediately once the RG process is started.
        // Otherwise Jenkins finish the process and kill all background thread, i.e. kill RG main.
        nodeScriptConfig.withWait();
        cmd = (String[]) ArrayUtils.addAll(cmd, nodeScriptConfig.getScriptCmd());
        
        //set up user after commands
        cmd = node.getAfterCmds() == null ? cmd : (String[])ArrayUtils.addAll(cmd, Functions.userCmdsToArray(node.getAfterCmds(), CMD_SEPARATOR, true));
        return cmd;
    }

    public String[] getMainCmdLine(RgBuild rgBuild, MainScriptConfig mainScriptConfig) throws InterruptedException, IOException {
        FilePath workspace = Functions.getRemoteWorkspace(rgBuild);
        String mainScriptPath = getMainScriptPath(workspace);
        return getNodeCmdLine(mainScriptPath, rgBuild.getNodes().getMain(), mainScriptConfig, workspace.getRemote(), rgBuild.getBuild().getNumber());
    }

    public String[] getWorkerCmdLine(int workerId, RgBuild rgBuild, WorkerScriptConfig nodeScriptConfig)
            throws InterruptedException, IOException {
        FilePath workspace = Functions.getRemoteWorkspace(rgBuild);
        String workerScriptPath = getWorkerScriptPath(workspace);
        return getNodeCmdLine(workerScriptPath, rgBuild.getNodes().getWorkers().get(workerId), nodeScriptConfig, workspace.getRemote(), rgBuild.getBuild().getNumber());
    }

    /**
     * 
     * @param envVars
     *            Map o environment variables to be passed to a node when executing command there. This map is expected
     *            not to be null.
     * @return string representation env. variables separated by semicolon
     */
    /*package*/ String prepareEnvVars(Map<String, String> envVars) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> envIter = new TreeSet<String>(envVars.keySet()).iterator();
        while (envIter.hasNext()) {
            String key = envIter.next();
            String value = envVars.get(key) instanceof String ? envVars.get(key) : envVars.get(key).toString();
            sb.append(key).append(ENV_KEY_VAL_SEPARATOR).append(ENV_VAR_QUOTE).append(value).append(ENV_VAR_QUOTE)
                    .append(VAR_SEPARATOR);
        }
        return sb.subSequence(0, sb.length() - 1).toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Descriptor<ScriptSource> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public static final DescriptorExtensionList<ScriptSource, Descriptor<ScriptSource>> all() {
        return Jenkins.getInstance().getDescriptorList(ScriptSource.class);
    }

    public static abstract class ScriptSourceDescriptor extends Descriptor<ScriptSource> {
    }

}
