package net.luversof.api.stock.service.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import net.luversof.api.stock.web.dto.response.DividendTaxReport.BasicTax;
import net.luversof.api.stock.web.dto.response.DividendTaxReport.Slice;

/**
 * 귀속연도별 배당 세금 규칙(국세 기준, 지방소득세는 국세의 10% 를 따로 붙인다).
 *
 * <ul>
 *   <li>종합소득 기본세율: 2023 귀속부터 같은 8 구간(소득세법 제55조).
 *   <li>금융소득 종합과세 기준 2천만 원, 원천징수 14%(소득세법 제14조 · 제129조).
 *   <li>Gross-up: 2024 귀속부터 10%, 그 전 11%(소득세법 제17조 ③).
 *   <li>고배당기업 배당소득 분리과세: 2026 ~ 2029 지급분, 14 / 20 / 25 / 30% 누진(조세특례제한법 제104조의27).
 * </ul>
 *
 * <p>shortcut: 2023 귀속 전 기본세율 구간은 넣지 않았다 - 그런 해는 {@link #supported()} 가 false 이고 세액을 내지 않는다. 지난 해
 * 신고 자료가 필요해지면 구간을 더한다.
 */
public record TaxRuleTable(
    int year, boolean supported, BigDecimal grossUpRate, boolean highDividendRule) {

  public static final BigDecimal THRESHOLD = new BigDecimal("20000000");

  public static final BigDecimal WITHHOLDING_RATE = new BigDecimal("0.14");

  public static final BigDecimal LOCAL_TAX_RATE = new BigDecimal("0.1");

  /** 기본세율 구간 상한과 세율, 누진공제. 마지막 상한은 Long.MAX_VALUE(끝없음). */
  private static final long[][] BASIC = {
    {14_000_000L, 6, 0L},
    {50_000_000L, 15, 1_260_000L},
    {88_000_000L, 24, 5_760_000L},
    {150_000_000L, 35, 15_440_000L},
    {300_000_000L, 38, 19_940_000L},
    {500_000_000L, 40, 25_940_000L},
    {1_000_000_000L, 42, 35_940_000L},
    {Long.MAX_VALUE, 45, 65_940_000L},
  };

  /** 고배당 분리과세 구간: 상한, 세율. */
  private static final long[][] HIGH_DIVIDEND = {
    {20_000_000L, 14},
    {300_000_000L, 20},
    {5_000_000_000L, 25},
    {Long.MAX_VALUE, 30},
  };

  public static TaxRuleTable forYear(int year) {
    return new TaxRuleTable(
        year,
        year >= 2023,
        new BigDecimal(year >= 2024 ? "0.10" : "0.11"),
        year >= 2026 && year <= 2029);
  }

  /** 종합소득 과세표준에 대한 기본세율 산출세액(원 미만 버림). */
  public BigDecimal basicTax(BigDecimal taxBase) {
    return basicTaxDetail(taxBase).tax();
  }

  /** 기본세율 산출세액과 그 구간(세율 · 누진공제) - 화면에 계산 과정을 보이려고 함께 낸다. */
  public BasicTax basicTaxDetail(BigDecimal taxBase) {
    if (taxBase == null || taxBase.signum() <= 0) {
      return new BasicTax(
          taxBase == null ? BigDecimal.ZERO : taxBase, 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }
    for (long[] b : BASIC) {
      if (taxBase.compareTo(BigDecimal.valueOf(b[0])) <= 0) {
        BigDecimal deduction = BigDecimal.valueOf(b[2]);
        BigDecimal tax =
            floor(taxBase.multiply(BigDecimal.valueOf(b[1])).movePointLeft(2).subtract(deduction));
        return new BasicTax(taxBase, (int) b[1], deduction, tax);
      }
    }
    throw new IllegalStateException("unreachable");
  }

  /** 고배당 분리과세 산출세액(구간마다 그 구간 몫에 세율, 원 미만 버림). */
  public BigDecimal highDividendTax(BigDecimal amount) {
    return floor(
        highDividendSlices(amount).stream()
            .map(Slice::tax)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  /** 고배당 분리과세 구간별 몫과 세액(구간 세액은 버리지 않는다 - 합을 버린다). */
  public List<Slice> highDividendSlices(BigDecimal amount) {
    List<Slice> slices = new ArrayList<>();
    if (amount == null || amount.signum() <= 0) {
      return slices;
    }
    BigDecimal lower = BigDecimal.ZERO;
    for (long[] b : HIGH_DIVIDEND) {
      BigDecimal upper = BigDecimal.valueOf(b[0]);
      BigDecimal slice = amount.min(upper).subtract(lower);
      if (slice.signum() <= 0) {
        break;
      }
      slices.add(
          new Slice(slice, (int) b[1], slice.multiply(BigDecimal.valueOf(b[1])).movePointLeft(2)));
      lower = upper;
    }
    return slices;
  }

  static BigDecimal floor(BigDecimal value) {
    return value.setScale(0, RoundingMode.FLOOR);
  }
}
