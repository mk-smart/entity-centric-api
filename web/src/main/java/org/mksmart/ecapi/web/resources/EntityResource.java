package org.mksmart.ecapi.web.resources;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mksmart.ecapi.api.AssemblyProvider;
import org.mksmart.ecapi.api.DebuggableEntityCompiler;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.GlobalType;
import org.mksmart.ecapi.api.TypeSupport;
import org.mksmart.ecapi.api.id.CanonicalGlobalURI;
import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.id.IdGenerator;
import org.mksmart.ecapi.api.id.ScopedGlobalURI;
import org.mksmart.ecapi.core.LaunchConfiguration;
import org.mksmart.ecapi.impl.GlobalTypeImpl;
import org.mksmart.ecapi.impl.format.JsonSimplifiedGenericRepresentationSerializer;
import org.mksmart.ecapi.web.Config;
import org.mksmart.ecapi.web.util.UriRewriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles compiled entities given their service-dependent global URIs.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
@Path("/entity")
public class EntityResource extends BaseResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Context
    ServletContext servletContext;

    @GET
    @Path("has/{id: .+}")
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response getPrimitiveInstance(@PathParam("id") String id,
                                         @DefaultValue("true") @QueryParam("global") boolean global,
                                         @DefaultValue("false") @QueryParam("debug") boolean debug,
                                         @Context HttpHeaders headers,
                                         @Context UriInfo uriInfo) {
        // Parse global URI first, also a security measure for unwanted accesses.
        if (id == null) throw new WebApplicationException(Status.BAD_REQUEST);
        this.init(uriInfo, headers);
        ResponseBuilder rb = null;
        Set<String> dss = handleCredentials(headers, debug);
        if (dss != null) for (String ds : dss)
            log.info("Dataset {}", ds);
        DebuggableEntityCompiler compiler = (DebuggableEntityCompiler) servletContext
                .getAttribute(DebuggableEntityCompiler.class.getName());
        GlobalURI gu;
        try {
            gu = ScopedGlobalURI.parse("has/" + id);
            log.info("Parsed scoped global URI as {} (absolute: {})", gu, gu.isRelative());
        } catch (Exception ex) {
            log.warn("Got request to handle unsupported URI pattern.");
            log.warn(" ... URI was <{}>", uriInfo.getRequestUri());
            gu = null;
            rb = Response.status(Status.BAD_REQUEST);
        }
        if (rb == null) { // No error status was assigned, so proceed
            compiler.assembleEntity(gu, dss, debug);
            // Map<URI,List<Query>> queries = computeResolution(gu, null); // TODO null means all open data
            // Map<URI,Entity> result = new HashMap<>();
            // if (!queries.isEmpty()) {
            // Map<URI,Entity> rtemp = DistributedQueries.executeEntities(queries);
            // if (rtemp != null) {
            // // -- BEGIN URI rewriting
            // if (!global) result = rtemp;
            // else { // Rewrite all local URIs if asked for
            // @SuppressWarnings("rawtypes")
            // UriRewriter rewriter = new UriRewriter(
            // (IdGenerator) servletContext.getAttribute(IdGenerator.class.getName()));
            // for (Entry<URI,Entity> entry : rtemp.entrySet()) {
            // Entity newe = rewriter.rewrite(entry.getValue(),
            // selectNamespace(uriInfo, headers, false));
            // RDFNode local = ResourceFactory.createResource(entry.getKey().toString());
            // local = rewriter.rewrite(local, uriInfo.getBaseUri().toString());
            // URI k = local.isURIResource() ? URI.create(local.asResource().getURI()) : URI
            // .create(entry.getKey().toString());
            // log.debug("key: {}", k);
            // result.put(k, newe);
            // }
            // }
            // // -- END URI rewriting
            // }
            // }
            // rb = Response.ok(JsonGenericRepresentationSerializer.serialize(result));
            rb = Response.status(Status.SERVICE_UNAVAILABLE).entity("This support is being re-implemented");
        }
        handleCors(rb);
        return rb.build();
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    /**
     * FIXME too much computation going on here
     *
     * @param id
     * @param global
     * @param headers
     * @param uriInfo
     * @return
     */
    @GET
    @Path("{type: [\\w\\-\\._]+}/{id: .+}")
    // @Produces(value = {MediaType.APPLICATION_JSON})
    @Produces("application/json; charset=UTF-8")
    public Response getSignature(@PathParam("type") String tyype,
                                 @PathParam("id") String id,
                                 @DefaultValue("true") @QueryParam("global") boolean global,
                                 @DefaultValue("false") @QueryParam("debug") boolean debug,
                                 @Context HttpHeaders headers,
                                 @Context UriInfo uriInfo) {
        if (id == null) throw new WebApplicationException(Status.BAD_REQUEST);
        this.init(uriInfo, headers);
        ResponseBuilder rb = null;
        Set<String> dss = handleCredentials(headers, debug);
        if (dss != null) for (String ds : dss)
            log.trace("Dataset {}", ds);
        id = tyype + '/' + id;
        DebuggableEntityCompiler compiler = (DebuggableEntityCompiler) servletContext
                .getAttribute(DebuggableEntityCompiler.class.getName());
        // Parse global URI first, also a security measure for unwanted accesses.
        GlobalURI gu = preprocessGlobalId(id);
        if (gu == null) rb = Response.status(Status.BAD_REQUEST);
        if (rb == null) { // No error status was assigned, so proceed
            Entity e = compiler.assembleEntity(gu, dss, debug);
            if (e == null) {
                rb = Response.status(Status.NOT_FOUND).entity("Not Found");
            } else {
                // The copy where URIs will be rewritten
                Entity e_rewr = global ? null : e;
                if (global) {
                    @SuppressWarnings("rawtypes")
                    UriRewriter rewriter = new UriRewriter(
                            (IdGenerator) servletContext.getAttribute(IdGenerator.class.getName()));
                    e_rewr = rewriter.rewrite(e, selectNamespace(uriInfo, headers, false));
                }
                JSONObject o = JsonSimplifiedGenericRepresentationSerializer.serialize(e_rewr, (URI) null);
                rb = Response.ok((JSONObject) o);
            }
        }
        handleCors(rb);
        return rb.build();
    }

    @GET
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response getSignOfLife(@Context HttpHeaders headers) {
        ResponseBuilder rb;
        JSONObject sol = new JSONObject();
        // TODO refer to the compiler instead
        AssemblyProvider<?> ep = (AssemblyProvider) servletContext.getAttribute(AssemblyProvider.class
                .getName());
        JSONArray types = new JSONArray();
        for (URI ut : ep.getSupportedTypes()) {
            String emp = ut.toString();
            emp = emp.substring(emp.lastIndexOf('/') + 1, emp.length());
            if (!"T".equals(emp)) types.put(emp);
        }
        try {
            sol.put("live", true);
            sol.put("subresources", types);
            sol.put("comment", "Beware: I live!");
            rb = Response.ok(sol);
        } catch (JSONException e) {
            rb = Response.serverError();
        }

        handleCors(rb);
        return rb.build();
    }

    @GET
    @Path("{type}")
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response getTypeInfo(@PathParam("type") String typename,
                                @DefaultValue("false") @QueryParam("debug") boolean debug,
                                @Context HttpHeaders headers,
                                @Context UriInfo uriInfo) {
        AssemblyProvider<?> ep = (AssemblyProvider) servletContext.getAttribute(AssemblyProvider.class
                .getName());
        DebuggableEntityCompiler compiler = (DebuggableEntityCompiler) servletContext
                .getAttribute(DebuggableEntityCompiler.class.getName());
        GlobalType ty = new GlobalTypeImpl(ScopedGlobalURI.parse("type/global:id/" + typename));
        ResponseBuilder rb = null;
        Set<String> dss = handleCredentials(headers, debug);
        if (dss != null) for (String ds : dss)
            log.info("Dataset {}", ds);
        JSONObject sol = new JSONObject();
        JSONArray aliases = new JSONArray();
        for (URI ut : ep.getTypeAliases(ty))
            aliases.put(ut);
        sol.put("short_name", typename);
        sol.put("aliases", aliases);
        sol.put("datasets", makeSupportJSON(typename, compiler, uriInfo.getRequestUri(), debug));

        JSONArray insts = new JSONArray();
        @SuppressWarnings("rawtypes")
        UriRewriter rewriter = new UriRewriter((IdGenerator) servletContext.getAttribute(IdGenerator.class
                .getName()));
        String ns = selectNamespace(uriInfo, headers, false);
        for (URI lu : compiler.getInstances(ty, debug))
            insts.put(rewriter.rewrite(lu, ns));
        sol.put("instances", insts);
        rb = Response.ok(sol);
        handleCors(rb);
        return rb.build();
    }

    @GET
    @Path("{type}")
    @Produces(value = {MediaType.TEXT_HTML})
    public Response getTypeInfoPrint(@PathParam("type") String typename,
                                     @DefaultValue("false") @QueryParam("debug") boolean debug,
                                     @Context HttpHeaders headers,
                                     @Context UriInfo uriInfo) {
        ResponseBuilder rb;
        Set<String> dss = handleCredentials(headers, debug);
        if (dss != null) for (String ds : dss)
            log.info("Dataset {}", ds);
        DebuggableEntityCompiler ec = (DebuggableEntityCompiler) servletContext
                .getAttribute(DebuggableEntityCompiler.class.getName());
        // FIXME I WILL replace this with a templating engine like Freemarker or Velocity, even if it's the
        // last thing I do.
        StringBuilder widget = new StringBuilder();
        widget.append("<h3>Stub for the HTML view of type information</h3>");

        JSONObject support = makeSupportJSON(typename, ec, uriInfo.getRequestUri(), debug);
        for (Iterator<?> it = support.keys(); it.hasNext();) {
            widget.append("<div>");
            String ds = it.next().toString();
            widget.append("Dataset <tt>" + ds + "</tt>");
            if (support.getJSONObject(ds).has("example_instances")) {
                JSONArray insts = support.getJSONObject(ds).getJSONArray("example_instances");
                if (insts.length() > 0) {
                    widget.append("<ul>Example resources:");
                    for (int i = 0; i < insts.length(); i++)
                        widget.append("<li><a href=\"" + insts.getString(i) + "\">" + insts.getString(i)
                                      + "</a></li>");

                    widget.append("</ul>");
                }
            }
            widget.append("</div>");
        }

        GlobalType ty = new GlobalTypeImpl(ScopedGlobalURI.parse("type/global:id/" + typename));
        @SuppressWarnings("rawtypes")
        UriRewriter rewriter = new UriRewriter((IdGenerator) servletContext.getAttribute(IdGenerator.class
                .getName()));
        widget.append("<h4>Pre-fetched instances</h4>");
        widget.append("<ul>");
        String ns = selectNamespace(uriInfo, headers, false);
        for (URI lu : ec.getInstances(ty, debug)) {
            String rew = rewriter.rewrite(lu, ns);
            widget.append("<li style=\"display:inline-block; padding-right:15px\"><a href=\"" + rew + "\">"
                          + rew.substring(rew.lastIndexOf('/') + 1) + "</a></li>");
        }
        widget.append("</ul>");
        widget.append("<p>This will look shiny once we introduce a templating engine like freemarker.</p>");
        rb = Response.ok(widget.toString());
        handleCors(rb);
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

    /**
     * Returns a JSON manifest of an entity type.
     * 
     * @param typeShortName
     * @param compiler
     * @param requestURI
     * @return
     */
    protected JSONObject makeSupportJSON(String typeShortName,
                                         DebuggableEntityCompiler compiler,
                                         URI requestURI,
                                         boolean debug) {
        JSONObject dsSupport = new JSONObject();
        GlobalType ty = new GlobalTypeImpl(ScopedGlobalURI.parse("type/global:id/" + typeShortName));
        String base = requestURI.toString();
        while (base.endsWith("/"))
            base = base.substring(0, base.length() - 1);
        base = base.substring(0, base.lastIndexOf('/'));
        TypeSupport ts = compiler.getTypeSupport(ty, debug);
        for (URI datasetId : ts.getDatasets()) {
            JSONObject dso = new JSONObject();
            JSONArray instances = new JSONArray();
            for (GlobalURI inst : ts.getExampleInstances(datasetId))
                instances.put(base + '/' + inst.toString());
            if (instances.length() > 0) {
                dso.put("example_instances", instances);
                dsSupport.put(datasetId.toString(), dso);
            }
        }
        return dsSupport;
    }

    @OPTIONS
    public Response options(@Context HttpHeaders headers, @Context UriInfo uriInfo) {
        ResponseBuilder rb = Response.ok();
        handleCors(rb);
        return rb.build();
    }

    @OPTIONS
    @Path("has/{id: .+}")
    public Response optionsPrimitiveInstance(@PathParam("id") String id,
                                             @Context HttpHeaders headers,
                                             @Context UriInfo uriInfo) {
        ResponseBuilder rb = Response.ok();
        handleCors(rb);
        return rb.build();
    }

    @OPTIONS
    @Path("{type}")
    public Response optionsType(@PathParam("type") String tyype,
                                @Context HttpHeaders headers,
                                @Context UriInfo uriInfo) {
        ResponseBuilder rb = Response.ok();
        handleCors(rb);
        return rb.build();
    }

    @OPTIONS
    @Path("{type: [\\w\\-\\._]+}/{id: .+}")
    public Response optionsTypeId(@PathParam("type") String tyype,
                                  @PathParam("id") String id,
                                  @Context HttpHeaders headers,
                                  @Context UriInfo uriInfo) {
        ResponseBuilder rb = Response.ok();
        handleCors(rb);
        return rb.build();
    }

    protected GlobalURI preprocessGlobalId(String id) {
        GlobalURI gu;
        try {
            gu = ScopedGlobalURI.parse(id);
            log.debug("Parsed scoped global URI as {} (absolute: {})", gu, !gu.isRelative());
            return gu;
        } catch (Exception ex) {
            try {
                gu = CanonicalGlobalURI.parse(id);
                log.debug("Parsed canonical global URI as {} (absolute: {})", gu, !gu.isRelative());
                return gu;
            } catch (Exception ex2) {
                return null;
            }
        }
    }

    protected String selectNamespace(UriInfo uriInfo, HttpHeaders headers, boolean https) {
        String ns;
        if (headers.getRequestHeader("X-Forwarded-Server") != null) {
            ns = (https ? "https" : "http") + "://"
                 + headers.getRequestHeader("X-Forwarded-Server").iterator().next();
            log.trace(" ... using header X-Forwarded-Server (value: <{}>)", ns);
        } else {
            ns = uriInfo.getBaseUri().toString();
            log.trace(" ... falling back to base URI <{}>", ns);
        }
        if (!ns.endsWith("/")) ns += '/';
        LaunchConfiguration mainConf = LaunchConfiguration.getInstance();
        if (mainConf.has(Config.WEB_PATH_PREFIX)) {
            String ppath = mainConf.get(Config.WEB_PATH_PREFIX).toString().trim();
            if (!ppath.isEmpty()) {
                while (ppath.startsWith("/"))
                    ppath = ppath.substring(1);
                while (ppath.endsWith("/"))
                    ppath = ppath.substring(0, ppath.length() - 1);
                if (!ppath.trim().isEmpty()) ns += ppath + '/';
            }
        }
        log.debug("<== DONE. Selected namespace is <{}>", ns);
        return ns;
    }

}
