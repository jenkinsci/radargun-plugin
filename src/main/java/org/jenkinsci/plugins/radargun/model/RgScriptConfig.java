package org.jenkinsci.plugins.radargun.model;

import java.io.Serializable;

/**
 * Base for representing RG exec scripts, i.e. scripts delivered by RG installation, not scripts provided by this plugin
 * or user. Currently these are especially shell scripts {@code main.sh} and {@code worker.sh}.
 * 
 * @author vjuranek
 * 
 */
public interface RgScriptConfig extends Serializable {

    /**
     * Provides path to interpreter of this script, e.g. <code>/bin/bash</code>, eventually with parameters like
     * <code>/bin/bash -x</code>. As it can contain also interpreter parameters, it's represented by the array. The
     * first item of the array is always path to interpreter binary.
     * 
     * @return Full path to the script interpreter
     */
    public String[] getInterpreter();

    /**
     * Name of the script, without full path.
     * 
     * @return Script file name
     */
    public String getScriptName();

    /**
     * Construct command for executing the script, including script configuration options
     * 
     * @return Script path and its configuration options as an array of strings, each item for one option/parameter
     */
    public String[] getScriptCmd();

    public String getScriptPath();

    /**
     * 
     * @param scriptPath
     *            Full path to the script, <b>does</b> include script name itself
     */
    public RgScriptConfig withScriptPath(String scriptPath);

}
