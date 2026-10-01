package net.luversof.web.gate.poe2.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.poe2.dto.Poe2;

/** 고유 ↔ 일반 탭 전환 필터 유지(2026-10-01, PoE1 과 같은 ?slot=) — 이 목록에 있는 분류만 받고, 없으면 빈 목록 대신 전체. */
class Poe2ViewControllerResolveClassTest {

  private static final List<Poe2.ItemClass> UNIQUE_CLASSES =
      List.of(
          new Poe2.ItemClass("Helmet", "투구", "armour"), new Poe2.ItemClass("Bow", "활", "weapon"));

  @Test
  void slotCarriedWhenThisListHasTheClass() {
    assertThat(Poe2ViewController.resolveClass(UNIQUE_CLASSES, "all", "Helmet"))
        .isEqualTo("Helmet");
    assertThat(Poe2ViewController.resolveClass(UNIQUE_CLASSES, null, "Bow")).isEqualTo("Bow");
  }

  @Test
  void unknownSlotFallsBackToAll() {
    // 클로는 PoE2 고유가 없다 — 그대로 넘기면 고유 목록이 0개가 된다
    assertThat(Poe2ViewController.resolveClass(UNIQUE_CLASSES, "all", "Claw")).isEqualTo("all");
    assertThat(Poe2ViewController.resolveClass(UNIQUE_CLASSES, "all", null)).isEqualTo("all");
    assertThat(Poe2ViewController.resolveClass(List.of(), "all", "Helmet")).isEqualTo("all");
  }

  @Test
  void explicitItemClassWins() {
    assertThat(Poe2ViewController.resolveClass(UNIQUE_CLASSES, "Bow", "Helmet")).isEqualTo("Bow");
  }
}
