package org.jenkinsci.plugins.radargun.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jenkinsci.plugins.radargun.RadarGunBuilder;
import org.jenkinsci.plugins.radargun.RemoteLoginProgram;
import org.jenkinsci.plugins.radargun.RgBuild;
import org.jenkinsci.plugins.radargun.config.RadarGunInstallationWrapper;
import org.junit.Test;

import hudson.FilePath;

/**
 * 
 * @author vjuranek
 *
 */
public class FunctionsTest {
    
    @Test
    public void testConvertWsToCanonicalPath() throws Exception {
        File f = new File("test.txt");
        String canonPath = Functions.convertWsToCanonicalPath(new FilePath(f));
        assertEquals(f.getCanonicalPath(), canonPath);
    }
    
    @Test
    public void testMakeExecutable() throws Exception {
        File f = File.createTempFile("test", ".sh");
        Functions.makeExecutable(f.getAbsolutePath());
        assertTrue(f.canExecute());
        
        Process p = new ProcessBuilder(f.getAbsolutePath()).start();
        assertEquals(0, p.waitFor());
    }
    
    @Test
    public void testUserCmdsToArrayBefore() {
       List<String> cmdsList = new LinkedList<String>();
       cmdsList.add("echo \"aaa\" > /tmp/aaa.txt");
       cmdsList.add("ls -la /tmp");
       String[] cmds = Functions.userCmdsToArray(cmdsList, ';', false);
       assertEquals(9, cmds.length);
       assertEquals("echo", cmds[0]);
       assertEquals("\"aaa\"", cmds[1]);
       assertEquals(">", cmds[2]);
       assertEquals("/tmp/aaa.txt", cmds[3]);
       assertEquals(";", cmds[4]);
       assertEquals("ls", cmds[5]);
       assertEquals("-la", cmds[6]);
       assertEquals("/tmp", cmds[7]);
       assertEquals(";", cmds[8]);
    }
    
    @Test
    public void testUserCmdsToArrayAfter() {
       List<String> cmdsList = new LinkedList<String>();
       cmdsList.add("echo \"aaa\" > /tmp/aaa.txt");
       cmdsList.add("ls -la /tmp");
       String[] cmds = Functions.userCmdsToArray(cmdsList, ';', true);
       assertEquals(9, cmds.length);
       assertEquals(";", cmds[0]);
       assertEquals("echo", cmds[1]);
       assertEquals("\"aaa\"", cmds[2]);
       assertEquals(">", cmds[3]);
       assertEquals("/tmp/aaa.txt", cmds[4]);
       assertEquals(";", cmds[5]);
       assertEquals("ls", cmds[6]);
       assertEquals("-la", cmds[7]);
       assertEquals("/tmp", cmds[8]);
    }
    
    @Test
    public void testBuildRemoteCmd() {
        RadarGunBuilder sshBuilder = new RadarGunBuilder(new RadarGunInstallationWrapper("testRGInstall"), null, null, null, "SSH", "  ", null, null, null, null, null);
        RgBuild rgBuild = new RgBuild(sshBuilder, null, null, null, null);
        String[] remoteSshCmd = Functions.buildRemoteCmd(rgBuild, "127.0.0.1", new String[] {"echo", "'test'"});
        
        String[] sshCmds = RemoteLoginProgram.SSH.getCmd();
        for (int i = 0; i < sshCmds.length; i++) {
            assertEquals(sshCmds[i], remoteSshCmd[i]);
        }
        assertEquals("127.0.0.1", remoteSshCmd[sshCmds.length + 1]);
        assertEquals("echo", remoteSshCmd[sshCmds.length + 2]);
        assertEquals("'test'", remoteSshCmd[sshCmds.length + 3]);
    }

    @Test
    public void testBuildRemoteCmdWithPrivateKeyPath() {
        RadarGunBuilder sshBuilder = new RadarGunBuilder(new RadarGunInstallationWrapper("testRGInstall"), null, null, null, "SSH", "  ", "/root/.ssh/id_rsa", null, null, null, null);
        RgBuild rgBuild = new RgBuild(sshBuilder, null, null, null, null);
        String[] remoteSshCmd = Functions.buildRemoteCmd(rgBuild, "127.0.0.1", new String[] {"echo", "'test'"});

        String[] sshCmds = RemoteLoginProgram.SSH.getCmd();
        for (int i = 0; i < sshCmds.length; i++) {
            assertEquals(sshCmds[i], remoteSshCmd[i]);
        }
        assertEquals("-i /root/.ssh/id_rsa", remoteSshCmd[sshCmds.length]);
        assertEquals("127.0.0.1", remoteSshCmd[sshCmds.length + 1]);
        assertEquals("echo", remoteSshCmd[sshCmds.length + 2]);
        assertEquals("'test'", remoteSshCmd[sshCmds.length + 3]);
    }
    
    @Test
    public void testIsNullOrEmpty() {
        String str1 = null;
        String str2 = "";
        String str3 = " ";
        String str4 = " ";
        String str5 = " a";
        assertTrue(Functions.isNullOrEmpty(str1));
        assertTrue(Functions.isNullOrEmpty(str2));
        assertTrue(Functions.isNullOrEmpty(str3));
        assertTrue(Functions.isNullOrEmpty(str4));
        assertTrue(!Functions.isNullOrEmpty(str5));
    }

}
