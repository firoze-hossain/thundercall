package com.roze.thundercall.api.util;

import java.util.*;

public final class MockPathMatcher {

    public record MatchResult(boolean matched, Map<String, String> pathVariables, int specificity) {}

    private MockPathMatcher() {}

    /**
     * Strips query string from path, e.g. "/users/1?foo=bar" -> "/users/1".
     */
    public static String extractPathWithoutQuery(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "/";
        }
        int qIdx = rawPath.indexOf('?');
        String clean = qIdx >= 0 ? rawPath.substring(0, qIdx) : rawPath;
        clean = clean.trim();
        if (!clean.startsWith("/")) {
            clean = "/" + clean;
        }
        while (clean.length() > 1 && clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    /**
     * Matches request path against a route pattern like "/users/:id" or "/users/{id}" or "/users/*".
     * Returns MatchResult with matched flag, extracted path variables, and specificity score (higher = more specific).
     */
    public static MatchResult match(String pattern, String requestPath) {
        String cleanPattern = extractPathWithoutQuery(pattern);
        String cleanPath = extractPathWithoutQuery(requestPath);

        // Exact match (highest specificity)
        if (cleanPattern.equals(cleanPath)) {
            return new MatchResult(true, Collections.emptyMap(), 1000 + cleanPattern.length());
        }

        String[] patternSegments = cleanPattern.split("/");
        String[] pathSegments = cleanPath.split("/");

        // Global wildcard
        if ("/*".equals(cleanPattern)) {
            return new MatchResult(true, Collections.emptyMap(), 1);
        }

        if (cleanPattern.endsWith("/*")) {
            String prefix = cleanPattern.substring(0, cleanPattern.length() - 2);
            if (cleanPath.startsWith(prefix)) {
                return new MatchResult(true, Collections.emptyMap(), 10 + prefix.length());
            }
        }

        if (patternSegments.length != pathSegments.length) {
            return new MatchResult(false, Collections.emptyMap(), 0);
        }

        Map<String, String> variables = new LinkedHashMap<>();
        int specificity = 100;

        for (int i = 0; i < patternSegments.length; i++) {
            String pSeg = patternSegments[i];
            String rSeg = pathSegments[i];

            if (pSeg.equals(rSeg)) {
                specificity += 10;
                continue;
            }

            // Postman style :param or OpenAPI style {param}
            if (pSeg.startsWith(":") && pSeg.length() > 1) {
                String varName = pSeg.substring(1);
                variables.put(varName, rSeg);
                specificity += 5;
            } else if (pSeg.startsWith("{") && pSeg.endsWith("}") && pSeg.length() > 2) {
                String varName = pSeg.substring(1, pSeg.length() - 1);
                variables.put(varName, rSeg);
                specificity += 5;
            } else if ("*".equals(pSeg)) {
                specificity += 1;
            } else {
                return new MatchResult(false, Collections.emptyMap(), 0);
            }
        }

        return new MatchResult(true, variables, specificity);
    }
}
