package org.example.service;

import org.example.entity.PriceEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class IndicatorService {
    
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
     */
    public MACDResult calculateMACD(List<PriceEntity> prices) {
        if (prices.size() < macdSlowPeriod) {
            return new MACDResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        // EMA hesaplamaları
        BigDecimal ema12 = calculateEMA(prices, macdFastPeriod);
        BigDecimal ema26 = calculateEMA(prices, macdSlowPeriod);
        
        // MACD Line
        BigDecimal macdLine = ema12.subtract(ema26);
        
        // MACD Signal Line (EMA of MACD Line)
        BigDecimal macdSignal = calculateEMASignal(prices, macdLine, macdSignalPeriod);
        
        // MACD Histogram
        BigDecimal macdHistogram = macdLine.subtract(macdSignal);
        
        return new MACDResult(
                macdLine.setScale(8, RoundingMode.HALF_UP),
                macdSignal.setScale(8, RoundingMode.HALF_UP),
                macdHistogram.setScale(8, RoundingMode.HALF_UP)
        );
    }
    
    /**
     * SMA (Simple Moving Average) hesaplar
     */
    public BigDecimal calculateSMA(List<PriceEntity> prices, int period) {
        if (prices.size() < period) {
            return null;
        }
        
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(prices.get(i).getClosePrice());
        }
        
        if (period > 0) {
            return sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        }
        return null;
    }
    
    /**
     * EMA (Exponential Moving Average) hesaplar
     */
    public BigDecimal calculateEMA(List<PriceEntity> prices, int period) {
        if (prices.size() < period) {
            return BigDecimal.ZERO;
        }
        
        // İlk EMA değeri SMA olarak hesaplanır
        BigDecimal ema = calculateSMA(prices, period);
        
        // Multiplier hesapla
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        
        // Kalan değerler için EMA hesapla
        for (int i = period; i < prices.size(); i++) {
            BigDecimal currentPrice = prices.get(i).getClosePrice();
            ema = currentPrice.multiply(multiplier)
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
     * RSI sinyali üretir
     */
    public String generateRSISignal(BigDecimal rsi) {
        if (rsi.compareTo(BigDecimal.valueOf(rsiOversold)) <= 0) {
            return "BUY";
        } else if (rsi.compareTo(BigDecimal.valueOf(rsiOverbought)) >= 0) {
            return "SELL";
        } else {
            return "HOLD";
        }
    }
    
    /**
     * MACD sinyali üretir
     */
    public String generateMACDSignal(MACDResult macd) {
        if (macd.macdLine.compareTo(macd.signalLine) > 0 && 
            macd.histogram.compareTo(BigDecimal.ZERO) > 0) {
            return "BUY";
        } else if (macd.macdLine.compareTo(macd.signalLine) < 0 && 
                   macd.histogram.compareTo(BigDecimal.ZERO) < 0) {
            return "SELL";
        } else {
            return "HOLD";
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