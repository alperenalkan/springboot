package org.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SignalDto {
    
    public enum SignalType {
        BUY("BUY", "💚 Al"),
        SELL("SELL", "❤️ Sat"),
        HOLD("HOLD", "⚪ Bekle");
        
        private final String value;
        private final String displayText;
        
        SignalType(String value, String displayText) {
            this.value = value;
            this.displayText = displayText;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getDisplayText() {
            return displayText;
        }
    }
    
    private SignalType signal;
    private String intervalType;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private BigDecimal currentPrice;
    private BigDecimal rsiValue;
    private BigDecimal macdValue;
    private BigDecimal macdSignal;
    private BigDecimal macdHistogram;
    private BigDecimal sma20;
    private BigDecimal ema12;
    
    private String reasoning;
    
    // Constructors
    public SignalDto() {}
    
    public SignalDto(SignalType signal, String intervalType, LocalDateTime timestamp, 
                    BigDecimal currentPrice, String reasoning) {
        this.signal = signal;
        this.intervalType = intervalType;
        this.timestamp = timestamp;
        this.currentPrice = currentPrice;
        this.reasoning = reasoning;
    }
    
    // Getters and Setters
    public SignalType getSignal() {
        return signal;
    }
    
    public void setSignal(SignalType signal) {
        this.signal = signal;
    }
    
    public String getIntervalType() {
        return intervalType;
    }
    
    public void setIntervalType(String intervalType) {
        this.intervalType = intervalType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public BigDecimal getRsiValue() {
        return rsiValue;
    }
    
    public void setRsiValue(BigDecimal rsiValue) {
        this.rsiValue = rsiValue;
    }
    
    public BigDecimal getMacdValue() {
        return macdValue;
    }
    
    public void setMacdValue(BigDecimal macdValue) {
        this.macdValue = macdValue;
    }
    
    public BigDecimal getMacdSignal() {
        return macdSignal;
    }
    
    public void setMacdSignal(BigDecimal macdSignal) {
        this.macdSignal = macdSignal;
    }
    
    public BigDecimal getMacdHistogram() {
        return macdHistogram;
    }
    
    public void setMacdHistogram(BigDecimal macdHistogram) {
        this.macdHistogram = macdHistogram;
    }
    
    public BigDecimal getSma20() {
        return sma20;
    }
    
    public void setSma20(BigDecimal sma20) {
        this.sma20 = sma20;
    }
    
    public BigDecimal getEma12() {
        return ema12;
    }
    
    public void setEma12(BigDecimal ema12) {
        this.ema12 = ema12;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    @Override
    public String toString() {
        return "SignalDto{" +
                "signal=" + signal +
                ", intervalType='" + intervalType + '\'' +
                ", timestamp=" + timestamp +
                ", currentPrice=" + currentPrice +
                ", rsiValue=" + rsiValue +
                ", macdValue=" + macdValue +
                ", macdSignal=" + macdSignal +
                ", macdHistogram=" + macdHistogram +
                ", sma20=" + sma20 +
                ", ema12=" + ema12 +
                ", reasoning='" + reasoning + '\'' +
                '}';
    }
} 