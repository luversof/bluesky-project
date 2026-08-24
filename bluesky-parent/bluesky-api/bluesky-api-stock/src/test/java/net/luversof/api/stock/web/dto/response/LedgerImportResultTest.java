package net.luversof.api.stock.web.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 원장 일괄 가져오기 결과가 "몇 행이 버려졌는지" 를 담는지 고정한다.
 *
 * <p>거래 가져오기는 종목명을 정확히 못 찾으면 그 행을 통째로 버리는데 {@code log.debug} 한 줄만 남겼다. 운영에서는 DEBUG 가 꺼져 있어 흔적이 전혀
 * 없다 &mdash; 시트에서 ETF 이름이 바뀌기만 해도 그 종목 거래가 전부 사라지고, 화면에는 그냥 거래가 적게 보인다.
 */
class LedgerImportResultTest {

  @Test
  void 전부_들어가면_버려진_행이_0이다() {
    var result = new LedgerImportResult(250, 250, List.of());
    assertEquals(0, result.droppedCount());
  }

  @Test
  void 버려진_행수는_시트행수와_저장행수의_차이다() {
    var result = new LedgerImportResult(250, 114, List.of("KODEX 200", "TIGER 리츠부동산인프라"));
    assertEquals(136, result.droppedCount());
    assertEquals(2, result.unknownStockNames().size());
  }

  /** 종목명을 못 찾은 이름이 결과에 남아야 원인을 바로 알 수 있다. */
  @Test
  void 못_찾은_종목명을_그대로_담는다() {
    var result = new LedgerImportResult(10, 9, List.of("이름이 바뀐 ETF"));
    assertEquals(1, result.droppedCount());
    assertEquals("이름이 바뀐 ETF", result.unknownStockNames().get(0));
  }
}
