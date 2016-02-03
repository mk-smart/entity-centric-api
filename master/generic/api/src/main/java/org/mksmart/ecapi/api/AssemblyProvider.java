package org.mksmart.ecapi.api;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.query.Query;

/**
 * An object responsible for the aggregation of data from resources of a specific type, or in a specific set.
 * It is not responsible for creating objects of type {@link Entity} by itself, which is delegated to the
 * {@link EntityCompiler}, in turn supported by entity providers.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface AssemblyProvider extends SupportRetriever {

    /**
     * Returns the possible types of the entity with the given global identifier.
     * 
     * @param guri
     *            the global identifier of the entity.
     * @return the set of candidate types.
     */
    public Set<String> getCandidateTypes(GlobalURI guri);

    /**
     * Returns the possible types of entities that have a value for this property.
     * 
     * XXX this aspect is extremely naive (we should consider property-value pairs, inference rules, OWL
     * defined and primitive classes)
     * 
     * @param property
     *            the property to be examined.
     * @return the set of candidate types.
     */
    @Deprecated
    public Set<URI> getCandidateTypes(URI property);

    /**
     * Gets a raw representation of the compiler code used for obtaining data on an entity with a certain
     * type. This is not executed by the provider itself, but can be run by an {@link EntityCompiler}.
     * 
     * @param type
     *            the type of entities for which support is requested.
     * @return the microcompiler code for this type.
     */
    public String getMicrocompiler(URI type);

    /**
     * Gets an associative object that maps service endpoints to the queries to be performed on them for
     * retrieving data about a global type with the supplied global identifier. These data can be used, for
     * instance, by an {@link EntityCompiler}. The keys of the resulting maps are dataset identifiers.
     * 
     * @param gtype
     * @return
     */
    public Map<URI,List<Query>> getQueryMap(GlobalType gtype);

    /**
     * Gets an associative object that maps service endpoints to the queries to be performed on them for
     * retrieving data about the entity with the supplied global identifier. These data can be used, for
     * instance, by an {@link EntityCompiler}. The keys of the resulting maps are query endpoint URIs (TODO
     * make them dataset identifiers).
     * 
     * <ul>
     * <li>TODO this somehow clashes with getting microcompilers separately.
     * <li>FIXME it shouldn't be part of this class anyway.
     * </ul>
     * 
     * @param guri
     *            the global identifier of the entity.
     * @return the map of queries to be performed on each endpoint.
     */
    public Map<URI,List<Query>> getQueryMap(GlobalURI guri);

    public Map<URI,List<Query>> getQueryMap(GlobalURI guri, Set<String> datasetNames);

    public Set<URI> getSupportedTypes();

    public Set<URI> getSupportedTypes(Set<String> datasetNames);

    public Set<URI> getTypeAliases(GlobalType type);

    /**
     * Returns the set of unifiers for an RDF type. A unifier is a set of properties which, if they all have a
     * value for a given resource, have a likelihood of assigning that resource the supplied type.
     * 
     * @param type
     * @return
     */
    public Set<Set<URI>> getUnifiers(URI type);

}
