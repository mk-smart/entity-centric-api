package org.mksmart.ecapi.web.resources;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import org.json.JSONObject;
import org.mksmart.ecapi.api.Catalogue;
import org.mksmart.ecapi.api.DebuggableEntityCompiler;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.id.IdGenerator;
import org.mksmart.ecapi.web.format.JsonProvenanceDataSerializer;
import org.mksmart.ecapi.web.util.UriRewriter;

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
        id = tyype + '/' + id; // not sure about keeping
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
                Map<URI,Entity> supportData = new HashMap<>();
                // Add dataset info
                Catalogue cat = compiler.getCatalogue();
                if (cat != null) {
                    List<URI> dids = new ArrayList<>();
                    for (String s : e.getProvenanceMap().keySet())
                        dids.add(URI.create(s));
                    Map<URI,String> uuids = cat.getUuids(dids.toArray(new URI[0]));
                    for (URI did : dids)
                        if (uuids.containsKey(did)) {
                            GlobalURI gdsid = preprocessGlobalId("dataset" + '/' + uuids.get(did));
                            supportData.put(did, compiler.assembleEntity(gdsid, dss));
                        }
                }

                @SuppressWarnings("rawtypes")
                UriRewriter rewriter = global ? new UriRewriter(
                        (IdGenerator) servletContext.getAttribute(IdGenerator.class.getName())) : null;
                JSONObject serial = new JsonProvenanceDataSerializer(supportData).serialize(e, gu, rewriter,
                    selectNamespace(uriInfo, headers, false));
                rb = Response.ok(serial);
            }
        }
        handleCors(rb);
        return rb.build();
    }
}
