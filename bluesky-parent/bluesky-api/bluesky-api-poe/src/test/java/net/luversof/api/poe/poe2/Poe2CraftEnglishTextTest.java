package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** PoE2 영어 화면 문구(10-22 C561 ⑦b, PoE1 C557 짝) — 상태 설명 · 반쪽 재조합 줄의 영어 판에 한글이 없고 뜻이 같다. */
class Poe2CraftEnglishTextTest {

  private static final Pattern HANGUL = Pattern.compile("[가-힣]");

  @Test
  void 상태_설명_영어_판() {
    assertThat(Poe2CraftOptimizer.describeEn("R|5|1,0|2|Dp", 3))
        .isEqualTo(
            "Rare · targets 1·3 hit · other mods prefix 1/suffix 0 · target 2 slot blocked by other"
                + " mods · unrevealed desecrated prefix");
    assertThat(Poe2CraftOptimizer.describeEn("M|0|0,0|0|", 2)).isEqualTo("Magic · no targets hit");
    // 한국어 판은 그대로(자가검사)
    assertThat(HANGUL.matcher(Poe2CraftOptimizer.describe("R|5|1,0|2|Dp", 3)).find()).isTrue();
  }

  @Test
  void 반쪽_재조합_줄_영어_판() {
    Poe2CraftCalcService.Method a =
        new Poe2CraftCalcService.Method(
            "CHAOS",
            "연금 → 카오스",
            10,
            5,
            0.5,
            Map.of("Chaos Orb", 2.0),
            1.0,
            null,
            "Alchemy → Chaos",
            null);
    Poe2CraftCalcService.Method b =
        new Poe2CraftCalcService.Method(
            "ALCHEMY", "연금 연타", 10, 5, 0.5, Map.of("Orb of Alchemy", 2.0), 1.0, null);
    Poe2CraftCalcService.Method m =
        Poe2CraftCalcService.combineHalves(a, b, 0.5, true, 50, "접두 · 접미", 0.1);
    assertThat(m.labelEn())
        .isEqualTo("Half recombination (prefix · suffix, recombine 50% + Omen of Recombination)");
    // 영어 라벨이 없는 반쪽은 한국어 라벨로(번역 칸이 없는 옛 결과)
    assertThat(m.noteEn()).startsWith("Half 1: Alchemy → Chaos · Half 2: 연금 연타");
    assertThat(HANGUL.matcher(m.label()).find()).isTrue();
  }
}
