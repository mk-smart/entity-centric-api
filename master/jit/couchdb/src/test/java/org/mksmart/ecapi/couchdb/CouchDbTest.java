package org.mksmart.ecapi.couchdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Properties;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.json.JSONObject;
import org.junit.BeforeClass;

/**
 * Inhibited. No need to use this system to check whether CouchDB works.
 * 
 * @author alessandro <alexdma@apache.org>
 *
 */
public class CouchDbTest {

    private static RestClient client;

    private static Properties config;

    private static final String PROPERTIES_COUCHDB = "couchdb.properties";

    @BeforeClass
    public static void init() throws IOException {
        client = new RestClient();
        config = new Properties();
        config.load(CouchDbTest.class.getClassLoader().getResourceAsStream(PROPERTIES_COUCHDB));
    }

    // @Test
    public void testC__D() throws Exception {
        String url = config.getProperty("couchdb.url");
        String db = config.getProperty("couchdb.db");
        String id = "org.mksmart.entityjit.test:" + System.currentTimeMillis();
        // Resource is the document to create and delete
        Resource resource = client.resource(url + '/' + db + '/' + id);
        // Create
        ClientResponse resp = resource.contentType(MediaType.APPLICATION_JSON).put("{}");
        assertEquals(Status.CREATED, resp.getStatusType());
        String response = resource.accept(MediaType.APPLICATION_JSON).get(String.class);
        assertNotNull(response);
        // Delete
        JSONObject json = new JSONObject(response);
        assertEquals(id, json.get("_id"));
        Object rev = json.get("_rev");
        assertNotNull(rev);
        assertTrue(rev instanceof String);
        resp = resource.header(HttpHeaders.IF_MATCH, (String) rev).delete();
        assertEquals(Status.OK, resp.getStatusType());
        // Verify it's no longer there
        resp = resource.get();
        assertEquals(Status.NOT_FOUND, resp.getStatusType());
    }

    // @Test
    public void testDbAccess() throws Exception {
        String url = config.getProperty("couchdb.url");
        String db = config.getProperty("couchdb.db");
        // Resource is the configured database
        Resource resource = client.resource(url + '/' + db);
        // perform a GET on the resource. The resource will be returned as plain text
        String response = resource.accept(MediaType.APPLICATION_JSON).get(String.class);
        assertNotNull(response);
        JSONObject json = new JSONObject(response);
        assertEquals(db, json.get("db_name"));
    }

    // @Test
    public void testDbPing() throws Exception {
        String url = config.getProperty("couchdb.url");
        // Resource is the CouchDB instance
        Resource resource = client.resource(url);
        // perform a GET on the resource. The resource will be returned as plain text
        String response = resource.accept(MediaType.APPLICATION_JSON).get(String.class);
        assertNotNull(response);
        JSONObject json = new JSONObject(response);
        assertEquals("welcome", json.getString("couchdb").toLowerCase());
        assertNotNull(json.get("uuid"));
    }

}
