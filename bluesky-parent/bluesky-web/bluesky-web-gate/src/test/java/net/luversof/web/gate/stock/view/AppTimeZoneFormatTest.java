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
 * 브라우저가 찍는 날짜·시각도 <b>앱이 정한 시간대</b>를 따른다.
 *
 * <p>서버는 {@code StockZoneUtil} 로 기준을 하나 정한다 &mdash; 주소의 {@code timeZone} 이 있으면 그것, 없으면 서버 존. 모르는 값은
 * 조용히 바꾸지 않고 끊는다("존은 일자 경계를 옮기므로 조용한 폴백은 틀린 값을 그럴듯하게 만든다").
 *
 * <p>그런데 브라우저에서 {@code toLocaleString()} 계열을 시간대 없이 부르면 그 자리만 <b>브라우저 시간대</b>를 따른다. 실측 2026-09-16:
 * 관리 화면의 '마지막 실행 시각' 이 같은 타임스탬프를 UTC+14 에서 "2026. 9. 16. 오전 10:30", UTC-12 에서 "2026. 9. 15. 오전
 * 8:30" 으로 찍었다(앱 기준 Asia/Seoul 이면 9. 16. 오전 5:30). 값이 아니라 <b>날짜</b>가 달라지는 어긋남이다.
 *
 * <p>규칙은 {@code common.js} 의 {@code appTimeZone()} 한 곳에 있다. JTE 인라인 스크립트까지 걷는다.
 */
class AppTimeZoneFormatTest {

  @Test
  void 날짜와_시각은_기준_시간대로_찍는다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int guarded = 0;
    List<Path> roots =
        List.of(
            Path.of("src/main/jte"),
            Path.of("src/main/frontend/src"),
            Path.of("src/main/resources/static/js"));
    for (Path root : roots) {
      if (!Files.exists(root)) {
        continue;
      }
      try (Stream<Path> walk = Files.walk(root)) {
        for (Path p : walk.filter(Files::isRegularFile).toList()) {
          String name = p.toString().replace('\\', '/');
          if (!name.endsWith(".ts") && !name.endsWith(".js") && !name.endsWith(".jte")) {
            continue;
          }
          if (name.contains("vendor") || name.contains("poe")) {
            continue;
          }
          // 주석에 적힌 글자는 코드가 아니다.
          String source = stripComments(Files.readString(p, StandardCharsets.UTF_8));
          String squeezed = source.replaceAll("\\s+", "");
          for (String argument : dateFormatArguments(source)) {
            if (argument.contains("timeZone") || namesTimeZoneOptions(squeezed, argument)) {
              guarded++;
            } else {
              offenders.add(p.getFileName() + ": toLocale...(" + argument + ")");
            }
          }
        }
      }
    }

    assertThat(offenders).as("브라우저 시간대를 따라가는 날짜·시각 자리").isEmpty();
    // 원본과 산출물 양쪽에 있어야 한다 - 하나만 고치고 빌드를 잊으면 브라우저는 그대로다.
    assertThat(guarded).as("기준 시간대로 찍는 자리(원본+산출물)").isGreaterThanOrEqualTo(4);
  }

  /** 앱 시간대 규칙은 한 곳에 있고, 레이아웃이 서버가 정한 기준을 알려 준다. */
  @Test
  void 앱_시간대_규칙은_공용이다() throws IOException {
    String common =
        Files.readString(Path.of("src/main/frontend/src/common.ts"), StandardCharsets.UTF_8);
    assertThat(common).as("공용 규칙 정의").contains("function appTimeZone()");
    assertThat(common).as("전역 노출(인라인 스크립트가 부른다)").contains("appTimeZone = appTimeZone");
    // 주소의 timeZone 이 이긴다 - 서버도 그렇다.
    assertThat(common.replaceAll("\\s+", "")).as("주소를 먼저 본다").contains("get(\"timeZone\")");

    String layout =
        Files.readString(Path.of("src/main/jte/_layout/defaultLayout.jte"), StandardCharsets.UTF_8);
    assertThat(layout).as("서버가 정한 기준 존을 화면에 실어 준다").contains("data-time-zone=");

    String built =
        Files.readString(Path.of("src/main/resources/static/js/common.js"), StandardCharsets.UTF_8);
    assertThat(built).as("산출물의 전역 노출").contains("appTimeZone");
  }

  /**
   * 날짜·시각을 찍는 호출의 <b>인자</b>를 모은다.
   *
   * <p>{@code toLocaleString} 은 숫자에도 쓰이므로 {@code new Date(...)} 로 만든 값에 건 것만 본다. {@code
   * toLocaleDateString}/{@code toLocaleTimeString} 은 언제나 날짜다.
   */
  private static List<String> dateFormatArguments(String source) {
    List<String> out = new ArrayList<>();
    for (String key : List.of(".toLocaleDateString(", ".toLocaleTimeString(", ".toLocaleString(")) {
      int at = source.indexOf(key);
      while (at >= 0) {
        boolean isDate = !key.equals(".toLocaleString(");
        if (!isDate) {
          // 바로 앞이 Date 인가 - 체인 앞쪽 60 자에 new Date( 가 있으면 날짜로 본다.
          String before = source.substring(Math.max(0, at - 60), at);
          isDate = before.contains("new Date(");
        }
        if (isDate) {
          int cursor = at + key.length();
          int depth = 1;
          while (cursor < source.length() && depth > 0) {
            char c = source.charAt(cursor);
            if (c == '(') {
              depth++;
            } else if (c == ')') {
              depth--;
            }
            cursor++;
          }
          out.add(source.substring(at + key.length(), Math.max(at + key.length(), cursor - 1)));
        }
        at = source.indexOf(key, at + 1);
      }
    }
    return out;
  }

  /**
   * 인자가 식별자면, 그 식별자가 {@code timeZone} 을 담은 객체로 만들어졌는지 본다.
   *
   * <p>옵션을 변수로 빼 쓰는 꼴({@code const dateOptions = { timeZone: ... }})을 위반으로 세면 멀쩡한 자리가 걸린다.
   */
  private static boolean namesTimeZoneOptions(String squeezedSource, String argument) {
    String last = argument.substring(argument.lastIndexOf(',') + 1).trim();
    if (last.isEmpty() || !last.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
      return false;
    }
    return squeezedSource.contains("const" + last + "={timeZone")
        || squeezedSource.contains("let" + last + "={timeZone")
        || squeezedSource.contains("var" + last + "={timeZone")
        || squeezedSource.contains(last + "={timeZone");
  }

  /** 주석을 지운다 - 블록 주석 · JTE 주석 · 줄 주석 순으로. */
  private static String stripComments(String source) {
    String withoutBlocks =
        source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?s)<%--.*?--%>", " ");
    StringBuilder out = new StringBuilder();
    for (String line : withoutBlocks.split("\\R", -1)) {
      String trimmed = line.trim();
      if (trimmed.startsWith("//") || trimmed.startsWith("*")) {
        out.append('\n');
        continue;
      }
      int at = line.indexOf("//");
      while (at > 0 && line.charAt(at - 1) == ':') {
        at = line.indexOf("//", at + 2);
      }
      out.append(at >= 0 ? line.substring(0, at) : line).append('\n');
    }
    return out.toString();
  }
}
