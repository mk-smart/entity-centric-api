package org.mksmart.ecapi.web.util;

import java.net.URI;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.id.IdGenerator;
import org.mksmart.ecapi.impl.EntityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * A utility that rewrites a name, or named object, according to the policies configured as part of the API.
 * 
 * @author Alessandro Adamou <alexdma@apache.org>
 * 
 */
public class UriRewriter {

    private IdGenerator<?,?> idgen;

    private Logger log = LoggerFactory.getLogger(getClass());

    public UriRewriter(IdGenerator<?,?> idgen) {
        this.idgen = idgen;
    }

    /**
     * FIXME reintroduce support for aliases
     * 
     * @param e
     * @param base
     * @return
     */
    public Entity rewrite(Entity e, String base) {
        long b4 = System.currentTimeMillis();
        Entity e_rewr = new EntityImpl();
        // for (URI alias : e.getAliases())
        // e_rewr.addAlias(alias);
        /*
         * Do not add types explicitly: this is just a shortcut for adding values for rdf:type and would get
         * around the rewriting process. Types will be returned as part of the value maps anyway.
         */
        // for (URI type : e.getTypes())
        // e_rewr.addType(type);

        // Rewrite general RDF references
        for (Entry<URI,Set<RDFNode>> entry2 : e.getAttributes().entrySet()) {
            Set<RDFNode> newValues = new HashSet<>();
            for (RDFNode nod : entry2.getValue())
                newValues.add(rewrite(nod, base));
            log.trace("Setting values for property <{}>", rewriteProperty(entry2.getKey()));
            e_rewr.setValues(rewriteProperty(entry2.getKey()), newValues);
        }
        // Rewrite entity references
        for (Entry<URI,Set<Entity>> entry2 : e.getEAttributes().entrySet()) {
            Set<Entity> newValues = new HashSet<>();
            for (Entity nod : entry2.getValue())
                newValues.add(rewrite(nod, base));
            e_rewr.setEValues(rewriteProperty(entry2.getKey()), newValues);
        }
        log.trace("Rewrote Entity in {} ms", System.currentTimeMillis() - b4);
        return e_rewr;
    }

    /**
     * mda's offline implementation
     * 
     * @param localValue
     * @param base
     * @param client
     * @return
     */
    public RDFNode rewrite(RDFNode localValue, String base) {
        // Literals go as they are
        if (!localValue.isURIResource()) return localValue;
        URI uri;
        try {
            uri = URI.create(localValue.asResource().getURI());
        } catch (IllegalArgumentException ex) {
            log.warn("Could not create URI from local value '" + localValue
                     + "' which was expected to be a URIResource.", ex);
            return localValue;
        }
        String u = rewrite(uri, base);
        com.hp.hpl.jena.rdf.model.Resource rdfRsrc = ResourceFactory.createResource(u);
        return rdfRsrc;
    }

    public String rewrite(URI localId, String base) {
        log.trace("rewriting {}", localId);
        String fallback = base + "entity/" + "thing/www:uri/" + localId.toString().replace("http://", "");
        String u = fallback;
        String nu = idgen.createIdFromUri(localId);
        if (nu != null) u = nu.toString();
        if (!u.startsWith(base)) u = base + "entity/" + u;
        log.trace("returning {}", u);
        return u;
    }

    public RDFNode rewriteFromData(RDFNode localValue, String base) {
        String baze = base + "/compiler";
        if (!localValue.isURIResource()) return localValue;
        String fallback = base + "entity/" + "thing/www:uri/"
                          + localValue.asResource().getURI().replace("http://", "");
        ClientConfig conf = new ClientConfig();
        conf.followRedirects(false);
        RestClient client = new RestClient(conf);
        Resource resource = client.resource(baze).queryParam("id", localValue.asResource().getURI());
        com.hp.hpl.jena.rdf.model.Resource rdfRsrc;
        String u;
        try {
            ClientResponse resp = resource.get();
            log.trace("Response was {}", resp.getStatusCode());
            if (resp.getHeaders().containsKey("Location")) u = resp.getHeaders().getFirst("Location");
            else u = fallback;
        } catch (Exception e) {
            log.warn("An error occurred while trying to rewrite local value <{}>", localValue);
            log.warn("Exception follows.", e);
            u = fallback;
        }
        rdfRsrc = ResourceFactory.createResource(u);
        return rdfRsrc;
    }

    public URI rewriteProperty(URI property) {
        return URI.create(idgen.createPropertyId(property));
    }

}
