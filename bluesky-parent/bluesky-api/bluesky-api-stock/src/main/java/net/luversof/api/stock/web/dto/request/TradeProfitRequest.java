package net.luversof.api.stock.web.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import net.luversof.api.stock.constant.StockErrorCode;

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

  /**
   * 일자 집계에 쓸 타임존(예: Asia/Seoul). 없거나 잘못되면 서버 기본값으로 폴백한다.
   *
   * <p>이 값을 무시하면 컨테이너가 UTC 로 뜬 환경에서 KST 오전 거래가 전날로 집계돼 차트와 연도 경계가 하루씩 밀린다.
   */
  private String timeZone;

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
    // 널이면 선언된 기본값을 유지한다(setter 와 같은 규칙).
    setGroupBy(groupBy);
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

  /**
   * 값이 없으면 선언된 기본값을 그대로 둔다.
   *
   * <p>빈 쿼리 파라미터({@code ?groupBy=})는 바인딩에서 {@code null} 이 되는데, 그 {@code null} 이 기본값을 지워 {@code
   * calculateProfit} 의 {@code switch} 가 {@code NullPointerException} 을 던졌다. 그 예외는 공통 처리로 떨어져 {@code
   * Accept} 에 따라 500 이거나 <b>200 · 본문 0 바이트</b>가 됐다(실측: {@code
   * /api/tradeProfit/calculateProfit?groupBy=} 가 별표 {@code Accept} 로 200 · 0 바이트).
   *
   * <p>값을 안 준 것과 빈 값을 준 것은 같은 뜻이므로 기본값을 유지한다.
   */
  public void setGroupBy(TradeProfitRequestGroup groupBy) {
    if (groupBy != null) {
      this.groupBy = groupBy;
    }
  }

  public String getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(String timeZone) {
    this.timeZone = timeZone;
  }

  /** 요청 타임존을 해석한다. 값이 없거나 알 수 없는 ID 면 서버 기본 타임존을 쓴다. */
  public java.time.ZoneId resolveZoneId() {
    if (timeZone == null || timeZone.isBlank()) {
      return java.time.ZoneId.systemDefault();
    }
    try {
      return java.time.ZoneId.of(timeZone);
    } catch (Exception ex) {
      return java.time.ZoneId.systemDefault();
    }
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
