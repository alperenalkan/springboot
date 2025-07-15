package org.example.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "price_data")
public class PriceEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "open_price", precision = 20, scale = 8, nullable = false)
    private BigDecimal openPrice;
    
    @Column(name = "high_price", precision = 20, scale = 8, nullable = false)
    private BigDecimal highPrice;
    
    @Column(name = "low_price", precision = 20, scale = 8, nullable = false)
    private BigDecimal lowPrice;
    
    @Column(name = "close_price", precision = 20, scale = 8, nullable = false)
    private BigDecimal closePrice;
    
    @Column(name = "volume", precision = 20, scale = 8, nullable = false)
    private BigDecimal volume;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "interval_type", nullable = false)
    private IntervalType intervalType;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    public enum IntervalType {
        ONE_HOUR("1h", "1hour", "hourly"),
        FOUR_HOURS("4h", "4hours"),
        ONE_DAY("1d", "1day", "daily");

        private final Set<String> aliases;

        IntervalType(String... aliases) {
            this.aliases = Set.of(aliases);
        }

        public static IntervalType fromString(String input) {
            for (IntervalType type : values()) {
                if (type.aliases.contains(input.toLowerCase())) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid interval: " + input);
        }

        public String getValue() {
            return aliases.iterator().next();
        }
    }
    
    // Constructors
    public PriceEntity() {
        this.createdAt = LocalDateTime.now();
    }
    
    public PriceEntity(LocalDateTime timestamp, BigDecimal openPrice, BigDecimal highPrice, 
                      BigDecimal lowPrice, BigDecimal closePrice, BigDecimal volume, IntervalType intervalType) {
        this();
        this.timestamp = timestamp;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.intervalType = intervalType;
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
    
    public IntervalType getIntervalType() {
        return intervalType;
    }
    
    public void setIntervalType(IntervalType intervalType) {
        this.intervalType = intervalType;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "PriceEntity{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", openPrice=" + openPrice +
                ", highPrice=" + highPrice +
                ", lowPrice=" + lowPrice +
                ", closePrice=" + closePrice +
                ", volume=" + volume +
                ", intervalType=" + intervalType +
                ", createdAt=" + createdAt +
                '}';
    }
} 