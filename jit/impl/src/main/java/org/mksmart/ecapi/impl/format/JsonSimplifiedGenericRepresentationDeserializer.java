package org.mksmart.ecapi.impl.format;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.id.CanonicalGlobalURI;
import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.id.ScopedGlobalURI;
import org.mksmart.ecapi.impl.EntityImpl;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class JsonSimplifiedGenericRepresentationDeserializer {

    public static GlobalURI parseEntityId(JSONObject obj) {
        if (obj == null) throw new IllegalArgumentException("Nothing to parse on a null JSON object");
        if (obj.has("@id")) {
            String atid = obj.getString("@id");
            try {
                return ScopedGlobalURI.parse(atid);
            } catch (Exception ex) {
                try {
                    GlobalURI gu = CanonicalGlobalURI.parse(atid);
                    return gu;
                } catch (Exception ex2) {
                    return null;
                }
            }

        }
        return null;
    }

    public static Entity deserialize(JSONObject json) {
        return deserialize(json, false);
    }

    public static Entity deserialize(JSONObject json, boolean isEncapsulated) {
        if (json == null) throw new IllegalArgumentException("Nothing to parse on a null JSON object");
        Entity result = new EntityImpl();
        if (isEncapsulated && json.has("equivalents")) {
            JSONArray eqs = json.getJSONArray("equivalents");
            for (int i = 0; i < eqs.length(); i++)
                result.addAlias(URI.create(eqs.getString(i)));
        }
        JSONObject repr = isEncapsulated && json.has("global-representation") ? json
                .getJSONObject("global-representation") : json;
        for (Object k : repr.keySet())
            if (k instanceof String) {
                URI property = URI.create((String) k);
                Object kv = repr.get((String) k);
                if (!(kv instanceof JSONArray)) continue;
                JSONArray valuez = (JSONArray) kv;
                for (int i = 0; i < valuez.length(); i++) {
                    Object v = valuez.get(i);
                    if (v instanceof JSONObject) {
                        result.addValue(property, deserialize((JSONObject) v));
                    } else {
                        RDFNode nodeVal;
                        // FIXME THIS IS NOT THE WAY I WANT TO DISCOVER IF A CACHED VALUE IS A RESOURCE OR
                        // LITERAL, FFS! (alexdma)
                        if (v instanceof String) try {
                            new URL((String) v);
                            nodeVal = ResourceFactory.createResource((String) v);
                        } catch (MalformedURLException ex) {
                            nodeVal = ResourceFactory.createPlainLiteral((String) v);
                        }
                        else nodeVal = ResourceFactory.createPlainLiteral(v.toString());
                        result.addValue(property, nodeVal);
                    }
                }
            }
        return result;
    }

}
