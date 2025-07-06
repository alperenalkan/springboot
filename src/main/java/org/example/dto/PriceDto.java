package org.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.example.entity.PriceEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PriceDto {
    
    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private BigDecimal volume;
    private String intervalType;
    
    private BigDecimal sma20;
    private BigDecimal sma50;
    private BigDecimal sma200;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    // Constructors
    public PriceDto() {}
    
    public PriceDto(PriceEntity entity) {
        this.id = entity.getId();
        this.timestamp = entity.getTimestamp();
        this.openPrice = entity.getOpenPrice();
        this.highPrice = entity.getHighPrice();
        this.lowPrice = entity.getLowPrice();
        this.closePrice = entity.getClosePrice();
        this.volume = entity.getVolume();
        this.intervalType = entity.getIntervalType().getValue();
        this.createdAt = entity.getCreatedAt();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public BigDecimal getOpenPrice() {
        return openPrice;
    }
    
    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }
    
    public BigDecimal getHighPrice() {
        return highPrice;
    }
    
    public void setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
    }
    
    public BigDecimal getLowPrice() {
        return lowPrice;
    }
    
    public void setLowPrice(BigDecimal lowPrice) {
        this.lowPrice = lowPrice;
    }
    
    public BigDecimal getClosePrice() {
        return closePrice;
    }
    
    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }
    
    public BigDecimal getVolume() {
        return volume;
    }
    
    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }
    
    public String getIntervalType() {
        return intervalType;
    }
    
    public void setIntervalType(String intervalType) {
        this.intervalType = intervalType;
    }
    
    public BigDecimal getSma20() {
        return sma20;
    }
    public void setSma20(BigDecimal sma20) {
        this.sma20 = sma20;
    }
    public BigDecimal getSma50() {
        return sma50;
    }
    public void setSma50(BigDecimal sma50) {
        this.sma50 = sma50;
    }
    public BigDecimal getSma200() {
        return sma200;
    }
    public void setSma200(BigDecimal sma200) {
        this.sma200 = sma200;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "PriceDto{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", openPrice=" + openPrice +
                ", highPrice=" + highPrice +
                ", lowPrice=" + lowPrice +
                ", closePrice=" + closePrice +
                ", volume=" + volume +
                ", intervalType='" + intervalType + '\'' +
                ", sma20=" + sma20 +
                ", sma50=" + sma50 +
                ", sma200=" + sma200 +
                ", createdAt=" + createdAt +
                '}';
    }
} 