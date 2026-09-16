package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 활동 요약 카드의 값 자리는 <b>자동 검사가 짚을 수 있어야</b> 한다.
 *
 * <p>이 카드 셋(매매 현황 · 배당 수령 · 활동 합계)에는 식별 속성이 없어서, QA 탐침이 {@code [data-activity-summary]} 를 찾다 못 찾고
 * 매번 "건너뜀" 만 찍었다 &mdash; 검사가 <b>영구히 안 도는</b> 상태였다. 같은 화면의 달 카드는 이미 {@code
 * data-activity-month-summary} 를 달고 있고, 달력 항목({@code data-calendar-entry})과 관리 실행 시각({@code
 * data-admin-last-run})도 같은 관례다.
 *
 * <p>⚠ 이 카드의 '건' 은 예전에 <b>묶인 줄 수</b>를 세어 매수 149(실제 203)로 나갔던 자리다. 검사가 닿지 않으면 그 회귀를 아무도 못 본다.
 */
class ActivitySummaryProbePointsTest {

  private static final Path FRAGMENT =
      Path.of("src/main/jte/stock/htmx/fragments/activityList.jte");

  /** 카드가 말하는 아홉 값. 하나라도 빠지면 그만큼 검사가 줄어든다. */
  private static final Set<String> KINDS =
      new TreeSet<>(
          List.of(
              "buy-amount",
              "buy-count",
              "sell-amount",
              "sell-count",
              "dividend-amount",
              "dividend-count",
              "total-count",
              "trade-count",
              "total-dividend-count"));

  @Test
  void 요약_카드에_짚을_자리가_있다() throws IOException {
    String template = Files.readString(FRAGMENT, StandardCharsets.UTF_8);

    assertThat(template).as("카드 묶음을 가리키는 자리").contains("data-activity-summary=");

    Matcher m = Pattern.compile("data-activity-kind=\"([a-z-]+)\"").matcher(template);
    Set<String> found = new TreeSet<>();
    while (m.find()) {
      found.add(m.group(1));
    }
    // 같은 이름이 여러 자리에 있을 수 있으므로 **집합**으로 맞댄다.
    assertThat(found).as("카드가 짚어 주는 값 자리").isEqualTo(KINDS);
  }

  /**
   * 값 자리는 <b>계산된 값</b>을 감싼다.
   *
   * <p>속성만 붙이고 엉뚱한 자리를 감싸면 탐침은 통과하는데 검사는 헛돈다. 각 자리가 그 값을 내는 식을 품고 있는지 본다.
   */
  @Test
  void 값_자리가_그_값을_감싼다() throws IOException {
    // 빌드가 ${} 안 공백을 지우므로 공백을 눌러 비교한다.
    String squeezed = Files.readString(FRAGMENT, StandardCharsets.UTF_8).replaceAll("\\s+", "");

    assertThat(squeezed).as("매수 금액").contains("data-activity-kind=\"buy-amount\"");
    assertThat(squeezed).as("매수 금액 값").contains("df.format(safeBuy)");
    assertThat(squeezed).as("매도 금액 값").contains("df.format(safeSell)");
    assertThat(squeezed).as("배당 금액 값").contains("df.format(safeDividend)");
    assertThat(squeezed).as("매수 건수 값").contains("countMessage.apply(buyCount)");
    assertThat(squeezed).as("매도 건수 값").contains("countMessage.apply(sellCount)");
    assertThat(squeezed).as("배당 건수 값").contains("countMessage.apply(dividendCount)");
    assertThat(squeezed)
        .as("활동 합계는 세 가지를 더한다")
        .contains("countMessage.apply(buyCount+sellCount+dividendCount)");
    assertThat(squeezed)
        .as("매매 건수는 매수와 매도를 더한다")
        .contains("countMessage.apply(buyCount+sellCount)");
  }
}
