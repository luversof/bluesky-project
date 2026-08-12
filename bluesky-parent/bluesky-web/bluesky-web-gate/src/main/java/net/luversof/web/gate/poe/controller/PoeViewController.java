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

  /**
   * 상세 페이지의 "찾을 수 없음" — 오타·오래된 북마크·데이터 갱신으로 사라진 slug 로 들어온 경우.
   *
   * <p>이전엔 API 404 가 게이트 공통 예외 핸들러로 흘러 <b>200 + 본문 0바이트</b>가 나갔다(실측: 젬·고유·일반 상세 3종 모두 백지). 사용자는 아무
   * 설명도 못 보고, 검색엔진·모니터링도 정상 응답으로 읽는다. 그래서 여기서 잡아 404 상태로 안내 화면을 그린다.
   */
  private String notFound(
      jakarta.servlet.http.HttpServletResponse response,
      Model model,
      String slug,
      String messageKey,
      String listUrl,
      String listLabelKey) {
    response.setStatus(org.springframework.http.HttpStatus.NOT_FOUND.value());
    model.addAttribute("notFoundSlug", slug);
    model.addAttribute("notFoundMessageKey", messageKey);
    model.addAttribute("notFoundListUrl", listUrl);
    model.addAttribute("notFoundListLabelKey", listLabelKey);
    return "poe/notFound";
  }

  @GetMapping("/gems/{slug}")
  public String gemPage(
      @org.springframework.web.bind.annotation.PathVariable String slug,
      @RequestParam(required = false, defaultValue = "20") int level,
      Model model,
      jakarta.servlet.http.HttpServletResponse response) {
    net.luversof.web.gate.poe.dto.PoeGem gem;
    try {
      gem = poeDataClient.gem(slug);
    } catch (RuntimeException e) {
      return notFound(response, model, slug, "poe.notfound.gem", "/poe", "layout.menu.poe.gems");
    }
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("gem", gem);
    model.addAttribute("displayLevel", level);
    return "poe/gemPage";
  }

  /** 고유 아이템 상세 페이지 — 툴팁 + 베이스/영문/요구 정보. */
  @GetMapping("/uniques/{slug}")
  public String uniquePage(
      @org.springframework.web.bind.annotation.PathVariable String slug,
      Model model,
      jakarta.servlet.http.HttpServletResponse response) {
    net.luversof.web.gate.poe.dto.PoeUniqueItem item;
    try {
      item = poeDataClient.unique(slug);
    } catch (RuntimeException e) {
      return notFound(
          response, model, slug, "poe.notfound.unique", "/poe/uniques", "poe.uniques.title");
    }
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
      Model model,
      jakarta.servlet.http.HttpServletResponse response) {
    net.luversof.web.gate.poe.dto.PoeBaseItem item;
    try {
      item = poeDataClient.baseItem(slug);
    } catch (RuntimeException e) {
      return notFound(response, model, slug, "poe.notfound.item", "/poe/items", "poe.items.title");
    }
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

  /**
   * 모드 페이지 기본 클래스 — mods.jte 의 표시 순서(방어구 → 무기 → 장신구 → 플라스크·주얼)에서 첫 칩.
   *
   * <p>순서 판정은 화면과 같은 규칙을 쓴다. 여기서만 알파벳순 첫 항목을 고르면 선택 칩이 화면 맨 아래에 찍힌다.
   */
  private static String defaultModClass(
      java.util.List<net.luversof.web.gate.poe.dto.PoeModItemClass> classes) {
    if (classes.isEmpty()) {
      return null;
    }
    java.util.List<java.util.function.Predicate<String>> displayOrder =
        java.util.List.of(
            id ->
                java.util.Set.of("Body Armour", "Helmet", "Gloves", "Boots", "Shield").contains(id),
            id ->
                !id.contains("Flask")
                    && !id.contains("Jewel")
                    && !java.util.Set.of("Amulet", "Ring", "Belt", "Quiver").contains(id),
            id -> java.util.Set.of("Amulet", "Ring", "Belt", "Quiver").contains(id),
            id -> id.contains("Flask") || id.contains("Jewel"));
    for (var group : displayOrder) {
      for (var c : classes) {
        if (group.test(c.itemClass())) {
          return c.itemClass();
        }
      }
    }
    return classes.get(0).itemClass();
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
    // 미지정/무효 값이면 **화면에서 첫 번째로 보이는 칩**으로 폴백한다.
    // modClasses 는 알파벳순이라 classes.get(0) 은 Abyss Jewel — mods.jte 가 표시 순서를
    // 방어구·무기·장신구·플라스크/주얼 로 다시 묶어 그리므로, 그대로 두면 맨 아랫줄 칩이
    // 선택된 채 페이지가 열려 무엇이 선택됐는지 눈에 안 들어온다.
    String active =
        itemClass != null && classes.stream().anyMatch(c -> c.itemClass().equals(itemClass))
            ? itemClass
            : defaultModClass(classes);
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

  /**
   * 지도 정규식 생성기 — 맵 모드 선택 → 인게임 검색용 정규식 생성·저장(poeregexkr 식). 데이터는 /poe-data/map-mods.json 을 클라이언트가
   * 직접 로드.
   */
  @GetMapping("/regex")
  public String regex(Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    return "poe/regex";
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
    java.util.List<DataArtifact> artifacts = dataArtifacts();
    model.addAttribute("artifacts", artifacts);
    // API 가 데이터를 마지막으로 읽은 시각 — 파일보다 이르면 **실행 중인 API 는 아직 옛 데이터**다.
    // 앱 밖(터미널)에서 파이프라인을 돌리면 재기동 전까지 반영되지 않는데, 화면엔 파일 시각만 보여
    // "갱신했는데 왜 그대로냐" / "재기동했더니 결과가 달라졌다"가 원인 불명으로 남았다(2026-08-11 실사고).
    Long loadedAt = null;
    try {
      loadedAt = poeDataClient.dataLoadedAt().get("loadedAtEpochMs");
    } catch (Exception e) {
      // API 가 옛 버전이면 이 엔드포인트가 없다 — 표시를 생략할 뿐 화면을 깨뜨리지 않는다
    }
    long newestFile =
        artifacts.stream()
            .filter(a -> a.modifiedEpochMs() != null)
            .mapToLong(DataArtifact::modifiedEpochMs)
            .max()
            .orElse(0L);
    model.addAttribute("dataLoadedAtEpochMs", loadedAt);
    model.addAttribute("dataStaleInApi", loadedAt != null && newestFile > loadedAt);
    return "poe/admin";
  }

  /** 데이터 산출물 하나 — 파일명/존재/크기/갱신시각, stale=가장 최근 갱신보다 하루 이상 뒤처짐. */
  public record DataArtifact(
      String file,
      String labelKey,
      boolean exists,
      long sizeBytes,
      Long modifiedEpochMs,
      boolean stale) {}

  /**
   * 추출 파이프라인이 만드는 산출물 목록 — 갱신 후 <b>무엇이 실제로 생성됐는지</b>를 화면에서 확인하려고.
   *
   * <p>이전엔 스킬젬·고유·일반·트리 5종만 보여줘서, 한 스텝이 조용히 실패해 특정 파일만 옛 버전으로 남아도 관리 화면에서는 드러나지 않았다(이번 세션의 타임리스
   * .bin stale 오판이 정확히 그 사고였다). 그래서 존재·크기뿐 아니라 <b>갱신 시각</b>을 함께 보여주고, 가장 최근 갱신보다 하루 이상 뒤처진 파일에 표시를
   * 남긴다.
   */
  private java.util.List<DataArtifact> dataArtifacts() {
    // {파일명, 메시지 키} — 파이프라인 산출물. 새 추출 스텝을 추가하면 여기에도 한 줄 넣는다.
    String[][] files = {
      {"skill-gems.json", "poe.admin.data.gems"},
      {"unique-items.json", "poe.admin.data.uniques"},
      {"base-items.json", "poe.admin.data.baseitems"},
      {"passive-tree.json", "poe.admin.data.tree"},
      {"atlas-tree.json", "poe.admin.data.atlas"},
      {"mods.json", "poe.admin.data.mods"},
      {"mod-pool.json", "poe.admin.data.modpool"},
      {"cluster-jewels.json", "poe.admin.data.clusters"},
      {"tattoos.json", "poe.admin.data.tattoos"},
      {"essences.json", "poe.admin.data.essences"},
      {"bench.json", "poe.admin.data.bench"},
      {"map-mods.json", "poe.admin.data.mapmods"},
      {"eldritch-implicits.json", "poe.admin.data.eldritch"},
      {"trade-stats.json", "poe.admin.data.tradestats"},
      {"skill-weapons.json", "poe.admin.data.skillweapons"},
    };
    java.util.List<DataArtifact> list = new java.util.ArrayList<>();
    long newest = 0L;
    java.util.Map<String, Long> modified = new java.util.LinkedHashMap<>();
    java.util.Map<String, Long> sizes = new java.util.LinkedHashMap<>();
    for (String[] f : files) {
      Path path = Path.of(dataDir, f[0]);
      if (Files.exists(path)) {
        try {
          long ms = Files.getLastModifiedTime(path).toMillis();
          modified.put(f[0], ms);
          sizes.put(f[0], Files.size(path));
          newest = Math.max(newest, ms);
        } catch (java.io.IOException e) {
          modified.put(f[0], null);
        }
      }
    }
    long staleBefore = newest - java.time.Duration.ofDays(1).toMillis();
    for (String[] f : files) {
      Long ms = modified.get(f[0]);
      boolean exists = modified.containsKey(f[0]);
      list.add(
          new DataArtifact(
              f[0],
              f[1],
              exists,
              sizes.getOrDefault(f[0], 0L),
              ms,
              exists && ms != null && newest > 0 && ms < staleBefore));
    }
    return list;
  }
}
