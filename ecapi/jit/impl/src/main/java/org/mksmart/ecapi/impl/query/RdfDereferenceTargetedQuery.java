package org.mksmart.ecapi.impl.query;

import java.net.URI;

import org.mksmart.ecapi.api.query.TargetedQuery;

public class RdfDereferenceTargetedQuery extends RdfDereferenceQuery implements TargetedQuery {

    protected URI dataset;

    public RdfDereferenceTargetedQuery(URI uri, URI dataset) {
        super(uri);
        this.dataset = dataset;
    }

    @Override
    public URI getTarget() {
        return dataset;
    }

    @Override
    public String toString() {
        return dataset.toString() + "::" + super.toString();
    }

}
