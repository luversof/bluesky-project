package net.luversof.web.gate.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;

/**
 * 고른 수익률 기간(1 · 3 · 6 · 12 개월 = 최근 N 회 분배금)의 연환산 배당 수익률. 추천 후보와 "지금 눈여겨볼 종목" 이 같은 값을 쓴다 - 이 셈을 바로
 * 보는 시험이 없어 2026-10-22 에 더했다.
 */
class MonthlyContributionPickPeriodYieldTest {

  private static <T> T record(Class<T> type, Map<String, Object> values) {
    try {
      RecordComponent[] components = type.getRecordComponents();
      Class<?>[] types = new Class<?>[components.length];
      Object[] args = new Object[components.length];
      for (int i = 0; i < components.length; i++) {
        types[i] = components[i].getType();
        Object value = values.get(components[i].getName());
        if (value == null && types[i] == boolean.class) {
          value = Boolean.FALSE;
        } else if (value == null && types[i] == long.class) {
          value = 0L;
        } else if (value == null && types[i] == double.class) {
          value = 0d;
        } else if (value == null && types[i] == int.class) {
          value = 0;
        }
        args[i] = value;
      }
      return type.getDeclaredConstructor(types).newInstance(args);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static MonthlyDividendPayoutResponse payout(
      String symbol, String recordDate, String perShare) {
    return record(
        MonthlyDividendPayoutResponse.class,
        Map.of(
            "stockItemSymbol",
            symbol,
            "recordDate",
            LocalDate.parse(recordDate),
            "dividendAmountPerShare",
            new BigDecimal(perShare)));
  }

  private static MonthlyDividendCatalogResponse item(String symbol, String price) {
    return record(
        MonthlyDividendCatalogResponse.class,
        price == null
            ? Map.of("stockItemSymbol", symbol)
            : Map.of("stockItemSymbol", symbol, "currentPrice", new BigDecimal(price)));
  }

  private static final List<MonthlyDividendPayoutResponse> HISTORY =
      List.of(
          payout("AAA", "2026-08-15", "100"),
          payout("AAA", "2026-10-15", "120"),
          payout("AAA", "2026-09-15", "110"));

  @Test
  void 최근_N_회_평균을_연환산한다() {
    var catalog = List.of(item("AAA", "10000"));

    // 최근 1 회 = 120 x 12 / 10,000 = 14.40%
    assertThat(MonthlyContributionPickSupport.periodYieldBySymbol(HISTORY, catalog, 1).get("AAA"))
        .isEqualByComparingTo("14.40");
    // 최근 3 회 평균 110 x 12 / 10,000 = 13.20%
    assertThat(MonthlyContributionPickSupport.periodYieldBySymbol(HISTORY, catalog, 3).get("AAA"))
        .isEqualByComparingTo("13.20");
    // 이력이 N 회보다 짧으면 있는 만큼의 평균
    assertThat(MonthlyContributionPickSupport.periodYieldBySymbol(HISTORY, catalog, 12).get("AAA"))
        .isEqualByComparingTo("13.20");
  }

  @Test
  void 심볼은_앞뒤_공백을_빼고_맞춘다() {
    var history = List.of(payout(" BBB ", "2026-10-15", "50"));

    assertThat(
            MonthlyContributionPickSupport.periodYieldBySymbol(
                history, List.of(item("BBB ", "6000")), 1))
        .containsOnlyKeys("BBB ");
  }

  @Test
  void 현재가나_이력이_없으면_담지_않는다() {
    var catalog = Arrays.asList(item("AAA", null), item("ZZZ", "1000"), item("AAA", "0"), null);

    assertThat(MonthlyContributionPickSupport.periodYieldBySymbol(HISTORY, catalog, 3)).isEmpty();
    assertThat(
            MonthlyContributionPickSupport.periodYieldBySymbol(
                HISTORY, List.of(item("AAA", "10000")), 0))
        .as("회수가 0 이하")
        .isEmpty();
    assertThat(
            MonthlyContributionPickSupport.periodYieldBySymbol(
                null, List.of(item("AAA", "10000")), 3))
        .isEmpty();
  }
}
