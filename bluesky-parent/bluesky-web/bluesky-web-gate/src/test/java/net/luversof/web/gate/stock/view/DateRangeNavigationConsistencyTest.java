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
 * 기간 이동 화살표의 활성 조건이 화면마다 같은지 본다.
 *
 * <p>{@code canPrev} / {@code canNext} 는 날짜 범위 네비게이션의 &lt;/&gt; 버튼을 켜고 끈다. 이 식이 <b>네 화면에 그대로
 * 복사</b>돼 있다 &mdash; 자산성장 · 활동 · 배당 이력 · 매매 목록. 한 곳만 고치면 같은 데이터인데 화면마다 화살표가 다르게 동작한다.
 *
 * <p>계산에 함정이 있어서 더 위험하다.
 *
 * <ul>
 *   <li>{@code endDate} 는 <b>배타적</b>이다. 그래서 {@code endLocal} 은 표시 구간의 마지막 날 <b>다음 날</b>이고, {@code
 *       canNext} 는 그 값이 오늘보다 이른지를 본다.
 *   <li>{@code startLocal} 은 instant 를 화면 존으로 바꾼 값이다. UTC 문자열을 자르면 하루 어긋난다.
 * </ul>
 *
 * <p>실측 2026-08-24: 네 곳의 {@code canNext} 와 {@code canPrev} 가 공백까지 정규화하면 완전히 같다. 이 검사는 그 상태를 못박는다
 * &mdash; 하나를 바꾸려면 전부 바꾸거나, 공용 조각으로 빼야 한다.
 *
 * <p>{@code @param boolean canNext = true} 같은 <b>파라미터 기본값</b>은 계산이 아니므로 뺀다 ({@code
 * detailDateFilter.jte} 가 그렇다 &mdash; 빼지 않으면 세미콜론이 없어 뒷문장까지 식으로 삼킨다).
 */
class DateRangeNavigationConsistencyTest {

  private static final Path JTE_ROOT = Path.of("src/main/jte/stock");

  private record Declaration(String file, String expression) {}

  /** {@code boolean canNext = ...;} 선언에서 식만 뽑는다. */
  private List<Declaration> declarations(String name) throws IOException {
    List<Declaration> found = new ArrayList<>();
    String marker = "boolean " + name + " =";
    try (Stream<Path> files = Files.walk(JTE_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).sorted().toList()) {
        String relative =
            JTE_ROOT.relativize(file).toString().replace(java.io.File.separatorChar, '/');
        String source = withoutParamLines(Files.readString(file, StandardCharsets.UTF_8));
        for (String line : splitStatements(source)) {
          String trimmed = line.trim();
          int at = trimmed.indexOf(marker);
          if (at < 0) {
            continue;
          }
          found.add(
              new Declaration(
                  relative,
                  trimmed.substring(at + marker.length()).replaceAll("\\s+", " ").trim()));
        }
      }
    }
    return found;
  }

  /**
   * {@code @param} 줄을 통째로 지운다.
   *
   * <p>파라미터 기본값에는 세미콜론이 없어서, 지우지 않으면 세미콜론 단위로 자를 때 뒷문장까지 한 조각으로 붙어 계산식처럼 보인다(실측: {@code
   * detailDateFilter.jte}).
   */
  private String withoutParamLines(String source) {
    StringBuilder kept = new StringBuilder();
    for (String line : source.split(System.lineSeparator().contains("\r") ? "\r?\n" : "\n", -1)) {
      kept.append(line.stripLeading().startsWith("@param") ? "" : line).append('\n');
    }
    return kept.toString();
  }

  /** 세미콜론 단위로 자른다. 선언이 여러 줄에 걸쳐 있어도 한 조각이 된다. */
  private List<String> splitStatements(String source) {
    List<String> statements = new ArrayList<>();
    int at = 0;
    while (true) {
      int end = source.indexOf(';', at);
      if (end < 0) {
        statements.add(source.substring(at));
        return statements;
      }
      statements.add(source.substring(at, end));
      at = end + 1;
    }
  }

  private void assertAllSame(String name, int floor) throws IOException {
    List<Declaration> found = declarations(name);
    assertThat(found).as("%s 선언을 충분히 찾지 못했다 - 검사가 무력해진다", name).hasSizeGreaterThanOrEqualTo(floor);

    List<String> distinct = found.stream().map(Declaration::expression).distinct().toList();
    List<String> detail =
        found.stream().map(d -> d.file() + " -> " + d.expression()).distinct().toList();
    assertThat(distinct)
        .as(
            "%s 의 계산이 화면마다 갈라졌다. 같은 데이터인데 기간 이동 화살표가 다르게 동작한다:%n%s",
            name, String.join("\n", detail))
        .hasSize(1);
  }

  @Test
  void 다음_기간_활성_조건이_모든_화면에서_같다() throws IOException {
    assertAllSame("canNext", 4);
  }

  @Test
  void 이전_기간_활성_조건이_모든_화면에서_같다() throws IOException {
    assertAllSame("canPrev", 4);
  }
}
