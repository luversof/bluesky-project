package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 큰 글꼴 + 폰 폭에서 탭 글자와 카드 글자가 한두 자씩 세로로 쌓이지 않는다(사용자 선택 2026-09-17: 탭 줄 · 2 열 카드부터).
 *
 * <p>탭 줄: 높이 2rem 인 flex-1 탭 안에서 글자가 여러 줄로 접혀 탭 밖으로 넘쳤다 &mdash; 실측(브라우저 기본 글꼴 설정): 글꼴 200% 375px
 * 시뮬레이터 "지속가능성" 5 줄 · 자산 성장 표 탭 4 ~ 7 줄, 기본 글꼴에서도 폰 폭 자산 성장 표 탭 2 ~ 4 줄. 고른 방식은 탭을 글자 폭 밑으로 줄이지 않고
 * 줄 안에서 가로 스크롤(접힌 탭 기본 글꼴 14 -> 0 · 200% 41 -> 0, 넘치지 않는 탭 줄 25/30 은 그대로). 지금 탭이 줄 밖이면 줄만 옆으로 옮겨
 * 보인다(common.ts revealActiveTabs, tabScrollReveal.test.mjs).
 *
 * <p>카드: 격자 폭이 17rem 보다 좁으면 한 줄 한 장. 기본 글꼴에서 가장 좁은 격자가 18rem(320px) 이라 기본 글꼴 화면의 열 수는 24 곳 모두
 * 그대로이고, 글꼴 200% 의 320 ~ 414px 만 한 장씩이 된다(카드 안쪽 1.4 ~ 2.8rem -> 5.8 ~ 8.7rem). 기준을 18rem 이상으로 올리면
 * 기본 글꼴 320px 이 바뀐다.
 *
 * <p>같은 우선순위 규칙은 뒤에 온 것이 이긴다 &mdash; .tabs-scroll 은 .tabs(flex-wrap: wrap) 뒤에 있어야 한다. 소스는 주석을 지우고
 * 공백을 눌러 본다.
 */
class LargeFontTabsAndCardGridTest {

  private static String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  private static String cssSource() throws IOException {
    return read("src/main/frontend/main.css")
        .replaceAll("(?s)/\\*.*?\\*/", "")
        .replaceAll("\\s+", " ");
  }

  private static String built() throws IOException {
    return read("src/main/resources/static/main.css");
  }

  private static int count(String haystack, String needle) {
    int n = 0;
    for (int at = haystack.indexOf(needle);
        at >= 0;
        at = haystack.indexOf(needle, at + needle.length())) {
      n++;
    }
    return n;
  }

  /** 산출물 규칙 한 덩어리(선택자 바로 뒤 중괄호 안). 속성 순서는 빌드가 바꾼다. */
  private static String builtRule(String css, String selector) {
    Matcher m = Pattern.compile(Pattern.quote(selector) + "\\{([^}]*)\\}").matcher(css);
    assertThat(m.find()).as("산출물에 " + selector + " 규칙이 없다 - npm run build").isTrue();
    return m.group(1);
  }

  @Test
  void 탭_줄은_접지_않고_가로로_스크롤한다() throws IOException {
    String css = cssSource();
    int tabs = css.indexOf(".tabs { display: flex; flex-wrap: wrap;");
    int scroll =
        css.indexOf(".tabs-scroll { flex-wrap: nowrap; overflow-x: auto; position: relative; }");
    assertThat(tabs).as("탭 기본 규칙을 못 찾았다 - 검사 전제").isGreaterThan(0);
    assertThat(scroll).as("원본에 가로 스크롤 탭 줄 규칙이 없다").isGreaterThan(tabs);
    assertThat(css)
        .contains(".tabs-scroll > .tab { min-width: max-content; white-space: nowrap; }");

    String out = built();
    assertThat(builtRule(out, ".tabs-scroll"))
        .contains("flex-wrap:nowrap")
        .contains("overflow-x:auto")
        .contains("position:relative");
    assertThat(builtRule(out, ".tabs-scroll>.tab"))
        .contains("min-width:max-content")
        .contains("white-space:nowrap");
    assertThat(out.indexOf(".tabs-scroll{"))
        .as("산출물에서도 .tabs 뒤")
        .isGreaterThan(out.indexOf(".tabs{"));
  }

  /** 글자 폭이 넉넉하지 않은 flex-1 탭 줄 다섯 곳. */
  @Test
  void 주식_화면의_flex_1_탭_줄은_모두_스크롤_줄이다() throws IOException {
    Map<String, Integer> bars =
        Map.of(
            "src/main/jte/stock/simulator.jte", 1,
            "src/main/jte/stock/admin.jte", 1,
            "src/main/jte/stock/htmx/asset-growth.jte", 1,
            "src/main/jte/stock/htmx/fragments/activityList.jte", 1,
            "src/main/jte/stock/htmx/fragments/dividend/dividendPeriodBreakdown.jte", 1);
    for (Map.Entry<String, Integer> bar : bars.entrySet()) {
      String jte = read(bar.getKey());
      assertThat(count(jte, "class=\"tabs tabs-box"))
          .as(bar.getKey() + ": 스크롤 줄이 아닌 탭 줄이 남았다")
          .isZero();
      assertThat(count(jte, "class=\"tabs tabs-scroll tabs-box"))
          .as(bar.getKey())
          .isEqualTo(bar.getValue());
    }
  }

  @Test
  void 지금_탭이_줄_밖이면_줄만_옮긴다() throws IOException {
    String ts = read("src/main/frontend/src/common.ts");
    assertThat(ts)
        .contains("document.addEventListener(\"DOMContentLoaded\", () => revealActiveTabs());")
        .contains("document.addEventListener(\"htmx:afterSettle\", () => revealActiveTabs());");
    // 복원 등록이 없으면 indexOf 가 -1 이라 순서 비교가 늘 참이 된다(복사본 변이로 확인) - 먼저 있는지 본다.
    int restore = ts.indexOf("document.addEventListener(\"DOMContentLoaded\", restorePanelTabs);");
    assertThat(restore).as("패널 탭 복원 등록을 못 찾았다 - 순서 검사의 전제").isGreaterThan(0);
    assertThat(ts.indexOf("document.addEventListener(\"DOMContentLoaded\", () => revealActiveTabs());"))
        .as("저장된 패널 탭을 복원한 뒤에 옮겨야 한다")
        .isGreaterThan(restore);
    assertThat(read("src/main/resources/static/js/common.js"))
        .as("메이븐은 프론트엔드를 빌드하지 않는다 - npm run build 산출물이 배포본이다(선택자는 통째로 본다)")
        .contains("function revealActiveTabs(")
        .contains("querySelectorAll(\".tabs-scroll\")");
  }

  @Test
  void 카드_격자는_좁으면_한_줄에_한_장() throws IOException {
    String css = cssSource();
    assertThat(css)
        .contains(".stat-card-grid { container: stat-card-grid / inline-size; }")
        .as("17rem - 기본 글꼴의 가장 좁은 격자(18rem)보다 작아야 기본 글꼴 화면이 안 바뀐다")
        .contains(
            "@container stat-card-grid (max-width: 17rem) { .stat-card-grid > *, .stat-card-grid > .contents > * { grid-column: 1 / -1; } }");

    String out = built();
    assertThat(builtRule(out, ".stat-card-grid")).contains("container:stat-card-grid/inline-size");
    assertThat(out)
        .contains(
            "@container stat-card-grid (max-width:17rem){.stat-card-grid>*,.stat-card-grid>.contents>*{grid-column:1/-1}}");
  }

  /** 지표 카드 격자 여섯 곳(대시보드 요약 둘 · 그 스켈레톤 둘 · 계좌 상세 · 종목 상세). 스켈레톤이 빠지면 큰 글꼴에서 조각이 들어올 때 밀린다. */
  @Test
  void 지표_카드_격자마다_표식이_있다() throws IOException {
    Map<String, Integer> grids =
        Map.of(
            "src/main/jte/stock/htmx/fragments/summary.jte", 2,
            "src/main/jte/stock/htmx/dashboardContent.jte", 2,
            "src/main/jte/stock/htmx/accountDetailContent.jte", 1,
            "src/main/jte/stock/htmx/stockItemDetailContent.jte", 1);
    for (Map.Entry<String, Integer> grid : grids.entrySet()) {
      assertThat(count(read(grid.getKey()), "class=\"grid stat-card-grid "))
          .as(grid.getKey())
          .isEqualTo(grid.getValue());
    }
  }
}
