package net.luversof.web.gate.poe.dto;

import java.util.List;

/** 한 아이템 종류의 섬뜩한 고정 속성(총주교/포식자) 풀 — API {@code /api/poe/eldritch/for-item-class} 응답 매핑용. */
public record PoeEldritch(
    String itemClass,
    Faction exarchFaction,
    Faction eaterFaction,
    List<Family> exarch,
    List<Family> eater) {

  /** 팩션 이름(총주교/포식자). */
  public record Faction(String name, String nameKo) {}

  /** 한 계열 = 등급 사다리(강→약). */
  public record Family(String key, List<Tier> tiers) {}

  /** 등급 한 줄 — tier(클수록 강함) + 최대롤 문장(en/ko). */
  public record Tier(int tier, List<String> en, List<String> ko) {}
}
