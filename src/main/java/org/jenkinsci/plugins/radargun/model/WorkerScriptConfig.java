package org.jenkinsci.plugins.radargun.model;

/**
 * Represent RG worker script, currently only shell script is provided by RG
 * 
 * @author vjuranek
 *
 */
public interface WorkerScriptConfig extends NodeScriptConfig {
    
    public int getWorkerIndex();
    
    public String getWorkerName();

    public WorkerScriptConfig withWorkerIndex(int workerIndex);
    
    public WorkerScriptConfig withWorkerName(String workerName);
    
    public static enum Options {
        WORKER_INDEX(new Option("-i", "getWorkerIndex", true, false)), 
        WORKER_NAME(new Option("-n", "getWorkerName", true, false));

        private Option option;

        private Options(Option option) {
            this.option = option;
        }

        public Option getOption() {
            return option;
        }
    }
}
