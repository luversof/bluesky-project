package net.luversof.web.gate.stock.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

/**
 * 병렬 조회 도우미가 <b>실패를 감추지 않는지</b> 고정한다.
 *
 * <p>주식 화면은 서로 의존이 없는 api-stock 호출을 이 도우미로 동시에 던진다. 그런데 {@link CompletableFuture#join()} 은 원래 예외를
 * {@link CompletionException} 으로 감싼다. 감싼 채로 나가면 컨트롤러의 {@code catch (BlueskyException)} 이 빗나가고, 백엔드가
 * 돌려준 사용자용 메시지({@code isDisplayableMessage})를 꺼내는 경로도 타지 못한다 &mdash; 사용자는 원인 대신 일반 오류를 보게 된다.
 *
 * <p>그래서 {@code join} 은 껍데기를 벗겨 <b>순차 호출과 똑같은 예외</b>가 나가게 한다. 이 성질에 화면의 오류 표시 전체가 걸려 있는데 지금까지 테스트가
 * 없었다.
 */
class StockAsyncSupportTest {

  @Test
  void 정상이면_값을_그대로_돌려준다() {
    assertThat(StockAsyncSupport.join(CompletableFuture.completedFuture("ok"))).isEqualTo("ok");
  }

  /** 껍데기를 벗겨 원래 예외 '그 인스턴스'가 나가야 한다. 타입만 같아서는 원인 메시지가 바뀔 수 있다. */
  @Test
  void 런타임_예외는_감싸지_않고_그대로_던진다() {
    var cause = new IllegalStateException("backend down");
    CompletableFuture<String> future = new CompletableFuture<>();
    future.completeExceptionally(cause);

    assertThatThrownBy(() -> StockAsyncSupport.join(future))
        .as("CompletionException 으로 감싸 나가면 컨트롤러의 예외별 처리가 전부 빗나간다")
        .isSameAs(cause);
  }

  @Test
  void Error_도_그대로_던진다() {
    var cause = new StackOverflowError("deep");
    CompletableFuture<String> future = new CompletableFuture<>();
    future.completeExceptionally(cause);

    assertThatThrownBy(() -> StockAsyncSupport.join(future)).isSameAs(cause);
  }

  /** 런타임도 Error 도 아닌 원인은 벗길 수 없다. 그때는 감싼 예외를 그대로 올린다(삼켜서는 안 된다). */
  @Test
  void 그_밖의_원인은_감싼_채로라도_올린다() {
    var cause = new Exception("checked");
    CompletableFuture<String> future = new CompletableFuture<>();
    future.completeExceptionally(cause);

    assertThatThrownBy(() -> StockAsyncSupport.join(future))
        .isInstanceOf(CompletionException.class)
        .hasCause(cause);
  }

  /** supply 는 주입된 실행기에서 돌아야 한다. 공용 풀로 새면 SecurityContext 전파가 깨진다. */
  @Test
  void supply_는_주입된_실행기에서_돈다() throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "stock-probe"));
    try {
      var support = new StockAsyncSupport(executor);
      AtomicReference<String> threadName = new AtomicReference<>();
      var future =
          support.supply(
              () -> {
                threadName.set(Thread.currentThread().getName());
                return "done";
              });

      assertThat(StockAsyncSupport.join(future)).isEqualTo("done");
      assertThat(threadName.get())
          .as("공용 ForkJoinPool 에서 돌면 인터셉터가 토큰을 못 찾아 Authorization 헤더가 조용히 빠진다")
          .isEqualTo("stock-probe");
    } finally {
      executor.shutdownNow();
    }
  }

  /**
   * 껍데기를 벗기는 규칙이 한 곳에만 있는지.
   *
   * <p>예전에는 같은 try/catch 가 요약 컨트롤러와 자산성장 컨트롤러에도 복사돼 있었다. 세 벌이 우연히 같았지만, 이 저장소에서 같은 공식이 여러 곳에 있으면
   * 한쪽만 고쳐져 갈라진 사례가 반복됐다(활동 묶기 3벌, 창 계산 3벌, 매도원가 2곳).
   */
  @Test
  void 껍데기를_벗기는_규칙은_한_곳에만_있다() throws java.io.IOException {
    java.nio.file.Path root = java.nio.file.Path.of("src/main/java/net/luversof/web/gate/stock");
    int copies = 0;
    try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(root)) {
      for (java.nio.file.Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source =
            java.nio.file.Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
        copies += source.split("instanceof RuntimeException runtimeException", -1).length - 1;
      }
    }
    assertThat(copies)
        .as("CompletionException 을 벗기는 코드는 StockAsyncSupport.join 에만 있어야 한다")
        .isEqualTo(1);
  }
}
