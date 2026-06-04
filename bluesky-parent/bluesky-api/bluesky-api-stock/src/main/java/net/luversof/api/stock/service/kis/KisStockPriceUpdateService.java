package net.luversof.api.stock.service.kis;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import net.luversof.api.stock.domain.OpenApiConfig;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockItemDateRange;
import net.luversof.api.stock.domain.StockItemTradeDate;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.repository.DailyAccountSnapshotRepository;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.service.kis.dto.KisDailyPriceItem;
import net.luversof.api.stock.service.kis.dto.KisDailyPriceResponse;

@Service
public class KisStockPriceUpdateService {

  private static final ZoneId MARKET_ZONE_ID = ZoneId.of("Asia/Seoul");

  @Autowired private DailyAccountSnapshotRepository dailyAccountSnapshotRepository;

  @Autowired private DividendRepository dividendRepository;

  @Autowired private TradeRepository tradeRepository;

  @Autowired private StockItemRepository stockItemRepository;

  @Autowired private StockPriceHistoryRepository stockPriceHistoryRepository;

  @Autowired private KisAuthService kisAuthService;

  private final RestTemplate restTemplate = new RestTemplate();

  public void updatePriceHistory() {
    Map<UUID, LocalDate> stockItemMinDateMap = new HashMap<>();
    Map<UUID, LocalDate> stockItemMaxDateMap = new HashMap<>();

    ZoneId zoneId = MARKET_ZONE_ID;

    for (StockItemDateRange range : dividendRepository.findDividendDateRanges()) {
      if (range.stockItemId() == null) continue;
      LocalDate minDate =
          range.minDate() != null ? range.minDate().atZone(zoneId).toLocalDate() : null;
      LocalDate maxDate =
          range.maxDate() != null ? range.maxDate().atZone(zoneId).toLocalDate() : null;

      updateMinMaxMap(
          stockItemMinDateMap, stockItemMaxDateMap, range.stockItemId(), minDate, maxDate);
    }

    for (StockItemDateRange range : tradeRepository.findTradeDateRanges()) {
      if (range.stockItemId() == null) continue;
      LocalDate minDate =
          range.minDate() != null ? range.minDate().atZone(zoneId).toLocalDate() : null;
      LocalDate maxDate =
          range.maxDate() != null ? range.maxDate().atZone(zoneId).toLocalDate() : null;

      updateMinMaxMap(
          stockItemMinDateMap, stockItemMaxDateMap, range.stockItemId(), minDate, maxDate);
    }

    LocalDate today = LocalDate.now(zoneId);

    // 현재 보유 중인 종목 ID 집합 (net quantity > 0)
    java.util.Set<UUID> currentlyHeldStockItemIds =
        new HashSet<>(tradeRepository.findCurrentlyHeldStockItemIds());

    Map<UUID, List<LocalDate>> historyRefreshTargetDatesByStockItemId =
        stockPriceHistoryRepository.findRefreshTargetTradeDates().stream()
            .collect(
                Collectors.groupingBy(
                    StockItemTradeDate::stockItemId,
                    Collectors.mapping(StockItemTradeDate::tradeDate, Collectors.toList())));

    // Trade 또는 Dividend 이력이 있는 종목만 갱신 대상
    java.util.Set<UUID> targetStockItemIds = new java.util.HashSet<>();
    targetStockItemIds.addAll(stockItemMinDateMap.keySet());
    targetStockItemIds.addAll(currentlyHeldStockItemIds);
    targetStockItemIds.addAll(historyRefreshTargetDatesByStockItemId.keySet());

    List<StockItem> stockItemsAssigned = new ArrayList<>();
    for (UUID id : targetStockItemIds) {
      stockItemRepository.findById(id).ifPresent(stockItemsAssigned::add);
    }
    Map<UUID, StockItem> stockItemMap =
        stockItemsAssigned.stream().collect(Collectors.toMap(StockItem::getId, item -> item));

    for (StockItem stockItem : stockItemsAssigned) {
      UUID stockItemId = stockItem.getId();
      LocalDate minDate = stockItemMinDateMap.getOrDefault(stockItemId, today);
      // 현재 보유 중이면 오늘까지, 더 이상 보유하지 않으면 마지막 거래/배당 날짜까지만 갱신
      LocalDate maxDate =
          currentlyHeldStockItemIds.contains(stockItemId)
              ? today
              : stockItemMaxDateMap.getOrDefault(stockItemId, today);
      List<LocalDate> refreshTargetDates = historyRefreshTargetDatesByStockItemId.get(stockItemId);

      if (stockItem.getSymbol() == null
          || (!"KRX".equalsIgnoreCase(stockItem.getMarket())
              && !"KOSPI".equalsIgnoreCase(stockItem.getMarket())
              && !"KOSDAQ".equalsIgnoreCase(stockItem.getMarket()))) {
        continue;
      }

      Optional<StockPriceHistory> topAsc =
          stockPriceHistoryRepository.findTopByStockItemIdOrderByTradeDateAsc(stockItemId);
      Optional<StockPriceHistory> topDesc =
          stockPriceHistoryRepository.findTopByStockItemIdOrderByTradeDateDesc(stockItemId);

      if (topAsc.isPresent() && topDesc.isPresent()) {
        LocalDate dbMin = topAsc.get().getTradeDate();
        LocalDate dbMax = topDesc.get().getTradeDate();

        // dbMin 이전에 가져와야할 과거 데이터가 있는 경우
        if (minDate.isBefore(dbMin)) {
          fetchRangesInBlocks(stockItemId, stockItem.getSymbol(), minDate, dbMin.minusDays(1));
        }

        if (refreshTargetDates != null && !refreshTargetDates.isEmpty()) {
          // tradeDate와 updatedDate가 같은 history의 날짜들만 다시 조회한다.
          fetchRangesInBlocksForDates(stockItemId, stockItem.getSymbol(), refreshTargetDates);
        } else if (maxDate.isAfter(dbMax)) {
          fetchRangesInBlocks(stockItemId, stockItem.getSymbol(), dbMax.plusDays(1), maxDate);
        }
      } else {
        fetchRangesInBlocks(stockItemId, stockItem.getSymbol(), minDate, maxDate);
      }
    }
  }

  private void fetchRangesInBlocksForDates(
      UUID stockItemId, String symbol, List<LocalDate> tradeDates) {
    if (tradeDates == null || tradeDates.isEmpty()) {
      return;
    }

    List<LocalDate> sortedDates = tradeDates.stream().distinct().sorted().toList();
    LocalDate rangeStart = sortedDates.get(0);
    LocalDate previousDate = sortedDates.get(0);

    for (int index = 1; index < sortedDates.size(); index++) {
      LocalDate currentDate = sortedDates.get(index);
      // 주말/휴장일 간격(1~2일)은 같은 조회 구간으로 묶어 API 호출 수를 줄인다.
      if (currentDate.isAfter(previousDate.plusDays(3))) {
        fetchRangesInBlocks(stockItemId, symbol, rangeStart, previousDate);
        rangeStart = currentDate;
      }
      previousDate = currentDate;
    }

    fetchRangesInBlocks(stockItemId, symbol, rangeStart, previousDate);
  }

  private void fetchRangesInBlocks(
      UUID stockItemId, String symbol, LocalDate startDate, LocalDate endDate) {
    LocalDate currentStartDate = startDate;
    while (!currentStartDate.isAfter(endDate)) {
      LocalDate currentEndDate = currentStartDate.plusDays(99);
      if (currentEndDate.isAfter(endDate)) {
        currentEndDate = endDate;
      }

      fetchAndSavePriceHistory(stockItemId, symbol, currentStartDate, currentEndDate);

      currentStartDate = currentEndDate.plusDays(1);

      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void updateMinMaxMap(
      Map<UUID, LocalDate> minMap,
      Map<UUID, LocalDate> maxMap,
      UUID stockItemId,
      LocalDate minDate,
      LocalDate maxDate) {
    if (minDate != null) {
      minMap.compute(stockItemId, (k, v) -> (v == null || minDate.isBefore(v)) ? minDate : v);
    }
    if (maxDate != null) {
      maxMap.compute(stockItemId, (k, v) -> (v == null || maxDate.isAfter(v)) ? maxDate : v);
    }
  }

  private LocalDate getMin(LocalDate d1, LocalDate d2) {
    if (d1 == null) return d2;
    if (d2 == null) return d1;
    return d1.isBefore(d2) ? d1 : d2;
  }

  private LocalDate getMax(LocalDate d1, LocalDate d2) {
    if (d1 == null) return d2;
    if (d2 == null) return d1;
    return d1.isAfter(d2) ? d1 : d2;
  }

  private void fetchAndSavePriceHistory(
      UUID stockItemId, String symbol, LocalDate startDate, LocalDate endDate) {
    OpenApiConfig config;
    try {
      config = kisAuthService.getValidConfig();
    } catch (Exception e) {
      System.out.println("KIS API Auth is not configured: " + e.getMessage());
      return;
    }

    String baseUrl = "https://openapi.koreainvestment.com:9443";
    String path = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";

    HttpHeaders headers = new HttpHeaders();
    headers.set("authorization", "Bearer " + config.getAccessToken());
    headers.set("appkey", config.getAppKey());
    headers.set("appsecret", config.getAppSecret());
    headers.set("tr_id", "FHKST03010100");

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    String url =
        UriComponentsBuilder.fromUriString(baseUrl + path)
            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
            .queryParam("FID_INPUT_ISCD", symbol)
            .queryParam("FID_INPUT_DATE_1", startDate.format(formatter))
            .queryParam("FID_INPUT_DATE_2", endDate.format(formatter))
            .queryParam("FID_PERIOD_DIV_CODE", "D")
            .queryParam("FID_ORG_ADJ_PRC", "0")
            .build()
            .toUriString();

    try {
      HttpEntity<?> entity = new HttpEntity<>(headers);
      ResponseEntity<KisDailyPriceResponse> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, KisDailyPriceResponse.class);

      if (response.getBody() == null || response.getBody().getOutput2() == null) {
        return;
      }

      List<KisDailyPriceItem> items = response.getBody().getOutput2();

      List<StockPriceHistory> existingHistories =
          stockPriceHistoryRepository.findByStockItemIdAndTradeDateBetween(
              stockItemId, startDate, endDate);
      Map<LocalDate, StockPriceHistory> existingHistoryMap =
          existingHistories.stream()
              .collect(Collectors.toMap(StockPriceHistory::getTradeDate, h -> h));

      List<StockPriceHistory> newHistories = new ArrayList<>();
      ZoneId zoneId = MARKET_ZONE_ID;
      LocalDate today = LocalDate.now(zoneId);
      Instant updatedNow = Instant.now();
      LocalDate snapshotInvalidationFromDate = null;
      boolean priceAdjustmentDetected = false;

      for (KisDailyPriceItem item : items) {
        if (item.getStck_bsop_date() == null || item.getStck_bsop_date().isEmpty()) {
          continue;
        }

        LocalDate tradeDate = LocalDate.parse(item.getStck_bsop_date(), formatter);
        StockPriceHistory history = existingHistoryMap.get(tradeDate);
        BigDecimal newOpen = new BigDecimal(item.getStck_oprc());
        BigDecimal newHigh = new BigDecimal(item.getStck_hgpr());
        BigDecimal newLow = new BigDecimal(item.getStck_lwpr());
        BigDecimal newClose = new BigDecimal(item.getStck_clpr());
        long newVolume = Long.parseLong(item.getAcml_vol());
        boolean shouldSave;

        if (history == null) {
          history = new StockPriceHistory();
          history.setStockItemId(stockItemId);
          history.setTradeDate(tradeDate);
          shouldSave = true;
        } else {
          boolean updatedOnSameTradeDate = false;
          if (history.getUpdatedDate() != null) {
            LocalDate updatedLocalDate = history.getUpdatedDate().atZone(zoneId).toLocalDate();
            updatedOnSameTradeDate = updatedLocalDate.equals(tradeDate);
          }

          // 오래된 날짜라도 KIS 응답 값이 바뀌면 재저장한다.
          // 이렇게 해야 과거 거래일의 수정주가/거래량 보정이 다음 실행 때 반영된다.
          shouldSave =
              history.getUpdatedDate() == null
                  || updatedOnSameTradeDate
                  || history.getVolume() == 0L && tradeDate.isBefore(today)
                  || hasMeaningfulHistoryChange(
                      history, newOpen, newHigh, newLow, newClose, newVolume);
        }

        if (shouldSave) {
          boolean shouldInvalidateSnapshots = history.getId() == null;
          if (!shouldInvalidateSnapshots) {
            shouldInvalidateSnapshots =
                hasMeaningfulHistoryChange(history, newOpen, newHigh, newLow, newClose, newVolume);
          }
          if (shouldInvalidateSnapshots) {
            snapshotInvalidationFromDate = getMin(snapshotInvalidationFromDate, tradeDate);
          }

          // 수정주가 재조정 감지: 기존 종가와 신규 종가 차이가 2% 초과이고,
          // 오늘 날짜(장중 업데이트)가 아닌 과거 날짜인 경우 → 분할/합병 등 이벤트로 판단
          if (history.getId() != null
              && history.getClosePrice() != null
              && !tradeDate.isEqual(LocalDate.now(zoneId))) {
            BigDecimal oldClose = history.getClosePrice();
            // 변동률 = |신규 - 기존| / 기존
            java.math.BigDecimal changeRatio =
                newClose
                    .subtract(oldClose)
                    .abs()
                    .divide(oldClose, 6, java.math.RoundingMode.HALF_UP);
            if (changeRatio.compareTo(new java.math.BigDecimal("0.02")) > 0) {
              priceAdjustmentDetected = true;
            }
          }

          history.setOpenPrice(newOpen);
          history.setHighPrice(newHigh);
          history.setLowPrice(newLow);
          history.setClosePrice(newClose);
          history.setVolume(newVolume);
          // Auditing 설정 유무와 무관하게 갱신 시각이 확실히 반영되도록 명시적으로 세팅한다.
          history.setUpdatedDate(updatedNow);

          newHistories.add(history);
        }
      }

      if (!newHistories.isEmpty()) {
        stockPriceHistoryRepository.saveAll(newHistories);
        invalidateSnapshotsFromChangedDate(
            stockItemId, snapshotInvalidationFromDate, priceAdjustmentDetected);
      }
    } catch (Exception e) {
      System.out.println(
          "Failed to fetch history for symbol "
              + symbol
              + " range "
              + startDate
              + " to "
              + endDate
              + ": "
              + e.getMessage());
    }
  }

  private boolean hasMeaningfulHistoryChange(
      StockPriceHistory history,
      BigDecimal newOpen,
      BigDecimal newHigh,
      BigDecimal newLow,
      BigDecimal newClose,
      long newVolume) {
    return history.getOpenPrice() == null
        || history.getHighPrice() == null
        || history.getLowPrice() == null
        || history.getClosePrice() == null
        || history.getOpenPrice().compareTo(newOpen) != 0
        || history.getHighPrice().compareTo(newHigh) != 0
        || history.getLowPrice().compareTo(newLow) != 0
        || history.getClosePrice().compareTo(newClose) != 0
        || history.getVolume() != newVolume;
  }

  private void invalidateSnapshotsFromChangedDate(
      UUID stockItemId, LocalDate fromDate, boolean priceAdjustmentDetected) {
    if (fromDate == null) {
      return;
    }
    try {
      dailyAccountSnapshotRepository.deleteByWmaStateContainingStockItemIdAndDateGreaterThanEqual(
          stockItemId.toString(), fromDate);
      if (priceAdjustmentDetected) {
        System.out.println(
            "Invalidated DailyAccountSnapshots for stockItemId "
                + stockItemId
                + " from "
                + fromDate
                + " due to price adjustment or corrected history.");
      } else {
        System.out.println(
            "Invalidated DailyAccountSnapshots for stockItemId "
                + stockItemId
                + " from "
                + fromDate
                + " due to inserted/updated price history.");
      }
    } catch (Exception ex) {
      System.out.println(
          "Failed to invalidate snapshots for stockItemId "
              + stockItemId
              + " from "
              + fromDate
              + ": "
              + ex.getMessage());
    }
  }
}
