package net.luversof.api.poe.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Service;

/**
 * 제작 에뮬레이터 · 계산기 서비스(10-15 C335) — {@link PoeCraftEngine} 에 데이터를 잇고, 결과에 옵션 문구(mods.json 등급 · 에센스)와
 * 그 풀 안 등급을 붙인다.
 */
@Service
public class PoeCraftService {

  /** 옵션 한 줄(표시용) — tierRank = 그 풀 같은 계열에서 1 이 최상위, essence = 에센스 보장 옵션. */
  public record Line(
      String id,
      String gen,
      String name,
      String nameKo,
      List<String> en,
      List<String> ko,
      // 최소 굴림 문구(범위 표기용 — en/ko 는 최대 굴림)
      List<String> enMin,
      List<String> koMin,
      Integer tierRank,
      int ilvl,
      boolean crafted,
      boolean fractured,
      boolean essence) {}

  /** 적용 결과 — error 면 item 은 그대로. */
  public record View(
      PoeCraftEngine.Item item,
      String error,
      List<String> added,
      List<String> removed,
      List<Line> lines,
      int maxPerGen) {}

  /** 옵션 풀 한 줄 — chance = 다음에 붙는 옵션 하나가 이것일 확률(0~1, 지금 아이템 상태 기준). */
  public record PoolLine(
      String id,
      String gen,
      String family,
      String name,
      String nameKo,
      List<String> en,
      List<String> ko,
      // 최소 굴림 문구(범위 표기용 — en/ko 는 최대 굴림)
      List<String> enMin,
      List<String> koMin,
      Integer tierRank,
      int ilvl,
      int weight,
      double chance) {}

  private final PoeCraftDataService data;
  private final PoeModDataService mods;
  private final PoeCraftEngine engine;

  public PoeCraftService(PoeCraftDataService data, PoeModDataService mods) {
    this.data = data;
    this.mods = mods;
    this.engine =
        new PoeCraftEngine(
            new PoeCraftEngine.Data() {
              @Override
              public Map<String, Integer> pool(String poolKey) {
                return data.pool(poolKey);
              }

              @Override
              public PoeCraftDataService.CraftMod mod(String id) {
                return data.mod(id);
              }

              @Override
              public PoeCraftDataService.Essence essence(String itemClass, String slug) {
                return data.essence(itemClass, slug);
              }
            });
  }

  public PoeCraftEngine engine() {
    return engine;
  }

  public PoeCraftDataService.CraftMod mod(String id) {
    return data.mod(id);
  }

  /** 옵션의 최소 굴림 영어 문구(계산기 에센스 비교용, C336). 모르면 null. */
  public List<String> minText(String id) {
    PoeModDataService.ModTier t = mods.tier(id);
    return t == null ? null : t.enMin();
  }

  public boolean knowsPool(PoeCraftEngine.Item item) {
    return data.hasPool(item.poolKey());
  }

  public List<PoeCraftDataService.Essence> essences(String itemClass) {
    return data.essences(itemClass);
  }

  public View apply(
      PoeCraftEngine.Item item, PoeCraftEngine.Action action, String essence, Long seed) {
    RandomGenerator rng =
        seed == null
            ? RandomGenerator.of("L64X128MixRandom")
            : new java.util.SplittableRandom(seed);
    PoeCraftEngine.Result r = engine.apply(normalize(item), action, essence, rng);
    return view(r.item(), r.error(), r.added(), r.removed());
  }

  public View view(PoeCraftEngine.Item item) {
    return view(normalize(item), null, List.of(), List.of());
  }

  /** 옵션 그룹 · 접두/접미는 데이터에서 다시 채운다(클라이언트가 보낸 값을 믿지 않는다). 모르는 옵션은 뺀다. */
  PoeCraftEngine.Item normalize(PoeCraftEngine.Item item) {
    List<PoeCraftEngine.Affix> out = new ArrayList<>();
    for (PoeCraftEngine.Affix a :
        item.affixes() == null ? List.<PoeCraftEngine.Affix>of() : item.affixes()) {
      PoeCraftDataService.CraftMod m = data.mod(a.id());
      if (m != null) {
        out.add(new PoeCraftEngine.Affix(a.id(), m.g(), m.grp(), a.crafted(), a.fractured()));
        continue;
      }
      PoeCraftDataService.Essence e =
          data.essences(item.itemClass()).stream()
              .filter(x -> a.id().equals(x.modId()))
              .findFirst()
              .orElse(null);
      if (e != null) {
        out.add(
            new PoeCraftEngine.Affix(
                a.id(),
                e.gen(),
                e.groups() == null ? List.of() : e.groups(),
                a.crafted(),
                a.fractured()));
      }
    }
    return new PoeCraftEngine.Item(
        item.itemClass(),
        item.variant(),
        item.influence(),
        Math.max(1, Math.min(100, item.ilvl())),
        item.rarity() == null ? PoeCraftEngine.Rarity.NORMAL : item.rarity(),
        out,
        item.corrupted());
  }

  private View view(
      PoeCraftEngine.Item item, String error, List<String> added, List<String> removed) {
    List<Line> lines = new ArrayList<>();
    for (PoeCraftEngine.Affix a : item.affixes()) {
      lines.add(line(item, a));
    }
    // 인게임 툴팁 순서: 접두 먼저, 그다음 접미
    lines.sort(Comparator.comparing((Line l) -> "prefix".equals(l.gen()) ? 0 : 1));
    return new View(
        item, error, added, removed, lines, PoeCraftEngine.maxPerGen(item, item.rarity()));
  }

  private Line line(PoeCraftEngine.Item item, PoeCraftEngine.Affix a) {
    PoeModDataService.ModTier t = mods.tier(a.id());
    PoeCraftDataService.CraftMod m = data.mod(a.id());
    if (t != null) {
      return new Line(
          a.id(),
          a.gen(),
          t.name(),
          t.nameKo(),
          t.en(),
          t.ko(),
          t.enMin(),
          t.koMin(),
          data.tierRank(item.poolKey(), a.id()),
          t.ilvl(),
          a.crafted(),
          a.fractured(),
          m != null && Boolean.TRUE.equals(m.ess()));
    }
    PoeCraftDataService.Essence e =
        data.essences(item.itemClass()).stream()
            .filter(x -> a.id().equals(x.modId()))
            .findFirst()
            .orElse(null);
    if (e != null) {
      return new Line(
          a.id(),
          a.gen(),
          e.modName(),
          e.modName(),
          e.en(),
          e.ko(),
          e.enMin(),
          e.koMin(),
          null,
          e.modLevel() == null ? 0 : e.modLevel(),
          a.crafted(),
          a.fractured(),
          true);
    }
    return new Line(
        a.id(),
        a.gen(),
        a.id(),
        a.id(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        null,
        0,
        a.crafted(),
        a.fractured(),
        false);
  }

  /**
   * 다음에 붙을 옵션의 풀 — rarity 를 주면 그 희귀도로 본 자리(일반 아이템에서 "변화를 쓰면" = MAGIC, "연금을 쓰면" = RARE). chance 는 다음
   * 옵션 하나가 그것일 확률.
   */
  public List<PoolLine> pool(PoeCraftEngine.Item item, PoeCraftEngine.Rarity rarity) {
    PoeCraftEngine.Item it = normalize(item);
    PoeCraftEngine.Rarity r =
        rarity != null
            ? rarity
            : it.rarity() == PoeCraftEngine.Rarity.NORMAL
                ? PoeCraftEngine.Rarity.RARE
                : it.rarity();
    Map<String, Integer> el = engine.eligible(it, r);
    long total = el.values().stream().mapToLong(Integer::longValue).sum();
    List<PoolLine> out = new ArrayList<>();
    for (Map.Entry<String, Integer> e : el.entrySet()) {
      PoeCraftDataService.CraftMod m = data.mod(e.getKey());
      PoeModDataService.ModTier t = mods.tier(e.getKey());
      out.add(
          new PoolLine(
              e.getKey(),
              m.g(),
              m.f(),
              t == null ? e.getKey() : t.name(),
              t == null ? null : t.nameKo(),
              t == null ? List.of() : t.en(),
              t == null ? List.of() : t.ko(),
              t == null ? List.of() : t.enMin(),
              t == null ? List.of() : t.koMin(),
              data.tierRank(it.poolKey(), e.getKey()),
              m.l(),
              e.getValue(),
              total == 0 ? 0 : (double) e.getValue() / total));
    }
    out.sort(
        Comparator.comparing((PoolLine p) -> "prefix".equals(p.gen()) ? 0 : 1)
            .thenComparing(PoolLine::family)
            .thenComparing(p -> p.tierRank() == null ? 99 : p.tierRank()));
    return out;
  }
}
