package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 포커스가 고정 막대 뒤로 숨지 않는다(WCAG 2.4.11 Focus Not Obscured).
 *
 * <p>실측 2026-09-10(qa/focus-obscured.cjs, 375px 10화면): Tab 으로 옮긴 포커스 95개가 하단 독(.dock, 고정 4rem) 뒤에
 * 들어갔다(배당 26·활동 21·매매 19). 브라우저의 포커스 스크롤은 뷰포트 가장자리까지만 보이게 하므로 고정 상단바(4rem)·독(lg 미만) 만큼 {@code
 * scroll-padding} 을 둔다. 1280px 에선 독이 없어 0 이었다.
 */
class FocusNotObscuredTest {

  @Test
  void html_에_고정_막대_높이만큼_scroll_padding_이_있다() throws IOException {
    String css = Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8);
    // 상단에 붙는 것이 헤더(실측 71px) + 구역 막대(실측 41px) 둘로 늘었다 - 합이 112px = 7rem 이다.
    // 실측 2026-09-10(qa/sectionnav-verify.cjs): 이 값으로 막대에서 마지막 구역을 눌렀을 때 대상 상단이
    // 정확히 112px 에 놓여 헤더에도 막대에도 가리지 않았다.
    assertThat(css).contains("html { scroll-padding-top: 7rem; }");
    assertThat(css)
        .as("구역 막대가 헤더 아래에 붙지 않으면 헤더를 파고든다(3.5rem 일 때 15px 겹쳤다)")
        .contains("position: sticky; top: 4.4375rem;");
    int narrow = css.indexOf("@media (width < 64rem)");
    assertThat(narrow).as("lg 미만(독이 보이는 폭) 규칙이 없다").isGreaterThan(0);
    String block =
        css.substring(narrow, css.indexOf("}", css.indexOf("scroll-padding-bottom", narrow)));
    assertThat(block).contains("scroll-padding-bottom: calc(4.5rem + env(safe-area-inset-bottom))");
  }

  @Test
  void 독은_lg_미만에서만_보이고_고정_높이는_4rem_이다() throws IOException {
    String layout =
        Files.readString(Path.of("src/main/jte/_layout/stockLayout.jte"), StandardCharsets.UTF_8);
    assertThat(layout).contains("<nav class=\"dock dock-sm lg:hidden");
    String css = Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8);
    assertThat(css).contains(".dock { position: fixed; left: 0; right: 0; bottom: 0;");
    assertThat(css).contains("min-height: 4rem; background-color: var(--color-base-100);");
  }
}
