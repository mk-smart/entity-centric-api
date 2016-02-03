package org.mksmart.ecapi.couchdb.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.mksmart.ecapi.api.query.TargetedQuery;

public class EncodeUtils {

    /**
     * Returns the string encoding for {@link TargetedQuery} objects that is used as key in stores and caches.
     * 
     * @param query
     * @return
     */
    public static String encode(TargetedQuery query) {
        return DigestUtils.shaHex(query.toString());
    }

}
