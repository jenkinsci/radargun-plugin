package org.jenkinsci.plugins.radargun.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.jenkinsci.plugins.radargun.model.MainScriptConfig;

/**
 * Represent RG main shell script, i.e. $RG_HOME/bin/msater.sh
 * 
 * @author vjuranek
 *
 */
public class MainShellScript extends NodeShellScript implements MainScriptConfig {

    public static final String MAIN_SCRIPT_NAME = "main.sh";
    
    protected String configPath;
    protected int workerNumber;
    protected String reporterPath;
    
    @Override
    public String getScriptName() {
        return MAIN_SCRIPT_NAME;
    }
    
    @Override
    public String getConfigPath() {
        return configPath;
    }

    @Override
    public int getWorkerNumber() {
        return workerNumber;
    }

    @Override
    public String getReporterPath() {
        return reporterPath;
    }

    @Override
    public MainScriptConfig withConfigPath(String configPath) {
        this.configPath = configPath;
        return this;
    }

    @Override
    public MainScriptConfig withNumberOfWorkers(int workerNumber) {
        this.workerNumber = workerNumber;
        return this;
    }

    @Override
    public MainScriptConfig withReporter(String reporterPath) {
        this.reporterPath = reporterPath;
        return this;
    }

    @Override
    public String[] getScriptCmd() {
        return (String[])ArrayUtils.addAll(super.getScriptCmd(), optionToArray());
    }
    
    private String[] optionToArray() {
        List<String> opts = new ArrayList<String>();
        for(MainScriptConfig.Options o : MainScriptConfig.Options.values()) {
            opts.addAll(o.getOption().getCmdOption(this));
        }
        return opts.toArray(new String[opts.size()]);
    }

}
