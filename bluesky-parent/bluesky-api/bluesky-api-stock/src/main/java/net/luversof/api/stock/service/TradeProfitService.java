package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.service.strategy.ProfitCalculator;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.request.TradeSearchRequest;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.api.stock.web.dto.response.TradeResponse;

/** 통합 주식 손익 계산 서비스 실현손익(매매손익)과 미실현손익(보유손익)을 하나의 객체로 제공 */
@Service
public class TradeProfitService {

    private static final Logger log = LoggerFactory.getLogger(TradeProfitService.class);

    @Autowired private AccountService accountService;

    @Autowired private TradeService tradeService;

    @Autowired private StockPriceService stockPriceService;

    @Autowired private ProfitCalculator profitCalculator;

    @Autowired private StockItemService stockItemService;

    @Autowired private DividendService dividendService;

    @Autowired
    private net.luversof.api.stock.repository.DailyAccountSnapshotRepository
            dailyAccountSnapshotRepository;

    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    public void setTradeService(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    public void setStockPriceService(StockPriceService stockPriceService) {
        this.stockPriceService = stockPriceService;
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
                        yield tradeService.findByAccountIdIn(
                                accountList.stream().map(Account::getId).toList());
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
                                accountList.stream().map(Account::getId).toList(),
                                request.getStockItemIdList());
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
                    case ACCOUNT_AND_STOCKITEM ->
                            calculateProfitByAccountAndStock(tradeList, request);
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
                        .collect(
                                Collectors.groupingBy(
                                        t -> t.getAccountId() + "-" + t.getStockItemId()));
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
        var stockItemIds =
                tradeList.stream().map(Trade::getStockItemId).collect(Collectors.toSet());
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
                                                if (si.getSymbol() != null
                                                        && !si.getSymbol().isBlank()) {
                                                    return "S:" + si.getSymbol(); // Symbol Prefix
                                                }
                                                // 2026-01-17: Name match fallback for inconsistent
                                                // data
                                                // Remove spaces to ensure better matching (e.g.
                                                // "Samsung Electronics" vs
                                                // "SamsungElectronics")
                                                // But risking collision? TIGER REITs name is
                                                // specific enough.
                                                if (si.getName() != null
                                                        && !si.getName().isBlank()) {
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
        public long quantity = 0;
        public BigDecimal totalCost = BigDecimal.ZERO;
        public BigDecimal totalCostNet = BigDecimal.ZERO;
        public UUID stockItemId;
    }

    public List<TradeProfitTimeSeriesPoint> aggregateTimeSeries(
            TradeProfitRequest request, String granularity) {
        Instant end = request.getEndDate() != null ? request.getEndDate() : Instant.now();
        Instant start = request.getStartDate();

        // 1) 전체 트레이드 조회 (날짜 제한 없이 전체 로딩)
        List<Trade> allTrades =
                switch (request.getRequestType()) {
                    case USER -> {
                        var accountList = accountService.findByUserId(request.getUserId());
                        if (accountList.isEmpty()) {
                            StockErrorCode.INVALID_USER_ID.throwException();
                        }
                        yield tradeService.findByAccountIdIn(
                                accountList.stream().map(Account::getId).toList());
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
                                accountList.stream().map(Account::getId).toList(),
                                request.getStockItemIdList());
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

        if (allTrades.isEmpty()) {
            return new ArrayList<>();
        } // 2) StockItem 정보 로딩 및 그룹핑 키 생성 (calculateProfitByStock과 동일 로직)
        var stockItemIds =
                allTrades.stream().map(Trade::getStockItemId).collect(Collectors.toSet());
        Map<UUID, StockItem> stockItemMap = new HashMap<>();
        stockItemService.findAllById(stockItemIds).forEach(si -> stockItemMap.put(si.getId(), si));

        // Fetch Dividends for the user/accounts
        net.luversof.api.stock.web.dto.request.DividendSearchRequest dividendRequest =
                new net.luversof.api.stock.web.dto.request.DividendSearchRequest();
        dividendRequest.setUserId(request.getUserId());
        dividendRequest.setAccountIdList(request.getAccountIdList());
        if (request.getRequestType()
                        == net.luversof.api.stock.web.dto.request.TradeProfitRequestType
                                .USER_STOCKITEM
                || request.getRequestType()
                        == net.luversof.api.stock.web.dto.request.TradeProfitRequestType
                                .USER_ACCOUNT_STOCKITEM) {
            dividendRequest.setStockItemIdList(request.getStockItemIdList());
        }
        List<net.luversof.api.stock.domain.Dividend> allDividends =
                dividendService.findDividends(dividendRequest);
        allDividends.sort(Comparator.comparing(net.luversof.api.stock.domain.Dividend::getPayDate));
        Iterator<net.luversof.api.stock.domain.Dividend> divIt = allDividends.iterator();
        net.luversof.api.stock.domain.Dividend nextDividend = divIt.hasNext() ? divIt.next() : null;

        // 그룹핑 키 생성 함수
        java.util.function.Function<Trade, String> getGroupKey =
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
                (request.getRequestType()
                                == net.luversof.api.stock.web.dto.request.TradeProfitRequestType
                                        .USER
                        && request.getUserId() != null);
        boolean isReadSingleAccountRequest =
                (request.getRequestType()
                                == net.luversof.api.stock.web.dto.request.TradeProfitRequestType
                                        .USER_ACCOUNT
                        && request.getAccountIdList() != null
                        && request.getAccountIdList().size() == 1);
        boolean shouldReadCache = isReadUserRequest || isReadSingleAccountRequest;
        if (shouldReadCache) {
            java.time.LocalDate targetDate =
                    java.time.LocalDate.ofInstant(outputStart, java.time.ZoneId.systemDefault());
            net.luversof.api.stock.domain.DailyAccountSnapshot snap = null;
            if (isReadUserRequest) {
                snap =
                        dailyAccountSnapshotRepository
                                .findTopByUserIdAndDateLessThanOrderByDateDesc(
                                        request.getUserId(), targetDate);
            } else {
                snap =
                        dailyAccountSnapshotRepository
                                .findTopByAccountIdAndDateLessThanOrderByDateDesc(
                                        request.getAccountIdList().get(0), targetDate);
            }
            if (snap != null) {
                simulationStart =
                        snap.getDate()
                                .plusDays(1)
                                .atStartOfDay(java.time.ZoneId.systemDefault())
                                .toInstant();
                globalCumulativeRealized =
                        snap.getCumulativeRealizedProfit() != null
                                ? snap.getCumulativeRealizedProfit()
                                : BigDecimal.ZERO;
                globalCumulativeDividend =
                        snap.getCumulativeDividend() != null
                                ? snap.getCumulativeDividend()
                                : BigDecimal.ZERO;
                if (snap.getWmaState() != null && !snap.getWmaState().isEmpty()) {
                    try {
                        com.fasterxml.jackson.core.type.TypeReference<HashMap<String, WmaState>>
                                typeRef =
                                        new com.fasterxml.jackson.core.type.TypeReference<
                                                HashMap<String, WmaState>>() {};
                        stateMap =
                                new com.fasterxml.jackson.databind.ObjectMapper()
                                        .convertValue(snap.getWmaState(), typeRef);
                    } catch (Exception ex) {
                        log.error("Failed to deserialize WmaState", ex);
                    }
                }
                // Remove trades properly BEFORE simulationStart
                final Instant finalSimStart = simulationStart;
                allTrades.removeIf(
                        t -> t.getTradeDate().truncatedTo(ChronoUnit.DAYS).isBefore(finalSimStart));
            }
        }
        // ---------------------------
        Instant outputEnd = end.truncatedTo(ChronoUnit.DAYS);

        // ---- Bulk Load Existing Snapshot Dates ----
        java.util.Set<java.time.LocalDate> existingSnapshotDates = new java.util.HashSet<>();
        if (shouldReadCache) {
            try {
                java.time.LocalDate fetchStartLocalDate =
                        java.time.LocalDate.ofInstant(
                                simulationStart.isBefore(outputStart)
                                        ? simulationStart
                                        : outputStart,
                                java.time.ZoneId.systemDefault());
                java.time.LocalDate fetchEndLocalDate =
                        java.time.LocalDate.ofInstant(outputEnd, java.time.ZoneId.systemDefault());

                if (isReadUserRequest) {
                    existingSnapshotDates.addAll(
                            dailyAccountSnapshotRepository
                                    .findDatesByUserIdAndAccountIdIsNullAndDateBetween(
                                            request.getUserId(),
                                            fetchStartLocalDate,
                                            fetchEndLocalDate));
                } else if (isReadSingleAccountRequest) {
                    existingSnapshotDates.addAll(
                            dailyAccountSnapshotRepository.findDatesByAccountIdAndDateBetween(
                                    request.getAccountIdList().get(0),
                                    fetchStartLocalDate,
                                    fetchEndLocalDate));
                }
            } catch (Exception e) {
                log.warn("Failed to load existing snapshot dates", e);
            }
        }
        // -------------------------------------------

        // Price History (Bulk Load)
        Instant fetchStart = simulationStart.isBefore(outputStart) ? simulationStart : outputStart;
        java.time.LocalDate startLocalDate =
                java.time.LocalDate.ofInstant(fetchStart, java.time.ZoneId.systemDefault());
        java.time.LocalDate endLocalDate =
                java.time.LocalDate.ofInstant(outputEnd, java.time.ZoneId.systemDefault());
        List<StockPriceHistory> priceHistory =
                stockPriceService.getPriceHistory(stockItemIds, startLocalDate, endLocalDate);

        Map<Instant, Map<UUID, BigDecimal>> dailyPriceMap = new HashMap<>();
        for (StockPriceHistory h : priceHistory) {
            Instant historyInstant =
                    h.getTradeDate()
                            .atStartOfDay(java.time.ZoneId.systemDefault())
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
                                    s.stockItemId = trade.getStockItemId();
                                    return s;
                                });
                if (state.stockItemId == null) state.stockItemId = trade.getStockItemId();

                BigDecimal fee = nz(trade.getFee());
                BigDecimal tax = nz(trade.getTax());
                int q = trade.getQuantity();
                BigDecimal price = trade.getPrice();
                BigDecimal amount = price.multiply(BigDecimal.valueOf(q));

                lastKnownPrices.put(trade.getStockItemId(), price);

                if (trade.getType() == TradeType.BUY) {
                    if (q > 0) {
                        state.quantity += q;
                        state.totalCost = state.totalCost.add(amount);
                        state.totalCostNet = state.totalCostNet.add(amount).add(fee);
                    }
                } else if (trade.getType() == TradeType.SELL) {
                    BigDecimal realProfit = nz(trade.getRealizedProfit());
                    BigDecimal tradeSellAmount = price.multiply(BigDecimal.valueOf(q));

                    // Deduce COGS from DB Profit for consistent holdings
                    BigDecimal sellProceeds = tradeSellAmount.subtract(fee).subtract(tax);
                    BigDecimal cogs = sellProceeds.subtract(realProfit);

                    if (state.quantity > 0) {
                        // Update State
                        if (state.quantity >= q) {
                            state.quantity -= q;
                            state.totalCost = state.totalCost.subtract(cogs);
                        } else {
                            state.quantity = 0;
                            state.totalCost = BigDecimal.ZERO;
                        }

                        if (state.quantity == 0) {
                            state.totalCost = BigDecimal.ZERO;
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

            // 출력 범위 내인지 확인 후 추가
            if (!currentDay.isBefore(outputStart)) {
                // Update Last Known Prices from History
                Map<UUID, BigDecimal> dayPrices = dailyPriceMap.getOrDefault(currentDay, Map.of());
                lastKnownPrices.putAll(dayPrices);

                // Calculate Holdings Value
                BigDecimal totalHoldingsValue = BigDecimal.ZERO;
                BigDecimal totalHoldingsCost = BigDecimal.ZERO;

                for (WmaState state : stateMap.values()) {
                    if (state.quantity > 0) {
                        totalHoldingsCost = totalHoldingsCost.add(state.totalCost);

                        BigDecimal price = lastKnownPrices.get(state.stockItemId);
                        if (price == null) price = BigDecimal.ZERO;

                        BigDecimal value = price.multiply(BigDecimal.valueOf(state.quantity));
                        totalHoldingsValue = totalHoldingsValue.add(value);
                    }
                }

                BigDecimal cumulativeTotalProfit =
                        globalCumulativeRealized.add(
                                totalHoldingsValue.subtract(totalHoldingsCost));

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
                        (request.getRequestType()
                                        == net.luversof.api.stock.web.dto.request
                                                .TradeProfitRequestType.USER
                                && request.getUserId() != null);
                boolean isSingleAccountRequest =
                        (request.getRequestType()
                                        == net.luversof.api.stock.web.dto.request
                                                .TradeProfitRequestType.USER_ACCOUNT
                                && request.getAccountIdList() != null
                                && request.getAccountIdList().size() == 1);

                if (isUserRequest || isSingleAccountRequest) {
                    try {
                        java.time.LocalDate snapDate =
                                java.time.LocalDate.ofInstant(
                                        currentDay, java.time.ZoneId.systemDefault());
                        if (!existingSnapshotDates.contains(snapDate)) {
                            java.util.Map<String, Object> wmaStateMap =
                                    new com.fasterxml.jackson.databind.ObjectMapper()
                                            .convertValue(
                                                    stateMap,
                                                    new com.fasterxml.jackson.core.type
                                                                    .TypeReference<
                                                            java.util.Map<String, Object>>() {});

                            net.luversof.api.stock.domain.DailyAccountSnapshot snap =
                                    new net.luversof.api.stock.domain.DailyAccountSnapshot();
                            snap.setUserId(request.getUserId());
                            if (isSingleAccountRequest) {
                                snap.setAccountId(request.getAccountIdList().get(0));
                            }
                            snap.setDate(
                                    java.time.LocalDate.ofInstant(
                                            currentDay, java.time.ZoneId.systemDefault()));
                            snap.setTotalCost(totalHoldingsCost);
                            snap.setTotalValue(totalHoldingsValue);
                            snap.setCumulativeRealizedProfit(globalCumulativeRealized);
                            snap.setCumulativeDividend(globalCumulativeDividend);
                            snap.setWmaState(wmaStateMap);
                            snap.setCreatedDate(java.time.Instant.now());

                            dailyAccountSnapshotRepository.save(snap);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to save DailyAccountSnapshot", e);
                    }
                }
            }

            currentDay = currentDay.plus(1, ChronoUnit.DAYS);
        }

        // Apply Granularity filtering if requested, dynamically sample for large ranges
        if ("WEEKLY".equalsIgnoreCase(granularity)
                || ("AUTO".equalsIgnoreCase(granularity)
                        && series.size() > 180
                        && series.size() <= 730)) {
            // keep 1 point per week
            series =
                    series.stream()
                            .filter(
                                    p ->
                                            p.timestamp()
                                                            .atZone(
                                                                    java.time.ZoneId
                                                                            .systemDefault())
                                                            .getDayOfWeek()
                                                    == java.time.DayOfWeek.FRIDAY)
                            .collect(Collectors.toList());
        } else if ("MONTHLY".equalsIgnoreCase(granularity)
                || ("AUTO".equalsIgnoreCase(granularity) && series.size() > 730)) {
            // keep 1 point per month (last day)
            series =
                    series.stream()
                            .collect(
                                    Collectors.groupingBy(
                                            p ->
                                                    java.time.YearMonth.from(
                                                            p.timestamp()
                                                                    .atZone(
                                                                            java.time.ZoneId
                                                                                    .systemDefault()))))
                            .values()
                            .stream()
                            .map(
                                    list ->
                                            list.stream()
                                                    .max(
                                                            Comparator.comparing(
                                                                    TradeProfitTimeSeriesPoint
                                                                            ::timestamp))
                                                    .orElse(null))
                            .filter(java.util.Objects::nonNull)
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
        stockItemService
                .findAll()
                .forEach(item -> stockItemNames.put(item.getId(), item.getName()));

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
                                trade.getType() == TradeType.SELL
                                        ? trade.getRealizedProfit()
                                        : null,
                                trade.getTradeDate()));
            }
        }

        // Global sort by date descending
        result.sort(Comparator.comparing(TradeResponse::tradeDate).reversed());

        return result;
    }
}
