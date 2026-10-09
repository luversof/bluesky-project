package net.luversof.web.gate.poe2.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import net.luversof.web.gate.poe2.dto.Poe2;
import net.luversof.web.gate.poe2.httpexchange.Poe2DataClient;

/**
 * PoE2 화면 — PoE1(/poe)과 같은 구성(젬 · 아이템 · 옵션 · 트리 · 관리)에 PoE2 전용 증강물(룬 · 영혼 핵) 메뉴를 더했다. 데이터는
 * bluesky-api-poe 의 /api/poe2 에서, 패시브 트리 JSON 은 게이트가 /poe-data/poe2/passive-tree.json 으로 직접 서빙한다.
 */
@Controller
@RequestMapping(value = "/poe2", produces = MediaType.TEXT_HTML_VALUE)
public class Poe2ViewController {

  private final Poe2DataClient client;

  public Poe2ViewController(Poe2DataClient client) {
    this.client = client;
  }

  /** 모든 화면 머리의 패치 배지 — API 가 내려가 있어도 화면은 그린다. */
  private Poe2.Meta meta() {
    try {
      return client.meta();
    } catch (RuntimeException e) {
      return new Poe2.Meta("", 0, 0, 0, 0, 0, false, "", null);
    }
  }

  private static String orEmpty(String s) {
    return s == null ? "" : s;
  }

  private String notFound(HttpServletResponse response, Model model, String slug, String listUrl) {
    response.setStatus(HttpStatus.NOT_FOUND.value());
    model.addAttribute("meta", meta());
    model.addAttribute("slug", slug);
    model.addAttribute("listUrl", listUrl);
    return "poe2/notFound";
  }

  @GetMapping
  public String gems(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String kind,
      @RequestParam(required = false, defaultValue = "all") String color,
      @RequestParam(required = false, defaultValue = "all") String tag,
      Model model) {
    model.addAttribute("meta", meta());
    // 태그 칩은 PoE1 젬 화면처럼 묶음별 줄로(10-01)
    List<Poe2.TagGroup> tagGroups;
    try {
      tagGroups = client.gemTagGroups();
    } catch (RuntimeException e) {
      tagGroups = List.of();
    }
    model.addAttribute("tagGroups", tagGroups);
    model.addAttribute("initialQ", orEmpty(q));
    model.addAttribute("initialKind", kind);
    model.addAttribute("initialColor", color);
    model.addAttribute("activeTag", tag);
    return "poe2/gems";
  }

  @GetMapping("/gems/{slug}")
  public String gemPage(
      @PathVariable String slug,
      @RequestParam(required = false) Integer level,
      Model model,
      HttpServletResponse response) {
    Poe2.Gem gem;
    try {
      gem = client.gem(slug);
    } catch (RuntimeException e) {
      return notFound(response, model, slug, "/poe2");
    }
    model.addAttribute("meta", meta());
    model.addAttribute("gem", gem);
    model.addAttribute("level", level);
    // "이 스킬로 최적화 →" — PoE1 젬 상세와 같은 바로가기(10-01). 시뮬레이터 스킬 목록에 있는 젬만(없으면 첫 스킬로 열려 엉뚱한 결과)
    boolean simulatable = false;
    try {
      Poe2.SimOptions o = client.simOptions("", "");
      simulatable =
          o != null
              && o.skills() != null
              && o.skills().stream().anyMatch(c -> c.name().equals(gem.name()));
    } catch (RuntimeException e) {
      // 시뮬레이터가 안 되면 바로가기만 숨긴다
    }
    model.addAttribute("simulatable", simulatable);
    return "poe2/gemPage";
  }

  @GetMapping("/uniques")
  public String uniques(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String itemClass,
      // 고유 ↔ 일반 탭 전환 시 common.ts([data-poe-item-tab])가 활성 칩을 ?slot= 로 넘긴다 — PoE1 과 같은 흐름(10-01)
      @RequestParam(required = false) String slot,
      Model model) {
    model.addAttribute("meta", meta());
    List<Poe2.ItemClass> classes;
    try {
      classes = client.uniqueClasses();
    } catch (RuntimeException e) {
      classes = List.of();
    }
    model.addAttribute("classes", classes);
    model.addAttribute("initialQ", orEmpty(q));
    model.addAttribute("activeClass", resolveClass(classes, itemClass, slot));
    return "poe2/uniques";
  }

  /**
   * 활성 분류 — itemClass 가 있으면 그대로, 없고 탭 전환 slot 이 이 목록에 있는 분류면 그것, 아니면 전체. 고유 목록에 없는 분류(클로 등)로 넘어오면 빈
   * 목록 대신 전체로 연다.
   */
  static String resolveClass(List<Poe2.ItemClass> classes, String itemClass, String slot) {
    if (itemClass != null && !itemClass.isBlank() && !"all".equals(itemClass)) {
      return itemClass;
    }
    if (slot != null && classes != null && classes.stream().anyMatch(c -> c.key().equals(slot))) {
      return slot;
    }
    return "all";
  }

  @GetMapping("/uniques/{slug}")
  public String uniquePage(
      @PathVariable String slug,
      @RequestParam(required = false) Integer variant,
      Model model,
      HttpServletResponse response) {
    Poe2.Unique item;
    try {
      item = client.unique(slug);
    } catch (RuntimeException e) {
      return notFound(response, model, slug, "/poe2/uniques");
    }
    model.addAttribute("meta", meta());
    model.addAttribute("item", item);
    model.addAttribute("base", baseOrNull(item.baseType()));
    model.addAttribute("variant", variant);
    model.addAttribute("forceable", FORCE_CLASSES.contains(item.itemClass()));
    return "poe2/uniquePage";
  }

  /** 고유의 베이스(방어·무기 수치, 요구치) — 못 찾으면 null 로 두고 화면은 모드만 그린다. */
  Poe2.BaseItem baseOrNull(String baseType) {
    if (baseType == null) {
      return null;
    }
    try {
      return client.searchBases(baseType, null, null).stream()
          .filter(b -> b.name().equalsIgnoreCase(baseType))
          .findFirst()
          .orElse(null);
    } catch (RuntimeException e) {
      return null;
    }
  }

  @GetMapping("/items")
  public String items(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String itemClass,
      @RequestParam(required = false) String slot,
      Model model) {
    model.addAttribute("meta", meta());
    List<Poe2.ItemClass> classes;
    try {
      classes = client.itemClasses();
    } catch (RuntimeException e) {
      classes = List.of();
    }
    model.addAttribute("classes", classes);
    model.addAttribute("initialQ", orEmpty(q));
    model.addAttribute("activeClass", resolveClass(classes, itemClass, slot));
    return "poe2/items";
  }

  @GetMapping("/items/{slug}")
  public String itemPage(@PathVariable String slug, Model model, HttpServletResponse response) {
    Poe2.BaseItem base;
    try {
      base = client.base(slug);
    } catch (RuntimeException e) {
      return notFound(response, model, slug, "/poe2/items");
    }
    model.addAttribute("meta", meta());
    model.addAttribute("base", base);
    Poe2.ModPool pool;
    try {
      pool = client.modPoolForBase(base.name());
    } catch (RuntimeException e) {
      pool = null;
    }
    model.addAttribute("pool", pool);
    // 이 베이스를 쓰는 고유 아이템 — PoE1 베이스 상세(poe/itemPage)와 같은 구획(10-01). 같은 분류로 좁혀 받고 베이스 이름으로 거른다
    java.util.List<Poe2.Unique> baseUniques = java.util.List.of();
    try {
      baseUniques =
          client.searchUniques(null, base.itemClass()).stream()
              .filter(u -> base.name().equals(u.baseType()))
              .toList();
    } catch (RuntimeException e) {
      // 목록이 안 와도 상세는 보인다
    }
    model.addAttribute("baseUniques", baseUniques);
    return "poe2/itemPage";
  }

  @GetMapping("/mods")
  public String mods(@RequestParam(required = false) String key, Model model) {
    model.addAttribute("meta", meta());
    List<Poe2.ModPoolSummary> pools;
    try {
      pools = client.modPools(null);
    } catch (RuntimeException e) {
      pools = List.of();
    }
    model.addAttribute("pools", pools);
    String active =
        key != null && pools.stream().anyMatch(p -> p.key().equals(key))
            ? key
            : pools.stream()
                .filter(p -> "Body Armour".equals(p.itemClass()))
                .map(Poe2.ModPoolSummary::key)
                .findFirst()
                .orElse(pools.isEmpty() ? null : pools.get(0).key());
    model.addAttribute("activeKey", active);
    return "poe2/mods";
  }

  @GetMapping("/augments")
  public String augments(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String kind,
      @RequestParam(required = false, defaultValue = "all") String itemClass,
      Model model) {
    model.addAttribute("meta", meta());
    List<Poe2.ItemClass> classes;
    try {
      classes = client.itemClasses();
    } catch (RuntimeException e) {
      classes = List.of();
    }
    model.addAttribute("classes", classes);
    model.addAttribute("initialQ", orEmpty(q));
    model.addAttribute("activeKind", kind);
    model.addAttribute("activeClass", itemClass);
    return "poe2/augments";
  }

  @GetMapping("/tree")
  public String tree(Model model) {
    model.addAttribute("meta", meta());
    // 트리 계산의 메인 스킬 선택지 — 시뮬레이터와 같은 목록(많이 쓰는 순). 없으면 스킬 없이(방어·능력치만) 계산
    try {
      Poe2.SimOptions o = client.simOptions("", "");
      model.addAttribute("evalSkills", o == null || o.skills() == null ? List.of() : o.skills());
    } catch (RuntimeException e) {
      model.addAttribute("evalSkills", List.of());
    }
    // 주얼 칸에 꽂을 고유 주얼(10-03 C73, PoE1 트리 주얼 장착의 짝) — 없으면 꽂기 메뉴를 감춘다
    try {
      List<Poe2.Unique> jewels = client.searchUniques(null, "Jewel");
      model.addAttribute("treeJewels", jewels == null ? List.of() : jewels);
    } catch (RuntimeException e) {
      model.addAttribute("treeJewels", List.of());
    }
    return "poe2/tree";
  }

  /** 아틀라스 패시브 트리 — 패시브 트리 뷰어(tree2.js)의 아틀라스 모드, 데이터는 /poe-data/poe2/atlas-tree.json. */
  @GetMapping("/atlas")
  public String atlas(Model model) {
    model.addAttribute("meta", meta());
    return "poe2/atlas";
  }

  /** 경로석 정규식 생성기 — PoE1 지도 정규식(js/poe/regex.js)을 그대로 쓰고 데이터·프리셋 주소만 PoE2 것으로 준다. */
  @GetMapping("/regex")
  public String regex(Model model) {
    model.addAttribute("meta", meta());
    return "poe2/regex";
  }

  /**
   * 시뮬레이터 — PoE1 시뮬(/poe/sim)과 같은 흐름: 메인 스킬·전직·적 시나리오를 고르고 실행하면 poe.ninja 실빌드 후보를 받아 재계산·선택하고
   * 목표(실빌드 성향 자동)에 맞춰 다듬은 빌드를 돌려준다. 선택지는 실빌드 인원 순(고른 쪽이 다른 쪽 순서를 바꾼다).
   */
  /** 시뮬레이터에 고정할 수 있는 고유 분류 — API Poe2RefineService.FORCE_SLOTS 와 같다(무기·보조 장비는 스킬 무기 요구와 얽혀 뺐다). */
  static final java.util.Set<String> FORCE_CLASSES =
      java.util.Set.of("Helmet", "Body Armour", "Gloves", "Boots", "Amulet", "Belt", "Ring");

  @GetMapping("/sim")
  public String sim(
      // 젬 상세 "이 스킬로 최적화 →" 바로가기 — 스킬을 미리 고른 채 연다(PoE1 /poe/sim?skills= 와 같은 흐름, 10-01)
      @RequestParam(required = false, defaultValue = "") String skill,
      // 고유 상세 "이 고유로 최적화 →" — 그 고유를 고정한 채 연다(PoE1 /poe/sim?uniques= 와 같은 흐름, 10-01)
      @RequestParam(required = false, defaultValue = "") String unique,
      // 트리 화면 "→ 시뮬"(10-02, PoE1 /poe/sim?treeNodes= 의 짝) — c=직업 a=전직 n=노드 s=능력치 선택(트리 주소와 같은 이름)
      @RequestParam(required = false, defaultValue = "") String c,
      @RequestParam(required = false, defaultValue = "") String a,
      @RequestParam(required = false, defaultValue = "") String n,
      @RequestParam(required = false, defaultValue = "") String s,
      // 무기 세트 전용 노드 "노드:1|2,…"(10-02)
      @RequestParam(required = false, defaultValue = "") String w,
      Model model) {
    model.addAttribute("meta", meta());
    boolean fromTree = !c.isBlank() && !n.isBlank();
    model.addAttribute("treeSets", fromTree ? w : "");
    model.addAttribute("treeClass", fromTree ? c : "");
    model.addAttribute("treeAsc", fromTree ? a : "");
    model.addAttribute("treeNodes", fromTree ? n : "");
    model.addAttribute("treeAttrs", fromTree ? s : "");
    List<Poe2.Unique> forceable = List.of();
    try {
      forceable =
          client.searchUniques(null, null).stream()
              .filter(u -> FORCE_CLASSES.contains(u.itemClass()))
              .sorted(
                  java.util.Comparator.comparing(
                      (Poe2.Unique u) ->
                          net.luversof.web.gate.poe.PoeText.name(u.nameKo(), u.name())))
              .toList();
    } catch (RuntimeException e) {
      // 목록이 안 와도 시뮬레이터는 연다
    }
    model.addAttribute("forceUniques", forceable);
    // "slug:변형번호" — 고유 상세에서 고른 변형(10-04 C96, PoE1 C95 짝). 목록 판정은 slug 로
    final String uq =
        unique.matches(".+:[0-9]+") ? unique.substring(0, unique.lastIndexOf(':')) : unique;
    model.addAttribute(
        "preUnique", forceable.stream().anyMatch(u -> u.slug().equals(uq)) ? unique : "");
    Poe2.SimOptions options = null;
    try {
      options = client.simOptions("", skill);
    } catch (RuntimeException e) {
      org.slf4j.LoggerFactory.getLogger(Poe2ViewController.class)
          .warn("PoE2 시뮬레이터 선택지 조회 실패: {}", e.toString());
    }
    model.addAttribute("options", options);
    // 목록에 있는 스킬만 미리 고른다(오타·옛 스킬이면 평소처럼 첫 스킬)
    final Poe2.SimOptions opts = options;
    model.addAttribute(
        "preSkill",
        opts != null
                && opts.skills() != null
                && opts.skills().stream().anyMatch(choice -> choice.name().equals(skill))
            ? skill
            : "");
    return "poe2/sim";
  }

  @GetMapping("/build")
  public String build(
      // 시뮬레이터의 "실빌드에서 출발" — 그 아키타입의 대표 실빌드를 불러온다
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam(required = false, defaultValue = "") String skill,
      // 시뮬레이터 결과 "빌드 화면에서 열기" — 마지막 시뮬레이션 결과 빌드를 불러온다
      @RequestParam(required = false, defaultValue = "") String sim,
      Model model) {
    model.addAttribute("startAscendancy", ascendancy);
    model.addAttribute("startSkill", skill);
    model.addAttribute("startSim", !sim.isBlank());
    // 결과 이력의 한 건(epochMs id) — 아니면(sim=1) 마지막 결과
    model.addAttribute("startSimId", sim.matches("\\d{10,}") ? sim : "");
    model.addAttribute("meta", meta());
    return "poe2/build";
  }

  @GetMapping("/admin")
  public String admin(Model model, Principal principal) {
    model.addAttribute("meta", meta());
    model.addAttribute("loggedIn", principal != null);
    return "poe2/admin";
  }
}
