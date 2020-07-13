package org.jenkinsci.plugins.radargun.model.impl;

import static org.junit.Assert.assertArrayEquals;

import org.jenkinsci.plugins.radargun.model.MainScriptConfig;
import org.junit.Test;

public class MainShellScriptTest {
    
    @Test
    public void testCmdLine() {
        MainScriptConfig main = new MainShellScript();
        main.withNumberOfWorkers(10)
            .withPlugin("testPlugin")
            .withTailFollow()
            .withWait()
            .withJavaOpts("'-Daaa=bbb -Dccc=ddd'")
            .withScriptPath("/tmp/main.sh");
        
        assertArrayEquals(new String[] {"/bin/sh", "/tmp/main.sh", "-t", "-w", "--add-plugin", "testPlugin", "-J", "'-Daaa=bbb -Dccc=ddd'", "-s", "10"}, main.getScriptCmd());
    }
    
    @Test
    public void testMultiValues() {
        MainScriptConfig main = new MainShellScript();
        main.withNumberOfWorkers(10)
            .withReporter("reporter1 reporter2")
            .withPlugin("testPlugin1 testPlugin2")
            .withPluginConfig("config1 config2 config3")
            .withTailFollow()
            .withWait()
            .withJavaOpts("'-Daaa=bbb -Dccc=ddd'")
            .withScriptPath("/tmp/main.sh");
        
        assertArrayEquals(new String[] {"/bin/sh", "/tmp/main.sh", "-t", "-w", "--add-plugin", "testPlugin1", "--add-plugin", "testPlugin2", 
                "--add-config", "config1", "--add-config", "config2", "--add-config", "config3", "-J", "'-Daaa=bbb -Dccc=ddd'", "-s", "10", 
                "--add-reporter", "reporter1", "--add-reporter", "reporter2"}, main.getScriptCmd());
    }

}
