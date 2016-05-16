package org.mksmart.ecapi.access.mysql;

import static org.mksmart.ecapi.access.Config.KEYMGMT_DATASET_PREFIX;
import static org.mksmart.ecapi.access.Config.KEYMGMT_MYSQL_DB;
import static org.mksmart.ecapi.access.Config.KEYMGMT_MYSQL_HOST;
import static org.mksmart.ecapi.access.Config.KEYMGMT_MYSQL_PASSWORD;
import static org.mksmart.ecapi.access.Config.KEYMGMT_MYSQL_USER;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.mksmart.ecapi.access.ApiKeyDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class MySqlKeyDriver implements ApiKeyDriver {

    protected String host, dbName, user, pw, prefix;

    protected Logger log = LoggerFactory.getLogger(getClass());

    public MySqlKeyDriver(Properties config) {
        this.host = config.getProperty(KEYMGMT_MYSQL_HOST);
        this.dbName = config.getProperty(KEYMGMT_MYSQL_DB);
        this.user = config.getProperty(KEYMGMT_MYSQL_USER);
        this.pw = config.getProperty(KEYMGMT_MYSQL_PASSWORD);
        this.prefix = config.getProperty(KEYMGMT_DATASET_PREFIX);
        // TODO support KEYMGMT_AUTHSVR_USEPREFIX
    }

    @Override
    public boolean exists(String... keys) {
        if (keys.length < 1) throw new IllegalArgumentException("Key set cannot be empty");
        Connection conn = null;
        boolean result = true;
        try {
            conn = getConnection();
        } catch (SQLException e1) {
            throw new RuntimeException(e1);
        }
        for (String key : keys) {
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try {
                StringBuilder query = new StringBuilder();
                query.append("SELECT COUNT( DISTINCT `ID` ) AS `n`");
                query.append(" FROM `APIKeyManagement` WHERE `APIKEY` = '" + key + "'");
                log.trace("Issuing database query:\r\n{}", query.toString());
                preparedStatement = conn.prepareStatement(query.toString());
                log.trace("<== SUCCESS. Query returned.");
                resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) result &= (1 == resultSet.getInt("n"));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (resultSet != null) resultSet.close();
                    if (preparedStatement != null) preparedStatement.close();
                } catch (SQLException ex) {
                    log.warn("SQLException caught while trying to close result set.", ex);
                }
            }
        }
        if (conn != null) try {
            conn.close();
        } catch (SQLException ex) {
            log.warn("SQLException caught while trying to close JDBC connection.", ex);
        }
        return result;
    }

    protected Connection getConnection() throws SQLException {
        try {
            String driver = "com.mysql.jdbc.Driver";
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No MySQL JDBC driver found.", e);
        }
        String strConn = "jdbc:mysql://" + this.host + "/" + this.dbName + "?" + "user=" + this.user
                         + "&password=" + this.pw;
        return DriverManager.getConnection(strConn);
    }

    @Override
    public Set<String> getDataSources(String... keys) {
        if (keys.length < 1) throw new IllegalArgumentException("Key set cannot be empty");
        Connection conn = null;
        Set<String> result = new HashSet<>();
        try {
            conn = getConnection();
        } catch (SQLException e1) {
            throw new RuntimeException(e1);
        }

        StringBuilder query = new StringBuilder();
        query.append("SELECT `ServiceID` as `dataset`");
        query.append(" FROM `managesubsciption` WHERE `API_KEY` IN (");
        for (int i = 0; i < keys.length; i++) {
            query.append("'" + keys[i] + "'");
            // query.append(this.prefix == null || this.prefix.isEmpty() ? "" : this.prefix);
            if (i < keys.length - 1) query.append(",");
        }
        query.append(")");
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            log.trace("Issuing database query:\r\n{}", query.toString());
            preparedStatement = conn.prepareStatement(query.toString());
            resultSet = preparedStatement.executeQuery();
            log.trace("<== SUCCESS. Query returned.");
            log.debug("Start list of datasets:");
            while (resultSet.next()) {
                String dsFull = resultSet.getString("dataset");
                String ds = this.prefix == null || this.prefix.isEmpty() ? dsFull : dsFull.replace(
                    this.prefix, "");
                result.add(ds);
                log.debug(" * <{}>{}", dsFull, (ds.equals(dsFull) ? "" : " (shortened to <" + ds + ">)"));
            }
            log.debug("End list of datasets.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (preparedStatement != null) preparedStatement.close();
            } catch (SQLException ex) {
                log.warn("SQLException caught while trying to close result set.", ex);
            }
        }
        if (conn != null) try {
            conn.close();
        } catch (SQLException ex) {
            log.warn("SQLException caught while trying to close JDBC connection.", ex);
        }
        return result;
    }

    @Override
    public boolean hasRight(String key, String resourceID, int right) {
        throw new UnsupportedOperationException("cannot check the rights of a key");
    }

    @Override
    public Set<String> getDataSources() {
        throw new NotImplementedException("NIY");
    }

    @Override
    public boolean grant(String key, String ukey, String resourceID, int right) {
        throw new NotImplementedException("NIY");
    }

}
