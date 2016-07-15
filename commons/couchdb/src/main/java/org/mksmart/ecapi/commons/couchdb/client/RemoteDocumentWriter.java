package org.mksmart.ecapi.commons.couchdb.client;

import java.net.URL;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.http.auth.Credentials;
import org.apache.wink.client.ClientWebException;
import org.apache.wink.client.Resource;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author alessandro <alexdma@apache.org>
 *
 */
public class RemoteDocumentWriter extends RemoteDocumentHandlerBase implements DocumentWriter<JSONObject> {

    private Logger log = LoggerFactory.getLogger(getClass());

    public RemoteDocumentWriter(URL serviceUrl, String dbName) {
        super(serviceUrl, dbName);
    }

    public RemoteDocumentWriter(URL serviceUrl, String dbName, Credentials credentials) {
        super(serviceUrl, dbName, credentials);
    }

    @Override
    public boolean addDesignDocument(JSONObject doc, String id, boolean replace) {
        return addDocument(doc, id, "_design/", replace);
    }

    @Override
    public boolean addDocument(JSONObject doc, boolean replace) {
        if (!doc.has("_id")) throw new IllegalArgumentException(
                "The document must have a value for '_id', otherwise you must call the variant with the id.");
        return addDocument(doc, doc.getString("_id"), replace);
    }

    @Override
    public boolean addDocument(JSONObject doc, String id, String path, boolean replace) {
        long before = System.currentTimeMillis();
        String didEnc;
        try {
            didEnc = new URLCodec().encode(id.toString());
        } catch (EncoderException e) {
            log.error("Failed to URLencode document ID <" + id + ">. This should not happen.", e);
            throw new RuntimeException(e);
        }

        String u = this.serviceUrl.toString() + '/' + this.dbName + '/' + path + didEnc;
        log.debug("PUTting document at <{}>", u);
        Resource resource = client.resource(u);

        try {
            String info = resource.accept(MediaType.APPLICATION_JSON).get(String.class);
            JSONObject oldDoc = new JSONObject(info);
            String rev = oldDoc.getString("_rev");
            log.trace(" ... revision was: {}", rev);
            if (!replace) return false;
            resource.header(HttpHeaders.IF_MATCH, rev).accept(MediaType.APPLICATION_JSON)
                    .delete(String.class);
        } catch (ClientWebException ex) {
            if (Status.NOT_FOUND.equals(ex.getResponse().getStatusType())) log
                    .debug(" ... resource was not present, will PUT.");
            else {
                log.error("Remote Database reported: \"{} {}\"", ex.getResponse().getStatusCode(), ex
                        .getResponse().getStatusType());
                throw ex;
            }
        }
        resource = client.resource(u);
        JSONObject response;
        try {
            long b1 = System.currentTimeMillis();
            response = resource.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .put(JSONObject.class, doc.toString());
            log.debug("<== SUCCESS - Document stored.");
            log.debug("Time: {} ms", System.currentTimeMillis() - b1);
        } catch (ClientWebException ex) {
            log.error("Remote Database reported: \"{} {}\"", ex.getResponse().getStatusCode(), ex
                    .getResponse().getStatusType());
            throw ex;

        }
        log.trace("Response was: {}", response);
        log.info("Storage overhead: {} ms", System.currentTimeMillis() - before);
        return true;
    }

    @Override
    public boolean addDocument(JSONObject doc, String id, boolean replace) {
        return addDocument(doc, id, "", replace);
    }

}
