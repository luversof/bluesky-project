package net.luversof.api.stock.service.kis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import net.luversof.api.stock.service.kis.KisStockPriceUpdateService.ProgressListener;
import net.luversof.api.stock.web.dto.response.PriceHistoryUpdateResult;

/**
 * 갱신 진행 상황의 <b>분모</b>가 처음부터 맞는가.
 *
 * <p>화면은 "53 종목 중 12 종목" 처럼 적는다. 분모를 루프를 돌며 세면 시작 직후에 "0 종목 중 0 종목" 이 나가고, 미리 세는 판정이 루프 안 판정과 다르면
 * 끝까지 가도 "51 / 53" 에서 멈춘 것처럼 보인다. 둘 다 같은 {@code fetchable} 을 쓰게 하고 여기서 못 박는다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PriceHistoryProgressTest {

  @Mock private DividendRepository dividendRepository;

  @Mock private TradeRepository tradeRepository;

  @Mock private MonthlyDividendProfileRepository monthlyDividendProfileRepository;

  @Mock private StockItemRepository stockItemRepository;

  @Mock private StockPriceHistoryRepository stockPriceHistoryRepository;

  @Mock private KisAuthService kisAuthService;

  @Mock private RestTemplate kisRestTemplate;

  @InjectMocks private KisStockPriceUpdateService kisStockPriceUpdateService;

  private static StockItem stockItem(String symbol, String market) {
    StockItem item = new StockItem();
    item.setId(UUID.randomUUID());
    item.setSymbol(symbol);
    item.setName("이름 " + symbol);
    item.setMarket(market);
    return item;
  }

  @Test
  void 분모는_시세를_받을_수_있는_종목만_센다() {
    // 국내 둘 + 해외 하나. 해외는 KIS 로 못 받으니 분모에도 들어가면 안 된다.
    StockItem krx = stockItem("476800", "KRX");
    StockItem kosdaq = stockItem("0094M0", "KOSDAQ");
    StockItem nasdaq = stockItem("SCHD", "NASDAQ");
    List<StockItem> items = List.of(krx, kosdaq, nasdaq);

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
    // 설정을 못 받으면 종목마다 조회가 실패한다 - 진행 알림은 그래도 나와야 한다.
    net.luversof.api.stock.domain.OpenApiConfig config =
        new net.luversof.api.stock.domain.OpenApiConfig();
    config.setAccessToken("t");
    config.setAppKey("k");
    config.setAppSecret("s");
    when(kisAuthService.getValidConfig(any())).thenReturn(config);
    // 조회는 실패해도 좋다 - 진행 알림은 성공 여부와 상관없이 나와야 한다.
    when(kisRestTemplate.exchange(
            any(String.class),
            any(org.springframework.http.HttpMethod.class),
            any(org.springframework.http.HttpEntity.class),
            any(Class.class)))
        .thenThrow(new org.springframework.web.client.RestClientException("no network"));

    List<int[]> progress = new ArrayList<>();
    ProgressListener listener = (done, total) -> progress.add(new int[] {done, total});

    PriceHistoryUpdateResult result =
        kisStockPriceUpdateService.updatePriceHistory(UUID.randomUUID(), listener);

    assertThat(progress).as("알림이 하나도 없으면 화면이 진행을 못 그린다").isNotEmpty();
    assertThat(progress.get(0))
        .as("첫 알림부터 분모가 맞아야 한다 - 0 이면 화면에 '0 중 0' 이 나간다")
        .containsExactly(0, 2);
    assertThat(progress.get(progress.size() - 1)).as("끝나면 분자와 분모가 같아야 한다").containsExactly(2, 2);
    assertThat(result.targetSymbolCount()).as("미리 센 분모와 실제로 시도한 수가 같아야 한다").isEqualTo(2);
  }
}
