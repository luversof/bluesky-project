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

    // 원 단위로 반올림해 넘긴다(차트 축이 원 단위다). 2026-09-10 부터 점선은 ChartSeriesJs 가 종가와 같은 길이의 배열로 채운다.
    assertThat(html).contains("cost:new Array(2).fill(71887)");
    assertThat(html).contains(MessageUtil.getMessage("stock.item.detail.price.chart.average"));
  }

  /** 그 점선이 '지금' 값이라 과거 구간의 실제 평단이 아니라는 것을 밝힌다. */
  @Test
  void 평균단가가_지금_값임을_밝힌다() {
    assertThat(render(prices()))
        .contains(MessageUtil.getMessage("stock.item.detail.price.chart.desc"));
  }

  /**
   * 시세가 없으면 빈 차트 틀을 그리지 않는다. 축만 있는 그림은 자료가 없다는 사실을 가린다.
   *
   * <p>2026-09-12 까지는 구역째 빼서 제목도 내지 않았다. 그런데 그러면 <b>아무 말도 하지 않는다</b> &mdash; 실측: 시세가 2026-04-01 에
   * 끝난 종목을 최근 3개월로 보면 이 구역만 조용히 사라지는데, 같은 화면의 매매·배당은 "이 종목의 … 내역이 없습니다" 를 띄운다. 화면만 봐서는 자료가 없는 것인지
   * 원래 없는 구역인지 알 수 없다. 그래서 <b>캔버스는 그대로 내지 않고</b>(원래 규칙) 글로 까닭만 남긴다.
   */
  @Test
  void 시세가_없으면_차트_대신_까닭을_적는다() {
    String html = render(List.of());

    assertThat(html).as("빈 차트 틀은 자료가 없다는 사실을 가린다 - 캔버스는 내지 않는다").doesNotContain("stockPriceChart");
    assertThat(html)
        .as("대신 구역 제목과 까닭을 남겨 '없다' 는 사실을 말한다")
        .contains(MessageUtil.getMessage("stock.item.detail.price.chart.title"))
        .contains(MessageUtil.getMessage("stock.item.detail.empty.price.history"));
  }

  private String renderCandle(boolean withAverage) {
    Map<String, Object> model = model(prices());
    model.put(
        "priceChart",
        List.of(
            new net.luversof.web.gate.stock.dto.response.StockPriceChartPoint(
                LocalDate.parse("2026-08-27"),
                new BigDecimal("57000"),
                new BigDecimal("58100"),
                new BigDecimal("56500"),
                new BigDecimal("57400"),
                withAverage ? new BigDecimal("55123.4") : null),
            new net.luversof.web.gate.stock.dto.response.StockPriceChartPoint(
                LocalDate.parse("2026-08-28"),
                new BigDecimal("57500"),
                new BigDecimal("61500"),
                new BigDecimal("57300"),
                new BigDecimal("61200"),
                withAverage ? new BigDecimal("56000") : null)));
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  /**
   * 캔들(시가 · 고가 · 저가 · 종가) + 그 날의 평균 단가(사용자 요청 2026-10-02). 차트 응답이 있으면 예전 선 차트(지금 평단 고정선) 대신 이것을
   * 그린다.
   */
  @Test
  void 차트_응답이_있으면_캔들과_그_시점_평단을_그린다() {
    String html = renderCandle(true);
    assertThat(html)
        .contains("createCandleChart")
        .contains("open:[57000,57500]")
        .contains("high:[58100,61500]")
        .contains("low:[56500,57300]")
        .contains("avg:[55123,56000]")
        .contains(MessageUtil.getMessage("stock.item.detail.price.chart.candle.desc"))
        .contains("data-candle-unit")
        // 평단이 축 밖이면(축을 캔들에 맞춤, 2026-10-02) 범례가 쓸 문구
        .contains("avgBelowLabel")
        .contains("avgAboveLabel");
    assertThat(html)
        .as("지금 평단 고정선(예전 선 차트)은 그리지 않는다")
        .doesNotContain("cost:new Array(2).fill(71887)")
        .doesNotContain(MessageUtil.getMessage("stock.item.detail.price.chart.desc"));
  }

  /** 그 기간에 보유가 한 번도 없으면 평단 점선을 설명하지 않는다 - 없는 선을 찾게 된다. */
  @Test
  void 보유가_없던_기간이면_평단_설명을_빼고_null_로_넘긴다() {
    String html = renderCandle(false);
    assertThat(html)
        .contains("avg:[null,null]")
        .contains(MessageUtil.getMessage("stock.item.detail.price.chart.candle.desc.nocost"))
        .doesNotContain(MessageUtil.getMessage("stock.item.detail.price.chart.candle.desc") + "<");
  }
}
