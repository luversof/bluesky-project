package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 표의 "-" 한 글자에는 까닭이 붙는다.
 *
 * <p>실측 2026-09-12(10 화면, 계좌 상세를 펼친 상태): 보이는 "-" 칸 75 개 중 <b>9 개</b>가 {@code title} 도 {@code
 * sr-only} 도 {@code aria-label} 도 없이 글자 하나뿐이었다.
 *
 * <ul>
 *   <li>매매 / 연도별 매매 / 실현 손익 &mdash; 3 칸(2024·2017·2009). 같은 줄 머리가 "매수 5 · <b>매도 0</b>" 이라고 적고 있었는데도
 *       정작 그 칸은 말이 없었다. 같은 줄의 다른 금액 칸들은 "0원" 을 달고 있었다.
 *   <li>배당 / 연도별 배당 수익률 · 종목별 배당 효율 랭킹 / 기준일 평균원금·그 수익률 &mdash; 6 칸. 왜 비었는지는 <b>옆 칸</b> 문구가 말하고
 *       있었지만("기준일 원금이 없는 배당 …원은 제외됩니다") 이 칸들은 침묵했다.
 * </ul>
 *
 * <p>0 원과 "그런 일이 없었다" 는 다르게 읽힌다 &mdash; 이 규칙은 {@code _components/ui/amountCell} 에 이미 적혀 있다.
 */
class DashReasonSweepTest {

  @Test
  void 판_적이_없어_비는_칸은_그렇게_말한다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/trade/tradePeriodBreakdown.jte"),
            StandardCharsets.UTF_8);

    assertThat(template).as("매도 건수로 갈라야 한다 - 0 원과 뜻이 다르다").contains("@if(row.sellCount() == 0)");
    assertThat(template).as("마우스에 닿는 까닭이 없다").contains("title=\"${noSaleTitle}\">-</span>");
    assertThat(template)
        .as("낭독기에 닿는 까닭이 없다 - title 만으로는 안 된다")
        .contains("<span class=\"sr-only\">${noSaleTitle}</span>");
    assertThat(template).contains("stock.trade.breakdown.realized.none");
  }

  @Test
  void 기준일_원금이_없어_비는_칸은_그렇게_말한다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte"),
            StandardCharsets.UTF_8);

    assertThat(template)
        .as("기준일 원금이 없거나 0 이면 까닭을 돌려주는 판정이 있어야 한다")
        .contains("principalCost == null || principalCost.signum() == 0 ? basisNoneTitle : null");
    assertThat(template).contains("stock.dividend.yield.basis.none");
    // 세 표(연도별·종목별·계좌별)의 금액 칸 하나씩, 수익률 칸 하나씩.
    assertThat(
            countOf(
                template,
                "title=" + Q + "${basisNoneOrNull.apply(row.averagePrincipalCost())}" + Q))
        .as("기준일 평균원금 금액 칸의 까닭")
        .isEqualTo(3);
    assertThat(
            countOf(
                template,
                "title="
                    + Q
                    + "${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle :"
                    + " yieldBasisTitle.apply("))
        .as("기준일 평균원금 수익률 칸의 까닭")
        .isEqualTo(3);
    // 금액 칸은 이 까닭 하나뿐이라 그대로 싣는다.
    assertThat(countOf(template, "<span class=\"sr-only\">${basisNoneTitle}</span>"))
        .as("기준일 평균원금 금액 칸의 낭독기 문구")
        .isEqualTo(3);
    // 수익률 칸은 title 만 단다 - 바로 옆 금액 칸이 같은 문장을 이미 읽어 주기 때문이다(실측: 같은 줄의
    // 근거 문장은 언제나 하나였다). 여기에 또 달면 줄마다 같은 말을 두 번 듣는다.
    assertThat(
            countOf(
                template, "${percentFormat.apply(row.yieldOnCostPct())}<span class=\"sr-only\">"))
        .as("수익률 칸에는 낭독기 문구를 겹쳐 달지 않는다")
        .isZero();
  }

  /** 기간 일평균 투입원금도 비면 까닭을 단다 - 매매 없이 배당만 있는 종목(실측: 하나금융지주)이 여기에 걸린다. */
  @Test
  void 기간_투입원금이_없어_비는_칸도_그렇게_말한다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte"),
            StandardCharsets.UTF_8);

    assertThat(template).contains("stock.dividend.yield.daily.basis.none");
    assertThat(
            countOf(
                template,
                "title="
                    + Q
                    + "${row.averageDailyPrincipalCost() == null ? dailyBasisNoneTitle :"
                    + " null}"
                    + Q))
        .as("세 표의 기간 일평균 투입원금 칸")
        .isEqualTo(3);
    assertThat(countOf(template, "<span class=\"sr-only\">${dailyBasisNoneTitle}</span>"))
        .as("낭독기에도 닿아야 한다")
        .isEqualTo(3);
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(
              Files.readString(
                  Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8))
          .as(bundle)
          .contains("stock.dividend.yield.daily.basis.none");
    }
  }

  /**
   * 관리 &gt; 월배당 기준 데이터의 "최종 검증일" 도 마찬가지다.
   *
   * <p>실측 2026-09-12: 프로필 8개가 모두 {@code lastVerifiedDate = null} 이라 그 칸이 {@code <td>-</td>} 였다 -
   * 까닭이 어디에도 없었다. 이 화면은 <b>탭 주소</b>(?tab=monthly-reference)를 목록에 넣지 않아 그동안 훑기에서 빠져 있었다.
   */
  @Test
  void 검증한_적이_없어_비는_칸도_그렇게_말한다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/fragments/monthlyDividendReference.jte"),
            StandardCharsets.UTF_8);

    assertThat(template).contains("stock.monthly.reference.profile.not.verified");
    assertThat(template)
        .as("마우스에 닿는 까닭")
        .contains("<td title=\"${row.lastVerifiedDate() == null ? notVerifiedLabel : null}\">");
    assertThat(template)
        .as("낭독기에 닿는 까닭")
        .contains(
            "@if(row.lastVerifiedDate() == null)<span class=\"sr-only\">${notVerifiedLabel}</span>@endif");
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(
              Files.readString(
                  Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8))
          .as(bundle)
          .contains("stock.monthly.reference.profile.not.verified");
    }
  }

  @Test
  void 두_언어에_문구가_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8);
      assertThat(text).as(bundle).contains("stock.trade.breakdown.realized.none");
      assertThat(text).as(bundle).contains("stock.dividend.yield.basis.none");
    }
  }

  private static final String Q = String.valueOf((char) 34);

  private int countOf(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }
}
