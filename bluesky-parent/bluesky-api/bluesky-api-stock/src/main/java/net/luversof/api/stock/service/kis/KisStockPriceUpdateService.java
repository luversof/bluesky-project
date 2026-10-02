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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import net.luversof.api.stock.domain.OpenApiConfig;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockItemDateRange;
import net.luversof.api.stock.domain.StockItemTradeDate;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.MonthlyDividendProfileRepository;
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

  /**
   * 월배당 프로필로 등록만 해 둔 종목(매매 · 배당 이력이 없는 종목)을 처음 채울 때 거슬러 갈 날수.
   *
   * <p>처음에는 10 영업일만 씨앗으로 받았다 &mdash; 현재가만 있으면 됐기 때문이다. 그런데 화면에 <b>1 · 3 · 6 · 12 개월 수익률</b>을 싣기로
   * 하면서(사용자 결정 2026-09-21) 그만큼의 이력이 필요해졌다. 실측 당시 18 종목 중 1 년치를 가진 것은 3 개뿐이었다 (1 · 3 개월 14 · 6 개월 6
   * · 1 년 3).
   *
   * <p>KIS 일별시세는 한 호출에 100 행이라 2 년(약 490 영업일)이면 종목당 5 호출이다 &mdash; 처음 한 번만 그렇고, 그 뒤로는 마지막 날 다음부터만
   * 받으므로 하루치씩이다. 상장이 2 년 안 된 ETF 는 상장 이후만 온다(없는 날은 응답에 없다).
   */
  private static final int MONTHLY_DIVIDEND_SEED_DAYS = 365 * 2;

  /**
   * KIS 호출 사이 간격(ms). <b>0.2 초에서 줄이지 말 것.</b>
   *
   * <p>실측 2026-09-23: 0.1 초로 줄였더니 KIS 가 "초당 거래건수 초과" 로 거절해 54 종목 중 4 · 2 종목이 실패했다(0.2 초는 실패 0). 속도는
   * 간격이 아니라 헛호출을 없애서 얻는다 - {@link #noHistoryBefore} 참고.
   */
  static final long KIS_CALL_INTERVAL_MS = 200;

  /**
   * 종목별 "이 날 이전에는 시세가 없다" (상장 전). 과거 구간을 받아 봤는데 한 행도 안 늘면 그 종목의 첫 시세일을 적어 둔다.
   *
   * <p>월배당 프로필 종목은 2 년 전부터 받는데, 상장이 늦은 ETF 는 그 앞 시세가 원래 없다 &mdash; 실측 2026-09-23: 호출 75 회 중 52 회가
   * 매번 같은 상장 전 빈 구간을 100 일씩 다시 부른 것이었다(0219E0 는 8 회 중 7 회). 서버 메모리라 재기동 뒤 첫 갱신은 한 번 더 확인한다 &mdash;
   * 상장일을 모으면 그때 영구 저장으로 바꾼다.
   */
  private final Map<UUID, LocalDate> noHistoryBefore =
      new java.util.concurrent.ConcurrentHashMap<>();

  /**
   * 과거 구간을 받아야 하는가. 원하는 시작일이 저장된 첫날보다 이르고, 그 첫날이 "이 앞엔 없다" 고 이미 확인된 날이 아니면 받는다. 저장된 첫날이 바뀌면(행이 지워지는
   * 등) 다시 확인한다.
   */
  static boolean shouldBackfill(
      LocalDate wantedStart, LocalDate storedFirst, LocalDate knownNoHistoryBefore) {
    return wantedStart.isBefore(storedFirst) && !storedFirst.equals(knownNoHistoryBefore);
  }

  @Autowired private DividendRepository dividendRepository;

  @Autowired private TradeRepository tradeRepository;

  @Autowired private MonthlyDividendProfileRepository monthlyDividendProfileRepository;

  @Autowired private StockItemRepository stockItemRepository;

  @Autowired private StockPriceHistoryRepository stockPriceHistoryRepository;

  @Autowired private KisAuthService kisAuthService;

  @Autowired private RestTemplate kisRestTemplate;

  @Value("${kis.api.base-url:https://openapi.koreainvestment.com:9443}")
  private String baseUrl;

  /**
   * 종목 하나만 갱신한다(사용자 요청 2026-09-22).
   *
   * <p>전체 갱신은 53 종목에 18~22 초가 걸려 게이트의 읽기 제한(10 초)에 매번 끊겼다(실측 2026-09-22). 프로필을 새로 넣은 종목 하나 때문에 전체를
   * 돌릴 까닭이 없다 &mdash; 한 종목이면 1 초 안쪽이다.
   *
   * <p>대상을 고르는 규칙(보유 · 매매 이력 · 월배당 프로필)은 전체 갱신과 같은 코드를 쓴다. 여기서는 고를 종목만 좁힌다.
   *
   * @param symbol 종목 코드. 모르는 코드면 404.
   */
  public PriceHistoryUpdateResult updatePriceHistory(UUID userId, String symbol) {
    if (!StringUtils.hasText(symbol)) {
      return updatePriceHistory(userId);
    }

    StockItem stockItem = stockItemRepository.findBySymbol(symbol.trim());
    if (stockItem == null) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "unknown stock item symbol: " + symbol);
    }
    return updatePriceHistory(userId, java.util.Set.of(stockItem.getId()), null);
  }

  /** 종목 하나를 끝낼 때마다 불린다. 백그라운드 작업이 "몇 / 몇" 을 말할 수 있게 하는 유일한 통로다. */
  @FunctionalInterface
  public interface ProgressListener {
    void onProgress(int processedSymbolCount, int targetSymbolCount);
  }

  public PriceHistoryUpdateResult updatePriceHistory(UUID userId, ProgressListener listener) {
    return updatePriceHistory(userId, (java.util.Set<UUID>) null, listener);
  }

  public PriceHistoryUpdateResult updatePriceHistory(UUID userId) {
    return updatePriceHistory(userId, (java.util.Set<UUID>) null, null);
  }

  /** onlyStockItemIds 가 null 이면 대상 전체, 아니면 그 종목만. */
  private PriceHistoryUpdateResult updatePriceHistory(
      UUID userId, java.util.Set<UUID> onlyStockItemIds, ProgressListener listener) {
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

    // 월배당 프로필로 등록한 종목(보유 · 매매와 무관). 목록 화면이 "현재가 기준 연배당 수익률" 로 종목을 견주므로 현재가가 있어야
    // 한다 - 실측 2026-09-21: 등록 12 종목 중 미보유 4 종목이 시세 없음 2 · 171 일 전 1 · 173 일 전 1 이었다(사용자 결정).
    // 활성 여부로 거르지 않는다 - 목록 화면은 비활성 프로필도 함께 보여 준다.
    java.util.Set<UUID> monthlyDividendStockItemIds = new HashSet<>();
    monthlyDividendProfileRepository
        .findAllByOrderByDisplayOrderAscUpdatedDateDesc()
        .forEach(
            profile -> {
              if (profile.getStockItemId() != null) {
                monthlyDividendStockItemIds.add(profile.getStockItemId());
              }
            });

    // Trade · Dividend 이력이 있거나, 보유 중이거나, 월배당 프로필로 등록한 종목이 갱신 대상
    java.util.Set<UUID> targetStockItemIds = new java.util.HashSet<>();
    targetStockItemIds.addAll(stockItemMinDateMap.keySet());
    targetStockItemIds.addAll(currentlyHeldStockItemIds);
    targetStockItemIds.addAll(historyRefreshTargetDatesByStockItemId.keySet());
    targetStockItemIds.addAll(monthlyDividendStockItemIds);

    // 한 종목만 고르라고 했으면 여기서 좁힌다 - 창(window) 을 정하는 규칙은 그대로 쓴다.
    if (onlyStockItemIds != null) {
      targetStockItemIds.clear();
      targetStockItemIds.addAll(onlyStockItemIds);
    }

    List<StockItem> stockItemsAssigned = new ArrayList<>();
    // 대상 종목을 단건 findById 루프(N+1) 대신 한 번의 findAllById로 조회한다.
    stockItemRepository.findAllById(targetStockItemIds).forEach(stockItemsAssigned::add);
    Map<UUID, StockItem> stockItemMap =
        stockItemsAssigned.stream().collect(Collectors.toMap(StockItem::getId, item -> item));

    // 종목별 실패를 세어 호출자에게 알린다. 예전에는 실패해도 경고 한 줄만 남기고 넘어가
    // 이 작업이 늘 성공으로 보였고, 가격에 구멍이 생겨도 화면에서 간접적으로만 드러났다.
    // 진행 상황의 분모는 처음부터 맞아야 한다 - 루프를 돌며 세면 "3/0" 같은 수가 나간다.
    // 루프 안 판정과 같은 메서드를 쓴다(따로 적으면 둘이 어긋난다).
    int plannedSymbolCount = (int) stockItemsAssigned.stream().filter(this::fetchable).count();
    if (listener != null) {
      listener.onProgress(0, plannedSymbolCount);
    }

    int targetSymbolCount = 0;
    List<String> failedSymbols = new ArrayList<>();

    for (StockItem stockItem : stockItemsAssigned) {
      UUID stockItemId = stockItem.getId();
      DateRange window =
          resolvePriceWindow(
              today,
              stockItemMinDateMap.get(stockItemId),
              stockItemMaxDateMap.get(stockItemId),
              currentlyHeldStockItemIds.contains(stockItemId),
              monthlyDividendStockItemIds.contains(stockItemId));
      LocalDate minDate = window.start();
      LocalDate maxDate = window.end();
      List<LocalDate> refreshTargetDates = historyRefreshTargetDatesByStockItemId.get(stockItemId);

      if (!fetchable(stockItem)) {
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

        // dbMin 이전에 가져와야할 과거 데이터가 있는 경우 - 상장 전이라 없다고 확인된 구간은 다시 부르지 않는다.
        if (shouldBackfill(minDate, dbMin, noHistoryBefore.get(stockItemId))) {
          if (!fetchRangesInBlocks(
              userId, stockItemId, stockItem.getSymbol(), minDate, dbMin.minusDays(1))) {
            symbolSucceeded = false;
          } else {
            LocalDate firstAfter =
                stockPriceHistoryRepository
                    .findTopByStockItemIdOrderByTradeDateAsc(stockItemId)
                    .map(StockPriceHistory::getTradeDate)
                    .orElse(dbMin);
            // 모두 성공했는데 첫날이 그대로면 그 앞에는 시세가 없다(실패한 호출로는 적지 않는다 - 일시 오류를 "없다" 로 굳히면 안 된다).
            if (firstAfter.equals(dbMin)) {
              noHistoryBefore.put(stockItemId, dbMin);
            }
          }
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

      if (listener != null) {
        listener.onProgress(targetSymbolCount, plannedSymbolCount);
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
  record DateRange(LocalDate start, LocalDate end) {}

  /**
   * 종목 하나를 어느 구간으로 받을지 정한다.
   *
   * <ul>
   *   <li>시작: 원장(매매 · 배당) 이력이 있으면 그 첫날. 없으면 오늘 &mdash; 단 월배당 프로필 종목은 10 영업일 전부터(휴장일 빈손 방지).
   *   <li>끝: 보유 중이거나 월배당 프로필 종목이면 오늘. 둘 다 아니면 마지막 거래 · 배당일까지만(예전 그대로).
   * </ul>
   *
   * <p>미보유인데 프로필인 종목은 끝이 오늘이 되므로, 이력이 끊긴 날부터 오늘까지 한 번에 메워진다(그 뒤로는 하루치씩).
   */
  /**
   * 시세를 받아 올 수 있는 종목인가. KIS 로 조회되는 시장만 대상이다.
   *
   * <p>진행 상황의 분모를 미리 세는 곳과 루프 안에서 거르는 곳이 <b>같은 판정</b>을 쓰게 한 메서드다. 둘을 따로 적으면 분모와 분자가 어긋나 "51 / 53
   * 완료" 로 멈춘 것처럼 보인다.
   */
  private boolean fetchable(StockItem stockItem) {
    return stockItem.getSymbol() != null
        && ("KRX".equalsIgnoreCase(stockItem.getMarket())
            || "KOSPI".equalsIgnoreCase(stockItem.getMarket())
            || "KOSDAQ".equalsIgnoreCase(stockItem.getMarket()));
  }

  static DateRange resolvePriceWindow(
      LocalDate today,
      LocalDate ledgerMinDate,
      LocalDate ledgerMaxDate,
      boolean currentlyHeld,
      boolean monthlyDividendProfile) {
    // 프로필 종목은 "원장 첫날" 과 "2 년 전" 중 이른 날부터 받는다 - 내가 산 날부터만 받으면 기간 수익률의 기준이
    // 종목마다 달라진다(실측 2026-09-21: 2 년 씨앗을 넣고도 원장이 2026-03-25 부터인 종목들 탓에 6 개월이 10/18 이었다).
    LocalDate seed = today.minusDays(MONTHLY_DIVIDEND_SEED_DAYS);
    LocalDate start;
    if (monthlyDividendProfile) {
      start = ledgerMinDate != null && ledgerMinDate.isBefore(seed) ? ledgerMinDate : seed;
    } else {
      start = ledgerMinDate != null ? ledgerMinDate : today;
    }
    LocalDate end =
        currentlyHeld || monthlyDividendProfile
            ? today
            : (ledgerMaxDate != null ? ledgerMaxDate : today);
    return new DateRange(start, end);
  }

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
        Thread.sleep(KIS_CALL_INTERVAL_MS);
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
      ResponseEntity<KisDailyPriceResponse> response = exchangeWithRateLimitRetry(url, entity);

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

  /** KIS 가 "초당 거래건수 초과" 로 거절했을 때 기다리는 시간(ms)과 다시 부르는 횟수. */
  static final long KIS_RATE_LIMIT_WAIT_MS = 1000;

  static final int KIS_RATE_LIMIT_RETRIES = 2;

  /** 원주가 채우기 결과. 열이 없으면 columnAvailable=false 로 아무것도 안 한다. */
  public record RawClosePriceFillResult(
      boolean columnAvailable,
      int targetDays,
      int filledRows,
      int apiCalls,
      List<String> failedSymbols) {}

  /**
   * 모든 시세 행의 원주가(수정 전 종가)를 KIS 에서 받아 채운다(2026-10-02).
   *
   * <p>수정 종가는 받은 때마다 기준이 다르다(KIS 는 분배금까지 수정한다) - 처음 한꺼번에 받은 옛 구간은 깎여 있고 매일 이어 받은 최근 구간은 그대로라, 한 종목
   * 안에서도 이어지지 않는다. 그 경계에 가짜 하루 변동이 생기고(0094M0 2026-03-25 수정 +18.65% / 실제 +1.92%), 수정 종가로 평가하면 분배락
   * 하락이 지워진 채 지급일에 분배금을 또 더해 두 번 셌다. 평가(TradeProfitService) · 기간 수익률 · 위험 지표 · 가격 차트를 원주가로 내려면 행마다
   * 원주가가 있어야 한다.
   *
   * <p>처음에는 매매일만, 다음엔 보유 기간 · 카탈로그 창만 받았다. 차트는 사용자가 고른 기간 전체를 그려 결국 모든 행이 필요하다. 이미 채운 날은 건너뛰어 두
   * 번째부터는 새 날만 부른다 - 단 최근 7 일은 늘 다시 받는다(장중 갱신으로 들어간 원주가를 확정 종가로 덮으려고). 연속한 날은 묶어 100 일 단위로 부른다(KIS
   * 한 호출 100 행, 호출 간격은 시세 갱신과 같은 0.2 초).
   *
   * <p>열이 없는 DB(schema-alter 2026-10-02 묶음 미적용)면 아무것도 하지 않는다.
   *
   * @param userId KIS 인증 설정을 쓸 사용자(대상 종목은 사용자와 무관하게 시세가 있는 전부다)
   */
  public RawClosePriceFillResult fillRawClosePrices(UUID userId) {
    if (stockPriceHistoryRepository.countRawClosePriceColumn() == 0) {
      log.info("rawClosePrice column is missing - skip raw close fill");
      return new RawClosePriceFillResult(false, 0, 0, 0, List.of());
    }
    java.util.Set<UUID> idSet = new HashSet<>();
    stockItemRepository.findAll().forEach(item -> idSet.add(item.getId()));
    if (idSet.isEmpty()) {
      return new RawClosePriceFillResult(true, 0, 0, 0, List.of());
    }
    String ids = idSet.stream().map(UUID::toString).collect(Collectors.joining(","));
    Map<UUID, java.util.TreeSet<LocalDate>> tradeDays = new HashMap<>();
    // 최근 7 일은 채워져 있어도 다시 받는다(장중 갱신으로 들어간 원주가를 확정 종가로) - 같은 구간이라 호출 수는 그대로다.
    LocalDate recentFrom = LocalDate.now(MARKET_ZONE_ID).minusDays(7);
    for (var row : stockPriceHistoryRepository.findRawCloseDaysToFill(ids, recentFrom)) {
      tradeDays
          .computeIfAbsent(row.stockItemId(), key -> new java.util.TreeSet<>())
          .add(row.tradeDate());
    }
    Map<UUID, StockItem> items = new HashMap<>();
    stockItemRepository
        .findAllById(tradeDays.keySet())
        .forEach(item -> items.put(item.getId(), item));

    OpenApiConfig config;
    try {
      config = kisAuthService.getValidConfig(userId);
    } catch (Exception e) {
      log.warn("KIS API Auth is not configured: {}", e.getMessage());
      return new RawClosePriceFillResult(true, 0, 0, 0, List.of("auth"));
    }
    int targetDays = 0;
    int filledRows = 0;
    int apiCalls = 0;
    List<String> failed = new ArrayList<>();
    for (var entry : tradeDays.entrySet()) {
      StockItem item = items.get(entry.getKey());
      if (item == null || !fetchable(item)) {
        continue;
      }
      List<LocalDate> needed = List.copyOf(entry.getValue());
      if (needed.isEmpty()) {
        continue;
      }
      targetDays += needed.size();
      java.util.Set<LocalDate> neededSet = new HashSet<>(needed);
      for (DateRange range : splitIntoBlocks(toContiguousRanges(needed))) {
        try {
          if (apiCalls > 0) {
            Thread.sleep(KIS_CALL_INTERVAL_MS);
          }
          apiCalls++;
          for (KisDailyPriceItem day :
              fetchDailyPrices(config, item.getSymbol(), range.start(), range.end(), "1")) {
            if (day.getStck_bsop_date() == null
                || day.getStck_bsop_date().isEmpty()
                || day.getStck_clpr() == null) {
              continue;
            }
            LocalDate date =
                LocalDate.parse(day.getStck_bsop_date(), DateTimeFormatter.ofPattern("yyyyMMdd"));
            if (neededSet.contains(date)) {
              filledRows +=
                  stockPriceHistoryRepository.updateRawClosePrice(
                      entry.getKey(), date, new BigDecimal(day.getStck_clpr()));
            }
          }
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          failed.add(item.getSymbol());
          break;
        } catch (RuntimeException e) {
          log.warn(
              "raw close fetch failed: {} {}~{} - {}",
              item.getSymbol(),
              range.start(),
              range.end(),
              e.getMessage());
          failed.add(item.getSymbol());
        }
      }
    }
    log.info(
        "raw close fill: target days {}, filled rows {}, api calls {}, failed {}",
        targetDays,
        filledRows,
        apiCalls,
        failed);
    return new RawClosePriceFillResult(true, targetDays, filledRows, apiCalls, List.copyOf(failed));
  }

  /** 구간을 100 일 이하로 쪼갠다 - KIS 일별시세는 한 호출에 100 행까지라 더 긴 구간은 앞쪽이 잘린다(fetchRangesInBlocks 와 같은 폭). */
  static List<DateRange> splitIntoBlocks(List<DateRange> ranges) {
    List<DateRange> blocks = new ArrayList<>();
    for (DateRange range : ranges) {
      LocalDate start = range.start();
      while (!start.isAfter(range.end())) {
        LocalDate end = start.plusDays(99);
        if (end.isAfter(range.end())) {
          end = range.end();
        }
        blocks.add(new DateRange(start, end));
        start = end.plusDays(1);
      }
    }
    return blocks;
  }

  /** 기간 일별 시세 한 번 조회. adjusted "0" = 수정주가, "1" = 원주가. */
  private List<KisDailyPriceItem> fetchDailyPrices(
      OpenApiConfig config,
      String symbol,
      LocalDate startDate,
      LocalDate endDate,
      String adjusted) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("authorization", "Bearer " + config.getAccessToken());
    headers.set("appkey", config.getAppKey());
    headers.set("appsecret", config.getAppSecret());
    headers.set("tr_id", "FHKST03010100");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    String url =
        UriComponentsBuilder.fromUriString(
                baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
            .queryParam("FID_INPUT_ISCD", symbol)
            .queryParam("FID_INPUT_DATE_1", startDate.format(formatter))
            .queryParam("FID_INPUT_DATE_2", endDate.format(formatter))
            .queryParam("FID_PERIOD_DIV_CODE", "D")
            .queryParam("FID_ORG_ADJ_PRC", adjusted)
            .build()
            .toUriString();
    ResponseEntity<KisDailyPriceResponse> response =
        exchangeWithRateLimitRetry(url, new HttpEntity<>(headers));
    if (response.getBody() == null || response.getBody().getOutput2() == null) {
      return List.of();
    }
    return response.getBody().getOutput2();
  }

  /**
   * 초당 한도 거절(EGW00201)만 쉬었다 다시 부른다. 다른 오류는 그대로 던진다(호출자가 실패로 센다).
   *
   * <p>실측 2026-09-23: 간격 0.2 초에서도 호출 75 회 중 1 회 · 28 회 중 1 회가 이 이유로 거절돼 종목이 "실패" 로 남았다. 잠깐 뒤 다시 부르면
   * 되는 일시 거절이라, 실패로 세면 사용자는 매번 다시 갱신해야 한다.
   */
  ResponseEntity<KisDailyPriceResponse> exchangeWithRateLimitRetry(
      String url, HttpEntity<?> entity) {
    for (int attempt = 0; ; attempt++) {
      try {
        return kisRestTemplate.exchange(url, HttpMethod.GET, entity, KisDailyPriceResponse.class);
      } catch (org.springframework.web.client.RestClientResponseException e) {
        if (attempt >= KIS_RATE_LIMIT_RETRIES || !isRateLimited(e.getResponseBodyAsString())) {
          throw e;
        }
        log.info(
            "KIS rate limited (EGW00201), retry {} after {}ms",
            attempt + 1,
            KIS_RATE_LIMIT_WAIT_MS);
        try {
          Thread.sleep(KIS_RATE_LIMIT_WAIT_MS);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw e;
        }
      }
    }
  }

  /** 응답 본문이 KIS 초당 한도 거절인가. */
  static boolean isRateLimited(String body) {
    return body != null && body.contains("EGW00201");
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
