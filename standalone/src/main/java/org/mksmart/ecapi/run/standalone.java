package org.mksmart.ecapi.run;

import static org.mksmart.ecapi.access.Config.KEYMGMT_ISAPI_HOST;
import static org.mksmart.ecapi.access.Config.KEYMGMT_KEY_OPENDATA;
import static org.mksmart.ecapi.access.Config.KEYMGMT_MYSQL_DB;
import static org.mksmart.ecapi.access.Config.KEYMGMT_MYSQL_HOST;
import static org.mksmart.ecapi.access.Config.KEYMGMT_MYSQL_USER;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.wink.client.ClientAuthenticationException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.json.JSONObject;
import org.mksmart.ecapi.access.ApiKeyDriver;
import org.mksmart.ecapi.access.PermissiveKeyDriver;
import org.mksmart.ecapi.access.auth.VisibilityChecker;
import org.mksmart.ecapi.access.isapi.IsapiKeyDriver;
import org.mksmart.ecapi.access.mysql.SsimpleAuthKeyDriver;
import org.mksmart.ecapi.api.AssemblyProvider;
import org.mksmart.ecapi.api.Catalogue;
import org.mksmart.ecapi.api.DebuggableEntityCompiler;
import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.id.IdGenerator;
import org.mksmart.ecapi.api.storage.Cache;
import org.mksmart.ecapi.api.storage.Store;
import org.mksmart.ecapi.commons.couchdb.client.DocumentProvider;
import org.mksmart.ecapi.commons.couchdb.client.RemoteDocumentProvider;
import org.mksmart.ecapi.core.LaunchConfiguration;
import org.mksmart.ecapi.couchdb.Config;
import org.mksmart.ecapi.couchdb.CouchDbAssemblyProvider;
import org.mksmart.ecapi.couchdb.CouchDbCatalogue;
import org.mksmart.ecapi.couchdb.CouchDbSanityChecker;
import org.mksmart.ecapi.couchdb.id.ProgrammaticGlobalURIGenerator;
import org.mksmart.ecapi.couchdb.storage.CacheImpl;
import org.mksmart.ecapi.couchdb.storage.FragmentPerQueryStore;
import org.mksmart.ecapi.impl.EntityCompilerImpl;
import org.mksmart.ecapi.impl.storage.NonStoringEntityStore;
import org.mksmart.ecapi.web.util.SPARQLWriter;
import org.mksmart.ecapi.web.util.SparqlHttpWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launcher class for standalone packaging.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class standalone {

    /**
     * Command-line parser for standalone application.
     * 
     * @author alessandro <alexdma@apache.org>
     * 
     */
    private static class Cli {

        private String[] args = null;
        private Options options = new Options();

        public Cli(String[] args) {
            this.args = args;
            options.addOption("c", "config", true, "Config file path (required).");
            options.addOption("f", "force", false,
                "Attempt to start ECAPI even if the sanity check on its database fails"
                        + " (WARNING: only use if you know what you are doing!).");
            options.addOption("h", "help", false, "Show this help.");
            options.addOption("p", "port", true, "Set the port the server will listen to (defaults to 8080).");
        }

        /**
         * Parses command line arguments and acts upon them.
         */
        public void parse() {
            CommandLineParser parser = new BasicParser();
            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);

                String[] args = cmd.getArgs();
                if (args.length > 0) {
                    if ("init".equals(args[0])) {
                        log.error("Requested init task but this is not implemented yet.");
                        System.exit(1);
                    } else if ("repair".equals(args[0])) {
                        log.error("Requested repair task but this is not implemented yet.");
                        System.exit(1);
                    } else {
                        log.error("Invalid argument " + args[0]);
                        help();
                        System.exit(1);
                    }
                }

                if (cmd.hasOption('h')) help();
                if (cmd.hasOption('c')) {
                    String confpath = cmd.getOptionValue('c');
                    try {
                        // also assigns singleton
                        new Config(confpath);
                        new LaunchConfiguration(confpath);
                        log.info("Using configuration at " + confpath);
                    } catch (Exception ex) {
                        log.error("Invalid configuration file " + confpath, ex);
                        System.exit(1);
                    }
                } else {
                    log.error("No configuration file specified");
                    help();
                }
                if (cmd.hasOption('f')) {
                    force = true;
                }
                if (cmd.hasOption('p')) {
                    port = Integer.parseInt(cmd.getOptionValue('p'));
                    if (port < 0 || port > 65535) {
                        log.error("Invalid port number " + port + ". Must be in the range [0,65535].");
                        System.exit(100);
                    }
                }
            } catch (UnrecognizedOptionException e) {
                System.err.println(e.getMessage());
                help();
            } catch (ParseException e) {
                log.error("Failed to parse comand line properties", e);
                help();

            }
        }

        /**
         * Prints help.
         */
        private void help() {
            String syntax = "java [java-opts] -jar [this-jarfile] -c [config-file] [TASK]";
            String footer = "TASK can be one of init|repair or none at all (for normal ECAPI operation).";
            new HelpFormatter().printHelp(syntax, "", options, footer);
            System.exit(0);
        }

    }

    public static final String _DEFAULT_STORE_CLASSNAME = "org.mksmart.ecapi.impl.storage.NonStoringEntityStore";

    private static boolean force = false;

    private static Logger log = LoggerFactory.getLogger(standalone.class);

    private static final int MISCONFIGURED = -3;

    private static int port = 8080;

    private static final int STORE_INITIALIZE = -2;

    private static final int UNREACHABLE_COMPILER = -1;

    public static void main(String[] args) throws Exception {
        long before = System.currentTimeMillis();
        new Cli(args).parse();
        log.info("Initialising Jetty server on port {}", port);
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        // Set some timeout options to make debugging easier.
        connector.setIdleTimeout(1000 * 60 * 60);
        connector.setSoLingerTime(-1);
        connector.setPort(port);
        server.setConnectors(new Connector[] {connector});

        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        String webxmlLocation = standalone.class.getResource("/WEB-INF/web.xml").toString();
        root.setDescriptor(webxmlLocation);
        String resLocation = standalone.class.getResource("/webroot").toString();
        root.setResourceBase(resLocation);
        root.setParentLoaderPriority(true);
        server.setHandler(root);

        initWebApp(root);

        try {
            server.start();
            // while (System.in.available() == 0) {
            // Thread.sleep(5000);
            // server.stop();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(100);
        }

        log.info("Startup completed in {} ms", System.currentTimeMillis() - before);
    }

    @SuppressWarnings("rawtypes")
    private static void initStore(WebAppContext wactx, Config config) {
        Store store;
        String prop = "store.class";
        String scname = config.asProperties().getProperty(prop, _DEFAULT_STORE_CLASSNAME);
        Class<? extends Store> storeClazz;
        if (config.getStorageDbName() == null || config.getStorageDbName().isEmpty()) {
            log.warn("Storage DB name not set. ECAPI will run in stateless mode.");
            storeClazz = NonStoringEntityStore.class;
        } else try {
            storeClazz = Class.forName(scname).asSubclass(Store.class);
        } catch (ClassNotFoundException | ClassCastException e) {
            String errMsg;
            if (e instanceof ClassNotFoundException) errMsg = "Illegal store class name {} : could not find such a class to load.";
            else errMsg = "Invalid store class name {} : it is not an implementation of "
                          + Store.class.getCanonicalName();
            log.error(" ### FATAL ### : " + errMsg, scname);
            log.error("If you do not know which class name to use for the store, "
                      + "unset the '{}' parameter in your properties file to launch a stateless runtime.",
                prop);
            log.error("Alternatively, refer to the ECAPI documentation for legal store implementation names.");
            log.error("Exiting now");
            System.exit(STORE_INITIALIZE);
            return;
        }
        log.debug("Initializing store of type {}", storeClazz.getCanonicalName());
        try {
            store = storeClazz.getDeclaredConstructor(new Class[] {Config.class}).newInstance(config);
        } catch (IllegalStateException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException | SecurityException ex) {
            log.error("{} caught for storage database.", ex.getClass());
            log.error(" ... is your storage DB running (most likely CouchDb)?");
            log.error("Exiting now");
            System.exit(UNREACHABLE_COMPILER);
            return;
        } catch (NoSuchMethodException e) {
            try {
                store = storeClazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e1) {
                log.error("{} caught for storage database.", e1.getClass());
                log.error("Exiting now");
                System.exit(UNREACHABLE_COMPILER);
                return;
            }
        }
        if (store != null) {
            log.debug("Store initialized.");
            wactx.getServletContext().setAttribute(Store.class.getName(), store);
        } else {
            log.error("Store is null, but expected a {} - This shouldn't be happening.",
                storeClazz.getCanonicalName());
            System.exit(STORE_INITIALIZE);
            return;
        }
    }

    /**
     * Initialises the main Web application on a given {@link WebAppContext} by registering shared resources
     * therein.
     * 
     * @param wactx
     *            the Web application context
     */

    private static void initWebApp(WebAppContext wactx) {
        // Instantiated context and configuration
        ServletContext sctx = wactx.getServletContext();
        Config couchConfig = Config.getInstance();
        LaunchConfiguration lc = LaunchConfiguration.getInstance();
        // Instantiate storage
        initStore(wactx, couchConfig);
        // Instantiate compiler
        Catalogue ctlg = null;
        try {
            DocumentProvider<JSONObject> dp = new RemoteDocumentProvider(new URL(
                    (String) lc.get(Config.SERVICE_URL)), (String) lc.get(Config.DB),
                    new UsernamePasswordCredentials((String) lc.get(Config.USERNAME), (String) lc
                            .get(Config.PASSWORD)));
            AssemblyProvider<String> ep = new CouchDbAssemblyProvider(couchConfig.asProperties(), dp);
            sctx.setAttribute(AssemblyProvider.class.getName(), ep);

            log.debug("Performing configuration database for sanity check...");
            Set<?> failures = new CouchDbSanityChecker(false).getFailingItems(ep);
            if (failures.isEmpty()) log.info("Configuration environment appears to be sane.");
            else {
                log.warn("The following documents of the configuration database are in an unexpected state:");
                for (Object o : failures)
                    log.warn(" * {}", o);
                log.warn("Issues detected in configuration environment! See previous errors and warnings for details.");
                if (force) log
                        .warn("force option was set, so attempting startup anyway (at your own risk!)...");
                else {
                    log.error("It seems the structure of your configuration database has been tampered with.");
                    log.error("By default ECAPI will not repair it. If you wish to attempt a repair, make sure the database user"
                              + " you set in the configuration file has write permissions and relaunch the ECAPI in the following way:");
                    log.error("");
                    log.error("    $ java [java-opts] -jar [this-jarfile] -c [config-file] repair");
                    log.error("");
                    log.error("Or, to (re)build the database from scratch (and erase all your dataset configurations!):");
                    log.error("");
                    log.error("    $ java [java-opts] -jar [this-jarfile] -c [config-file] init");
                    log.error("");
                    log.error("Alternatively, you can use the --force option to start ECAPI anyway, but expect things to go funny!");
                    log.error("Exiting now");
                    System.exit(MISCONFIGURED);
                }
            }

            IdGenerator<GlobalURI,?> idgen = new ProgrammaticGlobalURIGenerator(dp);
            sctx.setAttribute(IdGenerator.class.getName(), idgen);
            ctlg = new CouchDbCatalogue(dp);
            sctx.setAttribute(Catalogue.class.getName(), ctlg);
            Store<?,?> stor = (Store<?,?>) sctx.getAttribute(Store.class.getName());
            Cache cache = null;
            if (stor instanceof FragmentPerQueryStore) cache = new CacheImpl(dp, (FragmentPerQueryStore) stor);
            else log.warn("Entity store implementation '{}' does not support lookahead of cache hits.", stor
                    .getClass().getName());
            if (cache == null) log.warn("Could not initialise cache. All queries will be performed fresh.");
            sctx.setAttribute(DebuggableEntityCompiler.class.getName(), new EntityCompilerImpl(ep, ctlg,
                    cache, stor));
        } catch (IllegalStateException | ClientAuthenticationException ex) {
            log.error("Illegal state caught for compiler database.");
            log.error(" ... is CouchDb running and healthy?");
            log.error("Exiting now");
            System.exit(UNREACHABLE_COMPILER);
        } catch (MalformedURLException e) {
            log.error("Illegal state caught for compiler database.");
            log.error(" ... parameter {} does not seem to be a well-formed URL.", Config.SERVICE_URL);
            log.error("Exiting now");
            System.exit(UNREACHABLE_COMPILER);
        }

        // Instantiate access components
        Properties pro = couchConfig.asProperties();
        sctx.setAttribute(VisibilityChecker.class.getName(), new VisibilityChecker(ctlg));
        ApiKeyDriver driver = selectDriver(pro);
        log.debug("Instantiated permission checker of type {}", driver.getClass());
        sctx.setAttribute(ApiKeyDriver.class.getName(), driver);
        if (pro.containsKey("org.mksmart.web.util.sparql.writer")) {
            String clazzName = SPARQLWriter.class.getName();
            String sup = pro.getProperty("org.mksmart.web.util.sparql.writer");
            if (pro.containsKey("org.mksmart.web.util.sparql.data")) {
                String sda = pro.getProperty("org.mksmart.web.util.sparql.data");
                sctx.setAttribute(clazzName, new SparqlHttpWriter(sup, sda));
            } else sctx.setAttribute(clazzName, new SPARQLWriter(sup));
        }

    }

    private static ApiKeyDriver selectDriver(Properties configuration) {
        // Priority to ISAPI key driver
        if (configuration.containsKey(KEYMGMT_ISAPI_HOST)) return new IsapiKeyDriver(configuration);
        if ((configuration.containsKey(KEYMGMT_MYSQL_HOST) || configuration.containsKey(KEYMGMT_MYSQL_DB) || configuration
                .containsKey(KEYMGMT_MYSQL_USER)) || configuration.containsKey(KEYMGMT_KEY_OPENDATA)) return new SsimpleAuthKeyDriver(
                configuration);
        else return new PermissiveKeyDriver();
    }

}
