package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import net.luversof.api.poe.poe2.Poe2CraftEngine.Action;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Affix;
import net.luversof.api.poe.poe2.Poe2CraftEngine.DesecratedMod;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Item;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Mod;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Rarity;

/** 심연의 에센스 · 심연의 군주의 징표(10-17 C416) — 레어에서 하나 지우고 그 쪽에 징표, 훼손하면 징표 자리에 미공개(무작위로 지우지 않음). */
class Poe2AbyssMarkTest {

  private static final Map<String, Mod> MODS = new HashMap<>();
  private static final List<Mod> LIST = new ArrayList<>();

  static {
    for (int g = 1; g <= 3; g++) {
      put(new Mod("P" + g, "prefix", 40, List.of(g), List.of()));
      put(new Mod("S" + g, "suffix", 40, List.of(10 + g), List.of()));
    }
  }

  private static void put(Mod m) {
    MODS.put(m.id(), m);
    LIST.add(m);
  }

  private static final Poe2CraftEngine E =
      new Poe2CraftEngine(
          new Poe2CraftEngine.Data() {
            @Override
            public List<Mod> pool(String poolKey) {
              return LIST;
            }

            @Override
            public Mod mod(String id) {
              return MODS.get(id);
            }

            @Override
            public Poe2CraftEngine.Essence essence(String slug) {
              return new Poe2CraftEngine.Essence(
                  slug, "corrupted", Map.of("Gloves", Poe2CraftEngine.ABYSS_MARK));
            }

            @Override
            public List<DesecratedMod> desecrated(String poolKey) {
              return List.of(
                  new DesecratedMod(new Mod("DP", "prefix", 65, List.of(21), List.of()), null),
                  new DesecratedMod(new Mod("DS", "suffix", 65, List.of(22), List.of()), null));
            }

            @Override
            public Poe2CraftEngine.Bone bone(String slug) {
              return new Poe2CraftEngine.Bone(slug, List.of("Gloves"), 0, 0);
            }
          });

  private static Affix a(String id) {
    Mod m = MODS.get(id);
    return new Affix(m.id(), m.gen(), m.level(), m.groups(), false);
  }

  private static final Item FULL =
      new Item(
          "Gloves|Armour",
          82,
          Rarity.RARE,
          List.of(a("P1"), a("P2"), a("P3"), a("S1"), a("S2"), a("S3")),
          false);

  @Test
  void 심연의_에센스는_하나_지우고_그_쪽에_징표() {
    SplittableRandom rng = new SplittableRandom(3);
    for (int i = 0; i < 50; i++) {
      Poe2CraftEngine.Result r = E.apply(FULL, Action.ESSENCE, "Abyss", Set.of(), rng);
      assertThat(r.error()).isNull();
      assertThat(r.item().affixes()).hasSize(6);
      Affix mark = r.item().affixes().stream().filter(Poe2CraftEngine::abyssMark).findFirst().get();
      String goneGen = MODS.get(r.removed().get(0)).gen();
      assertThat(mark.gen()).isEqualTo(goneGen);
    }
    // 우측 결정화 = 접미를 지우고 접미 징표(징조 씀)
    Poe2CraftEngine.Result d =
        E.apply(FULL, Action.ESSENCE, "Abyss", Set.of("DextralCrystallisation"), rng);
    assertThat(d.item().affixes())
        .filteredOn(Poe2CraftEngine::abyssMark)
        .singleElement()
        .matches(x -> x.gen().equals("suffix"));
    assertThat(d.omensUsed()).containsExactly("DextralCrystallisation");
    // 매직 · 징표 둘은 안 됨
    assertThat(
            E.apply(
                    new Item("Gloves|Armour", 82, Rarity.MAGIC, List.of(a("P1")), false),
                    Action.ESSENCE,
                    "Abyss",
                    Set.of(),
                    rng)
                .error())
        .contains("레어");
    assertThat(E.apply(d.item(), Action.ESSENCE, "Abyss", Set.of(), rng).error()).contains("같은 그룹");
  }

  @Test
  void 징표가_있으면_훼손이_징표_자리를_쓴다() {
    SplittableRandom rng = new SplittableRandom(5);
    Item marked =
        E.apply(FULL, Action.ESSENCE, "Abyss", Set.of("SinistralCrystallisation"), rng).item();
    for (int i = 0; i < 30; i++) {
      Poe2CraftEngine.Result r = E.apply(marked, Action.DESECRATE, "Rib", Set.of(), rng);
      assertThat(r.error()).isNull();
      // 꽉 찬 아이템인데 다른 옵션은 그대로 — 징표만 미공개 접두로
      assertThat(r.removed()).singleElement().isEqualTo(Poe2CraftEngine.ABYSS_MARK + "P");
      assertThat(r.item().affixes()).hasSize(6).noneMatch(Poe2CraftEngine::abyssMark);
      assertThat(r.item().affixes())
          .filteredOn(Poe2CraftEngine::unrevealed)
          .singleElement()
          .matches(x -> x.gen().equals("prefix"));
      assertThat(r.note()).isEqualTo("abyssmark");
    }
    // 자가검사: 징표 없는 꽉 찬 아이템은 무작위 하나를 지운다
    Poe2CraftEngine.Result plain = E.apply(FULL, Action.DESECRATE, "Rib", Set.of(), rng);
    assertThat(plain.removed())
        .singleElement()
        .matches(id -> !id.startsWith(Poe2CraftEngine.ABYSS_MARK));
  }

  @Test
  void 훼손_옵션이_접미뿐인_부위면_징표는_늘_접미() {
    Poe2CraftEngine suffixOnly =
        new Poe2CraftEngine(
            new Poe2CraftEngine.Data() {
              @Override
              public List<Mod> pool(String poolKey) {
                return LIST;
              }

              @Override
              public Mod mod(String id) {
                return MODS.get(id);
              }

              @Override
              public Poe2CraftEngine.Essence essence(String slug) {
                return new Poe2CraftEngine.Essence(
                    slug, "corrupted", Map.of("Gloves", Poe2CraftEngine.ABYSS_MARK));
              }

              @Override
              public List<DesecratedMod> desecrated(String poolKey) {
                return List.of(
                    new DesecratedMod(new Mod("DS", "suffix", 65, List.of(22), List.of()), null));
              }

              @Override
              public Poe2CraftEngine.Bone bone(String slug) {
                return new Poe2CraftEngine.Bone(slug, List.of("Gloves"), 0, 0);
              }
            });
    SplittableRandom rng = new SplittableRandom(7);
    for (int i = 0; i < 60; i++) {
      Poe2CraftEngine.Result r = suffixOnly.apply(FULL, Action.ESSENCE, "Abyss", Set.of(), rng);
      assertThat(r.error()).isNull();
      // 접미가 꽉 찼으니 접미를 지우고 접미 징표
      assertThat(MODS.get(r.removed().get(0)).gen()).isEqualTo("suffix");
      assertThat(r.item().affixes())
          .filteredOn(Poe2CraftEngine::abyssMark)
          .singleElement()
          .matches(x -> x.gen().equals("suffix"));
      assertThat(suffixOnly.apply(r.item(), Action.DESECRATE, "Rib", Set.of(), rng).error())
          .isNull();
    }
    // 접두 결정화(접두를 지우라는 징조)인데 접미가 꽉 찼다 → 자리를 낼 수 없다
    assertThat(
            suffixOnly
                .apply(FULL, Action.ESSENCE, "Abyss", Set.of("SinistralCrystallisation"), rng)
                .error())
        .contains("자리를 낼 수 없습니다");
  }

  @Test
  void 분열은_훼손_옵션을_4개_셈엔_넣고_고정하지는_않는다() {
    // C434 — timesaver.gg 분열 가이드(0.5.4) "cannot be fractured — but they still count toward the
    // 4-modifier minimum"
    SplittableRandom rng = new SplittableRandom(11);
    Affix unrev = new Affix(Poe2CraftEngine.UNREVEALED + "S", "suffix", 65, List.of(), false);
    Affix shown = new Affix("DS", "suffix", 65, List.of(22), false);
    for (Affix d : List.of(unrev, shown)) {
      Item it =
          new Item("Gloves|Armour", 82, Rarity.RARE, List.of(a("P1"), a("P2"), a("S1"), d), false);
      for (int i = 0; i < 100; i++) {
        Poe2CraftEngine.Result r = E.apply(it, Action.FRACTURE, null, Set.of(), rng);
        assertThat(r.error()).isNull();
        assertThat(r.added()).singleElement().isIn("P1", "P2", "S1");
      }
    }
    // 징표도 셈에 든다(가정 — C436) · 분열은 실옵션에서만
    Affix mark = new Affix(Poe2CraftEngine.ABYSS_MARK, "suffix", 1, List.of(), false);
    Item withMark =
        new Item("Gloves|Armour", 82, Rarity.RARE, List.of(a("P1"), a("P2"), a("S1"), mark), false);
    for (int i = 0; i < 50; i++) {
      assertThat(E.apply(withMark, Action.FRACTURE, null, Set.of(), rng).added())
          .singleElement()
          .isIn("P1", "P2", "S1");
    }
    // 실옵션 셋뿐이면 여전히 거절
    Item three =
        new Item("Gloves|Armour", 82, Rarity.RARE, List.of(a("P1"), a("P2"), a("S1")), false);
    assertThat(E.apply(three, Action.FRACTURE, null, Set.of(), rng).error()).contains("4개");
  }

  @Test
  void 재조합기는_주얼을_받지_않는다() {
    // C434 — poe2db Recombinator "weapons, armour, or jewellery"(주얼 없음)
    Item j1 = new Item("Jewel|Ruby", 82, Rarity.MAGIC, List.of(a("P1")), false);
    Item j2 = new Item("Jewel|Ruby", 82, Rarity.MAGIC, List.of(a("S1")), false);
    assertThat(
            E.recombine(j1, j2, Set.of("P1"), Set.of("S1"), 100, false, new SplittableRandom(1))
                .error())
        .isEqualTo("아이템을 재조합기에서 사용할 수 없습니다");
  }

  @Test
  void 깎기의_징조는_미공개_훼손_자리를_레벨_1로_친다() {
    // C438 — poe2wiki 깎기의 징조 "remove an unrevealed desecrated modifier; it will always count as an
    // ilvl 1 modifier"
    SplittableRandom rng = new SplittableRandom(13);
    Affix unrev = new Affix(Poe2CraftEngine.UNREVEALED + "S", "suffix", 65, List.of(-3), false);
    Item it =
        new Item(
            "Gloves|Armour", 82, Rarity.RARE, List.of(a("P1"), a("P2"), a("S1"), unrev), false);
    for (int i = 0; i < 30; i++) {
      Poe2CraftEngine.Result r = E.apply(it, Action.CHAOS, null, Set.of("Whittling"), rng);
      assertThat(r.error()).isNull();
      assertThat(r.removed()).containsExactly(unrev.id());
    }
  }
}
