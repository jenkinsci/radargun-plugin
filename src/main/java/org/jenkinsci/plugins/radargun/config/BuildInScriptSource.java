package org.jenkinsci.plugins.radargun.config;

import hudson.Extension;
import hudson.FilePath;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * BuildInScriptSource
 * 
 * @author vjuranek
 * 
 */
@Deprecated
public class BuildInScriptSource extends ScriptSource {

    private static final String SCRIPT_DIR = "/scripts/";
    private static final String MAIN_SCRIPT_NAME = "start_main.sh";
    private static final String WORKER_SCRIPT_NAME = "start_worker.sh";

    private static final String MAIN_SCRIPT_PATH = BuildInScriptSource.class.getResource(
            SCRIPT_DIR + MAIN_SCRIPT_NAME).getPath();
    private static final String WORKER_SCRIPT_PATH = BuildInScriptSource.class.getResource(
            SCRIPT_DIR + WORKER_SCRIPT_NAME).getPath();

    @DataBoundConstructor
    public BuildInScriptSource() {
        // NO-OP

    }

    @Override
    public String getMainScriptPath(FilePath workspace) {
        return MAIN_SCRIPT_PATH;
    }

    @Override
    public String getWorkerScriptPath(FilePath workspace) {
        return WORKER_SCRIPT_PATH;
    }
    
    @Override
    public void cleanup() {
        // NO-OP
    }

    @Extension
    public static class DescriptorImpl extends ScriptSourceDescriptor {
        public String getDisplayName() {
            return "Build-in script source";
        }
    }

}
