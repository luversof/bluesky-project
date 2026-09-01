package net.luversof.web.gate.stock.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;

/**
 * 월배당 지급이력 입력 폼의 검증 규칙을 고정한다.
 *
 * <p>api-stock 도 같은 검증을 하지만, 화면에서 먼저 걸러야 사용자가 다른 항목과 같은 형식의 안내를 본다. 서버까지 가면 일반 오류로 표시된다.
 *
 * <p>특히 "주당 과세표준 ≤ 주당 분배금" 은 api-stock 에만 있고 화면에는 없어서, 그 값을 넣으면 다른 입력 오류와 다르게 처리됐다. 두 곳의 규칙을 맞춘다.
 */
class MonthlyDividendPayoutFormValidationTest {

  private final StockViewController controller = controllerWithKnownSymbol();

  /** 심볼 검증이 종목 목록을 조회하므로, 그 조회만 대신할 최소 스텁을 넣는다. */
  private StockViewController controllerWithKnownSymbol() {
    var created = new StockViewController();
    created.setStockItemClient(
        new StockItemClient() {
          @Override
          public List<StockItem> getStockItems() {
            return List.of(new StockItem(UUID.randomUUID(), "005930", "삼성전자", "KOSPI", List.of()));
          }

          @Override
          public Optional<StockItem> getStockItemById(UUID id) {
            return Optional.empty();
          }

          @Override
          public StockItem findByName(String name) {
            return null;
          }

          @Override
          public List<StockItem> getStockItemsByTag(String tag) {
            return List.of();
          }

          @Override
          public java.util.List<net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint>
              getPriceHistory(
                  java.util.UUID id,
                  org.springframework.util.MultiValueMap<String, String> params) {
            return java.util.List.of();
          }

          @Override
          public StockItem createStockItem(StockItem stockItem) {
            return stockItem;
          }
        });
    return created;
  }

  private MonthlyDividendPayoutUpsertRequest request() {
    var request = new MonthlyDividendPayoutUpsertRequest();
    request.setSymbol("005930");
    request.setRecordDate(LocalDate.parse("2026-08-19"));
    request.setPayDate(LocalDate.parse("2026-08-21"));
    request.setDistributionRatePct(new BigDecimal("0.5"));
    request.setDividendAmountPerShare(new BigDecimal("580"));
    request.setTaxableBasePerShare(new BigDecimal("23"));
    return request;
  }

  private void validate(MonthlyDividendPayoutUpsertRequest request) {
    try {
      Method method =
          StockViewController.class.getDeclaredMethod(
              "validateMonthlyDividendPayoutRequest", MonthlyDividendPayoutUpsertRequest.class);
      method.setAccessible(true);
      method.invoke(controller, request);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException(e);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void 정상_입력은_통과한다() {
    assertDoesNotThrow(() -> validate(request()));
  }

  @Test
  void 과세표준이_분배금을_넘으면_안내한다() {
    var request = request();
    request.setTaxableBasePerShare(new BigDecimal("581"));
    var thrown = assertThrows(IllegalArgumentException.class, () -> validate(request));
    assertTrue(thrown.getMessage().contains("과세표준"));
  }

  @Test
  void 전액_과세는_허용한다() {
    var request = request();
    request.setDividendAmountPerShare(new BigDecimal("29"));
    request.setTaxableBasePerShare(new BigDecimal("29"));
    assertDoesNotThrow(() -> validate(request));
  }

  @Test
  void 지급일이_기준일보다_빠르면_안내한다() {
    var request = request();
    request.setPayDate(LocalDate.parse("2026-08-18"));
    var thrown = assertThrows(IllegalArgumentException.class, () -> validate(request));
    assertTrue(thrown.getMessage().contains("실지급일"));
  }
}
