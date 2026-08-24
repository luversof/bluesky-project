package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * 요약 카드의 "합산 수익률"이 분자·분모를 같은 기준으로 쓰는지 본다.
 *
 * <p>이 화면에는 두 가지 기준이 섞여 있다 &mdash; 수동 설정 원금과 보유 수수료를 반영한 <b>표시</b> 평가손익, 그리고 그 조정을 반영하지 않은
 * <b>취득원가</b>. 합산 수익률의 분모는 취득원가이고, 분자는 표시 평가손익에서 조정분({@code combinedAdjustmentAmount})을 다시 빼서 같은
 * 기준으로 되돌린다. 즉 대수적으로 조정분은 상쇄되어야 한다.
 *
 * <pre>
 *   분자 = 표시평가손익 + (실현 + 배당) - (표시평가손익 - 원자료평가손익)
 *        = 원자료평가손익 + 실현 + 배당
 * </pre>
 *
 * <p>이 상쇄가 깨지면(예: 분자만 표시값으로 바꾸고 조정분 차감을 빼면) 수익률이 조용히 다른 기준이 된다 &mdash; 화면에는 여전히 취득원가로 나눈 값이라고 적혀 있는
 * 채로. 숫자만 보고는 알아채기 어려워 검사로 고정한다.
 *
 * <p>수동 원금이 설정된 계좌가 지금 하나도 없어(복구 대기) 실데이터로는 이 갈림이 드러나지 않는다. 그래서 조정분이 있는 경우를 값으로 만들어 확인한다.
 */
class CombinedRateBasisTest {

  private static final String TEMPLATE = "stock/htmx/fragments/summary.jte";

  /** 합산 손익 옆에 붙는 수익률 span. 화면에는 다른 백분율도 있어 이 span 에 앵커한다. */
  private static final Pattern COMBINED_RATE =
      Pattern.compile("ml-2 text-sm font-semibold[^>]*>([+-]\\d+\\.\\d{2})%");

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

  /**
   * @param adjustment 표시 평가손익 - 원자료 평가손익 (수동 원금 · 보유 수수료 조정의 합)
   */
  private String render(BigDecimal adjustment) {
    Map<String, Object> params = new HashMap<>();
    params.put("totalAsset", new BigDecimal("1493281835"));
    params.put("totalUnrealizedProfit", new BigDecimal("861058009"));
    params.put("totalRealizedProfit", new BigDecimal("225584549"));
    params.put("totalDividend", new BigDecimal("61646257"));
    params.put("winRate", 0.62);
    params.put("displayPrincipal", new BigDecimal("621595902"));
    // 표시 평가손익 = 원자료 + 조정분
    params.put("displayCurrentEvaluationProfit", new BigDecimal("861058009").add(adjustment));
    params.put("combinedAdjustmentAmount", adjustment);
    params.put("holdingFeeAdjustment", BigDecimal.ZERO);
    params.put("manualPrincipalAdjustment", adjustment);

    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, params, output);
    return output.toString();
  }

  private String combinedRate(String html) {
    Matcher matcher = COMBINED_RATE.matcher(html);
    String last = null;
    while (matcher.find()) {
      last = matcher.group(1);
    }
    assertThat(last).as("합산 수익률을 화면에서 찾지 못했다 — 이 검사가 무의미해진다").isNotNull();
    return last;
  }

  @Test
  void 조정분이_있어도_합산_수익률은_같다() {
    String withoutAdjustment = combinedRate(render(BigDecimal.ZERO));
    String withPositive = combinedRate(render(new BigDecimal("100000000")));
    String withNegative = combinedRate(render(new BigDecimal("-70000000")));

    assertThat(withPositive).as("조정분이 분자에 남아 수익률 기준이 분모(취득원가)와 어긋났다").isEqualTo(withoutAdjustment);
    assertThat(withNegative).isEqualTo(withoutAdjustment);
  }

  /** 기준이 실제로 취득원가인지. (평가손익 + 실현 + 배당) / (총자산 - 평가손익) 을 직접 계산해 맞춘다. */
  @Test
  void 합산_수익률의_분모는_취득원가다() {
    BigDecimal numerator =
        new BigDecimal("861058009")
            .add(new BigDecimal("225584549"))
            .add(new BigDecimal("61646257"));
    BigDecimal denominator = new BigDecimal("1493281835").subtract(new BigDecimal("861058009"));
    String expected =
        String.format("%+.2f", numerator.doubleValue() / denominator.doubleValue() * 100);

    assertThat(combinedRate(render(new BigDecimal("100000000")))).isEqualTo(expected);
  }
}
