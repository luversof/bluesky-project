package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 접힌 선택 요약 패널은 키보드와 보조기술에서도 빠져야 한다.
 *
 * <p>{@code .selection-reveal} 은 grid rows 0fr 로 접고 자식에 {@code overflow: hidden} 을 준다. 그것만으로는
 * <b>잘려서 안 보일 뿐</b>이라 그 안의 "선택 해제" 버튼이 Tab 순서에 남는다 &mdash; 실측 2026-09-12(배당 내역, 헤드풀 브라우저로 Tab 400
 * 회): 접힌 패널 <b>3 개</b>의 버튼에 45 &middot; 69 &middot; 99 번째로 닿았다. 페이지를 열자마자 그렇다.
 *
 * <p>낭독기도 마찬가지다. JS 는 {@code selectedCount === 0} 이면 값을 지우지 않고 나가므로, 마지막 한 줄을 해제하면 접힌 패널이 "1개 종목 선택
 * &middot; 배당금(세전) 169,950" 을 그대로 붙들고 있었다(실측: {@code display:grid} &middot; {@code
 * visibility:visible} &middot; {@code checkVisibility()} true &mdash; 화면에는 안 보이는데 접근성 트리에는 남았다).
 *
 * <p>{@code visibility: hidden} 은 Tab 순서와 접근성 트리 양쪽에서 뺀다. 네 패널(자산현황 2 &middot; 배당 효율 &middot; 월배당
 * 시뮬레이터)이 같은 클래스를 쓰므로 CSS 한 곳으로 끝난다.
 */
class SelectionRevealHiddenTest {

  @Test
  void 접힌_패널은_visibility_로_감춘다() throws IOException {
    String css = Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8);
    assertThat(flatten(css))
        .as("overflow:hidden 만으로는 Tab 순서에 남는다")
        .contains(".selection-reveal > * { min-height: 0; overflow: hidden; visibility: hidden;");
    assertThat(flatten(css))
        .as("펼치면 다시 보여야 한다")
        .contains(".selection-reveal.is-open > * { visibility: visible;");
  }

  /** 배포되는 것은 빌드 산출물이다 - 소스만 고치고 빌드를 안 하면 화면은 그대로다. */
  @Test
  void 빌드_산출물에도_들어가_있다() throws IOException {
    String flat =
        flatten(
            Files.readString(
                Path.of("src/main/resources/static/main.css"), StandardCharsets.UTF_8));
    assertThat(ruleBody(flat, ".selection-reveal>*{")).as("접힌 상태").contains("visibility:hidden");
    assertThat(ruleBody(flat, ".selection-reveal.is-open>*{"))
        .as("펼친 상태")
        .contains("visibility:visible");
  }

  /** 정규식 없이 규칙 한 덩어리를 떼어 낸다(빌드 도구가 이스케이프를 먹는 일이 반복됐다). */
  private String ruleBody(String css, String selector) {
    int at = css.indexOf(selector);
    assertThat(at).as(selector + " 규칙이 있어야 한다").isGreaterThan(-1);
    int end = css.indexOf(125, at);
    return css.substring(at, end < 0 ? css.length() : end);
  }

  private String flatten(String source) {
    StringBuilder out = new StringBuilder();
    boolean lastWasSpace = false;
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (Character.isWhitespace(c)) {
        if (!lastWasSpace) {
          out.append(' ');
        }
        lastWasSpace = true;
      } else {
        out.append(c);
        lastWasSpace = false;
      }
    }
    return out.toString();
  }
}
