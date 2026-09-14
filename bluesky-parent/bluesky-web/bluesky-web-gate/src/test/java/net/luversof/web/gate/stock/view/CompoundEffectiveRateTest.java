package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 복리 시뮬레이터는 월 이율을 연 수익률 / 12 로 잡는다 &mdash; 그 결과(실효 연이율)를 화면에 적는다.
 *
 * <p>실측 2026-09-11: 연 12% · 월 50 만원 · 20 년 · 기말 납입으로 1 년차 수익이 341,252 원인데, 이는 월 이율 1%(= 12/12)로 열두
 * 번 복리한 값이다(500,000 x ((1.01^12 - 1) / 0.01) = 6,341,252). 즉 입력 12% 는 실효 연 12.68% 로 돌아간다.
 *
 * <p>기제("연 수익률을 12로 나눈 월 이율")는 이미 적혀 있었지만 그 <b>결과</b>가 없었다. 다른 상품의 연 수익률과 그대로 견주면 20 년 뒤 최종 자산이
 * 8.6% 부풀어 보인다. 계산은 그대로 두고 실효 연이율만 함께 보여 준다.
 */
class CompoundEffectiveRateTest {

  private static final Path TEMPLATE =
      Path.of("src/main/jte/stock/fragments/compoundSimulator.jte");
  private static final Path BUILT =
      Path.of("src/main/resources/static/js/stock/compoundSimulator.js");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 월복리_안내에_실효_연이율_자리가_있다() throws IOException {
    String template = read(TEMPLATE);

    assertThat(template).contains("stock.simulator.compound.effective.rate");
    assertThat(template).contains("data-compound-effective");
  }

  @Test
  void 빌드된_스크립트가_실효_연이율을_계산한다() throws IOException {
    String built = read(BUILT);

    assertThat(built)
        .as("메이븐은 프론트엔드를 빌드하지 않는다 - npm run build 산출물이 배포본이다")
        .contains("data-compound-effective");
    // 빌드 산출물은 공백을 지운다(식별자는 보존). 공백을 뺀 뒤 비교한다.
    assertThat(built.replace(" ", ""))
        .as("(1 + 연이율/12)^12 - 1")
        .contains("Math.pow(1+annualPct/100/12,12)");
  }

  @Test
  void 계산_규칙_자체는_그대로_둔다() throws IOException {
    String built = read(BUILT);

    assertThat(built.replace(" ", "")).as("월 이율은 연 수익률 / 12 그대로").contains("rate/12");
  }

  @Test
  void 두_번들_모두_문구를_가진다() throws IOException {
    for (String name : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String bundle = read(Path.of("src/main/resources").resolve(name));
      assertThat(bundle).contains("stock.simulator.compound.effective.rate");
    }
  }
}
