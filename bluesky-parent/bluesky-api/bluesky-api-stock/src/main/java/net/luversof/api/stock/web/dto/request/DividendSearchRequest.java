package net.luversof.api.stock.web.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DividendSearchRequest {

    private UUID userId;
    private List<UUID> accountIdList;
    private List<UUID> stockItemIdList;
    private Instant startDate;
    private Instant endDate;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public List<UUID> getAccountIdList() {
        return accountIdList;
    }

    public void setAccountIdList(List<UUID> accountIdList) {
        this.accountIdList = accountIdList;
    }

    public List<UUID> getStockItemIdList() {
        return stockItemIdList;
    }

    public void setStockItemIdList(List<UUID> stockItemIdList) {
        this.stockItemIdList = stockItemIdList;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DividendSearchRequest that = (DividendSearchRequest) o;
        return Objects.equals(userId, that.userId)
                && Objects.equals(accountIdList, that.accountIdList)
                && Objects.equals(stockItemIdList, that.stockItemIdList)
                && Objects.equals(startDate, that.startDate)
                && Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, accountIdList, stockItemIdList, startDate, endDate);
    }

    @Override
    public String toString() {
        return "DividendSearchRequest{"
                + "userId="
                + userId
                + ", accountIdList="
                + accountIdList
                + ", stockItemIdList="
                + stockItemIdList
                + ", startDate="
                + startDate
                + ", endDate="
                + endDate
                + '}';
    }
}
