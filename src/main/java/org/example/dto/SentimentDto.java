package org.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class SentimentDto {
    private BigDecimal overallSentiment;
    private String overallClassification;
    private LocalDateTime timestamp;
    private Map<String, BigDecimal> platformSentiments; // Twitter, Reddit, etc.
    private Map<String, Integer> mentionCounts;
    private String trendingTopics;
    private String sentimentExplanation;
    
    public SentimentDto() {}
    
    public SentimentDto(BigDecimal overallSentiment, String overallClassification, 
                       LocalDateTime timestamp, Map<String, BigDecimal> platformSentiments,
                       Map<String, Integer> mentionCounts, String trendingTopics, 
                       String sentimentExplanation) {
        this.overallSentiment = overallSentiment;
        this.overallClassification = overallClassification;
        this.timestamp = timestamp;
        this.platformSentiments = platformSentiments;
        this.mentionCounts = mentionCounts;
        this.trendingTopics = trendingTopics;
        this.sentimentExplanation = sentimentExplanation;
    }
    
    // Getters and Setters
    public BigDecimal getOverallSentiment() {
        return overallSentiment;
    }
    
    public void setOverallSentiment(BigDecimal overallSentiment) {
        this.overallSentiment = overallSentiment;
    }
    
    public String getOverallClassification() {
        return overallClassification;
    }
    
    public void setOverallClassification(String overallClassification) {
        this.overallClassification = overallClassification;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, BigDecimal> getPlatformSentiments() {
        return platformSentiments;
    }
    
    public void setPlatformSentiments(Map<String, BigDecimal> platformSentiments) {
        this.platformSentiments = platformSentiments;
    }
    
    public Map<String, Integer> getMentionCounts() {
        return mentionCounts;
    }
    
    public void setMentionCounts(Map<String, Integer> mentionCounts) {
        this.mentionCounts = mentionCounts;
    }
    
    public String getTrendingTopics() {
        return trendingTopics;
    }
    
    public void setTrendingTopics(String trendingTopics) {
        this.trendingTopics = trendingTopics;
    }
    
    public String getSentimentExplanation() {
        return sentimentExplanation;
    }
    
    public void setSentimentExplanation(String sentimentExplanation) {
        this.sentimentExplanation = sentimentExplanation;
    }
} 