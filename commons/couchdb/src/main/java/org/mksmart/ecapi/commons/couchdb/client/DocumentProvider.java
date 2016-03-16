package org.mksmart.ecapi.commons.couchdb.client;

/**
 * A provider of CouchDB documents, including design documents and views. This interface is versatile and
 * allows implementations to choose in what form to provide documents (e.g. a simple JSONObject in org.json or
 * a CouchDbDocument/View in Ektorp).
 * 
 * TODO check if this paradigm can be extended to resources other than CouchDB documents.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 * @param <D>
 *            the supported type or expected documents.
 */
public interface DocumentProvider<D> {

    /**
     * Returns a document of any type (including design documents) given its unique identifier.
     * 
     * @param id
     *            the (URLdecoded) document identifier.
     * @return the corresponding document, or null if it could not be retrieved.
     */
    public D getDocument(String id);

    public D getDocuments(String... keys);

    public D getReducedView(String designDocId, String viewId, boolean group, String... keys);

    /**
     * Returns a resource of any type (including views) given its full URL. It is a low-level method that can
     * be called in lieu of {@link DocumentProvider#getDocument(String)} or
     * {@link DocumentProvider#getView(String, String, String...)} as required. If the requested resource is a
     * view, a range of keys can be provided to restrict results.
     * 
     * @param url
     *            the resource URL.
     * @param keys
     *            (for views) the keys to restrict the rows of a result set.
     * @return the requested resource, or null if it could not be found.
     */
    public D getResource(String url, String... keys);

    /**
     * Returns a view as parametrised with an optionally supplied key set.
     * 
     * @param designDocId
     *            the design document to compute the view with.
     * @param viewId
     *            the view ID
     * @param keys
     *            an optional array of keys to restrict the result set contained in the view.
     * @return the view as a document, or null if it could not be retrieved.
     */
    public D getView(String designDocId, String viewId, String... keys);

}
