package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 배당 수익률 표에서 "선택 합계"(브라우저)와 "합계행"(서버)이 같은 정의를 쓰는지 본다.
 *
 * <p>기준일 원금이 0 인 배당(지급일 이전에 이미 전량 매도한 건)은 분모에 기여하지 않는다. 그 세후액을 분자에 넣으면 수익률이 과대 계상되므로 서버는 {@code
 * netAmountWithPrincipalCost} 로 걸러서 계산한다.
 *
 * <p>그런데 표의 선택 합계는 브라우저가 다시 계산하는데, 예전에는 <b>선택된 행의 세후액 전부</b>를 분자에 넣었다. 같은 행을 전부 골라도 합계행과 숫자가 달라진다.
 *
 * <p>실측 2026-08-22(원장에서 직접 재계산): 배당 193 건 중 5 건, 세후 144,360 원이 기준일 보유수량 0 이하였다. 전체 세후 61,645,687 원의
 * 0.23% 이므로, 전부 선택하면 기준일 평균원가 수익률이 그만큼 높게 나왔다.
 *
 * <p>이 검사는 소스를 읽는다. 선택 합계는 템플릿 안 인라인 스크립트라 브라우저 없이는 실행할 수 없다.
 */
class DividendYieldSelectionConsistencyTest {

  private static final Path SELECTION =
      Path.of("src/main/jte/stock/htmx/fragments/tabsDividendHistory.jte");

  private static final Path ASSET_STATUS =
      Path.of("src/main/jte/stock/htmx/fragments/assetStatus.jte");

  private static final Path ROWS =
      Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte");

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 행이_걸러진_분자를_내보낸다() throws IOException {
    String rows = read(ROWS);
    // 종목별 표와 계좌별 표 두 곳 모두 필요하다.
    int count = rows.split("data-net-with-principal-cost=", -1).length - 1;
    assertThat(count).as("종목별/계좌별 두 표 모두 값을 내보내야 한다").isEqualTo(2);
    assertThat(rows).contains("row.netAmountWithPrincipalCost()");
  }

  @Test
  void 선택_합계가_걸러진_분자를_쓴다() throws IOException {
    String selection = read(SELECTION);
    assertThat(selection).as("행이 내보낸 걸러진 값을 읽지 않는다").contains("row.dataset.netWithPrincipalCost");
    assertThat(selection)
        .as("기준일 평균원가 수익률의 분자가 서버와 다르다(전체 세후액을 쓰면 과대 계상)")
        .contains("(totalNetWithPrincipalCost / totalAveragePrincipalCost) * 100");
    assertThat(selection)
        .as("걸러지지 않은 세후액을 그 분자로 되돌리면 안 된다")
        .doesNotContain("(totalNetAmount / totalAveragePrincipalCost) * 100");
  }

  /**
   * 일평균 원가 기준 수익률도 걸러진 분자를 쓴다 &mdash; 행·합계행·선택 합계 셋 다.
   *
   * <p>예전에는 합계행과 선택 합계만 걸러지지 않은 세후액을 썼다. 둘끼리는 맞았지만 <b>같은 열의 행</b>과 어긋나 있었다 &mdash; 행은 서버({@code
   * YieldAccumulator.toView})가 {@code netAmountWithPrincipalCost} 로 걸러 계산한 값을 그대로 그린다. 합계행 자신도 나머지
   * 두 수익률(기준평균원가·시장가)은 걸러진 값을 쓰고 있었으므로, 일평균만 예외였다(같은 화면 6 곳 중 1 곳).
   *
   * <p>실측 2026-08-24(원장에서 직접 재계산): 배당 193 건 중 5 건·세후 144,360 원이 기준일 원금 0 이다. 걸러지지 않은 분자 61,645,687
   * 원 vs 걸러진 분자 61,501,327 원 &mdash; 합계행 수익률이 상대 0.234% 높았다. 눈에 크게 띄는 쪽은 종목 하나만 볼 때다: NAVER 는 배당이 1
   * 건(세후 102,040 원)뿐이고 2021-01-18 전량매도 뒤 2021-04-08 에 지급돼, <b>행은 0.00%</b> 인데 그 행만 골랐을 때 선택 합계는 0 이
   * 아닌 값을 냈다.
   */
  @Test
  void 일평균_원가_수익률도_걸러진_분자를_쓴다() throws IOException {
    String selection = read(SELECTION);
    assertThat(selection)
        .as("선택 합계의 일평균원가 수익률 분자가 행·합계행과 다르다")
        .contains("(totalNetWithPrincipalCost / totalDailyPrincipalCost) * 100");
    assertThat(selection)
        .as("걸러지지 않은 세후액을 그 분자로 되돌리면 안 된다")
        .doesNotContain("(totalNetAmount / totalDailyPrincipalCost) * 100");

    String rows = read(ROWS);
    // 종목별·계좌별 두 합계행 모두.
    for (String prefix : new String[] {"stockYield", "accountYield"}) {
      assertThat(rows)
          .as(prefix + " 합계행의 일평균원가 수익률 분자가 행과 다르다")
          .contains(
              "computeYieldPct.apply("
                  + prefix
                  + "FooterNetWithPrincipalCost, "
                  + prefix
                  + "FooterDailyPrincipalCost)");
      assertThat(rows)
          .as(prefix + " 합계행이 걸러지지 않은 세후액으로 되돌아갔다")
          .doesNotContain(
              "computeYieldPct.apply("
                  + prefix
                  + "FooterNetAmount, "
                  + prefix
                  + "FooterDailyPrincipalCost)");
    }
  }

  /**
   * 자산현황 계좌 표도 같은 구조다 - 합계행은 서버가, 선택 합계는 브라우저가 계산한다.
   *
   * <p>2026-08-22 대조 결과 두 계산은 일치했다(분자·분모의 역할이 같다). 배당 표에서 실제로 어긋난 전례가 있으므로 여기서도 고정한다.
   *
   * <ul>
   *   <li>평가손익률 = 평가손익 합 / 매수금액 합
   *   <li>원금 대비 수익률 = (평가액 − 기준원금) 합 / 기준원금 합
   * </ul>
   *
   * <p>기준원금은 계좌 설정의 수동 입력값이 있으면 그 값이다({@code accountProfitBasisMap}). 행이 그 값을 그대로 {@code
   * data-principal} 로 내보내므로 선택 합계도 같은 기준을 쓴다.
   */
  @Test
  void 자산현황_선택_합계가_합계행과_같은_정의를_쓴다() throws IOException {
    String asset = read(ASSET_STATUS);

    assertThat(asset)
        .as("합계행의 평가손익률 정의가 바뀌었다")
        .contains(
            "totalAccountEvaluationProfit.doubleValue() / totalBuyAmount.doubleValue() * 100");
    assertThat(asset)
        .as("선택 합계의 평가손익률이 합계행과 다르다")
        .contains("(totalEvaluationProfitValue / totalBuyAmount) * 100");

    assertThat(asset)
        .as("합계행의 원금 대비 수익률 정의가 바뀌었다")
        .contains("totalPrincipalReturnAmount.doubleValue() / totalPrincipal.doubleValue() * 100");
    assertThat(asset)
        .as("선택 합계의 원금 대비 수익률이 합계행과 다르다")
        .contains("(totalPrincipalReturnValue / totalPrincipalValue) * 100");

    // 행이 서버와 같은 기준원금을 내보내야 선택 합계가 성립한다.
    assertThat(asset)
        .as("행이 기준원금(수동 입력 반영)을 내보내지 않는다")
        .contains("data-principal=\"${profitBasis.toPlainString()}\"")
        .contains("data-principal-return=\"${principalReturnAmount.toPlainString()}\"");
  }
}
