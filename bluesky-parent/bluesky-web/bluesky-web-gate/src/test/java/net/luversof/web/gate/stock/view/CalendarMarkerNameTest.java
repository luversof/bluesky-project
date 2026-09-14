package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 활동 캘린더의 종류 표시는 색만으로 갈리면 안 된다.
 *
 * <p>실측 2026-09-12: 칸 하나가 {@code 4 ●8 ₩ 2,211,589} 처럼 읽혔다. {@code ●N} 세 가지(매수 {@code text-profit} ·
 * 매도 {@code text-loss} · 배당 {@code text-dividend})가 <b>색으로만</b> 갈려 있어, 낭독기에는 "● 8" 로만 들리고 색을 못 가르는
 * 사람에게도 뜻이 전해지지 않았다(WCAG 1.4.1).
 *
 * <p>보이는 글자(●N)는 그대로 두고 이름만 붙인다 &mdash; 같은 칸의 금액은 이미 {@code title} 을 갖고 있었다.
 */
class CalendarMarkerNameTest {

  private static final Path TEMPLATE =
      Path.of("src/main/jte/stock/htmx/fragments/activityList.jte");

  private int count(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }

  @Test
  void 세_표시가_모두_이름을_가진다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    for (String pair :
        new String[] {
          "dayBuyCount:buyLabel", "daySellCount:sellLabel", "dayDividendCount:dividendLabel"
        }) {
      String countVar = pair.split(":")[0];
      String labelVar = pair.split(":")[1];
      int at = template.indexOf("@if(" + countVar + " > 0)");
      assertThat(at).as(countVar + " 표시").isPositive();
      String line = template.substring(at, template.indexOf((char) 10, at));
      assertThat(line)
          .as(countVar + " 는 title 을 가져야 한다")
          .contains("title=" + (char) 34 + "${" + labelVar + "}");
      assertThat(line)
          .as(countVar + " 는 sr-only 로도 이름이 나가야 한다(title 만으로는 안 닿는다)")
          .contains(
              "<span class=" + (char) 34 + "sr-only" + (char) 34 + "> ${" + labelVar + "}</span>");
    }
  }

  /** 보이는 글자는 그대로 - 화면에서 ●N 이 사라지면 안 된다. */
  @Test
  void 보이는_표시는_그대로_둔다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(count(template, "●${day")).as("매수·매도·배당 세 곳").isEqualTo(3);
  }
}
