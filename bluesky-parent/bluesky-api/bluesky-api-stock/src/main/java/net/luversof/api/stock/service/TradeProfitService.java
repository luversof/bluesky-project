package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.service.strategy.ProfitCalculator;
import net.luversof.api.stock.web.dto.request.DividendSearchRequest;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.request.TradeProfitRequestType;
import net.luversof.api.stock.web.dto.request.TradeSearchRequest;
import net.luversof.api.stock.web.dto.response.HoldingsSnapshotItem;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesResult;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesSummary;
import net.luversof.api.stock.web.dto.response.TradeProfitYearlySummary;
import net.luversof.api.stock.web.dto.response.TradeResponse;

/** 통합 주식 손익 계산 서비스 실현손익(매매손익)과 미실현손익(보유손익)을 하나의 객체로 제공 */
@Service
public class TradeProfitService {

  private static final Logger log = LoggerFactory.getLogger(TradeProfitService.class);
  private static final BigDecimal MIN_CORPORATE_ACTION_FACTOR = BigDecimal.valueOf(2);
  private static final BigDecimal CORPORATE_ACTION_FACTOR_TOLERANCE = new BigDecimal("0.15");

  private final AccountService accountService;
  private final TradeService tradeService;
  private final StockPriceService stockPriceService;
  private final ProfitCalculator profitCalculator;
  private final StockItemService stockItemService;
  private final DividendService dividendService;

  public TradeProfitService(
      AccountService accountService,
      TradeService tradeService,
      StockPriceService stockPriceService,
      ProfitCalculator profitCalculator,
      StockItemService stockItemService,
      DividendService dividendService) {
    this.accountService = accountService;
    this.tradeService = tradeService;
    this.stockPriceService = stockPriceService;
    this.profitCalculator = profitCalculator;
    this.stockItemService = stockItemService;
    this.dividendService = dividendService;
  }

  public List<TradeProfit> calculateProfit(TradeProfitRequest request) {
    // 요청 기준으로 tradeList를 조회
    // 기간 요청이 있더라도 평단가 계산을 위해 전체 데이터를 조회해야 함
    List<Trade> tradeList =
        switch (request.getRequestType()) {
          case USER -> {
            var accountList = accountService.findByUserId(request.getUserId());
            if (accountList.isEmpty()) {
              StockErrorCode.INVALID_USER_ID.throwException();
            }
            yield tradeService.findByAccountIdIn(accountList.stream().map(Account::getId).toList());
          }
          case USER_ACCOUNT -> {
            var accountList = accountService.findByIdIn(request.getAccountIdList());
            if (accountList.isEmpty()) {
              StockErrorCode.INVALID_USER_ID.throwException();
            }

            accountList.stream()
                .forEach(
                    account -> {
                      if (!account.getUserId().equals(request.getUserId())) {
                        StockErrorCode.INVALID_USER_ID.throwException(
                            request.getUserId(), account.getId());
                      }
                    });

            yield tradeService.findByAccountIdIn(request.getAccountIdList());
          }
          case USER_STOCKITEM -> {
            var accountList = accountService.findByUserId(request.getUserId());
            if (accountList.isEmpty()) {
              StockErrorCode.INVALID_USER_ID.throwException();
            }

            accountList.stream()
                .forEach(
                    x -> {
                      if (!x.getUserId().equals(request.getUserId())) {
                        StockErrorCode.INVALID_USER_ID.throwException();
                      }
                    });

            yield tradeService.findByAccountIdInAndStockItemIdIn(
                accountList.stream().map(Account::getId).toList(), request.getStockItemIdList());
          }
          case USER_ACCOUNT_STOCKITEM -> {
            var accountList = accountService.findByIdIn(request.getAccountIdList());
            if (accountList.isEmpty()) {
              StockErrorCode.INVALID_USER_ID.throwException();
            }

            accountList.stream()
                .forEach(
                    account -> {
                      if (!account.getUserId().equals(request.getUserId())) {
                        StockErrorCode.INVALID_USER_ID.throwException(
                            request.getUserId(), account.getId());
                      }
                    });

            yield tradeService.findByAccountIdInAndStockItemIdIn(
                request.getAccountIdList(), request.getStockItemIdList());
          }
        };

    // 그룹별로 기본 손익 계산
    List<TradeProfit> base =
        switch (request.getGroupBy()) {
          case ACCOUNT_AND_STOCKITEM -> calculateProfitByAccountAndStock(tradeList, request);
          case STOCKITEM -> calculateProfitByStock(tradeList, request);
        };

    if (base.isEmpty()) return base;

    return base;
  }

  /** accountId+stockItemId별 통합 손익 통계 (실현손익 + 미실현손익) */
  public List<TradeProfit> calculateProfitByAccountAndStock(
      List<Trade> tradeList, TradeProfitRequest request) {
    Map<String, List<Trade>> grouped =
        tradeList.stream()
            .collect(Collectors.groupingBy(t -> t.getAccountId() + "-" + t.getStockItemId()));
    List<TradeProfit> result = new ArrayList<>();

    for (List<Trade> group : grouped.values()) {
      Trade first = group.get(0);
      UUID accountId = first.getAccountId();
      UUID stockItemId = first.getStockItemId();

      TradeProfit profit = profitCalculator.calculate(group, request, stockPriceService);
      if (request.hasDateRange()) {
        // Include if Realized Profit != 0 OR if there was any Sell Activity OR any Buy
        // Activity
        boolean hasProfit = profit.getRealizedProfit().compareTo(BigDecimal.ZERO) != 0;
        boolean hasSell =
            profit.getTotalSellAmount() != null
                && profit.getTotalSellAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean hasBuy =
            profit.getTotalBuyAmount() != null
                && profit.getTotalBuyAmount().compareTo(BigDecimal.ZERO) > 0;
        if (hasProfit || hasSell || hasBuy) {
          result.add(profit);
        }
      } else {
        if (!isEmptyProfit(profit)) {
          result.add(profit);
        }
      }
    }
    return result;
  }

  /**
   * stockItemId별 통합 손익 통계 (accountId 무시, 실현손익 + 미실현손익) StockItem Symbol이 같으면 통합하여 계산 (중복 데이터 보정)
   */
  public List<TradeProfit> calculateProfitByStock(
      List<Trade> tradeList, TradeProfitRequest request) {
    // 1. StockItem 정보 조회 (Symbol 기준 병합을 위해)
    var stockItemIds = tradeList.stream().map(Trade::getStockItemId).collect(Collectors.toSet());
    Map<UUID, StockItem> stockItemMap = new HashMap<>();
    stockItemService.findAllById(stockItemIds).forEach(si -> stockItemMap.put(si.getId(), si));

    // 2. 그룹핑 (Symbol -> Name -> ID 순으로 식별)
    Map<String, List<Trade>> grouped =
        tradeList.stream()
            .collect(
                Collectors.groupingBy(
                    t -> {
                      var si = stockItemMap.get(t.getStockItemId());
                      if (si != null) {
                        if (si.getSymbol() != null && !si.getSymbol().isBlank()) {
                          return "S:" + si.getSymbol(); // Symbol Prefix
                        }
                        // 2026-01-17: Name match fallback for inconsistent
                        // data
                        // Remove spaces to ensure better matching (e.g.
                        // "Samsung Electronics" vs
                        // "SamsungElectronics")
                        // But risking collision? TIGER REITs name is
                        // specific enough.
                        if (si.getName() != null && !si.getName().isBlank()) {
                          return "N:" + si.getName().trim();
                        }
                      }
                      return "I:" + t.getStockItemId().toString();
                    }));

    List<TradeProfit> result = new ArrayList<>();

    for (List<Trade> group : grouped.values()) {
      if (group.isEmpty()) continue;

      // 대표 ID 사용 (첫번째 Trade의 StockItemId)
      UUID stockItemId = group.get(0).getStockItemId();

      TradeProfit profit = profitCalculator.calculate(group, request, stockPriceService);
      if (request.hasDateRange()) {
        // Include if Realized Profit != 0 OR if there was any Sell Activity OR any Buy
        // Activity
        boolean hasProfit = profit.getRealizedProfit().compareTo(BigDecimal.ZERO) != 0;
        boolean hasSell =
            profit.getTotalSellAmount() != null
                && profit.getTotalSellAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean hasBuy =
            profit.getTotalBuyAmount() != null
                && profit.getTotalBuyAmount().compareTo(BigDecimal.ZERO) > 0;
        if (hasProfit || hasSell || hasBuy) {
          result.add(profit);
        }
      } else {
        if (!isEmptyProfit(profit)) {
          result.add(profit);
        }
      }
    }
    return result;
  }

  private boolean isEmptyProfit(TradeProfit profit) {
    return profit.getHoldingQuantity() == 0
        && profit.getTotalSellQuantity() == 0
        && (profit.getTotalBuyAmount() == null
            || profit.getTotalBuyAmount().compareTo(BigDecimal.ZERO) == 0);
  }

  private static BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  /**
   * 시간 시계열 집계: 전체 거래 내역을 바탕으로 Rolling WMA 계산을 수행한 후, 요청된 기간(start ~ end)에 해당하는 일별 누적 실현손익 스냅샷을
   * 반환합니다.
   */
  public static class WmaState {
    /** 평가용 보유 수량. 일반적으로 rawQuantity와 같고, 정수 배 분할/병합 추정 시에만 환산됩니다. */
    private BigDecimal quantity = BigDecimal.ZERO;

    /** 실제 정수 주식 수량 (전량 매도 여부 판단용) */
    private long rawQuantity = 0;

    private BigDecimal totalCost = BigDecimal.ZERO;
    private BigDecimal totalCostNet = BigDecimal.ZERO;
    private UUID stockItemId;

    public BigDecimal getQuantity() {
      return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
      this.quantity = quantity;
    }

    public long getRawQuantity() {
      return rawQuantity;
    }

    public void setRawQuantity(long rawQuantity) {
      this.rawQuantity = rawQuantity;
    }

    public BigDecimal getTotalCost() {
      return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
      this.totalCost = totalCost;
    }

    public BigDecimal getTotalCostNet() {
      return totalCostNet;
    }

    public void setTotalCostNet(BigDecimal totalCostNet) {
      this.totalCostNet = totalCostNet;
    }

    public UUID getStockItemId() {
      return stockItemId;
    }

    public void setStockItemId(UUID stockItemId) {
      this.stockItemId = stockItemId;
    }

    /** 보유 스냅샷 캡처용 얕은 복사(필드가 모두 불변 타입이라 이것으로 충분). */
    private WmaState copy() {
      WmaState c = new WmaState();
      c.quantity = this.quantity;
      c.rawQuantity = this.rawQuantity;
      c.totalCost = this.totalCost;
      c.totalCostNet = this.totalCostNet;
      c.stockItemId = this.stockItemId;
      return c;
    }
  }

  public List<TradeProfitTimeSeriesPoint> aggregateTimeSeries(
      TradeProfitRequest request, String granularity) {
    return applyGranularity(simulateDailySeries(request, null, null), granularity);
  }

  /**
   * 시리즈와 기간 요약(TWR/손익 등)을 한 번의 시뮬레이션으로 함께 계산한다.
   *
   * <p>예전에는 화면이 시리즈용과 요약용으로 각각 호출해 같은 시뮬레이션(전체 거래 이력)을 두 번 돌렸다. 요약은 다운샘플 이전의 일별 시리즈로 계산해야
   * 정확하므로(주/월봉으로 TWR을 계산하면 틀림) 여기서 함께 만들어 돌려준다.
   */
  public TradeProfitTimeSeriesResult aggregateTimeSeriesWithSummary(
      TradeProfitRequest request, String granularity) {
    List<TradeProfitTimeSeriesPoint> dailySeries = simulateDailySeries(request, null, null);
    return new TradeProfitTimeSeriesResult(
        applyGranularity(dailySeries, granularity),
        summarizeSeries(dailySeries),
        summarizeByYear(dailySeries));
  }

  /**
   * 연도별 성과. 각 연도 구간의 앞에 전년도 마지막 지점을 기초로 붙여 계산한다. 그래야 연초 첫날의 수익률이 빠지지 않고, 기초 평가액이 전년도 종가가 되어 연 단위
   * 비교가 맞는다.
   */
  private List<TradeProfitYearlySummary> summarizeByYear(
      List<TradeProfitTimeSeriesPoint> dailySeries) {
    if (dailySeries == null || dailySeries.isEmpty()) {
      return List.of();
    }

    ZoneId zoneId = ZoneId.systemDefault();
    Map<Integer, List<TradeProfitTimeSeriesPoint>> byYear = new LinkedHashMap<>();
    TradeProfitTimeSeriesPoint previousPoint = null;
    Integer previousYear = null;

    for (TradeProfitTimeSeriesPoint point : dailySeries) {
      if (point == null || point.timestamp() == null) {
        continue;
      }
      int year = point.timestamp().atZone(zoneId).toLocalDate().getYear();
      List<TradeProfitTimeSeriesPoint> bucket = byYear.get(year);
      if (bucket == null) {
        bucket = new ArrayList<>();
        // 연도가 바뀌는 지점: 전년도 마지막 값을 기초로 삼는다.
        if (previousPoint != null && previousYear != null && previousYear != year) {
          bucket.add(previousPoint);
        }
        byYear.put(year, bucket);
      }
      bucket.add(point);
      previousPoint = point;
      previousYear = year;
    }

    List<TradeProfitYearlySummary> result = new ArrayList<>();
    byYear.forEach(
        (year, points) -> {
          TradeProfitTimeSeriesSummary summary = summarizeSeries(points);
          // 보유도 거래도 없던 해(전부 0)는 표에 노이즈만 되므로 제외한다.
          if (!isEmptyYear(summary)) {
            result.add(new TradeProfitYearlySummary(year, summary));
          }
        });
    result.sort(Comparator.comparingInt(TradeProfitYearlySummary::year).reversed());
    return result;
  }

  /**
   * 시계열 집계. captureDates 가 주어지면 해당 날짜의 보유 상태(WmaState)를 capturedStates 에 담는다. 보유 스냅샷 조회가 이 캡처를 쓰므로,
   * 차트 값과 스냅샷 값이 같은 계산에서 나와 항상 일치한다.
   */
  private List<TradeProfitTimeSeriesPoint> simulateDailySeries(
      TradeProfitRequest request,
      Set<LocalDate> captureDates,
      Map<LocalDate, Map<String, WmaState>> capturedStates) {
    Instant end = request.getEndDate() != null ? request.getEndDate() : Instant.now();
    Instant start = request.getStartDate();
    ZoneId zoneId = ZoneId.systemDefault();

    // 1) 전체 트레이드 조회 (날짜 제한 없이 전체 로딩)
    List<Trade> allTrades = new ArrayList<>(loadAllTrades(request));

    if (allTrades.isEmpty()) {
      return new ArrayList<>();
    } // 2) StockItem 정보 로딩 및 그룹핑 키 생성 (calculateProfitByStock과 동일 로직)
    var stockItemIds = allTrades.stream().map(Trade::getStockItemId).collect(Collectors.toSet());
    Map<UUID, StockItem> stockItemMap = new HashMap<>();
    stockItemService.findAllById(stockItemIds).forEach(si -> stockItemMap.put(si.getId(), si));

    // Fetch Dividends for the user/accounts
    DividendSearchRequest dividendRequest = new DividendSearchRequest();
    dividendRequest.setUserId(request.getUserId());
    dividendRequest.setAccountIdList(request.getAccountIdList());
    if (request.getRequestType() == TradeProfitRequestType.USER_STOCKITEM
        || request.getRequestType() == TradeProfitRequestType.USER_ACCOUNT_STOCKITEM) {
      dividendRequest.setStockItemIdList(request.getStockItemIdList());
    }
    List<Dividend> allDividends = new ArrayList<>(dividendService.findDividends(dividendRequest));
    allDividends.sort(Comparator.comparing(Dividend::getPayDate));
    Iterator<Dividend> divIt = allDividends.iterator();
    Dividend nextDividend = divIt.hasNext() ? divIt.next() : null;

    // 그룹핑 키 생성 함수
    Function<Trade, String> getGroupKey =
        t -> {
          var si = stockItemMap.get(t.getStockItemId());
          if (si != null) {
            if (si.getSymbol() != null && !si.getSymbol().isBlank()) {
              return "S:" + si.getSymbol();
            }
            if (si.getName() != null && !si.getName().isBlank()) {
              return "N:" + si.getName().trim();
            }
          }
          return "I:" + t.getStockItemId().toString();
        };

    // 3) 거래 정렬 (날짜 오름차순)
    // 같은 날짜 내에서는 BUY 먼저 처리 (논리적 재고 확보)
    allTrades.sort(
        (t1, t2) -> {
          int dateCompare = t1.getTradeDate().compareTo(t2.getTradeDate());
          if (dateCompare != 0) return dateCompare;
          if (t1.getType() == t2.getType()) return 0;
          return t1.getType() == TradeType.BUY ? -1 : 1;
        });

    // 4) 시뮬레이션 상태 관리 (WMA)
    Map<String, WmaState> stateMap = new HashMap<>();

    BigDecimal globalCumulativeRealized = BigDecimal.ZERO;
    BigDecimal globalCumulativeDividend = BigDecimal.ZERO;

    // 5) 시뮬레이션 루프
    // 시작일: 데이터가 있는 첫 로컬 거래일부터 시작 (Cost Basis 구축을 위해)
    LocalDate firstTradeDate = toLocalDate(allTrades.get(0).getTradeDate(), zoneId);
    LocalDate simulationStart = firstTradeDate;
    // 출력 시작일: 요청상 start 날짜 (없으면 첫 거래일)
    LocalDate outputStart = start != null ? toLocalDate(start, zoneId) : firstTradeDate;
    LocalDate outputEnd = toInclusiveEndDate(end, zoneId);

    // Price History (Bulk Load)
    LocalDate startLocalDate =
        simulationStart.isBefore(outputStart) ? simulationStart : outputStart;
    LocalDate endLocalDate = outputEnd;
    List<StockPriceHistory> priceHistory =
        stockPriceService.getPriceHistory(stockItemIds, startLocalDate, endLocalDate);

    Map<LocalDate, Map<UUID, BigDecimal>> dailyPriceMap = new HashMap<>();
    for (StockPriceHistory h : priceHistory) {
      dailyPriceMap
          .computeIfAbsent(h.getTradeDate(), k -> new HashMap<>())
          .put(h.getStockItemId(), h.getClosePrice());
    }
    Map<UUID, BigDecimal> lastKnownPrices = new HashMap<>();

    List<TradeProfitTimeSeriesPoint> series = new ArrayList<>();
    Iterator<Trade> it = allTrades.iterator();
    Trade nextTrade = it.hasNext() ? it.next() : null;

    LocalDate currentDay = simulationStart.isBefore(outputStart) ? simulationStart : outputStart;
    // ※ 단, simulationStart가 outputStart보다 늦으면 (데이터가 미래에 시작),
    // outputStart ~ simulationStart 구간은 데이터 없음(0)으로 채워야 함.
    // 편의상 currentDay를 Math.min(simulationStart, outputStart)로 잡고 진행.
    if (outputStart.isBefore(simulationStart)) {
      currentDay = outputStart;
    } else {
      currentDay = simulationStart;
    }

    while (!currentDay.isAfter(outputEnd)) {
      // 해당 일자(currentDay)에 포함되는 모든 거래 처리
      long dailyTradeCount = 0;
      long dailyBuyCount = 0;
      long dailyVolume = 0;
      BigDecimal dailyRealizedGain = BigDecimal.ZERO;

      // nextTrade가 currentDay의 끝(inclusive)까지인지 확인
      // tradeDate는 시분초를 포함하므로 로컬 거래일로 변환해 비교한다.
      while (nextTrade != null) {
        LocalDate tradeDay = toLocalDate(nextTrade.getTradeDate(), zoneId);
        if (tradeDay.isAfter(currentDay)) {
          break; // 미래의 거래는 대기
        }

        // 거래 처리 logic (WMA)
        Trade trade = nextTrade;
        String key = getGroupKey.apply(trade);
        WmaState state =
            stateMap.computeIfAbsent(
                key,
                k -> {
                  WmaState s = new WmaState();
                  s.setStockItemId(trade.getStockItemId());
                  return s;
                });
        if (state.getStockItemId() == null) state.setStockItemId(trade.getStockItemId());

        BigDecimal fee = nz(trade.getFee());
        BigDecimal tax = nz(trade.getTax());
        int q = trade.getQuantity();
        BigDecimal tradePrice = trade.getPrice();
        BigDecimal amount = tradePrice.multiply(BigDecimal.valueOf(q));

        // 수정주가 기준 환산 수량 계산:
        // 거래일의 수정주가(StockPriceHistory.closePrice)로 수량을 정규화한다.
        // totalCost = rawQty × tradePrice (불변, 실제 투자금)
        // adjustedQty = totalCost / adjustedClosePrice
        // → 향후 평가액 = adjustedClosePrice × adjustedQty = totalCost 와 일치 (손익=0 기준선)
        // 거래일의 수정주가(StockPriceHistory)로 adjustedQty 계산
        // fallback 순서: 당일 수정주가 → 가장 최근 알려진 수정주가 → tradePrice(원주가)
        // 마지막 fallback(tradePrice)은 분할/합병 후 18배 오류를 유발할 수 있으므로 경고 로그 발생
        Map<UUID, BigDecimal> tradeDayPrices = dailyPriceMap.getOrDefault(currentDay, Map.of());
        BigDecimal adjustedClose = tradeDayPrices.get(trade.getStockItemId());
        if (adjustedClose == null) adjustedClose = lastKnownPrices.get(trade.getStockItemId());
        if (adjustedClose == null || adjustedClose.compareTo(BigDecimal.ZERO) == 0) {
          log.warn(
              "[WMA] 수정주가 없음 - stockItemId={}, tradeDate={}, tradePrice={}. adjustedQty 계산에 원주가를 사용합니다. 액면분할/합병이 있었다면 평가액이 부정확할 수 있습니다.",
              trade.getStockItemId(),
              trade.getTradeDate(),
              tradePrice);
          adjustedClose = tradePrice;
        }

        if (trade.getType() == TradeType.BUY) {
          if (q > 0) {
            BigDecimal adjustedQty = resolveEvaluationQuantity(q, tradePrice, adjustedClose);
            state.setQuantity(state.getQuantity().add(adjustedQty));
            state.setRawQuantity(state.getRawQuantity() + q);
            state.setTotalCost(state.getTotalCost().add(amount));
            state.setTotalCostNet(state.getTotalCostNet().add(amount).add(fee));
            dailyTradeCount++;
            dailyBuyCount++;
          }
        } else if (trade.getType() == TradeType.SELL) {
          BigDecimal realProfit = nz(trade.getRealizedProfit());
          BigDecimal tradeSellAmount = tradePrice.multiply(BigDecimal.valueOf(q));

          // Deduce COGS from DB Profit for consistent holdings
          BigDecimal sellProceeds = tradeSellAmount.subtract(fee).subtract(tax);
          BigDecimal cogs = sellProceeds.subtract(realProfit);

          if (state.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal adjustedSellQty = resolveEvaluationQuantity(q, tradePrice, adjustedClose);
            if (state.getQuantity().compareTo(adjustedSellQty) >= 0) {
              state.setQuantity(state.getQuantity().subtract(adjustedSellQty));
              state.setTotalCost(state.getTotalCost().subtract(cogs));
            } else {
              state.setQuantity(BigDecimal.ZERO);
              state.setTotalCost(BigDecimal.ZERO);
            }

            state.setRawQuantity(state.getRawQuantity() - q);
            // rawQuantity가 0 이하이면 전량 매도: adjustedQty 반올림 오차 강제 제거
            if (state.getRawQuantity() <= 0) {
              state.setQuantity(BigDecimal.ZERO);
              state.setTotalCost(BigDecimal.ZERO);
              state.setRawQuantity(0);
            } else if (state.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
              state.setTotalCost(BigDecimal.ZERO);
            }
          }

          dailyRealizedGain = dailyRealizedGain.add(realProfit);

          dailyTradeCount++;
          dailyVolume += q;
        }

        nextTrade = it.hasNext() ? it.next() : null;
      }

      // 배당 처리 logic
      while (nextDividend != null) {
        LocalDate payDay = toLocalDate(nextDividend.getPayDate(), zoneId);
        if (payDay.isAfter(currentDay)) {
          break;
        }
        BigDecimal gross = nz(nextDividend.getGrossAmount());
        BigDecimal fee = nz(nextDividend.getFee());
        BigDecimal tax = nz(nextDividend.getTax());
        BigDecimal netDiv = gross.subtract(fee).subtract(tax);
        globalCumulativeDividend = globalCumulativeDividend.add(netDiv);

        nextDividend = divIt.hasNext() ? divIt.next() : null;
      }

      // 하루 마감 -> Global Cumulative Update
      globalCumulativeRealized = globalCumulativeRealized.add(dailyRealizedGain);

      // lastKnownPrices는 outputStart 여부와 무관하게 항상 업데이트
      // (주말/공휴일 거래, 스냅샷 복원 직후 첫 거래에서 adjustedClose fallback 방지)
      Map<UUID, BigDecimal> dayPricesForLastKnown =
          dailyPriceMap.getOrDefault(currentDay, Map.of());
      lastKnownPrices.putAll(dayPricesForLastKnown);

      // 출력 범위 내인지 확인 후 추가
      if (!currentDay.isBefore(outputStart)) {
        // Calculate Holdings Value
        BigDecimal totalHoldingsValue = BigDecimal.ZERO;
        BigDecimal totalHoldingsCost = BigDecimal.ZERO;

        for (WmaState state : stateMap.values()) {
          if (state.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            totalHoldingsCost = totalHoldingsCost.add(state.getTotalCost());

            BigDecimal price = lastKnownPrices.get(state.getStockItemId());
            if (price == null) price = BigDecimal.ZERO;

            // quantity는 수정주가 기준 환산 수량이므로 수정주가 × 환산수량 = 올바른 평가액
            BigDecimal value = price.multiply(state.getQuantity());
            totalHoldingsValue = totalHoldingsValue.add(value);
          }
        }

        BigDecimal cumulativeTotalProfit =
            globalCumulativeRealized.add(totalHoldingsValue.subtract(totalHoldingsCost));

        series.add(
            new TradeProfitTimeSeriesPoint(
                currentDay.atStartOfDay(zoneId).toInstant(),
                globalCumulativeRealized,
                dailyRealizedGain,
                dailyTradeCount,
                dailyBuyCount,
                dailyVolume,
                totalHoldingsValue,
                totalHoldingsCost,
                cumulativeTotalProfit,
                globalCumulativeDividend));
        // 보유 스냅샷 조회용 캡처: 요청된 날짜의 보유 상태를 그 시점 그대로 복사해 둔다.
        // (DB 캐시 대신 이 캡처를 쓰므로 시계열과 스냅샷이 어긋날 수 없다.)
        if (captureDates != null && capturedStates != null && captureDates.contains(currentDay)) {
          Map<String, WmaState> copied = new HashMap<>();
          stateMap.forEach((stateKey, stateValue) -> copied.put(stateKey, stateValue.copy()));
          capturedStates.put(currentDay, copied);
        }
      }

      currentDay = currentDay.plusDays(1);
    }

    // 다운샘플은 호출부에서 수행한다(요약은 일별 시리즈로 계산해야 정확하므로).
    return series;
  }

  /** 일별 시리즈로부터 기간 요약(성장률/TWR/손익 분해)을 계산한다. */
  private TradeProfitTimeSeriesSummary summarizeSeries(List<TradeProfitTimeSeriesPoint> series) {
    if (series == null || series.isEmpty()) {
      return TradeProfitTimeSeriesSummary.empty();
    }

    TradeProfitTimeSeriesPoint firstPoint = null;
    TradeProfitTimeSeriesPoint lastPoint = null;
    TradeProfitTimeSeriesPoint previousPoint = null;
    // TWR(시간가중수익률): 일별로 입출금(원금 변동)을 제거한 수익률을 곱해 누적한다.
    // 평가액 성장률은 입금까지 성과로 잡히므로, 순수 운용 성과는 이 값으로 본다.
    double timeWeightedFactor = 1.0d;
    // MDD(최대 낙폭)는 위 TWR 지수(=입출금 제거한 기준가) 위에서 구한다.
    // 평가액으로 구하면 입금으로 값이 뛰어 낙폭이 실제보다 작게 나온다.
    ZoneId zoneId = ZoneId.systemDefault();
    double peakFactor = 1.0d;
    double maxDrawdown = 0.0d;
    LocalDate runningPeakDate = null;
    LocalDate drawdownPeakDate = null;
    LocalDate drawdownTroughDate = null;

    for (TradeProfitTimeSeriesPoint point : series) {
      if (point == null || point.timestamp() == null) {
        continue;
      }
      LocalDate pointDate = point.timestamp().atZone(zoneId).toLocalDate();
      if (firstPoint == null) {
        firstPoint = point;
        runningPeakDate = pointDate;
      }
      lastPoint = point;

      if (previousPoint != null) {
        BigDecimal previousValue = nz(previousPoint.totalHoldingsValue());
        if (previousValue.compareTo(BigDecimal.ZERO) > 0) {
          // 당일 순수 손익 = (평가액 증가 - 원금 유입) + 실현손익 증가 + 배당 증가
          BigDecimal cashFlow =
              nz(point.totalHoldingsCost()).subtract(nz(previousPoint.totalHoldingsCost()));
          BigDecimal realizedGain =
              nz(point.cumulativeRealizedProfit())
                  .subtract(nz(previousPoint.cumulativeRealizedProfit()));
          BigDecimal dividendGain =
              nz(point.cumulativeDividend()).subtract(nz(previousPoint.cumulativeDividend()));
          BigDecimal dailyGain =
              nz(point.totalHoldingsValue())
                  .subtract(previousValue)
                  .subtract(cashFlow)
                  .add(realizedGain)
                  .add(dividendGain);
          timeWeightedFactor *=
              1.0d + dailyGain.divide(previousValue, 10, RoundingMode.HALF_UP).doubleValue();

          if (timeWeightedFactor > peakFactor) {
            peakFactor = timeWeightedFactor;
            runningPeakDate = pointDate;
          } else if (peakFactor > 0.0d) {
            double drawdown = (timeWeightedFactor - peakFactor) / peakFactor;
            if (drawdown < maxDrawdown) {
              maxDrawdown = drawdown;
              drawdownPeakDate = runningPeakDate;
              drawdownTroughDate = pointDate;
            }
          }
        }
      }
      previousPoint = point;
    }

    if (firstPoint == null || lastPoint == null) {
      return TradeProfitTimeSeriesSummary.empty();
    }

    BigDecimal openingValue = nz(firstPoint.totalHoldingsValue());
    BigDecimal closingValue = nz(lastPoint.totalHoldingsValue());
    Double growthRatePct =
        openingValue.compareTo(BigDecimal.ZERO) > 0
            ? closingValue
                .subtract(openingValue)
                .multiply(BigDecimal.valueOf(100))
                .divide(openingValue, 6, RoundingMode.HALF_UP)
                .doubleValue()
            : null;

    // 기간 총 손익 = 누적손익(미실현 + 실현 + 배당)의 기말 - 기초
    BigDecimal periodProfit = accumulatedProfit(lastPoint).subtract(accumulatedProfit(firstPoint));
    BigDecimal principalDelta =
        nz(lastPoint.totalHoldingsCost()).subtract(nz(firstPoint.totalHoldingsCost()));
    // 기간 손익이 "손실 회복분"인지 "순수 이익"인지 구분되도록 평가손익 기초/기말도 함께 준다.
    BigDecimal unrealizedStart = openingValue.subtract(nz(firstPoint.totalHoldingsCost()));
    BigDecimal unrealizedEnd = closingValue.subtract(nz(lastPoint.totalHoldingsCost()));
    BigDecimal endCost = nz(lastPoint.totalHoldingsCost());
    Double unrealizedEndPct =
        endCost.compareTo(BigDecimal.ZERO) > 0
            ? unrealizedEnd
                .multiply(BigDecimal.valueOf(100))
                .divide(endCost, 4, RoundingMode.HALF_UP)
                .doubleValue()
            : null;

    // 기간 손익 = 손실 회복분 + 순증분 (순증분은 잔차로 구해 합이 항상 맞게 한다)
    BigDecimal recoveredAmount =
        unrealizedStart
            .min(BigDecimal.ZERO)
            .negate()
            .subtract(unrealizedEnd.min(BigDecimal.ZERO).negate());
    BigDecimal netNewProfit = periodProfit.subtract(recoveredAmount);

    return new TradeProfitTimeSeriesSummary(
        openingValue,
        closingValue,
        growthRatePct,
        Double.isFinite(timeWeightedFactor) ? (timeWeightedFactor - 1.0d) * 100.0d : null,
        periodProfit,
        principalDelta,
        unrealizedStart,
        unrealizedEnd,
        unrealizedEndPct,
        recoveredAmount,
        netNewProfit,
        maxDrawdown < 0.0d ? maxDrawdown * 100.0d : null,
        maxDrawdown < 0.0d ? drawdownPeakDate : null,
        maxDrawdown < 0.0d ? drawdownTroughDate : null,
        peakFactor > 0.0d ? ((timeWeightedFactor - peakFactor) / peakFactor) * 100.0d : null);
  }

  /** 보유·거래·손익이 모두 0인 해(자산이 비어 있던 기간)인지. */
  private static boolean isEmptyYear(TradeProfitTimeSeriesSummary summary) {
    return summary == null
        || (nz(summary.periodProfit()).signum() == 0
            && nz(summary.principalDelta()).signum() == 0
            && nz(summary.closingValue()).signum() == 0
            && nz(summary.openingValue()).signum() == 0);
  }

  /** 시점까지 쌓인 총 손익 = 미실현(평가액 - 원금) + 누적 실현손익 + 누적 배당. */
  private static BigDecimal accumulatedProfit(TradeProfitTimeSeriesPoint point) {
    return nz(point.totalHoldingsValue())
        .subtract(nz(point.totalHoldingsCost()))
        .add(nz(point.cumulativeRealizedProfit()))
        .add(nz(point.cumulativeDividend()));
  }

  private LocalDate toLocalDate(Instant instant, ZoneId zoneId) {
    return instant.atZone(zoneId).toLocalDate();
  }

  private LocalDate toInclusiveEndDate(Instant instant, ZoneId zoneId) {
    return instant.atZone(zoneId).minusNanos(1).toLocalDate();
  }

  private BigDecimal resolveEvaluationQuantity(
      int rawQuantity, BigDecimal tradePrice, BigDecimal referencePrice) {
    BigDecimal rawQuantityValue = BigDecimal.valueOf(rawQuantity);
    if (rawQuantity <= 0
        || tradePrice == null
        || tradePrice.compareTo(BigDecimal.ZERO) <= 0
        || referencePrice == null
        || referencePrice.compareTo(BigDecimal.ZERO) <= 0) {
      return rawQuantityValue;
    }

    BigDecimal priceRatio = tradePrice.divide(referencePrice, 10, RoundingMode.HALF_UP);
    BigDecimal corporateActionFactor = detectLikelyCorporateActionFactor(priceRatio);
    return corporateActionFactor != null
        ? rawQuantityValue.multiply(corporateActionFactor)
        : rawQuantityValue;
  }

  private BigDecimal detectLikelyCorporateActionFactor(BigDecimal priceRatio) {
    if (priceRatio == null || priceRatio.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }

    if (priceRatio.compareTo(BigDecimal.ONE) >= 0) {
      BigDecimal directFactor = priceRatio.setScale(0, RoundingMode.HALF_UP);
      if (directFactor.compareTo(MIN_CORPORATE_ACTION_FACTOR) >= 0
          && isWithinCorporateActionTolerance(priceRatio, directFactor)) {
        return directFactor;
      }
    }

    BigDecimal inverseRatio = BigDecimal.ONE.divide(priceRatio, 10, RoundingMode.HALF_UP);
    BigDecimal reverseSplitBase = inverseRatio.setScale(0, RoundingMode.HALF_UP);
    if (reverseSplitBase.compareTo(MIN_CORPORATE_ACTION_FACTOR) >= 0) {
      BigDecimal reverseSplitFactor =
          BigDecimal.ONE.divide(reverseSplitBase, 10, RoundingMode.HALF_UP);
      if (isWithinCorporateActionTolerance(priceRatio, reverseSplitFactor)) {
        return reverseSplitFactor;
      }
    }

    return null;
  }

  private boolean isWithinCorporateActionTolerance(
      BigDecimal actualRatio, BigDecimal candidateFactor) {
    BigDecimal relativeDelta =
        actualRatio
            .subtract(candidateFactor)
            .abs()
            .divide(candidateFactor.abs(), 6, RoundingMode.HALF_UP);
    return relativeDelta.compareTo(CORPORATE_ACTION_FACTOR_TOLERANCE) <= 0;
  }

  private List<Trade> loadAllTrades(TradeProfitRequest request) {
    return switch (request.getRequestType()) {
      case USER -> {
        var accountList = accountService.findByUserId(request.getUserId());
        if (accountList.isEmpty()) {
          StockErrorCode.INVALID_USER_ID.throwException();
        }
        yield tradeService.findByAccountIdIn(accountList.stream().map(Account::getId).toList());
      }
      case USER_ACCOUNT -> {
        var accountList = accountService.findByIdIn(request.getAccountIdList());
        if (accountList.isEmpty()) {
          StockErrorCode.INVALID_USER_ID.throwException();
        }
        yield tradeService.findByAccountIdIn(request.getAccountIdList());
      }
      case USER_STOCKITEM -> {
        var accountList = accountService.findByUserId(request.getUserId());
        if (accountList.isEmpty()) {
          StockErrorCode.INVALID_USER_ID.throwException();
        }
        yield tradeService.findByAccountIdInAndStockItemIdIn(
            accountList.stream().map(Account::getId).toList(), request.getStockItemIdList());
      }
      case USER_ACCOUNT_STOCKITEM -> {
        var accountList = accountService.findByIdIn(request.getAccountIdList());
        if (accountList.isEmpty()) {
          StockErrorCode.INVALID_USER_ID.throwException();
        }
        yield tradeService.findByAccountIdInAndStockItemIdIn(
            request.getAccountIdList(), request.getStockItemIdList());
      }
    };
  }

  private List<TradeProfitTimeSeriesPoint> applyGranularity(
      List<TradeProfitTimeSeriesPoint> series, String granularity) {
    boolean weekly =
        "WEEKLY".equalsIgnoreCase(granularity)
            || ("AUTO".equalsIgnoreCase(granularity)
                && series.size() > 180
                && series.size() <= 730);
    boolean monthly =
        "MONTHLY".equalsIgnoreCase(granularity)
            || ("AUTO".equalsIgnoreCase(granularity) && series.size() > 730);
    if (!weekly && !monthly) {
      return series;
    }
    List<TradeProfitTimeSeriesPoint> downsampled =
        weekly ? downsampleWeekly(series) : downsampleMonthly(series);
    // 다운샘플은 각 버킷의 '마지막 거래일' 값만 남기므로, 버킷 중간(예: 주중)에 발생한 실제 일별
    // 최고/최저 평가액이 후보에서 누락된다. 구간 내 실제 최고·최저 '그 날' 포인트를 결과 시리즈에
    // 반드시 포함시켜, 차트의 최고/최저 주석이 정확한 값·날짜를 가리키고 선이 그 지점을 지나게 한다.
    return mergeHoldingsValueExtremes(series, downsampled);
  }

  /** 구간 내 실제 일별 최고/최저 평가액 포인트를 다운샘플 결과에 병합한다(누락 시에만 추가). */
  private List<TradeProfitTimeSeriesPoint> mergeHoldingsValueExtremes(
      List<TradeProfitTimeSeriesPoint> daily, List<TradeProfitTimeSeriesPoint> downsampled) {
    TradeProfitTimeSeriesPoint maxPoint = null;
    TradeProfitTimeSeriesPoint minPoint = null;
    for (TradeProfitTimeSeriesPoint p : daily) {
      if (p == null || p.timestamp() == null || p.totalHoldingsValue() == null) {
        continue;
      }
      if (maxPoint == null || p.totalHoldingsValue().compareTo(maxPoint.totalHoldingsValue()) > 0) {
        maxPoint = p;
      }
      if (minPoint == null || p.totalHoldingsValue().compareTo(minPoint.totalHoldingsValue()) < 0) {
        minPoint = p;
      }
    }
    if (maxPoint == null) {
      return downsampled;
    }
    Set<Instant> present =
        downsampled.stream()
            .map(TradeProfitTimeSeriesPoint::timestamp)
            .collect(Collectors.toCollection(HashSet::new));
    List<TradeProfitTimeSeriesPoint> result = new ArrayList<>(downsampled);
    for (TradeProfitTimeSeriesPoint extreme : List.of(maxPoint, minPoint)) {
      if (present.add(extreme.timestamp())) {
        // 순수 평가액 waypoint: 거래 카운트/일별 실현손익은 이미 인접 버킷 합계에 반영되어 있으므로
        // 0으로 두어 이중 계산을 막는다.
        result.add(
            new TradeProfitTimeSeriesPoint(
                extreme.timestamp(),
                extreme.cumulativeRealizedProfit(),
                BigDecimal.ZERO,
                0L,
                0L,
                extreme.tradeVolume(),
                extreme.totalHoldingsValue(),
                extreme.totalHoldingsCost(),
                extreme.cumulativeTotalProfit(),
                extreme.cumulativeDividend()));
      }
    }
    result.sort(Comparator.comparing(TradeProfitTimeSeriesPoint::timestamp));
    return result;
  }

  private List<TradeProfitTimeSeriesPoint> downsampleWeekly(
      List<TradeProfitTimeSeriesPoint> series) {
    // 주별로 그룹핑 후 마지막 포인트(금요일 or 마지막 거래일)를 유지하되,
    // 해당 주 전체의 tradeCount/dailyRealizedProfit을 합산하여 반영
    return series.stream()
        .collect(
            Collectors.groupingBy(
                p -> {
                  ZonedDateTime zdt = p.timestamp().atZone(ZoneId.systemDefault());
                  // ISO week: Monday=1, Sunday=7 → 해당 주 월요일 날짜를 키로 사용
                  return zdt.toLocalDate().with(java.time.temporal.WeekFields.ISO.dayOfWeek(), 1);
                }))
        .values()
        .stream()
        .map(
            week -> {
              // 마지막 포인트(기준값: 총자산, 원금 등)를 base로 사용
              TradeProfitTimeSeriesPoint last =
                  week.stream()
                      .max(Comparator.comparing(TradeProfitTimeSeriesPoint::timestamp))
                      .orElse(null);
              if (last == null) return null;
              // 주 전체 tradeCount / buyCount / dailyRealizedProfit 합산
              long weekTradeCount =
                  week.stream().mapToLong(TradeProfitTimeSeriesPoint::tradeCount).sum();
              long weekBuyCount =
                  week.stream().mapToLong(TradeProfitTimeSeriesPoint::buyCount).sum();
              BigDecimal weekDailyRealized =
                  week.stream()
                      .map(
                          p ->
                              p.dailyRealizedProfit() != null
                                  ? p.dailyRealizedProfit()
                                  : BigDecimal.ZERO)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
              return new TradeProfitTimeSeriesPoint(
                  last.timestamp(),
                  last.cumulativeRealizedProfit(),
                  weekDailyRealized,
                  weekTradeCount,
                  weekBuyCount,
                  last.tradeVolume(),
                  last.totalHoldingsValue(),
                  last.totalHoldingsCost(),
                  last.cumulativeTotalProfit(),
                  last.cumulativeDividend());
            })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(TradeProfitTimeSeriesPoint::timestamp))
        .collect(Collectors.toList());
  }

  private List<TradeProfitTimeSeriesPoint> downsampleMonthly(
      List<TradeProfitTimeSeriesPoint> series) {
    // 월별로 그룹핑 후 마지막 포인트를 유지하되 tradeCount/dailyRealizedProfit 합산
    return series.stream()
        .collect(
            Collectors.groupingBy(
                p -> YearMonth.from(p.timestamp().atZone(ZoneId.systemDefault()))))
        .values()
        .stream()
        .map(
            month -> {
              TradeProfitTimeSeriesPoint last =
                  month.stream()
                      .max(Comparator.comparing(TradeProfitTimeSeriesPoint::timestamp))
                      .orElse(null);
              if (last == null) return null;
              long monthTradeCount =
                  month.stream().mapToLong(TradeProfitTimeSeriesPoint::tradeCount).sum();
              long monthBuyCount =
                  month.stream().mapToLong(TradeProfitTimeSeriesPoint::buyCount).sum();
              BigDecimal monthDailyRealized =
                  month.stream()
                      .map(
                          p ->
                              p.dailyRealizedProfit() != null
                                  ? p.dailyRealizedProfit()
                                  : BigDecimal.ZERO)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
              return new TradeProfitTimeSeriesPoint(
                  last.timestamp(),
                  last.cumulativeRealizedProfit(),
                  monthDailyRealized,
                  monthTradeCount,
                  monthBuyCount,
                  last.tradeVolume(),
                  last.totalHoldingsValue(),
                  last.totalHoldingsCost(),
                  last.cumulativeTotalProfit(),
                  last.cumulativeDividend());
            })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(TradeProfitTimeSeriesPoint::timestamp))
        .collect(Collectors.toList());
  }

  public List<TradeResponse> getTradeHistory(TradeSearchRequest request) {
    // 1. Fetch all trades for the accounts/stockItems
    List<Trade> tradeList = null;

    List<Account> accountList = accountService.findByUserId(request.userId());
    if (accountList.isEmpty()) {
      return List.of();
    }

    List<UUID> validAccountIds = accountList.stream().map(Account::getId).toList();

    if (request.accountIdList() != null && !request.accountIdList().isEmpty()) {
      // Validate requested accounts belong to user
      if (!validAccountIds.containsAll(request.accountIdList())) {
        StockErrorCode.INVALID_USER_ID.throwException();
      }
      if (request.stockItemIdList() != null && !request.stockItemIdList().isEmpty()) {
        tradeList =
            tradeService.findByAccountIdInAndStockItemIdIn(
                request.accountIdList(), request.stockItemIdList());
      } else {
        tradeList = tradeService.findByAccountIdIn(request.accountIdList());
      }
    } else {
      // All user accounts
      if (request.stockItemIdList() != null && !request.stockItemIdList().isEmpty()) {
        tradeList =
            tradeService.findByAccountIdInAndStockItemIdIn(
                validAccountIds, request.stockItemIdList());
      } else {
        tradeList = tradeService.findByAccountIdIn(validAccountIds);
      }
    }

    if (tradeList == null || tradeList.isEmpty()) {
      return List.of();
    }

    // Map of StockItem Names
    Map<UUID, String> stockItemNames = new HashMap<>();
    stockItemService.findAll().forEach(item -> stockItemNames.put(item.getId(), item.getName()));

    List<TradeResponse> result = new ArrayList<>();

    // Group by accountId and stockItemId to calculate realized profit dynamically
    for (Trade trade : tradeList) {
      boolean inRange = true;
      if (request.startDate() != null && trade.getTradeDate().isBefore(request.startDate()))
        inRange = false;
      if (request.endDate() != null && trade.getTradeDate().isAfter(request.endDate()))
        inRange = false;

      if (inRange) {
        int q = trade.getQuantity();
        BigDecimal price = trade.getPrice() != null ? trade.getPrice() : BigDecimal.ZERO;
        BigDecimal amount = price.multiply(BigDecimal.valueOf(q));

        result.add(
            new TradeResponse(
                trade.getId(),
                trade.getAccountId(),
                trade.getStockItemId(),
                stockItemNames.getOrDefault(trade.getStockItemId(), ""),
                trade.getType(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getFee(),
                trade.getTax(),
                amount,
                trade.getType() == TradeType.SELL ? trade.getRealizedProfit() : null,
                trade.getTradeDate()));
      }
    }

    // Global sort by date descending
    result.sort(Comparator.comparing(TradeResponse::tradeDate).reversed());

    return result;
  }

  /** 특정 날짜의 보유 종목 스냅샷을 반환합니다. */
  public List<HoldingsSnapshotItem> getHoldingsSnapshot(UUID userId, LocalDate date) {
    return getHoldingsSnapshot(userId, date, null);
  }

  /** 보유 스냅샷. accountId 가 있으면 해당 계좌, 없으면 사용자 전체 기준. */
  public List<HoldingsSnapshotItem> getHoldingsSnapshot(
      UUID userId, LocalDate date, UUID accountId) {
    if (date == null) {
      return List.of();
    }
    return getHoldingsSnapshotBatch(userId, List.of(date), accountId).getOrDefault(date, List.of());
  }

  /**
   * 여러 날짜의 보유 스냅샷을 한 번의 시뮬레이션으로 계산한다.
   *
   * <p>예전에는 DailyAccountSnapshot 캐시를 읽었는데, 그 캐시는 한번 쓰이면 갱신되지 않아 거래 추가/정정이나 수정주가 반영 후 시계열 값과 어긋났다(실측
   * 62일 중 3일 불일치). 이제 시계열과 같은 시뮬레이션에서 해당 날짜 상태를 캡처하므로 두 값이 어긋날 수 없다. 날짜가 여러 개여도 시뮬레이션은 한 번만 돈다.
   */
  public Map<LocalDate, List<HoldingsSnapshotItem>> getHoldingsSnapshotBatch(
      UUID userId, List<LocalDate> dates, UUID accountId) {
    Map<LocalDate, List<HoldingsSnapshotItem>> result = new LinkedHashMap<>();
    if (userId == null || dates == null || dates.isEmpty()) {
      return result;
    }

    TreeSet<LocalDate> targetDates = new TreeSet<>();
    for (LocalDate date : dates) {
      if (date != null) {
        targetDates.add(date);
      }
    }
    if (targetDates.isEmpty()) {
      return result;
    }

    ZoneId zoneId = ZoneId.systemDefault();
    TradeProfitRequest request = new TradeProfitRequest();
    request.setUserId(userId);
    if (accountId != null) {
      request.setAccountIdList(List.of(accountId));
    }
    request.setStartDate(targetDates.first().atStartOfDay(zoneId).toInstant());
    request.setEndDate(targetDates.last().plusDays(1).atStartOfDay(zoneId).toInstant());

    Map<LocalDate, Map<String, WmaState>> capturedStates = new HashMap<>();
    try {
      simulateDailySeries(request, targetDates, capturedStates);
    } catch (Exception ex) {
      log.warn("Failed to build holdings snapshot: userId={}, dates={}", userId, targetDates, ex);
      return result;
    }

    for (LocalDate date : targetDates) {
      result.put(date, buildHoldingsSnapshotItems(capturedStates.get(date), date));
    }
    return result;
  }

  /** 캡처된 보유 상태(WmaState)를 화면용 항목으로 변환한다. */
  private List<HoldingsSnapshotItem> buildHoldingsSnapshotItems(
      Map<String, WmaState> stateMap, LocalDate date) {
    if (stateMap == null || stateMap.isEmpty()) {
      return List.of();
    }

    List<HoldingsSnapshotItem> result = new ArrayList<>();
    for (WmaState state : stateMap.values()) {
      if (state.getQuantity().compareTo(BigDecimal.ZERO) <= 0 || state.getStockItemId() == null)
        continue;

      String name = state.getStockItemId().toString();
      String symbol = null;
      var itemOpt = stockItemService.findById(state.getStockItemId());
      if (itemOpt.isPresent()) {
        var item = itemOpt.get();
        name = item.getName() != null ? item.getName() : name;
        symbol = item.getSymbol();
      }

      // 표시용 수량: rawQuantity(정수)가 있으면 사용, 없으면 반올림 처리
      long displayQty =
          state.getRawQuantity() > 0
              ? state.getRawQuantity()
              : state.getQuantity().setScale(0, RoundingMode.HALF_UP).longValue();
      BigDecimal displayQtyBd = BigDecimal.valueOf(displayQty);

      BigDecimal avgCost =
          displayQty > 0
              ? state.getTotalCost().divide(displayQtyBd, 2, RoundingMode.HALF_UP)
              : BigDecimal.ZERO;
      BigDecimal price = stockPriceService.getPriceAt(state.getStockItemId(), date);
      BigDecimal value = price.multiply(state.getQuantity());
      BigDecimal unrealizedProfit = value.subtract(state.getTotalCost());

      result.add(
          new HoldingsSnapshotItem(
              state.getStockItemId(),
              name,
              symbol,
              displayQtyBd,
              avgCost,
              price,
              value,
              unrealizedProfit));
    }

    result.sort(Comparator.comparing(HoldingsSnapshotItem::unrealizedProfit).reversed());
    return result;
  }
}
