package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 원장 점검을 부를 때 예시를 넉넉히 받아 오는지 본다.
 *
 * <p>api-stock 은 규칙마다 예시를 {@code maxExamples} 개까지만 담아 준다. 기본값은 3 인데 그대로 두면 화면이 발견의 절반 이상을 감춘다
 * &mdash; 실측 2026-08-23: 발견 45 건 중 25 건이 예시 밖이었다. 조치하려면 어느 행인지 알아야 하므로 게이트가 20 을 요청한다.
 *
 * <p>이 값이 조용히 내려가도 화면은 멀쩡해 보이고 목록만 짧아진다. 그래서 못박는다 &mdash; 실제로 20 을 2 로 낮추는 뮤테이션이 게이트 359 개 검사 중
 * <b>하나도</b> 깨뜨리지 않았다.
 */
class LedgerIntegrityRequestTest {

  @Test
  void 원장_점검_예시를_넉넉히_받아_온다() {
    assertThat(StockViewController.LEDGER_INTEGRITY_MAX_EXAMPLES)
        .as("예시가 적으면 조치할 행을 화면에서 볼 수 없다(현재 가장 많은 규칙이 12 건)")
        .isGreaterThanOrEqualTo(20);
  }
}
