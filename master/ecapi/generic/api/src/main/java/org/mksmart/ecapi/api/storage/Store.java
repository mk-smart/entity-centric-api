package org.mksmart.ecapi.api.storage;

/**
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 * @param <K>
 *            the class of storage keys.
 * @param <V>
 *            the class of stored values.
 */
public interface Store<K,V> {

    public Class<K> getSupportedKeyType();

    public Class<V> getSupportedValueType();

    /**
     * Fetches the object that matches the supplied key.
     * 
     * @param key
     *            the key of the stored object.
     * @return the matching item, or null if not present.
     */
    public V retrieve(K key);

    /**
     * Stores the supplied object and lets it compute its key based on its data and registered generation
     * algorithm.
     * 
     * @param entity
     *            the object to be stored.
     * @return the computed key of the stored object.
     */
    public K store(V item);

    /**
     * Stores the supplied object using the supplied key. There is no set behaviour for the case when an
     * object with that key is already present. Implementations may opt for replacement, generating a new key
     * and returning it, throwing an exception, and so on.
     * 
     * @param item
     *            the object to be stored.
     * @param key
     *            the desired key of the stored object.
     * @return the computed key of the stored object.
     */
    public K store(V item, K key);

}
