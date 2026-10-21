package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** 빈 자리 조건(10-20 C519, craftofexile 계산기 'open affix') — 쪽마다 (한도 − 옵션 수) ≥ 요구 수, 옵션 목표와 함께. */
class PoeCraftOpenSlotTest {

  private static final PoeCraftCalcService.Target OPEN_P =
      new PoeCraftCalcService.Target(List.of(PoeCraftCalcService.OPEN_PREFIX));

  private static final PoeCraftCalcService.Target OPEN_S =
      new PoeCraftCalcService.Target(List.of(PoeCraftCalcService.OPEN_SUFFIX));

  private static final PoeCraftCalcService.Target LIFE =
      new PoeCraftCalcService.Target(List.of("IncreasedLife8"));

  // 레어 장갑: 접두 둘(생명력 · 방어도) · 접미 하나(냉기) — 빈 접두 1 · 빈 접미 2
  private static final PoeCraftEngine.Item RARE =
      new PoeCraftEngine.Item(
          "Gloves",
          "str_armour",
          "",
          84,
          PoeCraftEngine.Rarity.RARE,
          List.of(
              new PoeCraftEngine.Affix("IncreasedLife8", "prefix", List.of(), false, false),
              new PoeCraftEngine.Affix(
                  "LocalIncreasedPhysicalDamageReductionRatingPercent5",
                  "prefix",
                  List.of(),
                  false,
                  false),
              new PoeCraftEngine.Affix("ColdResist7", "suffix", List.of(), false, false)),
          false);

  private static List<PoeCraftCalcService.Target> of(PoeCraftCalcService.Target... ts) {
    return new ArrayList<>(List.of(ts));
  }

  @Test
  void 쪽마다_남은_자리만큼만_충족() {
    assertThat(PoeCraftCalcService.meets(RARE, of(LIFE, OPEN_P))).isTrue();
    assertThat(PoeCraftCalcService.meets(RARE, of(LIFE, OPEN_P, OPEN_P))).isFalse();
    assertThat(PoeCraftCalcService.meets(RARE, of(LIFE, OPEN_S, OPEN_S))).isTrue();
    assertThat(PoeCraftCalcService.meets(RARE, of(LIFE, OPEN_S, OPEN_S, OPEN_S))).isFalse();
    // 옵션 목표가 안 맞으면 빈 자리가 있어도 아니다
    assertThat(
            PoeCraftCalcService.meets(
                RARE, of(new PoeCraftCalcService.Target(List.of("FireResist8")), OPEN_S)))
        .isFalse();
    // 남은 목표 수 — 생명력 1 + 빈 접두 1(둘 요구 중) = 2
    assertThat(PoeCraftCalcService.matched(RARE, of(LIFE, OPEN_P, OPEN_P))).isEqualTo(2);
  }

  @Test
  void 빈_자리_조건만으로는_계산하지_않는다() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft-mods.json")), "추출 데이터 없음");
    PoeCraftDataService data = new PoeCraftDataService(dir);
    PoeCraftCalcService calc =
        new PoeCraftCalcService(
            new PoeCraftService(data, new PoeModDataService(dir)), new PoeCraftPriceService(dir));
    PoeCraftCalcService.Response r =
        calc.calc(new PoeCraftCalcService.Request(RARE, of(OPEN_P), null, null, null));
    assertThat(r.error()).contains("목표 옵션");
  }
}
