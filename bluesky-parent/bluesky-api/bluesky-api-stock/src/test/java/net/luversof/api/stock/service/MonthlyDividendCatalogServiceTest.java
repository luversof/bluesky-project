package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.luversof.api.stock.domain.MonthlyDividendPayout;
import net.luversof.api.stock.domain.MonthlyDividendProfile;
import net.luversof.api.stock.domain.StockDailyClosePrice;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.MonthlyDividendPayoutRepository;
import net.luversof.api.stock.repository.MonthlyDividendProfileRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.service.MonthlyDividendPayoutService.SnapshotStats;
import net.luversof.api.stock.web.dto.response.MonthlyDividendCatalogResponse;

/**
 * 월배당 ETF 목록(종목 단위) &mdash; 사용자 요청 2026-09-21 로 만든 {@code /api/monthlyDividendCatalog}.
 *
 * <p>보유 수량이 없어도 값이 나와야 한다: 수익률은 주당 배당 &divide; 현재가라 수량이 필요 없고, 과세표준 비중은 운용사 지급 이력에서 낸다. 내가 가졌는지는
 * 게이트가 원장에서 따로 붙인다(이 응답에는 없다).
 */
@ExtendWith(MockitoExtension.class)
class MonthlyDividendCatalogServiceTest {

  @Mock private MonthlyDividendProfileRepository monthlyDividendProfileRepository;

  @Mock private MonthlyDividendPayoutRepository monthlyDividendPayoutRepository;

  @Mock private MonthlyDividendPayoutService monthlyDividendPayoutService;

  @Mock private StockItemRepository stockItemRepository;

  @Mock private StockPriceService stockPriceService;

  @Mock
  private net.luversof.api.stock.repository.StockPriceHistoryRepository stockPriceHistoryRepository;

  @InjectMocks private MonthlyDividendCatalogService monthlyDividendCatalogService;

  @Test
  void 보유하지_않아도_수익률과_과세_비중이_나온다() {
    UUID stockItemId = UUID.randomUUID();
    StockItem stockItem = stockItem(stockItemId, "489030", "PLUS 고배당주위클리커버드콜");
    MonthlyDividendProfile profile = profile(stockItemId, "MONTH_END", 3);

    when(monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc())
        .thenReturn(List.of(profile));
    when(stockItemRepository.findAllById(any())).thenReturn(List.of(stockItem));
    when(stockPriceService.getLatestPrices(anyCollection()))
        .thenReturn(
            Map.of(
                stockItemId,
                new StockDailyClosePrice(
                    stockItemId, LocalDate.of(2026, 9, 18), new BigDecimal("8520"))));
    when(monthlyDividendPayoutService.computeSnapshotStats(stockItemId))
        .thenReturn(
            new SnapshotStats(
                LocalDate.of(2026, 9, 17),
                new BigDecimal("300"),
                new BigDecimal("280"),
                new BigDecimal("12.50")));
    when(monthlyDividendPayoutRepository.findByStockItemIdOrderByPayDateDescRecordDateDesc(
            stockItemId))
        .thenReturn(List.of(payout(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 17))));

    List<MonthlyDividendCatalogResponse> rows = monthlyDividendCatalogService.findCatalog(null);

    assertThat(rows).hasSize(1);
    MonthlyDividendCatalogResponse row = rows.get(0);
    assertThat(row.stockItemSymbol()).isEqualTo("489030");
    assertThat(row.payoutWindow()).isEqualTo("MONTH_END");
    assertThat(row.averageDividendPerShare1y()).isEqualByComparingTo("280");
    assertThat(row.latestDividendPerShare()).isEqualByComparingTo("300");
    assertThat(row.averageTaxableBaseRatio1y()).isEqualByComparingTo("12.50");
    // 평균 주당 과세표준액 = 280 x 12.5%
    assertThat(row.averageTaxableBasePerShare1y()).isEqualByComparingTo("35.00");
    assertThat(row.currentPrice()).isEqualByComparingTo("8520");
    assertThat(row.currentPriceDate()).isEqualTo(LocalDate.of(2026, 9, 18));
    // 280 / 8520 = 3.29% · 연 39.48% - 수량이 없어도 나온다.
    assertThat(row.monthlyYieldPct()).isEqualByComparingTo("3.29");
    assertThat(row.annualYieldPct()).isEqualByComparingTo("39.48");
    assertThat(row.payoutCount()).isEqualTo(1);
    assertThat(row.latestPayDate()).isEqualTo(LocalDate.of(2026, 9, 17));
  }

  /** 지급 이력이 없는 종목도 줄은 나온다(0 으로) - 등록만 하고 아직 못 채운 종목을 화면에서 알아볼 수 있어야 한다. */
  @Test
  void 지급_이력이_없으면_0_으로_나온다() {
    UUID stockItemId = UUID.randomUUID();
    StockItem stockItem = stockItem(stockItemId, "466940", "TIGER 은행고배당플러스TOP10");

    when(monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc())
        .thenReturn(List.of(profile(stockItemId, "UNKNOWN", 9)));
    when(stockItemRepository.findAllById(any())).thenReturn(List.of(stockItem));
    when(stockPriceService.getLatestPrices(anyCollection())).thenReturn(Map.of());
    when(monthlyDividendPayoutService.computeSnapshotStats(stockItemId)).thenReturn(null);
    when(monthlyDividendPayoutRepository.findByStockItemIdOrderByPayDateDescRecordDateDesc(
            stockItemId))
        .thenReturn(List.of());

    List<MonthlyDividendCatalogResponse> rows = monthlyDividendCatalogService.findCatalog(null);

    assertThat(rows).hasSize(1);
    MonthlyDividendCatalogResponse row = rows.get(0);
    assertThat(row.payoutCount()).isZero();
    assertThat(row.averageDividendPerShare1y()).isEqualByComparingTo("0");
    assertThat(row.currentPrice()).isEqualByComparingTo("0");
    // 현재가가 없으면 수익률을 지어내지 않는다(0 ÷ 0 은 0 이 아니라 '모름'에 가깝지만, 화면은 0 으로 적고 이력 없음을 함께 보여 준다).
    assertThat(row.annualYieldPct()).isEqualByComparingTo("0");
    assertThat(row.latestPayDate()).isNull();
  }

  @Test
  void 활성만_보기는_활성_프로필만_읽는다() {
    when(monthlyDividendProfileRepository.findByActiveOrderByDisplayOrderAscUpdatedDateDesc(true))
        .thenReturn(List.of());

    assertThat(monthlyDividendCatalogService.findCatalog(true)).isEmpty();
  }

  private static StockItem stockItem(UUID id, String symbol, String name) {
    StockItem stockItem = new StockItem();
    stockItem.setId(id);
    stockItem.setSymbol(symbol);
    stockItem.setName(name);
    stockItem.setMarket("KRX");
    return stockItem;
  }

  private static MonthlyDividendProfile profile(UUID stockItemId, String payoutWindow, int order) {
    MonthlyDividendProfile profile = new MonthlyDividendProfile();
    profile.setId(UUID.randomUUID());
    profile.setStockItemId(stockItemId);
    profile.setPayoutWindow(payoutWindow);
    profile.setDisplayOrder(order);
    profile.setActive(true);
    profile.setSourceUrl("https://example.test/etf");
    profile.setLastVerifiedDate(LocalDate.of(2026, 9, 17));
    return profile;
  }

  @Test
  void 분배금_추세가_응답에_실린다() {
    // 최근 3 회 100 원, 그 앞 9 회 200 원 => 12 회 평균 175 원, 최근은 그보다 42.86% 적다.
    UUID stockItemId = UUID.randomUUID();
    StockItem stockItem = stockItem(stockItemId, "489030", "PLUS 고배당주위클리커버드콜");

    when(monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc())
        .thenReturn(List.of(profile(stockItemId, "MONTH_END", 3)));
    when(stockItemRepository.findAllById(any())).thenReturn(List.of(stockItem));
    when(monthlyDividendPayoutRepository.findByStockItemIdOrderByPayDateDescRecordDateDesc(
            stockItemId))
        .thenReturn(payouts(stockItemId));

    List<MonthlyDividendCatalogResponse> rows = monthlyDividendCatalogService.findCatalog(null);

    assertThat(rows).hasSize(1);
    MonthlyDividendCatalogResponse row = rows.get(0);
    assertThat(row.averageDividendPerShare3m())
        .as("최근 3 회 평균 - 두 값이 뒤바뀌면 여기서 걸린다")
        .isEqualByComparingTo("100.00");
    assertThat(row.payoutTrendPct()).isEqualByComparingTo("-42.86");
  }

  @Test
  void 지급이_모자란_종목은_추세가_비어_있다() {
    // 0 을 실어 보내면 화면이 "분배금이 그대로다" 로 적는다 - 실제로는 견줄 이력이 없는 것이다.
    UUID stockItemId = UUID.randomUUID();

    when(monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc())
        .thenReturn(List.of(profile(stockItemId, "MONTH_END", 3)));
    when(stockItemRepository.findAllById(any()))
        .thenReturn(List.of(stockItem(stockItemId, "0219E0", "KODEX 200커버드콜액티브")));
    when(monthlyDividendPayoutRepository.findByStockItemIdOrderByPayDateDescRecordDateDesc(
            stockItemId))
        .thenReturn(
            List.of(
                payout(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 17)),
                payout(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 17))));

    List<MonthlyDividendCatalogResponse> rows = monthlyDividendCatalogService.findCatalog(null);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).payoutTrendPct()).isNull();
    assertThat(rows.get(0).averageDividendPerShare3m()).isNull();
  }

  /**
   * 위험 지표는 1 년 기준이다(사용자 결정 2026-09-22).
   *
   * <p>짧게 잡으면 "작년에 많이 빠졌다" 를 놓치고, 실어 세면 지금 위험이 아니다. 그래서 기간 밖 하락을 넣어 두고 그것이 안 세지는지까지 본다.
   */
  @Test
  void 위험_지표가_1년_기준으로_응답에_실린다() {
    UUID stockItemId = UUID.randomUUID();
    LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

    when(monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc())
        .thenReturn(List.of(profile(stockItemId, "MONTH_END", 3)));
    when(stockItemRepository.findAllById(any()))
        .thenReturn(List.of(stockItem(stockItemId, "489030", "PLUS 고배당주위클리커버드콜")));
    when(monthlyDividendPayoutRepository.findByStockItemIdOrderByPayDateDescRecordDateDesc(
            stockItemId))
        .thenReturn(List.of(payout(today.minusDays(5), today.minusDays(1))));
    when(stockPriceHistoryRepository.findDailyClosePrices(stockItemId, null, null))
        .thenReturn(priceHistory(stockItemId, today));

    List<MonthlyDividendCatalogResponse> rows = monthlyDividendCatalogService.findCatalog(null);

    assertThat(rows).hasSize(1);
    MonthlyDividendCatalogResponse row = rows.get(0);
    assertThat(row.maxDrawdownPct())
        .as("1 년 안에서 100 -> 60 이 있었다 - 40% 빠졌다")
        .isEqualByComparingTo("-40.00");
    assertThat(row.volatilityPct()).as("표본은 넘치게 있다").isNotNull();
    assertThat(row.riskFromDate())
        .as("1 년을 다 덮으면 기간 안의 첫 거래일을 알린다 - 2 년 전이 아니다")
        .isAfter(today.minusMonths(12).minusDays(1));
  }

  /**
   * 2 년치 일별 종가. 1 년 밖(오래된 쪽)에만 100 -> 20 의 큰 하락을 두었고, 1 년 안에는 100 -> 60 이 한 번 있다. 기간을 잘못 잡으면 -80% 가
   * 나온다.
   */
  private static List<StockDailyClosePrice> priceHistory(UUID stockItemId, LocalDate today) {
    List<StockDailyClosePrice> rows = new java.util.ArrayList<>();
    LocalDate date = today.minusDays(730);
    while (!date.isAfter(today)) {
      long ago = java.time.temporal.ChronoUnit.DAYS.between(date, today);
      String close;
      if (ago > 365) {
        close = ago == 500 ? "20" : "100";
      } else {
        close = ago == 100 ? "60" : "100";
      }
      rows.add(new StockDailyClosePrice(stockItemId, date, new BigDecimal(close)));
      date = date.plusDays(1);
    }
    return rows;
  }

  /** 최근 3 회 100 원 + 그 앞 9 회 200 원(지급일 내림차순). */
  private static List<MonthlyDividendPayout> payouts(UUID stockItemId) {
    List<MonthlyDividendPayout> rows = new java.util.ArrayList<>();
    for (int index = 0; index < 12; index++) {
      LocalDate recordDate = LocalDate.of(2026, 9, 1).minusMonths(index);
      MonthlyDividendPayout row = payout(recordDate, recordDate.plusDays(16));
      row.setStockItemId(stockItemId);
      row.setDividendAmountPerShare(new BigDecimal(index < 3 ? "100" : "200"));
      rows.add(row);
    }
    return rows;
  }

  private static MonthlyDividendPayout payout(LocalDate recordDate, LocalDate payDate) {
    MonthlyDividendPayout payout = new MonthlyDividendPayout();
    payout.setRecordDate(recordDate);
    payout.setPayDate(payDate);
    payout.setDividendAmountPerShare(new BigDecimal("300"));
    payout.setTaxableBasePerShare(new BigDecimal("37.5"));
    return payout;
  }
}
