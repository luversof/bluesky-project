package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 툴팁 말풍선은 트리거 가운데에 맞춰 펼쳐지는데 뷰포트 충돌 회피가 없었다.
 *
 * <p>실측 2026-09-11(활동 화면): <b>375px 에서 툴팁 48 개 중 34 개</b>가 화면 밖으로 나갔다(계좌 "+N" 칩은 트리거 [309,335] 에
 * 말풍선 176px 이라 [234,410] - 35px 잘림). 숨어 있어도 레이아웃에 남아 문서 폭이 411px 이 되어 <b>가로 스크롤 36px</b> 이 생겼다.
 * 768px 에서도 5 개 잘림 + 32px 였다.
 *
 * <p>common.js 가 트리거 위치와 말풍선 폭을 재서 끝 맞춤 클래스를 붙이고, 그 클래스는 레이어 밖 규칙이라 컴포넌트 레이어의 가운데 맞춤을 이긴다. 폭 상한(좁은
 * 화면 min(11rem, ...))은 그대로 둔다.
 */
class TooltipEdgeAlignTest {

  private static final char q = (char) 34;

  private static final Path SOURCE_CSS = Path.of("src/main/frontend/main.css");
  private static final Path BUILT_CSS = Path.of("src/main/resources/static/main.css");
  private static final Path SOURCE_TS = Path.of("src/main/frontend/src/common.ts");
  private static final Path BUILT_JS = Path.of("src/main/resources/static/js/common.js");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 끝_맞춤_규칙이_양쪽에_있다() throws IOException {
    for (Path path : new Path[] {SOURCE_CSS, BUILT_CSS}) {
      String css = read(path);
      assertThat(css).as(path + " 에 start 끝 맞춤").contains(".tooltip-align-start");
      assertThat(css).as(path + " 에 end 끝 맞춤").contains(".tooltip-align-end");
    }
  }

  @Test
  void 끝_맞춤은_가운데_맞춤_변환을_지운다() throws IOException {
    String css = read(SOURCE_CSS);
    int at = css.indexOf(".tooltip-align-end[data-tip]::before");
    assertThat(at).isGreaterThan(0);
    String rule = css.substring(at, css.indexOf('}', at));
    // translateX(-50%) 가 남아 있으면 끝에 맞춰 놓고 다시 절반을 왼쪽으로 밀어 화면 밖으로 나간다.
    assertThat(rule).doesNotContain("translateX");
    assertThat(rule).contains("right: 0");
    assertThat(rule).contains("left: auto");
  }

  @Test
  void 레이어_밖_규칙이라_가운데_맞춤을_이긴다() throws IOException {
    String css = read(SOURCE_CSS);
    int align = css.indexOf(".tooltip-align-start[data-tip]::before");
    int layerEnd = css.lastIndexOf("@layer components");
    assertThat(align).isGreaterThan(0);
    // 컴포넌트 레이어 선언보다 뒤(=레이어 밖 구간)에 있어야 한다.
    assertThat(align).as("레이어 안에 넣으면 유틸리티/컴포넌트 우선순위에 밀린다").isGreaterThan(layerEnd);
  }

  @Test
  void 위치를_다시_재는_시점이_묶여_있다() throws IOException {
    String ts = read(SOURCE_TS);
    assertThat(ts).contains("alignTooltips");
    // 처음 그릴 때 / 조각 교체 뒤 / 화면 폭이 바뀔 때 - 셋 다 없으면 숨은 말풍선이 문서 폭을 다시 밀어낸다.
    assertThat(ts).contains("DOMContentLoaded" + q + ", () => alignTooltips()");
    assertThat(ts).contains("htmx:afterSettle" + q + ", () => alignTooltips()");
    assertThat(ts).contains("resize" + q + ", scheduleTooltipAlign");
  }

  @Test
  void 빌드_산출물에도_들어_있다() throws IOException {
    String js = read(BUILT_JS).replace(" ", "");
    assertThat(js).as("npm run build 를 안 돌리면 배포본에는 옛 파일이 간다").contains("tooltip-align-start");
    assertThat(js).contains("tooltip-align-end");
  }
}
