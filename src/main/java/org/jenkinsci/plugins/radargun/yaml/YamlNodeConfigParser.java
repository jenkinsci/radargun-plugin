package org.jenkinsci.plugins.radargun.yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import org.jenkinsci.plugins.radargun.RadarGunBuilder;
import org.jenkinsci.plugins.radargun.config.NodeConfigParser;
import org.jenkinsci.plugins.radargun.model.impl.MainNode;
import org.jenkinsci.plugins.radargun.model.impl.Node;
import org.jenkinsci.plugins.radargun.model.impl.NodeList;
import org.jenkinsci.plugins.radargun.util.ParseUtils;
import org.jenkinsci.plugins.radargun.util.Resolver;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * {@link NodeConfigParser} for YAML configurations. YAML file can contain arbitrary section, the only required is
 * {@code nodes} list, containing list of nodes, each represented by it's name followed by a map of options. This
 * options can contain following elements:
 * <ul>
 * <li>{@code fqdn} is node FQND or IP address. If not specified, node name is used as a hostname.</li>
 * <li>{@code jvmOtions} and {@code envVars}. {@code jvmOtions} is a plain string containing JVM options like -Xmx etc.</li>
 * <li>{@code javaProps} is a map of java propertied to be passed to RG startup script. Typically should be used for
 * setting up variables used in RG scenarios. Properties are entered without "-D" prefix, this will be added later on
 * automatically.</li>
 * <li>{@code envVars} is a map of environment variables and their values, which should be exported to given host.</li>
 * </ul>
 * The contract is that the first node is main node. Main node can contain all element worker can contain, 
 * no special main configuration is currently supported.
 * 
 * @author vjuranek
 * 
 */
public class YamlNodeConfigParser implements NodeConfigParser {

    private static Logger LOGGER = Logger.getLogger(YamlNodeConfigParser.class.getName());
    
    public static final String NODES_KEY = "nodes";
    public static final String FQDN = "fqdn";
    @Deprecated
    public static final String JVM_OPTS_KEY = "jvmOpts";
    public static final String JAVA_PROPS_KEY = "javaProps";
    public static final String ENV_VARS_KEY = "envVars";
    public static final String BEFORE_CMDS = "beforeCmds";
    public static final String AFTER_CMDS = "afterCmds";
    public static final String GATHET_LOGS = "gatherLogs";

    public static final String LINE_SEP = System.getProperty("line.separator");
    public static final String EOF_REG_EXP = "\\A";
    public static final String INCLUDE_TAG = "!include";

    private final Yaml yaml;

    public YamlNodeConfigParser() {
        this.yaml = new Yaml(new SafeConstructor());
    }

    @Override
    public NodeList parseNodeList(String nodesConfig) {
        String expandedConfig = expandIncludes(nodesConfig); // TODO solve in SnakeYAML or don't use includes at all! To
                                                             // be used really only as a last resort!

        @SuppressWarnings("unchecked")
        Map<String, Object> parsedConf = (Map<String, Object>) yaml.load(expandedConfig);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodesConf = (List<Map<String, Object>>) parsedConf.get(NODES_KEY);
        if (nodesConf.size() < 2)
            throw new IllegalArgumentException(
                    "Wrong node configuration, at least two nodes (one main and one worker) required!");

        Map<String, Object> mainConf = nodesConf.remove(0);
        Map.Entry<String, Object> mainHost = mainConf.entrySet().iterator().next();
        @SuppressWarnings("unchecked")
        MainNode main = parseMainNode(mainHost.getKey(), (Map<String, Object>) mainHost.getValue());

        List<Node> nodes = new LinkedList<Node>();
        for (Map<String, Object> nodeConf : nodesConf) {
            Map.Entry<String, Object> nodeHost = nodeConf.entrySet().iterator().next();
            @SuppressWarnings("unchecked")
            Node node = parseNode(nodeHost.getKey(), (Map<String, Object>)nodeHost.getValue());
            nodes.add(node);
        }

        return new NodeList(main, nodes);
    }

    private Node parseNode(String name, Map<String, Object> nodeConfig) {
        String fqdn = nodeConfig.containsKey(FQDN) ? (String) nodeConfig.get(FQDN) : null;
        String jvmOpts = nodeConfig.containsKey(JVM_OPTS_KEY) ? (String) nodeConfig.get(JVM_OPTS_KEY) : null;
        if (jvmOpts != null) {
            LOGGER.warning("Setting up JVM options via RG jenkins plugin is deprecated and will be removed. Please use RG 3 or higher and set up JVM options direcly in RG!");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> javaProps = nodeConfig.containsKey(JAVA_PROPS_KEY) ? (Map<String, Object>) nodeConfig
                .get(JAVA_PROPS_KEY) : null;
        @SuppressWarnings("unchecked")
        Map<String, String> envVars = nodeConfig.containsKey(ENV_VARS_KEY) ? ParseUtils.mapToStringMap(nodeConfig
                .get(ENV_VARS_KEY)) : null;
        @SuppressWarnings("unchecked")
        List<String> beforeCmds = nodeConfig.containsKey(BEFORE_CMDS) ? (List<String>) nodeConfig.get(BEFORE_CMDS) : null;
        @SuppressWarnings("unchecked")
        List<String> afterCmds = nodeConfig.containsKey(AFTER_CMDS) ? (List<String>) nodeConfig.get(AFTER_CMDS) : null;
        boolean gatherLogs = nodeConfig.containsKey(GATHET_LOGS) ? (Boolean) nodeConfig.get(GATHET_LOGS) : true; // by default gather logs from all machines
        return new Node(name, fqdn, jvmOpts, javaProps, envVars, beforeCmds, afterCmds, gatherLogs);
    }

    private MainNode parseMainNode(String name, Map<String, Object> nodeConfig) {
        Node node = parseNode(name, nodeConfig);
        return new MainNode(node);
    }

    private String expandIncludes(String orig) {
        StringBuilder sb = new StringBuilder();

        String[] lines = orig.split(LINE_SEP);
        for (String line : lines) {
            if (line.startsWith(INCLUDE_TAG)) {
                int fpStart = line.indexOf('"');
                if (fpStart < 0)
                    throw new IllegalArgumentException("String with file path (in quotes) expected");
                int fpEnd = line.indexOf('"', fpStart + 1);
                if (fpEnd < 0 || fpStart == fpEnd)
                    throw new IllegalArgumentException("String with file path (in quotes) expected");
                String filePath = line.substring(fpStart + 1, fpEnd);
                String filePathRes = Resolver.doResolve(filePath);
                sb.append(loadFile(filePathRes));
            } else {
                sb.append(line).append(LINE_SEP);
            }

        }
        return sb.toString();
    }

    private String loadFile(String filePath) {
        String content = null;
        File f = new File(filePath);
        try (InputStream is = new FileInputStream(f)) {
            Scanner s = new Scanner(is, Charset.defaultCharset().name());
            s.useDelimiter(EOF_REG_EXP);
            content = s.hasNext() ? s.next() : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("File to be included doesn't exists!", e);
        }
        return content;  //TODO expandIncludes(content)? Allow nested includes?
    }

}
