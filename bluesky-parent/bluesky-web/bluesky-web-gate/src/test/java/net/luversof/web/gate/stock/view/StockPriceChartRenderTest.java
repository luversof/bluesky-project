package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint;

/**
 * 종목 상세가 <b>주가 자체</b>를 그리는지 렌더해서 본다.
 *
 * <p>이 화면의 차트는 보유 평가액·원가 추이뿐이었다. 평가액은 <b>수량이 바뀌면 같이 움직이므로</b> 그 선에서 "산 뒤로 주가가 어떻게 됐는지" 를 읽어낼 수 없다
 * &mdash; 반토막 난 종목을 두 배로 더 사면 평가액 선은 올라간다.
 *
 * <p>점선(평균단가)은 <b>지금</b> 값이라 과거 구간에도 같은 값이 그어진다. 평단은 매매마다 달라졌으므로 그 구간의 실제 평단이 아니다. 그래도 "지금 내 단가가 이
 * 구간 어디쯤인지" 는 이 그림이 가장 잘 답하므로, 이름과 설명으로 그 뜻을 밝히고 함께 긋는다.
 */
class StockPriceChartRenderTest {

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

  private Map<String, Object> model(List<StockPriceHistoryPoint> prices) {
    Map<String, Object> model = new HashMap<>();
    model.put("contentReady", true);
    model.put("stockItem", new StockItem(UUID.randomUUID(), "005930", "표본종목", "KOSPI", List.of()));
    model.put("averageBuyPrice", new BigDecimal("71886.79"));
    model.put("priceHistory", prices);
    return model;
  }

  private String render(List<StockPriceHistoryPoint> prices) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model(prices), output);
    return output.toString();
  }

  private List<StockPriceHistoryPoint> prices() {
    return List.of(
        new StockPriceHistoryPoint(LocalDate.parse("2026-08-27"), new BigDecimal("57400")),
        new StockPriceHistoryPoint(LocalDate.parse("2026-08-28"), new BigDecimal("61200")));
  }

  @Test
  void 주가_차트를_그린다() {
    String html = render(prices());

    assertThat(html)
        .as("평가액 추이만으로는 주가 자체를 볼 수 없다")
        .contains(MessageUtil.getMessage("stock.item.detail.price.chart.title"))
        .contains("stockPriceChart");
  }

  @Test
  void 일별_종가를_그대로_넘긴다() {
    String html = render(prices());

    assertThat(html)
        .as("종가를 넘기지 않으면 빈 차트가 그려진다")
        .contains("2026-08-27")
        .contains("2026-08-28")
        .contains("57400")
        .contains("61200");
  }

  /** 평균단가를 함께 그어야 "지금 내 단가가 이 구간 어디쯤인지" 를 볼 수 있다. */
  @Test
  void 평균단가를_기준선으로_함께_긋는다() {
    String html = render(prices());

    // 원 단위로 반올림해 넘긴다(차트 축이 원 단위다).
    assertThat(html).contains("var avg = 71887;");
    assertThat(html).contains(MessageUtil.getMessage("stock.item.detail.price.chart.average"));
  }

  /** 그 점선이 '지금' 값이라 과거 구간의 실제 평단이 아니라는 것을 밝힌다. */
  @Test
  void 평균단가가_지금_값임을_밝힌다() {
    assertThat(render(prices()))
        .contains(MessageUtil.getMessage("stock.item.detail.price.chart.desc"));
  }

  /** 시세가 없으면 빈 차트 틀만 그리지 않는다. 축만 있는 그림은 자료가 없다는 사실을 가린다. */
  @Test
  void 시세가_없으면_차트를_그리지_않는다() {
    String html = render(List.of());

    assertThat(html)
        .as("빈 차트 틀은 자료가 없다는 사실을 가린다")
        .doesNotContain("stockPriceChart")
        .doesNotContain(MessageUtil.getMessage("stock.item.detail.price.chart.title"));
  }
}
