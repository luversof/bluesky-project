package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * 기간으로 좁힌 선택지 목록에 이미 고른 항목을 되살리는 규칙(2026-10-22 - 배당 · 매매 · 활동 · 자산 성장 9 곳의 같은 루프를 하나로 모음).
 *
 * <p>고른 것이 좁힌 목록에 없으면 전체 목록에서 찾아 맨 앞에 넣는다. 이미 있으면 그대로, 전체 목록에도 없으면(지워진 계좌 등) 넣지 않는다.
 */
class WithSelectedFirstTest {

  record Item(UUID id, String name) {}

  private static final Item A = new Item(UUID.randomUUID(), "A");
  private static final Item B = new Item(UUID.randomUUID(), "B");
  private static final Item C = new Item(UUID.randomUUID(), "C");

  private static List<Item> run(List<Item> narrowed, List<UUID> requested) {
    return StockBaseHtmxController.withSelectedFirst(
        narrowed, List.of(A, B, C), requested, Item::id);
  }

  @Test
  void 좁힌_목록에_없는_선택은_앞에_되살린다() {
    assertThat(run(List.of(B), List.of(C.id()))).containsExactly(C, B);
  }

  @Test
  void 이미_있거나_어디에도_없는_선택은_그대로() {
    assertThat(run(List.of(A, B), List.of(A.id()))).containsExactly(A, B);
    assertThat(run(List.of(A), List.of(UUID.randomUUID()))).containsExactly(A);
  }

  @Test
  void 같은_선택이_두_번_와도_한_번만_넣고_null_은_건너뛴다() {
    List<UUID> requested = new ArrayList<>(Arrays.asList(C.id(), null, C.id()));
    assertThat(run(List.of(B), requested)).containsExactly(C, B);
    assertThat(run(List.of(B), null)).containsExactly(B);
  }

  @Test
  void 받은_목록은_건드리지_않는다() {
    List<Item> narrowed = List.of(B);
    run(narrowed, List.of(A.id()));
    assertThat(narrowed).containsExactly(B);
  }
}
