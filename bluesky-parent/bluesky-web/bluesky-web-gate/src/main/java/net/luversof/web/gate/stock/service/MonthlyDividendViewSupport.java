package net.luversof.web.gate.stock.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;

/**
 * 월배당 프로파일/시뮬레이터 행에 대한 정렬·필터 등 표현(view) 정렬 로직. 데이터 조회나 모델 조립이 아닌 순수 정렬/필터만 담당해 컨트롤러에서 분리하고 단위 테스트가
 * 가능하도록 한다.
 */
@Component
public class MonthlyDividendViewSupport {

  public static final String SORT_DISPLAY_ORDER = "display-order";
  public static final String SORT_SYMBOL = "symbol";

  /** 프로파일 정렬 키 검증(허용되지 않은 값은 기본 정렬). */
  public String resolveProfileSort(String sort) {
    if (!StringUtils.hasText(sort)) {
      return SORT_DISPLAY_ORDER;
    }

    return switch (sort) {
      case SORT_DISPLAY_ORDER,
          "symbol",
          "payout-window",
          "source-url",
          "last-verified-date",
          "active" ->
          sort;
      default -> SORT_DISPLAY_ORDER;
    };
  }

  /** 프로파일 정렬 방향(미지정 시 정렬 키별 기본값). */
  public String resolveProfileDirection(String sort, String direction) {
    if ("asc".equalsIgnoreCase(direction) || "desc".equalsIgnoreCase(direction)) {
      return direction.toLowerCase(Locale.ROOT);
    }

    return switch (sort) {
      case "last-verified-date", "active" -> "desc";
      default -> "asc";
    };
  }

  /** 프로파일 목록 정렬(동률이면 심볼 오름차순). */
  public List<MonthlyDividendProfileResponse> sortProfiles(
      List<MonthlyDividendProfileResponse> profiles, String sort, String direction) {
    return profiles.stream()
        .sorted(
            (left, right) -> {
              int result =
                  switch (sort) {
                    case "symbol" ->
                        compareProfileText(
                            left.stockItemSymbol(), right.stockItemSymbol(), direction);
                    case "payout-window" ->
                        compareProfileText(left.payoutWindow(), right.payoutWindow(), direction);
                    case "source-url" ->
                        compareProfileText(left.sourceUrl(), right.sourceUrl(), direction);
                    case "last-verified-date" ->
                        compareNullableProfileValue(
                            left.lastVerifiedDate(), right.lastVerifiedDate(), direction);
                    case "active" ->
                        compareProfileBoolean(left.active(), right.active(), direction);
                    case SORT_DISPLAY_ORDER ->
                        compareNullableProfileValue(
                            left.displayOrder(), right.displayOrder(), direction);
                    default ->
                        compareNullableProfileValue(
                            left.displayOrder(), right.displayOrder(), direction);
                  };

              if (result != 0) {
                return result;
              }

              return compareProfileText(left.stockItemSymbol(), right.stockItemSymbol(), "asc");
            })
        .toList();
  }

  /** 시뮬레이터 행 정렬 키 검증(허용되지 않은 값은 기본 정렬). */
  public String resolveRowSort(String sort) {
    if (!StringUtils.hasText(sort)) {
      return SORT_DISPLAY_ORDER;
    }

    return switch (sort) {
      case SORT_DISPLAY_ORDER,
          SORT_SYMBOL,
          "monthly-yield-on-cost",
          "annual-yield-on-cost",
          "monthly-yield",
          "annual-yield",
          "monthly-dividend",
          "taxable-base",
          "updated-date" ->
          sort;
      default -> SORT_DISPLAY_ORDER;
    };
  }

  /** 시뮬레이터 행 정렬 방향(미지정 시 정렬 키별 기본값). */
  public String resolveRowDirection(String sort, String direction) {
    if ("asc".equalsIgnoreCase(direction) || "desc".equalsIgnoreCase(direction)) {
      return direction.toLowerCase(Locale.ROOT);
    }

    return SORT_DISPLAY_ORDER.equals(sort) || "taxable-base".equals(sort) ? "asc" : "desc";
  }

  /** 프로파일 표시 순서 맵(정규화된 심볼 → displayOrder). */
  public Map<String, Integer> buildProfileDisplayOrderMap(
      List<MonthlyDividendProfileResponse> profiles) {
    Map<String, Integer> displayOrders = new LinkedHashMap<>();
    if (profiles == null || profiles.isEmpty()) {
      return displayOrders;
    }

    for (MonthlyDividendProfileResponse profile : profiles) {
      String normalizedSymbol = normalizeSymbol(profile.stockItemSymbol());
      if (!StringUtils.hasText(normalizedSymbol) || displayOrders.containsKey(normalizedSymbol)) {
        continue;
      }

      displayOrders.put(
          normalizedSymbol,
          profile.displayOrder() != null ? profile.displayOrder() : Integer.MAX_VALUE);
    }

    return displayOrders;
  }

  /** 키워드/최소 연수익률/플러스 수익 필터. */
  public List<MonthlyDividendSnapshotResponse> filterRows(
      List<MonthlyDividendSnapshotResponse> rows,
      String keyword,
      BigDecimal minAnnualYield,
      boolean positiveOnly) {
    return rows.stream()
        .filter(row -> matchesKeyword(row, keyword))
        .filter(
            row ->
                minAnnualYield == null
                    || safe(row.expectedAnnualYieldPct()).compareTo(minAnnualYield) >= 0)
        .filter(row -> !positiveOnly || safe(row.expectedCombinedReturnPct()).signum() >= 0)
        .toList();
  }

  /** 정렬 키/방향에 따른 시뮬레이터 행 정렬(동률이면 심볼). */
  public List<MonthlyDividendSnapshotResponse> sortRows(
      List<MonthlyDividendSnapshotResponse> rows,
      String sort,
      String direction,
      Map<String, Integer> profileDisplayOrders) {
    Comparator<MonthlyDividendSnapshotResponse> comparator =
        switch (sort) {
          case SORT_DISPLAY_ORDER ->
              Comparator.comparing(row -> resolveProfileDisplayOrder(row, profileDisplayOrders));
          case SORT_SYMBOL ->
              Comparator.comparing(
                  row -> safeString(row.stockItemSymbol()), String.CASE_INSENSITIVE_ORDER);
          case "monthly-yield-on-cost" ->
              Comparator.comparing(row -> safe(row.expectedMonthlyYieldOnCostPct()));
          case "annual-yield-on-cost" ->
              Comparator.comparing(row -> safe(row.expectedAnnualYieldOnCostPct()));
          case "monthly-yield" -> Comparator.comparing(row -> safe(row.expectedMonthlyYieldPct()));
          case "annual-yield" -> Comparator.comparing(row -> safe(row.expectedAnnualYieldPct()));
          case "monthly-dividend" ->
              Comparator.comparing(row -> safe(row.expectedMonthlyDividend()));
          case "taxable-base" -> Comparator.comparing(row -> safe(row.averageTaxableBaseRatio1y()));
          case "updated-date" ->
              Comparator.comparing(
                  row -> row.updatedDate() != null ? row.updatedDate() : Instant.EPOCH);
          default -> Comparator.comparing(row -> safe(row.expectedCombinedReturnPct()));
        };

    if (!"asc".equals(direction)) {
      comparator = comparator.reversed();
    }

    return rows.stream()
        .sorted(
            comparator.thenComparing(
                row -> safeString(row.stockItemSymbol()), String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private boolean matchesKeyword(MonthlyDividendSnapshotResponse row, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }

    String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
    return safeString(row.stockItemSymbol()).toLowerCase(Locale.ROOT).contains(normalizedKeyword)
        || safeString(row.stockItemName()).toLowerCase(Locale.ROOT).contains(normalizedKeyword);
  }

  private Integer resolveProfileDisplayOrder(
      MonthlyDividendSnapshotResponse row, Map<String, Integer> profileDisplayOrders) {
    if (row == null || profileDisplayOrders == null) {
      return Integer.MAX_VALUE;
    }

    String normalizedSymbol = normalizeSymbol(row.stockItemSymbol());
    if (!StringUtils.hasText(normalizedSymbol)) {
      return Integer.MAX_VALUE;
    }

    return profileDisplayOrders.getOrDefault(normalizedSymbol, Integer.MAX_VALUE);
  }

  private int compareProfileText(String left, String right, String direction) {
    String leftValue = trimToNull(left);
    String rightValue = trimToNull(right);
    if (leftValue == null && rightValue == null) {
      return 0;
    }
    if (leftValue == null) {
      return 1;
    }
    if (rightValue == null) {
      return -1;
    }

    return "desc".equals(direction)
        ? String.CASE_INSENSITIVE_ORDER.compare(rightValue, leftValue)
        : String.CASE_INSENSITIVE_ORDER.compare(leftValue, rightValue);
  }

  private int compareProfileBoolean(boolean left, boolean right, String direction) {
    return "desc".equals(direction) ? Boolean.compare(right, left) : Boolean.compare(left, right);
  }

  private <T extends Comparable<? super T>> int compareNullableProfileValue(
      T left, T right, String direction) {
    if (left == null && right == null) {
      return 0;
    }
    if (left == null) {
      return 1;
    }
    if (right == null) {
      return -1;
    }

    return "desc".equals(direction) ? right.compareTo(left) : left.compareTo(right);
  }

  private String normalizeSymbol(String symbol) {
    return StringUtils.hasText(symbol) ? symbol.trim().toUpperCase(Locale.ROOT) : null;
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static BigDecimal safe(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private static String safeString(String value) {
    return value != null ? value : "";
  }
}
