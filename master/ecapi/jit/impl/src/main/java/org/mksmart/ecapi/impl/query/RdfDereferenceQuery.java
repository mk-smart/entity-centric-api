package org.mksmart.ecapi.impl.query;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.mksmart.ecapi.api.query.TargetedQuery;

/**
 * A query that corresponds to resolving the entity URI and trying to obtain RDF from it.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class RdfDereferenceQuery extends AbstractQueryImpl {

    public RdfDereferenceQuery(URI target) {
        super(Type.DEREFERENCE, target);
        supported.add(URL.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Q> Q getRawQueryObject(Class<Q> returnType) throws UnsupportedOperationException {
        try {
            return (Q) entryPoint.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Query target <" + entryPoint + "> is not a valid URL.", e);
        }
    }

    @Override
    public String toString() {
        String s = queryType.name() + "|";
        s += getResultEntryPoint();
        return s;
    }

    @Override
    public TargetedQuery wrap(URI target) {
        return new RdfDereferenceTargetedQuery(getResultEntryPoint(), target);
    }

}
