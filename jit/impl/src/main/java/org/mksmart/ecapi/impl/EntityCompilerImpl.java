package org.mksmart.ecapi.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.lang3.NotImplementedException;
import org.mksmart.ecapi.api.AssemblyProvider;
import org.mksmart.ecapi.api.Catalogue;
import org.mksmart.ecapi.api.DebuggableAssemblyProvider;
import org.mksmart.ecapi.api.DebuggableEntityCompiler;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.EntityCompiler;
import org.mksmart.ecapi.api.EntityFragment;
import org.mksmart.ecapi.api.GlobalType;
import org.mksmart.ecapi.api.TypeSupport;
import org.mksmart.ecapi.api.generic.AttributeEvent;
import org.mksmart.ecapi.api.generic.AttributeListener;
import org.mksmart.ecapi.api.generic.NotReconfigurableException;
import org.mksmart.ecapi.api.id.CanonicalGlobalURI;
import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.id.ScopedGlobalURI;
import org.mksmart.ecapi.api.provenance.PropertyPath;
import org.mksmart.ecapi.api.query.Query;
import org.mksmart.ecapi.api.query.TargetedQuery;
import org.mksmart.ecapi.api.storage.BulkStore;
import org.mksmart.ecapi.api.storage.Cache;
import org.mksmart.ecapi.api.storage.Store;
import org.mksmart.ecapi.impl.query.DistributedQueries;
import org.mksmart.ecapi.impl.query.SingletonQuerier;
import org.mksmart.ecapi.impl.resolve.DereferenceJob;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Reference implementation of {@link EntityCompiler}.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class EntityCompilerImpl implements DebuggableEntityCompiler {

    private Cache cache;

    private Catalogue catalogue;

    private Logger log = LoggerFactory.getLogger(getClass());

    private Set<AssemblyProvider> providers = new HashSet<>();

    private int quota = 1;

    private Set<Store<?,?>> stores = new HashSet<>();

    /**
     * TODO replace cache and stores with a single aggregated storage structure.
     * 
     * @param provider
     * @param catalogue
     * @param cache
     * @param stores
     */
    public EntityCompilerImpl(AssemblyProvider provider,
                              Catalogue catalogue,
                              Cache cache,
                              Store<?,?>... stores) {
        try {
            this.addEntityProvider(provider);
        } catch (NotReconfigurableException e) {
            String msg = "A NotReconfigurableException was caught while the object was being initialized. This should not happen.";
            log.error(msg, e);
            throw new RuntimeException(e);
        }
        this.catalogue = catalogue;
        this.cache = cache;
        for (Store<?,?> st : stores)
            this.stores.add(st);
    }

    public EntityCompilerImpl(AssemblyProvider provider, Catalogue catalogue, Store<?,?>... stores) {
        this(provider, catalogue, null, stores);
    }

    @Override
    public void addEntityProvider(AssemblyProvider provider) throws NotReconfigurableException {
        if (this.providers.size() == quota) throw new NotReconfigurableException(
                "An entity provider set is already set for this compiler and cannot be reset.");
        this.providers.add(provider);
    }

    @Override
    public Entity assembleEntity(GlobalURI gid, Set<String> datasets) {
        return assembleEntity(gid, datasets, false);
    }

    @Override
    public Entity assembleEntity(GlobalURI gid, Set<String> datasets, boolean debug) {
        // At this stage, datasets could be null, which means "open data".
        Entity e = new EntityImpl(), ecached = new EntityImpl();
        log.debug("Resolving global URI {}", gid);
        if (gid instanceof CanonicalGlobalURI) log.trace(" ... entity type: {}",
            ((CanonicalGlobalURI) gid).getEntityType());
        if (gid instanceof ScopedGlobalURI) {
            ScopedGlobalURI t = (ScopedGlobalURI) gid;
            log.trace(" ... identified BY: {}", t.getIdentifierRealm());
            log.trace(" ... identified USING: {}", t.getIdentifyingProperty());
        }
        if (gid instanceof CanonicalGlobalURI) log.trace(" ... identifier: {}",
            ((CanonicalGlobalURI) gid).getIdentifer());

        Map<URI,List<Query>> queries = new HashMap<>();
        for (AssemblyProvider<?> ap : providers) {
            log.debug("Requesting assembly from provider of type {}", ap.getClass());
            Map<URI,List<Query>> tqueries;
            if (ap instanceof DebuggableAssemblyProvider) tqueries = ((DebuggableAssemblyProvider) ap)
                    .getQueryMap(gid, datasets, debug);
            else tqueries = ap.getQueryMap(gid, datasets);
            if (tqueries.isEmpty()) log.warn("No queries to be performed on URI <{}>.", gid);
            for (Entry<URI,List<Query>> entry : tqueries.entrySet()) {
                if (!queries.containsKey(entry.getKey())) queries.put(entry.getKey(), new ArrayList<Query>());
                queries.get(entry.getKey()).addAll(entry.getValue());
            }
        }

        Set<TargetedQuery> hits = new HashSet<>();
        if (cache != null) {
            log.debug("Validating cache...");
            hits = cache.getCacheHits(queries);
            log.debug(" ... got {} cache hits", hits.size());
        }

        // Scan the query plan again and look for matches with the cache hits
        Map<URI,List<Query>> filtered = new HashMap<>();
        for (Entry<URI,List<Query>> entry : queries.entrySet()) {
            log.debug("Size of query map for <{}> :", entry.getKey());
            filtered.put(entry.getKey(), entry.getValue());
            log.debug(" .. before counting cache hits = {}", entry.getValue().size());
            filtered.get(entry.getKey()).removeAll(hits);
            log.debug(" .. after = {}", entry.getValue().size());
        }
        // Get the data from the cache hits
        getDataFromCacheHitsBulk(hits, ecached);
        // for (TargetedQuery tq : hits) {
        // Object o = null;
        // for (Iterator<Store<?,?>> it = stores.iterator(); it.hasNext() && o == null;) {
        // Store<?,?> st = it.next();
        // if (st.getSupportedKeyType() == TargetedQuery.class) {
        // try {
        // o = ((Store<TargetedQuery,?>) st).retrieve(tq);
        // } catch (Exception ex) {
        // log.error("Retrieval failed.", ex);
        // }
        // if (o != null) {
        // log.trace("Cache hit.");
        // log.trace(" ... is a {}", o.getClass().getCanonicalName());
        // if (!(o instanceof EntityFragment)) throw new IllegalStateException(
        // "Unexpected type " + o.getClass().getCanonicalName()
        // + " for an entity fragment. Supported types are "
        // + EntityFragment.class.getCanonicalName() + " and "
        // + Entity.class.getCanonicalName());
        // EntityFragment ef = (EntityFragment) o;
        // long before = System.currentTimeMillis();
        // reconstructProvenance(ef, ef, tq.getTarget().toString(), new PropertyPath(),
        // new HashSet<EntityFragment>());
        // log.debug("Provenance computation overhead = {} ms", System.currentTimeMillis()
        // - before);
        // if (ef instanceof Entity) Util.merge((Entity) ef, ecached);
        // else Util.merge(ef, ecached);
        // } else log.trace("Cache miss.");
        // }
        // }
        //
        // }

        // Query execution
        SingletonQuerier querier = SingletonQuerier.getInstance();
        // TODO will no longer be like this once we have the multithreaded API.
        synchronized (querier) {
            querier.addProvenanceListener(e);
            if (!queries.isEmpty()) for (Entry<URI,List<Query>> qtable : queries.entrySet())
                for (Query q : qtable.getValue()) {
                    long before = System.currentTimeMillis();
                    // They will all be targeted queries, eventually
                    TargetedQuery tq = q instanceof TargetedQuery ? (TargetedQuery) q : q.wrap(qtable
                            .getKey());
                    // a separate fragment, but OVERRIDE ENDPOINT
                    log.info("query target : <{}>", tq.getTarget());
                    EntityFragment partial = SingletonQuerier.getInstance().executeQuery(tq, qtable.getKey());
                    log.info(" ... turnaround time: {} ms", System.currentTimeMillis() - before);
                    for (Store<?,?> st : stores) {
                        if (st.getSupportedValueType() == EntityFragment.class
                            && st.getSupportedKeyType() == TargetedQuery.class) {
                            log.debug("Will now cache to store of type {}", st.getClass());
                            ((Store<TargetedQuery,EntityFragment>) st).store(partial, tq);
                            break;
                        }
                    }
                    Util.merge(partial, e);
                }
            querier.clearProvenanceListeners();
        }
        Util.merge(ecached, e);
        return e;
    }

    @Override
    public Entity compileEntity(URI localId) {
        long before = System.currentTimeMillis();
        log.info("Got request to compile entity.");
        log.info("ID : {}", localId);

        if (providers == null || providers.isEmpty()) log
                .warn("No AssemblyProvider is set. Some compilation strategies will not be available.");

        final Entity representation = new EntityImpl();
        representation.addAlias(localId);

        // TODO implement index
        log.warn("No index/caching mechanisms found. Will perform live compilation.");

        // Attempt resolution: values from this object are trusted blindly
        log.info("Attempt 1: dereference local ID (not synchronized).");

        // Type to properties
        final Map<URI,Set<URI>> typeSupport = new HashMap<>();

        DereferenceJob j1 = new DereferenceJob(localId, representation);
        j1.addListener(new AttributeListener() {
            @Override
            public void attributeSet(AttributeEvent event) {
                URI property = event.getProperty();
                RDFNode val = event.getValue();
                log.trace(event.getEntityId() + " : ADD <{}> <{}>", property, val);
                representation.addValue(property, val);
                // Check if this is a unifier, for what properties and with what
                // transform
                if (providers != null) for (AssemblyProvider<?> provider : providers)
                    try {
                        for (URI tt : provider.getCandidateTypes(property)) {
                            if (!typeSupport.containsKey(tt)) typeSupport.put(tt, new HashSet<URI>());
                            typeSupport.get(tt).add(property);
                        }
                    } catch (NotImplementedException ex) {} catch (Exception ex) {
                        log.warn("Candidate type check failed on provider {} (Reason: {} - {}). Skipping.",
                            new Object[] {provider, ex.getClass(), ex.getMessage()});
                    }
            }
        });

        Thread tDref = new Thread(j1);
        tDref.start();

        synchronized (representation) {
            try {
                representation.wait(5000);
                log.debug("Dereference thread came back");
            } catch (InterruptedException e) {
                log.warn("Idle wait was abruptly interrupted", e);
            }
        }

        log.debug("State of dereference thread : {}.", tDref.getState());

        int maxSupport = 0;
        Set<URI> typez = new HashSet<>();
        // Query datasets for unifying attributes
        for (URI ty : typeSupport.keySet()) {
            Set<URI> pts = typeSupport.get(ty);
            if (pts.size() > maxSupport) maxSupport = pts.size();
            log.debug("support of type <{}> : {}", ty, pts.size());
        }
        for (URI ty : typeSupport.keySet())
            if (typeSupport.get(ty).size() == maxSupport) {
                log.info("Will attempt compilation using type <{}>", ty);
                typez.add(ty);
            }

        if (providers != null) for (AssemblyProvider provider : providers) {

            // Execute microcompilers

            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("JavaScript");

            for (URI type : typez) {

                // Look for a match
                Map<String,Map<String,String>> unimap = null;

                for (Iterator<Set<URI>> it = provider.getUnifiers(type).iterator(); it.hasNext();) {
                    boolean complete = true;
                    // must have a value for every unifying property
                    // TODO use entire representation
                    unimap = new HashMap<>();
                    log.trace("New unifier group");
                    for (URI uProp : it.next()) {
                        log.trace(" * {}", uProp);
                        Set<Object> vals = representation.getValues(uProp);
                        if (vals.isEmpty()) {
                            log.debug("No value as required for this unifier. Skipping.");
                            complete = false;
                            break;
                        }
                        Object first = vals.iterator().next();
                        Map<String,String> sv = new HashMap<>();
                        if (first instanceof RDFNode && ((RDFNode) first).isURIResource()) {
                            sv.put("@id", ((RDFNode) first).asResource().getURI());
                            sv.put("@type", "@id");
                        } else if (first instanceof RDFNode && ((RDFNode) first).isLiteral()) {
                            Literal fl = ((RDFNode) first).asLiteral();
                            sv.put("@id", fl.getValue().toString());
                            sv.put("@type", fl.getDatatypeURI());
                        }
                        if (sv != null) unimap.put(uProp.toString(), sv);
                    }
                    if (complete) break;
                }

                if (unimap == null) continue;

                Bindings bindings = engine.createBindings();

                // ScriptContext newContext = new SimpleScriptContext();
                // Bindings engineScope =
                // newContext.getBindings(ScriptContext.ENGINE_SCOPE);
                //
                // // add new variable "x" to the new engineScope
                // engineScope.put("x", "world");

                // Get defining properties

                // unimap = new HashMap<String,String>();
                // unimap.put("http://purl.org/vocab/aiiso/schema#code", "W01");

                Context cx = Context.enter();
                Scriptable scope = cx.initStandardObjects();
                Scriptable nobj1 = cx.newObject(scope);
                // Scriptable nobj2 = cx.newObject(scope);

                // Convert it to a NativeObject (yes, this could have been done
                // directly)
                // NativeObject nobj1 = new NativeObject();
                NativeObject nobj2 = new NativeObject();
                for (Map.Entry<String,Map<String,String>> entry : unimap.entrySet())
                    nobj2.defineProperty(entry.getKey(), entry.getValue(), NativeObject.READONLY);
                bindings.put("o1", nobj1);
                bindings.put("o2", nobj2);

                scope.put("o1", scope, nobj1);
                scope.put("o2", scope, nobj2);

                String compiler = provider.getMicrocompiler(type);
                try {
                    Object result = // engine.eval(compiler, bindings);
                    cx.evaluateString(scope, compiler, "<cmd>", 1, null);
                    NativeObject nobj = ((NativeObject) result);
                    for (Object k : nobj.keySet()) {
                        if (k instanceof String) {
                            Object o = nobj.get(k);
                            if (o instanceof Map<?,?>) {
                                RDFNode vnod;
                                Map<?,?> val = (Map<?,?>) o;
                                Object tdef = val.get("@type");
                                if ("@id".equals(tdef)) vnod = ResourceFactory.createResource((String) val
                                        .get("@id"));
                                else vnod = ResourceFactory.createTypedLiteral(val.get("@id"));
                                try {
                                    representation.addValue(URI.create((String) k), vnod);
                                } catch (Exception ex) {
                                    log.warn("Addition of value '{}' failed.", vnod);
                                    log.warn(" ... reason: {}", ex.getLocalizedMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Trouble executing microcompiler", e);
                }

                for (URI p : representation.getAttributes().keySet()) {
                    log.trace("Compiled attribute <{}> :", p);
                    for (Object v : representation.getValues(p))
                        log.trace(" * value : {}", v);
                }
            } // END iterate matchable types
        }
        log.info(" ... total compilation time: {} ms", System.currentTimeMillis() - before);
        return representation;
    }

    @Override
    public Catalogue getCatalogue() {
        return this.catalogue;
    }

    @Override
    public Set<AssemblyProvider> getEntityProviders() {
        return this.providers;
    }

    @Override
    public Set<URI> getInstances(GlobalType type) {
        return getInstances(type, false);
    }

    @Override
    public Set<URI> getInstances(GlobalType type, boolean debug) {
        SortedSet<URI> instances = new TreeSet<>();
        for (AssemblyProvider ap : getEntityProviders()) {
            Map<URI,List<Query>> queries;
            if (ap instanceof DebuggableAssemblyProvider) queries = ((DebuggableAssemblyProvider) ap)
                    .getQueryMap(type, debug);
            else queries = ap.getQueryMap(type);
            log.debug("Dataset list from query map follows:");
            for (URI ds : queries.keySet().toArray(new URI[0])) {
                log.debug(" ... {}", ds);
            }
            Map<URI,List<Query>> queriesByEndpoint = new HashMap<>();
            for (Entry<URI,URI> ds2e : this.catalogue.getQueryEndpointMap(
                queries.keySet().toArray(new URI[0])).entrySet()) {
                URI endpoint = ds2e.getValue();
                log.debug("Associating <{}> to <{}>", endpoint, ds2e.getKey());
                List<Query> lq = queries.get(ds2e.getKey());
                if (lq == null) lq = new LinkedList<Query>();
                if (queriesByEndpoint.containsKey(endpoint)) queriesByEndpoint.get(endpoint).addAll(lq);
                else queriesByEndpoint.put(endpoint, lq);
                log.debug(" ... queries : {}", queriesByEndpoint.get(endpoint));
            }
            if (!queriesByEndpoint.isEmpty()) {
                Set<URI> localInstances = DistributedQueries.executeSubjects(queriesByEndpoint);
                log.trace("Local instances to be rewritten follow:");
                for (URI li : localInstances)
                    log.trace(" - <{}>", li);
                instances.addAll(localInstances);
            }
        }
        return instances;
    }

    @Override
    public TypeSupport getTypeSupport(GlobalType type) {
        return getTypeSupport(type, false);
    }

    public TypeSupport getTypeSupport(GlobalType type, boolean debug) {
        TypeSupport ts = null;
        for (AssemblyProvider ep : getEntityProviders()) {
            TypeSupport temp;
            if (ep instanceof DebuggableAssemblyProvider) temp = ((DebuggableAssemblyProvider) ep)
                    .getTypeSupport(type, debug);
            else temp = ep.getTypeSupport(type);
            if (ts == null) ts = temp;
            else for (URI ds : temp.getDatasets()) {
                log.debug("Dataset {}", ds);
                ts.addDataset(ds);
                for (GlobalURI exp : temp.getExampleInstances(ds))
                    ts.addExampleInstance(ds, exp);
            }
        }
        return ts;
    }

    private void getDataFromCacheHitsBulk(Collection<TargetedQuery> hits, Entity cachedEntity) {
        for (Iterator<Store<?,?>> it = stores.iterator(); it.hasNext();) {
            Store<?,?> st = it.next();
            Map<?,?> res = null;
            if (st instanceof BulkStore && st.getSupportedKeyType() == TargetedQuery.class) {
                try {
                    res = ((BulkStore<TargetedQuery,?>) st).retrieve(hits);
                } catch (Exception ex) {
                    log.error("Retrieval failed.", ex);
                }
                if (res != null) for (Entry<?,?> entry : res.entrySet()) {
                    Object o = entry.getValue();
                    log.trace("Cache hit.");
                    log.trace(" ... is a {}", o.getClass().getCanonicalName());
                    if (!(o instanceof EntityFragment)) throw new IllegalStateException(
                            "Unexpected type " + o.getClass().getCanonicalName()
                                    + " for an entity fragment. Supported types are "
                                    + EntityFragment.class.getCanonicalName() + " and "
                                    + Entity.class.getCanonicalName());
                    EntityFragment ef = (EntityFragment) o;
                    long before = System.currentTimeMillis();
                    Object tq = entry.getKey();
                    if (tq != null) {
                        if (!(tq instanceof TargetedQuery)) throw new IllegalStateException(
                                "Unexpected type " + tq.getClass().getCanonicalName()
                                        + " for a targeted query. Supported types include: "
                                        + TargetedQuery.class.getCanonicalName());
                        reconstructProvenance(ef, ef, ((TargetedQuery) tq).getTarget().toString(),
                            new PropertyPath(), new HashSet<EntityFragment>());
                        log.debug("Provenance computation overhead = {} ms", System.currentTimeMillis()
                                                                             - before);
                    }
                    if (ef instanceof Entity) Util.merge((Entity) ef, cachedEntity);
                    else Util.merge(ef, cachedEntity);
                }
                else log.trace("Cache miss.");
            }
        }
    }

    private void getDataFromCacheHitsPiecemeal(Collection<TargetedQuery> hits, Entity cachedEntity) {
        for (TargetedQuery tq : hits) {
            Object o = null;
            for (Iterator<Store<?,?>> it = stores.iterator(); it.hasNext() && o == null;) {
                Store<?,?> st = it.next();
                if (st.getSupportedKeyType() == TargetedQuery.class) {
                    try {
                        o = ((Store<TargetedQuery,?>) st).retrieve(tq);
                    } catch (Exception ex) {
                        log.error("Retrieval failed.", ex);
                    }
                    if (o != null) {
                        log.trace("Cache hit.");
                        log.trace(" ... is a {}", o.getClass().getCanonicalName());
                        if (!(o instanceof EntityFragment)) throw new IllegalStateException(
                                "Unexpected type " + o.getClass().getCanonicalName()
                                        + " for an entity fragment. Supported types are "
                                        + EntityFragment.class.getCanonicalName() + " and "
                                        + Entity.class.getCanonicalName());
                        EntityFragment ef = (EntityFragment) o;
                        long before = System.currentTimeMillis();
                        reconstructProvenance(ef, ef, tq.getTarget().toString(), new PropertyPath(),
                            new HashSet<EntityFragment>());
                        log.debug("Provenance computation overhead = {} ms", System.currentTimeMillis()
                                                                             - before);
                        if (ef instanceof Entity) Util.merge((Entity) ef, cachedEntity);
                        else Util.merge(ef, cachedEntity);
                    } else log.trace("Cache miss.");
                }
            }

        }
    }

    private EntityFragment reconstructProvenance(EntityFragment inspectMe,
                                                 EntityFragment target,
                                                 final String dataset,
                                                 PropertyPath ctx,
                                                 final Set<EntityFragment> visited) {

        for (URI attr : inspectMe.getAttributes().keySet()) {
            PropertyPath path = new PropertyPath();
            path.addAll(ctx);
            path.add(attr);
            log.trace("Dataset <{}> is provenance for path {}", dataset, path);
            target.addContributingSource(dataset, path);
        }
        for (Entry<URI,Set<Entity>> exps : inspectMe.getEAttributes().entrySet()) {
            PropertyPath newCtx = new PropertyPath();
            newCtx.addAll(ctx);
            newCtx.add(exps.getKey());
            for (Entity eexp : exps.getValue())
                if (!visited.contains(eexp)) {
                    reconstructProvenance(eexp, target, dataset, newCtx, visited);
                    visited.add(eexp);
                }
        }
        return target;
    }

}
