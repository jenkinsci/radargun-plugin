package org.jenkinsci.plugins.radargun.config;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.jenkinsci.plugins.radargun.RadarGunBuilder;
import org.jenkinsci.plugins.radargun.RadarGunBuilder.DescriptorImpl;
import org.jenkinsci.plugins.radargun.RadarGunInstallation;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTextInput;

public class RgInstancesTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    private static final String RG1_NAME = "rg-snapshot";
    private static final String RG1_PATH = "/tmp/radarun/RadarGun-SNAPSHOT";

    @Test
    @LocalData
    public void testSaveRgInstallations() throws Exception {
        DescriptorImpl desc = rule.jenkins.getDescriptorByType(RadarGunBuilder.DescriptorImpl.class);
        List<RadarGunInstallation> installs = desc.getInstallations();
        assertEquals(3, installs.size());
        assertEquals(RG1_NAME, installs.get(0).getName());
        assertEquals(RG1_PATH, installs.get(0).getHome());

        setRgInstallName("Test RG name");
        desc.load();

        installs = desc.getInstallations();
        assertEquals(3, installs.size());
        assertEquals("Test RG name", installs.get(0).getName());
        assertEquals(RG1_PATH, installs.get(0).getHome());

        // set the name back
        setRgInstallName(RG1_NAME);
    }

    private void setRgInstallName(String name) throws IOException, SAXException {
        // go to the tools config page and click and display RG installations
        JenkinsRule.WebClient wc = rule.createWebClient();
        HtmlPage confPage = wc.goTo("manage/configureTools");
        HtmlPage rgPage = null;
        List<DomElement> buttons = confPage.getElementsByTagName("button");
        for (DomElement b : buttons) {
            String text = b.getTextContent();
            if ("RadarGun installations...".equals(text)) {
                rgPage = b.click();
                break;
            }
        }

        // setup the name
        List<DomElement> inputs = rgPage.getElementsByName("_.name");
        for (DomElement i : inputs) {
            HtmlTextInput input = (HtmlTextInput) i;
            if (RG1_NAME.equals(input.getText())) {
                input.setText("Test RG name");
                break;
            }
        }

        // save config
        buttons = confPage.getElementsByTagName("button");
        for (DomElement b : buttons) {
            String text = b.getTextContent();
            if ("Save".equals(text)) {
                b.click();
                break;
            }
        }
    }

}
