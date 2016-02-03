package org.mksmart.ecapi.couchdb.storage;

import java.net.URI;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.ClientWebException;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.apache.wink.client.handlers.BasicAuthSecurityHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.id.IdGenerator;
import org.mksmart.ecapi.api.storage.EntityStore;
import org.mksmart.ecapi.commons.couchdb.client.RemoteDocumentProvider;
import org.mksmart.ecapi.couchdb.Config;
import org.mksmart.ecapi.couchdb.id.HeuristicIdGenerator;
import org.mksmart.ecapi.impl.format.JsonSimplifiedGenericRepresentationDeserializer;
import org.mksmart.ecapi.impl.format.JsonSimplifiedGenericRepresentationSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CouchDB wrapper
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class SimpleEntityStore implements EntityStore<URI> {

    /**
     * TODO centralise {@link RestClient}
     */
    private RestClient client;

    private Config config;

    private IdGenerator<URI,Entity> gen;

    private Logger log = LoggerFactory.getLogger(getClass());

    public SimpleEntityStore(Config config) {
        if (config == null) throw new IllegalArgumentException("config cannot be null");
        this.config = config;
        ClientConfig cconf = new ClientConfig();
        BasicAuthSecurityHandler auth = new BasicAuthSecurityHandler();
        // Only set credentials if there is a username, no spaces around
        String un = null, pw = null;
        if (this.config.getUsername() != null) {
            un = new String(this.config.getUsername()).trim();
            if (!un.isEmpty()) {
                auth.setUserName(un);
                if (this.config.getPassword() != null) {
                    pw = new String(this.config.getPassword()).trim();
                    if (!pw.isEmpty()) auth.setPassword(pw);
                }

            }
        }
        cconf.handlers(auth);
        client = new RestClient(cconf);
        this.gen = new HeuristicIdGenerator(new RemoteDocumentProvider(this.config.getServiceURL(),
                this.config.getDataDbName(), new UsernamePasswordCredentials(un, pw)));
    }

    public Config getConfiguration() {
        return config;
    }

    @Override
    public Class<URI> getSupportedKeyType() {
        return URI.class;
    }

    @Override
    public Class<Entity> getSupportedValueType() {
        return Entity.class;
    }

    @Override
    public Entity retrieve(URI globalId) {
        String didEnc;
        try {
            didEnc = new URLCodec().encode(globalId.toString());
        } catch (EncoderException e) {
            log.error("Failed to URLencode document ID <" + globalId + ">. This should not happen.", e);
            throw new RuntimeException(e);
        }
        String u = config.getServiceURL().toString() + '/' + config.getStorageDbName() + '/' + didEnc;
        log.debug("GETting document at <{}>", u);
        Resource resource = client.resource(u);
        JSONObject response;
        try {
            response = resource.accept(MediaType.APPLICATION_JSON).get(JSONObject.class);
            log.debug("Cache hit.");
            log.trace("{}", response);
        } catch (ClientWebException ex) {
            ClientResponse resp = ex.getResponse();
            if (resp.getStatusType() == Status.NOT_FOUND) log.warn("Cache miss on entity <{}>", globalId);
            else {
                log.warn("Cache access failure.");
                log.warn(" ... requested entity: <{}>.", globalId);
                log.warn(" ... reason: client response was '{} {}'", resp.getStatusCode(),
                    resp.getStatusType());
            }
            return null;
        }
        Entity e = JsonSimplifiedGenericRepresentationDeserializer.deserialize(response, true);
        return e;
    }

    @Override
    public URI store(Entity entity) {
        return store(entity, this.gen.createId(entity));
    }

    @Override
    public URI store(Entity entity, URI globalId) {
        long before = System.currentTimeMillis();
        String didEnc;
        try {
            didEnc = new URLCodec().encode(globalId.toString());
        } catch (EncoderException e) {
            log.error("Failed to URLencode document ID <" + globalId + ">. This should not happen.", e);
            throw new RuntimeException(e);
        }

        String u = config.getServiceURL().toString() + '/' + config.getStorageDbName() + '/' + didEnc;
        log.debug("PUTting document at <{}>", u);
        Resource resource = client.resource(u);

        JSONObject json = new JSONObject();
        JSONArray equivs = new JSONArray();
        for (URI equiv : entity.getAliases())
            equivs.put(equiv);
        json.put("equivalents", equivs);
        JSONObject repr = JsonSimplifiedGenericRepresentationSerializer.serialize(entity, globalId);

        json.put("global-representation", repr);
        try {
            String info = resource.accept(MediaType.APPLICATION_JSON).get(String.class);
            JSONObject oldDoc = new JSONObject(info);
            String rev = oldDoc.getString("_rev");
            log.trace(" ... revision was: {}", rev);
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
                    .put(JSONObject.class, json.toString());
            log.debug("<== SUCCESS - Document stored.");
            log.debug("Time: {} ms", System.currentTimeMillis() - b1);
        } catch (ClientWebException ex) {
            log.error("Remote Database reported: \"{} {}\"", ex.getResponse().getStatusCode(), ex
                    .getResponse().getStatusType());
            throw ex;

        }
        log.trace("Response was: {}", response);
        log.info("Storage overhead: {} ms", System.currentTimeMillis() - before);
        return globalId;
    }

}
