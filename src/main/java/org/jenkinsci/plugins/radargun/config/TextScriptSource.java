package org.jenkinsci.plugins.radargun.config;

import hudson.Extension;
import hudson.FilePath;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * TextScriptSource
 * 
 * @author vjuranek
 * 
 */
@Deprecated
public class TextScriptSource extends ScriptSource {

    private final String mainScript;
    private final String workerScript;
    
    private transient FilePath mainScriptPath;
    private transient FilePath workerScriptPath;

    @DataBoundConstructor
    public TextScriptSource(String mainScript, String workerScript) {
        this.mainScript = mainScript;
        this.workerScript = workerScript;
        mainScriptPath = null;
        workerScriptPath = null;
    }

    public String getMainScript() {
        return mainScript;
    }

    public String getWorkerScript() {
        return workerScript;
    }
    
    @Override
    public void cleanup() throws InterruptedException, IOException {
        try {
            if(mainScriptPath != null)
                mainScriptPath.delete();
            if(workerScriptPath != null)
                workerScriptPath.delete();
        } finally {
            mainScriptPath = null;
            workerScriptPath = null;
        }
    }

    @Override
    public String getMainScriptPath(FilePath workspace) throws InterruptedException, IOException {
        if(mainScriptPath == null)
            mainScriptPath = createMainScriptFile(workspace);
        return mainScriptPath.getRemote();
    }

    @Override
    public String getWorkerScriptPath(FilePath workspace) throws InterruptedException, IOException {
        if(workerScriptPath == null)
            workerScriptPath = createWorkerScriptPath(workspace); 
        return workerScriptPath.getRemote();
    }
    
    private FilePath createMainScriptFile(FilePath workspace) throws InterruptedException, IOException {
        FilePath main =  workspace.createTextTempFile("radargun_main", ".sh", mainScript, true);
        main.chmod(0777);
        return main;
    }
    
    private FilePath createWorkerScriptPath(FilePath workspace) throws InterruptedException, IOException {
        FilePath worker = workspace.createTextTempFile("radargun_worker", ".sh", workerScript, true);
        worker.chmod(0777);
        return worker;
    }

    @Extension
    public static class DescriptorImpl extends ScriptSourceDescriptor {
        public String getDisplayName() {
            return "Text script source";
        }
    }

}
