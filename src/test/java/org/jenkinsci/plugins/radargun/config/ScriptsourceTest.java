package org.jenkinsci.plugins.radargun.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.plugins.radargun.model.NodeScriptConfig;
import org.jenkinsci.plugins.radargun.model.impl.MainShellScript;
import org.jenkinsci.plugins.radargun.model.impl.Node;
import org.junit.Before;
import org.junit.Test;

public class ScriptsourceTest {

    private Map<String, String> env;
    private Map<String, Object> javaProps;
    private Node node;
    private NodeScriptConfig nodeCfg;
    private ScriptSource scriptSourceImpl;

    @Before
    public void setup() {
        env = new HashMap<>();
        env.put("key1", "value1");
        env.put("key2", "");
        env.put("key3", "value3");

        javaProps = new HashMap<>();
        javaProps.put("-Dmy_prop1", "prop1");
        javaProps.put("-Dmy_prop2", "prop2");

        node = new Node("testNode", "node.test.org", "-Xms4G -Xmx16G", javaProps, env, null, null, true);

        nodeCfg = new MainShellScript();
        nodeCfg.withJavaOpts("-Drg_pro1=rgProp1 -Drg_prop2=rgProp2");
        nodeCfg.withMainHost("mainNode");
        nodeCfg.withScriptPath("/opt/radargun/bin/main.sh");
        nodeCfg.withTailFollow();
        nodeCfg.withWait();

        scriptSourceImpl = new BuildInScriptSource();
    }

    @Test
    public void testGetNodeCmdLine() throws IOException, InterruptedException {
        String nodeScriptPath = "/tmp/script.sh";
        String wsPath = "/tmp/workspace";
        int buildNumber = 1;
        
        String[] expectedCmd = new String[] { 
                nodeScriptPath, 
                "node.test.org",
                "cd",
                wsPath + ScriptSource.CMD_SEPARATOR,
                "export",
                ScriptSource.RG_SUFFIX_ENV_VAR + ScriptSource.ENV_KEY_VAL_SEPARATOR + String.valueOf(buildNumber) + ScriptSource.CMD_SEPARATOR,
                "env",
                "key1=\"value1\" key2=\"\" key3=\"value3\"",
                "/bin/sh",
                "/opt/radargun/bin/main.sh",
                "-m",
                "mainNode",
                "-t",
                "-w",
                "-J",
                "-Drg_pro1=rgProp1 -Drg_prop2=rgProp2",
                "-s",
                "0"
                };
        assertArrayEquals(expectedCmd, scriptSourceImpl.getNodeCmdLine(nodeScriptPath, node, nodeCfg, "/tmp/workspace", 1));

    }

    @Test
    public void testPrepareEnvVars() {
        assertEquals("key1=\"value1\" key2=\"\" key3=\"value3\"", scriptSourceImpl.prepareEnvVars(env));
    }

}
