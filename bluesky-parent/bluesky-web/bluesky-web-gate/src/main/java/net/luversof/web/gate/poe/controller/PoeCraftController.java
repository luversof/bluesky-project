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
  static final int OPTIMIZE_BUDGET_MS = 20_000;

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

  public PoeCraftController(PoeCraftClient craftClient, PoeDataClient dataClient) {
    this.craftClient = craftClient;
    this.dataClient = dataClient;
  }

  @GetMapping(value = "/poe/craft", produces = MediaType.TEXT_HTML_VALUE)
  public String page(
      @RequestParam(required = false) String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      @RequestParam(required = false, defaultValue = "84") int ilvl,
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
    }
    if (active != null) {
      fill(
          model,
          new PoeCraft.Item(
              active.itemClass(), v, influence, clamp(ilvl), "NORMAL", List.of(), false),
          List.of(),
          null,
          null);
    }
    return "poe/craft";
  }

  /**
   * 화폐 적용 · 되돌리기 · 새로 시작 — action = 화폐 이름 | UNDO | RESET. state · history 는 화면 숨은 칸의 JSON(이력은 이전
   * 상태들, 끝이 가장 최근).
   */
  @PostMapping(value = "/poe/htmx/craft", produces = MediaType.TEXT_HTML_VALUE)
  public String apply(
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      @RequestParam(required = false, defaultValue = "84") int ilvl,
      @RequestParam(required = false, defaultValue = "") String state,
      @RequestParam(required = false, defaultValue = "[]") String history,
      @RequestParam(name = "action", required = false, defaultValue = "RESET") String rawAction,
      @RequestParam(required = false) String essence,
      // 섞어 쓰는 제작(10-15 C338) — 화석은 여러 개(체크), 제작대 · 수확은 고른 하나, 정복자는 action 값 "CONQUEROR:crusader"
      @RequestParam(required = false) List<String> fossil,
      @RequestParam(required = false) String bench,
      @RequestParam(required = false) String harvest,
      @RequestParam(required = false, defaultValue = "") String classLabel,
      // 짐승 제작 각인(10-15 C350) — 각인한 매직 아이템(JSON), 한 번 되돌리면 사라진다
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
      Model model) {
    model.addAttribute("classLabel", classLabel);
    List<PoeCraft.SpentStep> spentLog = parseSpent(spent);
    java.util.Map<String, Long> rollMap = parseRolls(rolls);
    java.util.Map<String, Long> imprintRollMap = parseRolls(imprintRolls);
    List<java.util.Map<String, Long>> rollHist = parseRollHist(rollHistory);
    boolean reroll = false;
    boolean blessed = false;
    Long lock = seedOf(foresee);
    // "이대로 적용"(COMMIT:화폐) — 미리 본 씨앗 · 인자로 그 화폐를 적용
    boolean commit = rawAction.startsWith("COMMIT:");
    String action = commit ? rawAction.substring("COMMIT:".length()) : rawAction;
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
      // 축복의 오브(C375) — 임플리싯 수치만 다시. 타락 · 임플리싯 없음이면 못 쓴다
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
    } else if (ACTIONS.contains(
        action.contains(":") ? action.substring(0, action.indexOf(':')) : action)) {
      String act = action.contains(":") ? action.substring(0, action.indexOf(':')) : action;
      String actArg = action.contains(":") ? action.substring(action.indexOf(':') + 1) : null;
      String arg = commit ? previewArg : argFor(act, actArg, essence, fossil, bench, harvest);
      if ("CATALYST".equals(act) && !commit && "1".equals(catalystFill) && arg != null) {
        arg = arg + "*20";
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
        historyModel(model, current, hist, spentLog, spentModel(model, itemClass, spentLog));
        rollsModel(model, ids(current), fixedIds(current), rollMap, false);
        model.addAttribute("rollHistJson", json.writeValueAsString(rollHist));
        model.addAttribute("preview", preview);
        model.addAttribute("foresee", lock.toString());
        model.addAttribute("imprintJson", imp == null ? "" : json.writeValueAsString(imp));
        model.addAttribute(
            "imprintRollsJson", imp == null ? "{}" : json.writeValueAsString(imprintRollMap));
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
        // 새로 붙은 옵션은 새 씨앗(C378 검토 — 같은 id 가 다시 붙으면 옛 씨앗이 남아 매번 같은 값이었다), 임플리싯도
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
      historyModel(model, current, hist, spentLog, spentModel(model, itemClass, spentLog));
      rollsModel(model, ids(current), fixedIds(current), rollMap, false);
      model.addAttribute("rollHistJson", json.writeValueAsString(rollHist));
      // 아이템이 바뀌면 미리 보기 능력은 사라진다(인게임 "Modifying the item in any way removes the ability to foresee")
      model.addAttribute("foresee", lock == null || current != before ? "" : lock.toString());
      model.addAttribute("imprintJson", imp == null ? "" : json.writeValueAsString(imp));
      model.addAttribute(
          "imprintRollsJson", imp == null ? "{}" : json.writeValueAsString(imprintRollMap));
      return "poe/htmx/craftEmu";
    }
    fill(model, current, hist, null, lastAction);
    historyModel(model, current, hist, spentLog, spentModel(model, itemClass, spentLog));
    rollsModel(
        model, ids(current), blessed ? explicitIds(current) : fixedIds(current), rollMap, reroll);
    model.addAttribute("rollHistJson", json.writeValueAsString(rollHist));
    model.addAttribute("foresee", lock == null || current != before ? "" : lock.toString());
    if (imprintError != null) {
      model.addAttribute("error", imprintError);
    }
    model.addAttribute("imprintJson", imp == null ? "" : json.writeValueAsString(imp));
    model.addAttribute(
        "imprintRollsJson", imp == null ? "{}" : json.writeValueAsString(imprintRollMap));
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
    return "poe/htmx/craftCalc";
  }

  /** 계산기 목표(계열마다 고른 최소 등급 · 장막 해제 옵션) — 계산기 · 최적 조합이 같이 쓴다(C457). labels 에 화면 이름. */
  private List<PoeCraft.Target> calcTargets(
      String itemClass,
      String variant,
      PoeCraft.Item item,
      org.springframework.util.MultiValueMap<String, String> params,
      List<String> labels) {
    List<PoeCraft.PoolLine> pool = craftClient.pool(new PoeCraft.PoolRequest(item, "RARE"));
    List<PoeCraft.Target> targets = new ArrayList<>();
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    List<PoeCraft.VeiledLine> veiled = null;
    for (String key : params.keySet()) {
      if (!key.startsWith("t_")) {
        continue;
      }
      String fam = key.substring(2);
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
      labels.add(spanLabel(in, ko) + " (T1" + (rank > 1 ? "~T" + rank : "") + ")");
    }
    return targets;
  }

  /**
   * 최적 조합(10-19 C457 사용자 요청 "에센스 · 제작대 · 야수 같은 다양한 방법을 조합해 최소 비용") — 계산기와 같은 목표로, 계산기 표가 뜬 뒤 이어서
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
    return "poe/htmx/craftOptimize";
  }

  /**
   * 다단계 전략 시뮬레이터(10-15 C339) — 계산기와 같은 목표 고르기(t_접두접미:계열 = 최소 등급)에 프리셋 전략을 얹어 모의한다. 목표 순서 = 화면 순서(접두
   * 먼저). 프리셋: alt-regal(변화 → [변경+확장]을 접두 · 접미 목표 하나씩 채울 때까지 → 제왕 → 엑잘 셋), chaos(연금 → 카오스 반복),
   * essence(고른 에센스 반복), prefix-lock / suffix-lock(연금 → 그쪽 목표를 카오스로 채운 뒤 [제작대 잠금 → 카오스] 묶음 반복).
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
    List<PoeCraft.PoolLine> pool = craftClient.pool(new PoeCraft.PoolRequest(item, "RARE"));
    List<PoeCraft.Target> targets = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    List<Boolean> isPrefix = new ArrayList<>();
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    List<PoeCraft.VeiledLine> veiled = null;
    for (String key : params.keySet()) {
      if (!key.startsWith("t_")) {
        continue;
      }
      String fam = key.substring(2);
      if (fam.startsWith("veil:")) {
        // 해제 목표(C426 검토 — 전략 모의는 계산기와 달리 말없이 버려 성공률이 부풀었다)
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
        isPrefix.add("prefix".equals(vl.gen()));
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
      isPrefix.add("prefix".equals(in.get(0).gen()));
      labels.add(spanLabel(in, ko) + " (T1" + (rank > 1 ? "~T" + rank : "") + ")");
    }
    List<Integer> pre = new ArrayList<>();
    List<Integer> suf = new ArrayList<>();
    for (int i = 0; i < isPrefix.size(); i++) {
      (isPrefix.get(i) ? pre : suf).add(i);
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
                      ? "그쪽(접두/접미) 목표나 잠금 제작대 옵션이 없습니다"
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
    return "poe/htmx/craftStrategy";
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

  /** 화폐 인자 — 화석 = 고른 화석 쉼표 목록, 제작대 = 옵션 id, 수확 = id, 정복자 = action 값 뒤, 에센스 = 슬러그. */
  /** 기폭제 쓴 개수(C385) — note "catalyst:종류:양:개수" → 쓴 화폐 인자 "종류*개수"(더럽혀진 기폭제는 "vaal" 하나). */
  static String catalystSpent(String note, String arg) {
    String base = arg == null ? "" : arg.contains("*") ? arg.substring(0, arg.indexOf('*')) : arg;
    if ("vaal".equals(base) || note == null) {
      return base;
    }
    String[] p = note.split(":");
    return p.length == 4 ? base + "*" + p[3] : base;
  }

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

  /** 미리 보기 씨앗(C360) — 숫자가 아니면 없음. */
  static Long seedOf(String s) {
    if (s == null || s.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(s.trim());
    } catch (NumberFormatException e) {
      return null;
    }
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
    model.addAttribute("pool", craftClient.pool(new PoeCraft.PoolRequest(view.item(), null)));
    PoeCraft.Options opts = craftClient.options(item.itemClass(), nz(item.variant()));
    model.addAttribute("options", opts);
    model.addAttribute("essences", opts.essences());
  }

  /**
   * 계산 · 전략의 출발 아이템(C349) — "지금 아이템에서 시작"(fromState=1)이면 에뮬레이터 상태(같은 종류 · 변형 · 아이템 레벨일 때만, API 가 분열
   * · 임플리싯만 남긴다), 아니면 고른 종류의 일반 아이템.
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

  /** 옵션 씨앗 키 — 옵션 id, 임플리싯은 "imp:" + id(C375). */
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

  /** 신성한 오브가 안 건드리는 씨앗 — 분열 옵션 · 임플리싯(C375). */
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
      PoeCraft.SpentResult spent) {
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
          add = lineDiff(lines.get(i + 1), lines.get(i), ko);
          rem = lineDiff(lines.get(i), lines.get(i + 1), ko);
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

  /** a 에만 있는 옵션 줄의 문구(id 대조) — "접두 T1 문구", 임플리싯은 진영 · 타락. */
  private static List<String> lineDiff(List<PoeCraft.Line> a, List<PoeCraft.Line> b, boolean ko) {
    java.util.Set<String> bIds = new java.util.HashSet<>();
    for (PoeCraft.Line l : b) {
      bIds.add(l.gen() + ":" + l.id());
    }
    List<String> out = new ArrayList<>();
    for (PoeCraft.Line l : a) {
      if (bIds.contains(l.gen() + ":" + l.id())) {
        continue;
      }
      List<String> t = ko && l.ko() != null && !l.ko().isEmpty() ? l.ko() : l.en();
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
}
