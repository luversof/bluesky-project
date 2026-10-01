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

  /**
   * 2026-10-01: 기준을 카드 폭(@container 480px)으로 바꿨다. 목록 최소 폭 11rem 으로 줄바꿈을 기다렸더니 1440px 대시보드 카드(약
   * 385px)에서는 요약 열이 옆에 남아 종목명 자리가 110px 뿐이었다("TIGER 배…" - 같은 운용사 종목끼리 구별이 안 됨). 좁은 카드에서는 늘 아래로.
   */
  @Test
  void 자리가_모자라면_요약이_아래_줄로_내려간다() throws IOException {
    String q = String.valueOf((char) 34);
    String markup = read();

    assertThat(markup)
        .as("카드 폭을 재는 상자 안에서, 좁으면 세로로 쌓고 넓을 때만 나란히")
        .contains(
            "<div class="
                + q
                + "@container"
                + q
                + "> <div class="
                + q
                + "flex flex-col gap-4 @[30rem]:flex-row"
                + q
                + ">");
    assertThat(markup)
        .as("목록 칸은 남은 폭을 다 쓴다")
        .contains("<div class=" + q + "flex-1 min-w-0" + q + ">");
  }

  /** 요약의 고정 폭(144px)은 나란히 놓일 때만 - 쌓일 때 고정 폭이면 목록 아래에서 오른쪽이 빈다. */
  @Test
  void 요약은_나란히_놓일_때만_고정_폭이다() throws IOException {
    assertThat(read())
        .contains("@[30rem]:w-36 @[30rem]:shrink-0 @[30rem]:border-t-0 @[30rem]:border-l")
        .doesNotContain("w-36 shrink-0 border-l border-base-200 pl-3")
        .as("쌓였을 때 요약 세 줄은 한 줄 세 칸 - 세로로 늘어놓으면 카드만 길어진다")
        .contains("grid grid-cols-3 gap-3 @[30rem]:block @[30rem]:space-y-2");
  }
}
