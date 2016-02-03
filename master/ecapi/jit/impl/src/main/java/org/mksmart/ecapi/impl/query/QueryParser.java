package org.mksmart.ecapi.impl.query;

import java.net.URI;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mksmart.ecapi.api.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for queries in a (regular) template language.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class QueryParser {

    protected static Pattern grammar = Pattern
            .compile("^([_\\w]+)\\(([\\[?\\w\\]?]+)(,(\\w+))?(,(\\w+))?\\)$");

    //            .compile("^ ([_\\w]+) \\( ([\\[?\\w\\]?]+) (,(\\w+))?(,(\\w+))?\\)$");

    private static Logger log = LoggerFactory.getLogger(QueryParser.class);

    public static Query parse(String template, URI entityId) {
        template = template.replace(" ", "");
        Matcher m = grammar.matcher(template);
        if (!m.find()) throw new IllegalArgumentException("Failed to parse query template.");
        log.trace("Found {} groups.", m.groupCount());
        if (m.groupCount() < 3) throw new IllegalArgumentException(
                "Failed to parse query template: not enough matches found to compile an expression.");
        String predicate = m.group(1);
        log.trace(" ... predicate = {}", predicate);
        String target = m.group(2);
        log.trace(" ... target = {}", target);
        if (!"[LURI]".equalsIgnoreCase(target)) throw new UnsupportedOperationException(
                "Only query templates whose first argument is [LURI] are supported at the moment.");
        List<String> varnames = new Vector<>();
        for (int i = 4; i <= m.groupCount(); i += 2) {
            log.trace(" ... {}", m.group(i));
            varnames.add(m.group(i));
        }
        if ("resolve".equalsIgnoreCase(predicate)) return new RdfDereferenceQuery(entityId);
        else if ("sparql_select".equalsIgnoreCase(predicate)) return new SparqlQuery(
                Query.Type.SPARQL_SELECT, entityId);
        else if ("sparql_describe".equalsIgnoreCase(predicate)) return new SparqlQuery(
                Query.Type.SPARQL_DESCRIBE, entityId);
        throw new UnsupportedOperationException("Predicate " + predicate + " is not supported.");
    }

}
