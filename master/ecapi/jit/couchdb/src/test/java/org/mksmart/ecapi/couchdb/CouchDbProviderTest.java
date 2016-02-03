package org.mksmart.ecapi.couchdb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Properties;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mksmart.ecapi.api.AssemblyProvider;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.EntityCompiler;
import org.mksmart.ecapi.commons.couchdb.client.DocumentProvider;
import org.mksmart.ecapi.commons.couchdb.client.LocalDocumentProvider;
import org.mksmart.ecapi.couchdb.id.HeuristicIdGenerator;
import org.mksmart.ecapi.impl.EntityCompilerImpl;
import org.mksmart.ecapi.impl.RDFCatalogue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CouchDbProviderTest {

    private static Logger log = LoggerFactory.getLogger(CouchDbProviderTest.class);

    private static Properties config;

    private DocumentProvider<JSONObject> localProvider;

    private URI uT1 = URI.create("http://data.open.ac.uk/qualification/w01");

    @BeforeClass
    public static void init() throws IOException {
        config = new Properties();
        URL base = CouchDbProviderTest.class.getClassLoader().getResource("");
        log.info("Using database provider at {}", base);
        config.put("couchdb.url", base.toString());
        config.put("couchdb.db", "db");
    }

    @Before
    public void prepareTest() throws Exception {
        new Config(config);
        localProvider = new LocalDocumentProvider(config.getProperty("couchdb.url"),
                config.getProperty("couchdb.db"));
    }

    @Test
    public void testInstantiateEntityProvider() throws Exception {
        AssemblyProvider provider = new CouchDbAssemblyProvider(config, localProvider);
        String mc = provider.getMicrocompiler(URI.create("http://purl.org/vocab/aiiso/schema#Course"));
        assertNotNull(mc);
        assertFalse(mc.isEmpty());
    }

    @Test
    public void testCompiler() throws Exception {
        // We use the RDF provider for now
        URL uCat = RDFCatalogue.class.getResource("/config.ttl");
        log.debug("{}", uCat);
        RDFCatalogue provider = new RDFCatalogue(uCat);

        EntityCompiler comp = new EntityCompilerImpl(provider, provider);
        Entity e = comp.compileEntity(uT1);

        URI id = new HeuristicIdGenerator(localProvider).createId(e);
        assertNotNull(id);
        // new SimpleEntityStore(Config.getInstance()).store(e, id);

    }

}
