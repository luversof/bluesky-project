package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;

/**
 * 관리 &gt; 월배당 기준 데이터의 프로필 목록은 지급 시기 칩 · 정렬 머리로 목록 구역만 바꿔 끼운다(사용자 요청 2026-10-13 - 시뮬레이터 · 월배당 ETF 와
 * 같은 방식).
 *
 * <p>목록 밖이지만 같은 값을 싣는 것: 끌어 옮기기 안내(거르면 꺼진다) · 지급 이력 행 링크 - 함께 바꾼다(hx-select-oob). 왼쪽 편집 폼은 입력값을
 * 지키려고 바꾸지 않고, 숨은 정렬 · 거르기 칸만 구역의 data-* 값으로 맞춘다. 종목 고르기(행 링크)는 편집 폼을 바꾸므로 부스트하지 않는다.
 */
class MonthlyReferenceListSwapTest {

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    source.setFallbackToSystemLocale(false);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private static MonthlyDividendProfileResponse profile(String symbol) {
    return new MonthlyDividendProfileResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        symbol,
        symbol + " 이름",
        "",
        "MONTH_END",
        1,
        true,
        "",
        null,
        Instant.parse("2026-09-01T00:00:00Z"),
        null,
        null);
  }

  private static String render() {
    Map<String, Object> model = new HashMap<>();
    model.put("monthlyDividendProfiles", List.of(profile("END1")));
    model.put("monthlyDividendProfileWindow", "MONTH_END");
    model.put("monthlyDividendProfileSort", "symbol");
    model.put("monthlyDividendProfileDirection", "desc");
    model.put("selectedMonthlyDividendSymbol", "END1");
    model.put("monthlyDividendProfileExists", true);
    model.put("monthlyDividendPayoutExists", true);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/fragments/monthlyDividendReference.jte", model, output);
    return output.toString();
  }

  private static String openTag(String html, String marker) {
    int at = html.indexOf(marker);
    assertThat(at).as("없음: " + marker).isNotNegative();
    return html.substring(html.lastIndexOf('<', at), html.indexOf('>', at) + 1);
  }

  @Test
  void 목록_구역이_자기만_바꾸고_같은_값을_싣는_곳도_함께_바꾼다() {
    String html = render();
    String region = openTag(html, "id=\"monthlyReferenceProfileList\"");
    assertThat(region)
        .contains("hx-target=\"#monthlyReferenceProfileList\"")
        .contains("hx-select=\"#monthlyReferenceProfileList\"")
        .contains("show:none")
        .contains("hx-replace-url=\"true\"")
        .contains("hx-select-oob=\"#monthlyReferenceReorderNote,#monthlyReferencePayoutList\"")
        .contains("data-swap-anchor=\"[data-profile-window-filter]\"")
        .contains("data-profile-sort-value=\"symbol\"")
        .contains("data-profile-direction-value=\"desc\"")
        .contains("data-profile-window-value=\"MONTH_END\"");
    assertThat(html)
        .contains("id=\"monthlyReferenceReorderNote\"")
        .contains("id=\"monthlyReferencePayoutList\"");
    // 칩 · 표 · 끌어 옮기기 목록이 구역 안
    int start = html.indexOf("id=\"monthlyReferenceProfileList\"");
    assertThat(html.indexOf("data-profile-window-filter")).isGreaterThan(start);
    assertThat(html.indexOf("monthlyDividendProfileOrderList")).isGreaterThan(start);
    assertThat(html.indexOf("id=\"monthlyReferencePayoutList\""))
        .isGreaterThan(html.indexOf("monthlyDividendProfileOrderList"));
  }

  @Test
  void 칩과_정렬_머리만_부스트하고_종목_고르기는_페이지를_옮긴다() {
    String html = render();
    // 구역의 data-swap-anchor 값("[data-profile-window-filter]")이 먼저 걸리지 않게 속성 자리(뒤에 공백)로 찾는다.
    assertThat(openTag(html, "data-profile-window-filter "))
        .contains("role=\"group\"")
        .contains("hx-boost=\"true\"");
    int list = html.indexOf("id=\"monthlyReferenceProfileList\"");
    int thead = html.indexOf("<thead", list);
    assertThat(html.substring(thead, html.indexOf('>', thead) + 1)).contains("hx-boost=\"true\"");
    String tbody =
        html.substring(
            html.indexOf("monthlyDividendProfileOrderList"), html.indexOf("</tbody>", list));
    assertThat(tbody).as("행 안의 종목 고르기 링크는 편집 폼을 바꾸므로 부스트하지 않는다").doesNotContain("hx-boost");
  }

  @Test
  void 편집_폼의_숨은_정렬_거르기_칸을_맞추는_스크립트가_있다() {
    String html = render();
    assertThat(html)
        .contains("document.addEventListener('htmx:afterSettle'")
        .contains("list.dataset.profileWindowValue")
        .contains("input[type=\"hidden\"][name=\"");
  }
}
