package org.mksmart.ecapi.impl.format;

import java.net.URI;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.api.Entity;

import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * A JSON serializer for that writes an entity in the following form:
 * 
 * <pre>
 * {
 *   "@id" : "&lt;global-uri&gt;",
 *   "&lt;attribute&gt;" : [ &lt;values&gt; ],
 *   ...
 * }
 * </pre>
 * 
 * Entity sets are serialized as JSON arrays of objects in the above form.
 * 
 * @author mdaquin <m.daquin@open.ac.uk>
 * 
 */
public class JsonSimplifiedGenericRepresentationSerializer extends JsonGenericRepresentationSerializer {

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
    public static JSONObject serialize(Entity e, URI name, boolean withAliases) {
        if (e == null) throw new IllegalArgumentException("Entity cannot be null.");
        JSONObject ob = new JSONObject();
        // Handle global identifier if present
        if (name != null) ob.put("@id", name.toString());
        // Handle attribute-value pairs
        for (Entry<URI,Set<RDFNode>> entry : e.getAttributes().entrySet()) {
            JSONArray bindings = new JSONArray();
            for (RDFNode val : entry.getValue())
                if (val != null) {
                    bindings.put(val.isResource() ? val.asResource().getURI() : val.asLiteral()
                            .getLexicalForm());
                }
            ob.put(entry.getKey().toString(), bindings);
        }
        for (Entry<URI,Set<Entity>> entry : e.getEAttributes().entrySet()) {
            JSONArray bindings = new JSONArray();
            for (Entity val : entry.getValue())
                if (val != null) {
                    JSONObject nb = serialize(val, (URI) null, withAliases);
                    bindings.put(nb);
                }
            ob.put(entry.getKey().toString(), bindings);
        }
        return ob;
    }

}
