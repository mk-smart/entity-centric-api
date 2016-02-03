package org.mksmart.ecapi.couchdb.id;

import java.net.URI;
import java.util.Iterator;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.id.IdGenerator;
import org.mksmart.ecapi.commons.couchdb.client.DocumentProvider;
import org.mksmart.ecapi.couchdb.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.vocabulary.RDF;

/*
 * Example for KIS course / OU qualification
 * 
 * We want something like:
 * 
 * http://id.mksmart.org/academic_degree/data.open.ac.uk/_self:id/W01
 * 
 * http://id.mksmart.org/location/ordnance_survey/postcode/MK76AA
 * 
 * (1) type: extracted from trusted provider but not in its domain
 * 
 * (2) trusted provider
 * 
 * (3) whoever is assigning the identifying property, or _self if it "owns" the data, _world if they
 * do not "belong" to anyone special, otherwise an ID of who it is talking about
 * 
 * (4) identifier within the provider's scope
 * 
 * Identify a way to find a 'trusted' type ID : who can say "academic_degree" that we trust?
 * 
 * Identify a 'trusted' property that identifies a provider:
 * 
 * data.open.ac.uk is an authoritative entity and it is saying this about itself
 * 
 * the identifier it gives is W01
 * 
 * collapse provider URI into an ID
 * 
 * collapse identifying value
 */

public class HeuristicIdGenerator implements IdGenerator<URI,Entity> {

    public static final String DEFAULT_NS = "";

    private static final String TEMP_COUCHDB_VIEW = "_design/type/_view/short";

    private DocumentProvider<JSONObject> documentProvider;

    private Logger log = LoggerFactory.getLogger(getClass());

    public HeuristicIdGenerator(DocumentProvider<JSONObject> documentProvider) {
        this.documentProvider = documentProvider;
    }

    public URI createId(Entity e) {

        log.info("Equivalent set:");
        for (URI eq : e.getAliases())
            log.info(" * <{}>", eq);

        // The global URI components
        String shortt = "", shortauth = "", shortattr = "", id = "";

        URI uType = URI.create(RDF.type.getURI());
        Config conf = Config.getInstance();

        // STEP 1: look for an short mnemonic of a preferred type
        String u = conf.getServiceURL().toString();
        if (!u.endsWith("/")) u += '/';
        u += conf.getDataDbName() + '/' + TEMP_COUCHDB_VIEW;
        log.debug("Getting view from URL <{}>", u);
        JSONObject json = this.documentProvider.getResource(u);
        Iterator<Object> it = e.getValues(uType).iterator();
        String fullt = "";
        // We should be able to work with full type only.
        while (it.hasNext() && shortt.isEmpty() && fullt.isEmpty()) {
            Object o = it.next();
            RDFNode nod = null;
            if (o instanceof RDFNode) {
                nod = (RDFNode) o;
                if (nod.isURIResource()) {
                    String t = nod.asResource().getURI();
                    JSONArray rows = json.getJSONArray("rows");
                    for (int i = 0; i < rows.length(); i++) {
                        JSONObject row = rows.getJSONObject(i);
                        if (t.equals(row.getString("key"))) {
                            shortt = row.getString("value");
                            fullt = t;
                            break;
                        }
                    }
                }
            }
        }
        log.debug("Short type string is '{}'", shortt);
        log.debug("Full type string is <{}>", fullt);

        // STEP 2: look for an authoritative provider of this type. Best bet is self

        json = this.documentProvider.getDocument(fullt);

        if ("type-spec".equals(json.getString("type"))) {
            JSONArray auth = json.getJSONObject("mks:support").getJSONArray("authoritative");

            // Check if any dataset ID is prefix for any equivalent
            for (URI eq : e.getAliases()) {
                com.hp.hpl.jena.rdf.model.Resource req = ResourceFactory.createResource(eq.toString());

                String authoritative = null;
                for (int i = 0; i < auth.length(); i++) {
                    String ds = auth.getString(i);
                    if (eq.toString().startsWith(ds)) {
                        authoritative = ds;
                        break;
                    }
                }
                log.debug("Will try authoritative dataset <{}>", authoritative);

                // First get its meta doc
                JSONObject ds_desc = this.documentProvider.getDocument(authoritative);
                String void_sparql = "http://rdfs.org/ns/void#sparqlEndpoint";
                String sparqlEndpoint = ds_desc.getString(void_sparql);

                // Then verify if it has that entity
                String ask = "ASK { <" + eq + "> ?p ?o }";
                Query query = QueryFactory.create(ask, Syntax.syntaxARQ);
                QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlEndpoint, query);
                boolean found = false;
                try {
                    found = httpQuery.execAsk();
                    log.info("Ask returned");
                } catch (Exception ex) {
                    log.error("ASK Query failed.");
                    log.error(" ... Endpoint was : {}", sparqlEndpoint);
                    log.error(" ... Query was:\n{}.", ask);
                    log.error(" ... Stack trace follows.", e);
                } finally {
                    httpQuery.close();
                }
                // Look for an identifying attribute
                if (found) {
                    shortauth = ds_desc.getString("mks:short");
                    String self = ds_desc.getString("mks:self");
                    String describe = "DESCRIBE <" + eq + ">";

                    query = QueryFactory.create(describe, Syntax.syntaxARQ);
                    httpQuery = new QueryEngineHTTP(sparqlEndpoint, query);
                    Model rdf = httpQuery.execDescribe();
                    log.info("Describe returned");
                    JSONArray unifs = json.getJSONArray("mks:unifier");
                    // Search for a satisfiable unifier
                    for (int i = 0; i < unifs.length(); i++) {
                        JSONObject un = unifs.getJSONObject(i);
                        Property pau = rdf.createProperty(un.getString("authority"));
                        for (NodeIterator itx = rdf.listObjectsOfProperty(req, pau); itx.hasNext();) {
                            RDFNode nod = itx.next();
                            log.info("Possible authority is {}", nod);
                            if (nod.isURIResource()) {
                                String utx = nod.asResource().getURI();
                                if (self.equals(utx)) {
                                    shortattr += "_self";
                                    break;
                                } else shortattr += DigestUtils.md5(utx);
                            }
                        }
                        Property pat = rdf.createProperty(un.getString("attribute"));
                        NodeIterator itx = rdf.listObjectsOfProperty(req, pat);
                        if (itx.hasNext()) {
                            JSONObject prop_desc = this.documentProvider.getDocument(pat.getURI());
                            String propc = prop_desc.getString("mks:propclass");
                            shortattr += ':' + propc;
                            RDFNode nod = itx.next();
                            log.info("Possible value is {}", nod);
                            if (nod.isLiteral()) id = nod.asLiteral().getLexicalForm();
                            else id = nod.toString();
                        }
                    }

                }
            }

        }

        URI result = URI.create(DEFAULT_NS + shortt + '/' + shortauth + '/' + shortattr + '/' + id);
        log.info("Global URI is {}", result);
        return result;
    }

    @Override
    public String createIdFromUri(URI localURI) {
        throw new NotImplementedException("NIY");
    }

    @Override
    public String createPropertyId(URI localProperty) {
        throw new NotImplementedException("NIY. Feature request came too late.");
    }

    @Override
    public void refresh() {
        // Nothing to refresh
    }

}
