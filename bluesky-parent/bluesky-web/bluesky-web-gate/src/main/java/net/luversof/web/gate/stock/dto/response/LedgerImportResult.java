package net.luversof.web.gate.stock.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 원장(거래/배당) 일괄 가져오기 결과(api-stock 응답 그대로).
 *
 * <p>매핑에 실패한 행은 예전에도 지금도 버려지지만, 이제는 몇 행이 버려졌는지 알 수 있다. 거래는 종목명을 못 찾으면 DEBUG 한 줄만 남기고 사라져 운영에서는 흔적이
 * 없었다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LedgerImportResult(
    int sourceRowCount, int importedCount, List<String> unknownStockNames) {

  public int droppedCount() {
    return sourceRowCount - importedCount;
  }
}
