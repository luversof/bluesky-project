package net.luversof.api.poe.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.poe.service.PoeBaseItem;
import net.luversof.api.poe.service.PoeBaseItemDataService;
import net.luversof.api.poe.service.PoeBenchDataService;
import net.luversof.api.poe.service.PoeEldritchDataService;
import net.luversof.api.poe.service.PoeEssenceDataService;
import net.luversof.api.poe.service.PoeFoulbornDataService;
import net.luversof.api.poe.service.PoeGem;
import net.luversof.api.poe.service.PoeGemDataService;
import net.luversof.api.poe.service.PoeMetaPopularityService;
import net.luversof.api.poe.service.PoeModDataService;
import net.luversof.api.poe.service.PoeModPoolDataService;
import net.luversof.api.poe.service.PoeTattooDataService;
import net.luversof.api.poe.service.PoeTreeGraphService;
import net.luversof.api.poe.service.PoeUniqueDataService;
import net.luversof.api.poe.service.PoeUniqueItem;

/** PoE 정적 게임 데이터 조회 API (게이트가 httpexchange 로 호출). 표시용 record 를 그대로 JSON 직렬화한다. */
@RestController
@RequestMapping(value = "/api/poe", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeDataController {

  private final PoeGemDataService poeGemDataService;
  private final PoeMetaPopularityService poeMetaPopularityService;
  private final PoeUniqueDataService poeUniqueDataService;
  private final PoeBaseItemDataService poeBaseItemDataService;
  private final PoeModPoolDataService poeModPoolDataService;
  private final PoeModDataService poeModDataService;
  private final PoeEldritchDataService poeEldritchDataService;
  private final PoeFoulbornDataService poeFoulbornDataService;
  private final PoeEssenceDataService poeEssenceDataService;
  private final PoeBenchDataService poeBenchDataService;
  private final PoeTreeGraphService poeTreeGraphService;
  private final PoeTattooDataService poeTattooDataService;
  private final net.luversof.api.poe.service.PoeDataLoadStamp poeDataLoadStamp;

  public PoeDataController(
      PoeGemDataService poeGemDataService,
      PoeMetaPopularityService poeMetaPopularityService,
      PoeUniqueDataService poeUniqueDataService,
      PoeBaseItemDataService poeBaseItemDataService,
      PoeModPoolDataService poeModPoolDataService,
      PoeModDataService poeModDataService,
      PoeEldritchDataService poeEldritchDataService,
      PoeFoulbornDataService poeFoulbornDataService,
      PoeEssenceDataService poeEssenceDataService,
      PoeBenchDataService poeBenchDataService,
      PoeTreeGraphService poeTreeGraphService,
      PoeTattooDataService poeTattooDataService,
      net.luversof.api.poe.service.PoeDataLoadStamp poeDataLoadStamp) {
    this.poeGemDataService = poeGemDataService;
    this.poeMetaPopularityService = poeMetaPopularityService;
    this.poeUniqueDataService = poeUniqueDataService;
    this.poeBaseItemDataService = poeBaseItemDataService;
    this.poeModPoolDataService = poeModPoolDataService;
    this.poeModDataService = poeModDataService;
    this.poeEldritchDataService = poeEldritchDataService;
    this.poeFoulbornDataService = poeFoulbornDataService;
    this.poeEssenceDataService = poeEssenceDataService;
    this.poeBenchDataService = poeBenchDataService;
    this.poeTreeGraphService = poeTreeGraphService;
    this.poeTattooDataService = poeTattooDataService;
    this.poeDataLoadStamp = poeDataLoadStamp;
  }

  // ── 스킬젬 ──
  @GetMapping("/gems/search")
  public List<PoeGem> searchGems(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String type,
      @RequestParam(required = false, defaultValue = "all") String color,
      @RequestParam(required = false, defaultValue = "all") String tag) {
    return poeGemDataService.search(q, type, color, tag);
  }

  /** 태그 그룹(유형/원소·피해/전달/특성) — 그룹 칩 UI용 */
  @GetMapping("/gems/tag-groups")
  public List<PoeGemDataService.TagGroup> gemTagGroups() {
    return poeGemDataService.tagGroups();
  }

  @GetMapping("/gems/{slug}")
  public PoeGem gem(@PathVariable String slug) {
    return poeGemDataService.findBySlug(slug).orElseThrow(PoeDataController::notFound);
  }

  /**
   * API 가 데이터 파일을 마지막으로 읽은 시각(epoch ms). 관리 화면이 <b>파일 갱신 시각</b>과 비교해 "파이프라인은 돌았는데 API 는 아직 옛 데이터"
   * 상태를 드러낸다 — 앱 밖에서 파이프라인을 돌리면 재기동 전까지 반영되지 않는다.
   */
  @GetMapping("/data/loaded-at")
  public Map<String, Object> dataLoadedAt() {
    return Map.of("loadedAtEpochMs", poeDataLoadStamp.loadedAt().toEpochMilli());
  }

  @GetMapping("/gems/meta")
  public Map<String, Object> gemMeta() {
    return Map.of("patch", poeGemDataService.patch(), "totalCount", poeGemDataService.totalCount());
  }

  // ── 고유 아이템 ──
  @GetMapping("/uniques/search")
  public List<PoeUniqueItem> searchUniques(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String itemClass) {
    return poeUniqueDataService.search(q, null, itemClass);
  }

  @GetMapping("/uniques/{slug}")
  public PoeUniqueItem unique(@PathVariable String slug) {
    return poeUniqueDataService.findBySlug(slug).orElseThrow(PoeDataController::notFound);
  }

  @GetMapping("/uniques/categories")
  public List<String> uniqueCategories() {
    return poeUniqueDataService.categories();
  }

  /** 고유를 세부 itemClass 그룹(일반 아이템과 동일 분류)으로 — 그룹 칩 UI용 */
  @GetMapping("/uniques/category-groups")
  public List<PoeBaseItemDataService.ClassGroup> uniqueCategoryGroups() {
    return poeUniqueDataService.categoryGroups();
  }

  @GetMapping("/uniques/meta")
  public Map<String, Object> uniqueMeta() {
    return Map.of("totalCount", poeUniqueDataService.totalCount());
  }

  // ── 일반(베이스) 아이템 ──
  @GetMapping("/base-items/search")
  public List<PoeBaseItem> searchBaseItems(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String itemClass) {
    return poeBaseItemDataService.search(q, itemClass);
  }

  @GetMapping("/base-items/{slug}")
  public PoeBaseItem baseItem(@PathVariable String slug) {
    return poeBaseItemDataService.findBySlug(slug).orElseThrow(PoeDataController::notFound);
  }

  /** 조인용 — 없으면 200 + 빈 본문(게이트에서 null 로 받음). */
  @GetMapping("/base-items/by-name")
  public PoeBaseItem baseItemByName(@RequestParam String name) {
    return poeBaseItemDataService.findByName(name).orElse(null);
  }

  /** poedb 속성부여식 도유 목록 — 노터블 + 성유 3종, 비싼 성유 우선 정렬. */
  @GetMapping("/tree/anoints")
  public java.util.List<net.luversof.api.poe.service.PoeTreeGraphService.AnointEntry> anoints() {
    return poeTreeGraphService.anointList();
  }

  @GetMapping("/base-items/item-classes")
  public Map<String, String> itemClasses() {
    return poeBaseItemDataService.itemClasses();
  }

  /** 아이템 클래스를 PoB식 그룹(한손/양손 무기·방어구·장신구·플라스크·주얼)으로 — 그룹 칩 UI용 */
  @GetMapping("/base-items/item-class-groups")
  public List<PoeBaseItemDataService.ClassGroup> itemClassGroups() {
    return poeBaseItemDataService.itemClassGroups();
  }

  @GetMapping("/base-items/meta")
  public Map<String, Object> baseItemMeta() {
    return Map.of("totalCount", poeBaseItemDataService.totalCount());
  }

  // ── 모드 풀 (일반 아이템 티어표) ──
  @GetMapping("/mod-pool/for-item-class")
  public List<PoeModPoolDataService.ModFamily> modFamiliesForItemClass(
      @RequestParam String itemClass) {
    return poeModPoolDataService.familiesForItemClass(itemClass);
  }

  // ── 전체 모드(poedb Modifiers식) ──
  @GetMapping("/mods/item-classes")
  public List<PoeModDataService.ModItemClass> modItemClasses() {
    return poeModDataService.itemClasses();
  }

  @GetMapping("/mods/for-item-class")
  public PoeModDataService.ClassMods modsForItemClass(
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence) {
    PoeModDataService.ClassMods mods =
        poeModDataService.forItemClass(itemClass, variant, influence);
    if (mods == null) {
      throw notFound();
    }
    return mods;
  }

  // ── 엘드리치 임플리싯(총주교/포식자) ──
  // 엘드리치 대상이 아닌 슬롯(무기/반지/허리띠 등)은 예외 대신 null(200) — 아이템 상세는 클래스마다 이걸
  // 호출하므로, 404→500 래핑 예외가 페이지 로드마다 로그를 더럽히는 것을 피한다.
  @GetMapping("/eldritch/for-item-class")
  public PoeEldritchDataService.ClassEldritch eldritchForItemClass(@RequestParam String itemClass) {
    return poeEldritchDataService.forItemClass(itemClass);
  }

  // ── 삿된(Foulborn) 모드 풀 ──
  // 유니크 이름 매핑이 게임 데이터에 없어 **토큰(Jewel85 등)** 이 곧 식별자다. 검색은 문구·토큰 양쪽을 본다.
  @GetMapping("/foulborn")
  public List<PoeFoulbornDataService.FoulbornGroup> foulborn(
      @RequestParam(required = false, defaultValue = "") String category,
      @RequestParam(required = false, defaultValue = "") String q) {
    return poeFoulbornDataService.search(category, q);
  }

  /** 분류(한글) → 모드 수 — 화면 칩 개수. 데이터 없으면 빈 맵(섹션 감춤). */
  @GetMapping("/foulborn/for")
  public List<PoeFoulbornDataService.FoulbornGroup> foulbornForUnique(@RequestParam String name) {
    return poeFoulbornDataService.forUnique(name);
  }

  /**
   * 실빌드 사용 빈도 기반 목록 순서 — 시뮬 폼이 "많이 쓰는 것 먼저"로 재배치하는 데 쓴다.
   *
   * @param ascendancy 선택된 전직(없으면 전체 합산)
   * @param skills 선택된 스킬 **영문명** 콤마 구분(없으면 전체 합산)
   */
  @GetMapping("/meta/order")
  public PoeMetaPopularityService.MetaOrder metaOrder(
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam(required = false, defaultValue = "") String skills) {
    List<String> skillNames =
        skills.isBlank()
            ? List.of()
            : java.util.Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    return poeMetaPopularityService.order(ascendancy, skillNames);
  }

  @GetMapping("/foulborn/names")
  public java.util.Set<String> foulbornNames() {
    return poeFoulbornDataService.uniqueNames();
  }

  @GetMapping("/foulborn/categories")
  public java.util.Map<String, Integer> foulbornCategories() {
    return poeFoulbornDataService.byCategory();
  }

  // ── 에센스 제작 정보 ──
  // 엘드리치와 같은 이유로 비대상 클래스(퀴버 등)는 예외 대신 null(200).
  @GetMapping("/essences/for-item-class")
  public List<PoeEssenceDataService.EssenceEntry> essencesForItemClass(
      @RequestParam String itemClass) {
    return poeEssenceDataService.forItemClass(itemClass);
  }

  // ── 장인 작업대(벤치크래프트) ──
  @GetMapping("/bench/for-item-class")
  public List<PoeBenchDataService.BenchEntry> benchForItemClass(@RequestParam String itemClass) {
    return poeBenchDataService.forItemClass(itemClass);
  }

  /** 클러스터 주얼 노터블 사전 — 브라우징 페이지용. */
  @GetMapping("/tree/cluster-notables")
  public List<PoeTreeGraphService.ClusterNotable> clusterNotables() {
    return poeTreeGraphService.clusterNotables();
  }

  // ── 패시브 트리 ──
  @GetMapping("/tree/jewel-sockets")
  public List<Integer> jewelSockets() {
    return poeTreeGraphService.jewelSockets();
  }

  @GetMapping("/tree/node/{id}")
  public PoeTreeGraphService.TreeNode treeNode(@PathVariable int id) {
    PoeTreeGraphService.TreeNode node = poeTreeGraphService.node(id);
    if (node == null) {
      throw notFound();
    }
    return node;
  }

  // ── 문신 ──
  /**
   * 특정 패시브에 새길 수 있는 문신 목록. 게임 규칙상 소형 패시브는 <b>속성 종류</b>까지 맞아야 한다(힘 소형에는 힘 문신 + 속성 공용 문신).
   *
   * @param nodeType 노드 종류(normal/notable/keystone/mastery) — 생략하면 소형으로 본다
   * @param attribute 소형 속성 패시브의 속성(Strength/Dexterity/Intelligence)
   */
  @GetMapping("/tree/tattoos")
  public List<PoeTattooDataService.Tattoo> tattoos(
      @RequestParam(required = false) String nodeType,
      @RequestParam(required = false) String attribute) {
    if (nodeType == null && attribute == null) {
      return poeTattooDataService.all();
    }
    return poeTattooDataService.candidates(nodeType, attribute);
  }

  private static ResponseStatusException notFound() {
    return new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
  }
}
