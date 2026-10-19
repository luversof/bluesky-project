package net.luversof.api.poe.poe2;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * PoE2 제작 에뮬레이터(10-15 C351, P5) — craft2-mods.json(옵션 그룹 · 태그 · 레벨 · 풀 · 징조) + mods.json
 * 문구(Poe2DataService). 가중치는 균등(GGG 비공개).
 */
@Service
public class Poe2CraftService {

  private static final Logger logger = LoggerFactory.getLogger(Poe2CraftService.class);

  record CraftMod(String g, int l, List<Integer> grp, List<String> t, String mt) {}

  public record Omen(String key, String name, String nameKo, String slug) {}

  /** 제작 화폐 — action = 엔진 동작, tier = "" · greater · perfect, 공식 이름 영 · 한, 아이콘 슬러그. */
  public record Currency(
      String action,
      String tier,
      String name,
      String nameKo,
      String slug,
      // 기폭제(C386) — 퀄리티 머리글 · 효과 설명(poe2db 와 같은 틀), 다른 화폐는 null
      String qualityLabel,
      String qualityLabelKo,
      String description,
      String descriptionKo) {}

  /** 에센스(C353) — kind = normal | perfect | corrupted, mods = 아이템 종류 → 보장 옵션 id. */
  public record EssenceInfo(
      String slug, String name, String nameKo, int tier, String kind, Map<String, String> mods) {}

  /** 에센스 전용 옵션(완벽 · 타락) — 풀엔 없다. */
  record EssenceMod(
      String g,
      int l,
      List<Integer> grp,
      List<String> t,
      String mt,
      String name,
      String nameKo,
      List<String> en,
      List<String> ko) {}

  /** 훼손(탈신성) 옵션(C354) — lich = ulaman | amanamu | kurgal | null. */
  record DesecratedModData(
      String g,
      int l,
      List<Integer> grp,
      List<String> t,
      String name,
      String nameKo,
      String lich,
      List<String> en,
      List<String> ko) {}

  /** 뼈(C354) — classes = 쓸 수 있는 종류, maxIlvl(0 = 없음), minModLevel. */
  public record BoneInfo(
      String slug,
      String name,
      String nameKo,
      List<String> classes,
      int maxIlvl,
      int minModLevel,
      boolean breach) {}

  record CraftData(
      String patch,
      Map<String, CraftMod> mods,
      Map<String, List<String>> pools,
      List<Omen> omens,
      List<Currency> currencies,
      List<EssenceInfo> essences,
      Map<String, EssenceMod> essenceMods,
      Map<String, List<String>> desecrated,
      Map<String, DesecratedModData> desecratedMods,
      List<BoneInfo> bones,
      // 바알 오브 타락 인챈트(C376)
      Map<String, List<String>> corrupted,
      Map<String, CorruptedModData> corruptedMods) {}

  /** 타락 인챈트(C376) — g = corrupted. */
  record CorruptedModData(
      String g, int l, List<Integer> grp, List<String> t, List<String> en, List<String> ko) {}

  /** 표시 줄 — tierRank = 그 풀 같은 계열에서 1 이 최상위. */
  public record Line(
      String id,
      String gen,
      String name,
      String nameKo,
      List<String> en,
      List<String> ko,
      Integer tierRank,
      int level,
      boolean fractured) {}

  public record View(
      Poe2CraftEngine.Item item,
      String error,
      List<String> added,
      List<String> removed,
      List<String> omensUsed,
      List<Line> lines,
      int maxPerGen,
      // 지금 쓸 수 없는 화폐 → 이유(엔진 시험 적용)
      Map<String, String> blocked,
      // 드러낼 셋(C354) — 훼손된(미공개) 옵션이 있을 때만
      List<Line> revealOffers,
      // 결과 설명(C376 — 바알 오브 "vaal:…", C386 기폭제 "catalyst:종류:양:개수"), 없으면 null
      String note,
      // 기폭제 퀄리티로 수치가 오르는 옵션 id(C386)
      List<String> boosted) {}

  /** 다음 옵션(균등) — chance = 1/개수(가중치 비공개). */
  public record PoolLine(
      String id,
      String gen,
      String name,
      String nameKo,
      List<String> en,
      List<String> ko,
      Integer tierRank,
      int level,
      double chance,
      // 계열(ModType) — 시뮬레이터 목표를 계열마다 최소 등급으로 고를 때
      String family) {}

  public record Options(
      List<Poe2.ModPoolSummary> pools,
      List<Omen> omens,
      List<Currency> currencies,
      List<EssenceInfo> essences,
      List<BoneInfo> bones,
      String patch) {}

  private final Poe2DataService data2;
  private final Path file;
  private volatile CraftData data =
      new CraftData(
          "", Map.of(), Map.of(), List.of(), List.of(), List.of(), Map.of(), Map.of(), Map.of(),
          List.of(), Map.of(), Map.of());
  private final Map<String, List<Poe2CraftEngine.Mod>> poolCache = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Object[]>> textCache = new ConcurrentHashMap<>();
  private final Poe2CraftEngine engine;

  public Poe2CraftService(
      Poe2DataService data2,
      @Value("${poe2.data-dir:${user.home}/.poe-gamedata/poe2}") String dataDir) {
    this.data2 = data2;
    this.file = Path.of(dataDir, "craft2-mods.json");
    reload();
    this.engine =
        new Poe2CraftEngine(
            new Poe2CraftEngine.Data() {
              @Override
              public List<Poe2CraftEngine.Mod> pool(String poolKey) {
                return poolMods(poolKey);
              }

              @Override
              public Poe2CraftEngine.Mod mod(String id) {
                return modOf(id);
              }

              @Override
              public Poe2CraftEngine.Essence essence(String slug) {
                EssenceInfo e =
                    data.essences() == null
                        ? null
                        : data.essences().stream()
                            .filter(x -> x.slug().equals(slug))
                            .findFirst()
                            .orElse(null);
                return e == null ? null : new Poe2CraftEngine.Essence(e.slug(), e.kind(), e.mods());
              }

              @Override
              public Poe2CraftEngine.Bone bone(String slug) {
                BoneInfo b =
                    data.bones() == null
                        ? null
                        : data.bones().stream()
                            .filter(x -> x.slug().equals(slug))
                            .findFirst()
                            .orElse(null);
                return b == null
                    ? null
                    : new Poe2CraftEngine.Bone(b.slug(), b.classes(), b.maxIlvl(), b.minModLevel());
              }

              @Override
              public List<Poe2CraftEngine.DesecratedMod> desecrated(String poolKey) {
                return desecratedOf(poolKey);
              }

              @Override
              public List<Poe2CraftEngine.Mod> corruptedPool(String poolKey) {
                List<Poe2CraftEngine.Mod> out = new ArrayList<>();
                for (String id :
                    data.corrupted() == null
                        ? List.<String>of()
                        : data.corrupted().getOrDefault(poolKey, List.of())) {
                  Poe2CraftEngine.Mod m = modOf(id);
                  if (m != null) {
                    out.add(m);
                  }
                }
                return out;
              }
            });
  }

  public final void reload() {
    if (!Files.exists(file)) {
      logger.warn("PoE2 제작 데이터 없음: {} — parse-craft2.mjs 실행 필요", file);
      return;
    }
    try (InputStream in = Files.newInputStream(file)) {
      CraftData d = withAbyssEssence(JsonMapper.builder().build().readValue(in, CraftData.class));
      this.data = d;
      poolCache.clear();
      textCache.clear();
      desecratedCache.clear();
      logger.info("PoE2 제작 데이터 로드: {} (옵션 {}, 풀 {})", file, d.mods().size(), d.pools().size());
    } catch (Exception e) {
      logger.warn("PoE2 제작 데이터 로드 실패: {}", file, e);
    }
  }

  /** 심연의 에센스(C416) — 게임 데이터 EssenceMods 에 옵션이 없어(징표) 추출본엔 빠진다: 부위 27종(poe2db)에 징표 하나로 붙인다. */
  static final String ABYSS_ESSENCE = "CurrencyCorruptedEssenceAbyss";

  static final List<String> ABYSS_CLASSES =
      List.of(
          "One Hand Sword",
          "One Hand Axe",
          "One Hand Mace",
          "Two Hand Sword",
          "Two Hand Axe",
          "Two Hand Mace",
          "Spear",
          "Bow",
          "Warstaff",
          "Dagger",
          "Helmet",
          "Body Armour",
          "Gloves",
          "Boots",
          "Shield",
          "Staff",
          "Wand",
          "Sceptre",
          "Ring",
          "Amulet",
          "Belt",
          "Quiver",
          "Crossbow",
          "Flail",
          "Talisman",
          "Focus",
          "Buckler");

  private static CraftData withAbyssEssence(CraftData d) {
    if (d.essences() == null
        || d.essences().stream().anyMatch(e -> ABYSS_ESSENCE.equals(e.slug()))) {
      return d;
    }
    Map<String, String> mods = new LinkedHashMap<>();
    ABYSS_CLASSES.forEach(c -> mods.put(c, Poe2CraftEngine.ABYSS_MARK));
    List<EssenceInfo> ess = new ArrayList<>(d.essences());
    ess.add(
        new EssenceInfo(ABYSS_ESSENCE, "Essence of the Abyss", "심연의 에센스", 1, "corrupted", mods));
    return new CraftData(
        d.patch(),
        d.mods(),
        d.pools(),
        d.omens(),
        d.currencies(),
        List.copyOf(ess),
        d.essenceMods(),
        d.desecrated(),
        d.desecratedMods(),
        d.bones(),
        d.corrupted(),
        d.corruptedMods());
  }

  private final Map<String, List<Poe2CraftEngine.DesecratedMod>> desecratedCache =
      new ConcurrentHashMap<>();

  /** 시뮬레이터 목표용 훼손된 속성(C420) — 등급 하나, family = 군주(ulaman · amanamu · kurgal, 없으면 null). */
  public List<PoolLine> desecratedLines(String poolKey) {
    List<PoolLine> out = new ArrayList<>();
    for (Poe2CraftEngine.DesecratedMod d : desecratedOf(poolKey)) {
      DesecratedModData dd = data.desecratedMods().get(d.mod().id());
      out.add(
          new PoolLine(
              d.mod().id(),
              d.mod().gen(),
              dd == null ? d.mod().id() : dd.name(),
              dd == null ? d.mod().id() : dd.nameKo(),
              dd == null || dd.en() == null ? List.of() : dd.en(),
              dd == null || dd.ko() == null ? List.of() : dd.ko(),
              1,
              d.mod().level(),
              0,
              d.lich()));
    }
    return out;
  }

  List<Poe2CraftEngine.DesecratedMod> desecratedOf(String poolKey) {
    return desecratedCache.computeIfAbsent(
        poolKey == null ? "" : poolKey,
        k -> {
          List<Poe2CraftEngine.DesecratedMod> out = new ArrayList<>();
          for (String id :
              data.desecrated() == null
                  ? List.<String>of()
                  : data.desecrated().getOrDefault(k, List.of())) {
            DesecratedModData d = data.desecratedMods().get(id);
            if (d != null) {
              out.add(
                  new Poe2CraftEngine.DesecratedMod(
                      new Poe2CraftEngine.Mod(
                          id,
                          d.g(),
                          d.l(),
                          d.grp() == null ? List.of() : d.grp(),
                          d.t() == null ? List.of() : d.t()),
                      d.lich()));
            }
          }
          return List.copyOf(out);
        });
  }

  /** 옵션 — 풀 옵션, 없으면 에센스 전용 옵션. */
  Poe2CraftEngine.Mod modOf(String id) {
    CraftMod m = data.mods().get(id);
    if (m != null) {
      return toMod(id, m);
    }
    CorruptedModData cm = data.corruptedMods() == null ? null : data.corruptedMods().get(id);
    if (cm != null) {
      return new Poe2CraftEngine.Mod(
          id,
          "corrupted",
          cm.l(),
          cm.grp() == null ? List.of() : cm.grp(),
          cm.t() == null ? List.of() : cm.t());
    }
    DesecratedModData dd = data.desecratedMods() == null ? null : data.desecratedMods().get(id);
    if (dd != null) {
      return new Poe2CraftEngine.Mod(
          id,
          dd.g(),
          dd.l(),
          dd.grp() == null ? List.of() : dd.grp(),
          dd.t() == null ? List.of() : dd.t());
    }
    EssenceMod e = data.essenceMods() == null ? null : data.essenceMods().get(id);
    return e == null
        ? null
        : new Poe2CraftEngine.Mod(
            id,
            e.g(),
            e.l(),
            e.grp() == null ? List.of() : e.grp(),
            e.t() == null ? List.of() : e.t());
  }

  private static Poe2CraftEngine.Mod toMod(String id, CraftMod m) {
    return new Poe2CraftEngine.Mod(
        id,
        m.g(),
        m.l(),
        m.grp() == null ? List.of() : m.grp(),
        m.t() == null ? List.of() : m.t(),
        m.mt());
  }

  List<Poe2CraftEngine.Mod> poolMods(String poolKey) {
    return poolCache.computeIfAbsent(
        poolKey == null ? "" : poolKey,
        k -> {
          List<Poe2CraftEngine.Mod> out = new ArrayList<>();
          for (String id : data.pools().getOrDefault(k, List.of())) {
            CraftMod m = data.mods().get(id);
            if (m != null) {
              out.add(toMod(id, m));
            }
          }
          return List.copyOf(out);
        });
  }

  /** 풀의 옵션 문구 · 등급 — id → {ModTier, tierRank}. */
  private Map<String, Object[]> texts(String poolKey) {
    return textCache.computeIfAbsent(
        poolKey == null ? "" : poolKey,
        k -> {
          Map<String, Object[]> out = new HashMap<>();
          data2
              .modPool(k)
              .ifPresent(
                  p -> {
                    for (Poe2.ModGroup g :
                        p.groups() == null ? List.<Poe2.ModGroup>of() : p.groups()) {
                      for (int i = 0; i < g.tiers().size(); i++) {
                        out.put(g.tiers().get(i).id(), new Object[] {g.tiers().get(i), i + 1});
                      }
                    }
                  });
          return out;
        });
  }

  public Poe2CraftEngine engine() {
    return engine;
  }

  public Options options() {
    return new Options(
        data2.modPools(null),
        data.omens() == null ? List.of() : data.omens(),
        data.currencies() == null ? List.of() : data.currencies(),
        data.essences() == null ? List.of() : data.essences(),
        data.bones() == null ? List.of() : data.bones(),
        data.patch());
  }

  /** 클라이언트가 보낸 옵션을 데이터에서 다시 채운다(모르는 옵션은 뺀다). */
  public Poe2CraftEngine.Item normalize(Poe2CraftEngine.Item item) {
    List<Poe2CraftEngine.Affix> out = new ArrayList<>();
    for (Poe2CraftEngine.Affix a :
        item.affixes() == null ? List.<Poe2CraftEngine.Affix>of() : item.affixes()) {
      if (Poe2CraftEngine.unrevealed(a)) {
        out.add(
            new Poe2CraftEngine.Affix(
                a.id(),
                a.id().startsWith(Poe2CraftEngine.UNREVEALED + "P") ? "prefix" : "suffix",
                a.level() >= 40 ? 40 : 0,
                List.of(-3),
                false));
        continue;
      }
      if (Poe2CraftEngine.abyssMark(a)) {
        out.add(
            new Poe2CraftEngine.Affix(
                a.id(), a.id().endsWith("P") ? "prefix" : "suffix", 0, List.of(-4), false));
        continue;
      }
      Poe2CraftEngine.Mod m = modOf(a.id());
      if (m != null) {
        out.add(new Poe2CraftEngine.Affix(a.id(), m.gen(), m.level(), m.groups(), a.fractured()));
      }
    }
    return new Poe2CraftEngine.Item(
        item.poolKey(),
        Math.max(1, Math.min(100, item.ilvl())),
        item.rarity() == null ? Poe2CraftEngine.Rarity.NORMAL : item.rarity(),
        out,
        item.corrupted(),
        quality(item),
        item.sanctified());
  }

  /** 기폭제 퀄리티(C386) — 이 아이템에 맞는 종류 · 1~20% 만 남긴다. */
  private static String quality(Poe2CraftEngine.Item item) {
    String k = item.qualityKey();
    int q = item.qualityAmount();
    return Poe2CraftEngine.catalystFits(item.poolKey(), k)
            && q >= 1
            && q <= Poe2CraftEngine.MAX_QUALITY
        ? k + ":" + q
        : null;
  }

  public View apply(
      Poe2CraftEngine.Item item,
      Poe2CraftEngine.Action action,
      String arg,
      Set<String> omens,
      Long seed) {
    Poe2CraftEngine.Item it = normalize(item);
    RandomGenerator rng =
        seed == null
            ? RandomGenerator.of("L64X128MixRandom")
            : new java.util.SplittableRandom(seed);
    Poe2CraftEngine.Result r = engine.apply(it, action, arg, omens, rng);
    return view(r.item(), r.error(), r.added(), r.removed(), r.omensUsed(), r.note());
  }

  /** 재조합기(C407) — 왼쪽 = 지금 아이템, 오른쪽 = 보관한 재료. */
  public View recombine(
      Poe2CraftEngine.Item left,
      Poe2CraftEngine.Item right,
      Set<String> leftIds,
      Set<String> rightIds,
      int chance,
      boolean lucky,
      Long seed) {
    Poe2CraftEngine.Item l = normalize(left);
    Poe2CraftEngine.Item r = right == null ? null : normalize(right);
    RandomGenerator rng =
        seed == null
            ? RandomGenerator.of("L64X128MixRandom")
            : new java.util.SplittableRandom(seed);
    Poe2CraftEngine.Result res = engine.recombine(l, r, leftIds, rightIds, chance, lucky, rng);
    return view(res.item(), res.error(), res.added(), res.removed(), res.omensUsed(), res.note());
  }

  public View view(Poe2CraftEngine.Item item) {
    Poe2CraftEngine.Item it = normalize(item);
    return view(it, null, List.of(), List.of(), List.of(), null);
  }

  /** 아이템마다 옵션 줄만(C453 — 에뮬레이터 단계 기록의 붙은 · 지운 옵션 문구, 차단 · 드러낼 셋은 계산하지 않는다). */
  public List<List<Line>> linesOf(List<Poe2CraftEngine.Item> items) {
    List<List<Line>> out = new ArrayList<>();
    for (Poe2CraftEngine.Item item : items == null ? List.<Poe2CraftEngine.Item>of() : items) {
      if (item == null || item.poolKey() == null) {
        out.add(List.of());
        continue;
      }
      Poe2CraftEngine.Item it = normalize(item);
      List<Line> ls = new ArrayList<>();
      for (Poe2CraftEngine.Affix a : it.affixes()) {
        ls.add(lineOf(it.poolKey(), a));
      }
      out.add(ls);
    }
    return out;
  }

  private View view(
      Poe2CraftEngine.Item item,
      String error,
      List<String> added,
      List<String> removed,
      List<String> omensUsed,
      String note) {
    List<Line> lines = new ArrayList<>();
    for (Poe2CraftEngine.Affix a : item.affixes()) {
      lines.add(lineOf(item.poolKey(), a));
    }
    // 타락 인챈트(C376)가 맨 위, 그다음 접두 · 접미(인게임 툴팁 순서)
    lines.sort(
        java.util.Comparator.comparing(
            (Line l) -> "corrupted".equals(l.gen()) ? 0 : "prefix".equals(l.gen()) ? 1 : 2));
    Poe2CraftEngine.Rarity r = item.rarity();
    return new View(
        item,
        error,
        added,
        removed,
        omensUsed,
        lines,
        Poe2CraftEngine.maxPerGen(
            item, r == Poe2CraftEngine.Rarity.NORMAL ? Poe2CraftEngine.Rarity.MAGIC : r),
        blocked(item),
        engine.revealOffers(item).stream()
            .map(
                id -> {
                  Poe2CraftEngine.Mod m = modOf(id);
                  return lineOf(
                      item.poolKey(),
                      new Poe2CraftEngine.Affix(id, m.gen(), m.level(), m.groups(), false));
                })
            .toList(),
        note,
        engine.boosted(item));
  }

  /** 줄 하나 — 풀 등급 문구, 없으면 에센스 전용 · 훼손 옵션 문구, 미공개 자리는 인게임 "훼손된 접두어/접미어". */
  private Line lineOf(String poolKey, Poe2CraftEngine.Affix a) {
    if (Poe2CraftEngine.abyssMark(a)) {
      // 심연의 군주의 징표(C416) — 인게임 이름(KeywordPopups MarkofAbyssalLord)
      return new Line(
          a.id(),
          a.gen(),
          "Mark of the Abyssal Lord",
          "심연의 군주의 징표",
          List.of("Mark of the Abyssal Lord"),
          List.of("심연의 군주의 징표"),
          null,
          0,
          false);
    }
    if (Poe2CraftEngine.unrevealed(a)) {
      boolean pre = "prefix".equals(a.gen());
      // 인게임 문구(ClientStrings ItemDisplayVeiledPrefix/Suffix)
      return new Line(
          a.id(),
          a.gen(),
          pre ? "Desecrated Prefix" : "Desecrated Suffix",
          pre ? "훼손된 접두어" : "훼손된 접미어",
          List.of(pre ? "Desecrated Prefix" : "Desecrated Suffix"),
          List.of(pre ? "훼손된 접두어" : "훼손된 접미어"),
          null,
          a.level(),
          false);
    }
    CorruptedModData cmd = data.corruptedMods() == null ? null : data.corruptedMods().get(a.id());
    if (cmd != null) {
      // 타락 인챈트(C376) — 이름 = 인게임 "타락"
      return new Line(
          a.id(), "corrupted", "Corrupted", "타락", cmd.en(), cmd.ko(), null, a.level(), false);
    }
    Object[] t = texts(poolKey).get(a.id());
    Poe2.ModTier tier = t == null ? null : (Poe2.ModTier) t[0];
    EssenceMod em =
        tier != null || data.essenceMods() == null ? null : data.essenceMods().get(a.id());
    DesecratedModData dd =
        tier != null || em != null || data.desecratedMods() == null
            ? null
            : data.desecratedMods().get(a.id());
    return new Line(
        a.id(),
        a.gen(),
        tier != null ? tier.name() : em != null ? em.name() : dd != null ? dd.name() : a.id(),
        tier != null ? tier.nameKo() : em != null ? em.nameKo() : dd != null ? dd.nameKo() : a.id(),
        tier != null ? tier.text() : em != null ? em.en() : dd != null ? dd.en() : List.of(),
        tier != null ? tier.textKo() : em != null ? em.ko() : dd != null ? dd.ko() : List.of(),
        t == null ? null : (Integer) t[1],
        a.level(),
        a.fractured());
  }

  /** 징조 없이 · 기본 등급으로 시험 적용해 못 쓰는 이유. */
  Map<String, String> blocked(Poe2CraftEngine.Item item) {
    return blockedFor(engine, item);
  }

  /** 등급이 있는 화폐(진화 · 확장 · 제왕 · 엑잘 · 카오스). */
  static final List<Poe2CraftEngine.Action> TIERED =
      List.of(
          Poe2CraftEngine.Action.TRANSMUTE,
          Poe2CraftEngine.Action.AUGMENTATION,
          Poe2CraftEngine.Action.REGAL,
          Poe2CraftEngine.Action.EXALT,
          Poe2CraftEngine.Action.CHAOS);

  /**
   * 못 쓰는 이유 — 기본 등급은 'ACTION', 상위 · 완벽은 'ACTION:greater|perfect'(기본이 막히지 않았는데 그 등급만 막힐 때 — 아이템 레벨이
   * 최소 옵션 레벨보다 낮으면, C435).
   */
  static Map<String, String> blockedFor(Poe2CraftEngine engine, Poe2CraftEngine.Item item) {
    Map<String, String> out = new LinkedHashMap<>();
    RandomGenerator rng = new java.util.SplittableRandom(0);
    for (Poe2CraftEngine.Action a : Poe2CraftEngine.Action.values()) {
      if (a == Poe2CraftEngine.Action.ESSENCE
          || a == Poe2CraftEngine.Action.DESECRATE
          || a == Poe2CraftEngine.Action.REVEAL
          || a == Poe2CraftEngine.Action.REVEAL_REROLL
          || a == Poe2CraftEngine.Action.MANUAL_ADD
          || a == Poe2CraftEngine.Action.MANUAL_REMOVE) {
        continue; // 에센스는 고른 것마다 — 화면이 종류 · 희귀도로 거른다
      }
      String e = engine.apply(item, a, null, Set.of(), rng).error();
      if (e != null) {
        out.put(a.name(), e);
      }
    }
    for (Poe2CraftEngine.Action a : TIERED) {
      if (out.containsKey(a.name())) {
        continue;
      }
      // 기본이 되면 등급만으로 막히는 건 아이템 레벨 < 최소 옵션 레벨뿐(C436 검토 — 유형 예외가 후보를 남긴다). apply 로 시험하면 카오스가
      // 지우는 옵션에 따라 무작위로 달라졌다
      for (String t : List.of("greater", "perfect")) {
        int min = Poe2CraftEngine.minLevel(a, t);
        if (min > 0 && item.ilvl() < min) {
          // 인게임 오류(ItemErrorItemLevelTooLow) — 엔진 apply 와 같은 문구
          out.put(a.name() + ":" + t, "아이템 레벨이 너무 낮습니다");
        }
      }
    }
    return out;
  }

  /** 이 아이템 종류에 쓸 수 있는 에센스 — 보장 옵션 문구 · 접두/접미와 함께(C353). */
  public record EssenceOption(
      String slug,
      String name,
      String nameKo,
      String kind,
      int tier,
      String modId,
      String gen,
      List<String> en,
      List<String> ko) {}

  public List<EssenceOption> essences(String poolKey) {
    String cls = poolKey == null ? "" : poolKey.split("[|]")[0];
    Map<String, Object[]> tx = texts(poolKey);
    List<EssenceOption> out = new ArrayList<>();
    for (EssenceInfo e : data.essences() == null ? List.<EssenceInfo>of() : data.essences()) {
      String id = e.mods() == null ? null : e.mods().get(cls);
      if (Poe2CraftEngine.ABYSS_MARK.equals(id)) {
        // 심연의 에센스(C416) — 보장 '옵션'은 징표(인게임 KeywordPopups 이름), 쪽은 지운 옵션 쪽
        out.add(
            new EssenceOption(
                e.slug(),
                e.name(),
                e.nameKo(),
                e.kind(),
                e.tier(),
                id,
                "either",
                List.of("Mark of the Abyssal Lord"),
                List.of("심연의 군주의 징표")));
        continue;
      }
      Poe2CraftEngine.Mod m = id == null ? null : modOf(id);
      if (m == null) {
        continue;
      }
      Object[] t = tx.get(id);
      Poe2.ModTier tier = t == null ? null : (Poe2.ModTier) t[0];
      EssenceMod em = data.essenceMods() == null ? null : data.essenceMods().get(id);
      out.add(
          new EssenceOption(
              e.slug(),
              e.name(),
              e.nameKo(),
              e.kind(),
              e.tier(),
              id,
              m.gen(),
              tier != null ? tier.text() : em != null ? em.en() : List.of(),
              tier != null ? tier.textKo() : em != null ? em.ko() : List.of()));
    }
    return out;
  }

  /** 다음 옵션(균등) — 지금 희귀도 자리 기준(일반이면 매직). */
  public List<PoolLine> pool(Poe2CraftEngine.Item item) {
    return pool(item, null);
  }

  /**
   * 다음 옵션(C396) — omens 에 엑잘 징조가 있으면 표에 반영: 좌측 찬미 = 접두만 · 우측 찬미 = 접미만(둘 다면 무시 — 엔진과 같다), 촉진하는 찬미 +
   * 맞는 기폭제 퀄리티 = 그 태그 옵션 가중치 × (1 + 0.2q). 나머지는 균등(가중치 비공개).
   */
  public List<PoolLine> pool(Poe2CraftEngine.Item item, List<String> omens) {
    return pool(item, omens, null);
  }

  /** 다음 옵션 — tier = 고른 화폐 등급(C441, greater · perfect, 없으면 기본). */
  public List<PoolLine> pool(Poe2CraftEngine.Item item, List<String> omens, String currencyTier) {
    Poe2CraftEngine.Item it = normalize(item);
    Map<String, Object[]> tx = texts(it.poolKey());
    // 줄은 다 준다 — 등급(C441)에 걸린 줄은 확률 0(C445 검토: 숨기면 '직접 편집' 넣기도 못 했다)
    List<Poe2CraftEngine.Mod> next = new ArrayList<>(engine.nextPool(it));
    java.util.Set<String> live =
        currencyTier == null || currencyTier.isBlank()
            ? null
            : engine.nextPool(it, currencyTier).stream()
                .map(Poe2CraftEngine.Mod::id)
                .collect(java.util.stream.Collectors.toSet());
    java.util.Set<String> om = omens == null ? java.util.Set.of() : new java.util.HashSet<>(omens);
    boolean sin = om.contains("SinistralExaltation");
    boolean dex = om.contains("DextralExaltation");
    if (sin != dex && it.rarity() == Poe2CraftEngine.Rarity.RARE) {
      String side = sin ? "prefix" : "suffix";
      next.removeIf(m -> !side.equals(m.gen()));
    }
    List<String> favour =
        om.contains("CatalysingExaltation")
                && it.rarity() == Poe2CraftEngine.Rarity.RARE
                && it.qualityAmount() > 0
                && Poe2CraftEngine.catalystFits(it.poolKey(), it.qualityKey())
            ? Poe2CraftEngine.catalystTags(it.qualityKey())
            : List.of();
    double mult = favour.isEmpty() ? 1 : Poe2CraftEngine.catalysingMult(it.qualityAmount());
    double total = 0;
    double[] w = new double[next.size()];
    for (int i = 0; i < next.size(); i++) {
      List<String> t = next.get(i).tags();
      w[i] =
          live != null && !live.contains(next.get(i).id())
              ? 0
              : !favour.isEmpty() && t != null && t.stream().anyMatch(favour::contains) ? mult : 1;
      total += w[i];
    }
    List<PoolLine> out = new ArrayList<>();
    for (int i = 0; i < next.size(); i++) {
      Poe2CraftEngine.Mod m = next.get(i);
      double c = total <= 0 ? 0 : w[i] / total;
      Object[] t = tx.get(m.id());
      Poe2.ModTier tier = t == null ? null : (Poe2.ModTier) t[0];
      out.add(
          new PoolLine(
              m.id(),
              m.gen(),
              tier == null ? m.id() : tier.name(),
              tier == null ? m.id() : tier.nameKo(),
              tier == null ? List.of() : tier.text(),
              tier == null ? List.of() : tier.textKo(),
              t == null ? null : (Integer) t[1],
              m.level(),
              c,
              data.mods().get(m.id()) == null ? null : data.mods().get(m.id()).mt()));
    }
    return out;
  }
}
