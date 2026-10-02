package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.domain.StockOhlcRow;
import net.luversof.api.stock.domain.StockRawClose;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.response.StockPriceChartPoint;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;

/**
 * 종목 상세 주가 차트(사용자 요청 2026-10-02: "일반적인 주식 막대 그래프(시가 · 종가 · 고가 · 저가)" + "각 날짜에 그 시점의 평균 단가").
 *
 * <p>가격: 원주가를 분할 · 병합 배율로만 맞춘 종가(TradeProfitService.splitAdjustedCloses, 평가와 같은 배율 규칙)에, 시가 · 고가 ·
 * 저가는 같은 날의 (차트 종가 / 수정 종가) 를 곱한다 - KIS 수정 주가는 한 날의 네 값을 같은 배율로 깎으므로 그 날 배율로 되돌리면 된다. 원주가로 이을 수 없는
 * 종목은 수정 주가 그대로다.
 *
 * <p>평균 단가: 그 날 보유분의 원가 x 차트 종가 / 평가액. 평가액이 (그 날 가격 x 실제 주식 수) 라 이 식은 (원가 / 실제 주식 수) 를 차트와 같은 단위로
 * 옮긴 것이다 - 분할 전 날의 평단도 분할 뒤 단위로 그려진다. 원주가로 평가하지 않는 종목이면 평가액이 수정 종가 x 환산 수량이고 차트 종가도 수정 종가라 같은 식이
 * 그대로 맞는다. 이동평균법이라 사면 바뀌고 팔면 그대로다. 보유가 없던 날은 null(선을 끊는다).
 */
@Service
public class StockPriceChartService {

  @Autowired private StockPriceService stockPriceService;

  @Autowired private TradeProfitService tradeProfitService;

  @Autowired
  private net.luversof.api.stock.repository.MonthlyDividendPayoutQuery monthlyDividendPayoutQuery;

  public List<StockPriceChartPoint> chart(
      UUID stockItemId, LocalDate startDate, LocalDate endDate, UUID userId, String timeZone) {
    if (stockItemId == null) {
      return List.of();
    }
    // 분할 배율은 고른 기간 뒤의 분할까지 알아야 하므로 끝은 늘 마지막 시세까지 읽고 자른다.
    List<StockOhlcRow> rows =
        stockPriceService.getOhlc(
            stockItemId, startDate != null ? startDate : LocalDate.of(1900, 1, 1));
    if (rows.isEmpty()) {
      return List.of();
    }
    Map<LocalDate, BigDecimal> chartClose = new HashMap<>();
    List<StockRawClose> rawRows = new ArrayList<>(rows.size());
    for (StockOhlcRow row : rows) {
      rawRows.add(new StockRawClose(stockItemId, row.tradeDate(), row.rawClose(), row.close()));
    }
    var splitAdjusted = TradeProfitService.splitAdjustedCloses(rawRows);
    if (splitAdjusted != null) {
      splitAdjusted.forEach(point -> chartClose.put(point.tradeDate(), point.closePrice()));
    }

    Map<LocalDate, TradeProfitTimeSeriesPoint> holdingsByDay = new HashMap<>();
    if (userId != null) {
      TradeProfitRequest request = new TradeProfitRequest();
      request.setUserId(userId);
      request.setStockItemIdList(List.of(stockItemId));
      request.setTimeZone(timeZone);
      for (TradeProfitTimeSeriesPoint point :
          tradeProfitService.aggregateTimeSeries(request, "DAILY")) {
        if (point.date() != null) {
          holdingsByDay.put(point.date(), point);
        }
      }
    }

    Map<LocalDate, BigDecimal> distributionByExDate =
        exDateDistributions(
            rows,
            monthlyDividendPayoutQuery.findByStockItemIdInOrderByPayDateDescRecordDateDesc(
                List.of(stockItemId)));

    List<StockPriceChartPoint> points = new ArrayList<>();
    for (StockOhlcRow row : rows) {
      if (endDate != null && row.tradeDate().isAfter(endDate)) {
        break;
      }
      if (row.close() == null || row.close().signum() <= 0) {
        continue;
      }
      BigDecimal close = chartClose.getOrDefault(row.tradeDate(), row.close());
      points.add(
          new StockPriceChartPoint(
              row.tradeDate(),
              scale(row.open(), close, row.close()),
              scale(row.high(), close, row.close()),
              scale(row.low(), close, row.close()),
              normalize(close),
              averageCost(holdingsByDay.get(row.tradeDate()), close),
              distribution(distributionByExDate.get(row.tradeDate()), close, row)));
    }
    return points;
  }

  /**
   * 지급기준일마다 분배락일(지급기준일 전 마지막 거래일)에 주당 분배금을 모은다(실제 주식 1 주당, 원 단위). 실측 0094M0 2026-09-15 기준일 540 원:
   * 원주가가 9-11 18,085 -> 9-14(분배락) 16,995. 기준일보다 앞선 시세가 없으면 뺀다.
   */
  static Map<LocalDate, BigDecimal> exDateDistributions(
      List<StockOhlcRow> rowsAsc,
      List<net.luversof.api.stock.domain.MonthlyDividendPayout> payouts) {
    Map<LocalDate, BigDecimal> byExDate = new HashMap<>();
    if (rowsAsc.isEmpty() || payouts == null) {
      return byExDate;
    }
    java.util.TreeSet<LocalDate> days = new java.util.TreeSet<>();
    rowsAsc.forEach(row -> days.add(row.tradeDate()));
    for (var payout : payouts) {
      if (payout == null
          || payout.getRecordDate() == null
          || payout.getDividendAmountPerShare() == null
          || payout.getDividendAmountPerShare().signum() <= 0) {
        continue;
      }
      LocalDate exDate = days.lower(payout.getRecordDate());
      if (exDate != null) {
        byExDate.merge(exDate, payout.getDividendAmountPerShare(), BigDecimal::add);
      }
    }
    return byExDate;
  }

  /** 주당 분배금(실제 주식 기준)을 차트 단위로 - 그 날 (차트 종가 / 원주가) 배율. 원주가가 없으면 그대로. */
  static BigDecimal distribution(BigDecimal amount, BigDecimal chartClose, StockOhlcRow row) {
    if (amount == null) {
      return null;
    }
    if (row.rawClose() == null || row.rawClose().signum() <= 0 || chartClose.compareTo(row.rawClose()) == 0) {
      return normalize(amount);
    }
    return normalize(amount.multiply(chartClose).divide(row.rawClose(), 2, RoundingMode.HALF_UP));
  }

  /** 수정 주가 한 값을 그 날 (차트 종가 / 수정 종가) 배율로 옮긴다. */
  static BigDecimal scale(BigDecimal adjusted, BigDecimal chartClose, BigDecimal adjustedClose) {
    if (adjusted == null) {
      return null;
    }
    if (chartClose.compareTo(adjustedClose) == 0) {
      return adjusted;
    }
    return normalize(adjusted.multiply(chartClose).divide(adjustedClose, 2, RoundingMode.HALF_UP));
  }

  /** 그 날 보유분 평균 단가를 차트 단위로: 원가 x 차트 종가 / 평가액. 보유가 없으면 null. */
  static BigDecimal averageCost(TradeProfitTimeSeriesPoint holdings, BigDecimal chartClose) {
    if (holdings == null
        || holdings.totalHoldingsValue() == null
        || holdings.totalHoldingsValue().signum() <= 0
        || holdings.totalHoldingsCost() == null
        || holdings.totalHoldingsCost().signum() <= 0) {
      return null;
    }
    return normalize(
        holdings
            .totalHoldingsCost()
            .multiply(chartClose)
            .divide(holdings.totalHoldingsValue(), 2, RoundingMode.HALF_UP));
  }

  /** 나눠 떨어지면 정수로(140800.00 이 아니라 140800) - 다른 가격 값과 같은 모양. */
  static BigDecimal normalize(BigDecimal value) {
    if (value == null) {
      return null;
    }
    return value.stripTrailingZeros().scale() <= 0
        ? value.setScale(0, RoundingMode.UNNECESSARY)
        : value;
  }
}
