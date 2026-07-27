package net.luversof.web.gate.poe.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.poe.dto.PoeGroups;
import net.luversof.web.gate.poe.httpexchange.PoeBuildClient;
import net.luversof.web.gate.poe.httpexchange.PoeDataClient;
import net.luversof.web.gate.poe.httpexchange.PoeExtractClient;

/**
 * PoE 화면 컨트롤러 — 데이터/엔진은 bluesky-api-poe 로 위임하고 여기선 뷰만 조립한다. 정적 게임 에셋(passive-tree.json/아이콘)은 게이트가
 * {@code poe.data-dir}(dev=로컬, k8s=공유 볼륨)에서 계속 서빙하므로 트리 존재 여부만 로컬로 확인한다.
 */
@Controller
@RequestMapping(value = "/poe", produces = MediaType.TEXT_HTML_VALUE)
public class PoeViewController {

  private final PoeDataClient poeDataClient;
  private final PoeBuildClient poeBuildClient;
  private final PoeExtractClient poeExtractClient;
  private final String dataDir;

  public PoeViewController(
      PoeDataClient poeDataClient,
      PoeBuildClient poeBuildClient,
      PoeExtractClient poeExtractClient,
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.poeDataClient = poeDataClient;
    this.poeBuildClient = poeBuildClient;
    this.poeExtractClient = poeExtractClient;
    this.dataDir = dataDir;
  }

  @GetMapping
  public String gems(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String type,
      @RequestParam(required = false, defaultValue = "all") String color,
      @RequestParam(required = false, defaultValue = "all") String tag,
      Model model) {
    var meta = poeDataClient.gemMeta();
    model.addAttribute("patch", meta.patch());
    model.addAttribute("totalCount", meta.totalCount());
    model.addAttribute("tagGroups", poeDataClient.gemTagGroups());
    model.addAttribute("initialQ", q == null ? "" : q);
    model.addAttribute("initialType", type);
    model.addAttribute("initialColor", color);
    model.addAttribute("activeTag", tag);
    return "poe/gems";
  }

  /** 젬 상세 페이지 — 호버 레이어(툴팁)를 그대로 바닥에 + 레벨별 진행표 등 상세 정보. */
  @GetMapping("/gems/{slug}")
  public String gemPage(
      @org.springframework.web.bind.annotation.PathVariable String slug,
      @RequestParam(required = false, defaultValue = "20") int level,
      Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("gem", poeDataClient.gem(slug));
    model.addAttribute("displayLevel", level);
    return "poe/gemPage";
  }

  /** 고유 아이템 상세 페이지 — 툴팁 + 베이스/영문/요구 정보. */
  @GetMapping("/uniques/{slug}")
  public String uniquePage(
      @org.springframework.web.bind.annotation.PathVariable String slug, Model model) {
    var item = poeDataClient.unique(slug);
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("item", item);
    model.addAttribute("base", poeDataClient.baseItemByName(item.baseType()));
    return "poe/uniquePage";
  }

  /** 베이스 아이템 상세 페이지 — 툴팁 + 모드 풀(티어표). */
  @GetMapping("/items/{slug}")
  public String itemPage(
      @org.springframework.web.bind.annotation.PathVariable String slug,
      @RequestParam(required = false, defaultValue = "") String influence,
      Model model) {
    var item = poeDataClient.baseItem(slug);
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("item", item);
    model.addAttribute("modFamilies", poeDataClient.modFamiliesForItemClass(item.itemClass()));
    // 상세 페이지엔 전체 모드 풀(poedb식)도 함께 — 장비 클래스만 mods.json 에 있고 플라스크/주얼은 없어 없으면 생략.
    boolean isEquipClass =
        poeDataClient.modItemClasses().stream()
            .anyMatch(c -> c.itemClass().equals(item.itemClass()));
    model.addAttribute(
        "fullMods",
        isEquipClass
            ? poeDataClient.modsForItemClass(item.itemClass(), variantOf(item), influence)
            : null);
    // 엘드리치 임플리싯(총주교/포식자) — 방어구·목걸이 등 일부 슬롯만 부여 가능. 대상 아니면 API 가 null 반환.
    model.addAttribute("eldritch", poeDataClient.eldritchForItemClass(item.itemClass()));
    model.addAttribute(
        "anoints", "Amulet".equals(item.itemClass()) ? poeDataClient.anoints() : null);
    model.addAttribute("essences", poeDataClient.essencesForItemClass(item.itemClass()));
    model.addAttribute("bench", poeDataClient.benchForItemClass(item.itemClass()));
    // poedb 식 — 이 베이스를 쓰는 고유 아이템 목록(클래스 검색 후 baseType 일치로 좁힘)
    model.addAttribute(
        "baseUniques",
        poeDataClient.searchUniques(null, item.itemClass()).stream()
            .filter(u -> item.name().equals(u.baseType()))
            .toList());
    model.addAttribute("slug", slug);
    return "poe/itemPage";
  }

  /**
   * 이 베이스의 속성 변형 태그를 요구 속성으로 추론한다 — 방어구는 변형마다 붙는 모드가 달라(ES=지능, 방어도=힘, 회피=민첩) 그 베이스에 맞는 풀을 보여줘야 한다.
   * 요구 속성이 없으면 빈 값(첫 변형/기본 풀).
   */
  private static String variantOf(net.luversof.web.gate.poe.dto.PoeBaseItem item) {
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

  @GetMapping("/uniques")
  public String uniques(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String slot,
      Model model) {
    var groups = poeDataClient.uniqueCategoryGroups();
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("totalCount", poeDataClient.uniqueMeta().totalCount());
    model.addAttribute("categoryGroups", groups);
    model.addAttribute("initialQ", q == null ? "" : q);
    model.addAttribute("activeValue", resolveSlot(groups, slot));
    return "poe/uniques";
  }

  /** 탭 전환 진입 slot 을 이 페이지의 필터 키로 해석 — slot 을 가진 첫 항목의 key, 없으면 all. */
  private static String resolveSlot(List<? extends PoeGroups.Group> groups, String slot) {
    if (slot == null || slot.isBlank()) {
      return "all";
    }
    for (PoeGroups.Group group : groups) {
      for (PoeGroups.Entry entry : group.entries()) {
        // 정규 슬롯명(탭 전환 링크) 외에 칩 키도 허용 — 필터 URL 동기화(?slot=키)가 새로고침에서 복원되도록
        if (slot.equals(entry.slot()) || slot.equals(entry.key())) {
          return entry.key();
        }
      }
    }
    return "all";
  }

  /** trees/<ver> 아카이브 목록을 싣고, 유효한 ver 선택 시 그 스냅샷의 데이터 경로를 돌려준다(무효/미지정=현행). */
  private String[] applyTreeVersion(Model model, String ver, String treeFile, String spritesFile) {
    List<String> versions = List.of();
    Path root = Path.of(dataDir, "trees");
    if (Files.isDirectory(root)) {
      try (var stream = Files.list(root)) {
        versions =
            stream
                .filter(Files::isDirectory)
                .map(p -> p.getFileName().toString())
                // "3.9" < "3.28" — 문자열 비교가 아니라 숫자 비교로 내림차순
                .sorted(
                    java.util.Comparator.comparingDouble(
                            (String v) -> {
                              String[] parts = v.split("\\.");
                              return Double.parseDouble(parts[0]) * 1000
                                  + (parts.length > 1 ? Double.parseDouble(parts[1]) : 0);
                            })
                        .reversed())
                .toList();
      } catch (java.io.IOException ignored) {
        // 아카이브 목록을 못 읽어도 현행 트리는 그대로 보여준다
      }
    }
    model.addAttribute("treeVersions", versions);
    boolean archived = ver != null && versions.contains(ver);
    model.addAttribute("activeVer", archived ? ver : "");
    // 아카이브 뷰일 때 그 스냅샷의 실제 패치(3.28.0.16 등)를 배지에 실어 "정말 바뀌었는지" 즉시 보이게 한다.
    // trees/index.json = [{"ver":"3.28","patch":"3.28.0.16"}, ...] 을 가볍게 정규식으로 읽는다(작은 파일).
    model.addAttribute("activePatch", archived ? patchForVersion(ver) : "");
    return archived
        ? new String[] {
          "/poe-data/trees/" + ver + "/" + treeFile, "/poe-data/trees/" + ver + "/" + spritesFile
        }
        : new String[] {"/poe-data/" + treeFile, "/poe-data/" + spritesFile};
  }

  /** trees/index.json 에서 해당 ver 의 게임 패치 문자열을 찾는다(없으면 빈 문자열). */
  private String patchForVersion(String ver) {
    try {
      String json = Files.readString(Path.of(dataDir, "trees", "index.json"));
      var m =
          java.util.regex.Pattern.compile(
                  "\"ver\"\\s*:\\s*\""
                      + java.util.regex.Pattern.quote(ver)
                      + "\"\\s*,\\s*\"patch\"\\s*:\\s*\"([^\"]+)\"")
              .matcher(json);
      if (m.find()) {
        return m.group(1);
      }
    } catch (java.io.IOException ignored) {
      // index.json 이 없거나 못 읽으면 패치 표기 없이 버전만 배지에 남는다
    }
    return "";
  }

  @GetMapping("/tree")
  public String tree(@RequestParam(required = false) String ver, Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("hasTreeData", Files.exists(Path.of(dataDir, "passive-tree.json")));
    String[] src = applyTreeVersion(model, ver, "passive-tree.json", "tree-sprites-skill.json");
    model.addAttribute("treeSrc", src[0]);
    model.addAttribute("spritesSrc", src[1]);
    // 트리 계산에 쓸 주 스킬 후보 — datalist 로 넘겨 브라우저 기본 검색을 그대로 쓴다
    model.addAttribute("activeGems", poeDataClient.searchGems(null, "active", "all", null));
    // 주얼 슬롯에 끼울 유니크 주얼 목록.
    // 타임리스(무궁한)는 **포함**한다 — 정복자·시드를 고르면 PoB 가 반경 변환을 실제로 계산한다(사이클 확인).
    // 클러스터는 전용 소켓·전용 UI(우클릭 → 클러스터 주얼 장착)로 다루므로 여기선 제외.
    model.addAttribute(
        "jewelUniques",
        // searchUniques 의 2번째 인자는 itemClass(세분류)라 "jewel" 로는 안 잡힌다 → 전체를 받아 category 로 거른다
        poeDataClient.searchUniques(null, "all").stream()
            .filter(u -> "jewel".equals(u.category()))
            .filter(u -> u.baseType() == null || !u.baseType().contains("Cluster"))
            .toList());
    return "poe/tree";
  }

  @GetMapping("/atlas")
  public String atlas(@RequestParam(required = false) String ver, Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("hasAtlasData", Files.exists(Path.of(dataDir, "atlas-tree.json")));
    String[] src = applyTreeVersion(model, ver, "atlas-tree.json", "tree-sprites-atlas.json");
    model.addAttribute("treeSrc", src[0]);
    model.addAttribute("spritesSrc", src[1]);
    return "poe/atlas";
  }

  @GetMapping("/items")
  public String items(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String slot,
      Model model) {
    var groups = poeDataClient.itemClassGroups();
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("totalCount", poeDataClient.baseItemMeta().totalCount());
    model.addAttribute("classGroups", groups);
    model.addAttribute("initialQ", q == null ? "" : q);
    model.addAttribute("activeValue", resolveSlot(groups, slot));
    return "poe/items";
  }

  /** 모드(poedb Modifiers식) 페이지 — 아이템 클래스 선택 시 접두/접미 티어 사다리 표시. */
  @GetMapping("/mods")
  public String mods(
      @RequestParam(required = false) String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      Model model) {
    var classes = poeDataClient.modItemClasses();
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("modClasses", classes);
    // 미지정이면 첫 클래스, 유효하지 않은 값이면 첫 클래스로 폴백
    String active =
        itemClass != null && classes.stream().anyMatch(c -> c.itemClass().equals(itemClass))
            ? itemClass
            : classes.isEmpty() ? null : classes.get(0).itemClass();
    model.addAttribute("activeClass", active);
    // 아뮬렛엔 poedb 속성부여식 도유 목록도 — 도유는 아뮬렛 전용 부여라 다른 클래스엔 무의미
    model.addAttribute("anoints", "Amulet".equals(active) ? poeDataClient.anoints() : null);
    model.addAttribute(
        "essences", active == null ? null : poeDataClient.essencesForItemClass(active));
    model.addAttribute("bench", active == null ? null : poeDataClient.benchForItemClass(active));
    model.addAttribute(
        "mods", active == null ? null : poeDataClient.modsForItemClass(active, variant, influence));
    model.addAttribute(
        "eldritch", active == null ? null : poeDataClient.eldritchForItemClass(active));
    return "poe/mods";
  }

  /** 클러스터 주얼 노터블 페이지 — craftofexile Cluster 식 브라우징(검색). */
  @GetMapping("/clusters")
  public String clusters(Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("notables", poeDataClient.clusterNotables());
    return "poe/clusters";
  }

  /** 문신 목록 페이지 — poedb Tattoos 식 브라우징(타입 칩 + 검색). */
  @GetMapping("/tattoos")
  public String tattoos(Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("tattoos", poeDataClient.tattoos());
    return "poe/tattoos";
  }

  @GetMapping("/build")
  public String build(Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    return "poe/build";
  }

  @GetMapping("/sim")
  public String sim(
      @RequestParam(required = false, defaultValue = "") String treeNodes,
      @RequestParam(required = false, defaultValue = "") String masteries,
      @RequestParam(required = false, defaultValue = "") String jewels,
      @RequestParam(required = false, defaultValue = "") String clusters,
      @RequestParam(required = false, defaultValue = "") String tattoos,
      @RequestParam(required = false, defaultValue = "") String anoint,
      // 상세 페이지 "이 젬/유니크로 최적화" 바로가기 — 멀티셀렉트 프리셀렉트(콤마 구분 slug)
      @RequestParam(required = false, defaultValue = "") String skills,
      @RequestParam(required = false, defaultValue = "") String uniques,
      @RequestParam(required = false, defaultValue = "") String className,
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      Model model) {
    model.addAttribute("treeNodes", treeNodes);
    model.addAttribute("masteries", masteries);
    model.addAttribute("jewels", jewels);
    model.addAttribute("clusters", clusters);
    model.addAttribute("tattoos", tattoos);
    model.addAttribute("anoint", anoint);
    model.addAttribute("preSkills", java.util.Set.of(skills.split(",")));
    model.addAttribute("preUniques", java.util.Set.of(uniques.split(",")));
    // 고정 트리는 그 직업의 시작점에서만 연결된다 — 직업을 함께 고정하지 않으면 트리가 통째로 버려진다
    model.addAttribute("fixedClassName", className);
    model.addAttribute("fixedAscendancy", ascendancy);
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute(
        "activeGems",
        poeDataClient.searchGems(null, "active", "all", null).stream()
            .sorted(
                java.util.Comparator.comparing(
                    gem -> gem.nameKo() != null ? gem.nameKo() : gem.name()))
            .toList());
    // 강제 장착 후보 유니크 — 아이템 슬롯에 장착 가능한 것만(주얼/팅크처/낚시 제외)
    java.util.Set<String> nonEquip = java.util.Set.of("jewel", "tincture", "fishing");
    model.addAttribute(
        "uniqueItems",
        poeDataClient.searchUniques(null, "all").stream()
            .filter(u -> u.category() == null || !nonEquip.contains(u.category()))
            .sorted(java.util.Comparator.comparing(u -> u.nameKo() != null ? u.nameKo() : u.name()))
            .toList());
    return "poe/sim";
  }

  @GetMapping("/admin")
  public String admin(Model model) {
    var version = poeExtractClient.version();
    model.addAttribute("patch", version.dataPatch());
    model.addAttribute("latestPatch", version.latestPatch());
    model.addAttribute("upToDate", version.upToDate());
    model.addAttribute("gemCount", poeDataClient.gemMeta().totalCount());
    model.addAttribute("uniqueCount", poeDataClient.uniqueMeta().totalCount());
    model.addAttribute("baseItemCount", poeDataClient.baseItemMeta().totalCount());
    model.addAttribute("hasTreeData", Files.exists(Path.of(dataDir, "passive-tree.json")));
    model.addAttribute("hasAtlasData", Files.exists(Path.of(dataDir, "atlas-tree.json")));
    model.addAttribute("dataDir", dataDir);
    model.addAttribute("imageMagickInstalled", poeExtractClient.status().imageMagickInstalled());
    model.addAttribute("engineAvailable", poeBuildClient.available());
    return "poe/admin";
  }
}
