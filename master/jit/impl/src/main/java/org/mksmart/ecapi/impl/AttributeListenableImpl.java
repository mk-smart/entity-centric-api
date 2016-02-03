package org.mksmart.ecapi.impl;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.mksmart.ecapi.api.generic.AttributeEvent;
import org.mksmart.ecapi.api.generic.AttributeListenable;
import org.mksmart.ecapi.api.generic.AttributeListener;

import com.hp.hpl.jena.rdf.model.RDFNode;

public class AttributeListenableImpl implements AttributeListenable {

    private Set<AttributeListener> listeners = new HashSet<AttributeListener>();

    public void addListener(AttributeListener listener) {
        listeners.add(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    protected void fireAttributeSet(URI localId, URI property, RDFNode value) {
        AttributeEvent event = new AttributeEvent(localId, property, value);
        for (AttributeListener l : listeners)
            l.attributeSet(event);
    }

    public Set<AttributeListener> getListeners() {
        return listeners;
    }

    public void removeListener(AttributeListener listener) {
        listeners.remove(listener);
    }

}
