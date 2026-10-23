package com.roze.thundercall.api.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockDynamicVariableResolverTest {

    @Test
    void testResolveStandardTokens() {
        String template = "{\"id\": \"{{$guid}}\", \"email\": \"{{$randomEmail}}\", \"name\": \"{{$randomFullName}}\"}";
        String resolved = MockDynamicVariableResolver.resolve(template, Map.of());

        assertFalse(resolved.contains("{{$guid}}"));
        assertFalse(resolved.contains("{{$randomEmail}}"));
        assertFalse(resolved.contains("{{$randomFullName}}"));
        assertTrue(resolved.contains("@example.com") || resolved.contains("@"));
    }

    @Test
    void testResolveNumbersAndTimestamps() {
        String template = "{\"price\": {{$randomPrice}}, \"ts\": {{$timestamp}}, \"active\": {{$randomBoolean}}}";
        String resolved = MockDynamicVariableResolver.resolve(template, Map.of());

        assertFalse(resolved.contains("{{$randomPrice}}"));
        assertFalse(resolved.contains("{{$timestamp}}"));
        assertFalse(resolved.contains("{{$randomBoolean}}"));
    }

    @Test
    void testResolvePathVariables() {
        String template = "{\"userId\": \"{{id}}\", \"status\": \"active\"}";
        String resolved = MockDynamicVariableResolver.resolve(template, Map.of("id", "98765"));

        assertTrue(resolved.contains("\"userId\": \"98765\""));
    }

    @Test
    void testNullAndEmptyHandling() {
        assertNull(MockDynamicVariableResolver.resolve(null, Map.of()));
        assertEquals("", MockDynamicVariableResolver.resolve("", Map.of()));
    }
}
