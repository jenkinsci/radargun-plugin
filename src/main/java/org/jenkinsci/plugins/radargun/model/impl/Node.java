package org.jenkinsci.plugins.radargun.model.impl;

import java.util.List;
import java.util.Map;

public class Node {

    private final String name; // name of the node. If fqdn is not specified, it's assumes that this is the hostname
    private final String fqdn; // fully qualified domain name
    private final String jvmOptions; // additional JVM settings, like -Xmx
    private final Map<String, Object> javaProps; // java properties, typically some RG scenatio variables, "-D" prefix
                                                 // will be automatically added
    private final Map<String, String> envVars;
    private final List<String> beforeCmds;
    private final List<String> afterCmds;
    private final boolean gatherLogs;

    public Node(String name) {
        this.name = name;
        this.fqdn = null;
        this.jvmOptions = null;
        this.javaProps = null;
        this.envVars = null;
        this.beforeCmds = null;
        this.afterCmds = null;
        this.gatherLogs = true;
    }
    
    public Node(String name, String fqdn) {
        this.name = name;
        this.fqdn = fqdn;
        this.jvmOptions = null;
        this.javaProps = null;
        this.envVars = null;
        this.beforeCmds = null;
        this.afterCmds = null;
        this.gatherLogs = true;
    }

    public Node(String name, String fqdn, String jvmOptions, Map<String, Object> javaProps, Map<String, String> envVars, List<String> beforeCmds, List<String> afterCmds, boolean gatherLogs) {
        this.name = name;
        this.fqdn = fqdn;
        this.jvmOptions = jvmOptions;
        this.javaProps = javaProps;
        this.envVars = envVars;
        this.beforeCmds = beforeCmds;
        this.afterCmds = afterCmds;
        this.gatherLogs = gatherLogs;
    }

    public boolean isMain() {
        return false;
    }
    
    public String getName() {
        return name;
    }
    
    public String getFqdn() {
        return fqdn;
    }

    public String getHostname() {
        return fqdn != null ? getFqdn() : getName();
    }
    
    /**
     * As of RG 3.0 JVM options setup is supported directly by RG and this is the preferred way how to setup JVM options.
     * Once RG 2.X is oboslete, this options will be removed!
     */
    @Deprecated
    public String getJvmOptions() {
        if(jvmOptions == null || jvmOptions.isEmpty())
            return null;
        return jvmOptions;
    }

    public Map<String, Object> getJavaProps() {
        return javaProps;
    }

    public Map<String, String> getEnvVars() {
        return envVars;
    }
    
    public List<String> getBeforeCmds() {
        return beforeCmds;
    }
    
    public List<String> getAfterCmds() {
        return afterCmds;
    }

    public boolean getGatherLogs() {
        return gatherLogs;
    }
    
    public String getJavaPropsWithPrefix() {
        if (javaProps == null || javaProps.size() == 0) return null;
        
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, Object> prop : javaProps.entrySet()) {
            sb.append("-D").append(prop.getKey()).append('=').append(prop.getValue()).append(' ');
        }
        return sb.toString();
    }
    
    public String getAllJavaOpts() {
        String jvmOpts = getJvmOptions();
        String javaProps = getJavaPropsWithPrefix();
        if(jvmOpts == null && javaProps == null)
            return null;
        
        StringBuilder sb = new StringBuilder(" \'");
        if(jvmOpts != null)
            sb.append(jvmOpts).append(' ');
        if(javaProps != null)
            sb.append(javaProps);
        sb.append('\'');
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Node{" +
              "name='" + name + '\'' +
              ", fqdn='" + fqdn + '\'' +
              '}';
    }
}
