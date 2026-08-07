package org.jenkinsci.plugins.radargun.util;

import org.jenkinsci.plugins.radargun.util.Functions;
import static org.junit.Assert.assertEquals;

import org.jenkinsci.plugins.radargun.RadarGunBuilder;
import org.jenkinsci.plugins.radargun.RadarGunInstallation;
import org.jenkinsci.plugins.radargun.config.RadarGunCustomInstallation;
import org.jenkinsci.plugins.radargun.config.RadarGunInstallationWrapper;
import org.jenkinsci.plugins.radargun.config.RadarGunInstance;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

public class RadarGunInstanceTest {

    @Rule public JenkinsRule rule = new JenkinsRule();
    
    @Test 
    public void testRgInstallationFormWrapper() throws Exception {
        RadarGunInstallation expected = new RadarGunInstallation("rgTest", "/opt/radargun", JenkinsRule.NO_PROPERTIES);
        rule.jenkins.getDescriptorByType(RadarGunBuilder.DescriptorImpl.class).setInstallations(expected);
        RadarGunInstance wrapper = new RadarGunInstallationWrapper("rgTest");
        RadarGunInstallation provided = Functions.getRgInstallation(wrapper); 
        assertEquals(expected.getName(), provided.getName());
        assertEquals(expected.getHome(), provided.getHome());
    }
    
    @Test 
    public void testCustomRgInstallation() throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        Resolver.init(build);

        RadarGunInstallation expected = new RadarGunInstallation("rgTest", "/opt/radargun", JenkinsRule.NO_PROPERTIES);
        rule.jenkins.getDescriptorByType(RadarGunBuilder.DescriptorImpl.class).setInstallations(expected);
        RadarGunInstance custom = new RadarGunCustomInstallation("/opt/custom");
        RadarGunInstallation provided = Functions.getRgInstallation(custom);
        assertEquals(custom.getName(), provided.getName());
        assertEquals(((RadarGunCustomInstallation)custom).getHome(), provided.getHome());
    }
    
    @Test 
    public void testCustomRgInstallWithEnvVar() throws Exception {
        System.setProperty("MY_TEST_ENV_VAR", "/opt/radargun");
        FreeStyleProject project = rule.createFreeStyleProject();
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        Resolver.init(build);
        RadarGunInstance custom = new RadarGunCustomInstallation("$MY_TEST_ENV_VAR");
        RadarGunInstallation provided = Functions.getRgInstallation(custom); 
        assertEquals("/opt/radargun", provided.getHome());
    }
    
}
