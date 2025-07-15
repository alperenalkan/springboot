package org.example.controller;

import org.example.entity.PriceEntity;
import org.example.service.FetchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/fetch")
@CrossOrigin(origins = "*")
public class FetchController {
    
    private final FetchService fetchService;
    // Son fetch zamanı için static referans
    private static final AtomicReference<LocalDateTime> lastFetchTime = new AtomicReference<>();
    
    public FetchController(FetchService fetchService) {
        this.fetchService = fetchService;
    }
    
    /**
     * POST /api/fetch/{interval} - Belirli interval için yeni verileri çeker
     */
    @PostMapping("/{interval}")
    public ResponseEntity<Object> fetchData(@PathVariable String interval) {
        try {
            PriceEntity.IntervalType intervalType = PriceEntity.IntervalType.fromString(interval);
            String result = fetchService.fetchDataManually(intervalType);
            lastFetchTime.set(LocalDateTime.now());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            response.put("interval", interval);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "errors", List.of(Map.of(
                    "status", "400",
                    "title", "Invalid Interval",
                    "detail", "The interval '" + interval + "' is not supported."
                ))
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "errors", List.of(Map.of(
                    "status", "500",
                    "title", "Internal Server Error",
                    "detail", e.getMessage()
                ))
            ));
        }
    }
    
    /**
     * POST /api/fetch/all - Tüm interval tipleri için veri çeker
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, String>> fetchAllData() {
        try {
            fetchService.fetchAllIntervalData();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Successfully fetched data for all intervals");
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error fetching data: " + e.getMessage());
            response.put("status", "error");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * GET /api/fetch/status - Veri çekme durumunu kontrol eder
     */
    @GetMapping("/status")
    public ResponseEntity<Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("lastFetch", lastFetchTime.get() != null ? lastFetchTime.get().toString() : "Never");
        status.put("autoFetchIntervalMinutes", 5);
        return ResponseEntity.ok(status);
    }
    
    /**
     * GET /api/fetch/test - Test endpoint to check WebClient
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testWebClient() {
        try {
            String response = fetchService.testWebClient();
            Map<String, String> result = new HashMap<>();
            result.put("status", "success");
            result.put("response", response);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> result = new HashMap<>();
            result.put("status", "error");
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * String interval'i PriceEntity.IntervalType'a çevirir
     */
    // parseIntervalType fonksiyonunu kaldır
} 