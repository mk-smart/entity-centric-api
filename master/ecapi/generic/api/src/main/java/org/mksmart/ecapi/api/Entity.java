package org.mksmart.ecapi.api;

import java.net.URI;
import java.util.Set;

/**
 * The atomic object on which public requests for information can be invoked. It is in essence a map of
 * attribute-value pairs.
 * 
 * An entity per se is agnostic to how it is generated, i.e. does not contain or provide provenance
 * information or other data as to how its attributes are fetched and compiled from datasets (e.g. query
 * templates). It also does not contain provider-dependent information such as its own global identifier. It
 * does, however, know its aliases (i.e. alternative ways of referencing them in datasets).
 * 
 * TODO support composite aliases than URIs, e.g. constructed through rules or queries
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface Entity extends EntityFragment {

    /**
     * Adds a new alias, i.e. a local URI, for this entity (in the simplest case, these are mapped to
     * owl:sameAs).
     * 
     * @param alias
     */
    public void addAlias(URI alias);

    /**
     * Gets a set of local URIs that identify this entity.
     * 
     * @return the local URIs.
     */
    public Set<URI> getAliases();

    /**
     * Or rather,
     * "get this value of which I'm giving you the local ID, for this property, and return it to me as an entity."
     * OMFG
     * 
     * FIXME this method must disappear.
     * 
     * @return
     */
    public Entity getEntityWithID(URI p, URI id);

}
