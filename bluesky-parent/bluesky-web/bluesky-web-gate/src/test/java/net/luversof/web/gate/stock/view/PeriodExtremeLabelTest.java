package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * "지금 대비" 는 기간이 오늘로 끝날 때만 맞는 말이다.
 *
 * <p>기간 최고/최저 평가액 옆의 비율은 분모가 <b>기간 말 평가액</b>(closingValue)이다. 그런데 이름은 늘 "지금 대비" 였다.
 *
 * <p>실측 2026-09-12(2016 년만 보기): "기간 최고 26,408,200 · 지금 대비 -39.90%" 로 적혔다. 지금 총 평가액은 1,622,109,770
 * 이므로 이름대로면 +6,000% 대여야 한다. 실제 분모는 2016 년 말 평가액(약 15.87M)이었다 &mdash; 두 카드(최고 x0.6010, 최저 x1.1108)가
 * 같은 값을 가리켜 확인된다. 전체 기간에서는 기간 말이 곧 오늘이라 1,622,109,770 / 2,098,800,125 - 1 = -22.71% 로 화면과 정확히 맞는다.
 *
 * <p>값은 맞고 이름만 틀렸으므로 이름을 기간에 맞춰 고른다.
 */
class PeriodExtremeLabelTest {

  private static final String FRAGMENT =
      "src/main/jte/stock/htmx/fragments/assetGrowthPeriodReturnSummary.jte";

  @Test
  void 기간에_따라_이름을_고른다() throws IOException {
    String jte = flatten(Files.readString(Path.of(FRAGMENT), StandardCharsets.UTF_8));
    assertThat(jte).as("기간 말로 끝나는지 받아야 한다").contains(flatten("@param boolean closingIsToday"));
    assertThat(jte)
        .as("고정 이름을 그대로 쓰면 과거 기간에서 거짓말이 된다")
        .doesNotContain(
            flatten(
                "String vsNowLabel = MessageUtil.getMessage(\"stock.asset.growth.summary.vs.now\");"));
    assertThat(jte).contains(flatten("closingIsToday ? " + q("stock.asset.growth.summary.vs.now")));
    assertThat(jte).contains(flatten(q("stock.asset.growth.summary.vs.period.end")));
  }

  @Test
  void 호출부가_오늘로_끝나는지_넘긴다() throws IOException {
    String jte =
        flatten(
            Files.readString(
                Path.of("src/main/jte/stock/htmx/asset-growth.jte"), StandardCharsets.UTF_8));
    assertThat(jte)
        .contains(
            flatten(
                "closingIsToday = endDisplayLocal == null || !endDisplayLocal.isBefore(today)"));
  }

  @Test
  void 두_번들에_문구가_다_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(
              Path.of("src/main/resources").resolve(bundle), StandardCharsets.ISO_8859_1);
      assertThat(text).as(bundle).contains("stock.asset.growth.summary.vs.period.end");
      assertThat(text).as(bundle).contains("stock.asset.growth.summary.vs.now");
    }
  }

  private static String q(String s) {
    return (char) 34 + s + (char) 34;
  }

  /** spotless·들여쓰기에 묶지 않는다. */
  private static String flatten(String source) {
    StringBuilder sb = new StringBuilder();
    boolean space = false;
    for (char c : source.toCharArray()) {
      if (Character.isWhitespace(c)) {
        space = true;
        continue;
      }
      if (space && sb.length() > 0) {
        sb.append(' ');
      }
      space = false;
      sb.append(c);
    }
    return sb.toString();
  }
}
