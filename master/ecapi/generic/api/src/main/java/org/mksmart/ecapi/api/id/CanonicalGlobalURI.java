package org.mksmart.ecapi.api.id;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A canonical global URI either is of the form <tt>{type}/{id}</tt>, or contains its members.
 * 
 * @author Alessandro Adamou <alexdma@apache.org>
 * 
 */
public class CanonicalGlobalURI implements GlobalURI {

    public static CanonicalGlobalURI parse(String s) {
        Pattern pattern = Pattern.compile("^([\\w\\-\\.:_]+)\\/(.+)$");
        // DON't touch for now
        Matcher m = pattern.matcher(s);
        // if(m.find(1)) gu.setNamespace(m.group(1));
        if (!m.find()) throw new IllegalArgumentException("String does not parse as a global URI.");
        CanonicalGlobalURI gu = new CanonicalGlobalURI();
        gu.setEntityType(m.group(1));
        gu.setIdentifer(m.group(2));

        return gu;
    }

    private String namespace, entityType, identifer;

    protected CanonicalGlobalURI() {}

    public CanonicalGlobalURI(String entityType, String id) {
        this();
        this.setEntityType(entityType);
        this.setIdentifer(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        return this.toString().equals(obj.toString());
    }

    /**
     * Gets the URI element that defines one of the types of the corresponding entity.
     * 
     * @return the entity type element.
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * Gets the URI element whose (scoped) value identifies the corresponding entity.
     * 
     * @return the identifier element.
     */
    @Override
    public String getIdentifer() {
        return identifer;
    }

    /**
     * Gets the namespace that prefixes all the data-dependent elements of the global URI, if present, in
     * which case the global URI is absolute.
     * 
     * @return the global URI namespace, or null if the global URI is relative.
     */
    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * Returns true if and only if there is a defined namespace for this global URI.
     * 
     * @return true iff a namespace is defined.
     */
    @Override
    public boolean isRelative() {
        return namespace == null || namespace.isEmpty();
    }

    /**
     * Sets the URI element that defines one of the types of the corresponding entity.
     * 
     * @param entityType
     *            the entity type element.
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * Sets the URI element whose (scoped) value identifies the corresponding entity.
     * 
     * @param identifer
     *            the identifier element.
     */
    @Override
    public void setIdentifer(String identifer) {
        this.identifer = identifer;
    }

    /**
     * Gets the namespace that prefixes all the data-dependent elements of the global URI, if present, in
     * which case the global URI is absolute.
     * 
     * @param namespace
     *            the global URI namespace, or null if the global URI is relative.
     */
    @Override
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        if (!isRelative()) s.append(getNamespace());
        s.append(getEntityType() + '/');
        s.append(getIdentifer());
        return s.toString();
    }

}
