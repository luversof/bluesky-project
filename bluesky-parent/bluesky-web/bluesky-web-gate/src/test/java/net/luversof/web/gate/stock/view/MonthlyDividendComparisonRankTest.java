package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.dto.view.MonthlyDividendSimulatorSummaryView;
import net.luversof.web.gate.stock.service.MonthlyDividendCalculator;
import net.luversof.web.gate.stock.service.MonthlyDividendViewSupport;

/**
 * 월배당 시뮬레이터는 비교 우위 <b>순위</b>를 보여 준다.
 *
 * <p>2026-09-17 까지 화면은 1 위 하나만 말했다 &mdash; 요약 카드 "우선 검토 종목" 과 표의 "비교 우위" 배지 하나. 사용자 요청: "비교 우위를 최상
 * 1 개만 표시하는데 비교 우위 순위를 확인할 수 있으면 좋겠다". 순위 기준(예상 합산 수익률 &rarr; 평단 기준 연배당 수익률 &rarr; 연배당 수익률 &rarr;
 * 과세표준 비중 낮은 순 &rarr; 종목코드)도 화면 어디에도 없었다.
 *
 * <p>합산 수익률 열 정렬은 동률을 종목코드로만 갈랐다 &mdash; 그대로 두면 내림차순으로 정렬한 표에서 1 위 배지가 맨 위에 오지 않을 수 있다. 그래서 셋(카드 ·
 * 배지 · 정렬)이 한 비교기를 쓴다.
 */
class MonthlyDividendComparisonRankTest {

  private static final String FRAGMENT = "stock/fragments/monthlyDividendSimulator.jte";

  private static final String QUOTE = String.valueOf((char) 34);

  private final MonthlyDividendCalculator calculator = new MonthlyDividendCalculator();

  private final MonthlyDividendViewSupport support = new MonthlyDividendViewSupport();

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
    LocaleContextHolder.setLocale(Locale.KOREA);
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
    LocaleContextHolder.resetLocaleContext();
  }

  private static MonthlyDividendSnapshotResponse row(
      String symbol, String combined, String yieldOnCost, String annualYield, String taxableRatio) {
    BigDecimal ten = BigDecimal.TEN;
    return new MonthlyDividendSnapshotResponse(
        UUID.nameUUIDFromBytes(symbol.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        null,
        UUID.nameUUIDFromBytes(
            ("item-" + symbol).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        symbol,
        symbol + " NAME",
        LocalDate.parse("2026-09-02"),
        ten,
        ten,
        new BigDecimal(taxableRatio),
        100,
        ten,
        ten,
        new BigDecimal("1000"),
        new BigDecimal("1000"),
        BigDecimal.ONE,
        new BigDecimal(annualYield),
        BigDecimal.ONE,
        new BigDecimal(yieldOnCost),
        new BigDecimal("100"),
        BigDecimal.ONE,
        new BigDecimal(combined),
        null);
  }

  /**
   * 순위마다 한 단계씩 동률을 만든다 - 앞 기준이 같을 때만 뒤 기준이 가른다.
   *
   * <p>B 는 합산이 가장 높다. C · D · A · E 는 합산이 같고, C 는 평단 기준 수익률로, D 는 과세표준 비중이 낮아서, A 와 E 는 종목코드로 갈린다.
   */
  private static List<MonthlyDividendSnapshotResponse> ladder() {
    return List.of(
        row("ZZE", "10", "5", "4", "50"),
        row("AAA", "10", "5", "4", "50"),
        row("MMD", "10", "5", "4", "10"),
        row("YYC", "10", "6", "4", "50"),
        row("BBB", "12", "1", "1", "90"));
  }

  private static UUID id(String symbol) {
    return UUID.nameUUIDFromBytes(symbol.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  @Test
  void 순위는_합산_수익률부터_동률_규칙까지_따른다() {
    MonthlyDividendSimulatorSummaryView summary = calculator.buildSimulatorSummary(ladder());

    assertThat(summary.comparisonRanks())
        .containsEntry(id("BBB"), 1)
        .containsEntry(id("YYC"), 2)
        .containsEntry(id("MMD"), 3)
        .containsEntry(id("AAA"), 4)
        .containsEntry(id("ZZE"), 5)
        .hasSize(5);
    assertThat(summary.bestChoice().stockItemSymbol()).as("우선 검토 종목은 1 위다").isEqualTo("BBB");
  }

  /** 합산 수익률 내림차순으로 정렬한 표는 순위 1, 2, 3 ... 순이다. 종목코드로만 가르면 AAA 가 YYC · MMD 보다 먼저 온다. */
  @Test
  void 합산_수익률_내림차순_정렬은_순위_순서와_같다() {
    List<MonthlyDividendSnapshotResponse> desc =
        support.sortRows(
            ladder(), MonthlyDividendViewSupport.SORT_COMBINED_RETURN, "desc", Map.of());
    assertThat(desc)
        .extracting(MonthlyDividendSnapshotResponse::stockItemSymbol)
        .containsExactly("BBB", "YYC", "MMD", "AAA", "ZZE");

    List<MonthlyDividendSnapshotResponse> asc =
        support.sortRows(
            ladder(), MonthlyDividendViewSupport.SORT_COMBINED_RETURN, "asc", Map.of());
    assertThat(asc)
        .extracting(MonthlyDividendSnapshotResponse::stockItemSymbol)
        .as("오름차순은 순위의 역순")
        .containsExactly("ZZE", "AAA", "MMD", "YYC", "BBB");
  }

  private static String render(
      List<MonthlyDividendSnapshotResponse> rows,
      MonthlyDividendSimulatorSummaryView summary,
      String keyword) {
    Map<String, Object> model = new HashMap<>();
    model.put("monthlyDividendRows", rows);
    model.put("monthlyDividendSummary", summary);
    model.put("monthlyDividendKeyword", keyword);
    model.put("monthlyDividendHasSavedRows", !rows.isEmpty());
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(FRAGMENT, model, output);
    return output.toString();
  }

  /** 표의 한 행 - data-symbol 로 찾아 그 행의 끝까지. */
  private static String rowHtml(String html, String symbol) {
    int at = html.indexOf("data-symbol=" + QUOTE + symbol + QUOTE);
    assertThat(at).as(symbol + " 행을 못 찾았다 - 검사가 헛돈다").isGreaterThan(0);
    return html.substring(at, html.indexOf("</tr>", at));
  }

  @Test
  void 표의_모든_행에_순위_배지가_붙고_1위만_강조한다() {
    List<MonthlyDividendSnapshotResponse> rows = ladder();
    MonthlyDividendSimulatorSummaryView summary = calculator.buildSimulatorSummary(rows);
    String html = render(rows, summary, "");

    Map<String, Integer> expected = Map.of("BBB", 1, "YYC", 2, "MMD", 3, "AAA", 4, "ZZE", 5);
    Pattern badge =
        Pattern.compile(
            "<span class="
                + QUOTE
                + "badge badge-xs ([a-z-]+)"
                + QUOTE
                + " data-comparison-rank="
                + QUOTE
                + "([0-9]+)"
                + QUOTE
                + ">([^<]*)</span>");
    for (Map.Entry<String, Integer> entry : expected.entrySet()) {
      Matcher matcher = badge.matcher(rowHtml(html, entry.getKey()));
      List<String> found = new ArrayList<>();
      while (matcher.find()) {
        found.add(matcher.group(2));
        assertThat(matcher.group(1))
            .as(entry.getKey() + " 배지 색")
            .isEqualTo(entry.getValue() == 1 ? "badge-success" : "badge-ghost");
        assertThat(matcher.group(3))
            .isEqualTo(
                java.text.MessageFormat.format(
                    MessageUtil.getMessage("stock.simulator.compare.rank"),
                    String.valueOf(entry.getValue())));
      }
      assertThat(found)
          .as(entry.getKey() + " 행의 순위 배지")
          .containsExactly(String.valueOf(entry.getValue()));
    }
  }

  @Test
  void 요약_카드가_순위_기준과_순위대로_보기를_준다() {
    List<MonthlyDividendSnapshotResponse> rows = ladder();
    String html = render(rows, calculator.buildSimulatorSummary(rows), "TIGER");

    String basis = MessageUtil.getMessage("stock.simulator.monthly.summary.comparison.basis");
    assertThat(basis).isNotEqualTo("stock.simulator.monthly.summary.comparison.basis");
    assertThat(html).contains("data-comparison-basis").contains(basis);

    Matcher link =
        Pattern.compile(
                "href=" + QUOTE + "([^" + QUOTE + "]*)" + QUOTE + " data-comparison-rank-link")
            .matcher(html);
    assertThat(link.find()).as("순위대로 보기 링크가 없다").isTrue();
    // JTE 는 변수로 넣은 앞부분의 & 만 &amp; 로 바꾼다(표 머리 정렬 링크들도 같은 모양) - 브라우저는 둘을 같게 읽는다.
    assertThat(link.group(1).replace("&amp;", "&"))
        .as("합산 수익률 내림차순이 순위 순서다")
        .contains("sort=combined-return&direction=desc")
        .as("정렬 한 번에 필터가 풀리면 안 된다")
        .contains("keyword=TIGER")
        .endsWith("#monthlyDividendCompareTable");
    assertThat(html).contains("id=" + QUOTE + "monthlyDividendCompareTable" + QUOTE);
  }

  /** 고를 종목이 없으면 순위도 링크도 없다. */
  @Test
  void 행이_없으면_순위도_링크도_없다() {
    String html = render(List.of(), calculator.buildSimulatorSummary(List.of()), "");

    assertThat(html)
        .doesNotContain("data-comparison-rank=")
        .doesNotContain("data-comparison-rank-link");
  }
}
