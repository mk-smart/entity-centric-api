package org.mksmart.ecapi.api.storage;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mksmart.ecapi.api.query.Query;
import org.mksmart.ecapi.api.query.TargetedQuery;

public interface Cache {

    /**
     * Given a query plan, determines which queries have a cache hit and creates a plan of "cache hit"
     * queries, which can then be used to skim the original query plan.
     * 
     * This method does not retrieve the actual data, nor should it alter the original query plan.
     * 
     * @param queryPlan
     *            the original query plan, which is not altered
     * @return the plan of cache hits
     */
    public Set<TargetedQuery> getCacheHits(final Map<URI,List<Query>> queryPlan);

}
