package org.mksmart.ecapi.api.storage;

import java.util.Collection;
import java.util.Map;

/**
 * A store that is able to retrieve multiple things at once.
 * 
 * @author alessandro <alexdma@apache.org>
 *
 * @param <K>
 * @param <V>
 */
public interface BulkStore<K,V> extends Store<K,V> {

    public Map<K,V> retrieve(Collection<K> key);

}
