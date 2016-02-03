package org.mksmart.ecapi.api;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.query.Query;

public interface DebuggableAssemblyProvider extends AssemblyProvider, DebuggableSupportRetriever {

    public Map<URI,List<Query>> getQueryMap(GlobalType gtype, boolean debug);

    public Map<URI,List<Query>> getQueryMap(GlobalURI guri, boolean debug);

    public Map<URI,List<Query>> getQueryMap(GlobalURI guri, Set<String> datasetNames, boolean debug);

}
