package org.mksmart.ecapi.impl.format;

import java.net.URI;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.EntityFragment;
import org.mksmart.ecapi.impl.EntityImpl;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class JsonGenericRepresentationDeserializer {

    public static EntityFragment deserialize(JSONObject json) {
        return deserialize(json, false);
    }

    /**
     * 
     * @param json
     * @param isEncapsulated
     *            If true, the parser will assume the actual entity data are included in a field called
     *            "global-representation".
     * @return
     */
    public static EntityFragment deserialize(JSONObject json, boolean isEncapsulated) {
        if (json == null) throw new IllegalArgumentException("Nothing to parse on a null JSON object");
        EntityFragment result = new EntityImpl();
        if (isEncapsulated && result instanceof Entity && json.has("equivalents")) {
            JSONArray eqs = json.getJSONArray("equivalents");
            for (int i = 0; i < eqs.length(); i++)
                ((Entity) result).addAlias(URI.create(eqs.getString(i)));
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
                    if (!(v instanceof JSONObject)) throw new IllegalStateException(
                            "Format error. Expected a " + JSONObject.class.getCanonicalName() + ", got a "
                                    + v.getClass().getCanonicalName());
                    if (!((JSONObject) v).has("type")) {
                        // It's an expanded query!
                        EntityFragment eff = deserialize((JSONObject) v, false);
                        if (eff instanceof Entity) result.addValue(property, (Entity) eff);

                        // for ( Object nestedProperty : ((JSONObject) v).keySet() ) {
                        // if (nestedProperty instanceof String) {
                        // URI p2 = URI.create((String) nestedProperty);
                        // }
                        // }
                        // throw new NotImplementedException("Deserialisation of expanded queries NIY");
                    } else {
                        String nature = ((JSONObject) v).getString("type");
                        Object actualValue = ((JSONObject) v).get("value");
                        if ("ref".equals(nature)) {
                            if (actualValue instanceof JSONObject) {
                                EntityFragment eff = deserialize((JSONObject) actualValue, false);
                                if (eff instanceof Entity) result.addValue(property, (Entity) eff);
                            } else result.addValue(property,
                                ResourceFactory.createResource((String) actualValue));
                        } else if ("terminal".equals(nature)) {
                            result.addValue(property,
                                ResourceFactory.createPlainLiteral((String) actualValue));
                        }
                    }
                }
            }
        return result;
    }

}
