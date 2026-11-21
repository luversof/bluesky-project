package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.service.analysis.StockAnalysisService.PredictionInput;

/** 규칙 예측: 야간선물 0.5% 이상이면 검증한 종목(삼성전자)만 야간선물 방향을 따른다. */
class StockAnalysisRuleTest {

  private static final List<String> SYMBOLS = List.of("005930", "000660");

  @Test
  void 크게_오른_날은_삼성전자만_UP_으로_D1_D5() {
    List<PredictionInput> p = StockAnalysisService.rulePredictions(new BigDecimal("1.82"), SYMBOLS);
    assertThat(p).extracting(PredictionInput::symbol).containsOnly("005930");
    assertThat(p).extracting(PredictionInput::horizonDays).containsExactly(1, 5);
    assertThat(p).extracting(PredictionInput::direction).containsOnly("UP");
    assertThat(p).extracting(PredictionInput::confidence).containsExactly(77, 55);
  }

  @Test
  void 덜_움직인_하락은_확신도를_낮춘다() {
    List<PredictionInput> p = StockAnalysisService.rulePredictions(new BigDecimal("-0.5"), SYMBOLS);
    assertThat(p).extracting(PredictionInput::direction).containsOnly("DOWN");
    assertThat(p.get(0).confidence()).isEqualTo(56);
  }

  @Test
  void 작게_움직였거나_값이_없으면_Claude_에_맡긴다() {
    assertThat(StockAnalysisService.rulePredictions(new BigDecimal("0.49"), SYMBOLS)).isEmpty();
    assertThat(StockAnalysisService.rulePredictions(new BigDecimal("-0.3"), SYMBOLS)).isEmpty();
    assertThat(StockAnalysisService.rulePredictions(null, SYMBOLS)).isEmpty();
  }

  @Test
  void 빠진_기준일은_예측이_없고_아침_분석_시각이_지난_날() {
    var d16 = java.time.LocalDate.of(2026, 11, 16);
    var d17 = java.time.LocalDate.of(2026, 11, 17);
    var d18 = java.time.LocalDate.of(2026, 11, 18);
    // 11-18 10:00 KST: 11-17 은 아침 분석 시각(11-18 08:00)이 지났고, 11-18 은 아직(장중 시세라 빼야 한다)
    var now = java.time.Instant.parse("2026-11-18T01:00:00Z");
    assertThat(StockAnalysisService.missing(List.of(d18, d17, d16), java.util.Set.of(d16), now))
        .containsExactly(d17);
  }

  @Test
  void 재분석은_그_버전_예측이나_재분석_회차가_있는_날만_덮였다고_본다() {
    var live = new net.luversof.api.stock.domain.StockPrediction();
    live.setPromptVersion("v9");
    assertThat(StockAnalysisService.coversVersion(live, "v9")).isTrue();
    assertThat(StockAnalysisService.coversVersion(live, "v10")).isFalse();
    // 규칙 날의 재분석은 promptVersion 이 rule 이어도 재분석 이름으로 덮인다(안 그러면 매일 다시 돈다)
    var rule = new net.luversof.api.stock.domain.StockPrediction();
    rule.setPromptVersion("rule");
    rule.setReplay("재분석 v10");
    assertThat(StockAnalysisService.coversVersion(rule, "v10")).isTrue();
  }

  @Test
  void 야간선물_변동_구간은_규칙_경계와_같다() {
    assertThat(StockAnalysisService.nightBand(new BigDecimal("-0.49"))).isEqualTo("0.0~0.5%");
    assertThat(StockAnalysisService.nightBand(new BigDecimal("0.5"))).isEqualTo("0.5~1.5%");
    assertThat(StockAnalysisService.nightBand(new BigDecimal("-1.5"))).isEqualTo("1.5%~");
    assertThat(StockAnalysisService.nightBand(null)).isEqualTo("n/a");
  }

  @Test
  void v10_D1_은_장전가_방향_D5_는_야간선물이_클_때만() {
    // 장전가 -0.8%, 야간선물 +1.2%: D+1 은 장전가(DOWN 57), D+5 는 야간선물 규칙(UP 55)
    List<PredictionInput> p =
        StockAnalysisService.rulePredictions(
            new BigDecimal("1.2"), new BigDecimal("-0.8"), SYMBOLS);
    assertThat(p).extracting(PredictionInput::symbol).containsOnly("005930");
    assertThat(p).extracting(PredictionInput::horizonDays).containsExactly(1, 5);
    assertThat(p).extracting(PredictionInput::direction).containsExactly("DOWN", "UP");
    assertThat(p).extracting(PredictionInput::confidence).containsExactly(58, 55);
  }

  @Test
  void v10_야간선물이_작으면_D1_만_내고_D5_는_Claude_에_맡긴다() {
    List<PredictionInput> p =
        StockAnalysisService.rulePredictions(new BigDecimal("0.2"), new BigDecimal("1.6"), SYMBOLS);
    assertThat(p).extracting(PredictionInput::horizonDays).containsExactly(1);
    assertThat(p.get(0).confidence()).isEqualTo(58);
    assertThat(
            StockAnalysisService.rulePredictions(
                    new BigDecimal("0.2"), new BigDecimal("0.1"), SYMBOLS)
                .get(0)
                .confidence())
        .isEqualTo(45);
  }

  @Test
  void v10_장전가가_없으면_야간선물_규칙() {
    assertThat(StockAnalysisService.rulePredictions(new BigDecimal("1.82"), null, SYMBOLS))
        .isEqualTo(StockAnalysisService.rulePredictions(new BigDecimal("1.82"), SYMBOLS));
    assertThat(StockAnalysisService.rulePredictions(new BigDecimal("0.3"), null, SYMBOLS))
        .isEmpty();
  }

  @Test
  void v10_확신도는_장전가_갭_크기별_적중에_맞춘다() {
    assertThat(
            StockAnalysisService.rulePredictions(null, new BigDecimal("-2.5"), SYMBOLS)
                .get(0)
                .confidence())
        .isEqualTo(88);
    assertThat(
            StockAnalysisService.rulePredictions(null, new BigDecimal("2.49"), SYMBOLS)
                .get(0)
                .confidence())
        .isEqualTo(58);
  }
}
