package org.mksmart.ecapi.access;

import java.util.Set;

/**
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface ApiKeyDriver {

    /**
     * 
     * 
     * @param key
     * @return true iff ALL keys are valid
     */
    public boolean exists(String... keys);

    /**
     * 
     * @param keys
     * @return
     */
    public Set<String> getDataSources(String... keys);

}
