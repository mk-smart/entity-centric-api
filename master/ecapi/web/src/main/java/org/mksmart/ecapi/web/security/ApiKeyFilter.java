package org.mksmart.ecapi.web.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.mksmart.ecapi.access.ApiKeyDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for processing credentials as API keys.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class ApiKeyFilter implements Filter {

    /** Logger */
    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private String realm = "Protected";

    private boolean force = false;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String paramRealm = filterConfig.getInitParameter("realm");
        if (StringUtils.isNotBlank(paramRealm)) realm = paramRealm;
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            // Fine, open data only
            if (true) filterChain.doFilter(servletRequest, servletResponse);
            else unauthorized(response); // TODO support force-authenticate
            return;
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
                        String _password = credentials.substring(p + 1).trim();
                        if (StringUtils.isNotBlank(_password)) {
                            log.warn("API key '{}' requested access to <{}> using a password (denied).",
                                _apikey, request.getRequestURI());
                            unauthorized(response, "Invalid authentication token");
                            return;
                        } else if (StringUtils.isBlank(_apikey)) {
                            log.info("blank APIKey!");
                            // Fine, open data only
                            filterChain.doFilter(servletRequest, servletResponse);
                        } else try {
                            if (!handleApiKeys(Collections.singletonList(_apikey), request.getSession()
                                    .getServletContext())) {
                                log.warn("Invalid API key '{}' requested access to <{}> (denied).", _apikey,
                                    request.getRequestURI());
                                unauthorized(response, "Invalid API key");
                                return;
                            }
                        } catch (Exception exx) {
                            log.error("Could not connect to MySQL database ", exx);
                            unauthorized(response, "Unable to verify API key");
                            return;
                        }
                        filterChain.doFilter(servletRequest, servletResponse);
                    } else {
                        unauthorized(response, "Invalid authentication token");
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new Error("Couldn't retrieve authentication", e);
                }
            } else unauthorized(response, "Authentication type '" + basic + "' is not supported");
        }

    }

    @Override
    public void destroy() {}

    private boolean handleApiKeys(final List<String> apiKeys, ServletContext servletContext) {
        if (apiKeys == null || apiKeys.isEmpty()
            || (apiKeys.size() == 1 && "null".equals(apiKeys.iterator().next()))) {
            log.info("No API key supplied by client. Will retrieve open data only.");
            return true; // not an empty set!
        }
        // log.debug("supplied API keys:");
        // for (String apikey : apiKeys)
        // log.debug(" ... {}", apikey);
        // log.debug("(not verified yet)");
        ApiKeyDriver keyDrv = (ApiKeyDriver) servletContext.getAttribute(ApiKeyDriver.class.getName());
        if (!keyDrv.exists(apiKeys.toArray(new String[0]))) return false;
        // servletContext.setAttribute("org.mksmart.ecapi.apikey", apiKeys);
        return true;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
        response.sendError(401, message);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        unauthorized(response, "Unauthorized");
    }

}
