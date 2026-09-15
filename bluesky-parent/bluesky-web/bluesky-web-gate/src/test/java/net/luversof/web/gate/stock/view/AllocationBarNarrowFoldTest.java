package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 보유 비중 막대는 아주 좁은 화면에서 <b>접는다</b>.
 *
 * <p>한 줄에 종목명(96px) · 비중(44px) · 금액(86px)이 모두 고정폭이고 막대만 {@code flex-1} 이라, 카드가 좁아지면 막대만 줄어든다
 * &mdash; 실측 2026-09-15(320px): 막대 폭 <b>4px</b>. 그 폭으로는 비중을 전혀 보여주지 못한다.
 *
 * <p>앞선 두 사례(도넛 범례 · 최근 활동)와 달리 <b>정보는 잃지 않았다</b> &mdash; 값은 옆의 백분율이 그대로 말한다. 그래서 줄바꿈 대신 <b>접기</b>를
 * 골랐다. 접히면 남는 셋이 양끝으로 퍼져야 하므로 {@code justify-between} 이 함께 필요하다.
 */
class AllocationBarNarrowFoldTest {

  private static final String FRAGMENT = "src/main/jte/stock/htmx/fragments/allocationBars.jte";

  private String read() throws IOException {
    return Files.readString(Path.of(FRAGMENT), StandardCharsets.UTF_8)
        .replaceAll("[ \t\r\n]+", " ");
  }

  @Test
  void 아주_좁으면_막대를_접는다() throws IOException {
    String markup = read();

    assertThat(markup)
        .as("4px 막대는 비중을 못 보여준다 - 자리가 없으면 접어야 한다")
        .contains("h-2 rounded-full bg-base-200 max-[400px]:hidden");
    assertThat(markup)
        .as("막대가 접히면 남는 셋이 양끝으로 퍼져야 한다")
        .contains("flex items-center justify-between gap-2 text-sm py-1.5");
  }

  /** 접어도 값은 남는다 - 이 검사의 전제라 함께 못 박는다. */
  @Test
  void 접혀도_백분율은_남는다() throws IOException {
    assertThat(read())
        .as("막대를 접는 근거는 옆의 백분율이 같은 값을 말한다는 것이다")
        .contains("tabular-nums w-11 text-right shrink-0");
  }
}
