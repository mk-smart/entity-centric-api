package org.mksmart.ecapi.couchdb.storage;

import java.net.URI;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.wink.client.ClientWebException;
import org.apache.wink.client.Resource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.EntityFragment;
import org.mksmart.ecapi.api.query.TargetedQuery;
import org.mksmart.ecapi.api.storage.FragmentStore;
import org.mksmart.ecapi.commons.couchdb.client.RemoteDocumentProvider;
import org.mksmart.ecapi.couchdb.Config;
import org.mksmart.ecapi.couchdb.util.EncodeUtils;
import org.mksmart.ecapi.impl.format.JsonGenericRepresentationDeserializer;
import org.mksmart.ecapi.impl.format.JsonGenericRepresentationSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class FragmentPerQueryStore extends RemoteDocumentProvider implements FragmentStore<TargetedQuery> {

    private Config config;

    private Logger log = LoggerFactory.getLogger(getClass());

    public FragmentPerQueryStore(Config config) {
        super(config.getServiceURL(), config.getStorageDbName()/* config.getDataDbName() */,
                new UsernamePasswordCredentials(new String(config.getUsername()).trim(), new String(
                        config.getPassword()).trim()));
        this.config = config;
    }

    public Config getConfiguration() {
        return config;
    }

    @Override
    public Class<TargetedQuery> getSupportedKeyType() {
        return TargetedQuery.class;
    }

    @Override
    public Class<EntityFragment> getSupportedValueType() {
        return EntityFragment.class;
    }

    @Override
    public EntityFragment retrieve(TargetedQuery key) {
        long before = System.currentTimeMillis();
        EntityFragment result = null;
        log.debug("Will retrieve cached fragment provided by <{}>", key.getTarget());
        log.trace("Detailed fragment descriptor follows :\r\n{}", key.toString());
        String didEnc = EncodeUtils.encode(key);
        String u = config.getServiceURL().toString() + '/' + config.getStorageDbName() + '/' + didEnc;
        Resource resource = client.resource(u);
        try {
            log.debug("GETting document with name '{}' using DB '{}'", key.getTarget(), super.dbName);
            log.debug("GETting document at <{}>", u);
            JSONObject data = resource.accept(MediaType.APPLICATION_JSON).get(JSONObject.class);
            result = JsonGenericRepresentationDeserializer.deserialize(data, true);
        } catch (ClientWebException ex) {
            if (Status.NOT_FOUND.equals(ex.getResponse().getStatusType())) log
                    .debug(" ... resource was not present, will PUT.");
            else {
                log.error("Remote Database reported: \"{} {}\"", ex.getResponse().getStatusCode(), ex
                        .getResponse().getStatusType());
                throw ex;
            }
        }
        log.info(" ... retrieval overhead: {} ms (dataset: <{}>)", System.currentTimeMillis() - before,
            key.getTarget());
        return result;
    }

    @Override
    public TargetedQuery store(EntityFragment item) {
        throw new UnsupportedOperationException(
                "This implementation does not support automatic generation of storage keys."
                        + " Please use store(EntityFragment, TargetedQuery) instead.");
    }

    @Override
    public TargetedQuery store(EntityFragment item, TargetedQuery key) {
        long before = System.currentTimeMillis();
        String didEnc = EncodeUtils.encode(key);
        String u = config.getServiceURL().toString() + '/' + config.getStorageDbName() + '/' + didEnc;
        log.debug("PUTting document at <{}>", u);
        Resource resource = client.resource(u);
        JSONObject json = new JSONObject(); // holds the final serialised object
        json.put("created", before);
        json.put("dataset", key.getTarget());
        if (item instanceof Entity) {
            JSONArray equivs = new JSONArray();
            for (URI equiv : ((Entity) item).getAliases())
                equivs.put(equiv);
            json.put("equivalents", equivs);
        }
        JSONObject repr = JsonGenericRepresentationSerializer.serialize(item);
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
        return key;
    }

}
