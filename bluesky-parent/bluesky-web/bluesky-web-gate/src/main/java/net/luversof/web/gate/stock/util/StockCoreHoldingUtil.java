package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 포트폴리오를 <b>핵심 보유 vs 나머지</b>로 가른다. 어느 쪽이 핵심인지는 종목의 {@link #CORE_TAG} 태그가 정한다.
 *
 * <p>실측 2026-09-08: 평가액의 83.9% 가 한 종목이라, 총자산 추이 · 자산 성장 차트 · 기간 손익 +9 억이 사실상 그 종목의 주가 그래프였다. 정작 매달
 * 사 모으는 배당 ETF 8 종(2.6 억)이 어떻게 되어 가는지는 어느 화면에서도 한눈에 안 보였다. 종목·태그 필터로 볼 수는 있지만 세 번 클릭해야 하고 나란히 비교가 안
 * 된다.
 *
 * <p>처음에는 <b>평가액 1 위</b>를 핵심으로 자동 판정했다. 그러면 주가가 움직이거나 다른 종목을 더 사는 것만으로 기준이 말없이 바뀌고, 핵심이 둘 이상인
 * 포트폴리오를 다룰 수 없다. 그래서 사용자가 태그로 <b>선언</b>하게 바꿨다(2026-09-08). 같은 방식의 태그 상수가 이미 있다 &mdash; 월배당 기준 관리의
 * {@code "월배당"}.
 *
 * <p>태그를 단 종목이 하나도 없으면 가를 것이 없으므로 화면은 이 카드를 내지 않는다.
 */
public final class StockCoreHoldingUtil {

  private StockCoreHoldingUtil() {}

  /**
   * 핵심 보유를 가리키는 종목 태그.
   *
   * <p>화면 문구가 아니라 <b>데이터 값</b>이다 &mdash; {@code StockItemTag} 테이블에 그대로 저장된 문자열이라 로케일에 따라 바뀌면 안 된다.
   */
  public static final String CORE_TAG = "핵심";

  private static BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  /** 핵심 태그가 달린 종목 id. 태그를 단 종목이 없으면 빈 집합이다. */
  public static Set<UUID> coreStockItemIds(List<StockItem> stockItemList) {
    Set<UUID> ids = new LinkedHashSet<>();
    if (stockItemList == null) {
      return ids;
    }
    for (StockItem item : stockItemList) {
      if (item != null
          && item.id() != null
          && item.tags() != null
          && item.tags().contains(CORE_TAG)) {
        ids.add(item.id());
      }
    }
    return ids;
  }

  /**
   * 한쪽(핵심 또는 나머지)의 합. 합산 손익 = 평가손익 + 실현손익 + 누적 배당.
   *
   * @param label 핵심이 한 종목일 때 그 종목명. 여럿이거나 나머지 쪽이면 null 이라 화면이 개수로 적는다.
   * @param stockCount 보유 중(평가액 &gt; 0)인 종목 수. 합에는 이미 다 판 종목의 실현손익 · 배당도 든다.
   */
  public record Sleeve(
      String label,
      int stockCount,
      BigDecimal evaluationAmount,
      BigDecimal weightPct,
      BigDecimal evaluationProfit,
      BigDecimal realizedProfit,
      BigDecimal dividendTotal,
      BigDecimal combinedProfit) {}

  public record Split(Sleeve core, Sleeve rest) {}

  /**
   * @param profitRows 종목(또는 계좌x종목) 단위 손익. 같은 종목이 여러 줄이면 합친다.
   * @param dividendByStock 종목별 누적 배당(세후)
   * @param coreIds 핵심 태그가 달린 종목 id({@link #coreStockItemIds})
   * @return 태그가 없거나 그 종목의 자료가 하나도 없으면 null
   */
  public static Split split(
      List<TradeProfit> profitRows, Map<UUID, BigDecimal> dividendByStock, Set<UUID> coreIds) {
    if (coreIds == null || coreIds.isEmpty()) {
      return null;
    }
    // 종목별로 모은다: [평가액, 평가손익, 실현손익]
    Map<UUID, BigDecimal[]> byStock = new LinkedHashMap<>();
    Map<UUID, String> names = new LinkedHashMap<>();
    if (profitRows != null) {
      for (TradeProfit row : profitRows) {
        if (row == null || row.stockItemId() == null) {
          continue;
        }
        BigDecimal[] sums =
            byStock.computeIfAbsent(
                row.stockItemId(),
                k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        sums[0] = sums[0].add(nz(row.evaluationAmount()));
        sums[1] = sums[1].add(nz(row.evaluationProfit()));
        sums[2] = sums[2].add(nz(row.realizedProfit()));
        if (row.stockItemName() != null) {
          names.putIfAbsent(row.stockItemId(), row.stockItemName());
        }
      }
    }
    Sleeve core = sleeve(byStock, names, dividendByStock, coreIds, true);
    if (core == null) {
      return null; // 태그는 달렸지만 이 사용자에게 그 종목의 자료가 없다
    }
    Sleeve rest = sleeve(byStock, names, dividendByStock, coreIds, false);
    BigDecimal total = core.evaluationAmount().add(rest.evaluationAmount());
    return new Split(withWeight(core, total), withWeight(rest, total));
  }

  /**
   * @param wantCore true 면 핵심 쪽, false 면 나머지 쪽. 핵심 쪽 자료가 하나도 없으면 null 을 돌려준다.
   */
  private static Sleeve sleeve(
      Map<UUID, BigDecimal[]> byStock,
      Map<UUID, String> names,
      Map<UUID, BigDecimal> dividendByStock,
      Set<UUID> coreIds,
      boolean wantCore) {
    BigDecimal evaluation = BigDecimal.ZERO;
    BigDecimal unrealized = BigDecimal.ZERO;
    BigDecimal realized = BigDecimal.ZERO;
    BigDecimal dividend = BigDecimal.ZERO;
    int holdingCount = 0;
    int rowCount = 0;
    UUID firstId = null;
    boolean any = false;
    for (Map.Entry<UUID, BigDecimal[]> e : byStock.entrySet()) {
      if (coreIds.contains(e.getKey()) != wantCore) {
        continue;
      }
      any = true;
      rowCount++;
      if (firstId == null) {
        firstId = e.getKey();
      }
      if (e.getValue()[0].signum() > 0) {
        // 종목 수는 보유 중인 것만 - 다 판 종목까지 세면 '43종목' 처럼 읽힌다(실측 2026-09-08)
        holdingCount++;
      }
      evaluation = evaluation.add(e.getValue()[0]);
      unrealized = unrealized.add(e.getValue()[1]);
      realized = realized.add(e.getValue()[2]);
    }
    if (dividendByStock != null) {
      for (Map.Entry<UUID, BigDecimal> e : dividendByStock.entrySet()) {
        if (e.getKey() == null || coreIds.contains(e.getKey()) != wantCore) {
          continue;
        }
        any = true;
        dividend = dividend.add(nz(e.getValue()));
      }
    }
    if (wantCore && !any) {
      return null;
    }
    // 핵심이 한 종목이면 이름을 그대로 쓴다. 여럿이면 화면이 개수로 적는다.
    String label = wantCore && rowCount == 1 && firstId != null ? names.get(firstId) : null;
    return new Sleeve(
        label,
        holdingCount,
        evaluation,
        BigDecimal.ZERO,
        unrealized,
        realized,
        dividend,
        unrealized.add(realized).add(dividend));
  }

  private static Sleeve withWeight(Sleeve sleeve, BigDecimal total) {
    return new Sleeve(
        sleeve.label(),
        sleeve.stockCount(),
        sleeve.evaluationAmount(),
        pct(sleeve.evaluationAmount(), total),
        sleeve.evaluationProfit(),
        sleeve.realizedProfit(),
        sleeve.dividendTotal(),
        sleeve.combinedProfit());
  }

  private static BigDecimal pct(BigDecimal part, BigDecimal total) {
    if (total == null || total.signum() <= 0) {
      return BigDecimal.ZERO;
    }
    return part.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
  }
}
