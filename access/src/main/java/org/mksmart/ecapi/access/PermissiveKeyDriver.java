package org.mksmart.ecapi.access;

import java.util.Set;

/**
 * A dummy key driver that says yes to everything.
 * 
 * @author aa8752
 *
 */
public class PermissiveKeyDriver implements ApiKeyDriver {

    @Override
    public boolean exists(String... keys) {
        return true;
    }

    @Override
    public Set<String> getDataSources() throws NotCheckingRightsException {
        throw new NotCheckingRightsException();
    }

    @Override
    public Set<String> getDataSources(String... keys) throws NotCheckingRightsException {
        throw new NotCheckingRightsException();
    }

    @Override
    public boolean grant(String key, String ukey, String resourceID, int right) {
        return true;
    }

    @Override
    public boolean hasRight(String key, String resourceID, int right) {
        return true;
    }

}
