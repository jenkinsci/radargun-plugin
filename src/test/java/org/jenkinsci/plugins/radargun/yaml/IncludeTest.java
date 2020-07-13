package org.jenkinsci.plugins.radargun.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.AbstractBuild;
import hudson.util.LogTaskListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.radargun.config.NodeConfigParser;
import org.jenkinsci.plugins.radargun.model.impl.MainNode;
import org.jenkinsci.plugins.radargun.model.impl.Node;
import org.jenkinsci.plugins.radargun.model.impl.NodeList;
import org.jenkinsci.plugins.radargun.testutil.IOUtils;
import org.jenkinsci.plugins.radargun.util.Resolver;
import org.junit.BeforeClass;
import org.junit.Test;

public class IncludeTest {

    private static final Logger LOG = Logger.getLogger(IncludeTest.class.getName());

    @BeforeClass
    public static void setup() throws Exception {
        AbstractBuild<?, ?> build = mock(FreeStyleBuild.class);
        //Mockito cannot mock final methods, needs to mock method it calls
        when(build.getBuildVariables()).thenReturn(new HashMap<String, String>());
        when(build.getEnvironment(new LogTaskListener(LOG, Level.INFO))).thenReturn(new EnvVars());
        Resolver.init(build);
    }

    @Test
    public void testParseNodeList() throws IOException {
        String config = IOUtils.loadResourceAsString("include.yaml");
        assertNotNull("Unable to load test YAML config file", config);

        // add include tag into original config file
        String includedPath = IOUtils.getAbsoluteResourcePath("default.yaml");
        assertNotNull("Unable to locate included YAML file", includedPath);
        StringBuilder sb = new StringBuilder("---");
        sb.append(YamlNodeConfigParser.LINE_SEP);
        sb.append(YamlNodeConfigParser.INCLUDE_TAG).append(" \"").append(includedPath).append("\"")
                .append(YamlNodeConfigParser.LINE_SEP);
        sb.append(config);

        NodeConfigParser parser = new YamlNodeConfigParser();
        NodeList nodes = parser.parseNodeList(sb.toString());
        assertEquals(2, nodes.getNodes().size());

        MainNode main = (MainNode) nodes.getMain();
        assertEquals("172.12.0.8", main.getFqdn());
        assertEquals("edg-perf08", main.getName());
        assertEquals("-server -Xms8g -Xmx8g -XX:+UseLargePages", main.getJvmOptions());
        Map<String, String> envVars = main.getEnvVars();
        assertNotNull(envVars);
        assertEquals("172.12.0.8", envVars.get("jgroups.udp.mcast_addr"));
        assertEquals("172.12.0.1", envVars.get("infinispan_server1_address"));

        assertEquals(1, nodes.getWorkerCount());
        Node worker = nodes.getNodes().get(1);
        assertEquals("edg-perf01", worker.getName());
        assertNull(worker.getJvmOptions());
        envVars = worker.getEnvVars();
        assertNotNull(envVars);
        assertEquals("172.12.0.1", envVars.get("jgroups.udp.mcast_addr"));
        assertEquals("172.12.0.1", envVars.get("infinispan_server1_address"));
    }

}
