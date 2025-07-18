package org.example.service;

import org.example.entity.PriceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
    "app.indicators.rsi.period=14",
    "app.indicators.rsi.oversold=30",
    "app.indicators.rsi.overbought=70",
    "app.indicators.macd.fast-period=12",
    "app.indicators.macd.slow-period=26",
    "app.indicators.macd.signal-period=9"
})
class IndicatorServiceTest {
    
    private IndicatorService indicatorService;
    private List<PriceEntity> testPrices;
    
    @BeforeEach
    void setUp() {
        indicatorService = new IndicatorService();
        // Testte property injection çalışmazsa elle ayarla
        try {
            java.lang.reflect.Field field = IndicatorService.class.getDeclaredField("rsiOversold");
            field.setAccessible(true);
            field.set(indicatorService, 30);
            java.lang.reflect.Field field2 = IndicatorService.class.getDeclaredField("rsiOverbought");
            field2.setAccessible(true);
            field2.set(indicatorService, 70);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        testPrices = new ArrayList<>();
        
        // Test verileri oluştur - yükselen trend
        for (int i = 0; i < 30; i++) {
            BigDecimal price = BigDecimal.valueOf(40000 + (i * 100)); // 40000'den başlayarak artan
            PriceEntity entity = new PriceEntity(
                LocalDateTime.now().minusDays(30 - i),
                price,
                price.add(BigDecimal.valueOf(50)),
                price.subtract(BigDecimal.valueOf(50)),
                price,
                BigDecimal.valueOf(1000),
                PriceEntity.IntervalType.ONE_DAY
            );
            testPrices.add(entity);
        }
    }
    
    @Test
    void testCalculateRSI() {
        BigDecimal rsi = indicatorService.calculateRSI(testPrices);
        
        assertNotNull(rsi);
        assertTrue(rsi.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(rsi.compareTo(BigDecimal.valueOf(100)) <= 0);
        
        System.out.println("RSI değeri: " + rsi);
    }
    
    @Test
    void testCalculateMACD() {
        IndicatorService.MACDResult macd = indicatorService.calculateMACD(testPrices);
        
        assertNotNull(macd);
        assertNotNull(macd.macdLine);
        assertNotNull(macd.signalLine);
        assertNotNull(macd.histogram);
        
        System.out.println("MACD Line: " + macd.macdLine);
        System.out.println("MACD Signal: " + macd.signalLine);
        System.out.println("MACD Histogram: " + macd.histogram);
    }
    
    @Test
    void testCalculateSMA() {
        BigDecimal sma20 = indicatorService.calculateSMA(testPrices, 20);
        
        assertNotNull(sma20);
        assertTrue(sma20.compareTo(BigDecimal.ZERO) > 0);
        
        // SMA değeri fiyatların ortalaması olmalı
        BigDecimal expectedSMA = BigDecimal.valueOf(40000); // Yaklaşık değer
        assertTrue(sma20.compareTo(expectedSMA) >= 0);
        
        System.out.println("SMA 20: " + sma20);
    }
    
    @Test
    void testCalculateEMA() {
        BigDecimal ema12 = indicatorService.calculateEMA(testPrices, 12);
        
        assertNotNull(ema12);
        assertTrue(ema12.compareTo(BigDecimal.ZERO) > 0);
        
        System.out.println("EMA 12: " + ema12);
    }
    
    @Test
    void testGenerateRSISignal() {
        // Aşırı satım durumu
        String oversoldSignal = indicatorService.generateRSISignal(BigDecimal.valueOf(25));
        assertEquals("BUY", oversoldSignal);
        
        // Aşırı alım durumu
        String overboughtSignal = indicatorService.generateRSISignal(BigDecimal.valueOf(75));
        assertEquals("SELL", overboughtSignal);
        
        // Nötr durum
        String neutralSignal = indicatorService.generateRSISignal(BigDecimal.valueOf(50));
        assertEquals("HOLD", neutralSignal);
        
        // Sınır değerler
        String boundarySignal = indicatorService.generateRSISignal(BigDecimal.valueOf(30));
        assertEquals("BUY", boundarySignal);
        String boundarySignal2 = indicatorService.generateRSISignal(BigDecimal.valueOf(70));
        assertEquals("SELL", boundarySignal2);
    }
    
    @Test
    void testGenerateMACDSignal() {
        // Yükseliş sinyali
        IndicatorService.MACDResult bullishMACD = new IndicatorService.MACDResult(
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(50),
            BigDecimal.valueOf(50)
        );
        String bullishSignal = indicatorService.generateMACDSignal(bullishMACD, BigDecimal.valueOf(-10));
        assertEquals("BUY", bullishSignal);
        
        // Düşüş sinyali
        IndicatorService.MACDResult bearishMACD = new IndicatorService.MACDResult(
            BigDecimal.valueOf(50),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(-50)
        );
        String bearishSignal = indicatorService.generateMACDSignal(bearishMACD, BigDecimal.valueOf(10));
        assertEquals("SELL", bearishSignal);
    }
    
    @Test
    void testInsufficientData() {
        List<PriceEntity> insufficientData = testPrices.subList(0, 5); // Sadece 5 veri
        
        BigDecimal rsi = indicatorService.calculateRSI(insufficientData);
        assertTrue(rsi.compareTo(BigDecimal.ZERO) == 0);
        
        IndicatorService.MACDResult macd = indicatorService.calculateMACD(insufficientData);
        assertTrue(macd.macdLine.compareTo(BigDecimal.ZERO) == 0);
        assertTrue(macd.signalLine.compareTo(BigDecimal.ZERO) == 0);
        assertTrue(macd.histogram.compareTo(BigDecimal.ZERO) == 0);
    }
    
    @Test
    void testDroppingPrices() {
        List<PriceEntity> droppingPrices = new ArrayList<>();
        
        // Düşen trend oluştur
        for (int i = 0; i < 30; i++) {
            BigDecimal price = BigDecimal.valueOf(50000 - (i * 100)); // 50000'den başlayarak azalan
            PriceEntity entity = new PriceEntity(
                LocalDateTime.now().minusDays(30 - i),
                price,
                price.add(BigDecimal.valueOf(50)),
                price.subtract(BigDecimal.valueOf(50)),
                price,
                BigDecimal.valueOf(1000),
                PriceEntity.IntervalType.ONE_DAY
            );
            droppingPrices.add(entity);
        }
        
        BigDecimal rsi = indicatorService.calculateRSI(droppingPrices);
        assertNotNull(rsi);
        assertTrue(rsi.compareTo(BigDecimal.ZERO) >= 0);
        
        System.out.println("Düşen trend RSI: " + rsi);
    }
} 