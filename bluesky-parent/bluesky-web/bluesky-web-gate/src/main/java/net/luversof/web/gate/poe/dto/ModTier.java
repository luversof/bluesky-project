package net.luversof.web.gate.poe.dto;

import java.util.List;

/** 모드 티어 하나 — 최대 롤 기준 문장 (여러 스탯이면 여러 줄). API mod-pool 응답 매핑용. */
public record ModTier(int level, List<String> en, List<String> ko) {}
