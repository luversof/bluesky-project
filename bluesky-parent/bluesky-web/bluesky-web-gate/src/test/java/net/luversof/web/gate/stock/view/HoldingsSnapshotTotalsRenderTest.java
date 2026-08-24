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
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;

/**
 * 보유 스냅샷 표의 합계 세 칸이 서로 맞는지 렌더해서 본다.
 *
 * <p>표에는 총원금 · 총평가 · 총평가손익이 한 줄에 나온다. <b>총평가 − 총원금 = 총평가손익</b> 이어야 하는데, 원금을 {@code avgCost x 수량} 으로
 * 만들면 그렇지 않다 &mdash; {@code avgCost} 는 표기용으로 <b>소수 2 자리 반올림</b>돼 오기 때문이다.
 *
 * <p>실측 2026-08-24(실데이터):
 *
 * <pre>
 *   2026-08-19  총평가 − 총원금 − 총평가손익 = +28원
 *   2026-06-18  같은 식 = −22원
 *   행별로는 KODEX 한국부동산리츠인프라 −16.85, RISE 200위클리커버드콜 −13.97
 * </pre>
 *
 * <p>api-stock 은 {@code unrealizedProfit} 을 {@code value − totalCost} 로 만든다. 그래서 {@code value −
 * unrealizedProfit} 이 곧 반올림 이전의 정밀 원가다. 그 값을 쓰면 세 칸이 정확히 닫힌다.
 */
class HoldingsSnapshotTotalsRenderTest {

  private static final String TEMPLATE = "stock/htmx/holdings-snapshot.jte";

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
   * 실데이터 모양 그대로의 행.
   *
   * <p>{@code avgCost} 는 반올림된 값이고 {@code value}/{@code unrealizedProfit} 은 정밀값이다 &mdash; 실제 응답이
   * 그렇다(실측: 삼성전자 avgCost 71,886.79 인데 정밀 원가는 362,525,079).
   */
  private static HoldingsSnapshotItem item(
      String name, String quantity, String avgCost, String value, String unrealized) {
    return new HoldingsSnapshotItem(
        UUID.randomUUID(),
        name,
        "000000",
        bd(quantity),
        bd(avgCost),
        BigDecimal.ONE,
        LocalDate.parse("2026-08-19"),
        bd(value),
        bd(unrealized));
  }

  private String render(List<HoldingsSnapshotItem> holdings) {
    Map<String, Object> model = new HashMap<>();
    model.put("holdings", holdings);
    model.put("date", "2026-08-19");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  @Test
  void 합계_세_칸이_서로_닫힌다() {
    // 반올림된 avgCost 로 만들면 원금이 71,886.79 x 5,043 = 362,525,081.97 이 되어 3 원 어긋난다.
    // 정밀 원가는 평가액 − 평가손익 = 1,248,142,500 − 885,617,421 = 362,525,079 이다.
    List<HoldingsSnapshotItem> holdings =
        List.of(
            item("삼성전자", "5043", "71886.79", "1248142500", "885617421"),
            item("리츠인프라", "18037", "4914.95", "75935770", "-12715200"));

    String html = render(holdings);

    // 총원금 = 362,525,079 + 88,650,970 = 451,176,049
    assertThat(html).as("보유 스냅샷 표를 그리지 못했다 - 검사가 무력해진다").contains("삼성전자");
    assertThat(html).as("총원금이 표기용 avgCost 로 계산돼 정밀 원가와 어긋난다").contains("451,176,049");
    // 총평가 1,324,078,270 · 총평가손익 872,902,221 -> 1,324,078,270 − 451,176,049 = 872,902,221
    assertThat(html).contains("1,324,078,270");
    assertThat(html).contains("872,902,221");
  }

  @Test
  void 행_수익률도_같은_원가를_쓴다() {
    // 평가 1,248,142,500 · 평가손익 885,617,421 -> 원가 362,525,079 -> 244.29%
    // 반올림 avgCost 로 만든 362,525,081.97 을 쓰면 소수점 아래가 달라진다.
    String html = render(List.of(item("삼성전자", "5043", "71886.79", "1248142500", "885617421")));

    assertThat(html).as("행 수익률이 합계와 다른 원가로 계산됐다").contains("244.29");
  }
}
