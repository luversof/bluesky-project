package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;

/** 시뮬레이터 월배당 표에서 지급 시기 &middot; 계좌 자리에 드는 행만 남긴다(사용자 요청 2026-09-23). */
class SimulatorKeepSlotRowsTest {

  private static MonthlyDividendSnapshotResponse row(String symbol) {
    MonthlyDividendSnapshotResponse row = Mockito.mock(MonthlyDividendSnapshotResponse.class);
    Mockito.when(row.stockItemSymbol()).thenReturn(symbol);
    return row;
  }

  @Test
  void 자리에_드는_행만_남기고_순서는_그대로다() {
    var a = row("0094M0");
    var b = row(" 476800 ");
    var c = row("472150");
    var noSymbol = row(null);

    assertThat(
            StockViewController.keepSlotRows(
                List.of(a, b, c, noSymbol), Set.of("476800", "0094M0")))
        .as("앞뒤 공백은 떼고 맞춘다 · 종목코드 없는 행은 자리를 못 정한다")
        .containsExactly(a, b);
  }

  @Test
  void 조건이_없으면_그대로다() {
    var rows = List.of(row("0094M0"), row(null));
    assertThat(StockViewController.keepSlotRows(rows, null)).isSameAs(rows);
    assertThat(StockViewController.keepSlotRows(rows, Set.of()))
        .as("조건은 걸렸는데 맞는 종목이 없으면 빈 표")
        .isEmpty();
  }
}
