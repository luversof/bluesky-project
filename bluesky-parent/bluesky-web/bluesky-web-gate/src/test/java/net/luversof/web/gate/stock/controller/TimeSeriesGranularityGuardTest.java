package net.luversof.web.gate.stock.controller;

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
 * 시계열을 부르는 자리가 {@code granularity} 를 반드시 실어 보내는지 본다.
 *
 * <p>{@code granularity} 는 {@code TradeProfitRequest} 의 필드가 아니라 {@code toParams()} 에 실리지 않는다. 호출부가
 * 직접 {@code params.add("granularity", ...)} 를 해야 하고, <b>빼먹어도 오류가 나지 않는다</b> &mdash; 백엔드가 조용히 일별로
 * 답한다.
 *
 * <p>실측 2026-08-24(이 사용자의 전 기간, api-stock 직접 호출):
 *
 * <pre>
 *   granularity 없음  -&gt; 6,167 점 · 1,592,715 바이트 · p50 110ms
 *   granularity=AUTO -&gt;   205 점 ·    61,399 바이트 · p50  40ms
 * </pre>
 *
 * <p>전송량 자체는 gzip 이 흡수한다(1,584,142 -&gt; 59,181 바이트, {@code BackendCompressionTest} 가 게이트가 실제로 gzip
 * 을 협상함을 확인한다). 남는 비용은 <b>서버가 30 배 많은 점을 만들고 직렬화하고 게이트가 다시 파싱하는 것</b>이다 &mdash; 위 p50 이 그 값이다.
 *
 * <p>실측 2026-08-24: 게이트의 시계열 호출은 6 곳이고 모두 명시하고 있다(AUTO 3 · MONTHLY 1 · DAILY 1, 그리고 값을 받아 넘기는 통과
 * 엔드포인트 1). 새 호출부가 하나 빠지는 것을 막는다.
 */
class TimeSeriesGranularityGuardTest {

  private static final Path CONTROLLER_DIR =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller");

  private static final List<String> TIME_SERIES_CALLS =
      List.of("tradeProfitClient.timeSeries(", "tradeProfitClient.timeSeriesWithSummary(");

  /** 호출 한 번의 인자 전체를 괄호 짝을 맞춰 잘라 낸다. */
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

  /**
   * 넘긴 파라미터 변수에 granularity 가 실렸는지.
   *
   * <p>같은 파일 안에서 {@code <변수>.add("granularity"} 를 찾는다. 통과 엔드포인트처럼 조건부로 싣는 자리도 그 문장은 있으므로 통과한다
   * &mdash; 그 자리는 값을 만들어 내는 곳이 아니라 받은 값을 넘기는 곳이다.
   */
  private boolean carriesGranularity(String source, String arguments) {
    String variable = arguments.trim();
    if (variable.isEmpty()) {
      return false;
    }
    return source.contains(variable + ".add(\"granularity\"");
  }

  private record Call(String file, int line, String arguments, boolean carries) {}

  private List<Call> calls() throws IOException {
    List<Call> found = new ArrayList<>();
    try (Stream<Path> files = Files.list(CONTROLLER_DIR)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        for (String call : TIME_SERIES_CALLS) {
          int at = source.indexOf(call);
          while (at >= 0) {
            String arguments = argumentsOf(source, at + call.length() - 1);
            int line = source.substring(0, at).split("\n", -1).length;
            found.add(
                new Call(
                    file.getFileName().toString(),
                    line,
                    arguments,
                    carriesGranularity(source, arguments)));
            at = source.indexOf(call, at + 1);
          }
        }
      }
    }
    return found;
  }

  @Test
  void 시계열_호출은_모두_granularity_를_싣는다() throws IOException {
    List<String> offenders =
        calls().stream()
            .filter(call -> !call.carries())
            .map(call -> call.file() + ":" + call.line() + " (" + call.arguments() + ")")
            .toList();

    assertThat(offenders)
        .as(
            "granularity 를 빼면 백엔드가 조용히 일별로 답한다"
                + " (실측: 205 점 40ms -> 6,167 점 110ms). params.add(\"granularity\", ...) 를 넣을 것")
        .isEmpty();
  }

  /** 검사가 실제로 호출을 훑는지. 하나도 못 찾으면 위 검사는 늘 통과한다. */
  @Test
  void 검사가_실제로_시계열_호출을_훑는다() throws IOException {
    // 실측 2026-08-24: 6 곳.
    assertThat(calls()).as("시계열 호출을 하나도 찾지 못했다").hasSizeGreaterThanOrEqualTo(5);
  }
}
