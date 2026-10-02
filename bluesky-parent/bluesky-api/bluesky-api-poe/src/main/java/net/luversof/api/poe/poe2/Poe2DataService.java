package net.luversof.api.poe.poe2;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * PoE2 정적 게임 데이터 — {@code ~/.poe-gamedata/poe2/*.json}(tools/poe2-extract 산출물, git 미관리)을 부팅 시 읽는다.
 * 파일이 없으면 빈 데이터로 정상 기동하고 {@link #reload()} 로 다시 읽는다(추출 후 재시작 없이 반영).
 *
 * <p>PoE1 서비스와 따로 둔 까닭: PoE1 서비스엔 PoE1 규칙(젬 색 3종·태그 묶음·삿된·문신)이 박혀 있고 데이터 모양도 다르다. PoE1 코드는 건드리지
 * 않는다.
 */
@Service
public class Poe2DataService {

  private static final Logger logger = LoggerFactory.getLogger(Poe2DataService.class);

  private final Path dataDir;
  private final JsonMapper jsonMapper =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private volatile Poe2.GemData gems = new Poe2.GemData("", List.of());
  private volatile Poe2.BaseItemData bases = new Poe2.BaseItemData("", List.of(), List.of());
  private volatile Poe2.ModData mods = new Poe2.ModData("", List.of());
  private volatile Poe2.AugmentData augments = new Poe2.AugmentData("", List.of());
  private volatile Poe2.UniqueData uniques = new Poe2.UniqueData("", List.of());
  private volatile String loadedAt = "";

  /** 패시브 트리 색인(빌드 요약용) — 노드 id → 이름·종류, 직업·전직 한국어. 트리 JSON 은 2MB 라 처음 쓸 때 읽는다. */
  public record TreeNodeLite(String name, String nameKo, String kind, String ascendancy) {}

  /** classAscendancies = 직업 → 전직 id 목록(트리 JSON 순서 — 시뮬 전직 셀렉트의 직업별 묶음). */
  public record TreeIndex(
      java.util.Map<Integer, TreeNodeLite> nodes,
      java.util.Map<String, String> classKo,
      java.util.Map<String, String> ascendancyKo,
      java.util.Map<String, java.util.List<String>> classAscendancies,
      // PoB <Spec classId> = 트리 classes 의 integerId(워리어 6, 위치 1 — 순번이 아니다), 직업 시작 노드(트리 평가, 10-01)
      java.util.Map<String, Integer> classIntegerId,
      java.util.Map<String, Integer> classStartNode) {}

  private volatile TreeIndex treeIndex;

  /** 옵션 번역 사전(빌드 요약용) — 처음 쓸 때 만들고 reload 때 버린다. */
  private volatile Poe2ModTranslator modTranslator;

  public Poe2DataService(
      @Value("${poe2.data-dir:${user.home}/.poe-gamedata/poe2}") String dataDir) {
    this.dataDir = Path.of(dataDir);
    reload();
  }

  /** 모든 데이터 파일을 다시 읽는다. 없는 파일은 빈 데이터로 둔다. */
  public synchronized void reload() {
    gems = read("gems.json", Poe2.GemData.class, new Poe2.GemData("", List.of()));
    bases =
        read(
            "base-items.json",
            Poe2.BaseItemData.class,
            new Poe2.BaseItemData("", List.of(), List.of()));
    mods = read("mods.json", Poe2.ModData.class, new Poe2.ModData("", List.of()));
    augments = read("augments.json", Poe2.AugmentData.class, new Poe2.AugmentData("", List.of()));
    uniques = read("uniques.json", Poe2.UniqueData.class, new Poe2.UniqueData("", List.of()));
    treeIndex = null;
    modTranslator = null;
    loadedAt = OffsetDateTime.now().withNano(0).toString();
    logger.info(
        "PoE2 데이터 로드: {} (젬 {} · 베이스 {} · 옵션 풀 {} · 증강물 {} · 고유 {}, patch {})",
        dataDir,
        gems.gems().size(),
        bases.items().size(),
        mods.pools().size(),
        augments.items().size(),
        uniques.items().size(),
        patch());
  }

  private <T> T read(String file, Class<T> type, T empty) {
    Path path = dataDir.resolve(file);
    if (!Files.exists(path)) {
      logger.warn("PoE2 데이터 없음: {} — tools/poe2-extract/run-all2.mjs 실행 필요", path);
      return empty;
    }
    try (InputStream in = Files.newInputStream(path)) {
      return jsonMapper.readValue(in, type);
    } catch (Exception e) {
      logger.warn("PoE2 데이터 로드 실패: {}", path, e);
      return empty;
    }
  }

  public String patch() {
    for (String p :
        List.of(gems.patch(), bases.patch(), mods.patch(), augments.patch(), uniques.patch())) {
      if (p != null && !p.isBlank()) {
        return p;
      }
    }
    return "";
  }

  public Poe2.Meta meta() {
    return new Poe2.Meta(
        patch(),
        gems.gems().size(),
        bases.items().size(),
        mods.pools().size(),
        augments.items().size(),
        uniques.items().size(),
        Files.exists(dataDir.resolve("passive-tree.json")),
        loadedAt);
  }

  // ─────────────────────────── 검색 공용 ───────────────────────────

  private static String norm(String s) {
    return s == null ? "" : s.toLowerCase(Locale.ROOT).replace(" ", "");
  }

  /** 이름(영/한) 부분 일치 — 공백 무시. 빈 검색어면 전부. */
  private static Predicate<String[]> nameMatches(String query) {
    String q = norm(query);
    return names -> {
      if (q.isEmpty()) {
        return true;
      }
      for (String n : names) {
        if (norm(n).contains(q)) {
          return true;
        }
      }
      return false;
    };
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }

  // ─────────────────────────── 젬 ───────────────────────────

  public List<Poe2.Gem> searchGems(String query, String kind, String color, String tag) {
    Predicate<String[]> byName = nameMatches(query);
    return gems.gems().stream()
        .filter(g -> byName.test(new String[] {g.name(), g.nameKo()}))
        .filter(g -> blank(kind) || kind.equals(g.kind()))
        .filter(g -> blank(color) || color.equals(g.color()))
        .filter(g -> blank(tag) || (g.tags() != null && g.tags().contains(tag)))
        .map(Poe2.Gem::withoutLevels)
        .toList();
  }

  public Optional<Poe2.Gem> gem(String slug) {
    return gems.gems().stream().filter(g -> g.slug().equals(slug)).findFirst();
  }

  /** 젬 태그 목록(영문 키 · 한국어) — 많이 쓰인 순. */
  public List<Poe2.ModLine> gemTags() {
    java.util.Map<String, String> ko = new java.util.LinkedHashMap<>();
    java.util.Map<String, Integer> count = new java.util.HashMap<>();
    for (Poe2.Gem g : gems.gems()) {
      if (g.tags() == null) {
        continue;
      }
      for (int i = 0; i < g.tags().size(); i++) {
        String t = g.tags().get(i);
        ko.putIfAbsent(t, g.tagsKo() != null && i < g.tagsKo().size() ? g.tagsKo().get(i) : t);
        count.merge(t, 1, Integer::sum);
      }
    }
    return ko.entrySet().stream()
        .sorted(
            Comparator.comparingInt(
                (java.util.Map.Entry<String, String> e) -> -count.get(e.getKey())))
        .map(e -> new Poe2.ModLine(e.getKey(), e.getValue()))
        .toList();
  }

  /**
   * 젬 태그를 PoE1 젬 화면과 같은 묶음(유형 · 원소·피해 · 전달 · 특성 · 기타)으로 — 10-01.
   *
   * <p>예전엔 많이 쓰인 30개만 한 줄로 보여 나머지 23개(갑주 · 늑대인간 · 기원 등)는 칩이 없었다. 묶음 기준은 PoE1
   * PoeGemDataService.TAG_GROUPS 와 같은 뜻으로 PoE2 태그에 맞췄다. 데이터에 없는 태그는 빠지고, 어디에도 없는 새 태그는 "other".
   */
  public List<Poe2.TagGroup> gemTagGroups() {
    java.util.Map<String, Poe2.ModLine> byKey = new java.util.LinkedHashMap<>();
    for (Poe2.ModLine t : gemTags()) {
      byKey.put(t.en(), t);
    }
    List<Poe2.TagGroup> out = new java.util.ArrayList<>();
    for (java.util.Map.Entry<String, List<String>> g : GEM_TAG_GROUPS) {
      List<Poe2.ModLine> tags =
          g.getValue().stream().map(byKey::remove).filter(java.util.Objects::nonNull).toList();
      if (!tags.isEmpty()) {
        out.add(new Poe2.TagGroup(g.getKey(), tags));
      }
    }
    if (!byKey.isEmpty()) {
      out.add(new Poe2.TagGroup("other", List.copyOf(byKey.values())));
    }
    return out;
  }

  private static final List<java.util.Map.Entry<String, List<String>>> GEM_TAG_GROUPS =
      List.of(
          java.util.Map.entry(
              "type",
              List.of(
                  "Attack",
                  "Spell",
                  "Minion",
                  "Companion",
                  "Aura",
                  "Curse",
                  "Mark",
                  "Herald",
                  "Warcry",
                  "Banner",
                  "Totem",
                  "Invocation",
                  "Command",
                  "Meta",
                  "Persistent",
                  "Buff",
                  "Shapeshift",
                  "Bear",
                  "Werewolf",
                  "Wyvern")),
          java.util.Map.entry("damage", List.of("Physical", "Fire", "Cold", "Lightning", "Chaos")),
          java.util.Map.entry(
              "delivery",
              List.of(
                  "Projectile",
                  "Melee",
                  "AoE",
                  "Nova",
                  "Strike",
                  "Slam",
                  "Chaining",
                  "Channelling",
                  "Travel",
                  "Ammunition",
                  "Grenade",
                  "Orb",
                  "Storm",
                  "Wind",
                  "Hazard",
                  "Remnant",
                  "Plant",
                  "Detonator")),
          java.util.Map.entry(
              "trait",
              List.of(
                  "Duration",
                  "Trigger",
                  "Support",
                  "Lineage",
                  "Repeatable",
                  "Sustained",
                  "Payoff",
                  "Conditional",
                  "Staged",
                  "Merging")));

  // ─────────────────────────── 베이스 ───────────────────────────

  public List<Poe2.ItemClass> itemClasses() {
    return bases.classes();
  }

  public List<Poe2.BaseItem> searchBases(String query, String itemClass, String category) {
    Predicate<String[]> byName = nameMatches(query);
    return bases.items().stream()
        .filter(b -> byName.test(new String[] {b.name(), b.nameKo()}))
        .filter(b -> blank(itemClass) || itemClass.equals(b.itemClass()))
        .filter(b -> blank(category) || category.equals(b.category()))
        .toList();
  }

  public Optional<Poe2.BaseItem> base(String slug) {
    return bases.items().stream().filter(b -> b.slug().equals(slug)).findFirst();
  }

  public Optional<Poe2.BaseItem> baseByName(String name) {
    return bases.items().stream().filter(b -> b.name().equalsIgnoreCase(name)).findFirst();
  }

  // ─────────────────────────── 옵션 ───────────────────────────

  public List<Poe2.ModPoolSummary> modPools(String itemClass) {
    return mods.pools().stream()
        .filter(p -> blank(itemClass) || itemClass.equals(p.itemClass()))
        .map(
            p ->
                new Poe2.ModPoolSummary(
                    p.key(),
                    p.itemClass(),
                    p.itemClassKo(),
                    p.category(),
                    p.variant(),
                    p.variantKo(),
                    p.bases() == null ? 0 : p.bases().size(),
                    p.groups() == null ? 0 : p.groups().size()))
        .toList();
  }

  public Optional<Poe2.ModPool> modPool(String key) {
    return mods.pools().stream().filter(p -> p.key().equals(key)).findFirst();
  }

  /** 베이스 이름이 속한 옵션 풀. */
  public Optional<Poe2.ModPool> modPoolForBase(String baseName) {
    return mods.pools().stream()
        .filter(p -> p.bases() != null && p.bases().contains(baseName))
        .findFirst();
  }

  // ─────────────────────────── 증강물 ───────────────────────────

  public List<Poe2.Augment> searchAugments(String query, String kind, String itemClass) {
    Predicate<String[]> byName = nameMatches(query);
    return augments.items().stream()
        .filter(a -> byName.test(new String[] {a.name(), a.nameKo()}))
        .filter(a -> blank(kind) || kind.equals(a.kind()))
        .filter(
            a -> blank(itemClass) || a.effects().stream().anyMatch(e -> appliesTo(e, itemClass)))
        .toList();
  }

  /** 효과가 그 아이템 클래스에 들어가는가 — 분류에 대상 클래스가 적혀 있으면 그것으로, 없으면(모든 장비·방어구·무도 무기) 분류 이름으로. */
  static boolean appliesTo(Poe2.AugmentEffect e, String itemClass) {
    if (e.targetClasses() != null && !e.targetClasses().isEmpty()) {
      return e.targetClasses().contains(itemClass);
    }
    String t = Objects.toString(e.target(), "");
    return switch (t) {
      case "All" -> true;
      case "Armour" ->
          List.of("Helmet", "Body Armour", "Gloves", "Boots", "Shield", "Buckler", "Focus")
              .contains(itemClass);
      case "Martial Weapon" ->
          !List.of(
                  "Helmet",
                  "Body Armour",
                  "Gloves",
                  "Boots",
                  "Shield",
                  "Buckler",
                  "Focus",
                  "Wand",
                  "Staff",
                  "Sceptre",
                  "Amulet",
                  "Ring",
                  "Belt")
              .contains(itemClass);
      default -> t.equals(itemClass);
    };
  }

  public Optional<Poe2.Augment> augment(String slug) {
    return augments.items().stream().filter(a -> a.slug().equals(slug)).findFirst();
  }

  // ─────────────────────────── 고유 ───────────────────────────

  public List<Poe2.Unique> searchUniques(String query, String itemClass) {
    Predicate<String[]> byName = nameMatches(query);
    List<Poe2.Unique> out = new ArrayList<>();
    for (Poe2.Unique u : uniques.items()) {
      if (byName.test(new String[] {u.name(), u.nameKo(), u.baseType(), u.baseTypeKo()})
          && (blank(itemClass) || itemClass.equals(u.itemClass()))) {
        out.add(u);
      }
    }
    return out;
  }

  /** 이름 안에 든 가장 긴 베이스 이름(매직 아이템 "Vivid Rusted Cuirass of the Whelpling" → Rusted Cuirass). */
  public Optional<Poe2.BaseItem> baseWithinName(String name) {
    if (name == null) {
      return Optional.empty();
    }
    return bases.items().stream()
        .filter(b -> name.contains(b.name()))
        .max(Comparator.comparingInt(b -> b.name().length()));
  }

  public Optional<Poe2.Unique> uniqueByName(String name) {
    return uniques.items().stream().filter(u -> u.name().equalsIgnoreCase(name)).findFirst();
  }

  public Optional<Poe2.Gem> gemByName(String name) {
    return gems.gems().stream().filter(g -> g.name().equalsIgnoreCase(name)).findFirst();
  }

  /** 옵션 번역 사전 — 옵션 풀 티어를 먼저(검증된 풀 문구가 이긴다), 이어서 베이스 암시·고유(변형 포함)·증강물 효과. */
  @SuppressWarnings("unchecked")
  public Poe2ModTranslator modTranslator() {
    Poe2ModTranslator t = modTranslator;
    if (t != null) {
      return t;
    }
    List<List<String>[]> pairs = new ArrayList<>();
    for (Poe2.ModPool pool : mods.pools()) {
      for (Poe2.ModGroup g : pool.groups()) {
        for (Poe2.ModTier tier : g.tiers()) {
          pairs.add(new List[] {tier.text(), tier.textKo()});
        }
      }
    }
    for (Poe2.BaseItem b : bases.items()) {
      if (b.implicits() != null) {
        pairs.add(
            new List[] {
              b.implicits().stream().map(Poe2.ModLine::en).toList(),
              b.implicits().stream().map(l -> l.ko() == null ? "" : l.ko()).toList()
            });
      }
    }
    for (Poe2.Unique u : uniques.items()) {
      pairs.add(new List[] {u.implicits(), u.implicitsKo()});
      pairs.add(new List[] {u.explicits(), u.explicitsKo()});
      if (u.variants() != null) {
        for (Poe2.UniqueVariant v : u.variants()) {
          pairs.add(new List[] {v.implicits(), v.implicitsKo()});
          pairs.add(new List[] {v.explicits(), v.explicitsKo()});
        }
      }
    }
    for (Poe2.Augment a : augments.items()) {
      if (a.effects() != null) {
        for (Poe2.AugmentEffect e : a.effects()) {
          pairs.add(new List[] {e.lines(), e.linesKo()});
        }
      }
    }
    t = Poe2ModTranslator.of(pairs);
    logger.info("PoE2 옵션 번역 사전: {}개", t.size());
    modTranslator = t;
    return t;
  }

  /** 트리 색인 — 없거나 못 읽으면 빈 색인(빌드 요약은 영문 이름으로 폴백). */
  public TreeIndex treeIndex() {
    TreeIndex idx = treeIndex;
    if (idx != null) {
      return idx;
    }
    synchronized (this) {
      if (treeIndex != null) {
        return treeIndex;
      }
      java.util.Map<Integer, TreeNodeLite> nodes = new java.util.HashMap<>();
      java.util.Map<String, String> classKo = new java.util.HashMap<>();
      java.util.Map<String, String> ascKo = new java.util.HashMap<>();
      java.util.Map<String, java.util.List<String>> classAsc = new java.util.LinkedHashMap<>();
      java.util.Map<String, Integer> classInt = new java.util.HashMap<>();
      java.util.Map<String, Integer> classStart = new java.util.HashMap<>();
      Path path = dataDir.resolve("passive-tree.json");
      if (Files.exists(path)) {
        try (InputStream in = Files.newInputStream(path)) {
          tools.jackson.databind.JsonNode root = jsonMapper.readTree(in);
          for (tools.jackson.databind.JsonNode c : root.path("classes")) {
            classKo.put(
                c.path("name").asString(), c.path("nameKo").asString(c.path("name").asString()));
            java.util.List<String> ids = new java.util.ArrayList<>();
            for (tools.jackson.databind.JsonNode a : c.path("ascendancies")) {
              ascKo.put(
                  a.path("id").asString(), a.path("nameKo").asString(a.path("name").asString()));
              ids.add(a.path("id").asString());
            }
            classAsc.put(c.path("name").asString(), java.util.List.copyOf(ids));
            if (c.has("integerId")) {
              classInt.put(c.path("name").asString(), c.path("integerId").asInt());
            }
            if (c.has("startNodeId")) {
              classStart.put(c.path("name").asString(), c.path("startNodeId").asInt());
            }
          }
          for (java.util.Map.Entry<String, tools.jackson.databind.JsonNode> e :
              root.path("nodes").properties()) {
            tools.jackson.databind.JsonNode n = e.getValue();
            nodes.put(
                Integer.valueOf(e.getKey()),
                new TreeNodeLite(
                    n.path("name").asString(),
                    n.path("nameKo").isNull() ? null : n.path("nameKo").asString(null),
                    n.path("kind").asString(),
                    n.path("ascendancy").isNull() ? null : n.path("ascendancy").asString(null)));
          }
        } catch (Exception e) {
          logger.warn("PoE2 트리 색인 로드 실패: {}", path, e);
        }
      }
      treeIndex = new TreeIndex(nodes, classKo, ascKo, classAsc, classInt, classStart);
      return treeIndex;
    }
  }

  public Optional<Poe2.Unique> unique(String slug) {
    return uniques.items().stream().filter(u -> u.slug().equals(slug)).findFirst();
  }

  /**
   * 고유가 있는 아이템 클래스(영문 키 · 한국어) — 목록 화면 칩.
   *
   * <p>분류(category)와 순서는 베이스 목록 칩과 같게 베이스 클래스에서 가져온다(10-01: 고유의 category 는 부위 이름 boots·wand 라 화면이
   * 분류별 줄로 묶을 때 대부분 "기타"로 떨어졌고, 순서도 데이터에 처음 나온 순이었다). 베이스에 없는 클래스만 고유 값 그대로 뒤에 붙인다.
   */
  public List<Poe2.ItemClass> uniqueClasses() {
    java.util.Map<String, Poe2.ItemClass> fromUniques = new java.util.LinkedHashMap<>();
    for (Poe2.Unique u : uniques.items()) {
      if (u.itemClass() != null) {
        fromUniques.putIfAbsent(
            u.itemClass(),
            new Poe2.ItemClass(
                u.itemClass(),
                u.itemClassKo() == null ? u.itemClass() : u.itemClassKo(),
                u.category(),
                null));
      }
    }
    List<Poe2.ItemClass> out = new java.util.ArrayList<>();
    for (Poe2.ItemClass c : bases.classes()) {
      Poe2.ItemClass u = fromUniques.remove(c.key());
      if (u != null) {
        out.add(new Poe2.ItemClass(c.key(), u.ko(), c.category(), c.en()));
      }
    }
    out.addAll(fromUniques.values());
    return List.copyOf(out);
  }
}
