package com.roze.thundercall.api.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockPathMatcherTest {

    @Test
    void testExactMatch() {
        assertTrue(MockPathMatcher.match("/users/list", "/users/list").matched());
        assertFalse(MockPathMatcher.match("/users/list", "/users/detail").matched());
    }

    @Test
    void testColonParameterMatch() {
        assertTrue(MockPathMatcher.match("/users/:id", "/users/42").matched());
        assertTrue(MockPathMatcher.match("/users/:userId/orders/:orderId", "/users/100/orders/555").matched());
        assertFalse(MockPathMatcher.match("/users/:id", "/users/42/details").matched());
    }

    @Test
    void testBraceParameterMatch() {
        assertTrue(MockPathMatcher.match("/items/{itemId}", "/items/99").matched());
        assertTrue(MockPathMatcher.match("/api/v1/{resource}/{id}", "/api/v1/customers/888").matched());
    }

    @Test
    void testWildcardMatch() {
        assertTrue(MockPathMatcher.match("/files/*", "/files/photo.png").matched());
        assertTrue(MockPathMatcher.match("/*", "/anything").matched());
    }

    @Test
    void testQueryStringIgnoredInMatching() {
        assertTrue(MockPathMatcher.match("/search", "/search?q=postman&page=1").matched());
        assertTrue(MockPathMatcher.match("/users/:id", "/users/42?includeDetails=true").matched());
    }

    @Test
    void testExtractPathVariables() {
        MockPathMatcher.MatchResult result = MockPathMatcher.match("/users/:id/profile/{section}", "/users/123/profile/security");
        assertTrue(result.matched());
        Map<String, String> vars = result.pathVariables();
        assertEquals("123", vars.get("id"));
        assertEquals("security", vars.get("section"));
    }

    @Test
    void testSpecificityScore() {
        MockPathMatcher.MatchResult exact = MockPathMatcher.match("/users/profile", "/users/profile");
        MockPathMatcher.MatchResult param = MockPathMatcher.match("/users/:id", "/users/profile");
        MockPathMatcher.MatchResult wild = MockPathMatcher.match("/*", "/users/profile");

        assertTrue(exact.specificity() > param.specificity());
        assertTrue(param.specificity() > wild.specificity());
    }
}
