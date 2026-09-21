package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.StockDailyClosePrice;
import net.luversof.api.stock.service.PeriodReturnCalculator.PayoutPoint;
import net.luversof.api.stock.service.PeriodReturnCalculator.PeriodReturn;

/** 기간 수익률(사용자 결정 2026-09-21: 가격 · 합산을 1 · 3 · 6 · 12 개월로). */
class PeriodReturnCalculatorTest {

  private static final UUID ITEM = UUID.randomUUID();
  private static final LocalDate TODAY = LocalDate.of(2026, 9, 21);

  private static StockDailyClosePrice price(String date, String close) {
    return new StockDailyClosePrice(ITEM, LocalDate.parse(date), new BigDecimal(close));
  }

  private static PayoutPoint payout(String recordDate, String amount) {
    return new PayoutPoint(LocalDate.parse(recordDate), new BigDecimal(amount));
  }

  @Test
  void 가격과_합산을_따로_낸다() {
    // 한 달 전 10,000 원 -> 오늘 9,800 원(-2%), 그 사이 분배금 300 원(+3%p) => 합산 +1%
    List<StockDailyClosePrice> prices =
        List.of(
            price("2026-08-21", "10000"), price("2026-09-15", "9900"), price("2026-09-21", "9800"));
    List<PayoutPoint> payouts = List.of(payout("2026-09-15", "300"));

    PeriodReturn result = PeriodReturnCalculator.compute(prices, payouts, TODAY, 1);

    assertThat(result).isNotNull();
    assertThat(result.months()).isEqualTo(1);
    assertThat(result.baseDate()).isEqualTo(LocalDate.of(2026, 8, 21));
    assertThat(result.priceReturnPct()).as("분배금을 뺀 순수 가격 변화").isEqualByComparingTo("-2.00");
    assertThat(result.totalReturnPct())
        .as("분배금을 더한 합산 - 커버드콜은 이 값으로 견줘야 한다")
        .isEqualByComparingTo("1.00");
  }

  /** 기준일이 휴장일이면 그 전 마지막 거래일과 견준다. */
  @Test
  void 기준일이_휴장일이면_직전_거래일을_쓴다() {
    List<StockDailyClosePrice> prices =
        List.of(
            price("2026-08-14", "10000"),
            price("2026-08-24", "10500"),
            price("2026-09-21", "11000"));

    PeriodReturn result = PeriodReturnCalculator.compute(prices, List.of(), TODAY, 1);

    assertThat(result.baseDate())
        .as("8/21 은 자료에 없으니 8/14 와 견준다")
        .isEqualTo(LocalDate.of(2026, 8, 14));
    assertThat(result.priceReturnPct()).isEqualByComparingTo("10.00");
  }

  @Test
  void 이력이_기간을_못_덮으면_아무_값도_내지_않는다() {
    List<StockDailyClosePrice> prices =
        List.of(price("2026-09-07", "10000"), price("2026-09-21", "10500"));

    assertThat(PeriodReturnCalculator.compute(prices, List.of(), TODAY, 1))
        .as("열흘치로 1 개월 수익률을 적으면 거짓이 된다")
        .isNull();
    assertThat(PeriodReturnCalculator.compute(prices, List.of(), TODAY, 12)).isNull();
  }

  /** 기초일 당일의 분배금은 이미 기초 가격에 반영돼 있다 - 두 번 세면 안 된다. */
  @Test
  void 기초일_당일_분배금은_세지_않는다() {
    List<StockDailyClosePrice> prices =
        List.of(price("2026-08-21", "10000"), price("2026-09-21", "10000"));
    List<PayoutPoint> sameDay = List.of(payout("2026-08-21", "500"));
    List<PayoutPoint> nextDay = List.of(payout("2026-08-22", "500"));

    assertThat(PeriodReturnCalculator.compute(prices, sameDay, TODAY, 1).totalReturnPct())
        .isEqualByComparingTo("0.00");
    assertThat(PeriodReturnCalculator.compute(prices, nextDay, TODAY, 1).totalReturnPct())
        .isEqualByComparingTo("5.00");
  }

  @Test
  void 기말_뒤의_분배금도_세지_않는다() {
    List<StockDailyClosePrice> prices =
        List.of(price("2026-08-21", "10000"), price("2026-09-18", "10000"));
    // 마지막 종가(9/18) 뒤의 기준일 - 아직 가격에 반영되지 않았다.
    List<PayoutPoint> later = List.of(payout("2026-09-20", "500"));

    assertThat(PeriodReturnCalculator.compute(prices, later, TODAY, 1).totalReturnPct())
        .isEqualByComparingTo("0.00");
  }

  @Test
  void 값이_모자라면_비운다() {
    assertThat(PeriodReturnCalculator.compute(null, List.of(), TODAY, 1)).isNull();
    assertThat(
            PeriodReturnCalculator.compute(
                List.of(price("2026-08-21", "10000")), List.of(), TODAY, 1))
        .as("한 점으로는 변화를 낼 수 없다")
        .isNull();
    assertThat(
            PeriodReturnCalculator.compute(
                List.of(price("2026-08-21", "0"), price("2026-09-21", "100")), List.of(), TODAY, 1))
        .as("기초가 0 이면 나눌 수 없다")
        .isNull();
    assertThat(
            PeriodReturnCalculator.compute(
                List.of(price("2026-08-21", "10000"), price("2026-09-21", "10000")),
                List.of(),
                TODAY,
                0))
        .isNull();
  }
}
