package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
 * 종목 상세가 <b>평가손익 + 실현손익 + 배당</b>을 합친 값을 함께 보여주는지 렌더해서 본다.
 *
 * <p>세 값은 각각 카드로 있었는데 합계가 없어서, "이 종목에서 결국 얼마를 벌었나" 를 사람이 더해야 알 수 있었다. 배당이 큰 종목은 셋 중 둘만 보면 부호까지 뒤집힌다
 * &mdash; 요약 화면의 '수익권 종목 비율' 이 실제로 그 이유로 틀렸었다(실측 2026-08-24: 42 종목 중 1 종목).
 *
 * <p>합산 금액만 두면 검산할 수 없으므로 <b>더한 세 값을 같은 자리에 적는다</b>. 화면에서 눈으로 맞춰 볼 수 있어야 한다.
 *
 * <p>비율의 분모는 요약 화면과 같은 취득원가(평가액 &minus; 평가손익)다. 전량 매도한 종목은 0 이 되므로 그때는 비율을 내지 않는다.
 */
class StockItemCombinedProfitRenderTest {

  private static final String TEMPLATE = "stock/stockItemDetail.jte";

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

  /** 평가손익 +100,000 · 실현손익 +50,000 · 배당 +30,000, 취득원가 1,000,000. */
  private Map<String, Object> model() {
    Map<String, Object> model = new HashMap<>();
    model.put("contentReady", true);
    model.put(
        "stockItem",
        new net.luversof.web.gate.stock.domain.StockItem(
            java.util.UUID.randomUUID(), "005930", "표본종목", "KOSPI", java.util.List.of()));
    model.put("evaluationAmount", bd("1100000"));
    model.put("evaluationProfit", bd("100000"));
    model.put("realizedProfit", bd("50000"));
    model.put("totalDividend", bd("30000"));
    return model;
  }

  private String render(Map<String, Object> model) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  @Test
  void 세_값을_합친_손익을_보여준다() {
    String html = render(model());

    assertThat(html)
        .as("합계가 없으면 사람이 세 카드를 더해야 한다")
        .contains(MessageUtil.getMessage("stock.item.detail.combined.profit"))
        .contains("+180,000");
  }

  @Test
  void 더한_세_값을_같은_자리에_적어_검산할_수_있게_한다() {
    String html = render(model());

    int combined = html.indexOf(MessageUtil.getMessage("stock.item.detail.combined.profit"));
    assertThat(combined).as("합산 손익 자리를 찾지 못했다 - 검사가 무력해진다").isGreaterThan(0);
    // 아래 카드 격자가 시작되기 전까지가 합산 손익 블록이다.
    int grid = html.indexOf("grid grid-cols-2 lg:grid-cols-4", combined);
    assertThat(grid).as("카드 격자를 찾지 못했다").isGreaterThan(combined);

    assertThat(html.substring(combined, grid))
        .as("합계만 있으면 어디서 나온 값인지 화면에서 맞춰 볼 수 없다")
        .contains("+100,000")
        .contains("+50,000")
        .contains("+30,000");
  }

  @Test
  void 취득원가_대비_비율을_함께_낸다() {
    String html = render(model());

    // 180,000 / 1,000,000 = 18.00%
    assertThat(html).as("금액만으로는 이 종목이 잘한 건지 알 수 없다").contains("+18.00%");
    assertThat(html).contains(MessageUtil.getMessage("stock.item.detail.combined.basis"));
  }

  /** 전량 매도한 종목은 취득원가가 0 이라 비율을 낼 수 없다. 0 으로 나눈 값을 그리면 안 된다. */
  @Test
  void 보유가_없으면_비율_없이_금액만_낸다() {
    Map<String, Object> model = model();
    model.put("evaluationAmount", BigDecimal.ZERO);
    model.put("evaluationProfit", BigDecimal.ZERO);

    String html = render(model);

    assertThat(html).as("실현손익과 배당만으로도 합계는 낼 수 있다").contains("+80,000");
    assertThat(html)
        .as("취득원가가 0 인데 비율을 그리면 안 된다")
        .doesNotContain(MessageUtil.getMessage("stock.item.detail.combined.basis"));
  }

  /**
   * 평가손익만 기간이 아니라 현재 시점 값이라는 것을 밝힌다.
   *
   * <p>api-stock 은 기간이 실리면 평가를 아예 계산하지 않으므로, 이 화면은 평가만 기간 없이 따로 부른다. 시작일을 바꿔도 그 몫은 그대로다 &mdash; 밝히지
   * 않으면 합계 전부가 기간 값으로 읽힌다.
   */
  @Test
  void 평가손익이_기간_값이_아님을_밝힌다() {
    assertThat(render(model())).contains(MessageUtil.getMessage("stock.item.detail.combined.note"));
  }
}
