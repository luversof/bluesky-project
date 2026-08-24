package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 계좌 소유권 검사가 한 곳에만 있는지 본다.
 *
 * <p>이 검사가 빠지면 <b>남의 계좌 id 를 넣은 요청이 그대로 읽힌다.</b> 실제로 그런 적이 있다 &mdash; 같은 조건을 쓰는 {@code
 * calculateProfit} 은 검증을 하고 있었는데 {@code loadAllTrades} 만 빠져 있었다(소스 주석에 기록됨). 원인은 같은 검사가 여러 벌 복사돼
 * 있었던 것이다.
 *
 * <p>게다가 사본들이 서로 달랐다.
 *
 * <ul>
 *   <li>헬퍼({@code assertAccountsOwnedBy})는 {@code userId} 가 null 인 경우도 거부한다.
 *   <li>인라인 사본은 {@code request.getUserId().equals(...)} 라 null 이면 NPE 다(400 대신 500).
 *   <li>한 사본은 오류에 계좌 id 를 싣지 않아 어느 계좌가 문제인지 알 수 없었다.
 * </ul>
 *
 * <p>런타임 쪽은 검증 스크립트의 "남의 계좌 조회 차단" 이 세 엔드포인트로 확인한다. 여기서는 <b>코드가 한 벌인지</b>를 지킨다.
 */
class AccountOwnershipCheckTest {

  private static final Path SERVICE =
      Path.of("src/main/java/net/luversof/api/stock/service/TradeProfitService.java");

  @Test
  void 소유권_비교는_헬퍼_한_곳에만_있다() throws IOException {
    String source = Files.readString(SERVICE, StandardCharsets.UTF_8);

    assertThat(countOccurrences(source, "equals(account.getUserId())"))
        .as("인라인 소유권 비교가 되살아났다. assertAccountsOwnedBy 를 쓸 것")
        .isEqualTo(1);
    assertThat(countOccurrences(source, "assertAccountsOwnedBy"))
        .as("헬퍼 선언 1 + 호출들이 있어야 한다")
        .isGreaterThanOrEqualTo(5);
  }

  /** 헬퍼는 요청자 기준으로 비교해야 한다. 계좌 쪽을 기준으로 삼으면 소유자가 빈 계좌에서 500 이 난다. */
  @Test
  void 헬퍼는_요청자_기준으로_비교하고_null_도_거부한다() throws IOException {
    String source = Files.readString(SERVICE, StandardCharsets.UTF_8);
    int at = source.indexOf("private void assertAccountsOwnedBy");
    assertThat(at).isGreaterThan(0);
    String body = source.substring(at, Math.min(source.length(), at + 400));

    assertThat(body)
        .as("userId 가 null 이면 인가 거부여야 한다(NPE 로 500 이 되면 안 된다)")
        .contains("userId == null");
    assertThat(body).as("요청자 기준 비교").contains("userId.equals(account.getUserId())");
  }

  private int countOccurrences(String source, String needle) {
    int found = 0;
    int at = source.indexOf(needle);
    while (at >= 0) {
      found++;
      at = source.indexOf(needle, at + needle.length());
    }
    return found;
  }
}
