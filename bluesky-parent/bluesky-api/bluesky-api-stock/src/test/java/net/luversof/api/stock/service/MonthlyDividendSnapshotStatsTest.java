package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.luversof.api.stock.domain.MonthlyDividendPayout;
import net.luversof.api.stock.repository.MonthlyDividendPayoutRepository;
import net.luversof.api.stock.service.MonthlyDividendPayoutService.SnapshotStats;

/**
 * 월배당 기준 데이터 카드의 세 수(최신 · 1년 평균 · 1년 평균 과세 표준 비중)를 만드는 계산.
 *
 * <p>실측 2026-09-12: 화면의 세 값은 지급 이력과 정확히 맞았다(KODEX 한국부동산리츠인프라, 이력 29 행, 최신 29 원, 1년 평균 30.0833 원, 과세
 * 비중 82.79%). 그때 이 계산에는 테스트가 없었다 - 이 값들은 시뮬레이터의 예상 배당과 과세표준으로 그대로 흘러가므로 조용히 틀리면 아래 화면 전체가 같이 틀린다.
 *
 * <p>'1년'은 <b>최근 12건</b>이다. 실측 2026-09-12(프로필 8 종목): 최근 12건이 덮는 기간은 302~336일로 실제로 1년에 가깝다 - 이름에
 * '위클리'가 붙은 종목들도 지급은 달마다 한다. 이력이 12건보다 적으면 있는 만큼으로 나눈다(그 사실은 화면이 따로 알린다).
 */
@ExtendWith(MockitoExtension.class)
class MonthlyDividendSnapshotStatsTest {

  @Mock private MonthlyDividendPayoutRepository monthlyDividendPayoutRepository;

  @InjectMocks private MonthlyDividendPayoutService monthlyDividendPayoutService;

  private final UUID stockItemId = UUID.randomUUID();

  private MonthlyDividendPayout payout(String payDate, String perShare, String taxableBase) {
    MonthlyDividendPayout payout = new MonthlyDividendPayout();
    payout.setId(UUID.randomUUID());
    payout.setStockItemId(stockItemId);
    payout.setRecordDate(LocalDate.parse(payDate).minusDays(2));
    payout.setPayDate(payDate == null ? null : LocalDate.parse(payDate));
    payout.setDistributionRatePct(BigDecimal.ZERO);
    payout.setDividendAmountPerShare(new BigDecimal(perShare));
    payout.setTaxableBasePerShare(new BigDecimal(taxableBase));
    payout.setCreatedDate(Instant.parse("2026-01-01T00:00:00Z"));
    payout.setUpdatedDate(Instant.parse("2026-01-02T00:00:00Z"));
    return payout;
  }

  private SnapshotStats statsOf(List<MonthlyDividendPayout> rows) {
    when(monthlyDividendPayoutRepository.findByStockItemIdOrderByPayDateDescRecordDateDesc(
            stockItemId))
        .thenReturn(rows);
    return monthlyDividendPayoutService.computeSnapshotStats(stockItemId);
  }

  /** 최신 한 건과 평균은 다른 값이다 - 최신만 보고 평균을 채우면 최근 증액이 과거로 번진다. */
  @Test
  void 최신은_첫_줄이고_평균은_열두_줄이다() {
    List<MonthlyDividendPayout> rows = new ArrayList<>();
    rows.add(payout("2026-09-02", "40", "20"));
    for (int i = 1; i < 12; i++) {
      rows.add(payout(LocalDate.of(2026, 9, 2).minusMonths(i).toString(), "30", "15"));
    }

    SnapshotStats stats = statsOf(rows);
    assertThat(stats.asOfDate()).isEqualTo(LocalDate.of(2026, 9, 2));
    assertThat(stats.latestPerShare()).isEqualByComparingTo(new BigDecimal("40"));
    // (40 + 30 x 11) / 12 = 30.8333
    assertThat(stats.averagePerShare1y()).isEqualByComparingTo(new BigDecimal("30.8333"));
    assertThat(stats.taxableBaseRatio1y()).isEqualByComparingTo(new BigDecimal("50.00"));
  }

  /** 열두 건이 넘으면 오래된 건은 평균에 들어오지 않는다. */
  @Test
  void 열세_번째부터는_평균에_들어오지_않는다() {
    List<MonthlyDividendPayout> rows = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      rows.add(payout(LocalDate.of(2026, 9, 2).minusMonths(i).toString(), "30", "15"));
    }
    rows.add(payout("2025-08-02", "1000", "1000"));

    SnapshotStats stats = statsOf(rows);
    assertThat(stats.averagePerShare1y()).isEqualByComparingTo(new BigDecimal("30"));
    assertThat(stats.taxableBaseRatio1y()).isEqualByComparingTo(new BigDecimal("50.00"));
  }

  /** 이력이 열두 건보다 적으면 있는 만큼으로 나눈다 - 12 로 나누면 평균이 낮게 나온다. */
  @Test
  void 이력이_적으면_있는_만큼으로_나눈다() {
    List<MonthlyDividendPayout> rows =
        List.of(payout("2026-09-02", "30", "15"), payout("2026-08-02", "60", "45"));

    SnapshotStats stats = statsOf(rows);
    assertThat(stats.averagePerShare1y())
        .as("(30 + 60) / 2")
        .isEqualByComparingTo(new BigDecimal("45"));
    // (50.00 + 75.00) / 2
    assertThat(stats.taxableBaseRatio1y()).isEqualByComparingTo(new BigDecimal("62.50"));
  }

  /**
   * 배당이 0 인 달은 평균은 낮추지만 과세 비중에서는 빠진다.
   *
   * <p>0 으로 나눌 수 없어서이기도 하고, 준 적 없는 달의 과세 비중은 0% 가 아니라 <b>없는 값</b>이라서다.
   */
  @Test
  void 배당이_0인_달은_평균에는_들어가고_비중에서는_빠진다() {
    List<MonthlyDividendPayout> rows =
        List.of(
            payout("2026-09-02", "30", "15"),
            payout("2026-08-02", "0", "0"),
            payout("2026-07-02", "30", "30"));

    SnapshotStats stats = statsOf(rows);
    assertThat(stats.averagePerShare1y())
        .as("(30 + 0 + 30) / 3")
        .isEqualByComparingTo(new BigDecimal("20"));
    assertThat(stats.taxableBaseRatio1y())
        .as("(50 + 100) / 2")
        .isEqualByComparingTo(new BigDecimal("75.00"));
  }

  /** 지급일이 비어 있으면 기준일로 답한다 - 날짜 없이 내보내면 화면이 기준을 말할 수 없다. */
  @Test
  void 지급일이_없으면_기준일을_쓴다() {
    MonthlyDividendPayout noPayDate = payout("2026-09-02", "30", "15");
    noPayDate.setPayDate(null);

    SnapshotStats stats = statsOf(List.of(noPayDate));
    assertThat(stats.asOfDate()).isEqualTo(LocalDate.of(2026, 8, 31));
  }

  /** 이력이 없으면 값을 지어내지 않는다. */
  @Test
  void 이력이_없으면_널이다() {
    assertThat(statsOf(List.of())).isNull();
    assertThat(monthlyDividendPayoutService.computeSnapshotStats(null)).isNull();
  }
}
