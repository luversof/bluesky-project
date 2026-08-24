package net.luversof.api.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 월배당 지급이력 저장 시 관계 검증을 고정한다.
 *
 * <p>이 원장은 시뮬레이터의 원천 데이터다. 금액이 음수인지는 이미 검사하고 있었지만, 값끼리의 관계는 보지 않았다. 실측(202건 전수)으로 지금 데이터가 지키고 있는 두
 * 성질을 API 도 지키게 한다.
 *
 * <ul>
 *   <li>지급일 ≥ 기준일 — 실측 간격은 2~8 일이었다. 뒤집힌 행이 들어오면 배당 달력과 월별 집계가 어긋나는데 값 자체는 그럴듯해 원인을 찾기 어렵다.
 *   <li>주당 과세표준 ≤ 주당 배당 — 배당 중 과세 대상 몫이므로 넘을 수 없다. 넘으면 과세표준 비중이 100% 를 넘고 세후 예상 배당이 실제보다 작아진다.
 * </ul>
 */
class MonthlyDividendPayoutValidationTest {

  private final MonthlyDividendPayoutService service = new MonthlyDividendPayoutService();

  private void invoke(String name, Class<?>[] types, Object... args) throws Exception {
    Method method = MonthlyDividendPayoutService.class.getDeclaredMethod(name, types);
    method.setAccessible(true);
    try {
      method.invoke(service, args);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw e;
    }
  }

  private void dates(LocalDate recordDate, LocalDate payDate) throws Exception {
    invoke(
        "requireConsistentDates",
        new Class<?>[] {LocalDate.class, LocalDate.class},
        recordDate,
        payDate);
  }

  private void taxable(String taxableBase, String dividendAmount) throws Exception {
    invoke(
        "requireTaxableWithinDividend",
        new Class<?>[] {BigDecimal.class, BigDecimal.class},
        taxableBase == null ? null : new BigDecimal(taxableBase),
        dividendAmount == null ? null : new BigDecimal(dividendAmount));
  }

  @Test
  void 지급일이_기준일보다_뒤면_통과한다() throws Exception {
    dates(LocalDate.parse("2026-08-19"), LocalDate.parse("2026-08-21"));
    // 같은 날도 허용한다.
    dates(LocalDate.parse("2026-08-19"), LocalDate.parse("2026-08-19"));
  }

  @Test
  void 지급일이_기준일보다_앞서면_400_이다() {
    var thrown =
        assertThrows(
            ResponseStatusException.class,
            () -> dates(LocalDate.parse("2026-08-21"), LocalDate.parse("2026-08-19")));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
    assertTrue(thrown.getMessage().contains("payDate"));
  }

  @Test
  void 과세표준이_배당_이하면_통과한다() throws Exception {
    taxable("23", "580");
    taxable("29", "29"); // 전액 과세(리츠 등)
    taxable("0", "100");
  }

  @Test
  void 과세표준이_배당을_넘으면_400_이다() {
    var thrown = assertThrows(ResponseStatusException.class, () -> taxable("581", "580"));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
    assertTrue(thrown.getMessage().contains("taxableBasePerShare"));
  }

  @Test
  void 값이_없으면_검증을_건너뛴다() throws Exception {
    dates(null, LocalDate.parse("2026-08-21"));
    dates(LocalDate.parse("2026-08-19"), null);
    taxable(null, "100");
    taxable("10", null);
  }
}
