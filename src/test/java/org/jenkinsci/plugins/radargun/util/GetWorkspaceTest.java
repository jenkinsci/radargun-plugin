package org.jenkinsci.plugins.radargun.util;

import static org.junit.Assert.assertEquals;

import org.jenkinsci.plugins.radargun.RadarGunBuilder;
import org.jenkinsci.plugins.radargun.RgBuild;
import org.jenkinsci.plugins.radargun.config.RadarGunInstallationWrapper;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

public class GetWorkspaceTest {

    @Rule public JenkinsRule rule = new JenkinsRule();
    
    @Test 
    public void testWorkspaceNull() throws Exception {
        RadarGunBuilder rgBuilder = new RadarGunBuilder(new RadarGunInstallationWrapper("testRGInstall"), null, null, null, "SSH", null, null, null, null, null, null);
        FreeStyleProject project = rule.createFreeStyleProject();
        project.getBuildersList().add(rgBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        RgBuild rgBuild = new RgBuild(rgBuilder, build, null, null, null);
        FilePath ws = Functions.getRemoteWorkspace(rgBuild);
        assertEquals(build.getWorkspace(), ws);
    }
    
    @Test 
    public void testWorkspaceEmpty() throws Exception {
        RadarGunBuilder rgBuilder = new RadarGunBuilder(new RadarGunInstallationWrapper("testRGInstall"), null, null, null, "SSH", null, null, " ", null, null, null);
        FreeStyleProject project = rule.createFreeStyleProject();
        project.getBuildersList().add(rgBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        RgBuild rgBuild = new RgBuild(rgBuilder, build, null, null, null);
        FilePath ws = Functions.getRemoteWorkspace(rgBuild);
        assertEquals(build.getWorkspace(), ws);
    }
    
    @Test 
    public void testWorkspaceNonEmpty() throws Exception {
        RadarGunBuilder rgBuilder = new RadarGunBuilder(new RadarGunInstallationWrapper("testRGInstall"), null, null, null, "SSH", null, null, "/tmp", null, null, null);
        FreeStyleProject project = rule.createFreeStyleProject();
        project.getBuildersList().add(rgBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        RgBuild rgBuild = new RgBuild(rgBuilder, build, null, null, null);
        FilePath ws = Functions.getRemoteWorkspace(rgBuild);
        assertEquals("/tmp", ws.getRemote());
    }
}
