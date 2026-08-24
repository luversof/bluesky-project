package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 화면에 나갈 문구가 소스에 한글로 박혀 있는지 본다.
 *
 * <p>게이트에는 영어 번들이 있고, 메시지 키에 대해서는 검사가 이미 두 겹이다({@code StockMessageKeyCoverageTest} · {@code
 * StockMessageKeyTest}). 그런데 <b>키를 아예 쓰지 않고 한글을 그대로 적으면</b> 그 검사들을 통째로 비켜 간다 &mdash; 영어 화면에 한글이 그대로
 * 나가도 아무 검사가 깨지지 않는다.
 *
 * <p>실측 2026-08-24: 주석을 걷어낸 뒤 사용자에게 나갈 수 있는 한글이 <b>111 줄</b> 있었고, 그중 105 줄이 {@code
 * StockViewController}(월배당 기준 데이터 관리)의 검증·실패 문구다. 그 밖의 3 곳을 고쳤다.
 *
 * <ul>
 *   <li>{@code monthlyDividendSimulator.jte} 저장/가져오기 성공 알림 2 개
 *   <li>{@code TradeProfit} 의 종목 합계행 계좌 이름 {@code "전체"}
 *   <li>{@code StockViewController} 의 순서 저장 API 가 내려주는 {@code "로그인이 필요합니다."}
 * </ul>
 *
 * <p>{@code StockViewController} 는 아직 빚으로 남아 있어 지금 수만 못박는다 &mdash; <b>늘어나면 실패</b>한다. 줄이는 것은 언제든 되고,
 * 다 없애면 상한을 0 으로 내리면 된다.
 *
 * <p>로그 문구({@code log.warn}), 데이터 값({@code @JsonProperty("매수")}), 태그 상수({@code "월배당"})는 화면 문구가 아니므로
 * 대상이 아니다.
 */
class HardcodedKoreanTextTest {

  private static final List<Path> SOURCE_ROOTS =
      List.of(Path.of("src/main/jte/stock"), Path.of("src/main/java/net/luversof/web/gate/stock"));

  private static final Pattern HANGUL = Pattern.compile("[\\uac00-\\ud7a3]");

  /**
   * 아직 고치지 못한 파일과 그 줄 수(실측 2026-08-24). 늘어나면 실패한다.
   *
   * <p>전부 월배당 <b>기준 데이터 관리</b> 한 갈래다 &mdash; 관리 화면의 검증 문구와 출처 파서의 오류 문구. 한 번에 옮기면 문구 100 개를 한꺼번에
   * 건드리게 되므로, 지금은 <b>늘지 않는 것</b>만 못박고 줄여 나간다. 다 없애면 상한을 0 으로 내리면 된다.
   */
  private static final Map<String, Integer> KNOWN_DEBT =
      Map.of(
          "StockViewController.java", 50,
          "MonthlyDividendPayoutImportParser.java", 24,
          "MonthlyDividendPayoutSourceImportService.java", 11,
          "RiseMonthlyDividendPayoutSourceParser.java", 5,
          "KodexMonthlyDividendPayoutSourceParser.java", 3,
          "PlusMonthlyDividendPayoutSourceParser.java", 3,
          "TigerMonthlyDividendPayoutSourceParser.java", 2);

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
        || trimmed.matches("private static final String \\w+ = \".*");
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
  void 알려진_빚_말고는_화면_문구가_한글로_박혀_있지_않다() throws IOException {
    List<String> found = hardcodedLines();
    // 하한을 두는 이유: 걷어내기가 한 줄도 못 찾으면 이 검사가 무력해진다.
    assertThat(found).as("한 줄도 찾지 못했다 - 걷어내기가 너무 많이 지웠다").isNotEmpty();

    List<String> outsideDebt =
        found.stream()
            .filter(line -> !KNOWN_DEBT.containsKey(line.substring(0, line.indexOf(':'))))
            .toList();

    assertThat(outsideDebt).as("화면 문구가 한글로 박혀 있다 - 영어 화면에 그대로 나간다. 메시지 키로 옮겨라").isEmpty();
  }

  @Test
  void 알려진_빚이_늘어나지_않는다() throws IOException {
    Map<String, Long> counted = new java.util.HashMap<>();
    for (String line : hardcodedLines()) {
      counted.merge(line.substring(0, line.indexOf(':')), 1L, Long::sum);
    }

    List<String> grown = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : KNOWN_DEBT.entrySet()) {
      long now = counted.getOrDefault(entry.getKey(), 0L);
      if (now > entry.getValue()) {
        grown.add(entry.getKey() + " " + entry.getValue() + " -> " + now);
      }
    }

    assertThat(grown).as("하드코딩 한글이 늘었다. 새 문구는 메시지 키로 넣어라").isEmpty();
  }
}
