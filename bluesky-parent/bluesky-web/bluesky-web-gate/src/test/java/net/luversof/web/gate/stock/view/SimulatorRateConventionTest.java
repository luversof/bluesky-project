package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 두 시뮬레이터는 연 입력을 서로 다르게 월 이율로 바꾼다 &mdash; 각자 그 사실을 화면에 적는다.
 *
 * <p>실측 2026-09-11:
 *
 * <ul>
 *   <li>은퇴 시뮬레이터(기본 탭)는 {@code (1+연이율)^(1/12)-1} &mdash; 연 4% 입력에 월 0.3279%, 12 개월 복리 4.001%. 즉 입력이
 *       <b>실효</b> 연이율이다.
 *   <li>복리 계산기 탭은 {@code 연이율/12} &mdash; 연 12% 입력이 실효 연 12.68% 로 돈다(1 년차 수익 341,252 원으로 확인).
 * </ul>
 *
 * <p>규약이 다른 것 자체는 흔한 일이지만, 화면이 말하지 않으면 두 탭의 같은 숫자를 같은 뜻으로 읽게 된다.
 */
class SimulatorRateConventionTest {

  private static final Path SIMULATOR = Path.of("src/main/frontend/src/stock/stockSimulator.ts");
  private static final Path COMPOUND = Path.of("src/main/frontend/src/stock/compoundSimulator.ts");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 은퇴_시뮬레이터는_기하_변환을_쓴다() throws IOException {
    assertThat(read(SIMULATOR).replace(" ", ""))
        .contains("Math.pow(1+numericAnnualRate,1/MONTHS_PER_YEAR)-1");
  }

  @Test
  void 복리_계산기는_연이율을_12로_나눈다() throws IOException {
    assertThat(read(COMPOUND).replace(" ", "")).contains("rate/12");
  }

  @Test
  void 두_규약_모두_화면_문구에_적혀_있다() throws IOException {
    for (String name : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String bundle = read(Path.of("src/main/resources").resolve(name));
      assertThat(bundle).as(name + " - 은퇴 시뮬레이터: 입력이 실효 연이율이라는 것").contains("^(1/12)-1");
      assertThat(bundle)
          .as(name + " - 복리 계산기: 실효 연이율 안내")
          .contains("stock.simulator.compound.effective.rate");
    }
  }
}
