package net.luversof.web.gate.bookkeeping.support;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

import net.luversof.web.gate.bookkeeping.constant.CategoryKind;
import net.luversof.web.gate.bookkeeping.constant.EntryType;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.AssetBalance;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient.Entry;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.DailyTotal;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.MonthRange;
import net.luversof.web.gate.stock.support.StockViewSupport;

/** 가계부 화면 표시 도우미: 금액 · 월 · 달력 · 통화별 합계. 모든 문구는 메시지 키(bookkeepingUiMessage). */
public final class BookkeepingView {

  /** 가계부 · 자산을 만들 때 고르는 통화. 그 밖의 ISO 코드는 API 가 받지만 화면에는 이것만 둔다. */
  public static final List<String> CURRENCIES = List.of("KRW", "USD", "JPY", "EUR", "CNY", "GBP");

  private BookkeepingView() {}

  public static String msg(String code, Object... args) {
    return StockViewSupport.msg(code, args);
  }

  /** 통화 소수 자릿수에 맞춘 금액(천 단위 구분). */
  public static String amount(BigDecimal value, String currency) {
    var format = NumberFormat.getNumberInstance(LocaleContextHolder.getLocale());
    int digits = fractionDigits(currency);
    format.setMinimumFractionDigits(digits);
    format.setMaximumFractionDigits(digits);
    return format.format(value == null ? BigDecimal.ZERO : value);
  }

  /** 입력 칸에 넣을 금액(구분 기호 없음, 뒤 0 제거). */
  public static String plainAmount(BigDecimal value) {
    return value == null ? "" : value.stripTrailingZeros().toPlainString();
  }

  static int fractionDigits(String currency) {
    try {
      int digits = Currency.getInstance(currency).getDefaultFractionDigits();
      return digits < 0 ? 2 : Math.min(digits, 4);
    } catch (IllegalArgumentException | NullPointerException e) {
      return 2;
    }
  }

  /** "2026-11" → YearMonth. 형식이 틀리면 null(오늘이 속한 월). */
  public static YearMonth parseMonth(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return YearMonth.parse(value.strip());
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  /** "yyyy-MM-dd" → LocalDate. 형식이 틀리면 null. */
  public static LocalDate parseDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.strip());
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  /** 월 이름(메시지 키 bookkeeping.month.pattern 의 형식, 예: 2026년 11월). */
  public static String monthLabel(YearMonth month) {
    return month.format(
        DateTimeFormatter.ofPattern(
            msg("bookkeeping.month.pattern"), LocaleContextHolder.getLocale()));
  }

  /** 기간 표시(예: 10.25 ~ 11.24). */
  public static String rangeLabel(MonthRange range) {
    var f = DateTimeFormatter.ofPattern("M.d");
    return range.from().format(f) + " ~ " + range.to().format(f);
  }

  public static String dayLabel(LocalDate date) {
    return date.format(
        DateTimeFormatter.ofPattern(
            msg("bookkeeping.day.pattern"), LocaleContextHolder.getLocale()));
  }

  /** 달력 한 칸. date 가 null 이면 기간 밖 빈칸. */
  public record CalendarDay(LocalDate date, boolean today, List<DailyTotal> totals) {}

  /** 일요일부터 시작하는 주 단위 칸. 기간(월 시작일 기준) 밖은 빈칸. */
  public static List<List<CalendarDay>> calendar(MonthRange range, List<DailyTotal> daily) {
    Map<LocalDate, List<DailyTotal>> byDate =
        daily.stream().collect(Collectors.groupingBy(DailyTotal::date));
    var today = LocalDate.now();
    var weeks = new ArrayList<List<CalendarDay>>();
    var week = new ArrayList<CalendarDay>();
    int lead = range.from().getDayOfWeek().getValue() % 7; // 일요일 = 0
    for (int i = 0; i < lead; i++) {
      week.add(new CalendarDay(null, false, List.of()));
    }
    for (var d = range.from(); !d.isAfter(range.to()); d = d.plusDays(1)) {
      week.add(new CalendarDay(d, d.equals(today), byDate.getOrDefault(d, List.of())));
      if (d.getDayOfWeek() == DayOfWeek.SATURDAY) {
        weeks.add(week);
        week = new ArrayList<>();
      }
    }
    if (!week.isEmpty()) {
      while (week.size() < 7) {
        week.add(new CalendarDay(null, false, List.of()));
      }
      weeks.add(week);
    }
    return weeks;
  }

  /** 통화별 수입 · 지출 합. */
  public record CurrencyTotal(String currency, BigDecimal income, BigDecimal expense) {
    public BigDecimal net() {
      return income.subtract(expense);
    }
  }

  /** 일별 합을 통화별로 더한다(통화 이름 순). */
  public static List<CurrencyTotal> totals(List<DailyTotal> daily) {
    var sums = new java.util.TreeMap<String, BigDecimal[]>();
    for (var d : daily) {
      var s =
          sums.computeIfAbsent(
              d.currency(), k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
      s[0] = s[0].add(d.income());
      s[1] = s[1].add(d.expense());
    }
    var result = new ArrayList<CurrencyTotal>();
    sums.forEach((currency, s) -> result.add(new CurrencyTotal(currency, s[0], s[1])));
    return result;
  }

  /** 자산 id → 자산(숨긴 자산 포함, 목록 순서 유지). */
  public static Map<UUID, AssetBalance> assetsById(List<AssetBalance> assets) {
    var map = new LinkedHashMap<UUID, AssetBalance>();
    assets.forEach(a -> map.put(a.asset().id(), a));
    return map;
  }

  /** 분류 id → 분류. */
  public static Map<UUID, Category> categoriesById(List<Category> categories) {
    var map = new LinkedHashMap<UUID, Category>();
    categories.forEach(c -> map.put(c.id(), c));
    return map;
  }

  /** "대분류 > 소분류" 또는 "대분류". 없으면 빈 문자열. */
  public static String categoryPath(Map<UUID, Category> byId, UUID categoryId) {
    var c = categoryId == null ? null : byId.get(categoryId);
    if (c == null) {
      return "";
    }
    var parent = c.parentId() == null ? null : byId.get(c.parentId());
    return parent == null ? c.name() : parent.name() + " > " + c.name();
  }

  /** 내역 한 줄 제목: 이체는 "보낸 자산 → 받은 자산", 나머지는 분류. */
  public static String entryTitle(
      Entry entry, Map<UUID, AssetBalance> assets, Map<UUID, Category> categories) {
    if (entry.type() == EntryType.TRANSFER) {
      return assetName(assets, entry.assetId()) + " → " + assetName(assets, entry.toAssetId());
    }
    return categoryPath(categories, entry.categoryId());
  }

  public static String assetName(Map<UUID, AssetBalance> assets, UUID assetId) {
    var a = assetId == null ? null : assets.get(assetId);
    return a == null ? "" : a.asset().name();
  }

  public static String assetCurrency(Map<UUID, AssetBalance> assets, UUID assetId) {
    var a = assetId == null ? null : assets.get(assetId);
    return a == null ? "" : a.asset().currency();
  }

  /** 선택지 한 줄. child 면 소분류(들여 보여 준다). */
  public record Option(UUID id, String label, boolean child) {}

  /** 이 종류의 분류: 대분류 다음에 그 소분류. 숨긴 분류는 빼되 keep(지금 고른 값)은 남긴다. */
  public static List<Option> categoryOptions(List<Category> all, CategoryKind kind, UUID keep) {
    var result = new ArrayList<Option>();
    for (var parent : all) {
      if (parent.parentId() != null
          || parent.kind() != kind
          || !visible(parent.hidden(), parent.id(), keep)) {
        continue;
      }
      result.add(new Option(parent.id(), parent.name(), false));
      for (var child : all) {
        if (parent.id().equals(child.parentId()) && visible(child.hidden(), child.id(), keep)) {
          result.add(new Option(child.id(), child.name(), true));
        }
      }
    }
    return result;
  }

  /** 자산 선택지(이름 · 통화). 숨긴 자산은 빼되 keep 은 남긴다. */
  public static List<Option> assetOptions(List<AssetBalance> all, UUID keep) {
    return all.stream()
        .map(AssetBalance::asset)
        .filter(a -> visible(a.hidden(), a.id(), keep))
        .map(a -> new Option(a.id(), a.name() + " (" + a.currency() + ")", false))
        .toList();
  }

  private static boolean visible(boolean hidden, UUID id, UUID keep) {
    return !hidden || id.equals(keep);
  }

  /**
   * 월 화면 주소. pairs 는 이름 · 값 순서이고 값이 null 이거나 빈 것은 뺀다.
   *
   * @param month null 이면 넣지 않는다(오늘이 속한 월)
   */
  public static String href(UUID bookId, YearMonth month, String... pairs) {
    var builder = UriComponentsBuilder.fromPath("/bookkeeping/" + bookId);
    if (month != null) {
      builder.queryParam("month", month.toString());
    }
    for (int i = 0; i + 1 < pairs.length; i += 2) {
      if (pairs[i + 1] != null && !pairs[i + 1].isEmpty()) {
        builder.queryParam(pairs[i], pairs[i + 1]);
      }
    }
    return builder.encode().toUriString();
  }

  public static String str(Object value) {
    return value == null ? null : value.toString();
  }
}
