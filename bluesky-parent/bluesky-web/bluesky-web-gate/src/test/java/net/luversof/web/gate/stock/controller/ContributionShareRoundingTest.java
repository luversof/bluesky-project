package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.util.StockFormatUtil;

/**
 * 기여 비중은 표시할 때 한 번만 반올림해야 한다.
 *
 * <p>자산성장 화면의 '종목별 기여' 표는 비중을 <b>두 자리로 먼저 반올림한 뒤</b> 한 자리로 적었다. 중간값이 {@code .x5} 로 떨어지면 마지막 반올림이 그
 * 값의 double 이진표현에 끌려가 위아래로 갈린다.
 *
 * <p>실측 2026-09-11:
 *
 * <pre>
 *   TIGER 리츠부동산인프라  1,957,337 / 1,271,376,178 = 0.153952%  ->  0.15  ->  화면 0.1%  (바르게는 0.2%)
 *   삼성전자(올해)      829,796,539 / 900,512,673 = 92.14703%  ->  92.15  ->  화면 92.2%  (바르게는 92.1%)
 * </pre>
 *
 * <p>같은 파일의 '기타' 행도 같은 식을 쓴다. 다른 화면의 비율은 미리 반올림한 자리와 적는 자리가 같아(둘 다 2 자리) 이 문제가 없다.
 */
class ContributionShareRoundingTest {

  private static final String TEMPLATE =
      "src/main/jte/stock/htmx/fragments/stockContributionTable.jte";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  private int count(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }

  /** 화면이 쓰는 식 그대로: 기여 x 100 / 총기여 를 주어진 자리로 나눈 뒤 한 자리로 적는다. */
  private String shareText(long contribution, long total, int scale) {
    double pct =
        BigDecimal.valueOf(contribution)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), scale, RoundingMode.HALF_UP)
            .doubleValue();
    return StockFormatUtil.pct(pct, 1);
  }

  @Test
  void 두_자리로_미리_반올림하면_틀린다() {
    assertThat(shareText(1_957_337L, 1_271_376_178L, 2)).isEqualTo("0.1%");
    assertThat(shareText(829_796_539L, 900_512_673L, 2)).isEqualTo("92.2%");
  }

  @Test
  void 한_번만_반올림하면_바르다() {
    assertThat(shareText(1_957_337L, 1_271_376_178L, 6)).isEqualTo("0.2%");
    assertThat(shareText(829_796_539L, 900_512_673L, 6)).isEqualTo("92.1%");
  }

  @Test
  void 화면은_미리_반올림하지_않는다() throws IOException {
    String template = read(TEMPLATE);

    assertThat(template)
        .as("표시 자릿수(1)보다 큰 자리로 미리 반올림하면 마지막 반올림이 이진표현에 끌려간다")
        .doesNotContain(".divide(totalContribution, 2, java.math.RoundingMode.HALF_UP)");
    assertThat(count(template, ".divide(totalContribution, 6, java.math.RoundingMode.HALF_UP)"))
        .as("종목 행과 '기타' 행 두 곳")
        .isEqualTo(2);
  }
}
