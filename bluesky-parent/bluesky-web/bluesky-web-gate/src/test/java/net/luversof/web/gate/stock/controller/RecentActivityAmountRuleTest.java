package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 대시보드 '최근 활동' 도 거래 금액에 손익 색을 쓰지 않고, 묶인 줄을 건수로 밝힌다.
 *
 * <p>실측 2026-09-11: 이 카드만 매도 금액을 {@code text-info}(rgb 32,98,171) 로, 매수 금액을 {@code
 * text-base-content/60} 으로 칠하고 있었다 &mdash; 파랑은 손실 색({@code text-loss} rgb 49,89,196) 과 같은 계열로 읽히고,
 * 같은 카드 안에서 매수만 흐려 위계도 어긋난다. 활동 목록 뷰·매매 상세 목록은 둘 다 거래 금액을 기본색으로 두고 배당만 색을 준다.
 *
 * <p>또 이 카드는 계좌를 보여주지 않는데(폭이 없다) 줄은 계좌를 가로질러 묶인 것이다 &mdash; 실측 2026-09-11: 이번 달 활동 7 줄 중 3 줄이 계좌 3
 * 개를 묶은 줄이었다. 그래서 {@code accountCount = 0} 으로 건수 배지를 단다.
 */
class RecentActivityAmountRuleTest {

  private static final Path CARD =
      Path.of("src/main/jte/stock/htmx/fragments/recentActivities.jte");

  private String card() throws IOException {
    return Files.readString(CARD, StandardCharsets.UTF_8);
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
  void 거래_금액에는_손익_색을_쓰지_않는다() throws IOException {
    String card = card();

    assertThat(card)
        .as("요약 칸 매도 금액에 파랑(text-info) 을 쓰면 안 된다")
        .doesNotContain("amount-value text-info");
    assertThat(card)
        .as("목록 줄 매도 금액에도 파랑을 쓰면 안 된다")
        .doesNotContain("amountColor = " + '"' + "text-info" + '"');
    assertThat(card)
        .as("매수 금액만 흐리게 두면 같은 카드 안에서 위계가 어긋난다")
        .doesNotContain("amountColor = " + '"' + "text-base-content/60" + '"');
    assertThat(card).as("배당만 색을 준다").contains("amountColor = " + '"' + "text-dividend" + '"');
  }

  @Test
  void 유형_배지의_색은_그대로_둔다() throws IOException {
    String card = card();

    assertThat(card).as("매수 배지").contains("badge-error badge-outline");
    assertThat(card).as("매도 배지").contains("badge-info badge-outline");
    assertThat(card).as("배당 배지").contains("badge-success badge-outline");
  }

  @Test
  void 묶인_줄은_건수로_밝힌다() throws IOException {
    String card = card();

    assertThat(count(card, "components.mergedRecordBadge(")).as("최근 활동 줄에 하나").isEqualTo(1);
    assertThat(card)
        .as("계좌를 보여주지 않는 화면이므로 accountCount 는 0")
        .contains("mergedRecordBadge(recordCount = activity.recordCount(), accountCount = 0)");

    // 배지를 종목명 줄에 두면 이름이 밀린다 - 실측 2026-09-11: 375px 에서 이름 폭이 48px 에서 11px 로 줄어
    // 한 글자도 못 읽었다(이름 전체는 196px). 수량/설명 줄에 두어야 이름 폭이 그대로다.
    // 2026-10-01: 종목명은 한 줄 잘림(truncate) 대신 두 줄까지(line-clamp-2) - 같은 운용사 종목끼리 구별이 안 됐다.
    int nameLine = card.indexOf("line-clamp-2 wrap-anywhere link link-hover");
    int subLine = card.indexOf("text-xs text-base-content/60 flex items-center gap-1");
    int badge = card.indexOf("components.mergedRecordBadge(");
    assertThat(nameLine).as("종목명 줄").isGreaterThan(0);
    assertThat(subLine).as("수량/설명 줄").isGreaterThan(nameLine);
    assertThat(badge).as("배지는 종목명 줄이 아니라 수량/설명 줄에 있어야 한다").isGreaterThan(subLine);
  }
}
