package org.mksmart.ecapi.api.id;

/**
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public interface GlobalURI {

    public String getIdentifer();

    public String getNamespace();

    public boolean isRelative();

    public void setIdentifer(String identifer);

    public void setNamespace(String namespace);

}
