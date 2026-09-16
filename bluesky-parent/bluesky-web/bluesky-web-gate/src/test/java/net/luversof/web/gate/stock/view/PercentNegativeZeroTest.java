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

import net.luversof.web.gate.stock.util.StockFormatUtil;

/**
 * 비율 표기는 "-0.0%" 를 내지 않는다.
 *
 * <p>실측 2026-09-10: {@code String.format("%+.1f%%", -0.04)} 는 "-0.0%", {@code "%.2f%%"} 의 -0.004 는
 * "-0.00%". 2년 평가액 시계열의 프리셋 구간 3,978개 중 16개(하루 구간 위주)가 이 범위라 자산 성장 기간 수익률에 실제로 찍힐 수 있었다. 주식 템플릿의 비율
 * String.format 40곳을 {@link StockFormatUtil#pct}/{@link StockFormatUtil#signedPct} 로 모았다.
 */
class PercentNegativeZeroTest {

  @Test
  void 반올림_결과가_영이면_부호를_지운다() {
    // 2026-09-11: 영에는 부호를 아예 붙이지 않는다(이 테스트 제목 그대로). 예전에는 "+0.0%" 였는데,
    // 부호는 방향을 말하고 0 에는 방향이 없다 - 금액은 "0", 비율만 "+0.0%" 가 되어 한 카드 안에서 어긋났다.
    assertThat(StockFormatUtil.signedPct(-0.04, 1)).isEqualTo("0.0%");
    assertThat(StockFormatUtil.pct(-0.04, 1)).isEqualTo("0.0%");
    assertThat(StockFormatUtil.pct(-0.004, 2)).isEqualTo("0.00%");
    assertThat(StockFormatUtil.signedPct(-0.0, 2)).isEqualTo("0.00%");
    assertThat(StockFormatUtil.signedPct(0.0, 1)).isEqualTo("0.0%");
  }

  @Test
  void 영이_아닌_값은_그대로_HALF_UP_이다() {
    assertThat(StockFormatUtil.signedPct(-0.05, 1)).isEqualTo("-0.1%");
    assertThat(StockFormatUtil.signedPct(1.25, 1)).isEqualTo("+1.3%");
    assertThat(StockFormatUtil.signedPct(-8.914, 2)).isEqualTo("-8.91%");
    assertThat(StockFormatUtil.pct(12.345, 2)).isEqualTo("12.35%");
    assertThat(StockFormatUtil.signedPct(Double.NaN, 1)).isEqualTo("NaN%");
  }

  @Test
  void 주식_템플릿에_비율_String_format_이_남아_있지_않다() throws IOException {
    Pattern raw = Pattern.compile("String\\.format\\(\"%\\+?\\.[0-9]f%%\"");
    List<String> offenders = new ArrayList<>();
    int helperCalls = 0;
    try (Stream<Path> walk = Files.walk(Path.of("src/main/jte/stock"))) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String html = Files.readString(p, StandardCharsets.UTF_8);
        Matcher m = raw.matcher(html);
        while (m.find()) offenders.add(p.getFileName() + ": " + m.group());
        Matcher h = Pattern.compile("StockFormatUtil\\.(?:signedPct|pct)\\(").matcher(html);
        while (h.find()) helperCalls++;
      }
    }
    assertThat(helperCalls).as("헬퍼 호출을 하나도 못 찾았다").isGreaterThanOrEqualTo(40);
    assertThat(offenders).as("음의 영을 낼 수 있는 비율 포맷").isEmpty();
  }

  /**
   * <b>브라우저 쪽</b>도 음의 영을 내지 않는다.
   *
   * <p>위 검사는 {@code src/main/jte/stock} 만 걷는다. 그런데 자바스크립트의 {@code (-0.001).toFixed(1)} 도 "-0.0" 이다
   * &mdash; 같은 함정이 브라우저에도 있다.
   *
   * <p>실측 2026-09-15: 복리 시뮬에 이율 -0.001% 를 넣으면 비율 칸이 <b>"-0.0%"</b>, 연도별 표의 분배 칸이 <b>"100.0 /
   * -0.0%"</b> 였다. 같은 파일의 {@code formatSignedPercent} 는 이미 이 함정을 막아 두고 주석까지 적어 두었는데, 비율 칸만 {@code
   * toFixed} 를 직접 부르고 있었다.
   *
   * <p>규칙: 먼저 반올림한 수를 다시 찍는다({@code Number(v.toFixed(n)).toFixed(n)}) &mdash; -0 은 "0.0" 이 된다.
   * <b>산출물까지 본다</b> &mdash; 원본만 고치고 빌드를 잊으면 화면은 그대로다.
   */
  @Test
  void 브라우저_쪽도_음의_영을_내지_않는다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int guarded = 0;
    // 값에 바로 toFixed 를 걸어 퍼센트를 만드는 꼴. 반올림한 수를 다시 찍는 꼴은 안전하다.
    Pattern rawPercent = Pattern.compile("([A-Za-z_$][A-Za-z0-9_$.]*)\\.toFixed\\([0-9]\\)\\}?%");
    Pattern safe = Pattern.compile("Number\\([^)]*toFixed\\([0-9]\\)\\)");
    // 걷는 자리가 곧 가드의 범위다. src/stock 만 걸으면 형제 파일(stock-charts)이 밖에 남는다.
    List<Path> roots =
        List.of(Path.of("src/main/frontend/src"), Path.of("src/main/resources/static/js"));
    for (Path root : roots) {
      if (!Files.exists(root)) {
        continue;
      }
      try (Stream<Path> walk = Files.walk(root)) {
        for (Path p : walk.filter(Files::isRegularFile).toList()) {
          String name = p.toString();
          if (!name.endsWith(".ts") && !name.endsWith(".js")) {
            continue;
          }
          String src = Files.readString(p, StandardCharsets.UTF_8);
          Matcher m = rawPercent.matcher(src);
          while (m.find()) {
            // 안전한 두 꼴을 인정한다:
            //  (1) 그 자리에서 Number(...toFixed()) 로 감쌌다
            //  (2) 찍는 변수가 **다른 줄에서** Number(...toFixed()) 로 만들어졌다
            // (2)를 안 보면 멀쩡한 자리가 걸린다 - 실측 2026-09-15: formatSignedPercent 가 그 꼴이다.
            String before = src.substring(Math.max(0, m.start() - 12), m.start());
            String varName = m.group(1);
            boolean roundedFirst =
                src.contains("const " + varName + " = Number(")
                    || src.contains("let " + varName + " = Number(")
                    || src.contains(varName + "=Number(");
            if (before.contains("Number(") || roundedFirst) {
              guarded++;
            } else {
              offenders.add(p.getFileName() + ": " + m.group());
            }
          }
          Matcher h = safe.matcher(src);
          while (h.find()) {
            guarded++;
          }
        }
      }
    }
    assertThat(offenders).as("브라우저 쪽에서 음의 영이 나갈 수 있는 자리").isEmpty();
    // 이 꼴을 쓰는 자리가 사라지면 검사가 헛돈다.
    assertThat(guarded).as("먼저 반올림해 찍는 자리").isGreaterThanOrEqualTo(4);
  }

  /**
   * Intl 포맷터로 찍는 퍼센트도 반올림한 뒤에 판정한다.
   *
   * <p>위 검사는 {@code toFixed} 로 찍는 꼴만 본다. 실측 2026-09-15: 지속가능성 시뮬레이터는 {@code Intl.NumberFormat} 으로
   * 퍼센트를 찍는데, Intl 은 반올림 결과가 0 이어도 원래 부호를 남긴다({@code format(-0.004)} 도 {@code format(-0)} 도
   * "-0.00"). 그 탓에 시나리오 배지 세 자리가 ko·en 양쪽에서 "+0.00%" / "-0.00%" 로 나갔다. 가드가 못 보던 꼴이라 여덟 회차 동안 살아남았다
   * &mdash; <b>가드의 범위 자체가 검사 대상</b>이다.
   */
  @Test
  void 포맷터로_찍는_퍼센트도_반올림_뒤에_판정한다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int guarded = 0;
    List<Path> roots =
        List.of(Path.of("src/main/frontend/src"), Path.of("src/main/resources/static/js"));
    for (Path root : roots) {
      if (!Files.exists(root)) {
        continue;
      }
      try (Stream<Path> walk = Files.walk(root)) {
        for (Path p : walk.filter(Files::isRegularFile).toList()) {
          String name = p.toString().replace('\\', '/');
          if (!name.endsWith(".ts") && !name.endsWith(".js")) {
            continue;
          }
          if (name.contains("vendor") || name.contains("poe")) {
            continue;
          }
          String src = Files.readString(p, StandardCharsets.UTF_8);
          for (String argument : percentFormatArguments(src)) {
            // 먼저 반올림한 수를 넘기는 꼴만 안전하다.
            if (argument.contains("roundPercent(") || argument.contains("toFixed(")) {
              guarded++;
            } else {
              offenders.add(p.getFileName() + ": format(" + argument + ")}%");
            }
          }
        }
      }
    }
    assertThat(offenders).as("포맷터가 반올림 전 값으로 퍼센트를 찍는 자리").isEmpty();
    // 원본과 산출물 둘 다 있어야 한다 - 하나만 고치고 빌드를 잊으면 화면은 그대로다.
    assertThat(guarded).as("포맷터로 퍼센트를 찍는 자리(원본+산출물)").isGreaterThanOrEqualTo(2);
  }

  /** {@code .format( ... )} 뒤가 곧바로 퍼센트 기호인 자리의 <b>인자</b>를 모은다(괄호 짝을 센다). */
  private static List<String> percentFormatArguments(String src) {
    List<String> out = new ArrayList<>();
    String key = ".format(";
    int at = src.indexOf(key);
    while (at >= 0) {
      int cursor = at + key.length();
      int depth = 1;
      while (cursor < src.length() && depth > 0) {
        char c = src.charAt(cursor);
        if (c == '(') {
          depth++;
        } else if (c == ')') {
          depth--;
        }
        cursor++;
      }
      String after = src.substring(cursor, Math.min(src.length(), cursor + 2));
      // 빌드가 ${} 안 공백을 지우므로 두 꼴을 다 본다.
      if (after.startsWith("}%") || after.startsWith("%")) {
        out.add(src.substring(at + key.length(), Math.max(at + key.length(), cursor - 1)));
      }
      at = src.indexOf(key, at + 1);
    }
    return out;
  }
}
