package net.luversof.api.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.service.TradeProfitService.WmaState;

/**
 * 평균단가(WMA) 상태는 '계좌 + 종목' 단위로 굴리고, 보유 목록은 종목 단위로 합쳐 보여준다.
 *
 * <p>종목만으로 상태를 굴리면 계좌를 가로질러 원가가 섞인다 — A 계좌에서 판 수량의 원가가 B 계좌 평균단가로 빠져나가 남은 원가가 실제와 달라진다(실측: 같은 보유인데
 * 시계열 원가와 계좌별 계산이 0.030% 차이. TIGER 리츠부동산인프라 평균단가가 계좌별 4,366~4,367 인데 합쳐 굴리면 4,380.85 로 나와 화면 두 곳이
 * 서로 다른 값을 보여줬다).
 */
class TradeProfitStateMergeTest {

  private static final UUID STOCK_A = UUID.randomUUID();
  private static final UUID STOCK_B = UUID.randomUUID();

  private WmaState state(UUID stockItemId, long rawQuantity, String cost, String costNet) {
    WmaState s = new WmaState();
    s.setStockItemId(stockItemId);
    s.setQuantity(BigDecimal.valueOf(rawQuantity));
    s.setRawQuantity(rawQuantity);
    s.setTotalCost(new BigDecimal(cost));
    s.setTotalCostNet(new BigDecimal(costNet));
    return s;
  }

  @Test
  void 같은_종목의_계좌별_상태는_한_줄로_합쳐진다() {
    Map<String, WmaState> stateMap = new LinkedHashMap<>();
    // 같은 종목을 두 계좌에 나눠 보유. 계좌별 평균단가는 4,366 과 4,368.
    stateMap.put("A:acc1|S:AAA", state(STOCK_A, 1000, "4366000", "4366500"));
    stateMap.put("A:acc2|S:AAA", state(STOCK_A, 500, "2184000", "2184300"));
    stateMap.put("A:acc1|S:BBB", state(STOCK_B, 10, "50000", "50010"));

    Map<UUID, WmaState> merged = TradeProfitService.mergeStatesByStockItem(stateMap);

    assertEquals(2, merged.size());
    WmaState a = merged.get(STOCK_A);
    assertEquals(1500L, a.getRawQuantity());
    assertEquals(0, new BigDecimal("1500").compareTo(a.getQuantity()));
    assertEquals(0, new BigDecimal("6550000").compareTo(a.getTotalCost()));
    assertEquals(0, new BigDecimal("6550800").compareTo(a.getTotalCostNet()));

    // 합산 평균단가는 계좌별 평균단가 사이에 있어야 한다(4,366 ~ 4,368).
    BigDecimal avg = a.getTotalCost().divide(a.getQuantity(), 2, RoundingMode.HALF_UP);
    assertTrue(avg.compareTo(new BigDecimal("4366")) >= 0, "avg=" + avg);
    assertTrue(avg.compareTo(new BigDecimal("4368")) <= 0, "avg=" + avg);
    assertEquals(0, new BigDecimal("4366.67").compareTo(avg));

    WmaState b = merged.get(STOCK_B);
    assertEquals(10L, b.getRawQuantity());
    assertEquals(0, new BigDecimal("50000").compareTo(b.getTotalCost()));
  }

  @Test
  void 종목_식별자가_없는_상태와_빈_입력은_건너뛴다() {
    Map<String, WmaState> stateMap = new LinkedHashMap<>();
    stateMap.put("A:acc1|I:unknown", state(null, 10, "1000", "1000"));
    stateMap.put("A:acc1|S:AAA", state(STOCK_A, 5, "500", "500"));

    Map<UUID, WmaState> merged = TradeProfitService.mergeStatesByStockItem(stateMap);
    assertEquals(1, merged.size());
    assertEquals(0, new BigDecimal("500").compareTo(merged.get(STOCK_A).getTotalCost()));

    assertTrue(TradeProfitService.mergeStatesByStockItem(null).isEmpty());
    assertTrue(TradeProfitService.mergeStatesByStockItem(new LinkedHashMap<>()).isEmpty());
  }
}
