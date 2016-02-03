package org.mksmart.ecapi.api.storage;

import org.mksmart.ecapi.api.EntityFragment;

/**
 * Stores entity fragments using a preferred key.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 * @param <K>
 */
public interface FragmentStore<K> extends PerTargetedQueryStore<EntityFragment> {

}
