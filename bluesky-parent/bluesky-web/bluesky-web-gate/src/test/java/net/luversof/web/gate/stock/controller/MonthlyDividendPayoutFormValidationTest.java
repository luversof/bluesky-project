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

  // 문구가 메시지 키로 옮겨가서(2026-09-08) 검사도 번들을 켜야 한다. 아래 단정은 한국어 문구를 본다.
  @org.junit.jupiter.api.BeforeAll
  static void primeMessages() {
    var source = new org.springframework.context.support.ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    source.setFallbackToSystemLocale(false);
    io.github.luversof.boot.context.support.MessageUtil.setMessageSourceAccessor(
        new org.springframework.context.support.MessageSourceAccessor(source));
    org.springframework.context.i18n.LocaleContextHolder.setDefaultLocale(java.util.Locale.KOREAN);
  }

  @org.junit.jupiter.api.AfterAll
  static void clearMessages() {
    io.github.luversof.boot.context.support.MessageUtil.setMessageSourceAccessor(null);
    org.springframework.context.i18n.LocaleContextHolder.setDefaultLocale(null);
  }

  private final StockDividendViewController controller = controllerWithKnownSymbol();

  /** 심볼 검증이 종목 목록을 조회하므로, 그 조회만 대신할 최소 스텁을 넣는다. */
  private StockDividendViewController controllerWithKnownSymbol() {
    var created = new StockDividendViewController();
    // 심볼 검증은 MonthlyDividendReferenceSupport 로 옮겼다. 컨트롤러는 그것을 통해 부른다.
    created.setMonthlyDividendReferenceSupport(support());
    return created;
  }

  /** 심볼 검증이 서포트로 옮겨갔다. 종목 조회만 대신하는 최소 스텁을 넣는다. */
  private net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport support() {
    var support = new net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport();
    support.setStockItemClient(
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
    support.setMonthlyDividendProfileClient(
        new net.luversof.web.gate.stock.httpexchange.MonthlyDividendProfileClient() {
          @Override
          public List<net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse>
              findProfiles(org.springframework.util.MultiValueMap<String, String> request) {
            return List.of();
          }

          @Override
          public net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse
              upsertProfile(
                  net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileUpsertRequest
                      request) {
            return null;
          }

          @Override
          public void reorderProfiles(
              net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileReorderRequest
                  request) {
            // 이 검사에서는 쓰지 않는다
          }

          @Override
          public void deleteProfile(String symbol) {
            // 이 검사에서는 쓰지 않는다
          }
        });
    return support;
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
          StockDividendViewController.class.getDeclaredMethod(
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
