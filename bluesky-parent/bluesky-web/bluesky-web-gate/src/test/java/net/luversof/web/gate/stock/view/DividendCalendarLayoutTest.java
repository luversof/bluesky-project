package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.view.DividendCalendarView;
import net.luversof.web.gate.stock.util.DividendCalendarGridUtil;
import net.luversof.web.gate.stock.util.StockFormatUtil;

/**
 * 달력은 "언제" 만, 금액은 목록이 말한다.
 *
 * <p>42 칸 격자에서 실제로 차는 칸은 <b>평균 2 개</b>다 &mdash; 실측 2026-09-15(최근 24 개월): 지급일이 1~3 일뿐이고 늘 월초(2~6 일)
 * · 월중(17~20 일) 두 덩어리라 격자의 <b>95%</b> 가 구조적으로 빈다. 그런데 그 빈 면적 때문에 정작 종목명과 금액은 한 칸 안에 11px · 10px 로
 * 눌려 있었다(사용자 지적).
 *
 * <p>그래서 칸은 "며칠에 얼마" 하나만 크게 말하고, 종목별 세전 · 실수령 · 과표는 달력 아래 지급일 목록이 제 크기로 말한다. 달력을 버리지는 않는다 &mdash;
 * 요일과 공휴일 맥락이 실제로 결함을 잡아 줬다(2 일=일요일, 17 일=대체공휴일에 배당이 찍혀 있던 일).
 */
class DividendCalendarLayoutTest {

  private static final String CALENDAR = "stock/fragments/dividendCalendarMonth.jte";

  private static final String FRAGMENT = "src/main/jte/stock/fragments/dividendCalendarMonth.jte";

  private static final String CONTROLLER =
      "src/main/java/net/luversof/web/gate/stock/controller/StockDividendViewController.java";

  private static final YearMonth AUGUST = YearMonth.of(2026, 8);

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
    LocaleContextHolder.setLocale(Locale.KOREA);
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
    LocaleContextHolder.resetLocaleContext();
  }

  private static DividendCalendarView.Entry entry(String name, int day, long gross, long net) {
    return new DividendCalendarView.Entry(
        name,
        name,
        BigDecimal.valueOf(net),
        BigDecimal.valueOf(gross),
        BigDecimal.ZERO,
        null,
        day,
        day,
        day,
        0,
        true,
        true);
  }

  private static String render(List<DividendCalendarView.Entry> entries) {
    DividendCalendarView calendar =
        new DividendCalendarView(
            AUGUST,
            DividendCalendarGridUtil.build(
                AUGUST, null, DividendCalendarGridUtil.groupByDay(entries, AUGUST)),
            List.of());
    Map<String, Object> model = new HashMap<>();
    model.put("calendar", calendar);
    model.put("prevHref", "/stock/dividend?tab=calendar&month=2026-07");
    model.put("nextHref", "/stock/dividend?tab=calendar&month=2026-09");
    model.put("thisHref", "/stock/dividend?tab=calendar&month=2026-09");
    model.put("isThisMonth", false);
    model.put("avgLabel", "AVG");
    model.put("latestLabel", "LATEST");
    model.put("actualGrossLabel", "GROSS");
    model.put("actualNetLabel", "NET");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(CALENDAR, model, output);
    return output.toString();
  }

  private static String cell(String html, String date) {
    int at = html.indexOf("data-calendar-day=" + String.valueOf((char) 34) + date);
    assertThat(at).as(date + " 칸을 찾지 못했다 - 검사가 무력해진다").isGreaterThan(0);
    return html.substring(at, html.indexOf("</td>", at));
  }

  private static String dayList(String html) {
    int at = html.indexOf("data-calendar-daylist");
    assertThat(at).as("지급일 목록이 없다 - 검사가 무력해진다").isGreaterThan(0);
    return html.substring(at);
  }

  private static String source(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  /** 칸에는 그 날 합계 하나만. 종목명과 종목별 금액은 칸에 없다. */
  @Test
  void 칸은_며칠에_얼마만_말한다() {
    String html =
        render(
            List.of(
                entry("RISE 200위클리커버드콜", 4, 1341850L, 1337470L),
                entry("TIGER 리츠부동산인프라", 4, 453684L, 453684L)));
    String august4 = cell(html, "2026-08-04");

    assertThat(august4).contains("data-calendar-day-total");
    assertThat(august4)
        .as("칸에 종목명을 넣으면 11px 로 눌린다 - 그래서 옮긴 것이다")
        .doesNotContain("RISE 200위클리커버드콜")
        .doesNotContain("TIGER 리츠부동산인프라");
    assertThat(august4)
        .as("종목별 금액도 칸에 남으면 안 된다")
        .doesNotContain("1,341,850")
        .doesNotContain("453,684");
    assertThat(august4)
        .as("칸이 말하는 건 그 날 합계다")
        .contains(StockFormatUtil.compactKrw(1341850L + 453684L));
  }

  /** 압축 표기는 눈으로만 읽는다 - 낭독기에는 정확한 금액을 준다. */
  @Test
  void 압축_금액_옆에_정확한_금액을_둔다() {
    String august4 = cell(render(List.of(entry("가", 4, 1341850L, 1337470L))), "2026-08-04");

    assertThat(august4).contains("aria-hidden=" + String.valueOf((char) 34) + "true");
    assertThat(august4).contains("sr-only");
    assertThat(august4).as("낭독기가 '134만' 만 듣게 두면 안 된다").contains("1,341,850");
    // 낭독기용 금액도 amount-value 안에 있어야 '금액 숨김' 이 닿는다 -
    // 클래스만 빠져도 율은 그대로라 눈에 안 띄는 결함이다.
    String afterSrOnly = august4.substring(august4.indexOf("sr-only"));
    assertThat(afterSrOnly.indexOf("amount-value"))
        .as("낭독기용 금액이 amount-value 밖에 있으면 '금액 숨김' 이 그것만 못 가린다")
        .isBetween(0, afterSrOnly.indexOf("1,341,850"));
  }

  /** 종목명과 세 금액은 달력 아래 목록에서 제 크기로 읽힌다. */
  @Test
  void 목록이_종목과_금액을_말한다() {
    String html =
        render(
            List.of(
                entry("RISE 200위클리커버드콜", 4, 1341850L, 1337470L),
                entry("KODEX 한국부동산리츠인프라", 19, 523073L, 493863L)));
    String list = dayList(html);

    assertThat(list).contains("RISE 200위클리커버드콜").contains("KODEX 한국부동산리츠인프라");
    assertThat(list).contains("1,341,850").contains("1,337,470");
    assertThat(list).contains("523,073").contains("493,863");
    assertThat(list)
        .as("지급일마다 한 블록 - 실측상 한 달에 한두 날뿐이다")
        .contains("data-calendar-daylist-day=" + String.valueOf((char) 34) + "2026-08-04")
        .contains("data-calendar-daylist-day=" + String.valueOf((char) 34) + "2026-08-19");
    assertThat(list).contains("GROSS").contains("NET");
  }

  /**
   * 종목 코드가 없어도 식별자는 남는다.
   *
   * <p>JTE 는 값이 {@code null} 인 속성을 <b>통째로 지운다</b>. 원장으로만 놓은 종목(월배당 기준 데이터가 없어 심볼이 없다)은 그래서 {@code
   * data-calendar-entry} 가 사라졌다 &mdash; 실측 2026-09-15: 2024-11 의 삼성전자 · SK텔레콤 두 종목이 화면에는 멀쩡히 있는데 QA
   * 탐침은 0 종목으로 읽었다. 심볼이 없으면 종목명을 쓴다.
   */
  @Test
  void 심볼이_없어도_식별자는_남는다() {
    DividendCalendarView.Entry noSymbol =
        new DividendCalendarView.Entry(
            null,
            "삼성전자",
            BigDecimal.valueOf(1858716L),
            BigDecimal.valueOf(2197046L),
            BigDecimal.ZERO,
            null,
            4,
            4,
            4,
            0,
            true,
            true);
    String list = dayList(render(List.of(noSymbol)));

    assertThat(list)
        .as("속성이 사라지면 화면엔 보여도 자동 검사는 그 종목을 못 본다")
        .contains("data-calendar-entry=" + String.valueOf((char) 34) + "삼성전자");
  }

  /**
   * 달과 무관한 숫자에는 그렇다고 적는다.
   *
   * <p>월중/월말 소계는 라벨 없는 숫자 셋이었고, 실은 <b>지금 보유 수량</b> 기준 예상이라 달을 넘겨도 값이 같다 &mdash; 실측 2026-09-15: 어느
   * 달을 봐도 3,956,660 원. 그런데 달력은 그 달 실제를 그리므로(2026-08 4,982,406 원) 한 화면에 축이 다른 두 숫자가 나란히 놀였다.
   */
  @Test
  void 월중_월말_소계가_기준을_밝힌다() throws IOException {
    String page = source("src/main/jte/stock/dividend.jte");

    assertThat(page)
        .as("라벨 없는 금액은 보는 달의 실적으로 읽힌다")
        .contains("data-calendar-subtotal-basis")
        .contains("stock.dividend.calendar.window.basis");
  }

  /**
   * 화면 맨 위 설명이 아래 달력과 반대로 말하지 않게.
   *
   * <p>탭 설명은 "보유한 월배당 종목의 <b>예상</b> 지급 일정입니다" 인데, 지나간 달은 달력이 그 달 <b>실제 지급</b>을 그린다. 그 달 안내는 달력 상자가
   * 이미 하므로 위 설명은 접는다.
   */
  @Test
  void 실제를_그리는_달에는_예상_설명을_접는다() throws IOException {
    String page = source("src/main/jte/stock/dividend.jte");

    assertThat(page)
        .as("위에서는 예상이라 하고 아래에서는 실제라 하면 한 화면이 서로 반대로 말한다")
        .contains(
            "boolean calendarShowsActual = dividendCalendar != null && dividendCalendar.hasActualAmount();")
        .contains("@if(!calendarShowsActual)");
  }

  private static DividendCalendarView.Entry projected(
      String name, int day, long latest, long average) {
    return new DividendCalendarView.Entry(
        name,
        name,
        BigDecimal.valueOf(average),
        BigDecimal.valueOf(latest),
        BigDecimal.ZERO,
        null,
        day,
        day,
        day,
        12,
        false,
        false);
  }

  /** 센티널이 아니라 진짜 문구로 그린다 - 낭독기가 듣는 것과 같게 만들어야 검사가 의미 있다. */
  private static String renderWithRealLabels(List<DividendCalendarView.Entry> entries) {
    DividendCalendarView calendar =
        new DividendCalendarView(
            AUGUST,
            DividendCalendarGridUtil.build(
                AUGUST, null, DividendCalendarGridUtil.groupByDay(entries, AUGUST)),
            List.of());
    Map<String, Object> model = new HashMap<>();
    model.put("calendar", calendar);
    model.put("prevHref", "#");
    model.put("nextHref", "#");
    model.put("thisHref", "#");
    model.put("isThisMonth", false);
    model.put("avgLabel", MessageUtil.getMessage("stock.dividend.calendar.basis.average"));
    model.put("latestLabel", MessageUtil.getMessage("stock.dividend.calendar.basis.latest"));
    model.put(
        "actualGrossLabel", MessageUtil.getMessage("stock.dividend.calendar.basis.actual.gross"));
    model.put("actualNetLabel", MessageUtil.getMessage("stock.dividend.calendar.basis.actual.net"));
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(CALENDAR, model, output);
    return output.toString();
  }

  private static int countOf(String haystack, String needle) {
    int found = 0;
    int at = 0;
    while ((at = haystack.indexOf(needle, at)) >= 0) {
      found++;
      at += needle.length();
    }
    return found;
  }

  private static java.util.List<String> entryRows(String html) {
    java.util.List<String> rows = new java.util.ArrayList<>();
    int at = 0;
    while ((at = html.indexOf("data-calendar-entry=", at)) >= 0) {
      int end = html.indexOf("</li>", at);
      rows.add(html.substring(at, end));
      at = end;
    }
    return rows;
  }

  /**
   * 값마다 이름이 붙어 있다.
   *
   * <p>한 줄에 금액이 셋(세전 · 실수령 · 과표)이라 하나라도 이름을 잃으면 숫자만 나란히 읽힌다 &mdash; 이 저장소가 이미 두 번 밟은 자리다({@code
   * two-numbers-one-header-name} · {@code trade-card-count-read-as-sell}). 실제 달과 추정 달은 이름이 서로 다르므로
   * 둘 다 본다.
   */
  @Test
  void 종목_줄의_값마다_이름이_있다() {
    String html =
        renderWithRealLabels(
            List.of(entry("실제종목", 4, 1341850L, 1337470L), projected("추정종목", 19, 500L, 400L)));

    java.util.List<String> rows = entryRows(html);
    assertThat(rows).as("종목 줄을 못 읽으면 검사가 헛돈다").hasSize(2);

    java.util.List<String> labels =
        List.of(
            MessageUtil.getMessage("stock.dividend.calendar.basis.actual.gross"),
            MessageUtil.getMessage("stock.dividend.calendar.basis.actual.net"),
            MessageUtil.getMessage("stock.dividend.calendar.basis.average"),
            MessageUtil.getMessage("stock.dividend.calendar.basis.latest"),
            MessageUtil.getMessage("stock.dividend.calendar.taxable.short"));

    for (String row : rows) {
      int amounts = countOf(row, "amount-value");
      int named = 0;
      for (String label : labels) {
        named += countOf(row, label);
      }
      assertThat(amounts).as("종목 줄에서 금액을 못 읽었다").isEqualTo(3);
      assertThat(named).as("이름 없는 숫자가 생기면 셋이 그냥 나란히 읽힌다: " + row).isGreaterThanOrEqualTo(amounts);
    }
  }

  /** 배당이 없으면 목록도 없다. */
  @Test
  void 배당이_없으면_목록도_없다() {
    assertThat(render(List.of())).doesNotContain("data-calendar-daylist");
  }

  /** 칸이 다시 커지면 이 재배치가 헛일이 된다. */
  @Test
  void 칸_높이는_줄어든_채로_둔다() throws IOException {
    String fragment = source(FRAGMENT);

    assertThat(fragment)
        .as("칸이 h-24 로 돌아가면 빈 면적이 다시 화면의 대부분을 차지한다")
        .doesNotContain("<td class=" + String.valueOf((char) 34) + "h-24 ");
    assertThat(fragment).contains("<td class=" + String.valueOf((char) 34) + "h-12 ");
  }

  /**
   * 지나간 달은 원장이 지급일을 아니 <b>모든 종목</b>을 제 날짜에 놓는다.
   *
   * <p>실측 2026-09-15: 2026-08 의 삼성전자 1,886,082 원은 원장에 8 월 28 일이라고 적혀 있는데도 월배당 기준 데이터가 없다는 이유로 '달력
   * 밖' 상자로 빠져, 그 달 배당의 37.9% 가 달력에서 안 보였다(사용자 결정 2026-09-15).
   */
  @Test
  void 컨트롤러가_지나간_달엔_모든_종목을_놓는다() throws IOException {
    String controller = source(CONTROLLER);

    assertThat(controller)
        .as("지나간 달에도 상자로 빼면 달력이 그 달 배당의 일부만 보여준다")
        .contains("if (actualOnly) {")
        .contains("dated.add( new DividendCalendarView.Entry(");
    assertThat(controller)
        .as("앞으로 올 달은 지급일을 알 길이 없으니 그때는 상자로 남는다")
        .contains("missing.add(new DividendCalendarView.Missing(name, actual.gross()));");
  }
}
