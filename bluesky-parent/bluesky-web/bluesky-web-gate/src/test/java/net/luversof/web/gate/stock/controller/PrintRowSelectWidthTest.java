package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 종이에서는 선택 체크박스 자리를 잡아 두지 않는다.
 *
 * <p>화면에서는 첫 칸에 24px 체크박스를 끼워 넣으므로 이름이 한 글자씩 끊기지 않게 최소 폭 8.125rem(130px)을 준다. 그런데 인쇄에서는 그 체크박스를
 * 숨기면서({@code .row-select-box { display:none }}) 최소 폭만 남아 130px 를 붙잡고 있었다.
 *
 * <p>실측 2026-09-11(816px, print 미디어): 배당 '종목별/계좌별 배당 수익률' 표가 첫 칸 130px 때문에 표폭 775px 가 되어
 * 컨테이너(750px)를 넘었다. 최소 폭을 풀면 첫 칸 61·41px, 표폭 750px 로 들어간다.
 */
class PrintRowSelectWidthTest {

  private static final String CSS = "src/main/frontend/main.css";

  private String read() throws IOException {
    return Files.readString(Path.of(CSS), StandardCharsets.UTF_8);
  }

  @Test
  void 화면에는_체크박스_자리를_준다() throws IOException {
    assertThat(read())
        .as("화면에서 최소 폭을 없애면 이름이 한 글자씩 끊긴다")
        .contains("table:has(tr[data-row-select]) tr > :first-child { min-width: 8.125rem; }");
  }

  @Test
  void 인쇄에서는_그_자리를_돌려준다() throws IOException {
    String css = read();

    int print = css.indexOf("@media print {");
    assertThat(print).as("인쇄 블록을 찾지 못했다").isGreaterThan(0);
    String block = css.substring(print);

    assertThat(block)
        .as("인쇄에서 체크박스를 숨기면서 그 자리는 그대로 두면 폭이 낭비된다")
        .contains("table:has(tr[data-row-select]) tr > :first-child {")
        .contains("min-width: 0;");
    assertThat(block).as("체크박스는 인쇄에서 숨긴다").contains(".row-select-box");
  }
}
