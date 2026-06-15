package net.luversof.api.stock.service.kis;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  private static final Logger log = LoggerFactory.getLogger(KisStockPriceUpdateService.class);

  private static final ZoneId MARKET_ZONE_ID = ZoneId.of("Asia/Seoul");

  @Autowired private DailyAccountSnapshotRepository dailyAccountSnapshotRepository;

  @Autowired private DividendRepository dividendRepository;

  @Autowired private TradeRepository tradeRepository;

  @Autowired private StockItemRepository stockItemRepository;

  @Autowired private StockPriceHistoryRepository stockPriceHistoryRepository;

  @Autowired private KisAuthService kisAuthService;

  @Autowired private RestTemplate kisRestTemplate;

  @Value("${kis.api.base-url:https://openapi.koreainvestment.com:9443}")
  private String baseUrl;

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
    // 대상 종목을 단건 findById 루프(N+1) 대신 한 번의 findAllById로 조회한다.
    stockItemRepository.findAllById(targetStockItemIds).forEach(stockItemsAssigned::add);
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

        // 과거 보정(refreshTargetDates)과 전진 갱신(dbMax+1 ~ today)을 하나의 구간 집합으로 모은다.
        // - 과거 보정 여부와 무관하게 전진 갱신을 항상 포함시켜, 한 번의 실행으로 오늘자까지 반영한다.
        //   (둘을 else if로 묶으면 refreshTargetDates가 있는 동안 오늘자 갱신이 스킵되어
        //    갱신을 2번 실행해야 반영되는 한 박자 지연이 발생한다.)
        // - 겹치거나 인접한(주말·휴장 ≤ 3일) 구간은 병합해 KIS API 호출 수를 줄인다.
        //   refreshTargetDates의 최근 날짜와 전진 구간은 대개 인접하므로 1콜로 합쳐진다.
        List<DateRange> fetchRanges = new ArrayList<>();
        if (refreshTargetDates != null && !refreshTargetDates.isEmpty()) {
          fetchRanges.addAll(toContiguousRanges(refreshTargetDates));
        }
        if (maxDate.isAfter(dbMax)) {
          fetchRanges.add(new DateRange(dbMax.plusDays(1), maxDate));
        }
        for (DateRange range : mergeAdjacentRanges(fetchRanges)) {
          fetchRangesInBlocks(stockItemId, stockItem.getSymbol(), range.start(), range.end());
        }
      } else {
        fetchRangesInBlocks(stockItemId, stockItem.getSymbol(), minDate, maxDate);
      }
    }
  }

  /** 조회 구간 [start, end] (양끝 포함). */
  private record DateRange(LocalDate start, LocalDate end) {}

  /**
   * 정렬된 날짜 목록을 연속 구간으로 묶는다. 주말/휴장일 간격(1~3일)은 같은 조회 구간으로 묶어 API 호출 수를 줄인다.
   */
  private List<DateRange> toContiguousRanges(List<LocalDate> tradeDates) {
    if (tradeDates == null || tradeDates.isEmpty()) {
      return List.of();
    }

    List<LocalDate> sortedDates = tradeDates.stream().distinct().sorted().toList();
    List<DateRange> ranges = new ArrayList<>();
    LocalDate rangeStart = sortedDates.get(0);
    LocalDate previousDate = sortedDates.get(0);

    for (int index = 1; index < sortedDates.size(); index++) {
      LocalDate currentDate = sortedDates.get(index);
      if (currentDate.isAfter(previousDate.plusDays(3))) {
        ranges.add(new DateRange(rangeStart, previousDate));
        rangeStart = currentDate;
      }
      previousDate = currentDate;
    }

    ranges.add(new DateRange(rangeStart, previousDate));
    return ranges;
  }

  /** 겹치거나 인접한(주말·휴장 ≤ 3일) 구간을 하나로 병합해 중복/연속 조회를 1콜로 합친다. */
  private List<DateRange> mergeAdjacentRanges(List<DateRange> ranges) {
    if (ranges.size() <= 1) {
      return ranges;
    }

    List<DateRange> sorted =
        ranges.stream().sorted(Comparator.comparing(DateRange::start)).toList();
    List<DateRange> merged = new ArrayList<>();
    LocalDate currentStart = sorted.get(0).start();
    LocalDate currentEnd = sorted.get(0).end();

    for (int index = 1; index < sorted.size(); index++) {
      DateRange range = sorted.get(index);
      if (!range.start().isAfter(currentEnd.plusDays(3))) {
        if (range.end().isAfter(currentEnd)) {
          currentEnd = range.end();
        }
      } else {
        merged.add(new DateRange(currentStart, currentEnd));
        currentStart = range.start();
        currentEnd = range.end();
      }
    }

    merged.add(new DateRange(currentStart, currentEnd));
    return merged;
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

  private void fetchAndSavePriceHistory(
      UUID stockItemId, String symbol, LocalDate startDate, LocalDate endDate) {
    OpenApiConfig config;
    try {
      config = kisAuthService.getValidConfig();
    } catch (Exception e) {
      log.warn("KIS API Auth is not configured: {}", e.getMessage());
      return;
    }

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
          kisRestTemplate.exchange(url, HttpMethod.GET, entity, KisDailyPriceResponse.class);

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

          // KIS 응답 값이 기존과 실제로 다를 때만 재저장한다(과거 수정주가/거래량 보정 반영).
          boolean meaningfulChange =
              hasMeaningfulHistoryChange(history, newOpen, newHigh, newLow, newClose, newVolume);

          // 장중에 저장되어(updatedDate==tradeDate) refresh 대상으로 남아있는 "과거" 거래일 레코드는,
          // 장 마감 후 값이 동일하더라도 updatedDate를 한 번 갱신해 refresh 대상에서 제외시킨다.
          // (그렇지 않으면 매 실행마다 재조회·재저장되는 무한 루프가 된다.)
          // 오늘자 레코드는 장중 변동을 계속 반영해야 하므로 여기서 제외 → 값이 바뀔 때만 저장한다.
          boolean needsFinalityConfirmation = updatedOnSameTradeDate && tradeDate.isBefore(today);

          // 값이 동일하면 저장하지 않는다. 단, updatedDate가 없는 레거시 레코드와
          // finality 확정이 필요한 과거 장중 레코드는 1회 저장한다.
          shouldSave =
              history.getUpdatedDate() == null || meaningfulChange || needsFinalityConfirmation;
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
      log.warn(
          "Failed to fetch history for symbol {} range {} to {}", symbol, startDate, endDate, e);
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
        log.info(
            "Invalidated DailyAccountSnapshots for stockItemId {} from {} due to price adjustment or corrected history.",
            stockItemId,
            fromDate);
      } else {
        log.info(
            "Invalidated DailyAccountSnapshots for stockItemId {} from {} due to inserted/updated price history.",
            stockItemId,
            fromDate);
      }
    } catch (Exception ex) {
      log.warn("Failed to invalidate snapshots for stockItemId {} from {}", stockItemId, fromDate, ex);
    }
  }
}
