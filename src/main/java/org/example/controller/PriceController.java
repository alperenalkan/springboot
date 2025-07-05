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
    public ResponseEntity<List<PriceDto>> getPriceData(@PathVariable String interval) {
        try {
            PriceEntity.IntervalType intervalType = parseIntervalType(interval);
            List<PriceDto> priceData = priceService.getPriceData(intervalType);
            return ResponseEntity.ok(priceData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * GET /api/price/{interval}/latest/{limit} - Son N kaydı getirir
     */
    @GetMapping("/price/{interval}/latest/{limit}")
    public ResponseEntity<List<PriceDto>> getLatestPriceData(
            @PathVariable String interval,
            @PathVariable int limit) {
        try {
            PriceEntity.IntervalType intervalType = parseIntervalType(interval);
            List<PriceDto> priceData = priceService.getLatestPriceData(intervalType, limit);
            return ResponseEntity.ok(priceData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * GET /api/signal/{interval} - İndikatöre göre sinyal döner
     */
    @GetMapping("/signal/{interval}")
    public ResponseEntity<SignalDto> getSignal(@PathVariable String interval) {
        try {
            PriceEntity.IntervalType intervalType = parseIntervalType(interval);
            SignalDto signal = priceService.generateSignal(intervalType);
            return ResponseEntity.ok(signal);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * GET /api/price/{interval}/range - Belirli tarih aralığındaki verileri getirir
     */
    @GetMapping("/price/{interval}/range")
    public ResponseEntity<List<PriceDto>> getPriceDataByRange(
            @PathVariable String interval,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            PriceEntity.IntervalType intervalType = parseIntervalType(interval);
            List<PriceDto> priceData = priceService.getPriceDataByDateRange(intervalType, startDate, endDate);
            return ResponseEntity.ok(priceData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * GET /api/price/{interval}/latest - En son fiyat verisini getirir
     */
    @GetMapping("/price/{interval}/latest")
    public ResponseEntity<PriceDto> getLatestPrice(@PathVariable String interval) {
        try {
            PriceEntity.IntervalType intervalType = parseIntervalType(interval);
            PriceDto latestPrice = priceService.getLatestPrice(intervalType);
            if (latestPrice != null) {
                return ResponseEntity.ok(latestPrice);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * GET /api/health - Uygulama sağlık kontrolü
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Bitcoin Signal App is running!");
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