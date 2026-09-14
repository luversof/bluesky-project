package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 활동 한 줄이 원본 여러 건을 묶었는데 그것을 숨기면 안 된다.
 *
 * <p>활동은 (날짜·유형·종목·매매구분) 이 같으면 계좌를 가로질러 한 줄로 묶인다. 계좌 칩은 한 계좌가 한 건일 때만 그 묶임을 설명한다 - 같은 계좌에 그날 두 건이
 * 있으면 칩은 그대로여서 줄이 말없이 더 많은 것을 대표한다.
 *
 * <p>실측 2026-09-11(전체 기간): 타임라인 310줄이 계좌 칩 452개를 달고 있었지만 원본은 460건이었다. 8건이 화면 어디에도 없었다. 그래서 칩으로 설명되지
 * 않는 줄(recordCount > accountCount)에만 건수를 덧붙인다 - 나머지는 칩이 이미 말하므로 배지가 소음이다.
 */
class ActivityMergedCountTest {

  private static final Path BADGE =
      Path.of("src/main/jte/stock/htmx/fragments/components/mergedRecordBadge.jte");
  private static final Path LIST = Path.of("src/main/jte/stock/htmx/fragments/activityList.jte");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
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
  void 칩으로_설명되는_줄에는_배지를_달지_않는다() throws IOException {
    String badge = read(BADGE);

    assertThat(badge)
        .as("계좌 칩이 이미 설명하는 묶임에는 배지를 달면 안 된다")
        .contains("recordCount > 1 && recordCount > accountCount");
    assertThat(badge).as("배지는 조건 안에서만 나와야 한다").contains("@if(merged)");
    assertThat(badge).as("건수 문구는 공용 메시지를 쓴다").contains("common.count");
    assertThat(badge).as("설명 문구도 메시지 키로").contains("stock.activity.merged.records");
    // 이 배지는 text-base-content/60 안에 들어가는 자리가 있어(대시보드 최근 활동) 투명도가 곱해진다 -
    // 실측 2026-09-11 axe: opacity-70 을 달았더니 fg #999b9f / bg #f9f9f9 = 2.64:1 로 serious 2건이었다.
    String markup = badge.substring(badge.indexOf("@if(merged)"));
    assertThat(markup).as("투명도를 겹치면 대비가 무너진다").doesNotContain("opacity-");
  }

  @Test
  void 세_뷰가_모두_같은_배지를_쓴다() throws IOException {
    String list = read(LIST);

    assertThat(count(list, "components.mergedRecordBadge(")).as("캘린더 상세·타임라인·목록 세 곳").isEqualTo(3);
    for (String var : new String[] {"dayDetailActivity", "tlRowActivity", "activity"}) {
      assertThat(list)
          .as(var + " 줄에 배지가 붙어야 한다")
          .contains("mergedRecordBadge(recordCount = " + var + ".recordCount()");
    }
  }

  @Test
  void 두_번들_모두_문구를_가진다() throws IOException {
    for (String name : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String bundle = read(Path.of("src/main/resources").resolve(name));
      assertThat(bundle).as(name + " 에 문구가 있어야 한다").contains("stock.activity.merged.records");
      assertThat(bundle).as(name + " 문구는 건수를 인자로 받아야 한다").contains("{0}");
    }
  }
}
