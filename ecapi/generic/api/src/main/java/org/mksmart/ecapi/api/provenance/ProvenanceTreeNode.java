package org.mksmart.ecapi.api.provenance;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * A tree structure that represents property path support (of a dataset, fragment, etc.).
 * 
 * This implementation does not provide any of the standard visitation strategies (i.e. pre-order, in-order or
 * post-order), but visits the children of a node in the order they were added.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class ProvenanceTreeNode /* implements Iterable<ProvenanceTreeNode> */{

    private List<ProvenanceTreeNode> children;
    private URI data;
    private ProvenanceTreeNode parent;

    /**
     * Creates a new labelled provenance node.
     * 
     * @param data
     *            the URI of the property used to label this node.
     */
    public ProvenanceTreeNode(URI data) {
        this.data = data;
        this.children = new LinkedList<ProvenanceTreeNode>();
    }

    /**
     * Adds a new child node with the supplied label.
     * 
     * @param child
     *            the value to label the new child node with.
     * @return the child node that was created and added.
     */
    public ProvenanceTreeNode addChild(URI child) {
        ProvenanceTreeNode childNode = new ProvenanceTreeNode(child);
        childNode.parent = this;
        this.children.add(childNode);
        return childNode;
    }

    /**
     * Returns the list of child nodes <em>in the same order as they were added</em>.
     * 
     * @return the child node list.
     */
    public List<ProvenanceTreeNode> getChildren() {
        return children;
    }

    /**
     * Gets the URI that this node was labelled with.
     * 
     * @return the node value.
     */
    public URI getValue() {
        return data;
    }

}
