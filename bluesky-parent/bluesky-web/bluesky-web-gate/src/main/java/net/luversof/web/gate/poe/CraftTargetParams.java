package net.luversof.web.gate.poe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.util.MultiValueMap;

/**
 * 계산기 목표 칸 해석 공용(10-21 C552 정리 — PoE1 · PoE2 컨트롤러에 글자 그대로 두 벌이던 것). 묶음(C522) · 빈 자리(C519) 목표와 미리 보기
 * 씨앗. 줄 · 목표 레코드 모양이 게임마다 달라 꺼내는 함수(CraftPoolGroups.Accessors) · 목표를 만드는 함수를 받는다.
 *
 * @author bluesky
 */
public final class CraftTargetParams {

  private CraftTargetParams() {}

  /** 묶음(10-20 C522, craftofexile 계산기 '일부 맞춤') — 줄 끝 체크(t_grp:접두접미:계열)한 계열들. */
  public static Set<String> groupMembers(MultiValueMap<String, String> params) {
    Set<String> out = new LinkedHashSet<>();
    for (String key : params.keySet()) {
      if (key.startsWith("t_grp:") && "1".equals(params.getFirst(key))) {
        out.add(key.substring("t_grp:".length()));
      }
    }
    return out;
  }

  /**
   * 묶음 목표(C522) — 체크한 계열 중 k(t_grpk)개 이상: 계열들의 등급 id 를 합친 목표 k 개(옵션 하나는 계열 하나에만 들어 '서로 다른 k 계열'과
   * 같다). 계열 등급 칸을 고르면 그 등급 이상, 안 고르면 모든 등급. 계산기 · 적중 확률 · 연타 · 전략 · 최적 조합이 같은 목표로. span = 등급 순 줄 →
   * 화면 이름(게임마다 문구 모양이 다르다).
   */
  public static <L, T> void groupTargets(
      MultiValueMap<String, String> params,
      Set<String> members,
      List<L> pool,
      CraftPoolGroups.Accessors<L> acc,
      BiFunction<List<L>, Boolean, String> span,
      Function<List<String>, T> target,
      List<T> targets,
      List<String> labels) {
    if (members.isEmpty()) {
      return;
    }
    boolean ko = PoeText.isKorean();
    List<String> ids = new ArrayList<>();
    List<String> names = new ArrayList<>();
    for (String fam : members) {
      int rank;
      try {
        rank = Integer.parseInt(params.getFirst("t_" + fam));
      } catch (RuntimeException e) {
        rank = 99;
      }
      int r = rank;
      List<L> in =
          pool.stream()
              .filter(p -> (acc.gen().apply(p) + ":" + acc.family().apply(p)).equals(fam))
              .filter(p -> acc.rank().apply(p) != null && acc.rank().apply(p) <= r)
              .sorted(Comparator.comparing(acc.rank()))
              .toList();
      if (in.isEmpty()) {
        continue;
      }
      in.forEach(p -> ids.add(acc.id().apply(p)));
      names.add(span.apply(in, ko) + (r < 99 ? " (T1" + (r > 1 ? "~T" + r : "") + ")" : ""));
    }
    if (names.isEmpty()) {
      return;
    }
    int k;
    try {
      k = Integer.parseInt(params.getFirst("t_grpk"));
    } catch (RuntimeException e) {
      k = 1;
    }
    k = Math.max(1, Math.min(names.size(), k));
    for (int i = 0; i < k; i++) {
      targets.add(target.apply(List.copyOf(ids)));
    }
    labels.add(
        (ko
                ? "묶음 " + names.size() + "개 중 " + k + "개 이상: "
                : "At least " + k + " of " + names.size() + ": ")
            + String.join(" · ", names));
  }

  /**
   * 빈 자리 조건(10-20 C519, craftofexile 계산기 'open affix') — t_open:prefix · t_open:suffix = 그 쪽에 남아야 할
   * 빈 자리 수(목표 하나 = 자리 하나). 이 칸이면 목표 · 이름을 넣고 true.
   */
  public static <T> boolean openTarget(
      String fam,
      String value,
      Function<List<String>, T> target,
      List<T> targets,
      List<String> labels) {
    if (!"open:prefix".equals(fam) && !"open:suffix".equals(fam)) {
      return false;
    }
    int n;
    try {
      n = Math.max(0, Math.min(3, Integer.parseInt(value)));
    } catch (RuntimeException e) {
      n = 0;
    }
    boolean pre = fam.endsWith("prefix");
    for (int i = 0; i < n; i++) {
      targets.add(target.apply(List.of(pre ? "@open:prefix" : "@open:suffix")));
    }
    if (n > 0) {
      labels.add(
          PoeText.isKorean()
              ? "빈 " + (pre ? "접두" : "접미") + " 자리 " + n + "개"
              : n + " open " + (pre ? "prefix" : "suffix"));
    }
    return true;
  }

  /** 미리 보기 씨앗(C360) — 숫자가 아니면 없음. */
  public static Long seedOf(String s) {
    if (s == null || s.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(s.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
