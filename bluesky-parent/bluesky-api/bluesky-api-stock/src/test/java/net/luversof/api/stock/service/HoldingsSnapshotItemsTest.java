package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.service.TradeProfitService.WmaState;
import net.luversof.api.stock.web.dto.response.HoldingsSnapshotItem;

/**
 * 고른 날짜의 보유 스냅숏 한 줄 만들기 - 이 셈을 부르는 시험이 없었다(13차 검토 2026-10-31). 표시 수량 x 표시 가격 = 평가액이 맞아야 게이트의 시가배당률이
 * 배수만큼 어긋나지 않는다.
 */
class HoldingsSnapshotItemsTest {

  private static final UUID PLAIN = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID SPLIT = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
  private static final UUID RAW = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
  private static final UUID SOLD = UUID.fromString("00000000-0000-0000-0000-0000000000a4");
  private static final LocalDate DAY = LocalDate.parse("2026-10-30");

  private static WmaState state(UUID id, String quantity, long rawQuantity, String cost) {
    WmaState state = new WmaState();
    state.setStockItemId(id);
    state.setQuantity(new BigDecimal(quantity));
    state.setRawQuantity(rawQuantity);
    state.setTotalCost(new BigDecimal(cost));
    return state;
  }

  private static HoldingsSnapshotItem find(Iterable<HoldingsSnapshotItem> items, UUID id) {
    for (HoldingsSnapshotItem item : items) {
      if (item.stockItemId().equals(id)) {
        return item;
      }
    }
    throw new AssertionError("no row " + id);
  }

  @Test
  void 수정_종가_환산_수량이면_표시_가격을_실제_주식_수_기준으로_바꾸고_원주가_종목은_원주가로_평가한다() {
    Map<String, WmaState> states = new LinkedHashMap<>();
    // 같은 종목을 두 계좌에 - 한 줄로 합친다.
    states.put("acc1:" + PLAIN, state(PLAIN, "4", 4, "400"));
    states.put("acc2:" + PLAIN, state(PLAIN, "6", 6, "600"));
    // 분할로 내부 수량 20 · 실제 10 주.
    states.put("acc1:" + SPLIT, state(SPLIT, "20", 10, "1000"));
    WmaState raw = state(RAW, "10.5", 10, "1500");
    raw.setRawShares(new BigDecimal("10"));
    raw.setCapturedRawClose(new BigDecimal("160"));
    states.put("acc1:" + RAW, raw);
    states.put("acc1:" + SOLD, state(SOLD, "0", 0, "0"));
    Map<UUID, BigDecimal> prices =
        Map.of(
            PLAIN, new BigDecimal("150"),
            SPLIT, new BigDecimal("75"),
            RAW, new BigDecimal("140"));

    var items =
        new TradeProfitService(null, null, null, null, null, null)
            .buildHoldingsSnapshotItems(states, DAY, prices, Map.of(PLAIN, DAY), Map.of());

    assertThat(items).as("보유 0 은 뺀다").hasSize(3);
    var plain = find(items, PLAIN);
    assertThat(plain.quantity()).isEqualByComparingTo("10");
    assertThat(plain.avgCost()).isEqualByComparingTo("100");
    assertThat(plain.priceAtDate()).isEqualByComparingTo("150");
    assertThat(plain.value()).isEqualByComparingTo("1500");
    assertThat(plain.unrealizedProfit()).isEqualByComparingTo("500");
    assertThat(plain.priceDate()).isEqualTo(DAY);

    var split = find(items, SPLIT);
    assertThat(split.quantity()).isEqualByComparingTo("10");
    assertThat(split.priceAtDate()).as("75 x 20 = 1500 을 10 주로").isEqualByComparingTo("150");
    assertThat(split.quantity().multiply(split.priceAtDate())).isEqualByComparingTo(split.value());

    var rawRow = find(items, RAW);
    assertThat(rawRow.value()).isEqualByComparingTo("1600");
    assertThat(rawRow.priceAtDate()).isEqualByComparingTo("160");
    assertThat(rawRow.quantity()).isEqualByComparingTo("10");
    assertThat(rawRow.avgCost()).isEqualByComparingTo("150");

    assertThat(items)
        .extracting(HoldingsSnapshotItem::unrealizedProfit)
        .as("평가 손익 큰 순")
        .isSortedAccordingTo((a, b) -> b.compareTo(a));
  }
}
