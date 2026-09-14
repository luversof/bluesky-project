package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 활동 화면의 세 뷰가 세는 "건" 은 모두 묶이기 전 원본 건수여야 한다.
 *
 * <p>활동 타임라인은 (날짜 · 유형 · 종목 · 매매구분) 이 같은 활동을 계좌를 가로질러 한 줄로 합친다. 2026-09-10 에 머리말 카드는 {@code
 * recordCount} 로 고쳤지만 세 자리가 남아 있었다 &mdash; 캘린더 월 카드의 "N건", 캘린더 날짜 칸의 "●N", 타임라인 하루 요약의 "매수 N건 · 배당
 * M건".
 *
 * <p>실측 2026-09-11(패널 안의 숫자만 더한 값):
 *
 * <pre>
 *   올해      머리말 214 / 캘린더 월합 114 / 타임라인 일합 114   (차이 100)
 *   전체 기간 머리말 460 / 캘린더 월합 310 / 타임라인 일합 310   (차이 150)
 * </pre>
 *
 * <p>114·310 은 각각 목록 뷰의 <b>행 수</b> 와 정확히 같다 &mdash; 셋 다 줄을 세고 있었다는 뜻이다. 목록 뷰는 행을 "건" 이라고 적지 않으므로
 * 그대로 둔다.
 */
class ActivityViewCountTest {

  private static final String TEMPLATE = "src/main/jte/stock/htmx/fragments/activityList.jte";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

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
  void 캘린더_월_카드는_원본_건수를_센다() throws IOException {
    String template = read(TEMPLATE);

    assertThat(template)
        .as("List::size 는 묶인 줄 수라 머리말과 어긋난다")
        .doesNotContain("mapToInt(java.util.List::size).sum()");
    assertThat(template).contains("mapToInt(Activity::recordCount).sum()");
  }

  @Test
  void 캘린더_날짜_칸과_타임라인_하루도_원본_건수를_센다() throws IOException {
    String template = read(TEMPLATE);

    assertThat(template).contains("int dayRecords = dayActivity.recordCount();");
    assertThat(template).contains("int tlRecords = tlActivity.recordCount();");

    assertThat(count(template, "dayBuyCount += dayRecords")).isEqualTo(1);
    assertThat(count(template, "daySellCount += dayRecords")).isEqualTo(1);
    assertThat(count(template, "dayDividendCount += dayRecords")).isEqualTo(1);
    assertThat(count(template, "tlBuyCount += tlRecords")).isEqualTo(1);
    assertThat(count(template, "tlSellCount += tlRecords")).isEqualTo(1);
    assertThat(count(template, "tlDividendCount += tlRecords")).isEqualTo(1);
  }

  @Test
  void 줄을_세는_증가연산이_남아_있지_않다() throws IOException {
    String template = read(TEMPLATE);

    for (String counter :
        new String[] {
          "dayBuyCount++", "daySellCount++", "dayDividendCount++",
          "tlBuyCount++", "tlSellCount++", "tlDividendCount++"
        }) {
      assertThat(template).as(counter + " 는 한 줄을 한 건으로 센다").doesNotContain(counter);
    }
  }
}
