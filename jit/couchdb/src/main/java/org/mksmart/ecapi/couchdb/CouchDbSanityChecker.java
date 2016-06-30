/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mksmart.ecapi.couchdb;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang.NotImplementedException;
import org.json.JSONException;
import org.json.JSONObject;
import org.mksmart.ecapi.api.AssemblyProvider;
import org.mksmart.ecapi.api.MisconfiguredException;
import org.mksmart.ecapi.api.SanityChecker;
import org.mksmart.ecapi.commons.couchdb.client.DocumentProvider;
import org.mksmart.ecapi.commons.couchdb.client.DocumentWriter;
import org.mksmart.ecapi.commons.couchdb.client.LocalDocumentProvider;
import org.skyscreamer.jsonassert.FieldComparisonFailure;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SanityChecker}
 * 
 * @author alessandro <alexdma@apache.org>
 *
 */
public class CouchDbSanityChecker implements SanityChecker<String> {

    protected static String DIR_DOC_DESIGN_CONFIG = "couchdb/config_db/_design";

    protected JSONComparator comparator;

    private DocumentProvider<JSONObject> configLocalProvider, storeLocalProvider;

    /**
     * FIXME move out of here and instantiate
     */
    private DocumentWriter<JSONObject> configRemoteWriter;

    private Logger log = LoggerFactory.getLogger(getClass());

    protected boolean severe;

    public CouchDbSanityChecker() {
        this(false);
    }

    /**
     * Constructs a new instance of {@link CouchDbSanityChecker}.
     * 
     * @param severe
     *            if true, the checker's methods will throw runtime exceptions whenever something is wrong.
     */
    public CouchDbSanityChecker(boolean severe) {
        this.severe = severe;
        this.comparator = new DefaultComparator(JSONCompareMode.LENIENT);
        Properties config = new Properties();
        URL base = getClass().getResource("/couchdb");
        config.put("couchdb.url", base.toString());
        config.put("couchdb.db", "config_db");
        new Config(config);
        configLocalProvider = new LocalDocumentProvider(config.getProperty("couchdb.url"),
                config.getProperty("couchdb.db"));
    }

    @Override
    public Set<Object> getFailingItems(AssemblyProvider<String> provider) {
        if (!getSupportedProviderTypes().contains(provider.getClass())) throw new UnsupportedOperationException(
                "AssemblyProviders of type" + provider.getClass().getName() + " are not supported.");
        Set<Object> failed = new HashSet<>();
        DocumentProvider<JSONObject> rp = ((CouchDbAssemblyProvider) provider).getDocumentProvider();
        try {
            for (String s : listSubResources(DIR_DOC_DESIGN_CONFIG))
                processResource(s, DIR_DOC_DESIGN_CONFIG, rp, configRemoteWriter, false, failed);
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(
                    "Something's wrong: could not look up resources in classpath for database documents!", e);
        }
        return failed;
    }

    @Override
    public Set<Object> getSaneItems(AssemblyProvider<String> provider) {
        throw new NotImplementedException("NIY");
    }

    @Override
    public Set<Class<?>> getSupportedProviderTypes() {
        Set<Class<?>> se = new HashSet<>();
        se.add(CouchDbAssemblyProvider.class);
        return Collections.unmodifiableSet(se);
    }

    @Override
    public boolean isSane(AssemblyProvider<String> provider) {
        return getFailingItems(provider).isEmpty();
    }

    /**
     * Utility method, might need to move elsewhere.
     * 
     * @param path
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    protected String[] listSubResources(String path) throws URISyntaxException, IOException {
        URL dirURL = getClass().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            // A file path: easy enough
            return new File(dirURL.toURI()).list();
        }
        // In case of a jar file, we can't actually find a directory.
        // Have to assume the same jar as this class.
        if (dirURL == null) {
            String me = getClass().getName().replace(".", "/") + ".class";
            dirURL = getClass().getClassLoader().getResource(me);
        }
        // a JAR path
        if (dirURL.getProtocol().equals("jar")) {
            // strip out only the JAR file
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
            Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
            Set<String> result = new HashSet<String>(); // avoid duplicates in case it is a subdirectory
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path)) { // filter according to the path
                    String entry = name.substring(path.length());
                    if (entry.startsWith("/")) entry = entry.substring(1);
                    int checkSubdir = entry.indexOf("/");
                    // if it is a subdirectory, we just return the directory name
                    if (checkSubdir >= 0) entry = entry.substring(0, checkSubdir);
                    if (!entry.isEmpty()) result.add(entry);
                }
            }
            jar.close();
            return result.toArray(new String[result.size()]);
        }
        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }

    private void processResource(String name,
                                 String dir,
                                 DocumentProvider<JSONObject> rp,
                                 DocumentWriter<JSONObject> writer,
                                 boolean repair,
                                 final Set<Object> failing) {
        log.debug("Checking remote design document that matches {}", name);
        log.debug(" ... Looking at {}", '/' + dir + '/' + name);
        URL resUrl = getClass().getResource('/' + dir + '/' + name);
        log.debug(" ... found at URL {}", resUrl);
        if (resUrl == null) throw new IllegalStateException("Could not find expected internal resource "
                                                            + name);
        JSONObject ddocLocal;
        try {
            ddocLocal = configLocalProvider.getResource(resUrl.toString());
        } catch (JSONException jex) {
            throw new IllegalStateException(
                    "Expected design document " + resUrl + " is not valid JSON! This should not happen"
                            + " and it could be an indication that the executable is corrupted.", jex);
        }
        if (ddocLocal != null) log.debug(" ... local resource is correct JSON and contains {} attributes",
            ddocLocal.keySet().size());
        String remoteLocation = "_design/" + name.replaceAll(".json$", "");
        log.debug(" ... now checking for remote design document in {}", remoteLocation);
        JSONObject ddocRemote;
        try {
            ddocRemote = rp.getDocument(remoteLocation);
        } catch (JSONException jex) {
            log.error("Document at {} is not valid JSON! Is that a real working CouchDB?", remoteLocation);
            failing.add(remoteLocation);
            if (severe) reactSeverely(failing);
            return;
        }
        if (ddocRemote != null) log.debug(" ... remote resource is correct JSON and contains {} attributes",
            ddocRemote.keySet().size());
        // Now check the objects one by one...
        JSONCompareResult res = JSONCompare.compareJSON(ddocLocal, ddocRemote, this.comparator);
        log.trace(" ... Checking if values are equal... {}", res.passed());
        for (FieldComparisonFailure fail : res.getFieldMissing())
            log.warn(" * Field {} : expected value, none found.", fail.getField());
        if (repair) {
            log.info("Attempting repair...");
        }
        for (FieldComparisonFailure fail : res.getFieldFailures()) {
            log.warn(" * Field {} : values do not match.", fail.getField());
            log.trace(" ** expected:\r\n{}", fail.getExpected());
            log.trace(" ** got:\r\n{}", fail.getActual());
        }
        if (res.failed()) {
            failing.add(remoteLocation);
            if (severe) reactSeverely(failing);
        }
    }

    private void reactSeverely(Set<Object> failed) {
        throw new MisconfiguredException(
                "Found " + failed.size() + " misconfigured objects."
                        + " This sanity checker is set to be severe, so exiting without further checks.",
                failed);
    }

    @Override
    public void rebuild(AssemblyProvider<String> provider) {
        throw new NotImplementedException("NIY");
    }

    @Override
    public void repair(AssemblyProvider<String> provider) {
        if (!getSupportedProviderTypes().contains(provider.getClass())) throw new UnsupportedOperationException(
                "AssemblyProviders of type" + provider.getClass().getName() + " are not supported.");
        DocumentProvider<JSONObject> rp = ((CouchDbAssemblyProvider) provider).getDocumentProvider();
        try {
            for (String s : listSubResources(DIR_DOC_DESIGN_CONFIG))
                processResource(s, DIR_DOC_DESIGN_CONFIG, rp, configRemoteWriter, true, new HashSet<>());
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(
                    "Something's wrong: could not look up resources in classpath for database documents!", e);
        }
    }

}
