package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.util.MultiValueMap;

import net.luversof.client.user.httpexchange.UserInfoApiClient.UserInfoResponse;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

/**
 * 자산 성장 보유 스냅숏은 화면 필터를 따른다(검토 지적 2026-10-29: 계좌 하나로 좁혀도 전 계좌 · 전 종목 스냅숏이 떴다). 필터가 있으면 날짜 하나짜리 묶음
 * 호출(목록 필터를 받는 API)로, 핵심 제외면 핵심 태그 종목을 결과에서 뺀다.
 */
class HoldingsSnapshotFilterTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
  private static final UUID ACCOUNT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID CORE = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
  private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000c2");

  private final TradeProfitClient tradeProfitClient = mock(TradeProfitClient.class);
  private final StockItemClient stockItemClient = mock(StockItemClient.class);
  private StockAssetGrowthHtmxController controller;

  private static HoldingsSnapshotItem item(UUID id) {
    return new HoldingsSnapshotItem(
        id,
        id.toString(),
        "S",
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        null,
        BigDecimal.ONE,
        BigDecimal.ZERO);
  }

  @BeforeEach
  void setUp() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.setUseCodeAsDefaultMessage(true);
    controller =
        new StockAssetGrowthHtmxController(
            tradeProfitClient, null, null, stockItemClient, null, null, null, null, messages);
    var userInfo =
        new UserInfoResponse(
            USER_ID.toString(), "tester", "google", "1", null, null, List.of(), Map.of());
    var principal =
        new DefaultOAuth2User(List.of(), Map.of("sub", "1", "userInfo", userInfo), "sub");
    SecurityContextHolder.getContext()
        .setAuthentication(new OAuth2AuthenticationToken(principal, List.of(), "google"));
    when(tradeProfitClient.holdingsSnapshot(any())).thenReturn(List.of(item(CORE), item(OTHER)));
    when(tradeProfitClient.holdingsSnapshotBatch(any()))
        .thenReturn(Map.of("2026-10-28", List.of(item(OTHER))));
    when(stockItemClient.getStockItems())
        .thenReturn(
            List.of(
                new StockItem(CORE, "1", "core", "KRX", List.of("핵심")),
                new StockItem(OTHER, "2", "other", "KRX", List.of())));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @SuppressWarnings("unchecked")
  private List<HoldingsSnapshotItem> holdings(ExtendedModelMap model) {
    return (List<HoldingsSnapshotItem>) model.get("holdings");
  }

  @Test
  void 필터가_없으면_단건_스냅숏() {
    var model = new ExtendedModelMap();
    controller.holdingsSnapshot("2026-10-28", null, "Asia/Seoul", null, null, false, model);

    assertThat(holdings(model)).hasSize(2);
    verify(tradeProfitClient, never()).holdingsSnapshotBatch(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void 계좌_필터는_날짜_하나짜리_묶음_호출로_넘긴다() {
    var model = new ExtendedModelMap();
    controller.holdingsSnapshot(
        "2026-10-28", null, "Asia/Seoul", List.of(ACCOUNT), null, false, model);

    ArgumentCaptor<MultiValueMap<String, String>> params =
        ArgumentCaptor.forClass(MultiValueMap.class);
    verify(tradeProfitClient).holdingsSnapshotBatch(params.capture());
    assertThat(params.getValue().get("dates")).containsExactly("2026-10-28");
    assertThat(params.getValue().get("accountIdList")).containsExactly(ACCOUNT.toString());
    assertThat(params.getValue().get("timeZone")).containsExactly("Asia/Seoul");
    assertThat(holdings(model))
        .extracting(HoldingsSnapshotItem::stockItemId)
        .containsExactly(OTHER);
  }

  @Test
  void 핵심_제외는_핵심_태그_종목을_뺀다() {
    var model = new ExtendedModelMap();
    controller.holdingsSnapshot("2026-10-28", null, "Asia/Seoul", null, null, true, model);

    assertThat(holdings(model))
        .extracting(HoldingsSnapshotItem::stockItemId)
        .containsExactly(OTHER);
  }

  /**
   * 모든 종목이 핵심이면 제외 뒤 남는 게 없다 - 빈 id 목록은 API 에서 '필터 없음' 이라 전체 이력이 나왔다(검토 지적 2026-10-29). 원격 호출 없이 빈
   * 이력.
   */
  @Test
  void 매매_이력_핵심_제외로_남는_종목이_없으면_빈_이력() {
    when(stockItemClient.getStockItems())
        .thenReturn(List.of(new StockItem(CORE, "1", "core", "KRX", List.of("핵심"))));
    var model = new ExtendedModelMap();

    controller.tradeHistory(
        "2026-01-01", "2026-10-28", null, null, 1, 20, "Asia/Seoul", true, model);

    assertThat(((Number) model.get("totalItems")).longValue()).isZero();
    assertThat((String) model.get("tradeHistoryFilterQuery")).contains("excludeCore=true");
  }
}
