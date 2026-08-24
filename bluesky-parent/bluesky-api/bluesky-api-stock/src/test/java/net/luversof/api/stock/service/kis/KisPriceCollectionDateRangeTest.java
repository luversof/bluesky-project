package net.luversof.api.stock.service.kis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.StockItemDateRange;

/**
 * 시세 수집 구간을 정하는 규칙을 고정한다.
 *
 * <p>수집 시작·종료일은 매매와 배당 두 원천의 instant 를 시장 타임존으로 바꿔 합친 값이다. 이 변환이나 합치기가 어긋나면 KIS 를 잘못된 구간으로 부르고,
 * 화면에는 "그 날 가격이 없다" 로만 보여 원인을 찾기 어렵다. 두 원천에 같은 코드가 복사돼 있던 자리라 규칙이 한쪽으로만 바뀔 위험이 있었다.
 */
class KisPriceCollectionDateRangeTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final UUID ITEM = UUID.randomUUID();
  private static final UUID OTHER = UUID.randomUUID();

  private Map<UUID, LocalDate> min;
  private Map<UUID, LocalDate> max;

  private void accumulate(StockItemDateRange... ranges) {
    if (min == null) {
      min = new HashMap<>();
      max = new HashMap<>();
    }
    KisStockPriceUpdateService.accumulateDateRanges(List.of(ranges), min, max, KST);
  }

  /**
   * KST 자정~오전 9 시의 기록은 UTC 로는 전날이다. 시장 타임존으로 바꿔야 그 종목이 실제로 거래된 날이 나온다.
   *
   * <p>이 프로젝트에서 같은 착각(instant 를 UTC 문자열로 자르기)이 반복해서 날짜를 하루 어긋나게 했다.
   */
  @Test
  void instant_는_시장_타임존으로_해석한다() {
    // 2026-08-19T15:00Z = 2026-08-20 00:00 KST
    accumulate(new StockItemDateRange(ITEM, Instant.parse("2026-08-19T15:00:00Z"), null));

    assertThat(min).containsEntry(ITEM, LocalDate.of(2026, 8, 20));
  }

  @Test
  void 두_원천의_범위를_최소_최대로_합친다() {
    // 배당 쪽: 2020-04-08 ~ 2026-07-20
    accumulate(
        new StockItemDateRange(
            ITEM, Instant.parse("2020-04-08T00:00:00Z"), Instant.parse("2026-07-20T00:00:00Z")));
    // 매매 쪽: 2009-10-06 ~ 2026-08-19 (양쪽으로 더 넓다)
    accumulate(
        new StockItemDateRange(
            ITEM, Instant.parse("2009-10-06T00:00:00Z"), Instant.parse("2026-08-19T00:00:00Z")));

    assertThat(min).containsEntry(ITEM, LocalDate.of(2009, 10, 6));
    assertThat(max).containsEntry(ITEM, LocalDate.of(2026, 8, 19));
  }

  /** 나중에 들어온 값이 더 좁아도 범위를 줄이면 안 된다(수집 구간이 잘린다). */
  @Test
  void 더_좁은_범위가_뒤에_와도_범위를_줄이지_않는다() {
    accumulate(
        new StockItemDateRange(
            ITEM, Instant.parse("2009-10-06T00:00:00Z"), Instant.parse("2026-08-19T00:00:00Z")));
    accumulate(
        new StockItemDateRange(
            ITEM, Instant.parse("2020-04-08T00:00:00Z"), Instant.parse("2026-07-20T00:00:00Z")));

    assertThat(min).containsEntry(ITEM, LocalDate.of(2009, 10, 6));
    assertThat(max).containsEntry(ITEM, LocalDate.of(2026, 8, 19));
  }

  /** 한쪽만 있는 범위도 그쪽만 반영한다(배당 쿼리는 recordDate/payDate 중 하나만 있을 수 있다). */
  @Test
  void 최소나_최대_한쪽만_있어도_그쪽만_반영한다() {
    accumulate(new StockItemDateRange(ITEM, Instant.parse("2020-04-08T00:00:00Z"), null));

    assertThat(min).containsEntry(ITEM, LocalDate.of(2020, 4, 8));
    assertThat(max).doesNotContainKey(ITEM);
  }

  @Test
  void 종목이_없는_행은_건너뛴다() {
    accumulate(
        new StockItemDateRange(null, Instant.parse("2020-04-08T00:00:00Z"), null),
        new StockItemDateRange(OTHER, Instant.parse("2021-01-04T00:00:00Z"), null));

    assertThat(min).containsOnlyKeys(OTHER);
  }

  @Test
  void 종목마다_따로_모은다() {
    accumulate(
        new StockItemDateRange(ITEM, Instant.parse("2009-10-06T00:00:00Z"), null),
        new StockItemDateRange(OTHER, Instant.parse("2021-01-04T00:00:00Z"), null));

    assertThat(min)
        .containsEntry(ITEM, LocalDate.of(2009, 10, 6))
        .containsEntry(OTHER, LocalDate.of(2021, 1, 4));
  }
}
