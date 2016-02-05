package org.mksmart.ecapi.api;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * A dataset catalogue that uses URIs to identify datasets.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface Catalogue {

    /**
     * Adds the supplied dataset to the list of datasets that provide support to entities of the given type.
     * 
     * @param type
     *            the RDF type to be supported.
     * @param dataset
     *            the dataset identifier.
     * @return true iff the addition operation was successful.
     */
    public boolean addSupportingDataset(URI type, URI dataset);

    public Set<URI> getDatasets();

    public URI getQueryEndpoint(URI dataset);

    public Map<URI,URI> getQueryEndpointMap(URI... datasets);

    /**
     * Gets the set of datasets that <em>explicitly</em> support a given type.
     * 
     * @param type
     *            the RDF type for which support is sought.
     * @return the set opf supporting datasets.
     */
    public Set<URI> getSupportingDatasets(URI type);

    public Map<URI,String> getUuids(URI... datasetIds);

}
