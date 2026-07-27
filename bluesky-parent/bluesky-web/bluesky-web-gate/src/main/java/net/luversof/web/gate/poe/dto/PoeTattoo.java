package net.luversof.web.gate.poe.dto;

import java.util.List;

/** 문신 하나 — API {@code /api/poe/tree/tattoos} 응답 매핑용(브라우징 페이지 표시). */
public record PoeTattoo(
    String dn,
    String name,
    String nameKo,
    String icon,
    /** 대체 대상 노드 종류 — Keystone/Notable/Mastery/Small Attribute/Small Strength/… */
    String targetType,
    int minConnected,
    int maxConnected,
    boolean notable,
    boolean keystone,
    List<String> stats,
    List<String> statsKo) {}
