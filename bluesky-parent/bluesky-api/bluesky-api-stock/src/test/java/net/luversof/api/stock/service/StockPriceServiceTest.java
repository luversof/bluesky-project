package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.repository.StockDailyClosePriceQuery;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;

/**
 * 시세 조회 서비스. 화면의 "현재가"·"평가 기준일"과 자산 성장 시뮬레이션이 모두 여기를 지난다.
 *
 * <p>테스트가 없었다(실측 2026-09-13: api-stock 의 service/util/support 에서 테스트가 한 번도 부르지 않는 public 메서드 19 개 중
 * 넷이 이 클래스에 있다).
 *
 * <p>고정하는 것은 셋이다.
 *
 * <ol>
 *   <li><b>거래량 0 행은 그 날의 종가가 아니다</b> - 무거래일에는 직전 종가가 그대로 실려 오므로, 그 행을 쓰면 평가 기준일만 앞당겨진다. 다만 모든 행이
 *       거래량 0 인 종목은 값이 사라지면 안 되므로 폴백한다.
 *   <li>{@code getPriceAt} 은 기준일 <b>이하</b>의 마지막 거래일을 쓰고, 없으면 현재가로 떨어진다.
 *   <li>일별 종가 묶음 조회는 세 CSV({@code ids} · {@code froms} · {@code tos})와 {@code idOrder} 가 <b>같은 순서로
 *       정렬</b>돼 있어야 한다 - 하나만 어긋나면 <b>남의 종목 가격</b>이 붙는데 화면에는 그럴듯한 숫자로 나온다.
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class StockPriceServiceTest {

  @Mock private StockPriceHistoryRepository stockPriceHistoryRepository;

  @Mock private StockDailyClosePriceQuery stockDailyClosePriceQuery;

  @InjectMocks private StockPriceService stockPriceService;

  private static final UUID ITEM_A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID ITEM_B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
  private static final UUID ITEM_C = UUID.fromString("00000000-0000-0000-0000-0000000000c3");

  private StockPriceHistory row(String tradeDate, String close, long volume) {
    StockPriceHistory history = new StockPriceHistory();
    history.setStockItemId(ITEM_A);
    history.setTradeDate(LocalDate.parse(tradeDate));
    history.setClosePrice(new BigDecimal(close));
    history.setVolume(volume);
    return history;
  }

  /** 무거래일 행(거래량 0)에는 직전 종가가 실려 온다 - 그 행을 현재가로 삼으면 기준일이 앞당겨진다. */
  @Test
  void 현재가는_거래가_있던_날을_먼저_본다() {
    when(stockPriceHistoryRepository.findTopByStockItemIdAndVolumeGreaterThanOrderByTradeDateDesc(
            ITEM_A, 0L))
        .thenReturn(Optional.of(row("2026-09-09", "269500", 1200L)));

    assertThat(stockPriceService.getCurrentPriceHistory(ITEM_A))
        .get()
        .extracting(StockPriceHistory::getTradeDate)
        .isEqualTo(LocalDate.parse("2026-09-09"));
    verify(stockPriceHistoryRepository, never()).findTopByStockItemIdOrderByTradeDateDesc(any());
  }

  /** 모든 행이 거래량 0 인 종목(수집이 무거래일에만 돌았던 경우)까지 값을 없애면 안 된다. */
  @Test
  void 거래가_있던_날이_없으면_그래도_마지막_행을_쓴다() {
    when(stockPriceHistoryRepository.findTopByStockItemIdAndVolumeGreaterThanOrderByTradeDateDesc(
            ITEM_A, 0L))
        .thenReturn(Optional.empty());
    when(stockPriceHistoryRepository.findTopByStockItemIdOrderByTradeDateDesc(ITEM_A))
        .thenReturn(Optional.of(row("2026-09-05", "250000", 0L)));

    assertThat(stockPriceService.getCurrentPrice(ITEM_A)).isEqualByComparingTo("250000");
  }

  @Test
  void 이력이_아예_없으면_0원이다() {
    when(stockPriceHistoryRepository.findTopByStockItemIdAndVolumeGreaterThanOrderByTradeDateDesc(
            ITEM_A, 0L))
        .thenReturn(Optional.empty());
    when(stockPriceHistoryRepository.findTopByStockItemIdOrderByTradeDateDesc(ITEM_A))
        .thenReturn(Optional.empty());

    assertThat(stockPriceService.getCurrentPrice(ITEM_A)).isEqualByComparingTo("0");
  }

  /** 기준일이 없으면 "지금" 을 묻는 것이다 - 과거 조회로 새지 않는다. */
  @Test
  void 기준일이_없으면_현재가다() {
    when(stockPriceHistoryRepository.findTopByStockItemIdAndVolumeGreaterThanOrderByTradeDateDesc(
            ITEM_A, 0L))
        .thenReturn(Optional.of(row("2026-09-09", "269500", 1200L)));

    assertThat(stockPriceService.getPriceAt(ITEM_A, null)).isEqualByComparingTo("269500");
    verify(stockPriceHistoryRepository, never())
        .findTopByStockItemIdAndTradeDateLessThanEqualAndVolumeGreaterThanOrderByTradeDateDesc(
            any(), any(), anyLong());
  }

  @Test
  void 기준일이_있으면_그_날_이하의_마지막_거래일을_쓴다() {
    LocalDate at = LocalDate.parse("2026-09-13");
    when(stockPriceHistoryRepository
            .findTopByStockItemIdAndTradeDateLessThanEqualAndVolumeGreaterThanOrderByTradeDateDesc(
                ITEM_A, at, 0L))
        .thenReturn(Optional.of(row("2026-09-09", "269500", 1200L)));

    assertThat(stockPriceService.getPriceAt(ITEM_A, at)).isEqualByComparingTo("269500");
  }

  /** 그 날 이전에 거래가 한 번도 없었으면(상장 전 등) 현재가로 떨어진다. */
  @Test
  void 기준일_이전_이력이_없으면_현재가로_떨어진다() {
    LocalDate at = LocalDate.parse("2019-01-01");
    when(stockPriceHistoryRepository
            .findTopByStockItemIdAndTradeDateLessThanEqualAndVolumeGreaterThanOrderByTradeDateDesc(
                ITEM_A, at, 0L))
        .thenReturn(Optional.empty());
    when(stockPriceHistoryRepository
            .findTopByStockItemIdAndTradeDateLessThanEqualOrderByTradeDateDesc(ITEM_A, at))
        .thenReturn(Optional.empty());
    when(stockPriceHistoryRepository.findTopByStockItemIdAndVolumeGreaterThanOrderByTradeDateDesc(
            ITEM_A, 0L))
        .thenReturn(Optional.of(row("2026-09-09", "269500", 1200L)));

    assertThat(stockPriceService.getPriceAt(ITEM_A, at)).isEqualByComparingTo("269500");
  }

  @Test
  void 최신_종가_묶음_조회는_빈_입력을_묻지_않는다() {
    assertThat(stockPriceService.getLatestPrices(null)).isEmpty();
    assertThat(stockPriceService.getLatestPrices(List.of())).isEmpty();
    assertThat(stockPriceService.getLatestPrices(java.util.Arrays.asList((UUID) null))).isEmpty();
    verify(stockPriceHistoryRepository, never()).findLatestClosePrices(any());
  }

  /** 세 CSV 와 {@code idOrder} 가 어긋나면 남의 종목 가격이 붙는다. */
  @Test
  void 일별_종가_묶음은_세_CSV_와_순번이_나란하다() {
    Map<UUID, LocalDate[]> ranges = new LinkedHashMap<>();
    ranges.put(
        ITEM_A, new LocalDate[] {LocalDate.parse("2026-01-01"), LocalDate.parse("2026-03-31")});
    ranges.put(
        ITEM_B, new LocalDate[] {LocalDate.parse("2026-02-01"), LocalDate.parse("2026-04-30")});
    ranges.put(
        ITEM_C, new LocalDate[] {LocalDate.parse("2026-05-01"), LocalDate.parse("2026-06-30")});
    when(stockDailyClosePriceQuery.findDailyClosePricesGrouped(any(), any(), any(), any()))
        .thenReturn(Map.of());

    stockPriceService.getDailyClosePricesGrouped(ranges);

    ArgumentCaptor<String> ids = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> froms = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> tos = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<UUID>> order = ArgumentCaptor.forClass(List.class);
    verify(stockDailyClosePriceQuery)
        .findDailyClosePricesGrouped(
            ids.capture(), froms.capture(), tos.capture(), order.capture());

    assertThat(ids.getValue()).isEqualTo(ITEM_A + "," + ITEM_B + "," + ITEM_C);
    assertThat(froms.getValue()).isEqualTo("2026-01-01,2026-02-01,2026-05-01");
    assertThat(tos.getValue()).isEqualTo("2026-03-31,2026-04-30,2026-06-30");
    assertThat(order.getValue()).containsExactly(ITEM_A, ITEM_B, ITEM_C);
  }

  /** 못 쓰는 항목(널·뒤집힌 구간)은 네 줄에서 <b>함께</b> 빠져야 한다 - 한 줄만 빠지면 값이 밀린다. */
  @Test
  void 못_쓰는_항목은_네_줄에서_함께_빠진다() {
    Map<UUID, LocalDate[]> ranges = new LinkedHashMap<>();
    ranges.put(
        ITEM_A, new LocalDate[] {LocalDate.parse("2026-01-01"), LocalDate.parse("2026-03-31")});
    ranges.put(
        ITEM_B, new LocalDate[] {LocalDate.parse("2026-04-30"), LocalDate.parse("2026-02-01")});
    ranges.put(ITEM_C, new LocalDate[] {LocalDate.parse("2026-05-01"), null});
    when(stockDailyClosePriceQuery.findDailyClosePricesGrouped(any(), any(), any(), any()))
        .thenReturn(Map.of());

    stockPriceService.getDailyClosePricesGrouped(ranges);

    ArgumentCaptor<String> ids = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> froms = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> tos = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<UUID>> order = ArgumentCaptor.forClass(List.class);
    verify(stockDailyClosePriceQuery)
        .findDailyClosePricesGrouped(
            ids.capture(), froms.capture(), tos.capture(), order.capture());

    assertThat(ids.getValue()).isEqualTo(ITEM_A.toString());
    assertThat(froms.getValue()).isEqualTo("2026-01-01");
    assertThat(tos.getValue()).isEqualTo("2026-03-31");
    assertThat(order.getValue()).containsExactly(ITEM_A);
  }

  /** 쓸 항목이 하나도 없으면 조회하지 않는다(빈 CSV 로 물으면 전 종목이 딸려 올 수 있다). */
  @Test
  void 쓸_항목이_없으면_묻지_않는다() {
    Map<UUID, LocalDate[]> ranges = new LinkedHashMap<>();
    ranges.put(ITEM_A, null);
    ranges.put(
        ITEM_B, new LocalDate[] {LocalDate.parse("2026-04-30"), LocalDate.parse("2026-02-01")});

    assertThat(stockPriceService.getDailyClosePricesGrouped(ranges)).isEmpty();
    assertThat(stockPriceService.getDailyClosePricesGrouped(null)).isEmpty();
    assertThat(stockPriceService.getDailyClosePricesGrouped(Map.of())).isEmpty();
    verify(stockDailyClosePriceQuery, never())
        .findDailyClosePricesGrouped(any(), any(), any(), any());
  }
}
