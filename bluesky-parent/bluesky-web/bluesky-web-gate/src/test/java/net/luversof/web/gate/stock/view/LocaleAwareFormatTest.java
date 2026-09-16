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
 * 화면 글자의 서식은 <b>앱 로케일</b>을 따른다 &mdash; 브라우저 로케일이 아니다.
 *
 * <p>로케일 인자 없이 {@code toLocaleString()} / {@code new Intl.NumberFormat()} 을 부르면 그 자리만 <b>브라우저</b>
 * 로케일을 따른다. 실측 2026-09-15: 브라우저를 de-DE 로 두고 한국어 화면(locale=ko)을 열자 같은 화면 안에서 16 자리가
 * "₩1.143.757.310"(독일식)으로 갈렸다 &mdash; 대시보드·매매·배당·활동의 차트 툴팁과 배당 요약 카드. 값은 맞는데 자릿수 구분만 다른 모양이라 한국어
 * 브라우저로는 영원히 안 보인다.
 *
 * <p>규칙은 {@code common.js} 의 {@code appLocale()} 한 곳에 있다. 인라인 스크립트도 그것을 부를 수 있다(common.js 가 가장 먼저
 * 로드된다).
 *
 * <p>JTE 인라인 스크립트까지 걷는다 &mdash; 여덟 자리 중 여섯이 거기 있었다.
 */
class LocaleAwareFormatTest {

  /** 로케일을 안 주고 부르는 꼴. 산출물은 공백이 없으므로 {@code \s*} 로 둘 다 본다. */
  private static final Pattern LOCALE_LESS =
      Pattern.compile(
          "(?:toLocaleString|toLocaleDateString|toLocaleTimeString)\\(\\s*(?:undefined\\s*)?\\)"
              + "|new\\s+Intl\\.(?:NumberFormat|DateTimeFormat)\\(\\s*(?:undefined\\s*)?\\)");

  /** 로케일을 주고 부르는 꼴(검사가 헛돌지 않는지 세는 용도). */
  private static final Pattern LOCALE_GIVEN =
      Pattern.compile(
          "(?:toLocaleString|toLocaleDateString|toLocaleTimeString)\\(\\s*[A-Za-z_$'\"]"
              + "|new\\s+Intl\\.(?:NumberFormat|DateTimeFormat)\\(\\s*[A-Za-z_$'\"]");

  @Test
  void 서식은_앱_로케일을_따른다() throws IOException {
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
          // poe 는 이 루프의 범위 밖이고, vendor 는 남의 코드다.
          if (name.contains("vendor") || name.contains("poe")) {
            continue;
          }
          // 주석에 적힌 글자는 코드가 아니다 - 실측: 이 규칙을 설명한 주석이 스스로 걸렸다.
          String source = stripComments(Files.readString(p, StandardCharsets.UTF_8));
          Matcher bad = LOCALE_LESS.matcher(source);
          while (bad.find()) {
            offenders.add(p.getFileName() + ": " + bad.group());
          }
          Matcher good = LOCALE_GIVEN.matcher(source);
          while (good.find()) {
            guarded++;
          }
        }
      }
    }

    assertThat(offenders).as("브라우저 로케일을 따라가는 서식 자리").isEmpty();
    // 로케일을 주는 자리가 사라지면 검사가 헛돈다. 원본과 산출물 양쪽에 있다.
    assertThat(guarded).as("로케일을 명시해 부르는 자리").isGreaterThanOrEqualTo(8);
  }

  /** 주석을 지운다 - 블록 주석({@code /* *}{@code /}) · JTE 주석 · 줄 주석 순으로. */
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
      // 문자열 안의 "//" 는 거의 주소다(http://) - 바로 앞이 : 면 주석이 아니다.
      while (at > 0 && line.charAt(at - 1) == ':') {
        at = line.indexOf("//", at + 2);
      }
      out.append(at >= 0 ? line.substring(0, at) : line).append('\n');
    }
    return out.toString();
  }

  /**
   * 앱 로케일 규칙은 한 곳에 있고, 인라인 스크립트가 그것을 부른다.
   *
   * <p>사본을 또 만들면(각자 {@code document.documentElement.lang} 을 읽으면) 규칙이 다시 갈린다.
   */
  @Test
  void 앱_로케일_규칙은_공용이다() throws IOException {
    String common =
        Files.readString(Path.of("src/main/frontend/src/common.ts"), StandardCharsets.UTF_8);
    assertThat(common).as("공용 규칙 정의").contains("function appLocale()");
    assertThat(common).as("전역 노출(인라인 스크립트가 부른다)").contains("appLocale = appLocale");

    String built =
        Files.readString(Path.of("src/main/resources/static/js/common.js"), StandardCharsets.UTF_8);
    // 산출물까지 본다 - 원본만 고치고 빌드를 잊으면 브라우저는 그대로다.
    assertThat(built).as("산출물의 전역 노출").contains("appLocale");
  }

  /**
   * 차트 라이브러리의 <b>기본 로케일</b>도 앱 로케일이다.
   *
   * <p>Chart.js 의 {@code defaults.locale} 기본값은 {@code navigator.language} 라, 툴팁 콜백을 안 준 차트는 브라우저
   * 로케일로 숫자를 찍는다. 실측 2026-09-15: 브라우저 de-DE 에서 매매 도넛의 내부 툴팁 모델이 "466.231.000" 이었다(그 차트는 external
   * 툴팁이라 화면엔 안 나왔다).
   */
  @Test
  void 차트_기본_로케일도_앱_로케일이다() throws IOException {
    String source =
        Files.readString(Path.of("src/main/frontend/src/stock-charts.ts"), StandardCharsets.UTF_8);
    String built =
        Files.readString(
            Path.of("src/main/resources/static/js/stock-charts.js"), StandardCharsets.UTF_8);
    // 빌드가 공백을 지우므로 공백을 눌러 비교한다.
    String squeezedSource = source.replaceAll("\\s+", "");
    String squeezedBuilt = built.replaceAll("\\s+", "");
    assertThat(squeezedSource)
        .as("원본의 기본 로케일 설정")
        .contains("chartLib.defaults.locale=resolveLocale()");
    assertThat(squeezedSource).as("원본에서 실제로 부른다").contains("applyChartLocaleDefault();");
    assertThat(squeezedBuilt).as("산출물의 기본 로케일 설정").contains("defaults.locale=resolveLocale()");
  }
}
