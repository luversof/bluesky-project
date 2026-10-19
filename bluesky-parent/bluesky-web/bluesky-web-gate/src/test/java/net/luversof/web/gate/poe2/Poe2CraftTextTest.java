package net.luversof.web.gate.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** PoE2 제작 엔진 문구의 영어(10-18 C447). */
class Poe2CraftTextTest {

  @Test
  void 인게임_문구는_게임_영어_원문() {
    assertThat(Poe2CraftText.en("아이템이 축성됨")).isEqualTo("Item is Sanctified");
    assertThat(Poe2CraftText.en("유효하지 않은 징조 조합입니다."))
        .isEqualTo("This combination of Omens is not valid");
    assertThat(Poe2CraftText.en("다른 접두어 속성 부여를 추가할 수 없습니다."))
        .isEqualTo("This item may not have another prefix mod.");
  }

  @Test
  void 값이_끼는_문구() {
    assertThat(Poe2CraftText.en("모르는 화폐: FOO")).isEqualTo("Unknown currency: FOO");
    assertThat(Poe2CraftText.en("단계를 하나 이상 넣으세요")).isEqualTo("Add at least one step");
    assertThat(Poe2CraftText.en("아이템 레벨 64 이하에만 쓸 수 있는 뼈입니다"))
        .isEqualTo("Item level 64 or lower only for this bone");
    assertThat(Poe2CraftText.en("아이템 레벨 75 이상이어야 붙는 옵션입니다"))
        .isEqualTo("This modifier needs item level 75 or higher");
    assertThat(
            Poe2CraftText.en(
                "다른 접두어 속성 부여를 추가할 수 없습니다. 다른 접미어 속성 부여를 추가할 수 없습니다. — 접두 · 접미가 각 1개로 가득 찼습니다"))
        .endsWith("— prefix and suffix slots are full (1 each)");
  }

  @Test
  void 모르는_문구와_null_은_그대로() {
    assertThat(Poe2CraftText.en("처음 보는 문구")).isEqualTo("처음 보는 문구");
    assertThat(Poe2CraftText.en((String) null)).isNull();
    assertThat(Poe2CraftText.en(Map.of("CHAOS", "레어 아이템에만 쓸 수 있습니다")))
        .containsEntry("CHAOS", "Rare items only");
    assertThat(Poe2CraftText.en((Map<String, String>) null)).isEmpty();
  }
}
