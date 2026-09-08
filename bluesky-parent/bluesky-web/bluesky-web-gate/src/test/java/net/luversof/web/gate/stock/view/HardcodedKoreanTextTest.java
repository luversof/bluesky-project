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
 * 화면 문구가 한글로 박혀 있지 않은지 본다.
 *
 * <p>게이트에는 영어 번들이 있고, 메시지 키에 대해서는 검사가 이미 두 겹이다({@code StockMessageKeyCoverageTest} · {@code
 * StockMessageKeyTest}). 그런데 <b>키를 아예 쓰지 않고 한글을 그대로 적으면</b> 그 검사들을 통째로 비켜 간다 &mdash; 영어 화면에 한글이 그대로
 * 나가도 아무 검사가 깨지지 않는다. 로케일 전환은 실제로 제공되는 기능이라 이것은 실사용 결함이다.
 *
 * <p>실측 2026-08-24: 사용자에게 나갈 수 있는 한글이 <b>111 줄</b> 있었고, 그중 105 줄이 월배당 기준 데이터 관리의 검증·실패 문구였다.
 * 2026-09-08 에 그 갈래 99 줄을 전부 {@code stock.monthly.reference.error.*} / {@code
 * stock.monthly.reference.field.*} 키로 옮겼다({@code StockViewSupport.msg}). 이제 <b>상한은 0</b> 이다.
 *
 * <p>대상이 아닌 것 &mdash; 로그 문구({@code log.warn}), 데이터 값({@code @JsonProperty("매수")}), 태그 상수({@code
 * "월배당"}), 그리고 <b>입력을 알아보는 데이터 리터럴</b>({@code header.contains("지급기준일")} 처럼 붙여넣은 표의 한글 헤더를 인식하는 비교).
 * 마지막 것은 화면에 나가는 문구가 아니라 사용자가 붙여넣는 한글을 읽는 쪽이라, 키로 옮기면 오히려 영어 로케일에서 한글 헤더를 못 알아본다.
 */
class HardcodedKoreanTextTest {

  private static final List<Path> SOURCE_ROOTS =
      List.of(Path.of("src/main/jte/stock"), Path.of("src/main/java/net/luversof/web/gate/stock"));

  private static final Pattern HANGUL = Pattern.compile("[\\uac00-\\ud7a3]");

  /**
   * 한국어에서만 쓰는 단위 접미사. 로케일 분기 안에 있어 영어 화면에는 나가지 않는다.
   *
   * <p>{@code activityList.jte} · {@code summary.jte} 의 차트 축 포맷터가 {@code
   * document.documentElement.lang} 이 {@code ko} 로 시작할 때만 붙인다. 문구가 아니라 숫자 단위라 메시지 키로 옮길 것이 아니다.
   */
  private static final List<String> LOCALE_GATED_UNITS = List.of("\uc5b5", "\ub9cc", "\uc6d0");

  /** 주석은 문구가 아니다. JTE · HTML · 블록 · 줄 주석을 모두 걷어낸다. */
  private static String withoutComments(String source) {
    String stripped = source.replaceAll("(?s)<%--.*?--%>", "");
    stripped = stripped.replaceAll("(?s)<!--.*?-->", "");
    stripped = stripped.replaceAll("(?s)/\\*.*?\\*/", "");
    StringBuilder kept = new StringBuilder();
    for (String line : stripped.split("\n", -1)) {
      kept.append(line.replaceAll("//.*$", "")).append('\n');
    }
    return kept.toString();
  }

  /** 화면 문구가 아닌 줄. 로그 · JSON 값 · 태그 상수. */
  private static boolean notUserFacing(String line) {
    String trimmed = line.trim();
    for (String unit : LOCALE_GATED_UNITS) {
      if (trimmed.contains(unit)) {
        return true;
      }
    }
    return trimmed.matches(".*\\blog\\.(warn|info|error|debug|trace)\\(.*")
        || trimmed.contains("@JsonProperty(")
        // 태그 상수는 화면 문구가 아니라 DB 에 저장된 데이터 값이다(예: 월배당 기준 관리의 "월배당",
        // 핵심 보유의 "핵심"). 데이터 값인지는 공개 여부와 무관하므로 접근 제어자를 가리지 않는다.
        || trimmed.matches("(?:public |private |protected )?static final String \\w+ = \".*")
        // 입력을 알아보는 비교 - "지급기준일".equals(...) / header.contains("분배율") / "분배금 지급현황".equals(...)
        || trimmed.matches(
            ".*\\.(equals|contains|startsWith|endsWith)\\(\"[^\"]*[\\uac00-\\ud7a3][^\"]*\".*")
        || trimmed.matches(".*\"[^\"]*[\\uac00-\\ud7a3][^\"]*\"\\.(equals|contains)\\(.*");
  }

  private List<String> hardcodedLines() throws IOException {
    List<String> found = new ArrayList<>();
    for (Path root : SOURCE_ROOTS) {
      try (Stream<Path> files = Files.walk(root)) {
        for (Path file :
            files
                .filter(p -> p.toString().endsWith(".jte") || p.toString().endsWith(".java"))
                .sorted()
                .toList()) {
          String[] lines =
              withoutComments(Files.readString(file, StandardCharsets.UTF_8)).split("\n", -1);
          for (int index = 0; index < lines.length; index++) {
            Matcher matcher = HANGUL.matcher(lines[index]);
            if (matcher.find() && !notUserFacing(lines[index])) {
              found.add(file.getFileName() + ":" + (index + 1) + " " + lines[index].trim());
            }
          }
        }
      }
    }
    return found;
  }

  @Test
  void 화면_문구가_한글로_박혀_있지_않다() throws IOException {
    List<String> found = hardcodedLines();

    assertThat(found)
        .as("화면 문구가 한글로 박혀 있다 - 영어 화면에 그대로 나간다. 메시지 키로 옮겨라(정적 문맥은 StockViewSupport.msg)")
        .isEmpty();
  }

  /** 걷어내기가 소스를 실제로 훑는지. 검사 대상 파일이 하나도 없으면 위 검사는 빈 목록으로 통과해 버린다. */
  @Test
  void 검사가_실제로_소스를_훑는다() throws IOException {
    long scanned = 0;
    for (Path root : SOURCE_ROOTS) {
      try (Stream<Path> files = Files.walk(root)) {
        scanned +=
            files
                .filter(p -> p.toString().endsWith(".jte") || p.toString().endsWith(".java"))
                .count();
      }
    }
    assertThat(scanned).as("훑은 파일이 없다 - 검사가 무력해진다").isGreaterThan(100);
  }
}
