package org.mksmart.ecapi.couchdb;

import static org.mksmart.ecapi.couchdb.Config.DB;
import static org.mksmart.ecapi.couchdb.Config.SERVICE_URL;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.NotImplementedException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.access.UnavailablePolicyTableException;
import org.mksmart.ecapi.access.auth.VisibilityChecker;
import org.mksmart.ecapi.api.AssemblyProvider;
import org.mksmart.ecapi.api.DebuggableAssemblyProvider;
import org.mksmart.ecapi.api.GlobalType;
import org.mksmart.ecapi.api.TypeSupport;
import org.mksmart.ecapi.api.id.CanonicalGlobalURI;
import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.id.ScopedGlobalURI;
import org.mksmart.ecapi.api.query.Query;
import org.mksmart.ecapi.commons.couchdb.client.DocumentProvider;
import org.mksmart.ecapi.impl.GlobalTypeImpl;
import org.mksmart.ecapi.impl.query.QueryParser;
import org.mksmart.ecapi.impl.query.SparqlQuery;
import org.mksmart.ecapi.impl.query.SparqlTargetedQuery;
import org.mksmart.ecapi.impl.script.ScriptUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AssemblyProvider} that relies upon the functions in a CouchDB database that stores all the
 * required configurations.
 * 
 * This implementation makes raw REST client calls using a CouchDB {@link DocumentProvider}.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class CouchDbAssemblyProvider implements DebuggableAssemblyProvider {

    public static final String DEFAULT_DESIGN_DOC_ID = "_design/compile";

    private String dB;

    private String designDocId;

    private DocumentProvider<JSONObject> documentProvider;

    private Logger log = LoggerFactory.getLogger(getClass());

    private String server_url;

    public CouchDbAssemblyProvider(Properties configuration, DocumentProvider<JSONObject> documentProvider) {
        this.server_url = configuration.getProperty(SERVICE_URL);
        this.dB = configuration.getProperty(DB);
        this.designDocId = DEFAULT_DESIGN_DOC_ID;
        this.documentProvider = documentProvider;
    }

    private void fallbackPostProcess(CanonicalGlobalURI guri, final Map<URI,List<Query>> queries) {
        String program = null, qtpl = null, superType = null;
        // Use the most specific localise function and query template you can find.

        String st = "type/global:id/" + guri.getEntityType();
        JSONObject stDoc = documentProvider.getDocument(st);
        if (stDoc != null) {
            log.debug("Falling back to global type spec <{}>", st);
            if (stDoc.has("localise")) program = stDoc.getString("localise");
            log.trace(" ... localisation function is '{}'", program);
            if (stDoc.has("query_tpl")) qtpl = stDoc.getString("query_tpl");
            log.trace(" ... query template is is '{}'", qtpl);
            if (stDoc.has("mks:super")) superType = stDoc.getString("mks:super");
            while ((program == null || qtpl == null) && superType != null) {
                log.debug("Falling back to supertype spec <{}>", superType);
                stDoc = documentProvider.getDocument(superType);
                if (program == null && stDoc.has("localise")) {
                    program = stDoc.getString("localise");
                    log.trace(" ... localisation function is '{}'", program);
                }
                if (qtpl == null && stDoc.has("query_tpl")) {
                    qtpl = stDoc.getString("query_tpl");
                    log.trace(" ... query template is is '{}'", qtpl);
                }
                superType = (stDoc.has("mks:super")) ? stDoc.getString("mks:super") : null;
            }
        } else { // No dice, just use the TOP type spec
            GlobalType gt = new GlobalTypeImpl(GlobalType.TOP_URI);
            log.debug("Falling back to TOP type spec <{}>", gt);
            JSONObject gtDoc = documentProvider.getDocument(gt.getId().toString());
            program = gtDoc.getString("localise");
            qtpl = gtDoc.getString("query_tpl");
        }
        if (program != null && qtpl != null) {
            Object deal = ScriptUtils.runJs(
                program,
                "localise",
                new Object[] {
                              guri.getEntityType(),
                              guri instanceof ScopedGlobalURI ? ((ScopedGlobalURI) guri).getIdentifierRealm()
                                      : null,
                              guri instanceof ScopedGlobalURI ? ((ScopedGlobalURI) guri)
                                      .getIdentifyingProperty() : null, guri.getIdentifer()}, String.class);
            log.info(" ... localised result is {}", deal);
            if (deal == null) {
                log.error("Null local URI. Cannot generate query.");
            } else {
                URI tgt = URI.create((String) deal);
                // qtpl = gtDoc.getString("query_tpl");
                if (!queries.containsKey(tgt)) queries.put(tgt, new LinkedList<Query>());
                queries.get(tgt).add(QueryParser.parse(qtpl, tgt));
            }
        }
    }

    private Set<String> filterDatasets(JSONObject view, Set<String> datasetNames) {
        boolean opendata = datasetNames == null;
        Set<String> filtered = new HashSet<>(), checkUs = new HashSet<>();
        JSONArray rows = view.getJSONArray("rows");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            checkUs.add(row.getString("id"));
        }
        if (opendata) try {
            filtered = VisibilityChecker.getInstance().filter(checkUs);
        } catch (UnavailablePolicyTableException e) {
            log.error("Denying all data access due to unavailable policy table.");
            throw new RuntimeException(e);
        }
        else {
            filtered.addAll(datasetNames);
            filtered.retainAll(checkUs);
        }
        return filtered;
    }

    @Override
    public Set<String> getCandidateTypes(GlobalURI guri) {
        log.debug("Requested candidate types for global entity {}", guri);
        if (!(guri instanceof CanonicalGlobalURI)) throw new UnsupportedOperationException(
                "This entity provider only supports candidate retrieval for global URIs that are at least instances of "
                        + CanonicalGlobalURI.class);
        Set<String> candidateTypes = new HashSet<>();
        JSONObject json = documentProvider.getView("type", "short-reverse",
            ((CanonicalGlobalURI) guri).getEntityType());
        JSONArray rows = json.getJSONArray("rows");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            log.debug(" ... Found candidate type {}", row.getString("value"));
            candidateTypes.add(row.getString("value"));
        }
        return candidateTypes;
    }

    @Override
    public Set<URI> getCandidateTypes(URI property) {
        throw new NotImplementedException("NIY");
    }

    @Override
    public String getMicrocompiler(URI type) {
        // Design document URL
        String u = this.server_url + '/' + this.dB + '/' + this.designDocId;
        log.debug("Getting design document from URL <{}>", u);
        JSONObject json = documentProvider.getResource(u);
        Object o = json.get("views");
        if (o == null || !(o instanceof JSONObject)) throw new IllegalStateException(
                "views did not parse as a JSON object");
        Object oP = ((JSONObject) o).get("public");
        if (oP == null || !(oP instanceof JSONObject)) throw new IllegalStateException(
                "public view did not parse as a JSON object");

        String rmap = ((JSONObject) oP).getString("map");
        Pattern p = Pattern.compile("^function\\(doc\\).*case '" + type.toString() + "':(.*)break;.*$");
        Matcher m = p.matcher(rmap);
        if (m.find()) return m.group(1);
        return rmap;
    }

    @Override
    public Map<URI,List<Query>> getQueryMap(GlobalType gtype) {
        return getQueryMap(gtype, false);

    }

    @Override
    public Map<URI,List<Query>> getQueryMap(GlobalType gtype, boolean debug) {
        Map<URI,List<Query>> result = new HashMap<>();
        // list datasets that have the type (new view typedataset)
        log.debug("Prefetching instances of type '{}'", gtype);
        JSONObject jds = documentProvider.getView("compile", "typedataset", gtype.toString());
        JSONArray rows = jds.getJSONArray("rows");
        // Iterate over dataset supports for this type.
        log.debug(" ... found support in {} datasets.", rows.length());
        if (rows.length() == 0) log.debug("<== No dataset support found.");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            JSONObject value = row.getJSONObject("value");
            if (value.has("dataset")) {
                URI ds = URI.create(value.getString("dataset"));
                log.debug(" ... Dataset is {}", ds);
                log.debug("Dataset {}", ds);
                if (value.has("debug") && value.getBoolean("debug") && !debug) {
                    log.debug(" ... is in debug state and debug mode is not set."
                              + " Will not contribute to final data feed.");
                    continue;
                }
                result.put(ds, new LinkedList<Query>());
                log.debug(" ... Queries before: {}", result.get(ds).size());
                // datasets.add(ds);
                if (value.has("fetch_query")) {
                    String fetch = value.getString("fetch_query");
                    log.debug(" ... query is \"{}\"", fetch);
                    Query query = new SparqlTargetedQuery(Query.Type.SPARQL_SELECT, fetch, ds);
                    result.get(ds).add(query);
                    log.debug(" ... Queries after: {}", result.get(ds).size());
                }
            } else log.error("<== FAIL - no query endpoint specified for this dataset. Skipping.");
        }

        return result;
    }

    @Override
    public Map<URI,List<Query>> getQueryMap(GlobalURI guri) {
        return getQueryMap(guri, false);
    }

    @Override
    public Map<URI,List<Query>> getQueryMap(GlobalURI guri, boolean debug) {
        return getQueryMap(guri, Collections.<String> emptySet(), debug);
    }

    @Override
    public Map<URI,List<Query>> getQueryMap(GlobalURI guri, Set<String> datasetNames) {
        return getQueryMap(guri, datasetNames, false);
    }

    @Override
    public Map<URI,List<Query>> getQueryMap(GlobalURI uuri, Set<String> datasetNames, boolean debug) {
        // At this stage, datasetNames could be null, which means "open data".
        log.info("Requested query map for global entity {}", uuri);
        if (!(uuri instanceof CanonicalGlobalURI)) throw new UnsupportedOperationException(
                "This entity provider only supports query mapping for global URIs that are at least instances of "
                        + CanonicalGlobalURI.class);
        CanonicalGlobalURI guri = (CanonicalGlobalURI) uuri;
        Map<URI,List<Query>> result = new HashMap<>();
        if ("has".equals(guri.getEntityType())) { // case for primitive classes
            if (!(guri instanceof ScopedGlobalURI)) throw new UnsupportedOperationException(
                    "This entity provider only supports query mapping for primitive global URIs that are at least instances of "
                            + ScopedGlobalURI.class);
            log.debug("Type is a primitive class.");
            String key = ((ScopedGlobalURI) guri).getIdentifierRealm() + ':'
                         + ((ScopedGlobalURI) guri).getIdentifyingProperty();
            log.debug("Check support for primitive {} (value: {})", guri.getIdentifer(), key);
            JSONObject primMap = documentProvider.getView("compile", "primitives", key);
            JSONArray rows = primMap.getJSONArray("rows");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                JSONObject value = row.getJSONObject("value");
                String ds = value.getString("dataset");
                log.debug("Dataset {}", ds);
                if (value.has("debug") && value.getBoolean("debug") && !debug) {
                    log.debug(" ... is in debug state and debug mode is not set."
                              + " Will not contribute to final data feed.");
                    continue;
                }
                String program = value.getJSONObject("query").getString("generator");
                log.trace("Query generation program: {}", program);
                if (program != null) {
                    Object deal = ScriptUtils.runJs(program, "generator", new Object[] {guri.getIdentifer()},
                        String.class);
                    log.trace("Generated query text: {}", deal);
                    String jep = documentProvider.getDocument(ds).getString(
                        "http://rdfs.org/ns/void#sparqlEndpoint");
                    // TODO delegate choice of query type
                    if ("sparql".equals(value.getJSONObject("query").getString("lang"))) {
                        Query q = new SparqlTargetedQuery(Query.Type.SPARQL_SELECT, (String) deal,
                                URI.create(ds));
                        URI k = URI.create(jep);
                        if (!result.containsKey(k)) result.put(k, new LinkedList<Query>());
                        result.get(k).add(q);
                    }
                }
            }
        } else { // case for asserted classes // start of mda's new version
            Vector<String> sparqleps = new Vector<String>();
            Vector<Query> queries = new Vector<Query>();
            // list datasets that have the type (new view typedataset)
            String entType = "type/global:id/" + guri.getEntityType();
            log.debug("Scanning dataset support for type '{}'", entType);
            JSONObject jds = documentProvider.getView("compile", "typedataset", entType);
            JSONArray rows = jds.getJSONArray("rows");
            // Iterate over dataset supports for this type.
            log.debug(" ... found support in {} datasets.", rows.length());
            if (rows.length() == 0) log.debug("<== No dataset support found.");
            Set<String> allowed = filterDatasets(jds, datasetNames);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                log.info("{}: dataset <{}>", i, row.getString("id"));
                JSONObject value = row.getJSONObject("value");
                // Debug restrictions come first.
                if (value.has("debug") && value.getBoolean("debug") && !debug) {
                    log.debug(" ... is in debug state and debug mode is not set."
                              + " Will not contribute to final data feed.");
                    continue;
                }
                if (allowed != null && !allowed.contains(row.getString("id"))) {
                    log.debug(" ... NOT allowed with supplied credentials! Skipping...");
                    continue;
                }
                String sep = value.getString("sep");
                String luri = null;
                if (sep == null) {
                    log.error("<== FAIL - no query endpoint specified for this dataset. Skipping.");
                    continue;
                }
                log.debug(" ... Query endpoint is <{}>", sep);

                // Look for the localise function in the dataset first, then in the type spec
                String localise = value.has("localise") ? value.getString("localise")
                        : localiseFromType(entType);
                if (localise == null) {
                    log.error("<== FAIL - no localise function specified for this dataset. Skipping.");
                    continue;
                }
                Object deal = ScriptUtils.runJs(
                    localise,
                    "localise",
                    new Object[] {
                                  guri.getEntityType(),
                                  guri instanceof ScopedGlobalURI ? ((ScopedGlobalURI) guri)
                                          .getIdentifierRealm() : null,
                                  guri instanceof ScopedGlobalURI ? ((ScopedGlobalURI) guri)
                                          .getIdentifyingProperty() : null, guri.getIdentifer()},
                    String.class);
                log.info("<== DONE - Local URI is {}", deal);
                luri = (String) deal;
                if (luri == null || luri.isEmpty()) {
                    log.debug("No local URI could be obtained from dataset <{}>. Skipping",
                        row.getString("id"));
                    continue;
                }
                Query query;
                // the dataset spec could have a standard query template
                if (value.has("query_tpl")) {
                    log.debug(" ... query template: \"{}\"", value.getString("query_tpl"));
                    URI tgt = URI.create(luri);
                    query = QueryParser.parse(value.getString("query_tpl"), tgt);
                    if (value.has("dataset")) query = query.wrap(URI.create(value.getString("dataset")));
                }
                // or it could have a bespoke query text
                else if (value.has("query_text")) {
                    String query_text = value.getString("query_text");
                    query_text = query_text.replaceAll("\\[LURI\\]", luri);
                    log.debug(" ... query text: \"{}\"", query_text);
                    if (value.has("dataset")) query = new SparqlTargetedQuery(Query.Type.SPARQL_SELECT,
                            query_text, URI.create(value.getString("dataset")));
                    else query = new SparqlQuery(Query.Type.SPARQL_SELECT, query_text);
                }
                // otherwise, the type spec could have one
                else query = queryFromType(entType, luri, value.has("dataset") ? value.getString("dataset")
                        : null);
                log.debug(" ... Query is \"{}\"", query);
                if (query != null) { // that sep is not null we already know by now
                    queries.add(query);
                    sparqleps.add(sep);
                }
            }
            if (queries.size() != sparqleps.size()) {
                log.error("No match for queries and endpoints! #queries = {} , #endpoints = {}",
                    queries.size(), sparqleps.size());
            } else for (int j = 0; j < sparqleps.size(); j++) {
                URI tgt = URI.create(sparqleps.elementAt(j));
                if (result.get(tgt) == null) result.put(tgt, new LinkedList<Query>());
                result.get(tgt).add(queries.elementAt(j));
            }
        }
        // If no queries were generated, do general fallback
        if (result.isEmpty()) fallbackPostProcess(guri, result);

        return result;
    }

    @Override
    public Set<URI> getSupportedTypes() {
        return getSupportedTypes(Collections.<String> emptySet());
    }

    @Override
    public Set<URI> getSupportedTypes(Set<String> datasetNames) {
        Set<URI> result = new HashSet<>();
        JSONObject typeMap = documentProvider.getView("compile", "typemaps");
        JSONArray rows = typeMap.getJSONArray("rows");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            result.add(URI.create(row.getString("key")));
        }
        return result;
    }

    @Override
    public Set<URI> getTypeAliases(GlobalType type) {
        Set<URI> result = new HashSet<>();
        JSONObject typeMap = documentProvider.getView("type", "short-reverse", type.getId().getIdentifer());
        JSONArray rows = typeMap.getJSONArray("rows");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            result.add(URI.create(row.getString("value")));
        }
        return result;
    }

    @Override
    public TypeSupport getTypeSupport(GlobalType type) {
        return getTypeSupport(type, false);
    }

    @Override
    public TypeSupport getTypeSupport(GlobalType type, boolean debug) {
        TypeSupport result = TypeSupport.create(type);
        log.info("type {}", type.getId());
        JSONObject typeMap = documentProvider.getView("compile", "typedataset", type.getId().toString());
        JSONArray rows = typeMap.getJSONArray("rows");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i).getJSONObject("value");
            log.trace(row.toString());
            if (row.has("dataset")) {
                URI uds = URI.create(row.getString("dataset"));
                log.debug("Dataset {}", uds);
                if (row.has("debug") && row.getBoolean("debug") && !debug) {
                    log.debug(" ... is in debug state and debug mode is not set."
                              + " Will not contribute to final data feed.");
                    continue;
                }
                result.addDataset(uds);
                if (row.has("examples")) {
                    log.debug(" ... has examples");
                    JSONArray exs = row.getJSONArray("examples");
                    for (int j = 0; j < exs.length(); j++) {
                        String stype = type.getId().toString();
                        stype = stype.substring(stype.lastIndexOf('/') + 1, stype.length());
                        GlobalURI exampleId = new CanonicalGlobalURI(stype, exs.getString(j));
                        result.addExampleInstance(uds, exampleId);
                    }
                }
            }

        }
        return result;
    }

    @Override
    public Set<Set<URI>> getUnifiers(URI type) {
        throw new NotImplementedException("NIY");
    }

    private String localiseFromType(String type) {
        log.debug("Selecting localisation function for type '{}'", type);
        JSONObject typev = documentProvider.getView("compile", "typefunctions", type);
        JSONArray rows = typev.getJSONArray("rows");
        log.debug(" ... Obtained {} types", rows.length());
        if (rows.length() == 1) {
            JSONObject val = rows.getJSONObject(0).getJSONObject("value");
            if (val.has("localise")) return val.getString("localise");
            else if (val.has("super")) return localiseFromType(val.getString("super"));
        }
        log.warn("Got more than one type functions, will skip type '{}'", type);
        return null;
    }

    /**
     * Query for retrieving an entity signature based only on its type.
     * 
     * @param type
     * @param luri
     * @return
     */
    private Query queryFromType(String type, String luri, String dataset) {
        if (luri == null) throw new IllegalArgumentException("Local URI cannot be null.");
        log.debug("Extracting query for type '{}'", type);
        JSONObject typev = documentProvider.getView("compile", "typefunctions", type);
        JSONArray rows = typev.getJSONArray("rows");
        log.debug(" ... Obtained {} types", rows.length());
        if (rows.length() == 1) {
            JSONObject val = rows.getJSONObject(0).getJSONObject("value");
            if (val.has("query_tpl")) {
                log.trace(" ... Query template is {}", val.getString("query_tpl"));
                URI tgt = URI.create(luri);
                Query query = QueryParser.parse(val.getString("query_tpl"), tgt);
                if (dataset != null) query = query.wrap(URI.create(dataset));
                log.trace(" ... Query is {}", query);
                return query;
            } else if (val.has("query_text")) {
                String query_text = val.getString("query_text");
                query_text = query_text.replaceAll("\\[LURI\\]", luri);
                log.trace(" ... query is {}", query_text);

                Query query;
                if (dataset != null) query = new SparqlTargetedQuery(Query.Type.SPARQL_SELECT, query_text,
                        URI.create(dataset));
                else query = new SparqlQuery(Query.Type.SPARQL_SELECT, query_text);
                return query;
            } else if (val.has("super")) return queryFromType(val.getString("super"), luri, dataset);
        } else log.warn("Got more than one type functions, will skip type '{}'", type);
        return null;
    }

}
