package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * 레어 크래프팅용 모드 패밀리 — API {@code /api/poe/mod-pool/for-item-class} 응답 매핑용.
 *
 * @param gen prefix | suffix
 * @param slots 적용 슬롯 카테고리
 * @param tiers best-first (index 0 = 최상위 티어)
 */
public record ModFamily(
    String key, String gen, List<String> slots, List<String> keywords, List<ModTier> tiers) {}
