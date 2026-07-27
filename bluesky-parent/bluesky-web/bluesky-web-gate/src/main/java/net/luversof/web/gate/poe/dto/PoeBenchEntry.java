package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * 장인 작업대(벤치크래프트) 모드 한 항목 — API {@code /api/poe/bench/for-item-class} 응답 매핑용. 접두→접미·계열·티어 정렬 순서 그대로
 * 표시한다.
 */
public record PoeBenchEntry(
    String gen,
    int tier,
    int reqLevel,
    String modName,
    List<String> en,
    List<String> enMin,
    List<String> ko,
    List<String> koMin,
    List<Cost> cost) {

  /** 제작 비용 — 화폐 이름(en/ko) × 수량. */
  /** icon = icons/currency 상대 경로(/poe-assets/ 서빙). 없으면 null. */
  public record Cost(String name, String nameKo, String icon, int count) {}
}
