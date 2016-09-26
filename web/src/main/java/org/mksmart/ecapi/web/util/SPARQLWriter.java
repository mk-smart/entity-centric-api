package org.mksmart.ecapi.web.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * send NT to a SPARQL Update endpoint
 * 
 * TODO Please. Use Jena ARQ instead.
 * 
 * @author Mathieu d'Aquin <mathieu.daquin.w@gmail.com>
 * 
 */
public class SPARQLWriter {

    private String endpointUrl = null;

    private Logger log = LoggerFactory.getLogger(getClass());

    public SPARQLWriter(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public int createGraph(String graph) {
        return doUpdate("CREATE GRAPH <" + graph + ">");
    }

    public int write(String NT, String graph) {
       // System.err.println("INSERT DATA { GRAPH <" + graph + "> {" + NT + "} }");
        return doUpdate("INSERT DATA { GRAPH <" + graph + "> {" + NT + "} }");
    }

    public boolean exists(String graph) {
        HttpEntity e = doQuery("ASK { GRAPH <" + graph + "> { ?s ?p ?o } } ", "text/plain");
        try {
            String yesno = IOUtils.toString(e.getContent());
            if ("yes".equals(yesno)) return true;
            else if ("no".equals(yesno)) return false;
            else return false;
        } catch (IllegalStateException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private HttpEntity doQuery(String query, String mimeType) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpEntity result;
        HttpPost httpPost = new HttpPost(this.endpointUrl.replaceFirst("[^/]*$", "query"));
        httpPost.setHeader("Accept", mimeType);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        // should sanitise the UUID, to make sure...
        nvps.add(new BasicNameValuePair("query", query));
        int code = 500;
        CloseableHttpResponse response = null;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            response = httpclient.execute(httpPost);
            result = response.getEntity();
            code = response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
           // tearDown(httpclient, response);
        }
        return result;
    }

    private int doUpdate(String query) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(this.endpointUrl);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        // should sanitise the UUID, to make sure...
        nvps.add(new BasicNameValuePair("update", query));
        int code = 500;
        CloseableHttpResponse response = null;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            response = httpclient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            code = response.getStatusLine().getStatusCode();
            EntityUtils.consume(entity);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            tearDown(httpclient, response);
        }
        return code;
    }

    private void tearDown(CloseableHttpClient client, CloseableHttpResponse response) {
        try {
            response.close();
        } catch (IOException e) {
            log.warn("Failed to close HTTP response.");
        }
        try {
            client.close();
        } catch (IOException e) {
            log.warn("Failed to close HTTP client.");
        }
    }
}
