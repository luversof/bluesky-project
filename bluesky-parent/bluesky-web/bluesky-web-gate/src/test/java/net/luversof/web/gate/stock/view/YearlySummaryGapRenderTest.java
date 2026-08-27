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
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesSummary;
import net.luversof.web.gate.stock.dto.response.TradeProfitYearlySummary;

/**
 * 연도별 표가 <b>건너뛴 해</b>를 밝히는지 렌더해서 본다.
 *
 * <p>이 표는 보유도 거래도 없던 해를 아예 내지 않는다. 그래서 연도가 건너뛴 자리가 생기는데, 그대로 두면 자료가 빠진 것처럼 보인다.
 *
 * <p>실측 2026-08-24(실데이터): 행이 2009 · 2010 · <b>2014</b> · 2015 &hellip; 로 이어지고 기말이 0 &rarr; 0 에서 갑자기
 * 뛴다. 2011~2013 은 거래 0 건 · 배당 0 건 · 보유 스냅샷 빈 배열로 실제로 비어 있는 해다(원장으로 확인). 즉 자료 누락이 아니라 "그 해엔 아무것도
 * 없었다"인데, 표만 보면 가릴 수 없다.
 *
 * <p>거래가 없어도 보유가 이어진 해는 그대로 나온다(실측: 2016 은 거래 0 건인데 기말 평가액이 있어 표에 남는다). 그러니 이 줄은 "거래가 없던 해"가 아니라
 * "포트폴리오 자체가 없던 해"를 뜻한다.
 */
class YearlySummaryGapRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/assetGrowthYearlySummary.jte";

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

  private static TradeProfitYearlySummary year(int value, String closing) {
    TradeProfitTimeSeriesSummary summary =
        new TradeProfitTimeSeriesSummary(
            BigDecimal.ZERO,
            new BigDecimal(closing),
            null,
            10.0d,
            new BigDecimal("1000"),
            new BigDecimal("2000"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            -5.0d,
            LocalDate.of(value, 3, 1),
            LocalDate.of(value, 6, 1),
            0.0d,
            null,
            null,
            null,
            null,
            null);
    return new TradeProfitYearlySummary(
        value, LocalDate.of(value, 1, 1), LocalDate.of(value, 12, 31), true, summary);
  }

  /** 표는 연도 내림차순으로 온다(api-stock 이 그렇게 정렬해 보낸다). */
  private String render(List<TradeProfitYearlySummary> rows) {
    Map<String, Object> model = new HashMap<>();
    model.put("yearlySummaries", rows);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  @Test
  void 건너뛴_해를_한_줄로_밝힌다() {
    String html = render(List.of(year(2014, "20000000"), year(2010, "0"), year(2009, "10000000")));

    assertThat(html).as("연도별 표를 그리지 못했다 - 검사가 무력해진다").contains("2014").contains("2009");
    assertThat(html).as("2011~2013 이 사라졌는데 아무 말이 없다").contains("data-yearly-gap=\"2011-2013\"");
    assertThat(html)
        .contains(
            java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.asset.growth.yearly.gap"), "2011", "2013"));
  }

  @Test
  void 연도가_이어지면_줄을_넣지_않는다() {
    // 늘 뜨는 줄은 곧 무시된다. 한 해도 건너뛰지 않았으면 조용해야 한다.
    String html = render(List.of(year(2026, "100"), year(2025, "90"), year(2024, "80")));

    assertThat(html).as("연도별 표를 그리지 못했다").contains("2025");
    assertThat(html).as("이어지는 연도에 건너뜀 줄이 붙었다").doesNotContain("data-yearly-gap");
  }

  @Test
  void 한_해만_건너뛰어도_밝힌다() {
    String html = render(List.of(year(2026, "100"), year(2024, "80")));

    assertThat(html).contains("data-yearly-gap=\"2025-2025\"");
  }
}
