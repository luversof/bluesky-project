package net.luversof.web.gate.stock.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DividendRequest {
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
        DividendRequest that = (DividendRequest) o;
        return (userId != null ? userId.equals(that.userId) : that.userId == null)
                && (accountIdList != null
                        ? accountIdList.equals(that.accountIdList)
                        : that.accountIdList == null)
                && (stockItemIdList != null
                        ? stockItemIdList.equals(that.stockItemIdList)
                        : that.stockItemIdList == null)
                && (startDate != null ? startDate.equals(that.startDate) : that.startDate == null)
                && (endDate != null ? endDate.equals(that.endDate) : that.endDate == null);
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (accountIdList != null ? accountIdList.hashCode() : 0);
        result = 31 * result + (stockItemIdList != null ? stockItemIdList.hashCode() : 0);
        result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DividendRequest{"
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

    public org.springframework.util.MultiValueMap<String, String> toParams() {
        org.springframework.util.MultiValueMap<String, String> params =
                new org.springframework.util.LinkedMultiValueMap<>();
        if (userId != null) params.add("userId", userId.toString());
        if (accountIdList != null)
            accountIdList.forEach(x -> params.add("accountIdList", x.toString()));
        if (stockItemIdList != null)
            stockItemIdList.forEach(x -> params.add("stockItemIdList", x.toString()));
        if (startDate != null) params.add("startDate", startDate.toString());
        if (endDate != null) params.add("endDate", endDate.toString());
        return params;
    }
}
