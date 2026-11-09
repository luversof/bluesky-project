package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 아이콘만 남는 자리에도 이름과 키보드 길이 있다.
 *
 * <p>실측 2026-09-09(13화면 svg 265개 조사): 네비바의 로케일·프로필 드롭다운 트리거는 {@code div.btn} 이라 role 이 없고 글자가
 * {@code max-xl:hidden} 이라 좁은 화면에서는 이름이 사라졌다. 설명 툴팁은 {@code data-tip}(CSS 로만 그림)이라 보조기술에 문구가 없고 마우스
 * 없이는 열 수 없었다. 장식 svg 의 {@code aria-hidden} 은 common.ts 가 일괄로 붙인다(decorativeSvg.test.mjs).
 */
class TooltipAndMenuTriggerA11yTest {

  private static final Path JTE = Path.of("src/main/jte");

  private static String read(String relative) throws IOException {
    return Files.readString(JTE.resolve(relative), StandardCharsets.UTF_8);
  }

  @Test
  void 네비바_드롭다운_트리거는_이름과_역할이_있다() throws IOException {
    assertThat(read("_components/body/navbar/end/locale.jte"))
        .contains(
            "role=\"button\" aria-haspopup=\"menu\" aria-label=\"${MessageUtil.getMessage(\"common.label.locale\")}\"");
    assertThat(read("_components/body/navbar/end/profileArea.jte"))
        .contains("role=\"button\" aria-haspopup=\"menu\" aria-label=\"${username}\"");
  }

  /**
   * 툴팁은 공용 조각 하나(tooltip.jte)로만 그린다(2026-11-10) - 그 조각이 짧은 말풍선에 aria-label 로 같은 문구를, 키보드
   * 초점(tabindex)을, 역할(role="note" - 역할 없는 span 의 aria-label 은 axe aria-prohibited-attr)을 준다. 안쪽 링크가
   * 초점을 받는 경우만 감싼 칸이 초점을 받지 않는다(data-tooltip-inner-focus, CSS 가 안쪽 초점에서 연다).
   */
  @Test
  void 공용_툴팁은_키보드로_열리고_읽힌다() throws IOException {
    String tooltip = read("_components/ui/tooltip.jte");
    assertThat(tooltip)
        .contains("role=\"${focusable ? \"note\" : null}\"")
        .contains("tabindex=\"${focusable ? \"0\" : null}\"")
        .contains("aria-label=\"${focusable ? text : null}\"")
        .contains("data-tooltip-inner-focus=\"${focusable ? null : \"true\"}\"");
    String css = Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8);
    assertThat(css).contains("[data-tooltip-inner-focus]:has(:focus-visible)");
  }
}
