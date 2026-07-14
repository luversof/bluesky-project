package net.luversof.web.gate.poe.service;

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
          "CritChance");

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

  public PoePobImportService(
      PoeGemDataService poeGemDataService,
      PoeUniqueDataService poeUniqueDataService,
      PoeBaseItemDataService poeBaseItemDataService) {
    this.poeGemDataService = poeGemDataService;
    this.poeUniqueDataService = poeUniqueDataService;
    this.poeBaseItemDataService = poeBaseItemDataService;
  }

  /**
   * PoB 공유 코드를 빌드 모델로 변환한다.
   *
   * @throws IllegalArgumentException 코드가 base64/zlib/PoB XML 형식이 아닐 때
   */
  /** PoB 공유 코드 → PoB XML 문자열 (엔진 재계산 등 XML 이 직접 필요할 때) */
  public String decodeToXml(String code) {
    return new String(inflate(decodeBase64Url(code)), java.nio.charset.StandardCharsets.UTF_8);
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
        parseItems(root));
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
      stats.add(new PoeBuild.PlayerStat(key.toLowerCase(Locale.ROOT), formatStatValue(raw)));
    }
    return stats;
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
                parseInt(gem.getAttribute("quality"), 0)));
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
    // 노멀 아이템은 이름 = 베이스라 베이스 한국어를 이름으로 쓴다
    String nameKo =
        unique
            .map(PoeUniqueItem::nameKo)
            .orElse(name.equals(baseType) ? base.map(PoeBaseItem::nameKo).orElse(null) : null);
    return new PoeBuild.BuildItem(
        slot,
        slotKo(slot),
        rarity,
        name,
        nameKo,
        baseType,
        base.map(PoeBaseItem::nameKo).orElse(null),
        unique.map(PoeUniqueItem::slug).orElse(null),
        base.map(PoeBaseItem::slug).orElse(null));
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
}
