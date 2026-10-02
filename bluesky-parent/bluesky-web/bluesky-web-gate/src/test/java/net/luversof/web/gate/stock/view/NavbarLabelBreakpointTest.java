package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * 영어 화면 점검(2026-10-01)에서 고친 것들.
 *
 * <ul>
 *   <li>주식 화면 머리 막대가 1280px(한국어) · 1280~1440px(영어)에서 두 줄(71 -&gt; 116px)이 되어 왼쪽 메뉴 첫 항목(대시보드)을 덮었다.
 *       금액 숨김 글자와 "로케일" 글자가 xl(1280px)부터 보였기 때문 - 2xl(1536px)부터로 미뤘다(실측: 그 둘만 숨기면 1280~1535px 한 · 영
 *       모두 71px).
 *   <li>"1 months" · "the 2th" - 수에 붙는 영어 꼴을 고정 접미로 적었다.
 * </ul>
 */
class NavbarLabelBreakpointTest {

  private static String read(String path) throws IOException {
    return Files.readString(Path.of("src/main/jte/" + path), StandardCharsets.UTF_8);
  }

  @Test
  void 머리_막대_글자_라벨은_2xl_부터() throws IOException {
    assertThat(read("_components/body/navbar/navbar.jte"))
        .contains("hidden 2xl:inline text-base-content/60\" id=\"hideAmountsText\"")
        .doesNotContain("hidden xl:inline text-base-content/60\" id=\"hideAmountsText\"");
    assertThat(read("_components/body/navbar/end/locale.jte"))
        .contains(
            "<span class=\"max-2xl:hidden whitespace-nowrap\">${MessageUtil.getMessage(\"common.label.locale\")}");
  }

  /** 배당 내역 월평균 설명은 브라우저가 replace 로 채워 choice 서식을 못 쓴다 - 1 개월용 문구를 따로 넘기고 고른다. */
  @Test
  void 배당_월평균_설명은_1개월일_때_단수_문구를_쓴다() throws IOException {
    assertThat(read("stock/htmx/fragments/tabsDividendHistory.jte"))
        .contains("MessageUtil.getMessage(\"stock.dividend.summary.average.desc.one\")")
        .contains("averageDescOneTemplate: \"${averageDescOneTemplate}\"");
    assertThat(
            Files.readString(
                Path.of("src/main/frontend/src/stock/dividendHistory.ts"), StandardCharsets.UTF_8))
        .contains("(monthText === \"1\" ? averageDescOneTemplate : averageDescTemplate)");
    Properties en = new Properties();
    try (var reader =
        Files.newBufferedReader(
            Path.of("src/main/resources/uiMessage.properties"), StandardCharsets.UTF_8)) {
      en.load(reader);
    }
    assertThat(en.getProperty("stock.dividend.summary.average.desc.one"))
        .startsWith("Average for 1 month ");
  }

  /** 수를 넘기는 자리(MessageFormat)의 영어 복수는 choice 로 - 1 이면 단수(동사까지). 인자는 문자열이 아니라 수여야 한다(템플릿에서 바꿨다). */
  @Test
  void 수를_받는_영어_문구는_1에서_단수다() throws IOException {
    Properties en = new Properties();
    try (var reader =
        Files.newBufferedReader(
            Path.of("src/main/resources/uiMessage.properties"), StandardCharsets.UTF_8)) {
      en.load(reader);
    }
    java.util.function.BiFunction<String, Object[], String> f =
        (key, args) -> java.text.MessageFormat.format(en.getProperty(key), args);
    assertThat(f.apply("stock.monthly.etf.table.cell.payout.count", new Object[] {1}))
        .isEqualTo("1 record");
    assertThat(f.apply("stock.dividend.calendar.sample", new Object[] {1}))
        .isEqualTo("Based on 1 payout.");
    assertThat(f.apply("stock.dividend.yield.cell.held.days", new Object[] {1L}))
        .isEqualTo("Held 1 day");
    assertThat(f.apply("stock.dividend.yield.cell.held.days", new Object[] {30L}))
        .isEqualTo("Held 30 days");
    assertThat(f.apply("stock.dividend.yield.year.partial.desc", new Object[] {1L}))
        .startsWith("Only 1 day of this year is in the period");
    assertThat(f.apply("stock.realized.summary.win.count", new Object[] {1L, 1}))
        .isEqualTo("1 win / 1 stock");
    assertThat(f.apply("stock.realized.summary.win.count", new Object[] {3L, 5}))
        .isEqualTo("3 wins / 5 stocks");
    assertThat(f.apply("stock.summary.win.rate.basis.tooltip", new Object[] {1L, 1}))
        .startsWith("1 of 1 stock is in profit");
    assertThat(f.apply("stock.summary.win.rate.basis.tooltip", new Object[] {2L, 9}))
        .startsWith("2 of 9 stocks are in profit");
    assertThat(f.apply("stock.dividend.yield.summary.annualized.note", new Object[] {1L}))
        .endsWith("(1 day)");
    assertThat(f.apply("stock.admin.ledger.multi.reason.rows", new Object[] {1}))
        .startsWith("1 row has more than one reason");
    assertThat(f.apply("stock.admin.ledger.multi.reason.rows", new Object[] {3}))
        .startsWith("3 rows have more than one reason");
    String dividendYield =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte"),
            StandardCharsets.UTF_8);
    assertThat(dividendYield)
        .as("문자열로 넘기면 choice 가 예외를 던진다")
        .doesNotContain("String.valueOf(days)")
        .doesNotContain("String.valueOf(partialYearDays.apply(row))");
  }

  /**
   * 매매 내역 표의 수량 머리글은 짧게(영어 "Qty") - 자산 성장 매매 내역 표가 영어 1280px 에서 16px 넓어 가로 스크롤이 생겼다(2026-10-02).
   * 낭독기 · 툴팁에는 원래 이름(aria-label · abbr title).
   */
  @Test
  void 매매_표_수량_머리글은_짧고_이름은_원래대로() throws IOException {
    for (String template :
        new String[] {
          "stock/htmx/tradeHistory.jte", "stock/htmx/fragments/trade/tradeDetailList.jte"
        }) {
      assertThat(read(template))
          .as(template)
          .contains(
              "aria-label=\"${quantityLabel}\"><abbr title=\"${quantityLabel}\" class=\"no-underline\">${quantityShortLabel}</abbr></th>");
    }
  }

  @Test
  void 영어_수_표기는_단수와_서수를_틀리지_않는다() throws IOException {
    Properties en = new Properties();
    try (var reader =
        Files.newBufferedReader(
            Path.of("src/main/resources/uiMessage.properties"), StandardCharsets.UTF_8)) {
      en.load(reader);
    }
    String period = en.getProperty("stock.monthly.etf.period.month");
    assertThat(java.text.MessageFormat.format(period, 1)).isEqualTo("1 month");
    assertThat(java.text.MessageFormat.format(period, 6)).isEqualTo("6 months");
    // 건수 공통 문구: 1 은 단수, 0 은 복수(choice 의 첫 갈래가 0 미만 · 0 을 받으므로 0# 이 있어야 "0 item" 이 안 된다).
    assertThat(en.getProperty("stock.activity.label.shares.one")).isEqualTo("{0} share");
    String count = en.getProperty("common.count");
    assertThat(java.text.MessageFormat.format(count, 1L)).isEqualTo("1 item");
    assertThat(java.text.MessageFormat.format(count, 0L)).isEqualTo("0 items");
    assertThat(java.text.MessageFormat.format(count, 211L)).isEqualTo("211 items");
    // 브라우저 쪽(배당 내역)도 같은 꼴을 고른다 - 그대로 replace 하면 choice 글자가 화면에 찍힌다.
    assertThat(
            Files.readString(
                Path.of("src/main/frontend/src/stock/dividendHistory.ts"), StandardCharsets.UTF_8))
        .contains("return countChoice(countPattern, value).replace('{0}', formatNumber(value));");
    assertThat(
            java.text.MessageFormat.format(
                en.getProperty("stock.summary.upcoming.dividend.payday.spread"), "2", "9"))
        .doesNotContain("2th")
        .contains("day 2")
        .contains("day 9");
  }
}
