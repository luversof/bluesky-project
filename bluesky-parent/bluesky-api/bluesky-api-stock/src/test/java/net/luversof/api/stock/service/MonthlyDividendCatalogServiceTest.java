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

  private static MonthlyDividendPayout payout(LocalDate recordDate, LocalDate payDate) {
    MonthlyDividendPayout payout = new MonthlyDividendPayout();
    payout.setRecordDate(recordDate);
    payout.setPayDate(payDate);
    payout.setDividendAmountPerShare(new BigDecimal("300"));
    payout.setTaxableBasePerShare(new BigDecimal("37.5"));
    return payout;
  }
}
