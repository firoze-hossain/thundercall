package com.roze.thundercall.api.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MockDynamicVariableResolver {

    private static final String[] FIRST_NAMES = {
            "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
            "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
            "Thomas", "Sarah", "Charles", "Karen", "Daniel", "Nancy", "Matthew", "Lisa",
            "Anthony", "Betty", "Mark", "Margaret", "Donald", "Sandra", "Steven", "Ashley",
            "Alex", "Emma", "Liam", "Sophia", "Noah", "Olivia", "Lucas", "Ava"
    };

    private static final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
            "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White"
    };

    private static final String[] CITIES = {
            "New York", "San Francisco", "London", "Tokyo", "Berlin", "Paris", "Toronto", "Sydney",
            "Singapore", "Amsterdam", "Stockholm", "Seoul", "Austin", "Chicago", "Dublin"
    };

    private static final String[] COUNTRIES = {
            "United States", "United Kingdom", "Germany", "France", "Japan", "Canada",
            "Australia", "Netherlands", "Sweden", "South Korea", "Ireland", "Switzerland"
    };

    private static final String[] COMPANIES = {
            "Acme Corp", "Globex Corporation", "Soylent Corp", "Initech", "Umbrella Corp",
            "Wayne Enterprises", "Stark Industries", "Cyberdyne Systems", "Hooli", "Pied Piper"
    };

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private MockDynamicVariableResolver() {}

    /**
     * Resolves dynamic tokens like {{$randomUUID}}, {{$randomEmail}}, as well as path variables
     * like {{id}} or :id captured from path parameters.
     */
    public static String resolve(String template, Map<String, String> pathVariables) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        String result = template;

        // Substitute path variables first (e.g. {{id}} or :id)
        if (pathVariables != null && !pathVariables.isEmpty()) {
            for (Map.Entry<String, String> entry : pathVariables.entrySet()) {
                String varName = entry.getKey();
                String val = Matcher.quoteReplacement(entry.getValue());
                result = result.replaceAll("(?i)\\{\\{" + Pattern.quote(varName) + "\\}\\}", val);
                result = result.replaceAll("(?i):" + Pattern.quote(varName) + "\\b", val);
            }
        }

        Matcher matcher = TOKEN_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String token = matcher.group(1).trim();
            String replacement = resolveToken(token);
            if (replacement != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String resolveToken(String token) {
        String key = token.toLowerCase(Locale.ROOT);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        switch (key) {
            case "$guid":
            case "$randomuuid":
                return UUID.randomUUID().toString();
            case "$randomfirstname":
                return FIRST_NAMES[rnd.nextInt(FIRST_NAMES.length)];
            case "$randomlastname":
                return LAST_NAMES[rnd.nextInt(LAST_NAMES.length)];
            case "$randomfullname":
                return FIRST_NAMES[rnd.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[rnd.nextInt(LAST_NAMES.length)];
            case "$randomemail": {
                String fn = FIRST_NAMES[rnd.nextInt(FIRST_NAMES.length)].toLowerCase(Locale.ROOT);
                String ln = LAST_NAMES[rnd.nextInt(LAST_NAMES.length)].toLowerCase(Locale.ROOT);
                return fn + "." + ln + rnd.nextInt(10, 99) + "@example.com";
            }
            case "$randomint":
                return String.valueOf(rnd.nextInt(1, 1000));
            case "$randomprice":
                return String.format(Locale.US, "%.2f", rnd.nextDouble(5.0, 999.0));
            case "$randomphonenumber":
                return String.format("+1-555-%04d", rnd.nextInt(1000, 9999));
            case "$randomboolean":
                return rnd.nextBoolean() ? "true" : "false";
            case "$timestamp":
                return String.valueOf(Instant.now().getEpochSecond());
            case "$isotimestamp":
                return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            case "$randomcity":
                return CITIES[rnd.nextInt(CITIES.length)];
            case "$randomcountry":
                return COUNTRIES[rnd.nextInt(COUNTRIES.length)];
            case "$randomcompanyname":
                return COMPANIES[rnd.nextInt(COMPANIES.length)];
            default:
                return null;
        }
    }
}
