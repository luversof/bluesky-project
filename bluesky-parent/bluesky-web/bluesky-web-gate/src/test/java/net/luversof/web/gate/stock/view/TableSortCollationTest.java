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

import org.junit.jupiter.api.Test;

/**
 * 표 정렬의 텍스트 비교 규칙이 화면마다 다르지 않은지 본다.
 *
 * <p>이 앱에는 표 정렬 구현이 셋이다 &mdash; 공용 {@code tableSort.ts}(계좌 상세/종목 상세)와 {@code assetStatus.jte},
 * {@code tabsDividendHistory.jte} 의 인라인 정렬기. 셋 다 {@code localeCompare} 로 텍스트 열을 정렬하는데, 로케일 인자를 하나만
 * 비워 두면 그 화면만 브라우저 로케일을 따라간다.
 *
 * <p>실측(같은 종목명 목록): {@code ko} 는 한글 먼저(가나다, 삼성전자, 하이닉스, CJ씨푸드, HD현대중공업, KODEX...), {@code en} 은 라틴
 * 먼저(CJ씨푸드, HD현대중공업, KODEX..., 가나다, 삼성전자, 하이닉스)로 <b>순서가 뒤집힌다</b>. 게이트는 영어 번들 ({@code
 * gateMessage_en.properties})이 있어 이 차이가 실제로 드러난다.
 */
class TableSortCollationTest {

  private static final List<Path> SORT_SOURCES =
      List.of(
          Path.of("src/main/frontend/src/stock/tableSort.ts"),
          Path.of("src/main/jte/stock/htmx/fragments/assetStatus.jte"),
          Path.of("src/main/jte/stock/htmx/fragments/tabsDividendHistory.jte"));

  /** {@code localeCompare(x, <로케일>, ...)} 의 두 번째 인자를 뽑는다. */
  private static final Pattern LOCALE_ARG =
      Pattern.compile("localeCompare\\(\\s*[^,]+?\\s*,\\s*([^,)]+)");

  private List<String> localeArguments(Path file) throws IOException {
    String source = Files.readString(file, StandardCharsets.UTF_8);
    List<String> found = new ArrayList<>();
    Matcher matcher = LOCALE_ARG.matcher(source);
    while (matcher.find()) {
      found.add(normalizeLocale(matcher.group(1).trim(), source));
    }
    return found;
  }

  /**
   * 인자를 실제 로케일 문자열로 바꾼다.
   *
   * <p>호출부에 리터럴을 바로 쓴 곳도 있고 상수로 뺀 곳도 있어서, 표기만 비교하면 값이 같아도 다르게 보인다. 상수면 같은 파일의 선언에서 값을 찾아 푼다.
   */
  private String normalizeLocale(String argument, String source) {
    if (argument.startsWith("'") || argument.startsWith("\"")) {
      return argument.substring(1, argument.length() - 1);
    }
    Matcher declaration =
        Pattern.compile(
                "(?:var|let|const)\\s+" + Pattern.quote(argument) + "\\s*=\\s*[\"']([^\"']*)[\"']")
            .matcher(source);
    return declaration.find() ? declaration.group(1) : argument;
  }

  @Test
  void 모든_표_정렬이_같은_콜레이션_로케일을_쓴다() throws IOException {
    List<String> all = new ArrayList<>();
    for (Path file : SORT_SOURCES) {
      assertThat(file).as("정렬 구현 파일이 옮겨졌거나 사라졌다: " + file).exists();
      List<String> arguments = localeArguments(file);
      assertThat(arguments).as(file + " 에서 localeCompare 호출을 찾지 못했다").isNotEmpty();
      all.addAll(arguments);
    }

    // 상수로 뺀 경우도 있으므로 이름이 아니라 '리터럴이 아닌 값' 을 배제하는 대신
    // undefined/null 처럼 로케일을 비우는 표기만 금지한다.
    assertThat(all)
        .as("로케일을 비우면 그 표만 브라우저 로케일을 따라가 다른 표와 순서가 어긋난다")
        .doesNotContain("undefined", "null");

    // 실제로 쓰이는 로케일 표기가 하나로 모여야 한다.
    List<String> distinct = all.stream().distinct().sorted().toList();
    assertThat(distinct).as("표마다 다른 로케일로 정렬하면 같은 목록이 화면마다 다르게 보인다: " + distinct).hasSize(1);
  }

  /** 공용 구현이 로케일을 상수로 두고 그 값이 'ko' 인지. */
  @Test
  void 공용_정렬기의_로케일은_ko다() throws IOException {
    String source = Files.readString(SORT_SOURCES.get(0), StandardCharsets.UTF_8);
    assertThat(source).contains("var TEXT_COLLATION_LOCALE = \"ko\";");
  }
}
