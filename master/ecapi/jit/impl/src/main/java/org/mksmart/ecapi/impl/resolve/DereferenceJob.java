package org.mksmart.ecapi.impl.resolve;

import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;

import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.impl.AttributeListenableImpl;
import org.mksmart.ecapi.impl.EntityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class DereferenceJob extends AttributeListenableImpl implements Runnable {

    private Entity representation;

    private Logger log = LoggerFactory.getLogger(getClass());

    private URI entityId;

    public DereferenceJob(URI entityId) {
        this(entityId, new EntityImpl());
        this.representation.addAlias(entityId);
        log.debug("New dereferencing job initialised.");
    }

    /**
     * Does not actually write into the entity representation, but uses it as a mutex monitor.
     * 
     * @param entityId
     * @param representation
     */
    public DereferenceJob(URI entityId, Entity representation) {
        this.setRepresentation(representation);
        this.entityId = entityId;
    }

    public Entity getRepresentation() {
        return representation;
    }

    @Override
    public void run() {
        log.debug(" ... ID is '{}'", this.entityId);
        String accept = "text/turtle,text/rdf+n3;q=0.9,text/rdf+nt;q=0.9,application/rdf+xml;q=0.8,application/rdf+json;q=0.8";
        synchronized (representation) {
            try {
                URLConnection yc = entityId.toURL().openConnection();
                yc.setRequestProperty("Accept", accept);
                Model rdf = ModelFactory.createDefaultModel().read(yc.getInputStream(), entityId.toString(),
                    "TTL");
                Resource s = rdf.createResource(entityId.toString());
                for (StmtIterator it = s.listProperties(); it.hasNext();) {
                    Statement stmt = it.next();
                    fireAttributeSet(entityId, URI.create(stmt.getPredicate().getURI()), stmt.getObject());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            log.debug(" <== SUCCESS. Dereferencing complete.");
            representation.notifyAll();
        }

    }

    protected void setRepresentation(Entity representation) {
        this.representation = representation;
    }

}
