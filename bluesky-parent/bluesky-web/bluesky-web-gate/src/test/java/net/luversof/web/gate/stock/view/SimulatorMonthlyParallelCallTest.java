package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 시뮬레이터 월배당 탭의 원격 호출은 서로 의존이 없으면 먼저 다 던진다.
 *
 * <p>실측 2026-09-23: 입력 폼의 종목 목록(월배당 태그 조회)만 맨 끝에서 따로 불러, 카탈로그가 끝난 뒤(+30~37ms)에야 출발했다. 그래서 이 조회가
 * 메서드의 첫 join 보다 앞에서 던져지는지, 동기로 한 번 더 부르지 않는지를 본다. 공백을 전부 지워 비교한다.
 */
class SimulatorMonthlyParallelCallTest {

  private static final Path CONTROLLER =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java");

  private static String squash(String text) {
    return text.replaceAll("[\\s]+", "");
  }

  private static int count(String text, String needle) {
    int found = 0;
    for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + needle.length())) {
      found++;
    }
    return found;
  }

  @Test
  void 태그_종목_조회도_첫_join_전에_던진다() throws IOException {
    String source = squash(Files.readString(CONTROLLER, StandardCharsets.UTF_8));
    int method = source.indexOf("privatevoidpopulateMonthlyDividendModel(");
    assertThat(method).as("월배당 탭 모델 메서드가 사라졌다").isGreaterThan(0);
    int end = source.indexOf("privatevoid", method + 10);
    String body = end > 0 ? source.substring(method, end) : source.substring(method);

    String supply =
        squash("stockAsync.supply(monthlyDividendReferenceSupport::loadMonthlyDividendStockItems)");
    int firstJoin = body.indexOf("StockAsyncSupport.join(");
    assertThat(count(body, supply)).as("태그 종목 조회를 던진다").isEqualTo(1);
    assertThat(firstJoin).as("받는 자리가 없다").isGreaterThan(0);
    assertThat(body.indexOf(supply)).as("첫 join 전에 던진다").isLessThan(firstJoin);
    assertThat(body)
        .as("던진 것을 받아 폼에 싣는다")
        .contains(
            squash(
                "\"stockItems\", net.luversof.web.gate.stock.support.StockAsyncSupport.join("
                    + " monthlyDividendStockItemsFuture)"));
    assertThat(count(source, "loadMonthlyDividendStockItems"))
        .as("동기로 한 번 더 부르면 동시화가 헛돈다")
        .isEqualTo(1);
  }
}
