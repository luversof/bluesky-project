package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 활동 캘린더의 날짜 상세도 계좌를 적어야 한다.
 *
 * <p>활동 한 줄은 (날짜 · 유형 · 종목 · 매매구분) 이 같은 활동을 <b>계좌를 가로질러</b> 묶은 것이라 한 줄이 여러 계좌를 뜻할 수 있다. 목록 뷰는 계좌를
 * 적는데 캘린더 날짜 상세는 적지 않아, 같은 줄이 어느 계좌 것인지 알 수 없었다.
 *
 * <p>실측 2026-09-11(2026-07-20, 활동 14건): 캘린더 상세와 목록의 7 줄·금액이 정확히 같았지만, 목록만 계좌를 보여 줬다 &mdash; "매수
 * TIGER 리츠부동산인프라 62주" 한 줄이 목록에서는 계좌 3 개(연금저축2 · 연금저축1 · +1)였다.
 *
 * <p>목록 뷰와 같은 표기를 쓴다: 최대 2 개를 배지로 적고 나머지는 {@code +N} 툴팁.
 */
class CalendarDetailAccountTest {

  private static final String TEMPLATE = "src/main/jte/stock/htmx/fragments/activityList.jte";

  private String read() throws IOException {
    return Files.readString(Path.of(TEMPLATE), StandardCharsets.UTF_8);
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
  void 날짜_상세가_계좌를_적는다() throws IOException {
    String template = read();

    assertThat(template).contains("data-cal-detail-accounts");
    assertThat(template)
        .as("계좌 id 로 이름을 찾는다(목록 뷰와 같은 규칙)")
        .contains("accountNames.getOrDefault(dayDetailActivity.accountIds().get(di)");
    assertThat(template)
        .as("계좌가 없으면 빈 자리를 만들지 않는다")
        .contains(
            "@if(dayDetailActivity.accountIds() != null && !dayDetailActivity.accountIds().isEmpty())");
  }

  @Test
  void 타임라인도_계좌를_적는다() throws IOException {
    String template = read();

    // 실측 2026-09-11: 타임라인 뷰는 계좌 표기가 0 개였다(같은 7 줄·금액·일 순액 -29,350 인데 계좌만 없음).
    assertThat(template).contains("data-timeline-accounts");
    assertThat(template).contains("accountNames.getOrDefault(tlRowActivity.accountIds().get(ti)");
    assertThat(template)
        .contains(
            "@if(tlRowActivity.accountIds() != null && !tlRowActivity.accountIds().isEmpty())");
  }

  @Test
  void 세_뷰가_같은_표기를_쓴다() throws IOException {
    String template = read();

    // 셋 다 '최대 2 개 + 나머지 +N' 이다.
    assertThat(count(template, "Math.min(2, activity.accountIds().size())")).isEqualTo(1);
    assertThat(count(template, "Math.min(2, dayDetailActivity.accountIds().size())")).isEqualTo(1);
    assertThat(count(template, "Math.min(2, tlRowActivity.accountIds().size())")).isEqualTo(1);
    assertThat(count(template, "cursor-help")).as("목록 1 + 캘린더 상세 1 + 타임라인 1").isEqualTo(3);
  }
}
