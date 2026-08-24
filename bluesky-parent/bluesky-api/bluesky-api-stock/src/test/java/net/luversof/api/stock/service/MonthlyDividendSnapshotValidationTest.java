package net.luversof.api.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.web.dto.request.MonthlyDividendSnapshotUpsertRequest;

/**
 * 월배당 스냅샷 저장 시 수치 검증을 고정한다.
 *
 * <p>예전에는 {@code userId} 와 {@code symbol} 만 검사하고 수량·단가는 그대로 저장했다. 화면 폼이 {@code min="0"} / {@code
 * min="1"} 을 선언하지만 그건 브라우저 표시일 뿐이고, api-stock 은 인증 없이 노출돼 있어 아무 값이나 그대로 들어온다. 음수 수량이나 음수 단가가 한 번
 * 저장되면 예상 월배당과 수익률이 음수로 나오고, 화면에서는 원인을 되짚을 수 없다.
 *
 * <p>과세표준 비율은 배당 중 과세 대상 비중이라 0~100% 를 벗어날 수 없다.
 */
class MonthlyDividendSnapshotValidationTest {

  private final MonthlyDividendSnapshotService service = new MonthlyDividendSnapshotService();

  private MonthlyDividendSnapshotUpsertRequest request() {
    var request = new MonthlyDividendSnapshotUpsertRequest();
    request.setUserId(UUID.randomUUID());
    request.setSymbol("005930");
    request.setHeldQuantity(10);
    request.setAverageBuyPrice(new BigDecimal("42.50"));
    request.setLatestMonthlyDividendPerShare(new BigDecimal("100"));
    request.setAverageMonthlyDividendPerShare1y(new BigDecimal("90"));
    request.setAverageTaxableBaseRatio1y(new BigDecimal("30"));
    return request;
  }

  /** {@code validateAmounts} 는 private 이라 리플렉션으로 부른다(저장소 없이 검증만 확인하기 위해서다). */
  private void validate(MonthlyDividendSnapshotUpsertRequest request) throws Exception {
    Method method =
        MonthlyDividendSnapshotService.class.getDeclaredMethod(
            "validateAmounts", MonthlyDividendSnapshotUpsertRequest.class);
    method.setAccessible(true);
    try {
      method.invoke(service, request);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw e;
    }
  }

  @Test
  void 정상_값은_통과한다() throws Exception {
    validate(request());
  }

  @Test
  void 음수_수량은_400_이다() {
    var request = request();
    request.setHeldQuantity(-1);
    var thrown = assertThrows(ResponseStatusException.class, () -> validate(request));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
    assertTrue(thrown.getMessage().contains("heldQuantity"));
  }

  @Test
  void 음수_단가와_음수_배당은_400_이다() {
    var negativePrice = request();
    negativePrice.setAverageBuyPrice(new BigDecimal("-1"));
    assertThrows(ResponseStatusException.class, () -> validate(negativePrice));

    var negativeDividend = request();
    negativeDividend.setLatestMonthlyDividendPerShare(new BigDecimal("-0.01"));
    assertThrows(ResponseStatusException.class, () -> validate(negativeDividend));

    var negativeAverage = request();
    negativeAverage.setAverageMonthlyDividendPerShare1y(new BigDecimal("-5"));
    assertThrows(ResponseStatusException.class, () -> validate(negativeAverage));
  }

  @Test
  void 과세표준_비율은_0에서_100_사이여야_한다() throws Exception {
    var tooHigh = request();
    tooHigh.setAverageTaxableBaseRatio1y(new BigDecimal("100.01"));
    assertThrows(ResponseStatusException.class, () -> validate(tooHigh));

    var negative = request();
    negative.setAverageTaxableBaseRatio1y(new BigDecimal("-0.01"));
    assertThrows(ResponseStatusException.class, () -> validate(negative));

    // 경계값은 허용한다.
    var zero = request();
    zero.setAverageTaxableBaseRatio1y(BigDecimal.ZERO);
    validate(zero);
    var hundred = request();
    hundred.setAverageTaxableBaseRatio1y(new BigDecimal("100"));
    validate(hundred);
  }

  @Test
  void 값이_없으면_검증을_건너뛴다() throws Exception {
    var request = request();
    request.setHeldQuantity(null);
    request.setAverageBuyPrice(null);
    request.setLatestMonthlyDividendPerShare(null);
    request.setAverageMonthlyDividendPerShare1y(null);
    request.setAverageTaxableBaseRatio1y(null);
    validate(request);
  }
}
