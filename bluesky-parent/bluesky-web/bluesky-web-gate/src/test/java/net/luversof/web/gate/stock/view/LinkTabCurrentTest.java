package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 주소를 바꾸는 링크형 탭은 지금 어디인지 표식을 갖는다.
 *
 * <p>실측 2026-09-12: 활동 화면의 보기 전환은 {@code role=tablist/tab + aria-selected + aria-controls +
 * tabpanel} 까지 갖췄는데, <b>시뮬레이터(3 탭)와 관리(2 탭)</b>는 {@code role} 도 {@code aria-selected} 도 {@code
 * aria-current} 도 없어 지금 어느 탭인지 시각 스타일({@code tab-active})로만 알렸다.
 *
 * <p>이 둘은 주소를 바꾸는 링크라 tab 역할을 붙이면 키보드 규약(좌우 화살표 이동)까지 맞춰야 한다. 링크형 탭의 표준 표식인 {@code
 * aria-current="page"} 를 쓴다 &mdash; 값이 없으면 JTE 가 속성을 통째로 뺀다.
 */
class LinkTabCurrentTest {

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
  void 시뮬레이터_세_탭이_현재를_알린다() throws IOException {
    String template =
        Files.readString(Path.of("src/main/jte/stock/simulator.jte"), StandardCharsets.UTF_8);

    assertThat(count(template, "aria-current=" + (char) 34 + "${")).as("탭 셋").isEqualTo(3);
    for (String flag : new String[] {"sustainabilityTab", "monthlyDividendTab", "compoundTab"}) {
      assertThat(template)
          .as(flag + " 탭")
          .contains(
              "aria-current="
                  + (char) 34
                  + "${"
                  + flag
                  + " ? "
                  + (char) 34
                  + "page"
                  + (char) 34
                  + " : null}");
    }
  }

  @Test
  void 관리_두_탭이_현재를_알린다() throws IOException {
    String template =
        Files.readString(Path.of("src/main/jte/stock/admin.jte"), StandardCharsets.UTF_8);

    assertThat(count(template, "aria-current=" + (char) 34 + "${")).as("탭 둘").isEqualTo(2);
    assertThat(template).contains("\"data-management\".equals(adminTab) ? \"page\" : null");
    assertThat(template).contains("\"monthly-reference\".equals(adminTab) ? \"page\" : null");
  }
}
