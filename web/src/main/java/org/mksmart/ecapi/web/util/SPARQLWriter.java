package org.mksmart.ecapi.web.util;

import org.apache.http.client.methods.CloseableHttpResponse; 
import org.apache.http.client.methods.HttpPost; 
import org.apache.http.impl.client.CloseableHttpClient; 
import org.apache.http.message.BasicNameValuePair; 
import org.apache.http.util.EntityUtils; 
import org.apache.http.HttpEntity; 
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.HttpClients;

import java.util.List;
import java.util.ArrayList;

/**
 * send NT to a SPARQL Update endpoint
 * 
 * @author Mathieu d'Aquin <mathieu.daquin.w@gmail.com>
 * 
 */
public class SPARQLWriter {
    
    private String endpointurl = null;

    public SPARQLWriter(String eurl) {
	this.endpointurl = eurl;
    }

    public int write(String NT, String graph){
	CloseableHttpClient httpclient = HttpClients.createDefault();
	HttpPost httpPost = new HttpPost(this.endpointurl);
	List <NameValuePair> nvps = new ArrayList <NameValuePair>();
	// should sanatize the UUID, to make sure...
	nvps.add(new BasicNameValuePair("update", "insert data {graph <"+graph+"> {"+NT+"}}"));
	int code = 500;
	CloseableHttpResponse response2 = null;
	try {
	    httpPost.setEntity(new UrlEncodedFormEntity(nvps));
	    response2 = httpclient.execute(httpPost);
	    HttpEntity entity2 = response2.getEntity();
	    code = response2.getStatusLine().getStatusCode();
	    EntityUtils.consume(entity2);
	    response2.close();
	} catch (Exception e){
	    e.printStackTrace();
	}
	return code;
    }
}
