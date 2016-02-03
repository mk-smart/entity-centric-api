package org.mksmart.ecapi.api;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.mksmart.ecapi.api.provenance.PropertyPath;
import org.mksmart.ecapi.api.provenance.ProvenanceListener;
import org.mksmart.ecapi.api.provenance.ProvenanceTreeNode;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * A materialisation of an entity that is being constructed. Can be used as the generic type to be returned by
 * a {@link Future}.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface EntityFragment extends ProvenanceListener {

    public void addContributingSource(String dataset, URI property);

    public void addContributingSource(String dataset, PropertyPath path);

    /**
     * Adds a new type to this entity. Unless implementations specify otherwise, this can be seen as a
     * shortcut to {@link #addValue(URI, RDFNode)} with {@link RDF#type} as the first argument.
     * 
     * @param type
     *            a type for this entity
     */
    public void addType(URI type);

    public void addValue(URI property, Entity value);

    /**
     * Adds an {@link RDFNode} as a value for the specified property.
     * 
     * @param property
     *            the property whose value is being added.
     * @param value
     *            a value for the specified property.
     */
    public void addValue(URI property, RDFNode value);

    /**
     * Gets the entire attribute/value map of this entity, possibly including types and aliases.
     * 
     * @return the attribute map.
     */
    public Map<URI,Set<RDFNode>> getAttributes();

    public Set<PropertyPath> getContributedProperties(String dataset);

    /**
     * FIXME this method must disappear.
     * 
     * @return
     */
    public Map<URI,Set<Entity>> getEAttributes();

    public Map<String,ProvenanceTreeNode> getProvenanceMap();

    /**
     * Gets the types of this entity. Unless implementations specify otherwise, this can be seen as a shortcut
     * to {@link #getValues(URI)} with {@link RDF#type} as the argument.
     * 
     * @return the set of types.
     */
    public Set<URI> getTypes();

    /**
     * Gets all the values that the specified property has for this entity.
     * 
     * @param property
     *            the property whose values are being requested.
     * @return the set of values for the property.
     */
    public Set<Object> getValues(URI property);

    /**
     * FIXME this method must disappear.
     * 
     * @return
     */
    public void setEValues(URI property, Set<Entity> values);

    /**
     * Sets the specified set of values for the specified property, overwriting any previous values.
     * 
     * @param property
     * @param values
     */
    public void setValues(URI property, Set<RDFNode> values);

    public Entity toEntity();

    /**
     * Removes any values from the specified property, which is no longer registered as an attribute name.
     * 
     * @param property
     *            the property to be unset;
     */
    public void unset(URI property);

}
