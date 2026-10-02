package net.luversof.api.poe.poe2;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.luversof.api.poe.service.PoePobImportService;

/**
 * PoB(PoE2) 코드 → 빌드 요약. PoB-PoE2 의 저장 형식은 PoE1 과 같은 구조의 XML(루트만 PathOfBuilding2)이라 코드 해독은 PoE1 의
 * {@link PoePobImportService#decodeToXml(String)} 을 그대로 쓴다. 요약은 PoB 가 저장해 둔 PlayerStat 값을 보이고, 재계산은
 * {@link #recalc(String)} 이 PoB-PoE2 엔진({@link Poe2PobEngineService})으로 따로 한다(약 2초 — 요약을 먼저 보이고 뒤이어
 * 붙인다).
 */
@Service
public class Poe2BuildService {

  /** 보일 스탯(PoB PlayerStat 키) — 순서대로. */
  static final List<String> STAT_KEYS =
      List.of(
          "Life",
          "EnergyShield",
          "Mana",
          "Spirit",
          "Armour",
          "Evasion",
          "TotalEHP",
          "CombinedDPS",
          "FullDPS",
          "TotalDPS",
          "FireResist",
          "ColdResist",
          "LightningResist",
          "ChaosResist",
          "Str",
          "Dex",
          "Int",
          "EffectiveMovementSpeedMod");

  private final PoePobImportService decoder;
  private final Poe2DataService data;
  private final Poe2PobEngineService engine;

  public Poe2BuildService(
      PoePobImportService decoder, Poe2DataService data, Poe2PobEngineService engine) {
    this.decoder = decoder;
    this.data = data;
    this.engine = engine;
  }

  /** 코드 → 업그레이드 가이드(칸·보조젬·패시브 기여 + 고유 교체 상위). 이름은 우리 데이터로 한국어를 붙인다. */
  public Poe2.BuildGuide guide(String code) {
    return guide(code, null);
  }

  /**
   * 레어 목표만(10-02 사용자 요청 + 같은 날 속도 — 가이드 뒤에 따로). 세트를 나눠 쓰는 빌드면 가이드와 같은 세트(set, 없으면 주 세트)를 켠 채.
   * guide2.lua 를 "레어만" 모드로(⑧ 만 하고 끝).
   */
  public Poe2.GuideRares guideRares(String code, Integer set) {
    String decoded = decoder.decodeToXml(code.trim());
    Integer weaponSet = null;
    if (Poe2WeaponSets.uses(decoded)) {
      weaponSet = set != null && (set == 1 || set == 2) ? set : mainSetOf(decoded);
      decoded = Poe2WeaponSets.withSet(decoded, weaponSet);
    }
    if (first(parse(decoded).getDocumentElement(), "Build") == null) {
      throw new IllegalArgumentException("PoB 빌드 형식이 아닙니다(<Build> 없음)");
    }
    if (!engine.available()) {
      return new Poe2.GuideRares(false, null, 0L, weaponSet, List.of());
    }
    Map<String, RareSpec> rares = rareCandidates(summarize(code));
    if (rares.isEmpty()) {
      return new Poe2.GuideRares(true, null, 0L, weaponSet, List.of());
    }
    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put("onlyRares", true);
    doc.put("rares", rareJson(rares));
    Poe2PobEngineService.Raw raw = engine.guide(decoded, GUIDE_JSON.writeValueAsString(doc));
    if (raw.error() != null) {
      return new Poe2.GuideRares(true, raw.error(), raw.elapsedMs(), weaponSet, List.of());
    }
    tools.jackson.databind.JsonNode g = GUIDE_JSON.readTree(raw.payload());
    return new Poe2.GuideRares(
        true, null, raw.elapsedMs(), weaponSet, rareTargets(g.path("rareTargets"), rares));
  }

  /**
   * 무기 세트별 가이드(10-01) — 세트를 나눠 쓰는 빌드면 그 세트를 켠 채 계산한다(엔진은 켜진 세트 하나로만 계산 — Poe2WeaponSets). set 이
   * null 이면 주 세트(두 세트를 병렬로 재 DPS 큰 쪽)로. 세트를 안 나누는 빌드는 set 을 무시하고 저장된 그대로.
   */
  public Poe2.BuildGuide guide(String code, Integer set) {
    String decoded = decoder.decodeToXml(code.trim());
    Integer weaponSet = null;
    Integer mainSet = null;
    if (Poe2WeaponSets.uses(decoded)) {
      if (set != null && (set == 1 || set == 2)) {
        weaponSet = set;
      } else {
        mainSet = mainSetOf(decoded);
        weaponSet = mainSet;
      }
      decoded = Poe2WeaponSets.withSet(decoded, weaponSet);
    }
    String xml = decoded;
    if (first(parse(xml).getDocumentElement(), "Build") == null) {
      throw new IllegalArgumentException("PoB 빌드 형식이 아닙니다(<Build> 없음)");
    }
    if (!engine.available()) {
      return new Poe2.BuildGuide(
          false, null, 0L, null, null, null, List.of(), List.of(), List.of(), List.of(), List.of(),
          0, null, null, List.of(), 0, List.of(), List.of(), List.of(), 0, weaponSet, mainSet,
          List.of());
    }
    // 옵션 목표 후보 — 희귀·마법·일반 장비 칸마다 그 베이스 옵션 풀의 계열별 최고 등급(최대 롤)
    Poe2.BuildSummary summary = summarize(code);
    Map<String, List<ModCandidate>> cands = modCandidates(summary);
    // 레어 목표는 본 가이드에서 빼고 guideRares 로 따로(본 가이드가 4~6초 느려지지 않게, 10-02)
    Map<String, RareSpec> rares = Map.of();
    Poe2PobEngineService.Raw raw = engine.guide(xml, candidatesJson(cands, rares));
    if (raw.error() != null) {
      return new Poe2.BuildGuide(
          true,
          raw.error(),
          raw.elapsedMs(),
          null,
          null,
          null,
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          0,
          null,
          null,
          List.of(),
          0,
          List.of(),
          List.of(),
          List.of(),
          0,
          weaponSet,
          mainSet,
          List.of());
    }
    tools.jackson.databind.JsonNode g = GUIDE_JSON.readTree(raw.payload());
    tools.jackson.databind.JsonNode b = g.path("base");
    Poe2DataService.TreeIndex tree = data.treeIndex();
    List<Poe2.GuideSlot> slots = new ArrayList<>();
    for (tools.jackson.databind.JsonNode s : g.path("slots")) {
      String item = s.path("item").asString(null);
      String rarity = s.path("rarity").asString(null);
      // 한국어: 고유 → 고유 이름, 일반 → 베이스 이름, 마법 → 이름 속 베이스 + "(마법)". 희귀 이름은 인게임에서 무작위라 원문 그대로
      String itemKo = data.uniqueByName(item).map(Poe2.Unique::nameKo).orElse(null);
      if (itemKo == null && "NORMAL".equals(rarity)) {
        itemKo = data.baseByName(item).map(Poe2.BaseItem::nameKo).orElse(null);
      } else if (itemKo == null && "MAGIC".equals(rarity)) {
        itemKo = data.baseWithinName(item).map(bi -> bi.nameKo() + " (마법)").orElse(null);
      }
      slots.add(
          new Poe2.GuideSlot(
              s.path("slot").asString(null),
              item,
              itemKo,
              s.path("base").asString(null),
              s.path("rarity").asString(null),
              num(s, "dps"),
              num(s, "ehp"),
              num(s, "maxHit")));
    }
    List<Poe2.GuideSupport> supports = new ArrayList<>();
    for (tools.jackson.databind.JsonNode s : g.path("supports")) {
      String name = s.path("name").asString(null);
      Optional<Poe2.Gem> gem = data.gemByName(name);
      supports.add(
          new Poe2.GuideSupport(
              name,
              gem.map(Poe2.Gem::nameKo).orElse(null),
              gem.map(Poe2.Gem::slug).orElse(null),
              s.path("applied").asBoolean(true),
              num(s, "dps"),
              num(s, "ehp"),
              gem.map(x -> Boolean.TRUE.equals(x.lineage())).orElse(false)));
    }
    List<Poe2.GuideNode> nodes = new ArrayList<>();
    for (tools.jackson.databind.JsonNode n : g.path("nodes")) {
      int id = n.path("id").asInt();
      Poe2DataService.TreeNodeLite lite = tree.nodes().get(id);
      nodes.add(
          new Poe2.GuideNode(
              id,
              n.path("name").asString(null),
              lite != null ? lite.nameKo() : null,
              n.path("type").asString(null),
              n.path("ascendancy").asString(null),
              num(n, "dps"),
              num(n, "ehp")));
    }
    // 기여가 큰 것부터(DPS+EHP) — 0 인 것(재분배 후보)은 뒤로
    nodes.sort((x, y) -> Double.compare(y.dps() + y.ehp(), x.dps() + x.ehp()));
    return new Poe2.BuildGuide(
        true,
        null,
        raw.elapsedMs(),
        new Poe2.GuideBase(
            num(b, "dps"),
            num(b, "ehp"),
            num(b, "life"),
            num(b, "es"),
            num(b, "maxHit"),
            b.path("skill").asString(null),
            data.gemByName(b.path("skill").asString(null)).map(Poe2.Gem::nameKo).orElse(null)),
        g.path("warn").path("unapplied").asInt(0),
        g.path("warn").path("mainCost").asBoolean(false),
        slots,
        supports,
        nodes,
        swaps(g.path("swapsDps")),
        swaps(g.path("swapsEhp")),
        g.path("tried").asInt(0),
        g.path("gemWeakest").asString(null),
        data.gemByName(g.path("gemWeakest").asString(null)).map(Poe2.Gem::nameKo).orElse(null),
        gemSwaps(g.path("gemSwaps")),
        g.path("gemTried").asInt(0),
        modTargets(g.path("modTargets"), cands),
        nextNodes(g.path("nextDps"), tree, summary.treeLink()),
        nextNodes(g.path("nextEhp"), tree, summary.treeLink()),
        g.path("nextTried").asInt(0),
        weaponSet,
        mainSet,
        rareTargets(g.path("rareTargets"), rares));
  }

  /** 옵션 목표 후보 한 줄(계열 하나의 최고 등급) — lines 는 최대 롤로 채운 영문(PoB 가 읽는 모양), linesKo 는 화면용. */
  record ModCandidate(
      String modType,
      String gen,
      String tierName,
      String tierNameKo,
      List<String> lines,
      List<String> linesKo) {}

  private static final java.util.regex.Pattern EN_RANGE =
      java.util.regex.Pattern.compile("\\((-?\\d+(?:\\.\\d+)?)-(-?\\d+(?:\\.\\d+)?)\\)");
  private static final java.util.regex.Pattern KO_RANGE =
      java.util.regex.Pattern.compile("\\(?(-?\\d+(?:\\.\\d+)?)[~-](-?\\d+(?:\\.\\d+)?)\\)?");

  /** 범위 → 최대 롤(뒤 수치). "(45-50)% increased Armour" → "50% increased Armour". */
  private static String maxRoll(String line, java.util.regex.Pattern range) {
    return line == null ? null : range.matcher(line).replaceAll("$2");
  }

  /** 요약의 장비(주 아이템 세트)에서 칸별 후보 — 고유는 옵션을 더할 수 없어 뺀다. */
  private Map<String, List<ModCandidate>> modCandidates(Poe2.BuildSummary summary) {
    Map<String, List<ModCandidate>> out = new LinkedHashMap<>();
    for (Poe2.BuildItem it : summary.items()) {
      if ("UNIQUE".equals(it.rarity())
          || it.slot() == null
          || it.slot().startsWith("Flask")
          || it.slot().startsWith("Charm")
          || it.slot().contains("Swap")) {
        continue;
      }
      Optional<Poe2.ModPool> pool = data.modPoolForBase(it.baseType());
      if (pool.isEmpty()) {
        continue;
      }
      List<ModCandidate> list = new ArrayList<>();
      for (Poe2.ModGroup g : pool.get().groups()) {
        if (g.tiers() == null || g.tiers().isEmpty()) {
          continue;
        }
        Poe2.ModTier t = g.tiers().get(0);
        if (t.text() == null || t.text().isEmpty()) {
          continue;
        }
        list.add(
            new ModCandidate(
                g.modType(),
                g.gen(),
                t.name(),
                t.nameKo(),
                t.text().stream().map(l -> maxRoll(l, EN_RANGE)).toList(),
                t.textKo() == null
                    ? List.of()
                    : t.textKo().stream().map(l -> maxRoll(l, KO_RANGE)).toList()));
      }
      out.put(it.slot(), list);
    }
    return out;
  }

  /**
   * 러너에 넘길 후보 JSON — { mods: { 칸: [[줄, …], …] }, rares: { 칸: { base, implicits, cands: [{gen, fam,
   * lines}] } } }(칸 안 순서 = 후보 번호). 둘 다 비면 null.
   */
  private static String candidatesJson(
      Map<String, List<ModCandidate>> cands, Map<String, RareSpec> rares) {
    if (cands.isEmpty() && rares.isEmpty()) {
      return null;
    }
    Map<String, List<List<String>>> plain = new LinkedHashMap<>();
    cands.forEach((slot, list) -> plain.put(slot, list.stream().map(ModCandidate::lines).toList()));
    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put("mods", plain);
    doc.put("rares", rareJson(rares));
    return GUIDE_JSON.writeValueAsString(doc);
  }

  private static Map<String, Object> rareJson(Map<String, RareSpec> rares) {
    Map<String, Object> rareJson = new LinkedHashMap<>();
    rares.forEach(
        (slot, spec) ->
            rareJson.put(
                slot,
                Map.of(
                    "base",
                    spec.base(),
                    "implicits",
                    spec.implicits(),
                    "cands",
                    spec.cands().stream()
                        .map(c -> Map.of("gen", c.gen(), "fam", c.modType(), "lines", c.lines()))
                        .toList())));
    return rareJson;
  }

  /** 레어 목표 한 칸의 재료 — 베이스 이름 · 베이스 암시(중간 롤) · 후보 옵션(계열별 2티어 중간 롤). */
  record RareSpec(String base, String baseKo, List<String> implicits, List<ModCandidate> cands) {}

  private static final java.util.Set<String> RARE_SLOTS =
      java.util.Set.of(
          "Weapon 1",
          "Weapon 2",
          "Helmet",
          "Body Armour",
          "Gloves",
          "Boots",
          "Amulet",
          "Ring 1",
          "Ring 2",
          "Belt");

  /** 범위 → 중간 롤(반올림, 소수 범위는 한 자리). "(45-50)% increased Armour" → "48% increased Armour". */
  static String midRoll(String line) {
    if (line == null) {
      return null;
    }
    java.util.regex.Matcher m = EN_RANGE.matcher(line);
    StringBuilder sb = new StringBuilder();
    while (m.find()) {
      double a = Double.parseDouble(m.group(1));
      double b = Double.parseDouble(m.group(2));
      double mid = (a + b) / 2.0;
      boolean decimal = m.group(1).contains(".") || m.group(2).contains(".");
      String v =
          decimal
              ? String.format(java.util.Locale.ROOT, "%.1f", mid)
              : String.valueOf(Math.round(mid));
      m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(v));
    }
    m.appendTail(sb);
    return sb.toString();
  }

  /** 한국어 범위("(20~24)" · "20~24") → 중간 롤 — 화면용(영문 midRoll 과 같은 값). */
  static String midRollKo(String line) {
    if (line == null) {
      return null;
    }
    java.util.regex.Matcher m = KO_RANGE.matcher(line);
    StringBuilder sb = new StringBuilder();
    while (m.find()) {
      double a = Double.parseDouble(m.group(1));
      double b = Double.parseDouble(m.group(2));
      double mid = (a + b) / 2.0;
      boolean decimal = m.group(1).contains(".") || m.group(2).contains(".");
      String v =
          decimal
              ? String.format(java.util.Locale.ROOT, "%.1f", mid)
              : String.valueOf(Math.round(mid));
      m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(v));
    }
    m.appendTail(sb);
    return sb.toString();
  }

  /**
   * 레어 목표 후보(10-02 사용자 요청 "고유만 보지 말고 레어로도") — PoE1 가이드 "레어 목표"(최상위는 못 사니 2티어 중간 롤)와 같은 기준. 고유를 낀 칸도
   * 포함(그 고유 대신 좋은 레어). 옵션 풀이 없는 베이스는 뺀다.
   */
  private Map<String, RareSpec> rareCandidates(Poe2.BuildSummary summary) {
    Map<String, RareSpec> out = new LinkedHashMap<>();
    for (Poe2.BuildItem it : summary.items()) {
      if (it.slot() == null || !RARE_SLOTS.contains(it.slot()) || it.baseType() == null) {
        continue;
      }
      Optional<Poe2.ModPool> pool = data.modPoolForBase(it.baseType());
      if (pool.isEmpty()) {
        continue;
      }
      List<ModCandidate> list = new ArrayList<>();
      for (Poe2.ModGroup g : pool.get().groups()) {
        if (g.tiers() == null || g.tiers().isEmpty()) {
          continue;
        }
        Poe2.ModTier t = g.tiers().size() > 1 ? g.tiers().get(1) : g.tiers().get(0);
        if (t.text() == null || t.text().isEmpty()) {
          continue;
        }
        list.add(
            new ModCandidate(
                g.modType(),
                g.gen(),
                t.name(),
                t.nameKo(),
                t.text().stream().map(Poe2BuildService::midRoll).toList(),
                t.textKo() == null
                    ? List.of()
                    : t.textKo().stream().map(Poe2BuildService::midRollKo).toList()));
      }
      if (list.isEmpty()) {
        continue;
      }
      Optional<Poe2.BaseItem> base = data.baseByName(it.baseType());
      List<String> implicits =
          base.map(
                  b ->
                      b.implicits() == null
                          ? List.<String>of()
                          : b.implicits().stream()
                              .map(l -> midRoll(l.en()))
                              .filter(java.util.Objects::nonNull)
                              .toList())
              .orElse(List.of());
      out.put(
          it.slot(),
          new RareSpec(
              it.baseType(), base.map(Poe2.BaseItem::nameKo).orElse(null), implicits, list));
    }
    return out;
  }

  private List<Poe2.GuideRareTarget> rareTargets(
      tools.jackson.databind.JsonNode list, Map<String, RareSpec> rares) {
    List<Poe2.GuideRareTarget> out = new ArrayList<>();
    for (tools.jackson.databind.JsonNode n : list) {
      String slot = n.path("slot").asString(null);
      RareSpec spec = rares.get(slot);
      if (spec == null) {
        continue;
      }
      String item = n.path("item").asString(null);
      out.add(
          new Poe2.GuideRareTarget(
              slot,
              item,
              data.uniqueByName(item).map(Poe2.Unique::nameKo).orElse(null),
              n.path("rarity").asString(null),
              spec.base(),
              spec.baseKo(),
              n.path("tried").asInt(0),
              upgradeOrNull(rareOf(n.path("dps"), spec)),
              upgradeOrNull(rareOf(n.path("ehp"), spec))));
    }
    // 오르는 폭이 큰 칸부터(DPS·EHP 중 큰 쪽)
    out.sort(
        java.util.Comparator.comparingDouble(
                (Poe2.GuideRareTarget r) ->
                    -Math.max(
                        r.dps() == null || r.dps().dps() == null ? -1e9 : r.dps().dps(),
                        r.ehp() == null || r.ehp().ehp() == null ? -1e9 : r.ehp().ehp()))
            .thenComparing(Poe2.GuideRareTarget::slot));
    return out;
  }

  /** 레어 목표를 보일 만한가 — PoE1 가이드 isUpgrade 와 같은 기준(한 축 +1% 이상, 어느 축도 -5% 넘게 안 깎임). */
  static final double RARE_MIN_GAIN_PCT = 1.0;

  static final double RARE_TRADE_TOLERANCE_PCT = 5.0;

  /**
   * 바꿀 이유가 없는 레어는 지운다(10-02 UU) — "피해용 레어 DPS -9.6%" 처럼 제 축이 내려가는 걸 보이면 헷갈린다. 둘 다 지워지면 화면은 "지금 아이템이
   * 더 좋음". 측정이 없는 축(null)은 0 으로 본다.
   */
  static Poe2.GuideRare upgradeOrNull(Poe2.GuideRare r) {
    if (r == null) {
      return null;
    }
    double dps = r.dps() == null ? 0 : r.dps();
    double ehp = r.ehp() == null ? 0 : r.ehp();
    boolean gains = dps >= RARE_MIN_GAIN_PCT || ehp >= RARE_MIN_GAIN_PCT;
    boolean within = dps >= -RARE_TRADE_TOLERANCE_PCT && ehp >= -RARE_TRADE_TOLERANCE_PCT;
    return gains && within ? r : null;
  }

  private static Poe2.GuideRare rareOf(tools.jackson.databind.JsonNode n, RareSpec spec) {
    if (n == null || n.isMissingNode() || n.isNull() || !n.has("picks")) {
      return null;
    }
    List<String> en = new ArrayList<>();
    List<String> ko = new ArrayList<>();
    for (tools.jackson.databind.JsonNode p : n.path("picks")) {
      int i = p.asInt(-1);
      if (i >= 0 && i < spec.cands().size()) {
        ModCandidate c = spec.cands().get(i);
        en.addAll(c.lines());
        ko.addAll(c.linesKo() == null || c.linesKo().isEmpty() ? c.lines() : c.linesKo());
      }
    }
    return new Poe2.GuideRare(num(n, "dps"), num(n, "ehp"), en, ko, rareItemText(spec, en));
  }

  /** guide2.lua ⑧ 이 재는 아이템과 같은 텍스트(머리 줄 · 암시 개수 · 암시 · 옵션) — 붙여 넣으면 엔진이 잰 그 아이템이 된다. */
  static String rareItemText(RareSpec spec, List<String> lines) {
    List<String> impl = spec.implicits() == null ? List.of() : spec.implicits();
    StringBuilder sb =
        new StringBuilder("Rarity: RARE\nGuide Rare\n")
            .append(spec.base())
            .append("\nItem Level: 82\nImplicits: ")
            .append(impl.size());
    impl.forEach(l -> sb.append('\n').append(l));
    lines.forEach(l -> sb.append('\n').append(l));
    return sb.toString();
  }

  private static List<Poe2.GuideNextNode> nextNodes(
      tools.jackson.databind.JsonNode list, Poe2DataService.TreeIndex tree, String baseTreeLink) {
    List<Poe2.GuideNextNode> out = new ArrayList<>();
    for (tools.jackson.databind.JsonNode n : list) {
      int id = n.path("id").asInt();
      Poe2DataService.TreeNodeLite lite = tree.nodes().get(id);
      // 트리 화면 링크 — 요약의 트리 링크(#c=직업&a=전직&n=할당) 뒤에 가는 길 노드를 잇는다
      StringBuilder extra = new StringBuilder();
      for (tools.jackson.databind.JsonNode p : n.path("path")) {
        extra.append(',').append(p.asInt());
      }
      String link = null;
      if (baseTreeLink != null && !extra.isEmpty()) {
        link =
            baseTreeLink.contains("n=")
                ? baseTreeLink + extra
                : baseTreeLink + (baseTreeLink.contains("#") ? "&n=" : "#n=") + extra.substring(1);
      }
      out.add(
          new Poe2.GuideNextNode(
              id,
              n.path("name").asString(null),
              lite != null ? lite.nameKo() : null,
              n.path("type").asString(null),
              n.path("points").asInt(0),
              num(n, "dps"),
              num(n, "ehp"),
              link));
    }
    return out;
  }

  private List<Poe2.GuideModTarget> modTargets(
      tools.jackson.databind.JsonNode list, Map<String, List<ModCandidate>> cands) {
    List<Poe2.GuideModTarget> out = new ArrayList<>();
    for (tools.jackson.databind.JsonNode t : list) {
      String slot = t.path("slot").asString(null);
      List<ModCandidate> cs = cands.getOrDefault(slot, List.of());
      java.util.function.Function<tools.jackson.databind.JsonNode, List<Poe2.GuideMod>> pick =
          arr -> {
            List<Poe2.GuideMod> mods = new ArrayList<>();
            for (tools.jackson.databind.JsonNode m : arr) {
              int i = m.path("i").asInt(-1);
              if (i < 0 || i >= cs.size()) {
                continue;
              }
              ModCandidate c = cs.get(i);
              mods.add(
                  new Poe2.GuideMod(
                      c.modType(),
                      c.gen(),
                      c.tierName(),
                      c.tierNameKo(),
                      c.lines(),
                      c.linesKo(),
                      num(m, "dps"),
                      num(m, "ehp")));
            }
            return mods;
          };
      String item = t.path("item").asString(null);
      out.add(
          new Poe2.GuideModTarget(
              slot,
              item,
              data.baseWithinName(item).map(Poe2.BaseItem::nameKo).orElse(null),
              t.path("tried").asInt(0),
              pick.apply(t.path("dps")),
              pick.apply(t.path("ehp"))));
    }
    return out;
  }

  private static final tools.jackson.databind.json.JsonMapper GUIDE_JSON =
      tools.jackson.databind.json.JsonMapper.builder().build();

  private static Double num(tools.jackson.databind.JsonNode n, String key) {
    tools.jackson.databind.JsonNode v = n.path(key);
    return v.isNumber() ? v.asDouble() : null;
  }

  /** 보조젬 교체 후보 — GuideSupport 모양을 빌려 쓴다(applied 는 항상 true: 적용되는 보조만 골랐다). */
  private List<Poe2.GuideSupport> gemSwaps(tools.jackson.databind.JsonNode list) {
    List<Poe2.GuideSupport> out = new ArrayList<>();
    for (tools.jackson.databind.JsonNode s : list) {
      String name = s.path("name").asString(null);
      Optional<Poe2.Gem> gem = data.gemByName(name);
      out.add(
          new Poe2.GuideSupport(
              name,
              gem.map(Poe2.Gem::nameKo).orElse(null),
              gem.map(Poe2.Gem::slug).orElse(null),
              true,
              num(s, "dps"),
              num(s, "ehp"),
              gem.map(x -> Boolean.TRUE.equals(x.lineage())).orElse(false)));
    }
    return out;
  }

  private List<Poe2.GuideSwap> swaps(tools.jackson.databind.JsonNode list) {
    List<Poe2.GuideSwap> out = new ArrayList<>();
    for (tools.jackson.databind.JsonNode s : list) {
      String item = s.path("item").asString(null);
      Optional<Poe2.Unique> u = data.uniqueByName(item);
      out.add(
          new Poe2.GuideSwap(
              s.path("slot").asString(null),
              item,
              u.map(Poe2.Unique::nameKo).orElse(null),
              u.map(Poe2.Unique::slug).orElse(null),
              u.map(Poe2.Unique::image).orElse(null),
              s.path("base").asString(null),
              num(s, "dps"),
              num(s, "ehp"),
              num(s, "maxHit"),
              num(s, "life")));
    }
    return out;
  }

  /** 코드 → 엔진 재계산(저장값 대조 + 인게임 경고). 코드를 못 읽으면 IllegalArgumentException. */
  /**
   * 패시브 트리 평가(10-01, PoE1 트리 "트리 계산"의 PoE2 짝) — 직업·전직·노드로 레벨 90 캐릭터 XML 을 만들어 엔진에 넣는다. 스킬을 주면 시뮬
   * 랭킹의 기준 캐릭터(표준 무기·젬 20레벨) 위에 트리만 바꾼다. classId 는 트리 integerId, ascendClassId 는 그 직업 전직 순번(1부터).
   */
  public Poe2.TreeEval treeEval(
      String className, String ascendancy, List<Integer> nodes, String skill) {
    return treeEval(className, ascendancy, nodes, skill, Map.of());
  }

  /**
   * attrs = 능력치 노드 선택(노드 id → 1 힘 · 2 민첩 · 3 지능) — PoB Spec 의 <Overrides><AttributeOverride> 로
   * 넣는다(안 넣으면 "+5 아무 능력치"가 아무것도 안 준다).
   */
  public Poe2.TreeEval treeEval(
      String className,
      String ascendancy,
      List<Integer> nodes,
      String skill,
      Map<Integer, Integer> attrs) {
    return treeEval(className, ascendancy, nodes, skill, attrs, Map.of());
  }

  /**
   * sets = 무기 세트 전용 노드(노드 → 1·2, 10-02) — Spec 의 WeaponSet1/2 로 넣는다(엔진은 켜진 세트 하나로 계산: 저장 세트 = 1).
   */
  public Poe2.TreeEval treeEval(
      String className,
      String ascendancy,
      List<Integer> nodes,
      String skill,
      Map<Integer, Integer> attrs,
      Map<Integer, Integer> sets) {
    Poe2.Gem gem = skill == null || skill.isBlank() ? null : data.gemByName(skill).orElse(null);
    String xml =
        gem != null
            ? Poe2SimRankingService.templateXml(
                gem, Poe2SimRankingService.weaponFor(gem.weaponRequirements()))
            : "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<PathOfBuilding2>\n"
                + "<Build level=\"90\" targetVersion=\"0_1\" className=\"Warrior\" ascendClassName=\"None\""
                + " mainSocketGroup=\"1\" characterLevelAutoMode=\"false\"/>\n"
                + "<Tree activeSpec=\"1\"><Spec ascendClassId=\"0\" treeVersion=\"0_5\" classId=\"6\" nodes=\"\"/></Tree>\n"
                + "</PathOfBuilding2>\n";
    xml = withTree(xml, className, ascendancy, nodes, attrs, sets);
    Poe2DataService.TreeIndex idx = data.treeIndex();
    Integer start = idx.classStartNode().get(className);
    java.util.Set<Integer> ids = new java.util.HashSet<>();
    for (Integer n : nodes) {
      if (n != null && idx.nodes().containsKey(n)) {
        ids.add(n);
      }
    }
    int allocated = (int) ids.stream().filter(i -> !i.equals(start)).count();
    if (!engine.available()) {
      return new Poe2.TreeEval(
          className,
          ascendancy,
          gem == null ? null : gem.name(),
          allocated,
          List.of(),
          "engine unavailable",
          0L,
          null);
    }
    // 세트 전용 노드가 있으면 세트 I · II 를 켠 계산을 따로(병렬) — 엔진은 켜진 세트 하나로만 계산한다(Poe2WeaponSets 머리말, 10-02)
    boolean split =
        sets != null && sets.values().stream().anyMatch(v -> v != null && (v == 1 || v == 2));
    final String base = withItemsBlock(xml);
    Poe2PobEngineService.Result r;
    Poe2PobEngineService.Result r2 = null;
    if (split) {
      java.util.concurrent.CompletableFuture<Poe2PobEngineService.Result> f2 =
          java.util.concurrent.CompletableFuture.supplyAsync(
              () -> engine.calc(Poe2WeaponSets.withSet(base, 2)));
      r = engine.calc(Poe2WeaponSets.withSet(base, 1));
      r2 = f2.join();
    } else {
      r = engine.calc(xml);
    }
    List<Poe2.TreeEvalRow> rows = rowsOf(r);
    List<Poe2.TreeEvalRow> rows2 = r2 == null ? null : rowsOf(r2);
    return new Poe2.TreeEval(
        className,
        ascendancy,
        gem == null ? null : gem.name(),
        allocated,
        rows,
        r.error(),
        r.elapsedMs(),
        rows2);
  }

  private static List<Poe2.TreeEvalRow> rowsOf(Poe2PobEngineService.Result r) {
    List<Poe2.TreeEvalRow> rows = new ArrayList<>();
    if (r != null && r.error() == null) {
      for (String key : STAT_KEYS) {
        Double v = r.values().get(key);
        if (v != null) {
          rows.add(new Poe2.TreeEvalRow(key, v));
        }
      }
    }
    return rows;
  }

  /** 세트를 켜려면 Items·ItemSet 의 useSecondWeaponSet 이 있어야 한다 — 스킬 없는 평가 XML 엔 Items 가 없어 빈 것을 넣는다. */
  static String withItemsBlock(String xml) {
    if (xml.contains("<Items")) {
      return xml;
    }
    return xml.replace(
        "</PathOfBuilding2>",
        "<Items activeItemSet=\"1\" useSecondWeaponSet=\"false\"><ItemSet useSecondWeaponSet=\"false\" id=\"1\"/></Items>\n</PathOfBuilding2>");
  }

  /**
   * 빌드 XML 의 트리를 이 직업·전직·노드·능력치 선택으로 갈아 끼운다(10-01) — 트리 평가와 시뮬 "내 트리에서 출발"이 같이 쓴다. &lt;Tree&gt; 전체를
   * 새 Spec 하나로 바꾸고 Build 의 className·ascendClassName 도 맞춘다. classId = 트리 integerId, ascendClassId =
   * 그 직업 전직 순번(1부터), 능력치 선택 = &lt;Overrides&gt;&lt;AttributeOverride&gt;(PoB 저장 형식). 잘못된 직업·전직이면
   * IllegalArgumentException.
   */
  public String withTree(
      String xml,
      String className,
      String ascendancy,
      java.util.Collection<Integer> nodes,
      Map<Integer, Integer> attrs) {
    return withTree(xml, className, ascendancy, nodes, attrs, Map.of());
  }

  /**
   * sets = 무기 세트 전용 노드(노드 → 1·2) — &lt;WeaponSet1/2 nodes&gt; 로(PoB 저장 형식, 10-02). 할당 노드에 없는 건 버린다.
   */
  public String withTree(
      String xml,
      String className,
      String ascendancy,
      java.util.Collection<Integer> nodes,
      Map<Integer, Integer> attrs,
      Map<Integer, Integer> sets) {
    Poe2DataService.TreeIndex idx = data.treeIndex();
    Integer classId = idx.classIntegerId().get(className);
    if (classId == null) {
      throw new IllegalArgumentException("알 수 없는 직업: " + className);
    }
    List<String> ascs = idx.classAscendancies().getOrDefault(className, List.of());
    int ascId = ascendancy == null || ascendancy.isBlank() ? 0 : ascs.indexOf(ascendancy) + 1;
    if (ascendancy != null && !ascendancy.isBlank() && ascId == 0) {
      throw new IllegalArgumentException("이 직업의 전직이 아닙니다: " + ascendancy);
    }
    // 화면이 보낸 노드 중 트리에 있는 것만 + 직업 시작 노드(PoB 저장 형식도 시작점을 넣는다)
    java.util.TreeSet<Integer> ids = new java.util.TreeSet<>();
    for (Integer n : nodes) {
      if (n != null && idx.nodes().containsKey(n)) {
        ids.add(n);
      }
    }
    Integer start = idx.classStartNode().get(className);
    if (start != null) {
      ids.add(start);
    }
    StringBuilder[] byAttr = {new StringBuilder(), new StringBuilder(), new StringBuilder()};
    for (Map.Entry<Integer, Integer> a :
        new java.util.TreeMap<>(attrs == null ? Map.<Integer, Integer>of() : attrs).entrySet()) {
      int v = a.getValue() == null ? 0 : a.getValue();
      if (v >= 1 && v <= 3 && ids.contains(a.getKey())) {
        StringBuilder sb = byAttr[v - 1];
        if (sb.length() > 0) {
          sb.append(',');
        }
        sb.append(a.getKey());
      }
    }
    String tree =
        "<Tree activeSpec=\"1\"><Spec ascendClassId=\""
            + ascId
            + "\" treeVersion=\"0_5\" classId=\""
            + classId
            + "\" nodes=\""
            + ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))
            + "\"><Overrides><AttributeOverride strNodes=\""
            + byAttr[0]
            + "\" dexNodes=\""
            + byAttr[1]
            + "\" intNodes=\""
            + byAttr[2]
            + "\"/></Overrides>"
            + weaponSetsXml(ids, sets)
            + "</Spec></Tree>";
    String out =
        xml.replaceFirst("(?s)<Tree\\b.*?</Tree>", java.util.regex.Matcher.quoteReplacement(tree));
    if (out.equals(xml)) {
      // 트리가 없는 빌드(드묾) — Build 뒤에 붙인다
      out =
          xml.replaceFirst(
              "(<Build\\b[^>]*?)(/?>)", "$1$2" + java.util.regex.Matcher.quoteReplacement(tree));
    }
    return out.replaceFirst(
            "(<Build\\b[^>]*?\\s)className=\"[^\"]*\"",
            "$1className=\"" + java.util.regex.Matcher.quoteReplacement(className) + "\"")
        .replaceFirst(
            "(<Build\\b[^>]*?\\s)ascendClassName=\"[^\"]*\"",
            "$1ascendClassName=\""
                + java.util.regex.Matcher.quoteReplacement(ascId == 0 ? "None" : ascendancy)
                + "\"");
  }

  private static String weaponSetsXml(java.util.Set<Integer> ids, Map<Integer, Integer> sets) {
    if (sets == null || sets.isEmpty()) {
      return "";
    }
    StringBuilder out = new StringBuilder();
    for (int k = 1; k <= 2; k++) {
      final int set = k;
      String list =
          new java.util.TreeMap<>(sets)
              .entrySet().stream()
                  .filter(
                      e -> e.getValue() != null && e.getValue() == set && ids.contains(e.getKey()))
                  .map(e -> String.valueOf(e.getKey()))
                  .collect(java.util.stream.Collectors.joining(","));
      out.append("<WeaponSet").append(k).append(" nodes=\"").append(list).append("\"/>");
    }
    return out.toString();
  }

  public Poe2.BuildRecalc recalc(String code) {
    String xml = decoder.decodeToXml(code.trim());
    Element build = first(parse(xml).getDocumentElement(), "Build");
    if (build == null) {
      throw new IllegalArgumentException("PoB 빌드 형식이 아닙니다(<Build> 없음)");
    }
    Map<String, Double> saved = new LinkedHashMap<>();
    for (Element s : children(build, "PlayerStat")) {
      Double v = doubleOrNull(s.getAttribute("value"));
      if (v != null) {
        saved.put(s.getAttribute("stat"), v);
      }
    }
    if (!engine.available()) {
      return new Poe2.BuildRecalc(false, null, 0L, List.of(), null, null, null, null, null);
    }
    // 무기 세트를 나눠 쓰면 세트 1·2 를 따로(병렬) 계산한다 — 엔진은 켜진 세트 하나로만 계산하고, 세트마다 패시브·무기가 달라 수치가 크게 갈린다
    //   (Poe2WeaponSets 머리말). 저장값과 견주는 열(computed)은 저장 당시 켜진 세트 값.
    boolean split = Poe2WeaponSets.uses(xml);
    int savedSet = Poe2WeaponSets.saved(xml);
    Poe2PobEngineService.Result r;
    Poe2PobEngineService.Result r1 = null;
    Poe2PobEngineService.Result r2 = null;
    if (split) {
      java.util.concurrent.CompletableFuture<Poe2PobEngineService.Result> f1 =
          java.util.concurrent.CompletableFuture.supplyAsync(
              () -> engine.calc(Poe2WeaponSets.withSet(xml, 1)));
      r2 = engine.calc(Poe2WeaponSets.withSet(xml, 2));
      r1 = f1.join();
      r = savedSet == 2 ? r2 : r1;
    } else {
      r = engine.calc(xml);
    }
    List<Poe2.BuildRecalcRow> rows = new ArrayList<>();
    if (r.error() == null) {
      for (String key : STAT_KEYS) {
        Double c = r.values().get(key);
        Double s = saved.get(key);
        Double c1 = r1 == null || r1.error() != null ? null : r1.values().get(key);
        Double c2 = r2 == null || r2.error() != null ? null : r2.values().get(key);
        if (c != null || s != null) {
          rows.add(new Poe2.BuildRecalcRow(key, s, c, split ? c1 : null, split ? c2 : null));
        }
      }
    }
    Integer mainSet = null;
    if (split && r1 != null && r2 != null && r1.error() == null && r2.error() == null) {
      double d1 = dpsOf(r1.values());
      double d2 = dpsOf(r2.values());
      mainSet = d1 == d2 ? savedSet : d2 > d1 ? 2 : 1;
    }
    Double unapplied = r.values().get("UnappliedSupportCount");
    Double mainWarn = r.values().get("MainSkillCostWarning");
    Double warnCount = r.values().get("CostWarningCount");
    return new Poe2.BuildRecalc(
        true,
        r.error(),
        r.elapsedMs(),
        rows,
        unapplied == null ? null : unapplied.intValue(),
        mainWarn == null ? null : mainWarn > 0,
        warnCount == null ? null : warnCount.intValue(),
        split ? savedSet : null,
        mainSet);
  }

  /** 주 세트 — 두 세트를 병렬로 계산해 DPS 가 큰 쪽(같거나 실패하면 저장된 세트). */
  private int mainSetOf(String xml) {
    java.util.concurrent.CompletableFuture<Poe2PobEngineService.Result> f1 =
        java.util.concurrent.CompletableFuture.supplyAsync(
            () -> engine.calc(Poe2WeaponSets.withSet(xml, 1)));
    Poe2PobEngineService.Result r2 = engine.calc(Poe2WeaponSets.withSet(xml, 2));
    Poe2PobEngineService.Result r1 = f1.join();
    int saved = Poe2WeaponSets.saved(xml);
    if (r1.error() != null || r2.error() != null) {
      return saved;
    }
    double d1 = dpsOf(r1.values());
    double d2 = dpsOf(r2.values());
    return d1 == d2 ? saved : d2 > d1 ? 2 : 1;
  }

  /** 엔진 값의 DPS — 전체 DPS(FullDPS)가 있으면 그것, 없으면 주 스킬(CombinedDPS). 자동 다듬기 measure 와 같은 규칙. */
  private static double dpsOf(Map<String, Double> v) {
    double full = v.getOrDefault("FullDPS", 0d);
    return full > 0 ? full : v.getOrDefault("CombinedDPS", 0d);
  }

  /** 코드 → 요약. 코드를 못 읽으면 IllegalArgumentException(사유 포함). */
  public Poe2.BuildSummary summarize(String code) {
    String xml = decoder.decodeToXml(code.trim());
    Document doc = parse(xml);
    Element root = doc.getDocumentElement();
    Element build = first(root, "Build");
    if (build == null) {
      throw new IllegalArgumentException("PoB 빌드 형식이 아닙니다(<Build> 없음)");
    }
    Poe2DataService.TreeIndex tree = data.treeIndex();
    String className = build.getAttribute("className");
    String ascendancy = build.getAttribute("ascendClassName");
    Integer level = intOrNull(build.getAttribute("level"));

    // 스탯
    Map<String, Double> byKey = new LinkedHashMap<>();
    for (Element s : children(build, "PlayerStat")) {
      Double v = doubleOrNull(s.getAttribute("value"));
      if (v != null) {
        byKey.put(s.getAttribute("stat"), v);
      }
    }
    List<Poe2.BuildStat> stats = new ArrayList<>();
    for (String k : STAT_KEYS) {
      if (byKey.containsKey(k)) {
        stats.add(new Poe2.BuildStat(k, byKey.get(k)));
      }
    }

    // 스킬 — 활성 스킬 세트(없으면 Skills 바로 아래)
    List<Poe2.BuildSkill> skills = new ArrayList<>();
    Element skillsEl = first(root, "Skills");
    int mainGroup = intOrDefault(build.getAttribute("mainSocketGroup"), 1);
    if (skillsEl != null) {
      Element set = activeSet(skillsEl, "SkillSet", "activeSkillSet");
      List<Element> groups = children(set != null ? set : skillsEl, "Skill");
      for (int i = 0; i < groups.size(); i++) {
        Element g = groups.get(i);
        List<Poe2.BuildGem> gems = new ArrayList<>();
        for (Element gem : children(g, "Gem")) {
          String name = gem.getAttribute("nameSpec");
          if (name.isBlank()) {
            continue;
          }
          Optional<Poe2.Gem> found = data.gemByName(name);
          if (found.isEmpty()) {
            found = data.gemByName(name + " Support");
          }
          boolean support =
              found
                  .map(x -> "support".equals(x.kind()))
                  .orElse(gem.getAttribute("skillId").startsWith("Support"));
          gems.add(
              new Poe2.BuildGem(
                  name,
                  found.map(Poe2.Gem::nameKo).orElse(null),
                  found.map(Poe2.Gem::slug).orElse(null),
                  intOrNull(gem.getAttribute("level")),
                  intOrNull(gem.getAttribute("quality")),
                  support,
                  !"false".equals(gem.getAttribute("enabled")),
                  found.map(Poe2.Gem::color).orElse(null),
                  found.map(Poe2.Gem::image).orElse(null)));
        }
        if (gems.isEmpty()) {
          continue;
        }
        skills.add(
            new Poe2.BuildSkill(
                g.getAttribute("slot").isBlank() ? null : g.getAttribute("slot"),
                !"false".equals(g.getAttribute("enabled")),
                i + 1 == mainGroup,
                gems));
      }
    }

    // 장비 — 활성 아이템 세트의 칸 → 아이템 원문
    List<Poe2.BuildItem> items = new ArrayList<>();
    Element itemsEl = first(root, "Items");
    if (itemsEl != null) {
      Map<String, String> textById = new LinkedHashMap<>();
      for (Element it : children(itemsEl, "Item")) {
        textById.put(it.getAttribute("id"), directText(it));
      }
      Element set = activeSet(itemsEl, "ItemSet", "activeItemSet");
      List<Element> slots = set != null ? children(set, "Slot") : children(itemsEl, "Slot");
      for (Element slot : slots) {
        String id = slot.getAttribute("itemId");
        String text = textById.get(id);
        if (id.isBlank() || "0".equals(id) || text == null) {
          continue;
        }
        items.add(item(slot.getAttribute("name"), text));
      }
    }

    // 트리 — 활성 스펙의 nodes
    Element treeEl = first(root, "Tree");
    String treeVersion = null;
    List<Integer> allocated = new ArrayList<>();
    // 능력치 노드 선택(PoB <Overrides><AttributeOverride strNodes dexNodes intNodes>) — 트리 뷰어 주소
    // s=id:1|2|3 으로 넘긴다(10-01).
    //   안 넘기면 빌드에서 트리를 열었을 때 "+5 아무 능력치"만 보이고 스탯 요약·트리 계산이 빌드와 달라진다
    List<String> attrPicks = new ArrayList<>();
    // 무기 세트 전용 노드(<WeaponSet1/2 nodes>) — 트리 뷰어 주소 w1=·w2= 로(10-02, 뷰어가 세트 색으로 보이고 계산·시뮬에 실린다)
    List<List<String>> setNodes = List.of(new ArrayList<>(), new ArrayList<>());
    if (treeEl != null) {
      Element spec = activeSet(treeEl, "Spec", "activeSpec");
      if (spec != null) {
        treeVersion = spec.getAttribute("treeVersion");
        for (String s : spec.getAttribute("nodes").split(",")) {
          Integer id = intOrNull(s.trim());
          if (id != null) {
            allocated.add(id);
          }
        }
        // 세트 전용 노드(<WeaponSet1/2 nodes>)는 Spec nodes 에 없다 — 저장 당시 켜진 세트의 것을 합쳐야 트리가 이어진다(10-02:
        // 빠뜨리면
        //   그 노드로만 이어지는 특화 9개가 트리 보기·내 트리 시뮬에서 끊겨 떨어졌다. 트리 뷰어는 아직 세트를 모르니 켜진 세트 하나만)
        Element setEl = first(spec, "WeaponSet" + Poe2WeaponSets.saved(xml));
        if (setEl == null) {
          Element sets = first(spec, "WeaponSets");
          setEl = sets == null ? null : first(sets, "WeaponSet" + Poe2WeaponSets.saved(xml));
        }
        if (setEl != null) {
          for (String s : setEl.getAttribute("nodes").split(",")) {
            Integer id = intOrNull(s.trim());
            if (id != null && !allocated.contains(id)) {
              allocated.add(id);
            }
          }
        }
        for (int k = 1; k <= 2; k++) {
          Element ws = first(spec, "WeaponSet" + k);
          if (ws == null) {
            Element wsAll = first(spec, "WeaponSets");
            ws = wsAll == null ? null : first(wsAll, "WeaponSet" + k);
          }
          if (ws != null) {
            for (String s2 : ws.getAttribute("nodes").split(",")) {
              Integer id = intOrNull(s2.trim());
              if (id != null) {
                setNodes.get(k - 1).add(String.valueOf(id));
              }
            }
          }
        }
        Element overrides = first(spec, "Overrides");
        Element attr = overrides == null ? null : first(overrides, "AttributeOverride");
        if (attr != null) {
          String[] keys = {"strNodes", "dexNodes", "intNodes"};
          for (int k = 0; k < keys.length; k++) {
            for (String s : attr.getAttribute(keys[k]).split(",")) {
              Integer id = intOrNull(s.trim());
              if (id != null && allocated.contains(id)) {
                attrPicks.add(id + ":" + (k + 1));
              }
            }
          }
        }
      }
    }
    List<Poe2.BuildNode> keystones = new ArrayList<>();
    List<Poe2.BuildNode> notables = new ArrayList<>();
    List<Poe2.BuildNode> ascNodes = new ArrayList<>();
    int ascPoints = 0;
    int mainPoints = 0;
    for (Integer id : allocated) {
      Poe2DataService.TreeNodeLite n = tree.nodes().get(id);
      if (n == null) {
        continue;
      }
      Poe2.BuildNode ref = new Poe2.BuildNode(id, n.name(), n.nameKo());
      if (n.ascendancy() != null) {
        if (!"ascendancyStart".equals(n.kind())) {
          ascPoints++;
          if ("notable".equals(n.kind())) {
            ascNodes.add(ref);
          }
        }
        continue;
      }
      if ("classStart".equals(n.kind())) {
        continue;
      }
      mainPoints++;
      if ("keystone".equals(n.kind())) {
        keystones.add(ref);
      } else if ("notable".equals(n.kind())) {
        notables.add(ref);
      }
    }
    // 우리 트리 뷰어로 이 빌드의 트리를 여는 주소(#c=직업&a=전직&n=노드…)
    String treeLink = null;
    if (!className.isBlank()) {
      StringBuilder link = new StringBuilder("/poe2/tree#c=").append(enc(className));
      String ascId = ascendancyId(ascendancy, tree);
      if (ascId != null) {
        link.append("&a=").append(enc(ascId));
      }
      if (!allocated.isEmpty()) {
        link.append("&n=")
            .append(String.join(",", allocated.stream().map(String::valueOf).toList()));
      }
      if (!attrPicks.isEmpty()) {
        link.append("&s=").append(String.join(",", attrPicks));
      }
      for (int k = 1; k <= 2; k++) {
        if (!setNodes.get(k - 1).isEmpty()) {
          link.append("&w").append(k).append('=').append(String.join(",", setNodes.get(k - 1)));
        }
      }
      treeLink = link.toString();
    }
    return new Poe2.BuildSummary(
        className.isBlank() ? null : className,
        tree.classKo().get(className),
        ascendancy.isBlank() || "None".equals(ascendancy) ? null : ascendancy,
        ascendancy.isBlank() ? null : tree.ascendancyKo().get(ascendancyId(ascendancy, tree)),
        level,
        treeVersion,
        stats,
        skills,
        items,
        new Poe2.BuildTree(mainPoints, ascPoints, keystones, notables, ascNodes),
        treeLink);
  }

  /** PoB 전직 이름 → 트리 JSON 의 전직 id(대부분 같은 영문 이름). */
  private static String ascendancyId(String ascendancy, Poe2DataService.TreeIndex tree) {
    if (ascendancy == null || ascendancy.isBlank() || "None".equals(ascendancy)) {
      return null;
    }
    if (tree.ascendancyKo().containsKey(ascendancy)) {
      return ascendancy;
    }
    for (String id : tree.ascendancyKo().keySet()) {
      if (id.equalsIgnoreCase(ascendancy.replace(" ", ""))) {
        return id;
      }
    }
    return ascendancy;
  }

  /** PoB 아이템 원문 → 칸 요약(이름 · 베이스 · 희귀도 · 옵션 줄). */
  Poe2.BuildItem item(String slot, String text) {
    List<String> lines = new ArrayList<>();
    for (String l : text.replace("\r", "").split("\n")) {
      String t = l.trim();
      if (!t.isEmpty()) {
        lines.add(t);
      }
    }
    String rarity = "NORMAL";
    int i = 0;
    if (!lines.isEmpty() && lines.get(0).startsWith("Rarity:")) {
      rarity = lines.get(0).substring("Rarity:".length()).trim().toUpperCase(Locale.ROOT);
      i = 1;
    }
    String name = i < lines.size() ? lines.get(i) : "";
    String baseType = name;
    if (("UNIQUE".equals(rarity) || "RARE".equals(rarity)) && i + 1 < lines.size()) {
      baseType = lines.get(i + 1);
    }
    // 옵션 줄 — "Implicits: N" 뒤 줄들(PoB 메타 줄은 뺀다), {태그} 접두는 벗긴다
    List<String> mods = new ArrayList<>();
    int start = -1;
    for (int k = 0; k < lines.size(); k++) {
      if (lines.get(k).startsWith("Implicits:")) {
        start = k + 1;
        break;
      }
    }
    if (start >= 0) {
      for (int k = start; k < lines.size(); k++) {
        String m = lines.get(k).replaceAll("^(\\{[^}]*\\})+", "").trim();
        if (!m.isEmpty()) {
          mods.add(m);
        }
      }
    }
    Optional<Poe2.Unique> unique =
        "UNIQUE".equals(rarity) ? data.uniqueByName(name) : Optional.empty();
    Optional<Poe2.BaseItem> base = data.baseByName(baseType);
    if (base.isEmpty() && !"UNIQUE".equals(rarity)) {
      // 매직 이름("Vivid Rusted Cuirass of the Whelpling")에서 베이스를 찾는다
      base = data.baseWithinName(name);
    }
    return new Poe2.BuildItem(
        slot,
        rarity,
        name,
        unique
            .map(Poe2.Unique::nameKo)
            .orElse("NORMAL".equals(rarity) ? base.map(Poe2.BaseItem::nameKo).orElse(null) : null),
        base.map(Poe2.BaseItem::name).orElse(baseType),
        base.map(Poe2.BaseItem::nameKo).orElse(null),
        unique.map(Poe2.Unique::slug).orElse(null),
        base.map(Poe2.BaseItem::slug).orElse(null),
        unique.map(Poe2.Unique::image).orElse(base.map(Poe2.BaseItem::image).orElse(null)),
        mods,
        translateAll(mods));
  }

  /** 옵션 줄 한국어 — 못 옮긴 줄은 영문 그대로(목록 길이·순서는 mods 와 같다). */
  private List<String> translateAll(List<String> mods) {
    Poe2ModTranslator t = data.modTranslator();
    List<String> out = new ArrayList<>(mods.size());
    for (String m : mods) {
      String ko = t.translate(m);
      out.add(ko != null ? ko : m);
    }
    return out;
  }

  // ── XML 도우미 ──

  private static Document parse(String xml) {
    try {
      DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
      // 외부 엔터티·DTD 차단(XXE) — 사용자가 붙여 넣은 코드다
      f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      return f.newDocumentBuilder()
          .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalArgumentException("PoB XML 을 읽지 못했습니다", e);
    }
  }

  private static Element first(Element parent, String tag) {
    for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n instanceof Element e && tag.equals(e.getTagName())) {
        return e;
      }
    }
    return null;
  }

  private static List<Element> children(Element parent, String tag) {
    List<Element> out = new ArrayList<>();
    if (parent == null) {
      return out;
    }
    for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n instanceof Element e && tag.equals(e.getTagName())) {
        out.add(e);
      }
    }
    return out;
  }

  /** activeX 속성이 가리키는 세트(없으면 첫 세트). */
  private static Element activeSet(Element parent, String tag, String activeAttr) {
    List<Element> sets = children(parent, tag);
    if (sets.isEmpty()) {
      return null;
    }
    String active = parent.getAttribute(activeAttr);
    for (Element s : sets) {
      if (!active.isBlank() && active.equals(s.getAttribute("id"))) {
        return s;
      }
    }
    return sets.get(0);
  }

  /** 요소의 직접 텍스트(자식 요소 텍스트 제외 — PoB 아이템은 ModRange 같은 자식 요소를 가진다). */
  private static String directText(Element e) {
    StringBuilder sb = new StringBuilder();
    for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
        sb.append(n.getNodeValue());
      }
    }
    return sb.toString();
  }

  private static Integer intOrNull(String s) {
    try {
      return s == null || s.isBlank() ? null : Integer.valueOf(s.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static int intOrDefault(String s, int d) {
    Integer v = intOrNull(s);
    return v == null ? d : v;
  }

  private static Double doubleOrNull(String s) {
    try {
      return s == null || s.isBlank() ? null : Double.valueOf(s.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String enc(String s) {
    return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
