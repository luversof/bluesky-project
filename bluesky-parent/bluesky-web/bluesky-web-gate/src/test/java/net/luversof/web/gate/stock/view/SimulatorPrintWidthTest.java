package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 월배당 시뮬레이터의 선택 표는 종이에 다 들어가야 한다.
 *
 * <p>실측 2026-09-11(816px, print): 표 폭이 988px 라 지면을 넘어 <b>오른쪽 두 열</b>(평단 기준 수익률 · 예상 합산)이 통째로 잘렸다.
 * 원인은 글자 크기가 아니라 열마다 못박은 최소 폭이었다 &mdash; {@code print-dense}(0.70rem) 만 붙이면 973px 로 15px 밖에 줄지 않고,
 * 최소 폭을 풀면 787px, 둘을 함께 쓰면 764px 로 문서 폭이 정확히 816px 가 된다.
 */
class SimulatorPrintWidthTest {

  private static final Path TEMPLATE =
      Path.of("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");

  @Test
  void 선택_표는_인쇄에서_한_단계_더_조인다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    int at = template.indexOf("data-monthly-selection-table");
    assertThat(at).as("선택 표를 찾을 수 있어야 한다").isPositive();
    String tag = template.substring(template.lastIndexOf("<table", at), at);
    assertThat(tag).as("선택 표에 print-dense 가 붙어 있어야 한다").contains("print-dense");
  }

  @Test
  void 인쇄에서는_열_최소폭을_푼다() throws IOException {
    String source = Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8);
    String built =
        Files.readString(Path.of("src/main/resources/static/main.css"), StandardCharsets.UTF_8);

    assertThat(source).contains("table[data-monthly-selection-table] :is(th, td)");
    assertThat(built)
        .as("npm run build 를 돌리지 않으면 배포본에는 규칙이 없다")
        .contains(
            "table[data-monthly-selection-table] :is(th,td){min-width:0;padding-left:2px;padding-right:2px}");
  }

  /**
   * 활동 내역의 '상세' 칸은 종이에서 한 줄을 접는다.
   *
   * <p>실측 2026-09-11(816px, print): 칸 폭 343px 에 내용 최소 폭 485px 라 표 781px · 문서 821px 로 지면(816px)을
   * 넘었다. 글자를 0.70rem 으로 줄여도, {@code nowrap} 을 풀어도 821px 그대로였고, 그 줄을 {@code block} 으로 바꿔 흐르게 하면 표
   * 753px · 문서 816px 로 들어간다.
   */
  @Test
  void 활동_상세_칸은_인쇄에서_줄을_접는다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/activityList.jte"), StandardCharsets.UTF_8);
    assertThat(template).as("표를 가리킬 표식").contains("data-activity-list-table");

    String source = Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8);
    String built =
        Files.readString(Path.of("src/main/resources/static/main.css"), StandardCharsets.UTF_8);
    assertThat(source).contains("table[data-activity-list-table] :is(th, td) .flex");
    assertThat(built)
        .as("npm run build 를 돌리지 않으면 배포본에는 규칙이 없다")
        .contains("table[data-activity-list-table] :is(th,td) .flex{display:block}");
  }

  /**
   * 마지막 하한은 셀 패딩이었다.
   *
   * <p>실측 2026-09-11(794px, print): 최소 폭을 풀고 {@code print-dense} 까지 붙인 뒤에도 표 764px · 문서 805px 로
   * 11px 넘쳤다. 글자 0.65rem · 첫 칸 폭 해제 · 체크박스 숨김은 모두 805px 그대로였고, 좌우 패딩만 4px -&gt; 2px 로 줄이면 표 720px ·
   * 문서 794px 로 들어간다.
   */
  @Test
  void 마지막_11px_는_셀_패딩이었다() throws IOException {
    String source = Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8);

    int at = source.indexOf("table[data-monthly-selection-table] :is(th, td)");
    assertThat(at).isPositive();
    String block = source.substring(at, source.indexOf("}", at));
    assertThat(block)
        .as("이 표에서만 한 단계 더 줄인다")
        .contains("padding-left: 2px")
        .contains("padding-right: 2px");
  }

  /** 최소 폭 자체는 화면에서 열 정렬을 잡아 준다 - 종이에서만 푼다. */
  @Test
  void 화면용_최소폭은_그대로_둔다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template).contains("min-w-[6.5rem]");
    assertThat(template).contains("min-w-[7.5rem]");
  }
}
