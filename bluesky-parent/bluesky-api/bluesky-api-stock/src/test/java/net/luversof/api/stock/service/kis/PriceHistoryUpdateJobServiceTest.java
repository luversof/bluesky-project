package net.luversof.api.stock.service.kis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import net.luversof.api.stock.service.kis.KisStockPriceUpdateService.ProgressListener;
import net.luversof.api.stock.web.dto.response.PriceHistoryUpdateJobStatus;
import net.luversof.api.stock.web.dto.response.PriceHistoryUpdateResult;

/**
 * 전체 가격 이력 갱신 작업(사용자 결정 2026-09-22: 백그라운드로).
 *
 * <p>여기서 막는 것은 셋이다 &mdash; 겹쳐 돌기, 지난 결과가 새 작업 중에 조회되는 것, {@code Error} 로 죽어 상태가 RUNNING 에 갇히는 것.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PriceHistoryUpdateJobServiceTest {

  private static final UUID USER = UUID.randomUUID();

  @Mock private KisStockPriceUpdateService kisStockPriceUpdateService;

  @InjectMocks private PriceHistoryUpdateJobService priceHistoryUpdateJobService;

  private static void awaitDone(PriceHistoryUpdateJobService service) {
    for (int attempt = 0; attempt < 200; attempt++) {
      if (!service.status().running()) {
        return;
      }
      try {
        Thread.sleep(25);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return;
      }
    }
    throw new AssertionError("작업이 끝나지 않았다 - 상태가 RUNNING 에 갇혔다");
  }

  @Test
  void 시작하면_즉시_돌고_있다고_답한다() {
    when(kisStockPriceUpdateService.updatePriceHistory(eq(USER), any(ProgressListener.class)))
        .thenReturn(new PriceHistoryUpdateResult(3, List.of()));

    PriceHistoryUpdateJobStatus started = priceHistoryUpdateJobService.start(USER);

    // 18~22 초짜리 일을 기다리지 않는다 - 부르는 쪽은 바로 답을 받는다.
    assertThat(started.status()).isEqualTo(PriceHistoryUpdateJobStatus.RUNNING);
    assertThat(started.startedAt()).isNotNull();
    assertThat(started.finishedAt()).isNull();

    awaitDone(priceHistoryUpdateJobService);
    PriceHistoryUpdateJobStatus done = priceHistoryUpdateJobService.status();
    assertThat(done.status()).isEqualTo(PriceHistoryUpdateJobStatus.DONE);
    assertThat(done.targetSymbolCount()).isEqualTo(3);
    assertThat(done.processedSymbolCount()).isEqualTo(3);
    assertThat(done.finishedAt()).isNotNull();
  }

  @Test
  void 돌고_있으면_겹쳐_시작하지_않는다() throws Exception {
    CountDownLatch hold = new CountDownLatch(1);
    when(kisStockPriceUpdateService.updatePriceHistory(eq(USER), any(ProgressListener.class)))
        .thenAnswer(
            invocation -> {
              hold.await(5, TimeUnit.SECONDS);
              return new PriceHistoryUpdateResult(1, List.of());
            });

    priceHistoryUpdateJobService.start(USER);
    PriceHistoryUpdateJobStatus second = priceHistoryUpdateJobService.start(USER);

    // 겹쳐 돌리면 KIS 호출이 두 배가 되고 같은 행을 두 번 쓴다.
    assertThat(second.status()).isEqualTo(PriceHistoryUpdateJobStatus.RUNNING);
    hold.countDown();
    awaitDone(priceHistoryUpdateJobService);
    verify(kisStockPriceUpdateService, times(1))
        .updatePriceHistory(eq(USER), any(ProgressListener.class));
  }

  @Test
  void 새_작업이_도는_동안_지난_결과가_안_보인다() throws Exception {
    when(kisStockPriceUpdateService.updatePriceHistory(eq(USER), any(ProgressListener.class)))
        .thenReturn(new PriceHistoryUpdateResult(7, List.of("489030")));
    priceHistoryUpdateJobService.start(USER);
    awaitDone(priceHistoryUpdateJobService);
    assertThat(priceHistoryUpdateJobService.status().failedSymbols()).containsExactly("489030");

    CountDownLatch hold = new CountDownLatch(1);
    when(kisStockPriceUpdateService.updatePriceHistory(eq(USER), any(ProgressListener.class)))
        .thenAnswer(
            invocation -> {
              hold.await(5, TimeUnit.SECONDS);
              return new PriceHistoryUpdateResult(2, List.of());
            });

    PriceHistoryUpdateJobStatus running = priceHistoryUpdateJobService.start(USER);

    // 지난 결과를 남겨 두면 새 작업이 도는 동안 옛 성공 · 옛 실패가 그대로 조회된다.
    assertThat(running.failedSymbols()).isEmpty();
    assertThat(running.targetSymbolCount()).isZero();
    assertThat(running.finishedAt()).isNull();
    hold.countDown();
    awaitDone(priceHistoryUpdateJobService);
  }

  @Test
  void 도는_동안_진행_상황을_알린다() throws Exception {
    // 끝난 뒤의 수만 보면 알림을 아예 안 해도 통과한다(DONE 이 그 자리를 덮어쓴다).
    // 돌고 있는 동안에 봐야 진짜 진행이 나가는지 알 수 있다.
    CountDownLatch reported = new CountDownLatch(1);
    CountDownLatch hold = new CountDownLatch(1);
    when(kisStockPriceUpdateService.updatePriceHistory(eq(USER), any(ProgressListener.class)))
        .thenAnswer(
            invocation -> {
              ProgressListener listener = invocation.getArgument(1);
              listener.onProgress(0, 5);
              listener.onProgress(3, 5);
              reported.countDown();
              hold.await(5, TimeUnit.SECONDS);
              return new PriceHistoryUpdateResult(5, List.of());
            });

    priceHistoryUpdateJobService.start(USER);
    assertThat(reported.await(5, TimeUnit.SECONDS)).isTrue();

    PriceHistoryUpdateJobStatus midway = priceHistoryUpdateJobService.status();
    assertThat(midway.status()).isEqualTo(PriceHistoryUpdateJobStatus.RUNNING);
    assertThat(midway.processedSymbolCount()).as("도는 동안 분자").isEqualTo(3);
    assertThat(midway.targetSymbolCount()).as("분모는 처음부터 맞아야 한다").isEqualTo(5);

    hold.countDown();
    awaitDone(priceHistoryUpdateJobService);
    assertThat(priceHistoryUpdateJobService.status().processedSymbolCount()).isEqualTo(5);
  }

  @Test
  void 늦게_온_진행_알림은_끝난_결과를_안_건드린다() throws Exception {
    // 작업이 끝난 뒤에 늦은 알림이 닿으면 DONE 이 다시 RUNNING 처럼 보이거나 실패 목록이 지워진다.
    java.util.concurrent.atomic.AtomicReference<ProgressListener> captured =
        new java.util.concurrent.atomic.AtomicReference<>();
    when(kisStockPriceUpdateService.updatePriceHistory(eq(USER), any(ProgressListener.class)))
        .thenAnswer(
            invocation -> {
              captured.set(invocation.getArgument(1));
              return new PriceHistoryUpdateResult(4, List.of("489030"));
            });

    priceHistoryUpdateJobService.start(USER);
    awaitDone(priceHistoryUpdateJobService);
    assertThat(captured.get()).isNotNull();

    captured.get().onProgress(1, 99);

    PriceHistoryUpdateJobStatus done = priceHistoryUpdateJobService.status();
    assertThat(done.status()).isEqualTo(PriceHistoryUpdateJobStatus.DONE);
    assertThat(done.processedSymbolCount()).isEqualTo(4);
    assertThat(done.targetSymbolCount()).isEqualTo(4);
    assertThat(done.failedSymbols()).containsExactly("489030");
    assertThat(done.finishedAt()).isNotNull();
  }

  @Test
  void Error_로_죽어도_상태가_갇히지_않는다() {
    // catch (Exception) 으로만 받으면 Error 가 빠져나가 상태가 영영 RUNNING 에 머문다 - 다시는 시작할 수 없다.
    when(kisStockPriceUpdateService.updatePriceHistory(eq(USER), any(ProgressListener.class)))
        .thenThrow(new StackOverflowError("깊이"));

    priceHistoryUpdateJobService.start(USER);
    awaitDone(priceHistoryUpdateJobService);

    PriceHistoryUpdateJobStatus failed = priceHistoryUpdateJobService.status();
    assertThat(failed.status()).isEqualTo(PriceHistoryUpdateJobStatus.FAILED);
    assertThat(failed.failureReason()).contains("깊이");
    assertThat(failed.running()).isFalse();

    // 그리고 다시 시작할 수 있어야 한다.
    when(kisStockPriceUpdateService.updatePriceHistory(eq(USER), any(ProgressListener.class)))
        .thenReturn(new PriceHistoryUpdateResult(1, List.of()));
    assertThat(priceHistoryUpdateJobService.start(USER).status())
        .isEqualTo(PriceHistoryUpdateJobStatus.RUNNING);
    awaitDone(priceHistoryUpdateJobService);
    assertThat(priceHistoryUpdateJobService.status().status())
        .isEqualTo(PriceHistoryUpdateJobStatus.DONE);
  }

  @Test
  void 아직_안_돌았으면_비어_있다() {
    PriceHistoryUpdateJobStatus idle = priceHistoryUpdateJobService.status();

    assertThat(idle.status()).isEqualTo(PriceHistoryUpdateJobStatus.IDLE);
    assertThat(idle.startedAt()).isNull();
    assertThat(idle.failedSymbols()).isEmpty();
  }
}
