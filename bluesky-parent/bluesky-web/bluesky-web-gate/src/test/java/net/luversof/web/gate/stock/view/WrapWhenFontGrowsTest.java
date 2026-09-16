package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 넘칠 수 있는 줄은 <b>줄을 바꾼다</b> &mdash; 넘침을 뷰포트 폭으로만 막으면 <b>글자가 커지는 축</b>에는 안 듣는다.
 *
 * <p>상단바의 글자 라벨은 {@code xl:} 부터, 비중 막대는 {@code max-[400px]:hidden} 으로 막아 뒀는데 둘 다 <b>뷰포트 폭</b> 기준이라
 * 창이 넓고 글꼴만 큰 상태에서는 발동하지 않는다. 실측 2026-09-16(글꼴 200%, 1440px): 11 화면 <b>모두</b> 문서가 486px 가로로 넘쳤다.
 *
 * <p>이어서 좁은 폭까지 겹친 자리(320px · 글꼴 200%)를 줄였다 &mdash; <b>11 화면 72~226px &rarr; 7 화면 3~65px</b>. 처방은
 * 모두 같은 결이다: <b>긴 것은 줄을 바꿀 수 있어야 한다.</b> 폭 조건을 쓰지 않으므로 어느 축에서도 듣는다.
 *
 * <ul>
 *   <li>{@code .navbar} · {@code .navbar-end} &mdash; 줄바꿈 허용
 *   <li>{@code .amount-value} &mdash; 금액은 공백 없는 한 덩어리라 {@code overflow-wrap} 이 있어야 끊긴다
 *   <li>기간 배지 &mdash; {@code .badge} 기본이 {@code nowrap} 이라 <b>유틸로는 못 이긴다</b>. 표식을 달고 규칙으로 푼다
 *   <li>상세의 날짜 입력 두 칸 · 비중 막대 행 &mdash; 줄바꿈 허용
 * </ul>
 *
 * <p>기본 글꼴에서는 아무것도 안 바뀐다 &mdash; 상단바는 여섯 폭(320~1920)에서 높이·넘침 그대로, 비중 막대 행은 높이 20px, 기간 배지는 198x20
 * 그대로다. 합이 부모보다 작으면 줄바꿈은 일어나지 않기 때문이다.
 *
 * <p>브라우저가 읽는 것은 <b>산출물</b>이라 CSS 는 원본과 산출물을 함께 본다 &mdash; 메이븐은 프론트엔드를 안 빌드하므로 고치고 빌드를 잊으면 배포본만 옛것이
 * 된다.
 */
class WrapWhenFontGrowsTest {

  private static final String CSS_SOURCE = "src/main/frontend/main.css";
  private static final String CSS_BUILT = "src/main/resources/static/main.css";
  private static final String ALLOCATION = "src/main/jte/stock/htmx/fragments/allocationBars.jte";
  private static final String RANGE_BAR =
      "src/main/jte/stock/htmx/fragments/components/dateRangeNavBar.jte";
  private static final String DETAIL_FILTER =
      "src/main/jte/stock/htmx/fragments/components/detailDateFilter.jte";

  @Test
  void 상단바는_원본과_산출물_모두_줄바꿈을_허용한다() throws IOException {
    for (String css : new String[] {CSS_SOURCE, CSS_BUILT}) {
      String text = Files.readString(Path.of(css), StandardCharsets.UTF_8);
      assertThat(ruleOf(text, ".navbar")).as(css + " 의 .navbar").contains("flex-wrap");
      assertThat(ruleOf(text, ".navbar-end")).as(css + " 의 .navbar-end").contains("flex-wrap");
    }
  }

  /** 금액은 공백 없는 한 덩어리다 - overflow-wrap 이 없으면 자리가 모자라도 안 끊긴다. */
  @Test
  void 금액은_자리가_모자라면_줄을_바꾼다() throws IOException {
    for (String css : new String[] {CSS_SOURCE, CSS_BUILT}) {
      assertThat(Files.readString(Path.of(css), StandardCharsets.UTF_8))
          .as(css)
          .contains("overflow-wrap");
    }
  }

  /**
   * 기간 배지는 <b>표식과 규칙이 짝</b>이라야 듣는다.
   *
   * <p>{@code .badge} 기본이 {@code white-space: nowrap} 이라 JTE 에서 유틸을 떼도 아무 일도 일어나지 않는다(실측
   * 2026-09-16: 유틸만 뗐을 때 넘침이 그대로 114px 이었다). 규칙이 표식을 집으므로 <b>둘 다</b> 있어야 한다.
   */
  @Test
  void 기간_배지는_표식과_규칙이_짝이다() throws IOException {
    String jte = Files.readString(Path.of(RANGE_BAR), StandardCharsets.UTF_8);
    for (String mark : new String[] {"data-range-text", "data-covered-range"}) {
      assertThat(jte).as(RANGE_BAR + " 의 " + mark).contains(mark);
      for (String css : new String[] {CSS_SOURCE, CSS_BUILT}) {
        assertThat(Files.readString(Path.of(css), StandardCharsets.UTF_8))
            .as(css + " 가 " + mark + " 를 집는 규칙")
            .contains("[" + mark + "]");
      }
    }
  }

  /** 비중 막대의 <b>모든</b> 행이 줄바꿈을 허용한다 - 한 줄만 고치면 다른 줄이 조용히 남는다. */
  @Test
  void 비중_막대_행도_줄바꿈을_허용한다() throws IOException {
    String jte = Files.readString(Path.of(ALLOCATION), StandardCharsets.UTF_8);
    int wrapped = countOf(jte, "flex flex-wrap items-center justify-between gap-2 text-sm");
    // 줄바꿈이 빠진 행은 "flex items-center ..." 로 남는다(가운데에 flex-wrap 이 없다).
    int unwrapped = countOf(jte, "\"flex items-center justify-between gap-2 text-sm");

    assertThat(wrapped).as("줄바꿈을 허용한 행").isGreaterThanOrEqualTo(2);
    assertThat(unwrapped).as("줄바꿈 없이 남은 행").isZero();
  }

  /** 상세의 날짜 입력 두 칸은 붙여 그리되, 한 줄에 안 들어가면 아래로 내려간다. */
  @Test
  void 상세_날짜_입력도_줄을_바꾼다() throws IOException {
    assertThat(Files.readString(Path.of(DETAIL_FILTER), StandardCharsets.UTF_8))
        .as(DETAIL_FILTER)
        .contains("class=\"join flex-wrap\"");
  }

  /** CSS 주석을 지운다 - 주석 속 글자를 코드로 세면 안 된다. */
  private static String stripComments(String css) {
    StringBuilder sb = new StringBuilder();
    int at = 0;
    while (at < css.length()) {
      int open = css.indexOf("/*", at);
      if (open < 0) {
        sb.append(css, at, css.length());
        break;
      }
      sb.append(css, at, open);
      int close = css.indexOf("*/", open);
      if (close < 0) {
        break;
      }
      at = close + 2;
    }
    return sb.toString();
  }

  private static int countOf(String source, String needle) {
    int n = 0;
    int at = source.indexOf(needle);
    while (at >= 0) {
      n++;
      at = source.indexOf(needle, at + needle.length());
    }
    return n;
  }

  /**
   * 주어진 선택자의 <b>기본</b> 규칙 블록(= display 를 가진 것). 글자만 세면 미디어쿼리 안의 같은 이름 블록이나 딴 규칙의 값에 속는다 - 그 선택자가 값을
   * 감싸는지까지 본다.
   */
  private static String ruleOf(String rawCss, String selector) {
    // 판정 전에 주석을 지운다 - 주석에 적은 설명에 선택자 이름이 들어 있으면 그 다음 블록을 집는다
    // (실측 2026-09-16: .navbar 주석에 ".navbar-end" 라고 써 둔 탓에 변이가 안 잡혔다).
    String css = stripComments(rawCss);
    int at = css.indexOf(selector);
    while (at >= 0) {
      char next = at + selector.length() < css.length() ? css.charAt(at + selector.length()) : ' ';
      // ".navbar" 로 ".navbar-end" 를 집지 않게 뒤 글자를 본다.
      if (next == '{' || next == ' ' || next == ',') {
        int open = css.indexOf('{', at);
        int close = open >= 0 ? css.indexOf('}', open) : -1;
        if (open >= 0 && close > open) {
          String block = css.substring(open + 1, close);
          if (block.contains("display")) {
            return block;
          }
        }
      }
      at = css.indexOf(selector, at + 1);
    }
    throw new AssertionError(selector + " 기본 규칙을 못 찾았다");
  }
}
