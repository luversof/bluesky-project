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
 * 금액 숨김(html.hide-amounts)은 시뮬레이터에도 통한다 - 차트 캔버스는 {@code amount-chart}, 스크립트가 그리는 금액 셀은 {@code
 * amount-value}.
 *
 * <p>실측 2026-09-09: 숨김 상태에서 흐려지지 않은 금액 문구 350개 중 340여 개가 인출 시뮬레이터(표 셀·요약 칩·입력 미리보기)였고, 캔버스 15개 중
 * 3개(시뮬레이터 2, 복리 1)가 amount-chart 가 없어 축 금액이 보였다. 다른 화면은 전부 가려지고 있었다.
 */
class SimulatorAmountMaskTest {

  private static final Path JTE = Path.of("src/main/jte/stock");
  private static final Path SIM_TS = Path.of("src/main/frontend/src/stock/stockSimulator.ts");
  private static final Pattern CANVAS = Pattern.compile("<canvas\\b[^>]*>");
  private static final Pattern UNMASKED_TD =
      Pattern.compile("<td(?![^>]*amount-value)[^>]*>\\$\\{formatCurrency\\(");

  @Test
  void 모든_차트_캔버스는_amount_chart_로_가려진다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int canvases = 0;
    try (Stream<Path> walk = Files.walk(JTE)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        Matcher m = CANVAS.matcher(Files.readString(p, StandardCharsets.UTF_8));
        while (m.find()) {
          canvases++;
          if (!m.group().contains("amount-chart"))
            offenders.add(JTE.relativize(p) + ": " + m.group());
        }
      }
    }
    assertThat(canvases).isGreaterThanOrEqualTo(15);
    assertThat(offenders).as("금액 숨김에서 축 금액이 그대로 보이는 캔버스").isEmpty();
  }

  @Test
  void 인출_시뮬레이터_스크립트의_금액_셀은_amount_value_다() throws IOException {
    String src = Files.readString(SIM_TS, StandardCharsets.UTF_8);
    Matcher m = UNMASKED_TD.matcher(src);
    List<String> offenders = new ArrayList<>();
    while (m.find()) offenders.add(m.group());
    assertThat(src).contains("<td class=\"amount-value\">${formatCurrency(");
    assertThat(offenders).as("금액 숨김에서 그대로 보이는 금액 셀").isEmpty();
    assertThat(src)
        .as("금액이 든 시나리오 칩도 가린다")
        .contains("segment.includes(\"\\u20a9\") ? \" amount-value\"");
  }
}
