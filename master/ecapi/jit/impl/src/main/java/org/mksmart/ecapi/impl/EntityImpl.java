package org.mksmart.ecapi.impl;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mksmart.ecapi.api.Entity;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class EntityImpl extends EntityFragmentImpl implements Entity {

    private Map<URI,Set<RDFNode>> data;
    private Map<URI,Set<Entity>> edata;

    private Set<URI> equivSet;

    private URI TYPE_URI = URI.create(RDF.type.getURI());

    public EntityImpl() {
        data = new HashMap<>();
        edata = new HashMap<>();
        equivSet = new HashSet<URI>();
    }

    @Override
    public void addAlias(URI equivalent) {
        equivSet.add(equivalent);
    }

    @Override
    public void addType(URI type) {
        this.addValue(TYPE_URI, ResourceFactory.createResource(type.toString()));
    }

    @Override
    public void addValue(URI property, Entity value) {
        if (!edata.containsKey(property)) edata.put(property, new HashSet<Entity>());
        edata.get(property).add(value);
    }

    @Override
    public void addValue(URI property, RDFNode value) {
        if (value.isAnon()) throw new UnsupportedOperationException(
                "Blank nodes are not yet supported as legal values.");
        if (!data.containsKey(property)) data.put(property, new HashSet<RDFNode>());
        data.get(property).add(value);
    }

    @Override
    public Set<URI> getAliases() {
        return equivSet;
    }

    @Override
    public Map<URI,Set<RDFNode>> getAttributes() {
        return data;
    }

    @Override
    public Map<URI,Set<Entity>> getEAttributes() {
        return edata;
    }

    @Override
    public Entity getEntityWithID(URI p, URI id) {
        Set<Entity> vals = edata.get(p);
        if (vals != null) for (Entity e : vals)
            if (e.getAliases().contains(id)) return e;
        return null;
    }

    @Override
    public Set<URI> getTypes() {
        Set<URI> result = new HashSet<URI>();
        for (Object typ : ((Set<Object>) this.getValues(TYPE_URI)))
            if (typ instanceof RDFNode && ((RDFNode) typ).isURIResource()) result.add(URI
                    .create(((RDFNode) typ).asResource().getURI()));
        return result;
    }

    @Override
    public Set<Object> getValues(URI property) {
        Set result = data.get(property);
        if (result == null) {
            result = edata.get(property);
            if (result == null) return Collections.emptySet();
        }
        return result;
    }

    @Override
    public void setEValues(URI property, Set<Entity> values) {
        edata.put(property, values);
    }

    @Override
    public void setValues(URI property, Set<RDFNode> values) {
        data.put(property, values);
    }

    @Override
    public Entity toEntity() {
        return this;
    }

    public String toString() {
        String r = "";
        for (URI ps : data.keySet())
            r += "[" + ps + "=" + data.get(ps) + "] ";
        for (URI ps : edata.keySet())
            r += "[" + ps + "=" + edata.get(ps) + "] ";
        return r;
    }

    @Override
    public void unset(URI property) {
        data.remove(property);
        edata.remove(property);
    }

}
