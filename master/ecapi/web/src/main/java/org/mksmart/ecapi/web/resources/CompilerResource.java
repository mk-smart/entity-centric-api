package org.mksmart.ecapi.web.resources;

import java.net.URI;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.json.JSONException;
import org.json.JSONObject;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.EntityCompiler;
import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.id.IdGenerator;
import org.mksmart.ecapi.api.id.ScopedGlobalURI;
import org.mksmart.ecapi.api.storage.EntityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RESTful resource for generating compiled representations of entities given one of their local IDs, i.e.
 * possible ways of identifying them in datasets.
 * 
 * This resource is non-normative, i.e. not part of the project specification, but can be used as an auxiliary
 * tool to infer how local URIs are mapped to global ones.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
@Path("/compiler")
public class CompilerResource extends BaseResource {

    /**
     * If this resource can be accessed, then the service should be pretty much alive...
     */
    private JSONObject liveMsg = new JSONObject();

    private Logger log = LoggerFactory.getLogger(getClass());

    @Context
    ServletContext servletContext;

    @GET
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response getMessage(@QueryParam("id") String id, @Context HttpHeaders headers) {
        long before = System.currentTimeMillis();
        ResponseBuilder rb;
        if (id == null) {
            try {
                liveMsg.put("live", true);
                liveMsg.put("comment", "Beware: I live!");
                rb = Response.ok(liveMsg);
                handleCors(rb);
            } catch (JSONException e) {
                rb = Response.serverError();
            }
        } else {
            // Get infrastructure from context
            @SuppressWarnings({"unchecked", "rawtypes"})
            EntityStore<URI> store = (EntityStore) servletContext.getAttribute(EntityStore.class.getName());
            EntityCompiler ec = (EntityCompiler) servletContext.getAttribute(EntityCompiler.class.getName());
            @SuppressWarnings({"unchecked", "rawtypes"})
            IdGenerator<ScopedGlobalURI,Entity> idgen = (IdGenerator) servletContext
                    .getAttribute(IdGenerator.class.getName());

            // First try to get it by local URI
            URI eid = URI.create(id);
            log.debug("Now fetching cached entity <{}>", id);
            Entity e = store.retrieve(eid);
            if (e == null) {
                // // Try to get the canonicalised cached entity.
                // String canid = "thing/www:uri/" + id.replace("http://", "");
                // e = store.retrieve(URI.create(canid));
                // log.debug("Global URI is <{}>", canid);
                // if (e != null) rb = Response.seeOther(URI.create("/entity/" + canid));
                // else {
                e = ec.compileEntity(eid); // Do compilation
                store.store(e, URI.create(/* idd.toString() */id));
                // }
            }
            if (e != null) {
                try {
                    GlobalURI idd = idgen.createId(e);
                    log.debug("Global URI is <{}>", idd);
                    // Redirect XXX other response?
                    rb = Response.seeOther(URI.create("/entity/" + idd));
                    handleCors(rb);
                } catch (Exception ex) {
                    // Reply 501 Not implemented
                    JSONObject errMsg = new JSONObject();
                    errMsg.put("status", "501 Not Implemented");
                    errMsg.put("localID", id);
                    errMsg.put("comment", "Local URIs of the provided form are not supported.");
                    rb = Response.status(501).entity(errMsg);
                }
            } else rb = Response.status(Status.NOT_FOUND);

        }
        log.debug("Total service turnaround time: {} ms", System.currentTimeMillis() - before);
        return rb.build();
    }

    @OPTIONS
    public Response getOptionsType(@QueryParam("id") String id,
                                   @Context HttpHeaders headers,
                                   @Context UriInfo uriInfo) {
        ResponseBuilder rb;
        rb = Response.ok();
        handleCors(rb);
        return rb.build();
    }

}