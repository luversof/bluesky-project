package net.luversof.web.gate.bookkeeping.support;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.CardBill;

/** 자산 화면 카드 줄의 결제 예정액(이번 달 · 다음 달). failed 면 못 불러왔다(안내만). */
public record CardBillView(
    Map<UUID, BigDecimal> thisMonth, Map<UUID, BigDecimal> nextMonth, boolean failed) {

  public static CardBillView of(List<CardBill> thisMonth, List<CardBill> nextMonth) {
    return new CardBillView(totals(thisMonth), totals(nextMonth), false);
  }

  public static CardBillView unavailable() {
    return new CardBillView(Map.of(), Map.of(), true);
  }

  public boolean has(UUID assetId) {
    return thisMonth.containsKey(assetId) || nextMonth.containsKey(assetId);
  }

  public BigDecimal thisMonth(UUID assetId) {
    return thisMonth.getOrDefault(assetId, BigDecimal.ZERO);
  }

  public BigDecimal nextMonth(UUID assetId) {
    return nextMonth.getOrDefault(assetId, BigDecimal.ZERO);
  }

  private static Map<UUID, BigDecimal> totals(List<CardBill> bills) {
    return bills.stream().collect(Collectors.toMap(CardBill::assetId, CardBill::total));
  }
}
