package org.mksmart.ecapi.api.provenance;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ProvenanceListenable {

    private Set<ProvenanceListener> listeners = new HashSet<>();

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public void addProvenanceListener(ProvenanceListener listener) {
        listeners.add(listener);
    }

    public void clearProvenanceListeners() {
        listeners.clear();
    }

    protected void firePropertyAdded(PropertyPath context, URI property, URI provenance) {
        PropertyPath path = new PropertyPath();
        log.trace("In context {}", context);
        log.trace(" ... dataset <{}> contributes new property <{}>", provenance, property);
        path.addAll(context);
        path.add(property);
        for (ProvenanceListener listener : listeners) {
            log.trace("Source <{}> contributes {}", provenance, path);
            listener.sourceContributes(provenance, path);
        }
    }

    public Set<ProvenanceListener> getProvenanceListeners() {
        return listeners;
    }

}
