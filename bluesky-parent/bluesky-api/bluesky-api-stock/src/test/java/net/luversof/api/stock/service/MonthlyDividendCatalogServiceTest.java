package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

  @Mock
  private net.luversof.api.stock.repository.MonthlyDividendPayoutQuery monthlyDividendPayoutQuery;

  @Mock private MonthlyDividendPayoutService monthlyDividendPayoutService;

  @Mock private StockItemRepository stockItemRepository;

  @Mock private StockPriceService stockPriceService;

  @Mock
  private net.luversof.api.stock.repository.StockPriceHistoryRepository stockPriceHistoryRepository;

  @Mock
  private net.luversof.api.stock.repository.StockDailyClosePriceQuery stockDailyClosePriceQuery;

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
    when(monthlyDividendPayoutService.computeSnapshotStatsFrom(anyList()))
        .thenReturn(
            new SnapshotStats(
                LocalDate.of(2026, 9, 17),
                new BigDecimal("300"),
                new BigDecimal("280"),
                new BigDecimal("12.50")));
    when(monthlyDividendPayoutQuery.findByStockItemIdInOrderByPayDateDescRecordDateDesc(
            anyCollection()))
        .thenReturn(
            withItem(
                stockItemId, List.of(payout(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 17)))));

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
    when(monthlyDividendPayoutService.computeSnapshotStatsFrom(anyList())).thenReturn(null);
    when(monthlyDividendPayoutQuery.findByStockItemIdInOrderByPayDateDescRecordDateDesc(
            anyCollection()))
        .thenReturn(withItem(stockItemId, List.of()));

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

  /** 프로필의 총보수 · 상장일이 그대로 실리고, 모르는 종목은 0 이 아니라 null 이다(0 은 "보수 없음" 으로 읽힌다). 2026-09-28. */
  @Test
  void 총보수와_상장일이_실리고_모르면_null_이다() {
    UUID knownId = UUID.randomUUID();
    UUID unknownId = UUID.randomUUID();
    MonthlyDividendProfile known = profile(knownId, "MID_MONTH", 1);
    known.setTotalExpenseRatioPct(new java.math.BigDecimal("0.0900"));
    known.setListingDate(LocalDate.of(2024, 3, 5));

    when(monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc())
        .thenReturn(List.of(known, profile(unknownId, "MONTH_END", 2)));
    when(stockItemRepository.findAllById(any()))
        .thenReturn(
            List.of(
                stockItem(knownId, "476800", "KODEX 한국부동산리츠인프라"),
                stockItem(unknownId, "466940", "TIGER 은행고배당플러스TOP10")));
    when(stockPriceService.getLatestPrices(anyCollection())).thenReturn(Map.of());
    when(monthlyDividendPayoutService.computeSnapshotStatsFrom(anyList())).thenReturn(null);
    when(monthlyDividendPayoutQuery.findByStockItemIdInOrderByPayDateDescRecordDateDesc(
            anyCollection()))
        .thenReturn(List.of());

    List<MonthlyDividendCatalogResponse> rows = monthlyDividendCatalogService.findCatalog(null);

    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).totalExpenseRatioPct()).isEqualByComparingTo("0.09");
    assertThat(rows.get(0).listingDate()).isEqualTo(LocalDate.of(2024, 3, 5));
    assertThat(rows.get(1).totalExpenseRatioPct()).isNull();
    assertThat(rows.get(1).listingDate()).isNull();
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
    when(monthlyDividendPayoutQuery.findByStockItemIdInOrderByPayDateDescRecordDateDesc(
            anyCollection()))
        .thenReturn(withItem(stockItemId, payouts(stockItemId)));

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
    when(monthlyDividendPayoutQuery.findByStockItemIdInOrderByPayDateDescRecordDateDesc(
            anyCollection()))
        .thenReturn(
            withItem(
                stockItemId,
                List.of(
                    payout(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 17)),
                    payout(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 17)))));

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
    when(monthlyDividendPayoutQuery.findByStockItemIdInOrderByPayDateDescRecordDateDesc(
            anyCollection()))
        .thenReturn(withItem(stockItemId, List.of(payout(today.minusDays(5), today.minusDays(1)))));
    when(stockDailyClosePriceQuery.findDailyClosePricesForItems(anyString(), any()))
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

  /** 한 번에 읽는 질의가 돌려준 이력은 종목 id 로 나뉜다 - 흉내에도 id 를 붙인다. */
  private static List<MonthlyDividendPayout> withItem(
      UUID stockItemId, List<MonthlyDividendPayout> rows) {
    rows.forEach(row -> row.setStockItemId(stockItemId));
    return rows;
  }

  /**
   * 종목이 늘어도 지급 이력 &middot; 시세 이력은 표 전체에 한 번씩만 읽는다.
   *
   * <p>실측 2026-09-23: 종목 21 개에 지급 이력 두 번 &middot; 시세 한 번씩 63 번 왕복해 응답이 190~230ms 였다. 종목마다 부르는 옛 질의로
   * 돌아가면 여기서 걸린다.
   */
  @Test
  void 종목이_여럿이어도_이력은_한_번씩만_읽는다() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    when(monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc())
        .thenReturn(List.of(profile(first, "MONTH_END", 1), profile(second, "MID_MONTH", 2)));
    when(stockItemRepository.findAllById(any()))
        .thenReturn(List.of(stockItem(first, "489030", "A"), stockItem(second, "498400", "B")));
    List<MonthlyDividendPayout> mixed = new java.util.ArrayList<>();
    mixed.addAll(withItem(first, new java.util.ArrayList<>(payouts(first))));
    mixed.addAll(
        withItem(
            second,
            new java.util.ArrayList<>(
                List.of(payout(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15))))));
    when(monthlyDividendPayoutQuery.findByStockItemIdInOrderByPayDateDescRecordDateDesc(
            anyCollection()))
        .thenReturn(mixed);

    List<MonthlyDividendCatalogResponse> rows = monthlyDividendCatalogService.findCatalog(null);

    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).payoutCount()).as("첫 종목 이력 12 회만 - 섞이면 13").isEqualTo(12);
    assertThat(rows.get(1).payoutCount()).as("둘째 종목 이력 1 회").isEqualTo(1);
    verify(monthlyDividendPayoutQuery, times(1))
        .findByStockItemIdInOrderByPayDateDescRecordDateDesc(anyCollection());
    verify(stockDailyClosePriceQuery, times(1)).findDailyClosePricesForItems(anyString(), any());
    verify(monthlyDividendPayoutRepository, never())
        .findByStockItemIdOrderByPayDateDescRecordDateDesc(any());
    verify(stockPriceHistoryRepository, never()).findDailyClosePrices(any(), any(), any());
    verify(monthlyDividendPayoutService, never()).computeSnapshotStats(any(UUID.class));
  }

  /**
   * 시세는 계산에 쓰는 창(15 개월)만 읽지만, "이력이 언제부터 있나" 는 창과 무관한 실제 첫 거래일이다.
   *
   * <p>실측 2026-09-23: 2 년치를 다 읽던 것을 창으로 줄여 응답 가운데 값 143 &rarr; 123ms, 응답 바이트는 전과 같다. 창의 첫 날을 시작일로
   * 쓰면 화면이 "1 년 전부터 있음" 으로 거짓을 말한다.
   */
  @Test
  void 시세는_창만_읽고_이력_시작일은_실제_첫_거래일이다() {
    UUID stockItemId = UUID.randomUUID();
    LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
    when(monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc())
        .thenReturn(List.of(profile(stockItemId, "MONTH_END", 1)));
    when(stockItemRepository.findAllById(any()))
        .thenReturn(List.of(stockItem(stockItemId, "489030", "A")));
    List<StockDailyClosePrice> window = new java.util.ArrayList<>();
    for (int ago = 400; ago >= 0; ago--) {
      window.add(
          new StockDailyClosePrice(stockItemId, today.minusDays(ago), new BigDecimal("100")));
    }
    org.mockito.ArgumentCaptor<LocalDate> from =
        org.mockito.ArgumentCaptor.forClass(LocalDate.class);
    when(stockDailyClosePriceQuery.findDailyClosePricesForItems(anyString(), from.capture()))
        .thenReturn(window);
    when(stockPriceHistoryRepository.findFirstTradeDatesForItems(anyString()))
        .thenReturn(List.of(new StockDailyClosePrice(stockItemId, today.minusDays(700), null)));

    List<MonthlyDividendCatalogResponse> rows = monthlyDividendCatalogService.findCatalog(null);

    assertThat(from.getValue()).as("가장 긴 기간 12 개월 + 여유 3 개월").isEqualTo(today.minusMonths(15));
    assertThat(rows.get(0).priceHistoryStartDate())
        .as("창의 첫 날(400 일 전)이 아니라 실제 첫 거래일(700 일 전)")
        .isEqualTo(today.minusDays(700));
  }
}
