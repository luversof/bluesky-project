package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 날짜는 한 덩어리(사용자 요청 2026-09-30: "날짜 줄바꿈도 날짜만 묶어서 고쳐줘").
 *
 * <p>좁은 칸 · 큰 글꼴에서 "2026-09-/17" 처럼 하이픈에서 갈렸다. 주식 화면 전수(_date-break-sweep.js: 날짜 7,770 개 x 폭 5 x 글꼴
 * 2)에서 월배당 ETF 표 밖에도 11 곳 23 건 - 기준일 범위 · 전기 대비 · 올해 · 최대 낙폭 · 기간 배지 · 관리 최신 시점 · 월 머리칸. 날짜 줄 전체를
 * 묶으면 칸이 넘치니(월배당 ETF 표 1134 → 1245px) 날짜만 묶고 물결표 · 앞뒤 낱말은 흐르게 둔다.
 *
 * <p>가드: 주식 템플릿에서 두 식을 물결표 · 화살표로 바로 잇는 모양(날짜 범위를 이어 붙이던 모양)이 없어야 한다 - 이으려면 양쪽을 nowrap 상자에 담는다.
 */
class DateStaysWholeTest {

  private static final Path STOCK_JTE = Path.of("src/main/jte/stock");

  private static final String NOWRAP = "<span class=\"whitespace-nowrap date-whole\">";

  /** 공백을 모두 지운다 - 서식이 바뀌어도 뜻으로 견준다. */
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

  private static String read(String relative) throws IOException {
    return squash(Files.readString(STOCK_JTE.resolve(relative), StandardCharsets.UTF_8));
  }

  @Test
  void 주식_템플릿에_날짜를_맨몸으로_잇는_곳이_없다() throws IOException {
    List<String> bare = new ArrayList<>();
    List<Path> files;
    try (Stream<Path> stream = Files.walk(STOCK_JTE)) {
      files = stream.filter(path -> path.toString().endsWith(".jte")).toList();
    }
    assertThat(files).as("템플릿을 못 찾았다 - 경로가 바뀌었다").hasSizeGreaterThan(50);
    for (Path file : files) {
      String source = squash(Files.readString(file, StandardCharsets.UTF_8));
      for (String joint : List.of("}~${", "}&#8594;${", "\"~\"+")) {
        if (source.contains(joint)) {
          bare.add(STOCK_JTE.relativize(file) + " : " + joint);
        }
      }
    }
    assertThat(bare).as("두 식을 바로 이으면 하이픈에서 갈린다 - 날짜를 nowrap 상자에 담을 것").isEmpty();
  }

  /**
   * flex 상자(배지 · inline-flex 알약) 안에 날짜 상자를 바로 넣으면 글자와 날짜가 따로 flex 칸이 되어 줄을 못 바꾼다(실측 2026-09-30
   * 320px/200%: 기간 배지 391px 로 10 화면이 75~161px 넘침). 날짜 상자를 담은 flex 상자는 안쪽을 한 상자(min-w-0)로 감싼다.
   */
  @Test
  void flex_상자_안_날짜는_한_상자에_담아_줄을_바꿀_수_있게_한다() throws IOException {
    java.util.regex.Pattern bare =
        java.util.regex.Pattern.compile(
            "class=\"[^\"]*(?:badge|inline-flex)[^\"]*\"[^>]*>(?!<spanclass=\"min-w-0\">)[^<]*<spanclass=\"whitespace-nowrap(?:date-whole)?\">\\$\\{[^}]*(?:[Dd]ate|fromDate|toDate)");
    List<String> found = new ArrayList<>();
    try (Stream<Path> stream = Files.walk(STOCK_JTE)) {
      for (Path file : stream.filter(path -> path.toString().endsWith(".jte")).toList()) {
        java.util.regex.Matcher matcher =
            bare.matcher(squash(Files.readString(file, StandardCharsets.UTF_8)));
        while (matcher.find()) {
          found.add(STOCK_JTE.relativize(file) + " : " + matcher.group());
        }
      }
    }
    assertThat(found).as("flex 상자에 날짜 상자가 바로 들어 있다").isEmpty();
  }

  /**
   * 종이에서도 날짜는 한 덩어리. 인쇄 규칙이 모든 whitespace-nowrap 을 풀어(넓은 표를 지면에 맞추려고) 날짜 상자까지 "2026-09-/30" 으로
   * 갈렸다(실측 2026-09-30: 배당 내역 42 · 배당 캘린더 28 곳). 날짜 상자만 date-whole 로 되살린다 - 산출물에서도 본다.
   */
  @Test
  void 인쇄에서도_날짜는_한_덩어리다() throws IOException {
    String css =
        Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8)
            .replaceAll("\\s+", " ");
    int release =
        css.indexOf(
            ".whitespace-nowrap, table .whitespace-nowrap { white-space: normal !important; }");
    int keep = css.indexOf(".date-whole { white-space: nowrap !important; }");
    assertThat(release).as("인쇄 해제 규칙").isGreaterThan(0);
    assertThat(keep).as("해제 뒤에 와야 이긴다(같은 !important)").isGreaterThan(release);
    assertThat(css).contains("table .amount-value { white-space: normal !important; }");
    assertThat(
            Files.readString(Path.of("src/main/resources/static/main.css"), StandardCharsets.UTF_8))
        .contains(".date-whole{white-space:nowrap!important}");
    assertThat(read("htmx/fragments/dividend/dividendTable.jte"))
        .contains(squash("<td class=\"whitespace-nowrap date-whole\">${item.payDate()"));
  }

  /** 종목 · 계좌 상세(가장 긴 표본): 매매 · 배당 날짜 칸과 연평균 카드의 "최초 매수 {날짜}". */
  @Test
  void 상세_화면의_날짜_칸과_카드_날짜도_한_덩어리다() throws IOException {
    for (String detail :
        List.of("htmx/stockItemDetailContent.jte", "htmx/accountDetailContent.jte")) {
      String source = read(detail);
      assertThat(source)
          .as(detail + " 매매 날짜 칸")
          .contains(
              squash(
                  "<td class=\"tabular-nums whitespace-nowrap date-whole\">${trade.tradeDate() != null"))
          .as(detail + " 배당 날짜 칸")
          .contains(
              squash(
                  "<td class=\"tabular-nums whitespace-nowrap date-whole\">${dividend.payDate() != null"))
          .as(detail + " 카드 보조줄은 문장을 갈라 날짜를 subDate 로 넘긴다")
          .contains(squash("subDate ="))
          .doesNotContain(
              squash(
                  "MessageUtil.getMessage(\"stock.asset.status.cell.holding.since\"), accountFirstTradeDate.toString())"))
          .doesNotContain(
              squash(
                  "MessageUtil.getMessage(\"stock.asset.status.cell.holding.since\"), holdingFirstBuyDate.toString())"));
    }
    assertThat(
            squash(
                Files.readString(
                    Path.of("src/main/jte/_components/ui/statCard.jte"), StandardCharsets.UTF_8)))
        .as("카드 보조줄 날짜 상자")
        .contains(squash(NOWRAP + "${subDate}</span>${subDateAfter}"));
  }

  /** 전수에서 갈렸던 자리마다 날짜가 상자에 들어 있다. */
  @Test
  void 갈렸던_자리의_날짜는_상자에_들어_있다() throws IOException {
    assertThat(read("fragments/monthlyDividendSimulator.jte"))
        .contains(squash(NOWRAP + "${oldestAsOfDate.toString()}</span>"))
        .contains(squash(NOWRAP + "${newestAsOfDate.toString()}</span>"));
    assertThat(read("fragments/upcomingDividendSchedule.jte"))
        .contains(squash(NOWRAP + "${asOfNewest.toString()}</span>"));
    assertThat(read("htmx/fragments/upcomingDividends.jte"))
        .contains(squash(NOWRAP + "${dividendAsOfNewest.toString()}</span>"));
    assertThat(read("htmx/fragments/dividend/dividendYieldAnalytics.jte"))
        .contains(squash("(" + NOWRAP + "${ttmStartDate.toString()}</span> ~ " + NOWRAP));
    assertThat(read("htmx/fragments/dividend/dividendSummaryCards.jte"))
        .contains(squash("vs " + NOWRAP + "${prevStartDate.toString()}</span>"));
    assertThat(read("htmx/fragments/summary.jte"))
        .contains(squash(NOWRAP + "${periodSummaryFrom.toString()}</span>"));
    assertThat(read("htmx/fragments/components/dateRangeNavBar.jte"))
        .contains(squash("${allLabel} · " + NOWRAP));
    assertThat(read("htmx/fragments/assetGrowthPeriodReturnSummary.jte"))
        .contains(squash(NOWRAP + "${maxDrawdownPeakDate.toString()}</span> &#8594; " + NOWRAP));
    assertThat(read("htmx/asset-growth.jte"))
        .as("평가 기준 {0} 종가 - 문장을 갈라 날짜만 담는다")
        .contains(squash(NOWRAP + "${priceBasisDate.toString()}</span>"))
        .doesNotContain(squash(".replace(\"{0}\", priceBasisDate.toString())"));
    assertThat(read("htmx/fragments/adminActions.jte"))
        .contains(squash(NOWRAP + "${asDate.apply(dataStatus.tradeLastDate())}</span>"))
        .contains(squash("${withCount.apply(\"\", dataStatus.tradeCount())}"));
    assertThat(read("htmx/fragments/dividend/dividendPeriodBreakdown.jte"))
        .as("월 머리칸 2026-09")
        .contains(
            squash(
                "<th scope=\"row\" class=\"font-medium whitespace-nowrap date-whole\">${row.label()}</th>"));
    assertThat(read("htmx/fragments/dividend/dividendTable.jte"))
        .as("배당 상세 목록(접힌 구역, 211 행) 지급일 칸 - 펼치면 1024~1440px 에서 211 개가 전부 \"2025-/10-17\" 로 갈렸다")
        .contains(
            squash(
                "<td class=\"whitespace-nowrap date-whole\">${item.payDate() != null ? dateFormatter.format("));
  }
}
