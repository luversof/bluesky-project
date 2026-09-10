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
import java.util.stream.Stream;

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
  // class 토큰이 정확히 tooltip 인 요소만(안쪽 tooltip-content 는 문구 상자라 대상이 아니다).
  private static final Pattern TOOLTIP =
      Pattern.compile("<(?:span|div)\\b[^>]*class=\"tooltip(?:\\s[^\"]*)?\"[^>]*>");

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

  /** data-tip 툴팁은 aria-label 로 같은 문구를, 모든 툴팁은 tabindex 로 키보드 포커스를 가진다. */
  @Test
  void 주식_템플릿의_툴팁은_키보드로_열리고_읽힌다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int tooltips = 0;
    try (Stream<Path> walk = Files.walk(JTE.resolve("stock"))) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        Matcher m = TOOLTIP.matcher(Files.readString(p, StandardCharsets.UTF_8));
        while (m.find()) {
          tooltips++;
          String tag = m.group();
          boolean focusable = tag.contains("tabindex=\"0\"");
          // aria-label 은 역할 없는 span/div 에 금지된다(axe aria-prohibited-attr, 실측 2026-09-09) -
          // role="note" 와 함께.
          boolean readable =
              !tag.contains("data-tip=")
                  || (tag.contains("aria-label=\"${") && tag.contains("role=\"note\""));
          if (!focusable || !readable) {
            offenders.add(JTE.relativize(p) + ": " + tag.substring(0, Math.min(tag.length(), 120)));
          }
        }
      }
    }
    assertThat(tooltips).as("툴팁을 하나도 못 찾았다").isGreaterThanOrEqualTo(5);
    assertThat(offenders).as("포커스 불가이거나 문구가 CSS 에만 있는 툴팁").isEmpty();
  }
}
