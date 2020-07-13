package org.jenkinsci.plugins.radargun.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.jenkinsci.plugins.radargun.model.WorkerScriptConfig;

/**
 * Represents RG worker script, i.e. $RG_HOME/bin/worker.sh
 * 
 * @author vjuranek
 *
 */
public class WorkerShellScript extends NodeShellScript implements WorkerScriptConfig {

    public static final String WORKER_SCRIPT_NAME = "worker.sh";
    
    protected int workerIndex;
    protected String workerName;
    
    @Override
    public String getScriptName() {
        return WORKER_SCRIPT_NAME;
    }
    
    @Override
    public int getWorkerIndex() {
        return workerIndex;
    }

    @Override
    public String getWorkerName() {
        return workerName;
    }

    @Override
    public WorkerScriptConfig withWorkerIndex(int workerIndex) {
        this.workerIndex = workerIndex;
        return this;
    }

    @Override
    public WorkerScriptConfig withWorkerName(String workerName) {
        this.workerName = workerName;
        return this;
    }

    @Override
    public String[] getScriptCmd() {
        return (String[])ArrayUtils.addAll(super.getScriptCmd(), optionToArray());
    }
    
    private String[] optionToArray() {
        List<String> opts = new ArrayList<String>();
        for(WorkerScriptConfig.Options o : WorkerScriptConfig.Options.values()) {
            opts.addAll(o.getOption().getCmdOption(this));
        }
        return opts.toArray(new String[opts.size()]);
    }

}
