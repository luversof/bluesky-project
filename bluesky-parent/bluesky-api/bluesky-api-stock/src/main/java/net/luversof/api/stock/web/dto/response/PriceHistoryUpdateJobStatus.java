package net.luversof.api.stock.web.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * 전체 가격 이력 갱신 작업의 지금 상태(사용자 결정 2026-09-22: 백그라운드 작업으로).
 *
 * <p><b>왜 작업으로 돌리나</b> &mdash; 전체 갱신은 53 종목에 18~22 초가 걸리는데 게이트의 읽기 제한은 10 초다(실측 2026-09-22:
 * 08:17:06 시작 &rarr; 08:17:28 완료). 그래서 누를 때마다 게이트가 먼저 끊고 500 을 냈고, 정작 api-stock 은 끝까지 돌아 데이터는 갱신돼
 * 있었다 &mdash; 화면만 실패로 보였다. 종목이 더 늘면 제한을 올려도 같은 일이 반복되므로, 부르는 쪽은 즉시 답을 받고 진행 상황을 따로 물어본다.
 *
 * @param status IDLE(아직 한 번도 안 돌았다) · RUNNING · DONE · FAILED
 * @param startedAt 이번 작업이 시작된 때(IDLE 이면 null)
 * @param finishedAt 끝난 때(RUNNING 이면 null)
 * @param processedSymbolCount 지금까지 처리한 종목 수
 * @param targetSymbolCount 처리할 종목 수. RUNNING 중에는 <b>아직 0 일 수 있다</b>(대상을 고르는 중)
 * @param failedSymbols 실패한 종목코드
 * @param failureReason 작업 자체가 엎어진 까닭(FAILED 일 때만)
 */
public record PriceHistoryUpdateJobStatus(
    String status,
    Instant startedAt,
    Instant finishedAt,
    int processedSymbolCount,
    int targetSymbolCount,
    List<String> failedSymbols,
    String failureReason) {

  public static final String IDLE = "IDLE";

  public static final String RUNNING = "RUNNING";

  public static final String DONE = "DONE";

  public static final String FAILED = "FAILED";

  public static PriceHistoryUpdateJobStatus idle() {
    return new PriceHistoryUpdateJobStatus(IDLE, null, null, 0, 0, List.of(), null);
  }

  public boolean running() {
    return RUNNING.equals(status);
  }

  public int failedSymbolCount() {
    return failedSymbols == null ? 0 : failedSymbols.size();
  }
}
