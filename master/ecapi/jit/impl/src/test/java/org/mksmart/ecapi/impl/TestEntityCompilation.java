package org.mksmart.ecapi.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.net.URI;
import java.net.URL;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.EntityCompiler;
import org.mksmart.ecapi.api.GlobalType;
import org.mksmart.ecapi.api.id.CanonicalGlobalURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author alexdma
 * 
 */
public class TestEntityCompilation {

    private static RDFCatalogue cat;
    private static Logger log = LoggerFactory.getLogger(TestEntityCompilation.class);

    @BeforeClass
    public static void setup() throws Exception {
        URL uCat = RDFCatalogue.class.getResource("/config.ttl");
        log.debug("{}", uCat);
        cat = new RDFCatalogue(uCat);
    }

    private EntityCompiler ec;

    private URI uT1 = URI.create("http://data.open.ac.uk/qualification/w01");

    private URI uT2 = URI.create("http://data.linkedu.eu/kis/course/10007773/w01");

    @Before
    public void initTests() throws Exception {
        this.ec = new EntityCompilerImpl(cat, cat);
    }

    @Test
    public void testGlobalType() throws Exception {
        // Null identifier implies TOP type
        GlobalType ntype = new GlobalTypeImpl(null);
        assertEquals(GlobalType.TOP_URI, ntype.getId());
        // Top types are equal
        GlobalType top = new GlobalTypeImpl(GlobalType.TOP_URI);
        assertEquals(ntype, top);
    }

    @Test
    public void testBuildCompiler() throws Exception {
        // Just make sure exceptions are not thrown
        new EntityCompilerImpl(null, cat);
    }

    // @Test
    public void testInstanceCheck() throws Exception {
        ec.getInstances(new GlobalTypeImpl(CanonicalGlobalURI.parse("type/example")));
    }

    @Test
    public void testRunCompiler() throws Exception {
        Entity compiled = this.ec.compileEntity(uT1);
        assertFalse(compiled.getAttributes().isEmpty());
    }

}
