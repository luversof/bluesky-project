package net.luversof.api.stock.service.kis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

  /**
   * KIS 를 부르는 갱신은 한 번에 하나 - 전체 작업 · 한 종목 갱신 · 원주가 채우기가 이 자물쇠를 함께 쓴다. 예전에는 전체 작업에만 있어, 그 사이 월배당 프로필을
   * 등록하면(게이트가 한 종목 갱신을 부른다) 같은 종목의 오늘 행을 두 스레드가 INSERT 해 유니크 키로 한쪽이 실패했고 호출 간격이 반으로 줄어 초당
   * 한도(EGW00201)에 걸렸다(2026-10-27 감사).
   */
  private final ReentrantLock kisLock = new ReentrantLock();

  /** 다른 갱신이 돌고 있으면 409, 아니면 그 자리에서 돌린다(한 종목 갱신 · 원주가 채우기). */
  public <T> T runExclusive(Supplier<T> work) {
    if (!kisLock.tryLock()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "price update already running");
    }
    try {
      return work.get();
    } finally {
      kisLock.unlock();
    }
  }

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

  /** 수동 갱신이 돌고 있으면 그것이 끝나길 기다렸다 시작한다(한 종목은 1 초 안쪽). */
  private void run(UUID userId) {
    kisLock.lock();
    try {
      runLocked(userId);
    } finally {
      kisLock.unlock();
    }
  }

  private void runLocked(UUID userId) {
    Instant startedAt = state.get().startedAt();
    try {
      PriceHistoryUpdateResult result =
          kisStockPriceUpdateService.updatePriceHistory(userId, this::publishProgress);
      // 새 날의 원주가도 채운다(2026-10-02, 평가 = 원주가 x 실제 주식 수). 이미 채운 날은 건너뛰어 보통 보유 종목마다 한 번.
      // 실패해도 시세 갱신 결과는 그대로 둔다 - 빈 날은 평가가 직전 날 배율로 메운다(TradeProfitService.RawValuation).
      List<String> failedSymbols =
          new ArrayList<>(result.failedSymbols() != null ? result.failedSymbols() : List.of());
      try {
        // 원주가 실패도 실패 목록에 싣는다(raw: 접두) - 예전엔 버려져 "실패 0" 으로 보였다(2026-10-27 감사).
        var rawClose = kisStockPriceUpdateService.fillRawClosePrices(userId);
        if (rawClose != null && rawClose.failedSymbols() != null) {
          rawClose.failedSymbols().forEach(symbol -> failedSymbols.add("raw:" + symbol));
        }
      } catch (RuntimeException rawCloseFailure) {
        log.warn("raw close fill after price update failed", rawCloseFailure);
      }
      state.set(
          new PriceHistoryUpdateJobStatus(
              PriceHistoryUpdateJobStatus.DONE,
              startedAt,
              Instant.now(),
              result.targetSymbolCount(),
              result.targetSymbolCount(),
              List.copyOf(failedSymbols),
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
