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
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.service.kis.dto.KisDailyPriceItem;
import net.luversof.api.stock.service.kis.dto.KisDailyPriceResponse;
import net.luversof.api.stock.web.dto.response.PriceHistoryUpdateResult;

@Service
public class KisStockPriceUpdateService {

  private static final Logger log = LoggerFactory.getLogger(KisStockPriceUpdateService.class);

  private static final ZoneId MARKET_ZONE_ID = ZoneId.of("Asia/Seoul");

  @Autowired private DividendRepository dividendRepository;

  @Autowired private TradeRepository tradeRepository;

  @Autowired private StockItemRepository stockItemRepository;

  @Autowired private StockPriceHistoryRepository stockPriceHistoryRepository;

  @Autowired private KisAuthService kisAuthService;

  @Autowired private RestTemplate kisRestTemplate;

  @Value("${kis.api.base-url:https://openapi.koreainvestment.com:9443}")
  private String baseUrl;

  public PriceHistoryUpdateResult updatePriceHistory(UUID userId) {
    Map<UUID, LocalDate> stockItemMinDateMap = new HashMap<>();
    Map<UUID, LocalDate> stockItemMaxDateMap = new HashMap<>();

    ZoneId zoneId = MARKET_ZONE_ID;

    accumulateDateRanges(
        dividendRepository.findDividendDateRanges(),
        stockItemMinDateMap,
        stockItemMaxDateMap,
        zoneId);
    accumulateDateRanges(
        tradeRepository.findTradeDateRanges(), stockItemMinDateMap, stockItemMaxDateMap, zoneId);

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

    // 종목별 실패를 세어 호출자에게 알린다. 예전에는 실패해도 경고 한 줄만 남기고 넘어가
    // 이 작업이 늘 성공으로 보였고, 가격에 구멍이 생겨도 화면에서 간접적으로만 드러났다.
    int targetSymbolCount = 0;
    List<String> failedSymbols = new ArrayList<>();

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

      targetSymbolCount++;
      boolean symbolSucceeded = true;
      if (topAsc.isPresent() && topDesc.isPresent()) {
        LocalDate dbMin = topAsc.get().getTradeDate();
        LocalDate dbMax = topDesc.get().getTradeDate();

        // dbMin 이전에 가져와야할 과거 데이터가 있는 경우
        if (minDate.isBefore(dbMin)
            && !fetchRangesInBlocks(
                userId, stockItemId, stockItem.getSymbol(), minDate, dbMin.minusDays(1))) {
          symbolSucceeded = false;
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
          if (!fetchRangesInBlocks(
              userId, stockItemId, stockItem.getSymbol(), range.start(), range.end())) {
            symbolSucceeded = false;
          }
        }
      } else if (!fetchRangesInBlocks(
          userId, stockItemId, stockItem.getSymbol(), minDate, maxDate)) {
        symbolSucceeded = false;
      }

      if (!symbolSucceeded) {
        failedSymbols.add(stockItem.getSymbol());
      }
    }

    // 흩어진 종목별 경고와 별개로, 한 줄로 결과를 남긴다. 실패가 몇 개인지 로그를 뒤지지 않고 알 수 있다.
    if (failedSymbols.isEmpty()) {
      log.info("price history update finished: {} symbols, no failures", targetSymbolCount);
    } else {
      log.warn(
          "price history update finished with failures: {}/{} symbols failed - {}",
          failedSymbols.size(),
          targetSymbolCount,
          failedSymbols);
    }
    return new PriceHistoryUpdateResult(targetSymbolCount, List.copyOf(failedSymbols));
  }

  /** 조회 구간 [start, end] (양끝 포함). */
  private record DateRange(LocalDate start, LocalDate end) {}

  /** 정렬된 날짜 목록을 연속 구간으로 묶는다. 주말/휴장일 간격(1~3일)은 같은 조회 구간으로 묶어 API 호출 수를 줄인다. */
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

  /** 구간 하나라도 실패하면 false. */
  private boolean fetchRangesInBlocks(
      UUID userId, UUID stockItemId, String symbol, LocalDate startDate, LocalDate endDate) {
    boolean allSucceeded = true;
    LocalDate currentStartDate = startDate;
    while (!currentStartDate.isAfter(endDate)) {
      LocalDate currentEndDate = currentStartDate.plusDays(99);
      if (currentEndDate.isAfter(endDate)) {
        currentEndDate = endDate;
      }

      if (!fetchAndSavePriceHistory(
          userId, stockItemId, symbol, currentStartDate, currentEndDate)) {
        allSucceeded = false;
      }

      currentStartDate = currentEndDate.plusDays(1);

      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return allSucceeded;
  }

  /**
   * 배당·매매 두 원천의 종목별 날짜 범위를 같은 규칙으로 합친다.
   *
   * <p>{@code minDate}/{@code maxDate} 는 <b>instant</b> 다. 시장 타임존으로 바꿔야 "그 종목의 거래가 있었던 날" 이 나온다
   * &mdash; UTC 로 읽으면 KST 오전 0~9 시 사이 기록이 하루 앞으로 밀려, 수집 시작일이 하루 어긋난 채로 KIS 를 부른다.
   *
   * <p>예전에는 배당용·매매용으로 같은 10 줄이 나란히 복사돼 있었다(저장소 이름만 달랐다). 한쪽만 고치면 그 원천의 날짜만 밀린다.
   *
   * @param ranges 종목별 최소/최대 일자(둘 다 null 일 수 있다)
   */
  static void accumulateDateRanges(
      List<StockItemDateRange> ranges,
      Map<UUID, LocalDate> minMap,
      Map<UUID, LocalDate> maxMap,
      ZoneId zoneId) {
    for (StockItemDateRange range : ranges) {
      if (range.stockItemId() == null) {
        continue;
      }
      LocalDate minDate =
          range.minDate() != null ? range.minDate().atZone(zoneId).toLocalDate() : null;
      LocalDate maxDate =
          range.maxDate() != null ? range.maxDate().atZone(zoneId).toLocalDate() : null;
      updateMinMaxMap(minMap, maxMap, range.stockItemId(), minDate, maxDate);
    }
  }

  private static void updateMinMaxMap(
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

  /** 조회·저장에 성공하면 true. 실패는 여기서 삼키지 않고 호출자가 셀 수 있게 알린다. */
  private boolean fetchAndSavePriceHistory(
      UUID userId, UUID stockItemId, String symbol, LocalDate startDate, LocalDate endDate) {
    OpenApiConfig config;
    try {
      config = kisAuthService.getValidConfig(userId);
    } catch (Exception e) {
      // 인증 설정이 없으면 이 실행의 모든 종목이 실패한다. 성공으로 넘기면 그 사실이 사라진다.
      log.warn("KIS API Auth is not configured: {}", e.getMessage());
      return false;
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

      // 응답이 비었다는 건 그 구간에 시세가 없다는 뜻(휴장 등)이지 실패가 아니다.
      if (response.getBody() == null || response.getBody().getOutput2() == null) {
        return true;
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
      // 과거 시세가 바뀐 가장 이른 날짜와 수정주가 재조정(분할/합병) 여부.
      // 예전에는 스냅샷 캐시 무효화에 썼고, 캐시를 없앤 지금은 "과거 평가액이 바뀐다"는
      // 신호로 로그에 남긴다(모든 과거 시점 평가가 재계산되므로 알아둘 가치가 있다).
      LocalDate changedHistoryFromDate = null;
      boolean priceAdjustmentDetected = false;
      // 거래가 없어 새 행을 만들지 않은 건수. 조용히 건너뛰면 "왜 어제까지만 있지?"를 설명할 수 없다.
      int skippedZeroVolume = 0;

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
          // 거래량 0 인 날은 그 날 거래가 없었다는 뜻이고, 그럴 때 KIS 는 종가 자리에 직전 종가를 실어 보낸다.
          // 그대로 새 행으로 넣으면 "그 날의 확정 종가"가 하나 생겨 평가 기준 일자가 실제보다 앞당겨진다
          // (실측 2026-08-22: 2026-08-20 행 9건이 전부 거래량 0 이고 종가는 08-19 와 동일한데, 화면은
          // "평가 기준 2026-08-20 종가"라고 적고 있었다. 시가/고가/저가/거래량까지 같은 건 0 건이라
          // 단순 행 복제가 아니라 이 유령 행이 원인이다).
          //
          // 넣지 않아도 평가액은 달라지지 않는다 - 어차피 직전 종가를 쓰고, 그 값이 같기 때문이다.
          // 달라지는 것은 화면에 적히는 '기준 일자'뿐이고, 그게 사실에 맞게 된다.
          // 이미 있는 행은 건드리지 않는다(과거 보정으로 거래량이 0 으로 정정되는 경우가 있을 수 있다).
          if (newVolume == 0L) {
            skippedZeroVolume++;
            continue;
          }
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
            changedHistoryFromDate = getMin(changedHistoryFromDate, tradeDate);
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
      }

      if (skippedZeroVolume > 0) {
        log.info(
            "{}: skipped {} zero-volume day(s) — no trading occurred, so no settled close exists"
                + " for those dates",
            symbol,
            skippedZeroVolume);
      }

      if (changedHistoryFromDate != null) {
        if (priceAdjustmentDetected) {
          log.info(
              "Price adjustment detected for {} from {} (split/merger suspected);"
                  + " historical valuations change accordingly.",
              symbol,
              changedHistoryFromDate);
        } else {
          log.debug("Price history changed for {} from {}", symbol, changedHistoryFromDate);
        }
      }
      return true;
    } catch (Exception e) {
      log.warn(
          "Failed to fetch history for symbol {} range {} to {}", symbol, startDate, endDate, e);
      return false;
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
}
