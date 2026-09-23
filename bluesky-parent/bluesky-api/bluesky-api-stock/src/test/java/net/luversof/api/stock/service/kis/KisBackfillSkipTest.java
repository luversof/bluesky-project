package net.luversof.api.stock.service.kis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import net.luversof.api.stock.domain.MonthlyDividendProfile;
import net.luversof.api.stock.domain.OpenApiConfig;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.MonthlyDividendProfileRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;
import net.luversof.api.stock.repository.TradeRepository;

/**
 * 시세 갱신 속도(사용자 요청 2026-09-23: "종목별 갱신이 너무 느리다").
 *
 * <p>결정: 빈 재조회 없애기. 호출 간격은 0.1 초로 줄였다가 KIS 초당 한도에 걸려 0.2 초로 되돌렸다.
 *
 * <p>실측: 전체 갱신 16.4 초 중 응답 대기는 0.9 초, 호출 뒤 쉰 시간이 15.0 초였고, 호출 75 회 중 52 회가 상장 전이라 시세가 없는 구간을 매번 다시
 * 부른 것이었다(월배당 종목은 2 년 전부터 받는데 상장이 늦은 ETF 는 그 앞이 원래 비었다).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KisBackfillSkipTest {

  @Mock private DividendRepository dividendRepository;

  @Mock private TradeRepository tradeRepository;

  @Mock private MonthlyDividendProfileRepository monthlyDividendProfileRepository;

  @Mock private StockItemRepository stockItemRepository;

  @Mock private StockPriceHistoryRepository stockPriceHistoryRepository;

  @Mock private KisAuthService kisAuthService;

  @Mock private RestTemplate kisRestTemplate;

  @InjectMocks private KisStockPriceUpdateService service;

  private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

  /** 저장된 첫 시세일 - 상장일이라 그 앞은 KIS 에도 없다. */
  private static final LocalDate LISTED = TODAY.minusDays(80);

  private final List<String> calledUrls = new ArrayList<>();

  private UUID prepareListedEtf(boolean kisFails) {
    StockItem item = new StockItem();
    item.setId(UUID.randomUUID());
    item.setSymbol("0219E0");
    item.setName("KODEX 200커버드콜액티브");
    item.setMarket("KRX");
    MonthlyDividendProfile profile = new MonthlyDividendProfile();
    profile.setStockItemId(item.getId());

    when(dividendRepository.findDividendDateRanges()).thenReturn(List.of());
    when(tradeRepository.findTradeDateRanges()).thenReturn(List.of());
    when(tradeRepository.findCurrentlyHeldStockItemIds()).thenReturn(List.of());
    when(stockPriceHistoryRepository.findRefreshTargetTradeDates()).thenReturn(List.of());
    when(monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc())
        .thenReturn(List.of(profile));
    when(stockItemRepository.findAllById(any())).thenReturn(List.of(item));
    StockPriceHistory first = new StockPriceHistory();
    first.setTradeDate(LISTED);
    StockPriceHistory last = new StockPriceHistory();
    last.setTradeDate(TODAY);
    // 받아 봐도 한 행도 안 는다 - 첫날은 늘 상장일 그대로다.
    when(stockPriceHistoryRepository.findTopByStockItemIdOrderByTradeDateAsc(any()))
        .thenReturn(Optional.of(first));
    when(stockPriceHistoryRepository.findTopByStockItemIdOrderByTradeDateDesc(any()))
        .thenReturn(Optional.of(last));
    OpenApiConfig config = new OpenApiConfig();
    config.setAccessToken("t");
    config.setAppKey("k");
    config.setAppSecret("s");
    when(kisAuthService.getValidConfig(any())).thenReturn(config);
    when(kisRestTemplate.exchange(
            any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
        .thenAnswer(
            invocation -> {
              calledUrls.add(invocation.getArgument(0));
              if (kisFails) {
                throw new RestClientException("잠시 끊김");
              }
              return ResponseEntity.ok().build();
            });
    return item.getId();
  }

  @Test
  void 상장_전_빈_구간은_한_번_확인하면_다시_부르지_않는다() {
    prepareListedEtf(false);

    service.updatePriceHistory(UUID.randomUUID());
    int firstRun = calledUrls.size();
    calledUrls.clear();
    service.updatePriceHistory(UUID.randomUUID());

    assertThat(firstRun).as("처음에는 2 년 전 ~ 상장 전을 100 일씩 확인한다").isGreaterThanOrEqualTo(6);
    assertThat(calledUrls).as("두 번째부터는 같은 빈 구간을 다시 부르지 않는다").isEmpty();
  }

  @Test
  void 호출이_실패했으면_없다고_굳히지_않는다() {
    prepareListedEtf(true);

    service.updatePriceHistory(UUID.randomUUID());
    int firstRun = calledUrls.size();
    calledUrls.clear();
    service.updatePriceHistory(UUID.randomUUID());

    assertThat(calledUrls).as("일시 오류를 '상장 전이라 없다' 로 적으면 그 구간은 다시는 안 받는다").hasSize(firstRun);
  }

  private static org.springframework.web.client.HttpServerErrorException kisError(String msgCd) {
    return org.springframework.web.client.HttpServerErrorException.create(
        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal Server Error",
        org.springframework.http.HttpHeaders.EMPTY,
        ("{\"rt_cd\":\"1\",\"msg_cd\":\"" + msgCd + "\"}")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8),
        java.nio.charset.StandardCharsets.UTF_8);
  }

  /** 초당 한도 거절은 쉬었다 다시 부른다 - 실측 2026-09-23: 0.2 초 간격에서도 가끔 거절돼 종목이 실패로 남았다. */
  @Test
  void 초당_한도_거절은_다시_불러_실패로_남기지_않는다() {
    java.util.concurrent.atomic.AtomicInteger calls =
        new java.util.concurrent.atomic.AtomicInteger();
    when(kisRestTemplate.exchange(
            any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
        .thenAnswer(
            invocation -> {
              if (calls.incrementAndGet() == 1) {
                throw kisError("EGW00201");
              }
              return ResponseEntity.ok().build();
            });

    var response = service.exchangeWithRateLimitRetry("http://kis/x", HttpEntity.EMPTY);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(calls.get()).as("한 번 거절 뒤 다시 불렀다").isEqualTo(2);
  }

  @Test
  void 갱신_중_한도_거절이_와도_종목은_실패가_아니다() {
    prepareListedEtf(false);
    java.util.concurrent.atomic.AtomicInteger calls =
        new java.util.concurrent.atomic.AtomicInteger();
    when(kisRestTemplate.exchange(
            any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
        .thenAnswer(
            invocation -> {
              if (calls.incrementAndGet() == 1) {
                throw kisError("EGW00201");
              }
              return ResponseEntity.ok().build();
            });

    var result = service.updatePriceHistory(UUID.randomUUID());

    assertThat(result.failedSymbols()).as("잠깐 뒤 다시 부르면 되는 거절이다 - 실패로 세면 매번 다시 갱신해야 한다").isEmpty();
  }

  @Test
  void 다른_오류는_다시_부르지_않는다() {
    java.util.concurrent.atomic.AtomicInteger calls =
        new java.util.concurrent.atomic.AtomicInteger();
    when(kisRestTemplate.exchange(
            any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
        .thenAnswer(
            invocation -> {
              calls.incrementAndGet();
              throw kisError("EGW00123");
            });

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.exchangeWithRateLimitRetry("http://kis/x", HttpEntity.EMPTY))
        .isInstanceOf(org.springframework.web.client.HttpServerErrorException.class);
    assertThat(calls.get()).as("한도 거절이 아니면 기다려도 소용없다").isEqualTo(1);
    assertThat(KisStockPriceUpdateService.KIS_RATE_LIMIT_RETRIES)
        .as("끝없이 다시 부르면 갱신이 멈춘다")
        .isEqualTo(2);
  }

  @Test
  void 저장된_첫날이_바뀌면_다시_확인한다() {
    assertThat(KisStockPriceUpdateService.shouldBackfill(LISTED.minusDays(300), LISTED, null))
        .isTrue();
    assertThat(KisStockPriceUpdateService.shouldBackfill(LISTED.minusDays(300), LISTED, LISTED))
        .isFalse();
    assertThat(
            KisStockPriceUpdateService.shouldBackfill(
                LISTED.minusDays(300), LISTED, LISTED.plusDays(5)))
        .as("그 사이 첫날이 바뀌었다(행 삭제 등) - 예전 확인은 더는 맞지 않는다")
        .isTrue();
    assertThat(KisStockPriceUpdateService.shouldBackfill(LISTED, LISTED, null))
        .as("원하는 시작이 첫날보다 이르지 않으면 받을 것이 없다")
        .isFalse();
  }

  @Test
  void 호출_간격은_초당_한도에_안_걸리는_0점2초다() throws java.io.IOException {
    // 실측 2026-09-23: 0.1 초는 KIS "초당 거래건수 초과" 로 54 종목 중 4 · 2 종목 실패, 0.2 초는 실패 0.
    // 더 줄이려면 한도 초과 응답을 쉬었다 다시 부르는 처리부터 있어야 한다.
    assertThat(KisStockPriceUpdateService.KIS_CALL_INTERVAL_MS).isEqualTo(200L);
    String source =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "src/main/java/net/luversof/api/stock/service/kis/KisStockPriceUpdateService.java"));
    assertThat(source)
        .as("상수만 바꾸고 쉬는 곳은 옛 숫자를 쓰면 아무것도 안 빨라진다")
        .contains("Thread.sleep(KIS_CALL_INTERVAL_MS);")
        .doesNotContainPattern("Thread[.]sleep[(][0-9]+[)]");
  }
}
