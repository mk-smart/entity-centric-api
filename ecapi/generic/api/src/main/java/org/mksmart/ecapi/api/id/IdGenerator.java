package org.mksmart.ecapi.api.id;

import java.net.URI;

/**
 * A factory object that implements a strategy for creating entity identifiers.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 * @param <I>
 *            the supported class of identifiers that can be generated.
 * 
 * @param <E>
 *            the supported class of entities.
 */
public interface IdGenerator<I,E> {

    /**
     * Generates an identifier of an entity given its data only. Note that data may include local aliases as
     * well.
     * 
     * @param e
     *            the entity for which the identifier is to be created.
     * @return a generated identifier.
     */
    public I createId(E e);

    public String createPropertyId(URI localProperty);

    public String createIdFromUri(URI localURI);

    void refresh();

}
