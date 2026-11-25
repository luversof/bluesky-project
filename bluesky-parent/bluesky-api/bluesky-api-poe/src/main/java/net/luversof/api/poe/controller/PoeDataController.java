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

/**
 * PoE1 게임 데이터
 *
 * <p>정적 게임 데이터 조회(게이트가 httpexchange 로 호출). 표시용 record 를 그대로 JSON 직렬화한다.
 */
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
  private final net.luversof.api.poe.service.PoeTradeStatDataService poeTradeStatDataService;
  private final net.luversof.api.poe.service.PoeClusterJewelDataService poeClusterJewelDataService;

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
      net.luversof.api.poe.service.PoeDataLoadStamp poeDataLoadStamp,
      net.luversof.api.poe.service.PoeTradeStatDataService poeTradeStatDataService,
      net.luversof.api.poe.service.PoeClusterJewelDataService poeClusterJewelDataService) {
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
    this.poeTradeStatDataService = poeTradeStatDataService;
    this.poeClusterJewelDataService = poeClusterJewelDataService;
  }

  // ── 스킬젬 ──
  /**
   * 젬 검색
   *
   * @param q 이름 검색어(선택)
   * @param type 젬 종류, 기본 all
   * @param color 색, 기본 all
   * @param tag 태그, 기본 all
   * @param lite true 면 레벨별 데이터를 뺀다(고르기 목록용)
   * @return 젬 목록
   */
  @GetMapping("/gems/search")
  public List<PoeGem> searchGems(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String type,
      @RequestParam(required = false, defaultValue = "all") String color,
      @RequestParam(required = false, defaultValue = "all") String tag,
      @RequestParam(required = false, defaultValue = "false") boolean lite) {
    List<PoeGem> gems = poeGemDataService.search(q, type, color, tag);
    // lite: 레벨별 데이터 없이(고르기 목록 — 게이트 트리 · 시뮬 화면은 이름 · slug 만 쓴다)
    return lite ? gems.stream().map(PoeGem::withoutLevels).toList() : gems;
  }

  /**
   * 젬 태그 그룹 조회
   *
   * <p>유형 / 원소 · 피해 / 전달 / 특성. 그룹 칩 UI 용.
   */
  @GetMapping("/gems/tag-groups")
  public List<PoeGemDataService.TagGroup> gemTagGroups() {
    return poeGemDataService.tagGroups();
  }

  /**
   * 젬 조회
   *
   * <p>레벨별 데이터 포함.
   *
   * @param slug 젬 slug
   * @return 젬, 없으면 404
   */
  @GetMapping("/gems/{slug}")
  public PoeGem gem(@PathVariable String slug) {
    return poeGemDataService.findBySlug(slug).orElseThrow(PoeDataController::notFound);
  }

  /**
   * 데이터 로드 시각 조회
   *
   * <p>API 가 데이터 파일을 마지막으로 읽은 시각(epoch ms). 관리 화면이 <b>파일 갱신 시각</b>과 비교해 "파이프라인은 돌았는데 API 는 아직 옛
   * 데이터" 상태를 드러낸다 — 앱 밖에서 파이프라인을 돌리면 재기동 전까지 반영되지 않는다.
   */
  @GetMapping("/data/loaded-at")
  public Map<String, Object> dataLoadedAt() {
    return Map.of("loadedAtEpochMs", poeDataLoadStamp.loadedAt().toEpochMilli());
  }

  /**
   * 젬 데이터 메타 조회
   *
   * <p>패치 버전과 개수.
   *
   * @return patch, totalCount
   */
  @GetMapping("/gems/meta")
  public Map<String, Object> gemMeta() {
    return Map.of("patch", poeGemDataService.patch(), "totalCount", poeGemDataService.totalCount());
  }

  // ── 고유 아이템 ──
  /**
   * 고유 아이템 검색
   *
   * <p>대분류를 주지 않은 전체 목록에는 리마인더 문구를 싣지 않는다(상세에서 받는다).
   *
   * @param q 이름 검색어(선택)
   * @param itemClass 아이템 분류, 기본 all
   * @param category 대분류(선택, 예: jewel)
   * @return 고유 아이템 목록
   */
  @GetMapping("/uniques/search")
  public List<PoeUniqueItem> searchUniques(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String itemClass,
      // 대분류(jewel · body …) — 트리 화면이 주얼만 받게(10-04 C122: 전체 3MB 를 받아 게이트에서 거르던 것)
      @RequestParam(required = false) String category) {
    // 목록엔 리마인더를 싣지 않는다 — 툴팁은 상세 응답으로 그린다(10-04 C119: 목록 2.86MB 중 415KB 였다)
    //   대분류를 지정한 작은 목록(트리 주얼 184종)은 남긴다 — 트리 주얼 칸 툴팁이 그 줄 밑 부연을 그린다(10-04 C125)
    return poeUniqueDataService.search(q, category, itemClass).stream()
        .map(u -> category == null ? u.withoutReminders() : u)
        .toList();
  }

  /**
   * 고유 아이템 이름 목록 조회
   *
   * <p>시뮬 강제 장착 셀렉트용(10-04 C124).
   */
  @GetMapping("/uniques/names")
  public List<PoeUniqueItem> uniqueNames() {
    return poeUniqueDataService.search(null, null, "all").stream()
        .map(PoeUniqueItem::namesOnly)
        .toList();
  }

  /**
   * 고유 아이템 조회
   *
   * @param slug 고유 아이템 slug
   * @return 고유 아이템, 없으면 404
   */
  @GetMapping("/uniques/{slug}")
  public PoeUniqueItem unique(@PathVariable String slug) {
    return poeUniqueDataService.findBySlug(slug).orElseThrow(PoeDataController::notFound);
  }

  /**
   * 고유 아이템 대분류 목록 조회
   *
   * @return 대분류 이름
   */
  @GetMapping("/uniques/categories")
  public List<String> uniqueCategories() {
    return poeUniqueDataService.categories();
  }

  /**
   * 고유 아이템 분류 그룹 조회
   *
   * <p>세부 itemClass 그룹(일반 아이템과 동일 분류). 그룹 칩 UI 용.
   */
  @GetMapping("/uniques/category-groups")
  public List<PoeBaseItemDataService.ClassGroup> uniqueCategoryGroups() {
    return poeUniqueDataService.categoryGroups();
  }

  /**
   * 고유 아이템 메타 조회
   *
   * <p>개수.
   *
   * @return totalCount
   */
  @GetMapping("/uniques/meta")
  public Map<String, Object> uniqueMeta() {
    return Map.of("totalCount", poeUniqueDataService.totalCount());
  }

  // ── 일반(베이스) 아이템 ──
  /**
   * 베이스 아이템 검색
   *
   * @param q 이름 검색어(선택)
   * @param itemClass 아이템 분류, 기본 all
   * @return 베이스 아이템 목록
   */
  @GetMapping("/base-items/search")
  public List<PoeBaseItem> searchBaseItems(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String itemClass) {
    return poeBaseItemDataService.search(q, itemClass);
  }

  /**
   * 베이스 아이템 조회
   *
   * <p>거래소에서 찾을 수 있는 베이스인지(tradable)도 담는다.
   *
   * @param slug 베이스 slug
   * @return 베이스 아이템, 없으면 404
   */
  @GetMapping("/base-items/{slug}")
  public PoeBaseItem baseItem(@PathVariable String slug) {
    // 거래소가 아는 베이스인가(10-08 C174) — 게이트 베이스 상세가 "거래소에서 찾기" 단추를 보일지 정한다
    return poeBaseItemDataService
        .findBySlug(slug)
        .map(b -> b.withTradable(poeTradeStatDataService.tradable(b.nameKo())))
        .orElseThrow(PoeDataController::notFound);
  }

  /**
   * 이름으로 베이스 아이템 조회
   *
   * <p>조인용. 없으면 200 + 빈 본문(게이트에서 null 로 받는다).
   */
  @GetMapping("/base-items/by-name")
  public PoeBaseItem baseItemByName(@RequestParam String name) {
    return poeBaseItemDataService.findByName(name).orElse(null);
  }

  /**
   * 성유 부여 목록 조회
   *
   * <p>poedb 속성부여식 성유 목록. 주요 스킬 + 성유 3종, 비싼 성유 우선 정렬.
   */
  @GetMapping("/tree/anoints")
  public java.util.List<net.luversof.api.poe.service.PoeTreeGraphService.AnointEntry> anoints() {
    return poeTreeGraphService.anointList();
  }

  /**
   * 베이스 아이템 분류 조회
   *
   * <p>코드 → 이름.
   *
   * @return 분류 맵
   */
  @GetMapping("/base-items/item-classes")
  public Map<String, String> itemClasses() {
    return poeBaseItemDataService.itemClasses();
  }

  /**
   * 아이템 종류 그룹 조회
   *
   * <p>PoB식 그룹(한손 / 양손 무기 · 방어구 · 장신구 · 플라스크 · 주얼). 그룹 칩 UI 용.
   */
  @GetMapping("/base-items/item-class-groups")
  public List<PoeBaseItemDataService.ClassGroup> itemClassGroups() {
    return poeBaseItemDataService.itemClassGroups();
  }

  /**
   * 베이스 아이템 메타 조회
   *
   * <p>개수.
   *
   * @return totalCount
   */
  @GetMapping("/base-items/meta")
  public Map<String, Object> baseItemMeta() {
    return Map.of("totalCount", poeBaseItemDataService.totalCount());
  }

  // ── 모드 풀 (일반 아이템 등급표) ──
  /**
   * 아이템 분류별 옵션 계열 조회
   *
   * <p>제작 시뮬레이터용 옵션 풀.
   *
   * @param itemClass 아이템 분류
   * @return 옵션 계열 목록
   */
  @GetMapping("/mod-pool/for-item-class")
  public List<PoeModPoolDataService.ModFamily> modFamiliesForItemClass(
      @RequestParam String itemClass) {
    return poeModPoolDataService.familiesForItemClass(itemClass);
  }

  // ── 전체 모드(poedb Modifiers식) ──
  /**
   * 옵션 표 아이템 분류 목록 조회
   *
   * @return 아이템 분류
   */
  @GetMapping("/mods/item-classes")
  public List<PoeModDataService.ModItemClass> modItemClasses() {
    return poeModDataService.itemClassesForList();
  }

  /**
   * 아이템 분류별 옵션 표 조회
   *
   * @param itemClass 아이템 분류
   * @param variant 세부 종류(선택)
   * @param influence 영향력(선택)
   * @return 옵션 표, 없는 분류면 404
   */
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

  // ── 섬뜩한 고정 속성(총주교/포식자) ──
  // 엘드리치 대상이 아닌 슬롯(무기/반지/허리띠 등)은 예외 대신 null(200) — 아이템 상세는 클래스마다 이걸
  // 호출하므로, 404→500 래핑 예외가 페이지 로드마다 로그를 더럽히는 것을 피한다.
  /**
   * 아이템 분류별 엘드리치 옵션 조회
   *
   * @param itemClass 아이템 분류
   * @return eldritch 옵션, 대상이 아닌 분류면 null
   */
  @GetMapping("/eldritch/for-item-class")
  public PoeEldritchDataService.ClassEldritch eldritchForItemClass(@RequestParam String itemClass) {
    return poeEldritchDataService.forItemClass(itemClass);
  }

  // ── 삿된(Foulborn) 모드 풀 ──
  // 유니크 이름 매핑이 게임 데이터에 없어 **토큰(Jewel85 등)** 이 곧 식별자다. 검색은 문구·토큰 양쪽을 본다.
  /**
   * foulborn 변형 검색
   *
   * <p>foulborn 고유 아이템 변형.
   *
   * @param category 대분류(선택)
   * @param q 검색어(선택)
   * @return 고유 아이템별 변형 묶음
   */
  @GetMapping("/foulborn")
  public List<PoeFoulbornDataService.FoulbornGroup> foulborn(
      @RequestParam(required = false, defaultValue = "") String category,
      @RequestParam(required = false, defaultValue = "") String q) {
    return poeFoulbornDataService.search(category, q);
  }

  /**
   * 고유 아이템의 foulborn 모드 수 조회
   *
   * <p>분류(한글) → 모드 수. 화면 칩 개수용. 데이터 없으면 빈 맵(섹션 감춤).
   */
  @GetMapping("/foulborn/for")
  public List<PoeFoulbornDataService.FoulbornGroup> foulbornForUnique(@RequestParam String name) {
    return poeFoulbornDataService.forUnique(name);
  }

  /**
   * 실빌드 사용 빈도 순서 조회
   *
   * <p>시뮬 폼이 "많이 쓰는 것 먼저"로 재배치하는 데 쓴다.
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

  /**
   * foulborn 고유 아이템 이름 조회
   *
   * @return 이름 집합
   */
  @GetMapping("/foulborn/names")
  public java.util.Set<String> foulbornNames() {
    return poeFoulbornDataService.uniqueNames();
  }

  /**
   * 대분류별 foulborn 변형 수 조회
   *
   * @return 대분류 → 개수
   */
  @GetMapping("/foulborn/categories")
  public java.util.Map<String, Integer> foulbornCategories() {
    return poeFoulbornDataService.byCategory();
  }

  // ── 에센스 제작 정보 ──
  // 엘드리치와 같은 이유로 비대상 클래스(주얼 · 플라스크 등)는 예외 대신 null(200). 화살통은 10-30 부터 대상.
  /**
   * 아이템 분류별 에센스 조회
   *
   * @param itemClass 아이템 분류
   * @return 에센스 목록, 에센스를 못 쓰는 분류면 null
   */
  @GetMapping("/essences/for-item-class")
  public List<PoeEssenceDataService.EssenceEntry> essencesForItemClass(
      @RequestParam String itemClass) {
    return poeEssenceDataService.forItemClass(itemClass);
  }

  // ── 작업대(벤치크래프트) ──
  /**
   * 아이템 분류별 작업대 옵션 조회
   *
   * @param itemClass 아이템 분류
   * @return 제작대 옵션 목록
   */
  @GetMapping("/bench/for-item-class")
  public List<PoeBenchDataService.BenchEntry> benchForItemClass(@RequestParam String itemClass) {
    return poeBenchDataService.forItemClass(itemClass);
  }

  /**
   * 클러스터 주얼 주요 스킬 조회
   *
   * <p>브라우징 페이지용 사전.
   */
  @GetMapping("/tree/cluster-notables")
  public List<PoeTreeGraphService.ClusterNotable> clusterNotables() {
    // 더 이상 생성되지 않는 주요 스킬(모드 가중치 0, 10-29 C2) 표시 — 옛 주얼엔 남아 있어 지우지 않고 알린다
    return poeTreeGraphService.clusterNotables().stream()
        .map(n ->
            poeClusterJewelDataService.retiredNotable(n.name())
                ? new PoeTreeGraphService.ClusterNotable(
                    n.name(), n.nameKo(), n.stats(), n.statsKo(), n.icon(), n.keystone(), true)
                : n)
        .toList();
  }

  // ── 패시브 트리 ──
  /**
   * 패시브 트리 주얼 소켓 조회
   *
   * @return 노드 id 목록
   */
  @GetMapping("/tree/jewel-sockets")
  public List<Integer> jewelSockets() {
    return poeTreeGraphService.jewelSockets();
  }

  /**
   * 패시브 트리 노드 조회
   *
   * @param id 노드 id
   * @return 노드, 없으면 404
   */
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
   * 패시브에 새길 문신 조회
   *
   * <p>게임 규칙상 소형 패시브는 <b>속성 종류</b>까지 맞아야 한다(힘 소형에는 힘 문신 + 속성 공용 문신).
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
