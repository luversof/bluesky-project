package net.luversof.web.gate.stock.support;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
}
