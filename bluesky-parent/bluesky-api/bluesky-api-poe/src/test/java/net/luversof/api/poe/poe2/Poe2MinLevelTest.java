package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import net.luversof.api.poe.poe2.Poe2CraftEngine.Action;
import net.luversof.api.poe.poe2.Poe2CraftEngine.DesecratedMod;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Item;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Mod;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Rarity;

/** PoE2 규칙 감사 반영(10-17 C414) — 최소 속성 레벨 예외 · 아이템 레벨 부족 · 군주 징조는 무기 · 장신구 뼈만. */
class Poe2MinLevelTest {

  private static final Map<String, Mod> MODS = new HashMap<>();
  private static final List<Mod> LIST = new ArrayList<>();

  static {
    // 접두 A(그룹 1): 10 · 40 · 75 / 접두 B(그룹 2): 20 · 30 — 모든 등급이 최소 레벨(70) 아래
    for (int lv : new int[] {10, 40, 75}) {
      put(new Mod("A" + lv, "prefix", lv, List.of(1), List.of()));
    }
    for (int lv : new int[] {20, 30}) {
      put(new Mod("B" + lv, "prefix", lv, List.of(2), List.of()));
    }
    put(new Mod("S75", "suffix", 75, List.of(11), List.of()));
  }

  private static void put(Mod m) {
    MODS.put(m.id(), m);
    LIST.add(m);
  }

  private static final Mod U1 = new Mod("Ulaman1", "suffix", 65, List.of(21), List.of());

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
            public List<DesecratedMod> desecrated(String poolKey) {
              return List.of(new DesecratedMod(U1, "ulaman"));
            }

            @Override
            public Poe2CraftEngine.Bone bone(String slug) {
              return new Poe2CraftEngine.Bone(slug, List.of("Gloves", "Ring"), 0, 0);
            }
          });

  @Test
  void 계열의_모든_등급이_최소_레벨_아래면_최고_등급은_남는다() {
    SplittableRandom rng = new SplittableRandom(1);
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < 400; i++) {
      Item t =
          E.apply(
                  new Item("Gloves|Armour", 82, Rarity.NORMAL, List.of(), false),
                  Action.TRANSMUTE,
                  "perfect",
                  Set.of(),
                  rng)
              .item();
      seen.add(t.affixes().get(0).id());
    }
    // A 는 75 만(최소 70 이상), B 는 모두 아래라 최고 등급 30 만, S75 — 그 밖(A10 · A40 · B20)은 안 나온다
    assertThat(seen).containsExactlyInAnyOrder("A75", "B30", "S75");
  }

  @Test
  void 아이템_레벨이_최소_속성_레벨보다_낮으면_못_쓴다() {
    Item low = new Item("Gloves|Armour", 60, Rarity.NORMAL, List.of(), false);
    assertThat(E.apply(low, Action.TRANSMUTE, "perfect", Set.of(), new SplittableRandom(1)).error())
        .isEqualTo("아이템 레벨이 너무 낮습니다");
    assertThat(E.apply(low, Action.TRANSMUTE, null, Set.of(), new SplittableRandom(1)).error())
        .isNull();
  }

  @Test
  void 군주_징조는_무기_장신구_뼈만() {
    Item rare =
        new Item(
            "Gloves|Armour",
            82,
            Rarity.RARE,
            List.of(new Poe2CraftEngine.Affix("A75", "prefix", 75, List.of(1), false)),
            false);
    assertThat(
            E.apply(
                    rare,
                    Action.DESECRATE,
                    "AbyssalBenchTicketArmour",
                    Set.of("Sovereign"),
                    new SplittableRandom(1))
                .error())
        .isEqualTo("이 훼손 아이템은 징조와 호환되지 않습니다");
    Item ring =
        new Item(
            "Ring|기본",
            82,
            Rarity.RARE,
            List.of(new Poe2CraftEngine.Affix("A75", "prefix", 75, List.of(1), false)),
            false);
    Poe2CraftEngine.Result ok =
        E.apply(
            ring,
            Action.DESECRATE,
            "AbyssalBenchTicketJewellery",
            Set.of("Sovereign"),
            new SplittableRandom(1));
    assertThat(ok.error()).isNull();
    assertThat(ok.omensUsed()).contains("Sovereign");
    // 아이템 레벨 60 — 울라만 옵션(65)이 나올 수 없다
    Item lowRing =
        new Item(
            "Ring|기본",
            60,
            Rarity.RARE,
            List.of(new Poe2CraftEngine.Affix("A40", "prefix", 40, List.of(1), false)),
            false);
    assertThat(
            E.apply(
                    lowRing,
                    Action.DESECRATE,
                    "AbyssalBenchTicketJewellery",
                    Set.of("Sovereign"),
                    new SplittableRandom(1))
                .error())
        .isEqualTo("이 훼손 아이템은 징조와 호환되지 않습니다");
  }

  @Test
  void 예외는_옵션_유형_단위_최고_레벨_동률은_모두() {
    // 같은 그룹(5) 안에 유형 둘: 생명(10 · 75) · 마나(20 · 63) — 완벽(70)이면 생명 75 와 마나 63(유형 전부가 아래) 둘 다
    // 유형 셋째(그룹 6): 30 · 30 동률 — 둘 다 남는다
    List<Mod> pool =
        List.of(
            new Mod("L10", "prefix", 10, List.of(5), List.of(), "life"),
            new Mod("L75", "prefix", 75, List.of(5), List.of(), "life"),
            new Mod("M20", "prefix", 20, List.of(5), List.of(), "mana"),
            new Mod("M63", "prefix", 63, List.of(5), List.of(), "mana"),
            new Mod("Ta", "suffix", 30, List.of(6), List.of(), "tie"),
            new Mod("Tb", "suffix", 30, List.of(6), List.of(), "tie"));
    Poe2CraftEngine e =
        new Poe2CraftEngine(
            new Poe2CraftEngine.Data() {
              @Override
              public List<Mod> pool(String poolKey) {
                return pool;
              }

              @Override
              public Mod mod(String id) {
                return pool.stream().filter(m -> m.id().equals(id)).findFirst().orElse(null);
              }
            });
    Set<String> ids = new HashSet<>();
    for (Mod m :
        e.eligible(
            new Item("Gloves|Armour", 82, Rarity.MAGIC, List.of(), false),
            Rarity.MAGIC,
            70,
            null,
            false)) {
      ids.add(m.id());
    }
    assertThat(ids).containsExactlyInAnyOrder("L75", "M63", "Ta", "Tb");
  }

  @Test
  void 차단_목록은_등급별로_아이템_레벨_부족을_알린다() {
    // C435 — 아이템 레벨 40: 제왕 상위(35)는 됨 · 완벽(50) 막힘, 진화는 기본부터 막혀 등급 항목 없음(기본 항목만)
    Item m =
        new Item(
            "Gloves|Armour",
            40,
            Rarity.MAGIC,
            List.of(new Poe2CraftEngine.Affix("A10", "prefix", 10, List.of(1), false)),
            false);
    Map<String, String> b = Poe2CraftService.blockedFor(E, m);
    assertThat(b).containsKeys("REGAL:perfect", "TRANSMUTE");
    assertThat(b)
        .doesNotContainKeys("REGAL", "REGAL:greater", "TRANSMUTE:greater", "TRANSMUTE:perfect");
    assertThat(b.get("REGAL:perfect")).isEqualTo("아이템 레벨이 너무 낮습니다");
    // 레어 · 아이템 레벨 40: 카오스 완벽(50)만 막힘
    Item r =
        new Item(
            "Gloves|Armour",
            40,
            Rarity.RARE,
            List.of(
                new Poe2CraftEngine.Affix("A10", "prefix", 10, List.of(1), false),
                new Poe2CraftEngine.Affix("B20", "prefix", 20, List.of(2), false)),
            false);
    Map<String, String> rb = Poe2CraftService.blockedFor(E, r);
    assertThat(rb).containsKey("CHAOS:perfect").doesNotContainKeys("CHAOS", "CHAOS:greater");
  }

  @Test
  void 다음_옵션_표는_고른_등급의_최소_레벨을_따른다() {
    // C441 — 일반 · 완벽 진화(70): A 계열은 A75 만, B 계열(20 · 30 모두 70 아래)은 최고 등급 B30 이 남는 예외, S75
    Item n = new Item("Gloves|Armour", 82, Rarity.NORMAL, List.of(), false);
    assertThat(E.nextPool(n, "perfect").stream().map(Mod::id))
        .containsExactlyInAnyOrder("A75", "B30", "S75");
    assertThat(E.nextPool(n, null)).hasSize(LIST.size());
    // 아이템 레벨 40 · 완벽(70) = 못 씀 → 빈 표, 상위(44) 도 못 씀, 레어 · 상위 엑잘(35) 은 됨
    Item low = new Item("Gloves|Armour", 40, Rarity.NORMAL, List.of(), false);
    assertThat(E.nextPool(low, "perfect")).isEmpty();
    assertThat(E.nextPool(low, "greater")).isEmpty();
    Item rare =
        new Item(
            "Gloves|Armour",
            40,
            Rarity.RARE,
            List.of(new Poe2CraftEngine.Affix("A10", "prefix", 10, List.of(1), false)),
            false);
    assertThat(E.nextPool(rare, "greater")).isNotEmpty();
  }

  @Test
  void 자리가_꽉_차면_인게임_자리_문구() {
    // C443 — 매직 접두 1 · 접미 1 에 확장 = 양쪽 다 참, 레어 접두 3 에 좌측 찬미 엑잘 = 접두 문구
    SplittableRandom rng = new SplittableRandom(3);
    Item magic =
        new Item(
            "Gloves|Armour",
            82,
            Rarity.MAGIC,
            List.of(
                new Poe2CraftEngine.Affix("A10", "prefix", 10, List.of(1), false),
                new Poe2CraftEngine.Affix("S75", "suffix", 75, List.of(11), false)),
            false);
    assertThat(E.apply(magic, Action.AUGMENTATION, null, Set.of(), rng).error())
        .startsWith("다른 접두어 속성 부여를 추가할 수 없습니다.")
        .contains("다른 접미어");
    Item rare3 =
        new Item(
            "Gloves|Armour",
            82,
            Rarity.RARE,
            List.of(
                new Poe2CraftEngine.Affix("A10", "prefix", 10, List.of(1), false),
                new Poe2CraftEngine.Affix("B20", "prefix", 20, List.of(2), false),
                new Poe2CraftEngine.Affix("X1", "prefix", 1, List.of(3), false)),
            false);
    assertThat(E.apply(rare3, Action.EXALT, null, Set.of("SinistralExaltation"), rng).error())
        .isEqualTo("다른 접두어 속성 부여를 추가할 수 없습니다.");
  }
}
