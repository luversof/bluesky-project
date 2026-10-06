package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport;
import net.luversof.web.gate.stock.service.MonthlyEtfViewSupport;
import net.luversof.web.gate.stock.support.StockAsyncSupport;

/**
 * 월배당 ETF 화면의 원격 호출 셋을 동시에 던지게 바꾼 뒤(2026-09-23)에도 실패했을 때의 결과는 예전과 같아야 한다.
 *
 * <p>예전 순차 코드의 약속: 카탈로그 · 원장 보유가 실패하면 그 예외가 그대로 올라가 오류 화면이 되고, 이번 적립 배지가 실패하면 배지만 비고 목록은 그린다.
 * 2026-10-02 부터 배지는 보유 여부와 상관없이 카탈로그 전체에서 고른다 - 월배당 스냅샷(예전 배지 출처)은 더 부르지 않는다. 동시화하면 예외가 다른 스레드에서 나서
 * {@code CompletionException} 으로 감싸이거나, 받는 자리(join)가 try 밖으로 빠지면 배지 실패가 화면 전체 실패로 번진다 &mdash; 그 둘을
 * 소스 모양이 아니라 실제로 불러서 본다.
 */
class MonthlyEtfRemoteFailureTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-00000000abcd");

  private final ExecutorService executor = Executors.newFixedThreadPool(3);
  private final MonthlyDividendCatalogClient catalogClient =
      mock(MonthlyDividendCatalogClient.class);
  private final MonthlyDividendReferenceSupport referenceSupport =
      mock(MonthlyDividendReferenceSupport.class);
  private final StockMonthlyEtfViewController controller = new StockMonthlyEtfViewController();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(controller, "monthlyDividendCatalogClient", catalogClient);
    ReflectionTestUtils.setField(controller, "monthlyDividendReferenceSupport", referenceSupport);
    ReflectionTestUtils.setField(
        controller,
        "monthlyEtfViewSupport",
        new MonthlyEtfViewSupport(new MonthlyContributionPickSupport()));
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
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    executor.shutdownNow();
  }

  private String open(ExtendedModelMap model) {
    return controller.monthlyEtfPage(
        new MockHttpServletRequest(),
        model,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  void 정상이면_두_호출을_한_번씩_하고_화면을_그린다() {
    var model = new ExtendedModelMap();
    assertThat(open(model)).isEqualTo("stock/monthlyEtf");
    verify(catalogClient, times(1)).findCatalog(any());
    verify(referenceSupport, times(1)).loadCurrentHoldings(USER_ID);
    verify(referenceSupport, never()).loadMonthlyDividendRows(any());
  }

  @Test
  void 스냅샷이_실패해도_배지는_카탈로그에서_낸다() {
    // 보유 스냅샷은 더 배지 출처가 아니다 - 실패하든 말든 화면 · 배지와 무관해야 한다.
    when(referenceSupport.loadMonthlyDividendRows(any()))
        .thenThrow(new IllegalStateException("스냅샷 실패"));
    var model = new ExtendedModelMap();

    assertThat(open(model)).isEqualTo("stock/monthlyEtf");
    assertThat(model.getAttribute("monthlyEtfContributionPicks"))
        .as("빈 카탈로그면 빈 배지")
        .isEqualTo(Map.of());
    assertThat(model.getAttribute("monthlyEtfRows")).as("목록은 그린다").isNotNull();
    verify(referenceSupport, never()).loadMonthlyDividendRows(any());
  }

  /** 카탈로그의 총보수 · 상장일이 목록 행까지 실린다(2026-09-28) - 옮기는 줄이 빠지면 화면이 늘 "미확인" 이다. */
  @Test
  void 총보수와_상장일이_목록_행에_실린다() {
    UUID id = UUID.randomUUID();
    when(catalogClient.findCatalog(any()))
        .thenReturn(
            List.of(
                new net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse(
                    id,
                    "476800",
                    "KODEX 한국부동산리츠인프라",
                    "MID_MONTH",
                    null,
                    null,
                    true,
                    1,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    new java.math.BigDecimal("0.0900"),
                    java.time.LocalDate.of(2024, 3, 5),
                    null,
                    null)));
    var model = new ExtendedModelMap();

    open(model);

    @SuppressWarnings("unchecked")
    var rows =
        (List<net.luversof.web.gate.stock.dto.view.MonthlyEtfRowView>)
            model.getAttribute("monthlyEtfRows");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).totalExpenseRatioPct()).isEqualByComparingTo("0.09");
    assertThat(rows.get(0).listingDate()).isEqualTo(java.time.LocalDate.of(2024, 3, 5));
  }

  @Test
  void 카탈로그가_실패하면_그_예외가_그대로_올라간다() {
    var failure = new IllegalStateException("카탈로그 실패");
    when(catalogClient.findCatalog(any())).thenThrow(failure);

    assertThatThrownBy(() -> open(new ExtendedModelMap()))
        .as("다른 스레드에서 났어도 CompletionException 으로 감싸지 않고 순차 호출 때와 같은 예외")
        .isSameAs(failure);
  }

  @Test
  void 원장_보유가_실패하면_그_예외가_그대로_올라간다() {
    var failure = new IllegalStateException("원장 실패");
    when(referenceSupport.loadCurrentHoldings(eq(USER_ID))).thenThrow(failure);

    assertThatThrownBy(() -> open(new ExtendedModelMap())).isSameAs(failure);
  }
}
