package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 기간별 집계는 <b>바로잡은</b> 구간으로 만든다.
 *
 * <p>이 컨트롤러는 들머리에서 역순 기간을 {@code effectiveRange} 로 바로잡아 {@code startInstant}/{@code endInstant} 에
 * 담는다. 그런데 원본 파라미터({@code startDate}/{@code endDate})는 그대로 남아 있어, 뒤이어 그 둘을 읽으면 바로잡기가 무위가 된다 &mdash;
 * 이 세션에서 같은 부류가 여러 번 나왔다("파생 구간을 보정 전 파라미터로 계산").
 *
 * <p>실측 2026-09-12(배당 화면, 정상 구간 vs 앞뒤를 뒤집은 주소):
 *
 * <pre>
 *   상세 목록        202 → 202     변동 요인 8 → 8     연도별 7 → 7     랭킹 18/5 → 18/5
 *   배당 기간별 집계 월별  213 →  37   ⚠ (합계는 202건 · 73,423,094 로 같았다)
 * </pre>
 *
 * <p>빈 달 채우기가 {@code rangeStart.isAfter(rangeEnd)} 에서 그냥 빠져나가 "자료가 있는 달" 만 남은 것이다. 같은 기간을 가리키는 두
 * 주소가 다른 표를 그리면 안 된다.
 */
class DividendBreakdownRangeTest {

  private static final Path CONTROLLER =
      Path.of(
          "src/main/java/net/luversof/web/gate/stock/controller/StockDividendHtmxController.java");

  @Test
  void 집계는_바로잡은_구간을_쓴다() throws IOException {
    String source = Files.readString(CONTROLLER, StandardCharsets.UTF_8);

    // 서식에는 묶지 않는다 - spotless 가 삼항을 두세 줄로 나눈다(실측 2026-09-12: 이 검사가 그래서 한 번 깨졌다).
    String flat = source.replaceAll("\\s+", " ");
    int at = flat.indexOf("DividendPeriodBreakdown.byYear(");
    assertThat(at).as("연도별 집계 호출을 찾지 못했다").isGreaterThan(0);
    int end = flat.indexOf("StockDividendTtmUtil", at);
    String block = flat.substring(at, end > at ? end : flat.length());

    assertThat(countOf(block, "startInstant != null ? startInstant.atZone(breakdownZone)"))
        .as("연도별 + 월별 두 곳 모두 바로잡은 시작을 쓴다")
        .isEqualTo(2);
    assertThat(countOf(block, "endInstant != null ? endInstant.atZone(breakdownZone)"))
        .as("연도별 + 월별 두 곳 모두 바로잡은 끝을 쓴다")
        .isEqualTo(2);
    assertThat(block)
        .as("원본 파라미터를 다시 읽으면 바로잡기가 무위가 된다")
        .doesNotContain("? startDate.atZone(breakdownZone)")
        .doesNotContain("? endDate.atZone(breakdownZone)");
  }

  /** 바로잡기 자체가 사라지면 안 된다. */
  @Test
  void 들머리에서_역순을_바로잡는다() throws IOException {
    String source = Files.readString(CONTROLLER, StandardCharsets.UTF_8);

    assertThat(source)
        .contains("Instant[] orderedRange = effectiveRange(rangeMode, startDate, endDate);");
    assertThat(source).contains("Instant startInstant = orderedRange[0];");
    assertThat(source).contains("Instant endInstant = orderedRange[1];");
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
