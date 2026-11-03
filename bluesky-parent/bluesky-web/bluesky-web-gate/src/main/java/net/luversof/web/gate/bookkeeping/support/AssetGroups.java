package net.luversof.web.gate.bookkeeping.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import net.luversof.web.gate.bookkeeping.constant.AssetKind;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.AssetBalance;

/** 자산 화면: 종류별 묶음 · 통화별 소계 · 총액. 숨긴 자산은 묶음과 총액에서 빼고 따로 둔다. */
public final class AssetGroups {

  private AssetGroups() {}

  /** 통화별 금액. */
  public record CurrencyAmount(String currency, BigDecimal amount) {}

  /** 한 종류의 자산(목록 순서 그대로)과 통화별 소계. */
  public record Group(AssetKind kind, List<AssetBalance> assets, List<CurrencyAmount> subtotals) {}

  /** 화면 한 장: 종류별 묶음(자산 없는 종류는 빼고 AssetKind 순서), 통화별 총액, 숨긴 자산. */
  public record Page(List<Group> groups, List<CurrencyAmount> totals, List<AssetBalance> hidden) {}

  public static Page of(List<AssetBalance> all) {
    var visible = all.stream().filter(a -> !a.asset().hidden()).toList();
    var groups = new ArrayList<Group>();
    for (var kind : AssetKind.values()) {
      var assets = visible.stream().filter(a -> a.asset().kind() == kind).toList();
      if (!assets.isEmpty()) {
        groups.add(new Group(kind, assets, sumByCurrency(assets)));
      }
    }
    return new Page(
        groups, sumByCurrency(visible), all.stream().filter(a -> a.asset().hidden()).toList());
  }

  /** 통화별 잔액 합(통화 이름 순). 카드 · 대출의 음수 잔액도 그대로 더한다(순자산). */
  static List<CurrencyAmount> sumByCurrency(List<AssetBalance> assets) {
    var sums = new TreeMap<String, BigDecimal>();
    for (var a : assets) {
      sums.merge(a.asset().currency(), a.balance(), BigDecimal::add);
    }
    var result = new ArrayList<CurrencyAmount>();
    sums.forEach((currency, amount) -> result.add(new CurrencyAmount(currency, amount)));
    return result;
  }
}
