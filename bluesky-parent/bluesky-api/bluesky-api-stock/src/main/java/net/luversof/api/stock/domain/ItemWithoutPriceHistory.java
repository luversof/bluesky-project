package net.luversof.api.stock.domain;

import java.util.UUID;

/**
 * 시세 이력이 한 행도 없는 종목.
 *
 * <p>관리 화면은 다른 이상 항목(거래량 0 인데 종가가 바뀐 행, 제한폭을 넘은 행)을 종목·날짜까지 적어 주는데, 이것만 <b>개수만</b> 말했다 - 실측
 * 2026-09-13: "(시세 이력이 없는 종목 4개)". 4 개가 무엇인지 모르면 수집을 고칠 수도, 무시해도 되는지 판단할 수도 없다.
 *
 * <p>개수는 이미 {@code PriceHistoryRowCounts.noHistoryItemCount} 가 센다. 여기서는 <b>무엇인지</b>를 담는다.
 */
public record ItemWithoutPriceHistory(UUID stockItemId, String symbol, String name) {}
