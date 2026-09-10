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

import net.luversof.web.gate.stock.util.StockFormatUtil;

/**
 * 비율 표기는 "-0.0%" 를 내지 않는다.
 *
 * <p>실측 2026-09-10: {@code String.format("%+.1f%%", -0.04)} 는 "-0.0%", {@code "%.2f%%"} 의 -0.004 는
 * "-0.00%". 2년 평가액 시계열의 프리셋 구간 3,978개 중 16개(하루 구간 위주)가 이 범위라 자산 성장 기간 수익률에 실제로 찍힐 수 있었다. 주식 템플릿의 비율
 * String.format 40곳을 {@link StockFormatUtil#pct}/{@link StockFormatUtil#signedPct} 로 모았다.
 */
class PercentNegativeZeroTest {

  @Test
  void 반올림_결과가_영이면_부호를_지운다() {
    assertThat(StockFormatUtil.signedPct(-0.04, 1)).isEqualTo("+0.0%");
    assertThat(StockFormatUtil.pct(-0.04, 1)).isEqualTo("0.0%");
    assertThat(StockFormatUtil.pct(-0.004, 2)).isEqualTo("0.00%");
    assertThat(StockFormatUtil.signedPct(-0.0, 2)).isEqualTo("+0.00%");
  }

  @Test
  void 영이_아닌_값은_그대로_HALF_UP_이다() {
    assertThat(StockFormatUtil.signedPct(-0.05, 1)).isEqualTo("-0.1%");
    assertThat(StockFormatUtil.signedPct(1.25, 1)).isEqualTo("+1.3%");
    assertThat(StockFormatUtil.signedPct(-8.914, 2)).isEqualTo("-8.91%");
    assertThat(StockFormatUtil.pct(12.345, 2)).isEqualTo("12.35%");
    assertThat(StockFormatUtil.signedPct(Double.NaN, 1)).isEqualTo("NaN%");
  }

  @Test
  void 주식_템플릿에_비율_String_format_이_남아_있지_않다() throws IOException {
    Pattern raw = Pattern.compile("String\\.format\\(\"%\\+?\\.[0-9]f%%\"");
    List<String> offenders = new ArrayList<>();
    int helperCalls = 0;
    try (Stream<Path> walk = Files.walk(Path.of("src/main/jte/stock"))) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String html = Files.readString(p, StandardCharsets.UTF_8);
        Matcher m = raw.matcher(html);
        while (m.find()) offenders.add(p.getFileName() + ": " + m.group());
        Matcher h = Pattern.compile("StockFormatUtil\\.(?:signedPct|pct)\\(").matcher(html);
        while (h.find()) helperCalls++;
      }
    }
    assertThat(helperCalls).as("헬퍼 호출을 하나도 못 찾았다").isGreaterThanOrEqualTo(40);
    assertThat(offenders).as("음의 영을 낼 수 있는 비율 포맷").isEmpty();
  }
}
