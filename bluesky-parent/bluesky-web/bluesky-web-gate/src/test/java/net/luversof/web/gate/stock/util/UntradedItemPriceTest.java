package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint;

/**
 * 거래한 적 없는 종목의 "현재가 0" 은 거짓이었다.
 *
 * <p>실측 2026-09-11: 전체 86 종목 중 43 종목은 이 사용자의 거래 이력이 없다. 그중 기업은행(024110) 상세 화면은 현재가를 {@code 0} 으로
 * 찍었는데, 같은 종목의 가격 이력에는 <b>2026-04-03 종가 21,350</b> 이 있었다(api-stock {@code
 * /api/stockItem/{id}/priceHistory} 8 점). 값이 없는 것과 0 원인 것은 다르고, 여기서는 값이 있다 &mdash; 화면이 이미 그 이력으로 가격
 * 차트를 그린다.
 */
class UntradedItemPriceTest {

  private StockPriceHistoryPoint point(String day, String close) {
    return new StockPriceHistoryPoint(LocalDate.parse(day), new BigDecimal(close));
  }

  @Test
  void 마지막_종가_점을_고른다() {
    List<StockPriceHistoryPoint> history =
        List.of(
            point("2026-04-01", "22000"),
            point("2026-04-03", "21350"),
            point("2026-04-02", "21650"));

    StockPriceHistoryPoint last = StockPriceBasisUtil.lastPricePoint(history);

    assertThat(last).isNotNull();
    assertThat(last.tradeDate()).as("순서가 뒤섞여도 가장 늦은 날").isEqualTo(LocalDate.parse("2026-04-03"));
    assertThat(last.closePrice()).isEqualByComparingTo("21350");
  }

  @Test
  void 쓸_수_없는_점은_거른다() {
    assertThat(StockPriceBasisUtil.lastPricePoint(null)).isNull();
    assertThat(StockPriceBasisUtil.lastPricePoint(List.of())).isNull();

    List<StockPriceHistoryPoint> broken =
        new ArrayList<>(
            Arrays.asList(
                (StockPriceHistoryPoint) null,
                new StockPriceHistoryPoint(null, new BigDecimal("100")),
                new StockPriceHistoryPoint(LocalDate.parse("2026-04-03"), null),
                point("2026-04-04", "0")));
    assertThat(StockPriceBasisUtil.lastPricePoint(broken))
        .as("날짜·종가가 없거나 0 인 점은 현재가로 쓸 수 없다")
        .isNull();
  }

  /** 손익 행이 있으면 그 값이 이긴다 - 되돌림은 0 일 때만이다. */
  @Test
  void 되돌림은_현재가가_0_일_때만_건다() throws IOException {
    String controller =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java"),
            StandardCharsets.UTF_8);

    assertThat(controller).contains("if (currentPrice.signum() == 0 && lastPricePoint != null)");
    assertThat(controller)
        .as("날짜도 같이 되돌려야 멈춘 종가가 오늘 값처럼 보이지 않는다")
        .contains("if (priceBasisDate == null && lastPricePoint != null)");
  }
}
