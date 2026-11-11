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
import net.luversof.api.poe.poe2.Poe2CraftEngine.Item;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Mod;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Rarity;

/** PoE2 제작 규칙(10-15 C351) — 화폐 등급 최소 레벨 · 연금 4옵션 · 카오스 하나 바꿈 · 징조(좌/우 · 고위 · 깎기 · 동질화). */
class Poe2CraftEngineTest {

  private static final String POOL = "Gloves|Armour";
  private static final Map<String, Mod> MODS = new HashMap<>();
  private static final List<Mod> LIST = new ArrayList<>();

  static {
    // 접두 6계열(그룹 1~6) · 접미 6계열(그룹 11~16), 계열마다 레벨 10 · 40 · 75 세 등급
    for (int g = 1; g <= 6; g++) {
      for (int lv : new int[] {10, 40, 75}) {
        put(
            new Mod(
                "P" + g + "_" + lv, "prefix", lv, List.of(g), List.of(g <= 2 ? "life" : "attack")));
        put(
            new Mod(
                "S" + g + "_" + lv,
                "suffix",
                lv,
                List.of(10 + g),
                List.of(g <= 2 ? "life" : "resistance")));
      }
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
              return POOL.equals(poolKey) ? LIST : List.of();
            }

            @Override
            public Mod mod(String id) {
              return MODS.get(id);
            }
          });

  private static Affix a(String id) {
    Mod m = MODS.get(id);
    return new Affix(m.id(), m.gen(), m.level(), m.groups(), false);
  }

  private static Item item(Rarity r, Affix... as) {
    return new Item(POOL, 82, r, List.of(as), false);
  }

  @Test
  void 화폐_등급은_최소_옵션_레벨() {
    SplittableRandom rng = new SplittableRandom(1);
    for (int i = 0; i < 300; i++) {
      Item t = E.apply(item(Rarity.NORMAL), Action.TRANSMUTE, "perfect", Set.of(), rng).item();
      assertThat(t.rarity()).isEqualTo(Rarity.MAGIC);
      assertThat(t.affixes()).singleElement().matches(x -> x.level() >= 70);
      Item ex =
          E.apply(item(Rarity.RARE, a("P1_10")), Action.EXALT, "greater", Set.of(), rng).item();
      assertThat(ex.affixes().get(1).level()).isGreaterThanOrEqualTo(35);
    }
  }

  @Test
  void 연금은_4옵션_좌_연금이면_접두_최대() {
    SplittableRandom rng = new SplittableRandom(2);
    for (int i = 0; i < 300; i++) {
      Item r = E.apply(item(Rarity.NORMAL), Action.ALCHEMY, null, Set.of(), rng).item();
      assertThat(r.affixes()).hasSize(4);
      Poe2CraftEngine.Result s =
          E.apply(item(Rarity.NORMAL), Action.ALCHEMY, null, Set.of("SinistralAlchemy"), rng);
      assertThat(s.item().count("prefix")).isEqualTo(3);
      assertThat(s.omensUsed()).containsExactly("SinistralAlchemy");
    }
  }

  @Test
  void 카오스는_하나_바꾸고_깎기는_가장_낮은_레벨_소거는_그쪽만() {
    SplittableRandom rng = new SplittableRandom(3);
    Item it = item(Rarity.RARE, a("P1_75"), a("P2_10"), a("S1_40"), a("S2_75"));
    for (int i = 0; i < 300; i++) {
      Poe2CraftEngine.Result r = E.apply(it, Action.CHAOS, null, Set.of(), rng);
      assertThat(r.item().affixes()).hasSize(4);
      assertThat(r.removed()).hasSize(1);
      assertThat(E.apply(it, Action.CHAOS, null, Set.of("Whittling"), rng).removed())
          .containsExactly("P2_10");
      assertThat(E.apply(it, Action.CHAOS, null, Set.of("DextralErasure"), rng).removed())
          .allMatch(id -> id.startsWith("S"));
    }
  }

  @Test
  void 엑잘_징조_고위는_둘_우는_접미만_동질화는_같은_태그() {
    SplittableRandom rng = new SplittableRandom(4);
    Item it = item(Rarity.RARE, a("P1_75"), a("S3_40"));
    for (int i = 0; i < 300; i++) {
      assertThat(E.apply(it, Action.EXALT, null, Set.of("GreaterExaltation"), rng).added())
          .hasSize(2);
      assertThat(E.apply(it, Action.EXALT, null, Set.of("DextralExaltation"), rng).added())
          .allMatch(id -> id.startsWith("S"));
      // 지금 태그 = life(P1) · resistance(S3) → attack 태그 접두(P3~P6)는 안 나온다
      assertThat(E.apply(it, Action.EXALT, null, Set.of("HomogenisingExaltation"), rng).added())
          .allMatch(
              id ->
                  MODS.get(id).tags().contains("life")
                      || MODS.get(id).tags().contains("resistance"));
    }
  }

  @Test
  void 소멸_고위는_둘_분열은_안_지움_분열은_4옵션부터() {
    SplittableRandom rng = new SplittableRandom(5);
    Affix fr = new Affix("P1_75", "prefix", 75, List.of(1), true);
    Item it = new Item(POOL, 82, Rarity.RARE, List.of(fr, a("P2_10"), a("S1_40")), false);
    for (int i = 0; i < 200; i++) {
      Poe2CraftEngine.Result r = E.apply(it, Action.ANNUL, null, Set.of("GreaterAnnulment"), rng);
      assertThat(r.removed()).hasSize(2).doesNotContain("P1_75");
    }
    assertThat(
            E.apply(item(Rarity.RARE, a("P1_75"), a("S1_40")), Action.FRACTURE, null, Set.of(), rng)
                .error())
        .contains("4개");
    assertThat(E.apply(item(Rarity.MAGIC, a("P1_75")), Action.EXALT, null, Set.of(), rng).error())
        .contains("레어");
  }

  @Test
  void 에센스_보통은_매직을_레어로_완벽은_하나_지우고_보장() {
    Map<String, Mod> mods = new HashMap<>(MODS);
    mods.put("X1", new Mod("X1", "suffix", 1, List.of(99), List.of()));
    Poe2CraftEngine e =
        new Poe2CraftEngine(
            new Poe2CraftEngine.Data() {
              @Override
              public List<Mod> pool(String poolKey) {
                return POOL.equals(poolKey) ? LIST : List.of();
              }

              @Override
              public Mod mod(String id) {
                return mods.get(id);
              }

              @Override
              public Poe2CraftEngine.Essence essence(String slug) {
                return switch (slug) {
                  case "Life" ->
                      new Poe2CraftEngine.Essence("Life", "normal", Map.of("Gloves", "P1_40"));
                  case "PerfX" ->
                      new Poe2CraftEngine.Essence("PerfX", "perfect", Map.of("Gloves", "X1"));
                  case "AlloyX" ->
                      new Poe2CraftEngine.Essence("AlloyX", "alloy", Map.of("Gloves", "X1"));
                  default -> null;
                };
              }
            });
    SplittableRandom rng = new SplittableRandom(6);
    Poe2CraftEngine.Result r =
        e.apply(item(Rarity.MAGIC, a("S2_10")), Action.ESSENCE, "Life", Set.of(), rng);
    assertThat(r.error()).isNull();
    assertThat(r.item().rarity()).isEqualTo(Rarity.RARE);
    assertThat(r.item().affixes())
        .extracting(Affix::id)
        .containsExactlyInAnyOrder("S2_10", "P1_40");
    assertThat(
            e.apply(item(Rarity.RARE, a("S2_10")), Action.ESSENCE, "Life", Set.of(), rng).error())
        .contains("매직");
    // 완벽 — 레어에서 하나 지우고 X1, 우측 결정화면 접미만 지움
    Item rare = item(Rarity.RARE, a("P1_75"), a("P2_40"), a("S1_10"), a("S3_40"));
    for (int i = 0; i < 200; i++) {
      Poe2CraftEngine.Result p =
          e.apply(rare, Action.ESSENCE, "PerfX", Set.of("DextralCrystallisation"), rng);
      assertThat(p.error()).isNull();
      assertThat(p.removed()).singleElement().matches(id -> id.startsWith("S"));
      assertThat(p.item().affixes()).extracting(Affix::id).contains("X1").hasSize(4);
      assertThat(p.omensUsed()).containsExactly("DextralCrystallisation");
    }
    assertThat(
            e.apply(item(Rarity.MAGIC, a("S2_10")), Action.ESSENCE, "Nope", Set.of(), rng).error())
        .contains("쓸 수 없는");
    // 합금 — 완벽처럼 하나 지우고 보장, 결정화 징조는 안 걸림(쓰이지도 않음)
    Poe2CraftEngine.Result al =
        e.apply(rare, Action.ESSENCE, "AlloyX", Set.of("DextralCrystallisation"), rng);
    assertThat(al.error()).isNull();
    assertThat(al.omensUsed()).isEmpty();
    assertThat(al.item().affixes()).extracting(Affix::id).contains("X1").hasSize(4);
  }

  @Test
  void 훼손은_미공개_하나_다시_못함_드러내기는_셋_중_하나_빛은_훼손만_지움() {
    List<Poe2CraftEngine.DesecratedMod> dpool = new ArrayList<>();
    for (int k = 1; k <= 6; k++) {
      dpool.add(
          new Poe2CraftEngine.DesecratedMod(
              new Mod("D" + k, "suffix", k <= 3 ? 30 : 65, List.of(50 + k), List.of()),
              k % 2 == 0 ? "ulaman" : "kurgal"));
    }
    Poe2CraftEngine e =
        new Poe2CraftEngine(
            new Poe2CraftEngine.Data() {
              @Override
              public List<Mod> pool(String poolKey) {
                return POOL.equals(poolKey) ? LIST : List.of();
              }

              @Override
              public Mod mod(String id) {
                return MODS.get(id);
              }

              @Override
              public Poe2CraftEngine.Bone bone(String slug) {
                return switch (slug) {
                  case "Rib" -> new Poe2CraftEngine.Bone("Rib", List.of("Gloves"), 0, 0);
                  case "AncientRib" ->
                      new Poe2CraftEngine.Bone("AncientRib", List.of("Gloves"), 0, 40);
                  // 군주 징조는 무기 · 장신구 뼈만(C414)
                  case "AncientJewelleryRib" ->
                      new Poe2CraftEngine.Bone("AncientJewelleryRib", List.of("Gloves"), 0, 40);
                  case "GnawedRib" ->
                      new Poe2CraftEngine.Bone("GnawedRib", List.of("Gloves"), 64, 0);
                  default -> null;
                };
              }

              @Override
              public List<Poe2CraftEngine.DesecratedMod> desecrated(String poolKey) {
                return dpool;
              }
            });
    SplittableRandom rng = new SplittableRandom(8);
    Item it = item(Rarity.RARE, a("P1_75"), a("S1_40"));
    Poe2CraftEngine.Result d = e.apply(it, Action.DESECRATE, "Rib", Set.of(), rng);
    assertThat(d.error()).isNull();
    assertThat(d.item().affixes())
        .filteredOn(Poe2CraftEngine::unrevealed)
        .singleElement()
        .matches(x -> x.gen().equals("suffix"));
    assertThat(e.apply(d.item(), Action.DESECRATE, "Rib", Set.of(), rng).error())
        .contains("훼손된 속성");
    assertThat(e.apply(it, Action.DESECRATE, "GnawedRib", Set.of(), rng).error()).contains("64");
    // 드러낼 셋 — 다시 봐도 같고, 고르면 바뀜
    List<String> o = e.revealOffers(d.item());
    assertThat(o).hasSize(3).isEqualTo(e.revealOffers(d.item()));
    // 공개 씨앗(C483) — 에뮬레이터가 되돌리면 새 씨앗을 줘 다른 셋이 나온다. 드러내기는 그 씨앗의 셋으로 검사
    java.util.Set<List<String>> seen = new java.util.HashSet<>();
    for (long s = 1; s <= 20; s++) {
      Item dx = d.item();
      dx =
          new Item(
              dx.poolKey(),
              dx.ilvl(),
              dx.rarity(),
              dx.affixes(),
              dx.corrupted(),
              dx.quality(),
              dx.sanctified(),
              s);
      List<String> os = e.revealOffers(dx);
      assertThat(os).isEqualTo(e.revealOffers(dx));
      assertThat(e.apply(dx, Action.REVEAL, os.get(os.size() - 1), Set.of(), rng).error()).isNull();
      seen.add(os);
    }
    assertThat(seen).hasSizeGreaterThan(1);
    Poe2CraftEngine.Result r = e.apply(d.item(), Action.REVEAL, o.get(0), Set.of(), rng);
    assertThat(r.item().affixes())
        .noneMatch(Poe2CraftEngine::unrevealed)
        .anyMatch(x -> x.id().equals(o.get(0)));
    // 고대(최소 레벨 40) = 65 레벨만, 주권 징조 = 울라만만
    assertThat(e.apply(it, Action.DESECRATE, "AncientRib", Set.of("Sovereign"), rng).error())
        .isEqualTo("이 훼손 아이템은 징조와 호환되지 않습니다");
    Item anc =
        e.apply(it, Action.DESECRATE, "AncientJewelleryRib", Set.of("Sovereign"), rng).item();
    assertThat(e.revealOffers(anc)).isNotEmpty().allMatch(id -> List.of("D4", "D6").contains(id));
    // 메아리 — 셋을 한 번 다시(다른 셋), 두 번째는 거절 · 징조 없으면 거절
    assertThat(e.apply(d.item(), Action.REVEAL_REROLL, null, Set.of(), rng).error())
        .contains("메아리");
    Item re = e.apply(d.item(), Action.REVEAL_REROLL, null, Set.of("AbyssalEchoes"), rng).item();
    assertThat(e.revealOffers(re)).hasSize(3);
    assertThat(e.apply(re, Action.REVEAL_REROLL, null, Set.of("AbyssalEchoes"), rng).error())
        .contains("한 번");
    // 부패 — 접미 3개 미공개 + 타락, 타락이어도 드러내기는 된다
    Item rot = e.apply(it, Action.DESECRATE, "Rib", Set.of("Putrefaction"), rng).item();
    assertThat(rot.corrupted()).isTrue();
    assertThat(rot.affixes()).hasSize(3).allMatch(Poe2CraftEngine::unrevealed);
    String first = e.revealOffers(rot).get(0);
    assertThat(e.apply(rot, Action.REVEAL, first, Set.of(), rng).item().affixes())
        .anyMatch(x -> x.id().equals(first));
    assertThat(e.apply(rot, Action.CHAOS, null, Set.of(), rng).error()).contains("타락");
    // 빛의 징조 — 소멸이 훼손 옵션만
    Item withD = r.item();
    for (int i = 0; i < 50; i++) {
      assertThat(e.apply(withD, Action.ANNUL, null, Set.of("Light"), rng).removed())
          .containsExactly(o.get(0));
    }
  }

  @Test
  void 직접_편집은_풀_그룹_자리를_지키고_희귀도를_올린다() {
    SplittableRandom rng = new SplittableRandom(9);
    Item n = item(Rarity.NORMAL);
    Item a1 = E.apply(n, Action.MANUAL_ADD, "P1_75:F", Set.of(), rng).item();
    assertThat(a1.rarity()).isEqualTo(Rarity.MAGIC);
    assertThat(a1.affixes()).singleElement().matches(Affix::fractured);
    Item a2 =
        E.apply(
                E.apply(a1, Action.MANUAL_ADD, "S1_40", Set.of(), rng).item(),
                Action.MANUAL_ADD,
                "P2_40",
                Set.of(),
                rng)
            .item();
    assertThat(a2.rarity()).isEqualTo(Rarity.RARE);
    assertThat(E.apply(a2, Action.MANUAL_ADD, "P1_10", Set.of(), rng).error()).contains("그룹");
    assertThat(E.apply(a2, Action.MANUAL_ADD, "Nope", Set.of(), rng).error()).contains("풀");
    Item low = new Item(POOL, 20, Rarity.NORMAL, List.of(), false);
    assertThat(E.apply(low, Action.MANUAL_ADD, "P1_75", Set.of(), rng).error()).contains("아이템 레벨");
    assertThat(E.apply(a1, Action.MANUAL_REMOVE, "P1_75", Set.of(), rng).item().rarity())
        .isEqualTo(Rarity.NORMAL);
  }

  @Test
  void 검토_수정_카오스_빈추가_실패_좌우동시_징조_안씀() {
    SplittableRandom rng = new SplittableRandom(10);
    // 아이템 레벨 30 · 완벽 카오스(50+) — 아이템 레벨이 최소 속성 레벨보다 낮으면 못 쓴다(C414 — 인게임 오류, 아이템 그대로)
    Item low =
        new Item(
            POOL, 30, Rarity.RARE, List.of(a("P1_10"), a("P2_10"), a("S1_10"), a("S2_10")), false);
    Poe2CraftEngine.Result c = E.apply(low, Action.CHAOS, "perfect", Set.of(), rng);
    assertThat(c.error()).isEqualTo("아이템 레벨이 너무 낮습니다");
    assertThat(c.item().affixes()).hasSize(4);
    // 좌 · 우 찬미를 둘 다 걸면 인게임 오류로 막힌다(C437 — 전엔 둘 다 무시하고 엑잘을 썼다), 아이템 · 징조 그대로
    Item one = item(Rarity.RARE, a("P1_75"));
    Poe2CraftEngine.Result ex =
        E.apply(one, Action.EXALT, null, Set.of("SinistralExaltation", "DextralExaltation"), rng);
    assertThat(ex.error()).isEqualTo("유효하지 않은 징조 조합입니다.");
    assertThat(ex.item()).isEqualTo(one);
    assertThat(ex.omensUsed()).isEmpty();
    // 다른 화폐의 짝은 상관없다(카오스에 좌 · 우 찬미)
    assertThat(
            E.apply(
                    item(Rarity.RARE, a("P1_75"), a("S1_10")),
                    Action.CHAOS,
                    null,
                    Set.of("SinistralExaltation", "DextralExaltation"),
                    rng)
                .error())
        .isNull();
    assertThat(
            E.apply(
                    item(Rarity.RARE, a("P1_75"), a("S1_10")),
                    Action.CHAOS,
                    null,
                    Set.of("SinistralErasure", "DextralErasure"),
                    rng)
                .error())
        .isEqualTo("유효하지 않은 징조 조합입니다.");
  }

  @Test
  void 바알_오브는_넷_중_하나_타락의_징조는_변화_없음을_뺀다() {
    Mod ench = new Mod("CorrEnch1", "corrupted", 1, List.of(900), List.of());
    Poe2CraftEngine ev =
        new Poe2CraftEngine(
            new Poe2CraftEngine.Data() {
              @Override
              public List<Mod> pool(String poolKey) {
                return POOL.equals(poolKey) ? LIST : List.of();
              }

              @Override
              public Mod mod(String id) {
                return "CorrEnch1".equals(id) ? ench : MODS.get(id);
              }

              @Override
              public List<Mod> corruptedPool(String poolKey) {
                return List.of(ench);
              }
            });
    SplittableRandom rng = new SplittableRandom(4);
    Item it = item(Rarity.RARE, a("P1_75"), a("P2_75"), a("S1_75"), a("S2_75"));
    Map<String, Integer> seen = new HashMap<>();
    for (int k = 0; k < 2000; k++) {
      Poe2CraftEngine.Result r = ev.apply(it, Action.VAAL, null, Set.of(), rng);
      assertThat(r.error()).isNull();
      assertThat(r.item().corrupted()).isTrue();
      String kind = r.note().replaceAll(":\\d+$", "");
      seen.merge(kind, 1, Integer::sum);
      if (kind.equals("vaal:enchant")) {
        assertThat(r.item().affixes()).extracting(Affix::id).contains("CorrEnch1");
        assertThat(r.item().count("prefix") + r.item().count("suffix")).isEqualTo(4);
      } else if (kind.equals("vaal:randomize")) {
        assertThat(r.item().affixes()).hasSize(4);
        assertThat(r.item().count("prefix")).isEqualTo(2);
      } else {
        assertThat(r.item().affixes()).isEqualTo(it.affixes());
      }
      assertThat(ev.apply(r.item(), Action.VAAL, null, Set.of(), rng).error()).contains("타락");
    }
    // 장갑 = 홈 결과가 있는 부위 → 넷이 대략 균등(각 500 ± 100)
    assertThat(seen.keySet())
        .containsExactlyInAnyOrder("vaal:none", "vaal:randomize", "vaal:enchant", "vaal:socket");
    assertThat(seen.values()).allMatch(n -> n > 400 && n < 600);
    // 타락의 징조 — 변화 없음이 빠지고 징조를 쓴다
    for (int k = 0; k < 300; k++) {
      Poe2CraftEngine.Result r = ev.apply(it, Action.VAAL, null, Set.of("Corruption"), rng);
      assertThat(r.note()).isNotEqualTo("vaal:none");
      assertThat(r.omensUsed()).containsExactly("Corruption");
    }
  }

  @Test
  void 기폭제는_반지_목걸이에_제련된_것은_주얼에() {
    SplittableRandom rng = new SplittableRandom(2);
    Item ring =
        new Item("Ring|기본", 82, Rarity.RARE, List.of(a("P1_75"), a("P3_75"), a("S1_75")), false);
    Item jewel = new Item("Jewel|Ruby", 82, Rarity.RARE, List.of(a("P1_75")), false);
    // 대상 아닌 것
    assertThat(E.apply(item(Rarity.RARE), Action.CATALYST, "life", Set.of(), rng).error())
        .contains("반지나 목걸이");
    assertThat(E.apply(ring, Action.CATALYST, "jewel:life", Set.of(), rng).error())
        .contains("주얼에만");
    assertThat(E.apply(jewel, Action.CATALYST, "jewel:life", Set.of(), rng).item().quality())
        .isEqualTo("jewel:life:1");
    // 쌓임 · 최대 · 다른 종류는 처음부터
    Poe2CraftEngine.Result r = E.apply(ring, Action.CATALYST, "life*30", Set.of(), rng);
    assertThat(r.item().quality()).isEqualTo("life:20");
    assertThat(r.note()).isEqualTo("catalyst:life:20:20");
    assertThat(E.apply(r.item(), Action.CATALYST, "life", Set.of(), rng).error()).contains("최대");
    assertThat(E.apply(r.item(), Action.CATALYST, "attack", Set.of(), rng).item().quality())
        .isEqualTo("attack:1");
    // 오르는 옵션 = 태그 life(P1 · S1), attack 은 P3
    assertThat(E.boosted(r.item())).containsExactlyInAnyOrder("P1_75", "S1_75");
    assertThat(E.boosted(E.apply(r.item(), Action.CATALYST, "attack", Set.of(), rng).item()))
        .containsExactly("P3_75");
    // 카오스로 다시 굴려도 퀄리티는 남고, 타락하면 못 쓴다
    assertThat(E.apply(r.item(), Action.CHAOS, "", Set.of(), rng).item().quality())
        .isEqualTo("life:20");
    Item cor = new Item("Ring|기본", 82, Rarity.RARE, ring.affixes(), true, "life:5");
    assertThat(E.apply(cor, Action.CATALYST, "life", Set.of(), rng).error()).contains("타락");
  }

  @Test
  void 촉진하는_찬미는_퀄리티를_소모해_같은_종류를_더_자주() {
    Poe2CraftEngine e =
        new Poe2CraftEngine(
            new Poe2CraftEngine.Data() {
              @Override
              public List<Mod> pool(String poolKey) {
                return "Ring|기본".equals(poolKey) ? LIST : List.of();
              }

              @Override
              public Mod mod(String id) {
                return MODS.get(id);
              }
            });
    SplittableRandom rng = new SplittableRandom(7);
    Item ring = new Item("Ring|기본", 82, Rarity.RARE, List.of(), false, "life:20");
    // 퀄리티가 없으면 징조는 쓰이지 않는다
    Poe2CraftEngine.Result plain =
        e.apply(ring.withQuality(null), Action.EXALT, "", Set.of("CatalysingExaltation"), rng);
    assertThat(plain.omensUsed()).doesNotContain("CatalysingExaltation");
    int life = 0;
    int n = 4000;
    for (int i = 0; i < n; i++) {
      Poe2CraftEngine.Result r =
          e.apply(ring, Action.EXALT, "", Set.of("CatalysingExaltation"), rng);
      assertThat(r.item().quality()).isNull();
      assertThat(r.omensUsed()).contains("CatalysingExaltation");
      assertThat(r.note()).isEqualTo("catalysing:life:20");
      if (MODS.get(r.added().get(0)).tags().contains("life")) {
        life++;
      }
    }
    // 균등이면 12/36 = 0.33, ×5 면 60/84 = 0.71
    assertThat(life / (double) n).isBetween(0.66, 0.77);
    assertThat(Poe2CraftEngine.catalysingMult(20)).isEqualTo(5.0);
    assertThat(Poe2CraftEngine.catalysingMult(40)).isEqualTo(7.5);
  }

  @Test
  void 축성은_레어만_이후_화폐는_막히고_타락에도_못_쓴다() {
    SplittableRandom rng = new SplittableRandom(3);
    Item rare = item(Rarity.RARE, a("P1_75"), a("S1_75"));
    assertThat(
            E.apply(
                    item(Rarity.MAGIC, a("P1_75")),
                    Action.SANCTIFY,
                    null,
                    Set.of("Sanctification"),
                    rng)
                .error())
        .contains("레어");
    Poe2CraftEngine.Result r = E.apply(rare, Action.SANCTIFY, null, Set.of("Sanctification"), rng);
    assertThat(r.error()).isNull();
    assertThat(r.item().sanctified()).isTrue();
    assertThat(r.item().affixes()).isEqualTo(rare.affixes());
    assertThat(r.omensUsed()).containsExactly("Sanctification");
    for (Action x :
        List.of(Action.CHAOS, Action.EXALT, Action.ANNUL, Action.VAAL, Action.CATALYST)) {
      assertThat(E.apply(r.item(), x, "", Set.of(), rng).error()).isEqualTo("아이템이 축성됨");
    }
    Item cor = new Item(rare.poolKey(), 82, Rarity.RARE, rare.affixes(), true);
    assertThat(E.apply(cor, Action.SANCTIFY, null, Set.of("Sanctification"), rng).error())
        .contains("타락");
  }
}
