package org.mksmart.ecapi.web.resources;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.codec.binary.Base64;
import org.mksmart.ecapi.access.ApiKeyDriver;
import org.mksmart.ecapi.access.NotCheckingRightsException;
import org.mksmart.ecapi.access.PermissiveKeyDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility resource used for shared capabilities such as CORS handling.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public abstract class BaseResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    public abstract ServletContext getServletContext();

    /**
     * Attaches Cross-Origin Resource Sharing (CORS) capabilities to a {@link ResponseBuilder} that represents
     * an HTTP response being constructed elsewhere. This is required if, for example, we want the API to be
     * accessed via Ajax from a different domain than the one hosting the ECAPI.
     * <p>
     * This variant allows every origin by setting "*" as the value for the Access-Control-Allow-Origin
     * header.
     * </p>
     * 
     * @param rb
     *            the response being built.
     */
    protected void handleCors(ResponseBuilder rb) {
        // addCORSOrigin(servletContext, rb, headers);
        handleCors(rb, null);
    }

    /**
     * Attaches Cross-Origin Resource Sharing (CORS) capabilities to a {@link ResponseBuilder} that represents
     * an HTTP response being constructed elsewhere. This is required if, for example, we want the API to be
     * accessed via Ajax from a different domain than the one hosting the ECAPI.
     * 
     * @param rb
     *            the response being built.
     * @param origin
     *            the request origin to allow. If null, defaults to all (*).
     */
    protected void handleCors(ResponseBuilder rb, URL origin) {
        rb.header("Access-Control-Allow-Origin", origin == null ? "*" : origin);
        rb.header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        rb.header("Access-Control-Allow-Credentials", "true");
        // TODO make allow-methods customisable
        rb.header("Access-Control-Allow-Methods", "GET, OPTIONS");
        rb.header("Access-Control-Max-Age", "1209600"); // Two weeks
    }

    // TODO: Add getting key through parameter
    protected Set<String> handleCredentials(HttpHeaders headers, boolean with_debug) {
        Set<String> apiKeys = new HashSet<>();
        String authHeader;
        List<String> authHeaderList = headers.getRequestHeader("Authorization");
        if (authHeaderList != null) {
            Iterator<String> authit = authHeaderList.iterator();
            if (authit.hasNext()) {
                authHeader = authit.next();
                log.trace("Got authorization header {}", authHeader);
            } else {
                log.trace("No authorization header found");
                return null;
            }
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens()) {
                String basic = st.nextToken();
                if (basic.equalsIgnoreCase("Basic")) {
                    try {
                        String credentials = new String(Base64.decodeBase64(st.nextToken()), "UTF-8");
                        int p = credentials.indexOf(":");
                        if (p != -1) {
                            String _apikey = credentials.substring(0, p).trim();
                            if (!_apikey.isEmpty()) {
                                log.trace("Adding API key {}", _apikey);
                                apiKeys.add(_apikey);
                            }
                        }
                    } catch (UnsupportedEncodingException e) {
                        throw new Error("Couldn't retrieve authentication", e);
                    }
                }
            }
        }
        // MDA: remove this - the APIKeyDriver should return the open data sets
        // from authorisation
        /*
         * if (apiKeys == null || apiKeys.isEmpty() || (apiKeys.size() == 1 &&
         * "null".equals(apiKeys.iterator().next()))) {
         * log.info("No API key supplied by client. Will retrieve open data only."); return null; // not an
         * empty set! }
         */
        ApiKeyDriver keyDrv = (ApiKeyDriver) getServletContext().getAttribute(ApiKeyDriver.class.getName());
        // if (!keyDrv.exists(apiKeys.toArray(new String[0]))) throw new RuntimeException(
        // "Unmatching API keys found. Deal with it.");
        try {
            if (apiKeys == null || apiKeys.isEmpty()) return keyDrv.getDataSources();
            else return keyDrv.getDataSources(apiKeys.toArray(new String[0]));
        } catch (NotCheckingRightsException e) {
            if (keyDrv instanceof PermissiveKeyDriver) return null;
            else throw new RuntimeException(
                    "Non-permissive authorisation checker refused to check for access rights. This should not happen.");
        }
    }

}
