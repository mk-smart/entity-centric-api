package org.mksmart.ecapi.impl.script;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class SparqlWrapper extends ScriptableObject {

    private static class MyFunctionObject extends FunctionObject {

        /**
         * 
         */
        private static final long serialVersionUID = 5500354910506669170L;

        private MyFunctionObject(String name, Member methodOrConstructor, Scriptable parentScope) {
            super(name, methodOrConstructor, parentScope);
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            // return super.call(cx, scope, getParentScope(), args);
            return super.call(cx, scope, thisObj, args);
        }
    }

    // private static Map<String,String> geocodes = new HashMap<>();

    /**
     * 
     */
    private static final long serialVersionUID = -3739491542305374459L;

    public static ResultSetWrapper[] runSparql(String cosa, String dove) {
        SparqlWrapper scr = new SparqlWrapper();
        List<ResultSetWrapper> result = new ArrayList<>();
        Query rawQuery = QueryFactory.create(cosa, Syntax.syntaxARQ);
        QueryEngineHTTP httpQuery;
        httpQuery = new QueryEngineHTTP(dove.toString(), rawQuery);
        switch (rawQuery.getQueryType()) {
            case Query.QueryTypeSelect:
                httpQuery.setSelectContentType("application/sparql-results+json");
                try {
                    ResultSet res = httpQuery.execSelect();
                    while (res.hasNext()) {
                        Binding bind = res.nextBinding();
                        Map<String,String> mbind = new HashMap<>();
                        for (Iterator<Var> it = bind.vars(); it.hasNext();) {
                            Var var = it.next();
                            Node nv = bind.get(var);
                            if (nv.isLiteral()) mbind.put(var.getVarName(), nv.getLiteralValue().toString());
                            else if (nv.isURI()) mbind.put(var.getVarName(), nv.getURI());
                        }
                        result.add(scr.new ResultSetWrapper(mbind));
                    }
                } finally {
                    httpQuery.close();
                }
                break;
            default:
                SparqlWrapper.log
                        .error("Compiler function tried to issue a SPARQL query of unsupported type.");
        }
        return result.toArray(new ResultSetWrapper[0]);
    }

    // public static String[] getSparqlResults(String query) {
    // return new String[] {geocodes.get("ashland"), geocodes.get("beanhill")};
    // }

    private static Logger log = LoggerFactory.getLogger(SparqlWrapper.class);

    public SparqlWrapper() {
        // geocodes.put("ashland", "00MGE001");
        // geocodes.put("astwood", "00MGE002");
        // geocodes.put("bancroft", "00MGE003");
        // geocodes.put("bancroft_park", "00MGE004");
        // geocodes.put("beanhill", "00MGE005");
    }

    public void doit(ScriptableObject scope) {
        Context.enter();
        try {
            Scriptable scriptExecutionScope = new ImporterTopLevel(Context.getCurrentContext());
            scope.setParentScope(scriptExecutionScope);

            // -- Get a reference to the instance method this is to be made available in javascript as a
            // global function.
            Method jsMethod = SparqlWrapper.class.getMethod("runSparql", new Class[] {String.class,
                                                                                      String.class});
            // -- Choose a name to be used for invoking the above instance method from within javascript.
            String jsFname = "sparql";
            // -- Create the FunctionObject that binds the above function name to the instance method.
            FunctionObject bound = new MyFunctionObject(jsFname, jsMethod, scope);
            // -- Make it accessible within the scriptExecutionScope.
            scriptExecutionScope.put(jsFname, scriptExecutionScope, bound);

        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        } finally {
            Context.exit();
        }
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    class ResultSetWrapper extends ScriptableObject {
        private static final long serialVersionUID = 1L;

        public ResultSetWrapper(final Map<String,String> map) {
            for (Entry<String,String> e : map.entrySet()) {
                put(e.getKey(), this, e.getValue());
            }
        }

        @Override
        public String getClassName() {
            return getClassName();
        }
    }

}
