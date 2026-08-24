package net.luversof.web.gate.stock.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/**
 * "이번 달 진행 중" 표기 판정을 고정한다.
 *
 * <p>전기 비교 카드는 진행 중인 이번 달을 <b>완전한 전월</b>과 견준다. 그래서 달 초·중순에는 증감률이 실제 추세와 반대로 읽힌다(실측 2026-08-22: 22
 * 일치 3,062,734 vs 31 일치 3,355,246 이라 화면은 -8.7% 인데, 일평균은 139,215 vs 108,234 로 +28.6%).
 *
 * <p>수치는 그대로 두고 "아직 진행 중"이라는 사실만 밝히므로, 언제 그 표기가 붙는지가 이 판정의 전부다.
 */
class InProgressPeriodTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final StockDividendHtmxController controller =
      new StockDividendHtmxController(null, null, null, null, null, null, null);

  private int[] progress(String from, String to, String rangeMode) {
    try {
      Method method =
          StockDividendHtmxController.class.getDeclaredMethod(
              "resolveInProgressPeriod", Instant.class, Instant.class, String.class, ZoneId.class);
      method.setAccessible(true);
      return (int[])
          method.invoke(
              controller,
              from == null ? null : LocalDate.parse(from).atStartOfDay(KST).toInstant(),
              to == null ? null : LocalDate.parse(to).atStartOfDay(KST).toInstant(),
              rangeMode,
              KST);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void 달_중간이면_경과일과_총일수를_준다() {
    assertArrayEquals(new int[] {22, 31}, progress("2026-08-01", "2026-08-22", "mtd"));
    assertArrayEquals(new int[] {1, 28}, progress("2026-02-01", "2026-02-01", "mtd"));
  }

  @Test
  void 달_마지막_날이면_표기하지_않는다() {
    assertNull(progress("2026-08-01", "2026-08-31", "mtd"));
    assertNull(progress("2026-02-01", "2026-02-28", "mtd"));
  }

  @Test
  void 올해_프리셋도_진행_중이면_표기한다() {
    assertArrayEquals(new int[] {234, 365}, progress("2026-01-01", "2026-08-22", "ytd"));
    // 윤년
    assertArrayEquals(new int[] {60, 366}, progress("2024-01-01", "2024-02-29", "ytd"));
  }

  @Test
  void 해의_마지막_날이면_표기하지_않는다() {
    assertNull(progress("2026-01-01", "2026-12-31", "ytd"));
  }

  @Test
  void 달력_프리셋이_아니면_표기하지_않는다() {
    assertNull(progress("2026-07-23", "2026-08-22", "1"));
    assertNull(progress("2025-08-23", "2026-08-22", "12"));
    assertNull(progress("2026-03-05", "2026-04-09", "manual"));
  }

  @Test
  void 기간이_없으면_표기하지_않는다() {
    assertNull(progress(null, "2026-08-22", "mtd"));
    assertNull(progress("2026-08-01", null, "mtd"));
  }
}
