package org.mksmart.ecapi.api.id;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A global URI is a URI that aggregates every instance of the resource associated to it in each data source
 * that describes it. Unless otherwise specified, the scheme for global URIs is assumed to be a regular
 * language.
 * 
 * Can be subclassed to provide support for further URI schemes.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class ScopedGlobalURI extends CanonicalGlobalURI {

    /**
     * Returns a structured form of a global URI from a string, if it matches the expected pattern. It will
     * throw an {@link IllegalArgumentException} if parsing fails.
     * 
     * @param s
     *            the string to be parsed for global URIs.
     * @return the parsed global URI.
     */
    public static ScopedGlobalURI parse(String s) {
        Pattern pattern = Pattern.compile("^([\\w\\-\\.:_]+)\\/(\\w+):(\\w+)\\/(.+)$");
        // DON't touch for now
        Matcher m = pattern.matcher(s);

        // if(m.find(1)) gu.setNamespace(m.group(1));
        if (!m.find()) throw new IllegalArgumentException("String does not parse as a global URI.");
        ScopedGlobalURI gu = new ScopedGlobalURI();
        gu.setEntityType(m.group(1));
        gu.setIdentifierRealm(m.group(2));
        gu.setIdentifyingProperty(m.group(3));
        gu.setIdentifer(m.group(4));

        return gu;
    }

    private String identifyingProperty, identifierRealm;

    protected ScopedGlobalURI() {}

    public ScopedGlobalURI(String entityType, String identifierRealm, String identifyingProperty, String id) {
        this();
        this.setEntityType(entityType);
        this.setIdentifierRealm(identifierRealm);
        this.setIdentifyingProperty(identifyingProperty);
        this.setIdentifer(id);
    }

    /**
     * Gets the URI element that defines the scope of the property used to identify the corresponding entity,
     * i.e. a definition of the realm where the value is guaranteed to uniquely identify the entity.
     * 
     * @return the identifying property scope element.
     */
    public String getIdentifierRealm() {
        return identifierRealm;
    }

    /**
     * Gets the URI element that defines the property (or category thereof) that is used to identify the
     * corresponding entity.
     * 
     * @return the identifying property element.
     */
    public String getIdentifyingProperty() {
        return identifyingProperty;
    }

    /**
     * Gets the URI element that defines the scope of the property used to identify the corresponding entity,
     * i.e. a definition of the realm where the value is guaranteed to uniquely identify the entity.
     * 
     * @param identifierScope
     *            the identifying property scope element.
     */
    public void setIdentifierRealm(String identifierRealm) {
        this.identifierRealm = identifierRealm;
    }

    /**
     * Sets the URI element that defines the property (or category thereof) that is used to identify the
     * corresponding entity.
     * 
     * @param identifyingProperty
     *            the identifying property element.
     */
    public void setIdentifyingProperty(String identifyingProperty) {
        this.identifyingProperty = identifyingProperty;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        if (!isRelative()) s.append(getNamespace());
        s.append(getEntityType() + '/');
        s.append(((identifierRealm == null || identifierRealm.isEmpty() ? "_self" : identifierRealm)) + ':');
        s.append(((identifyingProperty == null || identifyingProperty.isEmpty() ? "id" : identifyingProperty)) + '/');
        s.append(getIdentifer());
        return s.toString();
    }

}
