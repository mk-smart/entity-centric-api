package org.mksmart.ecapi.web.resources;

import java.net.URI;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.api.DebuggableEntityCompiler;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.provenance.PropertyPath;

import com.hp.hpl.jena.vocabulary.RDF;

@Path("/entity")
public class EntityResourceWithProvenance extends EntityResource {

    @GET
    @Path("{type: [\\w\\-\\._]+}/{id: .+}.prov")
    // @Produces(value = {MediaType.APPLICATION_JSON})
    @Produces("application/json; charset=UTF-8")
    public Response getProvenance(@PathParam("type") String tyype,
                                  @PathParam("id") String id,
                                  @DefaultValue("true") @QueryParam("global") boolean global,
                                  @DefaultValue("false") @QueryParam("debug") boolean debug,
                                  @Context HttpHeaders headers,
                                  @Context UriInfo uriInfo) {
        if (id == null) throw new WebApplicationException(Status.BAD_REQUEST);
        super.init(uriInfo, headers);
        ResponseBuilder rb;
        Set<String> dss = handleCredentials(headers, debug);
        if (dss != null) for (String ds : dss)
            log.info("Dataset {}", ds);
        id = tyype + '/' + id;
        DebuggableEntityCompiler compiler = (DebuggableEntityCompiler) servletContext
                .getAttribute(DebuggableEntityCompiler.class.getName());
        // Parse global URI first, also a security measure for unwanted accesses.
        GlobalURI gu = preprocessGlobalId(id);
        if (gu == null) rb = Response.status(Status.BAD_REQUEST);
        else { // No error status was assigned, so proceed
            Entity e = compiler.assembleEntity(gu, dss, debug);
            if (e == null) {
                rb = Response.status(Status.NOT_FOUND).entity("{ found: false }");
            } else {
                JSONObject mainObj = new JSONObject();
                JSONArray jProv = new JSONArray();
                mainObj.put("@id", gu);
                mainObj.put("description", "Provenance information for properties of entity <" + gu + ">");
                for (String ds : e.getProvenanceMap().keySet()) {
                    JSONObject jProvT = new JSONObject();
                    JSONArray jProps = new JSONArray();
                    jProvT.put("dataset", ds);
                    for (PropertyPath path : e.getContributedProperties(ds)) {
                        String spath = "";
                        for (URI prop : path) {
                            String s = prop.toString();
                            if (!("@types".equals(s) || RDF.type.getURI().equals(s))) {
                                if (!spath.isEmpty()) spath += "/";
                                spath += "<" + s + ">";
                            }
                        }
                        if (!spath.isEmpty()) jProps.put(spath);
                    }
                    jProvT.put("attributes", jProps);
                    jProv.put(jProvT);
                }
                mainObj.put("provenance", jProv);
                rb = Response.ok(mainObj);
            }
        }

        handleCors(rb);
        return rb.build();
    }
}
