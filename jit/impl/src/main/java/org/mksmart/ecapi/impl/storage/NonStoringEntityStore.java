package org.mksmart.ecapi.impl.storage;

import java.net.URI;

import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.storage.EntityStore;

/**
 * A dummy implementation of {@link EntityStore} that does not actually store anything. Can be used when one
 * wants an instance of the ECAPi to be stateless and non-caching.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class NonStoringEntityStore implements EntityStore<URI> {

    @Override
    public Class<URI> getSupportedKeyType() {
        return URI.class;
    }

    @Override
    public Class<Entity> getSupportedValueType() {
        return Entity.class;
    }

    @Override
    public Entity retrieve(URI globalId) {
        return null; // !IMPORTANT
    }

    @Override
    public URI store(Entity entity) {
        return null;
    }

    @Override
    public URI store(Entity entity, URI globalId) {
        return globalId;
    }

}
