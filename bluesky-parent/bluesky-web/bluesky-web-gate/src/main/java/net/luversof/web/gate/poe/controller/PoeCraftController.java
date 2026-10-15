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
  static final int HISTORY_MAX = 30;

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
          "ESSENCE");

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
      @RequestParam(required = false, defaultValue = "RESET") String action,
      @RequestParam(required = false) String essence,
      @RequestParam(required = false, defaultValue = "") String classLabel,
      Model model) {
    model.addAttribute("classLabel", classLabel);
    PoeCraft.Item fresh =
        new PoeCraft.Item(itemClass, variant, influence, clamp(ilvl), "NORMAL", List.of(), false);
    PoeCraft.Item current = parse(state, fresh);
    List<PoeCraft.Item> hist = parseHistory(history);
    String lastAction = null;
    if ("RESET".equals(action) || !same(current, fresh)) {
      // 종류 · 변형 · 영향력 · 아이템 레벨을 바꾸면 새 일반 아이템
      current = fresh;
      hist = new ArrayList<>();
    } else if ("UNDO".equals(action)) {
      if (!hist.isEmpty()) {
        current = hist.remove(hist.size() - 1);
      }
    } else if (ACTIONS.contains(action)) {
      PoeCraft.View v =
          craftClient.apply(new PoeCraft.ApplyRequest(current, action, essence, null));
      if (v.error() == null) {
        hist.add(current);
        while (hist.size() > HISTORY_MAX) {
          hist.remove(0);
        }
        current = v.item();
        lastAction = action;
      }
      fill(model, current, hist, v, lastAction);
      return "poe/htmx/craftEmu";
    }
    fill(model, current, hist, null, null);
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
    List<PoeCraft.PoolLine> pool = craftClient.pool(new PoeCraft.PoolRequest(item, "RARE"));
    List<PoeCraft.Target> targets = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    for (String key : params.keySet()) {
      if (!key.startsWith("t_")) {
        continue;
      }
      String fam = key.substring(2);
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
      PoeCraft.PoolLine best = in.get(0);
      List<String> txt = ko && best.ko() != null && !best.ko().isEmpty() ? best.ko() : best.en();
      labels.add(String.join(", ", txt) + " (T1" + (rank > 1 ? "~T" + rank : "") + ")");
    }
    PoeCraft.CalcResponse res =
        targets.isEmpty()
            ? new PoeCraft.CalcResponse(
                List.of(),
                null,
                null,
                ko ? "목표 옵션의 최소 등급을 하나 이상 고르세요" : "Pick a minimum tier for at least one modifier")
            : craftClient.calc(
                new PoeCraft.CalcRequest(
                    item, targets, null, essence.isBlank() ? null : essence, null));
    model.addAttribute("calc", res);
    model.addAttribute("targetLabels", labels);
    return "poe/htmx/craftCalc";
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
    model.addAttribute("essences", craftClient.essences(item.itemClass()));
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

  private List<PoeCraft.Item> parseHistory(String s) {
    try {
      List<PoeCraft.Item> l = json.readValue(s, new TypeReference<List<PoeCraft.Item>>() {});
      return l == null ? new ArrayList<>() : new ArrayList<>(l);
    } catch (RuntimeException e) {
      return new ArrayList<>();
    }
  }
}
