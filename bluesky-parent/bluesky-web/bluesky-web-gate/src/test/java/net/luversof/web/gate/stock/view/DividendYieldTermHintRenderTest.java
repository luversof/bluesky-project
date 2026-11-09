package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView;

/**
 * 배당 수익률 용어 풀이 &mdash; 사용자 요청 2026-09-28: "기간 일평균 투입원금과 배당 기준일 평균원금이 어떤 의미인지 볼 때마다 헷갈려".
 *
 * <p>용어 옆 ⓘ 에 마우스를 올리거나 키보드 초점이 오면 뜻이 뜬다({@code data-stock-tag-tooltip} - 화면에 고정으로 떠서 가로 스크롤 상자에 안
 * 잘린다). 낭독기는 용어 옆 sr-only 뜻을 읽는다. 2026-11-10 섹션 위 "용어 풀이"(details)는 없앴다(사용자 요청 - 툴팁과 중복).
 */
class DividendYieldTermHintRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/dividend/dividendYieldAnalytics.jte";

  // 용어 이름(바로 앞 글자 마디) + 공용 ⓘ 툴팁(tooltip.jte 의 짧은 말풍선). 정렬 머리글은 이름이 단추 안, 툴팁이 단추 옆이다.
  private static final Pattern HINT =
      Pattern.compile(
          ">([^<>]*?)\\s*(?:</span>\\s*<span data-sort-indicator[^>]*>[^<]*</span>\\s*</button>\\s*)?"
              + "<span class=\"tooltip inline-block tooltip-bottom\" role=\"note\" tabindex=\"0\" aria-label=\"([^\"]*)\" data-tip=\"([^\"]*)\"><svg");

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

  private static DividendYieldGroupView row(String label) {
    BigDecimal value = new BigDecimal("1000");
    return new DividendYieldGroupView(
        UUID.randomUUID(),
        label,
        value,
        value,
        value,
        value,
        value,
        value,
        value,
        value,
        BigDecimal.TEN,
        BigDecimal.TEN,
        BigDecimal.TEN,
        1L,
        Instant.parse("2026-08-19T00:00:00Z"));
  }

  private String render() {
    List<DividendYieldGroupView> rows = List.of(row("A"), row("B"));
    Map<String, Object> model = new HashMap<>();
    model.put("decimalFormat", new DecimalFormat("#,##0"));
    model.put(
        "percentFormat",
        (java.util.function.Function<BigDecimal, String>)
            value -> value == null ? "-" : value.setScale(2, RoundingMode.HALF_UP) + "%");
    model.put("countFormat", (java.util.function.Function<Long, String>) String::valueOf);
    model.put("safeAllNet", BigDecimal.ONE);
    model.put("portfolioYieldOnDailyAverageCostPct", BigDecimal.ZERO);
    model.put("portfolioYieldOnCostPct", BigDecimal.ZERO);
    model.put("portfolioYieldOnMarketPct", BigDecimal.ZERO);
    model.put("portfolioYieldAnnualizedPct", null);
    model.put("periodDayCount", 365L);
    model.put("periodStartPrincipal", null);
    model.put("periodEndPrincipal", null);
    model.put("periodPrincipalDelta", null);
    model.put("periodPrincipalDeltaPct", null);
    // 요약 카드 둘(가장 높은 종목 · 계좌)도 그려야 그 안의 용어까지 본다.
    model.put("bestYieldStock", rows.get(0));
    model.put("bestYieldAccount", rows.get(1));
    model.put("yearlyYieldRows", rows);
    model.put("stockYieldRows", rows);
    model.put("accountYieldRows", rows);
    model.put("stockItemList", List.of());
    for (String label :
        List.of(
            "accountLabel",
            "stockNameLabel",
            "tagLabel",
            "noDataLabel",
            "grossAmountLabel",
            "netAmountLabel",
            "taxLabel",
            "taxableAmountLabel",
            "totalLabel")) {
      model.put(label, label);
    }
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  private static String message(String key) {
    return MessageUtil.getMessage(key);
  }

  private static String htmlAttribute(String text) {
    StringOutput out = new StringOutput();
    gg.jte.html.escape.Escape.htmlAttribute(text, out);
    return out.toString();
  }

  private static final String DAILY_DESC =
      "stock.dividend.yield.term.daily.average.invested.capital.desc";

  private static final String BASIS_DESC =
      "stock.dividend.yield.term.basis.average.invested.capital.desc";

  private static final String MARKET_DESC = "stock.dividend.yield.term.basis.market.desc";

  /**
   * 밑줄 용어마다 <b>제</b> 뜻이 붙는다 - 풍선 문장은 용어 풀이와 같은 키를 쓰고, 이름과 뜻이 엇갈리면(투입원금에 기준일 원금 뜻) 안 된다. 뜻이 셋 중
   * 하나인지만 보면 엇갈려 붙인 것을 통과시킨다.
   */
  @Test
  void 밑줄_용어마다_제_뜻이_붙는다() {
    String html = render();
    String daily = htmlAttribute(message(DAILY_DESC));
    String basis = htmlAttribute(message(BASIS_DESC));
    String market = htmlAttribute(message(MARKET_DESC));
    Map<String, String> expected = new HashMap<>();
    expected.put(
        escapedText(message("stock.dividend.yield.column.daily.average.invested.capital")), daily);
    expected.put(escapedText(message("stock.dividend.yield.summary.period.daily")), daily);
    expected.put(
        escapedText(message("stock.dividend.yield.column.basis.average.invested.capital")), basis);
    expected.put(escapedText(message("stock.dividend.yield.summary.period.basis")), basis);
    expected.put(
        escapedText(message("stock.dividend.yield.column.yield.on.basis.average.cost")), basis);
    expected.put(escapedText(message("stock.dividend.yield.column.yield.on.basis.market")), market);
    // 2026-09-28 기본 열 둘과 첫 카드의 선택 기간 연 수익률.
    String held = htmlAttribute(message("stock.dividend.yield.term.held.average.principal.desc"));
    String annualized = htmlAttribute(message("stock.dividend.yield.term.annualized.desc"));
    expected.put(escapedText(message("stock.dividend.yield.column.held.average.principal")), held);
    expected.put(escapedText(message("stock.dividend.yield.column.annualized")), annualized);
    expected.put(escapedText(message("stock.dividend.yield.period.annualized")), annualized);
    // 11 번: 연 수익률 머리글을 배당금(세후) 머리글로 합쳤다 - 그 밑줄이 연 수익률 뜻을 띄운다(시험 모델의 라벨은 키 이름 그대로).
    expected.put("netAmountLabel", annualized);
    expected.put(
        escapedText(message("stock.dividend.yield.column.realized.profit")),
        htmlAttribute(message("stock.dividend.yield.term.realized.profit.desc")));
    expected.put(
        escapedText(message("stock.dividend.yield.column.evaluation.profit")),
        htmlAttribute(message("stock.dividend.yield.term.evaluation.profit.desc")));
    expected.put(
        escapedText(message("stock.dividend.yield.column.combined.profit")),
        htmlAttribute(message("stock.dividend.yield.term.combined.profit.desc")));

    Matcher matcher = HINT.matcher(html);
    List<String> labels = new ArrayList<>();
    while (matcher.find()) {
      String label = matcher.group(1).trim();
      assertThat(expected)
          .as(
              "모르는 밑줄 용어: %s / %s",
              label, html.substring(Math.max(0, matcher.start() - 200), matcher.start() + 40))
          .containsKey(label);
      assertThat(matcher.group(3)).as("%s 에 엉뚱한 뜻", label).isEqualTo(expected.get(label));
      labels.add(label);
    }
    // 2026-09-28: 첫 카드 "다른 기준 보기" 3 + 최고 효율 카드 둘의 평균 투입원금 2 + 연도별 머리글 4(예전 2 · 새 2)
    //   + 선택 요약 4(두 표 x 투입원금 · 연 수익률) + 정렬 머리글 10(두 표 x 예전 3 · 새 2) = 23.
    //   (선택 기간 연 수익률 밑줄은 기간 연환산 값이 있을 때만 나온다 - 이 모델은 null 이다.)
    // 같은 날 최고 효율 카드 둘의 평균 투입원금 밑줄은 현재 평가금액(설명이 필요 없는 말)으로 바뀌었다 - 23 - 2.
    // 10 번: 두 표의 평가 손익 · 합산 손익 머리글 밑줄 +4.
    // 12 번: 두 표의 실현 손익 머리글 +2.
    assertThat(labels).as("밑줄 용어 수").hasSize(27);
    assertThat(labels)
        .contains(
            escapedText(message("stock.dividend.yield.column.daily.average.invested.capital")),
            escapedText(message("stock.dividend.yield.column.basis.average.invested.capital")));
  }

  /**
   * 2026-11-10 "용어 풀이"(details)를 없앴다 - 뜻은 용어 옆 ⓘ 툴팁 한곳이다. 낭독기는 그 툴팁의 aria-label 을 읽으므로 말풍선 문구와 같아야
   * 한다(공용 조각이 같은 값을 쓴다 - 여기서는 실제 그린 결과로 본다).
   */
  @Test
  void 용어마다_낭독기용_뜻이_풍선과_같다() {
    String html = render();

    assertThat(html).doesNotContain("data-yield-glossary").doesNotContain("dividend-yield-term-");
    Matcher matcher = HINT.matcher(html);
    int count = 0;
    while (matcher.find()) {
      count++;
      assertThat(matcher.group(2))
          .as("%s", matcher.group(1))
          .isNotBlank()
          .isEqualTo(matcher.group(3));
    }
    assertThat(count).isEqualTo(27);
  }

  /** 공용 조각이 호출한 자리에 개행을 끼우면 "라벨 값" 사이 모양이 바뀐다(srExact 에서 실제로 있었다). */
  @Test
  void 밑줄_조각은_앞뒤에_개행을_끼우지_않는다() {
    String html = render();

    assertThat(html).doesNotContain("\n<span class=\"underline decoration-dotted");
    assertThat(html)
        .doesNotContain("data-term-hint>\n")
        .doesNotContain("</span>\n <span class=\"font-medium");
  }

  private static String escapedText(String text) {
    StringOutput out = new StringOutput();
    gg.jte.html.escape.Escape.htmlContent(text, out);
    return out.toString();
  }
}
