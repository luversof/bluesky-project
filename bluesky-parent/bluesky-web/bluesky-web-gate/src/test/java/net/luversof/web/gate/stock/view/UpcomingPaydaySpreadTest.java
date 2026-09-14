package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * '다가올 배당' 의 예상일은 지급 이력의 최빈일 하나다 &mdash; 실제 지급일이 퍼져 있으면 그 폭을 함께 적는다.
 *
 * <p>실측 2026-09-11(지급 이력 210 건): 월중 지급일 분포가 <b>17 일 x39 · 19 일 x20 · 18 일 x10 · 20 일 x9</b> 였고 최근
 * 여섯 달은 17/17/19/17/20/19 였다. 카드에는 최빈일만 "9 월 17 일" 로 찍혀, 최근 두 달처럼 19~20 일에 들어오는 경우를 그 날로 읽게 된다. 월말
 * 창도 2 일 x52 · 4 일 x38 · 3 일 x24 · 5 일 x10 · 6 일 x5 · 7 일 x3 으로 퍼져 있다.
 *
 * <p>최빈일과 마지막 관측일이 같으면(폭이 없으면) 아무것도 적지 않는다.
 */
class UpcomingPaydaySpreadTest {

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  @Test
  void 최빈일과_마지막_관측일이_다를_때만_적는다() throws IOException {
    String controller =
        read(
            "src/main/java/net/luversof/web/gate/stock/controller/StockSummaryHtmxController.java");
    String template = read("src/main/jte/stock/htmx/fragments/upcomingDividends.jte");

    assertThat(controller).contains("payDaySpreadFrom");
    assertThat(controller)
        .as("폭이 없으면 0 을 넘겨 화면이 아무것도 그리지 않게 한다")
        .contains("spreadTo > spreadFrom ? spreadFrom : 0");
    assertThat(template).contains("@if(payDaySpreadFrom > 0 && payDaySpreadTo > payDaySpreadFrom)");
  }

  @Test
  void 두_번들_모두_문구를_가진다() throws IOException {
    for (String name : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String bundle = read("src/main/resources/" + name);
      assertThat(bundle).contains("stock.summary.upcoming.dividend.payday.spread");
    }
  }
}
