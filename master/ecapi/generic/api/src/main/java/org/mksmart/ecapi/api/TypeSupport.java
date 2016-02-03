package org.mksmart.ecapi.api;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mksmart.ecapi.api.id.GlobalURI;

/**
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class TypeSupport {

    public static TypeSupport create(GlobalType gt) {
        return new TypeSupport(gt);
    }

    private Map<URI,Set<GlobalURI>> instances = new HashMap<>();

    private GlobalURI typeId;

    public TypeSupport(GlobalType gt) {
        if (gt == null) throw new IllegalArgumentException("Cannot create support for a null type.");
        this.typeId = gt.getId();
    }

    public void addDataset(URI datasetId) {
        if (!instances.containsKey(datasetId)) instances.put(datasetId, new HashSet<GlobalURI>());
    }

    public void addExampleInstance(URI datasetId, GlobalURI entity) {
        addDataset(datasetId);
        instances.get(datasetId).add(entity);
    }

    public Set<URI> getDatasets() {
        // FIXME not complete
        return instances.keySet();
    }

    public Set<GlobalURI> getExampleInstances() {
        Set<GlobalURI> inst = new HashSet<>();
        for (Entry<URI,Set<GlobalURI>> entry : instances.entrySet())
            if (entry.getValue() != null) inst.addAll(entry.getValue());
        return inst;
    }

    public Set<GlobalURI> getExampleInstances(URI datasetName) {
        return instances.get(datasetName);
    }

    public GlobalURI getTypeId() {
        return typeId;
    }

}
