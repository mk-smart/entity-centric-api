package org.mksmart.ecapi.impl.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URL;

import org.junit.Test;
import org.mksmart.ecapi.api.query.Query;
import org.mksmart.ecapi.impl.query.QueryParser;

public class QueryTemplateTest {

    private URI testUri = URI.create("http://stoc.az.zo");

    @Test
    public void testArityNegative() {
        String tpl = "sparql_describe([LURI],a,b,c,d)";
        try {
            QueryParser.parse(tpl, testUri);
            fail("Query with too many arguments was parsed correctly. This should not happen.");
        } catch (IllegalArgumentException | UnsupportedOperationException ex) {}
    }

    @Test
    public void testParser() throws Exception {
        String tpl = "resolve([LURI])";
        Query q = QueryParser.parse(tpl, testUri);
        assertNotNull(q);
        // Comparing URLs is slower.
        assertEquals(testUri.toString(), q.getRawQueryObject(URL.class).toString());
        tpl = "sparql_select([LURI],bc,def)";
        q = QueryParser.parse(tpl, testUri);
        assertNotNull(q);
        // TODO can we generate an equal com.hp.hpl.jena.query.Query?
        assertEquals(com.hp.hpl.jena.query.Query.QueryTypeSelect,
            q.getRawQueryObject(com.hp.hpl.jena.query.Query.class).getQueryType());
    }

    @Test
    public void testUnsupportedTemplates() {
        // unsupported predicate and first parameter
        String tpl = "zxcvb(a)";
        try {
            QueryParser.parse(tpl, testUri);
            fail("Query with unsupported predicate AND first parameter was parsed correctly. This should not happen.");
        } catch (IllegalArgumentException | UnsupportedOperationException ex) {}
        // unsupported predicate
        tpl = "zxcvb([LURI],bc,def)";
        try {
            QueryParser.parse(tpl, testUri);
            fail("Query with unsupported predicate was parsed correctly. This should not happen.");
        } catch (IllegalArgumentException | UnsupportedOperationException ex) {}
        // unsupported first parameter
        tpl = "sparql_describe(a)";
        try {
            QueryParser.parse(tpl, testUri);
            fail("Query with unsupported first parameter was parsed correctly. This should not happen.");
        } catch (IllegalArgumentException | UnsupportedOperationException ex) {}
    }

}
