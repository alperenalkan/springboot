package org.example.service;

import org.example.entity.PriceEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IndicatorService {
    
    private static final Logger logger = LoggerFactory.getLogger(IndicatorService.class);
    
    @Value("${app.indicators.rsi.period:14}")
    private int rsiPeriod;
    
    @Value("${app.indicators.rsi.oversold:30}")
    private int rsiOversold;
    
    @Value("${app.indicators.rsi.overbought:70}")
    private int rsiOverbought;
    
    @Value("${app.indicators.macd.fast-period:12}")
    private int macdFastPeriod;
    
    @Value("${app.indicators.macd.slow-period:26}")
    private int macdSlowPeriod;
    
    @Value("${app.indicators.macd.signal-period:9}")
    private int macdSignalPeriod;
    
    @Value("${app.indicators.atr.use-ema:false}")
    private boolean useAtrEmaMethod;
    
    // Indicator cache
    private final ConcurrentHashMap<String, BigDecimal> indicatorCache = new ConcurrentHashMap<>();
    
    /**
     * RSI (Relative Strength Index) hesaplar
     */
    public BigDecimal calculateRSI(List<PriceEntity> prices) {
        if (prices.size() < rsiPeriod + 1) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;
        
        // İlk RSI periyodu için ortalama gain/loss hesapla
        for (int i = 1; i <= rsiPeriod; i++) {
            BigDecimal change = prices.get(i).getClosePrice().subtract(prices.get(i - 1).getClosePrice());
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                avgGain = avgGain.add(change);
            } else {
                avgLoss = avgLoss.add(change.abs());
            }
        }
        
        if (rsiPeriod > 0) {
            avgGain = avgGain.divide(BigDecimal.valueOf(rsiPeriod), 8, RoundingMode.HALF_UP);
            avgLoss = avgLoss.divide(BigDecimal.valueOf(rsiPeriod), 8, RoundingMode.HALF_UP);
        }
        
        // Kalan periyotlar için exponential moving average
        for (int i = rsiPeriod + 1; i < prices.size(); i++) {
            BigDecimal change = prices.get(i).getClosePrice().subtract(prices.get(i - 1).getClosePrice());
            BigDecimal gain = BigDecimal.ZERO;
            BigDecimal loss = BigDecimal.ZERO;
            
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gain = change;
            } else {
                loss = change.abs();
            }
            
            if (rsiPeriod > 0) {
                avgGain = avgGain.multiply(BigDecimal.valueOf(rsiPeriod - 1))
                        .add(gain)
                        .divide(BigDecimal.valueOf(rsiPeriod), 8, RoundingMode.HALF_UP);
                avgLoss = avgLoss.multiply(BigDecimal.valueOf(rsiPeriod - 1))
                        .add(loss)
                        .divide(BigDecimal.valueOf(rsiPeriod), 8, RoundingMode.HALF_UP);
            }
        }
        
        // Edge-case: hem gain hem loss sıfırsa, veri yetersiz veya sabit fiyat, RSI=0 döndür
        if (avgGain.compareTo(BigDecimal.ZERO) == 0 && avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        // Sadece loss sıfırsa RSI=100
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        
        BigDecimal rs = avgGain.divide(avgLoss, 8, RoundingMode.HALF_UP);
        BigDecimal rsi = BigDecimal.valueOf(100)
                .subtract(BigDecimal.valueOf(100)
                        .divide(BigDecimal.ONE.add(rs), 8, RoundingMode.HALF_UP));
        
        return rsi.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * MACD (Moving Average Convergence Divergence) hesaplar
     * Performans için EMA hesaplamaları incremental yapılır.
     */
    public MACDResult calculateMACD(List<PriceEntity> prices) {
        if (prices == null || prices.size() < macdSlowPeriod) {
            return new MACDResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        // Fast ve slow EMA'lar için incremental hesaplama
        List<BigDecimal> fastEmaList = new java.util.ArrayList<>();
        List<BigDecimal> slowEmaList = new java.util.ArrayList<>();
        List<BigDecimal> macdLines = new java.util.ArrayList<>();
        double fastMultiplier = 2.0 / (macdFastPeriod + 1);
        double slowMultiplier = 2.0 / (macdSlowPeriod + 1);
        // İlk fast EMA
        double fastEma = 0;
        double slowEma = 0;
        // İlk fast EMA için SMA
        if (prices.size() >= macdFastPeriod) {
            double sum = 0;
            for (int i = 0; i < macdFastPeriod; i++) {
                sum += prices.get(i).getClosePrice().doubleValue();
            }
            fastEma = sum / macdFastPeriod;
        }
        // İlk slow EMA için SMA
        if (prices.size() >= macdSlowPeriod) {
            double sum = 0;
            for (int i = 0; i < macdSlowPeriod; i++) {
                sum += prices.get(i).getClosePrice().doubleValue();
            }
            slowEma = sum / macdSlowPeriod;
        }
        // EMA'ları incremental hesapla
        for (int i = 0; i < prices.size(); i++) {
            double close = prices.get(i).getClosePrice().doubleValue();
            if (i >= macdFastPeriod) {
                fastEma = (close - fastEma) * fastMultiplier + fastEma;
            }
            if (i >= macdSlowPeriod) {
                slowEma = (close - slowEma) * slowMultiplier + slowEma;
            }
            if (i >= macdSlowPeriod) {
                fastEmaList.add(BigDecimal.valueOf(fastEma));
                slowEmaList.add(BigDecimal.valueOf(slowEma));
                macdLines.add(BigDecimal.valueOf(fastEma - slowEma));
            }
        }
        if (macdLines.isEmpty()) {
            return new MACDResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        // Son MACD Line
        BigDecimal macdLine = macdLines.get(macdLines.size() - 1);
        // Signal Line: MACD Line'ların EMA'sı (incremental EMA)
        BigDecimal macdSignal = calculateEMAFromValues(macdLines, macdSignalPeriod);
        // MACD Histogram
        BigDecimal macdHistogram = macdLine.subtract(macdSignal);
        return new MACDResult(
                macdLine.setScale(8, RoundingMode.HALF_UP),
                macdSignal.setScale(8, RoundingMode.HALF_UP),
                macdHistogram.setScale(8, RoundingMode.HALF_UP)
        );
    }
    
    /**
     * SMA (Simple Moving Average) hesaplar (cache destekli)
     */
    public BigDecimal calculateSMA(List<PriceEntity> prices, int period) {
        try {
            if (prices == null || prices.size() < period || period <= 0) {
                return BigDecimal.ZERO;
            }
            
            // Null kontrolü
            PriceEntity lastPrice = prices.get(prices.size() - 1);
            if (lastPrice == null || lastPrice.getTimestamp() == null) {
                return BigDecimal.ZERO;
            }
            
            String cacheKey = "SMA-" + period + "-" + lastPrice.getTimestamp();
            if (indicatorCache.containsKey(cacheKey)) {
                return indicatorCache.get(cacheKey);
            }
            
            BigDecimal sum = BigDecimal.ZERO;
            // Son 'period' kadar veriyi al (en yeni veriler)
            int startIndex = prices.size() - period;
            for (int i = startIndex; i < prices.size(); i++) {
                PriceEntity price = prices.get(i);
                if (price == null || price.getClosePrice() == null) {
                    return BigDecimal.ZERO;
                }
                sum = sum.add(price.getClosePrice());
            }
            
            BigDecimal sma = sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
            indicatorCache.put(cacheKey, sma);
            return sma;
            
        } catch (Exception e) {
            logger.error("Error calculating SMA for period {}: {}", period, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * EMA (Exponential Moving Average) hesaplar (cache destekli)
     */
    public BigDecimal calculateEMA(List<PriceEntity> prices, int period) {
        if (prices == null || prices.size() < period || period <= 0) {
            return BigDecimal.ZERO;
        }
        String cacheKey = "EMA-" + period + "-" + prices.get(prices.size() - 1).getTimestamp();
        if (indicatorCache.containsKey(cacheKey)) {
            return indicatorCache.get(cacheKey);
        }
        BigDecimal ema = calculateSMA(prices, period);
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        for (int i = period; i < prices.size(); i++) {
            BigDecimal currentPrice = prices.get(i).getClosePrice();
            ema = currentPrice.multiply(multiplier)
                    .add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
        }
        ema = ema.setScale(8, RoundingMode.HALF_UP);
        indicatorCache.put(cacheKey, ema);
        return ema;
    }
    
    // EMA hesaplaması: BigDecimal listesi için (MACD Signal Line için)
    public BigDecimal calculateEMAFromValues(List<BigDecimal> values, int period) {
        if (values == null || values.size() < period || period <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal ema = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            ema = ema.add(values.get(i));
        }
        ema = ema.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        for (int i = period; i < values.size(); i++) {
            ema = values.get(i).multiply(multiplier)
                    .add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
        }
        return ema.setScale(8, RoundingMode.HALF_UP);
    }
    
    /**
     * MACD Signal Line için EMA hesaplar
     */
    private BigDecimal calculateEMASignal(List<PriceEntity> prices, BigDecimal macdLine, int period) {
        // Bu basitleştirilmiş bir hesaplama - gerçek uygulamada MACD değerlerinin geçmişini tutmak gerekir
        return macdLine.multiply(BigDecimal.valueOf(2.0 / (period + 1)));
    }
    
    /**
     * ATR (Average True Range) hesaplar (cache destekli)
     */
    public BigDecimal calculateATR(List<PriceEntity> prices, int period) {
        String cacheKey = (useAtrEmaMethod ? "ATREMA-" : "ATRSMA-") + period + "-" + prices.get(prices.size() - 1).getTimestamp();
        if (indicatorCache.containsKey(cacheKey)) {
            return indicatorCache.get(cacheKey);
        }
        BigDecimal atr;
        if (useAtrEmaMethod) {
            atr = calculateATREMA(prices, period);
        } else {
            atr = calculateATRSMA(prices, period);
        }
        indicatorCache.put(cacheKey, atr);
        return atr;
    }

    /**
     * Klasik ATR (SMA) yöntemi
     */
    public BigDecimal calculateATRSMA(List<PriceEntity> prices, int period) {
        if (prices == null || prices.size() < period + 1 || period <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal atr = BigDecimal.ZERO;
        for (int i = 1; i <= period; i++) {
            PriceEntity current = prices.get(i);
            PriceEntity prev = prices.get(i - 1);
            BigDecimal highLow = current.getHighPrice().subtract(current.getLowPrice()).abs();
            BigDecimal highClose = current.getHighPrice().subtract(prev.getClosePrice()).abs();
            BigDecimal lowClose = current.getLowPrice().subtract(prev.getClosePrice()).abs();
            BigDecimal trueRange = highLow.max(highClose).max(lowClose);
            atr = atr.add(trueRange);
        }
        atr = atr.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        return atr;
    }

    /**
     * Wilder's EMA ile ATR hesaplama
     */
    public BigDecimal calculateATREMA(List<PriceEntity> prices, int period) {
        if (prices == null || prices.size() <= period || period <= 0) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> trueRanges = new java.util.ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            PriceEntity current = prices.get(i);
            PriceEntity prev = prices.get(i - 1);
            BigDecimal highLow = current.getHighPrice().subtract(current.getLowPrice()).abs();
            BigDecimal highClose = current.getHighPrice().subtract(prev.getClosePrice()).abs();
            BigDecimal lowClose = current.getLowPrice().subtract(prev.getClosePrice()).abs();
            BigDecimal trueRange = highLow.max(highClose).max(lowClose);
            trueRanges.add(trueRange);
        }
        return calculateEMAFromValues(trueRanges, period);
    }
    
    /**
     * Bollinger Bands hesaplar (SMA ± k * stddev)
     */
    public BollingerBandsResult calculateBollingerBands(List<PriceEntity> prices, int period, double k) {
        if (prices == null || prices.size() < period || period <= 0) {
            return new BollingerBandsResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal sma = calculateSMA(prices, period);
        BigDecimal sumSq = BigDecimal.ZERO;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            BigDecimal diff = prices.get(i).getClosePrice().subtract(sma);
            sumSq = sumSq.add(diff.multiply(diff));
        }
        BigDecimal stddev = BigDecimal.valueOf(Math.sqrt(sumSq.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP).doubleValue()));
        BigDecimal upper = sma.add(stddev.multiply(BigDecimal.valueOf(k)));
        BigDecimal lower = sma.subtract(stddev.multiply(BigDecimal.valueOf(k)));
        return new BollingerBandsResult(upper, sma, lower);
    }

    /**
     * EMA Tabanlı Bollinger Bands hesaplar (EMA ± k * stddev)
     */
    public BollingerBandsResult calculateBollingerBandsEMA(List<PriceEntity> prices, int period, double k) {
        if (prices == null || prices.size() < period || period <= 0) {
            return new BollingerBandsResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Middle band olarak EMA kullan
        BigDecimal ema = calculateEMA(prices, period);

        // Stddev hesaplaması için EMA etrafında varyans alıyoruz
        BigDecimal sumSq = BigDecimal.ZERO;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            BigDecimal diff = prices.get(i).getClosePrice().subtract(ema);
            sumSq = sumSq.add(diff.multiply(diff));
        }

        BigDecimal stddev = BigDecimal.valueOf(
            Math.sqrt(sumSq.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP).doubleValue())
        );

        BigDecimal upper = ema.add(stddev.multiply(BigDecimal.valueOf(k)));
        BigDecimal lower = ema.subtract(stddev.multiply(BigDecimal.valueOf(k)));

        return new BollingerBandsResult(upper, ema, lower);
    }
    public static class BollingerBandsResult {
        public final BigDecimal upper;
        public final BigDecimal middle;
        public final BigDecimal lower;
        public BollingerBandsResult(BigDecimal upper, BigDecimal middle, BigDecimal lower) {
            this.upper = upper;
            this.middle = middle;
            this.lower = lower;
        }
    }

    /**
     * Stochastic RSI hesaplar (optimized)
     */
    public BigDecimal calculateStochasticRSI(List<PriceEntity> prices, int period) {
        if (prices == null || prices.size() < period + 1 || period <= 0) {
            return BigDecimal.ZERO;
        }
        
        // RSI değerlerini önceden hesapla (cache kullan)
        List<BigDecimal> rsiValues = new java.util.ArrayList<>();
        for (int i = period; i < prices.size(); i++) {
            List<PriceEntity> subList = prices.subList(i - period, i + 1);
            String cacheKey = "RSI-" + period + "-" + subList.get(subList.size() - 1).getTimestamp();
            
            BigDecimal rsi;
            if (indicatorCache.containsKey(cacheKey)) {
                rsi = indicatorCache.get(cacheKey);
            } else {
                rsi = calculateRSI(subList);
                indicatorCache.put(cacheKey, rsi);
            }
            rsiValues.add(rsi);
        }
        
        if (rsiValues.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Son RSI değeri
        BigDecimal lastRsi = rsiValues.get(rsiValues.size() - 1);
        
        // Min/Max hesapla (stream yerine döngü kullan - daha hızlı)
        BigDecimal minRsi = lastRsi;
        BigDecimal maxRsi = lastRsi;
        
        for (BigDecimal rsi : rsiValues) {
            if (rsi.compareTo(minRsi) < 0) minRsi = rsi;
            if (rsi.compareTo(maxRsi) > 0) maxRsi = rsi;
        }
        
        if (maxRsi.equals(minRsi)) return BigDecimal.ZERO;
        
        return lastRsi.subtract(minRsi).divide(maxRsi.subtract(minRsi), 8, RoundingMode.HALF_UP);
    }

    /**
     * ADX (Average Directional Index) hesaplar (Wilder's smoothing)
     */
    public BigDecimal calculateADX(List<PriceEntity> prices, int period) {
        if (prices == null || prices.size() < period * 2 + 1 || period <= 0) {
            return BigDecimal.ZERO;
        }
        
        // True Range, +DM, -DM hesapla
        List<BigDecimal> tr = new java.util.ArrayList<>();
        List<BigDecimal> plusDM = new java.util.ArrayList<>();
        List<BigDecimal> minusDM = new java.util.ArrayList<>();
        
        for (int i = 1; i < prices.size(); i++) {
            PriceEntity current = prices.get(i);
            PriceEntity prev = prices.get(i - 1);
            
            // True Range
            BigDecimal highLow = current.getHighPrice().subtract(current.getLowPrice()).abs();
            BigDecimal highClose = current.getHighPrice().subtract(prev.getClosePrice()).abs();
            BigDecimal lowClose = current.getLowPrice().subtract(prev.getClosePrice()).abs();
            tr.add(highLow.max(highClose).max(lowClose));
            
            // Directional Movement
            BigDecimal upMove = current.getHighPrice().subtract(prev.getHighPrice());
            BigDecimal downMove = prev.getLowPrice().subtract(current.getLowPrice());
            
            if (upMove.compareTo(downMove) > 0 && upMove.compareTo(BigDecimal.ZERO) > 0) {
                plusDM.add(upMove);
                minusDM.add(BigDecimal.ZERO);
            } else if (downMove.compareTo(upMove) > 0 && downMove.compareTo(BigDecimal.ZERO) > 0) {
                plusDM.add(BigDecimal.ZERO);
                minusDM.add(downMove);
            } else {
                plusDM.add(BigDecimal.ZERO);
                minusDM.add(BigDecimal.ZERO);
            }
        }
        
        // Wilder's smoothing için ilk değerler
        BigDecimal smoothedTR = tr.get(0);
        BigDecimal smoothedPlusDM = plusDM.get(0);
        BigDecimal smoothedMinusDM = minusDM.get(0);
        
        // Wilder's smoothing uygula
        for (int i = 1; i < period; i++) {
            smoothedTR = smoothedTR.add(tr.get(i));
            smoothedPlusDM = smoothedPlusDM.add(plusDM.get(i));
            smoothedMinusDM = smoothedMinusDM.add(minusDM.get(i));
        }
        
        // DX değerlerini hesapla
        List<BigDecimal> dxValues = new java.util.ArrayList<>();
        
        for (int i = period; i < tr.size(); i++) {
            // Wilder's smoothing
            smoothedTR = smoothedTR.subtract(smoothedTR.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP))
                    .add(tr.get(i));
            smoothedPlusDM = smoothedPlusDM.subtract(smoothedPlusDM.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP))
                    .add(plusDM.get(i));
            smoothedMinusDM = smoothedMinusDM.subtract(smoothedMinusDM.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP))
                    .add(minusDM.get(i));
            
            // DI hesapla
            BigDecimal plusDI = smoothedPlusDM.divide(smoothedTR, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            BigDecimal minusDI = smoothedMinusDM.divide(smoothedTR, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            
            // DX hesapla
            BigDecimal diSum = plusDI.add(minusDI);
            if (diSum.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dx = plusDI.subtract(minusDI).abs().divide(diSum, 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                dxValues.add(dx);
            } else {
                dxValues.add(BigDecimal.ZERO);
            }
        }
        
        // ADX = DX değerlerinin ortalaması
        if (dxValues.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Son period kadar DX değerinin ortalaması
        int startIndex = Math.max(0, dxValues.size() - period);
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        
        for (int i = startIndex; i < dxValues.size(); i++) {
            sum = sum.add(dxValues.get(i));
            count++;
        }
        
        return count > 0 ? sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    /**
     * Ichimoku Cloud göstergesi için komple hesaplama (shift'ler dahil)
     * 
     * Ichimoku Cloud komple bir sistemdir:
     * - Tenkan (Conversion Line): 9 periyotluk (High+Low)/2
     * - Kijun (Base Line): 26 periyotluk (High+Low)/2  
     * - Senkou A (Leading Span A): (Tenkan + Kijun)/2, 26 bar ileriye kaydırılır
     * - Senkou B (Leading Span B): 52 periyotluk (High+Low)/2, 26 bar ileriye kaydırılır
     * - Chikou (Lagging Span): Kapanış fiyatı, 26 bar geriye kaydırılır
     */
    public IchimokuResult calculateIchimoku(List<PriceEntity> prices) {
        if (prices == null || prices.size() < 52) {
            return new IchimokuResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        try {
            // Temel hesaplamalar
            BigDecimal tenkan = calculateMid(prices, 9);
            BigDecimal kijun = calculateMid(prices, 26);
            
            // Senkou A = (Tenkan + Kijun) / 2
            BigDecimal senkouA = tenkan.add(kijun).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
            
            // Senkou B = 52 periyotluk (High+Low)/2
            BigDecimal senkouB = calculateMid(prices, 52);
            
            // Chikou: 26 bar geriye kaydırılmış kapanış fiyatı
            BigDecimal chikou = BigDecimal.ZERO;
            if (prices.size() >= 26) {
                chikou = prices.get(prices.size() - 26 - 1).getClosePrice();
            } else {
                chikou = prices.get(prices.size() - 1).getClosePrice();
            }
            
            // Senkou A ve B 26 bar ileriye kaydırılır (gelecek projeksiyon)
            // Bu değerler cloud'un gelecekteki destek/direnç seviyelerini gösterir
            // Gerçek uygulamada bu değerler grafikte 26 bar ileriye çizilir
            
            return new IchimokuResult(tenkan, kijun, senkouA, senkouB, chikou);
            
        } catch (Exception e) {
            logger.error("Error calculating Ichimoku: {}", e.getMessage());
            return new IchimokuResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }
    private BigDecimal calculateMid(List<PriceEntity> prices, int period) {
        int start = prices.size() - period;
        BigDecimal highest = prices.get(start).getHighPrice();
        BigDecimal lowest = prices.get(start).getLowPrice();
        for (int i = start; i < prices.size(); i++) {
            if (prices.get(i).getHighPrice().compareTo(highest) > 0) highest = prices.get(i).getHighPrice();
            if (prices.get(i).getLowPrice().compareTo(lowest) < 0) lowest = prices.get(i).getLowPrice();
        }
        return highest.add(lowest).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
    }
    public static class IchimokuResult {
        public final BigDecimal tenkan;
        public final BigDecimal kijun;
        public final BigDecimal senkouA;
        public final BigDecimal senkouB;
        public final BigDecimal chikou;
        public IchimokuResult(BigDecimal tenkan, BigDecimal kijun, BigDecimal senkouA, BigDecimal senkouB, BigDecimal chikou) {
            this.tenkan = tenkan;
            this.kijun = kijun;
            this.senkouA = senkouA;
            this.senkouB = senkouB;
            this.chikou = chikou;
        }
    }
    
    /**
     * RSI sinyali üretir (gelişmiş):
     * - 30 altı: BUY
     * - 70 üstü: SELL
     * - 50 üstü: hafif pozitif (BUY sinyali güçlenir)
     * - 50 altı: hafif negatif (SELL sinyali güçlenir)
     */
    public String generateRSISignal(BigDecimal rsi) {
        if (rsi == null) return "HOLD";
        if (rsi.compareTo(BigDecimal.valueOf(rsiOversold)) <= 0) {
            return "BUY";
        } else if (rsi.compareTo(BigDecimal.valueOf(rsiOverbought)) >= 0) {
            return "SELL";
        } else if (rsi.compareTo(BigDecimal.valueOf(50)) > 0) {
            return "BUY_WEAK";
        } else if (rsi.compareTo(BigDecimal.valueOf(50)) < 0) {
            return "SELL_WEAK";
        } else {
            return "HOLD";
        }
    }
    
    /**
     * MACD sinyali üretir (gelişmiş):
     * - MACD histogram sıfırdan pozitife geçerse: BUY
     * - MACD histogram sıfırdan negatife geçerse: SELL
     * - MACD line > signal line ve histogram > 0: BUY
     * - MACD line < signal line ve histogram < 0: SELL
     * - Diğer durumlarda: HOLD
     *
     * Not: Histogram geçişi için önceki histogram değeri gereklidir, bu fonksiyonun parametresi olarak eklenebilir.
     */
    public String generateMACDSignal(MACDResult macd, BigDecimal prevHistogram) {
        if (macd == null) return "HOLD";
        // Histogram sıfır geçişi
        if (prevHistogram != null) {
            if (prevHistogram.compareTo(BigDecimal.ZERO) < 0 && macd.histogram.compareTo(BigDecimal.ZERO) > 0) {
                return "BUY";
            } else if (prevHistogram.compareTo(BigDecimal.ZERO) > 0 && macd.histogram.compareTo(BigDecimal.ZERO) < 0) {
                return "SELL";
            }
        }
        if (macd.macdLine.compareTo(macd.signalLine) > 0 && macd.histogram.compareTo(BigDecimal.ZERO) > 0) {
            return "BUY";
        } else if (macd.macdLine.compareTo(macd.signalLine) < 0 && macd.histogram.compareTo(BigDecimal.ZERO) < 0) {
            return "SELL";
        } else {
            return "HOLD";
        }
    }
    
    /**
     * Ichimoku Cloud sinyali üretir (basit versiyon - geriye uyumluluk için)
     */
    public String generateIchimokuSignal(IchimokuResult ichimoku, BigDecimal currentPrice) {
        IchimokuSignalResult result = generateIchimokuSignalWithPredictions(ichimoku, currentPrice);
        return result.signal;
    }
    
    /**
     * Ichimoku Cloud sinyali ve fiyat tahminleri üretir (komple sistem):
     * - Tenkan > Kijun: Yükseliş trendi
     * - Tenkan < Kijun: Düşüş trendi
     * - Fiyat > Senkou A ve Senkou B: Güçlü yükseliş
     * - Fiyat < Senkou A ve Senkou B: Güçlü düşüş
     * - Chikou > Fiyat (26 bar önce): Geçmişte yükseliş
     * - Chikou < Fiyat (26 bar önce): Geçmişte düşüş
     * 
     * Fiyat Tahminleri:
     * - Long Entry: Cloud'un üst sınırı (Senkou A veya B'nin yükseği)
     * - Short Entry: Cloud'un alt sınırı (Senkou A veya B'nin düşüğü)
     * - Stop Loss: Cloud'un karşı sınırı
     * - Take Profit: Trend yönünde 2x cloud mesafesi
     */
    public IchimokuSignalResult generateIchimokuSignalWithPredictions(IchimokuResult ichimoku, BigDecimal currentPrice) {
        if (ichimoku == null || currentPrice == null) {
            return new IchimokuSignalResult("HOLD", null, null, null, null, "");
        }
        
        try {
            // Trend yönü (Tenkan vs Kijun)
            boolean bullishTrend = ichimoku.tenkan.compareTo(ichimoku.kijun) > 0;
            boolean bearishTrend = ichimoku.tenkan.compareTo(ichimoku.kijun) < 0;
            
            // Cloud pozisyonu (Senkou A ve B)
            boolean aboveCloud = currentPrice.compareTo(ichimoku.senkouA) > 0 && 
                                currentPrice.compareTo(ichimoku.senkouB) > 0;
            boolean belowCloud = currentPrice.compareTo(ichimoku.senkouA) < 0 && 
                                currentPrice.compareTo(ichimoku.senkouB) < 0;
            
            // Chikou analizi (geçmiş trend)
            boolean chikouBullish = ichimoku.chikou.compareTo(currentPrice) > 0;
            boolean chikouBearish = ichimoku.chikou.compareTo(currentPrice) < 0;
            
            // Cloud sınırları (gelecek destek/direnç)
            BigDecimal cloudUpper = ichimoku.senkouA.max(ichimoku.senkouB);
            BigDecimal cloudLower = ichimoku.senkouA.min(ichimoku.senkouB);
            BigDecimal cloudRange = cloudUpper.subtract(cloudLower);
            
            // Fiyat tahminleri
            BigDecimal entryPrice = null;
            BigDecimal stopLoss = null;
            BigDecimal takeProfit = null;
            String predictionExplanation = "";
            
            // Sinyal üretimi ve fiyat tahminleri
            String signal;
            if (bullishTrend && aboveCloud && chikouBullish) {
                signal = "BUY"; // Güçlü yükseliş sinyali
                // Long pozisyon için fiyat tahminleri
                entryPrice = currentPrice; // Mevcut fiyattan giriş
                stopLoss = cloudLower; // Cloud'un alt sınırı stop loss
                takeProfit = currentPrice.add(cloudRange.multiply(BigDecimal.valueOf(2))); // 2x cloud mesafesi
                predictionExplanation = "LONG: Cloud üstünde güçlü yükseliş. Entry: Mevcut fiyat, SL: Cloud altı, TP: 2x cloud mesafesi";
                
            } else if (bearishTrend && belowCloud && chikouBearish) {
                signal = "SELL"; // Güçlü düşüş sinyali
                // Short pozisyon için fiyat tahminleri
                entryPrice = currentPrice; // Mevcut fiyattan giriş
                stopLoss = cloudUpper; // Cloud'un üst sınırı stop loss
                takeProfit = currentPrice.subtract(cloudRange.multiply(BigDecimal.valueOf(2))); // 2x cloud mesafesi
                predictionExplanation = "SHORT: Cloud altında güçlü düşüş. Entry: Mevcut fiyat, SL: Cloud üstü, TP: 2x cloud mesafesi";
                
            } else if (bullishTrend && aboveCloud) {
                signal = "BUY_WEAK"; // Yükseliş trendi, cloud üstünde
                entryPrice = cloudUpper; // Cloud'un üst sınırından giriş
                stopLoss = cloudLower;
                takeProfit = cloudUpper.add(cloudRange.multiply(BigDecimal.valueOf(1.5)));
                predictionExplanation = "LONG_WEAK: Cloud üstünde yükseliş. Entry: Cloud üstü, SL: Cloud altı, TP: 1.5x cloud mesafesi";
                
            } else if (bearishTrend && belowCloud) {
                signal = "SELL_WEAK"; // Düşüş trendi, cloud altında
                entryPrice = cloudLower; // Cloud'un alt sınırından giriş
                stopLoss = cloudUpper;
                takeProfit = cloudLower.subtract(cloudRange.multiply(BigDecimal.valueOf(1.5)));
                predictionExplanation = "SHORT_WEAK: Cloud altında düşüş. Entry: Cloud altı, SL: Cloud üstü, TP: 1.5x cloud mesafesi";
                
            } else if (bullishTrend) {
                signal = "BUY_WEAK"; // Sadece yükseliş trendi
                entryPrice = ichimoku.kijun; // Kijun'dan giriş
                stopLoss = ichimoku.tenkan;
                takeProfit = ichimoku.kijun.add(cloudRange);
                predictionExplanation = "LONG_WEAK: Sadece yükseliş trendi. Entry: Kijun, SL: Tenkan, TP: Kijun + cloud mesafesi";
                
            } else if (bearishTrend) {
                signal = "SELL_WEAK"; // Sadece düşüş trendi
                entryPrice = ichimoku.kijun; // Kijun'dan giriş
                stopLoss = ichimoku.tenkan;
                takeProfit = ichimoku.kijun.subtract(cloudRange);
                predictionExplanation = "SHORT_WEAK: Sadece düşüş trendi. Entry: Kijun, SL: Tenkan, TP: Kijun - cloud mesafesi";
                
            } else {
                signal = "HOLD"; // Nötr
                predictionExplanation = "Nötr durum - işlem önerilmez";
            }
            
            return new IchimokuSignalResult(signal, entryPrice, stopLoss, takeProfit, currentPrice, predictionExplanation);
            
        } catch (Exception e) {
            logger.error("Error generating Ichimoku signal with predictions: {}", e.getMessage());
            return new IchimokuSignalResult("HOLD", null, null, null, null, "Hata: " + e.getMessage());
        }
    }
    
    /**
     * Ichimoku sinyal sonuçlarını tutan sınıf
     */
    public static class IchimokuSignalResult {
        public final String signal;
        public final BigDecimal entryPrice;
        public final BigDecimal stopLoss;
        public final BigDecimal takeProfit;
        public final BigDecimal currentPrice;
        public final String explanation;
        
        public IchimokuSignalResult(String signal, BigDecimal entryPrice, BigDecimal stopLoss, 
                                   BigDecimal takeProfit, BigDecimal currentPrice, String explanation) {
            this.signal = signal;
            this.entryPrice = entryPrice;
            this.stopLoss = stopLoss;
            this.takeProfit = takeProfit;
            this.currentPrice = currentPrice;
            this.explanation = explanation;
        }
    }
    
    /**
     * MACD sonuçlarını tutan iç sınıf
     */
    public static class MACDResult {
        public final BigDecimal macdLine;
        public final BigDecimal signalLine;
        public final BigDecimal histogram;
        
        public MACDResult(BigDecimal macdLine, BigDecimal signalLine, BigDecimal histogram) {
            this.macdLine = macdLine;
            this.signalLine = signalLine;
            this.histogram = histogram;
        }
    }
} 