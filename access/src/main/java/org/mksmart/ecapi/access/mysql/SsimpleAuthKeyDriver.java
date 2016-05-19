package org.mksmart.ecapi.access.mysql;

import static org.mksmart.ecapi.access.Config.KEYMGMT_MYSQL_DB;
import static org.mksmart.ecapi.access.Config.KEYMGMT_MYSQL_HOST;
import static org.mksmart.ecapi.access.Config.KEYMGMT_MYSQL_PASSWORD;
import static org.mksmart.ecapi.access.Config.KEYMGMT_MYSQL_USER;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.mksmart.datahub.keyauth.KeyAuth;
import org.mksmart.datahub.keyauth.Right;
import org.mksmart.datahub.keyauth.User;
import org.mksmart.ecapi.access.ApiKeyDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SsimpleAuthKeyDriver implements ApiKeyDriver {

    protected String host, dbName, user, pw, prefix;
    protected KeyAuth keyauth = null;
    protected Logger log = LoggerFactory.getLogger(getClass());
    protected String opendatakey = null;

    public SsimpleAuthKeyDriver(Properties config) {
        this.host = config.getProperty(KEYMGMT_MYSQL_HOST);
        this.dbName = config.getProperty(KEYMGMT_MYSQL_DB);
        this.user = config.getProperty(KEYMGMT_MYSQL_USER);
        this.pw = config.getProperty(KEYMGMT_MYSQL_PASSWORD);
        this.opendatakey = config.getProperty("org.mksmart.ecapi.access.opendatakey");
        // this.prefix = config.getProperty(KEYMGMT_DATASET_PREFIX);
        // TODO support KEYMGMT_AUTHSVR_USEPREFIX
        try {
            log.info(this.host + " " + this.dbName + " " + this.user + " " + this.pw);
            this.keyauth = new KeyAuth(this.host, 3306, this.dbName, this.user, this.pw);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean exists(String... keys) {
        if (keys.length < 1) throw new IllegalArgumentException("Key set cannot be empty");
        /* not implemented */
        throw new UnsupportedOperationException("cannot verify the existance of a key");
    }

    @Override
    public Set<String> getDataSources(String... keys) {
        if (keys.length < 1) throw new IllegalArgumentException("Key set cannot be empty");
        log.info("Treating as key based request " + keys[0]);
        Set<String> result = new HashSet<>();
        for (String key : keys) {
            User u = new User(key);
            try {
                String[] res = keyauth.listResourcesWithRight(u, Right.READ);
                for (String r : res) {
                    result.add(r);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    // version without keys - i.e. only public things
    @Override
    public Set<String> getDataSources() {
        log.info("Treating as open data source");
        Set<String> result = new HashSet<>();
        User u = new User(this.opendatakey);
        try {
            String[] res = keyauth.listResourcesWithRight(u, Right.READ);
            for (String r : res) {
                result.add(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public boolean hasRight(String key, String resourceID, int right) {
        Right rr = null;
        if (right == ApiKeyDriver.READ_RIGHT) rr = Right.READ;
        if (right == ApiKeyDriver.WRITE_RIGHT) rr = Right.WRITE;
        if (right == ApiKeyDriver.GRANT_RIGHT) rr = Right.GRANT;
        User user = new User(key);
        try {
            return this.keyauth.authorize(user, resourceID, rr);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean grant(String key, String ukey, String resourceID, int right) {
        // if no key, then it is the open key
        String rukey = ukey;
        if (rukey == null) rukey = this.opendatakey;
        Right rr = null;
        if (right == ApiKeyDriver.READ_RIGHT) rr = Right.READ;
        if (right == ApiKeyDriver.WRITE_RIGHT) rr = Right.WRITE;
        if (right == ApiKeyDriver.GRANT_RIGHT) rr = Right.GRANT;
        User user1 = new User(key);
        User user = new User(rukey);
        try {
            this.keyauth.grant(user1, user, resourceID, rr);
            return true;
        } catch (Exception e) {
            // e.printStackTrace();
            return false;
        }
    }

}
