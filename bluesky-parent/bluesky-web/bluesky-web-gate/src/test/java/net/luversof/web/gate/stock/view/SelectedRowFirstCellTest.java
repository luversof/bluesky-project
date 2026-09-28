package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 선택한 행의 배경 · 왼쪽 막대는 첫 칸까지 칠한다 &mdash; 사용자 제보 2026-09-28(배당 계좌별 랭킹): 체크박스 · 이름 칸만 배경이 빠졌다.
 *
 * <p>이 표들의 첫 칸은 {@code <th scope="row">} 인데 규칙이 {@code > td} 만 칠했고, 막대는 {@code td:first-child} 라 어떤
 * 칸에도 걸리지 않았다. 고대비(forced-colors) 선택 표식도 같은 이유로 안 붙었다.
 */
class SelectedRowFirstCellTest {

  private static String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  @Test
  void 선택_행_규칙이_th_첫_칸까지_칠한다() throws IOException {
    for (String path :
        new String[] {
          "src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte",
          "src/main/jte/stock/htmx/fragments/assetStatus.jte"
        }) {
      String source = read(path);
      assertThat(source).as(path).contains("<th scope=\"row\"");
      assertThat(source).as(path + " - td 만 칠하면 첫 칸(th)이 빠진다").doesNotContain("-selected > td");
      assertThat(source)
          .as(path)
          .contains("-selected > :is(th, td) {")
          .contains("-selected > :first-child {");
    }
  }

  @Test
  void 고대비_선택_표식도_첫_칸에_붙는다() throws IOException {
    String built = read("src/main/resources/static/main.css");
    assertThat(built).contains("tr[aria-selected=true]>:first-child:before");
    assertThat(built).doesNotContain("tr[aria-selected=true]>td:first-child:before");
  }
}
