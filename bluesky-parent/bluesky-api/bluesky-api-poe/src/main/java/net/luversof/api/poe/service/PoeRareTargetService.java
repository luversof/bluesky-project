package net.luversof.api.poe.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * 업그레이드 가이드의 <b>레어 목표</b> — 지금 낀 아이템과 <b>같은 베이스</b>에 실제로 붙을 수 있는 접두·접미를 <b>2티어</b>(그 베이스 풀에서 최상위 바로
 * 아래 티어, 롤은 그 티어의 중간값)로 얹은 아이템을 만든다. 최상위 티어는 시장에 거의 없어 사기 어렵다(사용자 지적 2026-09-29) — 2티어가 "살 수 있는 좋은
 * 레어"의 기준이다.
 *
 * <p>티어는 반드시 <b>풀별 목록</b>({@link PoeModDataService#poolTierIds})으로 고른다. mods.json 의 티어 사다리는 모든 클래스를
 * 합친 것이라, 그걸로 고르면 장화에 갑옷 전용 생명력(+160~174)이 붙는다.
 *
 * <p>무엇을 얹을지는 여기서 정하지 않는다 — 후보만 추려 주고, 가이드가 하나씩 끼워 실측해 고른다.
 */
@Service
public class PoeRareTargetService {

  /**
   * 옵션 하나.
   *
   * @param en PoB 원문 — 범위 "(a-b)" 를 그대로 둬 PoB 가 중간 롤로 계산한다
   * @param ko 표시용 — "a~b"
   * @param tierRank 그 베이스 풀에서의 티어 순위(1 = 최상위). 2티어가 없는 옵션(단일 티어)은 1
   * @param families 게임 ModFamily 값 — 둘이 겹치는 옵션은 한 아이템에 같이 못 붙는다(방어도+생명력 복합과 회피+생명력 복합 등)
   */
  public record Affix(
      String family,
      boolean prefix,
      String tierId,
      int tierRank,
      List<String> en,
      List<String> ko,
      List<Integer> families) {

    /** 이 옵션과 같이 못 붙는 옵션인지 — ModFamily 가 하나라도 겹치면. */
    public boolean conflicts(Affix other) {
      return other.families().stream().anyMatch(families::contains);
    }
  }

  /**
   * 한 칸의 레어 목표 재료.
   *
   * @param forced 수치엔 안 잡혀도 실제로 사실상 필수라 늘 넣는 옵션(장화 이동 속도) — 접두 칸을 하나 차지한다
   * @param candidates 실측으로 고를 후보(아이템 희귀도·빛 반경처럼 DPS·EHP 와 무관한 옵션은 미리 뺀다)
   * @param keepLines 지금 아이템에서 그대로 옮기는 줄(품질·홈) — 조건을 같게 맞춘다
   */
  public record Plan(
      PoeBaseItem base,
      String itemClass,
      String variant,
      List<Affix> forced,
      List<Affix> candidates,
      List<String> keepLines) {}

  /** 레어 한 개의 접두·접미 한도. */
  public static final int MAX_PREFIXES = 3;

  public static final int MAX_SUFFIXES = 3;

  private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");

  /** 방어·능력치 쪽 관련 키워드(능력치는 요구치 때문에 필요할 수 있다). */
  private static final List<String> DEFENCE_KEYWORDS =
      List.of(
          "life",
          "energy shield",
          "armour",
          "evasion",
          "resistance",
          "suppress",
          "block",
          "strength",
          "dexterity",
          "intelligence",
          "attributes",
          "maximum mana");

  /** 키워드가 맞아도 DPS·EHP 와 무관한 옵션 — 평가를 아끼려고 미리 뺀다. */
  private static final List<String> IRRELEVANT =
      List.of(
          "rarity",
          "quantity",
          "light radius",
          "stun",
          "reflect",
          "attribute requirements",
          "recharge",
          "flask");

  private final PoeBaseItemDataService baseItems;
  private final PoeModDataService modData;

  public PoeRareTargetService(PoeBaseItemDataService baseItems, PoeModDataService modData) {
    this.baseItems = baseItems;
    this.modData = modData;
  }

  /**
   * 지금 아이템 원문으로 레어 목표 재료를 만든다. 베이스를 모르거나(데이터에 없는 베이스) 풀을 모르면 비어 있다.
   *
   * @param offenceKeywords 주 스킬 태그에서 나온 공격 키워드(가이드가 고유 후보 추릴 때 쓰는 것과 같다)
   */
  public Optional<Plan> plan(String currentItemText, List<String> offenceKeywords) {
    Optional<PoeBaseItem> found = baseOf(currentItemText);
    if (found.isEmpty()) {
      return Optional.empty();
    }
    PoeBaseItem base = found.get();
    String variant = "armour".equals(base.category()) ? attributeVariantOf(base) : "";
    PoeModDataService.ClassMods mods = modData.forItemClass(base.itemClass(), variant);
    if (mods == null) {
      return Optional.empty();
    }
    // 변형 키 확정만 전체 풀(forItemClass)에서 받고, 옵션 사다리는 게임 ModTypeKey 로 묶인 풀별 묶음을 쓴다 —
    // mods.json 패밀리는 Id 이름 규칙으로 묶여 억제 최상위(ChanceToSuppressSpells5__)가 떨어져 나가 2티어를 잘못 셌다.
    String resolved = mods.variant() == null ? "" : mods.variant();
    List<PoeModDataService.PoolGroup> groups =
        modData.poolGroups(base.itemClass(), resolved, base.name());
    if (groups.isEmpty()) {
      return Optional.empty();
    }
    List<Affix> forced = new ArrayList<>();
    List<Affix> candidates = new ArrayList<>();
    List<String> keywords = new ArrayList<>(DEFENCE_KEYWORDS);
    if (offenceKeywords != null) {
      keywords.addAll(offenceKeywords);
    }
    for (PoeModDataService.PoolGroup group : groups) {
      Affix affix = secondTier(group);
      if (affix == null) {
        continue;
      }
      if ("Boots".equals(base.itemClass()) && "MovementVelocity".equals(group.key())) {
        forced.add(affix); // 장화 이동 속도 — DPS·EHP 엔 안 잡히지만 없는 장화는 사실상 안 산다
        continue;
      }
      String text = String.join(" ", affix.en()).toLowerCase(Locale.ROOT);
      if (IRRELEVANT.stream().anyMatch(text::contains)
          || keywords.stream().noneMatch(text::contains)) {
        continue;
      }
      candidates.add(affix);
    }
    return Optional.of(
        new Plan(base, base.itemClass(), resolved, forced, candidates, keepLines(currentItemText)));
  }

  /** 사다리의 2티어(단일 티어면 1티어) — 접두/접미가 아니거나 문장이 없으면 null. */
  private Affix secondTier(PoeModDataService.PoolGroup group) {
    boolean prefix = "prefix".equals(group.gen());
    if (!prefix && !"suffix".equals(group.gen())) {
      return null;
    }
    List<String> ids = group.tiers();
    if (ids == null || ids.isEmpty()) {
      return null;
    }
    int rank = ids.size() >= 2 ? 2 : 1;
    String id = ids.get(rank - 1);
    PoeModDataService.ModTier tier = modData.tier(id);
    if (tier == null || tier.en() == null || tier.en().isEmpty()) {
      return null;
    }
    List<String> en = new ArrayList<>();
    List<String> ko = new ArrayList<>();
    for (int i = 0; i < tier.en().size(); i++) {
      String max = tier.en().get(i);
      String min = tier.enMin() != null && i < tier.enMin().size() ? tier.enMin().get(i) : null;
      en.add(mergeRange(min, max, "(", "-", ")"));
      String koMax = tier.ko() != null && i < tier.ko().size() ? tier.ko().get(i) : max;
      String koMin = tier.koMin() != null && i < tier.koMin().size() ? tier.koMin().get(i) : null;
      ko.add(mergeRange(koMin, koMax, "", "~", ""));
    }
    return new Affix(
        group.key(),
        prefix,
        id,
        rank,
        List.copyOf(en),
        List.copyOf(ko),
        group.families() == null ? List.of() : List.copyOf(group.families()));
  }

  /**
   * 최저·최고 롤 문장을 범위 문장으로 — 숫자 자리 말고는 같아야 한다(다르면 최고 롤 문장 그대로). 같은 숫자는 범위로 만들지 않는다.
   *
   * <p>예: ("+100 to maximum Life", "+114 to maximum Life") → "+(100-114) to maximum Life"
   */
  /** 이 옵션의 최저 롤 한국어 문장(거래소 최소값용, 10-08 C171) — 최저 롤 문장이 없으면 최고 롤 문장. */
  public List<String> koMinLines(Affix a) {
    PoeModDataService.ModTier tier = modData.tier(a.tierId());
    if (tier == null) {
      return List.of();
    }
    List<String> lines = tier.koMin() != null && !tier.koMin().isEmpty() ? tier.koMin() : tier.ko();
    return lines == null ? List.of() : lines;
  }

  static String mergeRange(String min, String max, String open, String sep, String close) {
    if (min == null || max == null) {
      return max;
    }
    List<String> lows = numbers(min);
    List<String> highs = numbers(max);
    if (lows.size() != highs.size()
        || !NUMBER.matcher(min).replaceAll("#").equals(NUMBER.matcher(max).replaceAll("#"))) {
      return max;
    }
    StringBuilder out = new StringBuilder();
    Matcher m = NUMBER.matcher(max);
    int last = 0;
    int i = 0;
    while (m.find()) {
      out.append(max, last, m.start());
      String lo = lows.get(i);
      String hi = highs.get(i);
      out.append(lo.equals(hi) ? hi : open + lo + sep + hi + close);
      last = m.end();
      i++;
    }
    out.append(max.substring(last));
    return out.toString();
  }

  private static List<String> numbers(String s) {
    List<String> out = new ArrayList<>();
    Matcher m = NUMBER.matcher(s);
    while (m.find()) {
      out.add(m.group());
    }
    return out;
  }

  /**
   * PoB 아이템 원문 — 베이스 임플리싯은 범위 그대로(PoB 중간 롤), 옵션은 2티어 범위. 품질·홈은 지금 아이템 것을 옮긴다(방어도·에너지 보호막이 품질에 따라
   * 달라지고, 주 스킬 그룹이 이 칸의 홈에 꽂혀 있을 수 있다).
   */
  public String itemText(Plan plan, List<Affix> affixes) {
    StringBuilder text =
        new StringBuilder("Rarity: RARE\nGuide Target\n")
            .append(plan.base().name())
            .append('\n')
            .append("Item Level: 84\n");
    for (String line : plan.keepLines()) {
      text.append(line).append('\n');
    }
    List<PoeBaseItem.ModLine> implicits =
        plan.base().implicits() == null ? List.of() : plan.base().implicits();
    text.append("Implicits: ").append(implicits.size()).append('\n');
    for (PoeBaseItem.ModLine line : implicits) {
      text.append(line.en()).append('\n');
    }
    for (Affix a : plan.forced()) {
      for (String line : a.en()) {
        text.append(line).append('\n');
      }
    }
    for (Affix a : affixes) {
      for (String line : a.en()) {
        text.append(line).append('\n');
      }
    }
    return text.toString();
  }

  /** PoB 아이템 원문에서 베이스 — 레어·고유는 이름 다음 줄, 노멀은 이름 줄(Superior 를 뗀 것), 매직은 이름 안에 든 베이스. */
  Optional<PoeBaseItem> baseOf(String itemText) {
    if (itemText == null) {
      return Optional.empty();
    }
    List<String> lines = new ArrayList<>();
    for (String raw : itemText.split("\n")) {
      String line = raw.trim();
      if (!line.isEmpty() && !line.startsWith("<")) {
        lines.add(unescapeXml(line));
      }
    }
    for (int i = 0; i < lines.size(); i++) {
      if (!lines.get(i).startsWith("Rarity:")) {
        continue;
      }
      String rarity = lines.get(i).substring("Rarity:".length()).trim();
      if (i + 1 >= lines.size()) {
        return Optional.empty();
      }
      String name = lines.get(i + 1);
      return switch (rarity) {
        case "RARE", "UNIQUE", "RELIC" ->
            i + 2 < lines.size() ? baseItems.findByName(lines.get(i + 2)) : Optional.empty();
        case "NORMAL" -> baseItems.findByName(name.replaceFirst("^Superior ", ""));
        default -> baseItems.findBaseWithinName(name);
      };
    }
    return Optional.empty();
  }

  /** 지금 아이템에서 옮길 줄 — 품질·홈. */
  private static List<String> keepLines(String itemText) {
    List<String> out = new ArrayList<>();
    if (itemText == null) {
      return out;
    }
    for (String raw : itemText.split("\n")) {
      String line = raw.trim();
      if (line.startsWith("Quality:") || line.startsWith("Sockets:")) {
        out.add(line);
      }
    }
    return out;
  }

  /** 베이스 요구 속성 → mods.json 풀의 변형 키(최적화기 attributeVariantOf 와 같은 규칙). */
  static String attributeVariantOf(PoeBaseItem item) {
    boolean str = item.reqStr() > 0;
    boolean dex = item.reqDex() > 0;
    boolean intel = item.reqInt() > 0;
    if (str && dex && intel) {
      return "str_dex_int_armour";
    }
    if (str && dex) {
      return "str_dex_armour";
    }
    if (str && intel) {
      return "str_int_armour";
    }
    if (dex && intel) {
      return "dex_int_armour";
    }
    if (str) {
      return "str_armour";
    }
    if (dex) {
      return "dex_armour";
    }
    if (intel) {
      return "int_armour";
    }
    return "";
  }

  private static String unescapeXml(String s) {
    return s.replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&");
  }
}
