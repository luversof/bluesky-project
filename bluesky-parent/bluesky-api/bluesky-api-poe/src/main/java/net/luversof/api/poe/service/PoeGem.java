package net.luversof.api.poe.service;

import java.util.List;

/** tools/poe-extract 파이프라인이 생성한 표시용 스킬젬 한 건 (resources/poe/skill-gems.json). */
public record PoeGem(
    String id,
    String slug,
    String name,
    String nameKo,
    boolean isSupport,
    String color,
    int dropLevel,
    boolean requiresStr,
    boolean requiresDex,
    boolean requiresInt,
    Integer castTimeMs,
    String description,
    String descriptionKo,
    List<String> tags,
    List<String> tagsKo,
    List<String> qualityStatLines,
    List<String> qualityStatLinesKo,
    List<PoeGemLevel> levels) {}
