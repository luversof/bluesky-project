package net.luversof.web.gate.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 본문 폰트 CSS 는 렌더를 막지 않는다.
 *
 * <p>이 CSS 는 외부(cdn.jsdelivr.net)에서 온다 &mdash; 자체 호스팅은 정적 파일 3.2MB 증가라 쓰지 않기로 한 결정이 있다. 그런데 보통의
 * {@code <link rel="stylesheet">} 는 렌더 차단이라, CDN 이 <b>끊기지 않고 느리기만 해도</b> 본문 글자가 그동안 보이지 않는다.
 *
 * <p>실측 2026-09-11(로그인 상태, /stock, 본문 글자 50자 이상이 보일 때까지): 정상 540ms · CDN 즉시 실패 291ms · <b>CDN 5초 지연
 * 5,296ms</b>. 즉 느린 CDN 이 화면을 통째로 붙잡았다.
 *
 * <p>{@code media="print"} 로 받아 차단을 피하고, 다 받은 뒤 nonce 스크립트가 {@code all} 로 바꾼다. 인라인 {@code on*} 속성은
 * CSP(script-src 'self' nonce)에 걸리므로 쓰지 않는다.
 */
class FontCssNonBlockingTest {

  private static final Path LAYOUT = Path.of("src/main/jte/_layout/defaultLayout.jte");

  private String layout() throws IOException {
    return Files.readString(LAYOUT, StandardCharsets.UTF_8);
  }

  @Test
  void 폰트_css_는_print_로_받는다() throws IOException {
    String layout = layout();

    int at = layout.indexOf("pretendardvariable-dynamic-subset.min.css");
    assertThat(at).as("폰트 CSS 링크").isPositive();
    String tag = layout.substring(layout.lastIndexOf("<link", at), at);
    assertThat(tag)
        .as("렌더를 막지 않으려면 media=print 로 받아야 한다")
        .contains("media=" + (char) 34 + "print" + (char) 34);
    assertThat(tag).contains("data-font-css");
  }

  @Test
  void 받은_뒤_all_로_되돌린다() throws IOException {
    String layout = layout();

    assertThat(layout).contains("link[data-font-css]");
    assertThat(layout).as("다 받으면 화면용으로 적용해야 한다").contains("fontCss.media = 'all'");
    assertThat(layout).as("이미 적용됐으면 바로 켠다").contains("if (fontCss.sheet)");
  }

  /** CSP 는 인라인 on* 속성을 막는다 - 그 방식으로 되돌아가면 폰트가 영영 print 로 남는다. */
  @Test
  void 인라인_이벤트_속성을_쓰지_않는다() throws IOException {
    String layout = layout();

    int at = layout.indexOf("pretendardvariable-dynamic-subset.min.css");
    String tag = layout.substring(layout.lastIndexOf("<link", at), layout.indexOf(">", at));
    assertThat(tag).doesNotContain("onload=");
  }
}
