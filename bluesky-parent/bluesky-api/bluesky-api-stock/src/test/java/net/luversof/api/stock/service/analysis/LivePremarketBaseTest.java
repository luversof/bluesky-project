package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.StockPriceHistory;

/** 실시간 장전가 줄의 기준 종가(2026-11-24: 장 마감 뒤에 돈 아침 작업이 지난 장전 갭으로 다음 날을 예측했다). */
class LivePremarketBaseTest {

  private static StockPriceHistory row(String date) {
    StockPriceHistory h = new StockPriceHistory();
    h.setTradeDate(LocalDate.parse(date));
    return h;
  }

  private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);

  @Test
  void 아침에는_오늘_앞의_마지막_거래일이_기준() {
    var base =
        MarketContextService.livePremarketBase(
            List.of(row("2026-11-20"), row("2026-11-19")), MONDAY);
    assertThat(base.getTradeDate()).isEqualTo(LocalDate.of(2026, 11, 20));
  }

  @Test
  void 오늘_종가가_이미_있으면_다음_날_장전가가_없으므로_null() {
    assertThat(
            MarketContextService.livePremarketBase(
                List.of(row("2026-11-23"), row("2026-11-20")), MONDAY))
        .isNull();
    assertThat(MarketContextService.livePremarketBase(List.of(), MONDAY)).isNull();
  }
}
