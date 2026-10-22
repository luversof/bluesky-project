package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.util.StockAmountUtil;

/**
 * 백분율은 <b>100 을 먼저 곱한 뒤</b> 나눈다 - 순서를 바꾸면 표시 자릿수보다 굵게 미리 반올림된다.
 *
 * <p>2026-09-12 까지 {@code StockPortfolioHtmxController.percentage} 는 {@code divide(base,
 * 4).multiply(100)} 이라 백분율이 <b>소수 2 자리</b>로 잘린 뒤 화면이 다시 1 자리로 반올림했다.
 *
 * <p>실측 2026-09-12(자산 현황, 계좌별 보유 종목 상세 37 행): 연금저축2 의 TIGER 리츠부동산인프라 가 8,847,600 / 1,622,109,770 =
 * <b>0.5454%</b> 인데 0.0055 x 100 = 0.55% 를 거쳐 <b>0.6%</b> 로 나갔다. 나머지 36 행은 잔차가 0.x5 경계에 닿지 않아 우연히
 * 맞았다. 같은 순서 실수가 {@code StockAssetStatusUtil.ratePctScaled} 에도 있었다.
 *
 * <p>2026-10-22: 같은 식이 주식 코드 열두 곳에 따로 있던 것을 {@link StockAmountUtil#percentOrNull} 하나로 모았다. 예전엔 세
 * 파일의 소스 글자를 고정했는데, 이제는 ① 공용 도우미의 <b>동작</b>을 실제로 걸렸던 값으로 보고 ② 주식 코드 <b>어디에도</b> "나누고 곱하는" 순서가 없는지
 * 훑는다 &mdash; 새로 생긴 사본이 실수를 되풀이해도 잡힌다.
 */
class PortfolioPercentageOrderTest {

  private static final Path STOCK_MAIN = Path.of("src/main/java/net/luversof/web/gate/stock");

  /** {@code x.divide(y, …)…multiply(BigDecimal.valueOf(100))} - 나눈 결과에 100 을 곱한다(한 문장 안). */
  private static final Pattern DIVIDE_THEN_TIMES_100 =
      Pattern.compile(
          "[.]divide[(][^;]*?[)]\\s*[.]multiply[(](java[.]math[.])?BigDecimal[.]valueOf[(]100[)][)]");

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
  void 공용_도우미가_곱하기를_먼저_한다() {
    BigDecimal pct =
        StockAmountUtil.percentOrNull(new BigDecimal("8847600"), new BigDecimal("1622109770"), 4);
    assertThat(pct).isEqualByComparingTo("0.5454");
    assertThat(pct.setScale(1, RoundingMode.HALF_UP)).isEqualByComparingTo("0.5");
    // 자릿수는 부르는 쪽이 정한다(화면마다 1 · 2 · 4).
    assertThat(StockAmountUtil.percentOrNull(new BigDecimal("1"), new BigDecimal("3"), 1))
        .isEqualByComparingTo("33.3");
    assertThat(StockAmountUtil.percentOrNull(new BigDecimal("-1"), new BigDecimal("8"), 2))
        .isEqualByComparingTo("-12.50");
  }

  @Test
  void 값이_없거나_기준이_0_이하면_null() {
    assertThat(StockAmountUtil.percentOrNull(null, BigDecimal.TEN, 2)).isNull();
    assertThat(StockAmountUtil.percentOrNull(BigDecimal.ONE, null, 2)).isNull();
    assertThat(StockAmountUtil.percentOrNull(BigDecimal.ONE, BigDecimal.ZERO, 2)).isNull();
    assertThat(StockAmountUtil.percentOrNull(BigDecimal.ONE, new BigDecimal("-5"), 2)).isNull();
    // 0 으로 읽는 화면(자산 현황 · 포트폴리오 · 금융소득 기준 · 핵심 보유)은 nz 로 감싼다.
    assertThat(
            StockAmountUtil.nz(StockAmountUtil.percentOrNull(BigDecimal.ONE, BigDecimal.ZERO, 4)))
        .isEqualByComparingTo("0");
  }

  @Test
  void 주식_코드_어디에도_나누고_곱하는_순서가_없다() throws IOException {
    List<Path> sources;
    try (Stream<Path> walk = Files.walk(STOCK_MAIN)) {
      sources = walk.filter(p -> p.toString().endsWith(".java")).toList();
    }
    List<String> offenders = new ArrayList<>();
    for (Path p : sources) {
      Matcher m =
          DIVIDE_THEN_TIMES_100.matcher(flatten(Files.readString(p, StandardCharsets.UTF_8)));
      while (m.find()) {
        offenders.add(p.getFileName() + ": " + m.group());
      }
    }
    // 자가검사: 훑기가 헛돌지 않는다 - 파일을 읽었고, 패턴이 틀린 순서를 실제로 잡는다.
    assertThat(sources).hasSizeGreaterThan(100);
    assertThat(
            DIVIDE_THEN_TIMES_100
                .matcher(
                    "x = amount.divide(base, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));")
                .find())
        .isTrue();
    // 이 패키지는 java.math.BigDecimal 을 완전한 이름으로도 적는다(검토 2026-10-22 - 그 표기를 놓쳤다).
    assertThat(
            DIVIDE_THEN_TIMES_100
                .matcher(
                    "x = a.divide(b, 1, java.math.RoundingMode.HALF_UP).multiply(java.math.BigDecimal.valueOf(100));")
                .find())
        .isTrue();
    assertThat(offenders).as("나눈 뒤 100 을 곱하면 표시 자릿수보다 굵게 미리 반올림된다").isEmpty();
  }

  /**
   * 예전에 고정하던 세 곳(포트폴리오 · 배당 컨트롤러 · 자산 현황 유틸)이 공용 도우미를 <b>그 자릿수로</b> 쓴다. 처음엔 "부른다" 만 봐서, 배당 컨트롤러는 두
   * 호출 중 하나만 남아도 · 자릿수가 바뀌어도 통과했다(검토 2026-10-22).
   */
  @Test
  void 예전_세_곳이_공용_도우미를_쓴다() throws IOException {
    String[][] calls = {
      {
        "controller/StockPortfolioHtmxController.java",
        "StockAmountUtil.percentOrNull(amount, base, 4)"
      },
      {
        "controller/StockDividendHtmxController.java",
        "StockAmountUtil.percentOrNull(amount, principal, 4)"
      },
      {
        "controller/StockDividendHtmxController.java",
        "StockAmountUtil.percentOrNull(principalDelta, startPrincipal, 2)"
      },
      {
        "util/StockAssetStatusUtil.java", "StockAmountUtil.percentOrNull(numerator, denominator, 4)"
      },
    };
    for (String[] call : calls) {
      assertThat(flatten(Files.readString(STOCK_MAIN.resolve(call[0]), StandardCharsets.UTF_8)))
          .as(call[0])
          .contains(call[1]);
    }
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
