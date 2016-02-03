package org.mksmart.ecapi.impl.query;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.mksmart.ecapi.api.query.Query;

/**
 * Base implementation of the {@link Query} interface.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public abstract class AbstractQueryImpl implements Query {

    protected URI entryPoint;

    protected Type queryType;

    protected Set<Class<?>> supported;

    protected String synopsis;

    public AbstractQueryImpl(Type queryType) {
        this(queryType, null);
    }

    public AbstractQueryImpl(Type queryType, URI entryPoint) {
        this.entryPoint = entryPoint;
        this.queryType = queryType;
        this.supported = new HashSet<>();
    }

    @Override
    public Type getQueryType() {
        return queryType;
    }

    @Override
    public URI getResultEntryPoint() {
        return entryPoint;
    }

    @Override
    public Set<Class<?>> getSupportedRawTypes() {
        return supported;
    }

    @Override
    public String getSynopsis() {
        return synopsis;
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == null || !(arg0 instanceof Query)) return false;
        if (arg0 == this) return true;
        Query asQ = (Query) arg0;
        return asQ.getSynopsis().equals(this.synopsis) && asQ.getQueryType().equals(this.queryType);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash *= 31 + queryType.hashCode();
        hash *= 31 + (synopsis == null ? 0 : synopsis.hashCode()); // Dereference has no synopsis
        hash *= 31 + (entryPoint == null ? 0 : entryPoint.hashCode()); // but it has an entry point
        return hash;
    }

}
