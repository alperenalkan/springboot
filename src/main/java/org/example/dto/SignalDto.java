package org.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SignalDto {
    
    public enum SignalType {
        BUY("BUY", "💚 Al"),
        SELL("SELL", "❤️ Sat"),
        HOLD("HOLD", "⚪ Bekle");
        
        private final String value;
        private final String displayText;
        
        SignalType(String value, String displayText) {
            this.value = value;
            this.displayText = displayText;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getDisplayText() {
            return displayText;
        }
    }
    
    private SignalType signal;
    private String intervalType;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private BigDecimal currentPrice;
    private BigDecimal rsiValue;
    private BigDecimal macdValue;
    private BigDecimal macdSignal;
    private BigDecimal macdHistogram;
    private BigDecimal sma20;
    private BigDecimal ema12;
    private BigDecimal sma50;
    private BigDecimal sma200;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private String sltpExplanation;
    private BigDecimal atr;
    private BigDecimal bollingerUpper;
    private BigDecimal bollingerMiddle;
    private BigDecimal bollingerLower;
    private BigDecimal stochasticRsi;
    private BigDecimal adx;
    private BigDecimal ichimokuTenkan;
    private BigDecimal ichimokuKijun;
    private BigDecimal ichimokuSenkouA;
    private BigDecimal ichimokuSenkouB;
    private BigDecimal ichimokuChikou;
    private String ichimokuSignal;
    private BigDecimal ichimokuEntryPrice;
    private BigDecimal ichimokuStopLoss;
    private BigDecimal ichimokuTakeProfit;
    private String ichimokuPredictionExplanation;
    private String aggressiveSignal;
    private String tradeAdvice;
    
    private BigDecimal altTakeProfit;
    private BigDecimal altStopLoss;
    private String altSltpExplanation;
    
    // Detaylı analiz alanları
    private BigDecimal entryPrice;
    private String entryExplanation;
    private int buySignals;
    private int sellSignals;
    private String rsiAnalysis;
    private String macdAnalysis;
    private String trendAnalysis;
    private String bollingerAnalysis;
    private String stochasticAnalysis;
    private String adxAnalysis;
    private String ichimokuAnalysis;
    
    // Market Sentiment alanları
    private BigDecimal fearGreedValue;
    private String fearGreedClassification;
    private String fearGreedDescription;
    private BigDecimal sentimentValue;
    private String sentimentClassification;
    private String sentimentExplanation;
    private BigDecimal whaleTransactions;
    private String whaleMovement;
    private String flowDirection;
    private String onChainExplanation;
    private String sentimentSignal;
    
    private String reasoning;
    
    // SuperTrend ve VWAP
    private java.math.BigDecimal superTrend;
    private java.math.BigDecimal vwap;

    public java.math.BigDecimal getSuperTrend() { return superTrend; }
    public void setSuperTrend(java.math.BigDecimal superTrend) { this.superTrend = superTrend; }
    public java.math.BigDecimal getVwap() { return vwap; }
    public void setVwap(java.math.BigDecimal vwap) { this.vwap = vwap; }
    
    // Constructors
    public SignalDto() {}
    
    public SignalDto(SignalType signal, String intervalType, LocalDateTime timestamp, 
                    BigDecimal currentPrice, String reasoning) {
        this.signal = signal;
        this.intervalType = intervalType;
        this.timestamp = timestamp;
        this.currentPrice = currentPrice;
        this.reasoning = reasoning;
    }
    
    // Getters and Setters
    public SignalType getSignal() {
        return signal;
    }
    
    public void setSignal(SignalType signal) {
        this.signal = signal;
    }
    
    public String getIntervalType() {
        return intervalType;
    }
    
    public void setIntervalType(String intervalType) {
        this.intervalType = intervalType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public BigDecimal getRsiValue() {
        return rsiValue;
    }
    
    public void setRsiValue(BigDecimal rsiValue) {
        this.rsiValue = rsiValue;
    }
    
    public BigDecimal getMacdValue() {
        return macdValue;
    }
    
    public void setMacdValue(BigDecimal macdValue) {
        this.macdValue = macdValue;
    }
    
    public BigDecimal getMacdSignal() {
        return macdSignal;
    }
    
    public void setMacdSignal(BigDecimal macdSignal) {
        this.macdSignal = macdSignal;
    }
    
    public BigDecimal getMacdHistogram() {
        return macdHistogram;
    }
    
    public void setMacdHistogram(BigDecimal macdHistogram) {
        this.macdHistogram = macdHistogram;
    }
    
    public BigDecimal getSma20() {
        return sma20;
    }
    
    public void setSma20(BigDecimal sma20) {
        this.sma20 = sma20;
    }
    
    public BigDecimal getEma12() {
        return ema12;
    }
    
    public void setEma12(BigDecimal ema12) {
        this.ema12 = ema12;
    }
    
    public BigDecimal getSma50() {
        return sma50;
    }
    public void setSma50(BigDecimal sma50) {
        this.sma50 = sma50;
    }
    public BigDecimal getSma200() {
        return sma200;
    }
    public void setSma200(BigDecimal sma200) {
        this.sma200 = sma200;
    }
    
    public BigDecimal getStopLoss() {
        return stopLoss;
    }
    public void setStopLoss(BigDecimal stopLoss) {
        this.stopLoss = stopLoss;
    }
    public BigDecimal getTakeProfit() {
        return takeProfit;
    }
    public void setTakeProfit(BigDecimal takeProfit) {
        this.takeProfit = takeProfit;
    }
    public String getSltpExplanation() {
        return sltpExplanation;
    }
    public void setSltpExplanation(String sltpExplanation) {
        this.sltpExplanation = sltpExplanation;
    }
    
    public BigDecimal getAtr() {
        return atr;
    }
    public void setAtr(BigDecimal atr) {
        this.atr = atr;
    }
    
    public BigDecimal getBollingerUpper() { return bollingerUpper; }
    public void setBollingerUpper(BigDecimal v) { this.bollingerUpper = v; }
    public BigDecimal getBollingerMiddle() { return bollingerMiddle; }
    public void setBollingerMiddle(BigDecimal v) { this.bollingerMiddle = v; }
    public BigDecimal getBollingerLower() { return bollingerLower; }
    public void setBollingerLower(BigDecimal v) { this.bollingerLower = v; }
    public BigDecimal getStochasticRsi() { return stochasticRsi; }
    public void setStochasticRsi(BigDecimal v) { this.stochasticRsi = v; }
    public BigDecimal getAdx() { return adx; }
    public void setAdx(BigDecimal v) { this.adx = v; }
    public BigDecimal getIchimokuTenkan() { return ichimokuTenkan; }
    public void setIchimokuTenkan(BigDecimal v) { this.ichimokuTenkan = v; }
    public BigDecimal getIchimokuKijun() { return ichimokuKijun; }
    public void setIchimokuKijun(BigDecimal v) { this.ichimokuKijun = v; }
    public BigDecimal getIchimokuSenkouA() { return ichimokuSenkouA; }
    public void setIchimokuSenkouA(BigDecimal v) { this.ichimokuSenkouA = v; }
    public BigDecimal getIchimokuSenkouB() { return ichimokuSenkouB; }
    public void setIchimokuSenkouB(BigDecimal v) { this.ichimokuSenkouB = v; }
    public BigDecimal getIchimokuChikou() { return ichimokuChikou; }
    public void setIchimokuChikou(BigDecimal v) { this.ichimokuChikou = v; }
    
    public String getIchimokuSignal() { return ichimokuSignal; }
    public void setIchimokuSignal(String v) { this.ichimokuSignal = v; }
    
    public BigDecimal getIchimokuEntryPrice() { return ichimokuEntryPrice; }
    public void setIchimokuEntryPrice(BigDecimal v) { this.ichimokuEntryPrice = v; }
    
    public BigDecimal getIchimokuStopLoss() { return ichimokuStopLoss; }
    public void setIchimokuStopLoss(BigDecimal v) { this.ichimokuStopLoss = v; }
    
    public BigDecimal getIchimokuTakeProfit() { return ichimokuTakeProfit; }
    public void setIchimokuTakeProfit(BigDecimal v) { this.ichimokuTakeProfit = v; }
    
    public String getIchimokuPredictionExplanation() { return ichimokuPredictionExplanation; }
    public void setIchimokuPredictionExplanation(String v) { this.ichimokuPredictionExplanation = v; }
    
    public String getAggressiveSignal() { return aggressiveSignal; }
    public void setAggressiveSignal(String aggressiveSignal) { this.aggressiveSignal = aggressiveSignal; }
    
    public BigDecimal getAltTakeProfit() { return altTakeProfit; }
    public void setAltTakeProfit(BigDecimal v) { this.altTakeProfit = v; }
    public BigDecimal getAltStopLoss() { return altStopLoss; }
    public void setAltStopLoss(BigDecimal v) { this.altStopLoss = v; }
    public String getAltSltpExplanation() { return altSltpExplanation; }
    public void setAltSltpExplanation(String v) { this.altSltpExplanation = v; }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public String getTradeAdvice() { return tradeAdvice; }
    public void setTradeAdvice(String v) { this.tradeAdvice = v; }
    
    // Detaylı analiz getter/setter metodları
    public BigDecimal getEntryPrice() { return entryPrice; }
    public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }
    
    public String getEntryExplanation() { return entryExplanation; }
    public void setEntryExplanation(String entryExplanation) { this.entryExplanation = entryExplanation; }
    
    public int getBuySignals() { return buySignals; }
    public void setBuySignals(int buySignals) { this.buySignals = buySignals; }
    
    public int getSellSignals() { return sellSignals; }
    public void setSellSignals(int sellSignals) { this.sellSignals = sellSignals; }
    
    public String getRsiAnalysis() { return rsiAnalysis; }
    public void setRsiAnalysis(String rsiAnalysis) { this.rsiAnalysis = rsiAnalysis; }
    
    public String getMacdAnalysis() { return macdAnalysis; }
    public void setMacdAnalysis(String macdAnalysis) { this.macdAnalysis = macdAnalysis; }
    
    public String getTrendAnalysis() { return trendAnalysis; }
    public void setTrendAnalysis(String trendAnalysis) { this.trendAnalysis = trendAnalysis; }
    
    public String getBollingerAnalysis() { return bollingerAnalysis; }
    public void setBollingerAnalysis(String bollingerAnalysis) { this.bollingerAnalysis = bollingerAnalysis; }
    
    public String getStochasticAnalysis() { return stochasticAnalysis; }
    public void setStochasticAnalysis(String stochasticAnalysis) { this.stochasticAnalysis = stochasticAnalysis; }
    
    public String getAdxAnalysis() { return adxAnalysis; }
    public void setAdxAnalysis(String adxAnalysis) { this.adxAnalysis = adxAnalysis; }
    
    public String getIchimokuAnalysis() { return ichimokuAnalysis; }
    public void setIchimokuAnalysis(String ichimokuAnalysis) { this.ichimokuAnalysis = ichimokuAnalysis; }
    
    // Market Sentiment getters and setters
    public BigDecimal getFearGreedValue() { return fearGreedValue; }
    public void setFearGreedValue(BigDecimal fearGreedValue) { this.fearGreedValue = fearGreedValue; }
    
    public String getFearGreedClassification() { return fearGreedClassification; }
    public void setFearGreedClassification(String fearGreedClassification) { this.fearGreedClassification = fearGreedClassification; }
    
    public String getFearGreedDescription() { return fearGreedDescription; }
    public void setFearGreedDescription(String fearGreedDescription) { this.fearGreedDescription = fearGreedDescription; }
    
    public BigDecimal getSentimentValue() { return sentimentValue; }
    public void setSentimentValue(BigDecimal sentimentValue) { this.sentimentValue = sentimentValue; }
    
    public String getSentimentClassification() { return sentimentClassification; }
    public void setSentimentClassification(String sentimentClassification) { this.sentimentClassification = sentimentClassification; }
    
    public String getSentimentExplanation() { return sentimentExplanation; }
    public void setSentimentExplanation(String sentimentExplanation) { this.sentimentExplanation = sentimentExplanation; }
    
    public BigDecimal getWhaleTransactions() { return whaleTransactions; }
    public void setWhaleTransactions(BigDecimal whaleTransactions) { this.whaleTransactions = whaleTransactions; }
    
    public String getWhaleMovement() { return whaleMovement; }
    public void setWhaleMovement(String whaleMovement) { this.whaleMovement = whaleMovement; }
    
    public String getFlowDirection() { return flowDirection; }
    public void setFlowDirection(String flowDirection) { this.flowDirection = flowDirection; }
    
    public String getOnChainExplanation() { return onChainExplanation; }
    public void setOnChainExplanation(String onChainExplanation) { this.onChainExplanation = onChainExplanation; }
    
    public String getSentimentSignal() { return sentimentSignal; }
    public void setSentimentSignal(String sentimentSignal) { this.sentimentSignal = sentimentSignal; }
    
    @Override
    public String toString() {
        return "SignalDto{" +
                "signal=" + signal +
                ", intervalType='" + intervalType + '\'' +
                ", timestamp=" + timestamp +
                ", currentPrice=" + currentPrice +
                ", rsiValue=" + rsiValue +
                ", macdValue=" + macdValue +
                ", macdSignal=" + macdSignal +
                ", macdHistogram=" + macdHistogram +
                ", sma20=" + sma20 +
                ", sma50=" + sma50 +
                ", sma200=" + sma200 +
                ", ema12=" + ema12 +
                ", altTakeProfit=" + altTakeProfit +
                ", altStopLoss=" + altStopLoss +
                ", altSltpExplanation='" + altSltpExplanation + '\'' +
                ", reasoning='" + reasoning + '\'' +
                ", tradeAdvice='" + tradeAdvice + '\'' +
                '}';
    }
} 