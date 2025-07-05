package org.example.controller;

import org.example.entity.PriceEntity;
import org.example.service.FetchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/fetch")
@CrossOrigin(origins = "*")
public class FetchController {
    
    private final FetchService fetchService;
    
    public FetchController(FetchService fetchService) {
        this.fetchService = fetchService;
    }
    
    /**
     * POST /api/fetch/{interval} - Belirli interval için yeni verileri çeker
     */
    @PostMapping("/{interval}")
    public ResponseEntity<Map<String, String>> fetchData(@PathVariable String interval) {
        try {
            PriceEntity.IntervalType intervalType = parseIntervalType(interval);
            String result = fetchService.fetchDataManually(intervalType);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            response.put("interval", interval);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Invalid interval: " + interval);
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
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
    public ResponseEntity<Map<String, Object>> getFetchStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("scheduled", true);
        status.put("lastFetch", "Auto-fetch every 5 minutes");
        status.put("supportedIntervals", new String[]{"1h", "4h", "1d"});
        status.put("dataSource", "CoinGecko API");
        
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
    private PriceEntity.IntervalType parseIntervalType(String interval) {
        switch (interval.toLowerCase()) {
            case "1h":
            case "1hour":
            case "hourly":
                return PriceEntity.IntervalType.ONE_HOUR;
            case "4h":
            case "4hours":
                return PriceEntity.IntervalType.FOUR_HOURS;
            case "1d":
            case "1day":
            case "daily":
                return PriceEntity.IntervalType.ONE_DAY;
            default:
                throw new IllegalArgumentException("Invalid interval: " + interval);
        }
    }
} 