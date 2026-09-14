package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint;

/**
 * 주가 추이 차트의 "현재 평균단가" 점선은 지금 보유가 있을 때만 긋는다.
 *
 * <p>실측 2026-09-12(/stock/item): 매매 이력이 있는 43 종목 중 <b>34 종목이 수량 0</b> 인데, 그 종목들은 평균단가 계열이 전 구간 0 으로
 * 채워져 있었다 &mdash; 기아 2,740 점 · 삼성SDI 1,575 점 · 나노팀 750 점 · 에스디바이오센서 1,151 점 모두 0 이 아닌 값이 하나도 없었다.
 * 종가가 각각 206,000 / 799,869 / 36,900 / 78,600 까지 오르는 차트라 그 선은 바닥에 깔려 <b>공짜로 산 것처럼</b> 읽혔고, 보조기술 요약에도
 * "현재 평균단가: 처음 0, 끝 0, 최고 0, 최저 0" 으로 나갔다.
 *
 * <p>0 과 "없음" 은 다르다는 이 저장소의 규칙과 같다(금액 칸의 0 → '-', 부호 붙은 0 금지).
 *
 * <p>보유 중인 종목은 그대로다 &mdash; 실측: 삼성전자 1,600 점 · KODEX 200타겟위클리커버드콜 167 점 모두 값이 실려 있었다.
 */
class ChartSeriesJsPriceTest {

  private static final List<StockPriceHistoryPoint> TWO_DAYS =
      List.of(point("2026-01-02", 61000), point("2026-01-03", 62000));

  private static StockPriceHistoryPoint point(String day, long close) {
    return new StockPriceHistoryPoint(LocalDate.parse(day), BigDecimal.valueOf(close));
  }

  @Test
  void 보유가_있으면_평균단가로_채운다() {
    String js = ChartSeriesJs.priceSeries(TWO_DAYS, BigDecimal.valueOf(55000));
    assertThat(js).contains("new Array(2).fill(55000)");
    assertThat(js).contains("cost:new Array(");
  }

  @Test
  void 보유가_없으면_원가_계열을_비운다() {
    for (BigDecimal none : new BigDecimal[] {null, BigDecimal.ZERO, new BigDecimal("0.00")}) {
      String js = ChartSeriesJs.priceSeries(TWO_DAYS, none);
      assertThat(js).as(String.valueOf(none) + " 는 0 으로 채우면 안 된다").contains("cost:[]");
      assertThat(js).as(String.valueOf(none) + " 에 0 채움이 남으면 안 된다").doesNotContain(".fill(0)");
    }
  }

  @Test
  void 종가_계열과_라벨은_그대로다() {
    String js = ChartSeriesJs.priceSeries(TWO_DAYS, BigDecimal.ZERO);
    assertThat(js).contains("\"2026-01-02\"").contains("\"2026-01-03\"");
    assertThat(js).contains("value:[61000,62000]");
  }

  /** 선을 안 그었으면 화면 문구도 점선을 설명하면 안 된다 - 없는 선을 찾게 된다. */
  @Test
  void 점선을_안_그으면_설명도_바뀐다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/stockItemDetailContent.jte"), StandardCharsets.UTF_8);
    assertThat(flatten(template))
        .as("평균단가가 없을 때는 다른 문구를 골라야 한다")
        .contains("averageBuyPrice != null && averageBuyPrice.signum() != 0")
        .contains("stock.item.detail.price.chart.desc.nocost");
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(
              Files.readString(
                  Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8))
          .as(bundle)
          .contains("stock.item.detail.price.chart.desc.nocost");
    }
  }

  private String flatten(String source) {
    StringBuilder out = new StringBuilder();
    boolean lastWasSpace = false;
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (Character.isWhitespace(c)) {
        if (!lastWasSpace) {
          out.append(' ');
        }
        lastWasSpace = true;
      } else {
        out.append(c);
        lastWasSpace = false;
      }
    }
    return out.toString();
  }
}
