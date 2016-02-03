package org.mksmart.ecapi.web.resources;

import java.net.URL;

import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Utility resource used for shared capabilities such as CORS handling.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public abstract class BaseResource {

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

}
