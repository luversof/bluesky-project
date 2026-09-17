package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.domain.StockItemCashFlow;
import net.luversof.api.stock.domain.StockItemFirstBuy;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.web.dto.response.StockCashFlowResponse;

/**
 * 종목별 최초 매수일.
 *
 * <p>자산 현황이 종목마다 보유 기간과 연평균 수익률을 적는 데 쓴다. 날짜 하나를 얻자고 원장 전체를 내려받지 않는다.
 *
 * <p>저장된 값은 instant 다. 존을 정해 날짜로 바꾸는 일은 <b>서버에서</b> 한다 &mdash; UTC 문자열을 잘라 쓰면 KST 기준으로 하루 앞으로
 * 밀린다(같은 실수를 세 번 했다).
 */
@Service
public class StockHoldingPeriodService {

  /** 존을 안 주면 한국 기준. 이 사용자의 원장은 한국 거래일 기준으로 적혀 있다. */
  public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

  @Autowired private TradeRepository tradeRepository;

  public void setTradeRepository(TradeRepository tradeRepository) {
    this.tradeRepository = tradeRepository;
  }

  /**
   * 종목별 최초 매수일(요청 존 기준).
   *
   * @param zoneId 널이면 {@link #DEFAULT_ZONE}
   * @param accountIdList 널이거나 비면 좁히지 않음
   */
  public Map<UUID, LocalDate> findFirstBuyDateByStockItem(
      UUID userId, ZoneId zoneId, List<UUID> accountIdList) {
    ZoneId zone = zoneId == null ? DEFAULT_ZONE : zoneId;
    List<StockItemFirstBuy> rows =
        tradeRepository.findFirstBuyDateByStockItem(userId, joinIds(accountIdList));
    Map<UUID, LocalDate> result = new LinkedHashMap<>();
    if (rows == null) {
      return result;
    }
    for (StockItemFirstBuy row : rows) {
      if (row == null || row.stockItemId() == null) {
        continue;
      }
      Instant firstBuy = row.firstBuyDate();
      if (firstBuy == null) {
        continue;
      }
      result.put(row.stockItemId(), firstBuy.atZone(zone).toLocalDate());
    }
    return result;
  }

  /**
   * 종목별 하루치 순현금흐름(요청 존의 날짜, 오름차순). 자산 현황 · 종목 상세의 연평균 수익률(XIRR)이 쓴다.
   *
   * <p>같은 날의 돈은 더해 한 건으로 둔다 &mdash; XIRR 은 날짜 단위로 할인하므로 같은 날의 매수 · 매도 · 배당을 나눠 둘 까닭이 없다. 날짜는 최초
   * 매수일과 같은 규칙으로 <b>서버에서</b> 존을 정해 바꾼다(UTC 문자열을 자르면 KST 로 하루 밀린다).
   *
   * @param zoneId 널이면 {@link #DEFAULT_ZONE}
   * @param accountIdList 널이거나 비면 좁히지 않음
   * @param stockItemIdList 널이거나 비면 좁히지 않음
   */
  public Map<UUID, List<StockCashFlowResponse>> findCashFlowsByStockItem(
      UUID userId, ZoneId zoneId, List<UUID> accountIdList, List<UUID> stockItemIdList) {
    return netByDay(
        tradeRepository.findCashFlowsByStockItem(
            userId, joinIds(accountIdList), joinIds(stockItemIdList)),
        zoneId == null ? DEFAULT_ZONE : zoneId);
  }

  /** 종목마다 같은 날(존 기준)의 돈을 더한다. 종목 · 날짜 · 금액이 빠진 줄은 버린다. */
  static Map<UUID, List<StockCashFlowResponse>> netByDay(
      List<StockItemCashFlow> rows, ZoneId zone) {
    Map<UUID, TreeMap<java.time.LocalDate, BigDecimal>> byItem = new LinkedHashMap<>();
    if (rows != null) {
      for (StockItemCashFlow row : rows) {
        if (row == null
            || row.stockItemId() == null
            || row.flowAt() == null
            || row.amount() == null) {
          continue;
        }
        byItem
            .computeIfAbsent(row.stockItemId(), key -> new TreeMap<>())
            .merge(row.flowAt().atZone(zone).toLocalDate(), row.amount(), BigDecimal::add);
      }
    }
    Map<UUID, List<StockCashFlowResponse>> result = new LinkedHashMap<>();
    byItem.forEach(
        (stockItemId, days) -> {
          List<StockCashFlowResponse> flows = new ArrayList<>();
          days.forEach((date, amount) -> flows.add(new StockCashFlowResponse(date, amount)));
          result.put(stockItemId, flows);
        });
    return result;
  }

  /** 빈 목록은 "좁히지 않음"(null)과 같게 둔다. */
  private static String joinIds(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return null;
    }
    String joined =
        ids.stream()
            .filter(java.util.Objects::nonNull)
            .map(UUID::toString)
            .collect(Collectors.joining(","));
    return joined.isEmpty() ? null : joined;
  }
}
