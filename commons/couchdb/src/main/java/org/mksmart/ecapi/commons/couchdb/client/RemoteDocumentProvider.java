package org.mksmart.ecapi.commons.couchdb.client;

import java.net.URL;

import javax.security.auth.login.Configuration;
import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.http.auth.Credentials;
import org.apache.wink.client.ClientAuthenticationException;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.ClientRuntimeException;
import org.apache.wink.client.ClientWebException;
import org.apache.wink.client.Resource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A document provider that uses the CouchDB RESTful API (via the Wink client, not Ektorp) and the singleton
 * {@link Configuration} to fetch documents as {@link JSONObject}s.
 * 
 * These document providers are lightweight objects that can be easily instantiated and garbage-collected on
 * every service call if need be.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class RemoteDocumentProvider extends RemoteDocumentHandlerBase implements DocumentProvider<JSONObject> {

    private Logger log = LoggerFactory.getLogger(getClass());

    public RemoteDocumentProvider(URL serviceUrl, String dbName) {
        super(serviceUrl, dbName);
    }

    public RemoteDocumentProvider(URL serviceUrl, String dbName, Credentials credentials) {
        super(serviceUrl, dbName, credentials);
    }

    @Override
    public JSONObject getDocument(String documentId) {
        log.debug("Requested CouchDB document {}", documentId);
        String didEnc;
        try {
            didEnc = new URLCodec().encode(documentId);
        } catch (EncoderException e) {
            log.error("Retrieval of document with ID {} FAILED. ", documentId);
            log.error(" ... reason: ID URLencode failed.");
            throw new RuntimeException(e);
        }
        String u = this.serviceUrl.toString() + '/' + this.dbName + '/' + didEnc;
        return getResource(u);
    }

    @Override
    public JSONObject getDocuments(String... keys) {
        log.debug("Requested all CouchDB documents");
        for (String k : keys)
            log.debug(" ... with key \"{}\"", k);
        String u = this.serviceUrl.toString() + '/' + this.dbName + '/' + "_all_docs?include_docs=true";
        return getResource(u, keys);
    }

    @Override
    public JSONObject getReducedView(String designDocId, String viewId, boolean group, String... keys) {
        log.debug("Requested CouchDB view {}:{}", designDocId, viewId);
        log.debug(" ... reduce grouping = {}", group);
        for (String k : keys)
            log.debug(" ... with key \"{}\"", k);
        try {
            designDocId = new URLCodec().encode(designDocId);
        } catch (EncoderException e) {
            log.error("Retrieval of view '{}/view/{}' FAILED. ", designDocId, viewId);
            log.error(" ... reason: could not URLencode design document ID '{}'.", designDocId);
            throw new RuntimeException(e);
        }
        try {
            viewId = new URLCodec().encode(viewId);
        } catch (EncoderException e) {
            log.error("Retrieval of view '{}/view/{}' FAILED. ", designDocId, viewId);
            log.error(" ... reason: could not URLencode view name '{}'.", viewId);
            throw new RuntimeException(e);
        }
        String u = this.serviceUrl.toString() + '/' + this.dbName + '/' + "_design" + '/' + designDocId + '/'
                   + "_view" + '/' + viewId + '?' + "group=" + group;
        return getResource(u, keys);
    }

    @Override
    public JSONObject getResource(String url, String... keys) {
        Resource resource = this.client.resource(url);
        if (keys != null && keys.length > 0) resource.queryParam("keys", new JSONArray(keys));
        // TODO reimplement using Jackson/Ektorp
        JSONObject response;
        try {
            response = resource.accept(MediaType.APPLICATION_JSON).get(JSONObject.class);
            log.trace("{}", response);
        } catch (ClientAuthenticationException ex) {
            log.error("Database authentication failed. Reason: {}", ex.getMessage());
            log.error("Failing resource was: <{}>", url);
            throw ex;
        } catch (ClientWebException ex) {
            ClientResponse resp = ex.getResponse();
            log.warn("Retrieval failed. Reason: client response was '{} {}'", resp.getStatusCode(),
                resp.getStatusType());
            log.warn("Failing resource was: <{}>", url);
            response = null;
        } catch (ClientRuntimeException ex) {
            log.error("Retrieval failed. Reason: '{}'", ex.getMessage());
            log.error("Failing resource was: <{}>", url);
            response = null;
        }
        return response;
    }

    @Override
    public JSONObject getView(String designDocId, String viewId, String... keys) {
        log.debug("Requested CouchDB view {}:{}", designDocId, viewId);
        for (String k : keys)
            log.debug(" ... with key \"{}\"", k);
        try {
            designDocId = new URLCodec().encode(designDocId);
        } catch (EncoderException e) {
            log.error("Retrieval of view '{}/view/{}' FAILED. ", designDocId, viewId);
            log.error(" ... reason: could not URLencode design document ID '{}'.", designDocId);
            throw new RuntimeException(e);
        }
        try {
            viewId = new URLCodec().encode(viewId);
        } catch (EncoderException e) {
            log.error("Retrieval of view '{}/view/{}' FAILED. ", designDocId, viewId);
            log.error(" ... reason: could not URLencode view name '{}'.", viewId);
            throw new RuntimeException(e);
        }
        String u = this.serviceUrl.toString() + '/' + this.dbName + '/' + "_design" + '/' + designDocId + '/'
                   + "_view" + '/' + viewId;
        return getResource(u, keys);
    }

}
