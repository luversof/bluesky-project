package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 시나리오 비교 설명이 실제 동작과 같아야 한다.
 *
 * <p>2026-09-12 까지 화면은 "<b>둘 다 미소진이면</b> 원금 훼손 시작 시점·마지막 해 지출 커버율·최종 총자산을 기준으로 더 유리한 시나리오를 강조합니다"
 * 라고 적었다. 실제 코드({@code buildBestScenarioSet} / {@code buildComparisonValues})는 다르다:
 *
 * <ul>
 *   <li>배지는 시나리오가 <b>2개 이상이면 늘</b> 붙는다 &mdash; 실측: 둘 다 소진(19년 vs 16년)일 때도, 하나만 소진일 때도 "비교 우위" 가 1개
 *       붙었다.
 *   <li>첫 기준은 설명에 없던 <b>유지 가능 기간</b>(sustainableMonths -&gt; sustainableYears)이다. 둘 다 소진한 사례에서 더 오래
 *       버틴 쪽이 뽑힌 것이 그 증거다.
 * </ul>
 *
 * <p>순서가 실제로 지켜지는 것도 실측했다: 커버율 1,983% / 최종 148.3억 인 쪽이, 커버율 1,454% / 최종 189.5억 인 쪽을 이겼다 &mdash;
 * 커버율이 최종 총자산보다 앞선다.
 */
class ScenarioCompareDescriptionTest {

  private static final String KEY = "stock.simulator.section.compare.desc";

  @Test
  void 설명이_없는_조건을_말하지_않는다() throws IOException {
    String ko = value("uiMessage_ko.properties");
    char bs = (char) 92;
    String misojin = bs + "uBBF8" + bs + "uC18C" + bs + "uC9C4";
    assertThat(ko).as("배지는 둘 다 미소진일 때만 붙는 것이 아니다").doesNotContain(misojin);
  }

  @Test
  void 첫_기준인_유지_가능_기간을_적는다() throws IOException {
    char bs = (char) 92;
    String yuji =
        bs + "uC720" + bs + "uC9C0 " + bs + "uAC00" + bs + "uB2A5 " + bs + "uAE30" + bs + "uAC04";
    assertThat(value("uiMessage_ko.properties")).as("ko").contains(yuji);
    assertThat(value("uiMessage.properties")).as("en").containsIgnoringCase("sustainable period");
  }

  @Test
  void 나머지_기준도_순서대로_남는다() throws IOException {
    String en = value("uiMessage.properties");
    int a = en.toLowerCase().indexOf("sustainable period");
    int b = en.toLowerCase().indexOf("principal drawdown");
    int c = en.toLowerCase().indexOf("spending coverage");
    int d = en.toLowerCase().indexOf("final wealth");
    assertThat(a).as("유지 가능 기간이 먼저").isGreaterThanOrEqualTo(0).isLessThan(b);
    assertThat(b).as("원금 훼손이 커버율보다 먼저").isLessThan(c);
    assertThat(c).as("커버율이 최종 총자산보다 먼저").isLessThan(d);
  }

  private static String value(String bundle) throws IOException {
    String text =
        Files.readString(
            Path.of("src/main/resources").resolve(bundle), StandardCharsets.ISO_8859_1);
    for (String line : text.split(String.valueOf((char) 10))) {
      String row = line.trim();
      if (row.startsWith(KEY) && row.contains("=")) {
        return row.substring(row.indexOf('=') + 1).trim();
      }
    }
    throw new IllegalStateException("키를 찾지 못했다: " + KEY);
  }
}
