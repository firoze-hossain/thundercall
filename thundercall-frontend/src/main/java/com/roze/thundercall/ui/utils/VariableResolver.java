package com.roze.thundercall.ui.utils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {{variable}} placeholders the Postman way: at SEND TIME,
 * without mutating what the user typed.
 *
 * The current implementation in MainController.updateEnvironmentVariables()
 * rewrites the URL field text when an environment is selected. That is
 * destructive: once {{baseUrl}} is replaced with http://localhost:8080,
 * switching environments no longer works and the saved request loses its
 * template. Instead, keep the template in the UI and resolve copies of the
 * url / headers / body just before building the ApiRequest:
 *
 *   Map<String,String> vars = currentEnvironmentVariables(); // may be empty
 *   String url    = VariableResolver.resolve(buildFullUrl(), vars);
 *   String body   = VariableResolver.resolve(buildRequestBody(), vars);
 *   Map<String,String> headers = VariableResolver.resolveMap(buildHeaders(), vars);
 *
 *   Set<String> missing = VariableResolver.findUnresolved(url, vars);
 *   if (!missing.isEmpty()) {
 *       AlertUtils.showError("Unresolved variables: " + String.join(", ", missing));
 *       return;
 *   }
 */
public final class VariableResolver {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([\\w.\\-]+)\\s*}}");

    private VariableResolver() {
    }

    /** Replaces every {{name}} found in {@code input} with its value from {@code variables}. */
    public static String resolve(String input, Map<String, String> variables) {
        if (input == null || input.isEmpty() || variables == null || variables.isEmpty()) {
            return input;
        }
        Matcher m = VARIABLE.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String value = variables.get(name);
            // Leave unknown variables untouched so findUnresolved can report them
            m.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : m.group(0)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Resolves variables in both keys and values of a header/param map. */
    public static Map<String, String> resolveMap(Map<String, String> map, Map<String, String> variables) {
        if (map == null || map.isEmpty()) {
            return map;
        }
        Map<String, String> out = new java.util.LinkedHashMap<>();
        map.forEach((k, v) -> out.put(resolve(k, variables), resolve(v, variables)));
        return out;
    }

    /** Returns the names of variables referenced in {@code input} that have no value. */
    public static Set<String> findUnresolved(String input, Map<String, String> variables) {
        if (input == null || input.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> missing = new LinkedHashSet<>();
        Matcher m = VARIABLE.matcher(input);
        while (m.find()) {
            String name = m.group(1);
            if (variables == null || !variables.containsKey(name)) {
                missing.add(name);
            }
        }
        return missing;
    }
}