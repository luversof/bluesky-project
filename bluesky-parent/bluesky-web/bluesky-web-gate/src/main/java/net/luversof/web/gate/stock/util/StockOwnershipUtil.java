package net.luversof.web.gate.stock.util;

import java.util.Optional;
import java.util.UUID;

import net.luversof.web.gate.stock.domain.Account;

/**
 * id 로 직접 가져온 계좌가 로그인한 사용자의 것인지 가린다.
 *
 * <p>백엔드의 계좌 단건 조회({@code GET /api/account/{id}})는 소유자를 보지 않는다 &mdash; 실측으로 없는 id 에도 200 과 {@code
 * null} 을, 있는 id 에는 요청자가 누구든 그 계좌를 그대로 돌려준다. 게이트가 유일한 관문이므로 확인은 여기서만 이뤄진다.
 *
 * <p>확인을 빠뜨리면 남의 계좌 id 로 들어왔을 때 계좌 <b>이름</b>이 화면에 찍힌다(거래·손익은 userId 로 조회하므로 금액까지 섞이지는 않는다). 규칙이 호출부
 * 안에 한 줄로만 있어 지워도 아무 테스트가 깨지지 않았기에, 밖으로 꺼내 고정한다.
 *
 * <p>소유권이 걸린 단건 조회는 계좌뿐이다(종목은 사용자 공용 참조 데이터라 해당 없음). 앞으로 계좌를 id 로 읽는 화면이 생기면 이 메서드를 쓴다.
 */
public final class StockOwnershipUtil {

  private StockOwnershipUtil() {}

  /**
   * 소유자가 일치할 때만 계좌를 돌려주고, 그 밖에는 모두 {@code null} 이다.
   *
   * <p>{@code userId} 가 없거나 계좌에 {@code userId} 가 없으면 "확인할 수 없음" 이므로 통과시키지 않는다.
   */
  public static Account ownedOrNull(Optional<Account> found, UUID userId) {
    if (found == null || userId == null) {
      return null;
    }
    return found.filter(account -> userId.equals(account.userId())).orElse(null);
  }
}
