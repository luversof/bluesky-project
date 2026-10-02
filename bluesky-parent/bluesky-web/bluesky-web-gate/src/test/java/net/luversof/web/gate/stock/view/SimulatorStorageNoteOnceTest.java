package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 시뮬레이터의 저장 안내(localStorage)는 한 화면에 한 번 - 머리 상자에만.
 *
 * <p>실측 2026-10-01(1440px): 지속가능성 탭은 머리 상자와 "가정 입력" 카드 배지에, 적립식 복리 탭은 머리 상자와 입력 카드 문단에 같은 문장이 두 번
 * 적혀 있었다. 배지는 두 줄짜리 큰 알약이 되어 카드 설명 옆을 눌렀다.
 */
class SimulatorStorageNoteOnceTest {

  private static String read(String path) throws IOException {
    return Files.readString(Path.of("src/main/jte/stock/" + path), StandardCharsets.UTF_8);
  }

  @Test
  void 저장_안내는_머리_상자에만_있다() throws IOException {
    String simulator = read("simulator.jte");
    int first = simulator.indexOf("${storageNote}");
    assertThat(first).as("머리 상자의 안내").isPositive();
    assertThat(simulator.indexOf("${storageNote}", first + 1)).as("두 번째 안내").isEqualTo(-1);
    assertThat(simulator).doesNotContain("compoundSimulator(storageNote");

    assertThat(read("fragments/compoundSimulator.jte"))
        .as("적립식 복리 입력 카드")
        .doesNotContain("storageNote")
        .doesNotContain("storage.note");
  }
}
