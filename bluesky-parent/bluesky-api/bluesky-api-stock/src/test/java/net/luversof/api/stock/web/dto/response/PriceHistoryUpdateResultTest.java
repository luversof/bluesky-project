package net.luversof.api.stock.web.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 가격 갱신 결과가 "몇 개가 실패했는지" 를 실제로 담는지 고정한다.
 *
 * <p>이 작업은 예전에 {@code void} 였다. 종목별 조회가 실패해도 경고 한 줄만 남기고 넘어가, 관리 화면은 몇 개가 실패하든 늘 성공으로 보였다. 실측
 * (2026-05-25~08-22, 보유 9종목): 평일인데 가격행이 아예 없는 날이 2일 있었다(2026-07-17, 2026-08-21 &mdash; 둘 다 휴장일이
 * 아니다).
 */
class PriceHistoryUpdateResultTest {

  @Test
  void 실패가_없으면_0건이다() {
    var result = new PriceHistoryUpdateResult(86, List.of());
    assertEquals(86, result.targetSymbolCount());
    assertEquals(0, result.failedSymbolCount());
  }

  @Test
  void 실패_종목을_그대로_센다() {
    var result = new PriceHistoryUpdateResult(86, List.of("005930", "069500"));
    assertEquals(2, result.failedSymbolCount());
    assertTrue(result.failedSymbols().contains("005930"));
  }

  /** 목록이 없어도 세다가 터지지 않는다(직렬화 왕복에서 null 이 될 수 있다). */
  @Test
  void 목록이_없으면_0으로_본다() {
    assertEquals(0, new PriceHistoryUpdateResult(0, null).failedSymbolCount());
  }
}
