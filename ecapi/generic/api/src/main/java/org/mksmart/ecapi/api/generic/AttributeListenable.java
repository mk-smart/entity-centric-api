package org.mksmart.ecapi.api.generic;

import java.util.Set;

/**
 * Objects that will notify listeners on changes to attribute-value sets will implement this interface.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface AttributeListenable {

    /**
     * Registers a new {@link AttributeListener} with this object.
     * 
     * @param listener
     *            the listener to be registered.
     */
    public void addListener(AttributeListener listener);

    /**
     * Unregisters all active listeners from this object.
     */
    public void clearListeners();

    /**
     * Gets the set of all the listeners registered with this object, in no particular order.
     * 
     * @return the set of listeners.
     */
    public Set<AttributeListener> getListeners();

    /**
     * Unregisters an active {@link AttributeListener} from this object. It is important to call this method
     * for objects the developer wishes to be GC'ed.
     * 
     * @param listener
     *            the listener to be unregistered.
     */
    public void removeListener(AttributeListener listener);

}
