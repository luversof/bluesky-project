package net.luversof.api.stock.web.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import net.luversof.api.stock.constant.StockErrorCode;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/** 주식 손익 계산 요청 DTO 조회 기준 조합 */
public class TradeProfitRequest {

    private UUID userId;
    private List<UUID> accountIdList;
    private List<UUID> stockItemIdList;

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private Instant startDate;

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private Instant endDate;

    private TradeProfitRequestGroup groupBy = TradeProfitRequestGroup.ACCOUNT_AND_STOCKITEM;

    public TradeProfitRequest() {}

    public TradeProfitRequest(
            UUID userId,
            List<UUID> accountIdList,
            List<UUID> stockItemIdList,
            Instant startDate,
            Instant endDate,
            TradeProfitRequestGroup groupBy) {
        this.userId = userId;
        this.accountIdList = accountIdList;
        this.stockItemIdList = stockItemIdList;
        this.startDate = startDate;
        this.endDate = endDate;
        this.groupBy = groupBy;
    }

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

    public TradeProfitRequestGroup getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(TradeProfitRequestGroup groupBy) {
        this.groupBy = groupBy;
    }

    public boolean hasDateRange() {
        return startDate != null || endDate != null;
    }

    public TradeProfitRequestType getRequestType() {
        if (userId == null) {
            StockErrorCode.NOT_EXIST_USER_ID.throwException();
        }

        if (accountIdList == null || accountIdList.isEmpty()) {
            if (stockItemIdList == null || stockItemIdList.isEmpty()) {
                return TradeProfitRequestType.USER;
            } else {
                return TradeProfitRequestType.USER_STOCKITEM;
            }
        } else {
            if (stockItemIdList == null || stockItemIdList.isEmpty()) {
                return TradeProfitRequestType.USER_ACCOUNT;
            } else {
                return TradeProfitRequestType.USER_ACCOUNT_STOCKITEM;
            }
        }
    }

    @Override
    public String toString() {
        return "TradeProfitRequest [userId="
                + userId
                + ", accountIdList="
                + accountIdList
                + ", stockItemIdList="
                + stockItemIdList
                + ", startDate="
                + startDate
                + ", endDate="
                + endDate
                + ", groupBy="
                + groupBy
                + "]";
    }
}
