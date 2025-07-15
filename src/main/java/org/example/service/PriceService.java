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
        
        // Sinyal üret
        SignalDto.SignalType signal = determineSignal(rsi, macd, currentPrice, sma20, sma50, sma200);
        String reasoning = generateReasoning(rsi, macd, currentPrice, sma20, sma50, sma200, signal);
        
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
        
        // --- Stop Loss & Take Profit Hesaplama ---
        // 1. Destek/Direnç (son dip/tepe)
        BigDecimal lastSupport = null;
        BigDecimal lastResistance = null;
        for (int i = 1; i < prices.size(); i++) {
            if (prices.get(i).getLowPrice().compareTo(prices.get(i-1).getLowPrice()) < 0) {
                lastSupport = prices.get(i).getLowPrice();
            }
            if (prices.get(i).getHighPrice().compareTo(prices.get(i-1).getHighPrice()) > 0) {
                lastResistance = prices.get(i).getHighPrice();
            }
        }
        // 2. EMA/MA
        BigDecimal stopEma = ema12;
        BigDecimal tpEma = sma50;
        // 3. RSI/MACD
        boolean rsiBuy = rsi.compareTo(BigDecimal.valueOf(30)) < 0;
        boolean rsiSell = rsi.compareTo(BigDecimal.valueOf(70)) > 0;
        boolean macdSell = macd.macdLine.compareTo(macd.signalLine) < 0;
        boolean macdBuy = macd.macdLine.compareTo(macd.signalLine) > 0;
        // 4. ATR
        BigDecimal atr = indicatorService.calculateATR(prices, 14);
        // --- Hesaplama ---
        BigDecimal stopLoss = null;
        BigDecimal takeProfit = null;
        StringBuilder sltpExplain = new StringBuilder();
        if (signal == SignalDto.SignalType.BUY) {
            // Destek/Direnç
            if (lastSupport != null) {
                stopLoss = lastSupport.subtract(atr);
                sltpExplain.append("Stop: Son destek - ATR (Destek/Direnç)");
            }
            if (lastResistance != null) {
                takeProfit = lastResistance;
                sltpExplain.append(", TP: İlk direnç (Destek/Direnç)");
            }
            // EMA/MA
            if (stopLoss == null && stopEma != null) {
                stopLoss = stopEma;
                sltpExplain.append(" | Stop: EMA12 (EMA/MA)");
            }
            if (takeProfit == null && tpEma != null) {
                takeProfit = tpEma;
                sltpExplain.append(" | TP: SMA50 (EMA/MA)");
            }
            // RSI/MACD
            if (stopLoss == null && rsiBuy && macdSell) {
                stopLoss = currentPrice.multiply(BigDecimal.valueOf(0.98));
                sltpExplain.append(" | Stop: RSI<30, MACD sat (RSI/MACD)");
            }
            if (takeProfit == null && rsiSell && macdBuy) {
                takeProfit = currentPrice.multiply(BigDecimal.valueOf(1.03));
                sltpExplain.append(" | TP: RSI>70, MACD al (RSI/MACD)");
            }
            // ATR
            if (stopLoss == null && atr.compareTo(BigDecimal.ZERO) > 0) {
                stopLoss = currentPrice.subtract(atr);
                sltpExplain.append(" | Stop: 1xATR (ATR)");
            }
            if (takeProfit == null && atr.compareTo(BigDecimal.ZERO) > 0) {
                takeProfit = currentPrice.add(atr.multiply(BigDecimal.valueOf(2)));
                sltpExplain.append(" | TP: 2xATR (ATR)");
            }
        } else if (signal == SignalDto.SignalType.SELL) {
            // Destek/Direnç
            if (lastResistance != null) {
                stopLoss = lastResistance.add(atr);
                sltpExplain.append("Stop: Son direnç + ATR (Destek/Direnç)");
            }
            if (lastSupport != null) {
                takeProfit = lastSupport;
                sltpExplain.append(", TP: İlk destek (Destek/Direnç)");
            }
            // EMA/MA
            if (stopLoss == null && stopEma != null) {
                stopLoss = stopEma;
                sltpExplain.append(" | Stop: EMA12 (EMA/MA)");
            }
            if (takeProfit == null && tpEma != null) {
                takeProfit = tpEma;
                sltpExplain.append(" | TP: SMA50 (EMA/MA)");
            }
            // RSI/MACD
            if (stopLoss == null && rsiSell && macdBuy) {
                stopLoss = currentPrice.multiply(BigDecimal.valueOf(1.02));
                sltpExplain.append(" | Stop: RSI>70, MACD al (RSI/MACD)");
            }
            if (takeProfit == null && rsiBuy && macdSell) {
                takeProfit = currentPrice.multiply(BigDecimal.valueOf(0.97));
                sltpExplain.append(" | TP: RSI<30, MACD sat (RSI/MACD)");
            }
            // ATR
            if (stopLoss == null && atr.compareTo(BigDecimal.ZERO) > 0) {
                stopLoss = currentPrice.add(atr);
                sltpExplain.append(" | Stop: 1xATR (ATR)");
            }
            if (takeProfit == null && atr.compareTo(BigDecimal.ZERO) > 0) {
                takeProfit = currentPrice.subtract(atr.multiply(BigDecimal.valueOf(2)));
                sltpExplain.append(" | TP: 2xATR (ATR)");
            }
        }
        signalDto.setStopLoss(stopLoss);
        signalDto.setTakeProfit(takeProfit);
        signalDto.setSltpExplanation(sltpExplain.toString());
        // --- Minimum fark kontrolü ---
        BigDecimal minDiffRatio = new BigDecimal("0.01"); // %1
        if (takeProfit != null && takeProfit.subtract(currentPrice).abs()
                .compareTo(currentPrice.multiply(minDiffRatio)) < 0) {
            takeProfit = null;
            sltpExplain.append(" | Uygun take profit seviyesi bulunamadı.");
        }
        if (stopLoss != null && stopLoss.subtract(currentPrice).abs()
                .compareTo(currentPrice.multiply(minDiffRatio)) < 0) {
            stopLoss = null;
            sltpExplain.append(" | Uygun stop loss seviyesi bulunamadı.");
        }
        // ---
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
                                               BigDecimal currentPrice, BigDecimal sma20, BigDecimal sma50, BigDecimal sma200) {
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
} 