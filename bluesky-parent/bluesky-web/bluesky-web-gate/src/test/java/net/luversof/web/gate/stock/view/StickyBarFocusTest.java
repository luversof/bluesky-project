package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 붙박이 막대(상단바 · 구역 막대)와 표의 고정 머리가 키보드 포커스를 가리거나 페이지를 튀게 하지 않는다(WCAG 2.4.11, 2026-09-17).
 *
 * <p>실측(진짜 Tab, focus-obscured-walk.js · bar-focus-jump.js · section-nav-keyscroll.js):
 *
 * <ul>
 *   <li>포커스 스크롤은 요소가 html scroll-padding-top 안쪽이면 이미 보여도 가운데로 굴린다 &mdash; 상단바 안 Tab 52 ~ 90 걸음, 구역
 *       막대 18 ~ 28 걸음마다 페이지가 수백 px 튀었다. 고정(fixed) 독 · 사이드바는 0. 음수 scroll-margin 으로 상쇄해 0.
 *   <li>상단바는 줄이 접혀 64 ~ 352px 인데 구역 막대는 71px 고정값에 붙어 375px 기본 글꼴에서 41px 중 29px, 글꼴 150 ~ 200% 에서는
 *       전부가 상단바 밑에 깔렸다. common.ts 가 실제 높이를 --site-header-height 로 알려 15 조건 모두 덮임 0.
 *   <li>상단바 lg 미만 반반 나누기 탓에 375px 기본 글꼴에서도 두 줄(100px)이었다. 내용 폭 기준으로 64px 한 줄, 겹침 21 조건 -> 0.
 *   <li>칩에 키보드 포커스를 둔 채 ↓ · PageDown 이면 막대가 현재 구역 칩으로 되굴러 21 번 중 21 번 포커스 칩이 밖으로 나갔다 -> 0.
 *   <li>월배당 시뮬 표(두 줄 머리, 첫 칸 rowspan): 둘째 줄 첫 th 가 첫 열처럼 고정되고, table-pin-rows 의 줄별 쌓임 탓에 둘째 줄 칸들이 첫
 *       칸을 덮어 정렬 링크 둘이 가려졌다(8 건) -> 0.
 * </ul>
 *
 * <p>소스는 주석을 지우고 공백을 눌러 보고, 규칙이 어느 @media · @layer 안에 있는지까지 본다. 메이븐은 프론트엔드를 빌드하지 않으므로 산출물도 본다.
 */
class StickyBarFocusTest {

  private static String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  private static String cssSource() throws IOException {
    return read("src/main/frontend/main.css")
        .replaceAll("(?s)/\\*.*?\\*/", "")
        .replaceAll("\\s+", " ");
  }

  private static String cssBuilt() throws IOException {
    return read("src/main/resources/static/main.css");
  }

  private static String tsSquashed() throws IOException {
    return read("src/main/frontend/src/common.ts").replaceAll("\\s+", "");
  }

  private static String jsBuilt() throws IOException {
    return read("src/main/resources/static/js/common.js");
  }

  /** pos 를 감싼 블록 머리들(바깥부터). 주석을 지운 CSS 에 쓴다. */
  private static List<String> enclosing(String css, int pos) {
    Deque<String> stack = new ArrayDeque<>();
    int headStart = 0;
    for (int i = 0; i < pos; i++) {
      char c = css.charAt(i);
      if (c == '{') {
        stack.addLast(css.substring(headStart, i).trim());
        headStart = i + 1;
      } else if (c == '}') {
        if (!stack.isEmpty()) {
          stack.removeLast();
        }
        headStart = i + 1;
      } else if (c == ';') {
        headStart = i + 1;
      }
    }
    return new ArrayList<>(stack);
  }

  private static int indexOfOnce(String haystack, String needle, String what) {
    int at = haystack.indexOf(needle);
    assertThat(at).as(what + " 이(가) 없다: " + needle).isGreaterThanOrEqualTo(0);
    assertThat(haystack.indexOf(needle, at + 1)).as(what + " 이(가) 두 번 있다").isLessThan(0);
    return at;
  }

  @Test
  void 붙박이_막대_안_항목은_포커스해도_페이지가_튀지_않는다() throws IOException {
    String css = cssSource();
    int padding =
        indexOfOnce(css, "html { scroll-padding-top: var(--sticky-top-stack); }", "기준점 여백");
    assertThat(enclosing(css, padding)).as("여백은 모든 폭에 걸린다").isEmpty();
    int margin =
        indexOfOnce(
            css,
            "header.navbar *, .page-section-nav * { scroll-margin-top: calc(-1 * var(--sticky-top-stack)); }",
            "막대 안 항목 음수 여백");
    assertThat(enclosing(css, margin))
        .as("1440px 에서도 상단바 Tab 144 걸음 중 52 걸음이 튀었다 - @media 에 가두면 안 된다")
        .isEmpty();

    String built = cssBuilt();
    int builtMargin =
        indexOfOnce(
            built,
            "header.navbar *,.page-section-nav *{scroll-margin-top:calc(-1 * var(--sticky-top-stack))}",
            "산출물 음수 여백");
    assertThat(enclosing(built, builtMargin)).isEmpty();
    assertThat(built).contains("html{scroll-padding-top:var(--sticky-top-stack)}");
  }

  @Test
  void 상단바_실제_높이를_알려_구역_막대와_기준점_여백이_따라간다() throws IOException {
    String navbar = read("src/main/jte/_components/body/navbar/navbar.jte");
    assertThat(navbar)
        .as("전제: 붙박이 상단바를 header.navbar 로 찾는다")
        .contains("<header class=\"navbar ")
        .contains(" sticky top-0 ");
    assertThat(cssSource())
        .as("변수가 오기 전에는 한 줄 높이(71px), 막대는 41px")
        .contains(
            ":root { --sticky-top-stack: calc(var(--site-header-height, 4.4375rem) + 2.5625rem); }")
        .contains("position: sticky; top: var(--site-header-height, 4.4375rem);");

    assertThat(tsSquashed())
        .contains(
            "if(height>0)document.documentElement.style.setProperty(\"--site-header-height\",height+\"px\");")
        .contains("constheader=document.querySelector(\"header.navbar\");")
        .as("글꼴 · 폭이 바뀌어 줄이 접히면 다시 알린다")
        .contains("newResizeObserver(()=>syncSiteHeaderHeight(header)).observe(header);")
        .contains(
            "if(document.readyState===\"loading\")document.addEventListener(\"DOMContentLoaded\",watchSiteHeaderHeight,{once:true});elsewatchSiteHeaderHeight();");
    assertThat(jsBuilt())
        .as("메이븐은 프론트엔드를 빌드하지 않는다 - npm run build 산출물이 배포본이다")
        .contains(
            "height>0&&document.documentElement.style.setProperty(\"--site-header-height\",height+\"px\")")
        .contains("new ResizeObserver(()=>syncSiteHeaderHeight(header)).observe(header)")
        .contains(
            "document.readyState===\"loading\"?document.addEventListener(\"DOMContentLoaded\",watchSiteHeaderHeight,{once:!0}):watchSiteHeaderHeight()");
    assertThat(cssBuilt())
        .contains(":root{--sticky-top-stack:calc(var(--site-header-height,4.4375rem) + 2.5625rem)}")
        .contains("top:var(--site-header-height,4.4375rem)");
  }

  @Test
  void 키보드로_칩을_고르는_중에는_구역_막대를_되굴리지_않는다() throws IOException {
    assertThat(tsSquashed())
        .contains(
            "constchoosingByKeyboard=!!focused&&nav.contains(focused)&&focused.matches(\":focus-visible\");")
        .contains("if(list&&currentLink&&!choosingByKeyboard&&list.scrollWidth>list.clientWidth){");
    assertThat(jsBuilt())
        .contains(
            "choosingByKeyboard=!!focused&&nav.contains(focused)&&focused.matches(\":focus-visible\")")
        .contains("list&&currentLink&&!choosingByKeyboard&&list.scrollWidth>list.clientWidth)");
  }

  @Test
  void lg_미만_상단바는_들어가면_한_줄_안_들어가면_묶음째_다음_줄() throws IOException {
    String css = cssSource();
    int base =
        indexOfOnce(
            css,
            ".navbar-end { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; flex: 1 1 0; justify-content: flex-end; }",
            "오른쪽 묶음 기본 규칙");
    int narrow =
        indexOfOnce(
            css,
            "@media (width < 64rem) { .navbar { row-gap: 0; } .navbar-start, .navbar-end { flex-basis: auto; } }",
            "lg 미만 내용 폭 기준");
    assertThat(narrow).as("같은 우선순위는 뒤에 온 것이 이긴다 - flex: 1 1 0 뒤에 둔다").isGreaterThan(base);
    assertThat(enclosing(css, narrow)).containsExactly("@layer components");

    String built = cssBuilt();
    int builtBase = built.indexOf(".navbar-end{");
    int builtNarrow =
        indexOfOnce(
            built,
            "@media not all and (min-width:64rem){.navbar{row-gap:0}.navbar-start,.navbar-end{flex-basis:auto}}",
            "산출물 lg 미만 규칙");
    assertThat(builtBase).isGreaterThanOrEqualTo(0).isLessThan(builtNarrow);
    assertThat(enclosing(built, builtNarrow)).containsExactly("@layer components");
  }

  /**
   * 좁은 화면에서 표 첫 열을 고정하지 않는다(사용자 결정 2026-09-21).
   *
   * <p>전에는 "가로로 밀 때 어느 행인지 놓치지 않도록" 48rem 미만에서 첫 칸을 왼쪽에 붙였다. 그런데 종목이 늘며 고정 첫 열이 표 상자를 통째로 먹었다
   * &mdash; 실측 375px: 월배당 시뮬레이터가 상자 293px 중 259px 를 고정에 내주어 뒤 열이 0/10 칸, 375px/200%: 고정 519px 가 상자
   * 213px 보다 커 대상 표 8 개 중 7 개가 뒤 열을 한 칸도 못 보여 줬다. 고정을 풀면 각각 0 개가 된다(폭 상한 120px 만으로는 200% 에서 6 개가
   * 그대로였다).
   *
   * <p>머리 줄 고정(table-pin-rows)은 그대로 둔다 &mdash; 세로로 밀 때 열 이름이 필요하다.
   */
  @Test
  void 좁은_화면에서_표_첫_열을_고정하지_않는다() throws IOException {
    String css = cssSource();
    int pinnedRows =
        indexOfOnce(
            css, ".table-pin-rows thead tr { position: sticky; top: 0; z-index: 1;", "머리 줄 고정 규칙");
    assertThat(enclosing(css, pinnedRows)).containsExactly("@layer components");

    // 첫 열을 왼쪽에 붙이는 규칙이 하나도 없어야 한다 - 붙이는 줄만 보지 말고 그 줄을 감싼 셀렉터까지 보고 센다.
    List<String> firstColumnSticky = new ArrayList<>();
    for (int at = css.indexOf("position: sticky");
        at >= 0;
        at = css.indexOf("position: sticky", at + 1)) {
      List<String> blocks = enclosing(css, at);
      String selector = blocks.isEmpty() ? "" : blocks.get(blocks.size() - 1);
      if (selector.contains(":first-child")) {
        firstColumnSticky.add(selector);
      }
    }
    assertThat(firstColumnSticky).as("첫 열 고정 규칙이 되살아났다").isEmpty();
    assertThat(css)
        .as("고정 칸 배경을 맞추던 변수도 함께 걷어낸다(줄 색은 tr.sim-tint-* > td 가 칠한다)")
        .doesNotContain("--sticky-first-bg")
        .doesNotContain("--row-zebra");
    assertThat(read("src/main/jte/stock/htmx/fragments/assetStatus.jte"))
        .as("선택 행이 고정 칸에 넘기던 색도 쓰이지 않는다")
        .doesNotContain("--sticky-first-bg");

    String built = cssBuilt();
    assertThat(built)
        .as("메이븐은 프론트엔드를 빌드하지 않는다 - 산출물에도 첫 열 고정이 없어야 한다")
        .doesNotContain("first-child{position:sticky");
    assertThat(built)
        .contains(
            ".table-pin-rows thead tr{z-index:1;background-color:var(--color-base-100);position:sticky;top:0}");

    // 포커스 보이기는 첫 칸이 '고정일 때만' 그 폭을 뺀다. 이 확인이 빠지면 고정을 푼 표에서도 첫 칸 폭만큼 더 밀어
    // 포커스한 칸을 상자 밖으로 보낸다(고정을 푼 뒤 이 줄이 유일한 방어다).
    assertThat(tsSquashed())
        .contains("if(!first||getComputedStyle(first).position!==\"sticky\")return0;");
    assertThat(jsBuilt())
        .contains("return!first||getComputedStyle(first).position!==\"sticky\"?0:");
  }
}
