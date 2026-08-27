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
 * 자산성장 요약이 <b>고른 기간에 답하는 값</b>을 맨 위에 두는지 렌더해서 본다.
 *
 * <p>이 화면은 기간을 고르는 화면이다. 그런데 주지표가 평가손익이었고, 그 값은 <b>기간이 끝나는 날</b>만 따라간다 &mdash; 시작일을 아무리 바꿔도 그대로다.
 * 화면의 프리셋(이번달 · 올해 · 1년 · 3년 · 전체)은 끝나는 날이 모두 오늘이라, 무엇을 골라도 맨 위 큰 숫자가 꿈쩍하지 않았다.
 *
 * <p>실측 2026-08-24(실데이터, 끝나는 날 모두 2026-08-25):
 *
 * <pre>
 *   기간        평가손익(주지표)   기간 손익
 *   이번달       다섯 기간 모두     손실        &lt;- 손실인데 맨 위엔 큰 이익
 *   올해         완전히 같은 값     큰 이익
 *   최근 1년                      더 큰 이익
 *   최근 3년                      더 큰 이익
 *   전체                          가장 큰 이익
 * </pre>
 *
 * <p>평가손익도 끝나는 날을 바꾸면 움직인다(예: 2026-01-01~06-30 으로 잡으면 다른 값이 된다). 즉 틀린 값이 아니라 <b>기간을 재는 값이 아닌 것</b>이
 * 기간 화면의 주지표였다. 자리를 바꾸고, 내려간 값에는 무엇을 재는지 한 줄을 붙였다.
 *
 * <p>아래 모델 값은 실제 금액이 아니라 그 관계(기간 손익 = 기말 - 기초 = 평가손익 변화, 손실)를 그대로 지킨 표본이다.
 */
class AssetGrowthHeadlineMetricTest {

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

  /** 이번달과 같은 모양. 기간 손익은 손실이고 평가손익은 큰 이익인, 둘이 갈리는 상태. */
  private String render() {
    Map<String, Object> model = new HashMap<>();
    model.put("fromDate", "2026-08-01");
    model.put("toDate", "2026-08-25");
    model.put("periodReturnRatePct", -1.18d);
    model.put("returnCalculable", true);
    model.put("timeWeightedReturnPct", -1.17d);
    model.put("periodProfit", bd("-20000000"));
    model.put("principalDelta", bd("5000000"));
    model.put("unrealizedStart", bd("920000000"));
    model.put("unrealizedEnd", bd("900000000"));
    model.put("unrealizedEndPct", 144.75d);
    model.put("recoveredAmount", BigDecimal.ZERO);
    model.put("netNewProfit", bd("-20000000"));
    model.put("maxDrawdownPct", -57.04d);
    model.put("maxDrawdownPeakDate", LocalDate.parse("2015-03-06"));
    model.put("maxDrawdownTroughDate", LocalDate.parse("2016-01-21"));
    model.put("currentDrawdownPct", -20.84d);
    model.put("openingValue", bd("1700000000"));
    model.put("closingValue", bd("1680000000"));

    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  /** 가장 큰 글씨(text-4xl) 자리에 있는 값. */
  private String headlineAmount(String html) {
    int at = html.indexOf("text-4xl");
    assertThat(at).as("주지표 자리를 찾지 못했다 - 검사가 무력해진다").isGreaterThan(0);
    int close = html.indexOf("</span>", at);
    return html.substring(at, close);
  }

  @Test
  void 맨_위_큰_숫자가_기간_손익이다() {
    String html = render();

    assertThat(headlineAmount(html))
        .as("맨 위 큰 숫자가 기간 손익이 아니다 - 기간을 골라도 값이 안 움직인다")
        .contains("20,000,000")
        .doesNotContain("900,000,000");
  }

  @Test
  void 주지표_제목이_기간_손익이고_평가손익은_보조로_내려간다() {
    String html = render();

    String headlineTitle = MessageUtil.getMessage("stock.asset.growth.summary.period.profit");
    String demotedTitle = MessageUtil.getMessage("stock.profit.unrealized");
    assertThat(html)
        .as("두 제목을 모두 그려야 한다 - 값을 지운 것이 아니라 자리를 바꾼 것이다")
        .contains(headlineTitle)
        .contains(demotedTitle);
    assertThat(html.indexOf(headlineTitle))
        .as("평가손익이 기간 손익보다 위에 있다")
        .isLessThan(html.indexOf(demotedTitle));
    // 평가손익 값 자체는 남아 있어야 한다.
    assertThat(html).contains("900,000,000");
  }

  @Test
  void 내려간_평가손익이_무엇을_재는_값인지_밝힌다() {
    String html = render();

    assertThat(html)
        .as("기간이 아니라 기말 시점 값이라는 것을 밝히지 않는다")
        .contains(MessageUtil.getMessage("stock.asset.growth.summary.unrealized.desc"));
  }

  @Test
  void 기간_손익이_없으면_큰_자리에_안내를_낸다() {
    Map<String, Object> model = new HashMap<>();
    model.put("fromDate", "2026-08-01");
    model.put("toDate", "2026-08-25");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    String html = output.toString();

    assertThat(html)
        .as("값이 없을 때 빈칸이 되면 화면이 깨진 것처럼 보인다")
        .contains(MessageUtil.getMessage("stock.asset.growth.summary.period.return.unavailable"));
  }
}
