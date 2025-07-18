package org.example.controller;

import org.example.dto.FearGreedDto;
import org.example.dto.SentimentDto;
import org.example.dto.OnChainDto;
import org.example.service.MarketSentimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sentiment")
@CrossOrigin(origins = "*")
public class MarketSentimentController {
    
    @Autowired
    private MarketSentimentService marketSentimentService;
    
    /**
     * Fear & Greed Index'i getir
     */
    @GetMapping("/fear-greed")
    public ResponseEntity<FearGreedDto> getFearGreedIndex() {
        try {
            FearGreedDto fearGreed = marketSentimentService.getFearGreedIndex();
            return ResponseEntity.ok(fearGreed);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Social Media Sentiment'i getir
     */
    @GetMapping("/social-media")
    public ResponseEntity<SentimentDto> getSocialMediaSentiment() {
        try {
            SentimentDto sentiment = marketSentimentService.getSocialMediaSentiment();
            return ResponseEntity.ok(sentiment);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * On-chain metrics'i getir
     */
    @GetMapping("/on-chain")
    public ResponseEntity<OnChainDto> getOnChainMetrics() {
        try {
            OnChainDto onChain = marketSentimentService.getOnChainMetrics();
            return ResponseEntity.ok(onChain);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Tüm market sentiment verilerini getir
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllSentimentData() {
        try {
            FearGreedDto fearGreed = marketSentimentService.getFearGreedIndex();
            SentimentDto sentiment = marketSentimentService.getSocialMediaSentiment();
            OnChainDto onChain = marketSentimentService.getOnChainMetrics();
            
            String sentimentSignal = marketSentimentService.generateSentimentSignal(fearGreed, sentiment, onChain);
            
            Map<String, Object> response = new HashMap<>();
            response.put("fearGreed", fearGreed);
            response.put("sentiment", sentiment);
            response.put("onChain", onChain);
            response.put("sentimentSignal", sentimentSignal);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Market sentiment sinyali getir
     */
    @GetMapping("/signal")
    public ResponseEntity<Map<String, String>> getSentimentSignal() {
        try {
            FearGreedDto fearGreed = marketSentimentService.getFearGreedIndex();
            SentimentDto sentiment = marketSentimentService.getSocialMediaSentiment();
            OnChainDto onChain = marketSentimentService.getOnChainMetrics();
            
            String signal = marketSentimentService.generateSentimentSignal(fearGreed, sentiment, onChain);
            
            Map<String, String> response = new HashMap<>();
            response.put("signal", signal);
            response.put("fearGreedClassification", fearGreed.getClassification());
            response.put("sentimentClassification", sentiment.getOverallClassification());
            response.put("whaleMovement", onChain.getWhaleMovement());
            response.put("flowDirection", onChain.getFlowDirection());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
} 