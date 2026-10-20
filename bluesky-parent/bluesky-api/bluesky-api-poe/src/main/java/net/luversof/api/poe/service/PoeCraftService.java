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
      int maxPerGen,
      // 메타 제작 상태(10-15 C338) — 접두/접미 잠금 · 제작 옵션 3개 · 공격/주문 옵션 금지
      PoeCraftEngine.Meta meta,
      // 해제 제시 셋(C341) — 장막 옵션이 있을 때만, 없으면 빈 목록
      List<Line> unveilOffers,
      // 지금 쓸 수 없는 화폐 → 이유(10-15 C340 — 엔진이 시험 적용해 본 결과, 화면이 버튼을 끄고 이유를 보인다). 정복자는 "CONQUEROR:영향력"
      Map<String, String> blocked,
      // 임플리싯 줄(C343, 엘드리치 — gen = exarch|eater, tierRank = 등급 1~6) · 우세 진영(exarch|eater|null)
      List<Line> implicits,
      String dominance,
      // 결과 설명(C358 — 바알 오브 "vaal:결과[:사유]", C385 기폭제 "catalyst:종류:양:개수"), 없으면 null
      String note,
      // 기폭제 퀄리티로 수치가 오르는 옵션 · 임플리싯 id(C385) — 화면이 굴린 값에 (100 + 퀄리티) / 100 을 곱한다
      List<String> boosted,
      // 옵션 태그(C480 — 인게임 상세 보기 " — 생명력, 방어력") — 줄 id → 태그 이름(영어 · 한국어), 이름 있는 태그만
      Map<String, List<String>> tags,
      Map<String, List<String>> tagsKo) {}

  /**
   * 아이템 종류에 쓸 수 있는 제작 수단(10-15 C338) — conquerors = 그 종류에 풀이 있는 정복자 영향력. 화석은 simple(엔진이 다루는 것)이 아닌
   * 것도 싣고 화면이 "준비 중"으로 보인다.
   */
  public record Options(
      List<PoeCraftDataService.Essence> essences,
      List<PoeCraftDataService.Fossil> fossils,
      List<PoeCraftDataService.Resonator> resonators,
      List<PoeCraftDataService.Harvest> harvest,
      List<PoeCraftDataService.BenchCraft> bench,
      List<String> conquerors,
      // 화폐 인게임 사용 설명(C340) — 버튼 설명 · 제약 근거
      List<PoeCraftDataService.CurrencyInfo> currencies) {}

  public Options options(String itemClass, String variant) {
    List<String> conq =
        PoeCraftEngine.INFLUENCE_EXALTS.stream()
            .filter(c -> data.hasPool(itemClass + "|" + (variant == null ? "" : variant) + "|" + c))
            .sorted()
            .toList();
    return new Options(
        data.essences(itemClass),
        data.fossils(),
        data.resonators(),
        data.harvests(),
        data.bench(itemClass),
        conq,
        data.currencyInfos());
  }

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

              @Override
              public PoeCraftDataService.Fossil fossil(String slug) {
                return data.fossil(slug);
              }

              @Override
              public PoeCraftDataService.BenchCraft bench(String itemClass, String modId) {
                return data.bench(itemClass, modId);
              }

              @Override
              public PoeCraftDataService.Harvest harvest(String id) {
                return data.harvest(id);
              }

              @Override
              public Map<String, Integer> veiledPool(String poolKey) {
                return data.veiledPool(poolKey);
              }

              @Override
              public PoeCraftDataService.VeiledMod veiledMod(String id) {
                return data.veiledMod(id);
              }

              @Override
              public Map<String, Integer> fossilPool(String poolKey) {
                return data.fossilPool(poolKey);
              }

              @Override
              public PoeCraftDataService.VeiledMod fossilMod(String id) {
                return data.fossilMod(id);
              }

              @Override
              public List<PoeCraftDataService.Essence> corruptEssences(String itemClass) {
                return data.corruptEssences(itemClass);
              }

              @Override
              public Map<String, Integer> eldritchPool(String poolKey) {
                return data.eldritchPool(poolKey);
              }

              @Override
              public PoeCraftDataService.EldritchMod eldritchMod(String id) {
                return data.eldritchMod(id);
              }

              @Override
              public Map<String, Integer> corruptedPool(String poolKey) {
                return data.corruptedPool(poolKey);
              }

              @Override
              public PoeCraftDataService.VeiledMod corruptedMod(String id) {
                return data.corruptedMod(id);
              }

              @Override
              public PoeCraftDataService.VeiledMod beastMod(String id) {
                return data.beastMod(id);
              }

              @Override
              public Map<String, PoeCraftDataService.HazardMod> hazardMods() {
                return data.hazardMods();
              }

              @Override
              public Map<String, PoeCraftDataService.InfamousMod> infamousMods() {
                return data.infamousMods();
              }

              @Override
              public Map<String, String> upgrade(String poolKey) {
                return data.upgrade(poolKey);
              }

              @Override
              public PoeCraftDataService.VeiledMod elevatedMod(String id) {
                return data.elevatedMod(id);
              }
            });
  }

  /** 해제 옵션 한 줄(C421) — 시뮬레이터 목표 고르기. */
  public record VeiledLine(
      String id, String gen, String name, String nameKo, List<String> en, List<String> ko) {}

  /** 이 종류(영향력 없음)의 해제 옵션(C421). */
  public List<VeiledLine> veiledLines(String itemClass, String variant) {
    List<VeiledLine> out = new ArrayList<>();
    for (String id :
        new java.util.TreeSet<>(
            data.veiledPool(itemClass + "|" + (variant == null ? "" : variant) + "|").keySet())) {
      PoeCraftDataService.VeiledMod v = data.veiledMod(id);
      if (v != null) {
        out.add(
            new VeiledLine(
                id,
                v.g(),
                v.name(),
                v.nameKo(),
                v.en() == null ? List.of() : v.en(),
                v.ko() == null ? List.of() : v.ko()));
      }
    }
    return out;
  }

  /** 해제 옵션 id 들(C421) — 계산기가 목표에 해제 옵션이 있는지 볼 때. */
  public java.util.Set<String> veiledIds(PoeCraftEngine.Item item) {
    return data.veiledPool(item.poolKey()).keySet();
  }

  public PoeCraftEngine engine() {
    return engine;
  }

  public PoeCraftDataService.CraftMod mod(String id) {
    return data.mod(id);
  }

  public List<PoeCraftDataService.Fossil> fossils() {
    return data.fossils();
  }

  public List<PoeCraftDataService.Resonator> resonators() {
    return data.resonators();
  }

  public List<PoeCraftDataService.Harvest> harvests() {
    return data.harvests();
  }

  public PoeCraftDataService.BenchCraft bench(String itemClass, String modId) {
    return data.bench(itemClass, modId);
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
    return view(r.item(), r.error(), r.added(), r.removed(), r.note());
  }

  public View view(PoeCraftEngine.Item item) {
    return view(normalize(item), null, List.of(), List.of(), null);
  }

  /** 상태마다 옵션 줄(C455 — 에뮬레이터 단계 기록이 붙은 · 지운 옵션을 id 로 대조). 임플리싯(엘드리치 · 타락)도 뒤에 붙인다. */
  public List<List<Line>> linesOf(List<PoeCraftEngine.Item> items) {
    List<List<Line>> out = new ArrayList<>();
    for (PoeCraftEngine.Item item : items == null ? List.<PoeCraftEngine.Item>of() : items) {
      if (item == null || item.itemClass() == null) {
        out.add(List.of());
        continue;
      }
      PoeCraftEngine.Item it = normalize(item);
      List<Line> ls = new ArrayList<>();
      for (PoeCraftEngine.Affix a : it.affixes()) {
        ls.add(line(it, a));
      }
      ls.sort(Comparator.comparing((Line l) -> "prefix".equals(l.gen()) ? 0 : 1));
      for (PoeCraftEngine.Affix a : it.implicits()) {
        ls.add(implicitLine(a));
      }
      out.add(ls);
    }
    return out;
  }

  /** 옵션 그룹 · 접두/접미는 데이터에서 다시 채운다(클라이언트가 보낸 값을 믿지 않는다). 모르는 옵션은 뺀다. */
  PoeCraftEngine.Item normalize(PoeCraftEngine.Item item) {
    List<PoeCraftEngine.Affix> out = new ArrayList<>();
    for (PoeCraftEngine.Affix a :
        item.affixes() == null ? List.<PoeCraftEngine.Affix>of() : item.affixes()) {
      // 장막 자리 표시 · 해제 옵션(C341)
      if (PoeCraftEngine.VEILED_PREFIX.equals(a.id())
          || PoeCraftEngine.VEILED_SUFFIX.equals(a.id())) {
        out.add(
            PoeCraftEngine.VEILED_PREFIX.equals(a.id())
                ? new PoeCraftEngine.Affix(a.id(), "prefix", List.of(-1), false, false)
                : new PoeCraftEngine.Affix(a.id(), "suffix", List.of(-2), false, false));
        continue;
      }
      PoeCraftDataService.VeiledMod vm = data.veiledMod(a.id());
      if (vm == null) {
        vm = data.fossilMod(a.id());
      }
      if (vm == null) {
        vm = data.elevatedMod(a.id());
      }
      if (vm != null) {
        out.add(
            new PoeCraftEngine.Affix(
                a.id(), vm.g(), vm.grp() == null ? List.of() : vm.grp(), false, a.fractured()));
        continue;
      }
      // 사이악산의 두카트 악명 높은 속성(C393) — 풀 밖, 보통 옵션
      PoeCraftDataService.InfamousMod inf = data.infamousMods().get(a.id());
      if (inf != null) {
        out.add(
            new PoeCraftEngine.Affix(
                a.id(), inf.g(), inf.grp() == null ? List.of() : inf.grp(), false, a.fractured()));
        continue;
      }
      // 부패모의 두카트 옵션(C391) — 풀 밖, 보통 옵션
      PoeCraftDataService.HazardMod hz = data.hazardMods().get(a.id());
      if (hz != null) {
        out.add(
            new PoeCraftEngine.Affix(
                a.id(), hz.g(), hz.grp() == null ? List.of() : hz.grp(), false, a.fractured()));
        continue;
      }
      // 짐승 제작 위상 스킬(C388) — 제작 접미어
      PoeCraftDataService.VeiledMod bm = a.crafted() ? data.beastMod(a.id()) : null;
      if (bm != null) {
        out.add(
            new PoeCraftEngine.Affix(
                a.id(), "suffix", bm.grp() == null ? List.of() : bm.grp(), true, false));
        continue;
      }
      // 제작대 옵션(메타 제작 포함)은 제작 풀 데이터에 없다 — 제작대 데이터에서(C338)
      PoeCraftDataService.BenchCraft b = a.crafted() ? data.bench(item.itemClass(), a.id()) : null;
      if (b != null) {
        out.add(
            new PoeCraftEngine.Affix(
                a.id(), b.gen(), b.groups() == null ? List.of() : b.groups(), true, false));
        continue;
      }
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
    List<PoeCraftEngine.Affix> imp = new ArrayList<>();
    for (PoeCraftEngine.Affix a :
        item.implicits() == null ? List.<PoeCraftEngine.Affix>of() : item.implicits()) {
      // 바알 오브 부패 임플리싯(C358) — 하나만, 있으면 다른 임플리싯은 없다
      PoeCraftDataService.VeiledMod cm = data.corruptedMod(a.id());
      if (cm != null) {
        imp.clear();
        imp.add(
            new PoeCraftEngine.Affix(
                a.id(),
                PoeCraftEngine.CORRUPTED,
                cm.grp() == null ? List.of() : cm.grp(),
                false,
                false));
        break;
      }
      PoeCraftDataService.EldritchMod em = data.eldritchMod(a.id());
      if (em != null && imp.stream().noneMatch(x -> x.gen().equals(em.g()))) {
        imp.add(
            new PoeCraftEngine.Affix(
                a.id(), em.g(), em.grp() == null ? List.of() : em.grp(), false, false));
      }
    }
    return new PoeCraftEngine.Item(
        item.itemClass(),
        item.variant(),
        item.influence(),
        Math.max(1, Math.min(100, item.ilvl())),
        item.rarity() == null ? PoeCraftEngine.Rarity.NORMAL : item.rarity(),
        out,
        item.corrupted(),
        imp,
        quality(item),
        item.veilSeed());
  }

  /** 기폭제 퀄리티(C385) — 장신구 · 아는 종류 · 1~20% 만 남긴다. */
  private static String quality(PoeCraftEngine.Item item) {
    String k = item.qualityKey();
    int q = item.qualityAmount();
    return k != null
            && PoeCraftEngine.CATALYST_TAGS.containsKey(k)
            && PoeCraftEngine.JEWELLERY.contains(item.itemClass())
            && q >= 1
            && q <= PoeCraftEngine.MAX_QUALITY
        ? k + ":" + q
        : null;
  }

  private View view(
      PoeCraftEngine.Item item,
      String error,
      List<String> added,
      List<String> removed,
      String note) {
    List<Line> lines = new ArrayList<>();
    for (PoeCraftEngine.Affix a : item.affixes()) {
      lines.add(line(item, a));
    }
    // 인게임 툴팁 순서: 접두 먼저, 그다음 접미
    lines.sort(Comparator.comparing((Line l) -> "prefix".equals(l.gen()) ? 0 : 1));
    return new View(
        item,
        error,
        added,
        removed,
        lines,
        PoeCraftEngine.maxPerGen(item, item.rarity()),
        PoeCraftEngine.meta(item.affixes()),
        engine.unveilOffers(item).stream()
            .map(
                id ->
                    line(
                        item,
                        new PoeCraftEngine.Affix(
                            id, data.veiledMod(id).g(), data.veiledMod(id).grp(), false, false)))
            .toList(),
        blocked(item),
        item.implicits().stream().map(this::implicitLine).toList(),
        engine.dominance(item),
        note,
        engine.boosted(item),
        lineTags(item, 0),
        lineTags(item, 1));
  }

  /** 줄 id → 태그 이름(C480) — lang 0 = 영어 · 1 = 한국어, 옵션 태그(ImplicitTags) 순서, 이름 없는 태그는 뺀다. */
  private Map<String, List<String>> lineTags(PoeCraftEngine.Item item, int lang) {
    Map<String, List<String>> names = data.tagNames();
    Map<String, List<String>> out = new java.util.LinkedHashMap<>();
    for (PoeCraftEngine.Affix a : item.affixes()) {
      PoeCraftDataService.CraftMod m = data.mod(a.id());
      if (m == null || m.t() == null) {
        continue;
      }
      List<String> ts = new ArrayList<>();
      for (String t : m.t()) {
        List<String> n = names.get(t);
        if (n != null && n.size() > lang) {
          ts.add(n.get(lang));
        }
      }
      if (!ts.isEmpty()) {
        out.put(a.id(), ts);
      }
    }
    return out;
  }

  /** 엘드리치 임플리싯 줄 — 이름 = 진영(인게임 "작열의 총주교 아이템" · "세계 포식자 아이템"), tierRank = 등급. */
  private Line implicitLine(PoeCraftEngine.Affix a) {
    if (PoeCraftEngine.CORRUPTED.equals(a.gen())) {
      // 바알 오브 부패 임플리싯(C358) — 이름 = 인게임 "타락"
      PoeCraftDataService.VeiledMod c = data.corruptedMod(a.id());
      return new Line(
          a.id(),
          a.gen(),
          "Corrupted",
          "타락",
          c == null ? List.of() : c.en(),
          c == null ? List.of() : c.ko(),
          c == null ? List.of() : c.enMin(),
          c == null ? List.of() : c.koMin(),
          null,
          c == null ? 0 : c.l(),
          false,
          false,
          false);
    }
    PoeCraftDataService.EldritchMod m = data.eldritchMod(a.id());
    boolean ex = PoeCraftEngine.EXARCH.equals(a.gen());
    return new Line(
        a.id(),
        a.gen(),
        ex ? "Searing Exarch" : "Eater of Worlds",
        ex ? "작열의 총주교" : "세계 포식자",
        m == null ? List.of() : m.en(),
        m == null ? List.of() : m.ko(),
        m == null ? List.of() : m.enMin(),
        m == null ? List.of() : m.koMin(),
        m == null ? null : m.tier(),
        m == null ? 0 : m.l(),
        false,
        false,
        false);
  }

  /** 인자 없는 화폐 · 정복자를 시험 적용해 못 쓰는 이유를 모은다(고정 난수 — 결과는 버린다). */
  Map<String, String> blocked(PoeCraftEngine.Item item) {
    Map<String, String> out = new java.util.LinkedHashMap<>();
    RandomGenerator rng = new java.util.SplittableRandom(0);
    for (PoeCraftEngine.Action a :
        List.of(
            PoeCraftEngine.Action.TRANSMUTE,
            PoeCraftEngine.Action.ALTERATION,
            PoeCraftEngine.Action.AUGMENTATION,
            PoeCraftEngine.Action.REGAL,
            PoeCraftEngine.Action.ALCHEMY,
            PoeCraftEngine.Action.CHAOS,
            PoeCraftEngine.Action.EXALT,
            PoeCraftEngine.Action.ANNUL,
            PoeCraftEngine.Action.SCOUR,
            PoeCraftEngine.Action.FRACTURE,
            PoeCraftEngine.Action.BENCH_REMOVE,
            PoeCraftEngine.Action.VEILED_CHAOS,
            PoeCraftEngine.Action.VEILED_EXALT,
            PoeCraftEngine.Action.ELDRITCH_CHAOS,
            PoeCraftEngine.Action.ELDRITCH_EXALT,
            PoeCraftEngine.Action.ELDRITCH_ANNUL,
            PoeCraftEngine.Action.DOMINANCE,
            PoeCraftEngine.Action.CONFLICT,
            PoeCraftEngine.Action.FOULBORN_AUG,
            PoeCraftEngine.Action.FOULBORN_REGAL,
            PoeCraftEngine.Action.FOULBORN_EXALT,
            PoeCraftEngine.Action.VAAL,
            PoeCraftEngine.Action.TAINTED_CHAOS,
            PoeCraftEngine.Action.TAINTED_EXALT,
            PoeCraftEngine.Action.TAINTED_DIVINE)) {
      String e = engine.apply(item, a, null, rng).error();
      if (e != null) {
        out.put(a.name(), e);
      }
    }
    // 불씨 · 영액 — 등급과 상관없는 제약(부위 · 영향력 · 타락)만 보려고 하급으로 시험
    for (PoeCraftEngine.Action a :
        List.of(PoeCraftEngine.Action.EMBER, PoeCraftEngine.Action.ICHOR)) {
      String e = engine.apply(item, a, "1", rng).error();
      if (e != null) {
        out.put(a.name(), e);
      }
    }
    for (String c : PoeCraftEngine.INFLUENCE_EXALTS) {
      String e = engine.apply(item, PoeCraftEngine.Action.CONQUEROR, c, rng).error();
      if (e != null) {
        out.put("CONQUEROR:" + c, e);
      }
    }
    // 묶음 안 옵션(C476 — 사용자 요청 "아이템 상태에 따라 사용 가능한 제작 방식과 세부 제작 옵션이 활성/비활성", "A 제작을 하면 B 에서 어떤 제작이
    // 비활성화되는 조건") — 에센스 · 화석(하나씩) · 수확 · 제작대 · 짐승도 엔진으로 시험 적용해 못 쓰는 이유를 모은다(엔진 규칙 = 화면 규칙). 메타 제작
    // (접두/접미 변경 불가 · 제작 옵션 셋) · 제작 옵션 한도 · 영향력 · 희귀도 · 타락 같은 앞선 제작의 결과가 그대로 반영된다
    for (PoeCraftDataService.Essence e : data.essences(item.itemClass())) {
      String r = engine.apply(item, PoeCraftEngine.Action.ESSENCE, e.slug(), rng).error();
      if (r != null) {
        out.put("ESSENCE:" + e.slug(), r);
      }
    }
    for (PoeCraftDataService.Fossil f : data.fossils()) {
      String r = engine.apply(item, PoeCraftEngine.Action.FOSSIL, f.slug(), rng).error();
      if (r != null) {
        out.put("FOSSIL:" + f.slug(), r);
      }
    }
    for (PoeCraftDataService.Harvest h : data.harvests()) {
      String r = engine.apply(item, PoeCraftEngine.Action.HARVEST, h.id(), rng).error();
      if (r != null) {
        out.put("HARVEST:" + h.id(), r);
      }
    }
    for (PoeCraftDataService.BenchCraft b : data.bench(item.itemClass())) {
      if (b.modId() == null || out.containsKey("BENCH:" + b.modId())) {
        continue;
      }
      String r = engine.apply(item, PoeCraftEngine.Action.BENCH, b.modId(), rng).error();
      if (r != null) {
        out.put("BENCH:" + b.modId(), r);
      }
    }
    for (String arg : PoeCraftCalcService.BEAST_RECIPE.keySet()) {
      if ("imprint".equals(arg)) {
        continue; // 각인은 게이트가 따로(매직 · 각인 유무)
      }
      String r = engine.apply(item, PoeCraftEngine.Action.BEAST, arg, rng).error();
      if (r != null) {
        out.put("BEAST:" + arg, r);
      }
    }
    for (String c : PoeCraftEngine.INFLUENCE_EXALTS) {
      // 각성자의 오브(C346) — 없앨 아이템 영향력별(C391 — 셰이퍼 · 엘더도)
      String w = engine.apply(item, PoeCraftEngine.Action.AWAKENER, c, rng).error();
      if (w != null) {
        out.put("AWAKENER:" + c, w);
      }
    }
    return out;
  }

  private Line line(PoeCraftEngine.Item item, PoeCraftEngine.Affix a) {
    if (PoeCraftEngine.VEILED_PREFIX.equals(a.id())
        || PoeCraftEngine.VEILED_SUFFIX.equals(a.id())) {
      boolean pre = PoeCraftEngine.VEILED_PREFIX.equals(a.id());
      return new Line(
          a.id(),
          a.gen(),
          pre ? "Veiled Prefix" : "Veiled Suffix",
          pre ? "장막 접두어" : "장막 접미어",
          // 인게임 문구(ClientStrings ItemDisplayVeiledPrefix/Suffix)
          List.of(pre ? "Veiled Prefix" : "Veiled Suffix"),
          List.of(pre ? "장막 접두어" : "장막 접미어"),
          List.of(),
          List.of(),
          null,
          0,
          false,
          false,
          false);
    }
    PoeCraftDataService.InfamousMod inf = data.infamousMods().get(a.id());
    if (inf != null) {
      // 사이악산의 두카트 악명 높은 속성(C393)
      return new Line(
          a.id(),
          a.gen(),
          inf.name(),
          inf.nameKo(),
          inf.en(),
          inf.ko(),
          inf.enMin(),
          inf.koMin(),
          null,
          inf.l(),
          false,
          a.fractured(),
          false);
    }
    PoeCraftDataService.HazardMod hz = data.hazardMods().get(a.id());
    if (hz != null) {
      // 부패모의 두카트 옵션(C391)
      return new Line(
          a.id(),
          a.gen(),
          hz.name(),
          hz.nameKo(),
          hz.en(),
          hz.ko(),
          hz.enMin(),
          hz.koMin(),
          null,
          hz.l(),
          false,
          a.fractured(),
          false);
    }
    PoeCraftDataService.VeiledMod bm = a.crafted() ? data.beastMod(a.id()) : null;
    if (bm != null) {
      // 짐승 제작 위상 스킬(C388) — 제작 옵션 줄
      return new Line(
          a.id(),
          a.gen(),
          bm.name(),
          bm.nameKo(),
          bm.en(),
          bm.ko(),
          bm.enMin(),
          bm.koMin(),
          null,
          bm.l(),
          true,
          false,
          false);
    }
    PoeCraftDataService.VeiledMod vm = data.veiledMod(a.id());
    if (vm == null) {
      vm = data.fossilMod(a.id());
    }
    if (vm == null) {
      vm = data.elevatedMod(a.id());
    }
    if (vm != null) {
      return new Line(
          a.id(),
          a.gen(),
          vm.name(),
          vm.nameKo(),
          vm.en(),
          vm.ko(),
          vm.enMin(),
          vm.koMin(),
          null,
          vm.l(),
          false,
          a.fractured(),
          false);
    }
    PoeCraftDataService.BenchCraft b = a.crafted() ? data.bench(item.itemClass(), a.id()) : null;
    if (b != null) {
      return new Line(
          a.id(),
          a.gen(),
          b.modName(),
          b.modName(),
          b.en(),
          b.ko(),
          b.enMin(),
          b.koMin(),
          null,
          b.modLevel() == null ? 0 : b.modLevel(),
          true,
          a.fractured(),
          false);
    }
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
