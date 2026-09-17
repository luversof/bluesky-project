package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 탭이 있는 화면은 탭마다 문서 제목이 달라야 한다.
 *
 * <p>실측 2026-09-12(네 화면의 탭 10개): 시뮬레이터는 3탭 3종, 관리는 2탭 2종으로 이미 탭 이름을 붙이는데 <b>배당은 2탭이 제목 한 종류</b>("배당
 * 내역")였다. 탭 둘을 함께 열어 두면 브라우저 탭·기록·즐겨찾기에서 구분되지 않는다.
 *
 * <p>활동 화면의 세 뷰도 제목이 하나인데, 그쪽은 브라우저 안에서 갈아 끼우는 전환이라 서버 렌더로는 닿지 않는다 &mdash; 여기서는 서버가 그리는 탭만 고정한다.
 */
class TabPageTitleTest {

  /**
   * 배당 화면은 2026-09-17 캘린더 탭을 실수령 배당에 통합해 탭이 하나도 남지 않았다(사용자 결정) - 제목은 화면 이름이다. 옛 탭 제목 규칙이 남아 있으면 없는
   * 탭 이름이 제목에 붙는다.
   */
  @Test
  void 배당_화면은_탭이_없어_화면_이름을_제목으로_쓴다() throws IOException {
    String jte =
        flatten(
            Files.readString(Path.of("src/main/jte/stock/dividend.jte"), StandardCharsets.UTF_8));
    assertThat(jte)
        .contains(flatten("pageTitle = MessageUtil.getMessage(\"stock.page.dividend.title\"),"));
    assertThat(jte)
        .doesNotContain("role=\"tablist\"")
        .doesNotContain("stock.page.dividend.tab.calendar");
  }

  /** 이미 지키고 있던 두 화면이 되돌아가지 않게 함께 묶는다. */
  @Test
  void 시뮬레이터와_관리도_탭_이름을_유지한다() throws IOException {
    String simulator =
        flatten(
            Files.readString(Path.of("src/main/jte/stock/simulator.jte"), StandardCharsets.UTF_8));
    assertThat(simulator).contains(flatten("stock.page.simulator.compound.title"));
    assertThat(simulator).contains(flatten("stock.page.simulator.monthly.title"));
    String admin =
        flatten(Files.readString(Path.of("src/main/jte/stock/admin.jte"), StandardCharsets.UTF_8));
    assertThat(admin).contains(flatten("String adminPageTitle ="));
    assertThat(admin).contains(flatten("pageTitle = adminPageTitle,"));
  }

  /** spotless·들여쓰기에 묶지 않는다. */
  private static String flatten(String source) {
    StringBuilder sb = new StringBuilder();
    boolean space = false;
    for (char c : source.toCharArray()) {
      if (Character.isWhitespace(c)) {
        space = true;
        continue;
      }
      if (space && sb.length() > 0) {
        sb.append(' ');
      }
      space = false;
      sb.append(c);
    }
    return sb.toString();
  }
}
