package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.YearlyDividendIncome;
import net.luversof.api.stock.domain.YearlyTradeCost;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.web.dto.response.YearlyCostSummary;

/**
 * 연도별 세금·비용은 두 질의를 해마다 하나의 줄로 합친다.
 *
 * <p>매매 쪽과 배당 쪽이 따로 오므로, 한 해에 둘 중 하나만 있는 경우가 흔하다 - 배당만 있는 해(아직 팔지 않은 해)와 매매만 있는 해(배당을 주지 않는 종목만 가진
 * 해) 둘 다 실제로 있다. 합치는 자리에서 한쪽을 덮어쓰거나 빠뜨리면 그 해 줄의 절반이 0 으로 나가는데, 0 은 "없었다"로 읽혀 화면에서는 틀린 줄로 보이지 않는다.
 *
 * <p>실측 2026-09-12(운영 데이터 14 해): 연도별 합계는 같은 기간의 기간 요약과 세 항목(거래 수수료 100,671 · 거래세 1,932,821 · 배당세
 * 7,770,960) 모두 정확히 일치했다. 이 테스트는 그 합치는 규칙을 저장소 없이 고정한다.
 */
class YearlyCostMergeTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final UUID USER = UUID.randomUUID();

  private TradeRepository tradeRepository;
  private DividendRepository dividendRepository;
  private YearlyCostService service;

  private BigDecimal won(String value) {
    return new BigDecimal(value);
  }

  @BeforeEach
  void setUp() {
    tradeRepository = mock(TradeRepository.class);
    dividendRepository = mock(DividendRepository.class);
    service = new YearlyCostService();
    service.setTradeRepository(tradeRepository);
    service.setDividendRepository(dividendRepository);
  }

  private void given(List<YearlyTradeCost> trades, List<YearlyDividendIncome> dividends) {
    when(tradeRepository.findYearlyCost(any(), any(), any(), any(), any(), any()))
        .thenReturn(trades);
    when(dividendRepository.findYearlyIncome(any(), any(), any(), any(), any(), any()))
        .thenReturn(dividends);
  }

  private YearlyCostSummary yearOf(List<YearlyCostSummary> rows, int year) {
    return rows.stream().filter(r -> r.year() == year).findFirst().orElseThrow();
  }

  @Test
  void 한_해에_매매와_배당이_모두_있으면_한_줄로_합친다() {
    given(
        List.of(new YearlyTradeCost(2025, won("1000"), won("2000"), won("30000"), 7L)),
        List.of(
            new YearlyDividendIncome(2025, won("50000"), won("40000"), won("6000"), won("500"))));

    YearlyCostSummary row = yearOf(service.findYearlyCost(USER, null, null, KST), 2025);
    assertThat(row.tradeFee()).isEqualByComparingTo(won("1000"));
    assertThat(row.tradeTax()).isEqualByComparingTo(won("2000"));
    assertThat(row.realizedProfit()).isEqualByComparingTo(won("30000"));
    assertThat(row.sellCount()).isEqualTo(7L);
    assertThat(row.dividendGross()).isEqualByComparingTo(won("50000"));
    assertThat(row.dividendTaxable()).isEqualByComparingTo(won("40000"));
    assertThat(row.dividendTax()).isEqualByComparingTo(won("6000"));
    // 세후 = 세전 - 세금 - 수수료
    assertThat(row.dividendNet()).isEqualByComparingTo(won("43500"));
  }

  /** 배당만 있는 해도 줄이 나와야 한다 - 매매 칸은 0 이고 매도 건수도 0 이다. */
  @Test
  void 배당만_있는_해도_줄이_나온다() {
    given(
        List.of(new YearlyTradeCost(2025, won("1000"), won("2000"), won("30000"), 7L)),
        List.of(
            new YearlyDividendIncome(
                2024, won("9000"), won("8000"), won("1200"), BigDecimal.ZERO)));

    List<YearlyCostSummary> rows = service.findYearlyCost(USER, null, null, KST);
    assertThat(rows).hasSize(2);
    YearlyCostSummary only = yearOf(rows, 2024);
    assertThat(only.dividendGross()).isEqualByComparingTo(won("9000"));
    assertThat(only.dividendNet()).isEqualByComparingTo(won("7800"));
    assertThat(only.tradeFee()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(only.tradeTax()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(only.realizedProfit()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(only.sellCount()).as("배당만 있는 해의 매도 건수").isZero();
  }

  /** 매매만 있는 해도 마찬가지로 배당 칸이 0 이다. */
  @Test
  void 매매만_있는_해도_줄이_나온다() {
    given(
        List.of(new YearlyTradeCost(2023, won("10"), won("20"), won("-500"), 1L)),
        List.of(
            new YearlyDividendIncome(
                2024, won("9000"), won("8000"), won("1200"), BigDecimal.ZERO)));

    YearlyCostSummary only = yearOf(service.findYearlyCost(USER, null, null, KST), 2023);
    assertThat(only.realizedProfit()).as("손실도 그대로 나간다").isEqualByComparingTo(won("-500"));
    assertThat(only.sellCount()).isEqualTo(1L);
    assertThat(only.dividendGross()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(only.dividendNet()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  /** 표는 늘 최근부터 읽는다. */
  @Test
  void 최근_해가_위에_온다() {
    given(
        List.of(
            new YearlyTradeCost(2023, won("10"), won("20"), won("30"), 1L),
            new YearlyTradeCost(2026, won("40"), won("50"), won("60"), 2L)),
        List.of(new YearlyDividendIncome(2024, won("70"), won("60"), won("10"), BigDecimal.ZERO)));

    List<YearlyCostSummary> rows = service.findYearlyCost(USER, null, null, KST);
    assertThat(rows.stream().map(YearlyCostSummary::year).toList())
        .containsExactly(2026, 2024, 2023);
  }

  /** 널은 0 으로 읽어야 한다 - 한 칸이라도 널이면 그 해 줄 전체가 널로 터진다. */
  @Test
  void 널은_0으로_읽는다() {
    given(
        List.of(new YearlyTradeCost(2025, null, null, null, 0L)),
        List.of(new YearlyDividendIncome(2025, null, null, null, null)));

    YearlyCostSummary row = yearOf(service.findYearlyCost(USER, null, null, KST), 2025);
    assertThat(row.tradeFee()).as("거래 수수료").isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(row.tradeTax()).as("거래세").isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(row.realizedProfit()).as("실현손익").isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(row.dividendGross()).as("배당 세전").isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(row.dividendTaxable()).as("배당 과세표준").isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(row.dividendTax()).as("배당세").isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(row.dividendNet()).as("배당 세후").isEqualByComparingTo(BigDecimal.ZERO);
  }
}
