package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.DividendView;

/**
 * 배당 요약의 "변동 요인 (전기 대비)"(StockDividendHtmxController.comparePreviousPeriod).
 *
 * <p>2026-10-22 까지 배당 목록 핸들러가 전기 배당을 합계용 · 종목별용으로 두 번 거르고 세후를 두 번 셈했고, 그 셈을 직접 보는 시험이 없었다.
 */
class StockDividendHtmxControllerPreviousPeriodTest {

  private static final UUID TAXABLE = new UUID(0, 1);
  private static final UUID DEFERRED = new UUID(0, 2);
  private static final UUID OTHER_ACCOUNT = new UUID(0, 3);
  private static final UUID SAMSUNG = new UUID(1, 1);
  private static final UUID KT = new UUID(1, 2);

  private static BigDecimal bd(String v) {
    return v == null ? null : new BigDecimal(v);
  }

  private static DividendResponse previous(
      UUID account, UUID stock, String name, String gross, String tax, String net) {
    return new DividendResponse(
        null, account, stock, name, null, null, null, null, bd(gross), null, bd(tax), null, bd(net),
        null, null);
  }

  /** 종목 이름과 세후 금액만 채운 이번 기간 행. */
  private static DividendView current(String name, String net) {
    try {
      RecordComponent[] components = DividendView.class.getRecordComponents();
      Class<?>[] types = new Class<?>[components.length];
      Object[] values = new Object[components.length];
      for (int i = 0; i < components.length; i++) {
        types[i] = components[i].getType();
        String component = components[i].getName();
        if (component.equals("stockItemName")) {
          values[i] = name;
        } else if (component.equals("netAmount")) {
          values[i] = bd(net);
        } else if (types[i] == boolean.class) {
          values[i] = false;
        } else if (types[i] == int.class) {
          values[i] = 0;
        } else if (types[i] == long.class) {
          values[i] = 0L;
        }
      }
      return DividendView.class.getDeclaredConstructor(types).newInstance(values);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static final Map<UUID, Boolean> DEFERRED_ACCOUNTS =
      Map.of(DEFERRED, true, TAXABLE, false);
  private static final List<StockItem> STOCKS =
      List.of(
          new StockItem(SAMSUNG, "005930", "삼성전자", "KRX", List.of()),
          new StockItem(KT, "030200", "KT", "KRX", List.of()));

  @Test
  void 전기_세후_합은_과세이연이면_세전_아니면_기록된_세후_없으면_세전_빼기_세금() {
    var comparison =
        StockDividendHtmxController.comparePreviousPeriod(
            List.of(
                previous(DEFERRED, SAMSUNG, "삼성전자", "1000", "154", "846"),
                previous(TAXABLE, SAMSUNG, "삼성전자", "500", "77", "423"),
                previous(TAXABLE, KT, "KT", "300", "46", null)),
            List.of(),
            null,
            null,
            DEFERRED_ACCOUNTS,
            STOCKS);
    // 1000(과세이연 - 세전 그대로) + 423(기록된 세후) + 254(세전 300 - 세금 46)
    assertThat(comparison.previousNetAmount()).isEqualByComparingTo("1677");
  }

  @Test
  void 계좌_종목_필터를_전기에도_똑같이_건다() {
    var comparison =
        StockDividendHtmxController.comparePreviousPeriod(
            List.of(
                previous(TAXABLE, SAMSUNG, "삼성전자", "500", "77", "423"),
                previous(OTHER_ACCOUNT, SAMSUNG, "삼성전자", "900", "0", "900"),
                previous(TAXABLE, KT, "KT", "300", "46", "254")),
            List.of(),
            List.of(TAXABLE),
            List.of(SAMSUNG),
            DEFERRED_ACCOUNTS,
            STOCKS);
    assertThat(comparison.previousNetAmount()).isEqualByComparingTo("423");
    assertThat(comparison.contributors())
        .extracting(StockDividendHtmxController.DividendChange::stockName)
        .containsExactly("삼성전자");
  }

  @Test
  void 증감은_차이_있는_종목만_차이_큰_순이고_링크는_같은_이름의_종목() {
    var comparison =
        StockDividendHtmxController.comparePreviousPeriod(
            List.of(
                previous(TAXABLE, SAMSUNG, "삼성전자", "500", "77", "423"),
                previous(TAXABLE, KT, "KT", "300", "46", "254"),
                previous(TAXABLE, null, null, "10", "0", "10")),
            List.of(current("삼성전자", "1000"), current("KT", "254"), current("새 종목", "50")),
            null,
            null,
            DEFERRED_ACCOUNTS,
            STOCKS);
    var changes = comparison.contributors();
    assertThat(changes)
        .extracting(StockDividendHtmxController.DividendChange::stockName)
        .as("KT 는 차이 0 이라 빠진다, 이름 없는 전기 행은 '-'")
        .containsExactly("삼성전자", "새 종목", "-");
    assertThat(changes.get(0).delta()).isEqualByComparingTo("577");
    assertThat(changes.get(0).previousNet()).isEqualByComparingTo("423");
    assertThat(changes.get(0).stockItemId()).isEqualTo(SAMSUNG);
    assertThat(changes.get(1).stockItemId()).as("목록에 없는 이름은 링크 없음").isNull();
    assertThat(changes.get(2).delta()).isEqualByComparingTo("-10");
  }

  /** 세후 규칙 한 줄(afterTaxNet) - 목록 행과 전기 비교가 함께 쓴다. */
  @Test
  void 세후는_과세이연이면_세전_아니면_기록된_세후_없으면_세전_빼기_세금() {
    var recorded = previous(TAXABLE, KT, "KT", "300", "46", "250");
    var missing = previous(TAXABLE, KT, "KT", "300", "46", null);
    var empty = previous(TAXABLE, KT, "KT", null, null, null);
    assertThat(StockDividendHtmxController.afterTaxNet(recorded, true)).isEqualByComparingTo("300");
    assertThat(StockDividendHtmxController.afterTaxNet(recorded, false))
        .isEqualByComparingTo("250");
    assertThat(StockDividendHtmxController.afterTaxNet(missing, false)).isEqualByComparingTo("254");
    assertThat(StockDividendHtmxController.afterTaxNet(empty, false)).isEqualByComparingTo("0");
  }

  /** 빈 목록 필터는 '필터 없음' 이 아니라 '아무것도 고르지 않음' 이다 - 이번 기간과 같은 규칙(DividendFilterRuleTest). */
  @Test
  void 빈_목록_필터는_아무것도_세지_않는다() {
    var previous = List.of(previous(TAXABLE, SAMSUNG, "삼성전자", "500", "77", "423"));
    assertThat(
            StockDividendHtmxController.comparePreviousPeriod(
                    previous, List.of(), List.of(), null, DEFERRED_ACCOUNTS, STOCKS)
                .previousNetAmount())
        .as("계좌 빈 목록")
        .isEqualByComparingTo("0");
    assertThat(
            StockDividendHtmxController.comparePreviousPeriod(
                    previous, List.of(), null, List.of(), DEFERRED_ACCOUNTS, STOCKS)
                .previousNetAmount())
        .as("종목 빈 목록")
        .isEqualByComparingTo("0");
    assertThat(
            StockDividendHtmxController.comparePreviousPeriod(
                    previous, List.of(), null, null, DEFERRED_ACCOUNTS, STOCKS)
                .previousNetAmount())
        .as("필터 없음(null)은 전부")
        .isEqualByComparingTo("423");
  }
}
