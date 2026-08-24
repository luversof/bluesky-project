package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * 자산현황이 쓰는 '지금 보유분 원가' 규칙을 고정한다.
 *
 * <p>api-stock 이 {@code evaluationProfit = 평가액 - 원가} 로 두므로 {@code 평가액 - 평가손익} 이 곧 원가다. 실측: 이렇게 구한 합
 * 632,223,826 이 api-stock 시계열 보유원가와 정확히 같다(참고로 {@code 평균단가 x 수량} 은 632,223,831 로 평균단가 2 자리 반올림만큼
 * 어긋난다).
 *
 * <p>폴백은 예전에 {@code totalBuyAmount} 였는데 그것은 <b>기간 누적 매수액</b>이라 성격이 다르다 &mdash; 실측: 포트폴리오
 * 735,929,747 vs 실제 보유원가 632,223,826, 삼성전자는 466,231,000 vs 362,525,079 로 29% 과대. 지금 api-stock 은 두
 * 값을 항상 채우므로 폴백이 타지 않지만, 타는 날에 틀린 값을 쓰면 안 된다.
 */
class CurrentHoldingCostTest {

  private static final class TestController extends StockPortfolioHtmxController {
    private TestController() {
      super(
          null,
          null,
          null,
          null,
          null,
          null,
          new org.springframework.context.support.StaticMessageSource(),
          null);
    }
  }

  private final TestController controller = new TestController();

  @Test
  void 평가액에서_평가손익을_빼면_원가다() {
    // 평가액 1,493,281,835 / 평가손익 861,058,009 -> 원가 632,223,826 (실측 합계)
    assertThat(
            controller.resolveCurrentHoldingCost(
                new BigDecimal("1493281835"), new BigDecimal("861058009"), null, 0))
        .isEqualByComparingTo("632223826");
  }

  /** 평가손익이 음수(손실)여도 같은 항등식이 성립한다. */
  @Test
  void 손실_상태에서도_같은_식이다() {
    assertThat(
            controller.resolveCurrentHoldingCost(
                new BigDecimal("900"), new BigDecimal("-100"), null, 0))
        .isEqualByComparingTo("1000");
  }

  /** 현재가가 없어 평가액이 0 이어도 원가는 그대로 나온다. */
  @Test
  void 평가액이_0이어도_원가를_잃지_않는다() {
    assertThat(
            controller.resolveCurrentHoldingCost(BigDecimal.ZERO, new BigDecimal("-1000"), null, 0))
        .isEqualByComparingTo("1000");
  }

  @Test
  void 폴백은_평균단가x수량이다() {
    assertThat(controller.resolveCurrentHoldingCost(null, null, new BigDecimal("71887"), 5043))
        .isEqualByComparingTo(new BigDecimal("71887").multiply(BigDecimal.valueOf(5043)));
    assertThat(
            controller.resolveCurrentHoldingCost(
                new BigDecimal("100"), null, new BigDecimal("10"), 3))
        .as("한쪽만 있으면 폴백을 쓴다")
        .isEqualByComparingTo("30");
  }

  @Test
  void 폴백_재료도_없으면_0이다() {
    assertThat(controller.resolveCurrentHoldingCost(null, null, null, 0)).isEqualByComparingTo("0");
    assertThat(controller.resolveCurrentHoldingCost(null, null, new BigDecimal("10"), 0))
        .as("수량이 없으면 원가를 지어내지 않는다")
        .isEqualByComparingTo("0");
  }

  /**
   * 폴백에 누적 매수액을 다시 쓰지 않는지 소스로 확인한다.
   *
   * <p>줄바꿈은 CRLF/LF 가 섞일 수 있으므로 공백에 기대지 않고 정규식으로 찾는다. 예전 판은 개행을 문자열로 그대로 찾다가 파일이 CRLF 가 되자 깨졌다(단독
   * 실행에서는 통과하고 전체 실행에서만 실패해 원인을 찾기 어려웠다).
   */
  @Test
  void 폴백에_누적_매수액을_쓰지_않는다() throws java.io.IOException {
    String source =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(
                // 헬퍼는 요약 화면도 같은 계산을 쓰도록 기반 클래스로 옮겼다(예전에는 두 화면의 폴백이 서로 달랐다).
                "src/main/java/net/luversof/web/gate/stock/controller/StockBaseHtmxController.java"),
            java.nio.charset.StandardCharsets.UTF_8);

    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile(
                "BigDecimal resolveCurrentHoldingCost\\(\\s*BigDecimal evaluationAmount.*?\\R\\s*\\}",
                java.util.regex.Pattern.DOTALL)
            .matcher(source);
    assertThat(matcher.find()).as("4 인자 resolveCurrentHoldingCost 를 찾지 못했다").isTrue();
    assertThat(matcher.group())
        .as("totalBuyAmount 는 기간 누적 매수액이라 보유 원가가 아니다")
        .doesNotContain("totalBuyAmount");
  }

  /**
   * 요약 화면과 포트폴리오 화면이 같은 헬퍼로 보유 원가를 구하는지.
   *
   * <p>예전에는 두 곳이 따로 계산했고 폴백이 달랐다 &mdash; 포트폴리오는 {@code 평균단가 x 보유수량}(실측 오차 5 원), 요약은 {@code
   * totalBuyCost}(실측 735,958,622 로 실제 632,223,825 보다 <b>103,734,796 원 과대</b>). 같은 뜻의 값이 화면마다 달라지면 안
   * 된다.
   *
   * <p>지금은 api-stock 이 평가액/평가손익을 항상 채워 폴백이 타지 않지만, 타는 날에 틀린 값을 쓰면 안 된다.
   */
  @Test
  void 요약과_포트폴리오가_같은_헬퍼를_쓴다() throws java.io.IOException {
    String base =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockBaseHtmxController.java"),
            java.nio.charset.StandardCharsets.UTF_8);
    assertThat(base)
        .as("보유 원가 헬퍼가 기반 클래스에 없다")
        .contains("protected BigDecimal resolveCurrentHoldingCost(");

    String summary =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockSummaryHtmxController.java"),
            java.nio.charset.StandardCharsets.UTF_8);
    assertThat(summary).as("요약 화면이 공용 헬퍼를 쓰지 않는다").contains("resolveCurrentHoldingCost(");
    assertThat(summary)
        .as("요약 화면에 totalBuyCost 폴백이 남아 있다(기간 누적 매수액이라 보유 원가가 아니다)")
        .doesNotContain("sums.totalBuyCost()")
        .doesNotContain("profit.totalBuyCost()");
  }
}
