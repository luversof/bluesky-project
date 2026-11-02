package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesResult;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesSummary;

/**
 * 종목 · 계좌 상세 공용 합산 손익 모델(StockDetailViewController.addPeriodSummaryAttributes, 계좌 상세는 사용자 결정
 * 2026-10-29). 평가 몫은 기간 평가 변동(기말 - 기초), 세 몫 비율의 분모는 기초 평가액 + max(기간 순유입, 0).
 */
class DetailPeriodSummaryAttributesTest {

  private static TradeProfitTimeSeriesSummary summary(
      String opening, String principalDelta, String unrealizedStart, String unrealizedEnd) {
    return new TradeProfitTimeSeriesSummary(
        new BigDecimal(opening),
        null,
        null,
        null,
        null,
        new BigDecimal(principalDelta),
        new BigDecimal(unrealizedStart),
        new BigDecimal(unrealizedEnd),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        -5.26,
        null,
        null,
        null,
        null);
  }

  @Test
  void 평가_변동과_분모를_요약에서_낸다() {
    var model = new ExtendedModelMap();

    StockDetailViewController.addPeriodSummaryAttributes(
        model,
        new TradeProfitTimeSeriesResult(
            List.of(), summary("100000000", "30000000", "-5000000", "-21171894"), null, null));

    assertThat(model.get("periodUnrealizedDelta")).isEqualTo(new BigDecimal("-16171894"));
    assertThat(model.get("periodCapitalBase")).isEqualTo(new BigDecimal("130000000"));
    assertThat(model.get("periodProfitRatePct")).isEqualTo(-5.26);
    assertThat(model.get("periodBreakdown")).isEqualTo(List.of());
  }

  @Test
  void 순유출이면_분모는_기초_평가액만이다() {
    var model = new ExtendedModelMap();

    StockDetailViewController.addPeriodSummaryAttributes(
        model,
        new TradeProfitTimeSeriesResult(
            null, summary("100000000", "-40000000", "0", "0"), null, null));

    assertThat(model.get("periodCapitalBase")).isEqualTo(new BigDecimal("100000000"));
  }

  @Test
  void 결과가_없으면_표는_비고_상자_값은_없다() {
    var model = new ExtendedModelMap();

    StockDetailViewController.addPeriodSummaryAttributes(model, null);

    assertThat(model.get("periodBreakdown")).isEqualTo(List.of());
    assertThat(model.containsAttribute("periodUnrealizedDelta"))
        .as("템플릿 기본값(null)이 살아 비율 없이 금액만 그린다")
        .isFalse();
  }
}
