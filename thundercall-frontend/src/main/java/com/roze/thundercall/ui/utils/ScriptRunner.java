package com.roze.thundercall.ui.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the most common Postman script patterns WITHOUT a JavaScript engine.
 * It understands the pm.* calls people actually paste from Postman:
 *
 *   const responseData = pm.response.json().data;
 *   pm.environment.set("ACCESS_TOKEN", responseData.token);
 *   pm.environment.set("REFRESH_TOKEN", pm.response.json().data.refreshToken);
 *   pm.environment.set("HOST", "http://localhost:8080");
 *   pm.environment.set("COPY", pm.environment.get("OTHER"));
 *   pm.environment.unset("OLD");
 *   console.log(...);            // captured to the log, not executed
 *
 * Anything it can't parse is reported in the log and skipped — the request
 * itself never fails because of a script.
 */
public final class ScriptRunner {

    public static final class Result {
        public final Map<String, String> setVariables = new LinkedHashMap<>();
        public final List<String> unsetVariables = new ArrayList<>();
        public final List<String> log = new ArrayList<>();
    }

    private static final Pattern CONST_ALIAS = Pattern.compile(
            "^(?:const|let|var)\\s+(\\w+)\\s*=\\s*pm\\.response\\.json\\(\\)((?:\\.[\\w\\[\\]0-9]+)*)\\s*;?$");
    private static final Pattern ENV_SET = Pattern.compile(
            "^pm\\.environment\\.set\\(\\s*[\"']([^\"']+)[\"']\\s*,\\s*(.+?)\\s*\\)\\s*;?$");
    private static final Pattern ENV_UNSET = Pattern.compile(
            "^pm\\.environment\\.unset\\(\\s*[\"']([^\"']+)[\"']\\s*\\)\\s*;?$");
    private static final Pattern ENV_GET = Pattern.compile(
            "^pm\\.environment\\.get\\(\\s*[\"']([^\"']+)[\"']\\s*\\)$");
    private static final Pattern RESPONSE_JSON = Pattern.compile(
            "^pm\\.response\\.json\\(\\)((?:\\.[\\w\\[\\]0-9]+)*)$");

    private ScriptRunner() {
    }

    /**
     * @param script       raw script text (may be null/blank)
     * @param responseBody HTTP response body, or null for pre-request scripts
     * @param statusCode   HTTP status, or -1 when not available
     * @param environment  current environment variables (read for pm.environment.get)
     */
    public static Result run(String script, String responseBody, int statusCode,
                             Map<String, String> environment) {
        Result result = new Result();
        if (script == null || script.isBlank()) {
            return result;
        }
        Map<String, Object> aliases = new LinkedHashMap<>();
        Object responseJson = tryParse(responseBody);

        for (String rawLine : script.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("//")) {
                continue;
            }
            try {
                if (line.startsWith("console.log")) {
                    result.log.add("log: " + line);
                    continue;
                }
                Matcher alias = CONST_ALIAS.matcher(line);
                if (alias.matches()) {
                    if (responseJson == null) {
                        result.log.add("skipped (no response yet): " + line);
                        continue;
                    }
                    aliases.put(alias.group(1), resolvePath(responseJson, alias.group(2)));
                    continue;
                }
                Matcher unset = ENV_UNSET.matcher(line);
                if (unset.matches()) {
                    result.unsetVariables.add(unset.group(1));
                    result.log.add("unset " + unset.group(1));
                    continue;
                }
                Matcher set = ENV_SET.matcher(line);
                if (set.matches()) {
                    String key = set.group(1);
                    Object value = evaluate(set.group(2).trim(), aliases, responseJson,
                            statusCode, environment);
                    if (value == null) {
                        result.log.add("skipped (value not found): " + line
                                + availableKeysHint(set.group(2).trim(), aliases, responseJson));
                    } else {
                        result.setVariables.put(key, String.valueOf(value));
                        result.log.add("set " + key + " = " + abbreviate(String.valueOf(value)));
                    }
                    continue;
                }
                result.log.add("unsupported line skipped: " + abbreviate(line));
            } catch (Exception e) {
                result.log.add("error on line \"" + abbreviate(line) + "\": " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * When a path fails ("responseData.token" but the API calls it
     * "accessToken"), tell the user what keys DO exist at the deepest
     * resolvable point — the single most useful script diagnostic.
     */
    private static String availableKeysHint(String expr, Map<String, Object> aliases,
                                            Object responseJson) {
        Object base = null;
        String path = null;
        Matcher resp = RESPONSE_JSON.matcher(expr);
        if (resp.matches()) {
            base = responseJson;
            path = resp.group(1);
        } else {
            int dot = expr.indexOf('.');
            String head = dot < 0 ? expr : expr.substring(0, dot);
            if (aliases.containsKey(head)) {
                base = aliases.get(head);
                path = dot < 0 ? "" : expr.substring(dot);
            }
        }
        if (base == null) {
            return "";
        }
        Object current = base;
        StringBuilder walked = new StringBuilder();
        if (path != null) {
            for (String part : path.split("\\.")) {
                if (part.isEmpty()) {
                    continue;
                }
                Object next = resolvePath(current, "." + part);
                if (next == null) {
                    break;
                }
                current = next;
                walked.append('.').append(part);
            }
        }
        if (current instanceof JSONObject) {
            return "  →  available keys" + (walked.length() > 0 ? " at '" + walked + "'" : "")
                    + ": " + ((JSONObject) current).keySet();
        }
        if (current instanceof JSONArray) {
            return "  →  found an array (length " + ((JSONArray) current).length()
                    + ") — use [0] etc.";
        }
        return "";
    }

    private static Object evaluate(String expr, Map<String, Object> aliases, Object responseJson,
                                   int statusCode, Map<String, String> environment) {
        // String literal
        if ((expr.startsWith("\"") && expr.endsWith("\"") && expr.length() >= 2)
                || (expr.startsWith("'") && expr.endsWith("'") && expr.length() >= 2)) {
            return expr.substring(1, expr.length() - 1);
        }
        // Number / boolean literal
        String lower = expr.toLowerCase(Locale.ROOT);
        if (lower.equals("true") || lower.equals("false") || expr.matches("-?\\d+(\\.\\d+)?")) {
            return expr;
        }
        if (expr.equals("pm.response.code") || expr.equals("pm.response.status")) {
            return statusCode >= 0 ? String.valueOf(statusCode) : null;
        }
        if (expr.equals("pm.response.text()")) {
            return responseJson != null ? responseJson.toString() : null;
        }
        Matcher get = ENV_GET.matcher(expr);
        if (get.matches()) {
            return environment != null ? environment.get(get.group(1)) : null;
        }
        Matcher resp = RESPONSE_JSON.matcher(expr);
        if (resp.matches()) {
            return responseJson == null ? null : resolvePath(responseJson, resp.group(1));
        }
        // alias(.path)*  e.g. responseData.token
        int dot = expr.indexOf('.');
        String head = dot < 0 ? expr : expr.substring(0, dot);
        if (aliases.containsKey(head)) {
            Object base = aliases.get(head);
            return dot < 0 ? base : resolvePath(base, expr.substring(dot));
        }
        return null;
    }

    /** Walks ".data.token" or ".items[0].id" style paths through org.json values. */
    private static Object resolvePath(Object root, String path) {
        Object current = root;
        if (path == null || path.isEmpty()) {
            return current;
        }
        for (String part : path.split("\\.")) {
            if (part.isEmpty() || current == null) {
                continue;
            }
            String name = part;
            Integer index = null;
            int bracket = part.indexOf('[');
            if (bracket >= 0 && part.endsWith("]")) {
                name = part.substring(0, bracket);
                index = Integer.parseInt(part.substring(bracket + 1, part.length() - 1));
            }
            if (!name.isEmpty()) {
                if (!(current instanceof JSONObject) || !((JSONObject) current).has(name)) {
                    return null;
                }
                current = ((JSONObject) current).get(name);
            }
            if (index != null) {
                if (!(current instanceof JSONArray) || ((JSONArray) current).length() <= index) {
                    return null;
                }
                current = ((JSONArray) current).get(index);
            }
        }
        return current;
    }

    private static Object tryParse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        String trimmed = body.trim();
        try {
            if (trimmed.startsWith("{")) {
                return new JSONObject(trimmed);
            }
            if (trimmed.startsWith("[")) {
                return new JSONArray(trimmed);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String abbreviate(String s) {
        return s.length() > 60 ? s.substring(0, 57) + "..." : s;
    }
}