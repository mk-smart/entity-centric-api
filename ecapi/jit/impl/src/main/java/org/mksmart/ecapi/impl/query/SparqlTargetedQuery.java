package org.mksmart.ecapi.impl.query;

import java.net.URI;

import org.mksmart.ecapi.api.query.TargetedQuery;

public class SparqlTargetedQuery extends SparqlQuery implements TargetedQuery {

    protected URI target;

    public SparqlTargetedQuery(Type queryType, String queryText, URI target) {
        super(queryType, queryText);
        this.target = target;
    }

    /**
     * Creates a new instance of SPARQL data query.
     * 
     * @param queryType
     *            the type (outermost predicate) of the SPARQL query.
     * @param queryText
     *            the query itself.
     */
    public SparqlTargetedQuery(Type queryType, String queryText, URI entryPoint, URI target) {
        super(queryType, queryText, entryPoint);
        this.target = target;
    }

    public SparqlTargetedQuery(Type queryType, URI entryPoint, URI target) {
        super(queryType, entryPoint);
        this.target = target;
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == null || !(arg0 instanceof TargetedQuery)) return false;
        if (arg0 == this) return true;
        TargetedQuery asQ = (TargetedQuery) arg0;
        return asQ.getTarget().equals(this.target) && asQ.getSynopsis().equals(this.synopsis)
               && asQ.getQueryType().equals(this.queryType);
    }

    @Override
    public URI getTarget() {
        return target;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash *= 31 + synopsis.hashCode();
        hash *= 31 + queryType.hashCode();
        hash *= 31 + target.hashCode();
        hash *= 31 + (entryPoint == null ? 0 : entryPoint.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return target.toString() + "::" + super.toString();
    }

    @Override
    public TargetedQuery wrap(URI target) {
        if (target.equals(getTarget())) return this;
        throw new UnsupportedOperationException(
                "Cannot wrap a TargetedQuery into another TargetedQuery with a different target.");
    }

}
