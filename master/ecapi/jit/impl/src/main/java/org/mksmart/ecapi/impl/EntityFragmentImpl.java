package org.mksmart.ecapi.impl;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.mksmart.ecapi.api.EntityFragment;
import org.mksmart.ecapi.api.provenance.PropertyPath;
import org.mksmart.ecapi.api.provenance.ProvenanceTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard implementation of {@link EntityFragment}. This implementation uses a forest to track provenance
 * information.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public abstract class EntityFragmentImpl implements EntityFragment {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * A forest of provenance trees, associated to dataset names. The roots of the trees are not labelled.
     */
    private Map<String,ProvenanceTreeNode> provenanceMap = new TreeMap<>();

    public EntityFragmentImpl() {}

    @Override
    public void addContributingSource(String dataset, PropertyPath path) {
        log.debug("Dataset <{}> contributes (potentially partial) path {}", dataset, path);
        ProvenanceTreeNode node;
        if (provenanceMap.containsKey(dataset)) node = provenanceMap.get(dataset);
        else {
            node = new ProvenanceTreeNode(null); // roots are not labelled
            provenanceMap.put(dataset, node);
        }
        for (URI step : path) {
            boolean found = false;
            Iterator<ProvenanceTreeNode> itc = node.getChildren().iterator();
            while (itc.hasNext() && !found) {
                ProvenanceTreeNode child = itc.next();
                if (step.equals(child.getValue())) {
                    node = child;
                    found = true;
                }
            }
            if (!found) node = node.addChild(step);
        }
    }

    @Override
    @Deprecated
    public void addContributingSource(String dataset, URI property) {
        PropertyPath p = new PropertyPath();
        p.add(property);
        addContributingSource(dataset, p);
    }

    @Override
    public Set<PropertyPath> getContributedProperties(String dataset) {
        Set<PropertyPath> result = new HashSet<>();
        ProvenanceTreeNode root = provenanceMap.get(dataset);
        buildPropertyPaths(root, new PropertyPath(), result);
        log.trace("Dataset <{}> trace follows:", dataset);
        for (PropertyPath pp : result)
            log.trace(" - {} (length={})", pp, pp.size());
        return result;
    }

    @Override
    public Map<String,ProvenanceTreeNode> getProvenanceMap() {
        return provenanceMap;
    }

    @Override
    public void sourceContributes(URI source, PropertyPath path) {
        addContributingSource(source.toString(), path);
    }

    /**
     * Visits a given provenance tree to recursively construct its property paths.
     * 
     * TODO make this an utility method as it does not use class members.
     * 
     * @param pos
     *            the tree node to inspect.
     * @param current
     *            the property path that is being populated in this iteration.
     * @param target
     *            the path set that is being populated.
     */
    protected void buildPropertyPaths(ProvenanceTreeNode pos, PropertyPath current, Set<PropertyPath> target) {
        if (current == null) throw new IllegalArgumentException(
                "Cannot work with a null property path. Use an empty one at least.");
        List<ProvenanceTreeNode> children = pos.getChildren();
        if (children.size() == 1) { // one child -> try to keep the path currently under construction
            ProvenanceTreeNode next = children.iterator().next();
            if (next.getValue() != null) current.add(next.getValue());
            buildPropertyPaths(next, current, target);
        } else if (children.size() > 1) { // many children -> discard this path and create new ones
            for (ProvenanceTreeNode ch : children) {
                PropertyPath newPath = new PropertyPath();
                newPath.addAll(current);
                newPath.add(ch.getValue());
                buildPropertyPaths(ch, newPath, target);
            }
        } else target.add(current); // a leaf -> this is a good path so store it
    }

}
