package org.mksmart.ecapi.api;

import java.net.URI;
import java.util.Set;

import org.mksmart.ecapi.api.id.GlobalURI;

/**
 * An entity compiler that supports debug-specific methods.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface DebuggableEntityCompiler extends EntityCompiler, DebuggableSupportRetriever {

    /**
     * 
     * @param localId
     * @param datasets
     *            if null, all open datasets will contribute to the compiled entity.
     * @param debug
     *            if true, datasets in debug model will contribute along with the others.
     * @return
     */
    public Entity assembleEntity(GlobalURI localId, Set<String> datasets, boolean debug);

    /**
     * 
     * @param type
     * @param debug
     *            if true, datasets in debug model will contribute along with the others.
     * @return
     */
    public Set<URI> getInstances(GlobalType type, boolean debug);

}
