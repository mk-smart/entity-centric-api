package org.mksmart.ecapi.impl.query;

import java.net.URI;
import java.util.Arrays;

import org.mksmart.ecapi.api.query.TargetedQuery;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;

/**
 * Generic object for SPARQL data queries.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class SparqlQuery extends AbstractQueryImpl {

    protected static String createDefault(Type queryType, URI entryPoint) {
        String text;
        switch (queryType) {
            case SPARQL_DESCRIBE:
                text = "DESCRIBE <" + entryPoint + ">";
                break;
            case SPARQL_SELECT:
                text = "SELECT DISTINCT ?p ?o WHERE { graph ?g {<" + entryPoint + "> ?p ?o }}";
                break;
            default:
                throw new UnsupportedOperationException("Unnsupported query type " + queryType
                                                        + " for this object.");
        }
        return text;
    }

    protected Query wrapped;

    /**
     * Creates a new instance of SPARQL data query.
     * 
     * @param queryType
     *            the type (outermost predicate) of the SPARQL query.
     * @param queryText
     *            the query itself.
     */
    public SparqlQuery(Type queryType, String queryText) {
        this(queryType, queryText, null);
    }

    /**
     * Creates a new instance of SPARQL data query.
     * 
     * @param queryType
     *            the type (outermost predicate) of the SPARQL query.
     * @param queryText
     *            the query itself.
     */
    public SparqlQuery(Type queryType, String queryText, URI entryPoint) {
        super(queryType, entryPoint);
        if (!Arrays.asList(new Type[] {Type.SPARQL_DESCRIBE, Type.SPARQL_SELECT}).contains(queryType)) throw new UnsupportedOperationException(
                "Unnsupported query type " + queryType + " for this object.");
        supported.add(Query.class);
        this.synopsis = queryText;
        wrapped = QueryFactory.create(queryText, Syntax.syntaxARQ);
    }

    public SparqlQuery(Type queryType, URI entryPoint) {
        this(queryType, createDefault(queryType, entryPoint), entryPoint);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Q> Q getRawQueryObject(Class<Q> returnType) throws UnsupportedOperationException {
        return (Q) wrapped;
    }

    @Override
    public String toString() {
        String s = queryType.name() + "|";
        s += getSynopsis();
        return s;
    }

    @Override
    public TargetedQuery wrap(URI target) {
        return new SparqlTargetedQuery(getQueryType(), getSynopsis(), getResultEntryPoint(), target);
    }

}
