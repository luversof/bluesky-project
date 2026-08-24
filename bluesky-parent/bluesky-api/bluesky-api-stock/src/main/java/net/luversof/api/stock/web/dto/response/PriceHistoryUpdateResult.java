package net.luversof.api.stock.web.dto.response;

import java.util.List;

/**
 * 가격 이력 갱신 한 번의 결과.
 *
 * <p>예전에는 이 작업이 {@code void} 였다. 종목별 조회가 실패해도 경고 한 줄만 남기고 넘어갔으므로, 관리 화면은 몇 개가 실패하든 늘 성공으로 보였다.
 *
 * <p>실측(2026-05-25~08-22, 보유 9종목): 평일인데 가격행이 아예 없는 날이 2일 있었다(2026-07-17, 2026-08-21 &mdash; 둘 다
 * 휴장일이 아니다). 이런 구멍은 화면에서 "가격 기준일이 예상보다 오래됐다" 로만 간접적으로 드러난다.
 *
 * @param targetSymbolCount 이번 실행에서 조회를 시도한 종목 수
 * @param failedSymbols 조회에 실패한 종목코드(성공하면 빈 목록)
 */
public record PriceHistoryUpdateResult(int targetSymbolCount, List<String> failedSymbols) {

  public int failedSymbolCount() {
    return failedSymbols == null ? 0 : failedSymbols.size();
  }
}
