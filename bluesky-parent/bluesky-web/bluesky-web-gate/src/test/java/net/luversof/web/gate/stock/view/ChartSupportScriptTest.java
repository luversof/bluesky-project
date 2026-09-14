package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Chart.js 를 직접 받는 화면은 {@code /js/stock-charts.js} 도 함께 받아야 한다.
 *
 * <p>그 파일이 차트의 <b>테마 색</b>과 <b>애니메이션 기본값</b>을 정한다. 시뮬레이터는 성능을 위해 {@code chart.umd.min.js} 만 직접 받고
 * 있었는데, 그래서 그 화면 차트만 두 가지가 어긋났다 &mdash; 실측 2026-09-14: 글자색이 라이트·다크 모두 {@code #666}(다크 배경 {@code
 * rgb(15,22,35)} 위에서 거의 안 보였다), 애니메이션도 다른 화면 600ms 와 달리 Chart.js 기본값 1000ms 였다.
 *
 * <p>여기서는 마크업을 읽어 못박는다 &mdash; 렌더 테스트는 화면마다 모델을 갖춰야 하는데, 이 검사는 "빠뜨리지 않았나" 하나만 보면 되기 때문이다.
 */
class ChartSupportScriptTest {

  private static final Path JTE_ROOT = Path.of("src/main/jte");
  private static final String VENDOR = "/js/vendor/chart.umd.min.js";
  private static final String SUPPORT = "/js/stock-charts.js";

  private static int count(String source, String needle) {
    int found = 0;
    for (int at = source.indexOf(needle);
        at >= 0;
        at = source.indexOf(needle, at + needle.length())) {
      found++;
    }
    return found;
  }

  @Test
  void Chart_를_직접_받는_화면은_stock_charts_도_받는다() throws IOException {
    List<String> missing = new ArrayList<>();
    List<String> checked = new ArrayList<>();
    try (Stream<Path> files = Files.walk(JTE_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).sorted().toList()) {
        String source = Files.readString(file);
        if (!source.contains(VENDOR)) {
          continue;
        }
        // 한 파일에 분기가 여럿이면(시뮬레이터는 탭마다 한 벌) 개수로 봐야 한다 - contains 로 보면
        // 한 분기에서 빠뜨려도 다른 분기 덕에 통과한다(이 검사를 그렇게 짰다가 변이를 놓쳤다).
        int vendor = count(source, VENDOR);
        int support = count(source, SUPPORT);
        checked.add(JTE_ROOT.relativize(file).toString() + " x" + vendor);
        if (support < vendor) {
          missing.add(
              JTE_ROOT.relativize(file).toString()
                  + " (chart.umd "
                  + vendor
                  + " vs stock-charts "
                  + support
                  + ")");
        }
      }
    }

    assertThat(checked).as("Chart.js 를 직접 받는 화면을 하나도 못 찾았다 - 검사가 무력해진다").isNotEmpty();
    assertThat(missing).as("이 화면들의 차트만 테마를 안 따르고 애니메이션 길이도 달라진다").isEmpty();
  }
}
