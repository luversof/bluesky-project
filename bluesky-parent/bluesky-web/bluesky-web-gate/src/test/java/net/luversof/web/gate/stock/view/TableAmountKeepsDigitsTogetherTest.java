package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 표 안 금액은 자릿수 중간에서 끊지 않는다.
 *
 * <p>2026-09-16 에 320px · 글꼴 200% 넘침을 막으려고 {@code .amount-value { overflow-wrap: anywhere }} 를 넣었다.
 * 카드 · 요약에서는 자리가 넉넉하면 아무 일도 없지만 <b>표는 다르다</b> &mdash; anywhere 는 칸의 최소 폭을 한 글자로 만들고 표는 그 최소 폭까지 칸을
 * 누른다. 실측 2026-09-17: 9 화면 x 6 폭에서 금액 7,534 곳이 "362,525,/079" 처럼 끊겼다(넓은 1440px 에서도 지속가능성 243 · 자산
 * 현황 30). 표는 가로 스크롤 상자 안에 있어 끊지 않아도 문서는 넘치지 않는다(같은 실측 문서 넘침 0). 사용자 결정 2026-09-17: 표 안 금액만 안 끊기.
 *
 * <p>카드 쪽 보호(anywhere)는 그대로 있어야 한다 &mdash; 둘 다 본다. 브라우저가 읽는 것은 산출물이라 원본과 산출물을 함께 보고, 원본은 주석을 지운 뒤
 * 판정한다(주석 속 선택자가 가드를 헛돌게 한 적이 있다).
 */
class TableAmountKeepsDigitsTogetherTest {

  private static final Path CSS_SOURCE = Path.of("src/main/frontend/main.css");

  private static final Path CSS_BUILT = Path.of("src/main/resources/static/main.css");

  private static String withoutComments(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
  }

  /** 선택자 그대로의 규칙 블록 본문(여러 개면 이어 붙인다). */
  private static String ruleBody(String css, String selector) {
    StringBuilder body = new StringBuilder();
    int at = 0;
    while ((at = css.indexOf(selector, at)) >= 0) {
      boolean startsRule =
          at == 0 || css.charAt(at - 1) == '}' || Character.isWhitespace(css.charAt(at - 1));
      int open = css.indexOf('{', at);
      String between = open < 0 ? "" : css.substring(at + selector.length(), open);
      if (startsRule && open > 0 && between.isBlank()) {
        body.append(css, open + 1, css.indexOf('}', open));
      }
      at += selector.length();
    }
    return body.toString().replaceAll("\\s+", "");
  }

  @Test
  void 원본은_표_안_금액을_끊지_않고_카드_금액은_끊을_수_있게_둔다() throws IOException {
    String css = withoutComments(CSS_SOURCE);

    assertThat(ruleBody(css, "table .amount-value")).as("표 안 금액").contains("overflow-wrap:normal");
    assertThat(ruleBody(css, ".amount-value"))
        .as("카드 금액(320px · 글꼴 200% 보호)")
        .contains("overflow-wrap:anywhere");
  }

  @Test
  void 산출물도_같다() throws IOException {
    String built = Files.readString(CSS_BUILT, StandardCharsets.UTF_8);

    assertThat(built)
        .as("메이븐은 프론트엔드를 빌드하지 않는다 - npm run build 산출물이 배포본이다")
        .contains("table .amount-value{overflow-wrap:normal}")
        .contains(".amount-value{overflow-wrap:anywhere}");
    assertThat(built.indexOf("table .amount-value{overflow-wrap:normal}"))
        .as("명시도가 더 높지만, 뒤에 와야 같은 명시도의 규칙이 끼어들어도 표 쪽이 이긴다")
        .isGreaterThan(built.indexOf(".amount-value{overflow-wrap:anywhere}"));
  }
}
