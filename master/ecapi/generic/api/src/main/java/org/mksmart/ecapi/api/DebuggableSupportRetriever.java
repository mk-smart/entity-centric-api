package org.mksmart.ecapi.api;

public interface DebuggableSupportRetriever extends SupportRetriever {

    public TypeSupport getTypeSupport(GlobalType type, boolean debug);

}
