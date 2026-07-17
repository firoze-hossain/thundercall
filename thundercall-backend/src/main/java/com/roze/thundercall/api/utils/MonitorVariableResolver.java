package com.roze.thundercall.api.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves {{variable}} placeholders against an environment's stored
 * variables. Deliberately simple — monitors run unattended on a
 * schedule with no user present, so there's no equivalent of the
 * frontend's "show unresolved variables and ask" flow; an unresolved
 * placeholder is just left as-is and the request will fail naturally
 * if that breaks the URL, same as hitting a literal "{{token}}" in a
 * browser would. */
public class MonitorVariableResolver {
    private static final Pattern PATTERN = Pattern.compile("\\{\\{(.+?)}}");

    public static String resolve(String text, Map<String, String> variables) {
        if (text == null || text.isEmpty() || variables == null || variables.isEmpty()) {
            return text;
        }
        Matcher matcher = PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = variables.get(key);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value != null ? value : matcher.group()));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}