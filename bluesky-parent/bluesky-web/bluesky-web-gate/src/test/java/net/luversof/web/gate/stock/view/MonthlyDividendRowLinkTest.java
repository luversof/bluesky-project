package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 표의 종목명은 어느 화면에서나 종목 상세로 이어진다.
 *
 * <p>실측 2026-09-11(9 화면의 표를 전수): 종목 열이 있는 표 <b>17 개 · 476 행</b>이 모두 {@code /stock/item} 링크를 달고 있었는데
 * <b>월배당 시뮬레이터 표(8 행)만</b> 링크가 하나도 없었다 &mdash; 같은 종목을 눌러 들어가는 길이 이 화면에만 없다.
 */
class MonthlyDividendRowLinkTest {

  private static final Path FRAGMENT =
      Path.of("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 종목명이_상세로_이어진다() throws IOException {
    String fragment = read(FRAGMENT);

    int at = fragment.indexOf("row.stockItemName()");
    assertThat(at).isGreaterThan(0);
    String block = fragment.substring(Math.max(0, at - 320), at + 60);
    assertThat(block).contains("/stock/item?stockItemId=");
    assertThat(block).contains("row.stockItemId().toString()");
  }

  @Test
  void 종목ID가_없으면_글자로만_둔다() throws IOException {
    String fragment = read(FRAGMENT);

    // id 가 없는 행에 빈 주소를 걸면 눌렀을 때 "종목을 찾을 수 없습니다" 로 떨어진다.
    int at = fragment.indexOf("/stock/item?stockItemId=");
    String block = fragment.substring(Math.max(0, at - 200), at);
    assertThat(block).contains("@if(row.stockItemId() != null)");
  }
}
