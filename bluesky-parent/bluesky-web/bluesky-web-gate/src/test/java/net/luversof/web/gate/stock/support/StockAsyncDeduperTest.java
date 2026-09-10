package net.luversof.web.gate.stock.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

/**
 * 한 요청 안에서 같은 백엔드 호출이 두 번 나가지 않는다.
 *
 * <p>실측 2026-09-10(로깅 프록시 + qa/iso-fragment.cjs): 조회 기간을 고르지 않은 기본 진입에서 종목 상세 조각이 {@code
 * /api/tradeProfit/calculateProfit?userId=…} 을 <b>똑같은 URL 로 두 번</b> 불렀다(계좌 상세도 같음). '기간 손익' 과 '전체
 * 스냅샷' 의 파라미터가 그때는 완전히 같아지기 때문이다. 기간을 고르면 두 호출은 서로 다르므로 그대로 둘 다 나가야 한다.
 */
class StockAsyncDeduperTest {

  private static StockAsyncSupport support() {
    return new StockAsyncSupport(Executors.newFixedThreadPool(4));
  }

  private static LinkedMultiValueMap<String, String> params(String... pairs) {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    for (int i = 0; i < pairs.length; i += 2) map.add(pairs[i], pairs[i + 1]);
    return map;
  }

  @Test
  void 같은_열쇠는_한_번만_던지고_같은_결과를_돌려준다() {
    StockAsyncSupport.Deduper calls = support().deduper();
    AtomicInteger fired = new AtomicInteger();
    var sameParams = params("userId", "u1");
    var alsoSame = params("userId", "u1");

    var first = calls.supply(List.of("calculateProfit", sameParams), () -> fired.incrementAndGet());
    var second = calls.supply(List.of("calculateProfit", alsoSame), () -> fired.incrementAndGet());

    assertThat(StockAsyncSupport.join(first)).isEqualTo(1);
    assertThat(StockAsyncSupport.join(second)).as("두 번째는 이미 던진 것을 그대로 쓴다").isEqualTo(1);
    assertThat(fired).hasValue(1);
    assertThat(calls.startedCount()).isEqualTo(1);
  }

  @Test
  void 기간이_다르면_두_호출_모두_나간다() {
    StockAsyncSupport.Deduper calls = support().deduper();
    AtomicInteger fired = new AtomicInteger();
    var periodParams = params("userId", "u1", "startDate", "2026-06-11T00:00:00Z");
    var snapshotParams = params("userId", "u1");

    StockAsyncSupport.join(
        calls.supply(List.of("calculateProfit", periodParams), () -> fired.incrementAndGet()));
    StockAsyncSupport.join(
        calls.supply(List.of("calculateProfit", snapshotParams), () -> fired.incrementAndGet()));

    assertThat(fired).as("기간 손익과 전체 스냅샷은 서로 다른 호출이다").hasValue(2);
    assertThat(calls.startedCount()).isEqualTo(2);
  }

  @Test
  void 엔드포인트가_다르면_파라미터가_같아도_따로_던진다() {
    StockAsyncSupport.Deduper calls = support().deduper();
    AtomicInteger fired = new AtomicInteger();
    var same = params("userId", "u1");

    StockAsyncSupport.join(
        calls.supply(List.of("calculateProfit", same), () -> fired.incrementAndGet()));
    StockAsyncSupport.join(
        calls.supply(List.of("timeSeries", same), () -> fired.incrementAndGet()));

    assertThat(fired).hasValue(2);
  }

  @Test
  void 요청마다_새_메모라_다른_요청의_결과를_쓰지_않는다() {
    StockAsyncSupport async = support();
    AtomicInteger fired = new AtomicInteger();
    var key = List.of("calculateProfit", params("userId", "u1"));

    StockAsyncSupport.join(async.deduper().supply(key, () -> fired.incrementAndGet()));
    StockAsyncSupport.join(async.deduper().supply(key, () -> fired.incrementAndGet()));

    assertThat(fired).as("요청이 다르면 다시 던진다(응답을 요청 간에 재사용하지 않는다)").hasValue(2);
  }

  @Test
  void 상세_컨트롤러는_두_손익_호출을_메모를_거쳐_던진다() throws IOException {
    String src =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java"),
            StandardCharsets.UTF_8);
    assertThat(src).doesNotContain("stockAsync.supply(() -> tradeProfitClient.calculateProfit(");
    assertThat(src).contains("var calls = stockAsync.deduper();");
    assertThat(src).contains("var accCalls = stockAsync.deduper();");
  }
}
