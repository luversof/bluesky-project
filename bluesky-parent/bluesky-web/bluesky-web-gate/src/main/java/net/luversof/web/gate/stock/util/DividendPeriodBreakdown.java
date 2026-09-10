package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.luversof.web.gate.stock.dto.response.DividendView;

/**
 * 배당 내역을 연도별·월별로 묶는다.
 *
 * <p>배당 화면에는 월별 막대 차트만 있어 "그 해에 얼마 받았나" 를 숫자로 읽을 수 없었다(사용자 요청 2026-09-10). 차트와 같은 자료를 표로도 보여 준다.
 *
 * <p>묶는 기준은 <b>지급일</b>이다. 표의 다른 숫자와 같은 존을 써야 하루가 어긋나지 않으므로 화면이 쓰는 {@code zone} 을 그대로 받는다 (지급일이 없으면
 * 기준일로 대신한다 - api-stock 이 예정 배당을 기준일만 채워 보내는 경우가 있다).
 */
public final class DividendPeriodBreakdown {

  private DividendPeriodBreakdown() {}

  /**
   * 한 구간(해 또는 달)의 합계.
   *
   * @param label 화면에 그대로 찍는 이름(예: {@code 2026}, {@code 2026-09})
   * @param sortKey 정렬용 키(달은 {@code yyyy-MM}, 해는 {@code yyyy})
   * @param count 배당 건수
   * @param grossAmount 세전 합계
   * @param tax 세금 합계
   * @param netAmount 실수령 합계
   */
  public record Row(
      String label,
      String sortKey,
      int count,
      BigDecimal grossAmount,
      BigDecimal tax,
      BigDecimal netAmount,
      boolean partial,
      LocalDate coveredFrom,
      LocalDate coveredTo) {}

  /** 최근이 위로 오도록 내림차순 정렬한 연도별 합계. */
  public static List<Row> byYear(List<DividendView> dividends, ZoneId zone) {
    return byYear(dividends, zone, null, null);
  }

  /**
   * 조회 구간을 함께 주면 그 해가 구간에 <b>온전히</b> 담겼는지도 표시한다.
   *
   * <p>실측 2026-09-10: '3년' 을 고르면 첫 해(2023)는 9월부터만 들어와 1건 220만원으로 보이는데, 그 해 전체는 4건 761만원이다. 자산 성장의
   * 연도별 성과도 같은 이유로 부분 연도를 표시한다.
   */
  public static List<Row> byYear(
      List<DividendView> dividends, ZoneId zone, LocalDate rangeStart, LocalDate rangeEnd) {
    List<Row> rows = aggregate(dividends, zone, true);
    if (rangeStart == null && rangeEnd == null) return rows;
    List<Row> marked = new ArrayList<>(rows.size());
    for (Row row : rows) {
      int year = Integer.parseInt(row.sortKey());
      LocalDate yearStart = LocalDate.of(year, 1, 1);
      LocalDate yearEnd = LocalDate.of(year, 12, 31);
      LocalDate from = rangeStart != null && rangeStart.isAfter(yearStart) ? rangeStart : yearStart;
      LocalDate to = rangeEnd != null && rangeEnd.isBefore(yearEnd) ? rangeEnd : yearEnd;
      boolean partial = from.isAfter(yearStart) || to.isBefore(yearEnd);
      marked.add(
          new Row(
              row.label(),
              row.sortKey(),
              row.count(),
              row.grossAmount(),
              row.tax(),
              row.netAmount(),
              partial,
              partial ? from : null,
              partial ? to : null));
    }
    return List.copyOf(marked);
  }

  /** 최근이 위로 오도록 내림차순 정렬한 월별 합계. */
  public static List<Row> byMonth(List<DividendView> dividends, ZoneId zone) {
    return byMonth(dividends, zone, null, null);
  }

  /**
   * 조회 구간을 함께 주면 배당이 없던 달도 0 으로 채운다.
   *
   * <p>실측 2026-09-10(qa/dividend-chart-vs-table.cjs): 같은 화면의 월별 막대 차트는 구간의 모든 달을 그리는데(3년 = 37달) 표는
   * 배당이 있는 달만 보여 23줄이었다. 2023-11 다음이 2024-04 로 건너뛰어, 빈 달을 못 보고 지나치기 쉽다.
   */
  public static List<Row> byMonth(
      List<DividendView> dividends, ZoneId zone, LocalDate rangeStart, LocalDate rangeEnd) {
    List<Row> rows = aggregate(dividends, zone, false);
    if (rangeStart == null || rangeEnd == null || rangeStart.isAfter(rangeEnd)) return rows;
    Map<String, Row> byKey = new LinkedHashMap<>();
    for (Row row : rows) byKey.put(row.sortKey(), row);
    List<Row> filled = new ArrayList<>();
    for (YearMonth cursor = YearMonth.from(rangeEnd);
        !cursor.isBefore(YearMonth.from(rangeStart));
        cursor = cursor.minusMonths(1)) {
      String key = cursor.toString();
      Row row = byKey.remove(key);
      filled.add(
          row != null
              ? row
              : new Row(
                  key,
                  key,
                  0,
                  BigDecimal.ZERO,
                  BigDecimal.ZERO,
                  BigDecimal.ZERO,
                  false,
                  null,
                  null));
    }
    // 구간 밖에 남은 달(지급일이 구간 경계 밖인 자료)은 그대로 뒤에 붙여 빠뜨리지 않는다.
    filled.addAll(byKey.values());
    return List.copyOf(filled);
  }

  private static List<Row> aggregate(List<DividendView> dividends, ZoneId zone, boolean yearly) {
    if (dividends == null || dividends.isEmpty()) return List.of();
    // 주변 타임존에 기대는 결정은 StockZoneUtil 한 곳에만 둔다(AmbientZoneDependencyTest).
    ZoneId resolved = zone != null ? zone : StockZoneUtil.resolve(null);
    Map<String, Row> buckets = new LinkedHashMap<>();
    for (DividendView dividend : dividends) {
      if (dividend == null) continue;
      Instant when = dividend.payDate() != null ? dividend.payDate() : dividend.recordDate();
      if (when == null) continue;
      YearMonth yearMonth = YearMonth.from(when.atZone(resolved));
      String key = yearly ? String.valueOf(yearMonth.getYear()) : yearMonth.toString();
      Row previous = buckets.get(key);
      Row merged =
          new Row(
              key,
              key,
              (previous != null ? previous.count() : 0) + 1,
              add(previous != null ? previous.grossAmount() : null, dividend.grossAmount()),
              add(previous != null ? previous.tax() : null, dividend.tax()),
              add(previous != null ? previous.netAmount() : null, dividend.netAmount()),
              false,
              null,
              null);
      buckets.put(key, merged);
    }
    List<Row> rows = new ArrayList<>(buckets.values());
    rows.sort((left, right) -> right.sortKey().compareTo(left.sortKey()));
    return List.copyOf(rows);
  }

  private static BigDecimal add(BigDecimal base, BigDecimal value) {
    BigDecimal safeBase = base != null ? base : BigDecimal.ZERO;
    return value != null ? safeBase.add(value) : safeBase;
  }

  /** 표 아래 합계 줄에 쓰는 총합. 행들을 그대로 더하므로 각 행과 합이 어긋나지 않는다. */
  public static Row total(List<Row> rows, String label) {
    BigDecimal gross = BigDecimal.ZERO;
    BigDecimal tax = BigDecimal.ZERO;
    BigDecimal net = BigDecimal.ZERO;
    int count = 0;
    for (Row row : rows == null ? List.<Row>of() : rows) {
      count += row.count();
      gross = add(gross, row.grossAmount());
      tax = add(tax, row.tax());
      net = add(net, row.netAmount());
    }
    return new Row(label, "", count, gross, tax, net, false, null, null);
  }
}
