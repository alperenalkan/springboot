package org.example.service;

import org.example.dto.PriceDto;
import org.example.dto.SignalDto;
import org.example.dto.FearGreedDto;
import org.example.dto.SentimentDto;
import org.example.dto.OnChainDto;
import org.example.entity.PriceEntity;
import org.example.repository.PriceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;

@Service
public class PriceService {
    
    private final PriceRepository priceRepository;
    private final IndicatorService indicatorService;
    private final MarketSentimentService marketSentimentService;
    
    // Kullanıcı dostu: application.properties veya parametre ile agresif sinyal seçimi
    @org.springframework.beans.factory.annotation.Value("${app.signal.aggressive:false}")
    private boolean useAggressiveSignal;
    
    public PriceService(PriceRepository priceRepository, IndicatorService indicatorService, MarketSentimentService marketSentimentService) {
        this.priceRepository = priceRepository;
        this.indicatorService = indicatorService;
        this.marketSentimentService = marketSentimentService;
    }
    
    /**
     * Belirli bir interval için fiyat verilerini getirir
     */
    public List<PriceDto> getPriceData(PriceEntity.IntervalType intervalType) {
        List<PriceEntity> entities = priceRepository.findByIntervalTypeOrderByTimestampDesc(intervalType);
        return entities.stream()
                .map(PriceDto::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Belirli bir interval için son N kaydı getirir
     */
    public List<PriceDto> getLatestPriceData(PriceEntity.IntervalType intervalType, int limit) {
        // Her zaman en az 300 veri çek
        List<PriceEntity> entities = priceRepository.findLatestNByIntervalType(intervalType, 300);
        // Veriler DESC geliyor, grafikte doğru sıralama için ters çevir
        Collections.reverse(entities);
        // 1. Her bar (gün) için sadece bir kapanış fiyatı kullan (en son kapanış)
        java.util.Map<java.time.LocalDate, PriceEntity> lastClosePerDay = new java.util.LinkedHashMap<>();
        for (PriceEntity e : entities) {
            java.time.LocalDate day = e.getTimestamp().toLocalDate();
            // Aynı gün için daha güncel bir kapanış varsa onu al
            lastClosePerDay.put(day, e);
        }
        // 2. Sıralı kapanış fiyatı listesi oluştur
        java.util.List<PriceEntity> dailyEntities = new java.util.ArrayList<>(lastClosePerDay.values());
        // En eski -> en yeni sıralı olmalı
        dailyEntities.sort(java.util.Comparator.comparing(PriceEntity::getTimestamp));
        List<PriceDto> dtos = new ArrayList<>();
        for (int i = 0; i < dailyEntities.size(); i++) {
            PriceEntity entity = dailyEntities.get(i);
            PriceDto dto = new PriceDto(entity);
            
            // SMA20 - son 20 veri (eğer yeterli veri varsa)
            if (i >= 19) {
                List<PriceEntity> sma20List = dailyEntities.subList(i - 19, i + 1);
                dto.setSma20(indicatorService.calculateSMA(sma20List, 20));
            } else {
                dto.setSma20(BigDecimal.ZERO);
            }
            
            // SMA50 - son 50 veri (eğer yeterli veri varsa)
            if (i >= 49) {
                List<PriceEntity> sma50List = dailyEntities.subList(i - 49, i + 1);
                dto.setSma50(indicatorService.calculateSMA(sma50List, 50));
            } else {
                dto.setSma50(BigDecimal.ZERO);
            }
            
            // SMA200 - son 200 veri (eğer yeterli veri varsa)
            if (i >= 199) {
                List<PriceEntity> sma200List = dailyEntities.subList(i - 199, i + 1);
                dto.setSma200(indicatorService.calculateSMA(sma200List, 200));
            } else {
                dto.setSma200(BigDecimal.ZERO);
            }
            
            // SuperTrend - son 10 bar ile hesapla (en az 10 bar olmalı)
            if (i >= 9) {
                List<PriceEntity> stList = dailyEntities.subList(i - 9, i + 1);
                dto.setSuperTrend(indicatorService.calculateSuperTrend(stList, 10, 3.0));
            } else {
                dto.setSuperTrend(BigDecimal.ZERO);
            }
            // VWAP - tüm geçmiş barlar ile hesapla (veya son 20 bar)
            if (i >= 1) {
                List<PriceEntity> vwapList = dailyEntities.subList(0, i + 1);
                dto.setVwap(indicatorService.calculateVWAP(vwapList));
            } else {
                dto.setVwap(BigDecimal.ZERO);
            }
            
            // Bollinger Bands - son 20 bar ile hesapla (en az 20 bar olmalı)
            if (i >= 19) {
                List<PriceEntity> bollList = dailyEntities.subList(i - 19, i + 1);
                IndicatorService.BollingerBandsResult boll = indicatorService.calculateBollingerBands(bollList, 20, 2.0);
                dto.setBollingerUpper(boll.upper);
                dto.setBollingerLower(boll.lower);
            } else {
                dto.setBollingerUpper(BigDecimal.ZERO);
                dto.setBollingerLower(BigDecimal.ZERO);
            }
            
            dtos.add(dto);
        }
        return dtos;
    }
    
    /**
     * Belirli bir interval için sinyal üretir
     */
    public SignalDto generateSignal(PriceEntity.IntervalType intervalType) {
        // SMA200 ve Ichimoku için en az 200 veri çek
        List<PriceEntity> prices = priceRepository.findLatestNByIntervalType(intervalType, 200);
        
        if (prices.isEmpty()) {
            return createEmptySignal(intervalType);
        }
        
        // Fiyatları en eski -> en yeni olacak şekilde sırala
        Collections.reverse(prices);
        // En güncel fiyat
        PriceEntity latestPrice = prices.get(prices.size() - 1);
        BigDecimal currentPrice = latestPrice.getClosePrice();
        
        // Teknik indikatörleri hesapla
        BigDecimal rsi = indicatorService.calculateRSI(prices);
        IndicatorService.MACDResult macd = indicatorService.calculateMACD(prices);
        BigDecimal sma20 = indicatorService.calculateSMA(prices, 20);
        BigDecimal sma50 = indicatorService.calculateSMA(prices, 50);
        BigDecimal sma200 = indicatorService.calculateSMA(prices, 200);
        BigDecimal ema12 = indicatorService.calculateEMA(prices, 12);
        BigDecimal atr = indicatorService.calculateATR(prices, 14);
        
        // Sinyal üret
        BigDecimal prevHistogram = null;
        if (prices.size() > 1) {
            IndicatorService.MACDResult prevMacd = indicatorService.calculateMACD(prices.subList(0, prices.size() - 1));
            prevHistogram = prevMacd.histogram;
        }
        String macdSignal = indicatorService.generateMACDSignal(macd, prevHistogram);
        
        // Yeni göstergeleri hesapla
        IndicatorService.BollingerBandsResult boll = indicatorService.calculateBollingerBandsEMA(prices, 20, 2.0);
        BigDecimal stochasticRsi = indicatorService.calculateStochasticRSI(prices, 14);
        BigDecimal adx = indicatorService.calculateADX(prices, 14);
        IndicatorService.IchimokuResult ichimoku = indicatorService.calculateIchimoku(prices);
        // SuperTrend ve VWAP
        BigDecimal vwap = indicatorService.calculateVWAP(prices);
        BigDecimal superTrend = indicatorService.calculateSuperTrend(prices, 10, 3.0);
        
        // --- Detaylı Analiz ---
        DetailedAnalysisResult detailedAnalysis = performDetailedAnalysis(rsi, macd, currentPrice, sma20, sma50, sma200, macdSignal, atr, boll, stochasticRsi, adx, ichimoku, prices);
        
        SignalDto.SignalType signal = detailedAnalysis.signal;
        SignalDto.SignalType aggressiveSignal;
        if (useAggressiveSignal) {
            aggressiveSignal = determineAggressiveSignal(rsi, macd, macdSignal, currentPrice, sma20, sma50, sma200);
        } else {
            aggressiveSignal = determineAggressiveSignal(rsi, macd, macdSignal, currentPrice, sma20, sma50, sma200);
        }
        
        // SignalDto oluştur
        SignalDto signalDto = new SignalDto(signal, intervalType.getValue(), 
                                          latestPrice.getTimestamp(), currentPrice, detailedAnalysis.reasoning);
        signalDto.setRsiValue(rsi);
        signalDto.setMacdValue(macd.macdLine);
        signalDto.setMacdSignal(macd.signalLine);
        signalDto.setMacdHistogram(macd.histogram);
        signalDto.setSma20(sma20);
        signalDto.setSma50(sma50);
        signalDto.setSma200(sma200);
        signalDto.setEma12(ema12);
        signalDto.setAtr(atr);
        signalDto.setBollingerUpper(boll.upper);
        signalDto.setBollingerMiddle(boll.middle);
        signalDto.setBollingerLower(boll.lower);
        signalDto.setStochasticRsi(stochasticRsi);
        signalDto.setAdx(adx);
        signalDto.setIchimokuTenkan(ichimoku.tenkan);
        signalDto.setIchimokuKijun(ichimoku.kijun);
        signalDto.setIchimokuSenkouA(ichimoku.senkouA);
        signalDto.setIchimokuSenkouB(ichimoku.senkouB);
        signalDto.setIchimokuChikou(ichimoku.chikou);
        // SuperTrend ve VWAP'i ekle
        signalDto.setVwap(vwap);
        signalDto.setSuperTrend(superTrend);
        
        // Ichimoku sinyali ve fiyat tahminleri hesapla
        IndicatorService.IchimokuSignalResult ichimokuResult = indicatorService.generateIchimokuSignalWithPredictions(ichimoku, currentPrice);
        signalDto.setIchimokuSignal(ichimokuResult.signal);
        signalDto.setIchimokuEntryPrice(ichimokuResult.entryPrice);
        signalDto.setIchimokuStopLoss(ichimokuResult.stopLoss);
        signalDto.setIchimokuTakeProfit(ichimokuResult.takeProfit);
        signalDto.setIchimokuPredictionExplanation(ichimokuResult.explanation);
        
        // Detaylı analiz sonuçlarını ekle
        signalDto.setEntryPrice(detailedAnalysis.entryPrice);
        signalDto.setStopLoss(detailedAnalysis.stopLoss);
        signalDto.setTakeProfit(detailedAnalysis.takeProfit);
        signalDto.setEntryExplanation(detailedAnalysis.entryExplanation);
        signalDto.setSltpExplanation(detailedAnalysis.sltpExplanation);
        signalDto.setBuySignals(detailedAnalysis.buySignals);
        signalDto.setSellSignals(detailedAnalysis.sellSignals);
        signalDto.setRsiAnalysis(detailedAnalysis.rsiAnalysis);
        signalDto.setMacdAnalysis(detailedAnalysis.macdAnalysis);
        signalDto.setTrendAnalysis(detailedAnalysis.trendAnalysis);
        
        // Yeni analiz alanlarını ekle
        signalDto.setBollingerAnalysis(detailedAnalysis.bollingerAnalysis);
        signalDto.setStochasticAnalysis(detailedAnalysis.stochasticAnalysis);
        signalDto.setAdxAnalysis(detailedAnalysis.adxAnalysis);
        signalDto.setIchimokuAnalysis(detailedAnalysis.ichimokuAnalysis);
        
        // Market Sentiment verilerini ekle
        try {
            FearGreedDto fearGreed = marketSentimentService.getFearGreedIndex();
            SentimentDto sentiment = marketSentimentService.getSocialMediaSentiment();
            OnChainDto onChain = marketSentimentService.getOnChainMetrics();
            
            signalDto.setFearGreedValue(fearGreed.getValue());
            signalDto.setFearGreedClassification(fearGreed.getClassification());
            signalDto.setFearGreedDescription(fearGreed.getDescription());
            signalDto.setSentimentValue(sentiment.getOverallSentiment());
            signalDto.setSentimentClassification(sentiment.getOverallClassification());
            signalDto.setSentimentExplanation(sentiment.getSentimentExplanation());
            signalDto.setWhaleTransactions(onChain.getWhaleTransactions());
            signalDto.setWhaleMovement(onChain.getWhaleMovement());
            signalDto.setFlowDirection(onChain.getFlowDirection());
            signalDto.setOnChainExplanation(onChain.getOnChainExplanation());
            
            String sentimentSignal = marketSentimentService.generateSentimentSignal(fearGreed, sentiment, onChain);
            signalDto.setSentimentSignal(sentimentSignal);
            
        } catch (Exception e) {
            // Market sentiment verisi alınamazsa varsayılan değerler
            signalDto.setFearGreedValue(BigDecimal.valueOf(50));
            signalDto.setFearGreedClassification("Neutral");
            signalDto.setFearGreedDescription("Market sentiment verisi alınamadı");
            signalDto.setSentimentValue(BigDecimal.valueOf(0.5));
            signalDto.setSentimentClassification("Neutral");
            signalDto.setSentimentSignal("NEUTRAL");
        }
        
        // --- Aggressive sinyalı da ekle ---
        signalDto.setAggressiveSignal(aggressiveSignal != null ? aggressiveSignal.name() : null);
        // --- Trade Advice ---
        String tradeAdvice;
        if (signal == SignalDto.SignalType.BUY) {
            tradeAdvice = "AL (LONG açılabilir)";
        } else if (signal == SignalDto.SignalType.SELL) {
            tradeAdvice = "SAT (SHORT açılabilir)";
        } else {
            tradeAdvice = "BEKLE (İşlem açma)";
        }
        signalDto.setTradeAdvice(tradeAdvice);
        // Detaylı analiz sonuçlarını kullan (eski TP hesaplama yerine)
        signalDto.setStopLoss(detailedAnalysis.stopLoss);
        signalDto.setTakeProfit(detailedAnalysis.takeProfit);
        signalDto.setSltpExplanation(detailedAnalysis.sltpExplanation);
        return signalDto;
    }
    
    /**
     * Boş sinyal oluşturur (veri yoksa)
     */
    private SignalDto createEmptySignal(PriceEntity.IntervalType intervalType) {
        SignalDto signalDto = new SignalDto(SignalDto.SignalType.HOLD, intervalType.getValue(), 
                                          LocalDateTime.now(), BigDecimal.ZERO, 
                                          "Yeterli veri bulunamadı");
        signalDto.setRsiValue(BigDecimal.ZERO);
        signalDto.setMacdValue(BigDecimal.ZERO);
        signalDto.setMacdSignal(BigDecimal.ZERO);
        signalDto.setMacdHistogram(BigDecimal.ZERO);
        signalDto.setSma20(BigDecimal.ZERO);
        signalDto.setEma12(BigDecimal.ZERO);
        return signalDto;
    }
    
    /**
     * Teknik indikatörlere göre sinyal belirler
     */
    private SignalDto.SignalType determineSignal(BigDecimal rsi, IndicatorService.MACDResult macd, 
                                               BigDecimal currentPrice, BigDecimal sma20, BigDecimal sma50, BigDecimal sma200, String macdSignal) {
        int buySignals = 0;
        int sellSignals = 0;

        // RSI sinyali
        String rsiSignal = indicatorService.generateRSISignal(rsi);
        if ("BUY".equals(rsiSignal)) buySignals++;
        else if ("SELL".equals(rsiSignal)) sellSignals++;

        // MACD sinyali (dışarıdan alınan macdSignal)
        if ("BUY".equals(macdSignal)) buySignals++;
        else if ("SELL".equals(macdSignal)) sellSignals++;

        // SMA20, SMA50, SMA200 sinyalleri
        if (sma20 != null && sma50 != null && sma200 != null) {
            boolean aboveAll = currentPrice.compareTo(sma20) > 0 && currentPrice.compareTo(sma50) > 0 && currentPrice.compareTo(sma200) > 0;
            boolean belowAll = currentPrice.compareTo(sma20) < 0 && currentPrice.compareTo(sma50) < 0 && currentPrice.compareTo(sma200) < 0;
            if (aboveAll) buySignals += 2; // Güçlü AL
            else if (belowAll) sellSignals += 2; // Güçlü SAT
            else {
                if (currentPrice.compareTo(sma20) > 0) buySignals++;
                else sellSignals++;
                if (currentPrice.compareTo(sma50) > 0) buySignals++;
                else sellSignals++;
                if (currentPrice.compareTo(sma200) > 0) buySignals++;
                else sellSignals++;
            }
            // Trend dizilimi
            if (sma20.compareTo(sma50) > 0 && sma50.compareTo(sma200) > 0) buySignals++; // Pozitif trend
            if (sma20.compareTo(sma50) < 0 && sma50.compareTo(sma200) < 0) sellSignals++; // Negatif trend
        } else if (sma20 != null) {
            if (currentPrice.compareTo(sma20) > 0) buySignals++;
            else sellSignals++;
        }

        // Çoğunluk oyuna göre karar ver
        if (buySignals > sellSignals) {
            return SignalDto.SignalType.BUY;
        } else if (sellSignals > buySignals) {
            return SignalDto.SignalType.SELL;
        } else {
            return SignalDto.SignalType.HOLD;
        }
    }
    
    /**
     * Sinyal için açıklama üretir
     */
    private String generateReasoning(BigDecimal rsi, IndicatorService.MACDResult macd, 
                                   BigDecimal currentPrice, BigDecimal sma20, BigDecimal sma50, BigDecimal sma200, SignalDto.SignalType signal) {
        StringBuilder reasoning = new StringBuilder();
        
        reasoning.append("RSI: ").append(rsi).append(" ");
        if (rsi.compareTo(BigDecimal.valueOf(30)) < 0) {
            reasoning.append("(Aşırı satım) ");
        } else if (rsi.compareTo(BigDecimal.valueOf(70)) > 0) {
            reasoning.append("(Aşırı alım) ");
        } else {
            reasoning.append("(Nötr) ");
        }
        
        reasoning.append("| MACD: ").append(macd.macdLine).append(" ");
        if (macd.macdLine.compareTo(macd.signalLine) > 0) {
            reasoning.append("(Yükseliş) ");
        } else {
            reasoning.append("(Düşüş) ");
        }
        
        reasoning.append("| Fiyat SMA20: ");
        if (sma20 != null && currentPrice.compareTo(sma20) > 0) {
            reasoning.append("Üstünde ");
        } else {
            reasoning.append("Altında ");
        }
        reasoning.append("| Fiyat SMA50: ");
        if (sma50 != null && currentPrice.compareTo(sma50) > 0) {
            reasoning.append("Üstünde ");
        } else {
            reasoning.append("Altında ");
        }
        reasoning.append("| Fiyat SMA200: ");
        if (sma200 != null && currentPrice.compareTo(sma200) > 0) {
            reasoning.append("Üstünde ");
        } else {
            reasoning.append("Altında ");
        }
        if (sma20 != null && sma50 != null && sma200 != null) {
            if (sma20.compareTo(sma50) > 0 && sma50.compareTo(sma200) > 0) {
                reasoning.append("| Trend: Pozitif (SMA20 > SMA50 > SMA200) ");
            } else if (sma20.compareTo(sma50) < 0 && sma50.compareTo(sma200) < 0) {
                reasoning.append("| Trend: Negatif (SMA20 < SMA50 < SMA200) ");
            }
        }
        reasoning.append("| Sinyal: ").append(signal.getDisplayText());
        
        return reasoning.toString();
    }
    
    /**
     * Belirli bir tarih aralığındaki fiyat verilerini getirir
     */
    public List<PriceDto> getPriceDataByDateRange(PriceEntity.IntervalType intervalType, 
                                                 LocalDateTime startDate, LocalDateTime endDate) {
        List<PriceEntity> entities = priceRepository.findByIntervalTypeAndDateRange(intervalType, startDate, endDate);
        return entities.stream()
                .map(PriceDto::new)
                .collect(Collectors.toList());
    }
    
    /**
     * En son fiyat verisini getirir
     */
    public PriceDto getLatestPrice(PriceEntity.IntervalType intervalType) {
        PriceEntity entity = priceRepository.findLatestByIntervalType(intervalType);
        return entity != null ? new PriceDto(entity) : null;
    }

    // Daha agresif sinyal: herhangi bir gösterge AL/SAT diyorsa onu döndür
    private SignalDto.SignalType determineAggressiveSignal(BigDecimal rsi, IndicatorService.MACDResult macd, String macdSignal, BigDecimal currentPrice, BigDecimal sma20, BigDecimal sma50, BigDecimal sma200) {
        // RSI
        String rsiSignal = indicatorService.generateRSISignal(rsi);
        if ("BUY".equals(rsiSignal)) return SignalDto.SignalType.BUY;
        if ("SELL".equals(rsiSignal)) return SignalDto.SignalType.SELL;
        // MACD
        if ("BUY".equals(macdSignal)) return SignalDto.SignalType.BUY;
        if ("SELL".equals(macdSignal)) return SignalDto.SignalType.SELL;
        // SMA
        if (sma20 != null && currentPrice.compareTo(sma20) > 0) return SignalDto.SignalType.BUY;
        if (sma20 != null && currentPrice.compareTo(sma20) < 0) return SignalDto.SignalType.SELL;
        // Diğer göstergeler eklenebilir
        return SignalDto.SignalType.HOLD;
    }

    /**
     * Detaylı analiz sonuçlarını tutan sınıf
     */
    public static class DetailedAnalysisResult {
        public final SignalDto.SignalType signal;
        public final BigDecimal entryPrice;
        public final BigDecimal stopLoss;
        public final BigDecimal takeProfit;
        public final String reasoning;
        public final String entryExplanation;
        public final String sltpExplanation;
        public final int buySignals;
        public final int sellSignals;
        public final String rsiAnalysis;
        public final String macdAnalysis;
        public final String trendAnalysis;
        public final String bollingerAnalysis;
        public final String stochasticAnalysis;
        public final String adxAnalysis;
        public final String ichimokuAnalysis;
        
        public DetailedAnalysisResult(SignalDto.SignalType signal, BigDecimal entryPrice, BigDecimal stopLoss, 
                                    BigDecimal takeProfit, String reasoning, String entryExplanation, 
                                    String sltpExplanation, int buySignals, int sellSignals, 
                                    String rsiAnalysis, String macdAnalysis, String trendAnalysis,
                                    String bollingerAnalysis, String stochasticAnalysis, String adxAnalysis, String ichimokuAnalysis) {
            this.signal = signal;
            this.entryPrice = entryPrice;
            this.stopLoss = stopLoss;
            this.takeProfit = takeProfit;
            this.reasoning = reasoning;
            this.entryExplanation = entryExplanation;
            this.sltpExplanation = sltpExplanation;
            this.buySignals = buySignals;
            this.sellSignals = sellSignals;
            this.rsiAnalysis = rsiAnalysis;
            this.macdAnalysis = macdAnalysis;
            this.trendAnalysis = trendAnalysis;
            this.bollingerAnalysis = bollingerAnalysis;
            this.stochasticAnalysis = stochasticAnalysis;
            this.adxAnalysis = adxAnalysis;
            this.ichimokuAnalysis = ichimokuAnalysis;
        }
    }
    
    /**
     * Detaylı analiz yapar ve entry, stop loss, take profit tahminleri üretir
     */
    private DetailedAnalysisResult performDetailedAnalysis(BigDecimal rsi, IndicatorService.MACDResult macd, 
                                                          BigDecimal currentPrice, BigDecimal sma20, BigDecimal sma50, 
                                                          BigDecimal sma200, String macdSignal, BigDecimal atr,
                                                          IndicatorService.BollingerBandsResult boll, BigDecimal stochasticRsi,
                                                          BigDecimal adx, IndicatorService.IchimokuResult ichimoku, List<PriceEntity> prices) {
        
        // Sinyal analizi
        int buySignals = 0;
        int sellSignals = 0;
        
        // RSI analizi
        String rsiAnalysis = "RSI: " + rsi + " ";
        String rsiSignal = indicatorService.generateRSISignal(rsi);
        if ("BUY".equals(rsiSignal)) {
            buySignals++;
            rsiAnalysis += "(Aşırı satım - AL sinyali)";
        } else if ("SELL".equals(rsiSignal)) {
            sellSignals++;
            rsiAnalysis += "(Aşırı alım - SAT sinyali)";
        } else {
            rsiAnalysis += "(Nötr)";
        }
        
        // MACD analizi
        String macdAnalysis = "MACD: " + macd.macdLine + " ";
        if ("BUY".equals(macdSignal)) {
            buySignals++;
            macdAnalysis += "(Yükseliş sinyali)";
        } else if ("SELL".equals(macdSignal)) {
            sellSignals++;
            macdAnalysis += "(Düşüş sinyali)";
        } else {
            macdAnalysis += "(Nötr)";
        }
        
        // Bollinger Bands analizi
        String bollingerAnalysis = "Bollinger: ";
        if (boll.upper != null && boll.lower != null && boll.middle != null) {
            if (currentPrice.compareTo(boll.upper) > 0) {
                sellSignals++;
                bollingerAnalysis += "Fiyat üst bandın üstünde (SAT sinyali)";
            } else if (currentPrice.compareTo(boll.lower) < 0) {
                buySignals++;
                bollingerAnalysis += "Fiyat alt bandın altında (AL sinyali)";
            } else {
                bollingerAnalysis += "Fiyat bantlar arasında (Nötr)";
            }
        } else {
            bollingerAnalysis += "Hesaplanamadı";
        }
        
        // Stochastic RSI analizi
        String stochasticAnalysis = "Stoch RSI: " + stochasticRsi + " ";
        if (stochasticRsi != null) {
            if (stochasticRsi.compareTo(BigDecimal.valueOf(20)) < 0) {
                buySignals++;
                stochasticAnalysis += "(Aşırı satım - AL sinyali)";
            } else if (stochasticRsi.compareTo(BigDecimal.valueOf(80)) > 0) {
                sellSignals++;
                stochasticAnalysis += "(Aşırı alım - SAT sinyali)";
            } else {
                stochasticAnalysis += "(Nötr)";
            }
        } else {
            stochasticAnalysis += "Hesaplanamadı";
        }
        
        // ADX analizi (trend gücü)
        String adxAnalysis = "ADX: " + adx + " ";
        if (adx != null) {
            if (adx.compareTo(BigDecimal.valueOf(25)) > 0) {
                adxAnalysis += "(Güçlü trend)";
                // Güçlü trend varsa mevcut sinyalleri güçlendir
                if (buySignals > sellSignals) buySignals++;
                else if (sellSignals > buySignals) sellSignals++;
            } else {
                adxAnalysis += "(Zayıf trend)";
            }
        } else {
            adxAnalysis += "Hesaplanamadı";
        }
        
        // Ichimoku analizi
        String ichimokuAnalysis = "Ichimoku: ";
        if (ichimoku.tenkan != null && ichimoku.kijun != null && ichimoku.senkouA != null && ichimoku.senkouB != null) {
            // Tenkan/Kijun kesişimi
            if (ichimoku.tenkan.compareTo(ichimoku.kijun) > 0) {
                buySignals++;
                ichimokuAnalysis += "Tenkan > Kijun (AL) ";
            } else {
                sellSignals++;
                ichimokuAnalysis += "Tenkan < Kijun (SAT) ";
            }
            
            // Cloud pozisyonu
            BigDecimal cloudTop = ichimoku.senkouA.compareTo(ichimoku.senkouB) > 0 ? ichimoku.senkouA : ichimoku.senkouB;
            BigDecimal cloudBottom = ichimoku.senkouA.compareTo(ichimoku.senkouB) < 0 ? ichimoku.senkouA : ichimoku.senkouB;
            
            if (currentPrice.compareTo(cloudTop) > 0) {
                buySignals++;
                ichimokuAnalysis += "| Fiyat bulutun üstünde (AL) ";
            } else if (currentPrice.compareTo(cloudBottom) < 0) {
                sellSignals++;
                ichimokuAnalysis += "| Fiyat bulutun altında (SAT) ";
            } else {
                ichimokuAnalysis += "| Fiyat bulut içinde (Nötr) ";
            }
        } else {
            ichimokuAnalysis += "Hesaplanamadı";
        }
        
        // SuperTrend analizi
        BigDecimal superTrend = indicatorService.calculateSuperTrend(prices, 10, 3.0);
        String superTrendAnalysis = "SuperTrend: ";
        if (superTrend != null && superTrend.compareTo(BigDecimal.ZERO) != 0) {
            if (currentPrice.compareTo(superTrend) > 0) {
                buySignals++;
                superTrendAnalysis += "Uptrend (AL sinyali)";
            } else if (currentPrice.compareTo(superTrend) < 0) {
                sellSignals++;
                superTrendAnalysis += "Downtrend (SAT sinyali)";
            } else {
                superTrendAnalysis += "Nötr";
            }
        } else {
            superTrendAnalysis += "Hesaplanamadı";
        }
        
        // VWAP analizi
        BigDecimal vwap = indicatorService.calculateVWAP(prices);
        String vwapAnalysis = "VWAP: ";
        if (vwap != null && vwap.compareTo(BigDecimal.ZERO) != 0) {
            if (currentPrice.compareTo(vwap) > 0) {
                buySignals++;
                vwapAnalysis += "Fiyat VWAP'in üstünde (AL sinyali)";
            } else if (currentPrice.compareTo(vwap) < 0) {
                sellSignals++;
                vwapAnalysis += "Fiyat VWAP'in altında (SAT sinyali)";
            } else {
                vwapAnalysis += "Fiyat VWAP'e eşit (Nötr)";
            }
        } else {
            vwapAnalysis += "Hesaplanamadı";
        }
        
        // Trend analizi (SMA'lar)
        String trendAnalysis = "";
        if (sma20 != null && sma50 != null && sma200 != null) {
            boolean aboveAll = currentPrice.compareTo(sma20) > 0 && currentPrice.compareTo(sma50) > 0 && currentPrice.compareTo(sma200) > 0;
            boolean belowAll = currentPrice.compareTo(sma20) < 0 && currentPrice.compareTo(sma50) < 0 && currentPrice.compareTo(sma200) < 0;
            
            if (aboveAll) {
                buySignals += 2;
                trendAnalysis = "Güçlü yükseliş trendi (Fiyat tüm SMA'ların üstünde)";
            } else if (belowAll) {
                sellSignals += 2;
                trendAnalysis = "Güçlü düşüş trendi (Fiyat tüm SMA'ların altında)";
            } else {
                trendAnalysis = "Karışık trend";
                if (currentPrice.compareTo(sma20) > 0) buySignals++;
                else sellSignals++;
                if (currentPrice.compareTo(sma50) > 0) buySignals++;
                else sellSignals++;
                if (currentPrice.compareTo(sma200) > 0) buySignals++;
                else sellSignals++;
            }
            
            // SMA dizilimi
            if (sma20.compareTo(sma50) > 0 && sma50.compareTo(sma200) > 0) {
                buySignals++;
                trendAnalysis += " | Pozitif SMA dizilimi (SMA20 > SMA50 > SMA200)";
            } else if (sma20.compareTo(sma50) < 0 && sma50.compareTo(sma200) < 0) {
                sellSignals++;
                trendAnalysis += " | Negatif SMA dizilimi (SMA20 < SMA50 < SMA200)";
            }
        }
        
        // Sinyal belirleme
        SignalDto.SignalType signal;
        if (buySignals > sellSignals) {
            signal = SignalDto.SignalType.BUY;
        } else if (sellSignals > buySignals) {
            signal = SignalDto.SignalType.SELL;
        } else {
            signal = SignalDto.SignalType.HOLD;
        }
        
        // Entry, Stop Loss, Take Profit hesaplama
        BigDecimal entryPrice = currentPrice;
        BigDecimal stopLoss = null;
        BigDecimal takeProfit = null;
        String entryExplanation = "";
        String sltpExplanation = "";
        
        if (signal == SignalDto.SignalType.BUY) {
            // LONG pozisyon için tahminler
            entryExplanation = "LONG Entry: Mevcut fiyattan giriş önerilir";
            
            // Stop Loss seçenekleri
            if (sma20 != null) {
                stopLoss = sma20.subtract(atr.multiply(BigDecimal.valueOf(0.5)));
                sltpExplanation += "Stop Loss: SMA20 - 0.5xATR = " + stopLoss + " ";
            }
            
            // Gelişmiş Take Profit hesaplama
            BigDecimal tp1 = null, tp2 = null, tp3 = null;
            String tpExplanation = "";
            
            // TP1: Bollinger üst bandı
            if (boll.upper != null) {
                tp1 = boll.upper;
                tpExplanation += "TP1(Bollinger Üst Band): " + tp1 + " ";
            }
            
            // TP2: Ichimoku bulut üst sınırı
            if (ichimoku.senkouA != null && ichimoku.senkouB != null) {
                BigDecimal cloudTop = ichimoku.senkouA.compareTo(ichimoku.senkouB) > 0 ? ichimoku.senkouA : ichimoku.senkouB;
                tp2 = cloudTop;
                tpExplanation += "TP2(Ichimoku Bulut Üst): " + tp2 + " ";
            }
            
            // TP3: Fibonacci 1.618 seviyesi (son dip'ten)
            BigDecimal recentLow = findRecentLow(prices, 20);
            if (recentLow != null) {
                BigDecimal range = currentPrice.subtract(recentLow);
                tp3 = currentPrice.add(range.multiply(BigDecimal.valueOf(1.618)));
                tpExplanation += "TP3(Fibonacci 1.618): " + tp3 + " (Fiyat: " + currentPrice + " + (" + currentPrice + " - " + recentLow + ") × 1.618) ";
            }
            
            // En uygun TP'yi seç (en yakın ama yeterli mesafede)
            takeProfit = selectBestTakeProfit(tp1, tp2, tp3, currentPrice, atr);
            sltpExplanation += "Take Profit: " + takeProfit + " (" + tpExplanation + ")";
            
            // Seçim mantığını açıkla
            if (takeProfit != null) {
                if (tp1 != null && takeProfit.compareTo(tp1) == 0) {
                    sltpExplanation += " | Seçilen: Bollinger Üst Band (En yakın geçerli seviye)";
                } else if (tp2 != null && takeProfit.compareTo(tp2) == 0) {
                    sltpExplanation += " | Seçilen: Ichimoku Bulut Üst (Trend bazlı direnç)";
                } else if (tp3 != null && takeProfit.compareTo(tp3) == 0) {
                    sltpExplanation += " | Seçilen: Fibonacci 1.618 (Altın oran seviyesi)";
                } else {
                    sltpExplanation += " | Seçilen: Varsayılan 2xATR (Diğer seviyeler çok uzak)";
                }
            }
            
        } else if (signal == SignalDto.SignalType.SELL) {
            // SHORT pozisyon için tahminler
            entryExplanation = "SHORT Entry: Mevcut fiyattan giriş önerilir";
            
            // Stop Loss seçenekleri
            if (sma20 != null) {
                stopLoss = sma20.add(atr.multiply(BigDecimal.valueOf(0.5)));
                sltpExplanation += "Stop Loss: SMA20 + 0.5xATR = " + stopLoss + " ";
            }
            
            // Gelişmiş Take Profit hesaplama
            BigDecimal tp1 = null, tp2 = null, tp3 = null;
            String tpExplanation = "";
            
            // TP1: Bollinger alt bandı
            if (boll.lower != null) {
                tp1 = boll.lower;
                tpExplanation += "TP1(Bollinger Alt Band): " + tp1 + " ";
            }
            
            // TP2: Ichimoku bulut alt sınırı
            if (ichimoku.senkouA != null && ichimoku.senkouB != null) {
                BigDecimal cloudBottom = ichimoku.senkouA.compareTo(ichimoku.senkouB) < 0 ? ichimoku.senkouA : ichimoku.senkouB;
                tp2 = cloudBottom;
                tpExplanation += "TP2(Ichimoku Bulut Alt): " + tp2 + " ";
            }
            
            // TP3: Fibonacci 1.618 seviyesi (son tepe'den)
            BigDecimal recentHigh = findRecentHigh(prices, 20);
            if (recentHigh != null) {
                BigDecimal range = recentHigh.subtract(currentPrice);
                tp3 = currentPrice.subtract(range.multiply(BigDecimal.valueOf(1.618)));
                tpExplanation += "TP3(Fibonacci 1.618): " + tp3 + " (Fiyat: " + currentPrice + " - (" + recentHigh + " - " + currentPrice + ") × 1.618) ";
            }
            
            // En uygun TP'yi seç (SHORT için)
            takeProfit = selectBestTakeProfitShort(tp1, tp2, tp3, currentPrice, atr);
            sltpExplanation += "Take Profit: " + takeProfit + " (" + tpExplanation + ")";
            
            // Seçim mantığını açıkla
            if (takeProfit != null) {
                if (tp1 != null && takeProfit.compareTo(tp1) == 0) {
                    sltpExplanation += " | Seçilen: Bollinger Alt Band (En yakın geçerli seviye)";
                } else if (tp2 != null && takeProfit.compareTo(tp2) == 0) {
                    sltpExplanation += " | Seçilen: Ichimoku Bulut Alt (Trend bazlı destek)";
                } else if (tp3 != null && takeProfit.compareTo(tp3) == 0) {
                    sltpExplanation += " | Seçilen: Fibonacci 1.618 (Altın oran seviyesi)";
                } else {
                    sltpExplanation += " | Seçilen: Varsayılan 2xATR (Diğer seviyeler çok uzak)";
                }
            }
            
        } else {
            entryExplanation = "HOLD: İşlem önerilmez";
            sltpExplanation = "Nötr durum - stop loss ve take profit hesaplanmadı";
        }
        
        // Genel açıklama - tüm indikatörleri dahil et
        StringBuilder reasoning = new StringBuilder();
        reasoning.append("Analiz Sonucu: ").append(buySignals).append(" AL sinyali, ").append(sellSignals).append(" SAT sinyali. ");
        reasoning.append(rsiAnalysis).append(" | ").append(macdAnalysis).append(" | ").append(bollingerAnalysis).append(" | ");
        reasoning.append(stochasticAnalysis).append(" | ").append(adxAnalysis).append(" | ").append(ichimokuAnalysis).append(" | ");
        reasoning.append(superTrendAnalysis).append(" | ").append(vwapAnalysis);
        
        return new DetailedAnalysisResult(
            signal, entryPrice, stopLoss, takeProfit, reasoning.toString(), entryExplanation, sltpExplanation,
            buySignals, sellSignals, rsiAnalysis, macdAnalysis, trendAnalysis, bollingerAnalysis, stochasticAnalysis, adxAnalysis, ichimokuAnalysis + " | " + superTrendAnalysis + " | " + vwapAnalysis
        );
    }
    
    /**
     * Son N bar içindeki en düşük fiyatı bulur
     */
    private BigDecimal findRecentLow(List<PriceEntity> prices, int bars) {
        if (prices == null || prices.isEmpty() || bars <= 0) return null;
        
        int startIndex = Math.max(0, prices.size() - bars);
        BigDecimal lowest = prices.get(startIndex).getLowPrice();
        
        for (int i = startIndex; i < prices.size(); i++) {
            BigDecimal low = prices.get(i).getLowPrice();
            if (low.compareTo(lowest) < 0) {
                lowest = low;
            }
        }
        return lowest;
    }
    
    /**
     * Son N bar içindeki en yüksek fiyatı bulur
     */
    private BigDecimal findRecentHigh(List<PriceEntity> prices, int bars) {
        if (prices == null || prices.isEmpty() || bars <= 0) return null;
        
        int startIndex = Math.max(0, prices.size() - bars);
        BigDecimal highest = prices.get(startIndex).getHighPrice();
        
        for (int i = startIndex; i < prices.size(); i++) {
            BigDecimal high = prices.get(i).getHighPrice();
            if (high.compareTo(highest) > 0) {
                highest = high;
            }
        }
        return highest;
    }
    
    /**
     * En uygun Take Profit seviyesini seçer
     */
    private BigDecimal selectBestTakeProfit(BigDecimal tp1, BigDecimal tp2, BigDecimal tp3, 
                                          BigDecimal currentPrice, BigDecimal atr) {
        List<BigDecimal> validTPs = new ArrayList<>();
        
        // Geçerli TP'leri topla (null olmayan ve yeterli mesafede olan)
        if (tp1 != null && tp1.compareTo(currentPrice) > 0) {
            BigDecimal distance = tp1.subtract(currentPrice);
            if (distance.compareTo(atr.multiply(BigDecimal.valueOf(0.5))) > 0) {
                validTPs.add(tp1);
            }
        }
        
        if (tp2 != null && tp2.compareTo(currentPrice) > 0) {
            BigDecimal distance = tp2.subtract(currentPrice);
            if (distance.compareTo(atr.multiply(BigDecimal.valueOf(0.5))) > 0) {
                validTPs.add(tp2);
            }
        }
        
        if (tp3 != null && tp3.compareTo(currentPrice) > 0) {
            BigDecimal distance = tp3.subtract(currentPrice);
            if (distance.compareTo(atr.multiply(BigDecimal.valueOf(0.5))) > 0) {
                validTPs.add(tp3);
            }
        }
        
        // En yakın geçerli TP'yi seç
        if (!validTPs.isEmpty()) {
            BigDecimal bestTP = validTPs.get(0);
            for (BigDecimal tp : validTPs) {
                if (tp.compareTo(bestTP) < 0) {
                    bestTP = tp;
                }
            }
            return bestTP;
        }
        
        // Hiç geçerli TP yoksa varsayılan hesapla
        return currentPrice.add(atr.multiply(BigDecimal.valueOf(2)));
    }
    
    /**
     * SHORT pozisyonlar için en uygun Take Profit seviyesini seçer
     */
    private BigDecimal selectBestTakeProfitShort(BigDecimal tp1, BigDecimal tp2, BigDecimal tp3, 
                                               BigDecimal currentPrice, BigDecimal atr) {
        List<BigDecimal> validTPs = new ArrayList<>();
        
        // Geçerli TP'leri topla (null olmayan ve yeterli mesafede olan)
        if (tp1 != null && tp1.compareTo(currentPrice) < 0) {
            BigDecimal distance = currentPrice.subtract(tp1);
            if (distance.compareTo(atr.multiply(BigDecimal.valueOf(0.5))) > 0) {
                validTPs.add(tp1);
            }
        }
        
        if (tp2 != null && tp2.compareTo(currentPrice) < 0) {
            BigDecimal distance = currentPrice.subtract(tp2);
            if (distance.compareTo(atr.multiply(BigDecimal.valueOf(0.5))) > 0) {
                validTPs.add(tp2);
            }
        }
        
        if (tp3 != null && tp3.compareTo(currentPrice) < 0) {
            BigDecimal distance = currentPrice.subtract(tp3);
            if (distance.compareTo(atr.multiply(BigDecimal.valueOf(0.5))) > 0) {
                validTPs.add(tp3);
            }
        }
        
        // En yakın geçerli TP'yi seç (SHORT için en yüksek değer)
        if (!validTPs.isEmpty()) {
            BigDecimal bestTP = validTPs.get(0);
            for (BigDecimal tp : validTPs) {
                if (tp.compareTo(bestTP) > 0) {
                    bestTP = tp;
                }
            }
            return bestTP;
        }
        
        // Hiç geçerli TP yoksa varsayılan hesapla
        return currentPrice.subtract(atr.multiply(BigDecimal.valueOf(2)));
    }
} 