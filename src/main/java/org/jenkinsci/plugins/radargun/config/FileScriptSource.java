package org.jenkinsci.plugins.radargun.config;

import hudson.Extension;
import hudson.FilePath;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * FileScriptSource
 * 
 * @author vjuranek
 * 
 */
@Deprecated
public class FileScriptSource extends ScriptSource {

    private final String maintPath;
    private final String workerPath;

    @DataBoundConstructor
    public FileScriptSource(String mainPath, String workerPath) {
        this.maintPath = mainPath;
        this.workerPath = workerPath;
    }

    public String getMainPath() {
        return maintPath;
    }

    public String getWorkerPath() {
        return workerPath;
    }
    
    @Override
    public void cleanup() {
        // NO-OP
    }

    @Override
    public String getMainScriptPath(FilePath workspace) {
        return maintPath;
    }

    @Override
    public String getWorkerScriptPath(FilePath workspace) {
        return workerPath;
    }

    @Extension
    public static class DescriptorImpl extends ScriptSourceDescriptor {
        public String getDisplayName() {
            return "File script source";
        }
    }

}
