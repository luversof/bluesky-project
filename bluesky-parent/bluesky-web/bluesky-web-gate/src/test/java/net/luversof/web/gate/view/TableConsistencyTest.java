package net.luversof.web.gate.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 주식 화면의 표는 한 가지 모양이다.
 *
 * <p>사용자 지적 2026-11-10: "테이블도 다 ui 가 제각각" - 표 39 개가 클래스 조합 14 가지(table-xs/sm · table-zebra ·
 * whitespace-nowrap · font-mono)로 갈라져 있었다. 견본(/dev/ui-catalog)에서 사용자가 기준을 골랐다: D2 촘촘 · Z2 줄 사이 선만 ·
 * B3 카드 안 · H2 정렬 단추 + ⓘ · N1 본문 글꼴 + 자릿수 맞춤 · W2 칸 안 줄바꿈. 모양은 main.css 의 {@code .data-table} 한곳에서
 * 정하고, 화면은 {@code class="table data-table"} 만 쓴다. 표마다 모양 클래스를 다시 고르면 또 갈라지므로 여기서 막는다.
 */
class TableConsistencyTest {

  private static final Path JTE = Path.of("src/main/jte/stock");

  /** 표 모양을 표마다 고르는 클래스 - .data-table 이 대신 정한다. */
  private static final Set<String> LOOK_CLASSES =
      Set.of(
          "table-xs",
          "table-sm",
          "table-md",
          "table-lg",
          "table-zebra",
          "whitespace-nowrap",
          "text-xs",
          "text-sm");

  /** 자료 표가 아닌 것: 달력 격자(칸이 날짜인 배치용 표). */
  private static final String LAYOUT_TABLE_MARK = "data-calendar-table";

  private static final Pattern TABLE_TAG = Pattern.compile("<table\\b[^>]*>");
  private static final Pattern CLASS_ATTR = Pattern.compile("class=\"([^\"]*)\"");

  private record Table(String file, int line, String tag, String region) {}

  /** 표 하나씩(중첩 표는 바깥 표 영역 안에 같이 든다). */
  private static List<Table> tables() throws IOException {
    List<Table> out = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(JTE)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String src = Files.readString(p, StandardCharsets.UTF_8);
        Matcher m = TABLE_TAG.matcher(src);
        while (m.find()) {
          int end = src.indexOf("</table>", m.end());
          out.add(
              new Table(
                  JTE.relativize(p).toString().replace('\\', '/'),
                  src.substring(0, m.start()).split("\n", -1).length,
                  m.group(),
                  src.substring(m.start(), end < 0 ? src.length() : end)));
        }
      }
    }
    return out;
  }

  @Test
  void 자료_표는_data_table_한_벌만_쓴다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int checked = 0;
    for (Table t : tables()) {
      if (t.tag().contains(LAYOUT_TABLE_MARK)) {
        continue;
      }
      checked++;
      Matcher c = CLASS_ATTR.matcher(t.tag());
      List<String> tokens = c.find() ? Arrays.asList(c.group(1).split("\\s+")) : List.of();
      List<String> look = tokens.stream().filter(LOOK_CLASSES::contains).toList();
      if (!tokens.contains("table") || !tokens.contains("data-table") || !look.isEmpty()) {
        offenders.add(t.file() + ":" + t.line() + " " + tokens);
      }
    }
    assertThat(checked).as("표를 하나도 못 찾았다 - 경로가 옮겨졌다").isGreaterThanOrEqualTo(30);
    assertThat(offenders)
        .as("class=\"table data-table\" + 동작 클래스만(table-pin-rows · print-dense 등)")
        .isEmpty();
  }

  @Test
  void 표_안_숫자는_고정폭_글꼴을_쓰지_않는다() throws IOException {
    // N1: 본문 글꼴 + 자릿수 맞춤(.table 의 tabular-nums). font-mono 가 섞이면 같은 표 안에서 숫자 글꼴이 둘이 된다.
    List<String> offenders = new ArrayList<>();
    for (Table t : tables()) {
      if (t.tag().contains(LAYOUT_TABLE_MARK)) {
        continue;
      }
      if (Pattern.compile("(?<![\\w-])font-mono(?![\\w-])").matcher(t.region()).find()) {
        offenders.add(t.file() + ":" + t.line());
      }
    }
    assertThat(offenders).isEmpty();
  }

  @Test
  void 머리글_줄에_따로_배경을_칠하지_않는다() throws IOException {
    // 머리글 배경은 .table thead 가 정한다(H2). thead · 머리 줄에 bg-* 를 또 칠하면 표마다 머리 띠 색이 달라진다.
    List<String> offenders = new ArrayList<>();
    Pattern headBg =
        Pattern.compile("<thead[^>]*class=\"[^\"]*\\bbg-|<thead>\\s*<tr class=\"[^\"]*\\bbg-");
    for (Table t : tables()) {
      if (t.tag().contains(LAYOUT_TABLE_MARK)) {
        continue;
      }
      if (headBg.matcher(t.region()).find()) {
        offenders.add(t.file() + ":" + t.line());
      }
    }
    assertThat(offenders).isEmpty();
  }

  @Test
  void 표_모양은_main_css_한곳에서_정한다() throws IOException {
    String css = Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8);
    assertThat(css).contains(".data-table :where(th, td)");
  }
}
