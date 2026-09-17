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

/**
 * 지나간 달은 금액도 <b>추정하지 않는다</b>.
 *
 * <p>날짜를 실제 지급일로 고친 뒤에도 금액은 여전히 "지금 보유 수량 x 최근 주당 배당" 이었다. 실제 날짜에 지어낸 금액이 붙으니 오히려 더 그럴듯해졌다 &mdash;
 * 실측 2026-09-15: 2026-08 달력 합계 3,956,660 원 대 그 여덟 종목이 실제로 받은 세전 3,096,324 원으로 <b>+27.8%</b>, 종목별로는
 * 최대 +132.5%(RISE 코리아밸류업) 였다. 예상치라 달을 넘겨도 값이 <b>똑같았다</b>.
 *
 * <p>게다가 그 달 배당(세전 4,982,406 원)의 <b>37.9%</b> 인 삼성전자(1,886,082 원)는 분기배당이라 월배당 기준 데이터가 없고, 달력에 아예
 * 나오지 않으면서 그 사실을 아무도 말하지 않았다. 2026-05 는 같은 종목이 42.9% 였다.
 *
 * <p>그래서 지나간 달은 원장이 답한다 &mdash; 금액은 실제 수령액으로 바꾸고, 달력에 놓지 못한 종목은 금액과 함께 밝힌다(사용자 결정 2026-09-15). 자산
 * 현황이 "이 표에 없는 종목" 을 적는 것과 같은 처방이다.
 */
class DividendCalendarActualAmountTest {

  private static final String CALENDAR = "stock/fragments/dividendCalendarMonth.jte";

  private static final YearMonth AUGUST = YearMonth.of(2026, 8);

  private static final String QUOTE = String.valueOf((char) 34);

  private static final String CONTROLLER =
      "src/main/java/net/luversof/web/gate/stock/controller/StockDividendViewController.java";

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

  private static DividendCalendarView.Entry entry(
      String name, int day, long gross, long net, boolean actualAmount) {
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
        12,
        true,
        actualAmount);
  }

  private static DividendCalendarView calendar(
      List<DividendCalendarView.Entry> entries, List<DividendCalendarView.Missing> missing) {
    return new DividendCalendarView(
        AUGUST,
        DividendCalendarGridUtil.build(
            AUGUST, null, DividendCalendarGridUtil.groupByDay(entries, AUGUST)),
        List.of(),
        missing);
  }

  private static String render(DividendCalendarView calendar) {
    Map<String, Object> model = new HashMap<>();
    model.put("calendar", calendar);
    model.put("prevHref", "/stock/dividend?tab=calendar&month=2026-07");
    model.put("nextHref", "/stock/dividend?tab=calendar&month=2026-09");
    model.put("thisHref", "/stock/dividend?tab=calendar&month=2026-09");
    model.put("isThisMonth", false);
    model.put("avgLabel", "AVG-BASIS");
    model.put("latestLabel", "LATEST-BASIS");
    model.put("actualGrossLabel", "ACTUAL-GROSS");
    model.put("actualNetLabel", "ACTUAL-NET");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(CALENDAR, model, output);
    return output.toString();
  }

  private static String controllerSource() throws IOException {
    return Files.readString(Path.of(CONTROLLER), StandardCharsets.UTF_8)
        .replaceAll("[ \t\r\n]+", " ");
  }

  /** 실제 수령액으로 적힌 칸은 "최근/평균" 이 아니라 "세전/실수령" 이라고 말한다. */
  @Test
  void 실제_수령액_칸은_기준_이름이_다르다() {
    String html = render(calendar(List.of(entry("가", 4, 1341850L, 1337470L, true)), List.of()));

    assertThat(html).contains("ACTUAL-GROSS").contains("ACTUAL-NET");
    assertThat(html)
        .as("실제 수령액에 '최근'·'평균' 이라고 적으면 예상치로 읽힌다")
        .doesNotContain("LATEST-BASIS")
        .doesNotContain("AVG-BASIS");
    assertThat(html).contains("1,341,850").contains("1,337,470");
  }

  /** 추정으로 적힌 칸은 그대로 "최근/평균" 이다. */
  @Test
  void 추정_칸은_기준_이름이_그대로다() {
    String html = render(calendar(List.of(entry("가", 4, 1000L, 900L, false)), List.of()));

    assertThat(html).contains("LATEST-BASIS").contains("AVG-BASIS");
    assertThat(html).doesNotContain("ACTUAL-GROSS").doesNotContain("ACTUAL-NET");
  }

  /** 날짜도 금액도 전부 실제일 때만 "실제" 라고 말한다. */
  @Test
  void 전부_실제인_달만_실제라고_말한다() {
    String allActual = render(calendar(List.of(entry("가", 4, 1000L, 900L, true)), List.of()));
    assertThat(allActual).contains("data-actual-amount=" + QUOTE + "true" + QUOTE);
    assertThat(allActual).contains(MessageUtil.getMessage("stock.dividend.calendar.actual"));

    String mixed =
        render(
            calendar(
                List.of(entry("가", 4, 1000L, 900L, true), entry("나", 19, 500L, 450L, false)),
                List.of()));
    assertThat(mixed)
        .as("한 칸이라도 추정이면 화면은 추정이라고 말해야 한다")
        .contains("data-actual-amount=" + QUOTE + "false" + QUOTE);
    assertThat(mixed).contains(MessageUtil.getMessage("stock.dividend.calendar.settled"));
  }

  /** 달력에 못 놓은 종목을 금액과 함께 밝힌다. 금액은 "금액 숨김" 이 가릴 수 있어야 한다. */
  @Test
  void 못_놓은_종목을_금액과_함께_밝힌다() {
    DividendCalendarView view =
        calendar(
            List.of(entry("가", 4, 3096324L, 3000000L, true)),
            List.of(new DividendCalendarView.Missing("삼성전자", BigDecimal.valueOf(1886082L))));
    String html = render(view);

    assertThat(html).contains("data-calendar-missing=" + QUOTE + "1" + QUOTE);
    assertThat(html).contains("삼성전자");
    assertThat(html).contains(MessageUtil.getMessage("stock.dividend.calendar.missing"));
    assertThat(html).contains("1,886,082");
    // 합이 맞아야 밝힌 보람이 있다: 달력에 놓인 것 + 놓지 못한 것 = 그 달 실제 수령.
    assertThat(view.actualMonthTotal()).isEqualByComparingTo(BigDecimal.valueOf(4982406L));
    assertThat(html).contains("4,982,406");
    assertThat(html).contains("data-calendar-actual-month-total");

    String block = html.substring(html.indexOf("data-calendar-missing"));
    assertThat(block.indexOf("1,886,082"))
        .as("금액이 amount-value 밖에 있으면 '금액 숨김' 이 못 가린다")
        .isGreaterThan(block.indexOf("amount-value"));
  }

  /** 놓지 못한 종목이 없으면 그 상자도 없다. */
  @Test
  void 못_놓은_종목이_없으면_상자도_없다() {
    String html = render(calendar(List.of(entry("가", 4, 1000L, 900L, true)), List.of()));

    assertThat(html).doesNotContain("data-calendar-missing");
  }

  /**
   * 그 달 실제 합계는 놓지 못한 종목이 없어도 적는다.
   *
   * <p>화면 위 요약 카드와 월중/월말 소계는 <b>지금 보유 수량</b> 기준 예상이라 달을 넘겨도 값이 같다 &mdash; 실측 2026-09-15: 어느 달을 봐도
   * 3,956,660 원. 그 옆에 그 달 실제 합계가 없으면 위 숫자가 그 달 얼기로 읽힌다(2026-08 실제 4,982,406 원과 -20.6%, 2026-07 은
   * +16.9% 어긋난다).
   */
  @Test
  void 실제_금액인_달은_그_달_합계를_적는다() {
    String actual = render(calendar(List.of(entry("가", 4, 1000L, 900L, true)), List.of()));
    assertThat(actual).contains("data-calendar-actual-month-total");
    assertThat(actual)
        .contains(MessageUtil.getMessage("stock.dividend.calendar.actual.month.total"));

    String projected = render(calendar(List.of(entry("가", 4, 1000L, 900L, false)), List.of()));
    assertThat(projected)
        .as("예상치뿐인 달에 '실제 수령 합계' 를 적으면 지어낸 숫자가 실적처럼 읽힐다")
        .doesNotContain("data-calendar-actual-month-total");
  }

  /** 합계는 <b>실제로 적힌 칸</b>만 센다. 추정 칸을 섞으면 "실제 수령 합계" 가 실제가 아니게 된다. */
  @Test
  void 실제_합계는_추정_칸을_세지_않는다() {
    DividendCalendarView view =
        calendar(
            List.of(entry("가", 4, 1000L, 900L, true), entry("나", 19, 7000L, 6000L, false)),
            List.of(new DividendCalendarView.Missing("삼성전자", BigDecimal.valueOf(500L))));

    assertThat(view.placedActualTotal()).isEqualByComparingTo(BigDecimal.valueOf(1000L));
    assertThat(view.missingTotal()).isEqualByComparingTo(BigDecimal.valueOf(500L));
    assertThat(view.actualMonthTotal()).isEqualByComparingTo(BigDecimal.valueOf(1500L));
  }

  /** 금액은 기본이 추정이다 &mdash; 옛 생성자로 만든 칸이 말없이 "실제" 가 되면 안 된다. */
  @Test
  void 금액은_기본이_추정이다() {
    DividendCalendarView.Entry old =
        new DividendCalendarView.Entry(
            "가", "가", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, null, 4, 2, 19, 12, true);

    assertThat(old.actualAmount()).isFalse();
    assertThat(new DividendCalendarView(AUGUST, List.of(), List.of()).hasMissing()).isFalse();
  }

  /**
   * 배당이 <b>한 건도 없는</b> 지난 달도 원장의 답이다.
   *
   * <p>비었다고 예상치로 되돌리면 받지도 않은 배당이 지난 달력에 그대로 찍힌다 &mdash; 실측 2026-09-15: 배당이 있는 첫 달(2020-04) 부터 지난
   * 달까지 <b>사이에만</b> 그런 달이 41 개(2020-06 · 2021-01 ...) 있고, 그 이전 달은 전부 그렇다. 그때마다 화면은 "월 예상 8 종목" 을
   * 그렸다.
   *
   * <p>빈 격자만 두면 이번엔 화면이 고장 난 것처럼 보이므로 그 달엔 받은 게 없다고 말하게 한다.
   */
  @Test
  void 배당이_없던_지난_달은_그렇다고_말한다() {
    DividendCalendarView empty =
        new DividendCalendarView(
            AUGUST,
            DividendCalendarGridUtil.build(
                AUGUST, null, DividendCalendarGridUtil.groupByDay(List.of(), AUGUST)),
            List.of(),
            List.of(),
            true);
    String html = render(empty);

    assertThat(empty.isSettledEmpty()).isTrue();
    assertThat(html).contains("data-settled-empty=" + QUOTE + "true" + QUOTE);
    assertThat(html).contains(MessageUtil.getMessage("stock.dividend.calendar.settled.empty"));
    assertThat(html)
        .as("받은 게 없는 달에 '날짜는 추정' 이라고 적으면 예상 일정이 있는 것처럼 읽힌다")
        .doesNotContain(MessageUtil.getMessage("stock.dividend.calendar.estimated"))
        .doesNotContain(MessageUtil.getMessage("stock.dividend.calendar.settled"));
  }

  /**
   * 한 칸도 못 채웠지만 <b>달력 밖에는 있는</b> 달.
   *
   * <p>그 달 배당이 전부 월배당 기준 데이터 없는 종목이면 격자가 텅 빈다 &mdash; 실측 2026-09-15: 2024-11 은 삼성전자 2,197,046 원 +
   * SK텔레콤 1,284,840 원이 전부라 한 칸도 안 찼고, 2020-04 도 삼성SDI · 하나금융지주뿐이었다. 그때 화면은 "금액은 지금 보유 수량 기준 예상치" 라고
   * 말했는데 <b>가리킬 칸이 하나도 없었다</b>.
   *
   * <p>빈 달(받은 게 없다)과도 다른 상태다 &mdash; 받기는 받았고 달력이 못 그린 것뿐이다.
   */
  @Test
  void 전부_달력_밖인_달은_그렇다고_말한다() {
    DividendCalendarView view =
        calendar(
            List.of(),
            List.of(new DividendCalendarView.Missing("삼성전자", BigDecimal.valueOf(2197046L))));
    String html = render(view);

    assertThat(view.hasPlacedEntries()).isFalse();
    assertThat(view.hasMissing()).isTrue();
    assertThat(view.isSettledEmpty()).as("받기는 받았으니 빈 달이 아니다").isFalse();

    assertThat(html).contains(MessageUtil.getMessage("stock.dividend.calendar.all.outside"));
    assertThat(html)
        .as("가리킬 칸이 없는데 '금액은 예상치' 라고 하면 없는 칸을 설명하는 셈이다")
        .doesNotContain(MessageUtil.getMessage("stock.dividend.calendar.settled"));
    assertThat(html).contains("2,197,046");
  }

  /** 칸이 하나라도 차면 그 달은 '전부 달력 밖' 이 아니다. */
  @Test
  void 칸이_있으면_전부_밖이_아니다() {
    DividendCalendarView view =
        calendar(
            List.of(entry("가", 4, 1000L, 900L, true)),
            List.of(new DividendCalendarView.Missing("삼성전자", BigDecimal.valueOf(500L))));

    assertThat(view.hasPlacedEntries()).isTrue();
    assertThat(render(view))
        .doesNotContain(MessageUtil.getMessage("stock.dividend.calendar.all.outside"));
  }

  /** 받은 것이 있는 달은 빈 달 안내를 하지 않는다. 기본값도 빈 달이 아니다. */
  @Test
  void 받은_것이_있으면_빈_달이_아니다() {
    DividendCalendarView view = calendar(List.of(entry("가", 4, 1000L, 900L, true)), List.of());

    assertThat(view.isSettledEmpty()).as("네 인자 생성자의 기본은 빈 달이 아니다").isFalse();
    String html = render(view);
    assertThat(html).contains("data-settled-empty=" + QUOTE + "false" + QUOTE);
    assertThat(html)
        .doesNotContain(MessageUtil.getMessage("stock.dividend.calendar.settled.empty"));
  }

  /**
   * 원장이 말한 0 에는 물음표를 달지 않는다.
   *
   * <p>과세표준 0 옆의 물음표는 "기준 데이터에 안 적혀 있다" 는 뜻이다. 금액이 원장에서 온 달에는 그 0 이 곧 <b>그 달의 사실</b>이라 물음표를 달면 확인된
   * 값을 의심하게 만든다 &mdash; 실측 2026-09-15: 2026-08 에 네 종목(TIGER 리츠부동산인프라 등)이 실제 과세표준 0 인데 물음표가 붙어 있었다.
   */
  @Test
  void 원장이_말한_과표_0_에는_물음표를_안_단다() {
    java.math.BigDecimal ratio = new java.math.BigDecimal("31.3");
    DividendCalendarView.Entry actual =
        new DividendCalendarView.Entry(
            "가",
            "가",
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            ratio,
            4,
            4,
            4,
            12,
            true,
            true);
    DividendCalendarView.Entry projected =
        new DividendCalendarView.Entry(
            "가",
            "가",
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            ratio,
            4,
            4,
            4,
            12,
            true,
            false);

    assertThat(actual.taxableLooksUnregistered())
        .as("원장이 0 이라고 말한 달에 '미등록' 물음표를 달면 안 된다")
        .isFalse();
    assertThat(projected.taxableLooksUnregistered()).as("예상치일 때는 0 이 미등록일 수 있으므로 그대로 밝힌다").isTrue();

    String html = render(calendar(List.of(actual), List.of()));
    assertThat(html).contains("data-taxable-unknown=" + QUOTE + "false" + QUOTE);
  }

  /** 기간은 그 달 전체다. endDate 가 배타적이라 다음 달 1 일을 준다. */
  @Test
  void 그_달_전체를_조회한다() throws IOException {
    String source = controllerSource();

    assertThat(source)
        .contains(
            "params.add("
                + QUOTE
                + "startDate"
                + QUOTE
                + ", month.atDay(1).atStartOfDay(zone).toInstant().toString());");
    assertThat(source)
        .as("endDate 는 배타적이라 그 달 말일을 주면 마지막 날 배당이 빠진다")
        .contains(
            "params.add("
                + QUOTE
                + "endDate"
                + QUOTE
                + ", month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toString());");
  }
}
