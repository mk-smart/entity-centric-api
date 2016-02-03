package org.mksmart.ecapi.core;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * The global way of accessing the ECAPI configuration.
 * 
 * Although the entire set of properties is expected to have set values here, this class not have knowledge of
 * all the configuration properties supported by the ECAPI. This is delegated to the respective modules, which
 * are responsible for verifying that a valid configuration is set for them.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class LaunchConfiguration {

    private static LaunchConfiguration me;

    public static LaunchConfiguration getInstance() {
        if (me == null) throw new IllegalStateException(
                "The singleton for " + LaunchConfiguration.class
                        + " cannot be obtained immediately. It must be initialised"
                        + " at least once by a call to a non-default constructor.");
        return me;
    }

    private Configuration conf;

    public LaunchConfiguration(String filename) throws ConfigurationException {
        conf = new PropertiesConfiguration(filename);
        me = this;
    }

    public Configuration asCommonConfiguration() {
        return this.conf;
    }

    public Object get(String property) {
        return conf.getProperty(property);
    }

    public boolean has(String property) {
        return conf.containsKey(property);
    }

}
