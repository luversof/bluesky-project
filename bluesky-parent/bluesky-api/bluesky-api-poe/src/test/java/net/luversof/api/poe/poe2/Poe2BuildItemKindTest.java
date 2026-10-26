package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * PoE2 빌드 아이템 줄 종류 · 아이템 레벨(10-23 Q117) — PoB2 앞 {태그} 중 화면이 가르는 것만, 'Item Level:' 은 Implicits 앞에서만.
 */
class Poe2BuildItemKindTest {

  @Test
  void 앞_태그_중_화면이_가르는_종류() {
    assertThat(Poe2BuildService.modKind("{rune}+10% to Fire Resistance")).isEqualTo("rune");
    assertThat(Poe2BuildService.modKind("{crafted}+44% to Cold Resistance")).isEqualTo("crafted");
    // 다른 태그(tags · range · variant) 뒤에 와도 찾는다
    assertThat(Poe2BuildService.modKind("{tags:life}{fractured}+50 to maximum Life"))
        .isEqualTo("fractured");
    assertThat(Poe2BuildService.modKind("{range:0.5}{enchant}Adds 1 to 2 Fire Damage"))
        .isEqualTo("enchant");
    assertThat(Poe2BuildService.modKind("{desecrated}+20 to Spirit")).isEqualTo("desecrated");
    assertThat(Poe2BuildService.modKind("{variant:1}{mutated}+1 to Level of all Skills"))
        .isEqualTo("mutated");
  }

  @Test
  void 태그_없거나_모르는_태그면_빈_문자열() {
    assertThat(Poe2BuildService.modKind("+12 to Strength")).isEmpty();
    assertThat(Poe2BuildService.modKind("{custom}+12 to Strength")).isEmpty();
    // 줄 가운데 중괄호는 태그가 아니다
    assertThat(Poe2BuildService.modKind("Grants {crafted} text")).isEmpty();
  }

  @Test
  void 아이템_레벨은_Implicits_앞에서만() {
    assertThat(
            Poe2BuildService.itemLevelOf(
                List.of(
                    "Rarity: RARE",
                    "Doom Grip",
                    "Titan Gauntlets",
                    "Item Level: 82",
                    "Implicits: 0")))
        .isEqualTo(82);
    assertThat(
            Poe2BuildService.itemLevelOf(List.of("Rarity: RARE", "Implicits: 1", "Item Level: 5")))
        .isNull();
    assertThat(
            Poe2BuildService.itemLevelOf(List.of("Rarity: MAGIC", "Item Level: x", "Implicits: 0")))
        .isNull();
  }
}
