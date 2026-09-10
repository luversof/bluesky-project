package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 차트 텍스트 대안 플러그인(common.ts {@code chartSummaryPlugin})은 Chart.js 를 쓰는 모든 진입점에서 등록된다.
 *
 * <p>실측 2026-09-10(qa/chart-alt.cjs): 캔버스 15개 중 13개가 같은 카드 안에 표·목록이 없어 보조기술엔 aria-label(제목)만 닿았다.
 * 시뮬레이터 두 화면은 chart.umd 를 직접 로드해 stock-charts.js 의 등록을 거치지 않으므로 각 모듈이 {@code new Chart} 앞에서 따로 등록해야
 * 한다. 동작은 chartSummary.test.mjs 가 검증하고, 여기서는 등록 지점이 빠지지 않았는지만 지킨다.
 */
class ChartSummaryRegistrationTest {

  private static final Path SRC = Path.of("src/main/frontend/src");
  private static final String PLUGIN_REF = "__chartSummaryInternals?.chartSummaryPlugin";

  @Test
  void 플러그인은_common_ts_가_정의한다() throws IOException {
    String common = Files.readString(SRC.resolve("common.ts"), StandardCharsets.UTF_8);
    assertThat(common)
        .contains(
            "__chartSummaryInternals = { chartSummaryText, syncChartSummary, chartSummaryPlugin }");
    assertThat(common)
        .contains("id: \"a11ySummary\"")
        .contains("afterInit")
        .contains("afterUpdate");
  }

  @Test
  void Chart_를_만드는_모듈은_전부_플러그인을_등록한다() throws IOException {
    for (String rel :
        List.of("stock-charts.ts", "stock/compoundSimulator.ts", "stock/stockSimulator.ts")) {
      String ts = Files.readString(SRC.resolve(rel), StandardCharsets.UTF_8);
      assertThat(ts).as(rel + " 는 new Chart 를 쓴다").contains("new Chart(");
      int reg = ts.indexOf(PLUGIN_REF);
      assertThat(reg).as(rel + " 에 요약 플러그인 등록이 없다").isGreaterThan(0);
      assertThat(ts.indexOf("Chart.register(summaryPlugin)", reg))
          .as(rel + " 는 참조만 하고 등록하지 않는다")
          .isGreaterThan(reg);
    }
  }

  @Test
  void 그_외_모듈은_Chart_를_직접_만들지_않는다() throws IOException {
    try (var walk = Files.walk(SRC)) {
      List<Path> offenders =
          walk.filter(p -> p.toString().endsWith(".ts"))
              .filter(p -> !p.toString().replace('\\', '/').contains("/poe/"))
              .filter(
                  p -> {
                    String rel = SRC.relativize(p).toString().replace('\\', '/');
                    if (List.of(
                            "stock-charts.ts",
                            "stock/compoundSimulator.ts",
                            "stock/stockSimulator.ts")
                        .contains(rel)) return false;
                    try {
                      return Files.readString(p, StandardCharsets.UTF_8).contains("new Chart(");
                    } catch (IOException e) {
                      return false;
                    }
                  })
              .toList();
      assertThat(offenders).as("새 차트 진입점이 생겼으면 요약 플러그인 등록을 추가하고 위 목록에 넣는다").isEmpty();
    }
  }
}
