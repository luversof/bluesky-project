package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
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
 * 배당 수익률 분석을 한눈에 &mdash; 사용자 요청 2026-09-28: "다양한 수익률과 투입원금을 보여주는데 솔직히 잘 눈에 안 들어와".
 *
 * <ul>
 *   <li>맨 위는 숫자 하나(최근 1 년 배당수익률)와 그 숫자를 되짚는 문장 한 줄. 예전 지표는 "다른 기준 보기" 안.
 *   <li>표 기본 열은 세후 배당 · 평균 투입원금(들고 있던 날) · 연 수익률 · 건수. 나머지는 "열 더 보기".
 *   <li>한 해를 다 못 채운 해(올해)는 날수를 적는다.
 * </ul>
 */
class DividendYieldHeadlineRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/dividend/dividendYieldAnalytics.jte";

  private static final Pattern HEAD_CELL = Pattern.compile("<th scope=\"col\"([^>]*)>");

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

  private static DividendYieldGroupView row(
      UUID groupId, String label, long heldDays, boolean shortHeld, long periodDays) {
    BigDecimal value = new BigDecimal("1000");
    return new DividendYieldGroupView(
        groupId,
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
        Instant.parse("2026-08-19T00:00:00Z"),
        new BigDecimal("3650000"),
        heldDays,
        new BigDecimal("10000.00"),
        new BigDecimal("10.0000"),
        shortHeld,
        periodDays);
  }

  private String render(BigDecimal ttmYield) {
    return render(ttmYield, null);
  }

  private String render(BigDecimal ttmYield, DividendYieldGroupView extraStock) {
    List<DividendYieldGroupView> stocks =
        List.of(
            row(UUID.randomUUID(), "A", 365, false, 1000),
            row(UUID.randomUUID(), "B", 30, true, 1000));
    List<DividendYieldGroupView> years =
        List.of(row(null, "2026", 271, false, 271), row(null, "2025", 365, false, 365));
    Map<String, Object> model = new HashMap<>();
    model.put("decimalFormat", new DecimalFormat("#,##0"));
    model.put(
        "percentFormat",
        (java.util.function.Function<BigDecimal, String>)
            value -> value == null ? "-" : value.setScale(2, RoundingMode.HALF_UP) + "%");
    model.put("countFormat", (java.util.function.Function<Long, String>) String::valueOf);
    model.put("safeAllNet", BigDecimal.ONE);
    model.put("portfolioYieldOnDailyAverageCostPct", new BigDecimal("16.23"));
    model.put("portfolioYieldOnCostPct", new BigDecimal("7.66"));
    model.put("portfolioYieldOnMarketPct", new BigDecimal("7.25"));
    model.put("portfolioYieldAnnualizedPct", new BigDecimal("2.52"));
    model.put("periodDayCount", 2354L);
    model.put("portfolioYield", row(null, "portfolio", 2345, false, 2354));
    model.put("ttmYieldPct", ttmYield);
    model.put("ttmAverageDailyPrincipalCost", new BigDecimal("600000000"));
    model.put("ttmNetWithPrincipalCost", new BigDecimal("30000000"));
    model.put("ttmNetAmount", new BigDecimal("31200000"));
    model.put("ttmStartDate", LocalDate.of(2025, 9, 29));
    model.put("ttmEndDate", LocalDate.of(2026, 9, 28));
    // A 만 지금 들고 있다(B 는 다 팔았다).
    if (extraStock != null) {
      stocks = List.of(stocks.get(0), stocks.get(1), extraStock);
    }
    model.put("currentValueByStock", Map.of(stocks.get(0).groupId(), new BigDecimal("1234567")));
    model.put("currentValueByAccount", Map.of());
    model.put("bestYieldStock", stocks.get(0));
    model.put("bestYieldAccount", stocks.get(0));
    model.put("yearlyYieldRows", years);
    model.put("stockYieldRows", stocks);
    model.put("accountYieldRows", stocks);
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

  private static String text(String html, String marker) {
    int at = html.indexOf(marker);
    assertThat(at).as("찾지 못함: %s", marker).isNotNegative();
    int start = html.indexOf('>', at) + 1;
    return html.substring(start, html.indexOf('<', start)).trim();
  }

  @Test
  void 맨_위는_최근_1년_숫자_하나와_되짚는_문장이다() {
    String html = render(new BigDecimal("5.0000"));

    assertThat(text(html, "data-ttm-yield")).isEqualTo("연 5.00%");
    // 문장의 두 금액으로 머리 숫자를 되짚을 수 있다: 30,000,000 / 600,000,000 = 5%.
    assertThat(text(html, "data-ttm-sentence"))
        .contains("600,000,000")
        .contains("30,000,000")
        .doesNotContain("{0}");
    assertThat(text(html, "data-ttm-monthly")).contains("2,600,000");
    assertThat(html).contains("(2025-09-29 ~ 2026-09-28)");
    // 예전 지표는 사라지지 않고 접힌 곳에 있다.
    int other = html.indexOf("data-yield-other-basis");
    assertThat(other).isPositive();
    assertThat(html.substring(other, html.indexOf("</details>", other)))
        .contains("16.23%")
        .contains("7.66%")
        .contains("7.25%");
    assertThat(html).as("선택 기간 연 수익률은 보이는 곳에").contains("data-period-annualized");
  }

  @Test
  void 최근_1년_배당이_없으면_그렇게_말한다() {
    String html = render(null);

    assertThat(html).contains("data-ttm-none").doesNotContain("data-ttm-yield");
  }

  /**
   * 세 표의 기본 머리글은 예전 열 표시가 없는 것뿐이다. 2026-09-28 사용자("평균 투입원금은 기간 평균이라 - 지금 평가 금액을 알고 싶다"): 종목 · 계좌 표는
   * 종목 · 세후 · 현재 평가금액 · 연 수익률 · 건수, 연도별 표는 연도 · 세후 · 연 수익률 · 건수(지금 값은 연도와 무관하다).
   */
  @Test
  void 표_기본_열은_다섯이고_나머지는_열_더_보기다() {
    String html = render(new BigDecimal("5"));

    for (String table :
        List.of(
            "aria-label=\"" + MessageUtil.getMessage("stock.dividend.yield.section.yearly") + "\"",
            "aria-label=\"" + MessageUtil.getMessage("stock.dividend.yield.section.stock") + "\"",
            "aria-label=\""
                + MessageUtil.getMessage("stock.dividend.yield.section.account")
                + "\"")) {
      int start = html.indexOf(table);
      assertThat(start).as(table).isPositive();
      String head = html.substring(start, html.indexOf("</thead>", start));
      Matcher matcher = HEAD_CELL.matcher(head);
      List<String> shown = new ArrayList<>();
      int extra = 0;
      while (matcher.find()) {
        if (matcher.group(1).contains("yield-extra-col")) {
          extra++;
        } else {
          shown.add(matcher.group(1));
        }
      }
      // 2026-09-28 10 번: 종목 · 계좌 표에 평가 손익 · 합산 손익(원금 대비 이익/손해, 합산 수익률) - 7 열.
      // 2026-09-28 11 번(사용자: "합산손익은 그 다음에 위치해야 a + b = c"): 종목 · 계좌 = 종목 · 현재 평가금액 · 평가 손익 · 배당금(+연
      // 수익률) · 합산 손익 · 건수,
      // 연도별 = 연도 · 배당금(+연 수익률) · 건수. 연 수익률 열은 배당금 칸으로 합쳤다.
      // 12 번: 평가 손익 뒤에 실현 손익(a + b + c = d) - 종목 · 계좌 7 열.
      assertThat(shown).as("%s 기본 열", table).hasSize(table.contains("연도") ? 3 : 7);
      assertThat(extra).as("%s 열 더 보기", table).isGreaterThanOrEqualTo(6);
    }
    assertThat(html.split("data-yield-extra-toggle>", -1)).hasSize(3 + 1);
  }

  @Test
  void 연_수익률_칸에_보유_일수가_붙고_짧으면_표시한다() {
    String html = render(new BigDecimal("5"));

    assertThat(html).contains("보유 365일").contains("보유 30일");
    assertThat(html.split("data-short-held-badge", -1)).as("종목 · 계좌 표에 짧은 행 하나씩").hasSize(2 + 1);
  }

  @Test
  void 한_해를_다_못_채운_해는_날수를_적는다() {
    String html = render(new BigDecimal("5"));

    int partial = html.indexOf("data-year-partial");
    assertThat(partial).isPositive();
    assertThat(html.substring(partial, html.indexOf("</th>", partial))).contains("271일");
    assertThat(html.split("data-year-partial", -1)).as("2025 는 다 찼다").hasSize(1 + 1);
    assertThat(html.split("data-yearly-bar", -1)).as("연도마다 막대").hasSize(2 + 1);
  }

  /** 현재 평가금액: 들고 있으면 금액, 다 팔았으면 대시와 그 까닭(0 원으로 적으면 값이 0 인 것처럼 읽힌다). 합계는 들고 있는 것의 합. */
  @Test
  void 현재_평가금액은_지금_들고_있는_것만_적는다() {
    String html = render(new BigDecimal("5"));
    String stockTable =
        html.substring(
            html.indexOf(
                "aria-label=\""
                    + MessageUtil.getMessage("stock.dividend.yield.section.stock")
                    + "\""));

    int first = stockTable.indexOf("data-current-value-cell>");
    assertThat(stockTable.substring(first, stockTable.indexOf("</td>", first)))
        .contains("1,234,567");
    int second = stockTable.indexOf("data-current-value-cell>", first + 1);
    assertThat(stockTable.substring(second, stockTable.indexOf("</td>", second)))
        .contains(">-</span>")
        .contains(MessageUtil.getMessage("stock.dividend.yield.current.value.none"));
    int total = stockTable.indexOf("data-current-value-total>");
    assertThat(stockTable.substring(total, stockTable.indexOf("</td>", total)))
        .contains("1,234,567");
    assertThat(html).as("최고 효율 카드도 평균 투입원금 대신 현재 평가금액").contains("data-best-current-value");
  }

  /**
   * 원금 기록 없이 배당만 있는 종목(실측: 하나금융지주 - 매매 기록 없음, 배당 2,100 원)은 연 수익률이 대시이고, 근거 문장("기준일 원금이 없는 배당
   * 2,100원은 제외")도 붙는다. 금액 가리기(common.ts maskAmountBasis)는 그 칸의 <b>첫</b> sr-only 를 근거 문장으로 가리므로 근거가
   * 대시 까닭보다 앞이어야 한다 - 2026-09-28 대시 까닭을 앞에 뒀다가 가리기를 켜도 "2,100원" 이 낭독기에 그대로
   * 남았다(hide-amounts-leak-check).
   */
  @Test
  void 연_수익률이_빈_칸도_근거_문장이_첫_낭독_글자다() {
    BigDecimal net = new BigDecimal("2100");
    var noPrincipal =
        new DividendYieldGroupView(
            UUID.randomUUID(),
            "원금없음",
            net,
            net,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            null,
            null,
            null,
            null,
            null,
            2L,
            Instant.parse("2026-08-19T00:00:00Z"),
            null,
            0L,
            null,
            null,
            false,
            1000L);
    String html = render(new BigDecimal("5"), noPrincipal);

    int row = html.indexOf("data-label=\"원금없음\"");
    assertThat(row).isPositive();
    int cell = html.indexOf("data-annualized-cell", row);
    String td = html.substring(cell, html.indexOf("</td>", cell));
    int firstSr = td.indexOf("<span class=\"sr-only\">");
    assertThat(firstSr).isPositive();
    assertThat(td.substring(firstSr, td.indexOf("</span>", firstSr))).contains("2,100");
    assertThat(td).contains(MessageUtil.getMessage("stock.dividend.yield.daily.basis.none"));
  }

  private static final Pattern ROW = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL);

  private static final Pattern CELL_OPEN = Pattern.compile("<t[dh](\s[^>]*)?>");

  /** 한 줄의 칸 여는 태그들(속성 포함). 칸 안의 중첩 표는 없다. */
  private static List<String> cellTags(String rowHtml) {
    List<String> tags = new ArrayList<>();
    Matcher matcher = CELL_OPEN.matcher(rowHtml);
    while (matcher.find()) {
      tags.add(matcher.group());
    }
    return tags;
  }

  /**
   * 머리 · 본문 · 합계 줄의 칸이 같은 자리에 선다 - 칸 수가 같고, 배당금 칸(연 수익률 포함)이 머리의 배당금 머리글과 같은 번호다. 2026-09-28 열을 옮기다
   * 연도별 표의 본문 · 합계에서 배당금 칸이 사라지고 옛 연 수익률 칸이 남았는데(머리는 맞았다) 머리만 보는 시험은 통과했다.
   */
  @Test
  void 머리_본문_합계의_칸이_같은_자리다() {
    String html = render(new BigDecimal("5"));
    for (String section :
        List.of(
            "stock.dividend.yield.section.yearly",
            "stock.dividend.yield.section.stock",
            "stock.dividend.yield.section.account")) {
      int start = html.indexOf("aria-label=\"" + MessageUtil.getMessage(section) + "\"");
      String table = html.substring(start, html.indexOf("</table>", start));
      String head = table.substring(table.indexOf("<thead"), table.indexOf("</thead>"));
      Matcher headRow = ROW.matcher(head);
      assertThat(headRow.find()).isTrue();
      List<String> headTags = cellTags(headRow.group(1));
      int dividendAt = -1;
      String headInner = headRow.group(1);
      List<String> headCells = new ArrayList<>();
      Matcher cells = Pattern.compile("<th[^>]*>(.*?)</th>", Pattern.DOTALL).matcher(headInner);
      while (cells.find()) {
        headCells.add(cells.group(1));
      }
      for (int i = 0; i < headCells.size(); i++) {
        if (headCells.get(i).contains(">netAmountLabel<")) {
          dividendAt = i;
        }
      }
      assertThat(dividendAt).as("%s 배당금 머리글", section).isPositive();
      for (String part : List.of("<tbody", "<tfoot")) {
        int at = table.indexOf(part);
        if (at < 0) {
          continue;
        }
        String body =
            table.substring(at, table.indexOf(part.equals("<tbody") ? "</tbody>" : "</tfoot>", at));
        Matcher row = ROW.matcher(body);
        int rows = 0;
        while (row.find()) {
          List<String> tags = cellTags(row.group(1));
          assertThat(tags).as("%s %s 칸 수", section, part).hasSameSizeAs(headTags);
          assertThat(tags.get(dividendAt))
              .as("%s %s 배당금 칸 자리", section, part)
              .contains(part.equals("<tbody") ? "data-annualized-cell" : "data-annualized-total");
          rows++;
        }
        assertThat(rows).as("%s %s 줄(자가검사)", section, part).isPositive();
      }
    }
  }
}
