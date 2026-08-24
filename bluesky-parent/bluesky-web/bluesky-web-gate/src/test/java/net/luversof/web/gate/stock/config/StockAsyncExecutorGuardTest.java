package net.luversof.web.gate.stock.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 화면을 그리며 던지는 동시 호출이 반드시 보안 컨텍스트를 옮기는 실행기를 쓰는지 본다.
 *
 * <p>api-stock 호출에 붙는 Authorization 헤더는 {@code GateHttpExchangeConfig} 의 RestClient 인터셉터가 {@code
 * SecurityContextHolder}(ThreadLocal)에서 꺼내 붙인다. 그래서 다른 스레드에서 호출하면 컨텍스트가 따라가지 않는다.
 *
 * <p>그 대비가 {@code GateStockConfig.stockRemoteCallExecutor} 다 &mdash; 가상 스레드 실행기를 {@code
 * DelegatingSecurityContextExecutorService} 로 감싸 둔다. 감싸지 않거나, 아예 실행기를 넘기지 않으면 {@code
 * CompletableFuture.supplyAsync} 는 공용 ForkJoinPool 에서 돌고 <b>헤더가 조용히 빠진다</b>.
 *
 * <p>조용하다는 것이 문제다. 호출은 그대로 200 으로 성공한다 &mdash; api-stock 이 토큰 없이도 응답하기 때문이다(배포 점검 스크립트가 매번 INFO 로
 * 알려 준다). 그래서 화면은 멀쩡해 보이고, 인가가 빠졌다는 사실만 사라진다.
 *
 * <p>실측 2026-08-23: 이 모듈의 {@code supplyAsync} 호출 9 곳은 모두 실행기를 넘기고 있다. 1 인자 오버로드도 그냥 컴파일되므로 새 호출이 하나
 * 빠지는 것을 막는다.
 */
class StockAsyncExecutorGuardTest {

  private static final Path STOCK_SOURCE = Path.of("src/main/java/net/luversof/web/gate/stock");

  private static final List<String> ASYNC_CALLS = List.of("supplyAsync(", "runAsync(");

  /** {@code supplyAsync(...)} 한 번의 인자 전체를 괄호 짝을 맞춰 잘라 낸다. */
  private String argumentsOf(String source, int callAt) {
    int open = source.indexOf('(', callAt);
    int depth = 1;
    int at = open + 1;
    while (at < source.length() && depth > 0) {
      char c = source.charAt(at++);
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
      }
    }
    return source.substring(open + 1, Math.max(open + 1, at - 1));
  }

  private List<String> callsWithoutExecutor() throws IOException {
    List<String> offenders = new ArrayList<>();
    try (Stream<Path> files = Files.walk(STOCK_SOURCE)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        for (String call : ASYNC_CALLS) {
          int at = source.indexOf(call);
          while (at >= 0) {
            String arguments = argumentsOf(source, at + call.length() - 1);
            // 실행기를 넘기는 형태거나, 실행기를 파라미터로 받는 헬퍼 자신이면 통과.
            boolean passesExecutor =
                arguments.contains("stockRemoteCallExecutor") || arguments.contains("executor");
            if (!passesExecutor) {
              int line = source.substring(0, at).split("\n", -1).length;
              offenders.add(file.getFileName() + ":" + line);
            }
            at = source.indexOf(call, at + 1);
          }
        }
      }
    }
    return offenders;
  }

  @Test
  void 동시_호출은_보안_컨텍스트를_옮기는_실행기를_쓴다() throws IOException {
    assertThat(callsWithoutExecutor())
        .as(
            "실행기를 넘기지 않으면 공용 ForkJoinPool 에서 돌아 Authorization 헤더가 조용히 빠진다."
                + " stockRemoteCallExecutor 를 함께 넘길 것")
        .isEmpty();
  }

  /** 검사가 실제로 훑고 있는지. 호출을 하나도 못 찾으면 위 검사는 늘 통과한다. */
  @Test
  void 검사가_실제로_동시_호출을_훑는다() throws IOException {
    int found = 0;
    try (Stream<Path> files = Files.walk(STOCK_SOURCE)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        for (String call : ASYNC_CALLS) {
          int at = source.indexOf(call);
          while (at >= 0) {
            found++;
            at = source.indexOf(call, at + 1);
          }
        }
      }
    }
    // 실측 2026-08-23: 9 곳.
    assertThat(found).as("동시 호출을 하나도 찾지 못했다").isGreaterThanOrEqualTo(5);
  }

  /**
   * 실행기 자체가 보안 컨텍스트를 옮기는지.
   *
   * <p>호출부가 전부 실행기를 넘겨도, 그 실행기가 감싸여 있지 않으면 같은 결과가 된다.
   */
  @Test
  void 실행기가_보안_컨텍스트를_옮긴다() throws IOException {
    String source =
        Files.readString(
            Path.of("src/main/java/net/luversof/web/gate/stock/config/GateStockConfig.java"),
            StandardCharsets.UTF_8);
    int at = source.indexOf("ExecutorService stockRemoteCallExecutor()");
    assertThat(at).as("실행기 빈을 찾지 못했다 - 검사가 무력해진다").isGreaterThan(0);
    String body = source.substring(at, source.indexOf('}', at));
    assertThat(body)
        .as("실행기를 DelegatingSecurityContextExecutorService 로 감싸지 않으면 헤더가 조용히 빠진다")
        .contains("DelegatingSecurityContextExecutorService");
  }
}
