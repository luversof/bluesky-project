package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.view.RecordDateBuyByView;
import net.luversof.web.gate.stock.util.KrxRecordDateUtil;

/**
 * 월배당 ETF 화면의 '이번 분배금 받으려면' &mdash; 다음 기준일과 매수 마감일(2026-10-20 자율 개선).
 *
 * <p>기준일 규칙은 지급 이력 실측(2025-09 ~ 2026-10)과 맞춰 본다: 월중 = 15 일(아니면 앞 거래일), 월말 = 마지막 거래일. 마감은 기준일의 2 거래일
 * 전(T+2).
 */
class RecordDateBuyByTest {

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    source.setFallbackToSystemLocale(false);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  @AfterEach
  void resetLocale() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void 기준일_규칙이_지급_이력_실측과_같다() {
    // 실측 2026-10-20 지급 이력(MonthlyDividendPayout)의 기준일. 주말 · 공휴일 · 연말 휴장을 모두 거친다.
    Map<String, String> midMonth =
        Map.of(
            "2025-10", "2025-10-15",
            "2025-12", "2025-12-15",
            "2026-01", "2026-01-15",
            "2026-02", "2026-02-13", // 15 일 일요일
            "2026-03", "2026-03-13", // 15 일 일요일
            "2026-08", "2026-08-14", // 15 일 토요일 · 광복절
            "2026-10", "2026-10-15");
    Map<String, String> monthEnd =
        Map.of(
            "2025-09", "2025-09-30",
            "2025-12", "2025-12-30", // 31 일 연말 휴장
            "2026-01", "2026-01-30", // 31 일 토요일
            "2026-02", "2026-02-27", // 28 일 토요일
            "2026-05", "2026-05-29", // 31 일 일요일
            "2026-08", "2026-08-31",
            "2026-09", "2026-09-30");
    midMonth.forEach(
        (month, record) ->
            assertThat(KrxRecordDateUtil.recordDate(YearMonth.parse(month), "MID_MONTH"))
                .as("월중 " + month)
                .isEqualTo(LocalDate.parse(record)));
    monthEnd.forEach(
        (month, record) ->
            assertThat(KrxRecordDateUtil.recordDate(YearMonth.parse(month), "MONTH_END"))
                .as("월말 " + month)
                .isEqualTo(LocalDate.parse(record)));
    assertThat(KrxRecordDateUtil.recordDate(YearMonth.parse("2026-10"), "OTHER")).isNull();
  }

  @Test
  void 마감은_기준일의_2거래일_전이고_휴일을_건너뛴다() {
    // 10-30(금) 기준 -> 10-28(수). 10-15(목) 기준 -> 10-13(화).
    assertThat(KrxRecordDateUtil.minusTradingDays(LocalDate.parse("2026-10-30"), 2))
        .isEqualTo(LocalDate.parse("2026-10-28"));
    // 10-13(화) 기준 -> 10-12(월) 하루, 10-09(금, 한글날) · 주말 건너 10-08(목).
    assertThat(KrxRecordDateUtil.minusTradingDays(LocalDate.parse("2026-10-13"), 2))
        .isEqualTo(LocalDate.parse("2026-10-08"));
    assertThat(KrxRecordDateUtil.isTradingDay(LocalDate.parse("2026-05-01")))
        .as("근로자의 날 휴장")
        .isFalse();
    assertThat(KrxRecordDateUtil.isTradingDay(LocalDate.parse("2026-12-31"))).as("연말 휴장").isFalse();
    assertThat(KrxRecordDateUtil.isTradingDay(LocalDate.parse("2026-10-20"))).isTrue();
    // 연말 휴장 = 12 월 마지막 평일(2026-10-20 검토): 31 일이 주말이면 앞 금요일. 시세 행으로 확인한 날들.
    assertThat(KrxRecordDateUtil.isTradingDay(LocalDate.parse("2022-12-30"))).isFalse();
    assertThat(KrxRecordDateUtil.isTradingDay(LocalDate.parse("2022-12-29"))).isTrue();
    assertThat(KrxRecordDateUtil.isTradingDay(LocalDate.parse("2023-12-29"))).isFalse();
    assertThat(KrxRecordDateUtil.recordDate(YearMonth.parse("2028-12"), "MONTH_END"))
        .as("2028-12-31 일요일 -> 29 일(금) 휴장 -> 기준일 28 일")
        .isEqualTo(LocalDate.parse("2028-12-28"));
  }

  @Test
  void 이번_달_마감이_지나면_다음_달_것을_보인다() {
    // 2026-10-20: 월중 10 월(마감 10-13)은 지났다 -> 11 월 기준 11-13(금), 마감 11-11(수).
    RecordDateBuyByView mid =
        KrxRecordDateUtil.nextBuyBy(LocalDate.parse("2026-10-20"), "MID_MONTH");
    assertThat(mid.recordDate()).isEqualTo(LocalDate.parse("2026-11-13"));
    assertThat(mid.buyByDate()).isEqualTo(LocalDate.parse("2026-11-11"));
    assertThat(mid.daysLeft()).isEqualTo(22);
    // 월말은 10 월 것이 아직 남았다 - 기준 10-30(금), 마감 10-28(수).
    RecordDateBuyByView end =
        KrxRecordDateUtil.nextBuyBy(LocalDate.parse("2026-10-20"), "MONTH_END");
    assertThat(end.recordDate()).isEqualTo(LocalDate.parse("2026-10-30"));
    assertThat(end.buyByDate()).isEqualTo(LocalDate.parse("2026-10-28"));
    assertThat(end.daysLeft()).isEqualTo(8);
    assertThat(end.holidaysCovered()).isTrue();

    // 마감 당일은 이번 달 것(0 일), 다음 날은 다음 달 것.
    assertThat(KrxRecordDateUtil.nextBuyBy(LocalDate.parse("2026-10-28"), "MONTH_END").daysLeft())
        .isZero();
    assertThat(KrxRecordDateUtil.nextBuyBy(LocalDate.parse("2026-10-29"), "MONTH_END").recordDate())
        .isEqualTo(LocalDate.parse("2026-11-30"));
    // 공휴일 표가 없는 해로 넘어가면 밝힌다(2027 은 2026-10-20 에 넣었다 - 2027-12 마감이 지나 2028 로).
    assertThat(
            KrxRecordDateUtil.nextBuyBy(LocalDate.parse("2027-12-29"), "MONTH_END")
                .holidaysCovered())
        .isFalse();
  }

  private static String render(List<RecordDateBuyByView> rows) {
    Map<String, Object> model = new HashMap<>();
    model.put("rows", rows);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/fragments/recordDateBuyBy.jte", model, output);
    return output.toString();
  }

  @Test
  void 지급_시기마다_기준일과_마감을_그린다() {
    LocaleContextHolder.setLocale(Locale.KOREAN);
    LocalDate today = LocalDate.parse("2026-10-20");
    String html =
        render(
            List.of(
                KrxRecordDateUtil.nextBuyBy(today, "MID_MONTH"),
                KrxRecordDateUtil.nextBuyBy(today, "MONTH_END")));
    assertThat(html)
        .contains("data-record-buyby-window=\"MID_MONTH\"")
        .contains("data-record-buyby-window=\"MONTH_END\"")
        .contains("11/13(금)")
        .contains("10/28(수)")
        .contains("data-buyby-days-left=\"8\"")
        .doesNotContain(MessageUtil.getMessage("stock.monthly.etf.buyby.uncovered"));
    assertThat(render(List.of())).doesNotContain("data-record-buyby");
  }

  /** 대시보드 다가올 배당 카드 끝의 한 줄(2026-10-20) - 시기별 마감만, 긴 안내는 월배당 ETF 화면에. */
  @Test
  void 대시보드_다가올_배당_카드에_마감_한_줄을_붙인다() {
    LocaleContextHolder.setLocale(Locale.KOREAN);
    LocalDate today = LocalDate.parse("2026-10-20");
    Map<String, Object> model = new HashMap<>();
    model.put("hasSnapshots", true);
    model.put(
        "upcomingBuyBy",
        List.of(
            KrxRecordDateUtil.nextBuyBy(today, "MID_MONTH"),
            KrxRecordDateUtil.nextBuyBy(today, "MONTH_END")));
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/htmx/fragments/upcomingDividends.jte", model, output);
    String html = output.toString();

    assertThat(html)
        .contains("data-record-buyby-compact")
        .contains("11/11(수)")
        .contains("10/28(수)")
        .doesNotContain("data-record-buyby-note");

    model.put("hasSnapshots", false);
    StringOutput empty = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/htmx/fragments/upcomingDividends.jte", model, empty);
    assertThat(empty.toString()).as("월배당 기준 데이터가 없으면 빈 상태만").doesNotContain("data-record-buyby");
  }

  /** 시뮬레이터 월배당 탭에도 같은 줄(2026-10-20) - 적립 추천 위, 결과 구역 밖(구역 교체 때 같이 바뀔 이유가 없다). */
  @Test
  void 시뮬레이터_월배당_탭에도_결과_구역_밖에_둔다() throws java.io.IOException {
    String page =
        java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/jte/stock/fragments/monthlyDividendSimulator.jte"),
            java.nio.charset.StandardCharsets.UTF_8);
    int buyBy =
        page.indexOf("@template.stock.fragments.recordDateBuyBy(rows = monthlyDividendBuyBy)");
    assertThat(buyBy).isPositive().isLessThan(page.indexOf("id=\"monthlyDividendResults\""));
  }

  /** 시뮬레이터 · 월배당 ETF · 대시보드 카드가 같은 목록을 쓴다(2026-10-22 - 세 컨트롤러가 같은 스트림을 따로 적고 있었다). */
  @Test
  void 두_지급_창의_매수_마감을_한_번에_돌려준다() {
    LocalDate today = LocalDate.parse("2026-10-20");
    assertThat(KrxRecordDateUtil.nextBuyByAllWindows(today))
        .containsExactly(
            KrxRecordDateUtil.nextBuyBy(today, "MID_MONTH"),
            KrxRecordDateUtil.nextBuyBy(today, "MONTH_END"));
  }

  /**
   * 마감일 당일 18:00(시간외 단일가 끝) 이 지나면 그 기준일은 놓친 것 - 다음 달 것을 보인다(2026-10-28 12차 자율: 18:34 에 "오늘 마감").
   */
  @Test
  void 마감일_18시가_지나면_다음_달_기준일을_보인다() {
    RecordDateBuyByView before =
        KrxRecordDateUtil.nextBuyByAllWindows(java.time.LocalDateTime.parse("2026-10-28T17:59"))
            .stream()
            .filter(view -> "MONTH_END".equals(view.payoutWindow()))
            .findFirst()
            .orElseThrow();
    RecordDateBuyByView after =
        KrxRecordDateUtil.nextBuyByAllWindows(java.time.LocalDateTime.parse("2026-10-28T18:00"))
            .stream()
            .filter(view -> "MONTH_END".equals(view.payoutWindow()))
            .findFirst()
            .orElseThrow();

    assertThat(before.buyByDate()).isEqualTo(LocalDate.parse("2026-10-28"));
    assertThat(before.daysLeft()).isZero();
    assertThat(after.recordDate()).isEqualTo(LocalDate.parse("2026-11-30"));
    assertThat(after.buyByDate()).isAfter(LocalDate.parse("2026-10-28"));
  }
}
