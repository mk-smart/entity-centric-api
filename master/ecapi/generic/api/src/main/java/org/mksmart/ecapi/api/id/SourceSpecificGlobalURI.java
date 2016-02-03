package org.mksmart.ecapi.api.id;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A form of Global URI that includes a specification of the data source that "owns" the entity as part of its
 * naming convention.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class SourceSpecificGlobalURI extends ScopedGlobalURI {

    public static SourceSpecificGlobalURI parse(String s) {
        Pattern pattern = Pattern.compile("([\\w\\-\\.:_]+)\\/(.+)\\/(.+):(.+)\\/(\\w+)$");
        Matcher m = pattern.matcher(s);

        // if(m.find(1)) gu.setNamespace(m.group(1));
        if (!m.find()) throw new IllegalArgumentException("String does not parse as a global URI.");
        SourceSpecificGlobalURI gu = new SourceSpecificGlobalURI();
        gu.setEntityType(m.group(1));
        gu.setOwningAuthority(m.group(2));
        gu.setIdentifierRealm(m.group(3));
        gu.setIdentifyingProperty(m.group(4));
        gu.setIdentifer(m.group(5));

        return gu;
    }

    private String authority;

    /**
     * Gets the owning authority element of this global URI, i.e. the primary source where the entity is
     * defined, or the one to which one wishes to restrict.
     * 
     * @return the string form of the owning authority.
     */
    public String getOwningAuthority() {
        return authority;
    }

    /**
     * Sets the owning authority element of this global URI, i.e. the primary source where the entity is
     * defined, or the one to which one wishes to restrict.
     * 
     * @param authority
     *            the string form of the owning authority.
     */
    public void setOwningAuthority(String authority) {
        this.authority = authority;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        if (!isRelative()) s.append(getNamespace());
        s.append(getEntityType() + '/');
        s.append(this.authority + ':');
        String ipo = getIdentifierRealm(), ip = getIdentifyingProperty();
        s.append(((ipo == null || ipo.isEmpty() ? "_self" : ipo)) + ':');
        s.append(((ip == null || ip.isEmpty() ? "id" : ip)) + '/');
        s.append(getIdentifer());
        return s.toString();
    }

}
