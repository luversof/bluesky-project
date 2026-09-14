package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 배당 내역 한 화면에 "기타" 가 둘이었고 뜻이 달랐다.
 *
 * <ul>
 *   <li>기간 배당금 카드의 기타 = <b>월중/월말 태그가 없는 종목</b>
 *   <li>월별 배당금 차트의 기타 = <b>총 배당 상위 8개 외 나머지 종목</b>
 * </ul>
 *
 * <p>실측 2026-09-12 &mdash; 올해: 카드 6,152,014원(3건) vs 차트 <b>10,899원</b>(565 배). 전체 기간:
 * 41,925,046원(68건) vs <b>829,848원</b>(50 배). 같은 낱말이 나란히 놓여 전혀 다른 수를 말하고 있었고, 차트 쪽에는 기준이 어디에도 없었다.
 *
 * <p>차트 라벨에 기준과 개수를 실어 가른다 &mdash; 자산 성장 기여 표가 이미 {@code 기타 {0}종목} 으로 쓰는 방식과 같다.
 */
class DividendChartOthersBasisTest {

  @Test
  void 차트_기타는_기준을_실은_문구를_쓴다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/tabsDividendHistory.jte"),
            StandardCharsets.UTF_8);
    assertThat(template)
        .as("맨 '기타' 로는 카드의 기타와 구분되지 않는다")
        .contains("stock.dividend.chart.others.basis")
        .doesNotContain("MessageUtil.getMessage(\"stock.chart.others\")");
  }

  @Test
  void 자리_채우기가_빌드_산출물에_들어가_있다() throws IOException {
    String built =
        Files.readString(
            Path.of("src/main/resources/static/js/stock/dividendHistory.js"),
            StandardCharsets.UTF_8);
    String flat = flatten(built);
    assertThat(flat).as("나머지 종목 수").contains("ranked.length-TOP");
    assertThat(flat).as("{0}=나머지 종목 수, {1}=상위 N").contains("{0}").contains("{1}");
  }

  @Test
  void 문구는_두_번들에_다_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8);
      assertThat(text).as(bundle).contains("stock.dividend.chart.others.basis");
      int at = text.indexOf("stock.dividend.chart.others.basis");
      String line =
          text.substring(at, text.indexOf(10, at) < 0 ? text.length() : text.indexOf(10, at));
      assertThat(line).as(bundle + " 는 자리 둘을 가져야 한다").contains("{0}").contains("{1}");
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
