package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * "스냅샷 이후 보유 수량이 바뀐 종목 N개 - 현재 수량 기준 금액" 안내 &mdash; 금액이 든 문장이라 금액 가리기 대상이다.
 *
 * <p>2026-09-29 보유가 바뀌어 이 안내가 처음 나타나자 가리기를 켜도 금액이 그대로 보였다(week-regression hide-amounts 4 화면). 같은
 * 안내가 세 곳에 있었고 금액 표기도 "₩3,228,752" · "3,228,752원" · "2,272,379" 로 갈려 있었다 - 표기는 로케일을 따르는 {@code
 * fullKrw} 하나로.
 */
class StaleQuantityNoticeTest {

  @Test
  void 세_곳_모두_가리기_대상이고_같은_표기다() throws IOException {
    for (String path :
        new String[] {
          "src/main/jte/stock/fragments/monthlyDividendSimulator.jte",
          "src/main/jte/stock/fragments/upcomingDividendSchedule.jte",
          "src/main/jte/stock/htmx/fragments/upcomingDividends.jte"
        }) {
      String source = Files.readString(Path.of(path), StandardCharsets.UTF_8);
      int key = source.indexOf("stock.summary.upcoming.dividend.stale.quantity");
      assertThat(key).as(path).isPositive();
      int open = source.lastIndexOf("<div", key);
      String tag = source.substring(open, source.indexOf('>', open));
      assertThat(tag)
          .as(path + " 안내 상자")
          .contains("amount-value")
          .contains("data-stale-quantity-notice");
      String call = source.substring(key, source.indexOf("</div>", key));
      assertThat(call).as(path + " 금액 표기").contains("StockFormatUtil.fullKrw(");
    }
  }
}
