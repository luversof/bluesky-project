package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.DividendView;

/** 배당 한 건 → 목록 한 줄의 규칙(2026-10-22 dividendList 람다에서 toDividendView 로 옮기며 처음 둔 시험). */
class StockDividendHtmxControllerToDividendViewTest {

  private static final UUID TAXABLE = UUID.randomUUID();
  private static final UUID DEFERRED = UUID.randomUUID();
  private static final UUID STOCK = UUID.randomUUID();
  private static final Map<UUID, String> ACCOUNTS = Map.of(TAXABLE, "위탁", DEFERRED, "연금저축");
  private static final Map<UUID, String> STOCKS = Map.of(STOCK, "삼성전자");
  private static final Map<UUID, Boolean> DEFERRED_MAP = Map.of(TAXABLE, false, DEFERRED, true);

  private static DividendResponse dividend(
      UUID account,
      UUID stock,
      String name,
      Integer quantity,
      String taxPerShare,
      String gross,
      String tax,
      String taxable,
      String net) {
    return new DividendResponse(
        UUID.randomUUID(),
        account,
        stock,
        name,
        null,
        quantity,
        null,
        bd(taxPerShare),
        bd(gross),
        null,
        bd(tax),
        bd(taxable),
        bd(net),
        null,
        null);
  }

  private static BigDecimal bd(String value) {
    return value == null ? null : new BigDecimal(value);
  }

  private static DividendView view(DividendResponse dividend) {
    return StockDividendHtmxController.toDividendView(
        dividend, ACCOUNTS, STOCKS, DEFERRED_MAP, "?");
  }

  @Test
  void 일반_계좌는_기록된_세금_과세표준_세후를_쓴다() {
    DividendView row = view(dividend(TAXABLE, STOCK, null, 10, "90", "1000", "154", "1000", "846"));

    assertThat(row.accountName()).isEqualTo("위탁");
    assertThat(row.stockItemName()).as("응답에 이름이 없으면 이름 지도에서").isEqualTo("삼성전자");
    assertThat(row.tax()).isEqualByComparingTo("154");
    assertThat(row.taxableAmount()).isEqualByComparingTo("1000");
    assertThat(row.netAmount()).isEqualByComparingTo("846");
  }

  @Test
  void 과세이연_계좌는_세금과_과세표준이_0_이고_세후는_세전() {
    DividendView row =
        view(dividend(DEFERRED, STOCK, "이름", 10, "90", "1000", "154", "1000", "846"));

    assertThat(row.stockItemName()).as("응답의 이름이 먼저").isEqualTo("이름");
    assertThat(row.tax()).isEqualByComparingTo("0");
    assertThat(row.taxableAmount()).isEqualByComparingTo("0");
    assertThat(row.netAmount()).isEqualByComparingTo("1000");
  }

  @Test
  void 과세표준이_비면_주당_과세표준_곱하기_수량() {
    DividendView row = view(dividend(TAXABLE, STOCK, null, 10, "90", "1000", "139", null, null));

    assertThat(row.taxableAmount()).isEqualByComparingTo("900");
    assertThat(row.netAmount()).as("세후 기록이 없으면 세전 - 세금").isEqualByComparingTo("861");
    assertThat(row.taxableBasePerShare()).isEqualByComparingTo("90");
  }

  @Test
  void 모르는_계좌_종목과_빈_금액() {
    DividendView row =
        view(
            dividend(
                UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, null, null, null));

    assertThat(row.accountName()).isEqualTo("?");
    assertThat(row.stockItemName()).isEqualTo("?");
    assertThat(view(dividend(TAXABLE, null, null, 1, null, "10", "0", null, null)).stockItemName())
        .as("종목 id 도 이름도 없으면")
        .isEqualTo("?");
    assertThat(row.grossAmount()).isEqualByComparingTo("0");
    assertThat(row.tax()).isEqualByComparingTo("0");
    assertThat(row.taxableAmount()).as("주당 과세표준도 수량도 없으면 0").isEqualByComparingTo("0");
  }
}
