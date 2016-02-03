package org.mksmart.ecapi.api.query;

import java.net.URI;

/**
 * A query that encapsulates its target (e.g. SPARQL endpoint)
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface TargetedQuery extends Query {

    /**
     * Typically the name of the dataset.
     * 
     * @return
     */
    public URI getTarget();

}
