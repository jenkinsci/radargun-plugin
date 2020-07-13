package org.jenkinsci.plugins.radargun.model.impl;

import static org.junit.Assert.assertArrayEquals;

import org.jenkinsci.plugins.radargun.model.WorkerScriptConfig;
import org.junit.Test;

public class WorkerShellScriptTest {

    @Test
    public void testCmdLine() {
        WorkerScriptConfig worker = new WorkerShellScript();
        worker.withWorkerIndex(1)
            .withPlugin("testPlugin")
            .withTailFollow() 
            .withWait() 
            .withScriptPath("/tmp/worker.sh");
         
        assertArrayEquals(new String[] {"/bin/sh", "/tmp/worker.sh", "-t", "-w", "--add-plugin", "testPlugin", "-i","1"}, worker.getScriptCmd());
    }
}
