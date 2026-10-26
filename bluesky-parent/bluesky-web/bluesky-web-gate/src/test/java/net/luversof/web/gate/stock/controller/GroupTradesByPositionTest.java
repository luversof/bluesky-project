package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.constant.TradeType;
import net.luversof.web.gate.stock.dto.response.TradeResponse;

/**
 * 배당 수익률 셈이 포지션(계좌 x 종목)마다 거래를 되짚는 차례 - 그 존의 날짜 → 같은 날이면 매수 먼저 → 시각. 같은 날 사고팔면 산 뒤에 판다(판 뒤에 사면 그날
 * 보유가 잠깐 음수가 된다). 2026-10-22 에 이 규칙을 바로 보는 시험을 더했다.
 */
class GroupTradesByPositionTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final UUID ACCOUNT = UUID.randomUUID();
  private static final UUID STOCK = UUID.randomUUID();

  private static TradeResponse trade(UUID id, UUID account, UUID stock, TradeType type, String at) {
    return new TradeResponse(
        id,
        account,
        stock,
        null,
        type,
        1,
        null,
        null,
        null,
        null,
        null,
        at == null ? null : Instant.parse(at));
  }

  @Test
  void 같은_날이면_매수가_먼저고_날짜는_그_존으로() {
    UUID earlier = UUID.randomUUID();
    UUID sellAfterMidnightKst = UUID.randomUUID();
    UUID buyAfternoon = UUID.randomUUID();
    var grouped =
        StockDividendHtmxController.groupTradesByPosition(
            Arrays.asList(
                // UTC 로는 10-21 이지만 KST 로는 10-22 01:00 - 오후 매수와 같은 날
                trade(sellAfterMidnightKst, ACCOUNT, STOCK, TradeType.SELL, "2026-10-21T16:00:00Z"),
                trade(
                    buyAfternoon,
                    ACCOUNT,
                    STOCK,
                    TradeType.BUY,
                    "2026-10-22T06:00:00Z"), // KST 15:00
                trade(earlier, ACCOUNT, STOCK, TradeType.BUY, "2026-10-20T00:00:00Z")),
            KST);

    var list = grouped.get(new StockDividendHtmxController.PositionKey(ACCOUNT, STOCK));
    assertThat(list)
        .as("같은 날(KST)은 시각이 늦어도 매수가 먼저")
        .extracting(TradeResponse::id)
        .containsExactly(earlier, buyAfternoon, sellAfterMidnightKst);
  }

  @Test
  void 포지션마다_나누고_계좌_종목_날짜가_없는_거래는_뺀다() {
    UUID otherAccount = UUID.randomUUID();
    var grouped =
        StockDividendHtmxController.groupTradesByPosition(
            Arrays.asList(
                trade(UUID.randomUUID(), ACCOUNT, STOCK, TradeType.BUY, "2026-10-20T00:00:00Z"),
                trade(
                    UUID.randomUUID(), otherAccount, STOCK, TradeType.BUY, "2026-10-20T00:00:00Z"),
                trade(UUID.randomUUID(), null, STOCK, TradeType.BUY, "2026-10-20T00:00:00Z"),
                trade(UUID.randomUUID(), ACCOUNT, null, TradeType.BUY, "2026-10-20T00:00:00Z"),
                trade(UUID.randomUUID(), ACCOUNT, STOCK, TradeType.BUY, null)),
            KST);

    assertThat(grouped)
        .containsOnlyKeys(
            new StockDividendHtmxController.PositionKey(ACCOUNT, STOCK),
            new StockDividendHtmxController.PositionKey(otherAccount, STOCK));
    assertThat(grouped.get(new StockDividendHtmxController.PositionKey(ACCOUNT, STOCK))).hasSize(1);
  }
}
