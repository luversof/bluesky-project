package net.luversof.api.poe.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * PoE1 제작 엔진(10-15 C335 — 사용자 요청 "직접 아이템을 제작해 보는 에뮬레이터 · 가장 적은 비용을 계산하는 시뮬레이터"의 바탕). 상태 없는 순수 규칙:
 * 아이템 상태 + 화폐 → 새 상태. 난수는 밖에서 받아(시드 고정 시험 · 몬테카를로) 같은 규칙을 에뮬레이터와 계산기가 같이 쓴다.
 *
 * <p>규칙(게임 · craftofexile 과 같은 계열):
 *
 * <ul>
 *   <li>옵션 하나 = 그 풀에서 붙을 수 있는 옵션(아이템 레벨 이하 · 자리가 남은 접두/접미 · 이미 있는 옵션 그룹이 아님) 중 가중치에 비례해 뽑는다
 *   <li>자리: 매직 접두 1 · 접미 1, 레어 접두 3 · 접미 3(주얼 2 · 2)
 *   <li>옵션 수: 매직 1 또는 2(반반), 레어 4 · 5 · 6 = 8 : 3 : 1(주얼 3 · 4 = 반반) — 클라이언트 데이터에 없는 값이라 커뮤니티
 *       측정값(craftofexile 계산과 같은 가정)
 *   <li>분열(fractured) 옵션은 카오스 · 소멸 · 정제가 건드리지 않고 옵션 수에 센다
 * </ul>
 */
public final class PoeCraftEngine {

  public enum Rarity {
    NORMAL,
    MAGIC,
    RARE
  }

  /** 화폐 — 에센스는 essence 인자로 어떤 에센스인지 받는다. */
  public enum Action {
    TRANSMUTE,
    ALTERATION,
    AUGMENTATION,
    REGAL,
    ALCHEMY,
    CHAOS,
    EXALT,
    ANNUL,
    SCOUR,
    ESSENCE
  }

  /** 옵션 하나 — crafted = 제작대, fractured = 분열(고정). */
  public record Affix(
      String id, String gen, List<Integer> groups, boolean crafted, boolean fractured) {}

  /** 아이템 상태 — 풀 키 = 클래스|변형|영향력. */
  public record Item(
      String itemClass,
      String variant,
      String influence,
      int ilvl,
      Rarity rarity,
      List<Affix> affixes,
      boolean corrupted) {

    public String poolKey() {
      return itemClass
          + "|"
          + (variant == null ? "" : variant)
          + "|"
          + (influence == null ? "" : influence);
    }

    public long count(String gen) {
      return affixes.stream().filter(a -> gen.equals(a.gen())).count();
    }

    Item with(Rarity r, List<Affix> a) {
      return new Item(itemClass, variant, influence, ilvl, r, List.copyOf(a), corrupted);
    }
  }

  /** 결과 — error 가 있으면 item 은 그대로(적용 안 됨). added/removed = 옵션 id. */
  public record Result(Item item, String error, List<String> added, List<String> removed) {

    static Result fail(Item item, String error) {
      return new Result(item, error, List.of(), List.of());
    }
  }

  /** 엔진이 보는 데이터 — 시험에선 작은 가짜 풀을 넣는다. */
  public interface Data {

    Map<String, Integer> pool(String poolKey);

    PoeCraftDataService.CraftMod mod(String id);

    PoeCraftDataService.Essence essence(String itemClass, String slug);
  }

  /** 주얼은 자리가 2 · 2(심연 주얼 포함). */
  static boolean jewel(String itemClass) {
    return itemClass != null && itemClass.contains("Jewel");
  }

  static int maxPerGen(Item item, Rarity rarity) {
    if (rarity == Rarity.MAGIC) {
      return 1;
    }
    return jewel(item.itemClass()) ? 2 : 3;
  }

  private final Data data;

  public PoeCraftEngine(Data data) {
    this.data = data;
  }

  public Result apply(Item item, Action action, String essenceSlug, RandomGenerator rng) {
    if (item.corrupted()) {
      return Result.fail(item, "부패한 아이템은 바꿀 수 없습니다");
    }
    List<Affix> fixed = item.affixes().stream().filter(Affix::fractured).toList();
    return switch (action) {
      case TRANSMUTE ->
          item.rarity() != Rarity.NORMAL
              ? Result.fail(item, "일반 아이템에만 쓸 수 있습니다")
              : roll(item, Rarity.MAGIC, List.of(), magicCount(rng), rng);
      case ALTERATION ->
          item.rarity() != Rarity.MAGIC
              ? Result.fail(item, "매직 아이템에만 쓸 수 있습니다")
              : roll(item, Rarity.MAGIC, fixed, magicCount(rng), rng);
      case AUGMENTATION -> {
        if (item.rarity() != Rarity.MAGIC) {
          yield Result.fail(item, "매직 아이템에만 쓸 수 있습니다");
        }
        if (item.affixes().size() >= 2) {
          yield Result.fail(item, "빈 자리가 없습니다");
        }
        yield add(item, Rarity.MAGIC, rng);
      }
      case REGAL ->
          item.rarity() != Rarity.MAGIC
              ? Result.fail(item, "매직 아이템에만 쓸 수 있습니다")
              : add(item, Rarity.RARE, rng);
      case ALCHEMY ->
          item.rarity() != Rarity.NORMAL
              ? Result.fail(item, "일반 아이템에만 쓸 수 있습니다")
              : roll(item, Rarity.RARE, List.of(), rareCount(item, rng), rng);
      case CHAOS ->
          item.rarity() != Rarity.RARE
              ? Result.fail(item, "레어 아이템에만 쓸 수 있습니다")
              : roll(item, Rarity.RARE, fixed, rareCount(item, rng), rng);
      case EXALT -> {
        if (item.rarity() != Rarity.RARE) {
          yield Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
        }
        if (item.count("prefix") >= maxPerGen(item, Rarity.RARE)
            && item.count("suffix") >= maxPerGen(item, Rarity.RARE)) {
          yield Result.fail(item, "빈 자리가 없습니다");
        }
        yield add(item, Rarity.RARE, rng);
      }
      case ANNUL -> annul(item, rng);
      case SCOUR -> {
        List<String> removed =
            item.affixes().stream().filter(a -> !a.fractured()).map(Affix::id).toList();
        yield new Result(
            item.with(fixed.isEmpty() ? Rarity.NORMAL : item.rarity(), fixed),
            null,
            List.of(),
            removed);
      }
      case ESSENCE -> essence(item, essenceSlug, fixed, rng);
    };
  }

  /** 매직 옵션 수 — 1 · 2 반반. */
  static int magicCount(RandomGenerator rng) {
    return rng.nextInt(2) + 1;
  }

  /** 레어 옵션 수 — 4 · 5 · 6 = 8 : 3 : 1, 주얼 3 · 4 반반. */
  static int rareCount(Item item, RandomGenerator rng) {
    if (jewel(item.itemClass())) {
      return 3 + rng.nextInt(2);
    }
    int r = rng.nextInt(12);
    return r < 8 ? 4 : r < 11 ? 5 : 6;
  }

  /** 남길 옵션(keep)에서 시작해 옵션 수 target 까지 채운다(자리가 없으면 거기서 멈춘다). */
  private Result roll(Item item, Rarity rarity, List<Affix> keep, int target, RandomGenerator rng) {
    List<Affix> out = new ArrayList<>(keep);
    List<String> added = new ArrayList<>();
    while (out.size() < target) {
      Affix a = pick(item.with(rarity, out), rarity, rng);
      if (a == null) {
        break;
      }
      out.add(a);
      added.add(a.id());
    }
    List<String> removed =
        item.affixes().stream().filter(a -> !keep.contains(a)).map(Affix::id).toList();
    return new Result(item.with(rarity, out), null, added, removed);
  }

  private Result add(Item item, Rarity rarity, RandomGenerator rng) {
    Affix a = pick(item, rarity, rng);
    if (a == null) {
      return Result.fail(item, "붙을 수 있는 옵션이 없습니다");
    }
    List<Affix> out = new ArrayList<>(item.affixes());
    out.add(a);
    return new Result(item.with(rarity, out), null, List.of(a.id()), List.of());
  }

  private Result annul(Item item, RandomGenerator rng) {
    if (item.rarity() == Rarity.NORMAL) {
      return Result.fail(item, "매직 · 레어 아이템에만 쓸 수 있습니다");
    }
    List<Affix> removable = item.affixes().stream().filter(a -> !a.fractured()).toList();
    if (removable.isEmpty()) {
      return Result.fail(item, "지울 옵션이 없습니다");
    }
    Affix gone = removable.get(rng.nextInt(removable.size()));
    List<Affix> out = new ArrayList<>(item.affixes());
    out.remove(gone);
    return new Result(item.with(item.rarity(), out), null, List.of(), List.of(gone.id()));
  }

  private Result essence(Item item, String slug, List<Affix> fixed, RandomGenerator rng) {
    PoeCraftDataService.Essence e = data.essence(item.itemClass(), slug);
    if (e == null || e.modId() == null) {
      return Result.fail(item, "이 아이템 종류에 쓸 수 없는 에센스입니다");
    }
    if (item.rarity() == Rarity.MAGIC) {
      return Result.fail(item, "일반 · 레어 아이템에만 쓸 수 있습니다");
    }
    if (e.ilvlMax() > 0 && item.ilvl() > e.ilvlMax()) {
      return Result.fail(item, "아이템 레벨 " + e.ilvlMax() + " 이하에만 쓸 수 있는 에센스입니다");
    }
    // 레어에 쓰면 카오스처럼 다시 굴리되 그 옵션을 보장(일반이면 레어로)
    Affix forced =
        new Affix(e.modId(), e.gen(), e.groups() == null ? List.of() : e.groups(), false, false);
    if (fixed.stream().anyMatch(a -> a.gen().equals(forced.gen()) && sharesGroup(a, forced))) {
      return Result.fail(item, "분열 옵션과 같은 그룹이라 쓸 수 없습니다");
    }
    List<Affix> keep = new ArrayList<>(fixed);
    keep.add(forced);
    Result r = roll(item, Rarity.RARE, keep, Math.max(rareCount(item, rng), keep.size()), rng);
    List<String> added = new ArrayList<>();
    added.add(forced.id());
    added.addAll(r.added());
    List<String> removed =
        item.affixes().stream().filter(a -> !a.fractured()).map(Affix::id).toList();
    return new Result(r.item(), null, added, removed);
  }

  private static boolean sharesGroup(Affix a, Affix b) {
    for (Integer g : a.groups()) {
      if (b.groups().contains(g)) {
        return true;
      }
    }
    return false;
  }

  /** 지금 붙을 수 있는 옵션 → 가중치(에뮬레이터 옵션 풀 패널 · 계산기와 같은 규칙). */
  /**
   * 풀 색인(10-15 C336 — 계산기가 수십만 번 굴린다): 옵션마다 맵 조회 · 스트림을 돌던 것을 배열 한 번 훑기로. 데이터가 다시 읽히면(풀 맵이 바뀌면) 새로
   * 만든다.
   */
  private record PoolIndex(
      Map<String, Integer> source,
      String[] ids,
      int[] weights,
      boolean[] prefix,
      int[] ilvl,
      int[][] groups,
      List<Integer>[] groupLists) {}

  private final java.util.concurrent.ConcurrentHashMap<String, PoolIndex> indexes =
      new java.util.concurrent.ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  private PoolIndex index(String poolKey) {
    Map<String, Integer> pool = data.pool(poolKey);
    PoolIndex cached = indexes.get(poolKey);
    if (cached != null && cached.source() == pool) {
      return cached;
    }
    List<String> ids = new ArrayList<>();
    for (Map.Entry<String, Integer> e : pool.entrySet()) {
      PoeCraftDataService.CraftMod m = data.mod(e.getKey());
      if (m != null && e.getValue() > 0 && !Boolean.TRUE.equals(m.ess())) {
        ids.add(e.getKey());
      }
    }
    int n = ids.size();
    String[] idArr = ids.toArray(new String[0]);
    int[] w = new int[n];
    boolean[] pre = new boolean[n];
    int[] lv = new int[n];
    int[][] grp = new int[n][];
    List<Integer>[] grpList = new List[n];
    for (int i = 0; i < n; i++) {
      PoeCraftDataService.CraftMod m = data.mod(idArr[i]);
      w[i] = pool.get(idArr[i]);
      pre[i] = "prefix".equals(m.g());
      lv[i] = m.l();
      grpList[i] = m.grp() == null ? List.of() : m.grp();
      grp[i] = grpList[i].stream().mapToInt(Integer::intValue).toArray();
    }
    PoolIndex px = new PoolIndex(pool, idArr, w, pre, lv, grp, grpList);
    indexes.put(poolKey, px);
    return px;
  }

  /** 지금 붙을 수 있으면 true — 아이템 레벨 · 자리 · 이미 있는 옵션 그룹. */
  private static boolean open(
      PoolIndex px,
      int i,
      int ilvl,
      boolean prefixOpen,
      boolean suffixOpen,
      int[] taken,
      int takenN) {
    if (px.ilvl()[i] > ilvl || (px.prefix()[i] ? !prefixOpen : !suffixOpen)) {
      return false;
    }
    for (int g : px.groups()[i]) {
      for (int k = 0; k < takenN; k++) {
        if (taken[k] == g) {
          return false;
        }
      }
    }
    return true;
  }

  /** 아이템의 옵션 그룹 — [0] 에 개수. */
  private static int[] takenGroups(Item item) {
    int n = 0;
    for (Affix a : item.affixes()) {
      n += a.groups().size();
    }
    int[] out = new int[n + 1];
    int k = 1;
    for (Affix a : item.affixes()) {
      for (Integer g : a.groups()) {
        out[k++] = g;
      }
    }
    out[0] = n;
    return out;
  }

  /** 지금 붙을 수 있는 옵션 → 가중치(에뮬레이터 옵션 풀 패널 · 계산기와 같은 규칙). */
  public Map<String, Integer> eligible(Item item, Rarity rarity) {
    PoolIndex px = index(item.poolKey());
    int max = maxPerGen(item, rarity);
    int pre = 0;
    int suf = 0;
    for (Affix a : item.affixes()) {
      if ("prefix".equals(a.gen())) {
        pre++;
      } else {
        suf++;
      }
    }
    int[] tg = takenGroups(item);
    int[] taken = java.util.Arrays.copyOfRange(tg, 1, tg.length);
    Map<String, Integer> out = new java.util.LinkedHashMap<>();
    for (int i = 0; i < px.ids().length; i++) {
      if (open(px, i, item.ilvl(), pre < max, suf < max, taken, tg[0])) {
        out.put(px.ids()[i], px.weights()[i]);
      }
    }
    return out;
  }

  private Affix pick(Item item, Rarity rarity, RandomGenerator rng) {
    PoolIndex px = index(item.poolKey());
    int max = maxPerGen(item, rarity);
    int pre = 0;
    int suf = 0;
    for (Affix a : item.affixes()) {
      if ("prefix".equals(a.gen())) {
        pre++;
      } else {
        suf++;
      }
    }
    boolean po = pre < max;
    boolean so = suf < max;
    int[] tg = takenGroups(item);
    int[] taken = java.util.Arrays.copyOfRange(tg, 1, tg.length);
    int n = px.ids().length;
    long total = 0;
    for (int i = 0; i < n; i++) {
      if (open(px, i, item.ilvl(), po, so, taken, tg[0])) {
        total += px.weights()[i];
      }
    }
    if (total <= 0) {
      return null;
    }
    long r = rng.nextLong(total);
    for (int i = 0; i < n; i++) {
      if (open(px, i, item.ilvl(), po, so, taken, tg[0])) {
        r -= px.weights()[i];
        if (r < 0) {
          return new Affix(
              px.ids()[i], px.prefix()[i] ? "prefix" : "suffix", px.groupLists()[i], false, false);
        }
      }
    }
    return null;
  }
}
