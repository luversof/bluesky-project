package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 금액에 쓰는 색 관례를 <b>모든 화면</b>에서 지킨다.
 *
 * <p>2026-09-11 에 대시보드 '최근 활동' 카드에서 정한 규칙이다 &mdash; 거래 금액(매수 · 매도)에는 손익 색을 쓰지 않고, 배당만 색을 준다. 그 결정이
 * 그 파일 하나에만 적용돼 있어서 활동 목록 · 매매 이력 · 매매 요약 카드 세 곳이 그대로 매수를 빨강, 매도를 파랑으로 칠하고 있었다(사용자 결정 2026-09-22 로
 * 해소).
 *
 * <p><b>왜 안 되나</b> &mdash; 실측 2026-09-22, 다크 테마: 매수 금액에 쓰인 {@code text-error} 는 rgb(248,113,113),
 * 이익 색 {@code text-profit} 은 rgb(255,107,107) 로 채널 차가 <b>7</b> 이다. 눈으로는 같은 빨강이고, 같은 화면이 실현 손익에 그 이익
 * 색을 쓰므로 매수 금액이 이득으로 읽힌다. 라이트에서도 매도 {@code text-info} rgb(32,98,171) 와 손실 {@code text-loss}
 * rgb(49,89,196) 의 차가 25 로 같은 파랑 계열이다.
 *
 * <p><b>쓸 수 있는 것</b> &mdash; 값의 부호에 따라 고르는 색은 {@code ${...}} 안에 있으므로 이 가드가 건드리지 않는다. 막는 것은 클래스에
 * <b>글자 그대로</b> 박힌 {@code text-error} · {@code text-info} 뿐이다. 유형 배지({@code badge-error} · {@code
 * badge-info})도 그대로 둔다 &mdash; 배지는 금액이 아니라 갈래를 가리킨다.
 */
class AmountColorConventionTest {

  private static final Path TEMPLATE_ROOT = Path.of("src/main/jte");

  /** 금액을 표시하는 칸에 붙는 표식. 자동 검사가 짚을 자리를 이 클래스로 준다. */
  private static final String AMOUNT_MARKER = "amount-value";

  /** 손익으로 읽히는 DaisyUI 색. 금액 클래스에 글자 그대로 있으면 안 된다. */
  private static final List<String> FORBIDDEN = List.of("text-error", "text-info");

  /** 한 자리: 파일 · 클래스 값 · 걸린 색. */
  private record Hit(Path file, String classValue, String token) {}

  @Test
  void 금액에는_손익으로_읽히는_색을_글자_그대로_박지_않는다() throws IOException {
    List<Path> templates = templates();
    assertThat(templates).as("템플릿을 하나도 못 찾았다 - 가드가 헛돈다").hasSizeGreaterThan(20);

    int scanned = 0;
    List<Hit> hits = new ArrayList<>();
    for (Path template : templates) {
      String text = Files.readString(template, StandardCharsets.UTF_8);
      for (String classValue : classValues(text)) {
        if (!hasToken(classValue, AMOUNT_MARKER)) {
          continue;
        }
        scanned++;
        String literal = withoutExpressions(classValue);
        for (String token : FORBIDDEN) {
          if (hasColor(literal, token)) {
            hits.add(new Hit(template, classValue, token));
          }
        }
      }
    }

    assertThat(scanned).as("금액 칸을 하나도 못 읽었다 - class 훑기가 깨졌다(가드가 헛돈다)").isGreaterThan(20);
    assertThat(hits)
        .as("거래 금액에는 손익 색을 쓰지 않는다(2026-09-11 결정). 부호로 고르는 색은 ${} 안에 두면 된다.\n" + describe(hits))
        .isEmpty();
  }

  @Test
  void 값의_부호로_고르는_색은_막지_않는다() {
    // 가드가 무엇이든 다 막아 버리면 쓸모가 없다 - 허용해야 하는 모양이 실제로 통과하는지 여기서 못 박는다.
    String allowed =
        "class=\"amount-value ${value.signum() > 0 ? \"text-profit\" : \"text-loss\"}\"";
    List<String> values = classValues(allowed);
    assertThat(values).hasSize(1);
    assertThat(hasToken(values.get(0), AMOUNT_MARKER)).isTrue();
    for (String token : FORBIDDEN) {
      assertThat(hasToken(withoutExpressions(values.get(0)), token)).isFalse();
    }
    assertThat(withoutExpressions(values.get(0)))
        .as("${} 안을 들어내야 부호로 고르는 색이 글자 그대로로 안 읽힌다")
        .doesNotContain("text-profit")
        .contains(AMOUNT_MARKER);

    // 반대로 글자 그대로 박힌 것은 걸려야 한다.
    String forbidden = "class=\"text-xl font-bold text-error amount-value\"";
    String literal = withoutExpressions(classValues(forbidden).get(0));
    assertThat(hasToken(literal, "text-error")).as("글자 그대로 박힌 색을 놓치면 가드가 헛돈다").isTrue();

    // 흐리게 칠해도 오류 색이다 - 투명도 변형까지 잡아야 한다.
    assertThat(hasColor("amount-value text-error/80", "text-error"))
        .as("text-error/80 을 놓치면 흐린 오류 색이 금액에 남는다")
        .isTrue();
    assertThat(hasColor("amount-value text-errorish", "text-error"))
        .as("이름이 겹칠 뿐인 다른 클래스까지 막으면 안 된다")
        .isFalse();

    // 배지는 금액이 아니다 - 걸리지 않아야 한다.
    String badge = "class=\"badge badge-error badge-outline\"";
    assertThat(hasToken(classValues(badge).get(0), AMOUNT_MARKER)).isFalse();
  }

  private static List<Path> templates() throws IOException {
    try (Stream<Path> found = Files.walk(TEMPLATE_ROOT)) {
      return found
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".jte"))
          .toList();
    }
  }

  /** {@code class="..."} 의 값들. JTE 는 {@code ${...}} 안에 큰따옴표를 또 쓰므로 표현식 안에서는 따옴표를 닫는 것으로 세지 않는다. */
  static List<String> classValues(String text) {
    List<String> values = new ArrayList<>();
    String marker = "class=\"";
    int at = text.indexOf(marker);
    while (at >= 0) {
      int index = at + marker.length();
      int depth = 0;
      StringBuilder value = new StringBuilder();
      while (index < text.length()) {
        char c = text.charAt(index);
        if (c == '$' && index + 1 < text.length() && text.charAt(index + 1) == '{') {
          depth++;
          value.append(c);
          index++;
          value.append(text.charAt(index));
        } else if (c == '}' && depth > 0) {
          depth--;
          value.append(c);
        } else if (c == '"' && depth == 0) {
          break;
        } else {
          value.append(c);
        }
        index++;
      }
      values.add(value.toString());
      at = text.indexOf(marker, index);
    }
    return values;
  }

  /** {@code ${...}} 구간을 들어낸 나머지 - 글자 그대로 박힌 클래스만 남는다. */
  static String withoutExpressions(String classValue) {
    StringBuilder out = new StringBuilder();
    int depth = 0;
    for (int index = 0; index < classValue.length(); index++) {
      char c = classValue.charAt(index);
      if (c == '$' && index + 1 < classValue.length() && classValue.charAt(index + 1) == '{') {
        depth++;
        index++;
      } else if (c == '}' && depth > 0) {
        depth--;
      } else if (depth == 0) {
        out.append(c);
      }
    }
    return out.toString();
  }

  /**
   * 그 색이 쓰였는가 &mdash; 투명도 변형({@code text-error/80})까지 센다. 흐리게 칠해도 오류 색은 오류 색이다. 낱말의 일부로만 겹치는
   * 것({@code text-errorish})은 다른 클래스이므로 세지 않는다.
   */
  static boolean hasColor(String value, String color) {
    for (String part : value.split("[\\s]+")) {
      if (part.equals(color) || part.startsWith(color + "/")) {
        return true;
      }
    }
    return false;
  }

  /** 공백으로 갈린 낱말로 있는가 - text-error 가 text-error/80 이나 다른 낱말의 일부로 걸리지 않게. */
  static boolean hasToken(String value, String token) {
    for (String part : value.split("[\\s]+")) {
      if (part.equals(token)) {
        return true;
      }
    }
    return false;
  }

  private static String describe(List<Hit> hits) {
    StringBuilder out = new StringBuilder();
    for (Hit hit : hits) {
      out.append("  ")
          .append(hit.file())
          .append(" -> ")
          .append(hit.token())
          .append(" : ")
          .append(hit.classValue())
          .append(System.lineSeparator());
    }
    return out.toString();
  }
}
