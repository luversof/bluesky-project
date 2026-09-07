package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;

/**
 * 기간 손익을 <b>종목으로</b> 쪼갠 줄.
 *
 * <p>자산 성장은 기간 손익을 시간(달/해)으로만 쪼갤 수 있었다. 종목으로 쪼갤 곳이 없어서 "이 기간에 누가 벌어줬나" 는 답이 없었다. 종목별 표는 두 군데 있었지만 둘
 * 다 반쪽이었다 &mdash; 자산 현황은 <b>지금 보유한 것만</b>, 매매 화면의 종목별 실현손익은 <b>배당이 빠져</b> 있었다. 그래서 배당까지 받고 판 종목은
 * (실측 2026-09-07: 9 종목) 종목 단위로는 어디에도 나타나지 않았다.
 *
 * <p>기여 = <b>평가 변동 + 실현손익 + 배당</b>. 화면의 월별/연도별 성과가 같은 기간을 시간으로 쪼갤 때 쓰는 것과 같은 쪼갬이라, 두 표가 같은 뜻을 말한다.
 *
 * <p>평가 변동은 <b>기말 스냅샷 − 기초 스냅샷</b>이다. 기초 스냅샷이 없으면(= '전체' 를 골라 시작일이 없을 때) 0 으로 둔다.
 *
 * <p>실측 2026-09-07 &mdash; 종목별 기여의 합이 요약의 기간 손익과 <b>정확히 같다</b>(세 기간 모두 차이 0).
 *
 * <ul>
 *   <li>전체: 1,195,952,068 (종목 44)
 *   <li>올해: 825,088,563 (종목 10)
 *   <li>최근 1년: 1,154,629,595 (종목 14)
 * </ul>
 */
public final class StockContributionUtil {

  private StockContributionUtil() {}

  /**
   * 한 종목의 기여.
   *
   * @param heldAtEnd 기말에도 들고 있었는지. 아니면 그 기간에 <b>다 판</b> 종목이라 화면이 그렇게 밝힌다.
   */
  public record Contribution(
      UUID stockItemId,
      String stockItemName,
      BigDecimal unrealizedDelta,
      BigDecimal realizedProfit,
      BigDecimal dividendTotal,
      BigDecimal contribution,
      boolean heldAtEnd) {}

  private static BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static Map<UUID, HoldingsSnapshotItem> index(List<HoldingsSnapshotItem> snapshot) {
    Map<UUID, HoldingsSnapshotItem> map = new LinkedHashMap<>();
    if (snapshot != null) {
      for (HoldingsSnapshotItem item : snapshot) {
        if (item != null && item.stockItemId() != null) {
          map.put(item.stockItemId(), item);
        }
      }
    }
    return map;
  }

  /**
   * @param startSnapshot 기초 보유. '전체' 처럼 시작일이 없으면 null 이어도 된다(기초 0).
   * @param endSnapshot 기말 보유
   * @param realizedByStock 그 기간의 종목별 실현손익({@code groupBy=STOCKITEM})
   * @param dividendByStock 그 기간의 종목별 배당(세후)
   */
  public static List<Contribution> of(
      List<HoldingsSnapshotItem> startSnapshot,
      List<HoldingsSnapshotItem> endSnapshot,
      List<TradeProfit> realizedByStock,
      Map<UUID, BigDecimal> dividendByStock) {
    return of(startSnapshot, endSnapshot, realizedByStock, dividendByStock, Map.of());
  }

  /**
   * @param nameByStock 종목 id -> 이름. <b>반드시 넘겨야 한다.</b>
   *     <p>{@code calculateProfit(groupBy=STOCKITEM)} 은 이름을 주지 않는다(실측 2026-09-07: 43 행 전부 {@code
   *     stockItemName} 이 null). 스냅샷에는 이름이 있지만 그건 <b>기말에 들고 있는 종목만</b>이라, 이름을 밖에서 채우지 않으면 이미 다 판 종목이
   *     전부 이름 없이 '-' 로 나간다 &mdash; 줄은 있는데 읽을 수가 없어 "보유 중인 것만 나온다" 로 보인다(실측: 44 줄 중 35 줄).
   */
  public static List<Contribution> of(
      List<HoldingsSnapshotItem> startSnapshot,
      List<HoldingsSnapshotItem> endSnapshot,
      List<TradeProfit> realizedByStock,
      Map<UUID, BigDecimal> dividendByStock,
      Map<UUID, String> nameByStock) {
    Map<UUID, HoldingsSnapshotItem> start = index(startSnapshot);
    Map<UUID, HoldingsSnapshotItem> end = index(endSnapshot);

    Map<UUID, BigDecimal> realized = new LinkedHashMap<>();
    Map<UUID, String> names = new LinkedHashMap<>();
    if (realizedByStock != null) {
      for (TradeProfit profit : realizedByStock) {
        if (profit == null || profit.stockItemId() == null) {
          continue;
        }
        realized.merge(profit.stockItemId(), nz(profit.realizedProfit()), BigDecimal::add);
        if (profit.stockItemName() != null) {
          names.putIfAbsent(profit.stockItemId(), profit.stockItemName());
        }
      }
    }
    start.forEach((id, item) -> names.putIfAbsent(id, item.name()));
    end.forEach((id, item) -> names.putIfAbsent(id, item.name()));
    if (nameByStock != null) {
      nameByStock.forEach(
          (id, name) -> {
            if (name != null && !name.isBlank()) {
              names.putIfAbsent(id, name);
            }
          });
    }

    // 네 재료 어디에든 나타난 종목이 대상이다. 한 곳만 보면 그 기간에 배당만 받은 종목을 놓친다.
    Set<UUID> ids = new LinkedHashSet<>();
    ids.addAll(end.keySet());
    ids.addAll(start.keySet());
    ids.addAll(realized.keySet());
    if (dividendByStock != null) {
      ids.addAll(dividendByStock.keySet());
    }

    List<Contribution> rows = new ArrayList<>();
    for (UUID id : ids) {
      BigDecimal unrealizedDelta =
          nz(end.containsKey(id) ? end.get(id).unrealizedProfit() : null)
              .subtract(nz(start.containsKey(id) ? start.get(id).unrealizedProfit() : null));
      BigDecimal realizedProfit = nz(realized.get(id));
      BigDecimal dividend = dividendByStock == null ? BigDecimal.ZERO : nz(dividendByStock.get(id));
      BigDecimal contribution = unrealizedDelta.add(realizedProfit).add(dividend);
      // 세 값이 모두 0 이면 그 기간에 아무 일도 없던 종목이다. 줄로 내면 표만 길어진다.
      if (unrealizedDelta.signum() == 0 && realizedProfit.signum() == 0 && dividend.signum() == 0) {
        continue;
      }
      HoldingsSnapshotItem endItem = end.get(id);
      boolean heldAtEnd = endItem != null && nz(endItem.quantity()).signum() > 0;
      rows.add(
          new Contribution(
              id,
              names.get(id),
              unrealizedDelta,
              realizedProfit,
              dividend,
              contribution,
              heldAtEnd));
    }
    // 많이 벌어준 순. 이 표를 읽는 이유가 "누가 벌어줬나" 다.
    rows.sort(
        Comparator.comparing(Contribution::contribution, Comparator.reverseOrder())
            .thenComparing(
                Contribution::stockItemName, Comparator.nullsLast(Comparator.naturalOrder())));
    return rows;
  }

  /** 합계. 이 값은 화면 위 요약 카드의 <b>기간 손익</b>과 같아야 한다(실측: 세 기간 모두 차이 0). */
  public static Contribution total(List<Contribution> rows) {
    return new Contribution(
        null,
        null,
        StockAmountUtil.sum(rows, Contribution::unrealizedDelta),
        StockAmountUtil.sum(rows, Contribution::realizedProfit),
        StockAmountUtil.sum(rows, Contribution::dividendTotal),
        StockAmountUtil.sum(rows, Contribution::contribution),
        false);
  }

  /** 화면에 그대로 내보내기에 표가 길어지는 기준. 전 기간이 44 줄이고 그중 25 줄이 100 만원 미만이었다. */
  public static final int DEFAULT_VISIBLE = 15;

  /**
   * 표에 낼 줄과, 접어 넣은 나머지.
   *
   * @param others 접힌 줄을 하나로 합친 것. 접을 것이 없으면 null
   * @param othersCount 접힌 줄 수
   */
  public record Folded(List<Contribution> rows, Contribution others, int othersCount) {}

  /**
   * 기여가 <b>큰 쪽부터</b> {@code limit} 줄만 내고 나머지는 한 줄로 접는다.
   *
   * <p>고르는 기준은 기여의 <b>절댓값</b>이다 &mdash; 크게 까먹은 종목도 "누가 벌어줬나" 를 읽는 데 꼭 필요한데, 부호 순으로 자르면 그런 줄이 먼저 잘려
   * 나간다.
   *
   * <p>접어도 합계는 그대로다. 합계 줄은 접기 전 전체를 더한 값을 쓴다.
   */
  public static Folded fold(List<Contribution> rows, int limit) {
    if (rows == null || rows.isEmpty()) {
      return new Folded(List.of(), null, 0);
    }
    if (limit <= 0 || rows.size() <= limit) {
      return new Folded(List.copyOf(rows), null, 0);
    }
    List<Contribution> byMagnitude = new ArrayList<>(rows);
    byMagnitude.sort(
        Comparator.comparing(
            (Contribution row) -> nz(row.contribution()).abs(), Comparator.reverseOrder()));
    List<Contribution> visible = new ArrayList<>(byMagnitude.subList(0, limit));
    List<Contribution> hidden = new ArrayList<>(byMagnitude.subList(limit, byMagnitude.size()));
    // 보이는 줄은 다시 기여 순으로. 표는 많이 벌어준 순으로 읽는다.
    visible.sort(
        Comparator.comparing(Contribution::contribution, Comparator.reverseOrder())
            .thenComparing(
                Contribution::stockItemName, Comparator.nullsLast(Comparator.naturalOrder())));
    Contribution others = total(hidden);
    return new Folded(visible, others, hidden.size());
  }
}
