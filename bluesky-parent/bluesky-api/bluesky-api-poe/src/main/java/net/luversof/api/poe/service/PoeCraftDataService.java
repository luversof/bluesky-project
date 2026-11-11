package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * 제작 엔진 데이터(10-15 C335 — 사용자 요청 "craftofexile 같은 제작 시뮬레이터 · 에뮬레이터").
 * tools/poe-extract/parse-mods-full.mjs 가 만든 {@code craft-mods.json}(풀마다 옵션 id → 그 풀의 가중치, 옵션마다 계열
 * · 접두/접미 · 아이템 레벨 · 옵션 그룹 · 옵션 태그)과 parse-essences.mjs 의 {@code essences.json}(보장 옵션 id · 그룹).
 *
 * <p>mods.json 의 등급 가중치는 그 등급이 처음 나온 풀의 값이라(옵션 254개는 풀마다 가중치가 다르다 — 힘 500/1000/333) 제작 확률에는 이 파일의
 * 풀별 가중치를 쓴다.
 */
@Service
public class PoeCraftDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeCraftDataService.class);

  /**
   * 옵션 한 줄 — f = 계열 키(mods.json families), g = prefix|suffix, l = 아이템 레벨 요구, grp = 옵션 그룹(같은 그룹은 한
   * 아이템에 하나), t = 옵션 태그(화석 · 수확 가중치용), ess = 에센스 전용.
   */
  public record CraftMod(
      String f,
      String g,
      int l,
      List<Integer> grp,
      List<String> t,
      Boolean ess,
      // 옵션 유형 이름(C384, ModType — 수확 원소 변환이 이 이름으로 원본 · 대상을 고른다). 옛 데이터 · 시험이면 null
      String mt) {

    public CraftMod(String f, String g, int l, List<Integer> grp, List<String> t, Boolean ess) {
      this(f, g, l, grp, t, ess, null);
    }
  }

  private record CraftData(
      String patch,
      Map<String, CraftMod> mods,
      Map<String, Map<String, Integer>> pools,
      // 해제(Unveiled) 옵션(10-15 C341) — 장막 옵션을 해제할 때 셋 중 고르는 Jun 옵션
      Map<String, VeiledMod> veiledMods,
      Map<String, Map<String, Integer>> veiled,
      // 화석 추가 · 강제 옵션(10-15 C342) — 델브 도메인 중 화석이 직접 넣는 것(젬 레벨 · 심연 홈 등). 모양은 해제 옵션과 같다
      Map<String, VeiledMod> fossilMods,
      Map<String, Map<String, Integer>> fossil,
      // 섬뜩한 고정 속성(C343) — g = exarch|eater, tier 1 하급 ~ 6 완벽한
      Map<String, EldritchMod> eldritchMods,
      Map<String, Map<String, Integer>> eldritch,
      // 지배의 오브(C345) — 고양 옵션 · 풀별 한 등급 위
      Map<String, VeiledMod> elevatedMods,
      Map<String, Map<String, String>> upgrade,
      // 바알 오브 부패 고정 속성(C358) — 모양은 해제 옵션과 같다(g = corrupted)
      Map<String, VeiledMod> corruptedMods,
      Map<String, Map<String, Integer>> corrupted,
      // 야수 제작 위상 스킬(C388) — 제작 접미어, 레시피로만(풀 없음). 판테온 위상(C391 브라인훅의 두카트)도
      Map<String, VeiledMod> beastMods,
      // 부패모의 두카트 덫 · 지뢰 옵션(C391, 도메인 42) — w = 가중치
      Map<String, HazardMod> hazardMods,
      // 사이악산의 두카트 악명 높은 속성(C393, 도메인 41) — cls = 부위 태그
      Map<String, InfamousMod> infamousMods,
      // 옵션 태그 표시 이름(C480) — 태그 id → [영어, 한국어](Tags.Name, 이름이 있는 태그만 인게임 상세 보기에 보인다)
      Map<String, List<String>> tagNames) {}

  /** 악명 높은 속성(C393) — 해제 옵션 모양 + 부위 태그 · 가중치. */
  public record InfamousMod(
      String g,
      int l,
      List<Integer> grp,
      List<String> t,
      String name,
      String nameKo,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin,
      List<String> cls,
      int w) {}

  /** 부패모의 두카트 옵션(C391) — 해제 옵션 모양 + 가중치. */
  public record HazardMod(
      String g,
      int l,
      List<Integer> grp,
      List<String> t,
      String name,
      String nameKo,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin,
      int w) {}

  /** 섬뜩한 고정 속성 — g = exarch(작열의 총주교)|eater(세계 포식자), tier = 화폐 등급과 같은 1~6. */
  public record EldritchMod(
      String g,
      int l,
      List<Integer> grp,
      List<String> t,
      String name,
      String nameKo,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin,
      int tier) {}

  /** 해제 옵션 — g = prefix|suffix, l = 아이템 레벨 요구, grp = 옵션 그룹, 문구 영 · 한(최대/최소 굴림). */
  public record VeiledMod(
      String g,
      int l,
      List<Integer> grp,
      List<String> t,
      String name,
      String nameKo,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin) {}

  /** 에센스 한 줄(essences.json) — modId · groups · modLevel · slug 는 10-15 C335 에 추가. */
  public record Essence(
      String family,
      String name,
      String nameKo,
      String icon,
      int tier,
      int ilvlMax,
      String gen,
      String modName,
      String modId,
      List<Integer> groups,
      Integer modLevel,
      String slug,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin,
      // 보장 옵션의 태그 id(10-21 C553, parse-essences.mjs — 자연 옵션 표에 없는 에센스 전용 옵션도 태그가 나오게). 옛 데이터면 null
      List<String> t) {

    public Essence(
        String family,
        String name,
        String nameKo,
        String icon,
        int tier,
        int ilvlMax,
        String gen,
        String modName,
        String modId,
        List<Integer> groups,
        Integer modLevel,
        String slug,
        List<String> en,
        List<String> enMin,
        List<String> ko,
        List<String> koMin) {
      this(
          family, name, nameKo, icon, tier, ilvlMax, gen, modName, modId, groups, modLevel, slug,
          en, enMin, ko, koMin, null);
    }
  }

  private record EssenceData(String patch, Map<String, List<Essence>> classes) {}

  /**
   * 화석(10-15 C338, craft-currencies.json) — weights = 옵션 태그 → 가중치 배수(0 = 막음), simple = 태그 배수만 있는 화석
   * — added · forced · corruptedEssenceChance(얽혀든 10% 포함, C400) · lucky(축성된, C399)까지 엔진이 다룬다.
   */
  public record Fossil(
      String slug,
      String name,
      String nameKo,
      String icon,
      Map<String, Double> weights,
      List<String> added,
      List<String> forced,
      int corruptedEssenceChance,
      boolean lucky,
      List<String> desc,
      List<String> descKo,
      boolean simple) {}

  /** 공명기 — kind = alchemical(일반 → 레어) | chaotic(레어 다시), sockets = 화석 칸 1~4. */
  public record Resonator(
      String slug, String name, String nameKo, String kind, int sockets, String icon) {}

  /**
   * 수확 제작 — command = reroll_with_mod | reroll_with_current_tags_affinity_multiplier | convert_mod
   * | remove_type_and_add_type_mod | reroll_with_influence_mod.
   */
  public record Harvest(
      String id,
      String command,
      String params,
      String text,
      String textKo,
      String lifeforce,
      int cost,
      int sacredCost) {}

  /** 제작 화폐 설명(10-15 C340, CurrencyItems) — action = 게임 동작 이름, directions = 인게임 사용 설명(영 · 한). */
  public record CurrencyInfo(
      String name,
      String nameKo,
      String slug,
      String action,
      String directions,
      String directionsKo,
      // 기폭제(C385) — 종류 키(attack … · vaal = 더럽혀진), 퀄리티 머리글("Attack Modifiers" / "공격 속성"), 효과 설명. 다른
      // 화폐는 null
      String catalyst,
      String qualityLabel,
      String qualityLabelKo,
      String description,
      String descriptionKo) {}

  private record CurrencyData(
      String patch,
      List<Fossil> fossils,
      List<Resonator> resonators,
      List<Harvest> harvest,
      List<CurrencyInfo> currencies) {}

  /** 작업대 옵션 한 줄(bench.json — modId · groups · tags · modLevel 은 10-15 C338). cost = 화폐 이름 · 수량. */
  public record BenchCost(String name, String nameKo, String icon, int count) {}

  public record BenchCraft(
      String gen,
      int tier,
      int reqLevel,
      String modName,
      String modId,
      List<Integer> groups,
      List<String> tags,
      Integer modLevel,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin,
      List<BenchCost> cost) {}

  private record BenchData(String patch, Map<String, List<BenchCraft>> classes) {}

  private final Path craftFile;
  private final Path essenceFile;
  private final Path currencyFile;
  private final Path benchFile;
  private volatile CurrencyData currencies =
      new CurrencyData("", List.of(), List.of(), List.of(), List.of());
  private volatile Map<String, List<BenchCraft>> bench = Map.of();
  private volatile CraftData data =
      new CraftData(
          "", Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
          Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
  private volatile Map<String, List<Essence>> essences = Map.of();
  // 에센스 보장 옵션 id → 태그 id(C553)
  private volatile Map<String, List<String>> essenceTags = Map.of();
  // 풀 → 옵션 id → 그 풀 안 같은 계열에서의 등급(1 = 아이템 레벨 요구가 가장 높은 것)
  private volatile Map<String, Map<String, Integer>> tierRank = Map.of();

  public PoeCraftDataService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.craftFile = Path.of(dataDir, "craft-mods.json");
    this.essenceFile = Path.of(dataDir, "essences.json");
    this.currencyFile = Path.of(dataDir, "craft-currencies.json");
    this.benchFile = Path.of(dataDir, "bench.json");
    reload();
  }

  public synchronized void reload() {
    JsonMapper json = JsonMapper.builder().build();
    CraftData loaded =
        new CraftData(
            "", Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
            Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    if (Files.exists(craftFile)) {
      try (InputStream in = Files.newInputStream(craftFile)) {
        loaded = json.readValue(in, CraftData.class);
        logger.info(
            "PoE 제작 데이터 로드: {} (옵션 {}개, 풀 {}개)",
            craftFile,
            loaded.mods().size(),
            loaded.pools().size());
      } catch (Exception e) {
        logger.warn("PoE 제작 데이터 로드 실패: {}", craftFile, e);
      }
    } else {
      logger.warn("PoE 제작 데이터 없음: {} — parse-mods-full.mjs 실행 필요", craftFile);
    }
    Map<String, List<Essence>> ess = Map.of();
    if (Files.exists(essenceFile)) {
      try (InputStream in = Files.newInputStream(essenceFile)) {
        EssenceData e = json.readValue(in, EssenceData.class);
        ess = e.classes() == null ? Map.of() : e.classes();
      } catch (Exception e) {
        logger.warn("PoE 에센스 로드 실패(제작): {}", essenceFile, e);
      }
    }
    this.data = loaded;
    this.essences = ess;
    Map<String, List<String>> et = new java.util.HashMap<>();
    ess.values()
        .forEach(
            l ->
                l.forEach(
                    e -> {
                      if (e.modId() != null && e.t() != null && !e.t().isEmpty()) {
                        et.putIfAbsent(e.modId(), e.t());
                      }
                    }));
    this.essenceTags = et;
    benchIndex.clear();
    if (Files.exists(currencyFile)) {
      try (InputStream in = Files.newInputStream(currencyFile)) {
        CurrencyData c = json.readValue(in, CurrencyData.class);
        this.currencies =
            new CurrencyData(
                c.patch(),
                c.fossils() == null ? List.of() : c.fossils(),
                c.resonators() == null ? List.of() : c.resonators(),
                c.harvest() == null ? List.of() : c.harvest(),
                c.currencies() == null ? List.of() : c.currencies());
      } catch (Exception e) {
        logger.warn("PoE 제작 화폐 로드 실패: {}", currencyFile, e);
      }
    }
    if (Files.exists(benchFile)) {
      try (InputStream in = Files.newInputStream(benchFile)) {
        BenchData b = json.readValue(in, BenchData.class);
        this.bench = b.classes() == null ? Map.of() : b.classes();
      } catch (Exception e) {
        logger.warn("PoE 작업대 로드 실패(제작): {}", benchFile, e);
      }
    }
    this.tierRank = ranks(loaded);
  }

  private static Map<String, Map<String, Integer>> ranks(CraftData d) {
    Map<String, Map<String, Integer>> out = new HashMap<>();
    for (Map.Entry<String, Map<String, Integer>> pool : d.pools().entrySet()) {
      Map<String, List<String>> byFamily = new HashMap<>();
      for (String id : pool.getValue().keySet()) {
        CraftMod m = d.mods().get(id);
        if (m != null) {
          byFamily.computeIfAbsent(m.g() + "|" + m.f(), k -> new ArrayList<>()).add(id);
        }
      }
      Map<String, Integer> rank = new HashMap<>();
      for (List<String> ids : byFamily.values()) {
        ids.sort(Comparator.comparingInt((String id) -> d.mods().get(id).l()).reversed());
        for (int i = 0; i < ids.size(); i++) {
          rank.put(ids.get(i), i + 1);
        }
      }
      out.put(pool.getKey(), rank);
    }
    return out;
  }

  public boolean hasData() {
    return !data.mods().isEmpty();
  }

  public String patch() {
    return data.patch();
  }

  public CraftMod mod(String id) {
    return data.mods().get(id);
  }

  /** 풀(클래스|변형|영향력) → 옵션 id → 가중치. 없으면 빈 맵. 영향력 둘("a,b" — 각성자의 오브)이면 두 풀을 합친다(가중치 큰 쪽). */
  public Map<String, Integer> pool(String poolKey) {
    return lookup("pool", data.pools(), poolKey);
  }

  /** 해제 옵션 풀(C341) — 옵션 id → 가중치. 옛 파일 · 없는 풀이면 빈 맵. */
  public Map<String, Integer> veiledPool(String poolKey) {
    return lookup("veiled", data.veiled(), poolKey);
  }

  // 영향력 둘 풀 합치기(C346) — 데이터가 다시 읽히면 비운다. 같은 맵 객체를 돌려줘야 엔진 풀 색인 캐시가 산다
  private final java.util.concurrent.ConcurrentHashMap<String, Map<String, Integer>> merged =
      new java.util.concurrent.ConcurrentHashMap<>();
  private volatile CraftData mergedFor;

  private Map<String, Integer> lookup(
      String kind, Map<String, Map<String, Integer>> src, String poolKey) {
    if (src == null || poolKey == null) {
      return Map.of();
    }
    Map<String, Integer> direct = src.get(poolKey);
    if (direct != null || !poolKey.contains(",")) {
      return direct == null ? Map.of() : direct;
    }
    CraftData d = data;
    if (mergedFor != d) {
      merged.clear();
      mergedFor = d;
    }
    return merged.computeIfAbsent(
        kind + "#" + poolKey,
        k -> {
          int bar = poolKey.lastIndexOf('|');
          String head = poolKey.substring(0, bar + 1);
          Map<String, Integer> out = new java.util.LinkedHashMap<>();
          for (String inf : poolKey.substring(bar + 1).split(",")) {
            for (Map.Entry<String, Integer> e : src.getOrDefault(head + inf, Map.of()).entrySet()) {
              out.merge(e.getKey(), e.getValue(), Math::max);
            }
          }
          return java.util.Collections.unmodifiableMap(out);
        });
  }

  public VeiledMod veiledMod(String id) {
    return data.veiledMods() == null ? null : data.veiledMods().get(id);
  }

  /** 화석 추가 · 강제 옵션 풀(C342) — 옵션 id → 가중치. 옛 파일 · 없는 풀이면 빈 맵. */
  public Map<String, Integer> fossilPool(String poolKey) {
    return lookup("fossil", data.fossil(), poolKey);
  }

  /** 지배의 오브(C345) — 영향력 옵션 id → 한 등급 위 id. 옛 파일 · 없는 풀이면 빈 맵. */
  public Map<String, String> upgrade(String poolKey) {
    if (data.upgrade() == null || poolKey == null) {
      return Map.of();
    }
    if (!poolKey.contains(",")) {
      return data.upgrade().getOrDefault(poolKey, Map.of());
    }
    int bar = poolKey.lastIndexOf('|');
    Map<String, String> out = new java.util.HashMap<>();
    for (String inf : poolKey.substring(bar + 1).split(",")) {
      out.putAll(data.upgrade().getOrDefault(poolKey.substring(0, bar + 1) + inf, Map.of()));
    }
    return out;
  }

  public VeiledMod elevatedMod(String id) {
    return data.elevatedMods() == null ? null : data.elevatedMods().get(id);
  }

  /** 섬뜩한 고정 속성 풀(C343). 옛 파일 · 없는 풀이면 빈 맵. */
  public Map<String, Integer> eldritchPool(String poolKey) {
    return data.eldritch() == null ? Map.of() : data.eldritch().getOrDefault(poolKey, Map.of());
  }

  public EldritchMod eldritchMod(String id) {
    return data.eldritchMods() == null ? null : data.eldritchMods().get(id);
  }

  /** 바알 오브 부패 고정 속성 풀(C358). 옛 파일 · 없는 풀(플라스크)이면 빈 맵. */
  public Map<String, Integer> corruptedPool(String poolKey) {
    return data.corrupted() == null ? Map.of() : data.corrupted().getOrDefault(poolKey, Map.of());
  }

  /** 옵션 태그 표시 이름(C480) — 태그 id → [영어, 한국어]. */
  public Map<String, List<String>> tagNames() {
    return data.tagNames() == null ? Map.of() : data.tagNames();
  }

  /** 악명 높은 속성(C393) — id → 옵션. */
  public Map<String, InfamousMod> infamousMods() {
    return data.infamousMods() == null ? Map.of() : data.infamousMods();
  }

  /** 부패모의 두카트 옵션(C391) — id → 옵션. */
  public Map<String, HazardMod> hazardMods() {
    return data.hazardMods() == null ? Map.of() : data.hazardMods();
  }

  /** 야수 제작 위상 스킬 옵션(C388). */
  public VeiledMod beastMod(String id) {
    return data.beastMods() == null ? null : data.beastMods().get(id);
  }

  public VeiledMod corruptedMod(String id) {
    return data.corruptedMods() == null ? null : data.corruptedMods().get(id);
  }

  public VeiledMod fossilMod(String id) {
    return data.fossilMods() == null ? null : data.fossilMods().get(id);
  }

  /** 부패 에센스(섬망 · 경악 · 발작 · 광기) — 상형 문자 화석이 그중 하나를 넣는다(C342). */
  public List<Essence> corruptEssences(String itemClass) {
    return essences(itemClass).stream()
        .filter(e -> e.modId() != null && CORRUPT_ESSENCES.contains(e.family()))
        .toList();
  }

  static final java.util.Set<String> CORRUPT_ESSENCES =
      java.util.Set.of("Delirium", "Horror", "Hysteria", "Insanity");

  public boolean hasPool(String poolKey) {
    return !pool(poolKey).isEmpty();
  }

  /** 그 풀 안 같은 계열에서의 등급(1 = 최상위). 모르면 null. */
  public Integer tierRank(String poolKey, String modId) {
    return tierRank.getOrDefault(poolKey, Map.of()).get(modId);
  }

  public List<Essence> essences(String itemClass) {
    return essences.getOrDefault(itemClass, List.of());
  }

  /** 에센스 보장 옵션의 태그 id(C553). 에센스 옵션이 아니거나 태그가 없으면 null. */
  public List<String> essenceModTags(String modId) {
    return modId == null ? null : essenceTags.get(modId);
  }

  /** 클래스 · 에센스 슬러그 → 에센스(그 클래스에 보장 옵션이 있는 것만). 없으면 null. */
  public List<CurrencyInfo> currencyInfos() {
    return currencies.currencies();
  }

  public List<Fossil> fossils() {
    return currencies.fossils();
  }

  public Fossil fossil(String slug) {
    return slug == null
        ? null
        : currencies.fossils().stream().filter(f -> slug.equals(f.slug())).findFirst().orElse(null);
  }

  public List<Resonator> resonators() {
    return currencies.resonators();
  }

  public List<Harvest> harvests() {
    return currencies.harvest();
  }

  public Harvest harvest(String id) {
    return id == null
        ? null
        : currencies.harvest().stream().filter(h -> id.equals(h.id())).findFirst().orElse(null);
  }

  /** 클래스의 작업대 옵션(옵션 id 가 있는 것만). */
  public List<BenchCraft> bench(String itemClass) {
    return bench.getOrDefault(itemClass, List.of()).stream()
        .filter(b -> b.modId() != null)
        .toList();
  }

  // (클래스, 옵션 id) → 작업대 옵션 — 전략 모의가 화폐마다 찾아 목록 훑기가 많았다(C339)
  private final java.util.concurrent.ConcurrentHashMap<String, java.util.Optional<BenchCraft>>
      benchIndex = new java.util.concurrent.ConcurrentHashMap<>();

  public BenchCraft bench(String itemClass, String modId) {
    if (modId == null) {
      return null;
    }
    return benchIndex
        .computeIfAbsent(
            itemClass + "|" + modId,
            k -> bench(itemClass).stream().filter(b -> modId.equals(b.modId())).findFirst())
        .orElse(null);
  }

  public Essence essence(String itemClass, String slug) {
    if (slug == null) {
      return null;
    }
    return essences(itemClass).stream().filter(e -> slug.equals(e.slug())).findFirst().orElse(null);
  }
}
