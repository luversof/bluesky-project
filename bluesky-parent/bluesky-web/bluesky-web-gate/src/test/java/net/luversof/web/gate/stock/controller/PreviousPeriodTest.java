package net.luversof.web.gate.stock.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/**
 * 배당 "전기 대비" 비교 구간을 고정한다.
 *
 * <p>비교가 성립하려면 전기가 현재 구간 시작 <b>바로 앞</b>에서 끝나야 한다. 하루라도 틈이 생기면 그 사이 배당이 어느 쪽에도 안 잡히고, 겹치면 양쪽에 두 번
 * 잡힌다. 두 경우 모두 증감률만 이상해질 뿐 오류는 나지 않아 알아채기 어렵다.
 *
 * <p>구간 종류마다 규칙이 다르다(화면에도 비교 기간을 그대로 표시한다).
 *
 * <ul>
 *   <li>{@code mtd} — 전월 1일~말일(통월). 이번 달은 진행 중이라 길이가 다를 수 있고, 그래서 화면이 비교 기간을 밝힌다.
 *   <li>{@code ytd} — 전년 1/1~12/31(통년)
 *   <li>{@code N} — 직전 N 개월(달력 정렬). 달 길이 차이로 현재와 최대 3 일까지 다를 수 있다(실측: 1년치 날짜에서 -3~+3일).
 *   <li>그 외(수동 지정) — 직전 동일 길이
 * </ul>
 */
class PreviousPeriodTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  // resolvePreviousPeriod 는 필드를 쓰지 않으므로 협력 객체 없이 만든다.
  private final StockDividendHtmxController controller =
      new StockDividendHtmxController(null, null, null, null, null, null, null);

  private record Period(LocalDate start, LocalDate end) {}

  private Period resolve(String from, String to, String rangeMode) {
    try {
      Method method =
          StockDividendHtmxController.class.getDeclaredMethod(
              "resolvePreviousPeriod", Instant.class, Instant.class, String.class, ZoneId.class);
      method.setAccessible(true);
      Object result =
          method.invoke(
              controller,
              LocalDate.parse(from).atStartOfDay(KST).toInstant(),
              LocalDate.parse(to).atStartOfDay(KST).toInstant(),
              rangeMode,
              KST);
      if (result == null) {
        return null;
      }
      Method start = result.getClass().getDeclaredMethod("start");
      Method end = result.getClass().getDeclaredMethod("end");
      start.setAccessible(true);
      end.setAccessible(true);
      return new Period((LocalDate) start.invoke(result), (LocalDate) end.invoke(result));
    } catch (InvocationTargetException e) {
      throw new IllegalStateException(e.getCause());
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void 전기는_현재_시작_바로_앞에서_끝난다() {
    for (String[] range :
        new String[][] {
          {"2026-08-01", "2026-08-22", "mtd"},
          {"2026-01-01", "2026-08-22", "ytd"},
          {"2026-07-23", "2026-08-22", "1"},
          {"2026-05-23", "2026-08-22", "3"},
          {"2025-08-23", "2026-08-22", "12"},
          {"2026-03-05", "2026-04-09", "manual"},
        }) {
      Period previous = resolve(range[0], range[1], range[2]);
      assertEquals(
          LocalDate.parse(range[0]).minusDays(1),
          previous.end(),
          "rangeMode=" + range[2] + " 의 전기 종료일이 현재 시작 하루 전이어야 한다");
    }
  }

  @Test
  void 이번달은_전월_통월과_비교한다() {
    Period previous = resolve("2026-08-01", "2026-08-22", "mtd");
    assertEquals(LocalDate.parse("2026-07-01"), previous.start());
    assertEquals(LocalDate.parse("2026-07-31"), previous.end());
  }

  @Test
  void 올해는_전년_통년과_비교한다() {
    Period previous = resolve("2026-01-01", "2026-08-22", "ytd");
    assertEquals(LocalDate.parse("2025-01-01"), previous.start());
    assertEquals(LocalDate.parse("2025-12-31"), previous.end());
  }

  @Test
  void 상대_개월수는_달력으로_맞춘다() {
    Period previous = resolve("2026-07-23", "2026-08-22", "1");
    assertEquals(LocalDate.parse("2026-06-23"), previous.start());
    assertEquals(LocalDate.parse("2026-07-22"), previous.end());
  }

  @Test
  void 수동_지정은_직전_동일_길이다() {
    // 2026-03-05 ~ 2026-04-09 = 36 일
    Period previous = resolve("2026-03-05", "2026-04-09", "manual");
    assertEquals(LocalDate.parse("2026-01-28"), previous.start());
    assertEquals(LocalDate.parse("2026-03-04"), previous.end());
  }

  @Test
  void 시작이나_종료가_없으면_비교하지_않는다() {
    assertNull(
        resolveRaw(null, LocalDate.parse("2026-08-22").atStartOfDay(KST).toInstant(), "mtd"));
    assertNull(
        resolveRaw(LocalDate.parse("2026-08-01").atStartOfDay(KST).toInstant(), null, "mtd"));
  }

  /** null 인스턴트를 그대로 넘겨보기 위한 경로. */
  private Object resolveRaw(Instant startDate, Instant endDate, String rangeMode) {
    try {
      Method method =
          StockDividendHtmxController.class.getDeclaredMethod(
              "resolvePreviousPeriod", Instant.class, Instant.class, String.class, ZoneId.class);
      method.setAccessible(true);
      return method.invoke(controller, startDate, endDate, rangeMode, KST);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
