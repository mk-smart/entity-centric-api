package org.mksmart.ecapi.access.auth;

import static org.mksmart.ecapi.access.Config.KEYMGMT_DATASET_PREFIX;

import java.net.URI;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.access.ConcurrentPolicyTable;
import org.mksmart.ecapi.access.Config;
import org.mksmart.ecapi.api.access.PolicyTable;
import org.mksmart.ecapi.core.LaunchConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dataset access policy checker that polls an authorisation server as supplied in the singleton
 * {@link LaunchConfiguration}.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class AuthServerPolicyChecker {

    enum Format {
        MKSMART_AUTH_10,
        MKSMART_AUTH_11
    }

    private URI authSvrLoc;

    private RestClient client;

    private Logger log = LoggerFactory.getLogger(getClass());

    private String prefix = null;

    public AuthServerPolicyChecker() {
        ClientConfig clientConf = new ClientConfig();
        client = new RestClient(clientConf);
        LaunchConfiguration config = LaunchConfiguration.getInstance();
        if (!config.has(Config.KEYMGMT_AUTHSVR_HOST)) {
            log.warn("No authorisation server URI given. Treating all datasets as open!");
        } else {
            authSvrLoc = URI.create((String) config.get(Config.KEYMGMT_AUTHSVR_HOST));
            log.info("Setting authorisation server as <{}>", authSvrLoc);
            boolean usePrefix = Boolean.parseBoolean((String) config.get(Config.KEYMGMT_AUTHSVR_USEPREFIX));
            if (usePrefix) this.prefix = (String) config.get(KEYMGMT_DATASET_PREFIX);
            if (this.prefix == null) this.prefix = "";
        }
    }

    public PolicyTable getPolicies(Set<String> datasetIds) {
        return getPolicies(datasetIds, VisibilityChecker.USER_EVERYONE);
    }

    public PolicyTable getPolicies(Set<String> datasetIds, String user) {
        // Initially, everything is open
        PolicyTable res = new ConcurrentPolicyTable();
        for (String ds : datasetIds)
            res.put(ds, true);
        if (authSvrLoc == null) return res;
        Resource resource = client.resource(authSvrLoc);
        long before = System.currentTimeMillis();
        JSONObject data = resource.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                .post(JSONObject.class, makeRequestEntity(datasetIds, user, Format.MKSMART_AUTH_11));
        log.trace(" ... turnaround time for POST to auth server: {} ms", System.currentTimeMillis() - before);
        log.trace("Detailed authorisation server RESPONSE follows:");
        log.trace("{}", data);
        /*
         * Expected response format:
         * 
         * {"userId": "guest", "datasets": [ {"datasetId": "<dataset id>", "privilege": {"view": <boolean>,
         * "subscribe": <boolean>}} ] }
         */
        if (!data.has("datasets")) throw new IllegalStateException(
                "Authorisation server response had no key 'datasets'.");
        JSONArray rows = data.getJSONArray("datasets");
        for (int i = 0; i < rows.length(); i++) {
            log.trace("Checking {}", i);
            JSONObject dsData = rows.getJSONObject(i);
            if (!dsData.has("datasetId")) {
                log.warn("No property 'datasetId' is specified for response item #{}. Will skip.", i);
                log.warn("Note that datasets for which the authorisation server does not respond are considered open!");
                continue;
            }
            String fullId = dsData.getString("datasetId");
            log.trace("Dataset full ID = {}", fullId);
            log.trace("Prefix = {}", this.prefix);
            String id = this.prefix == null || this.prefix.isEmpty() ? fullId.replace(this.prefix, "")
                    : fullId;
            log.trace("Checking rights for dataset <{}>", id);
            if (!id.equals(fullId)) log.debug(" ... (was <{}>)", fullId);
            JSONObject rights = dsData.getJSONObject("privilege");
            log.trace(" ... 'view' : {}", rights.getBoolean("view"));
            log.trace(" ... 'subscribe' : {}", rights.getBoolean("subscribe"));
            if (datasetIds.contains(id) && rights.getBoolean("view") && rights.getBoolean("subscribe")) {
                log.trace(" ... dataset is considered open as it can be viewed and subscribed to by unauthenticated clients.");
            } else {
                log.trace(" ... dataset will NOT be considered open.");
                res.put(id, false);
            }
        }
        return res;
    }

    protected JSONObject makeRequestEntity(Set<String> datasets, String user, Format format) {

        JSONObject obj = new JSONObject();
        obj.put("userId", user);
        JSONArray dsa = new JSONArray();
        for (String ds : datasets) {
            String tds = prefix == null || prefix.isEmpty() ? "" : this.prefix;
            tds += ds;
            switch (format) {
                case MKSMART_AUTH_10:
                    /*
                     * Request format (Auth server R1.0):
                     * 
                     * { "userId": "guest", "datasetIds": [ {"id" : "<dataset id>" } ... ] }
                     */
                    JSONObject temp = new JSONObject();
                    temp.put("id", tds);
                    dsa.put(temp);
                    break;

                case MKSMART_AUTH_11:
                    /*
                     * Request format (Auth server R1.1):
                     * 
                     * { "userId": "guest", "datasetIds": [ "<dataset id>" ... ] }
                     */
                    dsa.put(tds);
                    break;
            }
        }
        obj.put("datasetIds", dsa);
        log.trace("Detailed authorisation server REQUEST follows:");
        log.trace("{}", obj);
        return obj;
    }

}
