package org.mksmart.ecapi.api.generic;

/**
 * Object that are intended to react to changes in the attribute-value structure of an entity will implement
 * this interface.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface AttributeListener {

    /**
     * Fired whenever a change to an attribute-value pair is detected in an entity.
     * 
     * @param event
     *            the change event.
     */
    public void attributeSet(AttributeEvent event);

}
