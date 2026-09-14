package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 거래 금액에는 손익 색을 쓰지 않는다.
 *
 * <p>이 저장소의 손익 색은 <b>빨강 = 수익 · 파랑 = 손실</b>이다. 그런데 활동 화면의 캘린더 날짜 상세와 타임라인은 그 색을 <b>현금 방향</b>(매수 = 나감
 * · 매도 = 들어옴)에 쓰고 있었다 &mdash; 이익이 난 매도가 손실 색으로 찍힌다.
 *
 * <p>실측 2026-09-11(2026-01-12 매도 ₩10,248,225):
 *
 * <pre>
 *   매매 상세 목록   매수·매도 금액 rgb(13,17,27) 기본색 / 실현손익만 text-profit
 *   활동 목록 뷰     매수·매도 금액 rgb(13,17,27) 기본색 / 배당만 text-dividend
 *   활동 캘린더·타임라인  매수 text-profit · 매도 text-loss rgb(49,89,196)   <- 어긋남
 * </pre>
 *
 * <p>같은 화면 목록 뷰의 규칙(거래는 기본색, 배당만 색)에 맞춘다.
 */
class ActivityAmountColorTest {

  private static final String TEMPLATE = "src/main/jte/stock/htmx/fragments/activityList.jte";

  private String read() throws IOException {
    return Files.readString(Path.of(TEMPLATE), StandardCharsets.UTF_8);
  }

  @Test
  void 캘린더_상세와_타임라인이_목록_규칙을_따른다() throws IOException {
    String template = read();

    assertThat(template)
        .contains("String detailAmountColor = detailIsTrade ? \"\" : \"text-dividend\";");
    assertThat(template).contains("String tlAmountColor = tlIsTrade ? \"\" : \"text-dividend\";");
  }

  @Test
  void 손익_색을_현금_방향에_쓰지_않는다() throws IOException {
    String template = read();

    for (String bad :
        new String[] {
          "detailIsBuy ? \"text-profit\" : \"text-loss\"",
          "tlIsBuy ? \"text-profit\" : \"text-loss\"",
        }) {
      assertThat(template).as(bad + " 는 이익 난 매도를 손실 색으로 찍는다").doesNotContain(bad);
    }
  }

  @Test
  void 목록_뷰의_규칙은_그대로다() throws IOException {
    assertThat(read())
        .as("목록 뷰는 배당만 색을 준다 - 이 규칙이 기준이다")
        .contains("\"DIVIDEND\".equals(activity.type()) ? \"text-dividend\" : \"\"");
  }
}
