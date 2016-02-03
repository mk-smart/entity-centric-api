package org.mksmart.ecapi.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.mksmart.ecapi.api.Catalogue;
import org.mksmart.ecapi.api.DebuggableAssemblyProvider;
import org.mksmart.ecapi.api.GlobalType;
import org.mksmart.ecapi.api.TypeSupport;
import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * 
 * @author alexdma
 * 
 */
public class RDFCatalogue extends HashMap<URI,Set<URI>> implements Catalogue, DebuggableAssemblyProvider {

    private static final String _NS = "http://mksmart.org/jit/term/";

    private static final Property IS_IDENTIFYING_PROPERTY_FOR = ResourceFactory
            .createProperty(_NS + "isIdentifyingPropertyFor");

    private static final Property MICROCOMPILER = ResourceFactory.createProperty(_NS + "microcompiler");

    /**
	 * 
	 */
    private static final long serialVersionUID = -4786115695795563825L;

    private static final Property SUPPORTS_TYPE = ResourceFactory.createProperty(_NS + "supportsType");

    private static final Property UNIFIER = ResourceFactory.createProperty(_NS + "unifier");

    private Logger log = LoggerFactory.getLogger(getClass());

    private Map<URI,String> microcompilers;

    private Model rdf;

    public RDFCatalogue(URL configLocation) throws IOException {
        log.debug("Initialising RDF catalogue on resource {}", configLocation);
        microcompilers = new HashMap<>();

        rdf = ModelFactory.createDefaultModel().read(configLocation.openStream(),
            "http://mksmart.org/jit/catalogue", "TURTLE");
        for (ResIterator it = rdf.listSubjectsWithProperty(SUPPORTS_TYPE); it.hasNext();) {
            Resource r = it.next();
            log.debug("Endpoint {}", r);
            for (NodeIterator it2 = rdf.listObjectsOfProperty(r, SUPPORTS_TYPE); it2.hasNext();) {
                RDFNode nd = it2.next();
                log.debug(" ... supports type: {}", nd);
                addSupportingDataset(URI.create(nd.asResource().getURI()), URI.create(r.getURI()));
            }
        }

        for (StmtIterator it = rdf.listStatements(); it.hasNext();) {
            Statement s = it.next();
            if (MICROCOMPILER.equals(s.getPredicate())) {
                String mc = s.getObject().asLiteral().getString();
                microcompilers.put(URI.create(s.getSubject().getURI()), mc);
            }
        }
    }

    public boolean addSupportingDataset(URI type, URI dataset) {
        if (!this.containsKey(type)) this.put(type, new HashSet<URI>());
        return this.get(type).add(dataset);
    }

    @Override
    public Set<String> getCandidateTypes(GlobalURI guri) {
        throw new NotImplementedException("NIY");
    }

    @Override
    public Set<URI> getCandidateTypes(URI property) {
        if (property == null) throw new IllegalArgumentException("property cannot be null");

        Resource prop = rdf.createResource(property.toString());

        Set<URI> result = new HashSet<>();
        for (NodeIterator it = rdf.listObjectsOfProperty(prop, IS_IDENTIFYING_PROPERTY_FOR); it.hasNext();) {
            RDFNode n = it.next();
            if (n.isURIResource()) result.add(URI.create(n.asResource().getURI()));
            else log.warn("{} not a URI resource??", n);
        }
        return result;
    }

    @Override
    public Set<URI> getDatasets() {
        Set<URI> res = new HashSet<>();
        for (URI u : this.keySet())
            res.addAll(this.get(u));
        return res;
    }

    @Override
    public String getMicrocompiler(URI type) {
        return microcompilers.get(type);
    }

    @Override
    public URI getQueryEndpoint(URI dataset) {
        throw new NotImplementedException(
                "NIY, will be implemented when it becomes necessary for unit tests - bear with me.");
    }

    @Override
    public Map<URI,URI> getQueryEndpointMap(URI... datasets) {
        throw new NotImplementedException(
                "NIY, will be implemented when it becomes necessary for unit tests - bear with me.");
    }

    @Override
    public Map<URI,List<Query>> getQueryMap(GlobalType gtype) {
        throw new NotImplementedException("NIY - bear with me.");
    }

    @Override
    public Map<URI,List<Query>> getQueryMap(GlobalType gtype, boolean debug) {
        log.warn("Debug mode not supported for {}. Falling back to normal compilation.", getClass());
        return getQueryMap(gtype);
    }

    @Override
    public Map<URI,List<Query>> getQueryMap(GlobalURI guri) {
        throw new NotImplementedException("NIY - bear with me.");
    }

    @Override
    public Map<URI,List<Query>> getQueryMap(GlobalURI guri, boolean debug) {
        log.warn("Debug mode not supported for {}. Falling back to normal compilation.", getClass());
        return getQueryMap(guri);
    }

    @Override
    public Map<URI,List<Query>> getQueryMap(GlobalURI guri, Set<String> datasetNames) {

        return getQueryMap(guri);
    }

    @Override
    public Map<URI,List<Query>> getQueryMap(GlobalURI guri, Set<String> datasetNames, boolean debug) {
        log.warn("Debug mode not supported for {}. Falling back to normal compilation.", getClass());
        return getQueryMap(guri, datasetNames);
    }

    @Override
    public Set<URI> getSupportedTypes() {
        return this.keySet();
    }

    @Override
    public Set<URI> getSupportedTypes(Set<String> datasetNames) {
        return getSupportedTypes();
    }

    public Set<URI> getSupportingDatasets(URI type) {
        return this.get(type);
    }

    @Override
    public Set<URI> getTypeAliases(GlobalType type) {
        throw new NotImplementedException("NIY - bear with me.");
    }

    @Override
    public TypeSupport getTypeSupport(GlobalType type) {
        throw new NotImplementedException("NIY - bear with me.");
    }

    @Override
    public TypeSupport getTypeSupport(GlobalType type, boolean debug) {
        log.warn("Debug mode not supported for {}. Falling back to normal compilation.", getClass());
        return getTypeSupport(type);
    }

    @Override
    public Set<Set<URI>> getUnifiers(URI type) {
        log.info("Scanning for unifiers of type <{}>", type);
        Set<Set<URI>> result = new HashSet<>();
        Resource t = ResourceFactory.createResource(type.toString());
        int groupn = 1;
        for (NodeIterator it = rdf.listObjectsOfProperty(t, UNIFIER); it.hasNext();) {
            log.info("Group {}:", groupn);
            RDFNode uni = it.next();
            if (uni.canAs(Bag.class)) {
                Set<URI> uniC = new HashSet<>();
                Bag b = uni.as(Bag.class);
                for (NodeIterator it2 = b.iterator(); it2.hasNext();) {
                    RDFNode node = it2.next();
                    if (node.isURIResource()) {
                        log.info(" * <{}>", node);
                        uniC.add(URI.create(node.asResource().getURI()));
                    }
                }
                result.add(uniC);
                groupn++;
            }

        }
        return result;
    }

}
