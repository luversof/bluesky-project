package net.luversof.web.gate.bookkeeping.support;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.i18n.LocaleContextHolder;

import net.luversof.web.gate.bookkeeping.constant.EntryType;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.AssetBalance;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category;
import net.luversof.web.gate.bookkeeping.httpexchange.RecurringClient.Recurring;

/** 반복 내역 표시: 주기 문구 · 제목 · 모달의 주기 선택지. */
public final class RecurringView {

  /** 모달에서 고르는 주기(날짜에서 일 · 요일 · 월을 딴다). */
  public static final List<String> CYCLES = List.of("MONTHLY", "WEEKLY", "YEARLY");

  private RecurringView() {}

  /** "매월 31일" · "매주 월요일" · "매년 2월 29일"(그 달에 없으면 말일). */
  public static String cycleLabel(Recurring r) {
    return switch (r.cycle()) {
      case "WEEKLY" ->
          BookkeepingView.msg("bookkeeping.recurring.cycle.WEEKLY", weekday(r.dayOfWeek()));
      case "YEARLY" ->
          BookkeepingView.msg(
              "bookkeeping.recurring.cycle.YEARLY", r.monthOfYear(), r.dayOfMonth());
      default -> BookkeepingView.msg("bookkeeping.recurring.cycle.MONTHLY", r.dayOfMonth());
    };
  }

  /** 모달 선택지 문구: 그 날짜 기준. */
  public static String cycleOption(String cycle, LocalDate date) {
    return switch (cycle) {
      case "WEEKLY" ->
          BookkeepingView.msg(
              "bookkeeping.recurring.cycle.WEEKLY", weekday(date.getDayOfWeek().getValue()));
      case "YEARLY" ->
          BookkeepingView.msg(
              "bookkeeping.recurring.cycle.YEARLY", date.getMonthValue(), date.getDayOfMonth());
      default -> BookkeepingView.msg("bookkeeping.recurring.cycle.MONTHLY", date.getDayOfMonth());
    };
  }

  /** 이체는 "보낸 자산 → 받은 자산", 나머지는 분류. 내용이 있으면 뒤에 붙인다. */
  public static String title(
      Recurring r, Map<UUID, AssetBalance> assets, Map<UUID, Category> categories) {
    var head =
        r.type() == EntryType.TRANSFER
            ? BookkeepingView.assetName(assets, r.assetId())
                + " → "
                + BookkeepingView.assetName(assets, r.toAssetId())
            : BookkeepingView.categoryPath(categories, r.categoryId());
    return r.content() == null ? head : head + " · " + r.content();
  }

  private static String weekday(int dayOfWeek) {
    return DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, LocaleContextHolder.getLocale());
  }
}
