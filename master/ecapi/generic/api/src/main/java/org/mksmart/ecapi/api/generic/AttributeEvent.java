package org.mksmart.ecapi.api.generic;

import java.net.URI;

import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * 
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class AttributeEvent {

    private URI localId, property;

    private RDFNode value;

    /**
     * Creates a new instance of {@link AttributeEvent}.
     * 
     * @param localId
     *            the <em>local</em> identifier for the affected entity.
     * @param property
     *            the attribute property.
     * @param value
     *            the <em>new</em> value set for the attribute.
     */
    public AttributeEvent(URI localId, URI property, RDFNode value) {
        this.localId = localId;
        this.property = property;
        this.value = value;
    }

    /**
     * Returns a <em>local</em> identifier for the affected entity (TODO change to global?)
     * 
     * @return the entity ID.
     */
    public URI getEntityId() {
        return this.localId;
    }

    /**
     * Returns the property whose value was set or changed.
     * 
     * @return the property ID.
     */
    public URI getProperty() {
        return this.property;
    }

    /**
     * Returns the <em>new</em> value set for the attribute.
     * 
     * @return the new value.
     */
    public RDFNode getValue() {
        return this.value;
    }

}
