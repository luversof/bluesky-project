package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.domain.MonthlyDividendPayout;
import net.luversof.api.stock.domain.MonthlyDividendProfile;
import net.luversof.api.stock.domain.StockDailyClosePrice;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.MonthlyDividendPayoutRepository;
import net.luversof.api.stock.repository.MonthlyDividendProfileRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;
import net.luversof.api.stock.service.MonthlyDividendPayoutService.SnapshotStats;
import net.luversof.api.stock.web.dto.response.MonthlyDividendCatalogResponse;
import net.luversof.api.stock.web.dto.response.MonthlyDividendCatalogResponse.PeriodReturnView;

/**
 * 월배당 ETF 목록 &mdash; 등록된 프로필과 지급 이력으로 만드는 <b>종목 단위</b> 정보.
 *
 * <p>사용자 요청 2026-09-21: 보유하지 않은 월배당 ETF 도 등록해 두고 효율 · 과세 비중으로 견주고 싶다. 시뮬레이터의 월배당 탭은 내가 받을 배당을 다루므로
 * 보유 종목만 두고, 종목 정보는 이 목록이 맡는다.
 *
 * <p>수익률은 주당 배당 &divide; 현재가다 &mdash; 수량이 없어도 성립한다.
 */
@Service
public class MonthlyDividendCatalogService {

  /** 화면이 고를 수 있는 기간(개월). 사용자 결정 2026-09-21. */
  private static final int[] PERIOD_MONTHS = {1, 3, 6, 12};

  private static final ZoneId MARKET_ZONE_ID = ZoneId.of("Asia/Seoul");

  @Autowired private MonthlyDividendProfileRepository monthlyDividendProfileRepository;

  @Autowired private MonthlyDividendPayoutRepository monthlyDividendPayoutRepository;

  @Autowired private MonthlyDividendPayoutService monthlyDividendPayoutService;

  @Autowired private StockItemRepository stockItemRepository;

  @Autowired private StockPriceService stockPriceService;

  @Autowired private StockPriceHistoryRepository stockPriceHistoryRepository;

  /** 등록된 프로필 전부(표시 순서대로). activeOnly 면 활성 프로필만. */
  public List<MonthlyDividendCatalogResponse> findCatalog(Boolean activeOnly) {
    List<MonthlyDividendProfile> profiles =
        Boolean.TRUE.equals(activeOnly)
            ? monthlyDividendProfileRepository.findByActiveOrderByDisplayOrderAscUpdatedDateDesc(
                true)
            : monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc();
    if (profiles.isEmpty()) {
      return List.of();
    }

    // 종목 · 현재가는 한 번에 읽는다(행마다 읽으면 행 수만큼 질의가 나간다).
    Set<UUID> stockItemIds = new HashSet<>();
    profiles.forEach(
        profile -> {
          if (profile.getStockItemId() != null) {
            stockItemIds.add(profile.getStockItemId());
          }
        });
    Map<UUID, StockItem> stockItemById = new HashMap<>();
    stockItemRepository
        .findAllById(stockItemIds)
        .forEach(item -> stockItemById.put(item.getId(), item));
    Map<UUID, StockDailyClosePrice> priceById = stockPriceService.getLatestPrices(stockItemIds);

    List<MonthlyDividendCatalogResponse> rows = new ArrayList<>();
    for (MonthlyDividendProfile profile : profiles) {
      UUID stockItemId = profile.getStockItemId();
      StockItem stockItem = stockItemId != null ? stockItemById.get(stockItemId) : null;
      SnapshotStats stats =
          stockItemId != null
              ? monthlyDividendPayoutService.computeSnapshotStats(stockItemId)
              : null;
      List<MonthlyDividendPayout> payouts =
          stockItemId != null
              ? monthlyDividendPayoutRepository.findByStockItemIdOrderByPayDateDescRecordDateDesc(
                  stockItemId)
              : List.of();
      MonthlyDividendPayout latestPayout = payouts.isEmpty() ? null : payouts.get(0);
      StockDailyClosePrice price = stockItemId != null ? priceById.get(stockItemId) : null;

      // 기간 수익률은 시세 이력으로 낸다(저장하지 않는다 - 매일 바뀌는 계산값이다).
      List<StockDailyClosePrice> priceHistory =
          stockItemId != null
              ? stockPriceHistoryRepository.findDailyClosePrices(stockItemId, null, null)
              : List.of();
      List<PeriodReturnCalculator.PayoutPoint> payoutPoints =
          payouts.stream()
              .map(
                  payout ->
                      new PeriodReturnCalculator.PayoutPoint(
                          payout.getRecordDate(), payout.getDividendAmountPerShare()))
              .toList();
      LocalDate today = LocalDate.now(MARKET_ZONE_ID);
      List<PeriodReturnView> periodReturns = new ArrayList<>();
      for (int months : PERIOD_MONTHS) {
        PeriodReturnCalculator.PeriodReturn computed =
            PeriodReturnCalculator.compute(priceHistory, payoutPoints, today, months);
        if (computed != null) {
          periodReturns.add(
              new PeriodReturnView(
                  computed.months(),
                  computed.baseDate(),
                  computed.priceReturnPct(),
                  computed.totalReturnPct()));
        }
      }
      LocalDate priceHistoryStartDate =
          priceHistory.isEmpty() ? null : priceHistory.get(0).tradeDate();

      BigDecimal currentPrice = price != null ? safe(price.closePrice()) : BigDecimal.ZERO;
      BigDecimal averagePerShare =
          stats != null ? safe(stats.averagePerShare1y()) : BigDecimal.ZERO;
      BigDecimal taxableRatio = stats != null ? safe(stats.taxableBaseRatio1y()) : BigDecimal.ZERO;
      BigDecimal monthlyYieldPct = percent(averagePerShare, currentPrice);

      rows.add(
          new MonthlyDividendCatalogResponse(
              stockItemId,
              stockItem != null ? stockItem.getSymbol() : "",
              stockItem != null ? stockItem.getName() : "",
              profile.getPayoutWindow(),
              profile.getSourceUrl(),
              profile.getLastVerifiedDate(),
              Boolean.TRUE.equals(profile.getActive()),
              profile.getDisplayOrder(),
              payouts.size(),
              latestPayout != null ? latestPayout.getRecordDate() : null,
              latestPayout != null ? latestPayout.getPayDate() : null,
              stats != null ? safe(stats.latestPerShare()) : BigDecimal.ZERO,
              averagePerShare,
              taxableRatio,
              averagePerShare
                  .multiply(taxableRatio)
                  .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP),
              currentPrice,
              price != null ? price.tradeDate() : null,
              monthlyYieldPct,
              monthlyYieldPct.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP),
              priceHistoryStartDate,
              List.copyOf(periodReturns)));
    }
    return rows;
  }

  private BigDecimal safe(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  /** 분모가 0 이하면 0 - 시세를 못 받은 종목은 수익률을 지어내지 않는다. */
  private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
    if (numerator == null || denominator == null || denominator.signum() <= 0) {
      return BigDecimal.ZERO;
    }

    return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
  }
}
