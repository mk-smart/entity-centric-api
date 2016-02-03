package org.mksmart.ecapi.couchdb.id;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mksmart.ecapi.api.Entity;
import org.mksmart.ecapi.api.GlobalType;
import org.mksmart.ecapi.api.id.CanonicalGlobalURI;
import org.mksmart.ecapi.api.id.GlobalURI;
import org.mksmart.ecapi.api.id.IdGenerator;
import org.mksmart.ecapi.api.id.ScopedGlobalURI;
import org.mksmart.ecapi.commons.couchdb.client.DocumentProvider;
import org.mksmart.ecapi.impl.GlobalTypeImpl;
import org.mksmart.ecapi.impl.script.ScriptUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * A factory for {@link GlobalURI} objects that uses executable code provided by a {@link DocumentProvider} in
 * order to generate global identifiers.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class ProgrammaticGlobalURIGenerator implements IdGenerator<GlobalURI,Entity> {

    /**
     * The namespace document, which is cached until a refresh is forced.
     */
    private JSONObject docNS;

    private DocumentProvider<JSONObject> documentProvider;

    private Logger log = LoggerFactory.getLogger(getClass());

    public ProgrammaticGlobalURIGenerator(DocumentProvider<JSONObject> documentProvider) {
        this.documentProvider = documentProvider;
        refresh();
    }

    @Override
    public GlobalURI createId(Entity e) {
        URI uType = URI.create(RDF.type.getURI());
        String transformed = null;
        String program = null;
        Set<String> candidateTypes = new HashSet<>();
        // Candidates are the rdf:types of the entity representation
        for (Object tNode : e.getValues(uType))
            if (tNode instanceof RDFNode && ((RDFNode) tNode).isURIResource()) candidateTypes
                    .add(((RDFNode) tNode).asResource().getURI());

        for (String s : candidateTypes)
            log.debug(" ... Found candidate type '{}'", s);
        // match them with global

        JSONObject typeL2G = documentProvider.getView("type", "short", candidateTypes.toArray(new String[0]));

        Set<String> candidateGTypes = new HashSet<>();
        JSONArray rows = typeL2G.getJSONArray("rows");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            candidateGTypes.add(row.getString("value"));
        }

        JSONObject typeMap = documentProvider.getView("compile", "typemaps",
            candidateGTypes.toArray(new String[0]));

        // Scan (already filtered) type mappings
        rows = typeMap.getJSONArray("rows");
        for (int i = 0; i < rows.length() && program == null; i++) {
            JSONObject row = rows.getJSONObject(i);
            String ty = row.getString("key");
            log.debug("Trying type {}", ty);
            program = row.getJSONObject("value").getString("globalise");
        }
        if (program == null) { // Fall back to supertype/default
            GlobalType gt = new GlobalTypeImpl(GlobalType.TOP_URI);
            log.debug("Falling back to default global type <{}>", gt);
            JSONObject doc = documentProvider.getDocument(gt.getId().toString());
            program = doc.getString("globalise");
        }
        if (program != null) for (Iterator<URI> it = e.getAliases().iterator(); it.hasNext()
                                                                                && transformed == null;) {
            URI alias = it.next();
            log.trace("Attempting conversion");
            log.trace(" ... alias: {}", alias);
            log.trace(" ... program: \n{}", program);
            Object deal = ScriptUtils.runJs(program, "globalise", new Object[] {alias.toString()},
                String.class);
            transformed = (String) deal;
        }
        if (transformed == null) {
            log.warn("Global URI generation failed. Microcompilers returned nothing.");
            if (!e.getAliases().isEmpty()) log.warn("Aliases for failing entity were:");
            for (URI alias : e.getAliases())
                log.warn(" * <{}>", alias);
            throw new RuntimeException("Attempt at creating global URI returned nothing.");
        }
        log.debug("Globalised as {}", transformed);
        try {
            return ScopedGlobalURI.parse(transformed);
        } catch (Exception ex) {
            log.debug("Could not get a scoped global URI from <{}>", transformed);
            try {
                GlobalURI gu = CanonicalGlobalURI.parse(transformed);
                log.trace("Returning canonical URI {}", gu);
                return gu;
            } catch (Exception ex2) {
                log.warn("Could not get a canonical global URI from <{}>", transformed);
                return null;
            }
        }
    }

    @Override
    public String createIdFromUri(URI uri) {
        log.trace("Requested mapping-based rewrite of URI <{}>", uri);
        String func_pattern = "function\\s+rewrite\\s*\\(.*\\)\\s*\\{.*\\}";
        String suri = uri.toString(), u = null;
        if (this.docNS != null) {
            for (String field : JSONObject.getNames(this.docNS)) {
                log.trace(" ... checking matches for pattern '{}'", field);
                Pattern p = Pattern.compile(field);
                Matcher m = p.matcher(suri);
                if (m.matches()) {
                    log.trace("Found!");
                    String rewriter = this.docNS.getString(field);
                    log.trace("Rewriter: {}", rewriter);
                    if (rewriter.matches(func_pattern)) {
                        log.trace("Function found for {}", field);
                        Vector<Object> matches = new Vector<>();
                        for (int i = 1; i <= m.groupCount(); i++)
                            matches.add(m.group(i));
                        Object deal = ScriptUtils.runJs(rewriter, "rewrite", matches.toArray(), String.class);
                        u = (String) deal;
                    } else {
                        u = suri.replaceAll(field, rewriter);
                        log.trace(" ... match found. Rewritten as <{}>", u);
                    }
                    break;
                }
            }
        }
        return u;
    }

    @Override
    public String createPropertyId(URI localProperty) {
        log.trace("Globalising local property <{}>", localProperty);
        String s = localProperty.toString();
        if (this.docNS == null || !this.docNS.has("mks:prefixes")) return s;
        JSONObject prefixes = this.docNS.getJSONObject("mks:prefixes");
        String[] patterns = new String[] {"^([^#?]+#)(.+)$", "^(.+/)([^/?]+)$"};
        String shortname = s, ns = s;
        for (int i = 0; i < patterns.length && shortname == ns; i++) {
            Matcher m = Pattern.compile(patterns[i]).matcher(s);
            if (m.matches()) {
                ns = m.group(1);
                shortname = m.group(2);
            }
        }
        // String shortname = s.replaceFirst(".*[/#]([^/?]+).*", "$1");
        // String ns = s.replaceAll("(.*[/#])[^/?]+$", "$1");
        log.trace("shortname = {}", shortname);
        log.trace("namespace = {}", ns);
        String prefix = prefixes.has(ns) ? prefixes.getString(ns) : "global";
        log.trace("<== globalised to <{}>", prefix + ':' + shortname);
        return prefix + ':' + shortname;
    }

    @Override
    public void refresh() {
        this.docNS = this.documentProvider.getDocument("_design/namespace");
    }
}
