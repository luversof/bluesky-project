package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.Account;

/**
 * 계좌 단건 조회의 소유자 확인을 고정한다.
 *
 * <p>백엔드는 계좌 id 만 받으면 요청자가 누구든 그 계좌를 돌려준다(실측). 그래서 이 한 줄이 게이트에서 유일한 관문인데, 예전에는 컨트롤러 본문 안에만 있어서 지워도
 * 깨지는 테스트가 하나도 없었다.
 */
class StockOwnershipUtilTest {

  private static final UUID ME = UUID.randomUUID();
  private static final UUID OTHER = UUID.randomUUID();

  private Account account(UUID ownerId) {
    return new Account(UUID.randomUUID(), ownerId, "한국투자증권 ISA", null, Map.of());
  }

  @Test
  void 내_계좌는_그대로_돌려준다() {
    Account mine = account(ME);
    assertThat(StockOwnershipUtil.ownedOrNull(Optional.of(mine), ME)).isSameAs(mine);
  }

  @Test
  void 남의_계좌는_이름조차_넘기지_않는다() {
    assertThat(StockOwnershipUtil.ownedOrNull(Optional.of(account(OTHER)), ME)).isNull();
  }

  @Test
  void 없는_계좌는_null_이다() {
    assertThat(StockOwnershipUtil.ownedOrNull(Optional.empty(), ME)).isNull();
  }

  /** 로그인 사용자를 알 수 없으면 "확인할 수 없음" 이므로 통과시키지 않는다. */
  @Test
  void 로그인_사용자가_없으면_통과시키지_않는다() {
    assertThat(StockOwnershipUtil.ownedOrNull(Optional.of(account(ME)), null)).isNull();
  }

  /** 계좌에 소유자가 비어 있어도 마찬가지다(null == null 로 통과하면 안 된다). */
  @Test
  void 소유자가_비어있는_계좌도_통과시키지_않는다() {
    assertThat(StockOwnershipUtil.ownedOrNull(Optional.of(account(null)), ME)).isNull();
    assertThat(StockOwnershipUtil.ownedOrNull(Optional.of(account(null)), null)).isNull();
  }

  @Test
  void 조회_결과가_null_이어도_터지지_않는다() {
    assertThat(StockOwnershipUtil.ownedOrNull(null, ME)).isNull();
  }
}
