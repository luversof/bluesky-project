package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

/**
 * 앞뒤가 뒤집힌 기간은 어느 화면에서도 같은 규칙으로 바로잡는다.
 *
 * <p>실측 2026-09-11(주소에 startDate &gt; endDate 를 넣고 여섯 화면):
 *
 * <table>
 *   <tr><th>화면</th><th>배지</th><th>행</th></tr>
 *   <tr><td>매매·배당·활동·자산 성장</td><td>2026-01-01 ~ 2026-09-11(바로잡음)</td><td>115 · 149 · 114 · 41</td></tr>
 *   <tr><td><b>종목 상세·계좌 상세</b></td><td><b>2026-09-12 ~ 2025-12-31</b>(그대로)</td><td><b>1 · 3</b></td></tr>
 * </table>
 *
 * <p>상세 화면 둘만 규칙이 빠져 있었다. 규칙을 util 한 곳으로 모으고 두 컨트롤러가 그것을 쓴다.
 */
class RangeOrderRuleTest {

  @Test
  void 뒤집힌_기간은_앞뒤를_바꾼다() {
    Instant early = Instant.parse("2025-12-31T15:00:00Z");
    Instant late = Instant.parse("2026-09-11T15:00:00Z");

    assertThat(StockRangePresetUtil.ordered(late, early)).containsExactly(early, late);
    assertThat(StockRangePresetUtil.ordered(early, late)).containsExactly(early, late);
  }

  @Test
  void 한쪽이_비면_그대로_둔다() {
    Instant only = Instant.parse("2026-09-11T15:00:00Z");

    assertThat(StockRangePresetUtil.ordered(only, null)).containsExactly(only, null);
    assertThat(StockRangePresetUtil.ordered(null, only)).containsExactly(null, only);
    assertThat(StockRangePresetUtil.ordered(null, null)).containsExactly(null, null);
  }

  @Test
  void 같은_시각은_바꾸지_않는다() {
    Instant same = Instant.parse("2026-09-11T15:00:00Z");

    assertThat(StockRangePresetUtil.ordered(same, same)).containsExactly(same, same);
  }

  @Test
  void 매매이력_조각도_앞뒤를_바로잡는다() throws IOException {
    // 실측 2026-09-11: 조각을 직접 부르면(from > to) 배지가 "2026-09-11 ~ 2026-01-01" 로 뜨고
    // "해당 기간의 매매 내역이 없습니다" 가 나왔다 - 화면 경로는 이미 바로잡힌 값을 넘기지만 조각은 그대로였다.
    String controller =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockAssetGrowthHtmxController.java"),
            StandardCharsets.UTF_8);

    assertThat(controller).contains("orderedDayParams(from, to)");
    assertThat(controller).contains("fromDay.isAfter(toDay)");
  }

  @Test
  void 두_컨트롤러가_같은_규칙을_쓴다() throws IOException {
    String base =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockBaseHtmxController.java"),
            StandardCharsets.UTF_8);
    String detail =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java"),
            StandardCharsets.UTF_8);

    assertThat(base)
        .as("목록 화면들의 orderedRange 는 util 로 위임한다 - 두 벌이 되면 화면끼리 어긋난다")
        .contains("StockRangePresetUtil.ordered(startDate, endDate)");
    assertThat(base).doesNotContain("startDate.isAfter(endDate)");
    assertThat(detail)
        .as("상세 화면도 같은 규칙을 쓴다")
        .contains("StockRangePresetUtil.ordered(startDate, endDate)");
  }
}
