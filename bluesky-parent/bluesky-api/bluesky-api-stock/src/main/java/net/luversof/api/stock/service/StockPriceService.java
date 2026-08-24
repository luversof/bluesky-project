package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.domain.StockDailyClosePrice;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;

@Service
public class StockPriceService {

  @Autowired private StockPriceHistoryRepository stockPriceHistoryRepository;

  @Autowired
  private net.luversof.api.stock.repository.StockDailyClosePriceQuery stockDailyClosePriceQuery;

  public void setStockPriceHistoryRepository(
      StockPriceHistoryRepository stockPriceHistoryRepository) {
    this.stockPriceHistoryRepository = stockPriceHistoryRepository;
  }

  public BigDecimal getCurrentPrice(UUID stockItemId) {
    return getCurrentPriceHistory(stockItemId)
        .map(StockPriceHistory::getClosePrice)
        .orElse(BigDecimal.ZERO);
  }

  /**
   * 가장 최근 종가와 그 종가의 거래일을 함께 돌려준다.
   *
   * <p>화면의 "현재가"는 실제로는 마지막으로 수집된 종가라, 오늘 시세가 아직 없으면 며칠 전 값일 수 있다. 어느 날 기준인지 보여주려면 일자가 필요한데, 이미 같은
   * 조회에서 읽고 버리던 값이라 추가 쿼리는 없다.
   */
  public Optional<StockPriceHistory> getCurrentPriceHistory(UUID stockItemId) {
    // 거래가 있던 날을 먼저 찾는다. 거래량 0 행은 그 날 거래가 없었다는 뜻이라 "그 날의 종가"가 아니다.
    // 모든 행이 거래량 0 인 종목(수집이 무거래일에만 돌았던 경우)은 값이 아예 사라지면 안 되므로 폴백한다.
    return stockPriceHistoryRepository
        .findTopByStockItemIdAndVolumeGreaterThanOrderByTradeDateDesc(stockItemId, 0L)
        .or(
            () ->
                stockPriceHistoryRepository.findTopByStockItemIdOrderByTradeDateDesc(stockItemId));
  }

  /** 종목별 최신 종가(+거래일)를 한 번에 조회한다. 빈 입력이면 빈 맵. */
  public java.util.Map<UUID, StockDailyClosePrice> getLatestPrices(
      java.util.Collection<UUID> stockItemIds) {
    if (stockItemIds == null || stockItemIds.isEmpty()) {
      return java.util.Map.of();
    }
    java.util.Set<UUID> distinct = new java.util.LinkedHashSet<>(stockItemIds);
    distinct.remove(null);
    if (distinct.isEmpty()) {
      return java.util.Map.of();
    }
    String ids =
        distinct.stream().map(UUID::toString).collect(java.util.stream.Collectors.joining(","));
    java.util.Map<UUID, StockDailyClosePrice> result = new java.util.HashMap<>();
    for (StockDailyClosePrice row : stockPriceHistoryRepository.findLatestClosePrices(ids)) {
      if (row != null && row.stockItemId() != null) {
        result.put(row.stockItemId(), row);
      }
    }
    return result;
  }

  public BigDecimal getPriceAt(UUID stockItemId, LocalDate at) {
    if (at == null) return getCurrentPrice(stockItemId);
    return stockPriceHistoryRepository
        .findTopByStockItemIdAndTradeDateLessThanEqualAndVolumeGreaterThanOrderByTradeDateDesc(
            stockItemId, at, 0L)
        .or(
            () ->
                stockPriceHistoryRepository
                    .findTopByStockItemIdAndTradeDateLessThanEqualOrderByTradeDateDesc(
                        stockItemId, at))
        .map(StockPriceHistory::getClosePrice)
        .orElseGet(() -> getCurrentPrice(stockItemId));
  }

  /**
   * 종목마다 조회 구간이 다른 일별 종가 조회.
   *
   * <p>보유 시작 전과 전량 매도 후의 행은 시뮬레이션에서 쓰이지 않으므로 아예 읽지 않는다.
   */
  /**
   * 종목별 조회 구간을 (일자 -> (종목 -> 종가)) 맵으로 읽는다.
   *
   * <p>{@code ids} 문자열을 만드는 순서 그대로 {@code idOrder} 를 쌓아 넘긴다. 조회 쪽이 종목 UUID 대신 이 순번을 돌려주므로 행마다 UUID
   * 를 새로 만들지 않는다.
   */
  public Map<LocalDate, Map<UUID, BigDecimal>> getDailyClosePricesGrouped(
      Map<UUID, LocalDate[]> rangeByStockItemId) {
    if (rangeByStockItemId == null || rangeByStockItemId.isEmpty()) {
      return new HashMap<>();
    }
    StringBuilder ids = new StringBuilder();
    StringBuilder froms = new StringBuilder();
    StringBuilder tos = new StringBuilder();
    List<UUID> idOrder = new ArrayList<>();
    for (Map.Entry<UUID, LocalDate[]> entry : rangeByStockItemId.entrySet()) {
      LocalDate[] range = entry.getValue();
      if (entry.getKey() == null || range == null || range[0] == null || range[1] == null) {
        continue;
      }
      if (range[1].isBefore(range[0])) {
        continue;
      }
      if (!ids.isEmpty()) {
        ids.append(',');
        froms.append(',');
        tos.append(',');
      }
      ids.append(entry.getKey());
      froms.append(range[0]);
      tos.append(range[1]);
      idOrder.add(entry.getKey());
    }
    if (ids.isEmpty()) {
      return new HashMap<>();
    }
    return stockDailyClosePriceQuery.findDailyClosePricesGrouped(
        ids.toString(), froms.toString(), tos.toString(), idOrder);
  }

  /** 기준일 목록 각각에 대해 종목별 "그 날 이하의 마지막 종가"를 한 행씩 조회한다. */
  /** (종목, 기준일) 쌍 목록으로 최근 종가를 조회한다. 두 리스트는 같은 길이의 병렬 배열이다. */
  public List<StockDailyClosePrice> getLatestClosePricesForPairs(
      List<UUID> stockItemIds, List<LocalDate> days) {
    if (stockItemIds == null || days == null || stockItemIds.isEmpty()) {
      return List.of();
    }
    String ids =
        stockItemIds.stream().map(UUID::toString).collect(java.util.stream.Collectors.joining(","));
    String dayCsv =
        days.stream().map(LocalDate::toString).collect(java.util.stream.Collectors.joining(","));
    return stockDailyClosePriceQuery.findLatestClosePricesForPairs(ids, dayCsv);
  }
}
