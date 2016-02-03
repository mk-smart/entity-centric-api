package org.mksmart.ecapi.impl.format;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.EntityFragment;
import org.mksmart.ecapi.api.id.GlobalURI;

import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * A JSON serializer for that writes an entity in the following form:
 * 
 * <pre>
 * {
 *   "@id" : "&lt;global-uri&gt;",
 *   "@aliases" : [ &lt;sameAs&gt; ],
 *   "@types" : [ &lt;types&gt; ],
 *   "&lt;attribute&gt;" : [ {
 *          "type" : "ref|terminal"
 *          "value" : &lt;value&gt;
 *      }, 
 *      ... 
 *   ],
 *   ...
 * }
 * </pre>
 * 
 * Entity sets are serialized as JSON arrays of objects in the above form.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class JsonGenericRepresentationSerializer {

    /**
     * Writes an {@link Entity} with an associated name, as the entity itself does not know its public ID.
     * 
     * @param e
     *            the entity to be serialized.
     * @param name
     *            the global identifier of the entity.
     * @return a serialized representation of the entity.
     */
    public static JSONObject serialize(Entity e, GlobalURI name) {
        return serialize(e, URI.create(name.toString()));
    }

    /**
     * Writes an {@link Entity} with an associated name, as the entity itself does not know its public ID.
     * 
     * @param e
     *            the entity to be serialized.
     * @param name
     *            the identifier of the entity.
     * @return a serialized representation of the entity.
     */
    public static JSONObject serialize(Entity e, URI name, boolean withAliases) {
        JSONObject ob = serialize((EntityFragment) e);
        // Handle global identifier if present
        if (name != null) ob.put("@id", name.toString());
        // Handle aliases
        if (withAliases) {
            JSONArray aliases = new JSONArray();
            for (URI alias : e.getAliases()) {
                Map<String,Object> temp = new HashMap<>();
                temp.put("type", "ref");
                temp.put("value", alias);
                aliases.put(temp);
            }
            ob.put("@aliases", aliases);
        }
        return ob;
    }

    public static JSONObject serialize(Entity e, URI name) {
        return serialize(e, name, false);
    }

    /**
     * Writes an {@link Entity} with an associated name, as the entity itself does not know its public ID.
     * 
     * @param e
     *            the entity to be serialized.
     * @param name
     *            the identifier of the entity.
     * @return a serialized representation of the entity.
     */
    public static JSONObject serialize(EntityFragment e) {
        if (e == null) throw new IllegalArgumentException("Entity cannot be null.");
        JSONObject ob = new JSONObject();
        // Handle types
        JSONArray types = new JSONArray();
        for (URI ty : e.getTypes()) {
            Map<String,Object> temp = new HashMap<>();
            temp.put("type", "ref");
            temp.put("value", ty);
            types.put(temp);
        }
        ob.put("@types", types);
        // Handle attribute-value pairs
        for (Entry<URI,Set<RDFNode>> entry : e.getAttributes().entrySet()) {
            JSONArray bindings = new JSONArray();
            for (RDFNode val : entry.getValue())
                if (val != null) {
                    Map<String,Object> temp = new HashMap<>();
                    temp.put("type", val.isResource() ? "ref" : "terminal");
                    temp.put("value", val.isResource() ? val.asResource().getURI() : val.asLiteral()
                            .getLexicalForm());
                    bindings.put(temp);
                }
            ob.put(entry.getKey().toString(), bindings);
        }
        for (Entry<URI,Set<Entity>> entry : e.getEAttributes().entrySet()) {
            JSONArray bindings = new JSONArray();
            for (Entity val : entry.getValue())
                if (val != null) {
                    JSONObject nb = serialize(val, (URI) null);
                    bindings.put(nb);
                }
            ob.put(entry.getKey().toString(), bindings);
        }
        return ob;
    }

    /**
     * Writes a set of {@link Entity} instenaces into a JSON array.
     * 
     * @param list
     *            the ID->Entity map to be serialized.
     * @return a JSOn array of the serialized entities.
     */
    public static JSONArray serialize(Map<?,Entity> list) {
        JSONArray result = new JSONArray();
        for (Entry<?,Entity> entry : list.entrySet()) {
            JSONObject serialized;
            if (entry.getKey() instanceof GlobalURI) serialized = serialize(entry.getValue(),
                (GlobalURI) entry.getKey());
            else if (entry.getKey() instanceof URI) serialized = serialize(entry.getValue(),
                (URI) entry.getKey());
            else serialized = serialize(entry.getValue(), (URI) null);
            result.put(serialized);
        }
        return result;
    }

}
