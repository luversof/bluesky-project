package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequestGroup;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

class StockBaseHtmxControllerTest {

  @Test
  void getEnrichedTradeProfitsUsesOverrideGroupByWithoutMutatingOriginalRequest() {
    UUID stockItemId = UUID.randomUUID();
    CapturingTradeProfitClient tradeProfitClient = new CapturingTradeProfitClient(stockItemId);
    StockBaseHtmxController controller =
        new TestStockBaseHtmxController(
            tradeProfitClient,
            new StubTradeClient(),
            new StubAccountClient(),
            new StubStockItemClient(stockItemId),
            new StubDividendClient(),
            new StaticMessageSource());

    TradeProfitRequest request = new TradeProfitRequest();
    request.setUserId(UUID.randomUUID());
    request.setGroupBy(TradeProfitRequestGroup.ACCOUNT_AND_STOCKITEM);
    request.setTimeZone("Asia/Seoul");

    List<TradeProfit> tradeProfits =
        controller.getEnrichedTradeProfits(request, TradeProfitRequestGroup.STOCKITEM);

    assertThat(request.getGroupBy()).isEqualTo(TradeProfitRequestGroup.ACCOUNT_AND_STOCKITEM);
    assertThat(tradeProfitClient.lastRequest.getFirst("groupBy")).isEqualTo("STOCKITEM");
    assertThat(tradeProfitClient.lastRequest.getFirst("userId"))
        .isEqualTo(request.getUserId().toString());
    assertThat(tradeProfits)
        .singleElement()
        .satisfies(
            tradeProfit -> {
              assertThat(tradeProfit.stockItemName()).isEqualTo("TIGER 리츠부동산인프라");
              assertThat(tradeProfit.accountName()).isNull();
              assertThat(tradeProfit.averageBuyPrice()).isEqualByComparingTo("4383.03");
            });
  }

  private static final class TestStockBaseHtmxController extends StockBaseHtmxController {

    private TestStockBaseHtmxController(
        TradeProfitClient tradeProfitClient,
        TradeClient tradeClient,
        AccountClient accountClient,
        StockItemClient stockItemClient,
        DividendClient dividendClient,
        StaticMessageSource messageSource) {
      super(
          tradeProfitClient,
          tradeClient,
          accountClient,
          stockItemClient,
          dividendClient,
          messageSource);
    }
  }

  private static final class CapturingTradeProfitClient implements TradeProfitClient {

    private final UUID stockItemId;
    private MultiValueMap<String, String> lastRequest = new LinkedMultiValueMap<>();

    private CapturingTradeProfitClient(UUID stockItemId) {
      this.stockItemId = stockItemId;
    }

    @Override
    public List<TradeProfit> calculateProfit(MultiValueMap<String, String> request) {
      lastRequest = copyRequest(request);
      return List.of(
          new TradeProfit(
              stockItemId,
              null,
              null,
              null,
              new BigDecimal("59796963"),
              new BigDecimal("4383.03"),
              8437,
              new BigDecimal("4521.10"),
              new BigDecimal("38148177"),
              new BigDecimal("1023456"),
              13686,
              new BigDecimal("4300"),
              new BigDecimal("58849800"),
              new BigDecimal("-947163"),
              new BigDecimal("763293"),
              new BigDecimal("12345"),
              new BigDecimal("6789"),
              new BigDecimal("4567"),
              new BigDecimal("59820000"),
              new BigDecimal("38136821"),
              new BigDecimal("4383.51"),
              new BigDecimal("4519.75"),
              new BigDecimal("1004321"),
              new BigDecimal("-970200"),
              new BigDecimal("34121")));
    }

    @Override
    public List<TradeProfitTimeSeriesPoint> timeSeries(MultiValueMap<String, String> request) {
      return List.of();
    }

    @Override
    public List<HoldingsSnapshotItem> holdingsSnapshot(MultiValueMap<String, String> params) {
      return List.of();
    }

    @Override
    public java.util.Map<String, List<HoldingsSnapshotItem>> holdingsSnapshotBatch(
        MultiValueMap<String, String> params) {
      return java.util.Map.of();
    }

    private MultiValueMap<String, String> copyRequest(MultiValueMap<String, String> request) {
      LinkedMultiValueMap<String, String> copy = new LinkedMultiValueMap<>();
      request.forEach((key, value) -> copy.put(key, new ArrayList<>(value)));
      return copy;
    }
  }

  private static final class StubStockItemClient implements StockItemClient {

    private final UUID stockItemId;

    private StubStockItemClient(UUID stockItemId) {
      this.stockItemId = stockItemId;
    }

    @Override
    public StockItem createStockItem(StockItem stockItem) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<StockItem> getStockItemById(UUID id) {
      return Optional.empty();
    }

    @Override
    public StockItem findByName(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<StockItem> getStockItems() {
      return List.of(new StockItem(stockItemId, "329200", "TIGER 리츠부동산인프라", "KOSPI", List.of()));
    }

    @Override
    public List<StockItem> getStockItemsByTag(String tag) {
      return List.of();
    }
  }

  private static final class StubAccountClient implements AccountClient {

    @Override
    public Account createAccount(Account account) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Account> getAccountById(UUID id) {
      return Optional.empty();
    }

    @Override
    public List<Account> getAccountsByUserId(UUID userId) {
      return List.of();
    }
  }

  private static final class StubTradeClient implements TradeClient {

    @Override
    public List<TradeResponse> findTrades(MultiValueMap<String, String> request) {
      return List.of();
    }
  }

  private static final class StubDividendClient implements DividendClient {

    @Override
    public List<DividendResponse> findDividends(MultiValueMap<String, String> request) {
      return List.of();
    }

    @Override
    public net.luversof.web.gate.stock.dto.response.DividendMetaResponse findDividendMeta(
        java.util.UUID userId) {
      return new net.luversof.web.gate.stock.dto.response.DividendMetaResponse(null, List.of());
    }
  }
}
