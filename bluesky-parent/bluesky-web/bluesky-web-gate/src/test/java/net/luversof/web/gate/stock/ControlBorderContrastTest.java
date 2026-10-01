package net.luversof.web.gate.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 입력칸·체크박스는 배경이 주변과 완전히 같아(실측 대비 1.00) 경계선이 유일한 식별 단서다.
 *
 * <p>daisyUI 기본값 {@code --color-base-300} 을 쓰면 그 경계가 라이트 1.22 · 다크 1.34 로 WCAG 1.4.11(비텍스트 3:1)에 한참
 * 못 미친다(실측 2026-09-10: 화면당 10~16개). {@code --color-control-line} 으로 라이트 3.25 · 다크 3.34 를 만든다.
 *
 * <p>덮는 방법도 중요하다 &mdash; daisyUI 규칙이 {@code border: 2px solid var(--color-base-300)} 이라 {@code
 * :where()} 로 덮으면 특이도 0 이라 진다(실측: 값이 그대로 1.22 였다). 컨트롤 안에서 그 <b>변수 자체</b>를 바꿔 daisyUI 자신의 규칙이 새 값을
 * 집게 한다.
 */
class ControlBorderContrastTest {

  private static final Path MAIN_CSS = Path.of("src/main/frontend/main.css");

  private static final Path BUILT_CSS = Path.of("src/main/resources/static/main.css");

  @Test
  void controlLineIsDefinedForEveryPalette() throws IOException {
    String css = Files.readString(MAIN_CSS, StandardCharsets.UTF_8);

    long definitions = css.lines().filter(l -> l.contains("--color-control-line:")).count();

    assertThat(definitions)
        .as(
            "라이트·다크·인쇄·PoE 인게임(html.poe-theme, 10-01) 네 팔레트 모두에 있어야 한다 - 하나라도 빠지면 그 모드에서 경계가"
                + " 사라진다")
        .isEqualTo(4);
  }

  /**
   * 정의 개수만 세면 값이 틀려도 통과한다 - 경계색과 배경색을 함께 정의한 팔레트마다 실제 대비(WCAG, 3:1 이상)를 잰다. 배경은 컨트롤이 놓이는 두 면
   * (base-100 · base-200).
   */
  @Test
  void controlLineMeetsThreeToOneInEveryPaletteWithBackgrounds() throws IOException {
    String css = Files.readString(MAIN_CSS, StandardCharsets.UTF_8);
    java.util.regex.Matcher block =
        java.util.regex.Pattern.compile("([^{}]+)\\{([^{}]*)\\}").matcher(css);
    java.util.List<String> measured = new java.util.ArrayList<>();
    while (block.find()) {
      String body = block.group(2);
      String line = hexOf(body, "--color-control-line");
      String b100 = hexOf(body, "--color-base-100");
      String b200 = hexOf(body, "--color-base-200");
      if (line == null || b100 == null || b200 == null) {
        continue;
      }
      String selector = block.group(1).trim();
      selector = selector.substring(selector.lastIndexOf('\n') + 1).trim();
      measured.add(selector);
      assertThat(contrast(line, b100))
          .as(selector + " 경계 " + line + " vs base-100 " + b100)
          .isGreaterThanOrEqualTo(3.0);
      assertThat(contrast(line, b200))
          .as(selector + " 경계 " + line + " vs base-200 " + b200)
          .isGreaterThanOrEqualTo(3.0);
    }
    // 인쇄 팔레트도 다크 블록 안에서 배경을 정의해 함께 잰다(실측 4개: @theme · 다크 · PoE · 인쇄 다크)
    assertThat(measured)
        .as("라이트(@theme)·다크·PoE 인게임 팔레트는 반드시 쟀어야 한다 - 빠지면 이 시험이 그 팔레트를 못 본다")
        .contains("@theme", "[data-theme=\"dark\"]", "html.poe-theme");
  }

  private static String hexOf(String body, String name) {
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile(
                java.util.regex.Pattern.quote(name) + ":\\s*(#[0-9a-fA-F]{6})")
            .matcher(body);
    return m.find() ? m.group(1) : null;
  }

  private static double contrast(String a, String b) {
    double la = luminance(a);
    double lb = luminance(b);
    return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
  }

  private static double luminance(String hex) {
    double[] c = new double[3];
    for (int i = 0; i < 3; i++) {
      double v = Integer.parseInt(hex.substring(1 + i * 2, 3 + i * 2), 16) / 255.0;
      c[i] = v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
  }

  @Test
  void controlsOverrideTheVariableNotTheProperty() throws IOException {
    String css = Files.readString(MAIN_CSS, StandardCharsets.UTF_8);
    int at = css.indexOf(".input, .textarea, .select, .checkbox");

    assertThat(at).as("컨트롤 경계 규칙을 찾지 못했다").isGreaterThan(0);

    String rule = css.substring(at, css.indexOf("}", at));

    assertThat(rule)
        .as("border-color 를 직접 주면 daisyUI 규칙과 특이도 싸움이 된다 - 변수를 바꿔야 한다")
        .contains("--color-base-300: var(--color-control-line)");
  }

  @Test
  void builtCssCarriesTheToken() throws IOException {
    String built = Files.readString(BUILT_CSS, StandardCharsets.UTF_8);

    assertThat(built)
        .as("빌드 산출물이 배포본이다 - npm run build 를 빠뜨리면 원본만 고쳐진다")
        .contains("--color-control-line");
  }
}
