package org.example.controller;

import org.example.entity.PriceEntity;
import org.example.service.FetchService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FetchControllerTest {
    @Test
    void testFetchData_InvalidInterval_ReturnsBadRequest() {
        FetchService fetchService = org.mockito.Mockito.mock(FetchService.class);
        FetchController controller = new FetchController(fetchService);
        ResponseEntity<Object> response = controller.fetchData("99h");
        assertEquals(400, response.getStatusCodeValue());
        Map<?,?> body = (Map<?,?>) response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("errors"));
        var errors = (java.util.List<?>) body.get("errors");
        assertFalse(errors.isEmpty());
        Map<?,?> error = (Map<?,?>) errors.get(0);
        assertEquals("400", error.get("status"));
        assertEquals("Invalid Interval", error.get("title"));
    }
} 