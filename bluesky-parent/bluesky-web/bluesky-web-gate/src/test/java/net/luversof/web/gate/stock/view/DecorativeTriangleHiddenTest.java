package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 오르내림 · 정렬 화살표(▲ ▼)는 낭독기에 읽히면 "검은 아래쪽 삼각형" 이 된다(자율 점검 2026-09-30 decorative-glyph-sweep: 배당 내역 "▼
 * -16.5%"). 전기 대비 칸은 부호(-16.5%)가, 정렬 머리칸은 aria-sort 가 이미 같은 뜻을 전한다 - 화살표는 숨긴다.
 */
class DecorativeTriangleHiddenTest {

  private static String read(String path) throws IOException {
    return Files.readString(
        Path.of("src/main/jte/stock/htmx/fragments/dividend/" + path), StandardCharsets.UTF_8);
  }

  @Test
  void 전기_대비_화살표는_숨긴다() throws IOException {
    assertThat(read("dividendSummaryCards.jte"))
        .contains("<span aria-hidden=\"true\">${diffPositive ? \"▲\" : \"▼\"}</span>")
        .doesNotContain("(diffPositive ? \"▲\" : \"▼\")} ${diffPct}%");
  }

  /**
   * 서버 정렬 머리칸은 2026-10-22 부터 components/sortHeader.jte 한 곳에서 그린다. 그 전엔 배당 상세 표(7 칸)만 화살표를 숨기고 매매 상세
   * 표(5 칸)는 그대로 읽혔다 - 두 표가 같은 조각을 쓰는지까지 본다.
   */
  @Test
  void 정렬_머리칸_화살표는_숨긴다() throws IOException {
    String header =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/components/sortHeader.jte"),
            StandardCharsets.UTF_8);
    assertThat(header)
        .contains("<span aria-hidden=\"true\">${ascending ? \"▲\" : \"▼\"}</span>")
        .doesNotContain("<span>${");

    String sortHeaderCall = "@template.stock.htmx.fragments.components.sortHeader(";
    String dividend = read("dividendTable.jte");
    String trade =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/trade/tradeDetailList.jte"),
            StandardCharsets.UTF_8);
    assertThat(dividend.split(java.util.regex.Pattern.quote(sortHeaderCall), -1))
        .as("배당 상세 정렬 머리칸 일곱")
        .hasSize(8);
    assertThat(trade.split(java.util.regex.Pattern.quote(sortHeaderCall), -1))
        .as("매매 상세 정렬 머리칸 다섯")
        .hasSize(6);
    for (String table : new String[] {dividend, trade}) {
      assertThat(table).doesNotContain("\"▲\" : \"▼\"}</span>");
    }
  }
}
