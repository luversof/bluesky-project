package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;

/**
 * 자산성장 요약이 <b>서로 다른 세 비율을 한자리에</b> 모으고, 차트에만 있던 고점/저점을 카드에도 적는지 렌더해서 본다.
 *
 * <p>예전 카드에는 비율이 둘이었고 서로 다른 칸에 흩어져 있었다 &mdash; 투자 수익률(TWR)과 자산 증가율. 헤드라인인 기간 손익은 <b>금액만</b> 있어서 "그
 * 금액이 몇 %인지" 를 화면에서 알 수 없었고, 옆의 두 비율 중 어느 쪽이 그 금액의 비율인지도 알 수 없었다.
 *
 * <p>셋은 <b>분모가 다른 별개의 값</b>이다. 실측 2026-08-27(올해): 기간 손익률 94.93% / 투자 수익률 92.88% / 자산 증가율 76.66%.
 * 나란히 두고 각자의 분모를 한 줄로 적는다.
 *
 * <p>고점/저점은 차트 주석에만 있었다 &mdash; 차트를 눈으로 훑어야만 알 수 있었다. 서버 요약이 같은 규칙(평가액 0 인 날 제외)으로 계산해 내려주므로 카드와
 * 차트가 같은 지점을 가리킨다.
 */
class AssetGrowthRateGroupingTest {

  private static final String TEMPLATE = "stock/htmx/fragments/assetGrowthPeriodReturnSummary.jte";

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  /** '올해' 와 같은 모양: 세 비율이 모두 다르고, 기간 중 고점이 지금보다 높다. */
  private Map<String, Object> model() {
    Map<String, Object> model = new HashMap<>();
    model.put("fromDate", "2026-01-01");
    model.put("toDate", "2026-08-27");
    model.put("periodReturnRatePct", 76.66d);
    model.put("returnCalculable", true);
    model.put("timeWeightedReturnPct", 92.88d);
    model.put("periodProfitRatePct", 94.93d);
    model.put("periodProfit", bd("800000000"));
    model.put("principalDelta", bd("5000000"));
    model.put("unrealizedStart", bd("100000000"));
    model.put("unrealizedEnd", bd("900000000"));
    model.put("unrealizedEndPct", 144.75d);
    model.put("recoveredAmount", BigDecimal.ZERO);
    model.put("netNewProfit", bd("800000000"));
    model.put("maxDrawdownPct", -57.04d);
    model.put("maxDrawdownPeakDate", LocalDate.parse("2015-03-06"));
    model.put("maxDrawdownTroughDate", LocalDate.parse("2016-01-21"));
    model.put("currentDrawdownPct", -20.84d);
    model.put("openingValue", bd("1000000000"));
    model.put("closingValue", bd("1500000000"));
    model.put("peakValue", bd("2000000000"));
    model.put("peakValueDate", LocalDate.parse("2026-06-17"));
    model.put("troughValue", bd("800000000"));
    model.put("troughValueDate", LocalDate.parse("2025-12-31"));
    return model;
  }

  private String render(Map<String, Object> model) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  @Test
  void 헤드라인_금액_옆에_손익률이_붙는다() {
    String html = render(model());

    int headline = html.indexOf("text-4xl");
    assertThat(headline).as("주지표 자리를 찾지 못했다 - 검사가 무력해진다").isGreaterThan(0);
    int ratesSection =
        html.indexOf(MessageUtil.getMessage("stock.asset.growth.summary.rates.title"));
    assertThat(ratesSection).as("수익률 절을 찾지 못했다").isGreaterThan(0);
    assertThat(html.substring(headline, ratesSection))
        .as("금액만 있으면 그 금액이 많이 번 것인지 화면에서 알 수 없다")
        .contains("+94.93%");
  }

  @Test
  void 세_비율이_한_절에_나란히_있고_각자의_분모를_적는다() {
    String html = render(model());

    int ratesSection =
        html.indexOf(MessageUtil.getMessage("stock.asset.growth.summary.rates.title"));
    int movementSection =
        html.indexOf(MessageUtil.getMessage("stock.asset.growth.summary.movement.title"));
    assertThat(movementSection).as("움직임 절을 찾지 못했다").isGreaterThan(ratesSection);

    String rates = html.substring(ratesSection, movementSection);
    assertThat(rates)
        .as("세 비율이 한 절에 모이지 않으면 무엇이 다른지 화면에서 비교할 수 없다")
        .contains("+94.93%")
        .contains("+92.88%")
        .contains("+76.66%");
    assertThat(rates)
        .as("분모를 적지 않으면 셋이 왜 다른지 알 수 없다")
        .contains(MessageUtil.getMessage("stock.asset.growth.summary.profit.rate.desc"))
        .contains(MessageUtil.getMessage("stock.asset.growth.summary.twr.desc"))
        .contains(MessageUtil.getMessage("stock.asset.growth.summary.period.growth.desc"));
  }

  @Test
  void 기간_고점과_저점을_날짜와_지금_대비로_적는다() {
    String html = render(model());

    assertThat(html)
        .as("차트를 눈으로 훑어야만 알 수 있던 값이라 카드에 적는다")
        .contains(MessageUtil.getMessage("stock.asset.growth.summary.period.high"))
        .contains(MessageUtil.getMessage("stock.asset.growth.summary.period.low"))
        .contains("2,000,000,000")
        .contains("800,000,000")
        .contains("2026-06-17")
        .contains("2025-12-31");
    // 지금(15억) 대비 고점(20억) = -25.00%, 저점(8억) = +87.50%
    assertThat(html).as("고점 대비 현재 위치를 적지 않는다").contains("-25.00%");
    assertThat(html).as("저점 대비 현재 위치를 적지 않는다").contains("+87.50%");
  }

  /**
   * 저점이 첫 매수일이면 비율이 수십만 %가 되어 읽히지 않는다. 그럴 때는 배수로 답한다.
   *
   * <p>실측 2026-08-27 '전체' 기간: 최저 평가액 대비 +400,291% &mdash; 자릿수만 세다 끝난다. 약 4,004 배라고 적는 편이 읽힌다.
   */
  @Test
  void 비율이_너무_크면_배수로_적는다() {
    Map<String, Object> model = model();
    model.put("closingValue", bd("1000000000"));
    model.put("troughValue", bd("250000"));

    String html = render(model);

    assertThat(html).as("네 자리 %는 자릿수만 세게 된다").contains("x4,000").doesNotContain("+400000.00%");
  }

  /** 보유가 한 번도 없던 구간이면 고점/저점 자리가 비어야 한다. 0 원을 그리면 틀린 기준점이 된다. */
  @Test
  void 고점과_저점이_없으면_그리지_않는다() {
    Map<String, Object> model = model();
    model.put("peakValue", null);
    model.put("peakValueDate", null);
    model.put("troughValue", null);
    model.put("troughValueDate", null);

    String html = render(model);

    assertThat(html)
        .as("최대 낙폭이 있으면 움직임 절 자체는 남는다")
        .contains(MessageUtil.getMessage("stock.asset.growth.summary.movement.title"));
    assertThat(html).doesNotContain("2026-06-17");
  }

  /** 평가손익은 기간을 재는 값이 아니므로 세 비율보다 아래에 있어야 한다. */
  @Test
  void 평가손익은_비율_절보다_아래에_있다() {
    String html = render(model());

    assertThat(html.indexOf(MessageUtil.getMessage("stock.asset.growth.summary.rates.title")))
        .as("기간을 재지 않는 값이 비율보다 위에 있으면 다시 주지표처럼 읽힌다")
        .isLessThan(html.lastIndexOf(MessageUtil.getMessage("stock.profit.unrealized")));
    assertThat(html)
        .contains(MessageUtil.getMessage("stock.asset.growth.summary.unrealized.desc"))
        .contains("900,000,000");
  }
}
