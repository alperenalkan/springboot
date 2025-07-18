package org.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OnChainDto {
    private BigDecimal whaleTransactions; // 100k+ USD transactions
    private BigDecimal exchangeInflow;
    private BigDecimal exchangeOutflow;
    private BigDecimal netFlow;
    private BigDecimal activeAddresses;
    private BigDecimal transactionCount;
    private BigDecimal averageTransactionValue;
    private BigDecimal networkHashRate;
    private BigDecimal difficulty;
    private String whaleMovement; // "Accumulation", "Distribution", "Neutral"
    private String flowDirection; // "Inflow", "Outflow", "Neutral"
    private LocalDateTime timestamp;
    private String onChainExplanation;
    
    public OnChainDto() {}
    
    public OnChainDto(BigDecimal whaleTransactions, BigDecimal exchangeInflow, 
                     BigDecimal exchangeOutflow, BigDecimal netFlow, 
                     BigDecimal activeAddresses, BigDecimal transactionCount,
                     BigDecimal averageTransactionValue, BigDecimal networkHashRate,
                     BigDecimal difficulty, String whaleMovement, String flowDirection,
                     LocalDateTime timestamp, String onChainExplanation) {
        this.whaleTransactions = whaleTransactions;
        this.exchangeInflow = exchangeInflow;
        this.exchangeOutflow = exchangeOutflow;
        this.netFlow = netFlow;
        this.activeAddresses = activeAddresses;
        this.transactionCount = transactionCount;
        this.averageTransactionValue = averageTransactionValue;
        this.networkHashRate = networkHashRate;
        this.difficulty = difficulty;
        this.whaleMovement = whaleMovement;
        this.flowDirection = flowDirection;
        this.timestamp = timestamp;
        this.onChainExplanation = onChainExplanation;
    }
    
    // Getters and Setters
    public BigDecimal getWhaleTransactions() {
        return whaleTransactions;
    }
    
    public void setWhaleTransactions(BigDecimal whaleTransactions) {
        this.whaleTransactions = whaleTransactions;
    }
    
    public BigDecimal getExchangeInflow() {
        return exchangeInflow;
    }
    
    public void setExchangeInflow(BigDecimal exchangeInflow) {
        this.exchangeInflow = exchangeInflow;
    }
    
    public BigDecimal getExchangeOutflow() {
        return exchangeOutflow;
    }
    
    public void setExchangeOutflow(BigDecimal exchangeOutflow) {
        this.exchangeOutflow = exchangeOutflow;
    }
    
    public BigDecimal getNetFlow() {
        return netFlow;
    }
    
    public void setNetFlow(BigDecimal netFlow) {
        this.netFlow = netFlow;
    }
    
    public BigDecimal getActiveAddresses() {
        return activeAddresses;
    }
    
    public void setActiveAddresses(BigDecimal activeAddresses) {
        this.activeAddresses = activeAddresses;
    }
    
    public BigDecimal getTransactionCount() {
        return transactionCount;
    }
    
    public void setTransactionCount(BigDecimal transactionCount) {
        this.transactionCount = transactionCount;
    }
    
    public BigDecimal getAverageTransactionValue() {
        return averageTransactionValue;
    }
    
    public void setAverageTransactionValue(BigDecimal averageTransactionValue) {
        this.averageTransactionValue = averageTransactionValue;
    }
    
    public BigDecimal getNetworkHashRate() {
        return networkHashRate;
    }
    
    public void setNetworkHashRate(BigDecimal networkHashRate) {
        this.networkHashRate = networkHashRate;
    }
    
    public BigDecimal getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(BigDecimal difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getWhaleMovement() {
        return whaleMovement;
    }
    
    public void setWhaleMovement(String whaleMovement) {
        this.whaleMovement = whaleMovement;
    }
    
    public String getFlowDirection() {
        return flowDirection;
    }
    
    public void setFlowDirection(String flowDirection) {
        this.flowDirection = flowDirection;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getOnChainExplanation() {
        return onChainExplanation;
    }
    
    public void setOnChainExplanation(String onChainExplanation) {
        this.onChainExplanation = onChainExplanation;
    }
} 