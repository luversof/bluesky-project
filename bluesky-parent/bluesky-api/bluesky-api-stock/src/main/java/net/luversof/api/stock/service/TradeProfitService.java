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
import java.util.Locale;
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

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.StockDailyClosePrice;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.service.strategy.ProfitCalculator;
import net.luversof.api.stock.web.dto.request.DividendSearchRequest;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.request.TradeProfitRequestType;
import net.luversof.api.stock.web.dto.request.TradeSearchRequest;
import net.luversof.api.stock.web.dto.response.HoldingsSnapshotItem;
import net.luversof.api.stock.web.dto.response.TradeProfitPeriodSummary;
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
            // 계좌를 먼저 읽어 id 를 뽑고 다시 거래를 읽던 왕복 2회를 조인 1회로 줄인다.
            // 이 엔드포인트는 시간의 3/4 을 DB 왕복 대기로 쓰므로(실측: 소켓 대기 51.6% + 드라이버 9.1%)
            // 왕복 하나가 그대로 응답 시간이다. 거래가 하나도 없을 때만 "계좌가 아예 없는 사용자"인지
            // 확인해 예전과 같은 오류를 낸다.
            // 계좌도 거래도 없는 사용자는 오류가 아니라 '아직 아무것도 없는 사용자'다.
            // 예전에는 여기서 400 을 던져, 가입만 하고 계좌를 아직 안 만든 사용자에게 대시보드
            // 조각이 통째로 오류 상자로 나갔다(실측: 데이터 없는 userId 로 calculateProfit,
            // timeSeries, timeSeriesWithSummary, holdingsSnapshot, holdingsSnapshotBatch
            // 5 개가 모두 400 · StockErrorCode.INVALID_USER_ID). 빈 결과를 돌려주면 화면이
            // "데이터 없음" 을 그린다.
            //
            // 남의 계좌를 보려는 요청은 이 분기로 오지 않는다. accountIdList 를 준 요청은
            // USER_ACCOUNT 경로로 가고 거기서 소유권을 검사한다(그 검사는 그대로 둔다).
            yield tradeService.findByUserId(request.getUserId());
          }
          case USER_ACCOUNT -> {
            var accountList = accountService.findByIdIn(request.getAccountIdList());
            if (accountList.isEmpty()) {
              StockErrorCode.INVALID_USER_ID.throwException();
            }

            assertAccountsOwnedBy(accountList, request.getUserId());

            yield tradeService.findByAccountIdIn(request.getAccountIdList());
          }
          case USER_STOCKITEM -> {
            var accountList = accountService.findByUserId(request.getUserId());
            // 계좌가 아직 없는 사용자는 오류가 아니라 빈 결과다(위 USER 분기와 같은 이유).
            if (accountList.isEmpty()) {
              yield List.of();
            }

            assertAccountsOwnedBy(accountList, request.getUserId());

            yield tradeService.findByAccountIdInAndStockItemIdIn(
                accountList.stream().map(Account::getId).toList(), request.getStockItemIdList());
          }
          case USER_ACCOUNT_STOCKITEM -> {
            var accountList = accountService.findByIdIn(request.getAccountIdList());
            if (accountList.isEmpty()) {
              StockErrorCode.INVALID_USER_ID.throwException();
            }

            assertAccountsOwnedBy(accountList, request.getUserId());

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
  /** 현재가가 필요한 경로(기간 미지정)에서만 종목별 최신 종가를 일괄 조회한다. */
  private Map<UUID, net.luversof.api.stock.domain.StockDailyClosePrice> loadLatestPricesIfNeeded(
      List<Trade> tradeList, TradeProfitRequest request) {
    if (request.hasDateRange() || tradeList == null || tradeList.isEmpty()) {
      return Map.of();
    }
    return stockPriceService.getLatestPrices(
        tradeList.stream().map(Trade::getStockItemId).filter(java.util.Objects::nonNull).toList());
  }

  public List<TradeProfit> calculateProfitByAccountAndStock(
      List<Trade> tradeList, TradeProfitRequest request) {
    Map<String, List<Trade>> grouped =
        tradeList.stream()
            .collect(Collectors.groupingBy(t -> t.getAccountId() + "-" + t.getStockItemId()));
    List<TradeProfit> result = new ArrayList<>();

    // 기간 미지정(현재 보유 현황) 경로에서만 현재가가 필요하다. 그룹마다 1건씩 조회하면
    // 종목 수만큼 DB 왕복이 생기므로(실측: 스택 샘플의 다수가 이 조회) 한 번에 받아 둔다.
    var latestPrices = loadLatestPricesIfNeeded(tradeList, request);

    for (List<Trade> group : grouped.values()) {
      Trade first = group.get(0);
      UUID accountId = first.getAccountId();
      UUID stockItemId = first.getStockItemId();

      TradeProfit profit =
          profitCalculator.calculate(group, request, stockPriceService, latestPrices);
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
    // 이 목록은 groupingBy 가 만든 HashMap 의 values() 순서로 쌓인다. 같은 입력이면 재현되지만
    // 뜻이 없는 순서라, 계좌 하나만 늘어도 전체가 뒤섞인다. 같은 서비스의 다른 목록은 모두 정렬해서
    // 내보낸다(보유 스냅샷·시계열·거래 목록). 거래 목록에는 그 이유가 이미 적혀 있다 - 받는 쪽이
    // 페이지로 자르면 같은 행이 두 페이지에 나오거나 빠질 수 있다.
    //
    // 업무상 순위를 주장하지 않는 키(종목 -> 계좌)로만 고정한다. 화면은 각자 필요한 기준으로 다시
    // 정렬하므로(실측: 소비하는 컨트롤러 5 곳이 모두 자체 정렬한다) 표시 순서는 바뀌지 않는다.
    result.sort(
        Comparator.comparing(
                TradeProfit::getStockItemId,
                Comparator.nullsLast(Comparator.comparing(UUID::toString)))
            .thenComparing(
                TradeProfit::getAccountId,
                Comparator.nullsLast(Comparator.comparing(UUID::toString))));
    return result;
  }

  /**
   * stockItemId별 통합 손익 통계 (accountId 무시, 실현손익 + 미실현손익) StockItem Symbol이 같으면 통합하여 계산 (중복 데이터 보정)
   */
  /**
   * 한 종목의 거래를 계좌별로 계산한 뒤 하나로 합친다.
   *
   * <p>합계 항목(금액·수량·손익)은 단순 합, 평단은 수량 가중 평균이다. 전량 매도해 잔량이 0 인 계좌는 가중치 0 이라 평단에 영향을 주지 않는다 — 이것이 계좌를
   * 합쳐 한 번에 WMA 를 돌리는 것과 다른 점이다.
   */
  private TradeProfit mergeByAccount(
      List<Trade> group,
      TradeProfitRequest request,
      Map<UUID, net.luversof.api.stock.domain.StockDailyClosePrice> latestPrices) {
    Map<UUID, List<Trade>> byAccount =
        group.stream()
            .collect(
                Collectors.groupingBy(
                    Trade::getAccountId, LinkedHashMap::new, Collectors.toList()));
    if (byAccount.size() <= 1) {
      return profitCalculator.calculate(group, request, stockPriceService, latestPrices);
    }

    List<TradeProfit> parts = new ArrayList<>();
    for (List<Trade> accountTrades : byAccount.values()) {
      parts.add(
          profitCalculator.calculate(accountTrades, request, stockPriceService, latestPrices));
    }

    TradeProfit merged = new TradeProfit();
    merged.setStockItemId(group.get(0).getStockItemId());
    merged.setTotalBuyAmount(sum(parts, TradeProfit::getTotalBuyAmount));
    merged.setTotalBuyCost(sum(parts, TradeProfit::getTotalBuyCost));
    merged.setTotalBuyFee(sum(parts, TradeProfit::getTotalBuyFee));
    merged.setTotalSellAmount(sum(parts, TradeProfit::getTotalSellAmount));
    merged.setTotalSellProceeds(sum(parts, TradeProfit::getTotalSellProceeds));
    merged.setTotalSellFee(sum(parts, TradeProfit::getTotalSellFee));
    merged.setTotalSellTax(sum(parts, TradeProfit::getTotalSellTax));
    merged.setRealizedProfit(sum(parts, TradeProfit::getRealizedProfit));
    merged.setRealizedProfitNet(sum(parts, TradeProfit::getRealizedProfitNet));
    merged.setEvaluationAmount(sum(parts, TradeProfit::getEvaluationAmount));
    merged.setEvaluationProfit(sum(parts, TradeProfit::getEvaluationProfit));
    merged.setEvaluationProfitNet(sum(parts, TradeProfit::getEvaluationProfitNet));
    merged.setTotalProfit(sum(parts, TradeProfit::getTotalProfit));
    merged.setTotalProfitNet(sum(parts, TradeProfit::getTotalProfitNet));

    int holdingQuantity = 0;
    int totalSellQuantity = 0;
    for (TradeProfit part : parts) {
      holdingQuantity += part.getHoldingQuantity();
      totalSellQuantity += part.getTotalSellQuantity();
    }
    merged.setHoldingQuantity(holdingQuantity);
    merged.setTotalSellQuantity(totalSellQuantity);
    merged.setAverageBuyPrice(
        weighted(parts, TradeProfit::getAverageBuyPrice, TradeProfit::getHoldingQuantity));
    merged.setAverageBuyPriceNet(
        weighted(parts, TradeProfit::getAverageBuyPriceNet, TradeProfit::getHoldingQuantity));
    merged.setAverageSellPrice(
        weighted(parts, TradeProfit::getAverageSellPrice, TradeProfit::getTotalSellQuantity));
    merged.setAverageSellPriceNet(
        weighted(parts, TradeProfit::getAverageSellPriceNet, TradeProfit::getTotalSellQuantity));

    for (TradeProfit part : parts) {
      if (part.getCurrentPrice() != null) {
        merged.setCurrentPrice(part.getCurrentPrice());
        merged.setCurrentPriceDate(part.getCurrentPriceDate());
        break;
      }
    }
    return merged;
  }

  private static BigDecimal sum(List<TradeProfit> parts, Function<TradeProfit, BigDecimal> getter) {
    BigDecimal total = BigDecimal.ZERO;
    boolean any = false;
    for (TradeProfit part : parts) {
      BigDecimal value = getter.apply(part);
      if (value != null) {
        total = total.add(value);
        any = true;
      }
    }
    return any ? total : null;
  }

  /**
   * 수량 가중 평균. 가중치 합이 0 이면(전량 매도 등) 0 을 준다 — 계산기가 보유 0 일 때 0 을 주므로 그쪽에 맞춘다(null 을 주면 계좌별 경로와 달라져
   * 화면에서 빈칸이 된다).
   */
  private static BigDecimal weighted(
      List<TradeProfit> parts,
      Function<TradeProfit, BigDecimal> priceGetter,
      java.util.function.ToIntFunction<TradeProfit> quantityGetter) {
    BigDecimal weightedSum = BigDecimal.ZERO;
    long quantitySum = 0;
    for (TradeProfit part : parts) {
      BigDecimal price = priceGetter.apply(part);
      int quantity = quantityGetter.applyAsInt(part);
      if (price == null || quantity <= 0) {
        continue;
      }
      weightedSum = weightedSum.add(price.multiply(BigDecimal.valueOf(quantity)));
      quantitySum += quantity;
    }
    if (quantitySum == 0) {
      return BigDecimal.ZERO;
    }
    // 스케일 10 은 의도적이다. 계좌별로 이미 2자리로 반올림된 평단을 다시 평균 내므로,
    // 여기서 2자리로 또 자르면 (평단 x 수량) 합이 계좌별 합계와 원 단위로 어긋난다.
    return weightedSum.divide(BigDecimal.valueOf(quantitySum), 10, RoundingMode.HALF_UP);
  }

  public List<TradeProfit> calculateProfitByStock(
      List<Trade> tradeList, TradeProfitRequest request) {
    // 1. StockItem 정보 조회 (Symbol 기준 병합을 위해)
    var stockItemIds = tradeList.stream().map(Trade::getStockItemId).collect(Collectors.toSet());
    Map<UUID, StockItem> stockItemMap = new HashMap<>();
    stockItemService
        .findAllByIdWithoutTags(stockItemIds)
        .forEach(si -> stockItemMap.put(si.getId(), si));

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

    // 위와 같은 이유로 현재가는 한 번에 받아 둔다.
    var latestPrices = loadLatestPricesIfNeeded(tradeList, request);

    for (List<Trade> group : grouped.values()) {
      if (group.isEmpty()) continue;

      // 대표 ID 사용 (첫번째 Trade의 StockItemId)
      UUID stockItemId = group.get(0).getStockItemId();

      // 계좌를 합쳐 WMA 를 한 번 돌리면, 이미 전량 매도한 계좌의 매입가가 남아 있는 보유분의
      // 평단에 섞인다. 그러면 같은 화면의 계좌별 합계와 종목별 합계가 어긋난다
      // (실측: 잔여원가가 계좌별로 계산한 값과 0.028% 어긋났다 — 다계좌 보유 2종목에서 발생).
      // 계좌별로 계산한 뒤 합쳐야 "지금 들고 있는 수량을 실제로 얼마에 샀는지"가 된다.
      TradeProfit profit = mergeByAccount(group, request, latestPrices);
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

  private static final Set<String> SUPPORTED_GRANULARITIES =
      Set.of("DAILY", "WEEKLY", "MONTHLY", "AUTO");

  /**
   * 모르는 granularity 는 거절한다.
   *
   * <p>예전에는 인식 못 하는 값이 조용히 일별 원본으로 떨어졌다. 일별은 가장 무거운 응답이라 오타 하나가 응답 30 배·시간 2.7 배를 냈다(실측: AUTO
   * 51.6KB/29.4ms vs 오타 1,546.5KB/79.1ms). 시뮬레이션을 돌리기 전에 끊는다. 값을 아예 주지 않는 경우는 종전대로 일별이다.
   */
  private static void assertSupportedGranularity(String granularity) {
    if (granularity == null || granularity.isBlank()) {
      return;
    }
    if (!SUPPORTED_GRANULARITIES.contains(granularity.trim().toUpperCase(Locale.ROOT))) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST,
          "Unsupported granularity: " + granularity);
    }
  }

  public List<TradeProfitTimeSeriesPoint> aggregateTimeSeries(
      TradeProfitRequest request, String granularity) {
    assertSupportedGranularity(granularity);
    return applyGranularity(
        simulateDailySeries(request, null, null, null, null, null),
        granularity,
        request.resolveZoneId());
  }

  /**
   * 시리즈와 기간 요약(TWR/손익 등)을 한 번의 시뮬레이션으로 함께 계산한다.
   *
   * <p>예전에는 화면이 시리즈용과 요약용으로 각각 호출해 같은 시뮬레이션(전체 거래 이력)을 두 번 돌렸다. 요약은 다운샘플 이전의 일별 시리즈로 계산해야
   * 정확하므로(주/월봉으로 TWR을 계산하면 틀림) 여기서 함께 만들어 돌려준다.
   */
  public TradeProfitTimeSeriesResult aggregateTimeSeriesWithSummary(
      TradeProfitRequest request, String granularity) {
    return aggregateTimeSeriesWithSummary(request, granularity, null);
  }

  /**
   * @param breakdown 기간을 쪼개는 단위 &mdash; {@code AUTO} · {@code MONTH} · {@code YEAR}. 없으면 쪼개지 않는다(기존
   *     화면의 응답 크기를 그대로 둔다).
   */
  public TradeProfitTimeSeriesResult aggregateTimeSeriesWithSummary(
      TradeProfitRequest request, String granularity, String breakdown) {
    assertSupportedGranularity(granularity);
    ZoneId zoneId = request.resolveZoneId();
    List<TradeProfitTimeSeriesPoint> dailySeries =
        simulateDailySeries(request, null, null, null, null, null);
    // 시작일을 주지 않은 조회('전체' 기간)는 이력 맨 앞부터다. 이때 첫 지점은 '이전부터 있던 상태'가
    // 아니라 그날 처음 생긴 자산이므로, 기초를 0 으로 잡아야 첫날의 손익과 원금이 집계에 들어간다
    // (실측: 첫 매수일의 손익과 원금이 '전체' 합계에서 통째로 빠져 있었다.
    //  기간을 2009-01-01 처럼 명시하면 시리즈 앞에 0 인 날이 들어와 값이 맞는 것과 대비된다).
    boolean fullHistory = request.getStartDate() == null;
    List<TradeProfitPeriodSummary> periodBreakdown =
        breakdown == null || breakdown.isBlank()
            ? List.of()
            : summarizeByPeriod(
                dailySeries,
                zoneId,
                fullHistory,
                resolveBreakdownUnit(dailySeries, zoneId, breakdown));
    return new TradeProfitTimeSeriesResult(
        applyGranularity(dailySeries, granularity, zoneId),
        summarizeSeries(dailySeries, zoneId, fullHistory),
        summarizeByYear(dailySeries, zoneId, fullHistory),
        periodBreakdown);
  }

  /**
   * 연도별 성과. 각 연도 구간의 앞에 전년도 마지막 지점을 기초로 붙여 계산한다. 그래야 연초 첫날의 수익률이 빠지지 않고, 기초 평가액이 전년도 종가가 되어 연 단위
   * 비교가 맞는다.
   */
  // summarizeSeries 와 같은 이유로 static + package-private.
  static List<TradeProfitYearlySummary> summarizeByYear(
      List<TradeProfitTimeSeriesPoint> dailySeries, ZoneId zoneId, boolean fullHistory) {
    List<TradeProfitYearlySummary> result = new ArrayList<>();
    for (TradeProfitPeriodSummary period :
        summarizeByPeriod(dailySeries, zoneId, fullHistory, "YEAR")) {
      result.add(
          new TradeProfitYearlySummary(
              Integer.parseInt(period.label()),
              period.fromDate(),
              period.toDate(),
              period.complete(),
              period.summary()));
    }
    return result;
  }

  /**
   * 기간을 달 또는 해로 쪼갠 성과.
   *
   * <p>각 구간 앞에 <b>직전 구간의 마지막 지점</b>을 기초로 붙여 계산한다. 그래야 그 달/해 첫날의 손익이 빠지지 않고, 기초 평가액이 직전 구간의 종가가 되어
   * 구간끼리 견줄 수 있다.
   *
   * @param unit {@code YEAR} 또는 {@code MONTH}
   */
  static List<TradeProfitPeriodSummary> summarizeByPeriod(
      List<TradeProfitTimeSeriesPoint> dailySeries,
      ZoneId zoneId,
      boolean fullHistory,
      String unit) {
    if (dailySeries == null || dailySeries.isEmpty()) {
      return List.of();
    }
    boolean monthly = "MONTH".equalsIgnoreCase(unit);

    Map<String, List<TradeProfitTimeSeriesPoint>> byBucket = new LinkedHashMap<>();
    TradeProfitTimeSeriesPoint previousPoint = null;
    String previousKey = null;

    for (TradeProfitTimeSeriesPoint point : dailySeries) {
      if (point == null || point.timestamp() == null) {
        continue;
      }
      String key = bucketKey(pointDate(point, zoneId), monthly);
      List<TradeProfitTimeSeriesPoint> bucket = byBucket.get(key);
      if (bucket == null) {
        bucket = new ArrayList<>();
        // 구간이 바뀌는 지점: 직전 구간의 마지막 값을 기초로 삼는다.
        if (previousPoint != null && previousKey != null && !previousKey.equals(key)) {
          bucket.add(previousPoint);
        }
        byBucket.put(key, bucket);
      }
      bucket.add(point);
      previousPoint = point;
      previousKey = key;
    }

    List<TradeProfitPeriodSummary> result = new ArrayList<>();
    // 첫 구간만 앞에 붙일 직전 지점이 없어 자기 첫 지점을 기초로 쓰게 된다. 이력 맨 앞부터 보는
    // 조회라면 그 이전에는 아무것도 없었으므로 기초를 0 으로 잡는다(첫날이 빠지지 않게).
    String firstKey = byBucket.keySet().stream().findFirst().orElse(null);
    byBucket.forEach(
        (key, points) -> {
          TradeProfitTimeSeriesSummary summary =
              summarizeSeries(points, zoneId, fullHistory && key.equals(firstKey));
          // 보유도 거래도 없던 구간(전부 0)은 표에 노이즈만 되므로 제외한다.
          if (isEmptyBucket(summary)) {
            return;
          }
          // 실제로 덮은 구간. 앞에 붙인 기초 지점은 제외하고 그 구간의 날짜만 본다.
          LocalDate from = null;
          LocalDate to = null;
          for (TradeProfitTimeSeriesPoint point : points) {
            LocalDate date = pointDate(point, zoneId);
            if (!bucketKey(date, monthly).equals(key)) {
              continue;
            }
            if (from == null) {
              from = date;
            }
            to = date;
          }
          boolean complete = from != null && to != null && coversWholeBucket(from, to, monthly);
          result.add(
              new TradeProfitPeriodSummary(
                  monthly ? "MONTH" : "YEAR", key, from, to, complete, summary));
        });
    // 최신이 위로. 문자열 키가 YYYY / YYYY-MM 라 사전순 역정렬이 곧 시간 역순이다.
    result.sort(Comparator.comparing(TradeProfitPeriodSummary::label).reversed());
    return result;
  }

  private static String bucketKey(LocalDate date, boolean monthly) {
    return monthly
        ? String.format("%04d-%02d", date.getYear(), date.getMonthValue())
        : String.valueOf(date.getYear());
  }

  /** 그 달/해를 첫날부터 마지막 날까지 온전히 덮었는지. */
  private static boolean coversWholeBucket(LocalDate from, LocalDate to, boolean monthly) {
    if (monthly) {
      return from.getDayOfMonth() == 1 && to.equals(from.withDayOfMonth(from.lengthOfMonth()));
    }
    return from.getDayOfYear() == 1 && to.equals(LocalDate.of(from.getYear(), 12, 31));
  }

  /**
   * 조회 기간에 맞는 쪼갬 단위.
   *
   * <p>연 단위만 쓰면 "올해" 처럼 짧은 구간이 한 줄로 끝나 아무것도 말해 주지 않는다(실측 2026-08-31 삼성전자 '올해': 연도별 1 행). 반대로 17 년치를
   * 달로 쪼개면 200 줄이 되어 읽히지 않는다.
   */
  static String resolveBreakdownUnit(
      List<TradeProfitTimeSeriesPoint> dailySeries, ZoneId zoneId, String requested) {
    if ("MONTH".equalsIgnoreCase(requested) || "YEAR".equalsIgnoreCase(requested)) {
      return requested.toUpperCase(java.util.Locale.ROOT);
    }
    if (dailySeries == null || dailySeries.size() < 2) {
      return "MONTH";
    }
    LocalDate first = pointDate(dailySeries.get(0), zoneId);
    LocalDate last = pointDate(dailySeries.get(dailySeries.size() - 1), zoneId);
    long months = java.time.temporal.ChronoUnit.MONTHS.between(first, last);
    return months > MAX_MONTHLY_BREAKDOWN_MONTHS ? "YEAR" : "MONTH";
  }

  /** 이 개월수를 넘는 구간은 달이 아니라 해로 쪼갠다. 3 년치 36 줄이 표로 읽히는 한계다. */
  private static final int MAX_MONTHLY_BREAKDOWN_MONTHS = 36;

  /**
   * 시계열 집계. captureDates 가 주어지면 해당 날짜의 보유 상태(WmaState)를 capturedStates 에 담는다. 보유 스냅샷 조회가 이 캡처를 쓰므로,
   * 차트 값과 스냅샷 값이 같은 계산에서 나와 항상 일치한다.
   */
  private List<TradeProfitTimeSeriesPoint> simulateDailySeries(
      TradeProfitRequest request,
      Set<LocalDate> captureDates,
      Map<LocalDate, Map<String, WmaState>> capturedStates,
      Map<LocalDate, Map<UUID, BigDecimal>> capturedPrices,
      Map<LocalDate, Map<UUID, LocalDate>> capturedPriceDates,
      Map<UUID, StockItem> capturedStockItems) {
    Instant end = request.getEndDate() != null ? request.getEndDate() : Instant.now();
    Instant start = request.getStartDate();
    // 일자 집계 기준 타임존. 요청 값을 존중하지 않으면 컨테이너가 UTC 로 뜬 환경에서
    // KST 오전 거래가 전날로 집계돼 차트가 하루씩 밀린다.
    ZoneId zoneId = request.resolveZoneId();

    // 1) 전체 트레이드 조회 (날짜 제한 없이 전체 로딩)
    List<Trade> allTrades = new ArrayList<>(loadAllTrades(request));

    if (allTrades.isEmpty()) {
      return new ArrayList<>();
    } // 2) StockItem 정보 로딩 및 그룹핑 키 생성 (calculateProfitByStock과 동일 로직)
    var stockItemIds = allTrades.stream().map(Trade::getStockItemId).collect(Collectors.toSet());
    Map<UUID, StockItem> stockItemMap = new HashMap<>();
    stockItemService
        .findAllByIdWithoutTags(stockItemIds)
        .forEach(si -> stockItemMap.put(si.getId(), si));
    // 보유 스냅샷 경로가 이 맵을 다시 읽지 않도록 그대로 넘겨준다.
    // (실측: holdingsSnapshotBatch 한 요청이 StockItem 조회를 2회 냈다. 두 번째 대상은
    //  첫 번째의 부분집합이라 통째로 중복이었다.)
    if (capturedStockItems != null) {
      capturedStockItems.putAll(stockItemMap);
    }

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
    // 거래 커서와 같은 이유로 지급일도 옮겨갈 때만 계산한다.
    LocalDate nextDividendDay =
        nextDividend == null ? null : toRecordedDate(nextDividend.getPayDate(), zoneId);

    // 종목 식별 키. 같은 종목이 여러 stockItemId 로 들어오는 경우를 하나로 본다(심볼 -> 이름 -> id 순).
    Function<Trade, String> getStockKey =
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

    // 평균단가(WMA) 상태의 키는 '계좌 + 종목'이다. 종목만으로 묶으면 계좌를 가로질러 원가가 섞인다 —
    // A 계좌에서 판 수량의 원가가 B 계좌 평균단가로 빠져나가 남은 원가가 실제와 달라진다.
    // 실측: 같은 보유(수량 전부 일치)인데 이 경로의 원가 합이 계좌별로 계산하는 calculateProfit 과
    // 0.030% 어긋났다. TIGER 리츠부동산인프라는 계좌별 단가가
    // 4,366~4,367 인데 합쳐 굴리면 4,380.85 가 나왔다(화면 두 곳이 서로 다른 평균단가를 보여줬다).
    // 표시(보유 스냅샷)는 예전처럼 종목 단위라, 아래에서 계좌별 상태를 종목으로 합산해 내보낸다.
    Function<Trade, String> getGroupKey =
        t ->
            (t.getAccountId() == null ? "A:-" : "A:" + t.getAccountId())
                + "|"
                + getStockKey.apply(t);

    // 3) 거래 정렬 (날짜 오름차순)
    // 같은 날짜 내에서는 BUY 먼저 처리 (논리적 재고 확보)
    // 마지막 id 비교는 유형까지 같은 동률을 없애 DB 행 순서 의존을 끊는다(쿼리에 ORDER BY 가 없다).
    allTrades.sort(
        Comparator.comparing(Trade::getTradeDate)
            .thenComparing(trade -> trade.getType() == TradeType.BUY ? 0 : 1)
            .thenComparing(Trade::getId, Comparator.nullsLast(Comparator.naturalOrder())));

    // 4) 시뮬레이션 상태 관리 (WMA)
    Map<String, WmaState> stateMap = new HashMap<>();

    BigDecimal globalCumulativeRealized = BigDecimal.ZERO;
    BigDecimal globalCumulativeDividend = BigDecimal.ZERO;

    // 5) 시뮬레이션 루프
    // 시작일: 데이터가 있는 첫 로컬 거래일부터 시작 (Cost Basis 구축을 위해)
    LocalDate firstTradeDate = toRecordedDate(allTrades.get(0).getTradeDate(), zoneId);
    LocalDate simulationStart = firstTradeDate;
    // 출력 시작일: 요청상 start 날짜 (없으면 첫 거래일)
    LocalDate outputStart = start != null ? toLocalDate(start, zoneId) : firstTradeDate;
    LocalDate outputEnd = toInclusiveEndDate(end, zoneId);

    // Price History (Bulk Load)
    LocalDate startLocalDate =
        simulationStart.isBefore(outputStart) ? simulationStart : outputStart;
    LocalDate endLocalDate = outputEnd;
    // 시뮬레이션은 종가만 쓴다. 엔티티로 읽으면 쓰지 않는 컬럼까지 매핑하느라
    // 이 조회 하나가 응답 시간의 대부분을 차지한다(스택 샘플 28/28).
    //
    // 조회 범위도 나눈다. 출력 구간은 매일 평가액을 내야 하므로 일별 전부 읽지만,
    // 그 이전 과거에서 가격을 쓰는 곳은 "거래 수량의 수정주가 환산" 한 곳뿐이라
    // 거래가 있는 날의 최근 종가만 있으면 된다(측정: 전체 6441일 중 거래일 140일).
    // 종목마다 가격이 실제로 쓰이는 구간만 읽는다.
    //  - 시작: 그 종목의 첫 거래일 (그 전에는 보유 수량이 0)
    //  - 끝  : 전량 매도로 잔량이 0이 된 그룹은 마지막 거래일까지 (그 뒤로는 평가 대상이 아님)
    // 잔량 판정은 그룹 키 기준이다. 같은 종목이 여러 stockItemId 로 들어와 한 상태로 합쳐질 수 있는데,
    // id 별로 따지면 아직 보유 중인 그룹의 가격을 끊어버릴 수 있다.
    Map<String, Long> netQuantityByGroup = new HashMap<>();
    Map<String, LocalDate> lastTradeDayByGroup = new HashMap<>();
    Map<UUID, String> groupByStockItem = new HashMap<>();
    Map<UUID, LocalDate> firstTradeDayByStockItem = new HashMap<>();
    for (Trade trade : allTrades) {
      LocalDate tradeDay = toRecordedDate(trade.getTradeDate(), zoneId);
      String groupKey = getStockKey.apply(trade);
      groupByStockItem.put(trade.getStockItemId(), groupKey);
      firstTradeDayByStockItem.merge(
          trade.getStockItemId(), tradeDay, (a, b) -> a.isBefore(b) ? a : b);
      lastTradeDayByGroup.merge(groupKey, tradeDay, (a, b) -> a.isAfter(b) ? a : b);
      long signed = trade.getType() == TradeType.BUY ? trade.getQuantity() : -trade.getQuantity();
      netQuantityByGroup.merge(groupKey, signed, Long::sum);
    }

    Map<UUID, LocalDate[]> priceRangeByStockItem = new HashMap<>();
    for (Map.Entry<UUID, LocalDate> entry : firstTradeDayByStockItem.entrySet()) {
      UUID stockItemId = entry.getKey();
      String groupKey = groupByStockItem.get(stockItemId);
      LocalDate from = entry.getValue().isBefore(outputStart) ? outputStart : entry.getValue();
      LocalDate to = endLocalDate;
      Long net = netQuantityByGroup.get(groupKey);
      if (net != null && net <= 0L) {
        LocalDate closedOn = lastTradeDayByGroup.get(groupKey);
        if (closedOn != null && closedOn.isBefore(to)) {
          to = closedOn;
        }
      }
      if (from.isAfter(endLocalDate) || to.isBefore(from)) {
        continue;
      }
      priceRangeByStockItem.put(stockItemId, new LocalDate[] {from, to});
    }

    // 조회하면서 바로 (일자 -> (종목 -> 종가)) 로 담는다. 예전에는 87,465 행짜리 리스트를 만든 뒤
    // 아래에서 다시 전체를 훑어 같은 맵으로 옮겼다(레코드 8.7만 개 + 두 번째 순회가 통째로 낭비).
    Map<LocalDate, Map<UUID, BigDecimal>> dailyPriceMap =
        stockPriceService.getDailyClosePricesGrouped(priceRangeByStockItem);

    // 폴백 시드(직전 최근 종가). 예전에는 (outputStart 이전 거래일 전체) x (전 종목) 을 다 조회했는데
    // 실제로 쓰이는 값은 '각 종목이 자기 거래일에 갖는 가격'과 '구간 첫날 값'뿐이다
    // (실측: 날짜 115개 x 종목 42개 = 4,830 쌍 -> 실제 필요한 쌍 300개 미만).
    Set<String> pairSeen = new HashSet<>();
    List<UUID> asOfIds = new ArrayList<>();
    List<LocalDate> asOfDayList = new ArrayList<>();
    for (Trade trade : allTrades) {
      LocalDate tradeDay = toRecordedDate(trade.getTradeDate(), zoneId);
      if (tradeDay.isBefore(outputStart)
          && trade.getStockItemId() != null
          && pairSeen.add(trade.getStockItemId() + "@" + tradeDay)) {
        asOfIds.add(trade.getStockItemId());
        asOfDayList.add(tradeDay);
      }
    }
    for (UUID stockItemId : stockItemIds) {
      if (stockItemId != null && pairSeen.add(stockItemId + "@" + outputStart)) {
        asOfIds.add(stockItemId);
        asOfDayList.add(outputStart);
      }
    }

    for (StockDailyClosePrice h :
        stockPriceService.getLatestClosePricesForPairs(asOfIds, asOfDayList)) {
      // 루프가 방문하지 않는 날(startLocalDate 이전)의 행은 예전 조회에서도 읽지 않았다.
      // 넣어두면 lastKnownPrices 반영 없이 결과만 달라질 수 있으므로 제외한다.
      if (h.tradeDate().isBefore(startLocalDate)) {
        continue;
      }
      dailyPriceMap
          .computeIfAbsent(h.tradeDate(), k -> new HashMap<>())
          .put(h.stockItemId(), h.closePrice());
    }

    Map<UUID, BigDecimal> lastKnownPrices = new HashMap<>();
    // 각 종가가 '어느 날 종가'인지. 스냅샷이 사용자가 고른 날짜의 시세가 없을 때 조용히 옛 값을
    // 보여주는 것을 막기 위해 함께 들고 다닌다.
    Map<UUID, LocalDate> lastKnownPriceDates = new HashMap<>();

    List<TradeProfitTimeSeriesPoint> series = new ArrayList<>();
    Iterator<Trade> it = allTrades.iterator();
    Trade nextTrade = it.hasNext() ? it.next() : null;
    // 아래 일별 루프는 하루마다 '다음 거래가 오늘 것인지' 들여다본다. 그 자리에서 Instant 를 매번
    // LocalDate 로 바꾸면 같은 값을 하루에 한 번씩, 15년이면 5,800 번 다시 계산한다
    // (실측: java.time 변환이 이 엔드포인트 샘플의 15.3%). 커서가 옮겨갈 때만 계산해 들고 있는다.
    LocalDate nextTradeDay =
        nextTrade == null ? null : toRecordedDate(nextTrade.getTradeDate(), zoneId);

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
        LocalDate tradeDay = nextTradeDay;
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

          BigDecimal cogs = costOfGoodsSold(tradeSellAmount, tax, realProfit);

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
        nextTradeDay = nextTrade == null ? null : toRecordedDate(nextTrade.getTradeDate(), zoneId);
      }

      // 배당 처리 logic
      while (nextDividend != null) {
        LocalDate payDay = nextDividendDay;
        if (payDay.isAfter(currentDay)) {
          break;
        }
        BigDecimal gross = nz(nextDividend.getGrossAmount());
        BigDecimal fee = nz(nextDividend.getFee());
        BigDecimal tax = nz(nextDividend.getTax());
        BigDecimal netDiv = gross.subtract(fee).subtract(tax);
        globalCumulativeDividend = globalCumulativeDividend.add(netDiv);

        nextDividend = divIt.hasNext() ? divIt.next() : null;
        nextDividendDay =
            nextDividend == null ? null : toRecordedDate(nextDividend.getPayDate(), zoneId);
      }

      // 하루 마감 -> Global Cumulative Update
      globalCumulativeRealized = globalCumulativeRealized.add(dailyRealizedGain);

      // lastKnownPrices는 outputStart 여부와 무관하게 항상 업데이트
      // (주말/공휴일 거래, 스냅샷 복원 직후 첫 거래에서 adjustedClose fallback 방지)
      Map<UUID, BigDecimal> dayPricesForLastKnown =
          dailyPriceMap.getOrDefault(currentDay, Map.of());
      lastKnownPrices.putAll(dayPricesForLastKnown);
      for (UUID pricedItemId : dayPricesForLastKnown.keySet()) {
        lastKnownPriceDates.put(pricedItemId, currentDay);
      }

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
                globalCumulativeDividend,
                currentDay));
        // 보유 스냅샷 조회용 캡처: 요청된 날짜의 보유 상태를 그 시점 그대로 복사해 둔다.
        // (DB 캐시 대신 이 캡처를 쓰므로 시계열과 스냅샷이 어긋날 수 없다.)
        if (captureDates != null && capturedStates != null && captureDates.contains(currentDay)) {
          Map<String, WmaState> copied = new HashMap<>();
          stateMap.forEach((stateKey, stateValue) -> copied.put(stateKey, stateValue.copy()));
          capturedStates.put(currentDay, copied);
          // 그 날 시점의 최근 종가도 함께 남긴다. 스냅샷 표시가격을 종목마다 다시 조회하면
          // (날짜 x 종목) 만큼 단건 쿼리가 나가는데, 여기 값이 그 조회 결과와 같다.
          if (capturedPrices != null) {
            capturedPrices.put(currentDay, new HashMap<>(lastKnownPrices));
          }
          if (capturedPriceDates != null) {
            capturedPriceDates.put(currentDay, new HashMap<>(lastKnownPriceDates));
          }
        }
      }

      currentDay = currentDay.plusDays(1);
    }

    // 다운샘플은 호출부에서 수행한다(요약은 일별 시리즈로 계산해야 정확하므로).
    return series;
  }

  /** 일별 시리즈로부터 기간 요약(성장률/TWR/손익 분해)을 계산한다. */
  // 인스턴스 상태를 쓰지 않아 static 이며, 같은 패키지의 테스트가 직접 부를 수 있게 package-private 이다.
  static TradeProfitTimeSeriesSummary summarizeSeries(
      List<TradeProfitTimeSeriesPoint> series, ZoneId zoneId, boolean zeroOpening) {
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
    double peakFactor = 1.0d;
    double maxDrawdown = 0.0d;
    // 실제로 곱해진 일수. 0 이면 TWR/현재낙폭은 '측정되지 않음'이지 '0%' 가 아니다.
    // (실측: 거래가 하나도 없는 구간 2000~2005 을 물으면 growthRatePct/maxDrawdownPct 는 null 인데
    //  timeWeightedReturnPct 와 currentDrawdownPct 만 0.0 이 나가, 화면이 "수익률 +0.00%" 와
    //  "신고점"(currentDrawdownPct > -0.01 판정)으로 그렸다.)
    int compoundedDays = 0;
    LocalDate runningPeakDate = null;
    LocalDate drawdownPeakDate = null;
    LocalDate drawdownTroughDate = null;
    // 기간 중 평가액 고점/저점. 최대 낙폭(입출금 제거한 기준가 기준)과 달리 <b>화면에 찍히는 그 금액</b>이다.
    //
    // 평가액이 0 인 날은 세지 않는다 - 보유가 하나도 없던 날이라 "저점" 의 기준이 될 수 없다.
    // 실측 2026-08-27 '전체' 기간: 6,170 일 중 1,772 일이 평가액 0 이라, 세면 저점이 늘 0 원이 되어
    // 아무것도 말해 주지 않는다.
    TradeProfitTimeSeriesPoint peakPoint = null;
    TradeProfitTimeSeriesPoint troughPoint = null;
    LocalDate peakValueDate = null;
    LocalDate troughValueDate = null;

    for (TradeProfitTimeSeriesPoint point : series) {
      if (point == null || point.timestamp() == null) {
        continue;
      }
      LocalDate pointDate = pointDate(point, zoneId);
      if (firstPoint == null) {
        firstPoint = point;
        runningPeakDate = pointDate;
      }
      lastPoint = point;

      BigDecimal holdingsValue = point.totalHoldingsValue();
      if (holdingsValue != null && holdingsValue.signum() > 0) {
        if (peakPoint == null || holdingsValue.compareTo(peakPoint.totalHoldingsValue()) > 0) {
          peakPoint = point;
          peakValueDate = pointDate;
        }
        if (troughPoint == null || holdingsValue.compareTo(troughPoint.totalHoldingsValue()) < 0) {
          troughPoint = point;
          troughValueDate = pointDate;
        }
      }

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
          compoundedDays++;

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

    // zeroOpening: 기초 이전에 아무 자산도 없던 구간(이력 맨 앞). 첫 지점을 기초로 소비하지 않는다.
    BigDecimal openingValue = zeroOpening ? BigDecimal.ZERO : nz(firstPoint.totalHoldingsValue());
    BigDecimal openingCost = zeroOpening ? BigDecimal.ZERO : nz(firstPoint.totalHoldingsCost());
    BigDecimal openingAccumulated = zeroOpening ? BigDecimal.ZERO : accumulatedProfit(firstPoint);
    BigDecimal closingValue = nz(lastPoint.totalHoldingsValue());

    // 기간 총 손익 = 누적손익(미실현 + 실현 + 배당)의 기말 - 기초
    BigDecimal periodProfit = accumulatedProfit(lastPoint).subtract(openingAccumulated);
    BigDecimal principalDelta = nz(lastPoint.totalHoldingsCost()).subtract(openingCost);
    // 기간 손익이 "손실 회복분"인지 "순수 이익"인지 구분되도록 평가손익 기초/기말도 함께 준다.
    BigDecimal unrealizedStart = openingValue.subtract(openingCost);
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

    // 자산 증가율(입금 포함)은 기초 평가액이 의미 있는 규모일 때만 뜻이 있다.
    // 기초가 거의 0인 구간(예: 최초 매수 직후부터 시작하는 '전체' 기간)에서는 이후 유입된
    // 원금이 분자를 채워 386,300% 같은 해석 불가능한 값이 나온다. 기초가 '기초 + 유입원금'의
    // 1% 미만이면 성장률을 내보내지 않고(null) 화면이 금액(기초→기말)으로 답하게 한다.
    BigDecimal capitalBase = openingValue.add(principalDelta.max(BigDecimal.ZERO));
    boolean openingBaseMeaningful =
        openingValue.compareTo(BigDecimal.ZERO) > 0
            && (capitalBase.compareTo(BigDecimal.ZERO) <= 0
                || openingValue.multiply(BigDecimal.valueOf(100)).compareTo(capitalBase) >= 0);
    Double growthRatePct =
        openingBaseMeaningful
            ? closingValue
                .subtract(openingValue)
                .multiply(BigDecimal.valueOf(100))
                .divide(openingValue, 6, RoundingMode.HALF_UP)
                .doubleValue()
            : null;

    // 기간 손익률 - 넣어 둔 돈 대비 얼마를 벌었나. 자산 증가율(평가액 기준)·TWR(입출금 제거)과 분모가
    // 다른 별개의 값이다. 실측 2026-08-27(올해): 76.66% / 92.88% / 94.93% 로 셋 다 다르다.
    //
    // 기초 평가액이 0 이라 증가율을 못 내는 '전체' 기간에서도 이 값은 나온다 - 분모에 유입 원금이
    // 들어가기 때문이다(실측 '전체': 증가율 계산 불가, 손익률 190.18%).
    Double periodProfitRatePct =
        capitalBase.compareTo(BigDecimal.ZERO) > 0
            ? periodProfit
                .multiply(BigDecimal.valueOf(100))
                .divide(capitalBase, 6, RoundingMode.HALF_UP)
                .doubleValue()
            : null;

    return new TradeProfitTimeSeriesSummary(
        openingValue,
        closingValue,
        growthRatePct,
        compoundedDays > 0 && Double.isFinite(timeWeightedFactor)
            ? (timeWeightedFactor - 1.0d) * 100.0d
            : null,
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
        compoundedDays > 0 && peakFactor > 0.0d
            ? ((timeWeightedFactor - peakFactor) / peakFactor) * 100.0d
            : null,
        periodProfitRatePct,
        peakPoint == null ? null : peakPoint.totalHoldingsValue(),
        peakValueDate,
        troughPoint == null ? null : troughPoint.totalHoldingsValue(),
        troughValueDate);
  }

  /**
   * 매도로 빠져나가는 원가(COGS)를 증권사 기록 실현손익에서 역산한다.
   *
   * <p>기록 실현손익의 정의는 {@code 매도금액 - 원가 - 세금} 이다 &mdash; <b>매도 수수료는 빼지 않는다.</b> 실측(사용자 매도 중 실현손익이 기록된
   * 54 건 전부): 40 건이 이 정의와 1 원 이내로 일치했고, 수수료까지 뺀 정의와 일치한 건은 <b>0 건</b>이었다. 나머지 14 건은 증권사 평균단가가 앱의
   * WMA 와 달라 어느 쪽과도 맞지 않는다.
   *
   * <p>그래서 매도 <i>실수령</i>(수수료까지 뺀 금액)에서 역산하면 COGS 가 매도 수수료만큼 작아지고, 그만큼 보유 원가가 부풀어 남는다. 실측 피해: 삼성전자
   * 보유 원가가 시계열과 종목별 표(WMA)에서 어긋났는데, 그 차이가 마지막 전량매도 이후의 <b>매도 수수료 합과 1 원 오차 없이 같았다</b> &mdash; 원인이
   * 수수료라는 것을 그대로 보여 준다.
   *
   * @param sellAmount 매도금액(단가 x 수량, 수수료·세금 차감 전)
   * @param tax 매도 세금
   * @param recordedProfit 증권사가 기록한 실현손익
   */
  static BigDecimal costOfGoodsSold(
      BigDecimal sellAmount, BigDecimal tax, BigDecimal recordedProfit) {
    return nz(sellAmount).subtract(nz(tax)).subtract(nz(recordedProfit));
  }

  /** 타임존 ID 문자열을 해석한다. 비었거나 알 수 없으면 서버 기본 타임존. */
  private static ZoneId resolveZoneIdOrDefault(String timeZone) {
    if (timeZone == null || timeZone.isBlank()) {
      return ZoneId.systemDefault();
    }
    try {
      return ZoneId.of(timeZone);
    } catch (Exception ex) {
      log.debug("Unknown time zone '{}', falling back to system default", timeZone);
      return ZoneId.systemDefault();
    }
  }

  /** 보유·거래·손익이 모두 0인 해(자산이 비어 있던 기간)인지. */
  private static boolean isEmptyBucket(TradeProfitTimeSeriesSummary summary) {
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

  /**
   * 거래·배당 "일자"를 읽는다.
   *
   * <p>이 값들은 시각이 아니라 날짜다. 저장할 때 KST 09:00(= 자정 UTC)으로 박아 넣는다({@code StockAdminService} 의 변환 규칙, 실측:
   * 거래 250건·배당 193건 전부 00:00:00Z). 그런 값을 요청 타임존으로 변환하면 UTC 서쪽에서 하루 앞당겨져, 가격(LocalDate)과 하루씩 어긋난 채로
   * 시뮬레이션이 돈다.
   *
   * <p>실측 피해: {@code timeZone=America/New_York} 이면 TWR 이 +1,442% -> -292.65%, 최대 낙폭이 -57% ->
   * -311.65% 로 나왔다. 낙폭은 정의상 -100% 를 넘을 수 없으므로 명백한 오류다.
   *
   * <p>자정 UTC 인 값은 날짜 전용으로 보고 UTC 날짜를 쓴다(= KST 날짜와 같다). 실제 시각이 들어 있는 데이터는 예전처럼 요청 타임존을 따른다.
   */
  /** 시계열 지점의 '거래일'. 시뮬레이션이 담아 둔 값을 그대로 쓰고, 없을 때만 예전처럼 타임스탬프를 되돌린다. */
  private static LocalDate pointDate(TradeProfitTimeSeriesPoint point, ZoneId zoneId) {
    LocalDate date = point.date();
    return date != null ? date : point.timestamp().atZone(zoneId).toLocalDate();
  }

  private static LocalDate toRecordedDate(Instant instant, ZoneId zoneId) {
    java.time.ZonedDateTime utc = instant.atZone(java.time.ZoneOffset.UTC);
    if (utc.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
      return utc.toLocalDate();
    }
    return instant.atZone(zoneId).toLocalDate();
  }

  private LocalDate toInclusiveEndDate(Instant instant, ZoneId zoneId) {
    return instant.atZone(zoneId).minusNanos(1).toLocalDate();
  }

  // 인스턴스 상태를 쓰지 않아 static 이며, 같은 패키지의 테스트가 경계를 직접 확인할 수 있게 package-private 이다.
  static BigDecimal resolveEvaluationQuantity(
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

  static BigDecimal detectLikelyCorporateActionFactor(BigDecimal priceRatio) {
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

  private static boolean isWithinCorporateActionTolerance(
      BigDecimal actualRatio, BigDecimal candidateFactor) {
    BigDecimal relativeDelta =
        actualRatio
            .subtract(candidateFactor)
            .abs()
            .divide(candidateFactor.abs(), 6, RoundingMode.HALF_UP);
    return relativeDelta.compareTo(CORPORATE_ACTION_FACTOR_TOLERANCE) <= 0;
  }

  /**
   * 요청에 실린 계좌가 모두 그 사용자 것인지 확인한다. calculateProfit 이 하던 검증과 같다.
   *
   * <p>비교는 <b>요청자 기준</b>으로 한다. 계좌 쪽을 기준으로 삼으면 소유자가 비어 있는 계좌에서 {@link NullPointerException} 이 나 인가
   * 거부(400) 대신 500 이 된다. 소유자를 모르는 계좌는 "그 사용자 것이 아니다" 로 보는 편이 안전하다.
   */
  private void assertAccountsOwnedBy(List<Account> accountList, UUID userId) {
    for (Account account : accountList) {
      if (userId == null || !userId.equals(account.getUserId())) {
        StockErrorCode.INVALID_USER_ID.throwException(userId, account.getId());
      }
    }
  }

  private List<Trade> loadAllTrades(TradeProfitRequest request) {
    return switch (request.getRequestType()) {
      case USER -> {
        // 계좌를 먼저 읽어 id 를 뽑고 다시 거래를 읽던 왕복 2회를 조인 1회로 줄인다.
        // 거래가 하나도 없을 때만 "계좌가 아예 없는 사용자"인지 확인해 예전과 같은 오류를 낸다.
        // 계좌도 거래도 없는 사용자는 오류가 아니라 빈 결과다(calculateProfit 의 USER 분기와 같은 규칙).
        yield tradeService.findByUserId(request.getUserId());
      }
      case USER_ACCOUNT -> {
        var accountList = accountService.findByIdIn(request.getAccountIdList());
        if (accountList.isEmpty()) {
          StockErrorCode.INVALID_USER_ID.throwException();
        }
        // 남의 계좌 id 를 넣어도 그대로 읽히던 구멍. 같은 조건을 쓰는 calculateProfit 은 이 검증을
        // 하고 있었는데 여기만 빠져 있었다. 계좌는 이미 읽어둔 것이라 조회가 늘지 않는다.
        assertAccountsOwnedBy(accountList, request.getUserId());
        yield tradeService.findByAccountIdIn(request.getAccountIdList());
      }
      case USER_STOCKITEM -> {
        var accountList = accountService.findByUserId(request.getUserId());
        // 계좌가 아직 없는 사용자는 오류가 아니라 빈 결과다.
        if (accountList.isEmpty()) {
          yield List.of();
        }
        assertAccountsOwnedBy(accountList, request.getUserId());
        yield tradeService.findByAccountIdInAndStockItemIdIn(
            accountList.stream().map(Account::getId).toList(), request.getStockItemIdList());
      }
      case USER_ACCOUNT_STOCKITEM -> {
        var accountList = accountService.findByIdIn(request.getAccountIdList());
        if (accountList.isEmpty()) {
          StockErrorCode.INVALID_USER_ID.throwException();
        }
        assertAccountsOwnedBy(accountList, request.getUserId());
        yield tradeService.findByAccountIdInAndStockItemIdIn(
            request.getAccountIdList(), request.getStockItemIdList());
      }
    };
  }

  private List<TradeProfitTimeSeriesPoint> applyGranularity(
      List<TradeProfitTimeSeriesPoint> series, String granularity, ZoneId zoneId) {
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
        weekly ? downsampleWeekly(series, zoneId) : downsampleMonthly(series, zoneId);
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
      // 요약(summarizeSeries)과 같은 규칙으로 0 인 날을 뺀다. 규칙이 갈리면 카드가 말하는 저점과
      // 차트가 찍는 저점이 달라진다(실측 '전체': 0 을 세면 차트는 늘 "최저 0원" 을 그린다).
      if (p == null
          || p.timestamp() == null
          || p.totalHoldingsValue() == null
          || p.totalHoldingsValue().signum() <= 0) {
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
        // 순수 평가액 waypoint: 거래 카운트/매도 수량/일별 실현손익은 이미 인접 버킷 합계에
        // 반영되어 있으므로 0으로 두어 이중 계산을 막는다.
        result.add(
            new TradeProfitTimeSeriesPoint(
                extreme.timestamp(),
                extreme.cumulativeRealizedProfit(),
                BigDecimal.ZERO,
                0L,
                0L,
                0L,
                extreme.totalHoldingsValue(),
                extreme.totalHoldingsCost(),
                extreme.cumulativeTotalProfit(),
                extreme.cumulativeDividend(),
                extreme.date()));
      }
    }
    result.sort(Comparator.comparing(TradeProfitTimeSeriesPoint::timestamp));
    return result;
  }

  private List<TradeProfitTimeSeriesPoint> downsampleWeekly(
      List<TradeProfitTimeSeriesPoint> series, ZoneId zoneId) {
    // 주별로 그룹핑 후 마지막 포인트(금요일 or 마지막 거래일)를 유지하되,
    // 해당 주 전체의 tradeCount/dailyRealizedProfit을 합산하여 반영
    return series.stream()
        .collect(
            Collectors.groupingBy(
                p -> {
                  ZonedDateTime zdt = p.timestamp().atZone(zoneId);
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
              // tradeVolume 도 흐름값(그 기간의 매도 수량)이라 합산해야 한다. 마지막 날 값을 그대로
              // 쓰면 그 하루에 매도가 없었으면 0 이 되어, 같은 구간인데 granularity 에 따라 합계가
              // 달라진다(실측: 일 121,305 / 주 680 / 월 3,947).
              long weekTradeVolume =
                  week.stream().mapToLong(TradeProfitTimeSeriesPoint::tradeVolume).sum();
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
                  weekTradeVolume,
                  last.totalHoldingsValue(),
                  last.totalHoldingsCost(),
                  last.cumulativeTotalProfit(),
                  last.cumulativeDividend(),
                  last.date());
            })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(TradeProfitTimeSeriesPoint::timestamp))
        .collect(Collectors.toList());
  }

  private List<TradeProfitTimeSeriesPoint> downsampleMonthly(
      List<TradeProfitTimeSeriesPoint> series, ZoneId zoneId) {
    // 월별로 그룹핑 후 마지막 포인트를 유지하되 tradeCount/dailyRealizedProfit 합산
    return series.stream()
        .collect(Collectors.groupingBy(p -> YearMonth.from(pointDate(p, zoneId))))
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
              // tradeVolume 도 흐름값(그 기간의 매도 수량)이라 합산해야 한다. 마지막 날 값을 그대로
              // 쓰면 그 하루에 매도가 없었으면 0 이 되어, 같은 구간인데 granularity 에 따라 합계가
              // 달라진다(실측: 일 121,305 / 주 680 / 월 3,947).
              long monthTradeVolume =
                  month.stream().mapToLong(TradeProfitTimeSeriesPoint::tradeVolume).sum();
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
                  monthTradeVolume,
                  last.totalHoldingsValue(),
                  last.totalHoldingsCost(),
                  last.cumulativeTotalProfit(),
                  last.cumulativeDividend(),
                  last.date());
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

    // 거래 목록에 붙일 종목 '이름'만 필요하다. 예전에는 종목 테이블을 통째로 읽었는데
    // (실측: 전체 86개 중 이 사용자가 거래한 것은 42개 -> 44개가 낭비), findAll 은 태그 테이블까지
    // 한 번 더 읽는다(실측: /api/trade 한 요청의 DB 커넥션 획득 6회). 이름만 쓰므로 거래에 실제로
    // 등장하는 종목만, 태그 없이 읽는다. 사용자가 늘어도 조회량이 전체 종목 수를 따라가지 않는다.
    Set<UUID> tradedStockItemIds = new HashSet<>();
    for (Trade trade : tradeList) {
      if (trade.getStockItemId() != null) {
        tradedStockItemIds.add(trade.getStockItemId());
      }
    }
    Map<UUID, String> stockItemNames = new HashMap<>();
    stockItemService
        .findAllByIdWithoutTags(tradedStockItemIds)
        .forEach(item -> stockItemNames.put(item.getId(), item.getName()));

    List<TradeResponse> result = new ArrayList<>();

    // Group by accountId and stockItemId to calculate realized profit dynamically
    for (Trade trade : tradeList) {
      boolean inRange = true;
      if (request.startDate() != null && trade.getTradeDate().isBefore(request.startDate()))
        inRange = false;
      // endDate 는 배타적이다 - 게이트는 "선택한 마지막 날 + 1 일 00:00" 을 보낸다(게이트의
      // resolvePeriodEndDate 주석과 date-range-picker 가 같은 규약). 시계열(toInclusiveEndDate)과
      // 필터 id 조회(< :endDate)도 배타로 보는데 이 목록만 포함(<=)이라, 종료 시각에 정확히 걸린
      // 기록이 목록에는 나오고 필터 목록에는 빠졌다(실측: 시작=끝인 창에서 표 14건 · 필터 0건).
      if (request.endDate() != null && !trade.getTradeDate().isBefore(request.endDate()))
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
    // id 를 부키로 둬야 같은 시각 거래의 순서가 고정된다. 조회 쿼리에 ORDER BY 가 없어 DB 행 순서에
    // 기대면, 게이트가 이 목록을 페이지로 잘라 여러 번 요청할 때 같은 행이 두 페이지에 나오거나 빠질 수 있다.
    result.sort(
        Comparator.comparing(TradeResponse::tradeDate).thenComparing(TradeResponse::id).reversed());

    return result;
  }

  /** 특정 날짜의 보유 종목 스냅샷을 반환합니다. */
  public List<HoldingsSnapshotItem> getHoldingsSnapshot(UUID userId, LocalDate date) {
    return getHoldingsSnapshot(userId, date, null, null);
  }

  /** 보유 스냅샷. accountId 가 있으면 해당 계좌, 없으면 사용자 전체 기준. */
  public List<HoldingsSnapshotItem> getHoldingsSnapshot(
      UUID userId, LocalDate date, UUID accountId, String timeZone) {
    if (date == null) {
      return List.of();
    }
    return getHoldingsSnapshotBatch(userId, List.of(date), accountId, timeZone)
        .getOrDefault(date, List.of());
  }

  /**
   * 여러 날짜의 보유 스냅샷을 한 번의 시뮬레이션으로 계산한다.
   *
   * <p>예전에는 DailyAccountSnapshot 캐시를 읽었는데, 그 캐시는 한번 쓰이면 갱신되지 않아 거래 추가/정정이나 수정주가 반영 후 시계열 값과 어긋났다(실측
   * 62일 중 3일 불일치). 이제 시계열과 같은 시뮬레이션에서 해당 날짜 상태를 캡처하므로 두 값이 어긋날 수 없다. 날짜가 여러 개여도 시뮬레이션은 한 번만 돈다.
   */
  public Map<LocalDate, List<HoldingsSnapshotItem>> getHoldingsSnapshotBatch(
      UUID userId, List<LocalDate> dates, UUID accountId, String timeZone) {
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

    ZoneId zoneId = resolveZoneIdOrDefault(timeZone);
    TradeProfitRequest request = new TradeProfitRequest();
    request.setTimeZone(timeZone);
    request.setUserId(userId);
    if (accountId != null) {
      request.setAccountIdList(List.of(accountId));
    }
    request.setStartDate(targetDates.first().atStartOfDay(zoneId).toInstant());
    request.setEndDate(targetDates.last().plusDays(1).atStartOfDay(zoneId).toInstant());

    Map<LocalDate, Map<String, WmaState>> capturedStates = new HashMap<>();
    Map<LocalDate, Map<UUID, BigDecimal>> capturedPrices = new HashMap<>();
    Map<LocalDate, Map<UUID, LocalDate>> capturedPriceDates = new HashMap<>();
    Map<UUID, StockItem> capturedStockItems = new HashMap<>();
    try {
      simulateDailySeries(
          request,
          targetDates,
          capturedStates,
          capturedPrices,
          capturedPriceDates,
          capturedStockItems);
    } catch (BlueskyException ex) {
      // 잘못된 사용자/계좌 같은 '거절'을 여기서 삼키면 화면에는 오류가 아니라 '보유 없음'이 뜬다.
      // 뜻이 분명한 오류는 그대로 올리고, 데이터 문제로 인한 나머지 예외만 아래처럼 비워서 넘긴다.
      throw ex;
    } catch (Exception ex) {
      log.warn("Failed to build holdings snapshot: userId={}, dates={}", userId, targetDates, ex);
      return result;
    }

    // 종목 정보는 전 날짜에 걸쳐 한 번만 읽는다. 날짜마다 종목마다 findById 를 부르면
    // (날짜 x 종목) 만큼 단건 쿼리가 나간다.
    // 시뮬레이션이 이미 읽어 둔 종목 맵을 그대로 쓴다(위 capturedStockItems).
    Map<UUID, StockItem> stockItemMap = capturedStockItems;

    for (LocalDate date : targetDates) {
      result.put(
          date,
          buildHoldingsSnapshotItems(
              capturedStates.get(date),
              date,
              capturedPrices.get(date),
              capturedPriceDates.get(date),
              stockItemMap));
    }
    return result;
  }

  /**
   * WMA 상태를 종목 단위로 합친다.
   *
   * <p>상태의 키는 '계좌 + 종목'이라 같은 종목이 계좌 수만큼 들어 있다(계좌를 가로질러 원가가 섞이지 않게 하려는 의도적 분리). 화면의 보유 목록은 종목 단위 한
   * 줄이므로 여기서 수량과 원가를 더해 되돌린다. 평균단가는 합산 원가 ÷ 합산 수량이 되어 계좌별 평균단가와 앞뒤가 맞는다.
   */
  static Map<UUID, WmaState> mergeStatesByStockItem(Map<String, WmaState> stateMap) {
    Map<UUID, WmaState> mergedByStockItem = new LinkedHashMap<>();
    if (stateMap == null) {
      return mergedByStockItem;
    }
    for (WmaState state : stateMap.values()) {
      if (state == null || state.getStockItemId() == null) {
        continue;
      }
      WmaState merged =
          mergedByStockItem.computeIfAbsent(
              state.getStockItemId(),
              id -> {
                WmaState s = new WmaState();
                s.setStockItemId(id);
                return s;
              });
      merged.setQuantity(merged.getQuantity().add(state.getQuantity()));
      merged.setRawQuantity(merged.getRawQuantity() + state.getRawQuantity());
      merged.setTotalCost(merged.getTotalCost().add(state.getTotalCost()));
      merged.setTotalCostNet(merged.getTotalCostNet().add(state.getTotalCostNet()));
    }
    return mergedByStockItem;
  }

  /** 캡처된 보유 상태(WmaState)를 화면용 항목으로 변환한다. */
  private List<HoldingsSnapshotItem> buildHoldingsSnapshotItems(
      Map<String, WmaState> stateMap,
      LocalDate date,
      Map<UUID, BigDecimal> capturedPrices,
      Map<UUID, LocalDate> capturedPriceDates,
      Map<UUID, StockItem> stockItemMap) {
    if (stateMap == null || stateMap.isEmpty()) {
      return List.of();
    }

    List<HoldingsSnapshotItem> result = new ArrayList<>();
    for (WmaState state : mergeStatesByStockItem(stateMap).values()) {
      if (state.getQuantity().compareTo(BigDecimal.ZERO) <= 0 || state.getStockItemId() == null)
        continue;

      String name = state.getStockItemId().toString();
      String symbol = null;
      StockItem item = stockItemMap != null ? stockItemMap.get(state.getStockItemId()) : null;
      if (item != null) {
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
      // 시뮬레이션이 그 날 시점에 들고 있던 최근 종가를 그대로 쓴다(= getPriceAt 과 같은 값).
      // 캡처가 없을 때만 예전처럼 단건 조회로 폴백한다.
      BigDecimal price = capturedPrices != null ? capturedPrices.get(state.getStockItemId()) : null;
      if (price == null) {
        price = stockPriceService.getPriceAt(state.getStockItemId(), date);
      }
      BigDecimal value = price.multiply(state.getQuantity());
      BigDecimal unrealizedProfit = value.subtract(state.getTotalCost());

      // 표시 가격은 표시 수량과 같은 기준이어야 한다.
      // 수정주가가 반영된 종목은 시뮬레이션 내부 수량(state.getQuantity())과 실제 보유 주수
      // (rawQuantity)가 배수만큼 다르다. 이때 내부 가격을 그대로 내보내면 수량 x 가격 != 평가액 이 되고,
      // 게이트의 시가배당률(priceAtDate x 배당 수량)이 그 배수만큼 어긋난다
      // (실측: 047820/329180 의 8개 스냅샷 행에서 최대 13배).
      BigDecimal displayPrice = price;
      if (displayQty > 0 && state.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal ratio = state.getQuantity().divide(displayQtyBd, 10, RoundingMode.HALF_UP);
        if (ratio.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.0001")) > 0) {
          displayPrice = value.divide(displayQtyBd, 2, RoundingMode.HALF_UP);
        }
      }

      result.add(
          new HoldingsSnapshotItem(
              state.getStockItemId(),
              name,
              symbol,
              displayQtyBd,
              avgCost,
              displayPrice,
              capturedPriceDates != null ? capturedPriceDates.get(state.getStockItemId()) : null,
              value,
              unrealizedProfit));
    }

    result.sort(Comparator.comparing(HoldingsSnapshotItem::unrealizedProfit).reversed());
    return result;
  }
}
