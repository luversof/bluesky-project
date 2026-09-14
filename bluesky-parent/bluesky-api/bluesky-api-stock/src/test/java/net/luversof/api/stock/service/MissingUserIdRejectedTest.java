package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 사용자 없는 조회는 빈 결과가 아니라 잘못된 요청이다.
 *
 * <p>실측 2026-09-11(로컬 api-stock, GET): userId 를 빼고 부르면 dividend · ledgerIntegrity · dataStatus ·
 * dataFirstDate · periodSummary · yearlyCost · activityFilterIds 일곱은 400 인데, {@code /api/trade} 와
 * {@code /api/monthlyDividendSnapshot} 만 200 과 빈 배열을 돌려줬다. 빈 배열은 부르는 쪽이 userId 를 흘린 것을 "거래 0 건" 으로
 * 읽게 만든다.
 *
 * <p>계좌가 아직 없는 <b>정상적인 빈 사용자</b>는 그대로 빈 결과여야 한다 &mdash; 그 규칙(계좌 목록이 비면 빈 결과)은 건드리지 않는다.
 */
class MissingUserIdRejectedTest {

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  @Test
  void 매매_조회는_userId_가_없으면_거절한다() throws IOException {
    String service = read("src/main/java/net/luversof/api/stock/service/TradeProfitService.java");

    int at =
        service.indexOf("public List<TradeResponse> getTradeHistory(TradeSearchRequest request)");
    assertThat(at).as("getTradeHistory 를 찾지 못했다").isGreaterThan(0);
    String head = service.substring(at, at + 1200);
    assertThat(head).contains("request.userId() == null");
    assertThat(head).contains("StockErrorCode.INVALID_USER_ID.throwException()");
  }

  @Test
  void 월배당_스냅샷도_같은_규칙이다() throws IOException {
    String service =
        read("src/main/java/net/luversof/api/stock/service/MonthlyDividendSnapshotService.java");

    int at = service.indexOf("findByUserId(UUID userId)");
    assertThat(at).isGreaterThan(0);
    String head = service.substring(at, at + 600);
    assertThat(head).contains("userId == null");
    assertThat(head).contains("StockErrorCode.INVALID_USER_ID.throwException()");
    assertThat(head).as("빈 배열로 넘기던 옛 규칙이 남으면 안 된다").doesNotContain("return List.of();");
  }

  /**
   * 남의 계좌를 짚은 요청은 계좌가 없는 사용자에게도 같은 답을 줘야 한다.
   *
   * <p>실측 2026-09-11: 없는 userId 에 실제 계좌 id 를 붙여 부르면 200 + 빈 배열이었다(데이터는 새지 않았다). 계좌를 가진 사용자가 같은 잘못을
   * 하면 400 이므로, 같은 잘못에 두 갈래 답이 나가고 있었다. 소유권 검사를 빈 계좌 조기 반환보다 <b>앞으로</b> 옮겼다.
   */
  @Test
  void 남의_계좌_지정은_계좌_없는_사용자도_거절한다() throws IOException {
    String service = read("src/main/java/net/luversof/api/stock/service/TradeProfitService.java");

    int at =
        service.indexOf("public List<TradeResponse> getTradeHistory(TradeSearchRequest request)");
    String head = service.substring(at, at + 2600);
    int ownership = head.indexOf("!validAccountIds.containsAll(request.accountIdList())");
    int emptyReturn = head.indexOf("if (accountList.isEmpty()) {");
    assertThat(ownership).as("소유권 검사를 찾지 못했다").isGreaterThan(0);
    assertThat(emptyReturn).as("빈 계좌 조기 반환을 찾지 못했다").isGreaterThan(0);
    assertThat(ownership).as("소유권 검사가 빈 계좌 반환보다 먼저여야 한다").isLessThan(emptyReturn);
  }

  @Test
  void 계좌가_없는_사용자는_여전히_빈_결과다() throws IOException {
    String service = read("src/main/java/net/luversof/api/stock/service/TradeProfitService.java");

    assertThat(service)
        .as("가입만 하고 계좌를 안 만든 사용자에게 오류를 던지면 대시보드가 통째로 오류 상자가 된다")
        .contains("if (accountList.isEmpty()) {");
  }
}
