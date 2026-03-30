package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("daily_account_snapshot")
public class DailyAccountSnapshot {

        @Id
        private UUID id;

        private UUID userId;

        private UUID accountId;

        private LocalDate date;

        private BigDecimal totalCost;

        private BigDecimal totalValue;

        private BigDecimal cumulativeRealizedProfit;

        private BigDecimal cumulativeDividend;

        private ZonedDateTime createdDate;

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

        public ZonedDateTime getCreatedDate() {
                return createdDate;
        }

        public void setCreatedDate(ZonedDateTime createdDate) {
                this.createdDate = createdDate;
        }
}
