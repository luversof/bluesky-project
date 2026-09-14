package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 축약 표기의 부호 규칙.
 *
 * <p>{@code compactKrw} 는 음수에만 "-" 를 달고 양수는 맨 숫자였다. 그래서 대시보드의 손익 카드는 방향을 <b>글자색</b>으로만 말했다 &mdash;
 * 실측 2026-09-12(고대비 모드, 6 화면): {@code .text-profit}/{@code .text-loss} 요소 273 개의 색이 <b>모두 검정 한
 * 가지</b>로 합쳐졌다. 그 상태에서 "9억 9,117만" 은 벌었는지 잃었는지 알 수 없다.
 *
 * <p>0 에는 부호를 붙이지 않는다 &mdash; {@code signedWon}·{@code signedPct} 와 같은 규칙이다 ("+0" 은 "0 원 벌었다" 처럼
 * 읽힌다).
 */
class SignedCompactKrwTest {

  @Test
  void 양수에만_더하기를_붙인다() {
    assertThat(StockFormatUtil.signedCompactKrw(991175852L)).startsWith("+");
    assertThat(StockFormatUtil.signedCompactKrw(1L)).startsWith("+");
  }

  @Test
  void 음수는_원래대로_빼기다() {
    String negative = StockFormatUtil.signedCompactKrw(-991175852L);
    assertThat(negative).startsWith("-");
    assertThat(negative).doesNotContain("+");
  }

  @Test
  void 영에는_부호를_붙이지_않는다() {
    assertThat(StockFormatUtil.signedCompactKrw(0L)).isEqualTo("0");
  }

  /** 숫자 부분은 그대로여야 한다 - 부호만 덧붙이는 것이지 표기를 바꾸는 것이 아니다. */
  @Test
  void 숫자_표기는_그대로다() {
    long value = 991175852L;
    assertThat(StockFormatUtil.signedCompactKrw(value))
        .isEqualTo("+" + StockFormatUtil.compactKrw(value));
    assertThat(StockFormatUtil.signedCompactKrw(-value))
        .isEqualTo(StockFormatUtil.compactKrw(-value));
  }

  /**
   * 손익이 아닌 카드에는 쓰지 않는다.
   *
   * <p>총 자산·투자원금·누적 배당·매수 금액은 방향이 없는 값이다. 나간 돈에 "+" 를 붙이면 번 돈처럼 읽힌다.
   */
  @Test
  void 손익_자리에만_쓴다() throws IOException {
    String summary =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/summary.jte"), StandardCharsets.UTF_8);

    assertThat(countOf(summary, "signedCompactKrw("))
        .as("평가손익 · 실현손익(전체) · 기간 실현손익 · 합산손익(값+낭독 두 번)")
        .isEqualTo(5);
    assertThat(summary)
        .as("누적 배당은 방향이 없는 값이다")
        .contains("value = StockFormatUtil.compactKrw(totalDividend.longValue())");
    assertThat(summary)
        .as("총 자산도 그대로")
        .contains("StockFormatUtil.compactKrw(StockFormatUtil.displayWon(totalAsset))");
  }

  private int countOf(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }
}
