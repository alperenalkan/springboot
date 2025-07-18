package org.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FearGreedDto {
    private BigDecimal value;
    private String classification;
    private LocalDateTime timestamp;
    private String description;
    
    public FearGreedDto() {}
    
    public FearGreedDto(BigDecimal value, String classification, LocalDateTime timestamp, String description) {
        this.value = value;
        this.classification = classification;
        this.timestamp = timestamp;
        this.description = description;
    }
    
    // Getters and Setters
    public BigDecimal getValue() {
        return value;
    }
    
    public void setValue(BigDecimal value) {
        this.value = value;
    }
    
    public String getClassification() {
        return classification;
    }
    
    public void setClassification(String classification) {
        this.classification = classification;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
} 