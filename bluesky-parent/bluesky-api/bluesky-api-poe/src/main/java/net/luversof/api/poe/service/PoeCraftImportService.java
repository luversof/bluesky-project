package net.luversof.api.poe.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * 게임 아이템 복사 글 → 제작 에뮬레이터 아이템(10-20 C485, craftofexile 의 "Import item").
 *
 * <p>게임에서 아이템에 마우스를 올리고 Ctrl+C(옵션 줄만) 또는 Ctrl+Alt+C(옵션마다 "{ 접두어 속성 부여 "이름" (등급: 1) — 태그 }" 머리줄)로
 * 복사한 글을 받는다. 영어 · 한국어 클라이언트 둘 다 — 머리줄 · 희귀도 · 영향력 문구는 게임 ClientStrings(ModDescriptionLinePrefix ·
 * ModDescriptionLineTier · ItemPopupShaperItem 등) 그대로다. 옵션은 그 풀 안에서 문구 틀(숫자를 뺀 글)로 찾고, 같은 틀의 등급은
 * 머리줄 등급 → 수치가 들어가는 범위 순으로 가른다.
 */
@Service
public class PoeCraftImportService {

  /**
   * 가져온 결과 — error 가 있으면 item 은 null. values = 옵션 id → 복사 글의 수치(줄 순서, 화면이 그 수치에 가까운 굴림을 고른다),
   * warnings = 못 알아본 줄 · 무시한 것.
   */
  public record ImportResult(
      PoeCraftEngine.Item item,
      String baseName,
      String baseNameKo,
      // 레어 이름(복사 글 첫 이름 줄, C487) — 레어가 아니면 null
      String itemName,
      Map<String, List<Double>> values,
      List<String> warnings,
      String error) {

    static ImportResult fail(String error) {
      return new ImportResult(null, null, null, null, Map.of(), List.of(), error);
    }
  }

  /** 옵션 후보 하나 — kind = affix(접두 · 접미) | eldritch | corrupted. */
  record Cand(
      String id,
      String gen,
      String name,
      String nameKo,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin,
      Integer tierRank,
      boolean crafted,
      String kind,
      // 같은 문구일 때 먼저 볼 순서 — 0 일반 옵션 · 임플리싯, 1 제작대, 2 에센스, 3 해제 · 화석 · 고양
      int prio) {

    /** 자료 문구 한 항목에 줄바꿈이 든 것(플라스크 '출혈 · 부패한 피 면역')은 줄로 펼친다 — 복사 글은 줄마다 나온다. */
    Cand {
      en = flat(en);
      enMin = flat(enMin);
      ko = flat(ko);
      koMin = flat(koMin);
    }

    private static List<String> flat(List<String> l) {
      if (l == null || l.stream().noneMatch(x -> x != null && x.contains("\n"))) {
        return l;
      }
      List<String> o = new ArrayList<>();
      for (String x : l) {
        for (String y : (x == null ? "" : x).split("\n")) {
          if (!y.isBlank()) {
            o.add(y.strip());
          }
        }
      }
      return o;
    }
  }

  /** 옵션 머리줄(Ctrl+Alt+C) 하나와 그 아래 문구 줄. */
  record Block(
      String kind, String gen, String name, Integer tier, boolean crafted, boolean fractured) {}

  /** 방어 속성 변형이 있는 클래스(craft-mods 풀 키 "클래스|str_armour|"). */
  static final Set<String> VARIANT_CLASSES =
      Set.of("Body Armour", "Boots", "Gloves", "Helmet", "Shield");

  /** 영향력 줄(ClientStrings ItemPopup*Item, 영 · 한) → 풀 키. */
  static final Map<String, String> INFLUENCE =
      Map.ofEntries(
          Map.entry("shaper item", "shaper"),
          Map.entry("쉐이퍼 아이템", "shaper"),
          Map.entry("elder item", "elder"),
          Map.entry("엘더 아이템", "elder"),
          Map.entry("crusader item", "crusader"),
          Map.entry("십자군 아이템", "crusader"),
          Map.entry("redeemer item", "eyrie"),
          Map.entry("대속자 아이템", "eyrie"),
          Map.entry("hunter item", "basilisk"),
          Map.entry("사냥꾼 아이템", "basilisk"),
          Map.entry("warlord item", "adjudicator"),
          Map.entry("전쟁군주 아이템", "adjudicator"));

  /** 희귀도 값(ClientStrings ItemDisplayString{Normal,Magic,Rare,Unique}). */
  static final Map<String, String> RARITY =
      Map.ofEntries(
          Map.entry("normal", "NORMAL"),
          Map.entry("일반", "NORMAL"),
          Map.entry("magic", "MAGIC"),
          Map.entry("마법", "MAGIC"),
          Map.entry("rare", "RARE"),
          Map.entry("희귀", "RARE"),
          Map.entry("unique", "UNIQUE"),
          Map.entry("고유", "UNIQUE"));

  /**
   * 기폭제 퀄리티 줄 "Quality (Attack Modifiers): +20%" 의 괄호 안 낱말 → 기폭제 키(PoeCraftEngine.CATALYST_TAGS).
   */
  static final List<Map.Entry<String, String>> CATALYST_WORDS =
      List.of(
          Map.entry("physical", "physicalchaos"),
          Map.entry("물리", "physicalchaos"),
          Map.entry("elemental", "elemental"),
          Map.entry("원소", "elemental"),
          Map.entry("attack", "attack"),
          Map.entry("공격", "attack"),
          Map.entry("speed", "speed"),
          Map.entry("속도", "speed"),
          Map.entry("suffix", "suffix"),
          Map.entry("접미", "suffix"),
          Map.entry("prefix", "prefix"),
          Map.entry("접두", "prefix"),
          Map.entry("life", "resource"),
          Map.entry("생명력", "resource"),
          Map.entry("caster", "caster"),
          Map.entry("시전", "caster"),
          Map.entry("주문", "caster"),
          Map.entry("attribute", "attribute"),
          Map.entry("능력치", "attribute"),
          Map.entry("resistance", "resistance"),
          Map.entry("저항", "resistance"),
          Map.entry("defence", "defense"),
          Map.entry("방어", "defense"),
          Map.entry("critical", "critical"),
          Map.entry("치명", "critical"));

  private static final Pattern HEADER = Pattern.compile("^\\{\\s*(.*?)\\s*\\}$");

  private static final Pattern AFFIX_EN =
      Pattern.compile(
          "^(Master Crafted |Fractured )?(Prefix|Suffix) Modifier \"(.*?)\""
              + "(?:\\s*\\((?:Tier|Rank): (\\d+)\\))?.*$");

  private static final Pattern AFFIX_KO =
      Pattern.compile(
          "^(대가의 제작 |분열된 )?(접두어|접미어) 속성 부여 \"(.*?)\"(?:\\s*\\((?:등급|단계): (\\d+)\\))?.*$");

  private static final Pattern ELDRITCH =
      Pattern.compile(
          "^(Searing Exarch|Eater of Worlds|작열의 총주교|세계 포식자) (?:Implicit Modifier|고정 속성).*$");

  private static final Pattern KEY_VALUE = Pattern.compile("^([^:{]{1,40}):\\s*(.*)$");

  private static final Pattern QUALITY =
      Pattern.compile("^(?:Quality|퀄리티)\\s*\\((.+?)\\):\\s*\\+?(\\d+)%.*$");

  /** 고급 복사의 굴림 범위 "+70(70-79)" · "(15-20)" — 지우고 지금 값만 남긴다. */
  private static final Pattern RANGE =
      Pattern.compile("\\(\\s*[+-]?\\d+(?:\\.\\d+)?\\s*[-–]\\s*[+-]?\\d+(?:\\.\\d+)?\\s*\\)");

  private static final Pattern NUMBER = Pattern.compile("[+-]?\\d+(?:\\.\\d+)?");

  /** 줄 끝 꼬리표 "(implicit)" · "(crafted)" · "(fractured)" 등 — 영어 낱말 하나. */
  private static final Pattern TRAIL = Pattern.compile("\\s*\\(([^()\\d]{1,16})\\)\\s*$");

  private final PoeCraftService craft;
  private final PoeCraftDataService data;
  private final PoeModDataService mods;
  private final PoeBaseItemDataService bases;

  public PoeCraftImportService(
      PoeCraftService craft,
      PoeCraftDataService data,
      PoeModDataService mods,
      PoeBaseItemDataService bases) {
    this.craft = craft;
    this.data = data;
    this.mods = mods;
    this.bases = bases;
  }

  /** 복사 글 하나를 아이템으로. */
  public ImportResult parse(String text) {
    if (text == null || text.isBlank()) {
      return ImportResult.fail("붙여 넣은 글이 없습니다");
    }
    text = fromPob(text);
    List<List<String>> sections = new ArrayList<>();
    List<String> cur = new ArrayList<>();
    for (String raw : text.replace("\r", "").split("\n")) {
      String line = raw.strip();
      if (line.matches("^-{4,}$")) {
        sections.add(cur);
        cur = new ArrayList<>();
      } else if (!line.isEmpty()) {
        cur.add(line);
      }
    }
    sections.add(cur);
    if (sections.isEmpty() || sections.get(0).isEmpty()) {
      return ImportResult.fail("아이템 글이 아닙니다");
    }
    List<String> warnings = new ArrayList<>();
    // 머리 칸 — "Item Class: Gloves" · "Rarity: Rare" · 이름 줄
    String rarity = null;
    List<String> names = new ArrayList<>();
    for (String line : sections.get(0)) {
      Matcher kv = KEY_VALUE.matcher(line);
      if (kv.matches()) {
        String key = kv.group(1).strip().toLowerCase(Locale.ROOT);
        if (key.equals("rarity") || key.equals("아이템 희귀도") || key.equals("희귀도")) {
          rarity = RARITY.get(kv.group(2).strip().toLowerCase(Locale.ROOT));
        }
        continue;
      }
      names.add(line);
    }
    if (rarity == null) {
      return ImportResult.fail(
          "희귀도 줄(Rarity: · 아이템 희귀도:)을 찾지 못했습니다 — 게임에서 Ctrl+C 로 복사한 글을 붙여 넣으세요");
    }
    if ("UNIQUE".equals(rarity)) {
      return ImportResult.fail("고유 아이템은 제작 에뮬레이터 대상이 아닙니다");
    }
    PoeBaseItem base = findBase(names);
    if (base == null) {
      return ImportResult.fail("베이스를 찾지 못했습니다: " + String.join(" / ", names));
    }
    String itemClass = base.itemClass();
    String variant = VARIANT_CLASSES.contains(itemClass) ? attributeVariant(base) : "";
    // 베이스가 정하는 변형(C516 — 주얼 색 · 심연 주얼 종류, 방어구도 같은 결과)
    String byBase = mods.variantOfBase(itemClass, base.name());
    if (byBase != null) {
      variant = byBase;
    }
    if (base.name().endsWith("Cluster Jewel")) {
      // 군집 주얼(10-20 C515) — 인챈트 '작은 패시브 효과' 줄로 변형(cluster_<크기>.<인챈트>)을 고른다
      variant = clusterVariant(itemClass, base.name(), sections, warnings);
      if (variant == null) {
        return ImportResult.fail("군집 주얼의 인챈트(작은 패시브 효과) 줄을 찾지 못했습니다");
      }
    }
    if (!data.hasPool(itemClass + "|" + variant + "|")) {
      variant = "";
    }
    if (!data.hasPool(itemClass + "|" + variant + "|")) {
      return ImportResult.fail("제작 에뮬레이터가 다루지 않는 종류입니다: " + itemClass);
    }
    // 나머지 칸 — 아이템 레벨 · 영향력 · 타락 · 퀄리티, 그리고 옵션
    int ilvl = 84;
    String influence = "";
    boolean corrupted = false;
    String quality = null;
    for (int s = 1; s < sections.size(); s++) {
      for (String line : sections.get(s)) {
        String low = line.toLowerCase(Locale.ROOT);
        Matcher kv = KEY_VALUE.matcher(line);
        if (kv.matches() && !line.startsWith("{")) {
          String key = kv.group(1).strip().toLowerCase(Locale.ROOT);
          if (key.equals("item level") || key.equals("아이템 레벨")) {
            Matcher n = NUMBER.matcher(kv.group(2));
            if (n.find()) {
              ilvl = Math.max(1, Math.min(100, (int) Double.parseDouble(n.group())));
            }
          }
          Matcher q = QUALITY.matcher(line);
          if (q.matches()) {
            String k = catalystKey(q.group(1));
            if (k != null && PoeCraftEngine.JEWELLERY.contains(itemClass)) {
              quality =
                  k + ":" + Math.min(PoeCraftEngine.MAX_QUALITY, Integer.parseInt(q.group(2)));
            }
          }
          continue;
        }
        if (INFLUENCE.containsKey(low)) {
          if (!influence.isEmpty() && !influence.equals(INFLUENCE.get(low))) {
            warnings.add("영향력 둘은 하나만 반영합니다(" + line + " 무시)");
          } else {
            influence = INFLUENCE.get(low);
          }
        } else if (low.equals("corrupted") || line.equals("타락")) {
          corrupted = true;
        } else if (low.equals("mirrored") || line.equals("복제")) {
          warnings.add("복제된 아이템 — 게임에선 더 바꿀 수 없습니다");
        } else if (low.equals("unidentified") || line.equals("미확인")) {
          return ImportResult.fail("미확인 아이템은 옵션을 알 수 없습니다 — 감정한 뒤 복사하세요");
        }
      }
    }
    if (!influence.isEmpty() && !data.hasPool(itemClass + "|" + variant + "|" + influence)) {
      warnings.add("이 종류엔 그 영향력 풀이 없어 영향력은 뺐습니다");
      influence = "";
    }
    PoeCraftEngine.Item shell =
        new PoeCraftEngine.Item(
            itemClass,
            variant,
            influence,
            ilvl,
            PoeCraftEngine.Rarity.valueOf(rarity),
            List.of(),
            corrupted);
    List<Cand> cands = candidates(shell);
    List<PoeCraftEngine.Affix> affixes = new ArrayList<>();
    List<PoeCraftEngine.Affix> implicits = new ArrayList<>();
    Map<String, List<Double>> values = new LinkedHashMap<>();
    // 옵션 칸 — 고급 복사면 머리줄 단위, 아니면 줄 단위(두 줄 옵션 먼저)
    for (int s = 1; s < sections.size(); s++) {
      List<String> sec = sections.get(s);
      int i = 0;
      while (i < sec.size()) {
        String line = sec.get(i);
        Matcher h = HEADER.matcher(line);
        if (h.matches()) {
          Block b = block(h.group(1));
          int j = i + 1;
          List<String> stat = new ArrayList<>();
          while (j < sec.size() && !HEADER.matcher(sec.get(j)).matches()) {
            if (!isReminder(sec.get(j))) {
              stat.add(sec.get(j));
            }
            j++;
          }
          i = j;
          // 장막 줄은 머리줄 이름이 없다("{ Prefix Modifier }" — 내보내기 C489 도 이 꼴) → 머리줄 해석 전에
          if (veiled(stat, b, affixes)) {
            continue;
          }
          if (b == null) {
            continue; // 베이스 고정 속성 · 인챈트 등 — 베이스가 정한다
          }
          Cand c = match(cands, b, stat);
          if (c == null) {
            warnings.add("못 알아본 옵션: " + String.join(" / ", stat));
            continue;
          }
          add(c, b.crafted(), b.fractured(), stat, affixes, implicits, values);
          continue;
        }
        if (isMeta(line)) {
          i++;
          continue;
        }
        // 줄 단위(Ctrl+C) — 꼬리표로 종류를 알고, 두 줄 옵션을 먼저 본다
        String tag = trail(line);
        if ("implicit".equals(tag) || "enchant".equals(tag)) {
          Block ib = new Block("implicit", null, null, null, false, false);
          Cand c = match(cands, ib, List.of(line));
          if (c != null) {
            add(c, false, false, List.of(line), affixes, implicits, values);
          }
          i++;
          continue;
        }
        if (veiled(List.of(line), null, affixes)) {
          i++;
          continue;
        }
        boolean crafted = "crafted".equals(tag);
        boolean fractured = "fractured".equals(tag);
        Block pb = new Block("affix", null, null, null, crafted, fractured);
        Cand one0 = match(cands, pb, List.of(line));
        // 여러 줄 옵션(두 줄 혼성 · 플라스크 세 줄 등, 4줄까지) — 긴 묶음부터, 수치가 범위 안이고 첫 줄만으로 찾은 옵션보다 흔한 출처일 때만(일반
        // 생명력 + 방어도 두 줄을 장막 해제 옵션 하나로 읽었다)
        boolean grouped = false;
        for (int k = Math.min(4, sec.size() - i); k >= 2 && !grouped; k--) {
          List<String> grp = new ArrayList<>(sec.subList(i, i + k));
          if (grp.stream().skip(1).anyMatch(PoeCraftImportService::isMeta)) {
            continue;
          }
          Cand multi = match(cands, pb, grp);
          if (multi != null && fits(multi, grp) && (one0 == null || multi.prio() <= one0.prio())) {
            add(multi, crafted, fractured, grp, affixes, implicits, values);
            i += k;
            grouped = true;
          }
        }
        if (grouped) {
          continue;
        }
        Cand one = match(cands, pb, List.of(line));
        if (one != null) {
          add(one, crafted, fractured, List.of(line), affixes, implicits, values);
        } else if (s > 1 && looksLikeStat(line)) {
          warnings.add("못 알아본 줄: " + line);
        }
        i++;
      }
    }
    PoeCraftEngine.Item item =
        craft.normalize(
            new PoeCraftEngine.Item(
                itemClass,
                variant,
                influence,
                ilvl,
                PoeCraftEngine.Rarity.valueOf(rarity),
                affixes,
                corrupted,
                implicits,
                quality));
    long pre = item.affixes().stream().filter(a -> "prefix".equals(a.gen())).count();
    long suf = item.affixes().stream().filter(a -> "suffix".equals(a.gen())).count();
    int cap = "MAGIC".equals(rarity) ? 1 : "RARE".equals(rarity) ? 3 : 0;
    if (pre > cap || suf > cap) {
      warnings.add("자리 수가 희귀도 한도를 넘습니다(접두 " + pre + " · 접미 " + suf + ") — 알아본 옵션이 틀렸을 수 있습니다");
    }
    String rareName = "RARE".equals(rarity) && names.size() >= 2 ? names.get(0) : null;
    return new ImportResult(item, base.name(), base.nameKo(), rareName, values, warnings, null);
  }

  /**
   * Path of Building 아이템 글 → 게임 복사 글 모양(10-20 C509). PoB 는 구분선이 없고 "Rarity: RARE" 다음 이름 · 베이스, 메타
   * 줄(Unique ID · Item Level · Quality · Sockets · LevelReq · Implicits: N), 옵션 줄 앞에 {crafted} ·
   * {fractured} · {range:0.5} 같은 머리표를 쓴다. 구분선이 있으면(게임 글) 그대로.
   */
  /**
   * 군집 주얼 변형(C515) — 그 크기 변형 중 인챈트 '작은 패시브 효과' 줄(콜론 있는 줄, 영 · 한)이 글에 모두 있는 것, 여럿이면 줄이 많은 것. 꼬리표
   * (enchant · implicit — PoB 글은 Implicits 로 온다) · 굴림 범위는 떼고 비교. 패시브 수가 최소(변형 첫 줄)와 다르면 안내(옵션 확률엔
   * 영향 없음). 못 찾으면 null.
   */
  private String clusterVariant(
      String itemClass, String baseName, List<List<String>> sections, List<String> warnings) {
    String size =
        "cluster_" + baseName.substring(0, baseName.indexOf(' ')).toLowerCase(Locale.ROOT) + ".";
    java.util.Set<String> seen = new java.util.HashSet<>();
    for (int s = 1; s < sections.size(); s++) {
      for (String line : sections.get(s)) {
        seen.add(RANGE.matcher(TRAIL.matcher(line).replaceAll("")).replaceAll("").strip());
      }
    }
    PoeModDataService.ModVariant best = null;
    int bestLines = 0;
    for (PoeModDataService.ModItemClass c : mods.itemClasses()) {
      if (!c.itemClass().equals(itemClass) || c.variants() == null) {
        continue;
      }
      for (PoeModDataService.ModVariant v : c.variants()) {
        if (!v.key().startsWith(size) || v.enchant() == null) {
          continue;
        }
        for (List<String> lines : java.util.Arrays.asList(v.enchant(), v.enchantKo())) {
          List<String> grant =
              lines == null ? List.of() : lines.stream().filter(l -> l.contains(":")).toList();
          if (!grant.isEmpty() && seen.containsAll(grant) && grant.size() > bestLines) {
            best = v;
            bestLines = grant.size();
          }
        }
      }
    }
    if (best == null) {
      return null;
    }
    // 패시브 수는 게이트가 글에서 읽어 카드에 그대로(C531) — 예전 '최소로 표시' 안내는 뺐다
    return best.key();
  }

  public static String fromPob(String text) {
    List<String> lines = new ArrayList<>();
    for (String raw : text.replace("\r", "").split("\n")) {
      String l = raw.strip();
      if (l.matches("^-{4,}$")) {
        return text;
      }
      if (!l.isEmpty()) {
        lines.add(l);
      }
    }
    int r = -1;
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).regionMatches(true, 0, "Rarity:", 0, 7)) {
        r = i;
        break;
      }
    }
    if (r < 0) {
      return text;
    }
    String rarity = lines.get(r).substring(7).strip().toUpperCase(Locale.ROOT);
    int nameLines = "RARE".equals(rarity) || "UNIQUE".equals(rarity) ? 2 : 1;
    List<String> head =
        new ArrayList<>(lines.subList(0, Math.min(lines.size(), r + 1 + nameLines)));
    List<String> meta = new ArrayList<>();
    List<String> imps = new ArrayList<>();
    List<String> mods = new ArrayList<>();
    List<String> flags = new ArrayList<>();
    int pendingImp = 0;
    for (int i = r + 1 + nameLines; i < lines.size(); i++) {
      String l = lines.get(i);
      Matcher kv = KEY_VALUE.matcher(l);
      if (pendingImp == 0 && kv.matches() && !l.startsWith("{")) {
        if (kv.group(1).strip().equalsIgnoreCase("Implicits")) {
          try {
            pendingImp = Integer.parseInt(kv.group(2).strip());
          } catch (NumberFormatException e) {
            pendingImp = 0;
          }
        } else {
          meta.add(l);
        }
        continue;
      }
      // 머리표 — {crafted} · {fractured} 는 꼬리표로, 나머지({range:…} · {tags:…} · {exarch} 등)는 뗀다
      String tail = "";
      while (l.startsWith("{") && l.indexOf('}') > 0) {
        String tok = l.substring(1, l.indexOf('}')).toLowerCase(Locale.ROOT);
        if (tok.equals("crafted")) {
          tail = " (crafted)";
        } else if (tok.equals("fractured")) {
          tail = " (fractured)";
        }
        l = l.substring(l.indexOf('}') + 1).strip();
      }
      if (pendingImp > 0) {
        imps.add(l + " (implicit)");
        pendingImp--;
      } else if (INFLUENCE.containsKey(l.toLowerCase(Locale.ROOT))
          || l.equalsIgnoreCase("corrupted")
          || l.toLowerCase(Locale.ROOT).endsWith(" item")) {
        flags.add(l);
      } else {
        mods.add(l + tail);
      }
    }
    String sep = "\n--------\n";
    return String.join("\n", head)
        + sep
        + String.join("\n", meta)
        + (imps.isEmpty() ? "" : sep + String.join("\n", imps))
        + sep
        + String.join("\n", mods)
        + (flags.isEmpty() ? "" : sep + String.join("\n", flags))
        + "\n";
  }

  /** 이름 줄에서 베이스 — 마지막 줄 그대로(레어 = 둘째 줄) → 이름 안에 든 가장 긴 베이스(매직 · 일반, "상급 ..."). 영 · 한. */
  PoeBaseItem findBase(List<String> names) {
    List<PoeBaseItem> all = bases.items();
    for (int k = names.size() - 1; k >= 0; k--) {
      String n = names.get(k);
      for (PoeBaseItem b : all) {
        if (n.equalsIgnoreCase(b.name()) || n.equals(b.nameKo())) {
          return b;
        }
      }
    }
    PoeBaseItem best = null;
    for (String n : names) {
      String low = n.toLowerCase(Locale.ROOT);
      for (PoeBaseItem b : all) {
        boolean hit =
            (b.name() != null && containsWord(low, b.name().toLowerCase(Locale.ROOT)))
                || (b.nameKo() != null && !b.nameKo().isBlank() && n.contains(b.nameKo()));
        if (hit && (best == null || length(b) > length(best))) {
          best = b;
        }
      }
    }
    return best;
  }

  private static int length(PoeBaseItem b) {
    return Math.max(
        b.name() == null ? 0 : b.name().length(), b.nameKo() == null ? 0 : b.nameKo().length());
  }

  private static boolean containsWord(String hay, String needle) {
    int from = 0;
    while (true) {
      int at = hay.indexOf(needle, from);
      if (at < 0) {
        return false;
      }
      int end = at + needle.length();
      boolean l = at == 0 || !Character.isLetterOrDigit(hay.charAt(at - 1));
      boolean r = end == hay.length() || !Character.isLetterOrDigit(hay.charAt(end));
      if (l && r) {
        return true;
      }
      from = at + 1;
    }
  }

  /** 요구 속성으로 방어 변형(PoeOptimizeService.attributeVariantOf 와 같은 규칙). */
  static String attributeVariant(PoeBaseItem item) {
    boolean str = item.reqStr() > 0;
    boolean dex = item.reqDex() > 0;
    boolean in = item.reqInt() > 0;
    if (str && dex && in) {
      return "str_dex_int_armour";
    }
    if (str && dex) {
      return "str_dex_armour";
    }
    if (str && in) {
      return "str_int_armour";
    }
    if (dex && in) {
      return "dex_int_armour";
    }
    if (str) {
      return "str_armour";
    }
    if (dex) {
      return "dex_armour";
    }
    if (in) {
      return "int_armour";
    }
    return "";
  }

  private static String catalystKey(String words) {
    String low = words.toLowerCase(Locale.ROOT);
    for (Map.Entry<String, String> e : CATALYST_WORDS) {
      if (low.contains(e.getKey())) {
        return e.getValue();
      }
    }
    return null;
  }

  /** 옵션 머리줄 → 종류 · 자리 · 이름 · 등급. 베이스 고정 속성 · 인챈트 · 고유 옵션은 null(무시). */
  static Block block(String header) {
    Matcher m = AFFIX_EN.matcher(header);
    if (!m.matches()) {
      m = AFFIX_KO.matcher(header);
    }
    if (m.matches()) {
      String mark = m.group(1) == null ? "" : m.group(1).strip();
      boolean crafted = mark.equals("Master Crafted") || mark.equals("대가의 제작");
      boolean fractured = mark.equals("Fractured") || mark.equals("분열된");
      String gen = m.group(2).equals("Prefix") || m.group(2).equals("접두어") ? "prefix" : "suffix";
      Integer tier = m.group(4) == null ? null : Integer.valueOf(m.group(4));
      return new Block("affix", gen, m.group(3), tier, crafted, fractured);
    }
    if (ELDRITCH.matcher(header).matches()) {
      boolean exarch = header.startsWith("Searing") || header.startsWith("작열");
      // 등급 이름(ClientStrings EldritchCurrencyTier1~6) — 같은 문구가 등급마다 있어 이것으로 가른다
      Integer tier = null;
      List<String> en =
          List.of("lesser", "greater", "grand", "exceptional", "exquisite", "perfect");
      List<String> ko = List.of("하급", "상급", "우수한", "특출난", "정교한", "완벽한");
      Matcher tm = Pattern.compile("\\(([^)]+)\\)").matcher(header);
      if (tm.find()) {
        String w = tm.group(1).strip();
        int k = en.indexOf(w.toLowerCase(Locale.ROOT));
        if (k < 0) {
          k = ko.indexOf(w);
        }
        tier = k < 0 ? null : k + 1;
      }
      return new Block("eldritch", exarch ? "exarch" : "eater", null, tier, false, false);
    }
    if (header.startsWith("Corruption Implicit") || header.startsWith("타락 고정")) {
      return new Block("corrupted", PoeCraftEngine.CORRUPTED, null, null, false, false);
    }
    return null;
  }

  /** 장막 줄(ClientStrings ItemDisplayVeiledPrefix/Suffix) — 자리 표시 옵션으로. */
  private static boolean veiled(List<String> stat, Block b, List<PoeCraftEngine.Affix> out) {
    for (String s : stat) {
      String t = TRAIL.matcher(s).replaceAll("").strip();
      if (t.equalsIgnoreCase("Veiled Prefix") || t.equals("장막 접두어")) {
        out.add(
            new PoeCraftEngine.Affix(
                PoeCraftEngine.VEILED_PREFIX, "prefix", List.of(), false, false));
        return true;
      }
      if (t.equalsIgnoreCase("Veiled Suffix") || t.equals("장막 접미어")) {
        out.add(
            new PoeCraftEngine.Affix(
                PoeCraftEngine.VEILED_SUFFIX, "suffix", List.of(), false, false));
        return true;
      }
    }
    return false;
  }

  private static void add(
      Cand c,
      boolean crafted,
      boolean fractured,
      List<String> stat,
      List<PoeCraftEngine.Affix> affixes,
      List<PoeCraftEngine.Affix> implicits,
      Map<String, List<Double>> values) {
    List<Double> nums = new ArrayList<>();
    for (String s : stat) {
      Matcher n = NUMBER.matcher(RANGE.matcher(TRAIL.matcher(s).replaceAll("")).replaceAll(""));
      while (n.find()) {
        nums.add(Double.parseDouble(n.group()));
      }
    }
    if (affixes.stream().anyMatch(a -> a.id().equals(c.id()))
        || implicits.stream().anyMatch(a -> a.id().equals(c.id()))) {
      return;
    }
    values.put(c.id(), nums);
    PoeCraftEngine.Affix a =
        new PoeCraftEngine.Affix(c.id(), c.gen(), List.of(), crafted || c.crafted(), fractured);
    if ("affix".equals(c.kind())) {
      affixes.add(a);
    } else {
      implicits.add(a);
    }
  }

  /** 문구 틀 — 꼬리표 · 굴림 범위를 지우고 숫자를 # 로, 소문자 · 공백 하나. */
  static String template(String line) {
    String t = TRAIL.matcher(line).replaceAll("");
    t = RANGE.matcher(t).replaceAll("");
    t = NUMBER.matcher(t).replaceAll("#");
    return t.replaceAll("\\s+", " ").strip().toLowerCase(Locale.ROOT);
  }

  /** 줄 끝 꼬리표 → 영어 표준 이름(한국어 클라이언트 꼬리표 '제작' · '분열' · '고정' 등도, C491). 숫자 든 괄호(굴림 범위)는 꼬리표가 아니다. */
  private static String trail(String line) {
    Matcher m = TRAIL.matcher(line);
    if (!m.find()) {
      return null;
    }
    String w = m.group(1).strip().toLowerCase(Locale.ROOT);
    return switch (w) {
      case "제작", "제작됨", "대가의 제작" -> "crafted";
      case "분열", "분열됨" -> "fractured";
      case "고정", "고정 속성", "암시" -> "implicit";
      case "인챈트", "속성 부여 인챈트" -> "enchant";
      case "룬" -> "rune";
      default -> w;
    };
  }

  private static boolean isReminder(String line) {
    return line.startsWith("(") && line.endsWith(")");
  }

  /** 옵션이 아닌 줄 — "Armour: 50" 같은 속성, 요구 사항, 영향력 · 타락 표시. */
  private static boolean isMeta(String line) {
    String low = line.toLowerCase(Locale.ROOT);
    return KEY_VALUE.matcher(line).matches()
        || INFLUENCE.containsKey(low)
        || low.equals("corrupted")
        || line.equals("타락")
        || low.endsWith(" item")
        || line.endsWith(" 아이템")
        || isReminder(line);
  }

  private static boolean looksLikeStat(String line) {
    return NUMBER.matcher(line).find() && line.length() < 120;
  }

  /** 이 아이템 풀의 후보 — 일반 옵션 · 에센스 · 제작대 · 해제 · 화석 · 고양 옵션, 엘드리치 · 부패 임플리싯. */
  List<Cand> candidates(PoeCraftEngine.Item item) {
    String pk = item.poolKey();
    List<Cand> out = new ArrayList<>();
    for (String id : data.pool(pk).keySet()) {
      PoeModDataService.ModTier t = mods.tier(id);
      PoeCraftDataService.CraftMod m = data.mod(id);
      if (t != null && m != null) {
        out.add(
            new Cand(
                id,
                m.g(),
                t.name(),
                t.nameKo(),
                t.en(),
                t.enMin(),
                t.ko(),
                t.koMin(),
                data.tierRank(pk, id),
                false,
                "affix",
                0));
      }
    }
    for (PoeCraftDataService.Essence e : data.essences(item.itemClass())) {
      if (e.modId() != null && out.stream().noneMatch(c -> c.id().equals(e.modId()))) {
        out.add(
            new Cand(
                e.modId(),
                e.gen(),
                e.modName(),
                e.modName(),
                e.en(),
                e.enMin(),
                e.ko(),
                e.koMin(),
                null,
                false,
                "affix",
                2));
      }
    }
    for (PoeCraftDataService.BenchCraft b : data.bench(item.itemClass())) {
      out.add(
          new Cand(
              b.modId(),
              b.gen(),
              b.modName(),
              b.modName(),
              b.en(),
              b.enMin(),
              b.ko(),
              b.koMin(),
              null,
              true,
              "affix",
              1));
    }
    addVeiledLike(out, data.veiledPool(pk).keySet(), data::veiledMod, "affix");
    addVeiledLike(out, data.fossilPool(pk).keySet(), data::fossilMod, "affix");
    addVeiledLike(out, data.upgrade(pk).values(), data::elevatedMod, "affix");
    addVeiledLike(out, data.corruptedPool(pk).keySet(), data::corruptedMod, "corrupted");
    for (String id : data.eldritchPool(pk).keySet()) {
      PoeCraftDataService.EldritchMod m = data.eldritchMod(id);
      if (m != null) {
        out.add(
            new Cand(
                id,
                m.g(),
                m.name(),
                m.nameKo(),
                m.en(),
                m.enMin(),
                m.ko(),
                m.koMin(),
                m.tier(),
                false,
                "eldritch",
                0));
      }
    }
    return out;
  }

  private static void addVeiledLike(
      List<Cand> out,
      java.util.Collection<String> ids,
      java.util.function.Function<String, PoeCraftDataService.VeiledMod> get,
      String kind) {
    for (String id : ids) {
      PoeCraftDataService.VeiledMod m = get.apply(id);
      if (m != null && out.stream().noneMatch(c -> c.id().equals(id))) {
        out.add(
            new Cand(
                id,
                "corrupted".equals(kind) ? PoeCraftEngine.CORRUPTED : m.g(),
                m.name(),
                m.nameKo(),
                m.en(),
                m.enMin(),
                m.ko(),
                m.koMin(),
                null,
                false,
                kind,
                "corrupted".equals(kind) ? 0 : 3));
      }
    }
  }

  /**
   * 머리줄 · 문구에 맞는 후보 — 문구 틀이 같고(영 또는 한), 머리줄이 있으면 자리 · 이름이 같아야. 같은 틀이 여럿(등급 사다리)이면 머리줄 등급 → 수치가 범위 안
   * → 이름 순.
   */
  Cand match(List<Cand> cands, Block b, List<String> stat) {
    if (stat.isEmpty()) {
      return null;
    }
    List<String> want = stat.stream().map(PoeCraftImportService::template).sorted().toList();
    boolean implicit = "implicit".equals(b.kind());
    List<Cand> hit = new ArrayList<>();
    for (Cand c : cands) {
      if (implicit) {
        if (c.kind().equals("affix")) {
          continue;
        }
      } else if (!c.kind().equals(b.kind())) {
        continue;
      }
      if (b.gen() != null && !b.gen().equals(c.gen())) {
        continue;
      }
      if (b.crafted() != c.crafted() && "affix".equals(b.kind()) && b.name() != null) {
        continue;
      }
      if (sameTemplates(want, c.en()) || sameTemplates(want, c.ko())) {
        hit.add(c);
      }
    }
    if (hit.isEmpty() && b.name() != null && b.gen() != null) {
      // 문구 틀이 안 맞아도(번역 · 표기 차이) 머리줄 이름 · 자리가 맞으면 — 같은 이름이 여럿이면 등급으로
      for (Cand c : cands) {
        if (c.kind().equals(b.kind()) && b.gen().equals(c.gen()) && nameHit(b, c)) {
          hit.add(c);
        }
      }
      if (hit.size() > 1 && b.tier() != null) {
        List<Cand> t = hit.stream().filter(c -> b.tier().equals(c.tierRank())).toList();
        if (!t.isEmpty()) {
          hit = new ArrayList<>(t);
        }
      }
    }
    if (hit.isEmpty()) {
      return null;
    }
    List<Double> nums = new ArrayList<>();
    for (String s : stat) {
      Matcher n = NUMBER.matcher(RANGE.matcher(TRAIL.matcher(s).replaceAll("")).replaceAll(""));
      while (n.find()) {
        nums.add(Math.abs(Double.parseDouble(n.group())));
      }
    }
    return hit.stream()
        .max(
            Comparator.comparingInt((Cand c) -> nameHit(b, c) ? 1 : 0)
                .thenComparingInt(
                    c -> b.tier() != null && b.tier().equals(c.tierRank()) && !c.crafted() ? 1 : 0)
                .thenComparingInt(c -> inRange(c, stat, nums) ? 1 : 0)
                .thenComparingInt(c -> -c.prio())
                .thenComparingInt(c -> c.crafted() == b.crafted() ? 1 : 0)
                .thenComparingInt(c -> c.tierRank() == null ? 0 : -c.tierRank()))
        .orElse(null);
  }

  private static boolean nameHit(Block b, Cand c) {
    if (b.name() == null) {
      return false;
    }
    String n = bareName(b.name());
    return n.equalsIgnoreCase(bareName(c.name())) || n.equals(bareName(c.nameKo()));
  }

  /** 접미 이름 앞 "- "(한국어 데이터 표기)는 복사 글에 없을 수도 있어 떼고 비교. */
  private static String bareName(String name) {
    return name == null ? "" : name.replaceFirst("^-\\s*", "").strip();
  }

  private static boolean sameTemplates(List<String> want, List<String> lines) {
    if (lines == null || lines.size() != want.size()) {
      return false;
    }
    return lines.stream().map(PoeCraftImportService::template).sorted().toList().equals(want);
  }

  private static boolean fits(Cand c, List<String> stat) {
    List<Double> nums = new ArrayList<>();
    for (String s : stat) {
      Matcher n = NUMBER.matcher(RANGE.matcher(TRAIL.matcher(s).replaceAll("")).replaceAll(""));
      while (n.find()) {
        nums.add(Math.abs(Double.parseDouble(n.group())));
      }
    }
    return inRange(c, stat, nums);
  }

  /** 복사 글 수치가 후보의 최소 ~ 최대 굴림 안인지(같은 문구 틀의 줄끼리, 순서대로). */
  private static boolean inRange(Cand c, List<String> stat, List<Double> nums) {
    List<String> hi =
        sameTemplates(stat.stream().map(PoeCraftImportService::template).sorted().toList(), c.en())
            ? c.en()
            : c.ko();
    List<String> lo =
        hi == c.en()
            ? (c.enMin() == null ? c.en() : c.enMin())
            : (c.koMin() == null ? c.ko() : c.koMin());
    List<Double> max = new ArrayList<>();
    List<Double> min = new ArrayList<>();
    for (String s : stat) {
      String t = template(s);
      int k = -1;
      for (int x = 0; x < hi.size(); x++) {
        if (template(hi.get(x)).equals(t)) {
          k = x;
          break;
        }
      }
      if (k < 0) {
        return false;
      }
      Matcher a = NUMBER.matcher(hi.get(k));
      Matcher z = NUMBER.matcher(k < lo.size() ? lo.get(k) : hi.get(k));
      while (a.find()) {
        max.add(Math.abs(Double.parseDouble(a.group())));
        min.add(z.find() ? Math.abs(Double.parseDouble(z.group())) : max.get(max.size() - 1));
      }
    }
    if (max.size() != nums.size()) {
      return false;
    }
    for (int x = 0; x < nums.size(); x++) {
      double l = Math.min(min.get(x), max.get(x));
      double h = Math.max(min.get(x), max.get(x));
      if (nums.get(x) < l - 1e-9 || nums.get(x) > h + 1e-9) {
        return false;
      }
    }
    return true;
  }
}
