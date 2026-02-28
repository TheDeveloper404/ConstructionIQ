package com.constructiq.backendjava.controller;

import com.constructiq.backendjava.config.ConstructIQProperties;
import com.constructiq.backendjava.security.PasswordService;
import com.constructiq.backendjava.service.AlertService;
import com.constructiq.backendjava.service.PricePointService;
import com.constructiq.backendjava.service.QuoteService;
import com.constructiq.backendjava.store.SqlDocumentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiControllerContractTest {

    @Mock
    private SqlDocumentStore store;

    private DemoController demoController;
    private ProjectController projectController;
    private RfqController rfqController;
    private QuoteController quoteController;
    private AlertController alertController;

    @BeforeEach
    void setUp() {
        ConstructIQProperties props = new ConstructIQProperties();
        props.setDemoMode(true);
        props.setDemoOrgId("demo-org-001");
        props.setDemoUserId("demo-user-001");

        PasswordService passwordService = new PasswordService();

        PricePointService pricePointService = new PricePointService(store);
        AlertService alertService = new AlertService(store);
        QuoteService quoteService = new QuoteService(store, pricePointService, alertService);

        demoController = new DemoController(store, props, passwordService);
        projectController = new ProjectController(store, props);
        rfqController = new RfqController(store, props);
        quoteController = new QuoteController(store, props, quoteService, pricePointService, alertService);
        alertController = new AlertController(store, props);
    }

    @Test
    void demoStatus_hasFrontendExpectedKeys() {
        Map<String, Object> result = demoController.demoStatus();

        assertTrue(result.containsKey("demo_mode"));
        assertTrue(result.containsKey("demo_org_id"));
        assertTrue(result.containsKey("demo_user_id"));
        assertEquals(true, result.get("demo_mode"));
    }

    @Test
    void listProjects_returnsPaginatedShape() {
        when(store.count(eq("projects"), anyMap())).thenReturn(2L);
        when(store.find(eq("projects"), anyMap(), eq("created_at"), eq(true), eq(0), eq(10)))
                .thenReturn(List.of(
                        new LinkedHashMap<>(Map.of("id", "p1", "name", "A")),
                        new LinkedHashMap<>(Map.of("id", "p2", "name", "B"))
                ));

        Map<String, Object> result = projectController.listProjects(1, 10, "");

        assertTrue(result.containsKey("items"));
        assertTrue(result.containsKey("total"));
        assertTrue(result.containsKey("page"));
        assertTrue(result.containsKey("page_size"));
        assertTrue(result.containsKey("total_pages"));
        assertEquals(2L, result.get("total"));
        assertEquals(1, result.get("page"));
        assertEquals(10, result.get("page_size"));
    }

    @Test
    void createQuote_computesItemTotalsAndQuoteTotal() {
        Map<String, Object> quoteInput = new LinkedHashMap<>();
        quoteInput.put("supplier_id", "s1");
        quoteInput.put("currency", "RON");
        quoteInput.put("items", List.of(
                new LinkedHashMap<>(Map.of(
                        "raw_line_text", "Item 1",
                        "qty", 2,
                        "uom", "unit",
                        "unit_price", 11.5,
                        "normalized_product_id", ""
                )),
                new LinkedHashMap<>(Map.of(
                        "raw_line_text", "Item 2",
                        "qty", 3,
                        "uom", "unit",
                        "unit_price", 10.0,
                        "normalized_product_id", ""
                ))
        ));

        Map<String, Object> result = quoteController.createQuote(quoteInput);

        assertEquals(53.0, (Double) result.get("total_amount"), 0.0001);
        List<?> items = (List<?>) result.get("items");
        assertEquals(2, items.size());
        verify(store, atLeastOnce()).upsert(eq("quotes"), anyMap());
    }

    @Test
    void sendRfq_withoutSuppliers_returnsBadRequest() {
        when(store.findOne("rfqs", "rfq-1", "demo-org-001")).thenReturn(
                Optional.of(new LinkedHashMap<>(Map.of(
                        "id", "rfq-1", "org_id", "demo-org-001",
                        "status", "draft", "supplier_ids", List.of())))
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> rfqController.sendRfq("rfq-1"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void listRfqs_returnsPaginatedShape() {
        when(store.count(eq("rfqs"), anyMap())).thenReturn(1L);
        when(store.find(eq("rfqs"), anyMap(), eq("created_at"), eq(true), eq(0), eq(10)))
                .thenReturn(List.of(new LinkedHashMap<>(Map.of("id", "r1", "title", "RFQ 1", "status", "draft"))));

        Map<String, Object> result = rfqController.listRfqs(1, 10, "draft", "");

        assertEquals(1L, result.get("total"));
        assertEquals(1, result.get("page"));
        assertTrue(result.containsKey("items"));
        assertTrue(result.containsKey("total_pages"));
    }

    @Test
    void listAlertEvents_enrichesProductNameAndKeepsPaginationShape() {
        when(store.count(eq("alert_events"), anyMap())).thenReturn(1L);
        when(store.find(eq("alert_events"), anyMap(), eq("triggered_at"), eq(true), eq(0), eq(10)))
                .thenReturn(List.of(new LinkedHashMap<>(Map.of(
                        "id", "e1",
                        "normalized_product_id", "np-1",
                        "status", "new",
                        "severity", "medium",
                        "payload", Map.of("change_percent", 5.5)
                ))));
        when(store.find(eq("normalized_products"), anyMap(), isNull(), eq(false), eq(0), eq(100)))
                .thenReturn(List.of(new LinkedHashMap<>(Map.of("id", "np-1", "canonical_name", "Produs Test"))));

        Map<String, Object> result = alertController.listAlertEvents(1, 10, "new");

        assertEquals(1L, result.get("total"));
        List<?> items = (List<?>) result.get("items");
        assertEquals(1, items.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) items.get(0);
        assertEquals("Produs Test", first.get("product_name"));
    }

    @Test
    void compareQuotes_requiresAtLeastTwoQuotes() {
        when(store.find(eq("quotes"), anyMap(), isNull(), eq(false), eq(0), eq(10)))
                .thenReturn(List.of(new LinkedHashMap<>(Map.of("id", "q1", "supplier_id", "s1"))));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> quoteController.compareQuotes("q1"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void updateAlertEvent_withInvalidStatus_returnsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> alertController.updateAlertEvent("e1", Map.of("status", "invalid_status")));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void updateAlertEvent_withValidStatus_updatesEvent() {
        when(store.findOne("alert_events", "e1", "demo-org-001")).thenReturn(
                Optional.of(new LinkedHashMap<>(Map.of(
                        "id", "e1", "org_id", "demo-org-001", "status", "new"))));

        Map<String, Object> result = alertController.updateAlertEvent("e1", Map.of("status", "ack"));
        assertEquals("Event updated", result.get("message"));
        verify(store, atLeastOnce()).upsert(eq("alert_events"), anyMap());
    }

    @Test
    void requireContext_withoutAuth_inNonDemoMode_returns401() {
        ConstructIQProperties prodProps = new ConstructIQProperties();
        prodProps.setDemoMode(false);
        ProjectController prodController = new ProjectController(store, prodProps);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> prodController.listProjects(1, 10, null));
        assertEquals(401, ex.getStatusCode().value());
    }
}
