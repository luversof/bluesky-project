package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.poe.dto.PoeBaseItem;
import net.luversof.web.gate.poe.dto.PoeUniqueItem;
import net.luversof.web.gate.poe.dto.PoeUniqueVariant;

/** 변형마다 다른 베이스(10-09 C209). */
class PoeUniqueBaseTest {

  private static PoeBaseItem base(String name, String itemClass) {
    return new PoeBaseItem(
        name, name, name, itemClass, itemClass, null, 1, 0, 0, 0, null, null, null, List.of(),
        null);
  }

  private static final Map<String, PoeBaseItem> BASES =
      Map.of(
          "Topaz Ring", base("Topaz Ring", "Ring"),
          "Sapphire Ring", base("Sapphire Ring", "Ring"),
          "Onyx Amulet", base("Onyx Amulet", "Amulet"),
          "Energy Blade", base("Energy Blade", "One Hand Sword"));

  private static PoeUniqueVariant v(int index, String name) {
    return new PoeUniqueVariant(index, name, name, List.of(), null, List.of(), null, null, null);
  }

  private static PoeUniqueItem unique(String baseType, List<PoeUniqueVariant> variants) {
    return new PoeUniqueItem(
        "U", null, "u", baseType, null, "ring", 1, null, false, null, List.of(), null, List.of(),
        null, variants, 1, null, null, null, null, null, null, null, null);
  }

  @Test
  void 고른_변형의_베이스로_조인한다() {
    PoeUniqueItem item = unique("Topaz Ring", List.of(v(1, "Topaz Ring"), v(2, "Sapphire Ring")));
    assertThat(PoeUniqueBase.resolve(item, 2, BASES::get).name()).isEqualTo("Sapphire Ring");
    assertThat(PoeUniqueBase.resolve(item, 1, BASES::get).name()).isEqualTo("Topaz Ring");
    assertThat(PoeUniqueBase.resolve(item, null, BASES::get).name()).isEqualTo("Topaz Ring");
  }

  @Test
  void 분류가_다른_같은_이름은_베이스가_아니다() {
    PoeUniqueItem item = unique("Onyx Amulet", List.of(v(1, "Pre 3.20"), v(2, "Energy Blade")));
    assertThat(PoeUniqueBase.resolve(item, 2, BASES::get).name()).isEqualTo("Onyx Amulet");
    assertThat(PoeUniqueBase.resolve(item, 1, BASES::get).name()).isEqualTo("Onyx Amulet");
  }

  @Test
  void 베이스_목록_판별은_PoB_표기와_변형_베이스를_받는다() {
    PoeBaseItem boots =
        new PoeBaseItem(
            "Two-Toned Boots",
            "두 가지 색 장화",
            "BootsAtlas2",
            "Boots",
            null,
            null,
            70,
            0,
            0,
            0,
            new PoeBaseItem.Armour(126, 145, 126, 145, 0, 0, 0, 0, 0),
            null,
            null,
            List.of(),
            null);
    PoeUniqueItem beacon =
        unique(
            "Two-Toned Boots (Evasion/Energy Shield)",
            List.of(
                v(1, "Two-Toned Boots (Armour/Evasion)"),
                v(3, "Two-Toned Boots (Evasion/Energy Shield)")));
    assertThat(PoeUniqueBase.matches(beacon, boots)).isTrue();
    PoeUniqueItem esOnly = unique("Two-Toned Boots (Evasion/Energy Shield)", null);
    assertThat(PoeUniqueBase.matches(esOnly, boots)).isFalse();
    assertThat(PoeUniqueBase.sameBase("Maelstrom Staff", base("Maelström Staff", "Warstaff")))
        .isTrue();
    assertThat(PoeUniqueBase.sameBase("Topaz Ring", base("Sapphire Ring", "Ring"))).isFalse();
    PoeBaseItem twoStoneFc =
        new PoeBaseItem(
            "Two-Stone Ring",
            "쌍보석 반지",
            "Ring14",
            "Ring",
            null,
            null,
            20,
            0,
            0,
            0,
            null,
            null,
            null,
            List.of(
                new PoeBaseItem.ModLine(
                    "+(12-16)% to Fire and Cold Resistances", "화염 및 냉기 저항 +(12-16)%", null, null)),
            null);
    assertThat(PoeUniqueBase.sameBase("Two-Stone Ring (Fire/Cold)", twoStoneFc)).isTrue();
    assertThat(PoeUniqueBase.sameBase("Two-Stone Ring (Cold/Lightning)", twoStoneFc)).isFalse();
    PoeUniqueItem emblem = unique("Topaz Ring", List.of(v(1, "Topaz Ring"), v(2, "Sapphire Ring")));
    assertThat(PoeUniqueBase.matches(emblem, BASES.get("Sapphire Ring"))).isTrue();
  }

  @Test
  void 암시_줄은_베이스_문장을_쓴다() {
    PoeBaseItem staff =
        new PoeBaseItem(
            "Maelström Staff",
            "마엘스트롬 지팡이",
            "Staff17",
            "Warstaff",
            null,
            null,
            64,
            0,
            0,
            0,
            null,
            null,
            null,
            List.of(
                new PoeBaseItem.ModLine(
                    "+25% Chance to Block Attack Damage", "공격 피해 막기 확률 +25%", null, null)),
            null);
    List<String> en = List.of("+25% Chance to Block Attack Damage", "10% increased Damage");
    List<String> shown = List.of("+25%의 확률로 공격 피해를 막아냄", "피해 10% 증가");
    assertThat(PoeUniqueBase.implicitLines(shown, en, staff, true))
        .containsExactly("공격 피해 막기 확률 +25%", "피해 10% 증가");
    assertThat(PoeUniqueBase.implicitLines(en, en, staff, false)).isSameAs(en);
    assertThat(PoeUniqueBase.implicitLines(shown, en, null, true)).isSameAs(shown);
  }

  @Test
  void 기본_베이스가_없으면_null() {
    PoeUniqueItem item = unique("Fishing Rod", List.of(v(1, "Sapphire Ring")));
    assertThat(PoeUniqueBase.resolve(item, 1, BASES::get)).isNull();
  }
}
