package net.luversof.web.gate.poe.dto;

import java.util.List;

/** poedb 속성부여식 도유 목록 한 줄 — API PoeTreeGraphService.AnointEntry 미러. */
public record PoeAnointEntry(
    int nodeId,
    String name,
    String nameKo,
    List<String> stats,
    List<String> statsKo,
    List<OilRef> oils) {

  public record OilRef(String slug, String name, String nameKo, String icon) {}
}
