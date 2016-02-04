package org.mksmart.ecapi.web.format;

import java.net.URI;
import java.util.SortedSet;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.provenance.PropertyPath;
import org.mksmart.ecapi.web.util.UriRewriter;

import com.hp.hpl.jena.vocabulary.RDF;

public class JsonProvenanceDataSerializer {

    public static JSONObject serialize(Entity e, GlobalURI name) {
        return serialize(e, name, (UriRewriter) null);
    }

    public static JSONObject serialize(Entity e, GlobalURI name, UriRewriter rewriter) {
        JSONObject mainObj = new JSONObject();
        JSONArray jProv = new JSONArray();

        mainObj.put("@id", name);
        mainObj.put("description", "Provenance information for properties of entity <" + name + ">");
        for (String ds : e.getProvenanceMap().keySet()) {
            JSONObject jProvT = new JSONObject();
            SortedSet<String> propSort = new TreeSet<>();
            jProvT.put("dataset", ds);
            for (PropertyPath path : e.getContributedProperties(ds)) {
                String spath = "";
                for (URI prop : path) {
                    String s = prop.toString();
                    if (!("@types".equals(s) || RDF.type.getURI().equals(s))) {
                        if (!spath.isEmpty()) spath += "/";
                        spath += rewriter == null ? ("<" + s + ">") : rewriter.rewriteProperty(prop);
                    }
                }
                if (!spath.isEmpty()) propSort.add(spath);
            }
            jProvT.put("attributes", new JSONArray(propSort));
            jProv.put(jProvT);
        }
        mainObj.put("provenance", jProv);
        return mainObj;
    }

}
