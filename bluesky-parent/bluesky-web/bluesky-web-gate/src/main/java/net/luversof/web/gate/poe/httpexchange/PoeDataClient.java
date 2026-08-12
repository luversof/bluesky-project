package net.luversof.web.gate.poe.httpexchange;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.poe.dto.ModFamily;
import net.luversof.web.gate.poe.dto.PoeBaseItem;
import net.luversof.web.gate.poe.dto.PoeEldritch;
import net.luversof.web.gate.poe.dto.PoeGem;
import net.luversof.web.gate.poe.dto.PoeGroups;
import net.luversof.web.gate.poe.dto.PoeJobStatus;
import net.luversof.web.gate.poe.dto.PoeModClass;
import net.luversof.web.gate.poe.dto.PoeModItemClass;
import net.luversof.web.gate.poe.dto.PoeUniqueItem;

/** bluesky-api-poe 정적 게임 데이터 조회 클라이언트. */
@HttpExchange(url = "/api/poe", accept = MediaType.APPLICATION_JSON_VALUE)
public interface PoeDataClient {

  // ── 스킬젬 ──
  @GetExchange("/gems/search")
  List<PoeGem> searchGems(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String color,
      @RequestParam(required = false) String tag);

  @GetExchange("/gems/tag-groups")
  List<PoeGroups.TagGroup> gemTagGroups();

  @GetExchange("/gems/{slug}")
  PoeGem gem(@PathVariable String slug);

  @GetExchange("/gems/meta")
  PoeJobStatus.GemMeta gemMeta();

  /**
   * API 가 데이터 파일을 마지막으로 읽은 시각(epoch ms). 관리 화면이 파일 갱신 시각과 비교해 "파이프라인은 돌았는데 API 는 옛 데이터" 를 드러내는 데 쓴다
   * — 앱 밖에서 파이프라인을 돌리면 재기동 전까지 반영되지 않는다.
   */
  @GetExchange("/data/loaded-at")
  java.util.Map<String, Long> dataLoadedAt();

  // ── 고유 아이템 ──
  @GetExchange("/uniques/search")
  List<PoeUniqueItem> searchUniques(
      @RequestParam(required = false) String q, @RequestParam(required = false) String itemClass);

  @GetExchange("/uniques/{slug}")
  PoeUniqueItem unique(@PathVariable String slug);

  @GetExchange("/uniques/categories")
  List<String> uniqueCategories();

  @GetExchange("/uniques/category-groups")
  List<PoeGroups.ClassGroup> uniqueCategoryGroups();

  @GetExchange("/uniques/meta")
  PoeJobStatus.CountMeta uniqueMeta();

  // ── 일반(베이스) 아이템 ──
  @GetExchange("/base-items/search")
  List<PoeBaseItem> searchBaseItems(
      @RequestParam(required = false) String q, @RequestParam(required = false) String itemClass);

  @GetExchange("/base-items/{slug}")
  PoeBaseItem baseItem(@PathVariable String slug);

  /** 이름으로 조인(없으면 null). */
  @GetExchange("/base-items/by-name")
  PoeBaseItem baseItemByName(@RequestParam String name);

  @GetExchange("/base-items/item-classes")
  Map<String, String> itemClasses();

  @GetExchange("/base-items/item-class-groups")
  List<PoeGroups.ClassGroup> itemClassGroups();

  @GetExchange("/base-items/meta")
  PoeJobStatus.CountMeta baseItemMeta();

  // ── 모드 풀 (일반 아이템 티어표) ──
  @GetExchange("/mod-pool/for-item-class")
  List<ModFamily> modFamiliesForItemClass(@RequestParam String itemClass);

  @GetExchange("/mods/item-classes")
  List<PoeModItemClass> modItemClasses();

  @GetExchange("/mods/for-item-class")
  PoeModClass modsForItemClass(
      @RequestParam String itemClass, @RequestParam String variant, @RequestParam String influence);

  @GetExchange("/eldritch/for-item-class")
  PoeEldritch eldritchForItemClass(@RequestParam String itemClass);

  /** 에센스 제작 정보 — 비대상 클래스(퀴버 등)는 null. */
  @GetExchange("/essences/for-item-class")
  java.util.List<net.luversof.web.gate.poe.dto.PoeEssenceEntry> essencesForItemClass(
      @RequestParam String itemClass);

  /** 클러스터 주얼 노터블 사전 — 브라우징 페이지용. */
  @GetExchange("/tree/cluster-notables")
  java.util.List<net.luversof.web.gate.poe.dto.PoeClusterNotable> clusterNotables();

  /** 문신 목록 — nodeType/attribute 생략 시 전체(브라우징 페이지용). */
  @GetExchange("/tree/tattoos")
  java.util.List<net.luversof.web.gate.poe.dto.PoeTattoo> tattoos();

  /** 장인 작업대(벤치크래프트) 모드 — 비대상 클래스는 null. */
  @GetExchange("/bench/for-item-class")
  java.util.List<net.luversof.web.gate.poe.dto.PoeBenchEntry> benchForItemClass(
      @RequestParam String itemClass);

  /** poedb 속성부여식 도유 목록(비싼 성유 우선 정렬) */
  @GetExchange("/tree/anoints")
  java.util.List<net.luversof.web.gate.poe.dto.PoeAnointEntry> anoints();
}
