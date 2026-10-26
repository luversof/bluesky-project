package net.luversof.web.gate.poe.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.poe.dto.PoeCraft;
import net.luversof.web.gate.poe.dto.PoeModItemClass;
import net.luversof.web.gate.poe.httpexchange.PoeCraftClient;
import net.luversof.web.gate.poe.httpexchange.PoeDataClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * PoE1 제작 에뮬레이터 화면(10-15 C335 — 사용자 요청 "직접 아이템을 제작해 보는 에뮬레이터"). 아이템 상태와 되돌리기 이력은 화면이 숨은 칸으로 들고
 * 다니고(서버는 상태를 안 든다), 화폐를 누르면 결과 구역만 바꿔 끼운다.
 */
@Controller
public class PoeCraftController {

  /** 되돌리기 이력 상한 — 숨은 칸이 끝없이 커지지 않게. */
  // 단계 기록에서 아무 단계로 되돌리기(C455 — PoE2 C453 와 같게 100)
  static final int HISTORY_MAX = 100;

  /** 최적 조합 시간 예산(C457k) — 백그라운드 작업이라 게이트 읽기 제한과 무관, 화면은 조회하며 기다린다. */
  static final int OPTIMIZE_BUDGET_MS = 30_000;

  private static final List<String> ACTIONS =
      List.of(
          "TRANSMUTE",
          "ALTERATION",
          "AUGMENTATION",
          "REGAL",
          "ALCHEMY",
          "CHAOS",
          "EXALT",
          "ANNUL",
          "SCOUR",
          "ESSENCE",
          "FOSSIL",
          "BENCH",
          "BENCH_REMOVE",
          "HARVEST",
          "CONQUEROR",
          "FRACTURE",
          "VEILED_CHAOS",
          "VEILED_EXALT",
          "UNVEIL",
          "EMBER",
          "ICHOR",
          "ELDRITCH_CHAOS",
          "ELDRITCH_EXALT",
          "ELDRITCH_ANNUL",
          "DOMINANCE",
          "AWAKENER",
          "MANUAL_ADD",
          "MANUAL_REMOVE",
          "CATALYST",
          "BEAST",
          "DUCAT",
          "CONFLICT",
          "FOULBORN_AUG",
          "FOULBORN_REGAL",
          "FOULBORN_EXALT",
          "VAAL",
          "TAINTED_CHAOS",
          "TAINTED_EXALT",
          "TAINTED_DIVINE");

  private final PoeCraftClient craftClient;
  private final PoeDataClient dataClient;
  private final JsonMapper json = JsonMapper.builder().build();

  public PoeCraftController(
      PoeCraftClient craftClient,
      PoeDataClient dataClient,
      net.luversof.web.gate.poe.httpexchange.PoeBuildClient buildClient) {
    this.craftClient = craftClient;
    this.dataClient = dataClient;
    this.buildClient = buildClient;
  }

  private final net.luversof.web.gate.poe.httpexchange.PoeBuildClient buildClient;

  /** PoB 빌드 코드(C525) — 공백 없는 base64(url) 긴 글. */
  public static boolean buildCode(String text) {
    String t = text == null ? "" : text.strip();
    return t.length() >= 200 && t.matches("[A-Za-z0-9_+/=\\-]+");
  }

  /**
   * 빌드 아이템 → PoB 아이템 글(C525) — 붙여넣기 해석기(PoeCraftImportService.fromPob)가 읽는 모양: Rarity · 이름 · 베이스 ·
   * Quality · Implicits: N · {crafted}/{fractured} 머리표 · Corrupted · 영향력 줄.
   */
  public static String pobText(net.luversof.web.gate.poe.dto.PoeBuild.BuildItem bi) {
    StringBuilder sb = new StringBuilder("Rarity: ").append(bi.rarity()).append('\n');
    String name = bi.name() == null ? "" : bi.name().strip();
    if ("RARE".equals(bi.rarity())) {
      sb.append(name.isEmpty() ? "Rare Item" : name)
          .append('\n')
          .append(bi.baseType())
          .append('\n');
    } else if ("MAGIC".equals(bi.rarity()) && !name.isEmpty()) {
      sb.append(name).append('\n');
    } else {
      sb.append(bi.baseType()).append('\n');
    }
    if (bi.quality() > 0) {
      sb.append("Quality: ").append(bi.quality()).append('\n');
    }
    List<String> imp = bi.implicitLines() == null ? List.of() : bi.implicitLines();
    sb.append("Implicits: ").append(imp.size()).append('\n');
    for (int i = 0; i < imp.size(); i++) {
      String k = kindAt(bi.implicitKinds(), i);
      sb.append("crafted".equals(k) || "enchant".equals(k) ? "{crafted}" : "")
          .append(imp.get(i))
          .append('\n');
    }
    List<String> mods = bi.modLines() == null ? List.of() : bi.modLines();
    for (int i = 0; i < mods.size(); i++) {
      String k = kindAt(bi.modKinds(), i);
      sb.append("crafted".equals(k) ? "{crafted}" : "fractured".equals(k) ? "{fractured}" : "")
          .append(mods.get(i))
          .append('\n');
    }
    if (bi.corrupted()) {
      sb.append("Corrupted").append('\n');
    }
    java.util.Map<String, String> inf =
        java.util.Map.of(
            "shaper", "Shaper Item",
            "elder", "Elder Item",
            "crusader", "Crusader Item",
            "hunter", "Hunter Item",
            "redeemer", "Redeemer Item",
            "warlord", "Warlord Item");
    for (String f : bi.influences() == null ? List.<String>of() : bi.influences()) {
      if (inf.containsKey(f)) {
        sb.append(inf.get(f)).append('\n');
      }
    }
    return sb.toString();
  }

  private static String kindAt(List<String> kinds, int i) {
    return kinds != null && i < kinds.size() && kinds.get(i) != null ? kinds.get(i) : "";
  }

  @GetMapping(value = "/poe/craft", produces = MediaType.TEXT_HTML_VALUE)
  public String page(
      @RequestParam(required = false) String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      @RequestParam(required = false, defaultValue = "84") int ilvl,
      // 가져온 아이템(C485) — 아이템 JSON · 굴림 씨앗 JSON · 못 알아본 줄 안내. 같은 종류 · 변형 · 영향력 · 아이템 레벨일 때만
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String rolls,
      @RequestParam(required = false) String importNote,
      // 베이스(C487, craftofexile 처럼 베이스를 고른다 — 옵션 풀은 종류 · 변형이 정하고, 베이스는 카드 이름 · 표시) · 가져온 레어 이름
      @RequestParam(required = false) String base,
      @RequestParam(required = false) String itemName,
      // 레어 이름 한국어(C518 — 공유 · 보관 링크, 없으면 itemName 그대로)
      @RequestParam(required = false) String itemNameKo,
      // 군집 주얼 패시브 수(C531, 크기마다 범위 — 없으면 최소)
      @RequestParam(required = false) Integer passives,
      Model model) {
    List<PoeModItemClass> classes = dataClient.modItemClasses();
    PoeModItemClass active =
        classes.stream()
            .filter(c -> c.itemClass().equals(itemClass))
            .findFirst()
            .orElse(
                classes.stream()
                    .filter(c -> "Body Armour".equals(c.itemClass()))
                    .findFirst()
                    .orElse(classes.isEmpty() ? null : classes.get(0)));
    String v = variant;
    if (active != null
        && !active.variants().isEmpty()
        && active.variants().stream().noneMatch(x -> x.key().equals(variant))) {
      v = active.variants().get(0).key();
    }
    model.addAttribute("patch", dataClient.gemMeta().patch());
    model.addAttribute("modClasses", classes);
    model.addAttribute("activeClass", active == null ? "" : active.itemClass());
    model.addAttribute("activeVariant", v);
    model.addAttribute("activeInfluence", influence);
    model.addAttribute("ilvl", Math.max(1, Math.min(100, ilvl)));
    model.addAttribute("classLabel", active == null ? "" : label(active));
    String vKey = v;
    List<net.luversof.web.gate.poe.dto.PoeBaseItem> bases =
        active == null
            ? List.of()
            : basesFor(
                active.itemClass(),
                active.variants().isEmpty() ? "" : vKey,
                active.variants().stream()
                    .filter(x -> x.key().equals(vKey))
                    .map(PoeModItemClass.Variant::bases)
                    .findFirst()
                    .orElse(null));
    net.luversof.web.gate.poe.dto.PoeBaseItem chosen =
        bases.stream()
            .filter(b -> b.name().equals(base))
            .findFirst()
            .orElse(bases.isEmpty() ? null : bases.get(0));
    model.addAttribute("bases", bases);
    model.addAttribute("craftBase", chosen);
    model.addAttribute("craftPriceList", priceList(active == null ? "" : active.itemClass(), v));
    model.addAttribute(
        "craftEnchant", enchantOf(classes, active == null ? "" : active.itemClass(), v, passives));
    model.addAttribute("passives", clusterRange(v) == null ? null : clusterPassives(v, passives));
    model.addAttribute("baseName", chosen == null ? "" : chosen.name());
    model.addAttribute("baseNameKo", chosen == null ? "" : nz(chosen.nameKo()));
    model.addAttribute("itemName", "");
    model.addAttribute("itemNameKo", "");
    if (active != null) {
      // 계산기 목표 고르기 — 일반 → 레어 기준 전체 풀(지금 에뮬레이터 상태와 무관, 10-15 C336)
      model.addAttribute(
          "calcPool",
          craftClient.pool(
              new PoeCraft.PoolRequest(
                  new PoeCraft.Item(
                      active.itemClass(), v, influence, clamp(ilvl), "NORMAL", List.of(), false),
                  "RARE")));
      // 장막 해제 옵션 목표(C421) — 못 읽으면 칸을 그리지 않는다
      List<PoeCraft.VeiledLine> veiled;
      try {
        veiled = craftClient.veiled(active.itemClass(), v == null ? "" : v);
      } catch (RuntimeException e) {
        veiled = List.of();
      }
      model.addAttribute("calcVeiled", veiled);
      // 계산기 목표 칸 덧붙임(10-21 C548 사용자 요청 "타락 · 정복자 · 제작 옵션이 안 보인다") — 못 읽으면 칸을 그리지 않는다
      try {
        model.addAttribute(
            "calcExtras",
            craftClient.calcExtras(
                active.itemClass(),
                v == null ? "" : v,
                influence == null ? "" : influence,
                clamp(ilvl)));
      } catch (RuntimeException e) {
        // 덧붙임 없이
      }
    }
    if (active != null) {
      PoeCraft.Item start =
          new PoeCraft.Item(
              active.itemClass(), v, influence, clamp(ilvl), "NORMAL", List.of(), false);
      PoeCraft.Item imported = state == null || state.isBlank() ? null : parse(state, null);
      if (imported != null && same(imported, start)) {
        start = imported;
        rollsModel(
            model, ids(start), fixedIds(start), parseRolls(rolls == null ? "{}" : rolls), false);
        model.addAttribute("importNote", importNote);
        model.addAttribute("imported", true);
        model.addAttribute("itemName", nz(itemName));
        model.addAttribute(
            "itemNameKo", itemNameKo == null || itemNameKo.isBlank() ? nz(itemName) : itemNameKo);
      }
      fill(model, start, List.of(), null, null);
    }
    return "poe/craft";
  }

  /**
   * 제작할 아이템 검색(10-21 C527, craftofexile 'Search for a craftable item') — 베이스 이름(영 · 한)으로 종류 · 변형 ·
   * 베이스를 한 번에 연다. 변형은 변형의 베이스 목록(주얼 색 · 심연 · 군집 = 첫 인챈트) → 속성 변형(방어구) 순, 제작 대상 아닌 베이스(고유 전용 등)는 뺀다.
   */
  @GetMapping(value = "/poe/htmx/craft/base-search", produces = MediaType.TEXT_HTML_VALUE)
  public String baseSearch(
      @RequestParam(required = false, defaultValue = "") String q, Model model) {
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    List<String[]> hits = new ArrayList<>();
    String term = q.strip();
    if (term.length() >= 2) {
      List<PoeModItemClass> classes;
      List<net.luversof.web.gate.poe.dto.PoeBaseItem> found;
      try {
        classes = dataClient.modItemClasses();
        found = dataClient.searchBaseItems(term, null);
      } catch (RuntimeException e) {
        classes = List.of();
        found = List.of();
      }
      for (net.luversof.web.gate.poe.dto.PoeBaseItem b : found) {
        PoeModItemClass c =
            classes.stream()
                .filter(x -> x.itemClass().equals(b.itemClass()))
                .findFirst()
                .orElse(null);
        if (c == null) {
          continue;
        }
        String v = "";
        String vLabel = null;
        if (c.variants() != null && !c.variants().isEmpty()) {
          PoeModItemClass.Variant hit =
              c.variants().stream()
                  .filter(x -> x.bases() != null && x.bases().contains(b.name()))
                  .findFirst()
                  .orElse(null);
          if (hit == null) {
            String av = attributeVariant(b);
            hit = c.variants().stream().filter(x -> x.key().equals(av)).findFirst().orElse(null);
          }
          if (hit == null) {
            continue; // 제작 대상 아닌 베이스(분광 · 무궁한 주얼 등)
          }
          v = hit.key();
          vLabel = ko && hit.nameKo() != null ? hit.nameKo() : hit.name();
        }
        String url =
            "/poe/craft?itemClass="
                + enc(c.itemClass())
                + (v.isEmpty() ? "" : "&variant=" + enc(v))
                + "&base="
                + enc(b.name());
        String name = ko && b.nameKo() != null && !b.nameKo().isBlank() ? b.nameKo() : b.name();
        String cls = ko && c.nameKo() != null ? c.nameKo() : c.name();
        hits.add(
            new String[] {
              url,
              name,
              cls
                  + (vLabel == null || v.startsWith("cluster_") || vLabel.equals(name)
                      ? ""
                      : " · " + vLabel)
            });
        if (hits.size() >= 15) {
          break;
        }
      }
    }
    model.addAttribute("baseHits", hits);
    model.addAttribute("baseQuery", term);
    return "poe/htmx/craftBaseSearch";
  }

  /**
   * 게임 아이템 붙여넣기(10-20 C485, craftofexile 의 Import item) — 복사 글을 API 가 아이템으로 읽고, 수치에 맞는 굴림 씨앗을 찾아 그
   * 아이템으로 에뮬레이터를 연다(HX-Redirect — 주소에 아이템이 들어 있어 공유 · 새로 고침에도 그대로). 못 읽으면 이유를 그 자리에.
   */
  @PostMapping(value = "/poe/htmx/craft/import", produces = MediaType.TEXT_HTML_VALUE)
  public String importItem(
      @RequestParam(required = false, defaultValue = "") String text,
      Model model,
      jakarta.servlet.http.HttpServletResponse response) {
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    if (buildCode(text)) {
      // PoB 빌드 코드(10-21 C525, 사용자 요청 '아이템을 복사해서도 시뮬레이션') — 빌드의 일반 · 매직 · 희귀 아이템 목록, 고르면 그 아이템(PoB
      // 글로 되살려)으로
      net.luversof.web.gate.poe.dto.PoeBuild b;
      try {
        b = buildClient.importBuild(text.strip());
      } catch (RuntimeException e) {
        b = null;
      }
      if (b == null || b.items() == null) {
        model.addAttribute(
            "importError", ko ? "PoB 빌드 코드를 읽지 못했습니다" : "Could not read the PoB build code");
        return "poe/htmx/craftImportMsg";
      }
      List<String[]> picks = new ArrayList<>();
      for (net.luversof.web.gate.poe.dto.PoeBuild.BuildItem bi : b.items()) {
        if (bi.baseType() == null
            || !java.util.Set.of("RARE", "MAGIC", "NORMAL").contains(bi.rarity())) {
          continue;
        }
        String nm = ko && bi.nameKo() != null && !bi.nameKo().isBlank() ? bi.nameKo() : bi.name();
        String bt =
            ko && bi.baseTypeKo() != null && !bi.baseTypeKo().isBlank()
                ? bi.baseTypeKo()
                : bi.baseType();
        String slot = ko && bi.slotKo() != null ? bi.slotKo() : bi.slot();
        String label =
            (slot == null ? "" : slot + " · ")
                + ("NORMAL".equals(bi.rarity()) || nm == null || nm.isBlank()
                    ? bt
                    : nm + " (" + bt + ")");
        try {
          picks.add(
              new String[] {
                label, bi.rarity(), json.writeValueAsString(java.util.Map.of("text", pobText(bi)))
              });
        } catch (RuntimeException e) {
          // 이 아이템만 건너뛴다
        }
      }
      model.addAttribute("buildItems", picks);
      return "poe/htmx/craftImportBuild";
    }
    PoeCraft.ImportResult r;
    try {
      r =
          craftClient.importItem(
              new PoeCraft.ImportRequest(
                  text.length() > 20_000 ? text.substring(0, 20_000) : text));
    } catch (RuntimeException e) {
      r = null;
    }
    if (r == null || r.error() != null || r.item() == null) {
      model.addAttribute(
          "importError", r == null ? (ko ? "가져오기에 실패했습니다" : "Import failed") : r.error());
      model.addAttribute(
          "importWarnings", r == null || r.warnings() == null ? List.of() : r.warnings());
      return "poe/htmx/craftImportMsg";
    }
    PoeCraft.View v = craftClient.view(r.item());
    java.util.Map<String, Long> seeds =
        net.luversof.web.gate.poe.PoeCraftRollFit.fit(v.lines(), v.implicits(), r.values());
    String note = String.join(" · ", r.warnings() == null ? List.of() : r.warnings());
    if (note.length() > 400) {
      note = note.substring(0, 400) + "…";
    }
    PoeCraft.Item it = v.item();
    StringBuilder url = new StringBuilder("/poe/craft?itemClass=").append(enc(it.itemClass()));
    url.append("&variant=").append(enc(nz(it.variant())));
    url.append("&influence=").append(enc(nz(it.influence())));
    url.append("&ilvl=").append(it.ilvl());
    url.append("&state=").append(enc(json.writeValueAsString(it)));
    url.append("&rolls=").append(enc(json.writeValueAsString(seeds)));
    if (!note.isEmpty()) {
      url.append("&importNote=").append(enc(note));
    }
    if (r.baseName() != null) {
      url.append("&base=").append(enc(r.baseName()));
    }
    if (r.itemName() != null) {
      url.append("&itemName=").append(enc(r.itemName()));
    }
    // 군집 주얼 패시브 수(C531) — 글의 'Adds N Passive Skills' · '패시브 스킬 N개 추가'
    if (clusterRange(it.variant()) != null) {
      java.util.regex.Matcher pm =
          java.util.regex.Pattern.compile("(?:Adds (\\d+) Passive Skills|패시브 스킬 (\\d+)개 추가)")
              .matcher(text);
      if (pm.find()) {
        url.append("&passives=").append(pm.group(1) != null ? pm.group(1) : pm.group(2));
      }
    }
    response.setHeader("HX-Redirect", url.toString());
    model.addAttribute("importError", null);
    model.addAttribute("importWarnings", List.of());
    return "poe/htmx/craftImportMsg";
  }

  /** 군집 주얼 크기별 패시브 수 범위(C531, PoB ClusterJewels minNodes~maxNodes) — 대형 8~12 · 중형 4~6 · 소형 2~3. */
  static final java.util.Map<String, int[]> CLUSTER_NODES =
      java.util.Map.of(
          "large", new int[] {8, 12}, "medium", new int[] {4, 6}, "small", new int[] {2, 3});

  /** 변형(cluster_<크기>.<인챈트>)의 패시브 수 범위, 군집이 아니면 null. */
  public static int[] clusterRange(String variant) {
    if (variant == null || !variant.startsWith("cluster_") || variant.indexOf('.') < 9) {
      return null;
    }
    return CLUSTER_NODES.get(variant.substring(8, variant.indexOf('.')));
  }

  /** 고른 패시브 수 — 범위 안으로, 없으면 최소. */
  static int clusterPassives(String variant, Integer passives) {
    int[] r = clusterRange(variant);
    if (r == null) {
      return 0;
    }
    return passives == null ? r[0] : Math.max(r[0], Math.min(r[1], passives));
  }

  /**
   * 군집 주얼 인챈트 줄(10-20 C514) — 변형에 인챈트가 있으면(Jewel 의 cluster_*) 화면 언어로, 없으면 null. classes 가 없으면 읽는다.
   */
  private List<String> enchantOf(
      List<PoeModItemClass> classes, String itemClass, String variant, Integer passives) {
    List<String> lines = enchantOf(classes, itemClass, variant);
    if (lines == null || lines.isEmpty() || clusterRange(variant) == null) {
      return lines;
    }
    // 첫 줄 '패시브 스킬 N개 추가' · 'Adds N Passive Skills' 의 N 을 고른 수로(C531)
    List<String> out = new ArrayList<>(lines);
    out.set(
        0, lines.get(0).replaceFirst("\\d+", String.valueOf(clusterPassives(variant, passives))));
    return out;
  }

  private List<String> enchantOf(List<PoeModItemClass> classes, String itemClass, String variant) {
    if (variant == null || !variant.startsWith("cluster_")) {
      return null;
    }
    try {
      List<PoeModItemClass> list = classes != null ? classes : dataClient.modItemClasses();
      for (PoeModItemClass c : list) {
        if (c.itemClass().equals(itemClass) && c.variants() != null) {
          for (PoeModItemClass.Variant v : c.variants()) {
            if (v.key().equals(variant)) {
              boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
              return ko && v.enchantKo() != null ? v.enchantKo() : v.enchant();
            }
          }
        }
      }
    } catch (RuntimeException e) {
      return null;
    }
    return null;
  }

  /** 가격 직접 입력 칸의 화폐(C529) — 에뮬레이터가 쓰는 주요 화폐, 시세에 있는 것만. */
  static final List<String> PRICE_NAMES =
      List.of(
          "Divine Orb",
          "Exalted Orb",
          "Orb of Alchemy",
          "Orb of Alteration",
          "Orb of Augmentation",
          "Orb of Transmutation",
          "Regal Orb",
          "Orb of Scouring",
          "Orb of Annulment",
          "Vaal Orb",
          "Blessed Orb",
          "Fracturing Orb",
          "Veiled Chaos Orb",
          "Eldritch Chaos Orb",
          "Eldritch Exalted Orb",
          "Orb of Dominance",
          "Awakener's Orb",
          "Hinekora's Lock");

  /**
   * 가격 칸 화폐의 게임 한국어 이름(BaseItemTypes) — 지금 아이템의 화폐 목록에 없는 것도 한국어로(10-25 Q205: 신성한 오브 · 연금술의 오브 등
   * 6개가 영어로 나왔다).
   */
  static final java.util.Map<String, String> PRICE_KO =
      java.util.Map.ofEntries(
          java.util.Map.entry("Divine Orb", "신성한 오브"),
          java.util.Map.entry("Exalted Orb", "엑잘티드 오브"),
          java.util.Map.entry("Orb of Alchemy", "연금술의 오브"),
          java.util.Map.entry("Orb of Alteration", "변화의 오브"),
          java.util.Map.entry("Orb of Augmentation", "확장의 오브"),
          java.util.Map.entry("Orb of Transmutation", "진화의 오브"),
          java.util.Map.entry("Regal Orb", "제왕의 오브"),
          java.util.Map.entry("Orb of Scouring", "정제의 오브"),
          java.util.Map.entry("Orb of Annulment", "소멸의 오브"),
          java.util.Map.entry("Vaal Orb", "바알 오브"),
          java.util.Map.entry("Blessed Orb", "축복의 오브"),
          java.util.Map.entry("Fracturing Orb", "분열의 오브"),
          java.util.Map.entry("Veiled Chaos Orb", "장막의 카오스 오브"),
          java.util.Map.entry("Eldritch Chaos Orb", "섬뜩한 카오스 오브"),
          java.util.Map.entry("Eldritch Exalted Orb", "섬뜩한 엑잘티드 오브"),
          java.util.Map.entry("Orb of Dominance", "지배의 오브"),
          java.util.Map.entry("Hinekora's Lock", "히네코라의 머리카락"));

  /** 가격 직접 입력 칸(C529) — [영어 이름, 한국어 이름, 시세 카오스]. 못 읽으면 빈 목록. */
  private List<String[]> priceList(String itemClass, String variant) {
    try {
      PoeCraft.Prices p = craftClient.prices();
      java.util.Map<String, String> ko = new java.util.HashMap<>();
      try {
        PoeCraft.Options o = craftClient.options(itemClass, variant);
        if (o != null && o.currencies() != null) {
          o.currencies().forEach(c -> ko.putIfAbsent(c.name(), c.nameKo()));
        }
      } catch (RuntimeException e) {
        // 한국어 이름 없이
      }
      List<String[]> out = new ArrayList<>();
      for (String n : PRICE_NAMES) {
        Double v = p == null || p.chaos() == null ? null : p.chaos().get(n);
        if (v != null) {
          out.add(
              new String[] {
                n,
                ko.getOrDefault(n, PRICE_KO.getOrDefault(n, n)),
                v >= 100 ? String.format("%.0f", v) : String.format("%.2f", v)
              });
        }
      }
      return out;
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  /** 베이스가 변형을 정하는 종류(C514 · C516) — 주얼 색 · 군집 주얼 크기 · 심연 주얼 종류, 변형의 베이스 목록으로 거른다. */
  static final java.util.Set<String> BASE_VARIANT_CLASSES = java.util.Set.of("Jewel", "AbyssJewel");

  /**
   * 이 종류 · 변형의 베이스(C487) — 요구 속성으로 변형을 가르고(API PoeOptimizeService.attributeVariantOf 와 같은 규칙), 드롭
   * 레벨 높은 순. 못 읽으면 빈 목록(카드는 종류 이름).
   */
  private List<net.luversof.web.gate.poe.dto.PoeBaseItem> basesFor(
      String itemClass, String variant, List<String> variantBases) {
    // 주얼 · 심연 주얼은 변형의 베이스(진홍 · 대형 군집 · 살인적인 눈 …)만 — 분광 · 무궁한 · 조립된 눈 같은 고유 전용 베이스는 어느 변형에도 없다
    boolean byList = BASE_VARIANT_CLASSES.contains(itemClass);
    try {
      return dataClient.searchBaseItems(null, itemClass).stream()
          .filter(b -> itemClass.equals(b.itemClass()))
          .filter(b -> !byList || (variantBases != null && variantBases.contains(b.name())))
          .filter(b -> variant.isEmpty() || byList || variant.equals(attributeVariant(b)))
          .sorted(
              java.util.Comparator.comparingInt(
                      net.luversof.web.gate.poe.dto.PoeBaseItem::dropLevel)
                  .reversed())
          .toList();
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  /** 베이스 상세(C490 — 카드 속성 · 요구 사항) — 이름으로, 한 번 읽으면 기억. 못 읽으면 null(속성 칸 없음). */
  private final java.util.Map<String, net.luversof.web.gate.poe.dto.PoeBaseItem> baseCache =
      new java.util.concurrent.ConcurrentHashMap<>();

  private net.luversof.web.gate.poe.dto.PoeBaseItem baseByName(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    net.luversof.web.gate.poe.dto.PoeBaseItem b = baseCache.get(name);
    if (b == null) {
      try {
        b = dataClient.baseItemByName(name);
      } catch (RuntimeException e) {
        b = null;
      }
      if (b != null) {
        baseCache.put(name, b);
      }
    }
    return b;
  }

  static String attributeVariant(net.luversof.web.gate.poe.dto.PoeBaseItem b) {
    boolean str = b.reqStr() > 0;
    boolean dex = b.reqDex() > 0;
    boolean in = b.reqInt() > 0;
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
    return in ? "int_armour" : "";
  }

  /** 새 레어 이름을 지을 때 — 레어가 아니었거나, 옛 옵션(분열 제외)이 하나도 안 남았다(다시 만들기). */
  static boolean renamed(PoeCraft.Item before, PoeCraft.Item after) {
    if (!"RARE".equals(before.rarity())) {
      return true;
    }
    java.util.Set<String> old = new java.util.HashSet<>();
    before.affixes().stream().filter(a -> !a.fractured()).forEach(a -> old.add(a.id()));
    return !old.isEmpty()
        && after.affixes().stream()
            .filter(a -> !a.fractured())
            .noneMatch(a -> old.contains(a.id()));
  }

  private static String enc(String s) {
    return java.net.URLEncoder.encode(s == null ? "" : s, java.nio.charset.StandardCharsets.UTF_8);
  }

  /**
   * 화폐 적용 · 되돌리기 · 새로 시작 — action = 화폐 이름 | UNDO | RESET. state · history 는 화면 숨은 칸의 JSON(이력은 이전
   * 상태들, 끝이 가장 최근).
   */
  @PostMapping(value = "/poe/htmx/craft", produces = MediaType.TEXT_HTML_VALUE)
  public String apply(
      // 최적 조합 작업 id(C464) — 결과 카드에 있으면 에뮬레이터가 다음 수단을 받아 보인다
      @RequestParam(required = false) String optJob,
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      @RequestParam(required = false, defaultValue = "84") int ilvl,
      @RequestParam(required = false, defaultValue = "") String state,
      @RequestParam(required = false, defaultValue = "[]") String history,
      @RequestParam(name = "action", required = false, defaultValue = "RESET") String rawAction,
      @RequestParam(required = false) String essence,
      // 섞어 쓰는 제작(10-15 C338) — 화석은 여러 개(체크), 작업대 · 수확은 고른 하나, 정복자는 action 값 "CONQUEROR:crusader"
      @RequestParam(required = false) List<String> fossil,
      @RequestParam(required = false) String bench,
      @RequestParam(required = false) String harvest,
      @RequestParam(required = false, defaultValue = "") String classLabel,
      // 베이스 이름 · 레어 이름(C487) — 카드 머리 표시용, 숨은 칸
      @RequestParam(required = false, defaultValue = "") String baseName,
      @RequestParam(required = false, defaultValue = "") String baseNameKo,
      @RequestParam(required = false, defaultValue = "") String itemName,
      @RequestParam(required = false, defaultValue = "") String itemNameKo,
      // 군집 주얼 패시브 수(C531)
      @RequestParam(required = false) Integer passives,
      // 야수 제작 각인(10-15 C350) — 각인한 매직 아이템(JSON), 한 번 되돌리면 사라진다
      @RequestParam(required = false, defaultValue = "") String imprint,
      // 각인할 때 굴림 씨앗(C381) — 복원하면 그때 값으로
      @RequestParam(required = false, defaultValue = "{}") String imprintRolls,
      // 히네코라의 머리카락(C360) — 미리 보기 씨앗 · 미리 본 화폐의 인자(적용할 때 그대로)
      @RequestParam(required = false, defaultValue = "") String foresee,
      @RequestParam(required = false) String previewArg,
      // 쓴 화폐 기록(C365) — 아이템을 바꾼 단계마다 하나(되돌리기면 마지막 것을 뺀다), 머리카락은 되돌려도 남는다
      @RequestParam(required = false, defaultValue = "[]") String spent,
      // 굴림 씨앗(C370) — 옵션 id → 씨앗(툴팁 수치)
      @RequestParam(required = false, defaultValue = "{}") String rolls,
      // 이력과 짝인 굴림 씨앗(C371) — 되돌리면 그때 값으로
      @RequestParam(required = false, defaultValue = "[]") String rollHistory,
      // 기폭제 "20%까지 한 번에"(C385)
      @RequestParam(required = false) String catalystFill,
      // 목표까지 연타(C506) — 화폐 · 최대 횟수, 목표는 계산기 칸(t_*)을 함께 보낸다
      @RequestParam(required = false, defaultValue = "CHAOS") String spamAct,
      @RequestParam(required = false, defaultValue = "1000") int spamMax,
      // 올플레임 제작(10-20 C523, 3.29 Deepwater) — 화폐 한 번에 실체 없는 복제본 N 개(2~4, 0 = 끔)
      @RequestParam(required = false, defaultValue = "0") int ghost,
      @RequestParam org.springframework.util.MultiValueMap<String, String> params,
      Model model) {
    model.addAttribute("classLabel", classLabel);
    int ghostN = ghost >= 2 ? Math.min(4, ghost) : 0;
    model.addAttribute("ghost", ghostN);
    model.addAttribute("baseName", baseName);
    model.addAttribute("baseNameKo", baseNameKo);
    model.addAttribute("craftBase", baseByName(baseName));
    model.addAttribute("craftEnchant", enchantOf(null, itemClass, variant, passives));
    model.addAttribute(
        "passives", clusterRange(variant) == null ? null : clusterPassives(variant, passives));
    // 새로 시작하면 가져온 레어 이름은 버린다
    model.addAttribute("itemName", rawAction.startsWith("RESET") ? "" : itemName);
    model.addAttribute("itemNameKo", rawAction.startsWith("RESET") ? "" : itemNameKo);
    List<PoeCraft.SpentStep> spentLog = parseSpent(spent);
    java.util.Map<String, Long> rollMap = parseRolls(rolls);
    java.util.Map<String, Long> imprintRollMap = parseRolls(imprintRolls);
    List<java.util.Map<String, Long>> rollHist = parseRollHist(rollHistory);
    boolean reroll = false;
    boolean blessed = false;
    Long lock = net.luversof.web.gate.poe.CraftTargetParams.seedOf(foresee);
    // 올플레임 복제본 고르기(C523) — GHOST:씨앗:화폐 = 그 씨앗으로 그 화폐를 적용(고른 복제본 그대로, 미리 보기 적용과 같은 길)
    String rawAct = rawAction;
    if (rawAct.startsWith("GHOST:") && rawAct.indexOf(':', 6) > 6) {
      int c = rawAct.indexOf(':', 6);
      lock = net.luversof.web.gate.poe.CraftTargetParams.seedOf(rawAct.substring(6, c));
      rawAct = "COMMIT:" + rawAct.substring(c + 1);
    }
    // "이대로 적용"(COMMIT:화폐) — 미리 본 씨앗 · 인자로 그 화폐를 적용
    boolean commit = rawAct.startsWith("COMMIT:");
    String action = commit ? rawAct.substring("COMMIT:".length()) : rawAct;
    PoeCraft.View preview = null;
    PoeCraft.Item imp = parse(imprint, null);
    String imprintError = null;
    PoeCraft.Item fresh =
        new PoeCraft.Item(itemClass, variant, influence, clamp(ilvl), "NORMAL", List.of(), false);
    PoeCraft.Item current = parse(state, fresh);
    List<PoeCraft.Item> hist = parseHistory(history);
    PoeCraft.Item before = current;
    String lastAction = null;
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    if ("RESET".equals(action) || !same(current, fresh)) {
      // 종류 · 변형 · 영향력 · 아이템 레벨을 바꾸면 새 일반 아이템
      current = fresh;
      hist = new ArrayList<>();
      imp = null;
      spentLog = new ArrayList<>();
      rollHist = new ArrayList<>();
    } else if ("IMPRINT".equals(action)) {
      // 각인 — 매직 아이템만(커뮤니티 규칙, 게임 문구 "Item cannot be imprinted.")
      if (current.corrupted()) {
        // 타락한 아이템은 각인할 수 없다(C369 검토 — 각인 → 바알 → 복원으로 타락을 되돌릴 수 있었다)
        imprintError = ko ? "아이템을 각인할 수 없습니다.(타락한 아이템)" : "Item cannot be imprinted. (corrupted)";
      } else if (!"MAGIC".equals(current.rarity())) {
        imprintError = ko ? "아이템을 각인할 수 없습니다.(매직 아이템만)" : "Item cannot be imprinted. (magic only)";
      } else {
        imp = current;
        imprintRollMap = new java.util.LinkedHashMap<>(rollMap);
        lastAction = action;
      }
    } else if ("IMPRINT_RESTORE".equals(action)) {
      // 각인으로 되돌리기 — 같은 아이템이어야(게임 문구 "Target item does not match the imprint."), 각인은 사라진다
      if (imp == null) {
        imprintError = ko ? "각인이 없습니다" : "No imprint.";
      } else if (current.corrupted()) {
        // 타락한 아이템은 더 바꿀 수 없다 — 각인 복원도(C369 검토)
        imprintError =
            ko ? "타락한 아이템은 다시 속성 부여 개조할 수 없습니다" : "Corrupted items cannot be modified again.";
      } else if (!same(imp, current)) {
        imprintError = ko ? "대상 아이템이 각인본과 대응하지 않습니다." : "Target item does not match the imprint.";
      } else {
        hist.add(current);
        rollHist.add(new java.util.LinkedHashMap<>(rollMap));
        while (hist.size() > HISTORY_MAX) {
          hist.remove(0);
        }
        while (rollHist.size() > hist.size()) {
          rollHist.remove(0);
        }
        current = imp;
        imp = null;
        // 각인 때 굴린 값으로(C381)
        rollMap.clear();
        rollMap.putAll(imprintRollMap);
        imprintRollMap = new java.util.LinkedHashMap<>();
        lastAction = action;
        spentLog.add(new PoeCraft.SpentStep(action, null));
      }
    } else if ("FORESEE".equals(action)) {
      lock = java.util.concurrent.ThreadLocalRandom.current().nextLong();
      lastAction = action;
      spentLog.add(new PoeCraft.SpentStep("FORESEE", null));
    } else if ("FORESEE_OFF".equals(action)) {
      lock = null;
    } else if ("UNVEIL_OPEN".equals(action)) {
      // 장막 해제 칸 열기(C475) — 아이템은 그대로, 셋 중 하나를 고르는 칸만(인게임: 장막 옵션은 가려진 채 남고 해제를 따로)
      lastAction = action;
    } else if ("DIVINE".equals(action)) {
      // 신성한 오브(C370) — 옵션 수치만 다시(분열 옵션은 그대로). 타락 · 옵션 없음이면 못 쓴다
      if (current.corrupted()) {
        imprintError =
            ko ? "타락한 아이템은 다시 속성 부여 개조할 수 없습니다" : "Corrupted items cannot be modified again.";
      } else if (current.affixes().stream().allMatch(PoeCraft.Affix::fractured)) {
        imprintError = ko ? "다시 굴릴 옵션이 없습니다" : "No modifiers to reroll.";
      } else {
        reroll = true;
        lock = null;
        lastAction = action;
        spentLog.add(new PoeCraft.SpentStep("DIVINE", null));
      }
    } else if ("BLESSED".equals(action)) {
      // 축복의 오브(C375) — 고정 속성 수치만 다시. 타락 · 고정 속성 없음이면 못 쓴다
      if (current.corrupted()) {
        imprintError =
            ko ? "타락한 아이템은 다시 속성 부여 개조할 수 없습니다" : "Corrupted items cannot be modified again.";
      } else if (current.implicits() == null || current.implicits().isEmpty()) {
        imprintError = ko ? "다시 굴릴 고정 속성이 없습니다" : "No implicit modifiers to reroll.";
      } else {
        reroll = true;
        blessed = true;
        lock = null;
        lastAction = action;
        spentLog.add(new PoeCraft.SpentStep("BLESSED", null));
      }
    } else if ("UNDO".equals(action) || action.startsWith("UNDO_TO:")) {
      // 아무 단계로 되돌리기(C455) — UNDO_TO:n = 이력 n 개만 남긴 상태로(되돌리기를 그만큼 되풀이 — 쓴 화폐 · 굴린 값도 같이)
      int times = 1;
      if (action.startsWith("UNDO_TO:")) {
        try {
          times = hist.size() - Integer.parseInt(action.substring("UNDO_TO:".length()));
        } catch (NumberFormatException e) {
          times = 0;
        }
      }
      for (int u = 0; u < times && !hist.isEmpty(); u++) {
        current = hist.remove(hist.size() - 1);
        // 그때 굴린 값으로(C371)
        if (!rollHist.isEmpty()) {
          rollMap = rollHist.remove(rollHist.size() - 1);
        }
        // 되돌린 단계의 화폐는 뺀다(머리카락은 아이템을 바꾸지 않아 그대로)
        for (int i = spentLog.size() - 1; i >= 0; i--) {
          if (!sideStep(spentLog.get(i))) {
            spentLog.remove(i);
            break;
          }
        }
      }
      // 장막 해제 셋을 새로(C483) — 되돌리기는 보통 다른 셋을 노리고 하는데 같은 셋이 다시 나왔다
      current = current.reseedVeil();
    } else if ("SPAM".equals(action)) {
      // 목표까지 연타(10-20 C506, craftofexile 시뮬레이터의 '될 때까지') — 계산기 목표를 모두 채우거나 최대 횟수까지 같은 화폐, 한 단계로 기록
      String act = SPAM_ACTIONS.contains(spamAct) ? spamAct : "CHAOS";
      String arg = argFor(act, null, essence, fossil, bench, harvest);
      List<PoeCraft.Target> targets;
      try {
        targets = calcTargets(itemClass, variant, fresh, params, new ArrayList<>());
      } catch (RuntimeException e) {
        targets = List.of();
      }
      if (targets.isEmpty()) {
        imprintError =
            ko
                ? "아래 계산기에서 목표를 먼저 고르세요(확률 표의 ☆ 로도 고를 수 있습니다)"
                : "Pick targets in the calculator below first (or with ☆ in the odds table)";
      } else {
        PoeCraft.SpamResult res;
        try {
          res =
              craftClient.spam(
                  new PoeCraft.SpamRequest(
                      current,
                      act,
                      arg,
                      targets,
                      Math.max(1, Math.min(5000, spamMax)),
                      // 올플레임(C524) — 켜져 있고 쓸 수 있는 화폐면 한 번에 복제본 N 개 중 고르기
                      ghostN >= 2 && GHOST_ACTS.contains(act) ? ghostN : null));
        } catch (RuntimeException e) {
          res = null;
        }
        if (res == null) {
          imprintError = ko ? "연타에 실패했습니다" : "Repeat failed";
        } else if (res.error() != null) {
          imprintError = res.error();
        } else if (res.tries() > 0) {
          hist.add(current);
          rollHist.add(new java.util.LinkedHashMap<>(rollMap));
          while (hist.size() > HISTORY_MAX) {
            hist.remove(0);
          }
          while (rollHist.size() > hist.size()) {
            rollHist.remove(0);
          }
          current = res.view().item();
          for (String id : res.added() == null ? List.<String>of() : res.added()) {
            rollMap.remove(id);
          }
          spentLog.add(new PoeCraft.SpentStep(act, arg, res.tries()));
          lastAction = action;
          model.addAttribute(
              "spamNote",
              ko
                  ? (res.met()
                      ? res.tries()
                          + "번 만에 목표를 모두 채웠습니다"
                          + (ghostN >= 2 && GHOST_ACTS.contains(act)
                              ? "(올플레임 복제본 " + ghostN + "개 중 고르기)"
                              : "")
                      : res.tries() < Math.max(1, Math.min(5000, spamMax))
                          ? res.tries() + "번 쓴 뒤 더 쓸 수 없어 멈췄습니다(못 채움)"
                          : res.tries() + "번 써도 못 채웠습니다(최대 횟수)")
                  : (res.met()
                      ? "Met all targets after " + res.tries() + " uses"
                      : res.tries() < Math.max(1, Math.min(5000, spamMax))
                          ? "Stopped after "
                              + res.tries()
                              + " uses — the currency can no longer be used (not met)"
                          : "Not met after " + res.tries() + " uses (limit)"));
        } else {
          model.addAttribute(
              "spamNote", ko ? "이미 목표를 모두 채운 아이템입니다" : "The item already meets all targets");
        }
      }
    } else if (ACTIONS.contains(
        action.contains(":") ? action.substring(0, action.indexOf(':')) : action)) {
      String act = action.contains(":") ? action.substring(0, action.indexOf(':')) : action;
      String actArg = action.contains(":") ? action.substring(action.indexOf(':') + 1) : null;
      String arg = commit ? previewArg : argFor(act, actArg, essence, fossil, bench, harvest);
      if ("CATALYST".equals(act) && !commit && "1".equals(catalystFill) && arg != null) {
        arg = arg + "*20";
      }
      if (ghostN >= 2
          && !commit
          && lock == null
          && !current.corrupted()
          && GHOST_ACTS.contains(act)) {
        // 올플레임 제작(C523) — 원본에 각각 적용한 복제본 N 개(씨앗마다 미리 보기), 아이템은 그대로 두고 고르게 한다
        List<PoeCraft.View> ghosts = new ArrayList<>();
        List<String> ghostSeeds = new ArrayList<>();
        for (int g = 0; g < ghostN; g++) {
          long sd = java.util.concurrent.ThreadLocalRandom.current().nextLong();
          PoeCraft.View gv =
              craftClient.apply(new PoeCraft.ApplyRequest(current, act, essence, sd, arg));
          if (gv.error() == null) {
            ghosts.add(gv);
            ghostSeeds.add(Long.toString(sd));
          }
        }
        if (!ghosts.isEmpty()) {
          model.addAttribute("ghosts", ghosts);
          model.addAttribute("ghostSeeds", ghostSeeds);
          model.addAttribute("ghostAction", action);
          model.addAttribute("previewArg", "ESSENCE".equals(act) ? essence : arg);
          fill(model, current, hist, null, action);
          rollsModel(model, ids(current), fixedIds(current), rollMap, false);
          historyModel(
              model,
              current,
              hist,
              spentLog,
              spentModel(model, itemClass, spentLog),
              rollStates(rollHist, rollMap));
          model.addAttribute("rollHistJson", json.writeValueAsString(rollHist));
          model.addAttribute("foresee", "");
          model.addAttribute("imprintJson", imp == null ? "" : json.writeValueAsString(imp));
          model.addAttribute(
              "imprintRollsJson", imp == null ? "{}" : json.writeValueAsString(imprintRollMap));
          recommend(model, optJob);
          return "poe/htmx/craftEmu";
        }
      }
      PoeCraft.View v =
          craftClient.apply(
              new PoeCraft.ApplyRequest(
                  current, act, commit && "ESSENCE".equals(act) ? previewArg : essence, lock, arg));
      if (lock != null && !commit) {
        // 미리 보기 — 적용하지 않고 결과만(같은 씨앗이면 "이대로 적용"이 같은 결과)
        if (v.error() == null) {
          preview = v;
          model.addAttribute("previewAction", action);
          model.addAttribute("previewArg", arg);
        }
        fill(model, current, hist, v.error() == null ? null : v, action);
        rollsModel(model, ids(current), fixedIds(current), rollMap, false);
        historyModel(
            model,
            current,
            hist,
            spentLog,
            spentModel(model, itemClass, spentLog),
            rollStates(rollHist, rollMap));
        model.addAttribute("rollHistJson", json.writeValueAsString(rollHist));
        model.addAttribute("preview", preview);
        model.addAttribute("foresee", lock.toString());
        model.addAttribute("imprintJson", imp == null ? "" : json.writeValueAsString(imp));
        model.addAttribute(
            "imprintRollsJson", imp == null ? "{}" : json.writeValueAsString(imprintRollMap));
        recommend(model, optJob);
        return "poe/htmx/craftEmu";
      }
      if (v.error() == null) {
        hist.add(current);
        rollHist.add(new java.util.LinkedHashMap<>(rollMap));
        while (hist.size() > HISTORY_MAX) {
          hist.remove(0);
        }
        while (rollHist.size() > hist.size()) {
          rollHist.remove(0);
        }
        current = v.item();
        // 레어가 되거나 다시 만들어지면(연금 · 제왕 · 카오스 · 에센스 재련 등 — 남은 옵션이 없으면) 새 레어 이름(C488, 인게임처럼 종류별 낱말)
        if ("RARE".equals(current.rarity()) && renamed(before, current)) {
          try {
            java.util.Map<String, String> n = craftClient.rareName(itemClass);
            model.addAttribute("itemName", n.getOrDefault("en", ""));
            model.addAttribute("itemNameKo", n.getOrDefault("ko", ""));
          } catch (RuntimeException e) {
            model.addAttribute("itemName", "");
            model.addAttribute("itemNameKo", "");
          }
        }
        // 새로 붙은 옵션은 새 씨앗(C378 검토 — 같은 id 가 다시 붙으면 옛 씨앗이 남아 매번 같은 값이었다), 고정 속성도
        boolean luckyFossil =
            "FOSSIL".equals(act)
                && arg != null
                && arg.contains("CurrencyDelveCraftingLuckyModRolls");
        for (String id : v.added() == null ? List.<String>of() : v.added()) {
          rollMap.remove(id);
          rollMap.remove("imp:" + id);
          rollMap.remove("lucky:" + id);
          // 축성된 화석(C399) — 새로 붙은 옵션 수치는 행운(두 번 굴려 큰 값)
          if (luckyFossil) {
            rollMap.put("lucky:" + id, 1L);
          }
        }
        lastAction = action;
        spentLog.add(
            new PoeCraft.SpentStep(
                act, "CATALYST".equals(act) ? catalystSpent(v.note(), arg) : arg));
      }
      fill(model, current, hist, v, lastAction);
      rollsModel(model, ids(current), fixedIds(current), rollMap, false);
      historyModel(
          model,
          current,
          hist,
          spentLog,
          spentModel(model, itemClass, spentLog),
          rollStates(rollHist, rollMap));
      model.addAttribute("rollHistJson", json.writeValueAsString(rollHist));
      // 아이템이 바뀌면 미리 보기 능력은 사라진다(인게임 "Modifying the item in any way removes the ability to foresee")
      model.addAttribute("foresee", lock == null || current != before ? "" : lock.toString());
      model.addAttribute("imprintJson", imp == null ? "" : json.writeValueAsString(imp));
      model.addAttribute(
          "imprintRollsJson", imp == null ? "{}" : json.writeValueAsString(imprintRollMap));
      recommend(model, optJob);
      return "poe/htmx/craftEmu";
    }
    fill(model, current, hist, null, lastAction);
    rollsModel(
        model, ids(current), blessed ? explicitIds(current) : fixedIds(current), rollMap, reroll);
    historyModel(
        model,
        current,
        hist,
        spentLog,
        spentModel(model, itemClass, spentLog),
        rollStates(rollHist, rollMap));
    model.addAttribute("rollHistJson", json.writeValueAsString(rollHist));
    model.addAttribute("foresee", lock == null || current != before ? "" : lock.toString());
    if (imprintError != null) {
      model.addAttribute("error", imprintError);
    }
    model.addAttribute("imprintJson", imp == null ? "" : json.writeValueAsString(imp));
    model.addAttribute(
        "imprintRollsJson", imp == null ? "{}" : json.writeValueAsString(imprintRollMap));
    recommend(model, optJob);
    return "poe/htmx/craftEmu";
  }

  /**
   * 계산기(10-15 C336) — 계열마다 고른 최소 등급(t_접두접미:계열키 = 등급 숫자, 1 = 최상위만)을 목표로 바꿔 방법별 기대 비용을 낸다. 목표 = 그
   * 계열에서 그 등급 이상인 옵션 아무거나.
   */
  @PostMapping(value = "/poe/htmx/craft/calc", produces = MediaType.TEXT_HTML_VALUE)
  public String calc(
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      @RequestParam(required = false, defaultValue = "84") int ilvl,
      @RequestParam(required = false, defaultValue = "") String essence,
      @RequestParam org.springframework.util.MultiValueMap<String, String> params,
      Model model) {
    PoeCraft.Item item =
        new PoeCraft.Item(itemClass, variant, influence, clamp(ilvl), "NORMAL", List.of(), false);
    List<String> labels = new ArrayList<>();
    List<PoeCraft.Target> targets = calcTargets(itemClass, variant, item, params, labels);
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    PoeCraft.CalcResponse res =
        targets.isEmpty()
            ? new PoeCraft.CalcResponse(
                List.of(),
                null,
                null,
                ko ? "목표 옵션의 최소 등급을 하나 이상 고르세요" : "Pick a minimum tier for at least one modifier")
            : craftClient.calc(
                new PoeCraft.CalcRequest(
                    startItem(item, params),
                    targets,
                    null,
                    essence.isBlank() ? null : essence,
                    null));
    model.addAttribute("calc", res);
    model.addAttribute("targetLabels", labels);
    // 올플레임 복제본 수(C532) — 에뮬레이터에서 켜 둔 값(common.ts 가 ghost 로 보냄), 2~4 가 아니면 표 없음
    String g = params.getFirst("ghost");
    int ghostN = 0;
    try {
      ghostN = g == null ? 0 : Integer.parseInt(g);
    } catch (NumberFormatException e) {
      ghostN = 0;
    }
    model.addAttribute("ghost", ghostN >= 2 && ghostN <= 4 ? ghostN : 0);
    return "poe/htmx/craftCalc";
  }

  /** 묶음 목표(C522) — 공용 CraftTargetParams(C552, PoE2 와 같은 규칙). */
  void groupTargets(
      org.springframework.util.MultiValueMap<String, String> params,
      java.util.Set<String> members,
      List<PoeCraft.PoolLine> pool,
      List<PoeCraft.Target> targets,
      List<String> labels) {
    net.luversof.web.gate.poe.CraftTargetParams.groupTargets(
        params,
        members,
        pool,
        POOL_ACC,
        PoeCraftController::spanLabel,
        PoeCraft.Target::new,
        targets,
        labels);
  }

  /** 빈 자리 조건(C519) — 공용 CraftTargetParams(C552). */
  static boolean openTarget(
      String fam, String value, List<PoeCraft.Target> targets, List<String> labels) {
    return net.luversof.web.gate.poe.CraftTargetParams.openTarget(
        fam, value, PoeCraft.Target::new, targets, labels);
  }

  /** 계산기 목표(계열마다 고른 최소 등급 · 장막 해제 옵션) — 계산기 · 최적 조합이 같이 쓴다(C457). labels 에 화면 이름. */
  private List<PoeCraft.Target> calcTargets(
      String itemClass,
      String variant,
      PoeCraft.Item item,
      org.springframework.util.MultiValueMap<String, String> params,
      List<String> labels) {
    return calcTargets(itemClass, variant, item, params, labels, null, false);
  }

  /**
   * gens(null 이면 안 씀) = 목표마다 prefix · suffix · implicit(엘드리치) · null(묶음 · 빈 자리) — 전략 모의가 접두/접미 목표를
   * 고른다(C550). explicitOnly = 섬뜩한 고정 속성 목표를 뺀다(전략 모의는 명시 옵션만). labels 는 화면 이름(묶음 · 빈 자리는 목표 여럿에 이름
   * 하나). 패키지 공개 = 쪽 표시 시험(PoeCraftTargetSidesTest).
   */
  List<PoeCraft.Target> calcTargets(
      String itemClass,
      String variant,
      PoeCraft.Item item,
      org.springframework.util.MultiValueMap<String, String> params,
      List<String> labels,
      List<String> gens,
      boolean explicitOnly) {
    List<PoeCraft.PoolLine> pool = craftClient.pool(new PoeCraft.PoolRequest(item, "RARE"));
    List<PoeCraft.Target> targets = new ArrayList<>();
    // 목표마다 쪽(C550) — 앞의 빈 칸(묶음 · 빈 자리)은 null 로 채우고 이 목표의 쪽
    java.util.function.Consumer<String> gen =
        g -> {
          if (gens != null) {
            while (gens.size() < targets.size() - 1) {
              gens.add(null);
            }
            gens.add(g);
          }
        };
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    List<PoeCraft.VeiledLine> veiled = null;
    // 탐광 · 엘드리치 목표(C549) — 덧붙임에서 문구 · 등급 id
    PoeCraft.CalcExtras ext = null;
    java.util.Set<String> grp = net.luversof.web.gate.poe.CraftTargetParams.groupMembers(params);
    groupTargets(params, grp, pool, targets, labels);
    for (String key : params.keySet()) {
      if (!key.startsWith("t_")) {
        continue;
      }
      String fam = key.substring(2);
      if (openTarget(fam, params.getFirst(key), targets, labels)) {
        continue;
      }
      if (fam.startsWith("delve:") || fam.startsWith("eld:") || fam.startsWith("ess:")) {
        String val = params.getFirst(key);
        if (val == null || val.isBlank()) {
          continue;
        }
        if (ext == null) {
          try {
            ext = craftClient.calcExtras(itemClass, nz(variant), nz(item.influence()), item.ilvl());
          } catch (RuntimeException e) {
            ext = null;
          }
        }
        if (ext == null) {
          continue;
        }
        if (fam.startsWith("delve:")) {
          String did = fam.substring("delve:".length());
          PoeCraft.DelveLine dl =
              ext.delve() == null
                  ? null
                  : ext.delve().stream().filter(x -> x.id().equals(did)).findFirst().orElse(null);
          if (dl == null || !"1".equals(val)) {
            continue;
          }
          targets.add(new PoeCraft.Target(List.of(did)));
          gen.accept(dl.gen());
          List<String> dt = ko && dl.ko() != null && !dl.ko().isEmpty() ? dl.ko() : dl.en();
          labels.add(String.join(", ", dt == null ? List.of() : dt) + (ko ? " (탐광)" : " (delve)"));
        } else if (fam.startsWith("ess:")) {
          // 에센스 전용 옵션(C551) — 고른 에센스 등급 이상의 보장 옵션 id
          int min;
          try {
            min = Integer.parseInt(val);
          } catch (RuntimeException e) {
            continue;
          }
          String ef = fam.substring("ess:".length());
          PoeCraft.EssenceOnly eo =
              ext.essenceOnly() == null
                  ? null
                  : ext.essenceOnly().stream()
                      .filter(x -> x.family().equals(ef))
                      .findFirst()
                      .orElse(null);
          if (eo == null) {
            continue;
          }
          List<PoeCraft.EssenceTier> in = eo.tiers().stream().filter(t -> t.tier() >= min).toList();
          if (in.isEmpty()) {
            continue;
          }
          targets.add(new PoeCraft.Target(in.stream().map(PoeCraft.EssenceTier::modId).toList()));
          gen.accept(eo.gen());
          PoeCraft.EssenceTier t0 = in.get(0);
          List<String> et = ko && t0.ko() != null && !t0.ko().isEmpty() ? t0.ko() : t0.en();
          labels.add(
              String.join(", ", et == null ? List.of() : et)
                  + (in.size() > 1 ? (ko ? " 이상" : " or better") : "")
                  + (ko ? " (에센스)" : " (essence)"));
        } else {
          if (explicitOnly) {
            continue;
          }
          int min;
          try {
            min = Integer.parseInt(val);
          } catch (RuntimeException e) {
            continue;
          }
          String sk = fam.substring("eld:".length());
          PoeCraft.EldritchFamily ef =
              ext.eldritch() == null
                  ? null
                  : ext.eldritch().stream()
                      .filter(x -> (x.side() + ":" + x.key()).equals(sk))
                      .findFirst()
                      .orElse(null);
          if (ef == null) {
            continue;
          }
          List<PoeCraft.EldritchTier> in =
              ef.tiers().stream().filter(t -> t.tier() >= min).toList();
          if (in.isEmpty()) {
            continue;
          }
          targets.add(new PoeCraft.Target(in.stream().map(PoeCraft.EldritchTier::id).toList()));
          gen.accept("implicit");
          PoeCraft.EldritchTier t0 = in.get(0);
          List<String> et = ko && t0.ko() != null && !t0.ko().isEmpty() ? t0.ko() : t0.en();
          labels.add(
              ("exarch".equals(ef.side())
                      ? (ko ? "작열의 총주교: " : "Exarch: ")
                      : (ko ? "세계 포식자: " : "Eater: "))
                  + String.join(", ", et == null ? List.of() : et)
                  + (ko ? " 이상" : " or better"));
        }
        continue;
      }
      if (fam.startsWith("grp:") || "grpk".equals(fam) || grp.contains(fam)) {
        continue; // 묶음(C522) — 위에서 따로
      }
      if (fam.startsWith("veil:")) {
        // 장막 해제 옵션 목표(C421) — 체크하면 그 옵션
        if (!"1".equals(params.getFirst(key))) {
          continue;
        }
        if (veiled == null) {
          try {
            veiled = craftClient.veiled(itemClass, variant);
          } catch (RuntimeException e) {
            veiled = List.of();
          }
        }
        String vid = fam.substring("veil:".length());
        PoeCraft.VeiledLine vl =
            veiled.stream().filter(x -> x.id().equals(vid)).findFirst().orElse(null);
        if (vl == null) {
          continue;
        }
        targets.add(new PoeCraft.Target(List.of(vid)));
        gen.accept(vl.gen());
        List<String> vt = ko && vl.ko() != null && !vl.ko().isEmpty() ? vl.ko() : vl.en();
        labels.add(String.join(", ", vt == null ? List.of() : vt) + (ko ? " (해제)" : " (unveiled)"));
        continue;
      }
      int rank;
      try {
        rank = Integer.parseInt(params.getFirst(key));
      } catch (RuntimeException e) {
        continue;
      }
      List<PoeCraft.PoolLine> in =
          pool.stream()
              .filter(p -> (p.gen() + ":" + p.family()).equals(fam))
              .filter(p -> p.tierRank() != null && p.tierRank() <= rank)
              .sorted(java.util.Comparator.comparing(PoeCraft.PoolLine::tierRank))
              .toList();
      if (in.isEmpty()) {
        continue;
      }
      targets.add(new PoeCraft.Target(in.stream().map(PoeCraft.PoolLine::id).toList()));
      gen.accept(in.get(0).gen());
      labels.add(spanLabel(in, ko) + " (T1" + (rank > 1 ? "~T" + rank : "") + ")");
    }
    if (gens != null) {
      while (gens.size() < targets.size()) {
        gens.add(null);
      }
    }
    return targets;
  }

  /**
   * 같은 옵션을 얻는 다른 길(10-22 P4, poedb 속성 부여의 '같은 문구 다른 출처') — 확률 표에서 계열을 펼치면 불러온다. 에센스 · 작업대 · 화석 배수 ·
   * 수확(API familyWays), 못 읽으면 빈 안내.
   */
  @GetMapping(value = "/poe/htmx/craft/family-ways", produces = MediaType.TEXT_HTML_VALUE)
  public String familyWays(
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      @RequestParam(required = false, defaultValue = "84") int ilvl,
      @RequestParam String family,
      Model model) {
    PoeCraft.FamilyWays ways;
    try {
      ways =
          craftClient.familyWays(
              new PoeCraft.FamilyWaysRequest(
                  new PoeCraft.Item(
                      itemClass, variant, influence, clamp(ilvl), "NORMAL", List.of(), false),
                  family));
    } catch (RuntimeException e) {
      ways = null;
    }
    model.addAttribute("ways", ways);
    return "poe/htmx/craftFamilyWays";
  }

  /**
   * 고른 화폐 기준 옵션 표(10-22 P3, poedb 속성 부여 — 화석을 고르면 같은 표가 그 기준으로 바뀐다) — 에뮬레이터에서 화폐를 고른 상태(CoE 방식)면 계열
   * 표를 그 수단이 처음 뽑는 옵션의 가중치로 다시 그린다(화석 배수 · 메타 금지 · 축성 레벨 가중 · 다시 굴릴 바탕은 엔진이 실제로 쓰는 값). action 이 없으면
   * 지금 아이템 기준 표, 표의 옵션을 뽑지 않는 수단 · 못 쓰는 화폐면 지금 아이템 기준 표에 그 안내.
   */
  @PostMapping(value = "/poe/htmx/craft/action-pool", produces = MediaType.TEXT_HTML_VALUE)
  public String actionPool(
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      @RequestParam(required = false, defaultValue = "84") int ilvl,
      @RequestParam(required = false, defaultValue = "") String state,
      @RequestParam(required = false, defaultValue = "") String action,
      @RequestParam(required = false, defaultValue = "") String actionLabel,
      @RequestParam(required = false) String essence,
      @RequestParam(required = false) List<String> fossil,
      @RequestParam(required = false) String bench,
      @RequestParam(required = false) String harvest,
      Model model) {
    PoeCraft.Item fresh =
        new PoeCraft.Item(itemClass, variant, influence, clamp(ilvl), "NORMAL", List.of(), false);
    PoeCraft.Item cur = parse(state, fresh);
    if (!same(cur, fresh)) {
      cur = fresh;
    }
    PoeCraft.View view = craftClient.view(cur);
    String act = action.contains(":") ? action.substring(0, action.indexOf(':')) : action;
    String actArg = action.contains(":") ? action.substring(action.indexOf(':') + 1) : null;
    PoeCraft.ActionPool ap = null;
    if (ACTIONS.contains(act)) {
      try {
        ap =
            craftClient.actionPool(
                new PoeCraft.ApplyRequest(
                    cur, act, essence, null, argFor(act, actArg, essence, fossil, bench, harvest)));
      } catch (RuntimeException e) {
        ap = null;
      }
    }
    boolean picked = ap != null && ap.picked();
    List<PoeCraft.PoolLine> pool =
        picked ? ap.lines() : craftClient.pool(new PoeCraft.PoolRequest(view.item(), null));
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    List<PoeCraft.Line> lines = view.lines() == null ? List.of() : view.lines();
    var groups = poolGroups(view, pool, java.util.Set.of());
    model.addAttribute("poolGroups", groups);
    model.addAttribute("lines", lines);
    model.addAttribute(
        "waysUrl",
        net.luversof.web.gate.poe.CraftPoolGroups.waysUrl(
            "/poe/htmx/craft/family-ways",
            cur.itemClass(),
            cur.variant(),
            cur.influence(),
            cur.ilvl()));
    model.addAttribute("max", view.maxPerGen());
    model.addAttribute("prefixes", lines.stream().filter(l -> "prefix".equals(l.gen())).count());
    model.addAttribute("suffixes", lines.stream().filter(l -> "suffix".equals(l.gen())).count());
    model.addAttribute(
        "lineTags",
        ko
            ? (view.tagsKo() == null ? java.util.Map.of() : view.tagsKo())
            : (view.tags() == null ? java.util.Map.of() : view.tags()));
    if (!act.isEmpty()) {
      model.addAttribute("actionLabel", actionLabel.isBlank() ? act : actionLabel);
      model.addAttribute("actionPicked", picked);
      model.addAttribute("actionRarity", picked ? ap.baseRarity() : null);
      model.addAttribute(
          "actionMult", picked && ap.mult() != null ? ap.mult() : java.util.Map.of());
      // 확정 옵션(Q7 — 에센스) — 그 계열 · 등급 상당(계열 줄 배지)과 범위 글(안내)
      java.util.Map<String, Integer> forcedFam = new java.util.LinkedHashMap<>();
      List<String> forcedText = new ArrayList<>();
      for (PoeCraft.Forced fc :
          ap == null || ap.forced() == null ? List.<PoeCraft.Forced>of() : ap.forced()) {
        if (fc.family() != null) {
          forcedFam.put(fc.family(), fc.rank());
        }
        List<String> mx = ko && fc.ko() != null && !fc.ko().isEmpty() ? fc.ko() : fc.en();
        List<String> mn =
            ko && fc.koMin() != null && !fc.koMin().isEmpty() ? fc.koMin() : fc.enMin();
        for (int i = 0; mx != null && i < mx.size(); i++) {
          forcedText.add(
              net.luversof.web.gate.poe.PoeModRange.range(
                  mn != null && i < mn.size() ? mn.get(i) : null, mx.get(i)));
        }
      }
      model.addAttribute("forcedFamilies", forcedFam);
      model.addAttribute("forcedText", forcedText.isEmpty() ? null : String.join(", ", forcedText));
      model.addAttribute(
          "actionError",
          ap == null || ap.error() == null
              ? null
              : ko ? ap.error() : net.luversof.web.gate.poe.PoeCraftText.en(ap.error()));
    }
    return "poe/htmx/craftPoolGroups";
  }

  /**
   * 올플레임 제작에 쓸 수 있는 화폐(C523) — 인게임 안내: 타락한 아이템 · 타락시키는 화폐(바알 · 더럽혀진), 품질 · 홈 화폐 대부분, 여러 아이템을 만드는
   * 화폐는 안 된다. 작업대 · 수확 · 야수 · 직접 편집 · 장막 해제(셋 중 고르기) · 각성자(두 아이템)도 화폐 한 번이 아니라 뺀다.
   */
  static final java.util.Set<String> GHOST_ACTS =
      java.util.Set.of(
          "TRANSMUTE",
          "ALTERATION",
          "AUGMENTATION",
          "REGAL",
          "ALCHEMY",
          "CHAOS",
          "EXALT",
          "ANNUL",
          "SCOUR",
          "ESSENCE",
          "FOSSIL",
          "CONQUEROR",
          "FRACTURE",
          "VEILED_CHAOS",
          "VEILED_EXALT",
          "EMBER",
          "ICHOR",
          "ELDRITCH_CHAOS",
          "ELDRITCH_EXALT",
          "ELDRITCH_ANNUL",
          "DOMINANCE",
          "DUCAT",
          "CONFLICT",
          "FOULBORN_AUG",
          "FOULBORN_REGAL",
          "FOULBORN_EXALT");

  /** 목표까지 연타로 쓸 수 있는 화폐(C506) — 같은 화폐를 되풀이해 의미가 있는 것(고른 에센스 · 화석 · 수확 포함). */
  static final java.util.Set<String> SPAM_ACTIONS =
      java.util.Set.of(
          "CHAOS", "ALTERATION", "ESSENCE", "FOSSIL", "HARVEST", "VEILED_CHAOS", "EXALT");

  /** 목표 적중 확률을 보는 화폐(C494) — 인자 없는 기본 화폐. 에센스는 고른 것을 더한다. */
  static final List<String> HIT_ACTIONS =
      List.of(
          "TRANSMUTE",
          "ALTERATION",
          "AUGMENTATION",
          "REGAL",
          "ALCHEMY",
          "CHAOS",
          "EXALT",
          "ANNUL",
          "SCOUR",
          "VEILED_CHAOS",
          "VEILED_EXALT",
          "VAAL");

  /** 결과 분포 화폐(C533) — 같은 화폐를 지금 아이템에 여러 번 따로 써서 결과를 세는 것. */
  static final List<String> DIST_ACTIONS =
      List.of(
          "CHAOS",
          "ALCHEMY",
          "EXALT",
          "ALTERATION",
          "AUGMENTATION",
          "REGAL",
          "TRANSMUTE",
          "ESSENCE",
          "FOSSIL",
          "VEILED_CHAOS");

  /**
   * 결과 분포(10-21 C533, craftofexile '대량 모의 — 옵션 분포') — 고른 화폐를 지금 아이템에 5천 번 따로 써서 계열마다 붙는 비율 · 그중 1등급
   * · 옵션 개수. 계열 이름은 그 풀의 등급 범위 문구로.
   */
  @PostMapping(value = "/poe/htmx/craft/dist", produces = MediaType.TEXT_HTML_VALUE)
  public String distribution(
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      @RequestParam(required = false, defaultValue = "84") int ilvl,
      @RequestParam(required = false, defaultValue = "") String state,
      @RequestParam(required = false, defaultValue = "CHAOS") String distAct,
      @RequestParam(required = false, defaultValue = "") String essence,
      @RequestParam(required = false) List<String> fossil,
      Model model) {
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    PoeCraft.Item fresh =
        new PoeCraft.Item(itemClass, variant, influence, clamp(ilvl), "NORMAL", List.of(), false);
    PoeCraft.Item cur = parse(state, fresh);
    if (!same(cur, fresh)) {
      cur = fresh;
    }
    String act = DIST_ACTIONS.contains(distAct) ? distAct : "CHAOS";
    String arg = argFor(act, null, essence.isBlank() ? null : essence, fossil, null, null);
    PoeCraft.DistResult res;
    try {
      res = craftClient.distribution(new PoeCraft.DistRequest(cur, act, arg, 5000));
    } catch (RuntimeException e) {
      res =
          new PoeCraft.DistResult(
              0, List.of(), java.util.Map.of(), ko ? "모의에 실패했습니다" : "Simulation failed");
    }
    // 계열 이름 = 그 풀의 등급 범위 문구(낮은 등급 최소 ~ 1등급 최대)
    java.util.Map<String, String> labels = new java.util.HashMap<>();
    try {
      java.util.Map<String, List<PoeCraft.PoolLine>> byFam = new java.util.HashMap<>();
      for (PoeCraft.PoolLine p : craftClient.pool(new PoeCraft.PoolRequest(cur, "RARE"))) {
        byFam.computeIfAbsent(p.gen() + "|" + p.family(), k -> new ArrayList<>()).add(p);
      }
      byFam.forEach(
          (k, v) -> {
            List<PoeCraft.PoolLine> tiers =
                v.stream()
                    .filter(p -> p.tierRank() != null)
                    .sorted(java.util.Comparator.comparing(PoeCraft.PoolLine::tierRank))
                    .toList();
            if (!tiers.isEmpty()) {
              labels.put(k, spanLabel(tiers, ko));
            }
          });
    } catch (RuntimeException e) {
      // 이름 없이 계열 키로
    }
    model.addAttribute("dist", res);
    model.addAttribute("distLabels", labels);
    return "poe/htmx/craftDist";
  }

  /**
   * 화폐 한 번의 목표 적중 확률(10-20 C494, craftofexile 에뮬레이터의 목표 확률) — 아래 계산기에서 고른 목표를 지금 아이템에 화폐 하나로 다 채울
   * 확률. 화면이 화폐 단추마다 작은 표로 보인다. 목표가 없으면 targets 0.
   */
  @PostMapping(value = "/poe/htmx/craft/hit-chance", produces = MediaType.APPLICATION_JSON_VALUE)
  @org.springframework.web.bind.annotation.ResponseBody
  public java.util.Map<String, Object> hitChance(
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      @RequestParam(required = false, defaultValue = "84") int ilvl,
      @RequestParam(required = false, defaultValue = "") String state,
      @RequestParam(required = false, defaultValue = "") String essence,
      // 고른 화석 · 수확도(C507 — 그 '사용' 단추와 연타 줄에)
      @RequestParam(required = false) List<String> fossil,
      @RequestParam(required = false, defaultValue = "") String harvest,
      @RequestParam org.springframework.util.MultiValueMap<String, String> params) {
    PoeCraft.Item fresh =
        new PoeCraft.Item(itemClass, variant, influence, clamp(ilvl), "NORMAL", List.of(), false);
    List<PoeCraft.Target> targets;
    try {
      targets = calcTargets(itemClass, variant, fresh, params, new ArrayList<>());
    } catch (RuntimeException e) {
      targets = List.of();
    }
    if (targets.isEmpty()) {
      return java.util.Map.of("targets", 0);
    }
    PoeCraft.Item cur = parse(state, fresh);
    if (!same(cur, fresh)) {
      cur = fresh;
    }
    List<PoeCraft.HitAction> actions = new ArrayList<>();
    HIT_ACTIONS.forEach(a -> actions.add(new PoeCraft.HitAction(a, null)));
    if (!essence.isBlank()) {
      actions.add(new PoeCraft.HitAction("ESSENCE", essence));
    }
    if (fossil != null && !fossil.isEmpty()) {
      actions.add(new PoeCraft.HitAction("FOSSIL", String.join(",", fossil)));
    }
    if (!harvest.isBlank()) {
      actions.add(new PoeCraft.HitAction("HARVEST", harvest));
    }
    java.util.Map<String, Object> chances = new java.util.LinkedHashMap<>();
    try {
      for (PoeCraft.Hit h :
          craftClient.hitChance(new PoeCraft.HitRequest(cur, targets, actions, 1000))) {
        chances.put(h.action(), h.error() == null ? h.chance() : null);
      }
    } catch (RuntimeException e) {
      return java.util.Map.of("targets", targets.size(), "error", "fail");
    }
    return java.util.Map.of("targets", targets.size(), "chances", chances);
  }

  /**
   * 최적 조합(10-19 C457 사용자 요청 "에센스 · 작업대 · 야수 같은 다양한 방법을 조합해 최소 비용") — 계산기와 같은 목표로, 계산기 표가 뜬 뒤 이어서
   * 부른다(upper · upperLabel = 표의 가장 싼 방법). API 가 상태별 최소 기대 비용을 찾고(8초 안) 그 정책을 실제 아이템에 굴려 잰 비용.
   */
  @PostMapping(value = "/poe/htmx/craft/optimize", produces = MediaType.TEXT_HTML_VALUE)
  public String optimize(
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      @RequestParam(required = false, defaultValue = "84") int ilvl,
      @RequestParam(required = false) Double upper,
      @RequestParam(required = false) String upperLabel,
      // 표가 고른 화석 · 수확(쉼표 대신 '|' — 화석 조합 안에 쉼표가 있다)
      @RequestParam(required = false) String methods,
      @RequestParam org.springframework.util.MultiValueMap<String, String> params,
      Model model) {
    PoeCraft.Item item =
        new PoeCraft.Item(itemClass, variant, influence, clamp(ilvl), "NORMAL", List.of(), false);
    List<String> labels = new ArrayList<>();
    List<PoeCraft.Target> targets = calcTargets(itemClass, variant, item, params, labels);
    String error = null;
    String jobId = null;
    if (targets.isEmpty()) {
      error = net.luversof.web.gate.poe.PoeText.isKorean() ? "목표가 없습니다" : "No targets";
    } else {
      try {
        // 백그라운드로 시작하고 조회하며 기다린다(C457k — 8초 동기 호출로는 탐색이 모자랐다)
        jobId =
            craftClient
                .optimizeStart(
                    new PoeCraft.OptimizeRequest(
                        startItem(item, params),
                        targets,
                        OPTIMIZE_BUDGET_MS,
                        upper,
                        upperLabel,
                        methods == null ? null : List.of(methods.split("[|]"))))
                .get("id");
      } catch (RuntimeException e) {
        error =
            net.luversof.web.gate.poe.PoeText.isKorean()
                ? "최적 조합 계산을 시작하지 못했습니다"
                : "Could not start the optimal combination";
      }
    }
    model.addAttribute("optError", error);
    model.addAttribute("targetLabels", labels);
    model.addAttribute("jobId", jobId);
    model.addAttribute("progress", 0);
    return error != null ? "poe/htmx/craftOptimize" : "poe/htmx/craftOptimizeWait";
  }

  /** 최적 조합 진행 조회(C457k) — 끝났으면 결과, 아니면 기다림 조각(1.5초 뒤 다시). */
  @org.springframework.web.bind.annotation.GetMapping(
      value = "/poe/htmx/craft/optimize/{id}",
      produces = MediaType.TEXT_HTML_VALUE)
  public String optimizeStatus(
      @org.springframework.web.bind.annotation.PathVariable String id,
      @RequestParam(required = false) List<String> label,
      Model model) {
    PoeCraft.OptimizeStatus st;
    try {
      st = craftClient.optimizeStatus(id);
    } catch (RuntimeException e) {
      st = null;
    }
    model.addAttribute("targetLabels", label == null ? List.of() : label);
    if (st == null) {
      model.addAttribute(
          "optError",
          net.luversof.web.gate.poe.PoeText.isKorean()
              ? "최적 조합 작업을 찾지 못했습니다(다시 계산하세요)"
              : "The optimization job was not found (calculate again)");
      return "poe/htmx/craftOptimize";
    }
    if (!"done".equals(st.state())) {
      model.addAttribute("jobId", id);
      model.addAttribute(
          "progress", (int) Math.min(99, st.elapsedMs() * 100 / Math.max(1, st.budgetMs())));
      return "poe/htmx/craftOptimizeWait";
    }
    model.addAttribute("opt", st.result());
    model.addAttribute("jobId", id);
    return "poe/htmx/craftOptimize";
  }

  /**
   * 다단계 전략 시뮬레이터(10-15 C339) — 계산기와 같은 목표 고르기(t_접두접미:계열 = 최소 등급)에 프리셋 전략을 얹어 모의한다. 목표 순서 = 화면 순서(접두
   * 먼저). 프리셋: alt-regal(변화 → [변경+확장]을 접두 · 접미 목표 하나씩 채울 때까지 → 제왕 → 엑잘 셋), chaos(연금 → 카오스 반복),
   * essence(고른 에센스 반복), prefix-lock / suffix-lock(연금 → 그쪽 목표를 카오스로 채운 뒤 [작업대 잠금 → 카오스] 묶음 반복).
   */
  @PostMapping(value = "/poe/htmx/craft/strategy", produces = MediaType.TEXT_HTML_VALUE)
  public String strategy(
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      @RequestParam(required = false, defaultValue = "84") int ilvl,
      @RequestParam(required = false, defaultValue = "") String essence,
      @RequestParam(required = false, defaultValue = "chaos") String preset,
      @RequestParam org.springframework.util.MultiValueMap<String, String> params,
      Model model) {
    PoeCraft.Item item =
        new PoeCraft.Item(itemClass, variant, influence, clamp(ilvl), "NORMAL", List.of(), false);
    // 목표 해석은 계산기와 같은 것(10-21 C550 정리 — 두 벌이던 것을 하나로. 전략 쪽은 묶음 · 빈 자리 목표에 접두/접미 표시를 안 넣어 그 뒤
    // 목표의 접두/접미 번호가 한 칸씩 어긋났다). 섬뜩한 고정 속성은 명시 옵션 제작과 따로라 전략 모의에선 뺀다(C549)
    List<String> labels = new ArrayList<>();
    List<String> gens = new ArrayList<>();
    List<PoeCraft.Target> targets =
        calcTargets(itemClass, variant, item, params, labels, gens, true);
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    List<Integer> pre = new ArrayList<>();
    List<Integer> suf = new ArrayList<>();
    for (int i = 0; i < targets.size(); i++) {
      String g = i < gens.size() ? gens.get(i) : null;
      if ("prefix".equals(g)) {
        pre.add(i);
      } else if ("suffix".equals(g)) {
        suf.add(i);
      }
    }
    List<Integer> all = new ArrayList<>();
    for (int i = 0; i < targets.size(); i++) {
      all.add(i);
    }
    PoeCraft.Options opts = craftClient.options(itemClass, variant);
    String lockPre =
        opts.bench().stream()
            .map(PoeCraft.BenchCraft::modId)
            .filter(id -> id != null && id.contains("CannotChangePrefixes"))
            .findFirst()
            .orElse(null);
    String lockSuf =
        opts.bench().stream()
            .map(PoeCraft.BenchCraft::modId)
            .filter(id -> id != null && id.contains("CannotChangeSuffixes"))
            .findFirst()
            .orElse(null);
    List<PoeCraft.Step> steps = new ArrayList<>();
    String label;
    switch (preset) {
      case "alt-regal" -> {
        // 매직은 접두 하나 · 접미 하나 — 목표 중 첫 접두 · 첫 접미까지 변경(+확장)으로
        List<Integer> magic = new ArrayList<>();
        if (!pre.isEmpty()) {
          magic.add(pre.get(0));
        }
        if (!suf.isEmpty()) {
          magic.add(suf.get(0));
        }
        steps.add(step("TRANSMUTE", null, null, null));
        steps.add(
            new PoeCraft.Step(
                "SEQ",
                null,
                magic,
                3000,
                List.of(
                    step("ALTERATION", null, null, null), step("AUGMENTATION", null, null, null))));
        steps.add(step("REGAL", null, null, null));
        steps.add(step("EXALT", null, null, null));
        steps.add(step("EXALT", null, null, null));
        steps.add(step("EXALT", null, null, null));
        label = ko ? "변화(+확장) → 제왕 → 엑잘 ×3" : "Alteration (+aug) → Regal → Exalt ×3";
      }
      case "essence" -> {
        // 에센스를 안 골랐으면 모의하지 않는다(C452 UX QA — 2만 회 뒤 '성공 0'만 나와 원인을 몰랐다)
        if (essence == null || essence.isBlank()) {
          model.addAttribute(
              "result",
              new PoeCraft.StrategyResult(
                  0,
                  0,
                  0,
                  java.util.Map.of(),
                  null,
                  null,
                  List.of(),
                  ko
                      ? "에센스 반복 프리셋은 위 계산기의 '에센스' 칸에서 에센스를 골라야 합니다"
                      : "The Essence spam preset needs an essence chosen in the calculator's Essence box above"));
          model.addAttribute("presetLabel", preset);
          model.addAttribute("targetLabels", labels);
          return "poe/htmx/craftStrategy";
        }
        steps.add(step("ESSENCE", essence, all, 5000));
        label = ko ? "에센스 반복" : "Essence spam";
      }
      case "veiled" -> {
        // 장막 해제 목표(C427 — 실행기는 UNVEIL 을 목표 먼저로 처리하는데 프리셋이 없어 해제 목표가 늘 0%)
        steps.add(step("ALCHEMY", null, null, null));
        steps.add(
            new PoeCraft.Step(
                "SEQ",
                null,
                all,
                3000,
                List.of(step("VEILED_CHAOS", null, null, null), step("UNVEIL", null, null, null))));
        label =
            ko
                ? "연금 → [장막의 카오스 → 해제(목표 먼저)] 반복"
                : "Alchemy → [Veiled Chaos → unveil (target first)] repeat";
      }
      case "prefix-lock", "suffix-lock" -> {
        boolean p = "prefix-lock".equals(preset);
        String lock = p ? lockPre : lockSuf;
        List<Integer> side = p ? pre : suf;
        if (lock == null || side.isEmpty()) {
          model.addAttribute(
              "result",
              new PoeCraft.StrategyResult(
                  0,
                  0,
                  0,
                  java.util.Map.of(),
                  null,
                  null,
                  List.of(),
                  ko
                      ? "그쪽(접두/접미) 목표나 잠금 작업대 옵션이 없습니다"
                      : "No targets on that side or no lock craft"));
          model.addAttribute("presetLabel", preset);
          model.addAttribute("targetLabels", labels);
          return "poe/htmx/craftStrategy";
        }
        steps.add(step("ALCHEMY", null, null, null));
        steps.add(step("CHAOS", null, side, 5000));
        steps.add(
            new PoeCraft.Step(
                "SEQ",
                null,
                all,
                2000,
                List.of(step("BENCH", lock, null, null), step("CHAOS", null, null, null))));
        label =
            p
                ? (ko
                    ? "카오스로 접두 확보 → [접두 잠금 + 카오스] 반복"
                    : "Chaos for prefixes → [prefix lock + chaos] repeat")
                : (ko
                    ? "카오스로 접미 확보 → [접미 잠금 + 카오스] 반복"
                    : "Chaos for suffixes → [suffix lock + chaos] repeat");
      }
      case "custom" -> {
        steps.addAll(customSteps(params, all, pre, suf));
        label = ko ? "직접 엮은 단계 " + steps.size() + "개" : steps.size() + " custom steps";
      }
      default -> {
        steps.add(step("ALCHEMY", null, null, null));
        steps.add(step("CHAOS", null, all, 5000));
        label = ko ? "연금 → 카오스 반복" : "Alchemy → chaos spam";
      }
    }
    if (steps.isEmpty()) {
      model.addAttribute(
          "result",
          new PoeCraft.StrategyResult(
              0,
              0,
              0,
              java.util.Map.of(),
              null,
              null,
              List.of(),
              ko ? "단계 표에서 화폐를 하나 이상 고르세요" : "Pick at least one currency in the step table"));
      model.addAttribute("presetLabel", label);
      model.addAttribute("targetLabels", labels);
      return "poe/htmx/craftStrategy";
    }
    PoeCraft.StrategyResult res =
        targets.isEmpty()
            ? new PoeCraft.StrategyResult(
                0,
                0,
                0,
                java.util.Map.of(),
                null,
                null,
                List.of(),
                ko ? "목표 옵션의 최소 등급을 하나 이상 고르세요" : "Pick a minimum tier for at least one modifier")
            : craftClient.strategy(
                new PoeCraft.StrategyRequest(startItem(item, params), targets, steps, null));
    model.addAttribute("result", res);
    model.addAttribute("presetLabel", label);
    model.addAttribute("targetLabels", labels);
    List<String> sampleLinks = new ArrayList<>();
    for (PoeCraft.StrategySample sm :
        res.samples() == null ? List.<PoeCraft.StrategySample>of() : res.samples()) {
      sampleLinks.add(sampleLink(sm.item(), params));
    }
    model.addAttribute("sampleLinks", sampleLinks);
    return "poe/htmx/craftStrategy";
  }

  /**
   * 전략 성공 예시를 제작 화면에서 여는 링크(10-22 Q16 — craftofexile 'Load in Crafting Interface'). 가져오기 링크와 같은 모양,
   * 베이스 · 군집 패시브 수는 지금 제작 화면 것(전략 단추가 같이 보낸다).
   */
  private String sampleLink(
      PoeCraft.Item it, org.springframework.util.MultiValueMap<String, String> params) {
    StringBuilder url = new StringBuilder("/poe/craft?itemClass=").append(enc(it.itemClass()));
    url.append("&variant=").append(enc(nz(it.variant())));
    url.append("&influence=").append(enc(nz(it.influence())));
    url.append("&ilvl=").append(it.ilvl());
    url.append("&state=").append(enc(json.writeValueAsString(it)));
    String base = params.getFirst("baseName");
    if (base != null && !base.isBlank()) {
      url.append("&base=").append(enc(base));
    }
    String passives = params.getFirst("passives");
    if (passives != null && passives.matches("\\d{1,2}")) {
      url.append("&passives=").append(passives);
    }
    return url.toString();
  }

  /**
   * 직접 엮은 단계(C344) — s{n} = "화폐[:인자]", u{n} = ""(한 번) | all | pre | suf, m{n} = 최대, g{n} = 다음 줄과
   * 묶음. 묶음은 마지막 줄의 u · m 으로 SEQ 하나가 된다. 모르는 화폐는 건너뛴다(ACTIONS 만).
   */
  static List<PoeCraft.Step> customSteps(
      org.springframework.util.MultiValueMap<String, String> params,
      List<Integer> all,
      List<Integer> pre,
      List<Integer> suf) {
    List<PoeCraft.Step> out = new ArrayList<>();
    List<PoeCraft.Step> group = new ArrayList<>();
    for (int i = 1; i <= 6; i++) {
      String v = params.getFirst("s" + i);
      if (v == null || v.isBlank()) {
        continue;
      }
      String act = v.contains(":") ? v.substring(0, v.indexOf(':')) : v;
      String arg = v.contains(":") ? v.substring(v.indexOf(':') + 1) : null;
      if (!ACTIONS.contains(act)) {
        continue;
      }
      String u = params.getFirst("u" + i);
      List<Integer> until =
          "all".equals(u) ? all : "pre".equals(u) ? pre : "suf".equals(u) ? suf : null;
      Integer max = null;
      try {
        String m = params.getFirst("m" + i);
        max = m == null || m.isBlank() ? null : Math.max(1, Math.min(20_000, Integer.parseInt(m)));
      } catch (NumberFormatException e) {
        max = null;
      }
      if (until != null && until.isEmpty()) {
        until = null;
      }
      boolean grouped = params.getFirst("g" + i) != null;
      if (grouped) {
        group.add(step(act, arg, null, null));
        continue;
      }
      if (!group.isEmpty()) {
        group.add(step(act, arg, null, null));
        out.add(
            new PoeCraft.Step(
                "SEQ", null, until, until == null ? null : (max == null ? 5000 : max), group));
        group = new ArrayList<>();
        continue;
      }
      out.add(step(act, arg, until, until == null ? null : (max == null ? 5000 : max)));
    }
    // 마지막 줄까지 묶음이 열려 있으면 한 번 실행하는 묶음으로
    if (!group.isEmpty()) {
      out.add(new PoeCraft.Step("SEQ", null, null, null, group));
    }
    return out;
  }

  private static PoeCraft.Step step(String action, String arg, List<Integer> until, Integer max) {
    return new PoeCraft.Step(action, arg, until, max, null);
  }

  /** 기폭제 쓴 개수(C385) — note "catalyst:종류:양:개수" → 쓴 화폐 인자 "종류*개수"(더럽혀진 기폭제는 "vaal" 하나). */
  static String catalystSpent(String note, String arg) {
    String base = arg == null ? "" : arg.contains("*") ? arg.substring(0, arg.indexOf('*')) : arg;
    if ("vaal".equals(base) || note == null) {
      return base;
    }
    String[] p = note.split(":");
    return p.length == 4 ? base + "*" + p[3] : base;
  }

  /** 화폐 인자 — 화석 = 고른 화석 쉼표 목록, 작업대 = 옵션 id, 수확 = id, 정복자 = action 값 뒤, 에센스 = 슬러그. */
  static String argFor(
      String act,
      String actArg,
      String essence,
      List<String> fossil,
      String bench,
      String harvest) {
    return switch (act) {
      case "FOSSIL" -> fossil == null ? "" : String.join(",", fossil);
      case "BENCH" -> bench;
      case "HARVEST" -> harvest;
      case "CONQUEROR",
          "UNVEIL",
          "EMBER",
          "ICHOR",
          "AWAKENER",
          "MANUAL_ADD",
          "MANUAL_REMOVE",
          "CATALYST",
          "BEAST",
          "DUCAT" ->
          actArg;
      case "ESSENCE" -> essence;
      default -> null;
    };
  }

  /**
   * 목표 라벨(C373) — 고른 등급 전체 범위: 가장 낮은 등급의 최소 굴림 ~ T1 의 최대 굴림(전엔 T1 최대값만 보여 "T1~T3" 목표를 잘못 읽게 했다). in
   * = 등급 순(T1 먼저).
   */
  static String spanLabel(List<PoeCraft.PoolLine> in, boolean ko) {
    PoeCraft.PoolLine best = in.get(0);
    PoeCraft.PoolLine worst = in.get(in.size() - 1);
    boolean useKo = ko && best.ko() != null && !best.ko().isEmpty();
    List<String> mx = useKo ? best.ko() : best.en();
    List<String> mn = useKo ? worst.koMin() : worst.enMin();
    List<String> out = new ArrayList<>();
    for (int i = 0; i < mx.size(); i++) {
      out.add(
          net.luversof.web.gate.poe.PoeModRange.range(
              mn != null && i < mn.size() ? mn.get(i) : null, mx.get(i)));
    }
    return String.join(", ", out);
  }

  /** 같은 종류 · 변형 · 영향력 · 아이템 레벨인가(아니면 새로 시작). */
  private static boolean same(PoeCraft.Item a, PoeCraft.Item b) {
    return a.itemClass().equals(b.itemClass())
        && nz(a.variant()).equals(nz(b.variant()))
        && nz(a.influence()).equals(nz(b.influence()))
        && a.ilvl() == b.ilvl();
  }

  private static String nz(String s) {
    return s == null ? "" : s;
  }

  private static String label(PoeModItemClass c) {
    return net.luversof.web.gate.poe.PoeText.isKorean() && c.nameKo() != null
        ? c.nameKo()
        : c.name();
  }

  private static int clamp(int ilvl) {
    return Math.max(1, Math.min(100, ilvl));
  }

  /** 확률 표 계열 묶음에서 PoE1 줄 꺼내기(10-21). */
  private static final net.luversof.web.gate.poe.CraftPoolGroups.Accessors<PoeCraft.PoolLine>
      POOL_ACC =
          new net.luversof.web.gate.poe.CraftPoolGroups.Accessors<>(
              PoeCraft.PoolLine::id,
              PoeCraft.PoolLine::gen,
              PoeCraft.PoolLine::family,
              PoeCraft.PoolLine::tierRank,
              PoeCraft.PoolLine::chance);

  /**
   * 확률 표 계열 묶음(10-21 사용자 요청 "옵션별로 묶어 줄을 줄이고 누르면 등급 상세, 제작으로 붙은 옵션은 표시") — 이 종류의 전체 계열(일반 → 레어 풀,
   * 계산기 목표와 같은 풀)을 고정 순서로 두고 pool 의 확률(지금 아이템 · 고른 화폐 기준 P3) · 붙은 옵션을 얹는다. 못 읽으면 pool 만으로 묶는다.
   */
  private List<net.luversof.web.gate.poe.CraftPoolGroups.Family<PoeCraft.PoolLine>> poolGroups(
      PoeCraft.View view, List<PoeCraft.PoolLine> pool, java.util.Set<String> freshIds) {
    PoeCraft.Item it = view.item();
    List<PoeCraft.PoolLine> full;
    try {
      full =
          craftClient.pool(
              new PoeCraft.PoolRequest(
                  new PoeCraft.Item(
                      it.itemClass(),
                      it.variant(),
                      it.influence(),
                      it.ilvl(),
                      "NORMAL",
                      List.of(),
                      false),
                  "RARE"));
    } catch (RuntimeException e) {
      full = List.of();
    }
    java.util.Set<String> onIds = new java.util.HashSet<>();
    if (view.lines() != null) {
      view.lines().forEach(l -> onIds.add(l.id()));
    }
    return net.luversof.web.gate.poe.CraftPoolGroups.group(
        full == null ? List.of() : full, pool, POOL_ACC, onIds, freshIds);
  }

  private void fill(
      Model model,
      PoeCraft.Item item,
      List<PoeCraft.Item> hist,
      PoeCraft.View applied,
      String lastAction) {
    PoeCraft.View view =
        applied != null && applied.error() == null ? applied : craftClient.view(item);
    model.addAttribute("view", view);
    model.addAttribute("error", applied == null ? null : applied.error());
    model.addAttribute("lastAction", lastAction);
    model.addAttribute(
        "added", applied == null || applied.error() != null ? List.of() : applied.added());
    model.addAttribute("stateJson", json.writeValueAsString(view.item()));
    model.addAttribute("historyJson", json.writeValueAsString(hist));
    model.addAttribute("historySize", hist.size());
    // 다음에 붙을 옵션 — 일반이면 레어 자리로(연금 · 에센스), 그 밖엔 지금 희귀도
    List<PoeCraft.PoolLine> pool = craftClient.pool(new PoeCraft.PoolRequest(view.item(), null));
    model.addAttribute("pool", pool);
    java.util.Set<String> freshIds =
        applied == null || applied.error() != null || applied.added() == null
            ? java.util.Set.of()
            : new java.util.HashSet<>(applied.added());
    model.addAttribute("poolGroups", poolGroups(view, pool, freshIds));
    PoeCraft.Options opts = craftClient.options(item.itemClass(), nz(item.variant()));
    model.addAttribute("options", opts);
    model.addAttribute("essences", opts.essences());
  }

  /**
   * 계산 · 전략의 출발 아이템(C349) — "지금 아이템에서 시작"(fromState=1)이면 에뮬레이터 상태(같은 종류 · 변형 · 아이템 레벨일 때만, API 가 분열
   * · 고정 속성만 남긴다), 아니면 고른 종류의 일반 아이템.
   */
  PoeCraft.Item startItem(
      PoeCraft.Item picked, org.springframework.util.MultiValueMap<String, String> params) {
    if (!"1".equals(params.getFirst("fromState"))) {
      return picked;
    }
    PoeCraft.Item cur = parse(params.getFirst("state"), null);
    return cur != null
            && picked.itemClass().equals(cur.itemClass())
            && nz(picked.variant()).equals(nz(cur.variant()))
            && picked.ilvl() == cur.ilvl()
        ? cur
        : picked;
  }

  private PoeCraft.Item parse(String s, PoeCraft.Item fallback) {
    if (s == null || s.isBlank()) {
      return fallback;
    }
    try {
      PoeCraft.Item it = json.readValue(s, PoeCraft.Item.class);
      return it == null || it.itemClass() == null ? fallback : it;
    } catch (RuntimeException e) {
      return fallback;
    }
  }

  /** 굴림 씨앗(C370) — 숨은 칸 JSON(옵션 id → 씨앗). 깨졌으면 빈 맵. */
  private java.util.Map<String, Long> parseRolls(String s) {
    try {
      java.util.Map<String, Long> m =
          json.readValue(s, new TypeReference<java.util.LinkedHashMap<String, Long>>() {});
      return m == null ? new java.util.LinkedHashMap<>() : m;
    } catch (RuntimeException e) {
      return new java.util.LinkedHashMap<>();
    }
  }

  /** 지금 옵션에 맞춘다 — 새 옵션은 새 씨앗, 사라진 옵션은 지운다. reroll 이면 분열 아닌 옵션 씨앗을 모두 새로(신성한 오브). */
  private void rollsModel(
      Model model,
      java.util.List<String> ids,
      java.util.Set<String> fixed,
      java.util.Map<String, Long> rolls,
      boolean reroll) {
    java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
    // 행운 표시(C399) "lucky:id" 는 그 옵션이 남아 있으면 둔다
    rolls
        .keySet()
        .removeIf(
            k -> !(ids.contains(k) || (k.startsWith("lucky:") && ids.contains(k.substring(6)))));
    for (String id : ids) {
      boolean rerolled = rolls.containsKey(id) && reroll && !fixed.contains(id);
      if (!rolls.containsKey(id) || rerolled) {
        rolls.put(id, r.nextLong());
      }
      if (rerolled) {
        // 신성한 오브로 다시 굴리면 보통 굴림(행운은 축성된 화석으로 붙을 때만 — 처음 씨앗엔 그대로)
        rolls.remove("lucky:" + id);
      }
    }
    model.addAttribute("rolls", rolls);
    model.addAttribute("rollsJson", json.writeValueAsString(rolls));
  }

  /** 이력과 짝인 굴림 씨앗(C371) — 숨은 칸 JSON. 깨졌으면 빈 목록. */
  private List<java.util.Map<String, Long>> parseRollHist(String s) {
    try {
      List<java.util.LinkedHashMap<String, Long>> l =
          json.readValue(
              s,
              new tools.jackson.core.type.TypeReference<
                  List<java.util.LinkedHashMap<String, Long>>>() {});
      return l == null ? new ArrayList<>() : new ArrayList<java.util.Map<String, Long>>(l);
    } catch (RuntimeException e) {
      return new ArrayList<>();
    }
  }

  /** 옵션 씨앗 키 — 옵션 id, 고정 속성은 "imp:" + id(C375). */
  private static java.util.Set<String> explicitIds(PoeCraft.Item item) {
    return item.affixes() == null
        ? java.util.Set.of()
        : item.affixes().stream()
            .map(PoeCraft.Affix::id)
            .collect(java.util.stream.Collectors.toSet());
  }

  private static java.util.List<String> ids(PoeCraft.Item item) {
    if (item.implicits() != null && !item.implicits().isEmpty()) {
      java.util.List<String> all = new ArrayList<>(idsExplicit(item));
      item.implicits().forEach(a -> all.add("imp:" + a.id()));
      return all;
    }
    return idsExplicit(item);
  }

  private static java.util.List<String> idsExplicit(PoeCraft.Item item) {
    return item.affixes() == null
        ? List.of()
        : item.affixes().stream().map(PoeCraft.Affix::id).toList();
  }

  /** 신성한 오브가 안 건드리는 씨앗 — 분열 옵션 · 고정 속성(C375). */
  private static java.util.Set<String> fixedIds(PoeCraft.Item item) {
    java.util.Set<String> out = new java.util.HashSet<>();
    if (item.affixes() != null) {
      item.affixes().stream().filter(PoeCraft.Affix::fractured).forEach(a -> out.add(a.id()));
    }
    if (item.implicits() != null) {
      item.implicits().forEach(a -> out.add("imp:" + a.id()));
    }
    return out;
  }

  /** 쓴 화폐 기록(C365) — 숨은 칸 JSON. 깨졌으면 빈 기록. */
  private List<PoeCraft.SpentStep> parseSpent(String s) {
    try {
      List<PoeCraft.SpentStep> l =
          json.readValue(s, new TypeReference<List<PoeCraft.SpentStep>>() {});
      return l == null ? new ArrayList<>() : new ArrayList<>(l);
    } catch (RuntimeException e) {
      return new ArrayList<>();
    }
  }

  /** 쓴 화폐 표(C365) — 기록이 있으면 API 로 화폐별 개수 · 단가 · 합계. API 가 실패해도 화면은 그린다. */
  private PoeCraft.SpentResult spentModel(
      Model model, String itemClass, List<PoeCraft.SpentStep> log) {
    model.addAttribute("spentJson", json.writeValueAsString(log));
    PoeCraft.SpentResult r = null;
    if (!log.isEmpty()) {
      try {
        r = craftClient.spent(new PoeCraft.SpentRequest(itemClass, log));
      } catch (RuntimeException e) {
        r = null;
      }
    }
    model.addAttribute("spent", r);
    return r;
  }

  /**
   * 단계 기록(C455, PoE2 C453 의 짝) — 쓴 화폐 기록 한 줄 = 한 단계. 머리카락 · 신성한 오브 · 축복의 오브는 아이템을 이력에 안 남기는
   * 단계(되돌리기와 같은 규칙), 나머지 단계 j 는 이력 hist[j - 빠진 수] → 다음 상태. 붙은 · 지운 옵션은 상태마다 옵션 줄(API 한 번)을 id 로 대조.
   * 최근 단계가 위.
   */
  private void historyModel(
      Model model,
      PoeCraft.Item current,
      List<PoeCraft.Item> hist,
      List<PoeCraft.SpentStep> log,
      PoeCraft.SpentResult spent,
      // 상태마다 굴림 씨앗(C512) — hist + 지금, 단계 기록의 바뀐 옵션을 그때 굴린 값으로
      List<java.util.Map<String, Long>> rolls) {
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    int nf = (int) log.stream().filter(s -> !sideStep(s)).count();
    int dropped = Math.max(0, nf - hist.size());
    List<List<PoeCraft.Line>> lines = null;
    if (!hist.isEmpty()) {
      List<PoeCraft.Item> states = new ArrayList<>(hist);
      states.add(current);
      try {
        lines = craftClient.lines(states);
      } catch (RuntimeException e) {
        lines = null;
      }
    }
    List<PoeCraft.HistRow> rows = new ArrayList<>();
    double cum = 0;
    boolean cumKnown = true;
    int j = -1;
    for (int k = 0; k < log.size(); k++) {
      PoeCraft.SpentStep s = log.get(k);
      PoeCraft.StepCost c =
          spent != null && spent.steps() != null && k < spent.steps().size()
              ? spent.steps().get(k)
              : null;
      List<String> nm = c == null ? List.of() : ko ? c.namesKo() : c.names();
      String label = nm == null || nm.isEmpty() ? stepLabel(s, ko) : String.join(" + ", nm);
      Double chaos = c == null ? null : c.chaos();
      if (chaos == null) {
        cumKnown = false;
      } else {
        cum += chaos;
      }
      List<String> add = List.of();
      List<String> rem = List.of();
      Integer undoTo = null;
      boolean now = false;
      String note = null;
      if (sideStep(s)) {
        note =
            "FORESEE".equals(s.action())
                ? (ko ? "미리 보기 — 아이템은 그대로" : "Foresee — item unchanged")
                : (ko ? "수치만 다시 굴림 — 되돌리기 대상 아님" : "Values rerolled — not undoable");
      } else {
        j++;
        int i = j - dropped;
        if (i >= 0 && lines != null && i + 1 < lines.size()) {
          java.util.Map<String, Long> after =
              rolls != null && rolls.size() == lines.size() ? rolls.get(i + 1) : null;
          java.util.Map<String, Long> before =
              rolls != null && rolls.size() == lines.size() ? rolls.get(i) : null;
          add = lineDiff(lines.get(i + 1), lines.get(i), ko, after);
          rem = lineDiff(lines.get(i), lines.get(i + 1), ko, before);
          if (i + 1 < hist.size()) {
            undoTo = i + 1;
          } else {
            now = true;
          }
        } else if (i < 0) {
          note =
              ko
                  ? "기록 한도(" + HISTORY_MAX + "단계) 밖 — 되돌릴 수 없음"
                  : "Beyond the history limit — not undoable";
        }
      }
      rows.add(
          new PoeCraft.HistRow(
              k + 1, label, chaos, cumKnown ? cum : null, add, rem, undoTo, now, note));
    }
    java.util.Collections.reverse(rows);
    model.addAttribute("historyRows", rows);
    // 맨 처음(또는 기록 한도 안 가장 오래된) 상태로 — 이력이 있을 때만
    model.addAttribute("historyStartUndo", hist.isEmpty() ? null : 0);
    model.addAttribute("historyDropped", dropped);
  }

  /** 아이템을 이력에 안 남기는 단계 — 머리카락 · 신성한 오브 · 축복의 오브(되돌리기와 같은 규칙). */
  private static boolean sideStep(PoeCraft.SpentStep s) {
    return "FORESEE".equals(s.action())
        || "DIVINE".equals(s.action())
        || "BLESSED".equals(s.action());
  }

  /** 쓴 화폐 이름이 없을 때(직접 편집 · 장막 해제 · 각인 복원 등) 단계 이름. */
  private static String stepLabel(PoeCraft.SpentStep s, boolean ko) {
    String a = s.action() == null ? "" : s.action();
    return switch (a) {
      case "UNVEIL" -> ko ? "장막 해제" : "Unveil";
      case "MANUAL_ADD" -> ko ? "직접 편집 — 넣기" : "Manual add";
      case "MANUAL_REMOVE" -> ko ? "직접 편집 — 빼기" : "Manual remove";
      case "IMPRINT_RESTORE" -> ko ? "각인으로 되돌리기" : "Restore imprint";
      default -> a;
    };
  }

  /** 상태마다 굴림 씨앗 — 이력의 씨앗 + 지금(C512). */
  private static List<java.util.Map<String, Long>> rollStates(
      List<java.util.Map<String, Long>> rollHist, java.util.Map<String, Long> now) {
    List<java.util.Map<String, Long>> out = new ArrayList<>(rollHist);
    out.add(now);
    return out;
  }

  /** a 에만 있는 옵션 줄의 문구(id 대조) — "접두 T1 문구", 고정 속성은 진영 · 타락. */
  private static List<String> lineDiff(
      List<PoeCraft.Line> a, List<PoeCraft.Line> b, boolean ko, java.util.Map<String, Long> rolls) {
    java.util.Set<String> bIds = new java.util.HashSet<>();
    for (PoeCraft.Line l : b) {
      bIds.add(l.gen() + ":" + l.id());
    }
    List<String> out = new ArrayList<>();
    for (PoeCraft.Line l : a) {
      if (bIds.contains(l.gen() + ":" + l.id())) {
        continue;
      }
      List<String> mx = ko && l.ko() != null && !l.ko().isEmpty() ? l.ko() : l.en();
      List<String> mn = ko && l.koMin() != null && !l.koMin().isEmpty() ? l.koMin() : l.enMin();
      // 범위로(C511 — 최대 굴림 문구라 카드의 굴린 값과 달라 보였다 · 지난 단계의 굴린 값은 남기지 않는다)
      List<String> t = new ArrayList<>();
      Long seed = rolls == null ? null : rolls.get(l.id());
      for (int x = 0; mx != null && x < mx.size(); x++) {
        String range =
            net.luversof.web.gate.poe.PoeModRange.range(
                mn != null && x < mn.size() ? mn.get(x) : null, mx.get(x));
        // 그 단계에 굴린 값(C512) — 씨앗이 없으면 범위
        t.add(seed == null ? range : net.luversof.web.gate.poe.PoeModRoll.roll(range, seed + x));
      }
      String side =
          switch (l.gen() == null ? "" : l.gen()) {
            case "prefix" -> ko ? "접두 " : "P ";
            case "suffix" -> ko ? "접미 " : "S ";
            default -> (ko && l.nameKo() != null ? l.nameKo() : nz(l.name())) + " ";
          };
      out.add(
          side
              + (l.tierRank() == null ? "" : "T" + l.tierRank() + " ")
              + String.join(", ", t == null || t.isEmpty() ? List.of(l.id()) : t));
    }
    return out;
  }

  private List<PoeCraft.Item> parseHistory(String s) {
    try {
      List<PoeCraft.Item> l = json.readValue(s, new TypeReference<List<PoeCraft.Item>>() {});
      return l == null ? new ArrayList<>() : new ArrayList<>(l);
    } catch (RuntimeException e) {
      return new ArrayList<>();
    }
  }

  /** 에뮬레이터 따라 하기(C464) — 최적 조합 작업이 있으면 지금 아이템의 다음 수단. 작업이 사라졌거나 실패하면 표시 안 함. */
  private void recommend(Model model, String optJob) {
    if (optJob == null || optJob.isBlank()) {
      return;
    }
    Object sj = model.getAttribute("stateJson");
    PoeCraft.Item it = sj == null ? null : parse(sj.toString(), null);
    if (it == null) {
      return;
    }
    // 각인(C468) — 에뮬레이터 각인도 넘긴다(최적 조합이 각인 · 되돌리기를 쓴다)
    Object ij = model.getAttribute("imprintJson");
    PoeCraft.Item imp = ij == null || ij.toString().isBlank() ? null : parse(ij.toString(), null);
    try {
      model.addAttribute(
          "recommend", craftClient.optimizeNext(optJob, new PoeCraft.OptimizeNextRequest(it, imp)));
      model.addAttribute("optJob", optJob);
    } catch (RuntimeException e) {
      // 작업이 사라졌으면(10분) 표시 안 함
    }
  }
}
