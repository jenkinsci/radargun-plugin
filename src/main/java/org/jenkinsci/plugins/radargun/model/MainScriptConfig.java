package org.jenkinsci.plugins.radargun.model;

/**
 * Represent RG main script, currently only shell script is provided by RG
 * 
 * @author vjuranek
 *
 */
public interface MainScriptConfig extends NodeScriptConfig {

    public String getConfigPath();
    
    public int getWorkerNumber();
    
    public String getReporterPath();
    
    public MainScriptConfig withConfigPath(String configPath);
    
    public MainScriptConfig withNumberOfWorkers(int workerNumber);
    
    public MainScriptConfig withReporter(String reporterPath);
    
    public static enum Options {
        CONFIG_PATH(new Option("-c", "getConfigPath", true, false)), 
        WORKER_NUMBER(new Option("-s", "getWorkerNumber", true, false)),
        REPORTER_PATH(new Option("--add-reporter", "getReporterPath", true, true));

        private Option option;

        private Options(Option option) {
            this.option = option;
        }

        public Option getOption() {
            return option;
        }
    }
}
