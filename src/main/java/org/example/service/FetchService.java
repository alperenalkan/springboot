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
import java.time.Duration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient.Builder;

class DataFetchException extends RuntimeException {
    public DataFetchException(String msg, Throwable cause) { super(msg, cause); }
    public DataFetchException(String msg) { super(msg); }
}
class ApiTimeoutException extends RuntimeException {
    public ApiTimeoutException(String msg, Throwable cause) { super(msg, cause); }
    public ApiTimeoutException(String msg) { super(msg); }
}

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
    private final WebClient.Builder webClientBuilder;
    @Autowired
    public FetchService(PriceRepository priceRepository, WebClient.Builder webClientBuilder) {
        this.priceRepository = priceRepository;
        this.objectMapper = new ObjectMapper();
        this.webClientBuilder = webClientBuilder;
    }

    @PostConstruct
    public void initWebClient() {
        this.webClient = webClientBuilder
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeout))))
            .build();
    }
    
    /**
     * Belirli bir interval için Bitcoin fiyat verilerini çeker ve kaydeder
     */
    public void fetchAndSavePriceData(PriceEntity.IntervalType intervalType) {
        try {
            logger.info("Fetching price data for interval: {}", intervalType);
            String days = getDaysForInterval(intervalType);
            String apiInterval = getIntervalString(intervalType);
            // 4H için hourly veri çekilecek
            boolean isFourHour = intervalType == PriceEntity.IntervalType.FOUR_HOURS;
            String url = String.format("/coins/bitcoin/market_chart?vs_currency=usd&days=%s&interval=%s",
                                     days, isFourHour ? "hourly" : apiInterval);
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
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout")) {
                    throw new ApiTimeoutException("API timeout: " + url, e);
                }
                throw new DataFetchException("WebClient request failed: " + url, e);
            }
            if (response != null) {
                JsonNode rootNode = objectMapper.readTree(response);
                JsonNode pricesNode = rootNode.get("prices");
                JsonNode volumesNode = rootNode.has("total_volumes") ? rootNode.get("total_volumes") : null;
                logger.info("Found {} price entries in response", pricesNode.size());
                List<PriceEntity> priceEntities = new ArrayList<>();
                // Volume eşleştirme için timestamp->volume map'i oluştur
                java.util.Map<Long, BigDecimal> volumeMap = new java.util.HashMap<>();
                if (volumesNode != null) {
                    for (JsonNode volNode : volumesNode) {
                        long ts = volNode.get(0).asLong();
                        BigDecimal vol = new BigDecimal(volNode.get(1).asText());
                        volumeMap.put(ts, vol);
                    }
                }
                for (JsonNode priceNode : pricesNode) {
                    long timestamp = priceNode.get(0).asLong();
                    BigDecimal price = new BigDecimal(priceNode.get(1).asText());
                    LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, ZoneOffset.UTC);
                    BigDecimal volume = volumeMap.getOrDefault(timestamp, BigDecimal.ZERO);
                    if (!priceRepository.existsByTimestampAndIntervalType(dateTime, isFourHour ? PriceEntity.IntervalType.ONE_HOUR : intervalType)) {
                        PriceEntity entity = new PriceEntity(
                                dateTime,
                                price, // OHLC için aynı değer kullanıyoruz (basitleştirme)
                                price,
                                price,
                                price,
                                volume,
                                isFourHour ? PriceEntity.IntervalType.ONE_HOUR : intervalType
                        );
                        priceEntities.add(entity);
                        logger.debug("Added new entity for timestamp: {}", dateTime);
                    } else {
                        logger.debug("Skipped duplicate entity for timestamp: {}", dateTime);
                    }
                }
                // Eğer 4H ise, saatlik veriden 4H OHLC barları üret
                if (isFourHour) {
                    List<PriceEntity> fourHourBars = new ArrayList<>();
                    for (int i = 0; i + 3 < priceEntities.size(); i += 4) {
                        PriceEntity o = priceEntities.get(i);
                        PriceEntity h1 = priceEntities.get(i);
                        PriceEntity h2 = priceEntities.get(i+1);
                        PriceEntity h3 = priceEntities.get(i+2);
                        PriceEntity c = priceEntities.get(i+3);
                        BigDecimal open = o.getOpenPrice();
                        BigDecimal high = h1.getHighPrice().max(h2.getHighPrice()).max(h3.getHighPrice()).max(c.getHighPrice());
                        BigDecimal low = h1.getLowPrice().min(h2.getLowPrice()).min(h3.getLowPrice()).min(c.getLowPrice());
                        BigDecimal close = c.getClosePrice();
                        BigDecimal volume = h1.getVolume().add(h2.getVolume()).add(h3.getVolume()).add(c.getVolume());
                        LocalDateTime ts = c.getTimestamp(); // 4H barın timestamp'i son barın zamanı
                        fourHourBars.add(new PriceEntity(ts, open, high, low, close, volume, PriceEntity.IntervalType.FOUR_HOURS));
                    }
                    priceEntities = fourHourBars;
                }
                logger.info("Created {} new entities to save", priceEntities.size());
                if (!priceEntities.isEmpty()) {
                    logger.info("About to save {} entities to database", priceEntities.size());
                    List<PriceEntity> savedEntities = priceRepository.saveAll(priceEntities);
                    logger.info("Successfully saved {} new price records for interval: {}", savedEntities.size(), intervalType);
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
            throw new DataFetchException("JSON processing failed", e);
        } catch (ApiTimeoutException | DataFetchException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching price data for interval: {}", intervalType, e);
            throw new DataFetchException("Fetch failed", e);
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