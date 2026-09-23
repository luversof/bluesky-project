package net.luversof.web.gate.stock.dto.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 전체 가격 이력 갱신 작업의 지금 상태(api-stock 응답 그대로).
 *
 * <p>전체 갱신은 53 종목에 18~22 초가 걸리는데 이 게이트의 읽기 제한은 10 초다 &mdash; 그래서 누를 때마다 500 이 났고, 정작 api-stock 은
 * 끝까지 돌아 데이터는 갱신돼 있었다(실측 2026-09-22). 이제 누르면 <b>시작만</b> 하고, 화면이 진행 상황을 따로 물어본다.
 *
 * @param status IDLE · RUNNING · DONE · FAILED
 * @param processedSymbolCount 지금까지 처리한 종목 수
 * @param targetSymbolCount 처리할 종목 수. 막 시작했을 때는 아직 0 일 수 있다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PriceHistoryUpdateJobStatus(
    String status,
    Instant startedAt,
    Instant finishedAt,
    int processedSymbolCount,
    int targetSymbolCount,
    List<String> failedSymbols,
    String failureReason) {

  public boolean running() {
    return "RUNNING".equals(status);
  }

  public int failedSymbolCount() {
    return failedSymbols == null ? 0 : failedSymbols.size();
  }
}
