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
 * <p>용어에 점선 밑줄을 긋고 마우스를 올리면 뜻이 뜬다({@code data-stock-tag-tooltip} - 화면에 고정으로 떠서 가로 스크롤 상자에 안 잘린다).
 * 풍선은 마우스 전용이므로 같은 뜻을 섹션 위 "용어 풀이" 에 두고, 초점을 받는 정렬 단추는 {@code aria-describedby} 로 그 뜻에 잇는다.
 */
class DividendYieldTermHintRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/dividend/dividendYieldAnalytics.jte";

  private static final Pattern HINT =
      Pattern.compile(
          "<span [^>]*data-stock-tag-tooltip=\"([^\"]*)\" data-term-hint>([^<]*)</span>");

  private static final Pattern DESCRIBED_BY = Pattern.compile("aria-describedby=\"([^\"]+)\"");

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

  /** 사용자가 물은 두 용어와 형제 하나(기준일 평균시가 수익률)의 뜻이 실제 문장으로 나온다 - 키 이름이 그대로 새면 안 된다. */
  @Test
  void 용어_풀이에_세_용어의_뜻이_실린다() {
    String html = render();

    assertThat(html).contains("data-yield-glossary");
    assertThat(html)
        .contains("<summary")
        .contains(message("stock.dividend.yield.term.glossary.title"));
    for (String[] term :
        new String[][] {
          {"daily", DAILY_DESC, "stock.dividend.yield.column.daily.average.invested.capital"},
          {"basis", BASIS_DESC, "stock.dividend.yield.column.basis.average.invested.capital"},
          {"market", MARKET_DESC, "stock.dividend.yield.column.yield.on.basis.market"},
        }) {
      String desc = message(term[1]);
      assertThat(desc).as("문구가 비었거나 키가 그대로다: %s", term[1]).isNotBlank().doesNotContain("stock.");
      int dd = html.indexOf("id=\"dividend-yield-term-" + term[0] + "\"");
      assertThat(dd).as("용어 풀이 항목이 없다: %s", term[0]).isNotNegative();
      assertThat(html.substring(dd, html.indexOf("</dd>", dd))).contains(escapedText(desc));
      // 뜻 바로 앞에 그 용어 이름이 있다 - 뜻과 이름이 엇갈려 붙으면 안 된다.
      int dt = html.lastIndexOf("<dt", dd);
      assertThat(html.substring(dt, dd)).contains(escapedText(message(term[2])));
    }
  }

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
      String label = matcher.group(2);
      assertThat(expected).as("모르는 밑줄 용어: %s", label).containsKey(label);
      assertThat(matcher.group(1)).as("%s 에 엉뚱한 뜻", label).isEqualTo(expected.get(label));
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

  /** 정렬 단추는 초점을 받는다 - 그 단추가 가리키는 뜻이 실제로 화면에 있어야 낭독기가 읽는다. */
  @Test
  void 정렬_단추가_가리키는_뜻이_화면에_있다() {
    String html = render();

    Matcher matcher = DESCRIBED_BY.matcher(html);
    int count = 0;
    while (matcher.find()) {
      String id = matcher.group(1);
      if (!id.startsWith("dividend-yield-term-")) {
        continue;
      }
      count++;
      assertThat(html).as("가리키는 곳이 없다: %s", id).contains("id=\"" + id + "\"");
    }
    // 종목 · 계좌 표에 일곱 열씩(예전 3 · 연 수익률 · 평균 투입원금 · 평가 손익 · 합산 손익).
    // 12 번: 실현 손익 +1 씩.
    assertThat(count).isEqualTo(16);
    String dailySort = html.substring(html.indexOf("data-sort-key=\"averageDailyPrincipalCost\""));
    assertThat(dailySort.substring(0, dailySort.indexOf('>')))
        .contains("aria-describedby=\"dividend-yield-term-daily\"");
    String basisSort = html.substring(html.indexOf("data-sort-key=\"averagePrincipalCost\""));
    assertThat(basisSort.substring(0, basisSort.indexOf('>')))
        .contains("aria-describedby=\"dividend-yield-term-basis\"");
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

  /**
   * 정렬 단추에는 누르는 영역을 넓히는 투명 띠({@code [data-sort-key]::after}, 절대 위치)가 단추 전체를 덮는다. 그 띠가 안쪽 밑줄 위에 있으면
   * 마우스가 단추에만 닿아 풍선이 안 뜬다 - 사용자 제보 2026-09-28(랭킹 표 머리글 6 곳). 밑줄을 띠 위로 올리는 규칙이 <b>배포되는 빌드 산출물</b>에
   * 있어야 한다(메이븐은 프론트엔드를 빌드하지 않는다).
   */
  @Test
  void 정렬_단추_안_밑줄은_누름_영역_띠_위에_있다() throws java.io.IOException {
    String built =
        java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/resources/static/main.css"),
            java.nio.charset.StandardCharsets.UTF_8);
    String selector = "[data-sort-key] [data-term-hint]{";
    int at = built.indexOf(selector);
    assertThat(at).as("빌드된 main.css 에 규칙이 없다 - npm run build 를 했는가").isNotNegative();
    String rule = built.substring(at + selector.length(), built.indexOf('}', at));
    assertThat(rule).contains("position:relative").contains("z-index:1");
    assertThat(built).as("띠 규칙이 사라졌다면 이 시험의 전제도 바뀐 것이다").contains("[data-sort-key]:after");
  }

  private static String escapedText(String text) {
    StringOutput out = new StringOutput();
    gg.jte.html.escape.Escape.htmlContent(text, out);
    return out.toString();
  }
}
