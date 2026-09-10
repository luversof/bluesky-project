package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 차트 보정용 {@code resize()} 는 {@code StockCharts.resizeIfChanged} 를 거친다(크기가 실제로 달라졌을 때만).
 *
 * <p>실측 2026-09-10(qa/chart-resize-trace.cjs): Chart.js 4.5 retinaScale 은 컨테이너 폭을 0.1 단위로
 * 반올림(570.4)해 정수 canvas.width(570)와 비교하므로 소수 폭에선 resize() 마다 전부 다시 그렸다. htmx 교체 뒤 빈 차트 보정용
 * setTimeout resize() 7곳(0/60/200/250/500ms)이 매번 전체 재렌더(배당 780점 ~100ms, 종목 주가 3,198점 ~256ms @4x
 * CPU)였다. 동작은 chartResizeGuard.test.mjs 가 검증하고, 여기서는 맨 resize() 호출이 다시 생기지 않는지만 지킨다.
 */
class ChartResizeGuardTest {

  private static final Pattern BARE_RESIZE =
      Pattern.compile("\\b(?!window\\.)([A-Za-z_$][\\w$.]*)\\.resize\\(\\)");

  @Test
  void 주식_템플릿과_스크립트에_맨_resize_호출이_없다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int guarded = 0;
    for (Path root :
        List.of(Path.of("src/main/jte/stock"), Path.of("src/main/frontend/src/stock"))) {
      try (Stream<Path> walk = Files.walk(root)) {
        for (Path p :
            walk.filter(x -> x.toString().endsWith(".jte") || x.toString().endsWith(".ts"))
                .toList()) {
          for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            String code = line.replaceAll("//.*$", "");
            if (code.contains("resizeIfChanged(")) guarded++;
            if (code.contains("resizeIfChanged")) continue; // 폴백 분기(else c.resize())는 헬퍼가 없을 때만 돈다
            Matcher m = BARE_RESIZE.matcher(code);
            while (m.find()) offenders.add(p.getFileName() + ": " + m.group());
          }
        }
      }
    }
    assertThat(guarded).as("헬퍼 호출을 하나도 못 찾았다").isGreaterThanOrEqualTo(7);
    assertThat(offenders).as("소수 폭 컨테이너에선 맨 resize() 가 매번 전체 재렌더다").isEmpty();
  }

  @Test
  void 헬퍼는_stock_charts_가_정의하고_API_에_붙인다() throws IOException {
    String ts =
        Files.readString(Path.of("src/main/frontend/src/stock-charts.ts"), StandardCharsets.UTF_8);
    assertThat(ts).contains("function chartSizeChanged(chart: any): boolean");
    assertThat(ts).contains("StockCharts.resizeIfChanged = resizeIfChanged;");
    assertThat(ts).contains("Math.floor(parent.clientWidth) !== Math.floor(chart.width || 0)");
  }
}
