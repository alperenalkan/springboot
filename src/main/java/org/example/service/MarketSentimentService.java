package org.example.service;

import org.example.dto.FearGreedDto;
import org.example.dto.SentimentDto;
import org.example.dto.OnChainDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class MarketSentimentService {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketSentimentService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Fear & Greed Index'i CoinGecko API'den çeker
     */
    public FearGreedDto getFearGreedIndex() {
        try {
            // CoinGecko Fear & Greed API endpoint'i
            String url = "https://api.coingecko.com/api/v3/fear_greed_index";
            
            // Gerçek API çağrısı (rate limit nedeniyle şimdilik mock data)
            // Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            // Mock data (gerçek implementasyonda API'den gelecek)
            return generateMockFearGreedIndex();
            
        } catch (Exception e) {
            logger.error("Error fetching Fear & Greed Index: {}", e.getMessage());
            return generateMockFearGreedIndex();
        }
    }
    
    /**
     * Social Media Sentiment analizi
     */
    public SentimentDto getSocialMediaSentiment() {
        try {
            // Gerçek implementasyonda Twitter, Reddit API'leri kullanılacak
            return generateMockSentiment();
            
        } catch (Exception e) {
            logger.error("Error fetching social media sentiment: {}", e.getMessage());
            return generateMockSentiment();
        }
    }
    
    /**
     * On-chain metrics analizi
     */
    public OnChainDto getOnChainMetrics() {
        try {
            // Gerçek implementasyonda Blockchain.info, Glassnode API'leri kullanılacak
            return generateMockOnChainMetrics();
            
        } catch (Exception e) {
            logger.error("Error fetching on-chain metrics: {}", e.getMessage());
            return generateMockOnChainMetrics();
        }
    }
    
    /**
     * Market Sentiment sinyali üretir
     */
    public String generateSentimentSignal(FearGreedDto fearGreed, SentimentDto sentiment, OnChainDto onChain) {
        int bullishSignals = 0;
        int bearishSignals = 0;
        
        // Fear & Greed analizi
        if (fearGreed != null && fearGreed.getValue() != null) {
            BigDecimal value = fearGreed.getValue();
            if (value.compareTo(BigDecimal.valueOf(25)) <= 0) {
                bullishSignals += 2; // Extreme Fear = Buy signal
            } else if (value.compareTo(BigDecimal.valueOf(75)) >= 0) {
                bearishSignals += 2; // Extreme Greed = Sell signal
            }
        }
        
        // Social Media Sentiment analizi
        if (sentiment != null && sentiment.getOverallSentiment() != null) {
            BigDecimal sentimentValue = sentiment.getOverallSentiment();
            if (sentimentValue.compareTo(BigDecimal.valueOf(0.6)) > 0) {
                bullishSignals++;
            } else if (sentimentValue.compareTo(BigDecimal.valueOf(0.4)) < 0) {
                bearishSignals++;
            }
        }
        
        // On-chain analizi
        if (onChain != null) {
            if ("Accumulation".equals(onChain.getWhaleMovement())) {
                bullishSignals++;
            } else if ("Distribution".equals(onChain.getWhaleMovement())) {
                bearishSignals++;
            }
            
            if ("Inflow".equals(onChain.getFlowDirection())) {
                bullishSignals++;
            } else if ("Outflow".equals(onChain.getFlowDirection())) {
                bearishSignals++;
            }
        }
        
        // Sinyal kararı
        if (bullishSignals > bearishSignals) {
            return "BULLISH";
        } else if (bearishSignals > bullishSignals) {
            return "BEARISH";
        } else {
            return "NEUTRAL";
        }
    }
    
    // Mock data generators
    private FearGreedDto generateMockFearGreedIndex() {
        Random random = new Random();
        BigDecimal value = BigDecimal.valueOf(25 + random.nextInt(50)); // 25-75 arası
        
        String classification;
        String description;
        
        if (value.compareTo(BigDecimal.valueOf(25)) <= 0) {
            classification = "Extreme Fear";
            description = "Piyasa aşırı korku durumunda - alım fırsatı olabilir";
        } else if (value.compareTo(BigDecimal.valueOf(45)) <= 0) {
            classification = "Fear";
            description = "Piyasa korku durumunda - dikkatli alım";
        } else if (value.compareTo(BigDecimal.valueOf(55)) <= 0) {
            classification = "Neutral";
            description = "Piyasa nötr durumda - teknik analiz önemli";
        } else if (value.compareTo(BigDecimal.valueOf(75)) <= 0) {
            classification = "Greed";
            description = "Piyasa açgözlülük durumunda - dikkatli satış";
        } else {
            classification = "Extreme Greed";
            description = "Piyasa aşırı açgözlülük durumunda - satış fırsatı";
        }
        
        return new FearGreedDto(value, classification, LocalDateTime.now(), description);
    }
    
    private SentimentDto generateMockSentiment() {
        Random random = new Random();
        BigDecimal overallSentiment = BigDecimal.valueOf(0.3 + random.nextDouble() * 0.4); // 0.3-0.7 arası
        
        String classification;
        if (overallSentiment.compareTo(BigDecimal.valueOf(0.6)) > 0) {
            classification = "Bullish";
        } else if (overallSentiment.compareTo(BigDecimal.valueOf(0.4)) < 0) {
            classification = "Bearish";
        } else {
            classification = "Neutral";
        }
        
        Map<String, BigDecimal> platformSentiments = new HashMap<>();
        platformSentiments.put("Twitter", BigDecimal.valueOf(0.4 + random.nextDouble() * 0.3));
        platformSentiments.put("Reddit", BigDecimal.valueOf(0.3 + random.nextDouble() * 0.4));
        platformSentiments.put("Telegram", BigDecimal.valueOf(0.5 + random.nextDouble() * 0.2));
        
        Map<String, Integer> mentionCounts = new HashMap<>();
        mentionCounts.put("Twitter", 15000 + random.nextInt(10000));
        mentionCounts.put("Reddit", 8000 + random.nextInt(5000));
        mentionCounts.put("Telegram", 12000 + random.nextInt(8000));
        
        String trendingTopics = "Bitcoin ETF, Halving, Institutional Adoption";
        String sentimentExplanation = "Sosyal medyada Bitcoin hakkında genel olarak " + 
                                    classification.toLowerCase() + " bir sentiment var.";
        
        return new SentimentDto(overallSentiment, classification, LocalDateTime.now(),
                              platformSentiments, mentionCounts, trendingTopics, sentimentExplanation);
    }
    
    private OnChainDto generateMockOnChainMetrics() {
        Random random = new Random();
        
        BigDecimal whaleTransactions = BigDecimal.valueOf(50 + random.nextInt(100));
        BigDecimal exchangeInflow = BigDecimal.valueOf(1000 + random.nextInt(2000));
        BigDecimal exchangeOutflow = BigDecimal.valueOf(800 + random.nextInt(1500));
        BigDecimal netFlow = exchangeOutflow.subtract(exchangeInflow);
        
        String whaleMovement;
        if (whaleTransactions.compareTo(BigDecimal.valueOf(80)) > 0) {
            whaleMovement = "Accumulation";
        } else if (whaleTransactions.compareTo(BigDecimal.valueOf(30)) < 0) {
            whaleMovement = "Distribution";
        } else {
            whaleMovement = "Neutral";
        }
        
        String flowDirection;
        if (netFlow.compareTo(BigDecimal.valueOf(500)) > 0) {
            flowDirection = "Outflow";
        } else if (netFlow.compareTo(BigDecimal.valueOf(-500)) < 0) {
            flowDirection = "Inflow";
        } else {
            flowDirection = "Neutral";
        }
        
        BigDecimal activeAddresses = BigDecimal.valueOf(800000 + random.nextInt(200000));
        BigDecimal transactionCount = BigDecimal.valueOf(300000 + random.nextInt(100000));
        BigDecimal averageTransactionValue = BigDecimal.valueOf(1000 + random.nextInt(2000));
        BigDecimal networkHashRate = BigDecimal.valueOf(500 + random.nextInt(200));
        BigDecimal difficulty = BigDecimal.valueOf(70000 + random.nextInt(10000));
        
        String onChainExplanation = "Whale hareketleri: " + whaleMovement + 
                                  ", Exchange flow: " + flowDirection + 
                                  ", Network aktivitesi: " + (activeAddresses.compareTo(BigDecimal.valueOf(900000)) > 0 ? "Yüksek" : "Normal");
        
        return new OnChainDto(whaleTransactions, exchangeInflow, exchangeOutflow, netFlow,
                            activeAddresses, transactionCount, averageTransactionValue,
                            networkHashRate, difficulty, whaleMovement, flowDirection,
                            LocalDateTime.now(), onChainExplanation);
    }
} 