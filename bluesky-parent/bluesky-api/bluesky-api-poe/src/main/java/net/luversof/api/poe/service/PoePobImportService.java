package net.luversof.api.poe.service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.Inflater;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Path of Building 공유 코드 임포트 — base64url → zlib inflate → PoB XML → {@link PoeBuild}.
 *
 * <p>젬/고유/일반 아이템은 영문 이름으로 우리 데이터와 매칭해 한국어 이름과 상세 레이어 링크(slug)를 붙인다. 패시브 트리 노드 id 는 GGG 트리 익스포트의 id
 * 와 동일해 passive-tree.json 과 그대로 조인된다.
 */
@Service
public class PoePobImportService {

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(PoePobImportService.class);

  /** zlib 압축 해제 상한 (조작된 코드로 인한 메모리 폭주 방지) */
  private static final int MAX_INFLATED_BYTES = 32 * 1024 * 1024;

  /** 요약에 표시할 PlayerStat 키 (표시 순서). uiMessage 의 poe.build.stat.<소문자 키> 와 짝을 이룬다. */
  private static final List<String> STAT_KEYS =
      List.of(
          "CombinedDPS",
          "TotalDPS",
          "AverageDamage",
          "Life",
          "EnergyShield",
          "Mana",
          "Armour",
          "Evasion",
          "TotalEHP",
          "FireResist",
          "ColdResist",
          "LightningResist",
          "ChaosResist",
          // 방어 레이어 — PoB 는 저장 시 Effective* 로 심는다(BuildDisplayStats). 표시 키는 결과 스탯시트(#200)와
          // 같게 정규화(STAT_KEY_ALIAS)해 poe.build.stat.spellsuppressionchance 등 기존 라벨을 재사용한다.
          "EffectiveSpellSuppressionChance",
          "EffectiveBlockChance",
          "EffectiveSpellBlockChance",
          "CritChance");

  /** PoB 저장 키(Effective*) → 결과 스탯시트와 공유하는 표시 키. 라벨/일관성 재사용용. */
  private static final Map<String, String> STAT_KEY_ALIAS =
      Map.of(
          "EffectiveSpellSuppressionChance", "spellsuppressionchance",
          "EffectiveBlockChance", "blockchance",
          "EffectiveSpellBlockChance", "spellblockchance");

  private static final Map<String, String> CLASS_KO =
      Map.of(
          "Scion", "사이온",
          "Marauder", "머라우더",
          "Ranger", "레인저",
          "Witch", "위치",
          "Duelist", "듀얼리스트",
          "Templar", "템플러",
          "Shadow", "섀도우");

  private static final Map<String, String> ASCENDANCY_KO =
      Map.ofEntries(
          Map.entry("Ascendant", "어센던트"),
          Map.entry("Juggernaut", "저거너트"),
          Map.entry("Berserker", "버서커"),
          Map.entry("Chieftain", "치프틴"),
          Map.entry("Raider", "레이더"),
          Map.entry("Warden", "워든"),
          Map.entry("Deadeye", "데드아이"),
          Map.entry("Pathfinder", "패스파인더"),
          Map.entry("Occultist", "오컬티스트"),
          Map.entry("Elementalist", "엘리멘탈리스트"),
          Map.entry("Necromancer", "네크로맨서"),
          Map.entry("Slayer", "슬레이어"),
          Map.entry("Gladiator", "글래디에이터"),
          Map.entry("Champion", "챔피언"),
          Map.entry("Inquisitor", "인퀴지터"),
          Map.entry("Hierophant", "하이로펀트"),
          Map.entry("Guardian", "가디언"),
          Map.entry("Assassin", "어쌔신"),
          Map.entry("Saboteur", "사보추어"),
          Map.entry("Trickster", "트릭스터"));

  /** PoB 장착 부위명(뒤의 번호/Swap 제외) → 한국어 */
  private static final Map<String, String> SLOT_KO =
      Map.ofEntries(
          Map.entry("Weapon", "무기"),
          Map.entry("Helmet", "투구"),
          Map.entry("Body Armour", "갑옷"),
          Map.entry("Gloves", "장갑"),
          Map.entry("Boots", "장화"),
          Map.entry("Amulet", "목걸이"),
          Map.entry("Ring", "반지"),
          Map.entry("Belt", "허리띠"),
          Map.entry("Flask", "플라스크"));

  private final PoeGemDataService poeGemDataService;
  private final PoeUniqueDataService poeUniqueDataService;
  private final PoeBaseItemDataService poeBaseItemDataService;
  private final PoeModTranslateService poeModTranslateService;
  private final PoeClusterJewelDataService poeClusterJewelDataService;
  private final PoeTreeGraphService poeTreeGraphService;

  private final PoeRareNameService poeRareNameService;
  private final PoeModDataService poeModDataService;

  public PoePobImportService(
      PoeGemDataService poeGemDataService,
      PoeUniqueDataService poeUniqueDataService,
      PoeBaseItemDataService poeBaseItemDataService,
      PoeModTranslateService poeModTranslateService,
      PoeClusterJewelDataService poeClusterJewelDataService,
      PoeTreeGraphService poeTreeGraphService,
      PoeRareNameService poeRareNameService,
      PoeModDataService poeModDataService) {
    this.poeRareNameService = poeRareNameService;
    this.poeModDataService = poeModDataService;
    this.poeClusterJewelDataService = poeClusterJewelDataService;
    this.poeTreeGraphService = poeTreeGraphService;
    this.poeGemDataService = poeGemDataService;
    this.poeUniqueDataService = poeUniqueDataService;
    this.poeBaseItemDataService = poeBaseItemDataService;
    this.poeModTranslateService = poeModTranslateService;
  }

  /**
   * PoB 공유 코드를 빌드 모델로 변환한다.
   *
   * @throws IllegalArgumentException 코드가 base64/zlib/PoB XML 형식이 아닐 때
   */
  /** PoB 공유 코드 → PoB XML 문자열 (엔진 재계산 등 XML 이 직접 필요할 때) */
  public String decodeToXml(String code) {
    return sanitizeXml(
        new String(inflate(decodeBase64Url(code)), java.nio.charset.StandardCharsets.UTF_8));
  }

  /** {@code <Socket ... itemId="N"/>} — N 이 실제 아이템 id 인지 확인용. */
  private static final java.util.regex.Pattern SOCKET_ITEM_ID =
      java.util.regex.Pattern.compile("<Socket\\b[^>]*\\bitemId=\"(\\d+)\"[^>]*/>");

  private static final java.util.regex.Pattern ITEM_ID =
      java.util.regex.Pattern.compile("<Item\\b[^>]*\\bid=\"(\\d+)\"");

  /**
   * 외부에서 들어온 PoB XML 의 <b>실체 없는 주얼 소켓 참조</b>를 걷어낸다.
   *
   * <p>poe.ninja 실빌드 export 에는 {@code <Socket itemId="N"/>} 만 있고 정작 {@code <Item id="N">} 은 없는 경우가
   * 있다. PoB 의 {@code PassiveSpec:NodesInIntuitiveLeapLikeRadius} 는 {@code item.jewelRadiusIndex} 를
   * <b>nil 검사보다 먼저</b> 읽어(PassiveSpec.lua:1071) 여기서 터지고, 그 예외를 PoB 가 삼켜 스펙 임포트가 중단된다 → 클래스가 사이온으로
   * 떨어진 기본 빌드 수치가 예외 없이 나간다(실측: 아키타입 4건). 소켓 한 줄을 지우는 편이 정확하다 — 어차피 없는 주얼이다.
   */
  static String sanitizeXml(String xml) {
    java.util.Set<String> itemIds = new java.util.HashSet<>();
    java.util.regex.Matcher im = ITEM_ID.matcher(xml);
    while (im.find()) {
      itemIds.add(im.group(1));
    }
    java.util.regex.Matcher sm = SOCKET_ITEM_ID.matcher(xml);
    StringBuilder out = new StringBuilder();
    int dropped = 0;
    while (sm.find()) {
      String id = sm.group(1);
      if (!"0".equals(id) && !itemIds.contains(id)) {
        sm.appendReplacement(out, "");
        dropped++;
      } else {
        sm.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(sm.group()));
      }
    }
    sm.appendTail(out);
    if (dropped > 0) {
      logger.info("PoB XML 정화: 실체 없는 주얼 소켓 참조 {}개 제거", dropped);
    }
    return out.toString();
  }

  public PoeBuild importCode(String code) {
    Document document = parseXml(inflate(decodeBase64Url(code)));
    Element root = document.getDocumentElement();
    if (!"PathOfBuilding".equals(root.getTagName())) {
      throw new IllegalArgumentException("PoB XML 아님: " + root.getTagName());
    }

    Element build = firstChild(root, "Build");
    String className = build != null ? build.getAttribute("className") : "";
    String ascendancy = build != null ? build.getAttribute("ascendClassName") : "";
    if ("None".equals(ascendancy)) {
      ascendancy = "";
    }
    int level = build != null ? parseInt(build.getAttribute("level"), 1) : 1;

    Element spec = activeTreeSpec(root);
    return new PoeBuild(
        className,
        CLASS_KO.getOrDefault(className, className),
        ascendancy,
        ASCENDANCY_KO.getOrDefault(ascendancy, ascendancy),
        level,
        spec != null ? spec.getAttribute("treeVersion").replace('_', '.') : "",
        parseStats(build),
        parsePassiveNodes(spec),
        parseSkillGroups(root),
        parseItems(root),
        treeLink(root, build, spec));
  }

  // ── 디코딩 ──────────────────────────────────────────────

  private byte[] decodeBase64Url(String code) {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("빈 코드");
    }
    String normalized = code.trim().replaceAll("\\s+", "").replace('-', '+').replace('_', '/');
    try {
      return Base64.getDecoder().decode(padBase64(normalized));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("base64 형식 아님", e);
    }
  }

  private String padBase64(String value) {
    int remainder = value.length() % 4;
    return remainder == 0 ? value : value + "=".repeat(4 - remainder);
  }

  private byte[] inflate(byte[] compressed) {
    Inflater inflater = new Inflater();
    inflater.setInput(compressed);
    ByteArrayOutputStream output = new ByteArrayOutputStream(compressed.length * 4);
    byte[] buffer = new byte[16 * 1024];
    try {
      while (!inflater.finished()) {
        int count = inflater.inflate(buffer);
        if (count == 0 && inflater.needsInput()) {
          throw new IllegalArgumentException("zlib 스트림이 잘림");
        }
        output.write(buffer, 0, count);
        if (output.size() > MAX_INFLATED_BYTES) {
          throw new IllegalArgumentException("압축 해제 크기 초과");
        }
      }
      return output.toByteArray();
    } catch (java.util.zip.DataFormatException e) {
      throw new IllegalArgumentException("zlib 형식 아님", e);
    } finally {
      inflater.end();
    }
  }

  private Document parseXml(byte[] xmlBytes) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      return factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xmlBytes));
    } catch (Exception e) {
      throw new IllegalArgumentException("PoB XML 파싱 실패", e);
    }
  }

  // ── 섹션 파싱 ────────────────────────────────────────────

  private List<PoeBuild.PlayerStat> parseStats(Element build) {
    if (build == null) {
      return List.of();
    }
    Map<String, String> byKey = new LinkedHashMap<>();
    for (Element stat : childElements(build, "PlayerStat")) {
      byKey.put(stat.getAttribute("stat"), stat.getAttribute("value"));
    }
    List<PoeBuild.PlayerStat> stats = new ArrayList<>();
    for (String key : STAT_KEYS) {
      String raw = byKey.get(key);
      if (raw == null || raw.isBlank()) {
        continue;
      }
      // 방어 레이어가 0 이면(빌드에 없음) 표시 생략 — 결과 스탯시트(#200)와 같은 잡음 방지
      String outKey = STAT_KEY_ALIAS.getOrDefault(key, key.toLowerCase(Locale.ROOT));
      if (STAT_KEY_ALIAS.containsKey(key) && isZero(raw)) {
        continue;
      }
      stats.add(new PoeBuild.PlayerStat(outKey, formatStatValue(raw)));
    }
    return stats;
  }

  private boolean isZero(String raw) {
    try {
      return Double.parseDouble(raw) == 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private String formatStatValue(String raw) {
    try {
      double value = Double.parseDouble(raw);
      if (Math.abs(value) >= 100) {
        return String.format(Locale.ROOT, "%,.0f", value);
      }
      return String.format(Locale.ROOT, "%.1f", value).replaceAll("\\.0$", "");
    } catch (NumberFormatException e) {
      return raw;
    }
  }

  private Element activeTreeSpec(Element root) {
    Element tree = firstChild(root, "Tree");
    if (tree == null) {
      return null;
    }
    List<Element> specs = childElements(tree, "Spec");
    if (specs.isEmpty()) {
      return null;
    }
    int active = parseInt(tree.getAttribute("activeSpec"), 1);
    return specs.get(Math.min(Math.max(active, 1), specs.size()) - 1);
  }

  private List<Integer> parsePassiveNodes(Element spec) {
    if (spec == null || spec.getAttribute("nodes").isBlank()) {
      return List.of();
    }
    List<Integer> nodeIds = new ArrayList<>();
    for (String token : spec.getAttribute("nodes").split(",")) {
      try {
        nodeIds.add(Integer.parseInt(token.trim()));
      } catch (NumberFormatException ignored) {
        // 마스터리 효과 등 비정수 토큰은 무시
      }
    }
    return nodeIds;
  }

  private List<PoeBuild.SkillGroup> parseSkillGroups(Element root) {
    Element skills = firstChild(root, "Skills");
    if (skills == null) {
      return List.of();
    }
    // 최신 PoB 는 Skills > SkillSet > Skill, 옛 포맷은 Skills > Skill
    List<Element> skillSets = childElements(skills, "SkillSet");
    Element container = skills;
    if (!skillSets.isEmpty()) {
      int active = parseInt(skills.getAttribute("activeSkillSet"), 1);
      container = skillSets.get(Math.min(Math.max(active, 1), skillSets.size()) - 1);
    }
    List<PoeBuild.SkillGroup> groups = new ArrayList<>();
    for (Element skill : childElements(container, "Skill")) {
      List<PoeBuild.BuildGem> gems = new ArrayList<>();
      for (Element gem : childElements(skill, "Gem")) {
        String name = gem.getAttribute("nameSpec");
        if (name.isBlank()) {
          continue;
        }
        Optional<PoeGem> matched = matchGem(name);
        gems.add(
            new PoeBuild.BuildGem(
                matched.map(PoeGem::name).orElse(name),
                matched.map(PoeGem::nameKo).orElse(null),
                matched.map(PoeGem::slug).orElse(null),
                matched.map(PoeGem::isSupport).orElse(false),
                parseInt(gem.getAttribute("level"), 1),
                parseInt(gem.getAttribute("quality"), 0),
                matched.map(PoeGem::color).orElse(null)));
      }
      if (gems.isEmpty()) {
        continue;
      }
      String slot = skill.getAttribute("slot");
      groups.add(
          new PoeBuild.SkillGroup(
              slot, slotKo(slot), !"false".equals(skill.getAttribute("enabled")), gems));
    }
    return groups;
  }

  /** PoB 는 지원 젬을 "Added Fire Damage" 처럼 Support 접미 없이 기록한다 */
  private Optional<PoeGem> matchGem(String nameSpec) {
    Optional<PoeGem> exact = poeGemDataService.findByName(nameSpec);
    if (exact.isPresent()) {
      return exact;
    }
    return poeGemDataService.findByName(nameSpec + " Support");
  }

  private List<PoeBuild.BuildItem> parseItems(Element root) {
    Element items = firstChild(root, "Items");
    if (items == null) {
      return List.of();
    }
    Map<Integer, Element> itemById = new LinkedHashMap<>();
    for (Element item : childElements(items, "Item")) {
      itemById.put(parseInt(item.getAttribute("id"), 0), item);
    }

    // 활성 ItemSet 의 Slot 배치 (슬롯 없는 코드면 아이템 나열만)
    Map<Integer, String> slotByItemId = new LinkedHashMap<>();
    List<Element> itemSets = childElements(items, "ItemSet");
    if (!itemSets.isEmpty()) {
      int active = parseInt(items.getAttribute("activeItemSet"), 1);
      Element itemSet = itemSets.get(Math.min(Math.max(active, 1), itemSets.size()) - 1);
      for (Element slot : childElements(itemSet, "Slot")) {
        int itemId = parseInt(slot.getAttribute("itemId"), 0);
        if (itemId > 0 && !slotByItemId.containsKey(itemId)) {
          slotByItemId.put(itemId, slot.getAttribute("name"));
        }
      }
    }

    List<PoeBuild.BuildItem> result = new ArrayList<>();
    Iterable<Map.Entry<Integer, Element>> ordered =
        slotByItemId.isEmpty()
            ? itemById.entrySet()
            : slotByItemId.keySet().stream()
                .filter(itemById::containsKey)
                .map(id -> Map.entry(id, itemById.get(id)))
                .toList();
    for (Map.Entry<Integer, Element> entry : ordered) {
      PoeBuild.BuildItem parsed =
          parseItemText(
              slotByItemId.getOrDefault(entry.getKey(), ""), entry.getValue().getTextContent());
      if (parsed != null) {
        result.add(parsed);
      }
    }
    return result;
  }

  /** PoB Item 텍스트 블록: "Rarity: UNIQUE" / 이름 / 베이스 / 모드… */
  private PoeBuild.BuildItem parseItemText(String slot, String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    List<String> lines = text.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
    int rarityIndex = -1;
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).toUpperCase(Locale.ROOT).startsWith("RARITY:")) {
        rarityIndex = i;
        break;
      }
    }
    if (rarityIndex < 0 || rarityIndex + 1 >= lines.size()) {
      return null;
    }
    String rarity =
        lines.get(rarityIndex).substring("Rarity:".length()).trim().toUpperCase(Locale.ROOT);
    String name = lines.get(rarityIndex + 1);
    boolean hasBaseLine =
        (rarity.equals("UNIQUE") || rarity.equals("RELIC") || rarity.equals("RARE"))
            && rarityIndex + 2 < lines.size();
    String baseType = hasBaseLine ? lines.get(rarityIndex + 2) : name;

    Optional<PoeUniqueItem> unique =
        rarity.equals("UNIQUE") || rarity.equals("RELIC")
            ? poeUniqueDataService.findByName(name)
            : Optional.empty();
    Optional<PoeBaseItem> base = poeBaseItemDataService.findByName(baseType);
    // 매직/노멀은 PoB 가 베이스 줄을 따로 안 쓰고 "접두 베이스 접미" 한 줄만 준다 → 정확 일치는 항상 실패한다.
    // 이름 안에서 베이스를 찾아내야 플라스크 회복량·방어 수치·요구 사항·베이스 링크가 살아난다.
    // hasBaseLine 이 false 인 경우(=매직·노멀)로 한정한다. 유니크/레어는 베이스가 별도 줄로 오므로
    // 그쪽 조회가 실패했다면 데이터 문제이지 이름 파싱 문제가 아니고, 이름 안 검색은 오히려 엉뚱한 베이스를 붙인다.
    if (base.isEmpty() && !hasBaseLine) {
      base = poeBaseItemDataService.findBaseWithinName(name);
      if (base.isPresent()) {
        baseType = base.get().name();
      }
    }
    // 장비 베이스 표 밖 베이스(팅크처) — 이름 번역용 영 · 한 표로(10-05 C162). 베이스 링크 · 속성은 없다
    String extraBaseKo = null;
    if (base.isEmpty() && !hasBaseLine) {
      String[] extra = poeRareNameService.extraBaseWithin(name);
      if (extra != null) {
        baseType = extra[0];
        extraBaseKo = extra[1];
      }
    }
    // 노멀 아이템은 이름 = 베이스라 베이스 한국어를 이름으로 쓴다
    String nameKo =
        unique
            .map(PoeUniqueItem::nameKo)
            .orElse(
                name.equals(baseType)
                    ? base.map(PoeBaseItem::nameKo).orElse(extraBaseKo)
                    // 레어 이름 "Mind Reach" → "마음의 역량"(게임 Words 접두 · 접미, 10-04 C143)
                    : "RARE".equals(rarity)
                        ? poeRareNameService.translate(name)
                        : "MAGIC".equals(rarity)
                            ? magicNameKo(
                                name, baseType, base.map(PoeBaseItem::nameKo).orElse(extraBaseKo))
                            : null);
    // 롤된 모드 라인 추출 (베이스/이름 이후 메타데이터 제외한 실제 능력치 라인) — 비고유(레어/노멀)에서 특히 필요
    int modStart = (hasBaseLine ? rarityIndex + 3 : rarityIndex + 2);
    List<String> modLines = extractModLines(lines, modStart);
    List<String> kindsAll = extractModEntries(lines, modStart).stream().map(e -> e[1]).toList();
    // 모드 줄 한국어화(실패분은 영문 유지) — 고유도 옮긴다: 빌드 화면 고유 툴팁이 실제 굴림 수치를 보여 준다(10-04 C139, 예전엔 고유 일반 상세)
    List<String> modLinesKo = poeModTranslateService.translate(modLines);
    // PoB 는 "Implicits: N" 뒤 N줄을 임플리싯으로 읽는다. 메타 줄은 모드 앞에 몰려 있으므로
    // 추출된 모드 목록의 앞 N줄이 곧 임플리싯이다. 인게임처럼 구분선으로 갈라 보여주려면 여기서 나눠야 한다.
    int implicitCount = implicitCount(lines);
    int split = Math.min(implicitCount, modLines.size());
    List<String> implicitLines = List.copyOf(modLines.subList(0, split));
    List<String> implicitLinesKo =
        List.copyOf(modLinesKo.subList(0, Math.min(split, modLinesKo.size())));
    modLines = List.copyOf(modLines.subList(split, modLines.size()));
    modLinesKo =
        List.copyOf(modLinesKo.subList(Math.min(split, modLinesKo.size()), modLinesKo.size()));
    // 인게임 아이템 툴팁은 부패 아이템에 맨 아래 **빨간 "부패됨"** 줄을 붙인다(더 이상 제작 불가라는 뜻).
    // PoB 텍스트에도 "Corrupted" 줄이 있는데 ITEM_FLAGS 로 걸러만 내고 아무 데도 남기지 않아,
    // 부패 아이템을 임포트하면 화면에서 부패 여부가 통째로 사라졌다.
    int quality = itemQuality(lines);
    boolean corrupted =
        lines.subList(Math.min(rarityIndex, lines.size()), lines.size()).stream()
            .anyMatch(l -> l.trim().equalsIgnoreCase("corrupted"));
    return new PoeBuild.BuildItem(
        slot,
        slotKo(slot),
        rarity,
        name,
        nameKo,
        baseType,
        base.map(PoeBaseItem::nameKo).orElse(extraBaseKo),
        unique.map(PoeUniqueItem::slug).orElse(null),
        base.map(PoeBaseItem::slug).orElse(null),
        modLines,
        modLinesKo,
        implicitLines,
        implicitLinesKo,
        corrupted,
        quality,
        base.orElse(null),
        remindersOf(implicitLines, false),
        remindersOf(implicitLines, true),
        remindersOf(modLines, false),
        remindersOf(modLines, true),
        itemIntangibility(lines),
        itemInfluences(lines),
        // PoB 는 인챈트를 암시 칸의 {crafted} 로 적는다 — 인게임은 인챈트 색(하늘색)
        kindsAll.subList(0, Math.min(implicitCount(lines), kindsAll.size())).stream()
            .map(k -> "crafted".equals(k) ? "enchant" : k)
            .toList(),
        List.copyOf(
            kindsAll.subList(Math.min(implicitCount(lines), kindsAll.size()), kindsAll.size())),
        unique.map(PoeUniqueItem::flavour).orElse(null),
        unique.map(PoeUniqueItem::flavourKo).orElse(null));
  }

  private static final java.util.Map<String, String> INFLUENCE_KEYS =
      java.util.Map.of(
          "shaper item", "shaper",
          "elder item", "elder",
          "crusader item", "crusader",
          "hunter item", "hunter",
          "redeemer item", "redeemer",
          "warlord item", "warlord",
          "searing exarch item", "exarch",
          "eater of worlds item", "eater",
          "fractured item", "fractured",
          "synthesised item", "synthesised");

  /** PoB 아이템 텍스트의 영향력 · 분열 · 합성 표시 줄 → 심볼 키(등장 순). 인게임은 헤더 좌우 모서리 아이콘(10-04 C137). */
  private static List<String> itemInfluences(List<String> lines) {
    List<String> out = new ArrayList<>();
    for (String line : lines) {
      String key = INFLUENCE_KEYS.get(line.trim().toLowerCase(Locale.ROOT));
      if (key != null && !out.contains(key)) {
        out.add(key);
      }
    }
    return List.copyOf(out);
  }

  private static final java.util.regex.Pattern ITEM_INTANGIBILITY =
      java.util.regex.Pattern.compile(
          "^intangibility:\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);

  /** PoB 아이템 텍스트의 "Intangibility: N%"(없으면 0) — 메타 줄로 걸러낸 뒤 속성으로 따로 싣는다(10-04 C136). */
  private static int itemIntangibility(List<String> lines) {
    for (String line : lines) {
      java.util.regex.Matcher m = ITEM_INTANGIBILITY.matcher(line.trim());
      if (m.find()) {
        return Integer.parseInt(m.group(1));
      }
    }
    return 0;
  }

  /**
   * 마법 아이템 이름 "Vivid Rusted Cuirass of the Whelpling" → "선명한 녹슨 흉갑 - 새끼용". 게임 ClientStrings
   * MagicNamePrefixSuffix = "{1} {0} {2}" (한국어도 접두 · 베이스 · 접미 순), 옵션 이름은 모드 데이터의 티어 이름(영 · 한). 접두 ·
   * 접미 중 있는 것은 모두 옮겨져야 하고, 아니면 null(10-04 C145).
   */
  private String magicNameKo(String name, String baseType, String baseKo) {
    if (name == null || baseType == null || baseKo == null) {
      return null;
    }
    int at = name.indexOf(baseType);
    if (at < 0) {
      return null;
    }
    String prefix = name.substring(0, at).trim();
    String suffix = name.substring(at + baseType.length()).trim();
    String prefixKo = prefix.isEmpty() ? "" : poeModDataService.affixNameKo(prefix);
    String suffixKo = suffix.isEmpty() ? "" : poeModDataService.affixNameKo(suffix);
    if (prefixKo == null || suffixKo == null) {
      return null;
    }
    return String.join(
        " ",
        java.util.stream.Stream.of(prefixKo, baseKo, suffixKo).filter(s -> !s.isEmpty()).toList());
  }

  /** 줄별 인게임 리마인더 문구(영 · 한) — 모드 번역 사전과 같은 정규화로 찾는다. 하나도 없으면 null(10-04 C131). */
  private List<List<String>> remindersOf(List<String> lines, boolean ko) {
    List<List<String>> out = new ArrayList<>();
    boolean any = false;
    for (String line : lines) {
      List<String> texts =
          poeModTranslateService.reminders(line).stream()
              .map(r -> ko && r.ko() != null ? r.ko() : r.en())
              .toList();
      any |= !texts.isEmpty();
      out.add(texts);
    }
    return any ? List.copyOf(out) : null;
  }

  private static final java.util.regex.Pattern IMPLICIT_COUNT =
      java.util.regex.Pattern.compile(
          "^implicits:\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);

  /** PoB 아이템 텍스트의 "Implicits: N". 없으면 0. */
  /** PoB 아이템 텍스트의 "Quality: N" — 인게임 툴팁 속성 블록 **첫 줄**이 품질이다(PoB ItemsTab 4135/4170/4191 동일). */
  private static final java.util.regex.Pattern ITEM_QUALITY =
      java.util.regex.Pattern.compile(
          "^quality:\\s*\\+?(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);

  private static int itemQuality(List<String> lines) {
    for (String line : lines) {
      java.util.regex.Matcher m = ITEM_QUALITY.matcher(line.trim());
      if (m.find()) {
        try {
          return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
          return 0;
        }
      }
    }
    return 0;
  }

  private static int implicitCount(List<String> lines) {
    for (String line : lines) {
      java.util.regex.Matcher m = IMPLICIT_COUNT.matcher(line.trim());
      if (m.find()) {
        try {
          return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
          return 0;
        }
      }
    }
    return 0;
  }

  /** PoB 아이템 텍스트에서 메타데이터 줄을 걸러 실제 능력치(모드) 줄만 뽑는다 (최대 12줄). */
  private static final java.util.regex.Pattern ITEM_META =
      java.util.regex.Pattern.compile(
          "^(rarity|item level|quality|sockets|levelreq|implicits|requires|prefix|suffix|unique id"
              + "|selected variant|has alt variant|catalyst|catalystquality|talisman tier|league"
              + "|source|variant|note|crucible|scourge|influence|foil unique)\\b.*",
          java.util.regex.Pattern.CASE_INSENSITIVE);

  // PoB 가 아이템 텍스트에 써 넣는 속성 · 메타 줄(Classes/Item.lua BuildRaw) — "이름: 값" 모양일 때만 거른다.
  //   "Evasion Rating is increased by …" 같은 진짜 옵션이 같은 낱말로 시작하므로 콜론까지 본다(10-04 C134 실측: ninja 59빌드
  // 레어 줄 3951 중
  //   Intangibility 263 · Energy Shield/…BasePercentile 104씩 · Armour 61 · Evasion 57 줄이 파란 옵션 줄로
  // 툴팁에 섞였다)
  private static final java.util.regex.Pattern ITEM_PROPERTY =
      java.util.regex.Pattern.compile(
          "^(armour|evasion|energy shield|ward|\\w*basepercentile|intangibility|unreleased|crafted"
              + "|cluster jewel skill|cluster jewel node count|memory strands|version|selected version"
              + "|selected variant group|selected alt variant)\\s*:.*",
          java.util.regex.Pattern.CASE_INSENSITIVE);

  private static final java.util.Set<String> ITEM_FLAGS =
      java.util.Set.of(
          "corrupted",
          "mirrored",
          "split",
          "shaper item",
          "elder item",
          "fractured item",
          "synthesised item",
          "searing exarch item",
          "eater of worlds item",
          // 영향력 표시(PoB influenceInfo.display .. " Item") — 빠져 있던 넷(C134)
          "hunter item",
          "warlord item",
          "crusader item",
          "redeemer item",
          "unidentified");

  private List<String> extractModLines(List<String> lines, int start) {
    return extractModEntries(lines, start).stream().map(e -> e[0]).toList();
  }

  /** 옵션 줄과 그 종류([줄, crafted|fractured|""]) — 거르는 규칙은 하나(10-04 C138: 종류를 따로 세면 줄과 어긋난다). */
  private List<String[]> extractModEntries(List<String> lines, int start) {
    List<String[]> mods = new ArrayList<>();
    for (int i = start; i < lines.size() && mods.size() < 12; i++) {
      String line = lines.get(i).trim();
      if (line.isEmpty()) {
        continue;
      }
      String lower = line.toLowerCase(Locale.ROOT);
      if (ITEM_META.matcher(line).matches()
          || ITEM_PROPERTY.matcher(line).matches()
          || ITEM_FLAGS.contains(lower)) {
        continue;
      }
      // PoB 접두 태그 제거: {crafted}, {fractured}, {range:...} 등
      String cleaned = line.replaceAll("\\{[^}]*\\}", "").trim();
      if (!cleaned.isEmpty()) {
        // 종류(제작 · 분열)는 따로 남긴다 — 인게임 툴팁 색(C138)
        String kind =
            line.contains("{fractured}")
                ? "fractured"
                : line.contains("{crafted}") ? "crafted" : "";
        mods.add(new String[] {cleaned, kind});
      }
    }
    return mods;
  }

  private String slotKo(String slot) {
    if (slot == null || slot.isBlank()) {
      return "";
    }
    String stripped = slot.replace(" Swap", "").replaceAll("\\s*\\d+$", "").trim();
    String korean = SLOT_KO.get(stripped);
    if (korean == null) {
      return slot;
    }
    String suffix = slot.substring(stripped.length()).trim();
    return suffix.isEmpty() ? korean : korean + " " + suffix.replace("Swap", "(스왑)");
  }

  // ── DOM 헬퍼 ────────────────────────────────────────────

  private Element firstChild(Element parent, String tagName) {
    List<Element> children = childElements(parent, tagName);
    return children.isEmpty() ? null : children.get(0);
  }

  private List<Element> childElements(Element parent, String tagName) {
    List<Element> elements = new ArrayList<>();
    NodeList childNodes = parent.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      if (childNodes.item(i) instanceof Element element && element.getTagName().equals(tagName)) {
        elements.add(element);
      }
    }
    return elements;
  }

  private int parseInt(String value, int defaultValue) {
    try {
      return Integer.parseInt(value.trim());
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }

  // ── 트리 보기 주소(10-03 C41) ──────────────────────────────

  /**
   * /poe/tree 주소 — nodes= 에 클러스터 주얼 구성(c=소켓:크기:노드수:스킬키:노터블|..:주얼칸수)과 고유 주얼(j=노드:slug)을 붙인다. 트리 뷰어는
   * c= 로 클러스터 노드(id ≥ 65536)를 다시 만들어야 그 할당이 살고 점수에 든다 — 예전엔 nodes 만 넘겨 실빌드가 "포인트 103 / 123"(실제
   * 122)으로 보였다.
   */
  private String treeLink(Element root, Element build, Element spec) {
    List<Integer> nodeIds = parsePassiveNodes(spec);
    StringBuilder link =
        new StringBuilder("/poe/tree?nodes=")
            .append(String.join(",", nodeIds.stream().map(String::valueOf).toList()));
    Element sockets = spec == null ? null : firstChild(spec, "Sockets");
    Element itemsEl = firstChild(root, "Items");
    if (sockets == null || itemsEl == null) {
      appendTreeExtras(link, root, build, spec);
      return link.toString();
    }
    Map<String, String> textById = new java.util.HashMap<>();
    for (Element item : childElements(itemsEl, "Item")) {
      textById.put(item.getAttribute("id"), item.getTextContent());
    }
    List<String> clusters = new ArrayList<>();
    List<String> jewels = new ArrayList<>();
    // 연결 없이 찍기(10-03 C64) — Impossible Escape(l=핵심id:반경) · Thread of Hope · Intuitive
    // Leap(r=주얼칸:안:바깥).
    //   예전엔 링크에 없어 불러온 직후엔 멀쩡하다가 첫 편집에서 그 노드들이 고아로 걷혔다(실빌드 17/56).
    List<String> leapKeys = new ArrayList<>();
    List<String> leapRings = new ArrayList<>();
    for (Element socket : childElements(sockets, "Socket")) {
      String text = textById.get(socket.getAttribute("itemId"));
      String nodeId = socket.getAttribute("nodeId");
      if (text == null || nodeId.isBlank()) {
        continue;
      }
      String cluster = clusterEntry(nodeId, text, this::clusterSkillStats);
      if (cluster != null) {
        clusters.add(cluster);
        // 고유 클러스터(Voices · 메갈로매니악)는 j= 에도 — 소켓에 고유 이름이 뜬다
        if (!text.contains("Rarity: UNIQUE")) {
          continue;
        }
      }
      String slug = uniqueJewelSpec(text);
      if (slug != null) {
        jewels.add(nodeId + ":" + slug);
      }
      String key = impossibleEscapeKey(text, this::keystoneIdByName);
      if (key != null) {
        leapKeys.add(key);
      }
      String ring = leapRing(text);
      if (ring != null) {
        leapRings.add(nodeId + ":" + ring);
      }
    }
    if (!leapKeys.isEmpty()) {
      link.append("&l=").append(String.join(",", leapKeys));
    }
    if (!leapRings.isEmpty()) {
      link.append("&r=").append(String.join(",", leapRings));
    }
    if (!clusters.isEmpty()) {
      link.append("&c=")
          .append(
              java.net.URLEncoder.encode(
                  String.join(",", clusters), java.nio.charset.StandardCharsets.UTF_8));
    }
    if (!jewels.isEmpty()) {
      link.append("&j=")
          .append(
              java.net.URLEncoder.encode(
                  String.join(",", jewels), java.nio.charset.StandardCharsets.UTF_8));
    }
    appendTreeExtras(link, root, build, spec);
    return link.toString();
  }

  /** 크기("Large") → 스킬 키 → 첫 스탯 줄. 데이터가 없으면 빈 맵. */
  private Map<String, String> clusterSkillStats(String sizeName) {
    Map<String, String> out = new java.util.LinkedHashMap<>();
    poeClusterJewelDataService
        .def(sizeName)
        .ifPresent(
            def ->
                def.skills()
                    .forEach(
                        (key, skill) -> {
                          if (skill.stats() != null && !skill.stats().isEmpty()) {
                            out.put(key, skill.stats().get(0));
                          }
                        }));
    return out;
  }

  private static final java.util.regex.Pattern CLUSTER_BASE =
      // 매직 · 레어 이름에 베이스가 섞이기도 한다("Notable Large Cluster Jewel of Significance") — 줄 전체가 아니라
      // 어디든(C42)
      java.util.regex.Pattern.compile("(Small|Medium|Large) Cluster Jewel");
  private static final java.util.regex.Pattern CLUSTER_ADDS =
      java.util.regex.Pattern.compile("(?m)Adds ([0-9]+) Passive Skills");
  private static final java.util.regex.Pattern CLUSTER_SOCKETS =
      java.util.regex.Pattern.compile(
          "(?m)([0-9]+) Added Passive Skills? (?:are|is a) Jewel Sockets?");
  // Voices — "Adds 3 Jewel Socket Passive Skills" · "Adds 5 Small Passive Skills which grant
  // nothing"(PoB ModParser
  // clusterJewelSocketCountOverride · clusterJewelNothingnessCount, C48)
  private static final java.util.regex.Pattern CLUSTER_SOCKET_OVERRIDE =
      java.util.regex.Pattern.compile("(?mi)Adds ([0-9]+) Jewel Socket Passive Skills");
  private static final java.util.regex.Pattern CLUSTER_NOTHINGNESS =
      java.util.regex.Pattern.compile(
          "(?mi)Adds ([0-9]+) Small Passive Skills? which grants? nothing");
  private static final java.util.regex.Pattern CLUSTER_SKILL =
      java.util.regex.Pattern.compile("(?m)^Cluster Jewel Skill: ([A-Za-z0-9_]+)");
  private static final java.util.regex.Pattern CLUSTER_GRANT =
      java.util.regex.Pattern.compile("(?m)Added Small Passive Skills grant: (.+?)\\s*$");
  private static final java.util.regex.Pattern CLUSTER_NOTABLE =
      java.util.regex.Pattern.compile("(?m)1 Added Passive Skill is (.+?)\\s*$");

  /**
   * 클러스터 주얼 원문 → 트리 뷰어 c= 한 항목(소켓:크기:노드수:스킬키:노터블|..:주얼칸수). 클러스터가 아니면 null. 스킬 키는 "Added Small
   * Passive Skills grant:" 문구를 스킬 첫 스탯과 숫자를 지우고 맞댄다(값은 크기마다 달라도 문구는 같다).
   */
  static String clusterEntry(
      String socketId,
      String text,
      java.util.function.Function<String, Map<String, String>> statsBySize) {
    java.util.regex.Matcher base = CLUSTER_BASE.matcher(text);
    java.util.regex.Matcher adds = CLUSTER_ADDS.matcher(text);
    if (!base.find()) {
      return null;
    }
    java.util.regex.Matcher override = CLUSTER_SOCKET_OVERRIDE.matcher(text);
    java.util.regex.Matcher nothing = CLUSTER_NOTHINGNESS.matcher(text);
    if (override.find()) {
      // PoB PassiveSpec:BuildSubgraph — 노드 수 = 소켓 + 노터블(0) + 아무것도 없는 작은 패시브, 스킬 없음(Nothingness)
      int sockets = Integer.parseInt(override.group(1));
      int nodes = sockets + (nothing.find() ? Integer.parseInt(nothing.group(1)) : 0);
      return socketId + ":" + base.group(1) + ":" + nodes + ":::" + sockets;
    }
    if (!adds.find()) {
      return null;
    }
    String size = base.group(1);
    java.util.regex.Matcher sock = CLUSTER_SOCKETS.matcher(text);
    int socketCount = sock.find() ? Integer.parseInt(sock.group(1)) : 0;
    String skillKey = "";
    // PoB 가 적어 둔 스킬 키(Cluster Jewel Skill: affliction_minion_damage)가 있으면 그것 — 문구 맞대기보다 확실하다
    java.util.regex.Matcher skillLine = CLUSTER_SKILL.matcher(text);
    java.util.regex.Matcher grant = CLUSTER_GRANT.matcher(text);
    if (skillLine.find()) {
      skillKey = skillLine.group(1);
    } else if (grant.find()) {
      String want = numberless(grant.group(1));
      for (Map.Entry<String, String> e : statsBySize.apply(size).entrySet()) {
        if (numberless(e.getValue()).equalsIgnoreCase(want)) {
          skillKey = e.getKey();
          break;
        }
      }
    }
    List<String> notables = new ArrayList<>();
    java.util.regex.Matcher nm = CLUSTER_NOTABLE.matcher(text);
    while (nm.find()) {
      String name = nm.group(1).trim();
      if (!name.toLowerCase(Locale.ROOT).startsWith("a jewel socket")) {
        notables.add(name);
      }
    }
    return socketId
        + ":"
        + size
        + ":"
        + adds.group(1)
        + ":"
        + skillKey
        + ":"
        + String.join("|", notables)
        + ":"
        + socketCount;
  }

  private static String numberless(String s) {
    return s.replaceAll("[+-]?[0-9]+(?:[.][0-9]+)?", "#").replaceAll("[{][^}]*[}]", "").trim();
  }

  /**
   * PoB 3_16 jewelRadii — 원 반경(Small … Massive)과 Variable 고리(안 · 바깥). PoE1 은 1.2 배율이 없다(PoE2 와 다름).
   */
  private static final Map<String, Integer> JEWEL_RADIUS_1 =
      Map.of("Small", 960, "Medium", 1440, "Large", 1800, "Very Large", 2400, "Massive", 2880);

  private static final Map<String, int[]> JEWEL_RING_1 =
      Map.of(
          "small", new int[] {960, 1320},
          "medium", new int[] {1320, 1680},
          "large", new int[] {1680, 2040},
          "very large", new int[] {2040, 2400},
          "massive", new int[] {2400, 2880});

  private static String flat(String text) {
    return text.replaceAll("\\s+", " ");
  }

  private static Integer jewelRadius(String text) {
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("(?m)^Radius: (.+?)\\s*$").matcher(text);
    return m.find() ? JEWEL_RADIUS_1.get(m.group(1).trim()) : null;
  }

  /**
   * Impossible Escape — "Passives in radius of <Keystone> can be Allocated without being connected
   * to your tree" → "핵심id:반경". PoB ModParser impossibleEscapeKeystone + PassiveSpec
   * NodesInIntuitiveLeapLikeRadius(핵심의 nodesInRadius[jewelRadiusIndex]). 아니면 null.
   */
  static String impossibleEscapeKey(
      String text, java.util.function.Function<String, Integer> keystoneId) {
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile(
                "(?i)Passives? (?:Skills )?in radius of ([A-Za-z' ]+?) can be Allocated without being connected to your tree")
            .matcher(flat(text));
    if (!m.find()) {
      return null;
    }
    Integer id = keystoneId.apply(m.group(1).trim());
    Integer radius = jewelRadius(text);
    return id == null || radius == null ? null : id + ":" + radius;
  }

  /**
   * Thread of Hope · Intuitive Leap — "Passives in Radius can be Allocated without being connected
   * to your tree" → "안:바깥"(그 주얼 칸 둘레, PoB intuitiveLeapLike). 고리 줄("Only affects Passives in <X>
   * Ring")이 있으면 Variable 고리, 없으면 Radius 원. 아니면 null.
   */
  static String leapRing(String text) {
    String f = flat(text);
    if (!f.matches(
        "(?i).*Passives? (?:Skills )?in Radius can be Allocated without being connected to your tree.*")) {
      return null;
    }
    java.util.regex.Matcher ring =
        java.util.regex.Pattern.compile("(?i)affects Passives in ([A-Za-z ]+?) Ring").matcher(f);
    if (ring.find()) {
      int[] r = JEWEL_RING_1.get(ring.group(1).trim().toLowerCase(Locale.ROOT));
      return r == null ? null : r[0] + ":" + r[1];
    }
    Integer outer = jewelRadius(text);
    return outer == null ? null : "0:" + outer;
  }

  private Integer keystoneIdByName(String name) {
    for (PoeTreeGraphService.TreeNode n : poeTreeGraphService.allNodes()) {
      if ("keystone".equals(n.type()) && name.equalsIgnoreCase(n.name())) {
        return n.id();
      }
    }
    return null;
  }

  /** 고유 주얼이면 우리 고유 DB slug(트리 뷰어 j= — 주얼 효과 · 반경 표시). 아니면 null. */
  /**
   * 고유 주얼 → 트리 주소 j= 값 "slug" 또는 "slug:v번호"(10-03 C77). 변형이 여럿인 주얼(불가능한 탈출 핵심 · 희망의 실 고리 · 이중 인격
   * …)은 아이템 원문으로 변형을 맞춰 붙인다 — 예전엔 slug 만 넘어가 트리 툴팁 · 트리 계산이 기본 변형(희망의 실 = 거대 고리)으로 돌았다.
   */
  private String uniqueJewelSpec(String text) {
    String[] lines = text.trim().split("[\\r\\n]+");
    for (int i = 0; i + 1 < lines.length; i++) {
      if (lines[i].trim().equalsIgnoreCase("Rarity: UNIQUE")) {
        return poeUniqueDataService
            .findByName(lines[i + 1].trim())
            .map(
                u -> {
                  Integer variant = matchVariant(u.variants(), text);
                  return variant == null ? u.slug() : u.slug() + ":v" + variant;
                })
            .orElse(null);
      }
    }
    return null;
  }

  /**
   * 아이템 원문이 어느 변형인가(1-base, 모르면 null). PoB 원문의 "Selected Variant: N" 이 있으면 그것(우리 변형 번호 = PoB 순번),
   * 없으면(게임에서 복사한 원문 — poe.ninja 실빌드가 이 꼴) 변형마다 옵션 줄이 원문에 몇 줄 있는지 세어 가장 많은 하나. 숫자는 지우고 맞댄다(원문 "-11%"
   * ↔ 데이터 "-(20-10)%"). 최다가 둘 이상이면 가릴 수 없어 null.
   */
  static Integer matchVariant(List<PoeUniqueVariant> variants, String text) {
    return variants == null
        ? null
        : matchVariantLines(
            variants.stream()
                .map(
                    v ->
                        java.util.Map.entry(
                            v.index(), v.explicits() == null ? List.<String>of() : v.explicits()))
                .toList(),
            text);
  }

  /** {@link #matchVariant} 의 몸통 — (변형 번호, 옵션 줄) 목록으로 받는다. PoE2 빌드 트리 링크도 쓴다(10-04 C81). */
  public static Integer matchVariantLines(
      List<java.util.Map.Entry<Integer, List<String>>> variants, String text) {
    if (variants == null || variants.size() < 2 || text == null) {
      return null;
    }
    java.util.Set<String> have = new java.util.HashSet<>();
    for (String line : text.split("[\\r\\n]+")) {
      String t = line.trim();
      java.util.regex.Matcher selected =
          java.util.regex.Pattern.compile("^Selected Variant:\\s*(\\d+)$").matcher(t);
      if (selected.matches()) {
        int n = Integer.parseInt(selected.group(1));
        if (variants.stream().anyMatch(v -> v.getKey() != null && v.getKey() == n)) {
          return n;
        }
      }
      have.add(variantKey(t));
    }
    Integer best = null;
    int bestScore = 0;
    boolean tie = false;
    for (java.util.Map.Entry<Integer, List<String>> v : variants) {
      int score = 0;
      for (String line : v.getValue()) {
        if (have.contains(variantKey(line))) {
          score++;
        }
      }
      if (score > bestScore) {
        best = v.getKey();
        bestScore = score;
        tie = false;
      } else if (score == bestScore && score > 0) {
        tie = true;
      }
    }
    return tie ? null : best;
  }

  /** 맞대기 열쇠 — 마크업({…}) · 숫자 · 범위를 지운 소문자 */
  private static String variantKey(String line) {
    return line.replaceAll("\\{[^}]*\\}", "")
        .replaceAll("\\([0-9.]+-[0-9.]+\\)|[0-9]+(\\.[0-9]+)?", "#")
        .trim()
        .toLowerCase(java.util.Locale.ROOT);
  }

  /**
   * 트리 보기 주소의 나머지(10-03 C43) — 시뮬 결과 링크(simOptimizeResult.jte)와 같은 이름들: class · asc(없으면 사이온으로 열려 편집
   * 때 트리가 날아간다), masteries=노드:효과(PoB Spec masteryEffects "{노드,효과}"), tt=노드:문신명|…(Overrides 의
   * Tattoo), an=도유 노터블 id(장비 "Allocates X").
   */
  private void appendTreeExtras(StringBuilder link, Element root, Element build, Element spec) {
    java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;
    String className = build == null ? "" : build.getAttribute("className");
    String ascend = build == null ? "" : build.getAttribute("ascendClassName");
    if (!className.isBlank()) {
      link.append("&class=").append(java.net.URLEncoder.encode(className, utf8));
    }
    if (!ascend.isBlank() && !"None".equals(ascend)) {
      link.append("&asc=").append(java.net.URLEncoder.encode(ascend, utf8));
    }
    if (spec == null) {
      return;
    }
    // 혈맹(10-03 C44) — PoB Spec secondaryAscendClassId = GGG 대체 전직 번호(뷰어 bloodlines 순번과 같음). 안 실으면
    // 혈맹 노드가 숨는다
    String bloodline = spec.getAttribute("secondaryAscendClassId");
    if (bloodline.matches("[1-9][0-9]*")) {
      link.append("&bloodline=").append(bloodline);
    }
    List<String> masteries = new ArrayList<>();
    java.util.regex.Matcher mm =
        java.util.regex.Pattern.compile("[{]([0-9]+),([0-9]+)[}]")
            .matcher(spec.getAttribute("masteryEffects"));
    while (mm.find()) {
      masteries.add(mm.group(1) + ":" + mm.group(2));
    }
    if (!masteries.isEmpty()) {
      link.append("&masteries=")
          .append(java.net.URLEncoder.encode(String.join(",", masteries), utf8));
    }
    Element overrides = firstChild(spec, "Overrides");
    if (overrides != null) {
      List<String> tattoos = new ArrayList<>();
      for (Element ov : childElements(overrides, "Override")) {
        String dn = ov.getAttribute("dn");
        if (dn.startsWith("Tattoo") && !ov.getAttribute("nodeId").isBlank()) {
          tattoos.add(ov.getAttribute("nodeId") + ":" + dn);
        }
      }
      if (!tattoos.isEmpty()) {
        link.append("&tt=").append(java.net.URLEncoder.encode(String.join("|", tattoos), utf8));
      }
    }
    Integer anoint = anointNode(root);
    if (anoint != null) {
      link.append("&an=").append(anoint);
    }
  }

  /** 장비 원문의 "Allocates X" 중 도유 가능한 노터블 → 노드 id(처음 하나 — 뷰어 an= 은 하나). */
  private Integer anointNode(Element root) {
    Element itemsEl = firstChild(root, "Items");
    if (itemsEl == null || !poeTreeGraphService.hasData()) {
      return null;
    }
    Map<String, Integer> byName = new java.util.HashMap<>();
    for (PoeTreeGraphService.TreeNode n : poeTreeGraphService.anointableNotables()) {
      byName.putIfAbsent(n.name(), n.id());
    }
    java.util.regex.Pattern allocates =
        java.util.regex.Pattern.compile("(?m)^(?:[{][^}]*[}])*Allocates (.+?)\\s*$");
    for (Element item : childElements(itemsEl, "Item")) {
      java.util.regex.Matcher m = allocates.matcher(item.getTextContent());
      while (m.find()) {
        Integer id = byName.get(m.group(1).trim());
        if (id != null) {
          return id;
        }
      }
    }
    return null;
  }
}
