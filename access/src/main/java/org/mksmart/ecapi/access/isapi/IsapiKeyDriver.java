package org.mksmart.ecapi.access.isapi;

import static org.mksmart.ecapi.access.Config.KEYMGMT_ISAPI_HOST;
import static org.mksmart.ecapi.access.Config.KEYMGMT_ISAPI_PASSWORD;
import static org.mksmart.ecapi.access.Config.KEYMGMT_ISAPI_USER;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.NotImplementedException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.wink.client.ClientAuthenticationException;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.ClientWebException;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.apache.wink.client.handlers.BasicAuthSecurityHandler;
import org.mksmart.ecapi.access.ApiKeyDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A driver for API key checking based on MK:Smart ISAPI services.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class IsapiKeyDriver implements ApiKeyDriver {

    protected static final String APIKEY_CHECK = "getKeyInfo";

    protected static final String SUBSCRIPTION_LIST = "getKeysSubscription";

    protected String host;

    protected Logger log = LoggerFactory.getLogger(getClass());

    private RestClient client;

    public IsapiKeyDriver(Properties config) {
        log.debug("Initialised driver of type {}", getClass());
        this.host = config.getProperty(KEYMGMT_ISAPI_HOST);
        if (this.host == null) throw new IllegalArgumentException(
                "Property " + KEYMGMT_ISAPI_HOST + " cannot be empty. It must be a well-formed URL.");
        try {
            new URL(this.host);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Value " + this.host + " for property " + KEYMGMT_ISAPI_HOST
                                               + " is not a well-formed URL.");
        }
        ClientConfig clientConf = new ClientConfig();
        Credentials credentials = new UsernamePasswordCredentials(new String(
                config.getProperty(KEYMGMT_ISAPI_USER)).trim(), new String(
                config.getProperty(KEYMGMT_ISAPI_PASSWORD)).trim());
        BasicAuthSecurityHandler auth = new BasicAuthSecurityHandler();
        // Only set credentials if there is a username, no spaces around
        if (credentials.getUserPrincipal() != null) {
            String un = new String(credentials.getUserPrincipal().getName()).trim();
            if (!un.isEmpty()) {
                auth.setUserName(un);
                if (credentials.getPassword() != null) {
                    String pw = new String(credentials.getPassword()).trim();
                    if (!pw.isEmpty()) auth.setPassword(pw);
                }
            }
        }
        clientConf.handlers(auth);
        client = new RestClient(clientConf);
    }

    @Override
    public boolean exists(String... keys) {
        if (keys.length < 1) throw new IllegalArgumentException("Key set cannot be empty");
        if (host == null) throw new IllegalStateException(
                "Driver was initialised without a valid service URL. This should not have happened.");
        log.debug("Checking validity of {} API keys.", keys.length);
        Set<String> toCheck = new HashSet<>(Arrays.asList(keys)), remainder = new HashSet<>(toCheck);
        URI url = URI.create(host);
        for (String key : keys) {
            Resource resource = client.resource(url);
            resource.queryParam("action", APIKEY_CHECK);
            resource.queryParam("keyUuid", key);
            Document dom;
            try {
                dom = doQuery(resource);
            } catch (Exception ex) {
                log.error("Exception caught: {}", ex.getMessage());
                log.error("Failing URL was <{}>", url);
                return false;
            }
            Element apikey = dom.getDocumentElement();
            if (apikey.hasAttribute("key")) {
                String k = apikey.getAttribute("key");
                if (toCheck.contains(k)) remainder.remove(k);
                else log.warn("Got unexpected key {} for which no check was required.", k);
            } else {
                log.error("Returned XML missing expected attribute 'key'. Aborting check.");
                return false;
            }
        }
        if (remainder.isEmpty()) log.debug("|<== DONE. All keys were found.");
        else log.debug("|<== FAIL. Not all keys were matched.");
        return remainder.isEmpty();
    }

    @Override
    public Set<String> getDataSources(String... keys) {
        if (keys.length < 1) throw new IllegalArgumentException("Key set cannot be empty");
        if (host == null) throw new IllegalStateException(
                "Driver was initialised without a valid service URL. This should not have happened.");
        Set<String> result = new HashSet<>();
        URI url = URI.create(host);
        Resource resource = client.resource(url);
        resource.queryParam("action", SUBSCRIPTION_LIST);
        resource.queryParam("keyUuid", (Object[]) keys);
        Document dom;
        try {
            dom = doQuery(resource);
        } catch (Exception ex) {
            log.error("Exception caught: {}", ex.getMessage());
            log.error("Failing URL was <{}>", url);
            return Collections.emptySet();
        }
        Element subscriptions = dom.getDocumentElement();
        if (subscriptions.hasAttribute("key")
            && !Arrays.asList(keys).contains(subscriptions.getAttribute("key"))) throw new SecurityException(
                "ISAPI call returned unexpected key " + subscriptions.getAttribute("key"));
        NodeList feeds = subscriptions.getElementsByTagName("feed");
        log.debug("Start list of datasets (total={}):", feeds.getLength());
        for (int i = 0; i < feeds.getLength(); i++) {
            Node feed = feeds.item(i);
            if (feed instanceof Element) {
                String dsUuid = ((Element) feed).getAttribute("ckanUuid");
                log.debug(" * <{}>", dsUuid);
                result.add(dsUuid);
            }
        }
        log.debug("End list of datasets.");
        return result;
    }

    private Document doQuery(Resource resource) {
        Document doc = null;
        Source response;
        try {
            response = resource.accept(MediaType.APPLICATION_XML).get(Source.class);
            log.trace("{}", response);
        } catch (ClientAuthenticationException ex) {
            log.error("Database authentication failed. Reason: {}", ex.getMessage());
            throw ex;
        } catch (ClientWebException ex) {
            ClientResponse resp = ex.getResponse();
            log.warn("Retrieval failed. Reason: client response was '{} {}'", resp.getStatusCode(),
                resp.getStatusType());
            throw ex;
        }
        if (!(response instanceof StreamSource)) log.error(
            "Unsupported class for return type of XML response {} (was expecting a {})", response.getClass(),
            StreamSource.class);
        else {
            InputStream in = ((StreamSource) response).getInputStream();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.parse(in);
            } catch (ParserConfigurationException | SAXException | IOException e) {
                log.error("Failed to parse XML", e);
            }
        }
        return doc;
    }

    @Override
    public boolean hasRight(String key, String resourceID, int right) {
        throw new UnsupportedOperationException("cannot check the rights of a key");
    }

    @Override
    public Set<String> getDataSources() {
        throw new NotImplementedException("NIY");
    }

    @Override
    public boolean grant(String key, String ukey, String resourceID, int right) {
        throw new NotImplementedException("NIY");
    }

}
