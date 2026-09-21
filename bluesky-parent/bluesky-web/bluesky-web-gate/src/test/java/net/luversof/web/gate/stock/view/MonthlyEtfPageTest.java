package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.view.MonthlyEtfRowView;
import net.luversof.web.gate.stock.service.MonthlyEtfViewSupport;

/**
 * 월배당 ETF 목록 화면(사용자 요청 2026-09-21).
 *
 * <p>요청: "보유하지 않은 종목 등록이 많아질 테니 월배당 ETF 정보를 보여 주는 메뉴가 따로 있어야 할 것 같다 &mdash; 등록된 데이터를 기준으로 정렬하고 검색할
 * 수 있는 메뉴. 기존 시뮬레이터의 월배당은 보유 종목에 대해서만 보여 주어야 할 것 같다."
 *
 * <p>그래서 둘을 가른다: <b>내가 받을 배당금</b>(시뮬레이터, 보유 종목 · 수량 · 평단)과 <b>종목 정보</b>(이 화면, 등록된 기준 데이터). 이 화면에는
 * 금액 · 수량 · 예상 배당금이 없고, 보유 중인지만 표시한다.
 */
class MonthlyEtfPageTest {

  private static final String TEMPLATE_PATH = "src/main/jte/stock/monthlyEtf.jte";

  private final MonthlyEtfViewSupport support = new MonthlyEtfViewSupport();

  @Test
  void 정렬_키는_화면이_내보내는_것만_받는다() throws IOException {
    String template = Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8);
    String marker = "sort=";
    int at = template.indexOf(marker);
    int found = 0;
    while (at >= 0) {
      int from = at + marker.length();
      int to = from;
      while (to < template.length() && isSortKeyChar(template.charAt(to))) {
        to++;
      }
      String key = template.substring(from, to);
      if (!key.isEmpty() && !key.startsWith("$")) {
        found++;
        assertThat(support.resolveSort(key))
            .as(key + " 는 화면이 내보내는 정렬 키인데 허용 목록에 없다 - 눌러도 기본 순서로 되돌아간다")
            .isEqualTo(key);
      }
      at = template.indexOf(marker, from);
    }
    assertThat(found).as("템플릿에서 정렬 링크를 못 찾았다 - 스캔이 깨졌다").isGreaterThanOrEqualTo(5);
    assertThat(support.resolveSort("bogus")).isEqualTo("display-order");
  }

  @Test
  void 검색과_필터가_화면에_있다() throws IOException {
    String template = Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8);

    assertThat(template).contains("<input name=\"keyword\" type=\"search\"");
    assertThat(template).contains("<input name=\"minAnnualYield\" type=\"number\"");
    assertThat(template).contains("<select name=\"payoutWindow\"");
    assertThat(template).contains("<select name=\"holding\"");
    assertThat(template)
        .as("정렬 링크가 필터를 안 실으면 정렬 한 번에 검색어가 풀린다")
        .contains("String baseUrl = \"/stock/monthly-etf?\"")
        .contains("URLEncoder.encode(monthlyEtfKeyword");

    String controller =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/"
                    + "StockMonthlyEtfViewController.java"),
            StandardCharsets.UTF_8);
    assertThat(controller)
        .contains("@GetMapping(\"/monthly-etf\")")
        .contains("@RequestParam(required = false) String keyword")
        .contains("@RequestParam(required = false) String payoutWindow")
        .contains("@RequestParam(required = false) String holding");
    // 받기만 하고 거르는 데 안 넘기면 주소에 쳐도 아무 일이 없다 - 넘기는 줄까지 본다.
    assertThat(controller)
        .contains(
            "allRows, resolvedKeyword, minAnnualYield, resolvedPayoutWindow, resolvedHolding");
  }

  /** 이 화면은 종목 정보만 다룬다 - 금액 · 수량이 새어 들어오면 시뮬레이터와 역할이 섞인다. */
  @Test
  void 금액과_수량은_이_화면에_없다() throws IOException {
    String template = Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8);

    assertThat(template)
        .as("보유 수량 · 평단 · 예상 배당금은 시뮬레이터 몫이다")
        .doesNotContain("heldQuantity")
        .doesNotContain("averageBuyPrice")
        .doesNotContain("expectedMonthlyDividend")
        .doesNotContain("currentMarketValue");
    assertThat(MonthlyEtfRowView.class.getRecordComponents())
        .extracting(java.lang.reflect.RecordComponent::getName)
        .as("줄 정보에도 금액 · 수량을 담지 않는다(보유 여부만)")
        .contains("held")
        .doesNotContain("heldQuantity", "averageBuyPrice", "expectedMonthlyDividend");
  }

  /**
   * 시세가 없으면 수익률을 0% 라고 적지 않는다.
   *
   * <p>실측 2026-09-21: 새로 등록한 KODEX 200커버드콜액티브는 지급 이력 2 건인데 현재가가 0 원(시세 미수집)이라 "연 0.00%" 로 나왔다
   * &mdash; 0 은 "수익률이 0" 이 아니라 "아직 셀 수 없음" 이다. 기준일이 30 일보다 오래되면 함께 알린다(비보유 종목 시세는 뒤처진다).
   */
  @Test
  void 시세가_없으면_수익률을_0_으로_적지_않는다() throws IOException {
    String template = Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8);

    assertThat(template)
        .contains(
            "boolean priceKnown = row.currentPrice() != null && row.currentPrice().signum() > 0;")
        .contains("data-price-missing")
        .contains("data-yield-unknown")
        .as("오래된 기준일 표시")
        .contains("data-price-date")
        .contains("MonthlyEtfViewSupport.priceStale(row.currentPriceDate()");

    // 오래됨 판정은 글자가 아니라 동작으로 본다 - isBefore 를 isAfter 로 뒤집는 변이가 글자 대조를 통과했다(2026-09-21).
    LocalDate today = LocalDate.of(2026, 9, 21);
    assertThat(MonthlyEtfViewSupport.priceStale(LocalDate.of(2026, 4, 1), today))
        .as("173 일 전 시세는 오래됐다")
        .isTrue();
    assertThat(MonthlyEtfViewSupport.priceStale(today.minusDays(3), today))
        .as("사흘 전 시세에 (오래됨) 이 붙으면 전부 오래된 것처럼 읽힌다")
        .isFalse();
    assertThat(MonthlyEtfViewSupport.priceStale(today.minusDays(30), today))
        .as("경계(30 일)는 아직 오래된 것이 아니다")
        .isFalse();
    assertThat(MonthlyEtfViewSupport.priceStale(today.minusDays(31), today)).isTrue();
    assertThat(MonthlyEtfViewSupport.priceStale(null, today)).isFalse();
    // 수익률 칸이 priceKnown 안에서만 숫자를 찍는지 - 바깥에 남으면 0% 가 그대로 나간다.
    int yieldAt = template.indexOf("data-yield-unknown");
    String yieldCell = template.substring(template.lastIndexOf("<td", yieldAt), yieldAt);
    assertThat(yieldCell).as("수익률 숫자는 시세가 있을 때만").contains("@if(priceKnown)");
  }

  /**
   * 기간 수익률(사용자 결정 2026-09-21): 기간을 고르는 단추 한 줄 + 열 둘.
   *
   * <p>열을 여덟 개 늘리는 대신 한 번에 한 기간만 본다. 기간은 주소에 실려 다녀야 한다 &mdash; 안 그러면 정렬을 한 번 누를 때마다 기간이 풀린다.
   * 이력이 그 기간을 못 덮는 종목은 값 대신 "언제부터 있는지" 를 적는다(0% 라고 적으면 거짓이다).
   */
  @Test
  void 기간_단추와_두_열이_있다() throws IOException {
    String template = Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8);

    assertThat(template)
        .as("기간 단추 한 줄")
        .contains("data-period-option")
        .contains("aria-current=\"${periodSelected ? \"true\" : \"false\"}\"");
    assertThat(template)
        .as("정렬 링크가 기간을 싣고 다닌다")
        .contains("+ \"period=\" + monthlyEtfPeriod + \"&\";");
    assertThat(template)
        .as("가격 · 합산 두 열과 각각의 정렬 링크")
        .contains("sort=period-price")
        .contains("sort=period-total")
        .contains("data-period-price")
        .contains("data-period-total");

    // 값이 없을 때 0% 를 적지 않는다 - 이력 시작일을 대신 적는다.
    assertThat(template)
        .contains("data-period-missing")
        .contains("row.priceHistoryStartDate()");
    int missingAt = template.indexOf("data-period-missing");
    String missingCell = template.substring(template.lastIndexOf("<td", missingAt), missingAt);
    assertThat(missingCell).as("숫자는 값이 있을 때만").contains("@if(periodKnown)");

    assertThat(support.resolvePeriod(3)).isEqualTo(3);
    assertThat(support.resolvePeriod(null)).as("기본은 1 년").isEqualTo(12);
    assertThat(support.resolvePeriod(7)).as("모르는 기간은 기본값으로").isEqualTo(12);
  }

  /** 이력이 모자라 값이 없는 행은 방향을 뒤집어도 늘 뒤에 남아야 한다(빈 칸이 "가장 낮은 수익률" 로 읽히면 안 된다). */
  @Test
  void 값이_없는_행은_정렬_방향과_무관하게_뒤로_간다() {
    List<MonthlyEtfRowView> rows =
        List.of(
            rowWithPeriod("A00001", new BigDecimal("5.00")),
            rowWithPeriod("B00002", null),
            rowWithPeriod("C00003", new BigDecimal("15.00")));

    assertThat(support.sortRows(rows, "period-total", "desc"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("C00003", "A00001", "B00002");
    assertThat(support.sortRows(rows, "period-total", "asc"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .as("오름차순에서도 빈 칸은 뒤")
        .containsExactly("A00001", "C00003", "B00002");
  }

  @Test
  void 필터는_검색어_수익률_지급시기_보유구분을_함께_건다() {
    List<MonthlyEtfRowView> rows =
        List.of(
            row("498400", "KODEX 200타겟위클리커버드콜", "MID_MONTH", "15.36", 12, true),
            row("489030", "PLUS 고배당주위클리커버드콜", "MONTH_END", "39.48", 8, false),
            row("466940", "TIGER 은행고배당플러스TOP10", "MID_MONTH", "4.20", 0, false));

    assertThat(support.filterRows(rows, "tiger", null, "", "all"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("466940");
    assertThat(support.filterRows(rows, null, new BigDecimal("10"), "", "all"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("498400", "489030");
    assertThat(support.filterRows(rows, null, null, "MID_MONTH", "all"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("498400", "466940");
    assertThat(support.filterRows(rows, null, null, "", "held"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("498400");
    assertThat(support.filterRows(rows, null, null, "", "not-held"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("489030", "466940");
    assertThat(support.resolveHolding("bogus")).isEqualTo("all");
    assertThat(support.resolvePayoutWindow("bogus")).isEmpty();
  }

  /** 수익률 · 이력 건수는 큰 값부터, 이름 · 종목코드는 작은 값부터가 기본이다. */
  @Test
  void 정렬은_키마다_기본_방향이_있다() {
    assertThat(support.resolveDirection("annual-yield", null)).isEqualTo("desc");
    assertThat(support.resolveDirection("payout-count", null)).isEqualTo("desc");
    assertThat(support.resolveDirection("symbol", null)).isEqualTo("asc");
    assertThat(support.resolveDirection("taxable-base", null)).isEqualTo("asc");
    assertThat(support.resolveDirection("annual-yield", "ASC")).isEqualTo("asc");

    List<MonthlyEtfRowView> rows =
        List.of(
            row("498400", "KODEX", "MID_MONTH", "15.36", 12, true),
            row("489030", "PLUS", "MONTH_END", "39.48", 8, false));
    assertThat(support.sortRows(rows, "annual-yield", "desc"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("489030", "498400");
    assertThat(support.sortRows(rows, "payout-count", "desc"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("498400", "489030");
  }

  /** 메뉴에 들어가 있어야 찾아갈 수 있다 - 주소 · 활성 패턴 · 이름까지. */
  @Test
  void 주식_메뉴에_들어_있다() throws IOException {
    String properties =
        Files.readString(
            Path.of("src/main/resources/application.properties"), StandardCharsets.UTF_8);
    // 줄 단위로 정확히 본다 - contains 만 쓰면 /stock/monthly-etf-x 같은 주소도 통과한다(복사본 변이 2026-09-21).
    java.util.List<String> lines = properties.lines().map(String::trim).toList();
    assertThat(lines)
        .contains("bluesky.web.common.menu.stock[7].message-code=layout.menu.stock.monthlyEtf")
        .contains("bluesky.web.common.menu.stock[7].url=/stock/monthly-etf")
        .contains("bluesky.web.common.menu.stock[7].active-url-pattern=/stock/monthly-etf.*");
    assertThat(
            Files.readString(
                Path.of("src/main/resources/uiMessage.properties"), StandardCharsets.UTF_8))
        .contains("layout.menu.stock.monthlyEtf");
    assertThat(
            Files.readString(
                Path.of("src/main/resources/uiMessage_ko.properties"), StandardCharsets.UTF_8))
        .as("한글 메시지는 ASCII 이스케이프로 넣는다")
        .contains("layout.menu.stock.monthlyEtf = \\u");
  }

  /** 기간 합산 수익률만 다른 행(나머지는 같은 값). */
  private static MonthlyEtfRowView rowWithPeriod(String symbol, BigDecimal periodTotal) {
    MonthlyEtfRowView base = row(symbol, "이름 " + symbol, "MID_MONTH", "10.00", 12, false);
    return new MonthlyEtfRowView(
        base.stockItemId(),
        base.stockItemSymbol(),
        base.stockItemName(),
        base.payoutWindow(),
        base.sourceUrl(),
        base.lastVerifiedDate(),
        base.active(),
        base.displayOrder(),
        base.payoutCount(),
        base.latestRecordDate(),
        base.latestPayDate(),
        base.latestDividendPerShare(),
        base.averageDividendPerShare1y(),
        base.averageTaxableBaseRatio1y(),
        base.averageTaxableBasePerShare1y(),
        base.currentPrice(),
        base.currentPriceDate(),
        base.monthlyYieldPct(),
        base.annualYieldPct(),
        base.priceHistoryStartDate(),
        periodTotal,
        periodTotal,
        base.periodBaseDate(),
        base.held());
  }

  private static boolean isSortKeyChar(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-';
  }

  private static MonthlyEtfRowView row(
      String symbol,
      String name,
      String payoutWindow,
      String annualYieldPct,
      int payoutCount,
      boolean held) {
    BigDecimal annual = new BigDecimal(annualYieldPct);
    return new MonthlyEtfRowView(
        UUID.randomUUID(),
        symbol,
        name,
        payoutWindow,
        "https://example.test/" + symbol,
        LocalDate.of(2026, 9, 17),
        true,
        1,
        payoutCount,
        LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 17),
        new BigDecimal("300"),
        new BigDecimal("260"),
        new BigDecimal("4.06"),
        new BigDecimal("10.56"),
        new BigDecimal("20365"),
        LocalDate.of(2026, 9, 18),
        annual.divide(new BigDecimal("12"), 2, java.math.RoundingMode.HALF_UP),
        annual,
        LocalDate.of(2025, 9, 1),
        new BigDecimal("3.21"),
        new BigDecimal("12.34"),
        LocalDate.of(2025, 9, 19),
        held);
  }
}
