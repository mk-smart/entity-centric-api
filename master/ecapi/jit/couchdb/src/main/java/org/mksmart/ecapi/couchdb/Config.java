package org.mksmart.ecapi.couchdb;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Singleton that stores the database configuration.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class Config {

    public static final String DB = "couchdb.db";

    public static final String DB_STORE = "couchdb.db.store";

    private static Config me;

    public static final String PASSWORD = "couchdb.password";

    public static final String SERVICE_URL = "couchdb.url";

    public static final String USERNAME = "couchdb.user";

    /**
     * @deprecated the Config class has been generalised. Please refer to LaunchConfiguration in the core
     *             module.
     * @return
     */
    public static Config getInstance() {
        if (me == null) throw new IllegalStateException(
                "The singleton for " + Config.class
                        + " cannot be obtained immediately. It must be initialised"
                        + " at least once by a call to new Config(String) or new Config(Properties).");
        return me;
    }

    private Properties config;

    private String db;

    private String db_store;

    private URL url;

    /**
     * Credentials
     */
    private char[] username, pw;

    /**
     * @deprecated the Config class has been generalised. Please refer to LaunchConfiguration in the core
     *             module.
     */
    public Config(Properties props) {
        if (props == null) throw new IllegalArgumentException("Properties must be supplied.");
        this.config = props;
        parseProperties();
        me = this;
    }

    /**
     * @deprecated the Config class has been generalised. Please refer to LaunchConfiguration in the core
     *             module.
     */
    public Config(String path) {
        if (path == null) throw new IllegalArgumentException("Config path must be supplied.");
        config = new Properties();
        InputStream is;
        try {
            is = new BufferedInputStream(new FileInputStream(path));
        } catch (IOException e) {
            is = getClass().getClassLoader().getResourceAsStream(path);
        }
        try {
            config.load(is);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        parseProperties();
        me = this;
    }

    public Properties asProperties() {
        return config;
    }

    public String getDataDbName() {
        return this.db;
    }

    public char[] getPassword() {
        return pw;
    }

    public URL getServiceURL() {
        return url;
    }

    public String getStorageDbName() {
        return this.db_store;
    }

    public char[] getUsername() {
        return username;
    }

    private void parseProperties() {
        try {
            url = new URL(config.getProperty("couchdb.url"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        db = config.getProperty("couchdb.db");
        db_store = config.getProperty("couchdb.db.store");
        username = config.containsKey("couchdb.user") ? config.getProperty("couchdb.user").toCharArray()
                : null;
        pw = config.containsKey("couchdb.password") ? config.getProperty("couchdb.password").toCharArray()
                : null;
    }

}
