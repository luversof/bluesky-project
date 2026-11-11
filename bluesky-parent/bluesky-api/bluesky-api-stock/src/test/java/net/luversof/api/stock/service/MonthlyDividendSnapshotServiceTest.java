package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.luversof.api.stock.domain.MonthlyDividendSnapshot;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.MonthlyDividendSnapshotRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.web.dto.request.MonthlyDividendSnapshotUpsertRequest;
import net.luversof.api.stock.web.dto.response.MonthlyDividendSnapshotResponse;

@ExtendWith(MockitoExtension.class)
class MonthlyDividendSnapshotServiceTest {

  @Mock private MonthlyDividendSnapshotRepository monthlyDividendSnapshotRepository;

  @Mock private StockItemRepository stockItemRepository;

  @Mock private StockPriceService stockPriceService;

  @InjectMocks private MonthlyDividendSnapshotService monthlyDividendSnapshotService;

  @Test
  void upsertNewSnapshotDoesNotPreassignIdBeforeSave() {
    UUID userId = UUID.randomUUID();
    StockItem stockItem = createStockItem("O");
    UUID generatedId = UUID.randomUUID();

    MonthlyDividendSnapshotUpsertRequest request = new MonthlyDividendSnapshotUpsertRequest();
    request.setUserId(userId);
    request.setSymbol("O");
    request.setAsOfDate(LocalDate.of(2026, 3, 31));
    request.setLatestMonthlyDividendPerShare(new BigDecimal("1.20"));
    request.setAverageMonthlyDividendPerShare1y(new BigDecimal("1.00"));
    request.setAverageTaxableBaseRatio1y(new BigDecimal("15.00"));
    request.setHeldQuantity(10);
    request.setAverageBuyPrice(new BigDecimal("42.50"));

    when(stockItemRepository.findBySymbol("O")).thenReturn(stockItem);
    when(monthlyDividendSnapshotRepository.findByUserIdAndStockItemId(userId, stockItem.getId()))
        .thenReturn(Optional.empty());
    when(monthlyDividendSnapshotRepository.save(any(MonthlyDividendSnapshot.class)))
        .thenAnswer(
            invocation -> {
              MonthlyDividendSnapshot snapshot = invocation.getArgument(0);
              assertThat(snapshot.getId()).isNull();
              assertThat(snapshot.getCreatedDate()).isNotNull();
              snapshot.setId(generatedId);
              return snapshot;
            });
    when(stockPriceService.getCurrentPrice(stockItem.getId())).thenReturn(new BigDecimal("50.00"));

    MonthlyDividendSnapshotResponse response = monthlyDividendSnapshotService.upsert(request);

    assertThat(response.id()).isEqualTo(generatedId);
    assertThat(response.stockItemSymbol()).isEqualTo("O");
    assertThat(response.currentPrice()).isEqualByComparingTo("50.00");
  }

  /**
   * 수량이 0 인 스냅샷에도 현재가 기준 수익률이 나온다 &mdash; 주당 배당 &divide; 현재가는 <b>종목 속성</b>이라 수량이 필요 없다.
   *
   * <p>예전 구현은 예상 배당금(수량 배수) &divide; 평가액(수량 배수)이라 수량 0 이면 0 &divide; 0 으로 무너져 "수익률 0%" 라는 거짓을 냈다.
   * 화면은 보유 종목만 저장하지만 이 서비스는 인증 없이 열려 있어 0 도 들어온다(2026-09-21). 미보유 월배당 ETF 비교는 {@code
   * monthlyDividendCatalog} 가 맡는다.
   */
  @Test
  void 수량이_0_이어도_현재가_기준_수익률은_종목_속성이다() {
    MonthlyDividendSnapshotResponse candidate = upsertWith(0, BigDecimal.ZERO);

    assertThat(candidate.expectedMonthlyYieldPct()).isEqualByComparingTo("0.50");
    assertThat(candidate.expectedAnnualYieldPct()).isEqualByComparingTo("6.00");
    assertThat(candidate.heldQuantity()).isZero();
    assertThat(candidate.expectedMonthlyDividend()).isEqualByComparingTo("0");
    assertThat(candidate.currentMarketValue()).isEqualByComparingTo("0");
    assertThat(candidate.expectedMonthlyYieldOnCostPct()).isEqualByComparingTo("0");
    assertThat(candidate.expectedCombinedReturnPct()).isEqualByComparingTo("0.00");
  }

  /** 수량이 있어도 값은 그대로다 - 예전 식은 수량이 약분되므로 보유 종목 화면은 한 자리도 바뀌지 않는다. */
  @Test
  void 보유_종목의_현재가_기준_수익률은_그대로다() {
    MonthlyDividendSnapshotResponse held = upsertWith(10, new BigDecimal("19000"));

    assertThat(held.expectedMonthlyYieldPct()).isEqualByComparingTo("0.50");
    assertThat(held.expectedAnnualYieldPct()).isEqualByComparingTo("6.00");
    assertThat(held.expectedMonthlyDividend()).isEqualByComparingTo("1000");
    assertThat(held.currentMarketValue()).isEqualByComparingTo("200000");
    assertThat(held.expectedMonthlyYieldOnCostPct()).isEqualByComparingTo("0.53");
  }

  /** 주당 평균 배당 100원 · 현재가 20,000원인 종목을 수량 · 평단만 바꿔 저장한다. */
  private MonthlyDividendSnapshotResponse upsertWith(int heldQuantity, BigDecimal averageBuyPrice) {
    UUID userId = UUID.randomUUID();
    StockItem stockItem = createStockItem("498400");

    MonthlyDividendSnapshotUpsertRequest request = new MonthlyDividendSnapshotUpsertRequest();
    request.setUserId(userId);
    request.setSymbol("498400");
    request.setAsOfDate(LocalDate.of(2026, 9, 17));
    request.setLatestMonthlyDividendPerShare(new BigDecimal("120"));
    request.setAverageMonthlyDividendPerShare1y(new BigDecimal("100"));
    request.setAverageTaxableBaseRatio1y(new BigDecimal("4.06"));
    request.setHeldQuantity(heldQuantity);
    request.setAverageBuyPrice(averageBuyPrice);

    when(stockItemRepository.findBySymbol("498400")).thenReturn(stockItem);
    when(monthlyDividendSnapshotRepository.findByUserIdAndStockItemId(userId, stockItem.getId()))
        .thenReturn(Optional.empty());
    when(monthlyDividendSnapshotRepository.save(any(MonthlyDividendSnapshot.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(stockPriceService.getCurrentPrice(stockItem.getId())).thenReturn(new BigDecimal("20000"));

    return monthlyDividendSnapshotService.upsert(request);
  }

  private StockItem createStockItem(String symbol) {
    StockItem stockItem = new StockItem();
    stockItem.setId(UUID.randomUUID());
    stockItem.setSymbol(symbol);
    stockItem.setName(symbol + " Inc.");
    stockItem.setMarket("NYSE");
    return stockItem;
  }
}
