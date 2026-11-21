package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.service.analysis.StockAnalysisService.ShadowRow;

/** 장전가 그림자 집계: NXT 08:49 가격 수집과 채점. */
class PremarketShadowTest {

  private static LocalDate d(String s) {
    return LocalDate.parse(s);
  }

  private static BigDecimal n(String s) {
    return new BigDecimal(s);
  }

  @Test
  void 장전_가격은_그날_0850_전_마지막_분봉() {
    List<Map<String, Object>> bars =
        List.of(
            Map.of("stck_bsop_date", "20261120", "stck_cntg_hour", "084900", "stck_prpr", "188100"),
            Map.of("stck_bsop_date", "20261120", "stck_cntg_hour", "080000", "stck_prpr", "190800"),
            Map.of("stck_bsop_date", "20261120", "stck_cntg_hour", "085000", "stck_prpr", "1"),
            Map.of("stck_bsop_date", "20261119", "stck_cntg_hour", "084900", "stck_prpr", "2"));
    assertThat(MarketDailyCollectService.premarketPrice(bars, d("2026-11-20")))
        .isEqualByComparingTo("188100");
    assertThat(MarketDailyCollectService.premarketPrice(bars, d("2026-11-18"))).isNull();
    // 아침 분석(v10)은 08:30 전 마지막 봉
    assertThat(MarketDailyCollectService.premarketPriceBefore(bars, d("2026-11-20"), "083000"))
        .isEqualByComparingTo("190800");
    assertThat(MarketContextService.nextWeekday(d("2026-11-20"))).isEqualTo(d("2026-11-23"));
  }

  @Test
  void 장전_거래가_끝나지_않은_오늘과_주말은_묻지_않는다() {
    var at0840 = ZonedDateTime.of(2026, 11, 23, 8, 40, 0, 0, ZoneId.of("Asia/Seoul"));
    // 11-21 토 · 11-22 일은 빼고, 월요일 11-23 은 08:50 전이라 아직
    assertThat(MarketDailyCollectService.premarketDates(d("2026-11-20"), d("2026-11-23"), at0840))
        .containsExactly(d("2026-11-20"));
    assertThat(
            MarketDailyCollectService.premarketDates(
                d("2026-11-20"), d("2026-11-23"), at0840.withHour(9)))
        .containsExactly(d("2026-11-20"), d("2026-11-23"));
  }

  @Test
  void 장전가_방향으로_채점하고_표본_밖과_운영을_나눠_센다() {
    TreeMap<LocalDate, BigDecimal> closes = new TreeMap<>();
    closes.put(d("2026-11-20"), n("100"));
    closes.put(d("2026-11-23"), n("102")); // +2% UP (IN 의 마지막 기준일 다음)
    closes.put(d("2026-11-24"), n("101.8")); // -0.2% FLAT
    closes.put(d("2026-11-25"), n("100")); // -1.8% DOWN
    Map<LocalDate, BigDecimal> premarket =
        Map.of(
            d("2026-11-23"), n("101"), // 장전 UP, 실제 UP -> 적중 (IN)
            d("2026-11-24"), n("103"), // 장전 UP, 실제 FLAT -> 실패 (OOS)
            d("2026-11-25"), n("101"), // 장전 DOWN, 실제 DOWN -> 적중 (OOS)
            d("2026-11-26"), n("99")); // 종가 전 - 채점 안 함
    // 야간선물 변동은 기준일(앞 거래일) 기준: 11-23 의 기준일 11-20 은 값 없음
    Map<LocalDate, BigDecimal> moves =
        Map.of(d("2026-11-23"), n("0.2"), d("2026-11-24"), n("-1.0"));
    // 운영 예측은 기준일 11-24 만(실패)
    Map<LocalDate, Boolean> system = Map.of(d("2026-11-24"), false);

    List<ShadowRow> rows = StockAnalysisService.shadowRows(closes, premarket, moves, system);

    assertThat(rows)
        .extracting(r -> r.period() + " " + r.band())
        .containsExactly("OOS all", "OOS 0.0~0.5%", "OOS 0.5~1.5%", "IN all", "IN n/a");
    ShadowRow oos = rows.get(0);
    assertThat(oos.days()).isEqualTo(2);
    assertThat(oos.hit()).isEqualTo(1);
    // 11-25 (기준일 11-24) 만 운영 예측이 있다: 장전 적중 · 운영 실패
    assertThat(oos.pairedDays()).isEqualTo(1);
    assertThat(oos.pairedHit()).isEqualTo(1);
    assertThat(oos.pairedSystemHit()).isZero();
    assertThat(rows.get(3).hit()).isEqualTo(1);
  }
}
