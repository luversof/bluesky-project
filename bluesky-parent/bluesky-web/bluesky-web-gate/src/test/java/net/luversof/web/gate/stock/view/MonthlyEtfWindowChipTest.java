package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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

/**
 * 월배당 ETF 화면의 지급 시기 칩(사용자 요청 2026-10-13 - 시뮬레이터처럼 폼의 칸 대신 칩).
 *
 * <p>칩 주소는 지급 시기만 바꾸고 검색어 · 계좌 · 보기 · 기간 · 정렬은 그대로 실어야 한다. 기타 · 미확인은 그런 종목이 있을 때만 낸다(평소 0 이라 칩만
 * 늘어난다). 폼은 지금 값을 숨은 칸으로 실어 "적용" 해도 풀리지 않고, 초기화도 지급 시기는 남긴다.
 */
class MonthlyEtfWindowChipTest {

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

  private static String render(String window, Map<String, Integer> counts) {
    Map<String, Object> model = new HashMap<>();
    model.put("isAuthenticated", true);
    model.put("username", "probe");
    model.put("monthlyEtfPayoutWindow", window);
    model.put("monthlyEtfPayoutWindowCounts", counts);
    model.put("monthlyEtfKeyword", "TIGER");
    model.put("monthlyEtfAccount", "PENSION");
    model.put("monthlyEtfPeriod", 3);
    model.put("monthlyEtfSort", "symbol");
    model.put("monthlyEtfDirection", "desc");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/monthlyEtf.jte", model, output);
    // 표현식으로 나온 주소의 & 는 &amp; 로 이스케이프된다 - 주소 비교는 풀어서 한다.
    return output.toString().replace("&amp;", "&");
  }

  private static Map<String, Integer> counts(int other) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    counts.put("", 21);
    counts.put("MID_MONTH", 9);
    counts.put("MONTH_END", 12);
    counts.put("OTHER", other);
    counts.put("UNKNOWN", 0);
    return counts;
  }

  private static String chip(String html, String key) {
    Matcher m = Pattern.compile("<a [^>]*data-etf-window-chip=\"" + key + "\"[^>]*>").matcher(html);
    assertThat(m.find()).as("칩이 없다: %s", key).isTrue();
    return m.group();
  }

  @Test
  void 고른_칩과_개수를_적고_다른_조건은_싣는다() {
    String html = render("MONTH_END", counts(0));

    assertThat(chip(html, "MONTH_END")).contains("aria-current=\"true\"");
    assertThat(chip(html, "all")).doesNotContain("aria-current");
    String mid = chip(html, "MID_MONTH");
    assertThat(mid)
        .contains("keyword=TIGER")
        .contains("account=PENSION")
        .contains("period=3")
        .contains("sort=symbol&direction=desc")
        .contains("payoutWindow=MID_MONTH")
        .doesNotContain("MONTH_END");
    assertThat(chip(html, "all")).doesNotContain("payoutWindow");
    assertThat(html)
        .contains("<span class=\"tabular-nums\">21</span>")
        .contains("<span class=\"tabular-nums\">9</span>")
        .contains("<span class=\"tabular-nums\">12</span>");
  }

  @Test
  void 기타_미확인은_있을_때만_낸다() {
    assertThat(render("", counts(0))).doesNotContain("data-etf-window-chip=\"OTHER\"");
    assertThat(render("", counts(2))).contains("data-etf-window-chip=\"OTHER\"");
    assertThat(render("UNKNOWN", counts(0)))
        .as("지금 고른 값이면 0 이어도 낸다 - 안 그러면 무엇으로 걸렀는지 안 보인다")
        .contains("data-etf-window-chip=\"UNKNOWN\"");
  }

  @Test
  void 폼에는_칸이_없고_적용과_초기화가_지급_시기를_지킨다() {
    String html = render("MONTH_END", counts(0));
    int form = html.indexOf("<form method=\"get\" action=\"/stock/monthly-etf\"");
    assertThat(form).isNotNegative();
    String formHtml = html.substring(form, html.indexOf("</form>", form));
    assertThat(formHtml)
        .doesNotContain("<select name=\"payoutWindow\"")
        .contains("<input type=\"hidden\" name=\"payoutWindow\" value=\"MONTH_END\"");
    Matcher reset = Pattern.compile("data-filter-reset href=\"([^\"]+)\"").matcher(formHtml);
    assertThat(reset.find()).isTrue();
    assertThat(reset.group(1)).contains("payoutWindow=MONTH_END").doesNotContain("keyword=");
  }
}
