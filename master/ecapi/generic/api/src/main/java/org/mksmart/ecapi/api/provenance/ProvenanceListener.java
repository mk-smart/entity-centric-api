package org.mksmart.ecapi.api.provenance;

import java.net.URI;

/**
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface ProvenanceListener {

    public void sourceContributes(URI source, PropertyPath path);

}
