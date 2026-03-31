package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("DailyAccountSnapshot")
public class DailyAccountSnapshot {

    @Id
    @Column("id")
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("account_id")
    private UUID accountId;

    @Column("date")
    private LocalDate date;

    @Column("totalCost")
    private BigDecimal totalCost;

    @Column("totalValue")
    private BigDecimal totalValue;

    @Column("cumulativeRealizedProfit")
    private BigDecimal cumulativeRealizedProfit;

    @Column("cumulativeDividend")
    private BigDecimal cumulativeDividend;

    @Column("createdDate")
    private java.time.Instant createdDate;

    @Column("wmaState")
    private java.util.Map<String, Object> wmaState;

    public java.util.Map<String, Object> getWmaState() {
        return wmaState;
    }

    public void setWmaState(java.util.Map<String, Object> wmaState) {
        this.wmaState = wmaState;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getCumulativeRealizedProfit() {
        return cumulativeRealizedProfit;
    }

    public void setCumulativeRealizedProfit(BigDecimal cumulativeRealizedProfit) {
        this.cumulativeRealizedProfit = cumulativeRealizedProfit;
    }

    public BigDecimal getCumulativeDividend() {
        return cumulativeDividend;
    }

    public void setCumulativeDividend(BigDecimal cumulativeDividend) {
        this.cumulativeDividend = cumulativeDividend;
    }

    public java.time.Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(java.time.Instant createdDate) {
        this.createdDate = createdDate;
    }
}
