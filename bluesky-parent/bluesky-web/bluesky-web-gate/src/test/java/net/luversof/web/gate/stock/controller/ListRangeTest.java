package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/**
 * 목록 화면(매매 · 활동 · 배당)의 기간 규칙 - 2026-10-22 세 핸들러에서 StockBaseHtmxController.listRange 로 모으며 처음 둔 시험.
 */
class ListRangeTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final Instant EARLY = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant LATE = Instant.parse("2026-06-01T00:00:00Z");

  private static StockTradeHtmxController controller() {
    return new StockTradeHtmxController(null, null, null, null, null, null, null, null, null);
  }

  @Test
  void 전체면_날짜를_버린다() {
    var range = controller().listRange("all", EARLY, LATE, KST);

    assertThat(range.start()).isNull();
    assertThat(range.end()).isNull();
    assertThat(range.rangeMode()).isEqualTo("all");
  }

  @Test
  void 역순이면_바로잡고_모드는_그대로() {
    var range = controller().listRange("custom", LATE, EARLY, KST);

    assertThat(range.start()).isEqualTo(EARLY);
    assertThat(range.end()).isEqualTo(LATE);
    assertThat(range.rangeMode()).isEqualTo("custom");
  }

  @Test
  void 날짜가_없으면_프리셋_기본_기간과_그_모드() {
    var controller = controller();
    for (String mode : new String[] {null, "", "ytd", "mtd"}) {
      var before = controller.resolvePresetRange(mode, KST);
      var range = controller.listRange(mode, null, null, KST);
      var after = controller.resolvePresetRange(mode, KST);
      // 프리셋은 '오늘' 을 읽는다 - 자정을 넘는 순간에도 흔들리지 않게 앞뒤 어느 쪽과 같으면 된다. 끝(내일 0 시)은 매일
      // 자정에, 시작(연초 · 월초)은 달 · 해 경계에서만 바뀌므로 둘 다 견준다(검토 2026-10-22).
      var expected =
          java.util.Objects.equals(range.start(), after.start())
                  && java.util.Objects.equals(range.end(), after.end())
              ? after
              : before;

      assertThat(range.start()).as("mode=" + mode).isEqualTo(expected.start()).isNotNull();
      assertThat(range.end()).as("mode=" + mode).isEqualTo(expected.end());
      assertThat(range.rangeMode()).as("mode=" + mode).isEqualTo(expected.mode());
    }
  }

  @Test
  void 날짜가_하나라도_있으면_기본_기간을_쓰지_않는다() {
    var range = controller().listRange("ytd", EARLY, null, KST);

    assertThat(range.start()).isEqualTo(EARLY);
    assertThat(range.end()).isNull();
    assertThat(range.rangeMode()).isEqualTo("ytd");
  }
}
