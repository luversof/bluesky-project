package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * 전체 모드 풀(poedb Modifiers식)의 티어 한 줄 — API {@code /api/poe/mods/for-item-class} 응답 매핑용. 최대롤(en/ko)과
 * 최소롤(enMin/koMin)로 수치 범위를 표기한다.
 */
public record PoeModTier(
    String id,
    String name,
    String nameKo,
    int ilvl,
    int weight,
    List<String> en,
    List<String> enMin,
    List<String> ko,
    List<String> koMin) {}
