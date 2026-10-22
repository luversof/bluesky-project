package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 매매 화면의 계좌별 실현손익 표 줄(StockTradeHtmxController.accountRealizedRows).
 *
 * <p>2026-10-22 까지 매매 목록 핸들러 안에 적혀 있었고, 그림 시험(RealizedRateDenominatorRenderTest 등)은 줄을 만들어 넣어 줄 뿐 이
 * 셈을 보지 않았다.
 */
class StockTradeHtmxControllerAccountRealizedTest {

  private static final UUID A = new UUID(0, 1);
  private static final UUID B = new UUID(0, 2);
  private static final UUID C = new UUID(0, 3);

  /** 이름을 준 성분만 채운 손익 행(나머지는 null · 0). */
  private static TradeProfit row(Map<String, Object> values) {
    try {
      RecordComponent[] components = TradeProfit.class.getRecordComponents();
      Class<?>[] types = new Class<?>[components.length];
      Object[] args = new Object[components.length];
      for (int i = 0; i < components.length; i++) {
        types[i] = components[i].getType();
        Object v = values.get(components[i].getName());
        args[i] = v != null ? v : (types[i] == int.class ? 0 : null);
      }
      return TradeProfit.class.getDeclaredConstructor(types).newInstance(args);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static TradeProfit sale(
      UUID account, String name, String sell, String tax, String realized, String own) {
    return row(
        Map.of(
            "accountId", account,
            "accountName", name == null ? "" : name,
            "totalSellAmount", new BigDecimal(sell),
            "totalSellTax", new BigDecimal(tax),
            "realizedProfit", new BigDecimal(realized),
            "realizedProfitNet", new BigDecimal(own)));
  }

  @Test
  void 계좌로_묶고_이름_순이며_매도원가는_매도액_빼기_거래세_빼기_실현손익() {
    List<StockTradeHtmxController.AccountRealizedRow> rows =
        StockTradeHtmxController.accountRealizedRows(
            List.of(
                sale(B, "위탁", "1000000", "1800", "50000", "48000"),
                sale(A, "ISA", "300000", "0", "-20000", "-15000"),
                sale(B, "위탁", "500000", "900", "30000", "31000")));

    assertThat(rows)
        .extracting(StockTradeHtmxController.AccountRealizedRow::name)
        .containsExactly("ISA", "위탁");
    var wituk = rows.get(1);
    assertThat(wituk.accountId()).isEqualTo(B);
    assertThat(wituk.sellAmount()).isEqualByComparingTo("1500000");
    assertThat(wituk.realizedNet()).isEqualByComparingTo("80000");
    // 1,500,000 - 2,700(거래세) - 80,000
    assertThat(wituk.soldCost()).isEqualByComparingTo("1417300");
    assertThat(wituk.realizedOwnBasis()).isEqualByComparingTo("79000");
    assertThat(rows.get(0).soldCost()).as("손실이면 원가가 매도액보다 크다").isEqualByComparingTo("320000");
  }

  /** 같은 이름의 두 계좌는 한 줄로 합치지 않는다 - 계좌 id 로 묶는다. */
  @Test
  void 같은_이름의_계좌도_따로_센다() {
    List<StockTradeHtmxController.AccountRealizedRow> rows =
        StockTradeHtmxController.accountRealizedRows(
            List.of(sale(A, "위탁", "100", "0", "10", "10"), sale(C, "위탁", "200", "0", "20", "20")));
    assertThat(rows).hasSize(2);
    assertThat(rows)
        .extracting(StockTradeHtmxController.AccountRealizedRow::accountId)
        .containsExactlyInAnyOrder(A, C);
  }

  @Test
  void 손익_행이_없으면_줄도_없다() {
    assertThat(StockTradeHtmxController.accountRealizedRows(List.of())).isEmpty();
  }

  private static TradeProfit stockRealized(String name, String realized) {
    java.util.Map<String, Object> values = new java.util.HashMap<>();
    values.put("stockItemId", UUID.nameUUIDFromBytes(String.valueOf(name).getBytes()));
    values.put("stockItemName", name);
    values.put("realizedProfit", realized == null ? null : new BigDecimal(realized));
    return row(values);
  }

  /** 종목별 실현손익 표(stockRealizedRows) - 0 · 값 없음은 빼고, 실현손익 큰 순, 같으면 종목명 순(이름 없으면 뒤로). */
  @Test
  void 종목별은_0을_빼고_실현손익_큰_순이며_같으면_이름_순() {
    List<TradeProfit> rows =
        StockTradeHtmxController.stockRealizedRows(
            java.util.Arrays.asList(
                stockRealized("KT", "50000"),
                stockRealized("삼성전자", "0"),
                stockRealized("NAVER", "-30000"),
                stockRealized("하나금융", null),
                stockRealized("SK텔레콤", "50000"),
                stockRealized(null, "50000"),
                stockRealized("HK이노엔", "120000")));
    assertThat(rows)
        .extracting(TradeProfit::stockItemName)
        .containsExactly("HK이노엔", "KT", "SK텔레콤", null, "NAVER");
  }
}
