package org.mksmart.ecapi.couchdb.storage;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.api.query.Query;
import org.mksmart.ecapi.api.query.Query.Type;
import org.mksmart.ecapi.api.query.TargetedQuery;
import org.mksmart.ecapi.api.storage.Cache;
import org.mksmart.ecapi.commons.couchdb.client.DocumentProvider;
import org.mksmart.ecapi.couchdb.util.EncodeUtils;
import org.mksmart.ecapi.impl.query.SparqlTargetedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheImpl implements Cache {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static final String DESIGN_DOC_ID = "cachetable", VIEW_ID = "ages";

    private DocumentProvider<JSONObject> documentProvider;

    private FragmentPerQueryStore store;

    public CacheImpl(DocumentProvider<JSONObject> documentProvider, FragmentPerQueryStore store) {
        this.documentProvider = documentProvider;
        this.store = store;
    }

    @Override
    public Set<TargetedQuery> getCacheHits(Map<URI,List<Query>> queryPlan) {
        if (queryPlan.isEmpty()) {
            log.debug("Query plan is empty - Skipping cache hit check.");
            return Collections.emptySet();
        }
        long before = System.currentTimeMillis();
        Set<TargetedQuery> output = new HashSet<>();
        Set<String> targets = new HashSet<>();
        Map<String,TargetedQuery> encodings = new HashMap<>();
        Map<String,Long> lifetimes = new HashMap<>();

        // Compute query encodings and dataset names.
        for (Entry<URI,List<Query>> entry : queryPlan.entrySet()) {
            log.debug("Inspecting queries directed to <{}>", entry.getKey());
            for (Query qq : entry.getValue()) {
                TargetedQuery tq;
                if (qq instanceof TargetedQuery) tq = (TargetedQuery) qq;
                else switch (qq.getQueryType()) {
                    case SPARQL_DESCRIBE:
                    case SPARQL_SELECT:
                        tq = new SparqlTargetedQuery(Type.SPARQL_SELECT, qq.getSynopsis(),
                                qq.getResultEntryPoint(), entry.getKey());
                        break;
                    default:
                        log.warn("Generation of targeted query for type {} is not implemented.",
                            qq.getQueryType());
                        tq = null;
                }
                if (tq != null) {
                    encodings.put(EncodeUtils.encode(tq), tq);
                    targets.add(tq.getTarget().toString());
                }
            }
        }
        // Get the cache durations for all the datasets of interest.
        JSONObject dss = documentProvider.getView("catalogue", "datasets", targets.toArray(new String[0]));
        JSONArray rows = dss.getJSONArray("rows");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            JSONObject v = row.getJSONObject("value");
            if (v.has("cache_lifetime")) {
                log.debug("Storing lifetime dataset <{}> is {} ms.", row.getString("key"),
                    v.getLong("cache_lifetime"));
                lifetimes.put(row.getString("key"), v.getLong("cache_lifetime"));
            }

        }
        // Now do the filtering
        Set<String> keys = encodings.keySet();
        JSONObject hits = store.getView(DESIGN_DOC_ID, VIEW_ID, keys.toArray(new String[0]));
        rows = hits.getJSONArray("rows");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String k = row.getString("key"); // The encoded query
            if (!keys.contains(k)) {
                log.warn("Cache hit filtering returned an encoded TargetedQuery that was not requested. Discarding.");
                log.warn("This might be due to having traced up to a supertype.");
                log.warn(" ... (got {} cache table entries; {} were requested)", rows.length(), keys.size());
                continue;
            }

            long lifetime = -1;
            // If true, the data must always be retrieved from the cache, if present.
            boolean forceCacheHit = false;
            String ds = encodings.get(k).getTarget().toString();
            log.debug("Is there a set lifetime for <{}>? {}", ds, lifetimes.containsKey(ds));
            if (lifetimes.containsKey(ds) && lifetimes.get(ds) != null) lifetime = lifetimes.get(ds);
            else forceCacheHit = true;

            log.debug("Checking timestamp of cached document.");
            long cached = row.getLong("value");
            log.trace(" ... caching time was {} - {}", cached, new Date(cached));
            long age = before - cached;
            log.debug(" ... age is {} - lifetime is {}", age, lifetime);
            log.debug(" ... valid cache? {} - forced retrieve? {}", age < lifetime, forceCacheHit);
            if (forceCacheHit || age < lifetime) {
                log.debug("|<== Registering as cache hit.");
                output.add(encodings.get(k));
            }
        }
        return output;
    }
}
