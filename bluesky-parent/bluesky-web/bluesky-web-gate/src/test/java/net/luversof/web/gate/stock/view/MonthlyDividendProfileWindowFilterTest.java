package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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
import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;
import net.luversof.web.gate.stock.service.MonthlyDividendViewSupport;

/**
 * 관리 &gt; 월배당 기준 데이터 목록의 "월중만 · 월말만 보기"(사용자 요청 2026-10-13).
 *
 * <p>거르기는 주소({@code profileWindow})에 실린다. 정렬 머리 링크 · 종목 링크 · 저장 폼(숨은 칸)이 모두 그 값을 들고 다녀야 정렬을 바꾸거나
 * 저장한 뒤에도 거른 목록이 유지된다. 거른 동안에는 끌어 옮기기를 끈다 - 보이는 줄만 다시 번호를 매기면 숨은 줄과 표시 순서가 겹친다.
 */
class MonthlyDividendProfileWindowFilterTest {

  private static final String TEMPLATE = "stock/fragments/monthlyDividendReference.jte";

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

  private static MonthlyDividendProfileResponse profile(
      String symbol, int displayOrder, String window) {
    return new MonthlyDividendProfileResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        symbol,
        symbol + " 이름",
        "",
        window,
        displayOrder,
        true,
        "",
        null,
        Instant.parse("2026-09-01T00:00:00Z"),
        null,
        null);
  }

  private static List<MonthlyDividendProfileResponse> sample() {
    return List.of(
        profile("MID1", 1, "MID_MONTH"),
        profile("END1", 2, "MONTH_END"),
        profile("MID2", 3, "MID_MONTH"),
        profile("NONE", 4, null));
  }

  private static String render(String window, List<MonthlyDividendProfileResponse> rows) {
    Map<String, Long> counts = new LinkedHashMap<>();
    counts.put("", 4L);
    counts.put("MID_MONTH", 2L);
    counts.put("MONTH_END", 1L);
    Map<String, Object> model = new HashMap<>();
    model.put("monthlyDividendProfiles", rows);
    model.put("monthlyDividendProfileWindow", window);
    model.put("monthlyDividendProfileWindowCounts", counts);
    model.put("selectedMonthlyDividendSymbol", "MID1");
    // 저장 · 삭제 폼까지 나오게 - 숨은 칸이 빠진 폼이 없는지 보려면 폼이 다 있어야 한다.
    model.put("monthlyDividendProfileExists", true);
    model.put("monthlyDividendPayoutExists", true);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    // 표현식으로 나온 주소의 & 는 &amp; 로 이스케이프된다(속성 안이라 브라우저는 & 로 읽는다) - 주소 비교는 풀어서 한다.
    return output.toString().replace("&amp;", "&");
  }

  private static int count(String html, String needle) {
    return html.split(Pattern.quote(needle), -1).length - 1;
  }

  @Test
  void 거르기_값은_월중_월말만_받고_나머지는_전체다() {
    MonthlyDividendViewSupport support = new MonthlyDividendViewSupport();
    assertThat(support.resolveProfileWindow("mid_month ")).isEqualTo("MID_MONTH");
    assertThat(support.resolveProfileWindow("MONTH_END")).isEqualTo("MONTH_END");
    assertThat(support.resolveProfileWindow("WEEKLY")).isEmpty();
    assertThat(support.resolveProfileWindow(null)).isEmpty();
  }

  @Test
  void 지급_시기가_같은_프로필만_순서대로_남긴다() {
    assertThat(
            MonthlyDividendViewSupport.filterProfilesByWindow(sample(), "MID_MONTH").stream()
                .map(MonthlyDividendProfileResponse::stockItemSymbol))
        .containsExactly("MID1", "MID2");
    assertThat(MonthlyDividendViewSupport.filterProfilesByWindow(sample(), "")).hasSize(4);
  }

  @Test
  void 월말만_볼_때_칩_정렬_링크_종목_링크_저장_폼이_거르기를_들고_다닌다() {
    String html =
        render(
            "MONTH_END", MonthlyDividendViewSupport.filterProfilesByWindow(sample(), "MONTH_END"));

    // 칩: 고른 칩만 aria-current, 개수는 거르기 전 전체 기준
    Matcher current =
        Pattern.compile(
                "<a [^>]*aria-current=\"true\"[^>]*data-profile-window-link=\"([A-Z_a-z]+)\"")
            .matcher(html);
    assertThat(current.find()).isTrue();
    assertThat(current.group(1)).isEqualTo("MONTH_END");
    assertThat(current.find()).as("고른 칩은 하나").isFalse();
    assertThat(html)
        .contains("data-profile-window-link=\"all\"")
        .contains("data-profile-window-link=\"MID_MONTH\"");

    assertThat(html).contains("data-symbol=\"END1\"").doesNotContain("data-symbol=\"MID1\"");

    // 정렬 머리 링크 일곱 개 모두 거르기를 싣는다
    assertThat(count(html, "&profileWindow=MONTH_END&profileSort=")).isEqualTo(7);
    // 저장 폼의 숨은 칸 - profileDirection 숨은 칸과 개수가 같아야 한다
    int directionInputs = count(html, "<input type=\"hidden\" name=\"profileDirection\"");
    assertThat(directionInputs).as("전제: 저장 폼이 그려졌다").isGreaterThanOrEqualTo(5);
    assertThat(count(html, "<input type=\"hidden\" name=\"profileWindow\" value=\"MONTH_END\""))
        .isEqualTo(directionInputs);
    // 종목 링크(편집 · 종목코드)
    assertThat(html)
        .contains(
            "symbol=END1&profileSort=display-order&profileDirection=asc&profileWindow=MONTH_END");

    // 거른 동안 끌어 옮기기는 꺼지고 그 까닭을 적는다
    assertThat(html).doesNotContain("data-profile-order-handle>");
    assertThat(html)
        .contains(
            MessageUtil.getMessage(
                "stock.page.dividend.monthly.reference.profile.list.reorder.filtered"));
  }

  @Test
  void 전체일_때는_거르기를_싣지_않고_끌어_옮기기가_켜진다() {
    String html = render("", sample());
    // 주소에 profileWindow 가 붙는 곳은 월중 · 월말 칩 둘뿐
    assertThat(count(html, "profileWindow=")).isEqualTo(2);
    assertThat(html).contains("data-profile-order-handle>");
    assertThat(
            Pattern.compile("aria-current=\"true\"[^>]*data-profile-window-link=\"all\"")
                .matcher(html)
                .find())
        .isTrue();
  }

  @Test
  void 거른_결과가_비면_거르기_안내를_낸다() {
    String html = render("MID_MONTH", List.of());
    assertThat(html)
        .contains(
            MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.window.empty"))
        .contains("data-profile-window-filter");
  }
}
