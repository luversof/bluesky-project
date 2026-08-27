package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;

import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesResult;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesSummary;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

/**
 * 백엔드 실패를 "계산할 수 없는 기간" 으로 위장하지 않는지 본다.
 *
 * <p>자산증가 화면의 기간수익률 조각은 api-stock 호출이 예외로 끝나도 요약을 {@code null} 로 넘겼다. 그러면 템플릿이 기초 평가액이 너무 작아 비율을 못
 * 내는 <b>정상</b> 상황과 똑같이 "계산할 수 없음" 을 그린다. 두 상황은 사용자가 할 일이 다르다 &mdash; 앞은 다시 시도, 뒤는 기간 변경이다.
 */
class PeriodSummaryFailureTest {

  private static final UUID USER_ID = UUID.randomUUID();

  private StockAssetGrowthHtmxController controller(TradeProfitClient client) {
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage(
        "stock.error.fragment.title",
        org.springframework.context.i18n.LocaleContextHolder.getLocale(),
        "불러오지 못했습니다");
    messages.setUseCodeAsDefaultMessage(true);
    return new StockAssetGrowthHtmxController(client, null, null, null, null, null, null, messages);
  }

  @Test
  void 원격_호출이_실패하면_예외를_그대로_올린다() {
    var controller = controller(new ThrowingTradeProfitClient());

    assertThatThrownBy(
            () -> controller.loadPeriodSummary(USER_ID, "2026-01-01", "2026-08-22", "Asia/Seoul"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("backend down");
  }

  @Test
  void 정상이면_요약을_돌려준다() {
    var controller = controller(new StubTradeProfitClient());

    var summary = controller.loadPeriodSummary(USER_ID, "2026-01-01", "2026-08-22", "Asia/Seoul");

    assertThat(summary).isNotNull();
    assertThat(summary.growthRatePct()).isEqualTo(12.5d);
  }

  /** 입력이 없거나 말이 안 되는 기간은 원격 호출 없이 null. 이건 실패가 아니라 '계산할 값이 없음' 이다. */
  @Test
  void 잘못된_입력은_호출도_하지_않고_null이다() {
    var client = new ThrowingTradeProfitClient();
    var controller = controller(client);

    assertThat(controller.loadPeriodSummary(USER_ID, null, "2026-08-22", "Asia/Seoul")).isNull();
    assertThat(controller.loadPeriodSummary(USER_ID, "", "2026-08-22", "Asia/Seoul")).isNull();
    assertThat(controller.loadPeriodSummary(null, "2026-01-01", "2026-08-22", "Asia/Seoul"))
        .isNull();
    // 날짜 형식이 아니면 계산할 값이 없다(예외로 올리지 않는다).
    assertThat(controller.loadPeriodSummary(USER_ID, "2026-13-45", "2026-08-22", "Asia/Seoul"))
        .isNull();
    // 역전된 기간도 마찬가지.
    assertThat(controller.loadPeriodSummary(USER_ID, "2026-08-22", "2026-01-01", "Asia/Seoul"))
        .isNull();

    assertThat(client.calls).as("잘못된 입력에 원격 호출을 하면 안 된다").isZero();
  }

  @Test
  void 실패_안내는_오류_조각과_문구를_돌려준다() {
    var controller = controller(new StubTradeProfitClient());
    Model model = new ConcurrentModel();

    String view = controller.remoteFailureView(model);

    assertThat(view).isEqualTo("stock/htmx/error");
    assertThat(model.getAttribute("error")).isEqualTo("불러오지 못했습니다");
  }

  /** 안내 문구 키가 실제 번들에 있는지. 없으면 화면에 키 문자열이 그대로 나간다. */
  @Test
  void 안내_문구_키가_번들에_있다() throws java.io.IOException {
    for (String bundle : List.of("uiMessage.properties", "uiMessage_ko.properties")) {
      java.nio.file.Path path = java.nio.file.Path.of("src/main/resources", bundle);
      String source = java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
      assertThat(source)
          .as(bundle + " 에 stock.error.fragment.title 이 없다")
          .contains("stock.error.fragment.title");
    }
  }

  private static class StubTradeProfitClient implements TradeProfitClient {
    int calls;

    @Override
    public List<TradeProfit> calculateProfit(MultiValueMap<String, String> request) {
      return List.of();
    }

    @Override
    public List<TradeProfitTimeSeriesPoint> timeSeries(MultiValueMap<String, String> request) {
      return List.of();
    }

    @Override
    public TradeProfitTimeSeriesResult timeSeriesWithSummary(
        MultiValueMap<String, String> request) {
      calls++;
      return new TradeProfitTimeSeriesResult(
          List.of(),
          new TradeProfitTimeSeriesSummary(
              null, null, 12.5d, null, null, null, null, null, null, null, null, null, null, null,
              null, null, null, null, null, null),
          List.of());
    }

    @Override
    public List<HoldingsSnapshotItem> holdingsSnapshot(MultiValueMap<String, String> request) {
      return List.of();
    }

    @Override
    public java.util.Map<String, List<HoldingsSnapshotItem>> holdingsSnapshotBatch(
        MultiValueMap<String, String> request) {
      return java.util.Map.of();
    }
  }

  private static final class ThrowingTradeProfitClient extends StubTradeProfitClient {
    @Override
    public TradeProfitTimeSeriesResult timeSeriesWithSummary(
        MultiValueMap<String, String> request) {
      calls++;
      throw new IllegalStateException("backend down");
    }
  }
}
