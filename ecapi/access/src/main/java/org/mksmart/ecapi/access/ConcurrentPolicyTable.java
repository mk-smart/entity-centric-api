package org.mksmart.ecapi.access;

import java.util.Hashtable;

import org.mksmart.ecapi.api.access.PolicyTable;

/**
 * Thread-safe implementation of a {@link PolicyTable}.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class ConcurrentPolicyTable extends Hashtable<String,Boolean> implements PolicyTable {

    /**
     * 
     */
    private static final long serialVersionUID = -3260144510154521605L;

}
