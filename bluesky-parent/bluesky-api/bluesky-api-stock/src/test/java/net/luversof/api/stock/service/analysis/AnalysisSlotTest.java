package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.service.analysis.StockAnalysisService.SlotDate;

/** 분석 시점(2026-11-27): 아무 때나 돌린 예측을 만든 시각으로 네 시점 · 장중에 나눈다. */
class AnalysisSlotTest {

  private static final LocalDate THU = LocalDate.of(2026, 11, 26);
  private static final LocalDate FRI = LocalDate.of(2026, 11, 27);

  private static Instant kst(String localDateTime) {
    return java.time.LocalDateTime.parse(localDateTime)
        .atZone(PredictionMath.MARKET_ZONE_ID)
        .toInstant();
  }

  @Test
  void 만든_시각으로_시점을_정한다() {
    assertThat(AnalysisSlot.current(THU, kst("2026-11-26T15:45"))).isEqualTo(AnalysisSlot.CLOSE);
    assertThat(AnalysisSlot.current(THU, kst("2026-11-26T18:05"))).isEqualTo(AnalysisSlot.CLOSE);
    assertThat(AnalysisSlot.current(THU, kst("2026-11-26T20:00"))).isEqualTo(AnalysisSlot.EVENING);
    assertThat(AnalysisSlot.current(THU, kst("2026-11-27T01:48"))).isEqualTo(AnalysisSlot.EVENING);
    assertThat(AnalysisSlot.current(THU, kst("2026-11-27T06:10"))).isEqualTo(AnalysisSlot.NIGHT);
    assertThat(AnalysisSlot.current(THU, kst("2026-11-27T08:30")))
        .isEqualTo(AnalysisSlot.PREMARKET);
    assertThat(AnalysisSlot.current(THU, kst("2026-11-27T10:00"))).isEqualTo(AnalysisSlot.INTRADAY);
  }

  @Test
  void 금요일_기준일은_주말_내내_장전_시점이고_월요일_장이_열리면_장중() {
    assertThat(AnalysisSlot.current(FRI, kst("2026-11-29T15:00")))
        .isEqualTo(AnalysisSlot.PREMARKET);
    assertThat(AnalysisSlot.current(FRI, kst("2026-11-30T09:01"))).isEqualTo(AnalysisSlot.INTRADAY);
  }

  @Test
  void 채점된_예측은_늦음_표시로_장중을_가른다() {
    // 다음 평일이 휴장이면 그날 10:00 예측은 늦지 않다 - 채점 결과(late=false)를 따른다
    assertThat(AnalysisSlot.of(THU, kst("2026-11-27T10:00"), false))
        .isEqualTo(AnalysisSlot.PREMARKET);
    assertThat(AnalysisSlot.of(THU, kst("2026-11-27T10:00"), true))
        .isEqualTo(AnalysisSlot.INTRADAY);
    assertThat(AnalysisSlot.of(THU, kst("2026-11-27T10:00"), null))
        .isEqualTo(AnalysisSlot.INTRADAY);
    // 2026-11-27 전의 리플레이: 저녁 20:00 · 아침 08:30
    assertThat(AnalysisSlot.of(THU, AnalysisSlot.EVENING.at(THU), false))
        .isEqualTo(AnalysisSlot.EVENING);
    assertThat(AnalysisSlot.of(THU, AnalysisSlot.PREMARKET.at(THU), false))
        .isEqualTo(AnalysisSlot.PREMARKET);
  }

  @Test
  void 빈_시점은_최근_날짜부터_네_시점_순서로_시각이_지난_것만() {
    LocalDate wed = THU.minusDays(1);
    Set<SlotDate> covered = Set.of(new SlotDate(wed, AnalysisSlot.PREMARKET));
    // 11-27 07:00: 목요일은 CLOSE · EVENING · NIGHT 까지, PREMARKET 은 금요일 08:30 이 지나야
    List<SlotDate> missing =
        StockAnalysisService.missingSlots(List.of(THU, wed), covered, kst("2026-11-27T07:00"));
    assertThat(missing)
        .containsExactly(
            new SlotDate(THU, AnalysisSlot.CLOSE),
            new SlotDate(THU, AnalysisSlot.EVENING),
            new SlotDate(THU, AnalysisSlot.NIGHT),
            new SlotDate(wed, AnalysisSlot.CLOSE),
            new SlotDate(wed, AnalysisSlot.EVENING),
            new SlotDate(wed, AnalysisSlot.NIGHT));
  }
}
