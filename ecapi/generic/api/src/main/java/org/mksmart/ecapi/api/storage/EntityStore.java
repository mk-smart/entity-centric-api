package org.mksmart.ecapi.api.storage;

import org.mksmart.ecapi.api.Entity;

/**
 * A flexible persistence mechanism for entities managed by the entity-centric API.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 * @param <I>
 *            the supported class of entity identifiers
 */
public interface EntityStore<I> extends Store<I,Entity> {

}
