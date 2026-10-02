package net.luversof.web.gate.poe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 젬 DPS 랭킹 시즌 비교(2026-10-02 사용자 요청, PoE1·PoE2 공용) — 지금 보는 시즌의 각 젬이 비교 시즌에서 몇 위였는지, 순위가 얼마나 움직였는지,
 * DPS 가 몇 % 바뀌었는지. 키는 젬 slug(같은 젬이 시즌마다 같은 slug), 순위는 목록 순서(1부터 — 화면 순위 칸과 같다).
 */
public final class RankCompare {

  private RankCompare() {}

  /**
   * 한 젬의 변화 — prevRank 가 null 이면 비교 시즌엔 없던 젬(새로 들어옴), rankDelta 는 올라간 칸 수(양수 = 상승), dpsPct 는 DPS 증감
   * %.
   */
  public record Change(Integer prevRank, Integer rankDelta, Double dpsPct) {

    public boolean isNew() {
      return prevRank == null;
    }
  }

  /** slugs·dps 는 같은 순서(순위 순). 비교 목록이 비었으면 빈 맵(비교 없음). */
  public static Map<String, Change> compare(
      List<String> currentSlugs,
      List<Double> currentDps,
      List<String> previousSlugs,
      List<Double> previousDps) {
    Map<String, Change> out = new HashMap<>();
    if (previousSlugs == null || previousSlugs.isEmpty() || currentSlugs == null) {
      return out;
    }
    Map<String, Integer> prevRank = new HashMap<>();
    Map<String, Double> prevValue = new HashMap<>();
    for (int i = 0; i < previousSlugs.size(); i++) {
      String slug = previousSlugs.get(i);
      if (slug != null && !prevRank.containsKey(slug)) {
        prevRank.put(slug, i + 1);
        prevValue.put(
            slug, previousDps == null || i >= previousDps.size() ? null : previousDps.get(i));
      }
    }
    for (int i = 0; i < currentSlugs.size(); i++) {
      String slug = currentSlugs.get(i);
      if (slug == null || out.containsKey(slug)) {
        continue;
      }
      Integer before = prevRank.get(slug);
      if (before == null) {
        out.put(slug, new Change(null, null, null));
        continue;
      }
      Double was = prevValue.get(slug);
      Double now = currentDps == null || i >= currentDps.size() ? null : currentDps.get(i);
      Double pct = was == null || now == null || was == 0 ? null : (now - was) / was * 100.0;
      out.put(slug, new Change(before, before - (i + 1), pct));
    }
    return out;
  }

  /** 시즌 목록(새 것 먼저)에서 보는 시즌 바로 앞(더 오래된) 시즌 — 없으면 null. viewed 가 비었으면 맨 앞(지금 시즌)을 본다고 친다. */
  public static String previousSeason(List<String> seasons, String viewed) {
    if (seasons == null || seasons.isEmpty()) {
      return null;
    }
    int idx = viewed == null || viewed.isBlank() ? 0 : seasons.indexOf(viewed);
    return idx >= 0 && idx + 1 < seasons.size() ? seasons.get(idx + 1) : null;
  }
}
