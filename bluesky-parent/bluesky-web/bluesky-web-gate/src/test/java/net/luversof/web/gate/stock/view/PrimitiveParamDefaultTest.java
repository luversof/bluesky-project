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
 * {@code long} / {@code double} 파라미터의 기본값이 그 타입의 리터럴인지 본다.
 *
 * <p>JTE 는 운영에서도 모델을 {@code Map} 으로 받아 렌더한다. 키가 없으면 {@code @param} 의 기본값 식이 쓰이는데, {@code long}
 * 파라미터에 {@code = 0} 이라고 적으면 그 {@code 0} 은 <b>{@code Integer} 로 박싱된 뒤 {@code Long} 으로 캐스팅</b>되어
 * {@code ClassCastException} 이 난다. 조각 하나가 아니라 페이지 전체가 500 이 된다.
 *
 * <p>실측 2026-08-23: 실제로 그랬다 &mdash; {@code activityList.jte:30} 의 {@code @param long buyCount = 0}
 * 이 {@code ClassCastException: class java.lang.Integer cannot be cast to class java.lang.Long} 을
 * 냈고, 같은 형태가 주식 조각 4 개 파일 11 곳에 있었다. 모두 {@code 0L} 로 고쳤다.
 *
 * <p>렌더 검사({@code EmptyRowsRenderTest} / {@code EmptyDataRenderTest})가 대부분 잡지만 사각이 있다 &mdash; 두 검사는
 * "필수 파라미터가 있는 조각" 과 "전부 기본값인 조각" 을 각각 보므로, 어느 한쪽에도 들지 않게 되는 조합이 생길 수 있다. 규칙 자체는 소스에서 바로 볼 수 있으므로
 * 여기서 못박는다.
 *
 * <p>{@code int} 는 문제가 없다({@code Integer} 언박싱이 그대로 된다). {@code boolean} 도 마찬가지다.
 */
class PrimitiveParamDefaultTest {

  private static final Path JTE_ROOT = Path.of("src/main/jte/stock");

  /** 타입 이름 -> 그 타입의 리터럴이 반드시 가져야 하는 접미사. */
  private static final List<String[]> TYPES_NEEDING_SUFFIX =
      List.of(new String[] {"long", "L"}, new String[] {"double", "D"});

  private List<String> violations() throws IOException {
    List<String> found = new ArrayList<>();
    try (Stream<Path> files = Files.walk(JTE_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).sorted().toList()) {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int index = 0; index < lines.size(); index++) {
          String line = lines.get(index).trim();
          for (String[] typeAndSuffix : TYPES_NEEDING_SUFFIX) {
            String prefix = "@param " + typeAndSuffix[0] + " ";
            if (!line.startsWith(prefix) || !line.contains("=")) {
              continue;
            }
            String defaultValue = line.substring(line.indexOf('=') + 1).trim();
            if (!isNumericLiteral(defaultValue)) {
              // 리터럴이 아니면(식이나 상수 참조) 타입이 이미 맞춰져 있다.
              continue;
            }
            String suffix = typeAndSuffix[1];
            if (!defaultValue.toUpperCase(java.util.Locale.ROOT).endsWith(suffix)) {
              found.add(file.getFileName() + ":" + (index + 1) + " " + line);
            }
          }
        }
      }
    }
    return found;
  }

  private boolean isNumericLiteral(String value) {
    if (value.isEmpty()) {
      return false;
    }
    for (int index = 0; index < value.length(); index++) {
      char c = value.charAt(index);
      if (!Character.isDigit(c) && c != '.' && c != 'L' && c != 'l' && c != 'D' && c != 'd') {
        return false;
      }
    }
    return Character.isDigit(value.charAt(0));
  }

  @Test
  void long_double_기본값은_그_타입의_리터럴로_적는다() throws IOException {
    assertThat(violations())
        .as(
            "JTE 는 Map 으로 렌더하므로 키가 빠지면 이 기본값이 쓰인다."
                + " long 에 0 을 적으면 Integer 로 박싱돼 ClassCastException 이 나고 페이지가 500 이 된다 (0L 로 적을 것)")
        .isEmpty();
  }

  /** 검사가 실제로 훑고 있는지. 대상 줄을 하나도 못 찾으면 위 검사는 늘 통과한다. */
  @Test
  void 검사가_실제로_대상_줄을_훑는다() throws IOException {
    int found = 0;
    try (Stream<Path> files = Files.walk(JTE_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).toList()) {
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
          String trimmed = line.trim();
          if (trimmed.startsWith("@param long ") || trimmed.startsWith("@param double ")) {
            found++;
          }
        }
      }
    }
    // 실측 2026-08-24: long 19 곳 + double 1 곳.
    assertThat(found).as("long/double 파라미터를 하나도 찾지 못했다").isGreaterThanOrEqualTo(15);
  }
}
