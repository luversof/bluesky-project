package net.luversof.web.gate.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 툴팁은 공용 조각 하나로만 그린다.
 *
 * <p>사용자 지적 2026-11-10: 화면마다 툴팁이 제각각이었다 - CSS 말풍선(data-tip) · 커서를 따라다니는 떠 있는
 * 풍선(data-stock-tag-tooltip) · 점선 밑줄(termHint) · 정렬 단추 전체가 트리거인 머리글, 아이콘도 채운 원 · 테두리 원 · btn-circle
 * 이 섞였다. 화면마다 비슷하게 다시 짜면 또 갈라지므로, 마크업은 {@code _components/ui/tooltip.jte}(설명 아이콘은 그것을 쓰는 {@code
 * infoTip.jte}) 한곳에만 두고 화면은 그 조각을 부른다. 모양 · 여닫기 · 끝 맞춤 · Esc 닫기는 main.css 의 .tooltip 과 common.ts 가
 * 한곳에서 맡는다.
 */
class TooltipConsistencyTest {

  private static final Path JTE = Path.of("src/main/jte");

  /** 공용 조각 밖에서 쓰면 안 되는 것: 여러 줄 상자 · 떠 있는 풍선 · 밑줄 용어 · 직접 고른 말풍선 방향(방향은 tooltip.jte 가 정한다). */
  private static final Pattern RAW_TOOLTIP =
      Pattern.compile(
          "\\btooltip-content\\b|data-stock-tag-tooltip|ui\\.termHint|\\btooltip-(?:bottom|top|left|right)\\b");

  /** 브라우저 기본 title 툴팁. 2026-11-10 주식 화면에서 걷었다 - 모양이 공용 툴팁과 달랐다(사용자 지적). */
  private static final Pattern TITLE_ATTR = Pattern.compile("\\stitle=\"");

  /** 주식 화면 본문(레이아웃 머리 · 옆 메뉴 · PoE · 가계부는 아직 대상이 아니다). */
  private static boolean stockScope(String rel) {
    return rel.startsWith("stock/") || rel.startsWith("_components/ui/");
  }

  private static List<String> scan(java.util.function.BiPredicate<String, String> offends)
      throws IOException {
    List<String> offenders = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(JTE)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String rel = JTE.relativize(p).toString().replace('\\', '/');
        String[] lines = Files.readString(p, StandardCharsets.UTF_8).split("\n");
        for (int i = 0; i < lines.length; i++) {
          // 주석(<%-- --%>) 안의 낱말은 마크업이 아니다.
          String code = lines[i].replaceAll("<%--.*?--%>", "");
          String trimmed = code.trim();
          if (trimmed.startsWith("<%--") || trimmed.startsWith("*") || trimmed.startsWith("//")) {
            continue;
          }
          if (offends.test(rel, code)) {
            offenders.add(rel + ":" + (i + 1));
          }
        }
      }
    }
    return offenders;
  }

  @Test
  void 툴팁_구현은_공용_조각에만_있다() throws IOException {
    // 공용 조각 자신, daisyUI 견본 화면(dev.jte)은 제외.
    List<String> offenders =
        scan(
            (rel, code) ->
                !rel.startsWith("_components/ui/")
                    && !rel.equals("dev.jte")
                    && RAW_TOOLTIP.matcher(code).find());
    assertThat(offenders)
        .as("아이콘 · 감싸기 툴팁은 @template._components.ui.tooltip / infoTip 으로")
        .isEmpty();
  }

  @Test
  void 주식_화면은_브라우저_title_툴팁을_쓰지_않는다() throws IOException {
    List<String> offenders =
        scan((rel, code) -> stockScope(rel) && TITLE_ATTR.matcher(code).find());
    assertThat(offenders).as("title 대신 class=\"tooltip\" + data-tip(공용 CSS 툴팁)").isEmpty();
  }

  @Test
  void data_tip_은_tooltip_클래스와_같이_쓴다() throws IOException {
    // class 에 tooltip 이 없으면 CSS 가 말풍선을 그리지 않는다 - 문구만 있고 안 뜨는 툴팁이 된다.
    // 여는 태그가 여러 줄에 걸치기도 하므로 줄이 아니라 태그(앞의 '<' 부터 data-tip 뒤 '>' 까지)로 본다.
    List<String> offenders = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(JTE)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String rel = JTE.relativize(p).toString().replace('\\', '/');
        if (!stockScope(rel)) {
          continue;
        }
        String text = Files.readString(p, StandardCharsets.UTF_8);
        for (int at = text.indexOf("data-tip="); at >= 0; at = text.indexOf("data-tip=", at + 1)) {
          // class 값에 ${조건 ? "a" : "b"} 처럼 따옴표 · '>' 가 섞이므로 다음 태그 시작('<') 전까지를 태그로 본다.
          // 식 안의 비교(signum() < 0)는 태그가 아니다 - '<' 뒤에 글자가 오는 곳만 태그 시작으로 본다.
          int open = at;
          while (open > 0
              && !(text.charAt(open) == '<' && Character.isLetter(text.charAt(open + 1)))) {
            open--;
          }
          int next = text.indexOf("<", at);
          while (next >= 0
              && next + 1 < text.length()
              && !Character.isLetter(text.charAt(next + 1))
              && text.charAt(next + 1) != '/') {
            next = text.indexOf("<", next + 1);
          }
          String tag = text.substring(open, next < 0 ? text.length() : next);
          if (!tag.contains("class=") || !tag.matches("(?s).*[\" ]tooltip[\" ].*")) {
            offenders.add(rel + ":" + (text.substring(0, at).split("\n", -1).length));
          }
        }
      }
    }
    assertThat(offenders).isEmpty();
  }

  @Test
  void 떠_있는_풍선_구현은_다시_생기지_않는다() {
    assertThat(Path.of("src/main/frontend/src/stock/stockTagTooltip.ts")).doesNotExist();
    assertThat(Path.of("src/main/jte/_components/ui/termHint.jte")).doesNotExist();
  }

  @Test
  void 설명_아이콘은_공용_조각을_거친다() throws IOException {
    String infoTip =
        Files.readString(JTE.resolve("_components/ui/infoTip.jte"), StandardCharsets.UTF_8);
    assertThat(infoTip).contains("@template._components.ui.tooltip(");
  }
}
