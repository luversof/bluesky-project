package net.luversof.api.stock.web.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * 요청 조합이 어떤 조회 경로로 가는지 고정한다.
 *
 * <p>계좌·거래가 아직 없는 사용자는 오류가 아니라 빈 결과여야 한다. 예전에는 그 경우 {@code StockErrorCode.INVALID_USER_ID} 로 400 을
 * 던져서, 가입만 하고 계좌를 안 만든 사용자에게 대시보드 조각이 통째로 오류 상자로 나갔다(실측: 데이터 없는 userId 로 {@code calculateProfit},
 * {@code timeSeries}, {@code timeSeriesWithSummary}, {@code holdingsSnapshot}, {@code
 * holdingsSnapshotBatch} 5 개가 모두 400).
 *
 * <p>반대로 {@code accountIdList} 를 준 요청은 {@code USER_ACCOUNT} 경로로 가고 거기서 계좌 소유권을 검사한다. 남의 계좌 차단은 그
 * 검사에 달려 있으므로, 이 테스트는 "계좌를 지정하면 USER 경로로 새지 않는다" 를 고정해 그 보호가 우회되지 않게 한다.
 */
class RequestTypeGuardTest {

  private TradeProfitRequest request(List<UUID> accountIdList, List<UUID> stockItemIdList) {
    var request = new TradeProfitRequest();
    request.setUserId(UUID.randomUUID());
    request.setAccountIdList(accountIdList);
    request.setStockItemIdList(stockItemIdList);
    return request;
  }

  @Test
  void 계좌를_지정하지_않으면_사용자_경로다() {
    assertEquals(TradeProfitRequestType.USER, request(null, null).getRequestType());
    assertEquals(TradeProfitRequestType.USER, request(List.of(), List.of()).getRequestType());
  }

  @Test
  void 계좌를_지정하면_소유권을_검사하는_경로로_간다() {
    var accountId = UUID.randomUUID();
    assertEquals(
        TradeProfitRequestType.USER_ACCOUNT, request(List.of(accountId), null).getRequestType());
    assertEquals(
        TradeProfitRequestType.USER_ACCOUNT_STOCKITEM,
        request(List.of(accountId), List.of(UUID.randomUUID())).getRequestType());
  }

  @Test
  void 종목만_지정하면_사용자_종목_경로다() {
    assertEquals(
        TradeProfitRequestType.USER_STOCKITEM,
        request(null, List.of(UUID.randomUUID())).getRequestType());
  }
}
