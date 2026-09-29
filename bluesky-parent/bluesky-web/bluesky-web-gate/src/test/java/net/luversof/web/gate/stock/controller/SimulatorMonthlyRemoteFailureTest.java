package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import net.luversof.client.user.httpexchange.UserInfoApiClient.UserInfoResponse;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendCatalogClient;
import net.luversof.web.gate.stock.service.MonthlyContributionPickSupport;
import net.luversof.web.gate.stock.service.MonthlyDividendCalculator;
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport;
import net.luversof.web.gate.stock.service.MonthlyDividendViewSupport;
import net.luversof.web.gate.stock.support.StockAsyncSupport;

/**
 * 시뮬레이터 월배당 탭이 원격 실패 때 화면에 무엇을 말하는지(2026-09-23 - 태그 종목 조회를 동시화한 뒤 고정).
 *
 * <p>약속: 카탈로그 실패는 삼키고(적립 추천 · 지급 시기/계좌 필터만 건너뜀) 필터를 걸었으면 "필터를 쓸 수 없다" 를 알린다. 입력 폼 종목 목록(태그 조회)과 스냅샷
 * 행은 예전 순차 코드처럼 실패가 그대로 올라간다 - 동시화 뒤에도 같은 예외 인스턴스가(CompletionException 으로 감싸지지 않고).
 */
class SimulatorMonthlyRemoteFailureTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-00000000beef");

  private final ExecutorService executor = Executors.newFixedThreadPool(6);
  private final MonthlyDividendCatalogClient catalogClient =
      mock(MonthlyDividendCatalogClient.class);
  private final MonthlyDividendReferenceSupport referenceSupport =
      mock(MonthlyDividendReferenceSupport.class);
  private final StockViewController controller = new StockViewController();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(controller, "monthlyDividendCatalogClient", catalogClient);
    ReflectionTestUtils.setField(controller, "monthlyDividendReferenceSupport", referenceSupport);
    ReflectionTestUtils.setField(
        controller, "monthlyDividendViewSupport", new MonthlyDividendViewSupport());
    ReflectionTestUtils.setField(
        controller, "monthlyDividendCalculator", new MonthlyDividendCalculator());
    ReflectionTestUtils.setField(
        controller, "monthlyContributionPickSupport", new MonthlyContributionPickSupport());
    ReflectionTestUtils.setField(controller, "stockAsync", new StockAsyncSupport(executor));

    var userInfo =
        new UserInfoResponse(
            USER_ID.toString(), "tester", "google", "1", null, null, List.of(), Map.of());
    var principal =
        new DefaultOAuth2User(List.of(), Map.of("sub", "1", "userInfo", userInfo), "sub");
    SecurityContextHolder.getContext()
        .setAuthentication(new OAuth2AuthenticationToken(principal, List.of(), "google"));

    when(catalogClient.findCatalog(any())).thenReturn(List.of());
    when(referenceSupport.loadCurrentHoldings(eq(USER_ID)))
        .thenReturn(
            new MonthlyDividendReferenceSupport.CurrentHoldings(Map.of(), Map.of(), null, false));
    when(referenceSupport.referenceTaxableRatioBySymbol()).thenReturn(Map.of());
    when(referenceSupport.loadTaxableRatioBasis(eq(USER_ID))).thenReturn(Map.of());
    when(referenceSupport.loadMonthlyDividendRows(eq(USER_ID))).thenReturn(List.of());
    when(referenceSupport.loadMonthlyDividendProfiles()).thenReturn(List.of());
    when(referenceSupport.loadMonthlyDividendStockItems()).thenReturn(List.of());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    executor.shutdownNow();
  }

  private String open(ExtendedModelMap model, String payoutWindow, String account) {
    return controller.simulatorPage(
        new MockHttpServletRequest(),
        model,
        "monthly-dividend",
        null,
        null,
        null,
        null,
        false,
        payoutWindow,
        account,
        null);
  }

  @Test
  void 정상이면_필터를_쓸_수_있다() {
    var model = new ExtendedModelMap();
    assertThat(open(model, "MID_MONTH", "BROKERAGE")).isEqualTo("stock/simulator");
    assertThat(model.getAttribute("monthlyDividendSlotFilterUnavailable")).isEqualTo(false);
  }

  @Test
  void 카탈로그가_실패하고_필터를_걸었으면_쓸_수_없다고_알린다() {
    when(catalogClient.findCatalog(any())).thenThrow(new IllegalStateException("카탈로그 실패"));
    var model = new ExtendedModelMap();

    assertThat(open(model, "MID_MONTH", "BROKERAGE")).as("화면은 그린다").isEqualTo("stock/simulator");
    assertThat(model.getAttribute("monthlyDividendSlotFilterUnavailable"))
        .as("필터가 조용히 풀린 것을 걸린 것으로 읽지 않게 알린다")
        .isEqualTo(true);
  }

  @Test
  void 카탈로그가_실패해도_필터를_안_걸었으면_알릴_것이_없다() {
    when(catalogClient.findCatalog(any())).thenThrow(new IllegalStateException("카탈로그 실패"));
    var model = new ExtendedModelMap();

    assertThat(open(model, null, null)).isEqualTo("stock/simulator");
    assertThat(model.getAttribute("monthlyDividendSlotFilterUnavailable")).isEqualTo(false);
  }

  @Test
  void 태그_종목_조회가_실패하면_그_예외가_그대로_올라간다() {
    var failure = new IllegalStateException("태그 조회 실패");
    when(referenceSupport.loadMonthlyDividendStockItems()).thenThrow(failure);

    assertThatThrownBy(() -> open(new ExtendedModelMap(), null, null))
        .as("동시화 뒤에도 순차 호출 때와 같은 예외(감싸지 않음)")
        .isSameAs(failure);
  }

  @Test
  void 원장_보유가_실패하면_그_예외가_그대로_올라간다() {
    var failure = new IllegalStateException("원장 실패");
    when(referenceSupport.loadCurrentHoldings(eq(USER_ID))).thenThrow(failure);

    assertThatThrownBy(() -> open(new ExtendedModelMap(), null, null)).isSameAs(failure);
  }
}
