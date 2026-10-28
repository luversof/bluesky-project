package net.luversof.api.stock.service.kis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestTemplate;

import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.MonthlyDividendProfileRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.service.kis.dto.KisDailyPriceItem;
import net.luversof.api.stock.service.kis.dto.KisDailyPriceResponse;
import net.luversof.api.stock.web.dto.response.PriceHistoryUpdateResult;

/**
 * 시세 갱신이 실패를 성공으로 세지 않고, 실패를 키우지도 않는다(2026-10-27 감사).
 *
 * <p>감사 전: 인증을 블록마다 다시 받아 발급이 한 번 실패하면 발급 요청이 수십 번 나갔고(KIS 발급은 1 분 1 회), HTTP 200 + {@code rt_cd}
 * 업무 오류는 빈 목록(휴장)으로 받아 성공으로 셌다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KisRunFailureHandlingTest {

  @Mock private DividendRepository dividendRepository;

  @Mock private TradeRepository tradeRepository;

  @Mock private MonthlyDividendProfileRepository monthlyDividendProfileRepository;

  @Mock private StockItemRepository stockItemRepository;

  @Mock private StockPriceHistoryRepository stockPriceHistoryRepository;

  @Mock private KisAuthService kisAuthService;

  @Mock private RestTemplate kisRestTemplate;

  @InjectMocks private KisStockPriceUpdateService kisStockPriceUpdateService;

  private static StockItem stockItem(String symbol) {
    StockItem item = new StockItem();
    item.setId(UUID.randomUUID());
    item.setSymbol(symbol);
    item.setName("이름 " + symbol);
    item.setMarket("KRX");
    return item;
  }

  @Test
  void 인증을_못_받으면_한_번만_묻고_모든_종목을_실패로_센다() {
    List<StockItem> items = List.of(stockItem("476800"), stockItem("0094M0"));
    when(dividendRepository.findDividendDateRanges()).thenReturn(List.of());
    when(tradeRepository.findTradeDateRanges()).thenReturn(List.of());
    when(tradeRepository.findCurrentlyHeldStockItemIds()).thenReturn(List.of());
    when(stockPriceHistoryRepository.findRefreshTargetTradeDates()).thenReturn(List.of());
    when(monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc())
        .thenReturn(List.of());
    when(stockItemRepository.findAllById(any())).thenReturn(items);
    when(stockPriceHistoryRepository.findTopByStockItemIdOrderByTradeDateAsc(any()))
        .thenReturn(Optional.empty());
    when(stockPriceHistoryRepository.findTopByStockItemIdOrderByTradeDateDesc(any()))
        .thenReturn(Optional.empty());
    when(kisAuthService.getValidConfig(any())).thenThrow(new IllegalStateException("tokenP 403"));

    PriceHistoryUpdateResult result =
        kisStockPriceUpdateService.updatePriceHistory(UUID.randomUUID());

    verify(kisAuthService, times(1)).getValidConfig(any());
    assertThat(result.failedSymbols()).containsExactly("476800", "0094M0");
  }

  @Test
  void 업무_오류_응답은_빈_목록이_아니라_실패다() {
    KisDailyPriceResponse error = new KisDailyPriceResponse();
    error.setRt_cd("1");
    error.setMsg_cd("EGW00123");
    error.setMsg1("기간이 만료된 token 입니다.");

    assertThatThrownBy(() -> KisStockPriceUpdateService.itemsOf(error))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("EGW00123");
  }

  @Test
  void 정상_응답은_시세를_그대로_돌려준다() {
    KisDailyPriceItem day = new KisDailyPriceItem();
    day.setStck_bsop_date("20261023");
    day.setStck_clpr("10000");
    KisDailyPriceResponse ok = new KisDailyPriceResponse();
    ok.setRt_cd("0");
    ok.setOutput2(List.of(day));

    assertThat(KisStockPriceUpdateService.itemsOf(ok)).containsExactly(day);
    KisDailyPriceResponse holiday = new KisDailyPriceResponse();
    holiday.setRt_cd("0");
    assertThat(KisStockPriceUpdateService.itemsOf(holiday)).as("휴장 - 시세 없음").isEmpty();
    assertThat(KisStockPriceUpdateService.itemsOf(null)).isEmpty();
  }

  /** 기존 종가가 0 이면 견줄 수 없다 - 예전에는 0 으로 나누다 그 블록이 매번 저장되지 않았다. */
  @Test
  void 수정주가_재조정_판정은_기존_종가가_0_이어도_터지지_않는다() {
    assertThat(
            KisStockPriceUpdateService.isPriceAdjustment(BigDecimal.ZERO, new BigDecimal("1000")))
        .isFalse();
    assertThat(KisStockPriceUpdateService.isPriceAdjustment(null, new BigDecimal("1000")))
        .isFalse();
    assertThat(
            KisStockPriceUpdateService.isPriceAdjustment(
                new BigDecimal("1000"), new BigDecimal("500")))
        .as("1:2 분할")
        .isTrue();
    assertThat(
            KisStockPriceUpdateService.isPriceAdjustment(
                new BigDecimal("1000"), new BigDecimal("1020")))
        .as("딱 2% 는 아니다")
        .isFalse();
  }
}
