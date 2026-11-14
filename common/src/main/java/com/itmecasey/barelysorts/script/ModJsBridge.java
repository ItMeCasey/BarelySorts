package com.itmecasey.barelysorts.script;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

/**
 * Gives ModJS scripts a global {@code BarelySorts} (the {@link ScriptApi}). ModJS is optional, so everything here goes
 * through reflection: ModJS offers {@code ExtraBindings.register(BindingProvider)} and the provider's
 * {@code install(type, cx, scope)} adds globals to every script scope. Without ModJS nothing is loaded and nothing
 * fails. The global is only added to startup and server scripts; sorting is decided on the server.
 */
public final class ModJsBridge {
    private static final Logger LOG = Logger.getLogger("barelysorts");
    private static boolean installed;

    private ModJsBridge() {
    }

    /** Installs the binding when ModJS is on the classpath; returns whether it did. Safe to call more than once. */
    public static synchronized boolean install() {
        if (installed) {
            return true;
        }
        try {
            Class<?> provider = Class.forName("com.itmecasey.modjs.script.BindingProvider");
            Class<?> extra = Class.forName("com.itmecasey.modjs.script.ExtraBindings");
            Class<?> context = Class.forName("org.mozilla.javascript.Context");
            Class<?> scriptable = Class.forName("org.mozilla.javascript.Scriptable");
            Class<?> scriptableObject = Class.forName("org.mozilla.javascript.ScriptableObject");
            Method javaToJs = context.getMethod("javaToJS", Object.class, scriptable);
            Method put = scriptableObject.getMethod("putProperty", scriptable, String.class, Object.class);
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("install") && args != null && args.length == 3) {
                    String type = String.valueOf(args[0]).toLowerCase(java.util.Locale.ROOT);
                    if (type.equals("client")) {
                        return null;
                    }
                    // Loading this script type again replaces what it registered before.
                    SortScripts.clearOwner(type);
                    put.invoke(null, args[2], "BarelySorts", javaToJs.invoke(null, new ScriptApi(type), args[2]));
                    return null;
                }
                if (method.getName().equals("toString")) {
                    return "BarelySorts binding";
                }
                if (method.getName().equals("hashCode")) {
                    return System.identityHashCode(proxy);
                }
                if (method.getName().equals("equals")) {
                    return proxy == args[0];
                }
                return null;
            };
            Object binding = Proxy.newProxyInstance(provider.getClassLoader(), new Class<?>[] {provider}, handler);
            extra.getMethod("register", provider).invoke(null, binding);
            installed = true;
            LOG.info("Barely Sorts: ModJS scripts can use the BarelySorts global");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.warning("Barely Sorts: could not add the BarelySorts global to ModJS: " + e);
            return false;
        }
    }

    /** For tests: forget that the binding was installed. */
    static synchronized void reset() {
        installed = false;
    }
}
