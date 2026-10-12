package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** PoB 베이스 표기 → 게임 베이스(10-09 C209) — 발음 구별 기호 · 방어 유형 꼬리. */
class PoeBaseItemFindByNameTest {

  private static PoeBaseItem boots(String slug, int ar, int ev, int es) {
    return new PoeBaseItem(
        "Two-Toned Boots",
        "두 가지 색 장화",
        slug,
        "Boots",
        null,
        null,
        "armour",
        70,
        0,
        0,
        0,
        new PoeBaseItem.Armour(ar, ar, ev, ev, es, es, 0, 0, 0),
        null,
        null,
        List.of(),
        null);
  }

  private static final List<PoeBaseItem> ITEMS =
      List.of(
          boots("BootsAtlas1", 0, 145, 30),
          boots("BootsAtlas2", 145, 145, 0),
          boots("BootsAtlas3", 145, 0, 30),
          new PoeBaseItem(
              "Maelström Staff",
              "마엘스트롬 지팡이",
              "Staff17",
              "Staff",
              null,
              null,
              "weapon",
              64,
              113,
              0,
              113,
              null,
              null,
              null,
              List.of(),
              null));

  @Test
  void 방어_유형_꼬리로_같은_이름_베이스를_가른다() {
    assertThat(PoeBaseItemDataService.findByName(ITEMS, "Two-Toned Boots (Evasion/Energy Shield)"))
        .get()
        .extracting(PoeBaseItem::slug)
        .isEqualTo("BootsAtlas1");
    assertThat(PoeBaseItemDataService.findByName(ITEMS, "Two-Toned Boots (Armour/Evasion)"))
        .get()
        .extracting(PoeBaseItem::slug)
        .isEqualTo("BootsAtlas2");
    assertThat(PoeBaseItemDataService.findByName(ITEMS, "Two-Toned Boots (Armour/Energy Shield)"))
        .get()
        .extracting(PoeBaseItem::slug)
        .isEqualTo("BootsAtlas3");
  }

  private static PoeBaseItem twoStone(String slug, String implicit) {
    return new PoeBaseItem(
        "Two-Stone Ring",
        "쌍보석 반지",
        slug,
        "Ring",
        null,
        null,
        "accessory",
        20,
        0,
        0,
        0,
        null,
        null,
        null,
        List.of(new PoeBaseItem.ModLine(implicit, null, null, null)),
        null);
  }

  @Test
  void 원소_꼬리는_암시_옵션으로_가른다() {
    List<PoeBaseItem> rings =
        List.of(
            twoStone("Ring12", "+(12-16)% to Fire and Lightning Resistances"),
            twoStone("Ring13", "+(12-16)% to Cold and Lightning Resistances"),
            twoStone("Ring14", "+(12-16)% to Fire and Cold Resistances"));
    assertThat(PoeBaseItemDataService.findByName(rings, "Two-Stone Ring (Cold/Lightning)"))
        .get()
        .extracting(PoeBaseItem::slug)
        .isEqualTo("Ring13");
    assertThat(PoeBaseItemDataService.findByName(rings, "Two-Stone Ring (Fire/Cold)"))
        .get()
        .extracting(PoeBaseItem::slug)
        .isEqualTo("Ring14");
    assertThat(PoeBaseItemDataService.findByName(rings, "Two-Stone Ring (Chaos/Cold)")).isEmpty();
  }

  @Test
  void 발음_구별_기호를_빼고_쓴_이름도_찾는다() {
    assertThat(PoeBaseItemDataService.findByName(ITEMS, "Maelstrom Staff"))
        .get()
        .extracting(PoeBaseItem::slug)
        .isEqualTo("Staff17");
  }

  @Test
  void 정확_일치는_그대로_없는_이름은_빈값() {
    assertThat(PoeBaseItemDataService.findByName(ITEMS, "two-toned boots")).isPresent();
    assertThat(PoeBaseItemDataService.findByName(ITEMS, "Two-Toned Boots (Ward)")).isEmpty();
    assertThat(PoeBaseItemDataService.findByName(ITEMS, "Fishing Rod")).isEmpty();
    assertThat(PoeBaseItemDataService.findByName(ITEMS, null)).isEmpty();
  }
}
