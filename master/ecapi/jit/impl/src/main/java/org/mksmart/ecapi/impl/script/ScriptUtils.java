package org.mksmart.ecapi.impl.script;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rhino stuff etc. goes here.
 * 
 * @author alessandro <alexdma@apache.org>
 * 
 */
public class ScriptUtils {

    private static Logger log = LoggerFactory.getLogger(ScriptUtils.class);

    public static <C> Object runJs(String program, String func, Object[] args, Class<C> returnType) {
        return runJs(program, func, args, returnType, false);
    }

    public static <C> Object runJs(String program,
                                   String func,
                                   Object[] args,
                                   Class<C> returnType,
                                   boolean silent) {
        final Context context = Context.enter();
        Object result;
        try {
            final ScriptableObject scope = context.initStandardObjects();
            new SparqlWrapper().doit(scope);
            context.evaluateString(scope, program, "script", 1, null);
            Function fct = (Function) scope.get(func, scope);
            result = fct.call(context, scope, scope, args);
            return Context.jsToJava(result, returnType);
        } catch (Exception ex) {
            if (silent) {
                log.warn("Exception caught while executing microcompiler."
                         + " Will handle silently but return null object.");
                return null;
            } else throw ex;
        } finally {
            Context.exit();
        }
    }

}
