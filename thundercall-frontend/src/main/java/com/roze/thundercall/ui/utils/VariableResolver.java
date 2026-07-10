package com.roze.thundercall.ui.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {{variable}} placeholders the Postman way: at SEND TIME, without
 * mutating what the user typed. The URL field keeps showing {{baseUrl}}/users
 * while the actual request goes out with the value substituted.
 *
 * See FIXES_GUIDE.md for the exact MainController integration (two small
 * edits: handleSendRequest and updateEnvironmentVariables).
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
            String value = variables.get(m.group(1));
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
        Map<String, String> out = new LinkedHashMap<>();
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