package net.luversof.web.gate.poe2.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.poe2.dto.Poe2;
import net.luversof.web.gate.poe2.dto.Poe2Craft;
import net.luversof.web.gate.poe2.httpexchange.Poe2CraftClient;
import net.luversof.web.gate.poe2.httpexchange.Poe2DataClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * PoE2 제작 에뮬레이터 화면(10-15 C351 — 사용자 요청 "제작 에뮬레이터 · 시뮬레이터", P5). 새 컨트롤러(기존 PoE2 컨트롤러 생성자를 안 건드려 시험
 * 가짜 무사). 상태 · 이력은 숨은 칸(서버 무상태), 징조는 체크 — 화폐가 쓰면 사라진다.
 */
@Controller
public class Poe2CraftController {

  // 30 → 100(C453 — 단계 기록에서 한참 전 단계로도 되돌리게)
  static final int HISTORY_MAX = 100;

  /** 최적 조합 시간 예산(C461) — 백그라운드 작업, 화면은 조회하며 기다린다. */
  static final int OPTIMIZE_BUDGET_MS = 30_000;

  /** 재조합에 쓴 재료(C407) — 쓴 화폐 기록의 인자로 들고 다니다 되돌리면 보관함에 되살린다. */
  record StashSnap(Poe2Craft.Item item, java.util.Map<String, Long> rolls) {}

  /** 재조합 성공 확률 칸(C407) — 1~100 정수, 아니면 null. */
  static Integer chanceOf(String s) {
    try {
      int v = Integer.parseInt(s.trim());
      return v >= 1 && v <= 100 ? v : null;
    } catch (RuntimeException e) {
      return null;
    }
  }

  /** 기폭제 쓴 개수(C386) — note "catalyst:종류:양:개수"(종류에 "jewel:" 이 붙을 수 있다) → 쓴 화폐 인자 "종류*개수". */
  static String catalystSpent(String note, String arg) {
    String base = arg == null ? "" : arg.contains("*") ? arg.substring(0, arg.indexOf('*')) : arg;
    if (note == null) {
      return base;
    }
    return base + "*" + note.substring(note.lastIndexOf(':') + 1);
  }

  static final List<String> ACTIONS =
      List.of(
          "TRANSMUTE",
          "AUGMENTATION",
          "REGAL",
          "ALCHEMY",
          "EXALT",
          "CHAOS",
          "ANNUL",
          "FRACTURE",
          "ESSENCE",
          "DESECRATE",
          "REVEAL",
          "REVEAL_REROLL",
          "MANUAL_ADD",
          "MANUAL_REMOVE",
          "VAAL",
          "CATALYST");

  private final Poe2CraftClient craftClient;
  private final Poe2DataClient dataClient;
  private final JsonMapper json = JsonMapper.builder().build();

  public Poe2CraftController(Poe2CraftClient craftClient, Poe2DataClient dataClient) {
    this.craftClient = craftClient;
    this.dataClient = dataClient;
  }

  @GetMapping(value = "/poe2/craft", produces = MediaType.TEXT_HTML_VALUE)
  public String page(
      @RequestParam(required = false) String pool,
      @RequestParam(required = false, defaultValue = "82") int ilvl,
      // 재조합기 보관함(C407 검토 M1) — 위쪽 종류 · 아이템 레벨을 바꿔도 남는다(같은 클래스 다른 변형 · 다른 레벨과 합치기)
      @RequestParam(required = false, defaultValue = "") String stash,
      @RequestParam(required = false, defaultValue = "{}") String stashRolls,
      jakarta.servlet.http.HttpServletResponse response,
      Model model) {
    try {
      model.addAttribute("meta", dataClient.meta());
    } catch (RuntimeException e) {
      model.addAttribute("meta", null);
    }
    Poe2Craft.Options opts = craftClient.options();
    List<Poe2.ModPoolSummary> pools = opts.pools() == null ? List.of() : opts.pools();
    String active =
        pool != null && pools.stream().anyMatch(p -> p.key().equals(pool))
            ? pool
            : pools.stream()
                .filter(p -> "Body Armour".equals(p.itemClass()))
                .map(Poe2.ModPoolSummary::key)
                .findFirst()
                .orElse(pools.isEmpty() ? null : pools.get(0).key());
    model.addAttribute("pools", pools);
    model.addAttribute("activeKey", active);
    model.addAttribute("ilvl", clamp(ilvl));
    if (active != null) {
      // 시뮬레이터 목표 고르기 — 빈 레어 기준 전체 풀(C352)
      model.addAttribute(
          "calcPool",
          craftClient.pool(new Poe2Craft.Item(active, clamp(ilvl), "RARE", List.of(), false)));
      // 훼손된 속성 목표(C420) — 못 읽으면 칸을 그리지 않는다
      List<Poe2Craft.PoolLine> dese;
      try {
        dese = craftClient.desecrated(active, clamp(ilvl));
      } catch (RuntimeException e) {
        dese = List.of();
      }
      model.addAttribute("calcDesecrated", dese);
      fill(
          model,
          new Poe2Craft.Item(active, clamp(ilvl), "NORMAL", List.of(), false),
          new ArrayList<>(),
          null,
          null,
          List.of(),
          "",
          opts);
      Poe2Craft.Item kept = parse(stash, null);
      stashModel(model, kept, kept == null ? null : parseRolls(stashRolls), "", null, null);
      if (kept != null) {
        // 주소창에는 보관함을 남기지 않는다(htmx 가 요청 주소 대신 이 주소를 쌓는다)
        response.setHeader(
            "HX-Push-Url",
            "/poe2/craft?pool="
                + java.net.URLEncoder.encode(active, java.nio.charset.StandardCharsets.UTF_8)
                + "&ilvl="
                + clamp(ilvl));
      }
    }
    return "poe2/craft";
  }

  /** 화폐 적용 · 되돌리기 · 새로 시작 — tier = 화폐 등급(greater · perfect), omen = 체크한 징조 키들. */
  @PostMapping(value = "/poe2/htmx/craft", produces = MediaType.TEXT_HTML_VALUE)
  public String apply(
      // 최적 조합 작업 id(C464) — 결과 카드에 있으면 에뮬레이터가 다음 수단을 받아 보인다
      @RequestParam(required = false) String optJob,
      @RequestParam String poolKey,
      @RequestParam(required = false, defaultValue = "82") int ilvl,
      @RequestParam(required = false, defaultValue = "") String state,
      @RequestParam(required = false, defaultValue = "[]") String history,
      @RequestParam(name = "action", required = false, defaultValue = "RESET") String rawAction,
      @RequestParam(required = false, defaultValue = "") String tier,
      @RequestParam(required = false) List<String> omen,
      // 에센스(C353) — 고른 에센스 슬러그
      @RequestParam(required = false, defaultValue = "") String essence,
      // 훼손(C354) — 고른 뼈 슬러그, 드러내기는 action "REVEAL:옵션 id"
      @RequestParam(required = false, defaultValue = "") String bone,
      // 히네코라의 머리카락(C361) — 미리 보기 씨앗 · 미리 본 화폐의 인자 · 징조(적용할 때 그대로)
      @RequestParam(required = false, defaultValue = "") String foresee,
      @RequestParam(required = false) String previewArg,
      @RequestParam(required = false) String previewOmens,
      // 쓴 화폐 기록(C366) — 아이템을 바꾼 단계마다 하나(되돌리기면 마지막 것을 뺀다), 머리카락은 되돌려도 남는다
      @RequestParam(required = false, defaultValue = "[]") String spent,
      // 굴림 씨앗(C370) — 옵션 id → 씨앗(툴팁 수치)
      @RequestParam(required = false, defaultValue = "{}") String rolls,
      // 이력과 짝인 굴림 씨앗(C371) — 되돌리면 그때 값으로
      @RequestParam(required = false, defaultValue = "[]") String rollHistory,
      // 기폭제 "20%까지 한 번에"(C386)
      @RequestParam(required = false) String catalystFill,
      // 재조합기(C407) — 보관한 재료 · 그 굴림 씨앗, 고른 옵션(지금 아이템 · 재료), 성공 확률(인게임 화면 값)
      @RequestParam(required = false, defaultValue = "") String stash,
      @RequestParam(required = false, defaultValue = "{}") String stashRolls,
      @RequestParam(required = false) List<String> recombL,
      @RequestParam(required = false) List<String> recombR,
      @RequestParam(required = false, defaultValue = "") String recombChance,
      Model model) {
    Long lock = seedOf(foresee);
    List<Poe2Craft.SpentStep> spentLog = parseSpent(spent);
    java.util.Map<String, Long> rollMap = parseRolls(rolls);
    List<java.util.Map<String, Long>> rollHist = parseRollHist(rollHistory);
    boolean reroll = false;
    String divineError = null;
    boolean commit = rawAction.startsWith("COMMIT:");
    String action = commit ? rawAction.substring("COMMIT:".length()) : rawAction;
    Poe2Craft.Item fresh = new Poe2Craft.Item(poolKey, clamp(ilvl), "NORMAL", List.of(), false);
    Poe2Craft.Item current = parse(state, fresh);
    Poe2Craft.Item before = current;
    List<Poe2Craft.Item> hist = parseHistory(history);
    List<String> omens = omen == null ? new ArrayList<>() : new ArrayList<>(omen);
    Poe2Craft.Item stashItem = parse(stash, null);
    java.util.Map<String, Long> stashRollMap = parseRolls(stashRolls);
    boolean recombDone = false;
    String lastAction = null;
    Poe2Craft.View applied = null;
    if ("RESET".equals(action)
        || !current.poolKey().equals(fresh.poolKey())
        || current.ilvl() != fresh.ilvl()) {
      current = fresh;
      hist = new ArrayList<>();
      spentLog = new ArrayList<>();
      rollHist = new ArrayList<>();
    } else if ("FORESEE".equals(action)) {
      lock = java.util.concurrent.ThreadLocalRandom.current().nextLong();
      lastAction = action;
      spentLog.add(new Poe2Craft.SpentStep("FORESEE", null, List.of()));
    } else if ("FORESEE_OFF".equals(action)) {
      lock = null;
    } else if ("DIVINE".equals(action) && omens.contains("Sanctification")) {
      // 신성한 오브 + 축성의 징조(C390) — 축성: 옵션마다 0.78~1.22배(0.01 단위, poe2wiki), 배수는 굴림 씨앗 "sanct:id" 로(되돌리기
      // · 이력과 함께)
      Poe2Craft.View v =
          craftClient.apply(
              new Poe2Craft.ApplyRequest(
                  current, "SANCTIFY", null, List.of("Sanctification"), null));
      if (v.error() != null) {
        divineError = v.error();
      } else {
        hist.add(current);
        rollHist.add(new java.util.LinkedHashMap<>(rollMap));
        while (hist.size() > HISTORY_MAX) {
          hist.remove(0);
        }
        while (rollHist.size() > hist.size()) {
          rollHist.remove(0);
        }
        current = v.item();
        java.util.concurrent.ThreadLocalRandom tr =
            java.util.concurrent.ThreadLocalRandom.current();
        for (String id : ids(current)) {
          rollMap.put("sanct:" + id, (long) (78 + tr.nextInt(45)));
        }
        omens.remove("Sanctification");
        lock = null;
        lastAction = action;
        applied = v;
        spentLog.add(new Poe2Craft.SpentStep("SANCTIFY", null, List.of("Sanctification")));
      }
    } else if ("DIVINE".equals(action)) {
      // 신성한 오브(C370) — 옵션 수치만 다시(분열 옵션은 그대로). 타락 · 옵션 없음이면 못 쓴다
      boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
      if (current.sanctified()) {
        // 축성된 아이템(C390) — 인게임 문구
        divineError = ko ? "아이템이 축성됨" : "Item is Sanctified";
      } else if (current.corrupted()) {
        divineError = ko ? "타락한 아이템은 바꿀 수 없습니다" : "Corrupted items cannot be modified.";
      } else if (current.affixes().stream().allMatch(Poe2Craft.Affix::fractured)) {
        divineError = ko ? "다시 굴릴 옵션이 없습니다" : "No modifiers to reroll.";
      } else {
        reroll = true;
        lock = null;
        lastAction = action;
        spentLog.add(new Poe2Craft.SpentStep("DIVINE", null, List.of()));
      }
    } else if ("UNDO".equals(action) || action.startsWith("UNDO_TO:")) {
      // 아무 단계로 되돌리기(C453) — UNDO_TO:n = 이력 n 개만 남긴 상태로(되돌리기를 그만큼 되풀이 — 쓴 화폐 · 굴린 값 · 보관함도 같이)
      int times = 1;
      if (action.startsWith("UNDO_TO:")) {
        try {
          times = hist.size() - Integer.parseInt(action.substring("UNDO_TO:".length()));
        } catch (NumberFormatException e) {
          times = 0;
        }
      }
      for (int u = 0; u < times && !hist.isEmpty(); u++) {
        Poe2Craft.Item undone = current;
        java.util.Map<String, Long> undoneRolls = rollMap;
        current = hist.remove(hist.size() - 1);
        // 그때 굴린 값으로(C371)
        if (!rollHist.isEmpty()) {
          rollMap = rollHist.remove(rollHist.size() - 1);
        }
        // 되돌린 단계의 화폐 · 징조는 뺀다(머리카락은 아이템을 바꾸지 않아 그대로)
        Poe2Craft.SpentStep rm = null;
        for (int i = spentLog.size() - 1; i >= 0; i--) {
          if (!"FORESEE".equals(spentLog.get(i).action())
              && !"DIVINE".equals(spentLog.get(i).action())) {
            rm = spentLog.remove(i);
            break;
          }
        }
        // 재조합기(C407) — 바꿔 들기는 다시 바꾸고, 재조합은 그때 쓴 재료를 보관함에 되살린다
        if (rm != null && "SWAP".equals(rm.action())) {
          stashItem = undone;
          stashRollMap = undoneRolls;
        } else if (rm != null && "RECOMBINE".equals(rm.action())) {
          try {
            StashSnap snap = json.readValue(rm.arg(), StashSnap.class);
            if (snap != null && snap.item() != null) {
              stashItem = snap.item();
              stashRollMap =
                  snap.rolls() == null
                      ? new java.util.LinkedHashMap<>()
                      : new java.util.LinkedHashMap<>(snap.rolls());
            }
          } catch (RuntimeException e) {
            // 깨진 기록이면 보관함은 그대로
          }
        }
      }
      // 드러낼 셋을 새로(C483) — 되돌리기는 보통 다른 셋을 노리고 하는데 같은 셋이 다시 나왔다
      current = current.reseedReveal();
    } else if ("STASH".equals(action)) {
      // 재조합기(C407) — 지금 아이템을 재료로 보관(아이템은 그대로 · 비용 없음 · 이력 아님)
      stashItem = current;
      stashRollMap = new java.util.LinkedHashMap<>(rollMap);
      lastAction = action;
      // 고른 체크는 비운다(C407 검토 — 아이템이 바뀐 쪽에 같은 id 체크가 남았다)
      recombDone = true;
    } else if ("UNSTASH".equals(action)) {
      stashItem = null;
      stashRollMap = new java.util.LinkedHashMap<>();
      lastAction = action;
      recombDone = true;
    } else if ("SWAP".equals(action) && stashItem != null) {
      // 보관한 재료와 바꿔 들기 — 이력에 남아 되돌리면 다시 바뀐다
      hist.add(current);
      rollHist.add(new java.util.LinkedHashMap<>(rollMap));
      while (hist.size() > HISTORY_MAX) {
        hist.remove(0);
      }
      while (rollHist.size() > hist.size()) {
        rollHist.remove(0);
      }
      Poe2Craft.Item t = current;
      current = stashItem;
      stashItem = t;
      java.util.Map<String, Long> tr = rollMap;
      rollMap = stashRollMap;
      stashRollMap = tr;
      lock = null;
      lastAction = action;
      recombDone = true;
      spentLog.add(new Poe2Craft.SpentStep("SWAP", null, List.of()));
    } else if ("RECOMBINE".equals(action)) {
      // 재조합기(C407) — 성공 확률은 비공개 공식이라 인게임 화면 값을 받는다, 재조합의 징조 = 행운
      // 실패 · 오류여도 재조합기 묶음을 열어 두고 결과 한 줄을 보인다(C423)
      lastAction = action;
      Integer chance = chanceOf(recombChance);
      if (stashItem == null) {
        divineError =
            net.luversof.web.gate.poe.PoeText.isKorean()
                ? "보관한 재료가 없습니다"
                : "No stored item to recombine with.";
      } else if (chance == null) {
        divineError =
            net.luversof.web.gate.poe.PoeText.isKorean()
                ? "성공 확률(인게임 재조합기 화면의 1~100%)을 넣으세요"
                : "Enter the Chance for Success (1–100%) shown in the in-game Recombinator.";
      } else {
        List<String> ls = recombL == null ? List.of() : recombL;
        List<String> rs = recombR == null ? List.of() : recombR;
        Poe2Craft.View v =
            craftClient.recombine(
                new Poe2Craft.RecombineRequest(
                    current, stashItem, ls, rs, chance, omens.contains("Recombination"), null));
        if (v.error() != null) {
          applied = v;
        } else {
          hist.add(current);
          rollHist.add(new java.util.LinkedHashMap<>(rollMap));
          while (hist.size() > HISTORY_MAX) {
            hist.remove(0);
          }
          while (rollHist.size() > hist.size()) {
            rollHist.remove(0);
          }
          // 옮겨 온 옵션은 원래 아이템의 굴림 씨앗 그대로(수치가 따라온다)
          boolean keptLeft = v.note() != null && v.note().startsWith("recombine:ok:L");
          java.util.Map<String, Long> nr = new java.util.LinkedHashMap<>();
          for (String id : ids(v.item())) {
            Long s =
                rs.contains(id)
                    ? stashRollMap.get(id)
                    : ls.contains(id)
                        ? rollMap.get(id)
                        : (keptLeft ? rollMap : stashRollMap).get(id);
            if (s != null) {
              nr.put(id, s);
            }
          }
          List<String> used = v.omensUsed() == null ? List.of() : v.omensUsed();
          spentLog.add(
              new Poe2Craft.SpentStep(
                  "RECOMBINE",
                  json.writeValueAsString(new StashSnap(stashItem, stashRollMap)),
                  used));
          current = v.item();
          rollMap = nr;
          stashItem = null;
          stashRollMap = new java.util.LinkedHashMap<>();
          omens.removeAll(used);
          lock = null;
          lastAction = action;
          applied = v;
          recombDone = true;
        }
      }
    } else if (ACTIONS.contains(
        action.contains(":") ? action.substring(0, action.indexOf(':')) : action)) {
      String act = action.contains(":") ? action.substring(0, action.indexOf(':')) : action;
      String actArg = action.contains(":") ? action.substring(action.indexOf(':') + 1) : null;
      String arg =
          commit
              ? previewArg
              : switch (act) {
                case "ESSENCE" -> essence.isBlank() ? null : essence;
                case "DESECRATE" -> bone.isBlank() ? null : bone;
                case "REVEAL", "MANUAL_ADD", "MANUAL_REMOVE" -> actArg;
                case "CATALYST" ->
                    actArg == null ? null : actArg + ("1".equals(catalystFill) ? "*20" : "");
                default -> tier.isBlank() ? null : tier;
              };
      List<String> useOmens =
          commit && previewOmens != null
              ? (previewOmens.isBlank() ? List.of() : List.of(previewOmens.split(",")))
              : omens;
      applied = craftClient.apply(new Poe2Craft.ApplyRequest(current, act, arg, useOmens, lock));
      if (lock != null && !commit) {
        // 미리 보기 — 적용하지 않고 결과만(같은 씨앗 · 같은 징조면 "이대로 적용"이 같은 결과), 징조도 쓰지 않는다
        if (applied.error() == null) {
          model.addAttribute("preview", applied);
          model.addAttribute("previewAction", action);
          model.addAttribute("previewArg", arg);
          model.addAttribute("previewOmens", String.join(",", useOmens));
        }
        fill(
            model,
            current,
            hist,
            applied.error() == null ? null : applied,
            action,
            omens,
            tier,
            craftClient.options());
        model.addAttribute("selEssence", essence);
        model.addAttribute("selBone", bone);
        model.addAttribute("foresee", lock.toString());
        historyModel(model, current, hist, spentLog, spentModel(model, poolKey, spentLog));
        rollsModel(model, ids(current), fixedIds(current), rollMap, false);
        model.addAttribute("rollHistJson", json.writeValueAsString(rollHist));
        stashModel(model, stashItem, stashRollMap, recombChance, recombL, recombR);
        recommend(model, optJob);
        return "poe2/htmx/craftEmu";
      }
      if (applied.error() == null) {
        hist.add(current);
        rollHist.add(new java.util.LinkedHashMap<>(rollMap));
        while (hist.size() > HISTORY_MAX) {
          hist.remove(0);
        }
        while (rollHist.size() > hist.size()) {
          rollHist.remove(0);
        }
        current = applied.item();
        // 새로 붙은 옵션은 새 씨앗(C378 검토 — 같은 id 가 다시 붙으면 옛 씨앗이 남아 매번 같은 값이었다)
        for (String id : applied.added() == null ? List.<String>of() : applied.added()) {
          rollMap.remove(id);
        }
        lastAction = action;
        // 쓴 징조는 사라진다
        omens.removeAll(applied.omensUsed() == null ? List.of() : applied.omensUsed());
        spentLog.add(
            new Poe2Craft.SpentStep(
                act,
                "CATALYST".equals(act) ? catalystSpent(applied.note(), arg) : arg,
                applied.omensUsed() == null ? List.of() : applied.omensUsed()));
      }
    }
    fill(model, current, hist, applied, lastAction, omens, tier, craftClient.options());
    // 아이템이 바뀌면 미리 보기 능력은 사라진다(인게임 "Modifying the item in any way removes the ability to foresee")
    model.addAttribute("foresee", lock == null || current != before ? "" : lock.toString());
    // 고른 에센스 · 뼈는 다시 그려도 그대로(C354)
    model.addAttribute("selEssence", essence);
    model.addAttribute("selBone", bone);
    historyModel(model, current, hist, spentLog, spentModel(model, poolKey, spentLog));
    rollsModel(model, ids(current), fixedIds(current), rollMap, reroll);
    model.addAttribute("rollHistJson", json.writeValueAsString(rollHist));
    stashModel(
        model,
        stashItem,
        stashRollMap,
        recombChance,
        recombDone ? null : recombL,
        recombDone ? null : recombR);
    if (divineError != null) {
      model.addAttribute("error", divineError);
    }
    recommend(model, optJob);
    return "poe2/htmx/craftEmu";
  }

  /** 굴림 씨앗(C370) — 숨은 칸 JSON(옵션 id → 씨앗). 깨졌으면 빈 맵. */
  private java.util.Map<String, Long> parseRolls(String s) {
    try {
      java.util.Map<String, Long> m =
          json.readValue(
              s,
              new tools.jackson.core.type.TypeReference<
                  java.util.LinkedHashMap<String, Long>>() {});
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
    // 축성 배수 씨앗(C390) "sanct:id" 는 그 옵션이 남아 있으면 둔다
    rolls
        .keySet()
        .removeIf(
            k -> !(ids.contains(k) || (k.startsWith("sanct:") && ids.contains(k.substring(6)))));
    for (String id : ids) {
      if (!rolls.containsKey(id)
          || (reroll && !fixed.contains(id) && !rolls.containsKey("sanct:" + id))) {
        rolls.put(id, r.nextLong());
      }
    }
    model.addAttribute("rolls", rolls);
    model.addAttribute("rollsJson", json.writeValueAsString(rolls));
  }

  /** 재조합기 보관함(C407) — 숨은 칸 · 재료 줄(API view) · 고른 옵션 · 성공 확률 칸. */
  private void stashModel(
      Model model,
      Poe2Craft.Item stashItem,
      java.util.Map<String, Long> rolls,
      String chance,
      List<String> l,
      List<String> r) {
    model.addAttribute("stashJson", stashItem == null ? "" : json.writeValueAsString(stashItem));
    model.addAttribute(
        "stashRollsJson", json.writeValueAsString(rolls == null ? java.util.Map.of() : rolls));
    // 재조합 목록에 굴린 값(C423 UX QA — 범위만 보여 어느 쪽이 좋은지 몰랐다)
    model.addAttribute("stashRolls", rolls == null ? java.util.Map.of() : rolls);
    Poe2Craft.View sv = null;
    if (stashItem != null) {
      try {
        sv = craftClient.view(stashItem);
      } catch (RuntimeException e) {
        sv = null;
      }
    }
    model.addAttribute("stashView", sv);
    model.addAttribute("recombChance", chance == null ? "" : chance);
    model.addAttribute("recombLSel", l == null ? List.of() : l);
    model.addAttribute("recombRSel", r == null ? List.of() : r);
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

  private static java.util.List<String> ids(Poe2Craft.Item item) {
    return item.affixes() == null
        ? List.of()
        : item.affixes().stream().map(Poe2Craft.Affix::id).toList();
  }

  private static java.util.Set<String> fixedIds(Poe2Craft.Item item) {
    return item.affixes() == null
        ? java.util.Set.of()
        : item.affixes().stream()
            .filter(Poe2Craft.Affix::fractured)
            .map(Poe2Craft.Affix::id)
            .collect(java.util.stream.Collectors.toSet());
  }

  /** 쓴 화폐 기록(C366) — 숨은 칸 JSON. 깨졌으면 빈 기록. */
  private List<Poe2Craft.SpentStep> parseSpent(String s) {
    try {
      List<Poe2Craft.SpentStep> l =
          json.readValue(
              s, new tools.jackson.core.type.TypeReference<List<Poe2Craft.SpentStep>>() {});
      return l == null ? new ArrayList<>() : new ArrayList<>(l);
    } catch (RuntimeException e) {
      return new ArrayList<>();
    }
  }

  /** 쓴 화폐 표(C366) — 기록이 있으면 API 로 화폐별 개수 · 단가 · 합계(디바인). API 가 실패해도 화면은 그린다. */
  private Poe2Craft.SpentResult spentModel(
      Model model, String poolKey, List<Poe2Craft.SpentStep> log) {
    model.addAttribute("spentJson", json.writeValueAsString(log));
    Poe2Craft.SpentResult r = null;
    if (!log.isEmpty()) {
      try {
        r = craftClient.spent(new Poe2Craft.SpentRequest(poolKey, log));
      } catch (RuntimeException e) {
        r = null;
      }
    }
    model.addAttribute("spent", r);
    return r;
  }

  /**
   * 단계 기록(C453 사용자 요청) — 쓴 화폐 기록 한 줄 = 한 단계. 머리카락 · 신성한 오브는 아이템을 이력에 안 남기는 단계(되돌리기와 같은 규칙), 나머지 단계
   * j 는 이력 hist[j - 빠진 수] → 다음 상태. 붙은 · 지운 옵션은 상태마다 옵션 줄(API 한 번)을 id 로 대조. 최근 단계가 위.
   */
  private void historyModel(
      Model model,
      Poe2Craft.Item current,
      List<Poe2Craft.Item> hist,
      List<Poe2Craft.SpentStep> log,
      Poe2Craft.SpentResult spent) {
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    int nf = (int) log.stream().filter(s -> !sideStep(s)).count();
    int dropped = Math.max(0, nf - hist.size());
    List<List<Poe2Craft.Line>> lines = null;
    if (!hist.isEmpty()) {
      List<Poe2Craft.Item> states = new ArrayList<>(hist);
      states.add(current);
      try {
        lines = craftClient.lines(states);
      } catch (RuntimeException e) {
        lines = null;
      }
    }
    List<Poe2Craft.HistRow> rows = new ArrayList<>();
    double cum = 0;
    boolean cumKnown = true;
    int j = -1;
    for (int k = 0; k < log.size(); k++) {
      Poe2Craft.SpentStep s = log.get(k);
      Poe2Craft.StepCost c =
          spent != null && spent.steps() != null && k < spent.steps().size()
              ? spent.steps().get(k)
              : null;
      List<String> nm = c == null ? List.of() : ko ? c.namesKo() : c.names();
      String label = nm == null || nm.isEmpty() ? stepLabel(s, ko) : String.join(" + ", nm);
      Double div = c == null ? null : c.divine();
      if (div == null) {
        cumKnown = false;
      } else {
        cum += div;
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
          new Poe2Craft.HistRow(
              k + 1, label, div, cumKnown ? cum : null, add, rem, undoTo, now, note));
    }
    java.util.Collections.reverse(rows);
    model.addAttribute("historyRows", rows);
    // 맨 처음(또는 기록 한도 안 가장 오래된) 상태로 — 이력이 있을 때만
    model.addAttribute("historyStartUndo", hist.isEmpty() ? null : 0);
    model.addAttribute("historyDropped", dropped);
  }

  /** 아이템을 이력에 안 남기는 단계 — 머리카락 · 신성한 오브(되돌리기와 같은 규칙). */
  private static boolean sideStep(Poe2Craft.SpentStep s) {
    return "FORESEE".equals(s.action()) || "DIVINE".equals(s.action());
  }

  /** 쓴 화폐 이름이 없을 때(드러내기 · 직접 편집 · 바꿔 들기 등) 단계 이름. */
  private static String stepLabel(Poe2Craft.SpentStep s, boolean ko) {
    String a = s.action() == null ? "" : s.action();
    return switch (a) {
      case "REVEAL" -> ko ? "드러내기" : "Reveal";
      case "REVEAL_REROLL" -> ko ? "셋 다시(메아리)" : "Reroll the three";
      case "MANUAL_ADD" -> ko ? "직접 편집 — 넣기" : "Manual add";
      case "MANUAL_REMOVE" -> ko ? "직접 편집 — 빼기" : "Manual remove";
      case "SWAP" -> ko ? "재료와 바꿔 들기" : "Swap with stored item";
      case "RECOMBINE" -> ko ? "재조합" : "Recombine";
      default -> a;
    };
  }

  /** a 에만 있는 옵션 줄의 문구(id 대조) — "접두 T1 문구". */
  private static List<String> lineDiff(List<Poe2Craft.Line> a, List<Poe2Craft.Line> b, boolean ko) {
    java.util.Set<String> bIds = new java.util.HashSet<>();
    for (Poe2Craft.Line l : b) {
      bIds.add(l.id());
    }
    List<String> out = new ArrayList<>();
    for (Poe2Craft.Line l : a) {
      if (bIds.contains(l.id())) {
        continue;
      }
      List<String> t = ko && l.ko() != null && !l.ko().isEmpty() ? l.ko() : l.en();
      String side =
          "prefix".equals(l.gen())
              ? (ko ? "접두 " : "P ")
              : "suffix".equals(l.gen()) ? (ko ? "접미 " : "S ") : "";
      out.add(
          side
              + (l.tierRank() == null ? "" : "T" + l.tierRank() + " ")
              + String.join(", ", t == null ? List.of(l.id()) : t));
    }
    return out;
  }

  /**
   * 다단계 전략(C355) — 목표는 계산기와 같은 칸(t_…), 단계 표 s{n} = "화폐[:인자]", o{n} = 징조 키, u{n} = ""(한 번) | all |
   * pre | suf, m{n} = 최대, g{n} = 다음 줄과 묶음(묶음은 마지막 줄의 u · m 으로 반복).
   */
  @PostMapping(value = "/poe2/htmx/craft/strategy", produces = MediaType.TEXT_HTML_VALUE)
  public String strategy(
      @RequestParam String poolKey,
      @RequestParam(required = false, defaultValue = "82") int ilvl,
      @RequestParam org.springframework.util.MultiValueMap<String, String> params,
      Model model) {
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    List<Poe2Craft.PoolLine> pool =
        craftClient.pool(new Poe2Craft.Item(poolKey, clamp(ilvl), "RARE", List.of(), false));
    List<Poe2Craft.Target> targets = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    List<Integer> pre = new ArrayList<>();
    List<Integer> suf = new ArrayList<>();
    List<Poe2Craft.PoolLine> deseLines = null;
    for (String key : params.keySet()) {
      if (!key.startsWith("t_")) {
        continue;
      }
      String fam = key.substring(2);
      if (fam.startsWith("dese:")) {
        // 훼손된 속성 목표(C425 — 계산기만 받고 전략 모의는 조용히 무시했다)
        if (!"1".equals(params.getFirst(key))) {
          continue;
        }
        if (deseLines == null) {
          try {
            deseLines = craftClient.desecrated(poolKey, clamp(ilvl));
          } catch (RuntimeException e) {
            deseLines = List.of();
          }
        }
        String did = fam.substring("dese:".length());
        Poe2Craft.PoolLine dl =
            deseLines.stream().filter(p -> p.id().equals(did)).findFirst().orElse(null);
        if (dl == null) {
          continue;
        }
        ("prefix".equals(dl.gen()) ? pre : suf).add(targets.size());
        targets.add(new Poe2Craft.Target(List.of(did)));
        List<String> dt = ko && dl.ko() != null && !dl.ko().isEmpty() ? dl.ko() : dl.en();
        labels.add(
            String.join(", ", dt == null ? List.of() : dt) + (ko ? " (훼손)" : " (desecrated)"));
        continue;
      }
      int rank;
      try {
        rank = Integer.parseInt(params.getFirst(key));
      } catch (RuntimeException e) {
        continue;
      }
      List<Poe2Craft.PoolLine> in =
          pool.stream()
              .filter(p -> (p.gen() + ":" + p.family()).equals(fam))
              .filter(p -> p.tierRank() != null && p.tierRank() <= rank)
              .sorted(java.util.Comparator.comparing(Poe2Craft.PoolLine::tierRank))
              .toList();
      if (in.isEmpty()) {
        continue;
      }
      (fam.startsWith("prefix:") ? pre : suf).add(targets.size());
      targets.add(new Poe2Craft.Target(in.stream().map(Poe2Craft.PoolLine::id).toList()));
      labels.add(spanLabel(in, ko) + " (T1" + (rank > 1 ? "~T" + rank : "") + ")");
    }
    List<Integer> all = new ArrayList<>();
    for (int i = 0; i < targets.size(); i++) {
      all.add(i);
    }
    List<Poe2Craft.Step> steps = new ArrayList<>();
    List<Poe2Craft.Step> group = new ArrayList<>();
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
      String o = params.getFirst("o" + i);
      List<String> omens = o == null || o.isBlank() ? null : List.of(o);
      String u = params.getFirst("u" + i);
      List<Integer> until =
          "all".equals(u) ? all : "pre".equals(u) ? pre : "suf".equals(u) ? suf : null;
      if (until != null && until.isEmpty()) {
        until = null;
      }
      Integer max = null;
      try {
        String m = params.getFirst("m" + i);
        max = m == null || m.isBlank() ? null : Math.max(1, Math.min(1000, Integer.parseInt(m)));
      } catch (NumberFormatException e) {
        max = null;
      }
      Poe2Craft.Step one = new Poe2Craft.Step(act, arg, omens, null, null, null);
      if (params.getFirst("g" + i) != null) {
        group.add(one);
        continue;
      }
      if (!group.isEmpty()) {
        group.add(one);
        steps.add(
            new Poe2Craft.Step(
                "SEQ", null, null, until, until == null ? null : (max == null ? 300 : max), group));
        group = new ArrayList<>();
        continue;
      }
      steps.add(
          new Poe2Craft.Step(
              act, arg, omens, until, until == null ? null : (max == null ? 300 : max), null));
    }
    if (!group.isEmpty()) {
      steps.add(new Poe2Craft.Step("SEQ", null, null, null, null, group));
    }
    // 프리셋(C425 — UX QA: 단계 표가 다 비어 처음엔 무엇을 넣을지 몰랐다). '직접 엮기'면 위 단계 표 그대로
    String preset = params.getFirst("preset");
    boolean presetOn = preset != null && !preset.isBlank() && !"custom".equals(preset);
    if (presetOn) {
      steps = presetSteps(preset, poolKey, all);
    }
    Poe2Craft.StrategyResult res =
        targets.isEmpty()
            ? new Poe2Craft.StrategyResult(
                0,
                0,
                0,
                java.util.Map.of(),
                null,
                null,
                List.of(),
                ko ? "목표 옵션의 최소 등급을 하나 이상 고르세요" : "Pick a minimum tier for at least one modifier")
            : steps.isEmpty()
                ? new Poe2Craft.StrategyResult(
                    0,
                    0,
                    0,
                    java.util.Map.of(),
                    null,
                    null,
                    List.of(),
                    ko ? "단계 표에서 화폐를 하나 이상 고르세요" : "Pick at least one currency in the step table")
                : craftClient.strategy(
                    new Poe2Craft.StrategyRequest(
                        // 프리셋은 새 베이스에서(C426 검토 — 지금 아이템이 레어면 첫 단계 연금 · 진화가 실패해 늘 0%)
                        presetOn
                            ? new Poe2Craft.Item(poolKey, clamp(ilvl), "NORMAL", List.of(), false)
                            : startItem(poolKey, clamp(ilvl), params),
                        targets,
                        steps,
                        null));
    model.addAttribute("result", res);
    model.addAttribute("targetLabels", labels);
    model.addAttribute("nameKo", nameKoMap(poolKey));
    model.addAttribute("argName", argNameMap(poolKey));
    return "poe2/htmx/craftStrategy";
  }

  /** 시뮬레이터(C352) — t_접두접미:계열 = 최소 등급(1 = 최상위만). 목표 = 그 계열에서 그 등급 이상 아무거나. */
  @PostMapping(value = "/poe2/htmx/craft/calc", produces = MediaType.TEXT_HTML_VALUE)
  public String calc(
      @RequestParam String poolKey,
      @RequestParam(required = false, defaultValue = "82") int ilvl,
      @RequestParam org.springframework.util.MultiValueMap<String, String> params,
      Model model) {
    List<String> labels = new ArrayList<>();
    List<Poe2Craft.Target> targets = calcTargets(poolKey, ilvl, params, labels);
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    // 표는 지금 아이템이 레어일 때만 그 아이템에서(C461 — '지금 아이템에서 시작'을 기본으로 켰는데 표는 레어만 받는다), 아니면 새 베이스
    Poe2Craft.Item from = startItem(poolKey, clamp(ilvl), params);
    if (from != null
        && from.affixes() != null
        && !from.affixes().isEmpty()
        && !"RARE".equals(from.rarity())) {
      from = new Poe2Craft.Item(poolKey, clamp(ilvl), "NORMAL", List.of(), false);
    }
    Poe2Craft.CalcResponse res =
        targets.isEmpty()
            ? new Poe2Craft.CalcResponse(
                List.of(),
                null,
                null,
                null,
                ko ? "목표 옵션의 최소 등급을 하나 이상 고르세요" : "Pick a minimum tier for at least one modifier")
            : craftClient.calc(
                new Poe2Craft.CalcRequest(
                    from, targets, null, chanceOf(params.getFirst("recombChance"))));
    model.addAttribute("calc", res);
    model.addAttribute("targetLabels", labels);
    model.addAttribute("nameKo", nameKoMap(poolKey));
    return "poe2/htmx/craftCalc";
  }

  /** 시뮬레이터 목표(계열마다 최소 등급 · 훼손 목표) — 표 · 최적 조합이 같이 쓴다(C461). labels 에 화면 이름. */
  private List<Poe2Craft.Target> calcTargets(
      String poolKey,
      int ilvl,
      org.springframework.util.MultiValueMap<String, String> params,
      List<String> labels) {
    Poe2Craft.Item rareBase = new Poe2Craft.Item(poolKey, clamp(ilvl), "RARE", List.of(), false);
    List<Poe2Craft.PoolLine> pool = craftClient.pool(rareBase);
    List<Poe2Craft.Target> targets = new ArrayList<>();
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    List<Poe2Craft.PoolLine> dese = null;
    for (String key : params.keySet()) {
      if (!key.startsWith("t_")) {
        continue;
      }
      String fam = key.substring(2);
      if (fam.startsWith("dese:")) {
        // 훼손된 속성 목표(C420) — 등급 하나, 체크하면 그 옵션
        if (!"1".equals(params.getFirst(key))) {
          continue;
        }
        if (dese == null) {
          try {
            dese = craftClient.desecrated(poolKey, clamp(ilvl));
          } catch (RuntimeException e) {
            dese = List.of();
          }
        }
        String did = fam.substring("dese:".length());
        Poe2Craft.PoolLine dl =
            dese.stream().filter(p -> p.id().equals(did)).findFirst().orElse(null);
        if (dl == null) {
          continue;
        }
        targets.add(new Poe2Craft.Target(List.of(did)));
        List<String> dt = ko && dl.ko() != null && !dl.ko().isEmpty() ? dl.ko() : dl.en();
        labels.add(
            String.join(", ", dt == null ? List.of() : dt) + (ko ? " (훼손)" : " (desecrated)"));
        continue;
      }
      int rank;
      try {
        rank = Integer.parseInt(params.getFirst(key));
      } catch (RuntimeException e) {
        continue;
      }
      List<Poe2Craft.PoolLine> in =
          pool.stream()
              .filter(p -> (p.gen() + ":" + p.family()).equals(fam))
              .filter(p -> p.tierRank() != null && p.tierRank() <= rank)
              .sorted(java.util.Comparator.comparing(Poe2Craft.PoolLine::tierRank))
              .toList();
      if (in.isEmpty()) {
        continue;
      }
      targets.add(new Poe2Craft.Target(in.stream().map(Poe2Craft.PoolLine::id).toList()));
      labels.add(spanLabel(in, ko) + " (T1" + (rank > 1 ? "~T" + rank : "") + ")");
    }
    return targets;
  }

  /** 최적 조합(C461) — 백그라운드로 시작하고 조회하며 기다린다(화면 조각은 PoE1 것, 단위 디바인). */
  @PostMapping(value = "/poe2/htmx/craft/optimize", produces = MediaType.TEXT_HTML_VALUE)
  public String optimize(
      @RequestParam String poolKey,
      @RequestParam(required = false, defaultValue = "82") int ilvl,
      @RequestParam org.springframework.util.MultiValueMap<String, String> params,
      Model model) {
    List<String> labels = new ArrayList<>();
    List<Poe2Craft.Target> targets = calcTargets(poolKey, ilvl, params, labels);
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    String error = null;
    String jobId = null;
    if (targets.isEmpty()) {
      error = ko ? "목표가 없습니다" : "No targets";
    } else {
      try {
        jobId =
            craftClient
                .optimizeStart(
                    new Poe2Craft.OptimizeRequest(
                        startItem(poolKey, clamp(ilvl), params),
                        targets,
                        OPTIMIZE_BUDGET_MS,
                        null,
                        null,
                        chanceOf(params.getFirst("recombChance"))))
                .get("id");
      } catch (RuntimeException e) {
        error = ko ? "최적 조합 계산을 시작하지 못했습니다" : "Could not start the optimal combination";
      }
    }
    model.addAttribute("optError", error);
    model.addAttribute("targetLabels", labels);
    model.addAttribute("jobId", jobId);
    model.addAttribute("progress", 0);
    model.addAttribute("base", "/poe2");
    model.addAttribute("unit", "div");
    model.addAttribute("game", "poe2");
    return error != null ? "poe/htmx/craftOptimize" : "poe/htmx/craftOptimizeWait";
  }

  @org.springframework.web.bind.annotation.GetMapping(
      value = "/poe2/htmx/craft/optimize/{id}",
      produces = MediaType.TEXT_HTML_VALUE)
  public String optimizeStatus(
      @org.springframework.web.bind.annotation.PathVariable String id,
      @RequestParam(required = false) List<String> label,
      Model model) {
    net.luversof.web.gate.poe.dto.PoeCraft.OptimizeStatus st;
    try {
      st = craftClient.optimizeStatus(id);
    } catch (RuntimeException e) {
      st = null;
    }
    model.addAttribute("targetLabels", label == null ? List.of() : label);
    model.addAttribute("base", "/poe2");
    model.addAttribute("unit", "div");
    model.addAttribute("game", "poe2");
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

  /** 전략 프리셋(C425) — 목표는 화면에서 고른 것(all = 목표 전부 번호). */
  private List<Poe2Craft.Step> presetSteps(String preset, String poolKey, List<Integer> all) {
    List<Integer> until = all.isEmpty() ? null : all;
    List<Poe2Craft.Step> s = new ArrayList<>();
    switch (preset) {
      case "alch-chaos" -> {
        s.add(new Poe2Craft.Step("ALCHEMY", null, null, null, null, null));
        s.add(new Poe2Craft.Step("CHAOS", null, null, until, until == null ? null : 300, null));
      }
      case "exalt3" -> {
        s.add(new Poe2Craft.Step("TRANSMUTE", null, null, null, null, null));
        s.add(new Poe2Craft.Step("AUGMENTATION", null, null, null, null, null));
        s.add(new Poe2Craft.Step("REGAL", null, null, null, null, null));
        s.add(new Poe2Craft.Step("EXALT", null, null, until, until == null ? null : 3, null));
      }
      case "dese-echo" -> {
        // 이 종류의 보존된 뼈(아이템 레벨 제한 · 최소 레벨 없는 것)
        String cls = poolKey == null ? "" : poolKey.split("[|]")[0];
        String bone = null;
        try {
          Poe2Craft.Options o = craftClient.options();
          bone =
              (o.bones() == null ? List.<Poe2Craft.Bone>of() : o.bones())
                  .stream()
                      .filter(
                          b ->
                              b.classes() != null
                                  && b.classes().contains(cls)
                                  && b.maxIlvl() == 0
                                  && b.minModLevel() == 0
                                  && !b.breach())
                      .map(Poe2Craft.Bone::slug)
                      .findFirst()
                      .orElse(null);
        } catch (RuntimeException e) {
          bone = null;
        }
        // 맞는 보존된 뼈가 없으면 단계 없음(C426 검토 — 연금만 한 번 모의한 결과가 나왔다) → '화폐를 고르세요' 문구
        if (bone != null) {
          s.add(new Poe2Craft.Step("ALCHEMY", null, null, null, null, null));
          s.add(new Poe2Craft.Step("DESECRATE", bone, null, null, null, null));
          s.add(new Poe2Craft.Step("REVEAL", null, List.of("AbyssalEchoes"), null, null, null));
        }
      }
      default -> {}
    }
    return s;
  }

  /** 시뮬레이터 · 전략 결과의 화폐 · 징조 · 에센스 · 뼈 한국어 이름(C423 UX QA — 한국어 화면 결과에 영어 이름이 섞였다). */
  /**
   * 전략 단계 인자(뼈 · 에센스 슬러그 · 징조 키 · 등급) → 이름(C439 UX QA — 결과에 AbyssalBenchTicketArmour 같은 내부 id 가
   * 보였다).
   */
  private java.util.Map<String, String> argNameMap(String poolKey) {
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    java.util.Map<String, String> m = new java.util.HashMap<>();
    try {
      Poe2Craft.Options o = craftClient.options();
      if (o.bones() != null) {
        o.bones().forEach(b -> m.put(b.slug(), ko && b.nameKo() != null ? b.nameKo() : b.name()));
      }
      if (o.omens() != null) {
        o.omens().forEach(x -> m.put(x.key(), ko && x.nameKo() != null ? x.nameKo() : x.name()));
      }
      craftClient
          .essences(poolKey)
          .forEach(e -> m.put(e.slug(), ko && e.nameKo() != null ? e.nameKo() : e.name()));
    } catch (RuntimeException e) {
      // 이름을 못 읽으면 인자 그대로
    }
    m.put("greater", ko ? "상위" : "Greater");
    m.put("perfect", ko ? "완벽한" : "Perfect");
    m.values().removeIf(v -> v == null || v.isBlank());
    return m;
  }

  private java.util.Map<String, String> nameKoMap(String poolKey) {
    java.util.Map<String, String> m = new java.util.HashMap<>();
    try {
      Poe2Craft.Options o = craftClient.options();
      if (o.currencies() != null) {
        o.currencies().forEach(c -> m.putIfAbsent(c.name(), c.nameKo()));
      }
      if (o.omens() != null) {
        o.omens().forEach(c -> m.putIfAbsent(c.name(), c.nameKo()));
      }
      if (o.bones() != null) {
        o.bones().forEach(c -> m.putIfAbsent(c.name(), c.nameKo()));
      }
      craftClient.essences(poolKey).forEach(e -> m.putIfAbsent(e.name(), e.nameKo()));
    } catch (RuntimeException e) {
      // 이름을 못 읽으면 영어 그대로
    }
    m.put("Hinekora's Lock", "히네코라의 머리카락");
    m.put("Divine Orb", "신성한 오브");
    m.put("Vaal Orb", "바알 오브");
    m.values().removeIf(v -> v == null || v.isBlank());
    // 긴 이름부터(C426 검토 — 라벨 치환이 'Chaos Orb' 를 'Perfect Chaos Orb' 보다 먼저 바꾸면 영어 · 한국어가 섞인다)
    java.util.Map<String, String> sorted = new java.util.LinkedHashMap<>();
    m.entrySet().stream()
        .sorted((x, y) -> y.getKey().length() - x.getKey().length())
        .forEach(e -> sorted.put(e.getKey(), e.getValue()));
    return sorted;
  }

  /**
   * 목표 라벨(C373) — 고른 등급 전체 범위: 가장 낮은 등급 최솟값 ~ T1 최댓값(PoE2 문구는 범위가 들어 있어 영어 기준 자리로 바꾼다). in = 등급 순.
   */
  static String spanLabel(List<Poe2Craft.PoolLine> in, boolean ko) {
    Poe2Craft.PoolLine best = in.get(0);
    Poe2Craft.PoolLine worst = in.get(in.size() - 1);
    boolean useKo = ko && best.ko() != null && !best.ko().isEmpty();
    List<String> txt = useKo ? best.ko() : best.en();
    List<String> out = new ArrayList<>();
    for (int i = 0; i < txt.size(); i++) {
      String b = best.en() != null && i < best.en().size() ? best.en().get(i) : null;
      String w = worst.en() != null && i < worst.en().size() ? worst.en().get(i) : null;
      out.add(net.luversof.web.gate.poe.PoeModSpan.span(txt.get(i), b, w));
    }
    return String.join(", ", out);
  }

  /** 미리 보기 씨앗(C361) — 숫자가 아니면 없음. */
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

  private void fill(
      Model model,
      Poe2Craft.Item item,
      List<Poe2Craft.Item> hist,
      Poe2Craft.View applied,
      String lastAction,
      List<String> omens,
      String tier,
      Poe2Craft.Options opts) {
    Poe2Craft.View view =
        applied != null && applied.error() == null ? applied : craftClient.view(item);
    model.addAttribute("view", view);
    model.addAttribute("error", applied == null ? null : applied.error());
    model.addAttribute("lastAction", lastAction);
    model.addAttribute(
        "added", applied == null || applied.error() != null ? List.of() : applied.added());
    model.addAttribute("stateJson", json.writeValueAsString(view.item()));
    model.addAttribute("historyJson", json.writeValueAsString(hist));
    model.addAttribute("historySize", hist.size());
    // 걸어 둔 엑잘 징조를 표에 반영(C396 — 좌측/우측 찬미 · 촉진하는 찬미)
    model.addAttribute(
        "pool",
        craftClient.pool(
            view.item(),
            omens == null || omens.isEmpty() ? null : omens,
            // 고른 화폐 등급(C441 UX QA — 완벽 진화인데 레벨 1 옵션까지 균등으로 보였다)
            tier == null || tier.isBlank() ? null : tier));
    model.addAttribute("omens", opts.omens() == null ? List.of() : opts.omens());
    model.addAttribute("currencies", opts.currencies() == null ? List.of() : opts.currencies());
    model.addAttribute("activeOmens", omens);
    model.addAttribute("tier", tier == null ? "" : tier);
    List<Poe2Craft.EssenceOption> ess;
    try {
      ess = craftClient.essences(item.poolKey());
    } catch (RuntimeException e) {
      ess = List.of();
    }
    model.addAttribute("essences", ess);
    String cls = item.poolKey() == null ? "" : item.poolKey().split("[|]")[0];
    model.addAttribute(
        "bones",
        (opts.bones() == null ? List.<Poe2Craft.Bone>of() : opts.bones())
            .stream().filter(b -> b.classes() != null && b.classes().contains(cls)).toList());
    Poe2.ModPoolSummary p =
        (opts.pools() == null ? List.<Poe2.ModPoolSummary>of() : opts.pools())
            .stream().filter(x -> x.key().equals(item.poolKey())).findFirst().orElse(null);
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    model.addAttribute(
        "classLabel",
        p == null
            ? item.poolKey()
            : ko && p.itemClassKo() != null ? p.itemClassKo() : p.itemClass());
  }

  /**
   * 시뮬레이터 · 전략 출발 아이템(C357) — "지금 아이템에서 시작"(fromState=1)이고 같은 풀 · 아이템 레벨이면 에뮬레이터 상태, 아니면 새 일반 아이템.
   */
  Poe2Craft.Item startItem(
      String poolKey, int ilvl, org.springframework.util.MultiValueMap<String, String> params) {
    Poe2Craft.Item fresh = new Poe2Craft.Item(poolKey, ilvl, "NORMAL", List.of(), false);
    if (!"1".equals(params.getFirst("fromState"))) {
      return fresh;
    }
    Poe2Craft.Item cur = parse(params.getFirst("state"), null);
    return cur != null && poolKey.equals(cur.poolKey()) && cur.ilvl() == ilvl ? cur : fresh;
  }

  private Poe2Craft.Item parse(String s, Poe2Craft.Item fallback) {
    if (s == null || s.isBlank()) {
      return fallback;
    }
    try {
      Poe2Craft.Item it = json.readValue(s, Poe2Craft.Item.class);
      return it == null || it.poolKey() == null ? fallback : it;
    } catch (RuntimeException e) {
      return fallback;
    }
  }

  private List<Poe2Craft.Item> parseHistory(String s) {
    try {
      List<Poe2Craft.Item> l = json.readValue(s, new TypeReference<List<Poe2Craft.Item>>() {});
      return l == null ? new ArrayList<>() : new ArrayList<>(l);
    } catch (RuntimeException e) {
      return new ArrayList<>();
    }
  }

  private static int clamp(int ilvl) {
    return Math.max(1, Math.min(100, ilvl));
  }

  /** 에뮬레이터 따라 하기(C464) — 최적 조합 작업이 있으면 지금 아이템의 다음 수단. 작업이 사라졌거나 실패하면 표시 안 함. */
  private void recommend(Model model, String optJob) {
    if (optJob == null || optJob.isBlank()) {
      return;
    }
    Object sj = model.getAttribute("stateJson");
    Poe2Craft.Item it = sj == null ? null : parse(sj.toString(), null);
    if (it == null) {
      return;
    }
    try {
      model.addAttribute("recommend", craftClient.optimizeNext(optJob, it));
      model.addAttribute("optJob", optJob);
    } catch (RuntimeException e) {
      // 작업이 사라졌으면(10분) 표시 안 함
    }
  }
}
