package net.luversof.web.gate.stock.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 가격 이력 갱신 결과(api-stock 응답 그대로).
 *
 * <p>이 작업은 예전에 {@code void} 였다. 종목별 조회가 실패해도 경고 한 줄만 남기고 넘어가, 관리 화면은 몇 개가 실패하든 늘 성공으로 보였다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PriceHistoryUpdateResult(int targetSymbolCount, List<String> failedSymbols) {

  public int failedSymbolCount() {
    return failedSymbols == null ? 0 : failedSymbols.size();
  }
}
