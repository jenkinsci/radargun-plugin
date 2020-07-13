package org.jenkinsci.plugins.radargun.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.radargun.model.impl.MainNode;
import org.jenkinsci.plugins.radargun.model.impl.Node;
import org.jenkinsci.plugins.radargun.model.impl.NodeList;
import org.jenkinsci.plugins.radargun.testutil.IOUtils;
import org.junit.Test;

public class ParseUtilsTest {

    @Test
    public void testParseNodeList() throws IOException {
        String config = IOUtils.loadResourceAsString("testNodeConfig.yaml");
        
        assertNotNull("Unable to load test YAML config file", config);
        NodeList nodes = ParseUtils.parseNodeList(config);
        assertEquals(3, nodes.getNodes().size());
        
        MainNode main = (MainNode) nodes.getMain();
        assertEquals("172.12.0.8", main.getFqdn());
        assertEquals("edg-perf08", main.getName());
        assertEquals("-server -Xms8g -Xmx8g -XX:+UseLargePages", main.getJvmOptions());
        Map<String, String> envVars = main.getEnvVars();
        assertNotNull(envVars);
        assertEquals("172.12.0.8", envVars.get("jgroups.udp.mcast_addr"));
        assertEquals("172.12.0.1", envVars.get("infinispan_server1_address"));
        Map<String, Object> javaProps = main.getJavaProps();
        assertNotNull(javaProps);
        assertEquals("192.168.117.12:7800;192.168.117.13:7800;192.168.117.14:7800;", javaProps.get("site.default_site.tcp"));
        List<String> beforeCmds = main.getBeforeCmds();
        assertNotNull(beforeCmds);
        assertEquals(2, beforeCmds.size());
        assertEquals("echo \"aaa\" > /tmp/aaa.txt", beforeCmds.get(0));
        assertEquals("ls -la /tmp", beforeCmds.get(1));
        List<String> afterCmds = main.getAfterCmds();
        assertNotNull(afterCmds);
        assertEquals(1, afterCmds.size());
        assertEquals("rm -rf /tmp/aaa.txt", afterCmds.get(0));
        
        assertEquals(2, nodes.getWorkerCount());
        
        Node worker = nodes.getNodes().get(1);
        assertEquals("edg-perf01", worker.getName());
        assertNull(worker.getJvmOptions());
        envVars = worker.getEnvVars();
        assertNotNull(envVars);
        assertEquals("172.12.0.1", envVars.get("jgroups.udp.mcast_addr"));
        assertEquals("172.12.0.1", envVars.get("infinispan_server1_address"));
        javaProps = worker.getJavaProps();
        assertNotNull(javaProps);
        assertEquals("192.168.117.12:7800;192.168.117.13:7800;192.168.117.14:7800;", javaProps.get("site.default_site.tcp"));
        beforeCmds = worker.getBeforeCmds();
        assertNotNull(beforeCmds);
        assertEquals(1, beforeCmds.size());
        assertEquals("rm -rf /tmp/aaa.txt", beforeCmds.get(0));
        afterCmds = worker.getAfterCmds();
        assertNull(afterCmds);
        assertTrue(worker.getGatherLogs());
        
        worker = nodes.getNodes().get(2);
        assertEquals("edg-perf02", worker.getName());
        assertFalse(worker.getGatherLogs());
    }
}
