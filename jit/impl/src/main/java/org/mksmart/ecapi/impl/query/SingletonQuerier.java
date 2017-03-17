package org.mksmart.ecapi.impl.query;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.EntityFragment;
import org.mksmart.ecapi.api.provenance.PropertyPath;
import org.mksmart.ecapi.api.provenance.ProvenanceListenable;
import org.mksmart.ecapi.api.query.Query;
import org.mksmart.ecapi.api.query.TargetedQuery;
import org.mksmart.ecapi.impl.EntityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class SingletonQuerier extends ProvenanceListenable {

    private static SingletonQuerier me = new SingletonQuerier();

    public static SingletonQuerier getInstance() {
        return me;
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private SingletonQuerier() {}

    /**
     * Executes a query on a service endpoint, and compiles its results into an {@link EntityFragment}.
     * 
     * @param query
     *            the query to be executed
     * @param endpoint
     *            the service URI where the query should be executed.
     * @param recipient
     *            the compilation target of the query results. If not null, it will also be returned by the
     *            method.
     * @return the compiled result of the query. If a non-null recipient was supplied, it will be the same
     *         object, otherwise a new one is created.
     */
    public EntityFragment executeQuery(Query query, URI endpoint, EntityFragment recipient) {
        URI dataset = query instanceof TargetedQuery ? ((TargetedQuery) query).getTarget() : null;
        log.debug(" ... service : {}", endpoint);
        log.debug(" ... type : {}", query.getQueryType());
        if (recipient == null) recipient = new EntityImpl();
        if (query.getResultEntryPoint() != null && recipient instanceof Entity) ((Entity) recipient)
                .addAlias(query.getResultEntryPoint());
        switch (query.getQueryType()) {
            case DEREFERENCE:
                URL uquery = query.getRawQueryObject(URL.class);
                try {
                    URLConnection yc = uquery.openConnection();
                    yc.setRequestProperty("Accept", "text/turtle");
                    Model rdf = ModelFactory.createDefaultModel().read(yc.getInputStream(),
                        endpoint.toString(), "TTL");
                    Resource s = rdf.createResource(uquery.toString());
                    for (StmtIterator it = s.listProperties(); it.hasNext();) {
                        Statement t = it.next();
                        PropertyPath path = new PropertyPath();
                        path.add(URI.create(t.getPredicate().getURI()));
                        doAdd(recipient, t.getPredicate(), t.getObject(), dataset, path);
                    }
                    log.debug("<== SUCCESS");
                } catch (IOException e) {
                    log.debug("<== FAIL");
                    log.error("Dereference failed.");
                    log.error(" ... Query was:\n{}.", uquery);
                    log.trace(" ... Stack trace follows.", e);
                    // throw new RuntimeException(e);
                }
                break;
            case SPARQL_DESCRIBE:
                com.hp.hpl.jena.query.Query rawQuery = query
                        .getRawQueryObject(com.hp.hpl.jena.query.Query.class);
                QueryEngineHTTP httpQuery = new QueryEngineHTTP(endpoint.toString(), rawQuery);
                if (rawQuery.getQueryType() == com.hp.hpl.jena.query.Query.QueryTypeDescribe) try {
                    Model m = httpQuery.execDescribe();
                    if (query.getResultEntryPoint() != null) {
                        StmtIterator it = m.listStatements(new SimpleSelector(m.createResource(query
                                .getResultEntryPoint().toString()), null, (Object) null));
                        while (it.hasNext()) {
                            Statement t = it.next();
                            PropertyPath path = new PropertyPath();
                            path.add(URI.create(t.getPredicate().getURI()));
                            doAdd(recipient, t.getPredicate(), t.getObject(), dataset, path);
                        }
                    }
                    log.debug("<== SUCCESS");
                } catch (Exception ex) {
                    log.debug("<== FAIL");
                    handleException(query, endpoint, ex);
                } finally {
                    httpQuery.close();
                }
                break;
            case SPARQL_SELECT:
                rawQuery = query.getRawQueryObject(com.hp.hpl.jena.query.Query.class);
                log.debug(" ... query syntax : {}", rawQuery.toString(rawQuery.getSyntax()));
                if (rawQuery.getQueryType() != com.hp.hpl.jena.query.Query.QueryTypeSelect) throw new IllegalArgumentException(
                        "Wrapped query is a " + rawQuery.getQueryType() + " - expected SPARQL SELECT.");
                httpQuery = new QueryEngineHTTP(endpoint.toString(), rawQuery);
                // Some services return text/html by default... dumb
                httpQuery.setSelectContentType("application/sparql-results+json");
                try {
                    ResultSet res = httpQuery.execSelect();
                    while (res.hasNext()) {
                        QuerySolution sol = res.next();
                        PropertyPath path = new PropertyPath();
                        // if it is a simple, level 1 query - keep for backward compatibility
                        if (sol.get("p") != null) {
                            RDFNode p = sol.get("p"), o = sol.get("o");
                            doAdd(recipient, p, o, dataset, path);
                        } else { // Nested queries need special handling
                            EntityFragment rs = recipient;
                            log.trace("<<< Entering nested context");
                            RDFNode pLast = null;
                            int i;
                            for (i = 1; sol.get("p" + (i + 1)) != null; i++) {
                                RDFNode p = sol.get("p" + i);
                                URI up = URI.create(p.asResource().getURI());
                                // Typically these are intermediate dummy nodes that should be skipped.
                                // Only the final 'o' will be considered.
                                RDFNode val = sol.get("o" + i);
                                log.trace("o{} is '{}' (will be skipped)", i, val);
                                if (rs instanceof Entity && val.isResource()) {
                                    String urs = val.asResource().getURI();
                                    if (urs != null) {
                                        URI uv = URI.create(urs);
                                        Entity eval = ((Entity) rs).getEntityWithID(up, uv);
                                        if (eval == null) {
                                            eval = new EntityImpl();
                                            eval.addAlias(uv);
                                        }
                                        doAdd(rs, p, eval, dataset, path);
                                        rs = eval;
                                    } else {
                                        log.warn("WTF? value <{}> is a resource but has no URI.", val);
                                    }
                                } else {
                                    log.warn("Expected value '{}' to be a RDF resource, got a {} instead.",
                                        val, val.getClass().getCanonicalName());
                                    log.warn("Is the integration query well-formed?");
                                }
                                path.add(up);
                                pLast = sol.get("p" + (i + 1));
                            }
                            if (pLast != null) { // The final bit of the expanded query results
                                log.trace("final p is <{}>", pLast);
                                RDFNode o = sol.get("o" + i);
                                log.trace("hit terminal object {}", o);
                                doAdd(rs, pLast, o, dataset, path);
                            }
                            log.trace(">>> Exiting nested context");
                        }
                    }
                    log.debug("<== SUCCESS");
                } catch (Exception ex) {
                    log.debug("<== FAIL");
                    handleException(query, endpoint, ex);
                } finally {
                    httpQuery.close();
                }
                break;
            default:
                throw new UnsupportedOperationException(
                        query.getQueryType() + " is not a supported query type for distributed queries.");
        }
        return recipient;

    }

    /**
     * Executes a query on the service indicated by the query itself, and compiles its results into an
     * {@link EntityFragment}.
     * 
     * @param query
     *            the query to be executed, which also contains an indication of the endpoint.
     * @return the compiled result of the query.
     */
    public EntityFragment executeQuery(TargetedQuery query) {
        return executeQuery(query, query.getTarget(), null);
    }

    /**
     * Executes a query on the service indicated by the query itself, and compiles its results into an
     * {@link EntityFragment}.
     * 
     * @param query
     *            the query to be executed, which also contains an indication of the endpoint.
     * @param recipient
     *            the compilation target of the query results. If not null, it will also be returned by the
     *            method.
     * @return the compiled result of the query. If a non-null recipient was supplied, it will be the same
     *         object, otherwise a new one is created.
     */
    public EntityFragment executeQuery(TargetedQuery query, EntityFragment recipient) {
        return executeQuery(query, query.getTarget(), recipient);
    }

    /**
     * Executes a query on a service endpoint, and compiles its results into an {@link EntityFragment}.
     * 
     * @param query
     *            the query to be executed
     * @param endpoint
     *            the service URI where the query should be executed.
     * @return the compiled result of the query.
     */
    public EntityFragment executeQuery(TargetedQuery query, URI endpoint) {
        return executeQuery(query, endpoint, null);
    }

    /**
     * Utility method that handles the addition of a property-value pair.
     * 
     * @param recipient
     * @param property
     * @param value
     * @param source
     */
    private void doAdd(EntityFragment recipient, RDFNode property, Object value, URI source, PropertyPath path) {
        URI p;
        if (property.isURIResource()) p = URI.create(property.asResource().getURI());
        else throw new IllegalArgumentException("property must be a URI resource, "
                                                + property.getClass().getCanonicalName() + " found instead");
        try {
            if (value instanceof RDFNode) recipient.addValue(p, (RDFNode) value);
            else if (value instanceof Entity) recipient.addValue(p, (Entity) value);
            else throw new IllegalArgumentException("value must be either a "
                                                    + RDFNode.class.getCanonicalName() + " or a "
                                                    + Entity.class.getCanonicalName() + " - "
                                                    + value.getClass().getCanonicalName() + " found instead");
            firePropertyAdded(path, p, source);
        } catch (UnsupportedOperationException ex) {
            log.warn("Unsupported addition of value to property <{}> detected."
                     + " It might be a blank node (see exception message below).", p);
            log.warn(" ... reason: {}", ex.getLocalizedMessage());
        } catch (Exception ex) {
            log.error("Addition of value '{}' failed.", value);
            log.error(" ... reason: {}", ex.getLocalizedMessage());
        }
    }

    private void handleException(Query wrappedQuery, URI sparqlEndpoint, Exception ex) {
        log.error("Query failed.");
        log.error(" ... Endpoint was : {}", sparqlEndpoint);
        log.error(" ... Query was:\n{}.", wrappedQuery.getRawQueryObject(com.hp.hpl.jena.query.Query.class));
        log.error(" ... Stack trace follows.", ex);
    }
}
