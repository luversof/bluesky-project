package net.luversof.api.stock.web.dto.response;

import java.util.List;

/**
 * 원장(거래/배당) 일괄 가져오기 한 번의 결과.
 *
 * <p>예전에는 이 작업이 {@code void} 였고, 매핑에 실패한 행은 {@code filter(Objects::nonNull)} 로 조용히 사라졌다. 특히 거래는
 * 종목명을 정확히 못 찾으면 {@code log.debug} 한 줄만 남기고 그 행을 통째로 버렸다 &mdash; 운영에서는 DEBUG 가 꺼져 있어 <b>아무 흔적도 남지
 * 않는다</b>. 시트에서 종목명이 바뀌기만 해도(국내 ETF 는 개명이 잦다) 그 종목 거래가 전부 사라지고 화면에는 그냥 거래가 적게 보인다.
 *
 * @param sourceRowCount 시트에서 읽은 행 수
 * @param importedCount 실제로 저장한 행 수
 * @param unknownStockNames 종목을 찾지 못해 버린 이름(중복 제거)
 */
public record LedgerImportResult(
    int sourceRowCount, int importedCount, List<String> unknownStockNames) {

  /** 시트에 있었지만 저장되지 않은 행 수. */
  public int droppedCount() {
    return sourceRowCount - importedCount;
  }
}
