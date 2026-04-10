package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.DailyAccountSnapshot;
import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.repository.DailyAccountSnapshotRepository;
import net.luversof.api.stock.service.strategy.ProfitCalculator;
import net.luversof.api.stock.web.dto.request.DividendSearchRequest;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.request.TradeProfitRequestType;
import net.luversof.api.stock.web.dto.request.TradeSearchRequest;
import net.luversof.api.stock.web.dto.response.HoldingsSnapshotItem;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.api.stock.web.dto.response.TradeResponse;

/** 통합 주식 손익 계산 서비스 실현손익(매매손익)과 미실현손익(보유손익)을 하나의 객체로 제공 */
@Service
public class TradeProfitService {

  private static final Logger log = LoggerFactory.getLogger(TradeProfitService.class);

  private final AccountService accountService;
  private final TradeService tradeService;
  private final StockPriceService stockPriceService;
  private final ProfitCalculator profitCalculator;
  private final StockItemService stockItemService;
  private final DividendService dividendService;
  private final DailyAccountSnapshotRepository dailyAccountSnapshotRepository;

  public TradeProfitService(
      AccountService accountService,
      TradeService tradeService,
      StockPriceService stockPriceService,
      ProfitCalculator profitCalculator,
      StockItemService stockItemService,
      DividendService dividendService,
      DailyAccountSnapshotRepository dailyAccountSnapshotRepository) {
    this.accountService = accountService;
    this.tradeService = tradeService;
    this.stockPriceService = stockPriceService;
    this.profitCalculator = profitCalculator;
    this.stockItemService = stockItemService;
    this.dividendService = dividendService;
    this.dailyAccountSnapshotRepository = dailyAccountSnapshotRepository;
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
    /** 수정주가 기준으로 환산된 보유 수량 (分割/합병 등 이벤트 반영) */
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
  }

  public List<TradeProfitTimeSeriesPoint> aggregateTimeSeries(
      TradeProfitRequest request, String granularity) {
    Instant end = request.getEndDate() != null ? request.getEndDate() : Instant.now();
    Instant start = request.getStartDate();

    // 1) 전체 트레이드 조회 (날짜 제한 없이 전체 로딩)
    List<Trade> allTrades = loadAllTrades(request);

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
    List<Dividend> allDividends = dividendService.findDividends(dividendRequest);
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
    // 시작일: 데이터가 있는 첫 날짜부터 시작 (Cost Basis 구축을 위해)
    Instant firstTradeDate = allTrades.get(0).getTradeDate().truncatedTo(ChronoUnit.DAYS);
    Instant simulationStart = firstTradeDate;
    // 출력 시작일: 요청상 start 날짜 (없으면 첫 거래일)
    Instant outputStart = start != null ? start.truncatedTo(ChronoUnit.DAYS) : firstTradeDate;
    // ---- Cache Read Logic ----
    boolean isReadUserRequest =
        (request.getRequestType() == TradeProfitRequestType.USER && request.getUserId() != null);
    boolean isReadSingleAccountRequest =
        (request.getRequestType() == TradeProfitRequestType.USER_ACCOUNT
            && request.getAccountIdList() != null
            && request.getAccountIdList().size() == 1);
    boolean shouldReadCache = isReadUserRequest || isReadSingleAccountRequest;
    if (shouldReadCache) {
      LocalDate targetDate = LocalDate.ofInstant(outputStart, ZoneId.systemDefault());
      DailyAccountSnapshot snap = null;
      if (isReadUserRequest) {
        snap =
            dailyAccountSnapshotRepository.findTopByUserIdAndDateLessThanOrderByDateDesc(
                request.getUserId(), targetDate);
      } else {
        snap =
            dailyAccountSnapshotRepository.findTopByAccountIdAndDateLessThanOrderByDateDesc(
                request.getAccountIdList().get(0), targetDate);
      }
      if (snap != null) {
        simulationStart = snap.getDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        globalCumulativeRealized =
            snap.getCumulativeRealizedProfit() != null
                ? snap.getCumulativeRealizedProfit()
                : BigDecimal.ZERO;
        globalCumulativeDividend =
            snap.getCumulativeDividend() != null ? snap.getCumulativeDividend() : BigDecimal.ZERO;
        if (snap.getWmaState() != null && !snap.getWmaState().isEmpty()) {
          try {
            TypeReference<HashMap<String, WmaState>> typeRef =
                new TypeReference<HashMap<String, WmaState>>() {};
            stateMap = new ObjectMapper().convertValue(snap.getWmaState(), typeRef);
          } catch (Exception ex) {
            log.error("Failed to deserialize WmaState", ex);
          }
        }
        // Remove trades properly BEFORE simulationStart
        final Instant finalSimStart = simulationStart;
        allTrades.removeIf(
            t -> t.getTradeDate().truncatedTo(ChronoUnit.DAYS).isBefore(finalSimStart));
        // Advance dividend iterator past simulationStart:
        // dividends before simulationStart are already counted in the snapshot's
        // cumulativeDividend
        while (nextDividend != null
            && nextDividend.getPayDate().truncatedTo(ChronoUnit.DAYS).isBefore(finalSimStart)) {
          nextDividend = divIt.hasNext() ? divIt.next() : null;
        }
      }
    }
    // ---------------------------
    Instant outputEnd = end.truncatedTo(ChronoUnit.DAYS);

    // ---- Bulk Load Existing Snapshot Dates ----
    Set<LocalDate> existingSnapshotDates = new HashSet<>();
    if (shouldReadCache) {
      try {
        LocalDate fetchStartLocalDate =
            LocalDate.ofInstant(
                simulationStart.isBefore(outputStart) ? simulationStart : outputStart,
                ZoneId.systemDefault());
        LocalDate fetchEndLocalDate = LocalDate.ofInstant(outputEnd, ZoneId.systemDefault());

        if (isReadUserRequest) {
          existingSnapshotDates.addAll(
              dailyAccountSnapshotRepository.findDatesByUserIdAndAccountIdIsNullAndDateBetween(
                  request.getUserId(), fetchStartLocalDate, fetchEndLocalDate));
        } else if (isReadSingleAccountRequest) {
          existingSnapshotDates.addAll(
              dailyAccountSnapshotRepository.findDatesByAccountIdAndDateBetween(
                  request.getAccountIdList().get(0), fetchStartLocalDate, fetchEndLocalDate));
        }
      } catch (Exception e) {
        log.warn("Failed to load existing snapshot dates", e);
      }
    }
    // -------------------------------------------

    // Price History (Bulk Load)
    Instant fetchStart = simulationStart.isBefore(outputStart) ? simulationStart : outputStart;
    LocalDate startLocalDate = LocalDate.ofInstant(fetchStart, ZoneId.systemDefault());
    LocalDate endLocalDate = LocalDate.ofInstant(outputEnd, ZoneId.systemDefault());
    List<StockPriceHistory> priceHistory =
        stockPriceService.getPriceHistory(stockItemIds, startLocalDate, endLocalDate);

    Map<Instant, Map<UUID, BigDecimal>> dailyPriceMap = new HashMap<>();
    for (StockPriceHistory h : priceHistory) {
      Instant historyInstant =
          h.getTradeDate()
              .atStartOfDay(ZoneId.systemDefault())
              .toInstant()
              .truncatedTo(ChronoUnit.DAYS);
      dailyPriceMap
          .computeIfAbsent(historyInstant, k -> new HashMap<>())
          .put(h.getStockItemId(), h.getClosePrice());
    }
    Map<UUID, BigDecimal> lastKnownPrices = new HashMap<>();

    List<TradeProfitTimeSeriesPoint> series = new ArrayList<>();
    Iterator<Trade> it = allTrades.iterator();
    Trade nextTrade = it.hasNext() ? it.next() : null;

    Instant currentDay = simulationStart.isBefore(outputStart) ? simulationStart : outputStart;
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
      long dailyVolume = 0;
      BigDecimal dailyRealizedGain = BigDecimal.ZERO;

      // nextTrade가 currentDay의 끝(inclusive)까지인지 확인
      // tradeDate는 시분초 포함이므로, truncatedTo(DAYS) 결과가 currentDay와 같거나 이전이면 처리
      while (nextTrade != null) {
        Instant tradeDay = nextTrade.getTradeDate().truncatedTo(ChronoUnit.DAYS);
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
            // 수정주가 기준 환산 수량: amount(투자금) / 당일 수정주가
            // adjustedClose가 0이면 rawQty를 그대로 사용 (평가액 계산 불가 상황)
            BigDecimal adjustedQty =
                (adjustedClose == null || adjustedClose.compareTo(BigDecimal.ZERO) == 0)
                    ? BigDecimal.valueOf(q)
                    : amount.divide(adjustedClose, 10, java.math.RoundingMode.HALF_UP);
            state.setQuantity(state.getQuantity().add(adjustedQty));
            state.setRawQuantity(state.getRawQuantity() + q);
            state.setTotalCost(state.getTotalCost().add(amount));
            state.setTotalCostNet(state.getTotalCostNet().add(amount).add(fee));
          }
        } else if (trade.getType() == TradeType.SELL) {
          BigDecimal realProfit = nz(trade.getRealizedProfit());
          BigDecimal tradeSellAmount = tradePrice.multiply(BigDecimal.valueOf(q));

          // Deduce COGS from DB Profit for consistent holdings
          BigDecimal sellProceeds = tradeSellAmount.subtract(fee).subtract(tax);
          BigDecimal cogs = sellProceeds.subtract(realProfit);

          if (state.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            // 매도 수량도 수정주가 기준으로 환산
            BigDecimal adjustedSellQty =
                (adjustedClose == null || adjustedClose.compareTo(BigDecimal.ZERO) == 0)
                    ? BigDecimal.valueOf(q)
                    : amount.divide(adjustedClose, 10, RoundingMode.HALF_UP);
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
        Instant payDay = nextDividend.getPayDate().truncatedTo(ChronoUnit.DAYS);
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
                currentDay,
                globalCumulativeRealized,
                dailyRealizedGain,
                dailyTradeCount,
                dailyVolume,
                totalHoldingsValue,
                totalHoldingsCost,
                cumulativeTotalProfit,
                globalCumulativeDividend));
        // 스냅샷 저장이 가능한 요청(USER 전체, 또는 단일 계좌)인 경우
        boolean isUserRequest =
            (request.getRequestType() == TradeProfitRequestType.USER
                && request.getUserId() != null);
        boolean isSingleAccountRequest =
            (request.getRequestType() == TradeProfitRequestType.USER_ACCOUNT
                && request.getAccountIdList() != null
                && request.getAccountIdList().size() == 1);

        if (isUserRequest || isSingleAccountRequest) {
          LocalDate snapDate = LocalDate.ofInstant(currentDay, ZoneId.systemDefault());
          saveDailySnapshotIfNeeded(
              request,
              snapDate,
              isSingleAccountRequest,
              existingSnapshotDates,
              totalHoldingsCost,
              totalHoldingsValue,
              globalCumulativeRealized,
              globalCumulativeDividend,
              stateMap);
        }
      }

      currentDay = currentDay.plus(1, ChronoUnit.DAYS);
    }

    // Apply Granularity filtering if requested, dynamically sample for large ranges
    return applyGranularity(series, granularity);
  }

  private void saveDailySnapshotIfNeeded(
      TradeProfitRequest request,
      LocalDate snapDate,
      boolean isSingleAccountRequest,
      Set<LocalDate> existingSnapshotDates,
      BigDecimal totalHoldingsCost,
      BigDecimal totalHoldingsValue,
      BigDecimal globalCumulativeRealized,
      BigDecimal globalCumulativeDividend,
      Map<String, WmaState> stateMap) {
    if (existingSnapshotDates.contains(snapDate)) {
      return;
    }
    try {
      Map<String, Object> wmaStateMap =
          new ObjectMapper().convertValue(stateMap, new TypeReference<Map<String, Object>>() {});

      DailyAccountSnapshot snap = new DailyAccountSnapshot();
      snap.setUserId(request.getUserId());
      if (isSingleAccountRequest) {
        snap.setAccountId(request.getAccountIdList().get(0));
      }
      snap.setDate(snapDate);
      snap.setTotalCost(totalHoldingsCost);
      snap.setTotalValue(totalHoldingsValue);
      snap.setCumulativeRealizedProfit(globalCumulativeRealized);
      snap.setCumulativeDividend(globalCumulativeDividend);
      snap.setWmaState(wmaStateMap);
      snap.setCreatedDate(java.time.Instant.now());

      dailyAccountSnapshotRepository.save(snap);
    } catch (Exception e) {
      log.warn("Failed to save DailyAccountSnapshot", e);
    }
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
    if ("WEEKLY".equalsIgnoreCase(granularity)
        || ("AUTO".equalsIgnoreCase(granularity) && series.size() > 180 && series.size() <= 730)) {
      // keep 1 point per week
      return series.stream()
          .filter(
              p -> p.timestamp().atZone(ZoneId.systemDefault()).getDayOfWeek() == DayOfWeek.FRIDAY)
          .collect(Collectors.toList());
    } else if ("MONTHLY".equalsIgnoreCase(granularity)
        || ("AUTO".equalsIgnoreCase(granularity) && series.size() > 730)) {
      // keep 1 point per month (last day)
      return series.stream()
          .collect(
              Collectors.groupingBy(
                  p -> YearMonth.from(p.timestamp().atZone(ZoneId.systemDefault()))))
          .values()
          .stream()
          .map(
              list ->
                  list.stream()
                      .max(Comparator.comparing(TradeProfitTimeSeriesPoint::timestamp))
                      .orElse(null))
          .filter(Objects::nonNull)
          .sorted(Comparator.comparing(TradeProfitTimeSeriesPoint::timestamp))
          .collect(Collectors.toList());
    }
    return series;
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

  /** 특정 날짜의 보유 종목 스냅샷을 반환합니다. DailyAccountSnapshot의 wmaState를 활용합니다. */
  public List<HoldingsSnapshotItem> getHoldingsSnapshot(UUID userId, LocalDate date) {
    var snapshot =
        dailyAccountSnapshotRepository.findLatestByUserIdAndAccountIdIsNullOnOrBefore(userId, date);
    if (snapshot.isEmpty() || snapshot.get().getWmaState() == null) {
      return List.of();
    }

    Map<String, WmaState> stateMap;
    try {
      TypeReference<HashMap<String, WmaState>> typeRef = new TypeReference<>() {};
      stateMap = new ObjectMapper().convertValue(snapshot.get().getWmaState(), typeRef);
    } catch (Exception ex) {
      log.error("Failed to deserialize WmaState for holdingsSnapshot", ex);
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

      // 표시용 수량: rawQuantity(정수)가 있으면 사용, 없으면(구버전 스냅샷)은 반올림 처리
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
              name, symbol, displayQtyBd, avgCost, price, value, unrealizedProfit));
    }

    result.sort(Comparator.comparing(HoldingsSnapshotItem::unrealizedProfit).reversed());
    return result;
  }
}
