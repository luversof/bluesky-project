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

  @Test
  void 배당_상세_정렬_화살표는_숨긴다() throws IOException {
    String table = read("dividendTable.jte");
    assertThat(table).doesNotContain("<span>${sortDir.equals(\"asc\") ? \"▲\" : \"▼\"}</span>");
    assertThat(table.split("<span aria-hidden=\"true\">\\$\\{sortDir.equals", -1))
        .as("정렬 머리칸 일곱")
        .hasSize(8);
  }
}
