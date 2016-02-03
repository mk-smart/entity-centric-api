package org.mksmart.ecapi.api;

import java.net.URI;
import java.util.Set;

import org.mksmart.ecapi.api.generic.NotReconfigurableException;
import org.mksmart.ecapi.api.id.GlobalURI;

/**
 * Executes the core application logic for the assembly of entity representations from one or more sources.
 * Implementations will handle local identifiers of the type {@link URI} and return objects of type
 * {@link Entity}.
 * 
 * TODO split this into a structure that reflects the frontend/backend model.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface EntityCompiler extends SupportRetriever {

    /**
     * Registers an entity provider with this compiler.
     * 
     * @param provider
     *            the entity provider to be registered.
     * @throws NotReconfigurableException
     *             if the addition of further entity providers is not allowed.
     */
    public void addEntityProvider(AssemblyProvider provider) throws NotReconfigurableException;

    /**
     * Newer and more general brother of {@link #compileEntity(URI)}, also takes into account stores
     * 
     * @param localId
     * @return
     */
    public Entity assembleEntity(GlobalURI localId, Set<String> datasets);

    /**
     * Creates a new Entity given one of its local identifiers, i.e. a URI that names this entity in at least
     * one data source.
     * 
     * @deprecated please refer to the {@link #assembleEntity(URI)} method of this class.
     * 
     * @param localId
     *            a URI that identifies this entity in some data source. It will become an alias in the
     *            compiled entity.
     * @return a compiled representation of the entity.
     */
    public Entity compileEntity(URI localId);

    /**
     * Gets the set of all entity providers registered with this compiler
     * 
     * @return the set of entity providers.
     */
    public Set<AssemblyProvider> getEntityProviders();

    /**
     * localised instances
     * 
     * @param type
     * @return
     */
    public Set<URI> getInstances(GlobalType type);

}
