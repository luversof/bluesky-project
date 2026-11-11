package net.luversof.api.poe.poe2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * PoE2 게임 아이템 복사 글 → 제작 에뮬레이터 아이템(10-20 C486, PoE1 C485 와 같은 방식). 베이스 → 옵션 풀(mods.json
 * pools.bases), 옵션은 머리줄 이름 · 자리(Ctrl+Alt+C) 또는 문구 틀 + 수치 범위(Ctrl+C)로 찾는다. 영어 · 한국어.
 */
@Service
public class Poe2CraftImportService {

  /** 가져온 결과 — error 면 item 은 null, values = 옵션 id → 복사 글 수치(줄 순서). */
  public record ImportResult(
      Poe2CraftEngine.Item item,
      String baseName,
      String baseNameKo,
      // 레어 이름(복사 글 첫 이름 줄, C501) — 레어가 아니면 null
      String itemName,
      Map<String, List<Double>> values,
      List<String> warnings,
      String error) {

    static ImportResult fail(String error) {
      return new ImportResult(null, null, null, null, Map.of(), List.of(), error);
    }
  }

  /** 후보 — kind = affix | corrupted, prio = 같은 문구일 때 먼저 볼 순서(0 일반 · 1 에센스 · 2 훼손). */
  record Cand(
      String id,
      String gen,
      int level,
      String name,
      String nameKo,
      List<String> en,
      List<String> ko,
      Integer tierRank,
      String kind,
      int prio) {}

  record Block(String kind, String gen, String name, Integer tier, boolean fractured) {}

  private static final Pattern HEADER = Pattern.compile("^\\{\\s*(.*?)\\s*\\}$");

  private static final Pattern AFFIX_EN =
      Pattern.compile(
          "^(Fractured |Desecrated )?(Prefix|Suffix) Modifier \"(.*?)\"(?:\\s*\\((?:Tier|Rank): (\\d+)\\))?.*$");

  private static final Pattern AFFIX_KO =
      Pattern.compile("^(분열된 |훼손된 )?(접두어|접미어) 속성 부여 \"(.*?)\"(?:\\s*\\((?:등급|단계): (\\d+)\\))?.*$");

  private static final Pattern KEY_VALUE = Pattern.compile("^([^:{]{1,40}):\\s*(.*)$");

  /** 굴림 범위 — 앞에 숫자가 붙으면 고급 복사의 "45(39-44)"(지운다), 아니면 자료의 "(39-44)"(# 하나). */
  private static final Pattern RANGE =
      Pattern.compile("(\\d)?\\(\\s*[+-]?\\d+(?:\\.\\d+)?\\s*[-–]\\s*[+-]?\\d+(?:\\.\\d+)?\\s*\\)");

  /** 한국어 자료의 범위 "45~50" — # 하나(PoE2 textKo). */
  private static final Pattern KO_RANGE = Pattern.compile("\\d+(?:\\.\\d+)?~\\d+(?:\\.\\d+)?");

  private static final Pattern NUMBER = Pattern.compile("[+-]?\\d+(?:\\.\\d+)?");

  private static final Pattern TRAIL = Pattern.compile("\\s*\\(([^()\\d]{1,16})\\)\\s*$");

  private final Poe2CraftService craft;
  private final Poe2DataService data2;

  public Poe2CraftImportService(Poe2CraftService craft, Poe2DataService data2) {
    this.craft = craft;
    this.data2 = data2;
  }

  public ImportResult parse(String text) {
    if (text == null || text.isBlank()) {
      return ImportResult.fail("붙여 넣은 글이 없습니다");
    }
    // Path of Building 2 아이템 글도(C509 — PoE1 과 같은 모양)
    text = net.luversof.api.poe.service.PoeCraftImportService.fromPob(text);
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
    List<String> warnings = new ArrayList<>();
    String rarity = null;
    List<String> names = new ArrayList<>();
    for (String line : sections.get(0)) {
      Matcher kv = KEY_VALUE.matcher(line);
      if (kv.matches()) {
        String key = kv.group(1).strip().toLowerCase(Locale.ROOT);
        if (key.equals("rarity") || key.equals("아이템 희귀도") || key.equals("희귀도")) {
          rarity =
              switch (kv.group(2).strip().toLowerCase(Locale.ROOT)) {
                case "normal", "일반" -> "NORMAL";
                case "magic", "마법" -> "MAGIC";
                case "rare", "희귀" -> "RARE";
                case "unique", "고유" -> "UNIQUE";
                default -> null;
              };
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
    Poe2.ModPool pool = null;
    String baseName = null;
    String baseNameKo = null;
    for (int k = names.size() - 1; k >= 0 && pool == null; k--) {
      String n = names.get(k);
      Optional<Poe2.ModPool> p = data2.modPoolForBase(n);
      if (p.isEmpty()) {
        Poe2.BaseItem ko =
            data2.searchBases(n, null, null).stream()
                .filter(b -> n.equals(b.nameKo()) || n.equalsIgnoreCase(b.name()))
                .findFirst()
                .orElse(null);
        if (ko != null) {
          p = data2.modPoolForBase(ko.name());
          baseName = ko.name();
          baseNameKo = ko.nameKo();
        }
      } else {
        baseName = n;
      }
      pool = p.orElse(null);
    }
    if (pool == null) {
      // 매직 · 일반 — 이름 안에 든 가장 긴 베이스
      String best = null;
      Poe2.ModPool bestPool = null;
      for (String n : names) {
        for (Poe2.BaseItem b : data2.searchBases("", null, null)) {
          boolean hit =
              (b.name() != null
                      && n.toLowerCase(Locale.ROOT).contains(b.name().toLowerCase(Locale.ROOT)))
                  || (b.nameKo() != null && !b.nameKo().isBlank() && n.contains(b.nameKo()));
          if (hit && (best == null || b.name().length() > best.length())) {
            Optional<Poe2.ModPool> p = data2.modPoolForBase(b.name());
            if (p.isPresent()) {
              best = b.name();
              baseNameKo = b.nameKo();
              bestPool = p.get();
            }
          }
        }
      }
      pool = bestPool;
      baseName = best;
    }
    if (pool == null) {
      return ImportResult.fail("베이스를 찾지 못했습니다: " + String.join(" / ", names));
    }
    int ilvl = 82;
    boolean corrupted = false;
    for (int s = 1; s < sections.size(); s++) {
      for (String line : sections.get(s)) {
        Matcher kv = KEY_VALUE.matcher(line);
        if (kv.matches() && !line.startsWith("{")) {
          String key = kv.group(1).strip().toLowerCase(Locale.ROOT);
          if (key.equals("item level") || key.equals("아이템 레벨")) {
            Matcher n = NUMBER.matcher(kv.group(2));
            if (n.find()) {
              ilvl = Math.max(1, Math.min(100, (int) Double.parseDouble(n.group())));
            }
          }
        } else if (line.equalsIgnoreCase("corrupted") || line.equals("타락")) {
          corrupted = true;
        } else if (line.equalsIgnoreCase("unidentified") || line.equals("미확인")) {
          return ImportResult.fail("미확인 아이템은 옵션을 알 수 없습니다 — 감정한 뒤 복사하세요");
        }
      }
    }
    List<Cand> cands = candidates(pool);
    List<Poe2CraftEngine.Affix> affixes = new ArrayList<>();
    Map<String, List<Double>> values = new LinkedHashMap<>();
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
            if (!(sec.get(j).startsWith("(") && sec.get(j).endsWith(")"))) {
              stat.add(sec.get(j));
            }
            j++;
          }
          i = j;
          if (unrevealed(stat, affixes) || b == null) {
            continue;
          }
          Cand c = match(cands, b, stat);
          if (c == null) {
            warnings.add("못 알아본 옵션: " + String.join(" / ", stat));
          } else {
            add(c, b.fractured(), stat, affixes, values);
          }
          continue;
        }
        if (KEY_VALUE.matcher(line).matches()
            || line.equalsIgnoreCase("corrupted")
            || line.equals("타락")
            || (line.startsWith("(") && line.endsWith(")"))) {
          i++;
          continue;
        }
        String tag = trail(line);
        if ("implicit".equals(tag) || "rune".equals(tag)) {
          i++;
          continue;
        }
        if (unrevealed(List.of(line), affixes)) {
          i++;
          continue;
        }
        boolean fractured = "fractured".equals(tag);
        String kind = "enchant".equals(tag) ? "corrupted" : "affix";
        Block pb = new Block(kind, null, null, null, fractured);
        Cand two =
            i + 1 < sec.size() && trail(sec.get(i + 1)) == null
                ? match(cands, pb, List.of(line, sec.get(i + 1)))
                : null;
        Cand one = match(cands, pb, List.of(line));
        if (two != null
            && fits(two, List.of(line, sec.get(i + 1)))
            && (one == null || two.prio() <= one.prio())) {
          add(two, fractured, List.of(line, sec.get(i + 1)), affixes, values);
          i += 2;
          continue;
        }
        if (one != null) {
          add(one, fractured, List.of(line), affixes, values);
        } else if (s > 1 && NUMBER.matcher(line).find() && line.length() < 120) {
          warnings.add("못 알아본 줄: " + line);
        }
        i++;
      }
    }
    Poe2CraftEngine.Item item =
        craft.normalize(
            new Poe2CraftEngine.Item(
                pool.key(),
                ilvl,
                Poe2CraftEngine.Rarity.valueOf(rarity),
                affixes,
                corrupted,
                null,
                false));
    String rareName = "RARE".equals(rarity) && names.size() >= 2 ? names.get(0) : null;
    return new ImportResult(item, baseName, baseNameKo, rareName, values, warnings, null);
  }

  static Block block(String header) {
    Matcher m = AFFIX_EN.matcher(header);
    if (!m.matches()) {
      m = AFFIX_KO.matcher(header);
    }
    if (m.matches()) {
      String mark = m.group(1) == null ? "" : m.group(1).strip();
      boolean fractured = mark.equals("Fractured") || mark.equals("분열된");
      String gen = m.group(2).equals("Prefix") || m.group(2).equals("접두어") ? "prefix" : "suffix";
      Integer tier = m.group(4) == null ? null : Integer.valueOf(m.group(4));
      return new Block("affix", gen, m.group(3), tier, fractured);
    }
    if (header.startsWith("Corruption") || header.startsWith("타락")) {
      return new Block("corrupted", "corrupted", null, null, false);
    }
    return null;
  }

  /** 미공개 훼손 줄(ClientStrings 훼손된 접두어 · 접미어) — 자리 표시 옵션으로. */
  private static boolean unrevealed(List<String> stat, List<Poe2CraftEngine.Affix> out) {
    for (String s : stat) {
      String t = TRAIL.matcher(s).replaceAll("").strip();
      boolean pre = t.equalsIgnoreCase("Desecrated Prefix") || t.equals("훼손된 접두어");
      boolean suf = t.equalsIgnoreCase("Desecrated Suffix") || t.equals("훼손된 접미어");
      if (pre || suf) {
        out.add(
            new Poe2CraftEngine.Affix(
                Poe2CraftEngine.UNREVEALED + (pre ? "P" : "S"),
                pre ? "prefix" : "suffix",
                1,
                List.of(-3),
                false));
        return true;
      }
    }
    return false;
  }

  private static void add(
      Cand c,
      boolean fractured,
      List<String> stat,
      List<Poe2CraftEngine.Affix> affixes,
      Map<String, List<Double>> values) {
    if (affixes.stream().anyMatch(a -> a.id().equals(c.id()))) {
      return;
    }
    values.put(c.id(), numbers(stat));
    affixes.add(new Poe2CraftEngine.Affix(c.id(), c.gen(), c.level(), List.of(), fractured));
  }

  private static List<Double> numbers(List<String> stat) {
    List<Double> nums = new ArrayList<>();
    for (String s : stat) {
      String t = TRAIL.matcher(s).replaceAll("");
      t = RANGE.matcher(t).replaceAll(r -> r.group(1) == null ? "" : r.group(1));
      Matcher n = NUMBER.matcher(t);
      while (n.find()) {
        nums.add(Double.parseDouble(n.group()));
      }
    }
    return nums;
  }

  /** 문구 틀 — 꼬리표를 지우고, 굴림 범위 · 숫자를 # 로. */
  static String template(String line) {
    String t = TRAIL.matcher(line).replaceAll("");
    t = RANGE.matcher(t).replaceAll(r -> r.group(1) == null ? "#" : r.group(1));
    t = KO_RANGE.matcher(t).replaceAll("#");
    t = NUMBER.matcher(t).replaceAll("#");
    // 한국어 자료 "(20~24)~(33~36)" · 복사 글 "22~35" 를 같은 틀로 — 괄호 친 # 와 "# ~ #" 를 # 하나로
    t = t.replace("(#)", "#").replaceAll("#\\s*~\\s*#", "#").replaceAll("[+-]#", "#");
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

  List<Cand> candidates(Poe2.ModPool pool) {
    List<Cand> out = new ArrayList<>();
    for (Poe2.ModGroup g : pool.groups() == null ? List.<Poe2.ModGroup>of() : pool.groups()) {
      for (int i = 0; i < g.tiers().size(); i++) {
        Poe2.ModTier t = g.tiers().get(i);
        out.add(
            new Cand(
                t.id(),
                g.gen(),
                t.level() == null ? 1 : t.level(),
                t.name(),
                t.nameKo(),
                t.text(),
                t.textKo(),
                i + 1,
                "affix",
                0));
      }
    }
    Poe2CraftService.CraftData d = craft.craftData();
    if (d.essenceMods() != null) {
      d.essenceMods()
          .forEach(
              (id, m) -> {
                if (out.stream().noneMatch(c -> c.id().equals(id))) {
                  out.add(
                      new Cand(
                          id,
                          m.g(),
                          m.l(),
                          m.name(),
                          m.nameKo(),
                          m.en(),
                          m.ko(),
                          null,
                          "affix",
                          1));
                }
              });
    }
    List<String> des = d.desecrated() == null ? null : d.desecrated().get(pool.key());
    for (String id : des == null ? List.<String>of() : des) {
      Poe2CraftService.DesecratedModData m = d.desecratedMods().get(id);
      if (m != null) {
        out.add(new Cand(id, m.g(), m.l(), m.name(), m.nameKo(), m.en(), m.ko(), null, "affix", 2));
      }
    }
    List<String> cor = d.corrupted() == null ? null : d.corrupted().get(pool.key());
    for (String id : cor == null ? List.<String>of() : cor) {
      Poe2CraftService.CorruptedModData m = d.corruptedMods().get(id);
      if (m != null) {
        out.add(
            new Cand(
                id, "corrupted", m.l(), "Corrupted", "타락", m.en(), m.ko(), null, "corrupted", 0));
      }
    }
    return out;
  }

  Cand match(List<Cand> cands, Block b, List<String> stat) {
    if (stat.isEmpty()) {
      return null;
    }
    List<String> want = stat.stream().map(Poe2CraftImportService::template).sorted().toList();
    List<Cand> hit = new ArrayList<>();
    for (Cand c : cands) {
      if (!c.kind().equals(b.kind()) || (b.gen() != null && !b.gen().equals(c.gen()))) {
        continue;
      }
      if (same(want, c.en()) || same(want, c.ko())) {
        hit.add(c);
      }
    }
    if (hit.isEmpty() && b.name() != null && b.gen() != null) {
      for (Cand c : cands) {
        if (c.kind().equals(b.kind()) && b.gen().equals(c.gen()) && nameHit(b, c)) {
          hit.add(c);
        }
      }
    }
    if (hit.isEmpty()) {
      return null;
    }
    return hit.stream()
        .max(
            Comparator.comparingInt((Cand c) -> nameHit(b, c) ? 1 : 0)
                .thenComparingInt(c -> b.tier() != null && b.tier().equals(c.tierRank()) ? 1 : 0)
                .thenComparingInt(c -> fits(c, stat) ? 1 : 0)
                .thenComparingInt(c -> -c.prio())
                .thenComparingInt(c -> c.tierRank() == null ? 0 : -c.tierRank()))
        .orElse(null);
  }

  private static boolean nameHit(Block b, Cand c) {
    if (b.name() == null) {
      return false;
    }
    String n = b.name().replaceFirst("^-\\s*", "").strip();
    return n.equalsIgnoreCase(c.name() == null ? "" : c.name().replaceFirst("^-\\s*", "").strip())
        || n.equals(c.nameKo() == null ? "" : c.nameKo().replaceFirst("^-\\s*", "").strip());
  }

  private static boolean same(List<String> want, List<String> lines) {
    if (lines == null || lines.size() != want.size()) {
      return false;
    }
    return lines.stream().map(Poe2CraftImportService::template).sorted().toList().equals(want);
  }

  /** 수치가 영어 자료의 범위 "(a-b)" 안인지 — 줄을 문구 틀로 맞춰 순서대로. */
  static boolean fits(Cand c, List<String> stat) {
    if (c.en() == null) {
      return false;
    }
    List<Double> nums = numbers(stat);
    List<double[]> ranges = new ArrayList<>();
    for (String s : stat) {
      String t = template(s);
      String src = c.en().stream().filter(x -> template(x).equals(t)).findFirst().orElse(null);
      if (src == null) {
        return false;
      }
      Matcher m =
          Pattern.compile(
                  "\\(\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*[-–]\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*\\)|([+-]?\\d+(?:\\.\\d+)?)")
              .matcher(src);
      while (m.find()) {
        if (m.group(1) != null) {
          double a = Math.abs(Double.parseDouble(m.group(1)));
          double z = Math.abs(Double.parseDouble(m.group(2)));
          ranges.add(new double[] {Math.min(a, z), Math.max(a, z)});
        } else {
          double v = Math.abs(Double.parseDouble(m.group(3)));
          ranges.add(new double[] {v, v});
        }
      }
    }
    if (ranges.size() != nums.size()) {
      return false;
    }
    for (int i = 0; i < nums.size(); i++) {
      double v = Math.abs(nums.get(i));
      if (v < ranges.get(i)[0] - 1e-9 || v > ranges.get(i)[1] + 1e-9) {
        return false;
      }
    }
    return true;
  }
}
