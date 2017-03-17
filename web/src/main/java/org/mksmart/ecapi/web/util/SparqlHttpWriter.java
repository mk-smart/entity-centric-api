package org.mksmart.ecapi.web.util;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparqlHttpWriter extends SPARQLWriter {

    private Logger log = LoggerFactory.getLogger(getClass());

    protected String dataEndpoint;

    /**
     * 
     * @param updateEndpoint
     *            the URL where SPARQL UPDATE requests should be sent.
     * @param dataEndpoint
     *            the URL where data requests should be sent.
     */
    public SparqlHttpWriter(String updateEndpoint, String dataEndpoint) {
        super(updateEndpoint);
        this.dataEndpoint = dataEndpoint;
    }

    @Override
    public int write(String NT, String graph) {
        // curl -X PUT -H "Content-type:text/plain" -T alex.nt
        // -G http://localhost:3030/afel/data --data-urlencode
        // graph=http://diocane.org/pippobaudo
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(dataEndpoint + "?graph=" + graph);
        httpPost.setHeader("Content-type", "text/plain");
        log.trace("RDF being POSTed follows:");
        log.trace(NT);
        httpPost.setEntity(new StringEntity(NT, "UTF-8"));
        int code = 500;
        CloseableHttpResponse response = null;
        try {
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
}
