package org.mksmart.ecapi.couchdb;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.api.Catalogue;
import org.mksmart.ecapi.commons.couchdb.client.DocumentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Catalogue} implemented by binding to a CouchDB {@link DocumentProvider}. It is completely
 * persisted as the catalogue, once read, is never kept in memory, and is therefore sensitive to changes in
 * the database.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class CouchDbCatalogue implements Catalogue {

    private DocumentProvider<JSONObject> documentProvider;

    private Logger log = LoggerFactory.getLogger(getClass());

    public CouchDbCatalogue(DocumentProvider<JSONObject> documentProvider) {
        this.documentProvider = documentProvider;
    }

    @Override
    public boolean addSupportingDataset(URI type, URI dataset) {
        throw new UnsupportedOperationException(
                "This implementation only works live and cannot alter a catalogue that is already configured."
                        + " It will however reflect changes in the catalogue database.");
    }

    @Override
    public Set<URI> getDatasets() {
        Set<URI> res = new HashSet<>();
        JSONObject dsMap = documentProvider.getView("catalogue", "datasets");
        JSONArray rows = dsMap.getJSONArray("rows");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            res.add(URI.create(row.getString("key")));
        }
        return res;
    }

    @Override
    public URI getQueryEndpoint(URI dataset) {
        JSONObject typeMap = documentProvider.getView("catalogue", "datasets", dataset.toString());
        JSONArray rows = typeMap.getJSONArray("rows");
        if (rows.length() < 1) return null;
        return URI.create(rows.getJSONObject(0).getJSONObject("value").getString("service_url"));
    }

    @Override
    public Map<URI,URI> getQueryEndpointMap(URI... datasets) {
        Map<URI,URI> result = new HashMap<>();
        List<String> dss = new ArrayList<>();
        for (URI ds : datasets)
            dss.add(ds.toString());
        JSONObject typeMap = documentProvider.getView("catalogue", "datasets", dss.toArray(new String[0]));
        JSONArray rows = typeMap.getJSONArray("rows");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            result.put(URI.create(row.getString("key")),
                URI.create(row.getJSONObject("value").getString("service_url")));
        }
        return result;
    }

    @Override
    public Set<URI> getSupportingDatasets(URI type) {
        throw new NotImplementedException("NIY");
    }

}
