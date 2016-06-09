package org.mksmart.ecapi.web.resources;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mksmart.ecapi.access.ApiKeyDriver;
import org.mksmart.ecapi.api.AssemblyProvider;
import org.mksmart.ecapi.api.DebuggableEntityCompiler;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.GlobalType;
import org.mksmart.ecapi.api.TypeSupport;
import org.mksmart.ecapi.api.id.CanonicalGlobalURI;
import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.id.IdGenerator;
import org.mksmart.ecapi.api.id.ScopedGlobalURI;
import org.mksmart.ecapi.impl.GlobalTypeImpl;
import org.mksmart.ecapi.impl.format.JsonSimplifiedGenericRepresentationSerializer;
import org.mksmart.ecapi.web.util.UriRewriter;
import org.mksmart.ecapi.web.util.SPARQLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Handles actions related to datasets
 * 
 * @author Mathieu d'Aquin <mathieu.daquin.w@gmail.com>
 * 
 */
@Path("/dataset")
public class DatasetResource extends BaseResource {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Context
    ServletContext servletContext;

    // TODO: 
    //       Add a new endpoint grant/{uuid}
    //       Check with A why DSS is ignored when using key above
    @POST
    @Path("{uuid}")
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response write(@PathParam("uuid") String uuid,
			  @QueryParam("key") String key,
			  @FormParam("data") String data, 
			  @Context HttpHeaders headers,
			  @Context HttpServletRequest request) {
	ResponseBuilder rb = null;
	if (key == null || !writeAuthorised(key, uuid, headers, request)){
	    rb = Response.status(Response.Status.FORBIDDEN);
	    return rb.build();
	}
	SPARQLWriter writer = (SPARQLWriter) servletContext.getAttribute(SPARQLWriter.class.getName());
	int code = writer.write(data, "urn:dataset/"+uuid+"/graph");
	if (code != 200) { 
	    rb = Response.status(code); 
	}
	else {
	    JSONObject o = new JSONObject().put("status:", "written to "+uuid+"\n");
	    rb = Response.ok((JSONObject) o);
	}
	return rb.build();
    }

    protected boolean writeAuthorised(String key, String uuid, HttpHeaders headers, HttpServletRequest request){
        ApiKeyDriver keyDrv = (ApiKeyDriver) servletContext.getAttribute(ApiKeyDriver.class.getName());	
	return keyDrv.hasRight(key, uuid, ApiKeyDriver.WRITE_RIGHT);
   } 

    @POST
    @Path("{uuid}/grant")
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response grant(@PathParam("uuid") String uuid,
			  @QueryParam("key") String key,
			  @FormParam("right") String right, 
			  @FormParam("ukey") String ukey, 
			  @Context HttpHeaders headers,
			  @Context HttpServletRequest request) {
	ResponseBuilder rb = null;
	if (key == null){
	    rb = Response.status(Response.Status.FORBIDDEN);
	    return rb.build();
	}
	// if ukey = null, should be public	
	if (right == null){
	    rb = Response.status(Response.Status.BAD_REQUEST);
	    return rb.build();
	}
	if (!right.equals("write") && !right.equals("read") && !right.equals("grant")){
	    rb = Response.status(Response.Status.BAD_REQUEST);
	    return rb.build();
	}
	ApiKeyDriver keyDrv = (ApiKeyDriver) servletContext.getAttribute(ApiKeyDriver.class.getName());	       
	boolean res = false;
	if (right.equals("write")){
	    // if you can write, you can read
	    res = keyDrv.grant(key, ukey, uuid, ApiKeyDriver.WRITE_RIGHT);
	    if (res) res = keyDrv.grant(key, ukey, uuid, ApiKeyDriver.READ_RIGHT);
	}
	if (right.equals("read")){	    
	    res = keyDrv.grant(key, ukey, uuid, ApiKeyDriver.READ_RIGHT);
	}
	if (right.equals("grant")){	    
	    res = keyDrv.grant(key, ukey, uuid, ApiKeyDriver.GRANT_RIGHT);
	}
	if (!res) rb = Response.status(Response.Status.FORBIDDEN);
	else {
	    JSONObject o = new JSONObject().put("status:", "rights updated");
	    rb = Response.ok((JSONObject) o);
	}
	return rb.build();
    }


    protected void init(UriInfo uriInfo, HttpHeaders headers) {
        log.trace("Request URI was {}", uriInfo.getRequestUri());
        log.trace("Base URI was {}", uriInfo.getBaseUri());
        log.trace("HTTP Headers follow");
        for (Entry<String,List<String>> head : headers.getRequestHeaders().entrySet()) {
            log.trace("{} : ", head.getKey());
            for (String hv : head.getValue())
                log.trace(" - {}", hv);
        }
        @SuppressWarnings("rawtypes")
        IdGenerator ig = (IdGenerator) servletContext.getAttribute(IdGenerator.class.getName());
        ig.refresh();
    }

}
