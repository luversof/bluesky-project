package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 백분율은 <b>100 을 먼저 곱한 뒤</b> 나눈다 - 순서를 바꾸면 표시 자릿수보다 굵게 미리 반올림된다.
 *
 * <p>2026-09-12 까지 {@code StockPortfolioHtmxController.percentage} 는 {@code divide(base,
 * 4).multiply(100)} 이라 백분율이 <b>소수 2 자리</b>로 잘린 뒤 화면이 다시 1 자리로 반올림했다.
 *
 * <p>실측 2026-09-12(자산 현황, 계좌별 보유 종목 상세 37 행): 연금저축2 의 TIGER 리츠부동산인프라 가 8,847,600 / 1,622,109,770 =
 * <b>0.5454%</b> 인데 0.0055 x 100 = 0.55% 를 거쳐 <b>0.6%</b> 로 나갔다. 나머지 36 행은 잔차가 0.x5 경계에 닿지 않아 우연히
 * 맞았다.
 *
 * <p>{@code StockDividendHtmxController.percentage} 는 처음부터 곱하기가 먼저였다.
 */
class PortfolioPercentageOrderTest {

  /** 실제로 걸렸던 값으로 두 순서를 견준다. */
  @Test
  void 곱하기를_먼저_해야_표시값이_맞는다() {
    BigDecimal amount = new BigDecimal("8847600");
    BigDecimal base = new BigDecimal("1622109770");

    BigDecimal wrong =
        amount.divide(base, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    BigDecimal right =
        amount.multiply(BigDecimal.valueOf(100)).divide(base, 4, RoundingMode.HALF_UP);

    assertThat(wrong.setScale(1, RoundingMode.HALF_UP)).isEqualByComparingTo("0.6");
    assertThat(right.setScale(1, RoundingMode.HALF_UP))
        .as("0.5454% 는 0.5% 로 보여야 한다")
        .isEqualByComparingTo("0.5");
  }

  @Test
  void 컨트롤러가_곱하기를_먼저_한다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockPortfolioHtmxController.java"),
            StandardCharsets.UTF_8);
    String flat = flatten(source);
    assertThat(flat)
        .as("표시 자릿수보다 굵게 미리 반올림하면 안 된다")
        .contains(
            "return amount.multiply(BigDecimal.valueOf(100)).divide(base, 4, java.math.RoundingMode.HALF_UP);")
        .doesNotContain(
            "amount.divide(base, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))");
  }

  /**
   * 같은 순서 실수가 {@code StockAssetStatusUtil.ratePctScaled} 에도 있었다.
   *
   * <p>이쪽은 실측 2026-09-12(자산 현황 손익률 32 행) 기준으로 어긋난 줄이 없었다 - 잔차가 0.x5 경계에 닿지 않았을 뿐이라 자료가 바뀌면 같은 식으로
   * 틀린다. 세 곳의 순서를 함께 고정한다.
   */
  @Test
  void 자산현황_유틸도_곱하기를_먼저_한다() throws IOException {
    String source =
        Files.readString(
            Path.of("src/main/java/net/luversof/web/gate/stock/util/StockAssetStatusUtil.java"),
            StandardCharsets.UTF_8);
    String flat = flatten(source);
    assertThat(flat)
        .contains(
            "return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 4, RoundingMode.HALF_UP);")
        .doesNotContain(
            "numerator.divide(denominator, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))");
  }

  /** 형제 컨트롤러도 같은 순서를 지켜야 한다 - 두 화면이 같은 비율을 다르게 적으면 안 된다. */
  @Test
  void 배당_컨트롤러도_같은_순서다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockDividendHtmxController.java"),
            StandardCharsets.UTF_8);
    assertThat(flatten(source))
        .contains(
            "return amount.multiply(BigDecimal.valueOf(100)).divide(principal, 4, RoundingMode.HALF_UP);");
  }

  private String flatten(String source) {
    StringBuilder out = new StringBuilder();
    boolean lastWasSpace = false;
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (Character.isWhitespace(c)) {
        if (!lastWasSpace) {
          out.append(' ');
        }
        lastWasSpace = true;
      } else {
        out.append(c);
        lastWasSpace = false;
      }
    }
    return out.toString();
  }
}
