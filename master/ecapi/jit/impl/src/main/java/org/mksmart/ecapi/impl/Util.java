package org.mksmart.ecapi.impl;

import java.net.URI;
import java.util.Map.Entry;
import java.util.Set;

import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.EntityFragment;
import org.mksmart.ecapi.api.provenance.PropertyPath;

import com.hp.hpl.jena.rdf.model.RDFNode;

public class Util {

    public static EntityFragment merge(EntityFragment from, EntityFragment to) {
        for (URI t : from.getTypes())
            to.addType(t);
        for (Entry<URI,Set<RDFNode>> attr : from.getAttributes().entrySet())
            for (RDFNode val : attr.getValue())
                to.addValue(attr.getKey(), val);
        for (Entry<URI,Set<Entity>> attr : from.getEAttributes().entrySet())
            for (Entity val : attr.getValue())
                to.addValue(attr.getKey(), val);
        for (String ds : from.getProvenanceMap().keySet())
            for (PropertyPath p : from.getContributedProperties(ds))
                to.addContributingSource(ds, p);
        return to;
    }

    public static EntityFragment merge(Entity from, Entity to) {
        for (URI alias : from.getAliases())
            to.addAlias(alias);
        return merge((EntityFragment) from, (EntityFragment) to);
    }
}
