package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.service.MonthlyDividendCalculator;

/**
 * 원 표기는 한 반올림(HALF_UP = {@link StockFormatUtil#displayWon})으로 한다(2026-09-24).
 *
 * <p>템플릿마다 {@code new DecimalFormat("#,##0")} 을 만들어 썼는데 그 기본 반올림은 HALF_EVEN 이다. 합계 · 연 값은
 * displayWon(HALF_UP)으로 내므로 x.5 원에서 같은 값이 1 원 갈렸다 &mdash; 시뮬레이터 월배당은 주당 260.5 원 x 홀수 주 같은 행이 흔해,
 * 카드의 월 값 x 12 가 연 값과 12 원 어긋날 수 있었다(코드 주석이 막으려던 바로 그 불일치).
 */
class WonFormatRoundingTest {

  @Test
  void 반_원에서_displayWon_과_같게_올린다() {
    for (String value :
        List.of(
            "2.5",
            "258054.5",
            "258055.5",
            "0.5",
            "1234566.5",
            "-2.5",
            "-258054.5",
            "10.49",
            "10.51")) {
      BigDecimal amount = new BigDecimal(value);
      String expected =
          new java.text.DecimalFormat("#,##0").format(StockFormatUtil.displayWon(amount));
      assertThat(StockFormatUtil.wonFormat().format(amount)).as(value).isEqualTo(expected);
    }
    assertThat(StockFormatUtil.wonFormat().format(new BigDecimal("258054.5"))).isEqualTo("258,055");
  }

  @Test
  void 템플릿은_기본_반올림_원_서식을_만들지_않는다() throws IOException {
    List<String> offenders;
    try (Stream<Path> files = Files.walk(Path.of("src/main/jte"))) {
      offenders =
          files
              .filter(path -> path.toString().endsWith(".jte"))
              .filter(
                  path -> {
                    try {
                      String squashed =
                          Files.readString(path, StandardCharsets.UTF_8).replaceAll("\\s+", "");
                      return squashed.contains("DecimalFormat(\"#,##0\")");
                    } catch (IOException ex) {
                      throw new java.io.UncheckedIOException(ex);
                    }
                  })
              .map(Path::toString)
              .toList();
    }
    assertThat(offenders)
        .as("원 표기는 StockFormatUtil.wonFormat() 으로(HALF_EVEN 서식은 x.5 원에서 합계와 1 원 갈린다)")
        .isEmpty();
  }

  @Test
  void 시뮬_월배당_카드의_월_x_12_는_연_값이다() {
    // 월 합계가 정확히 x.5 원이고 정수부가 짝수 - HALF_EVEN 이면 258,054 x 12 로, 연 값(HALF_UP) 과 12 원 어긋나는 자리.
    var row =
        new MonthlyDividendSnapshotResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "498400",
            "x",
            null,
            new BigDecimal("300"),
            new BigDecimal("260.5"),
            new BigDecimal("4.06"),
            991,
            new BigDecimal("17797"),
            new BigDecimal("21030"),
            new BigDecimal("20840730"),
            new BigDecimal("258155.5").subtract(new BigDecimal("101")),
            null,
            null,
            null,
            null,
            new BigDecimal("10474.5"),
            null,
            null,
            null);
    var summary = new MonthlyDividendCalculator().buildSimulatorSummary(List.of(row));

    String monthlyShown =
        StockFormatUtil.wonFormat().format(summary.totalExpectedMonthlyDividend());
    long monthlyTimes12 = Long.parseLong(monthlyShown.replace(",", "")) * 12;
    assertThat(summary.totalExpectedAnnualDividend().longValueExact()).isEqualTo(monthlyTimes12);
    String taxableShown =
        StockFormatUtil.wonFormat().format(summary.totalExpectedTaxableBaseAmount());
    assertThat(summary.totalExpectedAnnualTaxableBaseAmount().longValueExact())
        .isEqualTo(Long.parseLong(taxableShown.replace(",", "")) * 12);
  }
}
