package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;

/**
 * 월배당 시뮬레이터 표 위의 지급 시기 칩(전체 · 월중 · 월말, 사용자 요청 2026-10-13).
 *
 * <p>필터 폼에도 지급 시기 칸이 있지만 고른 뒤 "적용" 을 눌러야 걸렸다. 칩은 한 번 눌러 거른다. 칩 주소는 지급 시기만 바꾸고 검색어 · 계좌 · 정렬은 그대로
 * 실어야 한다 - 하나라도 빠지면 칩 한 번에 다른 조건이 조용히 풀린다(SimulatorSortKeepsFilterTest 와 같은 까닭).
 */
class SimulatorWindowChipTest {

  private static final String TEMPLATE = "stock/fragments/monthlyDividendSimulator.jte";

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

  private static MonthlyDividendSnapshotResponse row() {
    return new MonthlyDividendSnapshotResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "0000A0",
        "TIGER 시험",
        LocalDate.parse("2026-07-20"),
        new BigDecimal("323"),
        new BigDecimal("240"),
        new BigDecimal("0.5"),
        100,
        new BigDecimal("10000"),
        new BigDecimal("11000"),
        new BigDecimal("1100000"),
        new BigDecimal("24000"),
        new BigDecimal("2.1"),
        new BigDecimal("25.2"),
        new BigDecimal("2.4"),
        new BigDecimal("28.8"),
        new BigDecimal("100000"),
        new BigDecimal("30"),
        new BigDecimal("55"),
        null);
  }

  private static String render(String window, Map<String, Integer> counts) {
    Map<String, Object> params = new HashMap<>();
    params.put("monthlyDividendRows", List.of(row()));
    params.put("monthlyDividendHasSavedRows", true);
    params.put("monthlyDividendPayoutWindowFilter", window);
    params.put("monthlyDividendPayoutWindowCounts", counts);
    params.put("monthlyDividendKeyword", "TIGER");
    params.put("monthlyDividendAccountFilter", "PENSION");
    params.put("monthlyDividendSort", "symbol");
    params.put("monthlyDividendDirection", "desc");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, params, output);
    // 표현식으로 나온 주소의 & 는 &amp; 로 이스케이프된다 - 주소 비교는 풀어서 한다.
    return output.toString().replace("&amp;", "&");
  }

  /** 칩 하나의 여는 a 태그. */
  private static String chip(String html, String key) {
    Matcher m =
        Pattern.compile("<a [^>]*data-monthly-window-chip=\"" + key + "\"[^>]*>").matcher(html);
    assertThat(m.find()).as("칩이 없다: %s", key).isTrue();
    return m.group();
  }

  @Test
  void 고른_칩만_표시되고_개수를_적는다() {
    Map<String, Integer> counts = new LinkedHashMap<>();
    counts.put("", 5);
    counts.put("MID_MONTH", 2);
    counts.put("MONTH_END", 3);
    String html = render("MONTH_END", counts);

    assertThat(chip(html, "MONTH_END")).contains("aria-current=\"true\"").contains("btn-primary");
    assertThat(chip(html, "all")).doesNotContain("aria-current");
    assertThat(chip(html, "MID_MONTH")).doesNotContain("aria-current");
    String group =
        html.substring(
            html.indexOf("data-monthly-window-chips"),
            html.indexOf("</div>", html.indexOf("data-monthly-window-chips")));
    assertThat(group)
        .contains("<span class=\"tabular-nums\">5</span>")
        .contains("<span class=\"tabular-nums\">2</span>")
        .contains("<span class=\"tabular-nums\">3</span>");
  }

  @Test
  void 칩은_지급_시기만_바꾸고_다른_조건과_정렬은_싣는다() {
    String html = render("MONTH_END", Map.of());

    String all = chip(html, "all");
    assertThat(all)
        .contains("keyword=TIGER")
        .contains("account=PENSION")
        .contains("sort=symbol&direction=desc")
        .doesNotContain("payoutWindow");
    String mid = chip(html, "MID_MONTH");
    assertThat(mid).contains("keyword=TIGER").contains("account=PENSION").contains("sort=symbol");
    assertThat(mid.split("payoutWindow=", -1)).as("지급 시기는 한 번만").hasSize(2);
    assertThat(mid).contains("payoutWindow=MID_MONTH").doesNotContain("MONTH_END");
    // 폼에는 지급 시기 칸이 없다 - 숨은 칸이 지금 값을 실어 "적용" 해도 풀리지 않는다(2026-10-13 칩만 남김).
    assertThat(html)
        .doesNotContain("<select name=\"payoutWindow\"")
        .contains("<input type=\"hidden\" name=\"payoutWindow\" value=\"MONTH_END\"");
    Matcher reset = Pattern.compile("<a data-filter-reset href=\"([^\"]+)\"").matcher(html);
    assertThat(reset.find()).isTrue();
    assertThat(reset.group(1))
        .as("필터 지우기는 폼 조건만 비우고 칩이 고른 지급 시기는 남긴다")
        .contains("payoutWindow=MONTH_END")
        .doesNotContain("keyword=")
        .doesNotContain("account=");
  }

  @Test
  void 개수를_모르면_칩만_그린다() {
    String html = render("", Map.of());
    assertThat(chip(html, "all")).contains("aria-current=\"true\"");
    String group =
        html.substring(
            html.indexOf("data-monthly-window-chips"),
            html.indexOf("</div>", html.indexOf("data-monthly-window-chips")));
    assertThat(group).doesNotContain("tabular-nums");
  }

  /**
   * 저장 · 시트 가져오기 폼도 지금 표 좁히기와 정렬을 싣는다(2026-10-13 자율 - 예전에는 정렬만 실어 저장할 때마다 검색어 · 지급 시기 · 계좌가 풀렸다). 두
   * 폼은 표 구역 밖이라 구역을 바꿔 끼울 때 이 묶음만 함께 바꾼다(hx-select-oob).
   */
  @Test
  void 저장_폼과_시트_가져오기_폼이_지금_조건을_싣는다() {
    String html = render("MONTH_END", Map.of());
    for (String carry :
        List.of("monthlyDividendFilterCarrySave", "monthlyDividendFilterCarryImport")) {
      int at = html.indexOf("id=\"" + carry + "\"");
      assertThat(at).as(carry).isNotNegative();
      String block = html.substring(at, html.indexOf("</div>", at));
      assertThat(block)
          .as(carry)
          .contains("name=\"keyword\" value=\"TIGER\"")
          .contains("name=\"account\" value=\"PENSION\"")
          .contains("name=\"payoutWindow\" value=\"MONTH_END\"")
          .contains("name=\"sort\" value=\"symbol\"")
          .contains("name=\"direction\" value=\"desc\"");
      int form = html.lastIndexOf("<form method=\"post\"", at);
      assertThat(html.indexOf("</form>", form)).as(carry + " 는 폼 안").isGreaterThan(at);
    }
    assertThat(html)
        .contains(
            "hx-select-oob=\"#simulatorTabs,#monthlyDividendFilterCarrySave,#monthlyDividendFilterCarryImport\"");
  }
}
