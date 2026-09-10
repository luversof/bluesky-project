package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 다크 테마로 인쇄해도 종이에서 글자가 보인다.
 *
 * <p>실측 2026-09-10(qa/print-dark.cjs): 브라우저는 기본으로 배경을 찍지 않으므로 다크 테마 그대로 인쇄하면 흰 종이에 밝은 글자만 남는다 - 본문색
 * rgb(230,234,242)의 흰 종이 대비는 1.21 이고, 4화면에서 검사한 텍스트 1,770곳 중 1,765곳이 AA(4.5) 미달이었다(대시보드 69/69, 자산
 * 현황 228/228, 배당 1,129/1,133, 종목 상세 339/340). 인쇄에서는 라이트 팔레트를 쓴다.
 *
 * <p>값이 라이트와 어긋나면 종이 색이 화면과 달라지므로, 두 블록을 파일에서 직접 뽑아 대조한다.
 */
class PrintDarkThemeTest {

  private static final Pattern PROP = Pattern.compile("(--color-[\\w-]+)\\s*:\\s*([^;]+);");

  private static String css() throws IOException {
    return Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8);
  }

  private static Map<String, String> props(String block) {
    Map<String, String> out = new LinkedHashMap<>();
    Matcher m = PROP.matcher(block);
    while (m.find()) out.put(m.group(1), m.group(2).trim());
    return out;
  }

  /**
   * 인쇄용 다크 팔레트 블록. 파일 안에 {@code @media print} 가 여러 개라(유틸 레이어 안 탭 규칙 등) 첫 번째를 잡으면 엉뚱한 블록을 읽는다 - 팔레트
   * 자체를 가리키는 표식으로 찾는다(2026-09-10 실측: 첫 번째를 잡아 compound 블록을 읽고 가드가 헛돌았다).
   */
  private static String printPaletteBlock(String css) {
    int marker = css.indexOf("다크 테마로 인쇄하면");
    assertThat(marker).as("인쇄용 팔레트 표식을 찾지 못했다").isGreaterThan(0);
    return block(css, "[data-theme=\"dark\"] {", marker);
  }

  private static String block(String css, String startsWith, int from) {
    int start = css.indexOf(startsWith, from);
    assertThat(start).as("블록을 찾지 못했다: " + startsWith).isGreaterThan(-1);
    int open = css.indexOf('{', start);
    int depth = 0;
    for (int i = open; i < css.length(); i++) {
      char c = css.charAt(i);
      if (c == '{') depth++;
      else if (c == '}' && --depth == 0) return css.substring(open + 1, i);
    }
    throw new AssertionError("블록의 끝을 찾지 못했다: " + startsWith);
  }

  @Test
  void 인쇄용_다크_재정의는_라이트_팔레트와_값이_같다() throws IOException {
    String css = css();
    Map<String, String> light = props(block(css, "@theme {", 0));
    int printStart = css.indexOf("@media print {");
    assertThat(printStart).as("인쇄 규칙이 없다").isGreaterThan(0);
    Map<String, String> printDark = props(printPaletteBlock(css));

    assertThat(printDark).as("인쇄용 재정의가 비었다").hasSizeGreaterThanOrEqualTo(20);
    assertThat(printDark).containsKey("--color-base-content");
    printDark.forEach(
        (key, value) -> {
          if (!light.containsKey(key)) return; // 라이트 @theme 밖 토큰(예: compound)은 따로 확인
          assertThat(value).as(key + " 가 라이트 값과 다르다 - 종이 색이 화면과 어긋난다").isEqualTo(light.get(key));
        });
  }

  @Test
  void 다크가_바꾸는_토큰은_인쇄에서_모두_되돌린다() throws IOException {
    String css = css();
    int printStart = css.indexOf("@media print {");
    Map<String, String> screenDark = props(block(css, "[data-theme=\"dark\"] {", 0));
    Map<String, String> printDark = props(printPaletteBlock(css));
    assertThat(printDark.keySet())
        .as("다크에서 바꾼 토큰인데 인쇄에서 되돌리지 않은 것이 있다")
        .containsAll(screenDark.keySet());
  }

  @Test
  void 손익_배당_색도_인쇄에서_라이트_값으로_되돌린다() throws IOException {
    String css = css();
    String print = css.substring(css.indexOf("@media print {"));
    assertThat(print).contains("[data-theme=\"dark\"] .text-profit { color: #bd2c38; }");
    assertThat(print).contains("[data-theme=\"dark\"] .text-loss { color: #3159c4; }");
    assertThat(print).contains("[data-theme=\"dark\"] .text-dividend { color: #147449; }");
    assertThat(css).contains(".text-profit { color: #bd2c38; font-weight: 600; }");
    assertThat(css).contains(".text-loss { color: #3159c4; font-weight: 600; }");
    assertThat(css).contains(".text-dividend { color: #147449; font-weight: 600; }");
  }
}
