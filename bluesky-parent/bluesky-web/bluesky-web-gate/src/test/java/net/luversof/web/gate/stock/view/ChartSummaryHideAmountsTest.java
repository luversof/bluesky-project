package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 금액 가리기는 <b>차트 요약(sr-only)</b>까지 가려야 한다.
 *
 * <p>가리기는 {@code filter: blur(10px)} 라 화면만 흐려진다. 차트마다 붙는 sr-only 요약은 글자라 보조기술에는 그대로 읽혔다 &mdash; 실측
 * 2026-09-11(가리기를 켠 채 9 화면): 요약 <b>14 곳</b>이 금액을 담고 있었다(예: "투자원금: 39개 지점, 처음 2026-01-01
 * 637,902,360, 끝 ..."). 라벨과 비중(%)은 화면에서도 안 가리므로 그대로 두고 금액만 뺀다.
 */
class ChartSummaryHideAmountsTest {

  private static final Path SOURCE = Path.of("src/main/frontend/src/common.ts");
  private static final Path BUILT = Path.of("src/main/resources/static/js/common.js");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 요약이_가리기_상태를_본다() throws IOException {
    String source = read(SOURCE);

    assertThat(source).contains("function chartSummaryAmountsHidden");
    assertThat(source)
        .contains("classList.contains(" + (char) 34 + "hide-amounts" + (char) 34 + ")");
    assertThat(source).contains("개 지점(금액 가림)");
    assertThat(source).contains("points (amounts hidden)");
  }

  @Test
  void 토글이_바뀌면_이미_그린_요약도_다시_쓴다() throws IOException {
    String source = read(SOURCE);

    // 토글은 클래스만 바꾸고 차트는 갱신되지 않는다 - 관찰자가 없으면 켠 뒤에도 옛 요약이 그대로 남는다.
    assertThat(source).contains("function resyncChartSummaries");
    assertThat(source).contains("attributeFilter: [" + (char) 34 + "class" + (char) 34 + "]");
    // 호출을 주석 처리해도 문자열은 남는다 - 줄 단위로 못박는다(이번에 변이가 통과했다).
    assertThat(source.lines().map(String::strip).toList())
        .as("관찰자를 시작하지 않으면 토글을 켜도 옛 요약이 그대로 남는다")
        .contains("watchHideAmounts();");
  }

  @Test
  void 근거_문구의_금액도_가린다() throws IOException {
    String source = read(SOURCE);
    String summary = read(Path.of("src/main/jte/stock/htmx/fragments/summary.jte"));

    // 백분율 옆 근거 문구는 가림 대상 요소가 아니라, 켠 채로 hover 하면 금액이 그대로 보였다(실측 2026-09-11).
    assertThat(summary).contains("data-amount-basis=");
    assertThat(source).contains("function maskAmountBasis");
    assertThat(source.lines().map(String::strip).toList())
        .as("가리기 토글에서도 다시 칠해야 한다")
        .contains("maskAmountBasis();");
  }

  @Test
  void 빌드_산출물에도_들어_있다() throws IOException {
    String built = read(BUILT);
    // 압축기가 한글을 유니코드 이스케이프(역슬래시 u 네자리)로 바꾼다 - 한글 그대로 찾으면 늘 실패한다(이번에 한 번 걸렸다).
    String esc = String.valueOf((char) 92) + "u";

    assertThat(built)
        .as("금액 가림")
        .contains(esc + "AE08" + esc + "C561 " + esc + "AC00" + esc + "B9BC");
    assertThat(built).contains("amounts hidden");
  }
}
