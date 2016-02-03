package org.mksmart.ecapi.impl;

import java.util.HashSet;
import java.util.Set;

import org.mksmart.ecapi.api.GlobalType;
import org.mksmart.ecapi.api.id.GlobalURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reference implementation of {@link GlobalType}.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class GlobalTypeImpl implements GlobalType {

    protected String defaultQueryTemplate;

    protected GlobalURI id;

    private Logger log = LoggerFactory.getLogger(getClass());

    protected Set<GlobalType> subtypes, supertypes;

    /**
     * 
     * @param id
     *            an identifer for the type. If null, the TOP type is assumed
     */
    public GlobalTypeImpl(GlobalURI id) {
        subtypes = new HashSet<>();
        supertypes = new HashSet<>();
        if (id == null) {
            log.warn("Null ID supplied for global type creation. Assuming TOP type <{}>", GlobalType.TOP_URI);
            id = GlobalType.TOP_URI;
        }
        this.id = id;
    }

    @Override
    public void addSubtype(GlobalType subType) {
        subtypes.add(subType);
    }

    @Override
    public void addSupertype(GlobalType superType) {
        supertypes.add(superType);
    }

    @Override
    public Set<GlobalType> getAssertedSupertypes() {
        return supertypes;
    }

    @Override
    public String getDefaultQueryTemplate() {
        return defaultQueryTemplate;
    }

    @Override
    public GlobalURI getId() {
        return id;
    }

    @Override
    public Set<GlobalType> getSubtypes() {
        return subtypes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof GlobalType)) return false;
        GlobalType gt = (GlobalType) obj;
        boolean eq = this.getId().equals(gt.getId());
        if (!eq) return eq;
        eq &= (this.getDefaultQueryTemplate() == null && gt.getDefaultQueryTemplate() == null || this
                .getDefaultQueryTemplate().equals(gt.getDefaultQueryTemplate()));
        return eq;
    }

    @Override
    public void setDefaultQueryTemplate(String queryTemplate) {
        this.defaultQueryTemplate = queryTemplate;
    }

    @Override
    public String toString() {
        return getId().toString();
    }

}
