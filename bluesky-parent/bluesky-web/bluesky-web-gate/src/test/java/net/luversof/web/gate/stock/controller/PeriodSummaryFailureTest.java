package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
 * 원격 실패 안내({@code remoteFailureView})는 오류 조각과 번들 문구로 나간다.
 *
 * <p>2026-09-09: 이 테스트가 함께 지키던 {@code loadPeriodSummary} 와 {@code /asset-growth/period-return} 라우트는
 * 지웠다. 화면은 {@code /asset-growth/view} 가 요약을 함께 그리고, 그 라우트는 어디에서도 호출되지 않았다(UnreachableEndpointTest 가
 * 2026-08-23 부터 기록). 죽은 라우트만 고친 수정이 사용자에게 아무 효과가 없던 일이 두 번이라 코드를 남길 이유가 없었다.
 */
class PeriodSummaryFailureTest {

  private StockAssetGrowthHtmxController controller(TradeProfitClient client) {
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage(
        "stock.error.fragment.title",
        org.springframework.context.i18n.LocaleContextHolder.getLocale(),
        "불러오지 못했습니다");
    messages.setUseCodeAsDefaultMessage(true);
    return new StockAssetGrowthHtmxController(
        client, null, null, null, null, null, null, null, messages);
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
          List.of(),
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
}
