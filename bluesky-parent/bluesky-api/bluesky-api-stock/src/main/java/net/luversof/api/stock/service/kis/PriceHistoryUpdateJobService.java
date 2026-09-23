package net.luversof.api.stock.service.kis;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.web.dto.response.PriceHistoryUpdateJobStatus;
import net.luversof.api.stock.web.dto.response.PriceHistoryUpdateResult;

/**
 * 전체 가격 이력 갱신을 <b>뒤에서</b> 돌린다(사용자 결정 2026-09-22).
 *
 * <p>부르는 쪽은 즉시 지금 상태를 받고, 진행 상황은 따로 물어본다. 전체 갱신이 53 종목에 18~22 초라 게이트의 읽기 제한(10 초)을 늘 넘겼기 때문이다
 * &mdash; 제한을 올려도 종목이 늘면 같은 일이 되풀이된다.
 *
 * <p><b>한 번에 하나만 돈다.</b> 이미 돌고 있으면 새로 시작하지 않고 돌고 있는 상태를 그대로 돌려준다. 같은 작업을 겹쳐 돌리면 KIS 호출이 두 배가 되고 같은
 * 행을 두 번 쓴다.
 *
 * <p><b>시작할 때 상태를 통째로 갈아 끼운다.</b> 지난 결과를 남겨 두면 새 작업이 도는 동안 옛 성공이 조회된다 &mdash; 최적화기에서 같은 함정으로 죽은 잡이
 * 남의 결과를 돌려준 적이 있다. 그리고 {@code Throwable} 까지 받는다. {@code Error} 로 죽으면 상태가 영영 RUNNING 에 머물러 다시는 시작할
 * 수 없게 된다.
 */
@Service
public class PriceHistoryUpdateJobService {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(PriceHistoryUpdateJobService.class);

  @Autowired private KisStockPriceUpdateService kisStockPriceUpdateService;

  /**
   * 작업 전용 실행기. 앱 전체에 {@code @EnableAsync} 를 켜지 않는다 &mdash; 이 하나 때문에 다른 곳의 실행 방식까지 바뀌면 안 된다. 스레드는
   * 데몬이라 종료를 막지 않는다.
   */
  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "price-history-update");
            thread.setDaemon(true);
            return thread;
          });

  private final AtomicReference<PriceHistoryUpdateJobStatus> state =
      new AtomicReference<>(PriceHistoryUpdateJobStatus.idle());

  /** 지금 상태. 아무 일도 일으키지 않는다. */
  public PriceHistoryUpdateJobStatus status() {
    return state.get();
  }

  /**
   * 갱신을 시작한다. 이미 돌고 있으면 그 상태를 그대로 돌려준다(새로 시작하지 않는다).
   *
   * @return 시작 직후의 상태. 거의 언제나 RUNNING 이다.
   */
  public PriceHistoryUpdateJobStatus start(UUID userId) {
    PriceHistoryUpdateJobStatus current = state.get();
    if (current.running()) {
      log.info("price history update already running - skipping duplicate start");
      return current;
    }

    PriceHistoryUpdateJobStatus started =
        new PriceHistoryUpdateJobStatus(
            PriceHistoryUpdateJobStatus.RUNNING, Instant.now(), null, 0, 0, List.of(), null);
    if (!state.compareAndSet(current, started)) {
      // 그 사이 누가 먼저 시작했다. 겹쳐 돌리지 않는다.
      return state.get();
    }

    executor.execute(() -> run(userId));
    return started;
  }

  private void run(UUID userId) {
    Instant startedAt = state.get().startedAt();
    try {
      PriceHistoryUpdateResult result =
          kisStockPriceUpdateService.updatePriceHistory(userId, this::publishProgress);
      state.set(
          new PriceHistoryUpdateJobStatus(
              PriceHistoryUpdateJobStatus.DONE,
              startedAt,
              Instant.now(),
              result.targetSymbolCount(),
              result.targetSymbolCount(),
              result.failedSymbols() != null ? List.copyOf(result.failedSymbols()) : List.of(),
              null));
    } catch (Throwable throwable) {
      // Error 까지 받는다 - 안 그러면 상태가 RUNNING 에 갇혀 다시는 시작할 수 없다.
      log.error("price history update job failed", throwable);
      state.set(
          new PriceHistoryUpdateJobStatus(
              PriceHistoryUpdateJobStatus.FAILED,
              startedAt,
              Instant.now(),
              state.get().processedSymbolCount(),
              state.get().targetSymbolCount(),
              List.of(),
              throwable.getMessage() != null ? throwable.getMessage() : throwable.toString()));
    }
  }

  /** 종목 하나를 끝낼 때마다 불린다. 끝난 작업의 상태는 건드리지 않는다. */
  private void publishProgress(int processed, int total) {
    state.updateAndGet(
        current ->
            current.running()
                ? new PriceHistoryUpdateJobStatus(
                    current.status(),
                    current.startedAt(),
                    null,
                    processed,
                    total,
                    current.failedSymbols(),
                    null)
                : current);
  }
}
