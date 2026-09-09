package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * href 없는 {@code <a class="tab">} 는 브라우저가 포커스를 주지 않는다 - Tab 으로 갈 수도, Enter 로 누를 수도 없다.
 *
 * <p>실측 2026-09-09: 주식 화면 4곳(활동 뷰 3, 자산 성장 패널 4, 도넛 전환 6)의 탭 13개가 그랬다. 템플릿이 {@code tabindex="0"} 과
 * {@code role="tab"} 을 주고 common.ts 가 Enter/Space 를 click 으로 바꾼다. 새 탭을 같은 모양으로 만들면 여기서 걸린다.
 */
class TabAnchorKeyboardTest {

  /**
   * {@code <a ... class="tab ...">} 여는 태그. 주의: 자바 문자열의 {@code \\b} 는 백스페이스라 정규식 단어 경계는 두 번 이스케이프한다.
   */
  private static final Pattern TAB_ANCHOR = Pattern.compile("<a\\b[^>]*\\bclass=\"tab[ \"][^>]*>");

  @Test
  void href_없는_탭_anchor_는_tabindex_와_role_을_가진다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int seen = 0;
    Path base = Path.of("src/main/jte/stock");
    try (var stream = Files.walk(base)) {
      for (Path template : stream.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String source = Files.readString(template, StandardCharsets.UTF_8);
        Matcher matcher = TAB_ANCHOR.matcher(source);
        while (matcher.find()) {
          String tag = matcher.group();
          if (tag.contains("href=")) {
            continue;
          }
          seen++;
          if (!tag.contains("tabindex=\"0\"") || !tag.contains("role=\"tab\"")) {
            offenders.add(base.relativize(template) + ": " + tag);
          }
        }
      }
    }
    // 정규식이 아무것도 못 잡으면 검사가 통째로 헛돈다(실제로 그랬다 - \b 이스케이프 실수). 13개가 있어야 한다.
    assertThat(seen).as("href 없는 탭 anchor 수").isGreaterThanOrEqualTo(13);
    assertThat(offenders).as("키보드로 닿지 않는 탭").isEmpty();
  }

  /**
   * 선택 가능한 표 행은 role="button" 이 아니다. 버튼 역할은 안의 링크를 스크린리더가 못 닿게 만든다(axe nested-interactive, 실측
   * 2026-09-09 53건). 행(row 역할)은 aria-selected 를 지원하므로 그걸 쓴다. 인라인 스크립트도 같은 속성을 읽어야 한다 - 매매 화면에서 한 번
   * 어긋나 선택이 끊겼다.
   */
  @Test
  void 표_행은_button_역할이_아니라_aria_selected_를_쓴다() throws IOException {
    List<String> offenders = new ArrayList<>();
    Pattern rowAsButton = Pattern.compile("<tr\\b[^>]*?role=\"button\"", Pattern.DOTALL);
    Path base = Path.of("src/main/jte/stock");
    try (var stream = Files.walk(base)) {
      for (Path template : stream.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String source = Files.readString(template, StandardCharsets.UTF_8);
        if (rowAsButton.matcher(source).find()) {
          offenders.add(base.relativize(template).toString());
        }
        // 행이 aria-selected 인데 같은 파일의 스크립트가 aria-pressed 로 행을 찾으면 선택이 조용히 끊긴다.
        if (source.contains("<tr")
            && source.contains("aria-selected=\"false\"")
            && source.contains("[aria-pressed=")) {
          offenders.add(base.relativize(template) + " (script still reads aria-pressed)");
        }
      }
    }
    assertThat(offenders).as("role=button 인 표 행 / aria-pressed 로 행을 찾는 스크립트").isEmpty();
  }

  /**
   * hx-get 을 단 <th>(배당 목록 정렬 머리)는 버튼이 아니라 포커스가 없다 - tabindex 와 aria-sort 를 함께 가져야 한다. 실측
   * 2026-09-09: 7개 머리가 마우스로만 정렬됐고 정렬 상태는 글자(▲▼)로만 보였다. Enter/Space 는 common.ts 가 click 으로 바꾼다.
   */
  @Test
  void hx_get_을_단_표_머리는_키보드로_닿고_aria_sort_를_가진다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int seen = 0;
    Pattern thWithHx = Pattern.compile("<th[^>]*hx-get[^>]*>", Pattern.DOTALL);
    Path base = Path.of("src/main/jte/stock");
    try (var stream = Files.walk(base)) {
      for (Path template : stream.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String source = Files.readString(template, StandardCharsets.UTF_8);
        Matcher matcher = thWithHx.matcher(source);
        while (matcher.find()) {
          seen++;
          String tag = matcher.group();
          if (!tag.contains("tabindex=\"0\"") || !tag.contains("aria-sort=")) {
            offenders.add(
                base.relativize(template)
                    + ": "
                    + tag.replaceAll("[\\s]+", " ")
                        .substring(0, Math.min(120, tag.replaceAll("[\\s]+", " ").length())));
          }
        }
      }
    }
    assertThat(seen).as("hx-get 을 단 th 수").isGreaterThanOrEqualTo(7);
    assertThat(offenders).as("키보드로 닿지 않거나 정렬 상태가 없는 표 머리").isEmpty();
  }

  /** 스크립트 쪽 짝 - 템플릿만 고치면 포커스는 가지만 Enter 는 여전히 안 된다. */
  @Test
  void 스크립트가_Enter_와_Space_를_클릭으로_바꾼다() throws IOException {
    String source =
        Files.readString(Path.of("src/main/frontend/src/common.ts"), StandardCharsets.UTF_8);

    assertThat(source)
        .contains("a[role=\"tab\"]:not([href])")
        .contains("event.key === \"Enter\" || event.key === \" \"")
        .contains("target.click();");
  }
}
