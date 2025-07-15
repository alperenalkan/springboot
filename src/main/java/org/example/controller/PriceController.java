package org.example.controller;

import org.example.dto.PriceDto;
import org.example.dto.SignalDto;
import org.example.entity.PriceEntity;
import org.example.service.PriceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PriceController {
    
    private final PriceService priceService;
    
    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }
    
    /**
     * GET /api/price/{interval} - Belirli periyottaki fiyat verilerini döner
     */
    @GetMapping("/price/{interval}")
    public ResponseEntity<Object> getPriceData(@PathVariable String interval) {
        try {
            PriceEntity.IntervalType intervalType = PriceEntity.IntervalType.fromString(interval);
            List<PriceDto> priceData = priceService.getPriceData(intervalType);
            return ResponseEntity.ok(priceData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Invalid interval: " + interval
            ));
        }
    }
    
    /**
     * GET /api/price/{interval}/latest/{limit} - Son N kaydı getirir
     */
    @GetMapping("/price/{interval}/latest/{limit}")
    public ResponseEntity<Object> getLatestPriceData(
            @PathVariable String interval,
            @PathVariable int limit) {
        try {
            if (limit <= 0 || limit > 1000) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Limit must be between 1 and 1000."
                ));
            }
            PriceEntity.IntervalType intervalType = PriceEntity.IntervalType.fromString(interval);
            List<PriceDto> priceData = priceService.getLatestPriceData(intervalType, limit);
            return ResponseEntity.ok(priceData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Invalid interval: " + interval
            ));
        }
    }
    
    /**
     * GET /api/signal/{interval} - İndikatöre göre sinyal döner
     */
    @GetMapping("/signal/{interval}")
    public ResponseEntity<Object> getSignal(@PathVariable String interval) {
        try {
            PriceEntity.IntervalType intervalType = PriceEntity.IntervalType.fromString(interval);
            SignalDto signal = priceService.generateSignal(intervalType);
            return ResponseEntity.ok(signal);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Invalid interval: " + interval
            ));
        }
    }
    
    /**
     * GET /api/price/{interval}/range - Belirli tarih aralığındaki verileri getirir
     */
    @GetMapping("/price/{interval}/range")
    public ResponseEntity<Object> getPriceDataByRange(
            @PathVariable String interval,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "startDate must be before endDate."
                ));
            }
            PriceEntity.IntervalType intervalType = PriceEntity.IntervalType.fromString(interval);
            List<PriceDto> priceData = priceService.getPriceDataByDateRange(intervalType, startDate, endDate);
            return ResponseEntity.ok(priceData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Invalid interval: " + interval
            ));
        }
    }
    
    /**
     * GET /api/price/{interval}/latest - En son fiyat verisini getirir
     */
    @GetMapping("/price/{interval}/latest")
    public ResponseEntity<Object> getLatestPrice(@PathVariable String interval) {
        try {
            PriceEntity.IntervalType intervalType = PriceEntity.IntervalType.fromString(interval);
            PriceDto latestPrice = priceService.getLatestPrice(intervalType);
            if (latestPrice != null) {
                return ResponseEntity.ok(latestPrice);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Invalid interval: " + interval
            ));
        }
    }
    
    /**
     * GET /api/health - Uygulama sağlık kontrolü
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("message", "Bitcoin Signal App is running!");
        status.put("timestamp", java.time.LocalDateTime.now());
        status.put("version", "1.0.0");
        return ResponseEntity.ok(status);
    }
    
    /**
     * String interval'i PriceEntity.IntervalType'a çevirir
     */
    // parseIntervalType fonksiyonunu kaldır
} 