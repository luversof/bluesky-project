package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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
import net.luversof.web.gate.stock.controller.StockTradeHtmxController.Activity;

/**
 * 활동 화면의 템플릿 계산을 고정한다.
 *
 * <p>이 템플릿은 계산 블록이 14개로 주식 화면 중 가장 많은데 렌더 테스트가 하나도 없었다 &mdash; 달력 격자, 월별 집계, 차트 배열 문자열이 전부 템플릿 안에서
 * 만들어진다.
 *
 * <p>특히 월별 막대 차트는 <b>활동이 있는 달만</b> 그린다. 실측(전체 기간 조회): 2009-10~2026-08 의 203 개월 중 132 개월(65%)이 축에서
 * 빠지고, 2010-03 막대 바로 옆에 2014-11 막대가 붙는다(실제 공백 55 개월). 막대를 203 개로 채우면 읽을 수 없으므로 압축은 두되, 생략된 달 수를 화면에
 * 밝히는 것으로 처리했다. 그 안내가 실제로 나오는지 여기서 지킨다.
 *
 * <p>검사는 정규식을 쓰지 않고 문자열 위치로 찾는다 &mdash; 이 파일을 셸 heredoc 으로 쓰면 역슬래시가 한 겹 줄어 컴파일이 깨진 적이 있다.
 */
class ActivityListRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/activityList.jte";
  private static final UUID ACCOUNT_ID = UUID.fromString("01a0289d-8900-74b1-8d01-1e857fa3b2c6");
  private static final UUID STOCK_ITEM_ID = UUID.fromString("019d271d-ca42-7ad6-bd37-29cc9f7a0eef");

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

  /** KST 기준 그 날짜의 정오. 자정을 쓰면 존 변환에서 날이 넘어가 무엇을 재는지 흐려진다. */
  private static Instant kstNoon(String localDate) {
    return Instant.parse(localDate + "T03:00:00Z");
  }

  private static Activity trade(String localDate, String tradeType, String amount) {
    return new Activity(
        "TRADE",
        STOCK_ITEM_ID,
        "RISE 200위클리커버드콜",
        tradeType,
        10,
        null,
        new BigDecimal(amount),
        kstNoon(localDate),
        List.of(ACCOUNT_ID));
  }

  private static Activity dividend(String localDate, String amount) {
    return new Activity(
        "DIVIDEND",
        STOCK_ITEM_ID,
        "RISE 200위클리커버드콜",
        null,
        null,
        "배당",
        new BigDecimal(amount),
        kstNoon(localDate),
        List.of(ACCOUNT_ID));
  }

  private String render(List<Activity> activities) {
    Map<String, Object> params = new HashMap<>();
    params.put("activities", activities);
    params.put("activityView", "all");
    params.put("startDate", Instant.parse("2009-10-01T00:00:00Z"));
    params.put("endDate", Instant.parse("2026-09-01T00:00:00Z"));
    params.put("timeZone", "Asia/Seoul");
    params.put("rangeMode", "all");
    params.put("dataFirstDate", "2009-10-01");
    params.put("accountList", List.of());
    params.put("stockItemList", List.of());
    params.put("accountNames", Map.of(ACCOUNT_ID, "한국투자증권 ISA"));
    // long 파라미터는 Map 렌더에서 기본값이 Integer 로 들어가 ClassCastException 이 난다. 명시적으로 넘긴다.
    params.put("buyCount", 0L);
    params.put("sellCount", 0L);
    params.put("dividendCount", 0L);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, params, output);
    return output.toString();
  }

  /** 안내가 없으면 -1. */
  private long skippedMonths(String html) {
    String marker = "data-chart-skipped-months=\"";
    int at = html.indexOf(marker);
    if (at < 0) {
      return -1L;
    }
    int from = at + marker.length();
    return Long.parseLong(html.substring(from, html.indexOf('"', from)));
  }

  /** 차트에 실제로 들어간 월 라벨. */
  private List<String> chartLabels(String html) {
    String marker = "var activityChartLabels=[";
    int at = html.indexOf(marker);
    assertThat(at).as("차트 라벨 배열을 찾지 못했다").isGreaterThan(-1);
    int from = at + marker.length();
    String body = html.substring(from, html.indexOf(']', from));
    List<String> labels = new ArrayList<>();
    for (String piece : body.split(",")) {
      String trimmed = piece.trim();
      if (trimmed.length() >= 2) {
        labels.add(trimmed.substring(1, trimmed.length() - 1));
      }
    }
    return labels;
  }

  /** 달력 한 칸의 표식 개수. */
  private long gridCells(String html) {
    String marker = "class=\"min-h-14 ";
    long count = 0;
    for (int at = html.indexOf(marker); at >= 0; at = html.indexOf(marker, at + 1)) {
      count++;
    }
    return count;
  }

  @Test
  void 연속된_달만_있으면_생략_안내가_없다() {
    String html =
        render(List.of(trade("2026-07-15", "BUY", "100000"), trade("2026-08-19", "BUY", "200000")));

    assertThat(chartLabels(html)).containsExactly("2026.07", "2026.08");
    assertThat(skippedMonths(html)).as("빈 달이 없으면 안내를 내지 않는다").isEqualTo(-1L);
  }

  @Test
  void 빈_달이_있으면_몇_개월이_생략됐는지_밝힌다() {
    // 실데이터의 가장 큰 공백을 그대로 재현: 2010-03 다음 막대가 2014-11(55 개월 공백)
    String html =
        render(
            List.of(trade("2010-03-10", "BUY", "500000"), trade("2014-11-20", "SELL", "700000")));

    assertThat(chartLabels(html))
        .as("막대는 두 개뿐인데 실제로는 4년 7개월이 떨어져 있다")
        .containsExactly("2010.03", "2014.11");
    assertThat(skippedMonths(html)).isEqualTo(55L);
  }

  /** 달력 격자는 (앞 여백 + 그 달 일수) 를 7 의 배수로 올림한 칸 수여야 한다. */
  @Test
  void 달력_격자는_그_달을_모두_담는_칸_수를_만든다() {
    // 2026-08-01 은 토요일 -> 앞 여백 6 칸, 31 일 -> 37 -> 올림해서 42 칸(6 주)
    assertThat(gridCells(render(List.of(trade("2026-08-19", "BUY", "100000"))))).isEqualTo(42L);

    // 2026-02-01 은 일요일 -> 앞 여백 0 칸, 28 일 -> 정확히 28 칸(4 주, 올림 없음)
    assertThat(gridCells(render(List.of(trade("2026-02-10", "BUY", "100000")))))
        .as("딱 떨어지는 달에 빈 주가 더 붙으면 안 된다")
        .isEqualTo(28L);

    // 두 달이면 각 달의 격자가 따로 만들어진다.
    assertThat(
            gridCells(
                render(List.of(trade("2026-02-10", "BUY", "1"), trade("2026-08-19", "BUY", "1")))))
        .isEqualTo(28L + 42L);
  }

  @Test
  void 월별_집계는_매수_매도_배당을_따로_쌓는다() {
    String html =
        render(
            List.of(
                trade("2026-08-03", "BUY", "100000"),
                trade("2026-08-04", "BUY", "50000"),
                trade("2026-08-05", "SELL", "70000"),
                dividend("2026-08-06", "1234")));

    assertThat(html).contains("var activityChartBuy=[150000]");
    assertThat(html).contains("var activityChartSell=[70000]");
    assertThat(html).contains("var activityChartDividend=[1234]");
  }

  @Test
  void 활동이_없으면_차트도_생략_안내도_그리지_않는다() {
    String html = render(List.of());

    // 초기화 스크립트 자체는 늘 실려 나오지만 캔버스 부재를 스스로 확인하고 빠진다(activityList.jte:617).
    // 그래서 여기서 볼 것은 캔버스 요소의 유무다.
    assertThat(html).doesNotContain("<canvas id=\"activityMonthlyChart\"");
    assertThat(skippedMonths(html)).isEqualTo(-1L);
  }
}
