package org.mksmart.ecapi.api.query;

import java.net.URI;
import java.util.Set;

/**
 * An object that represents a query, to be performed on either a specific source or the entity-centric API
 * itself, in order to retrieve data about an entity.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface Query {

    /**
     * The category of a data query.
     * 
     * @author alessandro <alexdma@apache.org>
     * 
     */
    enum Type {

        /**
         * Denotes querying data on an item by resolving its name.
         */
        DEREFERENCE,
        /**
         * The SPARQL DESCRIBE query type.
         */
        SPARQL_DESCRIBE,

        /**
         * The SPARQL SELECT query type.
         */
        SPARQL_SELECT,

        /**
         * The SQL-92 SELECT query type.
         */
        SQL_SELECT

    }

    /**
     * Gets the category associated with this data query.
     * 
     * @return the query type.
     */
    public Type getQueryType();

    /**
     * Utility method that exports the query object that was wrapped into this {@link Query} object. It can be
     * used in order to execute the query autonomously. The result of {@link Query#getSupportedRawTypes()}
     * dictates which classes may be used as a return type.
     * 
     * @param returnType
     *            the desired class of the wrapped query object.
     * @return the wrapped query object.
     * @throws UnsupportedOperationException
     *             if the return type is not supported.
     */
    public <Q> Q getRawQueryObject(Class<Q> returnType) throws UnsupportedOperationException;

    /**
     * If the result set of this query is expected to be explored starting with a specific entity (as can be
     * the result of e.g. a SPARQL DESCRIBE), the identifier of that entity is returned by this method.
     * 
     * @return the entry point identifier for exploring the result set.
     */
    public URI getResultEntryPoint();

    /**
     * Gets the classes that can be used to export and execute the query autonomously with a call to
     * {@link Query#getRawQueryObject(Class)}.
     * 
     * @return the set of supported classes
     */
    public Set<Class<?>> getSupportedRawTypes();

    public String getSynopsis();

    public TargetedQuery wrap(URI target);

}
