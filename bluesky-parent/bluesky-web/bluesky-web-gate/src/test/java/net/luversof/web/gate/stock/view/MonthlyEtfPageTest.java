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
import net.luversof.web.gate.stock.service.MonthlyContributionPickSupport;
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

  private static final String CONTROLLER_PATH =
      "src/main/java/net/luversof/web/gate/stock/controller/StockMonthlyEtfViewController.java";

  private final MonthlyEtfViewSupport support =
      new MonthlyEtfViewSupport(new MonthlyContributionPickSupport());

  @Test
  void 위험_지표_두_열이_기준을_적는다() throws IOException {
    // 사용자 결정 2026-09-22: 1 년 기준으로 내고 추천 점수에는 안 섮는다. 기간을 안 적으면
    // 어느 창의 수인지 알 수 없어 숫자를 견줄 수 없다.
    String template = squash(Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8));

    assertThat(template)
        .as("낙폭 열과 그 기간 설명이 있어야 한다")
        .contains(
            squash(
                "<span>${MessageUtil.getMessage(\"stock.monthly.etf.table.header.drawdown\")}</span>"))
        .contains(
            squash(
                "text-base-content/60\">${MessageUtil.getMessage(\"stock.monthly.etf.table.header.drawdown.desc\")}</div>"));
    assertThat(template)
        .as("변동성 열과 그 기간 설명이 있어야 한다")
        .contains(
            squash(
                "<span>${MessageUtil.getMessage(\"stock.monthly.etf.table.header.volatility\")}</span>"))
        .contains(
            squash(
                "text-base-content/60\">${MessageUtil.getMessage(\"stock.monthly.etf.table.header.volatility.desc\")}</div>"));
    assertThat(template)
        .as("값이 없으면 0% 가 아니라 까닭을 적어야 한다")
        .contains(
            squash(
                "@else<div class=\"text-base-content/60\" data-risk-missing>"
                    + "${MessageUtil.getMessage(\"stock.monthly.etf.table.cell.risk.unknown\")}</div>"));
  }

  @Test
  void 낙폭은_손실_색이고_0은_중립이다() throws IOException {
    // 낙폭은 늘 0 이하다. 0 을 손실 색으로 칠하면 "빠졌다" 로 읽힌다.
    String template = squash(Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8));

    assertThat(template)
        .contains(
            squash(
                "${row.maxDrawdownPct().signum() < 0 ? \"text-loss\" : "
                    + "\"text-base-content/60\"}"));
  }

  @Test
  void 이력이_1년을_못_덮으면_언제부터인지_적는다() throws IOException {
    String template = squash(Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8));

    assertThat(template)
        .as("일지 않으면 모자란 이력으로 낸 수가 1 년치처럼 읽힌다")
        .contains(squash("data-risk-from"))
        .contains(
            squash(
                "String[] riskFromParts = aroundDate.apply(\"stock.monthly.etf.table.cell.risk.from\");"))
        .contains(
            squash(
                "data-risk-from>${riskFromParts[0]}<span class=\"whitespace-nowrap date-whole\">${String.valueOf(row.riskFromDate())}</span><wbr>${riskFromParts[1]}"));
    assertThat(template)
        .as("몇일 어긋나는 것까지 적으면 거의 모든 줄에 붙는다 - 한 달 넘게 모자랄 때만")
        .contains(squash("riskPartial.test(row.riskFromDate())"));
  }

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
        .contains("@RequestParam(required = false) String holding")
        .contains("@RequestParam(required = false) String account");
    // 받기만 하고 거르는 데 안 넘기면 주소에 쳐도 아무 일이 없다 - 넘기는 줄까지 본다.
    // 인자가 늘면 줄이 나뉜다 - 공백을 눌러 보아야 서식 바뀜에 가드가 깨지지 않는다.
    assertThat(squash(controller))
        .contains(
            squash(
                "allRows, resolvedKeyword, minAnnualYield, resolvedPayoutWindow,"
                    + " resolvedHolding, resolvedAccount"));
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
    assertThat(squash(template))
        .as("연배당 칸은 표시로 찾을 수 있어야 한다 - 2026-09-22 열이 늘자 칸 번호로 읽던 탐침이 낙폭 칸을 읽었다")
        .contains(squash("<div class=\"font-semibold\" data-annual-yield>"));

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
   * <p>열을 여덟 개 늘리는 대신 한 번에 한 기간만 본다. 기간은 주소에 실려 다녀야 한다 &mdash; 안 그러면 정렬을 한 번 누를 때마다 기간이 풀린다. 이력이 그
   * 기간을 못 덮는 종목은 값 대신 "언제부터 있는지" 를 적는다(0% 라고 적으면 거짓이다).
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
    assertThat(template).contains("data-period-missing").contains("row.priceHistoryStartDate()");
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

    assertThat(support.filterRows(rows, "tiger", null, "", "all", ""))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("466940");
    assertThat(support.filterRows(rows, null, new BigDecimal("10"), "", "all", ""))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("498400", "489030");
    assertThat(support.filterRows(rows, null, null, "MID_MONTH", "all", ""))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("498400", "466940");
    assertThat(support.filterRows(rows, null, null, "", "held", ""))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("498400");
    assertThat(support.filterRows(rows, null, null, "", "not-held", ""))
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

  /** 낙폭은 음수라 큰 쪽(덜 빠진 쪽)이 좋고, 변동성은 작은 쪽이 좋다. */
  @Test
  void 위험_지표는_좋은_쪽부터_보여_준다() {
    assertThat(support.resolveDirection("max-drawdown", null))
        .as("낙폭은 -5% 가 -40% 보다 좋다 - 기본은 큰 값부터")
        .isEqualTo("desc");
    assertThat(support.resolveDirection("volatility", null)).as("변동성은 작은 쪽이 좋다").isEqualTo("asc");

    List<MonthlyEtfRowView> rows =
        List.of(
            rowWithRisk("A00001", new BigDecimal("-40.00"), new BigDecimal("55.00")),
            rowWithRisk("B00002", null, null),
            rowWithRisk("C00003", new BigDecimal("-5.00"), new BigDecimal("15.00")));

    assertThat(support.sortRows(rows, "max-drawdown", "desc"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .as("덜 빠진 종목이 먼저, 빈 칸은 뒤")
        .containsExactly("C00003", "A00001", "B00002");
    assertThat(support.sortRows(rows, "max-drawdown", "asc"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("A00001", "C00003", "B00002");
    assertThat(support.sortRows(rows, "volatility", "asc"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("C00003", "A00001", "B00002");
    assertThat(support.sortRows(rows, "volatility", "desc"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .as("내림차순에서도 빈 칸은 뒤")
        .containsExactly("A00001", "C00003", "B00002");
  }

  /**
   * 계좌 대상으로도 걸러 본다(사용자 요청 2026-09-22).
   *
   * <p>가르는 자리는 과세표준 비중 10% 다 - 적립 추천과 같은 규칙을 써야 두 화면이 같은 말을 한다. 10.00% 는 "이내" 라 위탁이다(경계를 반대로 잡으면 한
   * 종목이 통째로 다른 계좌로 간다).
   */
  @Test
  void 계좌_대상으로도_걸러람다() {
    List<MonthlyEtfRowView> rows =
        List.of(
            rowWithTaxable("A00001", new BigDecimal("4.06")),
            rowWithTaxable("B00002", new BigDecimal("10.00")),
            rowWithTaxable("C00003", new BigDecimal("10.01")),
            rowWithTaxable("D00004", new BigDecimal("37.50")),
            rowWithTaxable("E00005", null));

    assertThat(support.filterRows(rows, null, null, "", "all", "BROKERAGE"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .as("10.00% 는 이내라 위탁이다")
        .containsExactly("A00001", "B00002");
    assertThat(support.filterRows(rows, null, null, "", "all", "PENSION"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("C00003", "D00004");
    assertThat(support.filterRows(rows, null, null, "", "all", ""))
        .as("전체로 볼 때는 비중을 모르는 종목도 남는다")
        .hasSize(5);
    assertThat(support.filterRows(rows, null, null, "", "all", "bogus"))
        .as("모르는 값은 전체로 본다")
        .hasSize(5);
    assertThat(support.resolveAccount("PENSION")).isEqualTo("PENSION");
    assertThat(support.resolveAccount("bogus")).isEmpty();
  }

  /** 화면에도 고르는 칸이 있고, 정렬 링크가 그 값을 버리지 않아야 한다. */
  @Test
  void 계좌_고르는_칸이_화면에_있다() throws IOException {
    String template = squash(Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8));

    assertThat(template)
        .contains(squash("<select name=\"account\""))
        .contains(squash("<option value=\"BROKERAGE\""))
        .contains(squash("<option value=\"PENSION\""));
    assertThat(template)
        .as("정렬 한 번에 계좌 조건이 풀리면 다른 목록을 보게 된다")
        .contains(squash("\"account=\" + monthlyEtfAccount"));
  }

  /**
   * 걸러 놓은 목록 안에서 셋을 고른다(사용자 결정 2026-09-22).
   *
   * <p>셈은 적립 추천과 같고, <b>분배금 추세를 모르는 종목은 뺀다</b> - 감점이 0 이라 이력이 짧은 종목이 연배당만으로 위로 올라오기 때문이다(실측: 추세를
   * 모르는 두 종목이 2 · 3 위였다).
   */
  @Test
  void 추천은_걸러_놓은_것_중에서_셋을_고른다() {
    List<MonthlyEtfRowView> rows =
        List.of(
            rowWithScore("A00001", "10.00", "5.00"),
            rowWithScore("B00002", "20.00", "-12.00"),
            rowWithScore("C00003", "9.00", "1.00"),
            rowWithScore("D00004", "30.00", null),
            rowWithScore("E00005", "8.50", "0.00"));

    List<MonthlyEtfViewSupport.MonthlyEtfPick> picks = support.pickRows(rows);

    assertThat(picks).as("셋까지만").hasSize(3);
    assertThat(picks)
        .extracting(pick -> pick.row().stockItemSymbol())
        .as("D00004 는 연배당 30 이지만 추세를 몰라 빠진다 · B00002 는 20 에서 12 를 깎아 8")
        .containsExactly("A00001", "C00003", "E00005");
    assertThat(picks.get(0).score()).isEqualByComparingTo("10.00");
    assertThat(picks.get(2).score()).isEqualByComparingTo("8.50");
    assertThat(support.pickRows(List.of())).isEmpty();
  }

  /**
   * 점수순 정렬(사용자 요청 2026-09-30: "점수 기준으로 정렬해서 볼 수 있으면"). 점수가 없는 종목(추세를 모르는 종목)은 방향과 무관하게 뒤다 - 빈 칸이
   * "가장 낮은 점수" 나 "가장 높은 점수" 로 읽히면 안 된다.
   */
  @Test
  void 점수순_정렬은_추천과_같은_셈이고_점수_없는_종목은_늘_뒤다() {
    List<MonthlyEtfRowView> rows =
        List.of(
            rowWithScore("A00001", "10.00", "5.00"),
            rowWithScore("B00002", "20.00", "-12.00"),
            rowWithScore("C00003", "9.00", "1.00"),
            rowWithScore("D00004", "30.00", null),
            rowWithScore("E00005", "8.50", "0.00"));

    assertThat(support.resolveSort("score")).isEqualTo(MonthlyEtfViewSupport.SORT_SCORE);
    assertThat(support.resolveDirection("score", null)).as("높은 점수부터가 기본").isEqualTo("desc");
    assertThat(support.sortRows(rows, "score", "desc"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .as("A 10 · C 9 · E 8.5 · B 20-12=8 · D 는 추세를 몰라 점수 없음")
        .containsExactly("A00001", "C00003", "E00005", "B00002", "D00004");
    assertThat(support.sortRows(rows, "score", "asc"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .as("오름차순이어도 점수 없는 D 는 맨 뒤")
        .containsExactly("B00002", "E00005", "C00003", "A00001", "D00004");
    assertThat(support.pickRows(rows).stream().map(pick -> pick.row().stockItemSymbol()).toList())
        .as("표의 점수순 앞 셋 = 카드 셋 - 두 곳이 다른 말을 하면 안 된다")
        .containsExactly("A00001", "C00003", "E00005");
    assertThat(MonthlyEtfViewSupport.scoreOf(rows.get(3))).isNull();
  }

  /** 화면이 "연배당 − 감소 = 점수" 를 적는다. 합친 뒤 반올림하면 적힌 뺄셈이 0.01 어긋난다 - 두 수를 먼저 맞추고 뺀다. */
  @Test
  void 점수는_반올림한_두_수의_뺄셈이라_적힌_셈이_맞는다() {
    // 7.205 → 7.21, -0.804 → 감점 0.80. 합친 뒤 반올림이면 6.401 → 6.40 이 되어 "7.21 − 0.80 = 6.40" 이 적힌다.
    BigDecimal score =
        MonthlyContributionPickSupport.score(new BigDecimal("7.205"), new BigDecimal("-0.804"));
    assertThat(score).isEqualByComparingTo("6.41");
    assertThat(MonthlyContributionPickSupport.cutOf(new BigDecimal("-0.804")))
        .isEqualByComparingTo("0.80");
    assertThat(MonthlyContributionPickSupport.cutOf(new BigDecimal("3.10")))
        .as("분배금이 늘면 감점 0 - 늘어난 만큼 더해 주지 않는다")
        .isEqualByComparingTo("0");
    assertThat(MonthlyContributionPickSupport.cutOf(null)).isEqualByComparingTo("0");
  }

  /** 점수 열 · 칸 · 카드 설명이 화면에 있다(사용자 요청 2026-09-30). */
  @Test
  void 점수_열과_계산_방법이_화면에_있다() throws IOException {
    String template = squash(Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8));

    assertThat(template)
        .as("머리글에서 점수순으로 정렬할 수 있다")
        .contains(squash("<a href=\"${baseUrl}sort=score&direction="))
        .contains(
            squash("${MessageUtil.getMessage(\"stock.monthly.etf.table.header.score.desc\")}"));
    assertThat(template)
        .as("점수 칸은 추천과 같은 함수로 낸다")
        .contains(squash("!{BigDecimal rowScore = MonthlyEtfViewSupport.scoreOf(row);}"));
    assertThat(template)
        .as("칸 안에 뺄셈을 적고, 낭독기에는 무엇에서 무엇을 뺐는지 말한다")
        .contains(
            squash(
                "data-score-basis>${rowYield} − ${percentFormat.format(rowCut)}<span class=\"sr-only\">"));
    assertThat(template)
        .as("점수가 없으면 까닭을 적는다 - 시세가 없는 때와 이력이 짧은 때")
        .contains(
            squash(
                "data-score-missing>${MessageUtil.getMessage(\"stock.monthly.etf.table.cell.yield.unknown\")}"))
        .contains(
            squash(
                "data-score-missing>${MessageUtil.getMessage(\"stock.monthly.etf.table.cell.score.unknown\")}"));
    assertThat(template)
        .as("카드도 뺄셈을 적는다 - 추세 % 를 그대로 적으면 점수와 이어지지 않는다")
        .contains(
            squash(
                "MessageUtil.getMessage(\"stock.monthly.etf.pick.basis\"), pickYield, percentFormat.format(pickCut), percentFormat.format(pick.score()))"));
    assertThat(template)
        .as("계산 방법 · 예 · 점수순 보기 링크")
        .contains(squash("data-score-how"))
        .contains(squash("MessageUtil.getMessage(\"stock.monthly.etf.pick.how.formula\")"))
        .contains(squash("MessageUtil.getMessage(\"stock.monthly.etf.pick.how.example\")"))
        .contains(squash("<a href=\"${baseUrl}sort=score&direction=desc\""));

    // 머리글과 본문 칸 수가 같아야 열이 밀리지 않는다 - 점수 칸은 연배당 칸 바로 뒤.
    int header = template.indexOf(squash("data-score-header"));
    int yieldHeader =
        template.indexOf(
            squash("MessageUtil.getMessage(\"stock.monthly.etf.table.header.yield\")"));
    int periodHeader = template.indexOf(squash("sort=period-price&direction"));
    assertThat(yieldHeader).isLessThan(header);
    assertThat(header).isLessThan(periodHeader);
    int cell = template.indexOf(squash("data-score-cell"));
    assertThat(template.indexOf(squash("data-annual-yield"))).isLessThan(cell);
    assertThat(cell).isLessThan(template.indexOf(squash("data-period-price")));
  }

  /** 새 메시지는 ko · en 모두 있고 ko 는 \\u 이스케이프다(편집기 코드페이지로 깨지지 않게). */
  @Test
  void 점수_메시지는_두_언어에_있다() throws IOException {
    String korean =
        Files.readString(
            Path.of("src/main/resources/uiMessage_ko.properties"), StandardCharsets.UTF_8);
    String english =
        Files.readString(
            Path.of("src/main/resources/uiMessage.properties"), StandardCharsets.UTF_8);
    for (String key :
        List.of(
            "stock.monthly.etf.pick.basis.nocut",
            "stock.monthly.etf.pick.how.title",
            "stock.monthly.etf.pick.how.formula",
            "stock.monthly.etf.pick.how.example",
            "stock.monthly.etf.pick.how.sort",
            "stock.monthly.etf.table.header.score",
            "stock.monthly.etf.table.cell.score.basis",
            "stock.monthly.etf.table.cell.score.unknown")) {
      assertThat(korean).contains(key + " = \\u");
      assertThat(english).contains(key + " = ");
    }
  }

  /** 화면에도 카드가 있고, 까닭(연배당 · 추세)을 함께 적어야 근거가 된다. */
  @Test
  void 추천_카드가_화면에_있다() throws IOException {
    String template = squash(Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8));

    assertThat(template)
        .contains(squash("data-etf-picks"))
        .contains(squash("data-etf-pick data-pick-symbol="))
        .contains(squash("data-pick-score"))
        .as("점수만 적으면 왜 그 종목인지 알 수 없다")
        .contains(squash("MessageUtil.getMessage(\"stock.monthly.etf.pick.basis\")"));
    assertThat(template)
        .as("월중과 월말은 적립하는 날이 다르다 - 지급 시기를 함께 적는다(사용자 요청 2026-09-23)")
        .contains(squash("data-pick-window=\"${pick.row().payoutWindow()}\""))
        .as(
            "코드를 그대로 적으면 MID_MONTH 가 화면에 나간다 - 라벨을 골라 적는다."
                + " 표 본문에도 같은 식이 있어 조각만 보면 통과한다 - 배지 식을 통째로 본다")
        .contains(
            squash(
                "data-pick-window=\"${pick.row().payoutWindow()}\">${\"MID_MONTH\".equals(pick.row().payoutWindow()) ? midMonthLabel : (\"MONTH_END\".equals(pick.row().payoutWindow()) ? monthEndLabel "));
    assertThat(squash(Files.readString(Path.of(CONTROLLER_PATH), StandardCharsets.UTF_8)))
        .as("컨트롤러가 안 넘기면 카드는 그려지지 않는다 - 거른 목록을 넘겨야 한다")
        .contains(
            squash(
                "model.addAttribute(\"monthlyEtfPicks\", monthlyEtfViewSupport.pickRows(rows))"));
  }

  /** "이번 적립만 보기" - ETF 가 많아지면 네 자리만 보고 싶다(사용자 요청 2026-09-23). */
  @Test
  void 이번_적립만_보기는_적립_자리_종목만_남긴다() throws IOException {
    List<MonthlyEtfRowView> rows =
        List.of(
            rowWithTaxable("A00001", new BigDecimal("4.00")),
            rowWithTaxable("B00002", new BigDecimal("40.00")),
            rowWithTaxable("C00003", new BigDecimal("4.00")));

    assertThat(support.filterContribution(rows, java.util.Set.of("B00002"), "contribution"))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .containsExactly("B00002");
    assertThat(support.filterContribution(rows, java.util.Set.of("B00002"), ""))
        .as("전체 보기면 걸르지 않는다")
        .hasSize(3);
    assertThat(support.resolveView("contribution")).isEqualTo("contribution");
    assertThat(support.resolveView("bogus")).as("모르는 값은 전체 보기").isEmpty();

    String controller = squash(Files.readString(Path.of(CONTROLLER_PATH), StandardCharsets.UTF_8));
    assertThat(controller)
        .as("추천은 걸러 놓은 목록이 아니라 보유 종목으로 낸 것으로 거른다 - 걸러진 목록으로 내면 답이 바뀐다")
        .contains(squash("contributionPicks.keySet(), resolvedView)"));
    String template = squash(Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8));
    assertThat(template)
        .as("정렬 · 기간 링크와 필터 폼이 보기를 버리면 한 번 누른 보기가 풀린다")
        .contains(
            squash(
                "+ (monthlyEtfView == null || monthlyEtfView.isBlank() ? \"\" : \"view=\" + monthlyEtfView + \"&\")"))
        .contains(squash("<input type=\"hidden\" name=\"view\" value=\"${monthlyEtfView}\" />"))
        .contains(squash("data-contribution-view-link=\"contribution\""))
        .contains(squash("data-contribution-view-link=\"all\""));
  }

  /**
   * 자리별 적립 추천(사용자 요청 2026-09-30: "이번적립으로 보기하면 그냥 월중 월말 섞어서 나오는데 기준이 뭐야?"). 다음 지급일이 아니라 자리(지급 시기 x
   * 계좌)마다 하나다 - 그래서 자리 차례로 놓고, 무슨 자리인지 적고, 규칙을 보기 안에 적는다.
   */
  @Test
  void 자리별_적립_추천은_자리_차례로_놓고_규칙을_적는다() throws IOException {
    List<MonthlyEtfRowView> rows =
        List.of(
            rowWithTaxable("A00001", new BigDecimal("4.00")),
            rowWithTaxable("B00002", new BigDecimal("40.00")),
            rowWithTaxable("C00003", new BigDecimal("4.00")));

    assertThat(support.sortRows(rows, "slot", "desc", List.of("C00003", "A00001")))
        .extracting(MonthlyEtfRowView::stockItemSymbol)
        .as("자리 차례대로, 추천에 없는 종목은 뒤 - 방향은 없다")
        .containsExactly("C00003", "A00001", "B00002");
    assertThat(support.resolveSort("slot")).isEqualTo(MonthlyEtfViewSupport.SORT_SLOT);
    assertThat(support.resolveSortForView("slot", "contribution")).isEqualTo("slot");
    assertThat(support.resolveSortForView("slot", ""))
        .as("전체 보기에는 자리가 없다 - 표시 순서로")
        .isEqualTo("display-order");
    assertThat(support.resolveSortForView("score", "")).isEqualTo("score");

    String controller = squash(Files.readString(Path.of(CONTROLLER_PATH), StandardCharsets.UTF_8));
    assertThat(controller)
        .contains(squash("monthlyEtfViewSupport.resolveSortForView(resolvedSort, resolvedView)"))
        .contains(squash("viewSort, resolvedDirection, List.copyOf(contributionPicks.keySet()));"))
        .as("머리글 aria-sort 가 실제 차례를 말해야 한다")
        .contains(squash("model.addAttribute(\"monthlyEtfSort\", viewSort);"));

    String template = squash(Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8));
    assertThat(template)
        .as("보기 단추는 자리 차례로 연다")
        .contains(squash("+ \"sort=slot&direction=asc\"}\""))
        .as("보기 안에 규칙(자리마다 하나 · 자리 차례 · 계좌 경계)을 적는다")
        .contains(
            squash(
                "data-contribution-view-desc>${MessageUtil.getMessage(\"stock.monthly.etf.view.contribution.desc\")}"));

    String korean =
        Files.readString(
            Path.of("src/main/resources/uiMessage_ko.properties"), StandardCharsets.UTF_8);
    for (String key :
        List.of(
            "stock.monthly.etf.table.cell.contribution",
            "stock.monthly.etf.view.contribution",
            "stock.simulator.monthly.contribution.title")) {
      String line = korean.substring(korean.indexOf(key + " = "));
      line = line.substring(0, line.indexOf((char) 10));
      assertThat(unescape(line))
          .as(key + " - 이번 적립이 아니라 자리별 추천이다")
          .contains("자리")
          .doesNotContain("이번");
    }
  }

  /** properties 의 역슬래시-u 네 자리 이스케이프를 글자로 푼다(백슬래시를 문자 코드로 적어 소스의 유니코드 이스케이프를 피한다). */
  private static String unescape(String value) {
    char slash = (char) 92;
    StringBuilder builder = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      char c = value.charAt(index);
      if (c == slash && index + 5 < value.length() && value.charAt(index + 1) == 'u') {
        builder.append((char) Integer.parseInt(value.substring(index + 2, index + 6), 16));
        index += 5;
      } else {
        builder.append(c);
      }
    }
    return builder.toString();
  }

  /**
   * 날짜는 한 덩어리(사용자 요청 2026-09-30: "날짜 줄바꿈도 날짜만 묶어서"). 1440px 에서 "2024-12-/03" 처럼 하이픈에서 갈렸다. 날짜 줄 전체를
   * 묶으면 표가 1134 → 1245px 로 넘쳐(실측) 날짜만 묶는다.
   */
  @Test
  void 표의_날짜는_날짜만_한_줄로_묶는다() throws IOException {
    String template = squash(Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8));
    String nowrap = "<span class=\"whitespace-nowrap date-whole\">";

    for (String date :
        List.of(
            "${String.valueOf(row.listingDate())}",
            "${row.currentPriceDate().toString()}",
            "${String.valueOf(row.riskFromDate())}",
            "${String.valueOf(row.periodBaseDate())}",
            "${String.valueOf(row.priceHistoryStartDate())}",
            "${row.latestPayDate().toString()}",
            "${row.lastVerifiedDate().toString()}")) {
      assertThat(template).as(date + " 가 한 덩어리가 아니다").contains(squash(nowrap + date + "</span>"));
    }
    // 날짜 바로 뒤 "부터" 는 빈칸 없이 붙어 "2026-06-23부터" 가 한 덩어리(118px)가 됐다 - 1440px 표가 상자를 36px
    // 넘쳤다(2026-09-30).
    // 날짜 뒤에 줄바꿈 자리(wbr)를 둔다. 날짜는 그대로 한 덩어리.
    for (String parts :
        List.of("listingDateParts", "riskFromParts", "periodBaseParts", "periodMissingParts")) {
      assertThat(template).as(parts).contains(squash("</span><wbr>${" + parts + "[1]}"));
    }
    assertThat(template)
        .as("날짜 앞뒤 낱말은 흘러야 표가 자리에 들어온다 - 문장을 표식으로 갈라 날짜만 묶는다")
        .contains(squash("return parts.length == 2 ? parts : new String[] { parts[0], \"\" };"))
        .doesNotContain(squash("<div class=\"whitespace-nowrap"));
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

  @Test
  void 분배금_추세를_주당_월배당금_칸에_적는다() throws IOException {
    // 사용자 결정 2026-09-22: 열을 하나 더 늘리는 대신 평균 · 최근 바로 밑에 셋째 줄로 붙인다(1440px 에서 표가 이미 넘친다).
    String template = squash(Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8));

    assertThat(template)
        .as("추세 줄이 주당 월배당금 칸 안에 있어야 한다 - 최근 줄 바로 뒤")
        .contains(
            squash(
                "${StockFormatUtil.fullKrw(row.latestDividendPerShare(), 2)}</span></div>"
                    + "<%--"));
    assertThat(template)
        .as("추세 값은 3 회 평균 금액과 증감률을 함께 적는다")
        // data-payout-trend 만 찾으면 data-payout-trend-missing 이 대신 걸려 값 칸이 사라져도 통과한다.
        .contains(squash("data-payout-trend title=\"${trendBasis}\">"))
        .contains(squash("${percentFormat.format(row.payoutTrendPct())}%"));
  }

  @Test
  void 이력이_모자라면_0퍼센트_대신_까닭을_적는다() throws IOException {
    // 0.00% 를 적으면 "분배금이 그대로다" 로 읽힌다 - 실제로는 견줄 이력이 없는 것이다.
    String template = squash(Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8));

    int at = template.indexOf(squash("@if(row.payoutTrendPct() != null"));
    assertThat(at).as("추세 판정을 못 찾았다 - 가드가 헛돈다").isGreaterThanOrEqualTo(0);
    int end = template.indexOf(squash("@endif"), at);
    assertThat(end).isGreaterThan(at);
    String block = template.substring(at, end);

    assertThat(block)
        .as("판정과 빈 값 처리를 한 덩이로 본다 - 한쪽만 잘리면 0% 가 새 나간다")
        .contains(
            squash("@if(row.payoutTrendPct() != null && row.averageDividendPerShare3m() != null)"))
        .contains(
            squash(
                "@else<span class=\"text-base-content/60\" data-payout-trend-missing>"
                    + "${trendUnknownLabel}</span>"));
  }

  @Test
  void 추세는_늘면_빨강_줄면_파랑_그대로면_중립이다() throws IOException {
    // 게이트 관례: 빨강 = 이득. 0 에 + 를 붙이면 이득처럼 읽힌다(signed-zero-reads-as-gain).
    String template = squash(Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8));

    assertThat(template)
        .as("0.00% 가 빨강이면 안 변한 값이 늘어난 것처럼 읽힌다 - 0 은 중립색")
        .contains(
            squash(
                "${row.payoutTrendPct().signum() > 0 ? \"text-profit\" : ("
                    + "row.payoutTrendPct().signum() < 0 ? \"text-loss\" : "
                    + "\"text-base-content/60\")}"))
        .contains(squash("${row.payoutTrendPct().signum() > 0 ? \"+\" : \"\"}"));
  }

  @Test
  void 손익_색은_게이트_관례_토큰을_쓴다() throws IOException {
    // text-profit(#bd2c38) · text-loss(#3159c4) 는 base-300 hover 표면에서도 AA 를 넘도록 고른 색이다.
    // DaisyUI 의 text-error · text-info 는 오류 · 정보를 뜻하는 다른 색이라 손익에 쓰면 안 된다
    // (RecentActivityAmountRuleTest 가 같은 규칙을 활동 카드에서 지킨다).
    String template = squash(Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8));

    for (String value :
        List.of(
            "row.payoutTrendPct()", "row.periodPriceReturnPct()", "row.periodTotalReturnPct()")) {
      assertThat(template)
          .as(value + " 가 게이트 손익 색을 안 쓴다")
          .contains(
              squash(
                  "${"
                      + value
                      + ".signum() > 0 ? \"text-profit\" : ("
                      + value
                      + ".signum() < 0 ? \"text-loss\" : \"text-base-content/60\")}"));
    }
    assertThat(template)
        .as("손익에 DaisyUI 의 오류 · 정보 색을 쓰면 안 된다")
        .doesNotContain("text-error")
        .doesNotContain("text-info");
  }

  @Test
  void 추세_문구가_두_로케일에_다_있다() throws IOException {
    String english =
        Files.readString(
            Path.of("src/main/resources/uiMessage.properties"), StandardCharsets.UTF_8);
    String korean =
        Files.readString(
            Path.of("src/main/resources/uiMessage_ko.properties"), StandardCharsets.UTF_8);

    for (String key :
        List.of(
            "stock.monthly.etf.table.cell.trend.short",
            "stock.monthly.etf.table.cell.trend.unknown",
            "stock.monthly.etf.table.cell.trend.title")) {
      assertThat(english).as(key + " 가 영어 메시지에 없다").contains(key + " =");
      assertThat(korean).as(key + " 는 한글도 ASCII 이스케이프라야 한다").contains(key + " = ");
    }
    assertThat(korean).contains("stock.monthly.etf.table.cell.trend.unknown = \\u");
  }

  /** 서식이 바뀌어도 뜻이 같으면 통과하게 - 공백을 전부 지우고 견준다. */
  private static String squash(String value) {
    StringBuilder builder = new StringBuilder(value.length());
    for (int index = 0; index < value.length(); index++) {
      char c = value.charAt(index);
      if (c != 32 && c != 9 && c != 10 && c != 13) {
        builder.append(c);
      }
    }
    return builder.toString();
  }

  /** 기간 합산 수익률만 다른 행(나머지는 같은 값). */
  /** 연배당 · 분배금 추세만 다른 행(추세 null = 이력 부족). */
  private static MonthlyEtfRowView rowWithScore(
      String symbol, String annualYieldPct, String payoutTrendPct) {
    MonthlyEtfRowView base = row(symbol, "이름 " + symbol, "MID_MONTH", annualYieldPct, 12, false);
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
        base.periodPriceReturnPct(),
        base.periodTotalReturnPct(),
        base.periodBaseDate(),
        base.averageDividendPerShare3m(),
        payoutTrendPct == null ? null : new BigDecimal(payoutTrendPct),
        base.maxDrawdownPct(),
        base.volatilityPct(),
        base.riskFromDate(),
        base.held(),
        null,
        null);
  }

  /** 과세표준 비중만 다른 행. null 은 "아직 모른다" 를 뜻한다. */
  private static MonthlyEtfRowView rowWithTaxable(String symbol, BigDecimal taxableRatioPct) {
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
        taxableRatioPct,
        base.averageTaxableBasePerShare1y(),
        base.currentPrice(),
        base.currentPriceDate(),
        base.monthlyYieldPct(),
        base.annualYieldPct(),
        base.priceHistoryStartDate(),
        base.periodPriceReturnPct(),
        base.periodTotalReturnPct(),
        base.periodBaseDate(),
        base.averageDividendPerShare3m(),
        base.payoutTrendPct(),
        base.maxDrawdownPct(),
        base.volatilityPct(),
        base.riskFromDate(),
        base.held(),
        null,
        null);
  }

  /** 위험 지표만 다른 행. null 은 "이력 부족" 을 뜻한다. */
  private static MonthlyEtfRowView rowWithRisk(
      String symbol, BigDecimal maxDrawdownPct, BigDecimal volatilityPct) {
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
        base.periodPriceReturnPct(),
        base.periodTotalReturnPct(),
        base.periodBaseDate(),
        base.averageDividendPerShare3m(),
        base.payoutTrendPct(),
        maxDrawdownPct,
        volatilityPct,
        base.riskFromDate(),
        base.held(),
        null,
        null);
  }

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
        base.averageDividendPerShare3m(),
        base.payoutTrendPct(),
        base.maxDrawdownPct(),
        base.volatilityPct(),
        base.riskFromDate(),
        base.held(),
        null,
        null);
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
        new BigDecimal("280.00"),
        new BigDecimal("7.69"),
        new BigDecimal("-12.34"),
        new BigDecimal("45.67"),
        LocalDate.of(2025, 9, 22),
        held,
        null,
        null);
  }
}
