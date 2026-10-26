package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
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
import net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse;
import net.luversof.web.gate.stock.dto.view.MonthlyEtfRowView;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendCatalogClient;
import net.luversof.web.gate.stock.service.MonthlyContributionPickSupport;
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport;
import net.luversof.web.gate.stock.service.MonthlyEtfViewSupport;
import net.luversof.web.gate.stock.support.StockAsyncSupport;

/**
 * 월배당 ETF 화면의 지급 시기 칩 개수는 "다른 조건은 그대로 두고 지급 시기만 바꿨을 때 남는 줄 수" 다 - 고른 칩의 개수는 지금 목록의 줄 수와 같아야 한다. 목록과
 * 칩 개수가 같은 거르기를 따로 적고 있던 것을 하나로 묶으며(2026-10-22) 그 약속을 실제로 불러서 본다.
 */
class MonthlyEtfPayoutWindowCountTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-00000000abcd");

  private final ExecutorService executor = Executors.newFixedThreadPool(3);
  private final MonthlyDividendCatalogClient catalogClient =
      mock(MonthlyDividendCatalogClient.class);
  private final MonthlyDividendReferenceSupport referenceSupport =
      mock(MonthlyDividendReferenceSupport.class);
  private final StockMonthlyEtfViewController controller = new StockMonthlyEtfViewController();

  private static <T> T record(Class<T> type, Map<String, Object> values) {
    try {
      RecordComponent[] components = type.getRecordComponents();
      Class<?>[] types = new Class<?>[components.length];
      Object[] args = new Object[components.length];
      for (int i = 0; i < components.length; i++) {
        types[i] = components[i].getType();
        Object value = values.get(components[i].getName());
        if (value == null && types[i] == boolean.class) {
          value = Boolean.FALSE;
        } else if (value == null && types[i] == long.class) {
          value = 0L;
        } else if (value == null && types[i] == double.class) {
          value = 0d;
        } else if (value == null && types[i] == int.class) {
          value = 0;
        }
        args[i] = value;
      }
      return type.getDeclaredConstructor(types).newInstance(args);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static MonthlyDividendCatalogResponse item(String symbol, String name, String window) {
    return record(
        MonthlyDividendCatalogResponse.class,
        Map.of(
            "stockItemId",
            UUID.nameUUIDFromBytes(symbol.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
            "stockItemSymbol",
            symbol,
            "stockItemName",
            name,
            "payoutWindow",
            window));
  }

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

    when(catalogClient.findCatalog(any()))
        .thenReturn(
            List.of(
                item("A00001", "알파 월중", "MID_MONTH"),
                item("A00002", "알파 월말", "MONTH_END"),
                item("B00001", "베타 월중", "MID_MONTH")));
    when(referenceSupport.loadCurrentHoldings(eq(USER_ID)))
        .thenReturn(
            new MonthlyDividendReferenceSupport.CurrentHoldings(Map.of(), Map.of(), null, false));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    executor.shutdownNow();
  }

  @SuppressWarnings("unchecked")
  private ExtendedModelMap open(String keyword, String payoutWindow) {
    var model = new ExtendedModelMap();
    controller.monthlyEtfPage(
        new MockHttpServletRequest(),
        model,
        null,
        null,
        keyword,
        null,
        payoutWindow,
        null,
        null,
        null,
        null,
        null);
    return model;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Integer> counts(ExtendedModelMap model) {
    return (Map<String, Integer>) model.getAttribute("monthlyEtfPayoutWindowCounts");
  }

  @SuppressWarnings("unchecked")
  private static List<MonthlyEtfRowView> rows(ExtendedModelMap model) {
    return (List<MonthlyEtfRowView>) model.getAttribute("monthlyEtfRows");
  }

  @Test
  void 지급_시기를_골라도_칩은_시기마다_센다() {
    var model = open(null, "MID_MONTH");

    assertThat(rows(model))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsOnly("A00001", "B00001");
    assertThat(counts(model))
        .containsExactly(
            Map.entry("", 3),
            Map.entry("MID_MONTH", 2),
            Map.entry("MONTH_END", 1),
            Map.entry("OTHER", 0),
            Map.entry("UNKNOWN", 0));
    assertThat(counts(model).get("MID_MONTH")).as("고른 칩 = 지금 목록").isEqualTo(rows(model).size());
  }

  @Test
  void 검색어는_칩_개수에도_걸린다() {
    var model = open("알파", "");

    assertThat(rows(model))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsOnly("A00001", "A00002");
    assertThat(counts(model))
        .containsExactly(
            Map.entry("", 2),
            Map.entry("MID_MONTH", 1),
            Map.entry("MONTH_END", 1),
            Map.entry("OTHER", 0),
            Map.entry("UNKNOWN", 0));
    assertThat(counts(model).get("")).as("고른 칩(전체) = 지금 목록").isEqualTo(rows(model).size());
  }
}
