package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
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
import net.luversof.web.gate.stock.dto.response.TradeProfitPeriodSummary;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesSummary;

/**
 * 종목 상세의 <b>기간별 손익</b> 표를 렌더해서 본다.
 *
 * <p>합산 손익은 고른 기간 전체를 한 숫자로 답한다. 그래서 "언제 벌고 언제 잃었나" 는 차트를 눈으로 훑어야만 알 수 있었다. 구간별로 쪼개면 그 답이 표로 나온다.
 *
 * <p>쪼갠 값도 위 카드와 <b>같은 방식으로 나눈다</b> &mdash; 평가 변동 / (실현+배당). 두 자리가 다른 쪼갬을 쓰면 같은 화면에서 같은 뜻의 값이 서로 안
 * 맞는 것처럼 보인다. 실현·배당은 따로 오지 않으므로 {@code 손익 - 평가 변동} 잔차로 구한다.
 */
class StockItemPeriodBreakdownRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/stockItemPeriodBreakdown.jte";

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

  /** 화면에 찍히는 금액 문자열. 부호와 숫자 사이에 통화 기호가 들어간다(자산 성장 표와 같은 표기). */
  private static String won(String signed) {
    return signed.charAt(0) + "&#8361;" + signed.substring(1);
  }

  /**
   * 한 구간.
   *
   * @param profit 그 구간의 손익
   * @param unrealizedStart 기초 평가손익
   * @param unrealizedEnd 기말 평가손익
   */
  private static TradeProfitPeriodSummary row(
      String label,
      boolean complete,
      String profit,
      String unrealizedStart,
      String unrealizedEnd,
      Double ratePct) {
    TradeProfitTimeSeriesSummary summary =
        new TradeProfitTimeSeriesSummary(
            null,
            null,
            null,
            ratePct,
            bd(profit),
            null,
            bd(unrealizedStart),
            bd(unrealizedEnd),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    return new TradeProfitPeriodSummary(
        "MONTH",
        label,
        LocalDate.parse(label + "-01"),
        LocalDate.parse(label + "-28"),
        complete,
        summary);
  }

  private String render(List<TradeProfitPeriodSummary> rows) {
    Map<String, Object> model = new HashMap<>();
    model.put("periodBreakdown", rows);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  /** 8월: 손익 +300,000, 평가 변동 +200,000 → 실현+배당 +100,000. 7월은 손실 구간. */
  private List<TradeProfitPeriodSummary> months() {
    return List.of(
        row("2026-08", false, "300000", "500000", "700000", 12.34d),
        row("2026-07", true, "-150000", "650000", "500000", -5.67d));
  }

  @Test
  void 구간마다_한_줄씩_손익을_낸다() {
    String html = render(months());

    assertThat(html)
        .as("고른 기간이 한 숫자로만 답하면 언제 벌고 잃었는지 알 수 없다")
        .contains(MessageUtil.getMessage("stock.item.detail.breakdown.title"))
        .contains("2026-08")
        .contains("2026-07")
        .contains(won("+300,000"))
        .contains(won("-150,000"));
  }

  @Test
  void 손익을_평가_변동과_실현_배당으로_쪼갠다() {
    String html = render(months());

    // 8월: 평가 변동 700,000 - 500,000 = +200,000, 실현+배당 = 300,000 - 200,000 = +100,000
    assertThat(html)
        .as("위 카드와 같은 쪼갬이 아니면 같은 화면의 두 자리가 안 맞는 것처럼 보인다")
        .contains(won("+200,000"))
        .contains(won("+100,000"));
    // 7월: 평가 변동 500,000 - 650,000 = -150,000, 실현+배당 = 0
    assertThat(html).contains(won("-150,000"));
  }

  @Test
  void 수익률도_구간마다_낸다() {
    String html = render(months());

    assertThat(html).contains("+12.34%").contains("-5.67%");
  }

  /** 조회 기간에 잘린 구간은 그 사실을 밝힌다. 안 밝히면 그 달 전체 성과로 오해한다. */
  @Test
  void 온전하지_않은_구간을_밝힌다() {
    String html = render(months());

    assertThat(html).contains(MessageUtil.getMessage("stock.item.detail.breakdown.partial"));
    // 온전한 7월 줄에는 붙지 않는다.
    int august = html.indexOf("2026-08");
    int july = html.indexOf("2026-07");
    assertThat(july).isGreaterThan(august);
    assertThat(html.substring(july))
        .as("온전한 구간에까지 '일부 구간' 을 붙이면 표기가 무의미해진다")
        .doesNotContain(MessageUtil.getMessage("stock.item.detail.breakdown.partial"));
  }

  /**
   * 구간이 하나뿐이면 그리지 않는다.
   *
   * <p>'이번달' 을 고르면 달 단위 쪼갬이 한 줄이 되는데, 그 줄은 바로 위 합산 손익을 그대로 되풀이할 뿐이다. 같은 값을 두 번 적으면 읽는 사람이 둘을 견주려다
   * 시간을 쓴다.
   */
  @Test
  void 구간이_하나뿐이면_표를_그리지_않는다() {
    String html = render(List.of(row("2026-08", false, "300000", "500000", "700000", 12.34d)));

    assertThat(html.trim())
        .as("한 줄짜리 표는 위의 합산 손익을 되풀이할 뿐이다")
        .doesNotContain(MessageUtil.getMessage("stock.item.detail.breakdown.title"));
  }

  @Test
  void 쪼갬이_없으면_아무것도_그리지_않는다() {
    assertThat(render(List.of()).trim()).isEmpty();
  }
}
