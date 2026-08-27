package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;

/**
 * 대시보드 요약 카드에서 <b>두 수익률이 서로 다른 원금으로 나뉜다는 사실</b>이 화면에 남는지 본다.
 *
 * <p>이 카드에는 비율이 둘 있고 분모가 다르다.
 *
 * <ul>
 *   <li>평가 수익률 &mdash; {@code currentPrincipal} = 설정 원금이 있으면 그 값, 없으면 취득원가
 *   <li>합산 수익률 &mdash; {@code combinedPrincipal} = <b>항상</b> 취득원가({@code 총자산 − 평가손익})
 * </ul>
 *
 * <p>이것은 의도된 설계다. 그래서 화면이 두 가지를 밝힌다 &mdash; "설정 원금 기준 차이" 줄과 "수익률 기준 원금" 줄 (툴팁: <i>"기준 원금은 수동 설정
 * 원금이 아닌 취득원가입니다"</i>). 그 공시가 사라지면 사용자는 같은 카드 안의 두 비율이 왜 다른지 알 수 없다.
 *
 * <p>지금은 드러나지 않는다 &mdash; 실측 2026-08-24: 이 사용자의 6 계좌는 {@code manualPrincipalAmount} 가 모두 비어 있어 두
 * 분모가 같다. 사용자가 그 값을 되살리는 순간 갈라지므로, 그 상태를 여기서 미리 그려 고정한다.
 */
class SummaryPrincipalBasisRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/summary.jte";

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  /**
   * 설정 원금을 쓰는 화면 상태.
   *
   * @param manualPrincipal 설정 원금. null 이면 취득원가를 쓴다.
   */
  private String render(BigDecimal manualPrincipal, BigDecimal manualPrincipalAdjustment) {
    Map<String, Object> model = new HashMap<>();
    // 총자산 1,200,000 · 평가손익 200,000 -> 취득원가 1,000,000
    model.put("totalAsset", bd("1200000"));
    model.put("totalUnrealizedProfit", bd("200000"));
    model.put("totalRealizedProfit", bd("300000"));
    model.put("totalDividend", bd("50000"));
    model.put("winRate", 78.57142857142857d);
    model.put("winCount", 33L);
    model.put("winDenominator", 42);
    model.put("displayPrincipal", manualPrincipal);
    model.put("displayCurrentEvaluationProfit", bd("200000"));
    model.put("combinedAdjustmentAmount", BigDecimal.ZERO);
    model.put("holdingFeeAdjustment", BigDecimal.ZERO);
    model.put("excludedHoldingBuyFee", BigDecimal.ZERO);
    model.put("manualPrincipalAdjustment", manualPrincipalAdjustment);
    model.put("trendChartLabelsJs", "[]");
    model.put("trendChartValuesJs", "[]");
    model.put("trendPointCount", 0);
    model.put("priceBasisDate", LocalDate.parse("2026-08-19"));

    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  /**
   * "수익권 종목 비율"이 무엇을 세는지 화면에 남는지 본다.
   *
   * <p>이 비율의 손익 기준은 <b>실현 + 평가 + 배당</b>이다. 배당을 빼고 세면 배당이 큰 종목이 실제로는 이익인데 패로 잡힌다(실측 2026-08-24:
   * TIGER 리츠부동산인프라는 실현+평가가 손실인데 배당이 그 1.75 배 -> 42 종목 중 1 종목이 뒤집혀 76.19% 대신 78.57%). 숫자만 바뀌고 기준이 안
   * 보이면 사용자는 같은 카드의 '합산 손익'과 왜 맞물리는지 알 수 없다.
   */
  @Test
  void 수익권_종목_비율이_분자_분모와_손익_기준을_밝힌다() {
    String html = render(null, BigDecimal.ZERO);

    assertThat(html).as("요약 카드를 그리지 못했다 - 검사가 무력해진다").contains("78.6%");
    assertThat(html)
        .as("수익권 종목 비율이 무엇을 세는지 밝히지 않는다")
        .contains(
            java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.summary.win.rate.basis.tooltip"), 33L, 42));
  }

  @Test
  void 설정_원금이_있으면_기준이_다르다는_것을_밝힌다() {
    // 설정 원금 800,000 (취득원가 1,000,000 과 다르다) -> 두 비율의 분모가 갈린다.
    String html = render(bd("800000"), bd("-200000"));

    assertThat(html).as("요약 카드를 그리지 못했다 - 검사가 무력해진다").contains("합산");
    assertThat(html)
        .as("설정 원금과 취득원가가 다른데 그 차이를 밝히지 않는다")
        .contains(MessageUtil.getMessage("stock.summary.label.manual.principal.adjustment"));
    assertThat(html)
        .as("합산 수익률이 어떤 원금으로 나뉘었는지 밝히지 않는다")
        .contains(MessageUtil.getMessage("stock.summary.label.combined.rate.basis"));
    // 합산 수익률의 분모는 설정 원금이 아니라 취득원가 1,000,000 이어야 한다.
    assertThat(html).contains("1,000,000");
  }

  @Test
  void 설정_원금이_없으면_두_비율의_분모가_같다() {
    // 지금 이 사용자의 상태. 분모가 같으므로 "설정 원금 기준 차이" 줄은 나오지 않는다.
    String html = render(null, BigDecimal.ZERO);

    assertThat(html)
        .as("설정 원금이 없는데 기준 차이 줄이 나온다")
        .doesNotContain(MessageUtil.getMessage("stock.summary.label.manual.principal.adjustment"));
    assertThat(html)
        .as("기준 원금 공시는 설정 원금 유무와 무관하게 나와야 한다")
        .contains(MessageUtil.getMessage("stock.summary.label.combined.rate.basis"));
  }
}
