package net.luversof.web.gate.stock.support;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

/**
 * 한 화면을 그리는 데 필요한, 서로 의존이 없는 api-stock 호출을 동시에 던지기 위한 도우미.
 *
 * <p>실행기는 SecurityContext 를 전파하도록 감싸져 있다({@code GateStockConfig}). RestClient 인터셉터가
 * SecurityContextHolder 에서 토큰을 꺼내므로, 감싸지 않은 스레드에서 호출하면 Authorization 헤더가 조용히 빠진다.
 *
 * <p>이름 붙이기(메시지 조회)처럼 LocaleContextHolder 에 의존하는 작업은 여기서 돌리지 말고 요청 스레드에서 처리할 것.
 */
@Component
public class StockAsyncSupport {

  private final ExecutorService executor;

  public StockAsyncSupport(ExecutorService stockRemoteCallExecutor) {
    this.executor = stockRemoteCallExecutor;
  }

  public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
    return CompletableFuture.supplyAsync(supplier, executor);
  }

  /** join 이 감싸는 CompletionException 을 벗겨 순차 호출과 같은 예외가 밖으로 나가게 한다. */
  public static <T> T join(CompletableFuture<T> future) {
    try {
      return future.join();
    } catch (CompletionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException runtimeException) throw runtimeException;
      if (cause instanceof Error error) throw error;
      throw e;
    }
  }

  /**
   * 한 요청 안에서 같은 호출이 두 번 나가지 않게 한다.
   *
   * <p>실측 2026-09-10(qa/iso-fragment.cjs + 로깅 프록시): 조회 기간을 고르지 않은 기본 진입에서는 '기간 손익' 과 '전체 스냅샷' 의
   * 파라미터가 완전히 같아져, 종목 상세·계좌 상세 조각이 {@code calculateProfit} 을 똑같은 URL 로 두 번 불렀다. 기간을 고르면 서로 달라지므로 조건
   * 분기 대신 '같은 열쇠면 이미 던진 것을 그대로 쓴다' 로 푼다.
   */
  public Deduper deduper() {
    return new Deduper(this);
  }

  /** 요청 하나의 수명 동안만 쓰는 호출 메모. 열쇠는 (엔드포인트 이름, 파라미터) 처럼 호출을 유일하게 가리키는 값이어야 한다. */
  public static final class Deduper {

    private final StockAsyncSupport async;
    private final Map<Object, CompletableFuture<?>> started = new ConcurrentHashMap<>();

    private Deduper(StockAsyncSupport async) {
      this.async = async;
    }

    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> supply(Object key, Supplier<T> supplier) {
      return (CompletableFuture<T>) started.computeIfAbsent(key, ignored -> async.supply(supplier));
    }

    /** 던진 서로 다른 호출 수(테스트·측정용). */
    public int startedCount() {
      return started.size();
    }
  }
}
