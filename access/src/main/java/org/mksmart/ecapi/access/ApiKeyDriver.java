package org.mksmart.ecapi.access;

import java.util.Set;

/**
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface ApiKeyDriver {

    public static final int GRANT_RIGHT = 2;
    public static final int READ_RIGHT = 0;
    public static final int WRITE_RIGHT = 1;

    /**
     * 
     * 
     * @param key
     * @return true iff ALL keys are valid
     */
    public boolean exists(String... keys);

    public Set<String> getDataSources() throws NotCheckingRightsException;

    /**
     * 
     * @param keys
     * @return
     */
    public Set<String> getDataSources(String... keys) throws NotCheckingRightsException;

    public boolean grant(String key, String ukey, String resourceID, int right);

    /**
     * 
     * @param key
     * @param rightone
     *            of APIKeyDriver.READ_RIGHT|WRITE_RIGHT|GRANT_RIGHT
     * @return
     */
    public boolean hasRight(String key, String resourceID, int right);

}
