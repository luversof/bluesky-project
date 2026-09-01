package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.domain.StockItem;

/**
 * 종목 상세의 <b>합산 손익</b>이 그 아래 '기간별 손익' 표와 같은 것을 말하는지 렌더해서 본다.
 *
 * <p>세 값(평가·실현·배당)은 각각 카드로 있었는데 합계가 없어서, "이 종목에서 결국 얼마를 벌었나" 를 사람이 더해야 알 수 있었다. 배당이 큰 종목은 셋 중 둘만 보면
 * 부호까지 뒤집힌다 &mdash; 요약 화면의 '수익권 종목 비율' 이 실제로 그 이유로 틀렸었다(실측 2026-08-24: 42 종목 중 1 종목).
 *
 * <p>합계의 '평가' 몫은 <b>기간 평가 변동</b>(기말 &minus; 기초)이다. 처음에는 현재 시점 평가손익을 더했는데, 그러면 아래 표의 구간을 다 더한 값과 맞지
 * 않는다 &mdash; 실측 2026-09-01 삼성전자 '올해' 2.9 억 차이, '최근 1년' 은 부호까지 반대였다. 같은 화면의 두 숫자가 안 맞으면 어느 쪽이 맞는지 알
 * 수 없다. 평가 변동으로 바꾸면 요약의 {@code periodProfit} 과 1 원 오차 없이 같다(실측 두 기간 모두 차이 0).
 *
 * <p>합산 금액만 두면 검산할 수 없으므로 <b>더한 세 값을 같은 자리에 적는다</b>.
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

  /**
   * 기간 평가 변동 +100,000 · 실현손익 +50,000 · 배당 +30,000 &rarr; 합산 +180,000.
   *
   * <p>현재 시점 평가손익은 일부러 <b>다른 값</b>(+900,000)으로 둔다. 그걸 더하면 합계가 980,000 이 되므로, 잘못된 값을 쓰면 검사가 깨진다.
   */
  private Map<String, Object> model() {
    Map<String, Object> model = new HashMap<>();
    model.put("contentReady", true);
    model.put("stockItem", new StockItem(UUID.randomUUID(), "005930", "표본종목", "KOSPI", List.of()));
    model.put("evaluationAmount", bd("1900000"));
    model.put("evaluationProfit", bd("900000"));
    model.put("periodUnrealizedDelta", bd("100000"));
    model.put("periodProfitRatePct", 18.0d);
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
  void 기간_값들만_더한다() {
    String html = render(model());

    assertThat(html)
        .as("현재 시점 평가손익을 더하면 아래 표의 합과 맞지 않는다")
        .contains(MessageUtil.getMessage("stock.item.detail.combined.profit"))
        .contains("+180,000");
  }

  @Test
  void 더한_세_값을_같은_자리에_적어_검산할_수_있게_한다() {
    String html = render(model());

    int combined = html.indexOf(MessageUtil.getMessage("stock.item.detail.combined.profit"));
    assertThat(combined).as("합산 손익 자리를 찾지 못했다 - 검사가 무력해진다").isGreaterThan(0);
    int grid = html.indexOf("grid grid-cols-2 lg:grid-cols-4", combined);
    assertThat(grid).as("카드 격자를 찾지 못했다").isGreaterThan(combined);

    String block = html.substring(combined, grid);
    assertThat(block)
        .as("합계만 있으면 어디서 나온 값인지 화면에서 맞춰 볼 수 없다")
        .contains(MessageUtil.getMessage("stock.item.detail.combined.unrealized.delta"))
        .contains("+100,000")
        .contains("+50,000")
        .contains("+30,000");
    assertThat(block).as("합산 블록에 현재 시점 평가손익이 섞이면 무엇을 더한 것인지 알 수 없다").doesNotContain("+900,000");
  }

  /** 비율은 자산 성장 화면과 같은 정의를 받아 쓴다. 화면마다 분모가 다르면 같은 이름이 다른 수를 뜻한다. */
  @Test
  void 기간_손익률을_그대로_받아_쓴다() {
    String html = render(model());

    assertThat(html).as("금액만으로는 이 종목이 잘한 건지 알 수 없다").contains("+18.00%");
    assertThat(html).contains(MessageUtil.getMessage("stock.item.detail.combined.basis"));
  }

  /** 비율을 낼 수 없는 구간(기초도 유입도 없음)이면 금액만 낸다. 0 으로 나눈 값을 그리면 안 된다. */
  @Test
  void 손익률이_없으면_금액만_낸다() {
    Map<String, Object> model = model();
    model.put("periodProfitRatePct", null);

    String html = render(model);

    assertThat(html).contains("+180,000");
    assertThat(html).doesNotContain(MessageUtil.getMessage("stock.item.detail.combined.basis"));
  }

  /** 아래 표와 같은 값이라는 것, 그리고 평가손익 카드와는 다른 값이라는 것을 밝힌다. */
  @Test
  void 표와_같은_값이고_평가손익_카드와는_다름을_밝힌다() {
    assertThat(render(model())).contains(MessageUtil.getMessage("stock.item.detail.combined.note"));
  }
}
