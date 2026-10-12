package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * PoE2 시뮬 중지(10-12 C269, PoE1 최적화 중지 단추의 짝) — 잠금(시뮬 · 다듬기 실행 중)일 때만 중지가 받아들여지고, 다음
 * 경계(checkCancel)에서 Cancelled 로 끝난다. 새로 잠그면 지난 중지 표시 · 계산 수가 지워진다(앞 실행의 중지가 다음 실행을 바로 끝내면 안 된다).
 */
class Poe2RefineCancelTest {

  private final Poe2RefineService refine = new Poe2RefineService(null, null, null, null, "unused");

  @Test
  void 돌고_있지_않으면_중지_거절() {
    assertThat(refine.requestCancel()).isFalse();
    refine.checkCancel(); // 던지지 않는다
  }

  @Test
  void 잠금_중_중지는_다음_경계에서_끝낸다() {
    assertThat(refine.tryLock()).isTrue();
    refine.checkCancel(); // 아직 요청 없음
    assertThat(refine.requestCancel()).isTrue();
    assertThatThrownBy(refine::checkCancel).isInstanceOf(Poe2RefineService.Cancelled.class);
    refine.unlock();
  }

  @Test
  void 새로_잠그면_지난_중지가_지워진다() {
    assertThat(refine.tryLock()).isTrue();
    refine.requestCancel();
    refine.unlock();
    assertThat(refine.tryLock()).isTrue();
    refine.checkCancel(); // 지난 실행의 중지가 남아 있으면 여기서 던졌다
    assertThat(refine.evals()).isZero();
    assertThat(refine.status().evals()).isZero();
    refine.unlock();
  }
}
