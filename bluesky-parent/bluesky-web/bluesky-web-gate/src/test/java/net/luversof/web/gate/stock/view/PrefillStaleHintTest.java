package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 스냅샷 폼이 원장과 어긋나면 폼에서도 알려 준다.
 *
 * <p>월배당 폼은 <b>저장된 스냅샷</b>을 그대로 채운다(라이브 보유량이 아니다 &mdash; 의도된 규약). 그래서 사람이 갱신하지 않은 사이 원장과 어긋날 수 있고,
 * 아래 표는 이미 "현재 N" 으로 그 사실을 말한다. 정작 값을 고치는 자리인 폼에는 아무 말이 없었다.
 *
 * <p>실측 2026-09-12(관리 &gt; 월배당 기준의 "이 값으로 시뮬레이터 채우기" 로 들어가 8 종목):
 *
 * <pre>
 *   476800 폼 4,914  ↔ 표 "현재 4,915"
 *   329200 폼 4,381  ↔ 표 "현재 4,367"
 *   0018C0 폼 11,307 ↔ 표 "현재 11,281"
 *   472150 폼 23,205 ↔ 차이 없음(경고도 없음)
 * </pre>
 *
 * <p>수량은 여덟 종목 모두 같았으므로 경고는 평균단가에만 떴다 &mdash; 조건이 값마다 따로 걸려 있다는 뜻이다.
 */
class PrefillStaleHintTest {

  private static final Path TEMPLATE =
      Path.of("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");

  @Test
  void 폼도_현재값과_어긋나면_말한다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template).contains("boolean formQuantityDiffers");
    assertThat(template).contains("boolean formAverageBuyPriceDiffers");
    assertThat(template).contains("@if(formQuantityDiffers)");
    assertThat(template).contains("@if(formAverageBuyPriceDiffers)");
    assertThat(template).contains("data-form-current-quantity");
    assertThat(template).contains("data-form-current-average-buy-price");
  }

  /** 표와 같은 판정이어야 한다 - 한쪽만 반올림하면 같은 값에 경고가 붙거나 빠진다. */
  @Test
  void 판정_규칙이_표와_같다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template)
        .as("평균단가는 원 단위 반올림으로 견준다")
        .contains("formCurrentAverageBuyPrice.setScale(0, java.math.RoundingMode.HALF_UP)");
    assertThat(countOf(template, "setScale(0, java.math.RoundingMode.HALF_UP)"))
        .as("표의 판정 + 폼의 판정(양쪽 값 각각)")
        .isGreaterThanOrEqualTo(3);
    assertThat(template)
        .as("문구는 표가 쓰는 것과 같은 키여야 한다")
        .contains("stock.simulator.monthly.table.cell.quantity.current")
        .contains("stock.simulator.monthly.table.cell.average.buy.price.current");
  }

  /** 종목을 못 찾으면 아무 말도 하지 않는다 - 없는 값으로 경고를 만들면 안 된다. */
  @Test
  void 종목을_못_찾으면_조용하다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template).contains("java.util.UUID formStockItemId = null;");
    assertThat(template).contains("Integer formCurrentQuantity = formStockItemId != null ?");
    assertThat(template)
        .contains("BigDecimal formCurrentAverageBuyPrice = formStockItemId != null ?");
  }

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
