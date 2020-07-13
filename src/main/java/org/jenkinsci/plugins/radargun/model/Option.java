package org.jenkinsci.plugins.radargun.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a switch in RG shell scripts. Used in classes which describe these script, e.g. {@link MainScriptConfig}
 * or {@link WorkerScriptConfig}.
 * 
 * @author vjuranek
 * 
 */
public class Option {

    public static final String MULTI_VALUE_SEPARATOR = " ";
    
    private String optionSwitch;
    private String getterName;
    private boolean hasOptionValue;
    private boolean isMultiValue;

    public Option(String optionSwitch, String getterName, boolean hasOptionValue, boolean isMultiValue) {
        this.optionSwitch = optionSwitch;
        this.getterName = getterName;
        this.hasOptionValue = hasOptionValue;
        this.isMultiValue = isMultiValue;
    }

    public String getOptionSwitch() {
        return optionSwitch;
    }

    public String getGetterName() {
        return getterName;
    }

    public boolean hasOptionValue() {
        return hasOptionValue;
    }
    
    public boolean isMultiValue() {
        return isMultiValue;
    }

    public List<String> getCmdOption(RgScriptConfig cfg) throws IllegalArgumentException {
        List<String> options = new ArrayList<String>();

        try {
            Method m = cfg.getClass().getMethod(getGetterName());
            Object val = m.invoke(cfg);
            if (val != null) {
                if (hasOptionValue()) {
                    if (isMultiValue()) {
                        String[] values = val.toString().split(MULTI_VALUE_SEPARATOR);
                        for (String value : values) {
                            options.add(getOptionSwitch());
                            options.add(value);
                        }
                    } else {
                        options.add(getOptionSwitch());
                        options.add(val.toString());
                    }
                } else { // no value for this switch, getter should be boolean
                    if (((Boolean) val).booleanValue())
                        options.add(getOptionSwitch());
                }
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format("Cannot obtain value for switch %s", getOptionSwitch()),
                    e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Cannot resolve value for switch %s", getOptionSwitch()),
                    e.getCause());
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(String.format("Some issue when resoling value for switch %s",
                    getOptionSwitch()), e.getCause());
        }

        return options;
    }
}
