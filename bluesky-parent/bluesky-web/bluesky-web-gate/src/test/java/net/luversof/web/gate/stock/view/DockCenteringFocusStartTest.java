package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 모바일 하단 독의 활성 항목 센터링은 {@code scrollLeft} 계산으로 하고 {@code scrollIntoView} 를 쓰지 않는다.
 *
 * <p>실측 2026-09-09: Chromium 은 {@code scrollIntoView()} 된 요소로 순차 포커스 시작점을 옮긴다(1600px·375px 모두 h2 를
 * scrollIntoView 한 뒤 Tab 이 그 다음 요소로 감). 로드 시 독 항목을 scrollIntoView 로 센터링하던 탓에 375px 에서는 첫 Tab 이 스킵
 * 링크가 아니라 독의 다음 항목(시뮬레이터)에 떨어졐다(1600px 은 독이 숨어 정상). 사용자 조작 뒤의 scrollIntoView(매매 목록 페이징 등)는 그 자리로
 * 시작점을 옮기는 것이 바람직하므로 대상이 아니다 - 이 검사는 로드 시 실행되는 레이아웃 스크립트만 본다.
 */
class DockCenteringFocusStartTest {

  private static final Path LAYOUT = Path.of("src/main/jte/_layout/stockLayout.jte");

  @Test
  void 독_센터링은_scrollLeft_로_하고_scrollIntoView_를_쓰지_않는다() throws IOException {
    String src = Files.readString(LAYOUT, StandardCharsets.UTF_8);
    int dock = src.indexOf("<nav class=\"dock");
    assertThat(dock).as("모바일 독이 없다").isGreaterThan(0);
    String afterDock = src.substring(dock);
    assertThat(afterDock)
        .contains(
            "nav.scrollLeft = active.offsetLeft + active.offsetWidth / 2 - nav.clientWidth / 2");
    assertThat(afterDock)
        .as("로드 시 scrollIntoView 는 첫 Tab 의 시작점을 독으로 옮긴다")
        .doesNotContain(".scrollIntoView(");
  }
}
