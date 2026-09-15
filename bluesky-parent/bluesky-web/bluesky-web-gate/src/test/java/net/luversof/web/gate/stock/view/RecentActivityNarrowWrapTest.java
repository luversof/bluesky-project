package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 대시보드 "최근 활동" 의 목록 칸이 좁은 화면에서 <b>찌부러지지 않는다</b>.
 *
 * <p>오른쪽 이번 달 요약이 {@code w-36 shrink-0} 이라 폭을 고수한다. 카드가 좁아지면 왼쪽 목록이 그만큼 눌리는데, 그 안에서 활동 배지(34px)와
 * 금액(66px)도 고정폭이라 <b>종목명이 0px</b> 이 되어 통째로 사라졌다 &mdash; 실측 2026-09-15(320px): 카드 254px 중 요약이 144px
 * 을 가져가 목록이 94px, 종목명 "KODEX 200타겟위클리커버드콜"(내용 186px)이 폭 0.
 *
 * <p>도넛 범례가 640px 에서 사라진 것과 <b>같은 모양</b>이다({@link DonutLegendVisibleTest}) &mdash; 고정폭 형제 옆의 {@code
 * flex-1} 칸은 자리가 모자라면 아래 줄로 내려가야 한다. 375px 이상에서는 멀쩡했으므로 폭을 성기게 재면 놓친다.
 */
class RecentActivityNarrowWrapTest {

  private static final String FRAGMENT = "src/main/jte/stock/htmx/fragments/recentActivities.jte";

  private String read() throws IOException {
    return Files.readString(Path.of(FRAGMENT), StandardCharsets.UTF_8)
        .replaceAll("[ \t\r\n]+", " ");
  }

  @Test
  void 자리가_모자라면_요약이_아래_줄로_내려간다() throws IOException {
    String markup = read();

    assertThat(markup)
        .as("요약이 폭을 고수하는데 줄바꿈까지 막으면 목록이 눌려 종목명이 0px 이 된다")
        .contains("<div class=" + String.valueOf((char) 34) + "flex flex-wrap gap-4");
    assertThat(markup)
        .as("목록 칸에 최소 폭이 없으면 줄바꿈 없이 그대로 찌부러진다")
        .contains(
            "<div class="
                + String.valueOf((char) 34)
                + "flex-1 min-w-[11rem]"
                + String.valueOf((char) 34));
  }

  /** 요약 쪽 고정 폭은 그대로 둔다 - 이 검사의 전제라 함께 못 박는다. */
  @Test
  void 요약은_여전히_고정_폭이다() throws IOException {
    assertThat(read()).as("요약이 안 줄어든다는 전제가 사라지면 위 검사도 뜻을 잃는다").contains("w-36 shrink-0 border-l");
  }
}
