package org.mksmart.ecapi.web.resources;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.json.JSONException;
import org.json.JSONObject;
import org.mksmart.ecapi.access.ApiKeyDriver;
import org.mksmart.ecapi.api.id.IdGenerator;
import org.mksmart.ecapi.web.JsonMessageFactory;
import org.mksmart.ecapi.web.util.SPARQLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles actions related to datasets
 * 
 * @author Mathieu d'Aquin <mathieu.daquin.w@gmail.com>
 * 
 */
@Path("/dataset")
public class DatasetResource extends BaseResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Context
    ServletContext servletContext;

    @PUT
    @Path("{uuid}")
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response create(@PathParam("uuid") String uuid,
                           @QueryParam("api_key") String key,
                           @QueryParam("key") String oldKey,
                           @Context HttpHeaders headers,
                           @Context HttpServletRequest request) {
        ResponseBuilder rb;
        key = selectKey(key, oldKey);
        if (key != null && writeAuthorised(key, uuid, headers, request)) {
            SPARQLWriter writer = (SPARQLWriter) servletContext.getAttribute(SPARQLWriter.class.getName());
            int code = writer.createGraph("urn:dataset/" + uuid + "/graph");
            if (code == 200) {
                JSONObject o = new JSONObject().put("status", "written to " + uuid);
                rb = Response.ok((JSONObject) o);
            } else rb = Response.status(code);
        } else rb = Response.status(FORBIDDEN);
        handleCors(rb, "GET", "POST", "PUT");
        return rb.build();
    }

    @GET
    @Path("{uuid}")
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response getInfo(@PathParam("uuid") String uuid,
                            @QueryParam("api_key") String key,
                            @QueryParam("key") String oldKey,
                            @Context HttpHeaders headers,
                            @Context HttpServletRequest request) {
        ResponseBuilder rb;
        key = selectKey(key, oldKey);
        SPARQLWriter writer = (SPARQLWriter) servletContext.getAttribute(SPARQLWriter.class.getName());
        boolean exi = writer.exists("urn:dataset/" + uuid + "/graph");
        JSONObject json = new JSONObject();
        json.put("requested", uuid);
        json.put("found", exi);
        if (exi) {
            json.put(
                "comment",
                "Dataset exists but retrieving raw data from it is not implemented yet."
                        + " Entity-specific data can be retrieved by configuring the entity API to work with this dataset.");
            rb = Response.ok(json);
        } else rb = Response.status(NOT_FOUND).entity(json);
        handleCors(rb, "GET", "POST", "PUT");
        return rb.build();
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @GET
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response getSignOfLife(@Context HttpHeaders headers) {
        ResponseBuilder rb;
        Set<String> datasets = handleCredentials(headers, false);
        try {
            rb = Response.ok(JsonMessageFactory.alive(datasets));
        } catch (JSONException e) {
            rb = Response.serverError();
        }
        handleCors(rb);
        return rb.build();
    }

    @POST
    @Path("{uuid}/grant")
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response grant(@PathParam("uuid") String uuid,
                          @QueryParam("api_key") String key,
                          @QueryParam("key") String oldKey,
                          @FormParam("right") String right,
                          @FormParam("ukey") String ukey) {
        ResponseBuilder rb = null;
        key = selectKey(key, oldKey);
        if (key == null) rb = Response.status(FORBIDDEN);
        else if (right == null) rb = Response.status(BAD_REQUEST);
        else if (!right.equals("write") && !right.equals("read") && !right.equals("grant")) rb = Response
                .status(BAD_REQUEST);
        else {
            ApiKeyDriver keyDrv = (ApiKeyDriver) servletContext.getAttribute(ApiKeyDriver.class.getName());
            boolean res = false;
            if (right.equals("write")) {
                // if you can write, you can read
                res = keyDrv.grant(key, ukey, uuid, ApiKeyDriver.WRITE_RIGHT);
                if (res) res = keyDrv.grant(key, ukey, uuid, ApiKeyDriver.READ_RIGHT);
            }
            if (right.equals("read")) {
                res = keyDrv.grant(key, ukey, uuid, ApiKeyDriver.READ_RIGHT);
            }
            if (right.equals("grant")) {
                res = keyDrv.grant(key, ukey, uuid, ApiKeyDriver.GRANT_RIGHT);
            }
            if (!res) rb = Response.status(FORBIDDEN);
            else {
                JSONObject o = new JSONObject().put("status:", "rights updated");
                rb = Response.ok((JSONObject) o);
            }
        }
        return rb.build();
    }

    @OPTIONS
    public Response options(@Context HttpHeaders headers, @Context UriInfo uriInfo) {
        ResponseBuilder rb = Response.ok();
        handleCors(rb);
        return rb.build();
    }

    @OPTIONS
    @Path("{uuid}")
    public Response optionsType(@PathParam("uuid") String uuid,
                                @QueryParam("api_key") String key,
                                @QueryParam("key") String oldKey,
                                @Context HttpHeaders headers,
                                @Context HttpServletRequest request) {
        ResponseBuilder rb = Response.ok();
        key = selectKey(key, oldKey);
        handleCors(rb, "GET", "POST", "PUT");
        return rb.build();
    }

    // TODO:
    // Add a new endpoint grant/{uuid}
    // Check with A why DSS is ignored when using key above
    @POST
    @Path("{uuid}")
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response write(@PathParam("uuid") String uuid,
                          @QueryParam("api_key") String key,
                          @QueryParam("key") String oldKey,
                          @FormParam("data") String data,
                          @FormParam("rdf") String rdf,
                          @Context HttpHeaders headers,
                          @Context HttpServletRequest request) {
        ResponseBuilder rb;
        key = selectKey(key, oldKey);
        if (key == null || !writeAuthorised(key, uuid, headers, request)) {
            rb = Response.status(FORBIDDEN);
            handleCors(rb, "GET", "POST", "PUT");
            return rb.build();
        } else if (rdf != null) {
            if (data == null) data = rdf;
            else {
                rb = Response
                        .status(BAD_REQUEST)
                        .entity(
                            JsonMessageFactory
                                    .badRequest("You can interchangeably use form params 'data' or 'rdf' but not both"));
                handleCors(rb, "GET", "POST", "PUT");
                return rb.build();
            }
        }
        if (data == null) rb = Response
                .status(BAD_REQUEST)
                .entity(
                    JsonMessageFactory
                            .badRequest("Form param 'data' is required, and you do not seem to have used the deprecated 'rdf' parameter."));
        SPARQLWriter writer = (SPARQLWriter) servletContext.getAttribute(SPARQLWriter.class.getName());
        // FIXME THIS MUST NOT BE HARDCODED !!!!!!!!!!!!!!!!!!!!
        int code = writer.write(data, "urn:dataset:" + uuid + ":graph");
        if (code < 200 || code >= 400) rb = Response.status(code).entity(
            JsonMessageFactory.response(code, "Writer returned erroneous HTTP code " + code));
        else {
            JSONObject o = new JSONObject().put("status:", "written to " + uuid);
            rb = Response.ok((JSONObject) o);
        }
        handleCors(rb, "GET", "POST", "PUT");
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

    protected String selectKey(String newer, String older) {
        if (newer != null && !newer.isEmpty()) {
            if (older != null && !older.isEmpty()) log
                    .warn("API key {} was supplied using both the old query param and the new one. They should not be used together.");
            return newer;
        }
        return older;
    }

    protected boolean writeAuthorised(String key, String uuid, HttpHeaders headers, HttpServletRequest request) {
        ApiKeyDriver keyDrv = (ApiKeyDriver) servletContext.getAttribute(ApiKeyDriver.class.getName());
        boolean haz = keyDrv.hasRight(key, uuid, ApiKeyDriver.WRITE_RIGHT);
        log.debug("Checking if allowed to write to {}... {}", key, haz);
        return haz;
    }

}
