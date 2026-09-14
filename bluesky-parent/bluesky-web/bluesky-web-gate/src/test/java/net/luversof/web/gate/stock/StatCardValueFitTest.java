package net.luversof.web.gate.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 지표 카드의 값은 카드 폭에 맞춰 줄어들어야 한다.
 *
 * <p>값 글자를 18px 에서 26px 로 키운 뒤, 좁은 카드에서 숫자가 카드 밖으로 삐져나왔다 &mdash; 실측 2026-09-10: 종목 상세 375px 5개 ·
 * 1024px 4개, 계좌 상세 375px 4개 카드에서 "1,359,088,500" 이 179px 을 요구하는데 카드 안쪽 폭은 128~145px 이었다. 종목 상세는 그
 * 때문에 <b>페이지 자체가 가로로</b> 밀렸다(360px 24 · 375px 17 · 390px 9 · 408px 0).
 *
 * <p>뷰포트 기준으로 줄이면 안 된다 &mdash; 같은 375px 이라도 1열이면 카드가 327px 로 넓다. 카드 자신을 컨테이너로 삼아야 열 수가 바뀌어도 맞는다.
 */
class StatCardValueFitTest {

  private static final Path MAIN_CSS = Path.of("src/main/frontend/main.css");

  private static final Path BUILT_CSS = Path.of("src/main/resources/static/main.css");

  @Test
  void statCardIsItsOwnContainer() throws IOException {
    String css = Files.readString(MAIN_CSS, StandardCharsets.UTF_8);
    int card = css.indexOf(".stat-card {");

    assertThat(card).as(".stat-card 규칙을 찾지 못했다").isGreaterThan(0);

    String rule = css.substring(card, css.indexOf("}", card));

    assertThat(rule).as("카드가 컨테이너가 아니면 카드 폭에 맞춰 줄일 수 없다").contains("container-type: inline-size");
  }

  @Test
  void narrowCardsGetASmallerValue() throws IOException {
    String css = Files.readString(MAIN_CSS, StandardCharsets.UTF_8);

    assertThat(css)
        .as("좁은 카드에서 값을 줄이는 컨테이너 질의가 사라지면 숫자가 카드 밖으로 나간다")
        .contains("@container (max-width: 12rem)");
  }

  @Test
  void builtCssCarriesTheContainerQuery() throws IOException {
    String built = Files.readString(BUILT_CSS, StandardCharsets.UTF_8);

    assertThat(built).as("빌드 산출물이 배포본이다 - npm run build 를 빠뜨리면 원본만 고쳐진다").contains("@container");
    assertThat(built).contains("container-type");
  }
}
