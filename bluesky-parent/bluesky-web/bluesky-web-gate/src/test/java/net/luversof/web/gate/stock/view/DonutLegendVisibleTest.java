package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 도넛 차트 범례가 좁은 카드에서 <b>사라지지 않는다</b>.
 *
 * <p>캔버스는 {@code flex-shrink-0 w-[200px]} 로 줄지 않는데 범례는 {@code w-0 flex-1} 이었다. 카드가 좁아지면 남는 폭이 음수가
 * 되어 범례 폭이 <b>0px</b> 이 되고, 종목별 비중이 통째로 안 보였다 &mdash; 실측 2026-09-15(640px, 2열 배치): 카드 203px 안에 캔버스
 * 200px + gap 12px 라 범례가 0px. 배당 · 매매 두 화면이 같은 모양이었다.
 *
 * <p>자리가 모자라면 <b>아래 줄로 내려가게</b> 한다({@code flex-wrap} + 범례 최소 폭). 375 · 414px 에서는 카드가 넓어 멀쩡했기 때문에,
 * 폭을 몇 개만 재면 놓친다.
 *
 * <p>최소 폭은 카드 안쪽을 넘지 않게 {@code min(10rem, 100%)} 다. {@code 10rem} 만 두면 글꼴을 키울 때 같이 커진다 &mdash; 실측
 * 2026-09-17(브라우저 기본 글꼴 32px, 320px): 범례 최소 폭이 320px 이 되어 카드 밖으로 65px 삐져 매매 · 배당 · 배당 이번달 페이지가 가로로
 * 밀렸다.
 */
class DonutLegendVisibleTest {

  private static final List<String> SHELLS =
      List.of(
          "src/main/jte/stock/htmx/fragments/components/doughnutShell.jte",
          "src/main/jte/stock/htmx/fragments/dividend/dividendCharts.jte");

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  @Test
  void 자리가_모자라면_범례가_아래_줄로_내려간다() throws IOException {
    for (String shell : SHELLS) {
      String markup = read(shell);

      assertThat(markup)
          .as(shell + ": 캔버스가 안 줄어드는데 줄바꿈까지 막으면 범례가 0px 이 된다")
          .contains("flex flex-wrap gap-3");
      assertThat(markup)
          .as(shell + ": 범례에 최소 폭이 없으면 줄바꿈 없이 그대로 찌부러진다")
          .contains("flex flex-col min-w-[min(10rem,100%)] flex-1 h-[200px] overflow-hidden")
          .doesNotContain("flex flex-col w-0 flex-1 h-[200px]")
          .as(shell + ": 상한 없는 rem 최소 폭은 큰 글꼴에서 카드보다 넓어진다")
          .doesNotContain("min-w-[10rem]");
    }
  }

  @Test
  void 산출물에_상한_있는_최소_폭_규칙이_있다() throws IOException {
    assertThat(
            Files.readString(Path.of("src/main/resources/static/main.css"), StandardCharsets.UTF_8))
        .as("메이븐은 프론트엔드를 빌드하지 않는다 - npm run build 가 클래스를 규칙으로 만든다")
        .contains(".min-w-\\[min\\(10rem\\,100\\%\\)\\]{min-width:min(10rem,100%)}");
  }

  /** 캔버스 쪽 고정 폭은 그대로 둔다 - 그게 줄면 차트가 뭉개진다. 이 검사가 헛돌지 않게 짝을 확인한다. */
  @Test
  void 캔버스는_여전히_고정_폭이다() throws IOException {
    for (String shell : SHELLS) {
      assertThat(read(shell))
          .as(shell + ": 이 검사의 전제(캔버스가 안 줄어든다)가 사라지면 위 검사도 뜻을 잃는다")
          .contains("flex-shrink-0 w-[200px] h-[200px]");
    }
  }
}
