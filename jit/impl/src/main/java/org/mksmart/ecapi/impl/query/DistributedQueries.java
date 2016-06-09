package org.mksmart.ecapi.impl.query;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.EntityFragment;
import org.mksmart.ecapi.api.query.Query;
import org.mksmart.ecapi.impl.EntityImpl;
import org.mksmart.ecapi.impl.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;

/**
 * Utility class for executing distributed queries and producing an aggregated result.
 * 
 * TODO this should grow to be an artifact by its own right (e.g. querying.sparql)
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class DistributedQueries {

    private static final Logger log = LoggerFactory.getLogger(DistributedQueries.class);

    /**
     * Issues a distributed query to retrieve the description of multiple entities.
     * 
     * Use this method when you expect the results of queries to reference more than one distinct entity.
     * Query templates will have to deliver result sets of the s,p,o type in order to be in the output of this
     * method.
     * 
     * @param fragments
     * @return aliases to data
     */
    public static Map<URI,Entity> executeEntities(Map<URI,List<Query>> fragments) {
        Map<URI,Entity> result = new HashMap<>();
        log.info("Ready to issue federated query (number of streams={})", fragments.size());

        // Copying the whole code (sorry - you wanted this to happen)
        int index = 0;
        for (Entry<URI,List<Query>> entry : fragments.entrySet()) {
            log.debug("Query stream #{}:", ++index);
            URI endpoint = entry.getKey();
            for (Query qdata : entry.getValue()) {
                log.debug(" ... service : {}", endpoint);
                log.debug(" ... type : {}", qdata.getQueryType());
                switch (qdata.getQueryType()) {
                    case DEREFERENCE:
                        URL uquery = qdata.getRawQueryObject(URL.class);
                        try {
                            URLConnection yc = uquery.openConnection();
                            yc.setRequestProperty("Accept", "text/turtle");
                            Model rdf = ModelFactory.createDefaultModel().read(yc.getInputStream(),
                                endpoint.toString(), "TTL");
                            Resource s = rdf.createResource(uquery.toString());
                            Entity target;
                            URI uu = uquery.toURI();
                            if (result.containsKey(uu)) target = result.get(uu);
                            else {
                                target = new EntityImpl();
                                result.put(uu, target);
                            }
                            target.addAlias(uu);
                            for (StmtIterator it = s.listProperties(); it.hasNext();) {
                                Statement t = it.next();
                                URI prop = URI.create(t.getPredicate().toString());
                                try {
                                    target.addValue(prop, t.getObject());
                                } catch (Exception ex) {
                                    log.warn("Addition of value '{}' failed.", t.getObject());
                                    log.warn(" ... reason: {}", ex.getLocalizedMessage());
                                }
                            }
                            log.debug("<== SUCCESS");
                        } catch (IOException | URISyntaxException e) {
                            log.debug("<== FAIL");
                            log.error("Dereference failed.");
                            log.error(" ... Query was:\n{}.", uquery);
                            log.error(" ... Stack trace follows.", e);
                            // throw new RuntimeException(e);
                        }
                        break;
                    case SPARQL_SELECT:
                        com.hp.hpl.jena.query.Query query = qdata
                                .getRawQueryObject(com.hp.hpl.jena.query.Query.class);
                        log.debug(" ... query syntax : {}", query.toString(query.getSyntax()));
                        if (query.getQueryType() != com.hp.hpl.jena.query.Query.QueryTypeSelect) throw new IllegalArgumentException(
                                "Wrapped query is a " + query.getQueryType() + " - expected SPARQL SELECT.");
                        QueryEngineHTTP httpQuery = new QueryEngineHTTP(endpoint.toString(), query);
                        // Some services return text/html by default... dumb
                        httpQuery.setSelectContentType("application/sparql-results+json");
                        try {
                            ResultSet res = httpQuery.execSelect();
                            int j = 0;
                            while (res.hasNext()) {
                                QuerySolution sol = res.next();
                                if (!sol.contains("s") || !sol.get("s").isURIResource()) {
                                    log.warn("Invalid subject name for list. Skipping binding.");
                                    continue;
                                }
                                if (!sol.contains("p") || !sol.get("p").isURIResource()) {
                                    log.warn("Invalid property name for list. Skipping binding.");
                                    continue;
                                }
                                RDFNode s = sol.get("s"), p = sol.get("p"), o = sol.get("o");
                                Entity target;
                                URI uu = URI.create(s.asResource().getURI());
                                if (result.containsKey(uu)) target = result.get(uu);
                                else {
                                    target = new EntityImpl();
                                    result.put(uu, target);
                                }
                                target.addAlias(uu);
                                target.addValue(URI.create(p.asResource().getURI()), o);
                                j++;
                            }
                            log.debug("<== SUCCESS - {} bindings processed", j);
                        } catch (Exception ex) {
                            log.debug("<== FAIL");
                            handleException(qdata, endpoint, ex);
                        } finally {
                            httpQuery.close();
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException(
                                qdata.getQueryType()
                                        + " is not a supported query type for distributed queries.");
                }
            }
        }
        log.info("SUCCESS - All streams returned.");
        return result;
    }

    public static Entity executeEntity(Map<URI,List<Query>> fragments) {
        return executeEntity(fragments, null);
    }

    /**
     * Issues a distributed query to retrieve the description of a single entity.
     * 
     * FIXME does not support multiple queries on the same endpoint!
     * 
     * @param fragments
     * @param target
     * @return
     */
    public static Entity executeEntity(Map<URI,List<Query>> queryPlan, Entity target) {
        log.info("Ready to issue federated query (number of streams={})", queryPlan.size());
        if (target == null) target = new EntityImpl();
        int index = 0;
        for (Entry<URI,List<Query>> entry : queryPlan.entrySet()) {
            log.debug("Query stream #{}:", ++index);
            URI endpoint = entry.getKey();
            for (Query qdata : entry.getValue()) {
                EntityFragment fr = SingletonQuerier.getInstance().executeQuery(qdata, endpoint, target);
                Util.merge(fr, target);
            }
        }
        log.info("SUCCESS - All streams returned.");
        return target;
    }

    /**
     * Issues a distributed query to retrieve a set of subjects
     * 
     * @param fragments
     * @return
     */
    public static Set<URI> executeSubjects(Map<URI,List<Query>> fragments) {
        Set<URI> insts = new HashSet<>();
        int index = 0;
        for (Entry<URI,List<Query>> entry : fragments.entrySet()) {
            log.debug("Query stream #{}:", ++index);
            URI endpoint = entry.getKey();
            log.debug(" ... service : {}", endpoint);
            if (endpoint == null) log.debug(" ... (NULL, not valid)");
            log.debug(" ... raw querylist : {}", entry.getValue());
            if (entry.getValue() == null) log.debug(" ... no associated query list.");
            if (endpoint == null || entry.getValue() == null) continue;
            log.debug(" ... there are {} queries.", entry.getValue().size());
            for (Query qdata : entry.getValue()) {

                log.debug(" ...... type : {}", qdata.getQueryType());
                switch (qdata.getQueryType()) {
                    case SPARQL_SELECT:
                        com.hp.hpl.jena.query.Query query = qdata
                                .getRawQueryObject(com.hp.hpl.jena.query.Query.class);
                        log.debug(" ...... query syntax : {}", query.toString(query.getSyntax()));
                        if (query.getQueryType() != com.hp.hpl.jena.query.Query.QueryTypeSelect) throw new IllegalArgumentException(
                                "Wrapped query is a " + query.getQueryType() + " - expected SPARQL SELECT.");
                        QueryEngineHTTP httpQuery = new QueryEngineHTTP(endpoint.toString(), query);
                        // Some services return text/html by default... dumb
                        httpQuery.setSelectContentType("application/sparql-results+json");
                        try {
                            ResultSet res = httpQuery.execSelect();
                            int j = 0;
                            while (res.hasNext()) {
                                QuerySolution sol = res.next();
                                j++;
                                if (!sol.contains("s") || !sol.get("s").isURIResource()) {
                                    log.warn("Invalid subject name for list. Skipping binding.");
                                    continue;
                                }
                                String s = sol.getResource("s").getURI();
                                try {
                                    insts.add(URI.create(s));
                                } catch (Exception uriex) {
                                    log.warn("Illegal subject URI <{}>. Skipping whole binding.", s);
                                    continue;
                                }
                            }
                            log.debug("<== SUCCESS - {} bindings processed", j);
                        } catch (Exception ex) {
                            log.debug("<== FAIL");
                            handleException(qdata, endpoint, ex);
                        } finally {
                            httpQuery.close();
                        }
                        break;
                    default:
                        log.error("Unsupported query type {}", qdata.getQueryType());

                }
            }
        }
        return insts;
    }

    private static void handleException(Query wrappedQuery, URI sparqlEndpoint, Exception ex) {
        log.error("Query failed.");
        log.error(" ... Endpoint was : {}", sparqlEndpoint);
        if (ex instanceof QueryExceptionHTTP) {
            log.error(" ... Reason: HTTP query transport failed. Message was: {}", ex.getMessage());
        } else if (ex instanceof ConnectException) {
            log.error(" ... Reason: could not connect to endpoint. Message was: {}", ex.getMessage());
        } else {
            log.error(" ... Unhandled exception type {}.", ex.getClass());
            log.error(" ... Stack trace follows.", ex);
        }
        log.error(" ... Query was:\n{}.", wrappedQuery.getRawQueryObject(com.hp.hpl.jena.query.Query.class));
    }

}
