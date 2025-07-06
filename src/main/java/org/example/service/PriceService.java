package org.example.service;

import org.example.dto.PriceDto;
import org.example.dto.SignalDto;
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
    
    public PriceService(PriceRepository priceRepository, IndicatorService indicatorService) {
        this.priceRepository = priceRepository;
        this.indicatorService = indicatorService;
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
        List<PriceEntity> entities = priceRepository.findLatestNByIntervalType(intervalType, limit);
        // Veriler DESC geliyor, grafikte doğru sıralama için ters çevir
        Collections.reverse(entities);
        List<PriceDto> dtos = new ArrayList<>();
        List<PriceEntity> subList;
        for (int i = 0; i < entities.size(); i++) {
            PriceEntity entity = entities.get(i);
            PriceDto dto = new PriceDto(entity);
            // SMA20
            subList = entities.subList(Math.max(0, i - 19), i + 1);
            dto.setSma20(indicatorService.calculateSMA(subList, 20));
            // SMA50
            subList = entities.subList(Math.max(0, i - 49), i + 1);
            dto.setSma50(indicatorService.calculateSMA(subList, 50));
            // SMA200
            subList = entities.subList(Math.max(0, i - 199), i + 1);
            dto.setSma200(indicatorService.calculateSMA(subList, 200));
            dtos.add(dto);
        }
        return dtos;
    }
    
    /**
     * Belirli bir interval için sinyal üretir
     */
    public SignalDto generateSignal(PriceEntity.IntervalType intervalType) {
        // Son 50 kaydı al (teknik analiz için yeterli veri)
        List<PriceEntity> prices = priceRepository.findLatestNByIntervalType(intervalType, 50);
        
        if (prices.isEmpty()) {
            return createEmptySignal(intervalType);
        }
        
        // En son fiyat
        PriceEntity latestPrice = prices.get(0);
        BigDecimal currentPrice = latestPrice.getClosePrice();
        
        // Teknik indikatörleri hesapla
        BigDecimal rsi = indicatorService.calculateRSI(prices);
        IndicatorService.MACDResult macd = indicatorService.calculateMACD(prices);
        BigDecimal sma20 = indicatorService.calculateSMA(prices, 20);
        BigDecimal sma50 = indicatorService.calculateSMA(prices, 50);
        BigDecimal sma200 = indicatorService.calculateSMA(prices, 200);
        BigDecimal ema12 = indicatorService.calculateEMA(prices, 12);
        
        // Sinyal üret
        SignalDto.SignalType signal = determineSignal(rsi, macd, currentPrice, sma20);
        String reasoning = generateReasoning(rsi, macd, currentPrice, sma20, signal);
        
        // SignalDto oluştur
        SignalDto signalDto = new SignalDto(signal, intervalType.getValue(), 
                                          latestPrice.getTimestamp(), currentPrice, reasoning);
        signalDto.setRsiValue(rsi);
        signalDto.setMacdValue(macd.macdLine);
        signalDto.setMacdSignal(macd.signalLine);
        signalDto.setMacdHistogram(macd.histogram);
        signalDto.setSma20(sma20);
        signalDto.setSma50(sma50);
        signalDto.setSma200(sma200);
        signalDto.setEma12(ema12);
        
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
                                               BigDecimal currentPrice, BigDecimal sma20) {
        int buySignals = 0;
        int sellSignals = 0;
        
        // RSI sinyali
        String rsiSignal = indicatorService.generateRSISignal(rsi);
        if ("BUY".equals(rsiSignal)) buySignals++;
        else if ("SELL".equals(rsiSignal)) sellSignals++;
        
        // MACD sinyali
        String macdSignal = indicatorService.generateMACDSignal(macd);
        if ("BUY".equals(macdSignal)) buySignals++;
        else if ("SELL".equals(macdSignal)) sellSignals++;
        
        // Moving Average sinyali
        if (currentPrice.compareTo(sma20) > 0) {
            buySignals++; // Fiyat SMA'nın üstünde
        } else {
            sellSignals++; // Fiyat SMA'nın altında
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
                                   BigDecimal currentPrice, BigDecimal sma20, SignalDto.SignalType signal) {
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
        if (currentPrice.compareTo(sma20) > 0) {
            reasoning.append("Üstünde ");
        } else {
            reasoning.append("Altında ");
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
} 