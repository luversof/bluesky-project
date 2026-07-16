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
import net.luversof.api.poe.service.PoeGem;
import net.luversof.api.poe.service.PoeGemDataService;
import net.luversof.api.poe.service.PoeModPoolDataService;
import net.luversof.api.poe.service.PoeTreeGraphService;
import net.luversof.api.poe.service.PoeUniqueDataService;
import net.luversof.api.poe.service.PoeUniqueItem;

/** PoE 정적 게임 데이터 조회 API (게이트가 httpexchange 로 호출). 표시용 record 를 그대로 JSON 직렬화한다. */
@RestController
@RequestMapping(value = "/api/poe", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeDataController {

  private final PoeGemDataService poeGemDataService;
  private final PoeUniqueDataService poeUniqueDataService;
  private final PoeBaseItemDataService poeBaseItemDataService;
  private final PoeModPoolDataService poeModPoolDataService;
  private final PoeTreeGraphService poeTreeGraphService;

  public PoeDataController(
      PoeGemDataService poeGemDataService,
      PoeUniqueDataService poeUniqueDataService,
      PoeBaseItemDataService poeBaseItemDataService,
      PoeModPoolDataService poeModPoolDataService,
      PoeTreeGraphService poeTreeGraphService) {
    this.poeGemDataService = poeGemDataService;
    this.poeUniqueDataService = poeUniqueDataService;
    this.poeBaseItemDataService = poeBaseItemDataService;
    this.poeModPoolDataService = poeModPoolDataService;
    this.poeTreeGraphService = poeTreeGraphService;
  }

  // ── 스킬젬 ──
  @GetMapping("/gems/search")
  public List<PoeGem> searchGems(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String type,
      @RequestParam(required = false, defaultValue = "all") String color) {
    return poeGemDataService.search(q, type, color);
  }

  @GetMapping("/gems/{slug}")
  public PoeGem gem(@PathVariable String slug) {
    return poeGemDataService.findBySlug(slug).orElseThrow(PoeDataController::notFound);
  }

  @GetMapping("/gems/meta")
  public Map<String, Object> gemMeta() {
    return Map.of("patch", poeGemDataService.patch(), "totalCount", poeGemDataService.totalCount());
  }

  // ── 고유 아이템 ──
  @GetMapping("/uniques/search")
  public List<PoeUniqueItem> searchUniques(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String category) {
    return poeUniqueDataService.search(q, category);
  }

  @GetMapping("/uniques/{slug}")
  public PoeUniqueItem unique(@PathVariable String slug) {
    return poeUniqueDataService.findBySlug(slug).orElseThrow(PoeDataController::notFound);
  }

  @GetMapping("/uniques/categories")
  public List<String> uniqueCategories() {
    return poeUniqueDataService.categories();
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

  @GetMapping("/base-items/item-classes")
  public Map<String, String> itemClasses() {
    return poeBaseItemDataService.itemClasses();
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

  private static ResponseStatusException notFound() {
    return new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
  }
}
