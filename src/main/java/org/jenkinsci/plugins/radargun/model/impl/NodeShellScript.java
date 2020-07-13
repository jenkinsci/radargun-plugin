package org.jenkinsci.plugins.radargun.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.jenkinsci.plugins.radargun.model.NodeScriptConfig;

public abstract class NodeShellScript extends RgShellScript implements NodeScriptConfig {
    
    protected String mainHost;
    protected String outputPath;
    protected boolean tailFollow;
    protected boolean wait;
    protected String plugin;
    protected String pluginConfig;
    protected String javaOpts;
    
    @Override
    public abstract String getScriptName();
    
    @Override
    public String getMainHost() {
        return mainHost;
    }

    @Override
    public String getOutputPath() {
        return outputPath;
    }

    @Override
    public boolean isTailFollow() {
        return tailFollow;
    }

    @Override
    public boolean isWait() {
        return wait;
    }

    @Override
    public String getPlugin() {
        return plugin;
    }

    @Override
    public String getPluginConfig() {
        return pluginConfig;
    }
    
    @Override
    public String getJavaOpts() {
        return javaOpts;
    }

    @Override
    public NodeScriptConfig withMainHost(String mainHostname) {
        this.mainHost = mainHostname;
        return this;
    }

    @Override
    public NodeScriptConfig withOutput(String outputPath) {
        this.outputPath = outputPath;
        return this;
    }

    @Override
    public NodeScriptConfig withTailFollow() {
        this.tailFollow = true;
        return this;
    }

    @Override
    public NodeScriptConfig withWait() {
        this.wait = true;
        return this;
    }

    @Override
    public NodeScriptConfig withPlugin(String plugin) {
        this.plugin = plugin;
        return this;
    }

    @Override
    public NodeScriptConfig withPluginConfig(String pluginConfig) {
        this.pluginConfig = pluginConfig;
        return this;
    }
    
    @Override
    public NodeScriptConfig withJavaOpts(String javaOpts) {
        this.javaOpts = javaOpts;
        return this;
    }

    @Override
    public String[] getScriptCmd() {
        return (String[])ArrayUtils.addAll(super.getScriptCmd(), optionToArray());
    }
    
    private String[] optionToArray() {
        List<String> opts = new ArrayList<String>();
        for(Options o : NodeScriptConfig.Options.values()) {
            opts.addAll(o.getOption().getCmdOption(this));
        }
        return opts.toArray(new String[opts.size()]);
    }

}
