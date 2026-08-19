package net.luversof.api.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.service.AccountService;
import net.luversof.api.stock.service.DividendService;
import net.luversof.api.stock.service.StockItemService;
import net.luversof.api.stock.service.StockPriceService;
import net.luversof.api.stock.service.TradeProfitService;
import net.luversof.api.stock.service.TradeService;
import net.luversof.api.stock.service.strategy.AverageCostProfitCalculator;
import net.luversof.api.stock.service.strategy.ProfitCalculator;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.request.TradeProfitRequestGroup;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;

/**
 * Reproduction test for Realized Profit calculation issue. Scenario: Buy in 2025, Sell in 2026.
 * Calculate Profit for 2026.
 */
@ExtendWith(MockitoExtension.class)
public class TradeProfitServiceReproductionTest {

  @Mock private AccountService accountService;
  @Mock private TradeService tradeService;
  @Mock private StockPriceService stockPriceService;
  @Spy private ProfitCalculator profitCalculator = new AverageCostProfitCalculator();
  @Mock private StockItemService stockItemService;
  @Mock private DividendService dividendService;

  @InjectMocks private TradeProfitService tradeProfitService;

  @BeforeEach
  void setUp() {
    when(stockItemService.findAllById(any())).thenReturn(Collections.emptyList());
  }

  @Test
  void testRealizedProfitWithPriorBuy() {
    UUID stockItemId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();

    // 1. Setup Trades
    List<Trade> tradeList = new ArrayList<>();

    // BUY: 2025-01-01. 7000 shares @ 5000 KRW.
    Trade buy = new Trade();
    setField(buy, "stockItemId", stockItemId);
    setField(buy, "accountId", accountId);
    setField(buy, "type", TradeType.BUY);
    setField(buy, "quantity", 7000);
    setField(buy, "price", new BigDecimal("5000"));
    setField(buy, "fee", BigDecimal.ZERO);
    setField(buy, "tax", BigDecimal.ZERO);
    setField(
        buy,
        "tradeDate",
        LocalDate.of(2025, 1, 1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant());
    tradeList.add(buy);

    // SELL: 2026-01-01. 7000 shares @ 5400 KRW.
    Trade sell = new Trade();
    setField(sell, "stockItemId", stockItemId);
    setField(sell, "accountId", accountId);
    setField(sell, "type", TradeType.SELL);
    setField(sell, "quantity", 7000);
    setField(sell, "price", new BigDecimal("5400"));
    setField(sell, "fee", new BigDecimal("751")); // Set fee to match the user's "missing" amount
    setField(sell, "tax", BigDecimal.ZERO);
    setField(
        sell,
        "tradeDate",
        LocalDate.of(2026, 1, 1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant());

    // Set Realized Profit explicitly as logic now relies on DB value
    sell.setRealizedProfit(new BigDecimal("2800000"));

    tradeList.add(sell);

    // 2. Setup Request
    // Request for 2026 only
    Instant start = LocalDate.of(2026, 1, 1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
    Instant end = LocalDate.of(2026, 12, 31).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();

    TradeProfitRequest request = new TradeProfitRequest();
    request.setStartDate(start);
    request.setEndDate(end);
    request.setGroupBy(TradeProfitRequestGroup.STOCKITEM);

    // 3. Execution
    var profits = tradeProfitService.calculateProfitByStock(tradeList, request);

    assertThat(profits).hasSize(1);
    var p = profits.get(0);

    // 4. Assertions
    // Sell Amount = 7000 * 5400 = 37,800,000
    BigDecimal sellAmount = new BigDecimal("37800000");
    assertThat(p.getTotalSellAmount()).isEqualByComparingTo(sellAmount);

    // Cost = 7000 * 5000 = 35,000,000
    // Gross Realized Profit = 37,800,000 - 35,000,000 = 2,800,000
    BigDecimal expectedGrossProfit = new BigDecimal("2800000");
    assertThat(p.getRealizedProfit()).isEqualByComparingTo(expectedGrossProfit);

    // Net Realized Profit = (SellAmount - Fee) - Cost = 2,800,000 - 751 = 2,799,249
    BigDecimal expectedNetProfit = expectedGrossProfit.subtract(new BigDecimal("751"));
    assertThat(p.getRealizedProfitNet()).isEqualByComparingTo(expectedNetProfit);

    System.out.println("Realized Profit Net: " + p.getRealizedProfitNet());
  }

  @Test
  void aggregateTimeSeriesKeepsRawQuantityForNormalPriceMoves() {
    UUID userId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();
    UUID stockItemId = UUID.randomUUID();

    stubAggregateTimeSeriesDefaults();
    when(accountService.findByIdIn(List.of(accountId))).thenReturn(List.of(new Account()));
    when(tradeService.findByAccountIdIn(List.of(accountId)))
        .thenReturn(
            new ArrayList<>(
                List.of(
                    createTrade(stockItemId, accountId, TradeType.BUY, 10, "100", "2026-05-14"))));
    when(stockPriceService.getPriceHistory(any(), any(), any()))
        .thenReturn(
            List.of(
                createPriceHistory(stockItemId, LocalDate.of(2026, 5, 14), "98"),
                createPriceHistory(stockItemId, LocalDate.of(2026, 5, 15), "99")));

    TradeProfitRequest request =
        createUserAccountRequest(userId, accountId, "2026-05-14", "2026-05-16");

    List<TradeProfitTimeSeriesPoint> series =
        tradeProfitService.aggregateTimeSeries(request, "DAILY");

    assertThat(series).hasSize(2);
    assertThat(series.get(0).totalHoldingsValue()).isEqualByComparingTo("980");
    assertThat(series.get(1).totalHoldingsValue()).isEqualByComparingTo("990");
  }

  @Test
  void aggregateTimeSeriesAdjustsQuantityForLikelyStockSplit() {
    UUID userId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();
    UUID stockItemId = UUID.randomUUID();

    stubAggregateTimeSeriesDefaults();
    when(accountService.findByIdIn(List.of(accountId))).thenReturn(List.of(new Account()));
    when(tradeService.findByAccountIdIn(List.of(accountId)))
        .thenReturn(
            new ArrayList<>(
                List.of(
                    createTrade(stockItemId, accountId, TradeType.BUY, 10, "100", "2026-05-14"))));
    when(stockPriceService.getPriceHistory(any(), any(), any()))
        .thenReturn(
            List.of(
                createPriceHistory(stockItemId, LocalDate.of(2026, 5, 14), "50"),
                createPriceHistory(stockItemId, LocalDate.of(2026, 5, 15), "60")));

    TradeProfitRequest request =
        createUserAccountRequest(userId, accountId, "2026-05-14", "2026-05-16");

    List<TradeProfitTimeSeriesPoint> series =
        tradeProfitService.aggregateTimeSeries(request, "DAILY");

    assertThat(series).hasSize(2);
    assertThat(series.get(0).totalHoldingsValue()).isEqualByComparingTo("1000");
    assertThat(series.get(1).totalHoldingsValue()).isEqualByComparingTo("1200");
  }

  private void stubAggregateTimeSeriesDefaults() {
    when(dividendService.findDividends(any())).thenReturn(Collections.emptyList());
  }

  private TradeProfitRequest createUserAccountRequest(
      UUID userId, UUID accountId, String startDate, String endDateExclusive) {
    TradeProfitRequest request = new TradeProfitRequest();
    request.setUserId(userId);
    request.setAccountIdList(List.of(accountId));
    request.setStartDate(
        LocalDate.parse(startDate).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant());
    request.setEndDate(
        LocalDate.parse(endDateExclusive).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant());
    request.setGroupBy(TradeProfitRequestGroup.STOCKITEM);
    return request;
  }

  private Trade createTrade(
      UUID stockItemId,
      UUID accountId,
      TradeType type,
      int quantity,
      String price,
      String tradeDate) {
    Trade trade = new Trade();
    setField(trade, "stockItemId", stockItemId);
    setField(trade, "accountId", accountId);
    setField(trade, "type", type);
    setField(trade, "quantity", quantity);
    setField(trade, "price", new BigDecimal(price));
    setField(trade, "fee", BigDecimal.ZERO);
    setField(trade, "tax", BigDecimal.ZERO);
    setField(
        trade,
        "tradeDate",
        LocalDate.parse(tradeDate).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant());
    return trade;
  }

  private StockPriceHistory createPriceHistory(
      UUID stockItemId, LocalDate tradeDate, String closePrice) {
    StockPriceHistory history = new StockPriceHistory();
    history.setStockItemId(stockItemId);
    history.setTradeDate(tradeDate);
    history.setClosePrice(new BigDecimal(closePrice));
    return history;
  }

  private void setField(Object obj, String fieldName, Object value) {
    try {
      var field = obj.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(obj, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
