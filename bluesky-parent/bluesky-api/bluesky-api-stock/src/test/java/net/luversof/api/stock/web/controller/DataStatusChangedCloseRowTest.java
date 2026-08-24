package net.luversof.api.stock.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.ZeroVolumeChangedClose;

/**
 * 거래량 0 인데 종가가 바뀐 행을 관리 화면이 읽을 수 있게 옮기는 규칙을 고정한다.
 *
 * <p>지금까지 이 값은 <b>개수만</b> 나왔다(실측 2026-08-23: "1"). 개수만으로는 그것이 수집 오류인지 액면분할 같은 정상 조정인지 알 수 없고, 이 앱에는
 * 시세 이력을 읽는 다른 경로가 없어 확인할 방법 자체가 없었다. 실제 그 한 건은 쌍방울 2025-05-08 로 종가가 13,450 에서 2,690 으로 정확히 1/5 이
 * 됐다 &mdash; 액면분할이다.
 */
class DataStatusChangedCloseRowTest {

  private static final UUID ITEM = UUID.randomUUID();

  private ZeroVolumeChangedClose row(String previousClose, String close) {
    return new ZeroVolumeChangedClose(
        ITEM, LocalDate.of(2025, 5, 8), new BigDecimal(close), new BigDecimal(previousClose));
  }

  @Test
  void 종목_이름을_붙인다() {
    var rows = DataStatusController.withNames(List.of(row("13450", "2690")), Map.of(ITEM, "쌍방울"));

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).stockItemName()).isEqualTo("쌍방울");
    assertThat(rows.get(0).previousClosePrice()).isEqualByComparingTo("13450");
    assertThat(rows.get(0).closePrice()).isEqualByComparingTo("2690");
    assertThat(rows.get(0).tradeDate()).isEqualTo(LocalDate.of(2025, 5, 8));
  }

  /** 이름을 못 찾아도 행을 숨기지 않는다. 숨기면 정작 이상한 데이터를 못 보게 된다. */
  @Test
  void 이름을_못_찾으면_id를_그대로_쓴다() {
    var rows = DataStatusController.withNames(List.of(row("13450", "2690")), Map.of());

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).stockItemName()).isEqualTo(ITEM.toString());
  }

  @Test
  void 행이_없거나_null_이면_빈_목록이다() {
    assertThat(DataStatusController.withNames(List.of(), Map.of())).isEmpty();
    assertThat(DataStatusController.withNames(null, Map.of())).isEmpty();
    assertThat(DataStatusController.withNames(List.of(row("1", "2")), null)).hasSize(1);
  }
}
