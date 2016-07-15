package org.mksmart.ecapi.commons.couchdb.client;

/**
 * @author alessandro <alexdma@apache.org>
 * 
 * @param <D>
 *            the supported type or expected documents.
 */
public interface DocumentWriter<D> {

    public boolean addDesignDocument(D doc, String id, boolean replace);

    public boolean addDocument(D doc, boolean replace);

    public boolean addDocument(D doc, String id, boolean replace);

    public boolean addDocument(D doc, String id, String path, boolean replace);
}
