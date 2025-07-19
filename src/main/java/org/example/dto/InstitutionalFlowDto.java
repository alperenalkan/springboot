package org.example.dto;

import java.util.List;

public class InstitutionalFlowDto {
    private double netInflow7d;
    private double netInflow30d;
    private double balanceChangePercent;
    private List<String> majorTransfers;
    private String comment;

    public double getNetInflow7d() { return netInflow7d; }
    public void setNetInflow7d(double netInflow7d) { this.netInflow7d = netInflow7d; }
    public double getNetInflow30d() { return netInflow30d; }
    public void setNetInflow30d(double netInflow30d) { this.netInflow30d = netInflow30d; }
    public double getBalanceChangePercent() { return balanceChangePercent; }
    public void setBalanceChangePercent(double balanceChangePercent) { this.balanceChangePercent = balanceChangePercent; }
    public List<String> getMajorTransfers() { return majorTransfers; }
    public void setMajorTransfers(List<String> majorTransfers) { this.majorTransfers = majorTransfers; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
} 