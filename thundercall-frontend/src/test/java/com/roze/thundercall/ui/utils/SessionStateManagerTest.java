package com.roze.thundercall.ui.utils;

import com.roze.thundercall.ui.models.KeyValuePairDto;
import com.roze.thundercall.ui.models.SessionData;
import com.roze.thundercall.ui.models.TabStateDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SessionStateManagerTest {

    @Test
    void testSessionSaveAndLoadRoundTrip() {
        Long testUserId = 999999L;

        // Construct SessionData matching Postman scenario
        List<TabStateDto> tabs = new ArrayList<>();

        // Tab 1: GET short_basic_search
        List<KeyValuePairDto> params = new ArrayList<>();
        params.add(KeyValuePairDto.builder().enabled(true).key("page").value("1").description("").build());
        params.add(KeyValuePairDto.builder().enabled(true).key("limit").value("10").description("").build());
        params.add(KeyValuePairDto.builder().enabled(true).key("searchStr").value("1000805767").description("").build());
        params.add(KeyValuePairDto.builder().enabled(true).key("dir").value("desc").description("").build());

        TabStateDto tab1 = TabStateDto.builder()
                .name("short_basic_search")
                .requestId(101L)
                .collectionId(5L)
                .method("GET")
                .url("{{DBASE_URL}}/bbill/short_basic_search?page=1&limit=10&searchStr=1000805767&dir=desc")
                .params(params)
                .dirty(false)
                .build();
        tabs.add(tab1);

        // Tab 2: GET generate_patient_pharm (dirty = true)
        TabStateDto tab2 = TabStateDto.builder()
                .name("generate_patient_pharm")
                .requestId(102L)
                .collectionId(5L)
                .method("GET")
                .url("{{DBASE_URL}}/bbill/generate_patient_pharm")
                .dirty(true)
                .build();
        tabs.add(tab2);

        // Tab 3: POST create_general_bill (dirty = true, GraphQL body)
        TabStateDto tab3 = TabStateDto.builder()
                .name("create_general_bill")
                .requestId(103L)
                .collectionId(5L)
                .method("POST")
                .url("{{DBASE_URL}}/bbill/create_general_bill")
                .bodyType("GraphQL")
                .graphqlQuery("query GetBill($id: ID!) { bill(id: $id) { total } }")
                .graphqlVariables("{\"id\": \"123\"}")
                .dirty(true)
                .build();
        tabs.add(tab3);

        SessionData session = SessionData.builder()
                .activeTabIndex(2)
                .tabs(tabs)
                .selectedEnvironment("NexaCommerce")
                .expandedTreeKeys(Set.of("c:5", "f:12", "f:15"))
                .selectedTreeKey("r:103")
                .activeWorkspaceId(42L)
                .build();

        // Save
        SessionStateManager.saveSession(testUserId, session);

        // Load
        SessionData restored = SessionStateManager.loadSession(testUserId);

        assertNotNull(restored);
        assertEquals(2, restored.getActiveTabIndex());
        assertEquals("NexaCommerce", restored.getSelectedEnvironment());
        assertEquals("r:103", restored.getSelectedTreeKey());
        assertEquals(42L, restored.getActiveWorkspaceId());
        assertEquals(3, restored.getExpandedTreeKeys().size());
        assertTrue(restored.getExpandedTreeKeys().contains("c:5"));
        assertTrue(restored.getExpandedTreeKeys().contains("f:12"));
        assertTrue(restored.getExpandedTreeKeys().contains("f:15"));

        // Verify tabs
        assertNotNull(restored.getTabs());
        assertEquals(3, restored.getTabs().size());

        TabStateDto rTab1 = restored.getTabs().get(0);
        assertEquals("short_basic_search", rTab1.getName());
        assertEquals("GET", rTab1.getMethod());
        assertEquals(101L, rTab1.getRequestId());
        assertFalse(rTab1.isDirty());
        assertEquals(4, rTab1.getParams().size());
        assertEquals("page", rTab1.getParams().get(0).getKey());
        assertEquals("1", rTab1.getParams().get(0).getValue());

        TabStateDto rTab2 = restored.getTabs().get(1);
        assertEquals("generate_patient_pharm", rTab2.getName());
        assertTrue(rTab2.isDirty());

        TabStateDto rTab3 = restored.getTabs().get(2);
        assertEquals("create_general_bill", rTab3.getName());
        assertEquals("POST", rTab3.getMethod());
        assertEquals("GraphQL", rTab3.getBodyType());
        assertEquals("query GetBill($id: ID!) { bill(id: $id) { total } }", rTab3.getGraphqlQuery());
        assertEquals("{\"id\": \"123\"}", rTab3.getGraphqlVariables());
        assertTrue(rTab3.isDirty());
    }

    @Test
    void testLoadSessionNonExistentUserReturnsNonNullSession() {
        SessionData session = SessionStateManager.loadSession(-999999L);
        assertNotNull(session);
        assertNotNull(session.getTabs());
    }

    @Test
    void testSharedWorkspaceSessionPersistenceRoundTrip() {
        Long testUserId = 888888L;

        SessionData session = SessionData.builder()
                .activeTabIndex(0)
                .activeWorkspaceId(150L)
                .sharedWorkspace(true)
                .sharedWorkspaceRole("EDITOR")
                .sharedWorkspaceName("CHL ERP Workspace")
                .sharedWorkspaceOwnerUsername("firoze")
                .expandedTreeKeys(Set.of("c:10", "f:20"))
                .selectedTreeKey("r:300")
                .build();

        SessionStateManager.saveSession(testUserId, session);

        SessionData restored = SessionStateManager.loadSession(testUserId);

        assertNotNull(restored);
        assertEquals(150L, restored.getActiveWorkspaceId());
        assertTrue(restored.isSharedWorkspace());
        assertEquals("EDITOR", restored.getSharedWorkspaceRole());
        assertEquals("CHL ERP Workspace", restored.getSharedWorkspaceName());
        assertEquals("firoze", restored.getSharedWorkspaceOwnerUsername());
        assertTrue(restored.getExpandedTreeKeys().contains("c:10"));
        assertTrue(restored.getExpandedTreeKeys().contains("f:20"));
        assertEquals("r:300", restored.getSelectedTreeKey());
    }
}
