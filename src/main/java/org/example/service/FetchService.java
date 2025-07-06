package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.PriceEntity;
import org.example.repository.PriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;

@Service
public class FetchService {
    
    private static final Logger logger = LoggerFactory.getLogger(FetchService.class);
    
    @Value("${app.coingecko.base-url}")
    private String baseUrl;
    
    @Value("${app.coingecko.timeout}")
    private int timeout;
    
    private WebClient webClient;
    private final PriceRepository priceRepository;
    private final ObjectMapper objectMapper;
    
    public FetchService(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;
        this.objectMapper = new ObjectMapper();
        // Do not initialize webClient here
    }

    @PostConstruct
    public void initWebClient() {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }
    
    /**
     * Belirli bir interval için Bitcoin fiyat verilerini çeker ve kaydeder
     */
    public void fetchAndSavePriceData(PriceEntity.IntervalType intervalType) {
        try {
            logger.info("Fetching price data for interval: {}", intervalType);
            
            String days = getDaysForInterval(intervalType);
            String url = String.format("/coins/bitcoin/market_chart?vs_currency=usd&days=%s&interval=%s", 
                                     days, getIntervalString(intervalType));
            
            logger.info("Making request to: {}", baseUrl + url);
            
            String response;
            try {
                response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                
                logger.info("Response received, length: {}", response != null ? response.length() : 0);
            } catch (Exception e) {
                logger.error("WebClient request failed: {}", e.getMessage(), e);
                throw e;
            }
            
            if (response != null) {
                JsonNode rootNode = objectMapper.readTree(response);
                JsonNode pricesNode = rootNode.get("prices");
                
                logger.info("Found {} price entries in response", pricesNode.size());
                
                List<PriceEntity> priceEntities = new ArrayList<>();
                
                for (JsonNode priceNode : pricesNode) {
                    long timestamp = priceNode.get(0).asLong();
                    BigDecimal price = new BigDecimal(priceNode.get(1).asText());
                    
                    LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, ZoneOffset.UTC);
                    
                    logger.debug("Processing timestamp: {}, price: {}, dateTime: {}", timestamp, price, dateTime);
                    
                    // Sadece yeni veri ekle
                    if (!priceRepository.existsByTimestampAndIntervalType(dateTime, intervalType)) {
                        PriceEntity entity = new PriceEntity(
                                dateTime,
                                price, // OHLC için aynı değer kullanıyoruz (basitleştirme)
                                price,
                                price,
                                price,
                                BigDecimal.ZERO, // Volume bilgisi yok
                                intervalType
                        );
                        priceEntities.add(entity);
                        logger.debug("Added new entity for timestamp: {}", dateTime);
                    } else {
                        logger.debug("Skipped duplicate entity for timestamp: {}", dateTime);
                    }
                }
                
                logger.info("Created {} new entities to save", priceEntities.size());
                
                if (!priceEntities.isEmpty()) {
                    logger.info("About to save {} entities to database", priceEntities.size());
                    List<PriceEntity> savedEntities = priceRepository.saveAll(priceEntities);
                    logger.info("Successfully saved {} new price records for interval: {}", savedEntities.size(), intervalType);
                    
                    // Verify the first saved entity
                    if (!savedEntities.isEmpty()) {
                        PriceEntity firstEntity = savedEntities.get(0);
                        logger.info("First saved entity: ID={}, timestamp={}, price={}, interval={}", 
                                  firstEntity.getId(), firstEntity.getTimestamp(), firstEntity.getClosePrice(), firstEntity.getIntervalType());
                    }
                } else {
                    logger.info("No new price data to save for interval: {}", intervalType);
                }
            } else {
                logger.warn("Response was null for interval: {}", intervalType);
            }
            
        } catch (JsonProcessingException e) {
            logger.error("JSON processing error for interval: {}", intervalType, e);
            throw new RuntimeException("JSON processing failed", e);
        } catch (Exception e) {
            logger.error("Error fetching price data for interval: {}", intervalType, e);
            throw new RuntimeException("Fetch failed", e);
        }
    }
    
    /**
     * Tüm interval tipleri için veri çeker
     */
    public void fetchAllIntervalData() {
        for (PriceEntity.IntervalType intervalType : PriceEntity.IntervalType.values()) {
            fetchAndSavePriceData(intervalType);
        }
    }
    
    /**
     * Scheduled job - her 5 dakikada bir veri çeker
     */
    @Scheduled(fixedDelayString = "${app.scheduling.price-fetch-interval}")
    public void scheduledFetch() {
        logger.info("Running scheduled price data fetch");
        fetchAllIntervalData();
    }
    
    /**
     * Interval tipine göre gün sayısını döner
     */
    private String getDaysForInterval(PriceEntity.IntervalType intervalType) {
        switch (intervalType) {
            case ONE_HOUR:
                return "7"; // Son 7 gün
            case FOUR_HOURS:
                return "30"; // Son 30 gün
            case ONE_DAY:
                return "365"; // Son 1 yıl
            default:
                return "30";
        }
    }
    
    /**
     * Interval tipine göre CoinGecko API interval string'ini döner
     */
    private String getIntervalString(PriceEntity.IntervalType intervalType) {
        switch (intervalType) {
            case ONE_HOUR:
                return "hourly";
            case FOUR_HOURS:
                return "daily"; // CoinGecko 4H desteklemiyor, daily kullanıyoruz
            case ONE_DAY:
                return "daily";
            default:
                return "daily";
        }
    }
    
    /**
     * Manuel veri çekme için endpoint
     */
    public String fetchDataManually(PriceEntity.IntervalType intervalType) {
        try {
            fetchAndSavePriceData(intervalType);
            return "Successfully fetched and saved data for interval: " + intervalType;
        } catch (Exception e) {
            logger.error("Manual fetch failed for interval: {}", intervalType, e);
            return "Failed to fetch data: " + e.getMessage();
        }
    }
    
    /**
     * Test WebClient connection
     */
    public String testWebClient() {
        try {
            String response = webClient.get()
                    .uri("/coins/bitcoin")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return response != null ? "Response length: " + response.length() : "null response";
        } catch (Exception e) {
            logger.error("WebClient test failed: {}", e.getMessage(), e);
            throw new RuntimeException("WebClient test failed: " + e.getMessage(), e);
        }
    }
} 