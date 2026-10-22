package net.luversof.web.gate.poe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * 확률 표 계열 묶음(10-21 사용자 요청 "접두 · 접미를 등급별로 한 줄씩 무수히 — 옵션별로 묶어 줄을 줄이고, 누르면 등급 상세, 제작으로 붙은 옵션은 표시"). 이
 * 아이템 종류의 전체 계열(일반 → 레어 풀)을 같은 순서로 한 줄씩 두고(제작할 때마다 줄이 사라지거나 순서가 바뀌지 않게), 등급은 펼침에. 확률은 지금 아이템에 붙을 수
 * 있는 풀에서, 붙은 옵션(아이템 줄 id)은 그 계열 줄에 표시한다. PoE1 · PoE2 공용 — 줄 모양이 달라 꺼내는 함수를 받는다.
 *
 * @author bluesky
 */
public final class CraftPoolGroups {

  private CraftPoolGroups() {}

  /**
   * 계열을 펼치면 불러오는 '같은 옵션을 얻는 다른 길' 주소(10-22 P4) — 아이템 종류 · 변형 · 영향력 · 아이템 레벨까지, 계열은 줄마다 &family= 로
   * 붙인다.
   */
  public static String waysUrl(
      String base, String itemClass, String variant, String influence, int ilvl) {
    java.util.function.UnaryOperator<String> enc =
        v ->
            java.net.URLEncoder.encode(v == null ? "" : v, java.nio.charset.StandardCharsets.UTF_8);
    return base
        + "?itemClass="
        + enc.apply(itemClass)
        + "&variant="
        + enc.apply(variant)
        + "&influence="
        + enc.apply(influence)
        + "&ilvl="
        + ilvl;
  }

  /** 계열 키를 주소 인자로. */
  public static String enc(String v) {
    return java.net.URLEncoder.encode(v == null ? "" : v, java.nio.charset.StandardCharsets.UTF_8);
  }

  /** 줄에서 꺼내는 함수 — id · 접두/접미 · 계열 · 등급 순위(1 = 최상위, 없으면 null) · 지금 붙을 확률. */
  public record Accessors<T>(
      Function<T, String> id,
      Function<T, String> gen,
      Function<T, String> family,
      Function<T, Integer> rank,
      ToDoubleFunction<T> chance) {}

  /**
   * 등급 하나 — chance = 지금 붙을 확률(지금 풀에 없으면 0), rollable = 지금 풀에 있음, applied = 아이템에 붙은 등급, fresh = 방금
   * 붙은 등급.
   */
  public record Tier<T>(T line, double chance, boolean rollable, boolean applied, boolean fresh) {}

  /**
   * 계열 한 줄 — key = "접두/접미:계열"(계산기 목표 키와 같다), top = 가장 높은 등급, sum = 지금 붙을 확률 합, topChance = top 이 붙을
   * 확률, applied = 붙은 등급(없으면 null).
   */
  public record Family<T>(
      String gen,
      String key,
      T top,
      List<Tier<T>> tiers,
      double sum,
      double topChance,
      Tier<T> applied) {

    /** 지금 붙을 수 있는 등급이 하나라도 있나. */
    public boolean rollable() {
      return tiers.stream().anyMatch(Tier::rollable);
    }
  }

  /**
   * 묶는다. full = 전체 계열(고정 순서), pool = 지금 붙을 수 있는 줄(확률), appliedIds = 아이템에 붙은 옵션 id, freshIds = 방금 붙은
   * id. 전체에 없는 풀 줄은 뒤에 붙인다(빠지는 줄이 없게). 계열이 없는 줄은 혼자 한 줄.
   */
  public static <T> List<Family<T>> group(
      Collection<T> full,
      Collection<T> pool,
      Accessors<T> a,
      Set<String> appliedIds,
      Set<String> freshIds) {
    Map<String, T> rollable = new HashMap<>();
    for (T p : pool) {
      rollable.putIfAbsent(a.id().apply(p), p);
    }
    Map<String, List<T>> byKey = new LinkedHashMap<>();
    Set<String> seen = new HashSet<>();
    List<T> all = new ArrayList<>(full);
    all.addAll(pool);
    for (T p : all) {
      String id = a.id().apply(p);
      if (id == null || !seen.add(id)) {
        continue;
      }
      String fam = a.family().apply(p);
      String key = a.gen().apply(p) + ":" + (fam == null || fam.isBlank() ? "id:" + id : fam);
      byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
    }
    List<Family<T>> out = new ArrayList<>();
    for (Map.Entry<String, List<T>> e : byKey.entrySet()) {
      List<T> lines = new ArrayList<>(e.getValue());
      lines.sort(
          Comparator.comparing(
                  (T p) -> a.rank().apply(p) == null ? Integer.MAX_VALUE : a.rank().apply(p))
              .thenComparing(p -> a.id().apply(p)));
      List<Tier<T>> tiers = new ArrayList<>();
      double sum = 0;
      Tier<T> applied = null;
      for (T p : lines) {
        String id = a.id().apply(p);
        T now = rollable.get(id);
        double c = now == null ? 0 : a.chance().applyAsDouble(now);
        boolean on = appliedIds.contains(id);
        Tier<T> t =
            new Tier<>(now == null ? p : now, c, now != null, on, on && freshIds.contains(id));
        tiers.add(t);
        sum += c;
        if (on && applied == null) {
          applied = t;
        }
      }
      T top = lines.get(0);
      out.add(
          new Family<>(
              a.gen().apply(top), e.getKey(), top, tiers, sum, tiers.get(0).chance(), applied));
    }
    return out;
  }
}
