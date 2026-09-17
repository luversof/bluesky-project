package net.luversof.web.gate.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 열을 펴는 분기점은 그 열이 실제로 들어가는 폭이어야 한다.
 *
 * <p>배당 효율 랭킹 표는 11열이라 2xl(1536px)에서 펴면 표가 1,380px 인데 본문 열은 1,246px 뿐이라 가로 스크롤이 생겼다. 실측 2026-09-10:
 * 1536px 넘침 134px · 1600px 70px · 1660px 10px · 1680px 0px. 1536px 은 1920x1080 을 125% 배율로 쓸 때의 폭이라
 * 흔하다.
 *
 * <p>1,535px 에서는 6열만 보여 멀쩡하다가 1,536px 에서 갑자기 가로 스크롤이 생기는 것이 더 나쁘므로, 들어가는 폭에서 편다.
 */
class TableColumnBreakpointTest {

  private static final Path YIELD_ANALYTICS =
      Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte");

  private static final String SORTABLE_TABLE = "data-dividend-yield-sortable-table";

  private static List<String> linesFrom(String source, int from) {
    List<String> out = new ArrayList<>();
    for (String line : source.substring(from).split("\n")) {
      out.add(line);
    }
    return out;
  }

  @Test
  void rankingTablesRevealColumnsWhereTheyFit() throws IOException {
    String source = Files.readString(YIELD_ANALYTICS, StandardCharsets.UTF_8);
    int firstRanking = source.indexOf(SORTABLE_TABLE);

    assertThat(firstRanking).as("정렬 가능한 랭킹 표를 찾지 못했다").isGreaterThan(0);

    long tooEarly =
        linesFrom(source, firstRanking).stream().filter(l -> l.contains("2xl:table-cell")).count();

    assertThat(tooEarly).as("11열 랭킹 표의 열을 2xl(1536px)에서 펴면 본문 열보다 134px 넓어져 가로 스크롤이 생긴다").isZero();
  }

  @Test
  void rankingTablesStillHaveTheWideOnlyColumns() throws IOException {
    String source = Files.readString(YIELD_ANALYTICS, StandardCharsets.UTF_8);
    int firstRanking = source.indexOf(SORTABLE_TABLE);

    long wide =
        linesFrom(source, firstRanking).stream()
            .filter(l -> l.contains("min-[1150px]:table-cell"))
            .count();

    assertThat(wide).as("넓은 화면에서만 펴는 열이 사라지면 그 값들을 어디서도 볼 수 없다").isGreaterThanOrEqualTo(20);
  }

  /**
   * 매매 상세 표의 수수료·거래세 열. lg(1024px)에서 폈는데 그 폭에서 사이드바(224px)가 함께 나타나 본문 열이 991px -&gt; 768px 로 줄어든다.
   * 실측 2026-09-10: 1023px 은 8열 · 표 991px · 넘침 0 인데 1024px 은 10열 · 표 1,033px · 컨테이너 768px 로 넘침 265px
   * 이 됐다(1100px 189 · 1200px 89 · 1280px 9 · 1300px 0). 넓혔는데 더 나빠지는 구간이라 들어가는 폭에서 편다.
   */
  @Test
  void tradeDetailRevealsFeeColumnsWhereTheyFit() throws IOException {
    String source =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/trade/tradeDetailList.jte"),
            StandardCharsets.UTF_8);

    assertThat(source.lines().filter(l -> l.contains("lg:table-cell")).count())
        .as("lg(1024px)에서 펴면 사이드바가 같이 나타나 본문이 좁아져 265px 넘친다")
        .isZero();
    assertThat(source.lines().filter(l -> l.contains("min-[1300px]:table-cell")).count())
        .as("수수료·거래세 열이 사라지면 그 값을 어디서도 볼 수 없다")
        .isEqualTo(6);
  }

  /**
   * 자산 현황의 종목별 표(11열). 평균 단가·실현 손익·누적 배당 열을 xl(1280px)에서 폈는데 그 폭에서 표는 1,125px 이 필요하고 본문 열은 1,022px
   * 뿐이다. 실측 2026-09-10: 1279px 8열·표 1,021·넘침 0 → 1280px 11열·표 1,125·넘침 103 → 1366px 17 → 1400px 0.
   * 같은 파일의 계좌별 보유 상세 표(47열)는 xl 에서 들어가므로 그대로 둔다.
   */
  @Test
  void stockHoldingsTableRevealsColumnsWhereTheyFit() throws IOException {
    Path assetStatus = Path.of("src/main/jte/stock/htmx/fragments/assetStatus.jte");
    String source = Files.readString(assetStatus, StandardCharsets.UTF_8);
    int table = source.indexOf("data-asset-status-stock-table");

    assertThat(table).as("종목별 자산 현황 표를 찾지 못했다").isGreaterThan(0);

    String stockTable = source.substring(table);
    // 종목 줄은 "보유 계좌 보기" 로 펼칠 표를 안에 품는다(2026-09-17). 그 표는 계좌 표 안의 보유 상세 표처럼 제 분기점을 쓰고
    // 펼친 줄 안에서 가로로 스크롤되므로, 바깥 표의 열만 센다 - 짝을 세어 바깥 표 끝을 찾고 안쪽 표는 빼고 본다.
    int depth = 1;
    int at = 0;
    StringBuilder outer = new StringBuilder();
    int copyFrom = 0;
    int endOfTable = -1;
    while (depth > 0) {
      int open = stockTable.indexOf("<table", at);
      int close = stockTable.indexOf("</table>", at);
      assertThat(close).as("표의 끝을 찾지 못했다").isGreaterThan(0);
      if (open >= 0 && open < close) {
        if (depth == 1) {
          outer.append(stockTable, copyFrom, open);
        }
        depth++;
        at = open + 1;
      } else {
        depth--;
        at = close + "</table>".length();
        if (depth == 1) {
          copyFrom = at;
        } else if (depth == 0) {
          outer.append(stockTable, copyFrom, close);
          endOfTable = close;
        }
      }
    }

    assertThat(endOfTable).as("표의 끝을 찾지 못했다").isGreaterThan(0);

    String body = outer.toString();

    assertThat(body.lines().filter(l -> l.contains("xl:table-cell")).count())
        .as("xl(1280px)에서 펴면 본문 열보다 103px 넓어져 가로 스크롤이 생긴다")
        .isZero();
    assertThat(body.lines().filter(l -> l.contains("min-[1340px]:table-cell")).count())
        .as("평균 단가·실현 손익·누적 배당 열이 사라지면 그 값을 어디서도 볼 수 없다")
        .isEqualTo(9);
  }

  /**
   * 자산 현황의 계좌별 표(7열). 투자원금 열을 md(768px)에서 폈는데 그 폭에서 표는 813px 이 필요하고 본문 열은 734px 뿐이다. 실측 2026-09-10:
   * 767px 6열·표 733·넘침 0 → 768px 7열·표 813·컨 734·넘침 79 → 820px 27 → 845px 2 → 850px 0.
   *
   * <p>안쪽의 계좌별 보유 상세 표는 별도 분기점을 쓰므로 함께 옮기면 안 된다 &mdash; 그 표는 md 에서 들어간다.
   */
  @Test
  void accountStatusTableRevealsPrincipalWhereItFits() throws IOException {
    Path assetStatus = Path.of("src/main/jte/stock/htmx/fragments/assetStatus.jte");
    String source = Files.readString(assetStatus, StandardCharsets.UTF_8);
    int accountTable = source.indexOf("data-asset-status-account-table");
    int nested = source.indexOf("<table", accountTable + 1);

    assertThat(accountTable).as("계좌별 자산 현황 표를 찾지 못했다").isGreaterThan(0);
    assertThat(nested).as("안쪽 보유 상세 표를 찾지 못했다").isGreaterThan(accountTable);

    String outerHead = source.substring(accountTable, nested);

    assertThat(outerHead.lines().filter(l -> l.contains("md:table-cell")).count())
        .as("md(768px)에서 펴면 본문 열보다 79px 넓어져 가로 스크롤이 생긴다")
        .isZero();
    assertThat(source.lines().filter(l -> l.contains("min-[850px]:table-cell")).count())
        .as("투자원금 열이 사라지면 그 값을 어디서도 볼 수 없다")
        .isEqualTo(3);
    assertThat(source.lines().filter(l -> l.contains("md:table-cell")).count())
        .as("안쪽 보유 상세 표(계좌 표 · 2026-09-17 종목 표의 보유 계좌 상세)의 md 분기점까지 옮기면 넓은 화면에서 괜히 열이 줄어든다")
        .isEqualTo(4);
  }

  /**
   * 분기점을 넓은 쪽으로 옮기면 인쇄가 같이 좁아진다 &mdash; 인쇄 폭은 A4 794px 이라 850px 아래다. 실측 2026-09-10: 투자원금 열이 인쇄에서 빠져
   * 7열 → 6열이 됐다.
   *
   * <p>인쇄에는 스크롤이 없어 열이 좁아질 뿐 표 폭은 760px 로 같고 넘침도 0 이므로, 인쇄에서는 항상 보여 준다.
   */
  @Test
  void printKeepsTheColumnTheScreenHidesAtNarrowWidths() throws IOException {
    String source =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/assetStatus.jte"), StandardCharsets.UTF_8);

    long screenOnly =
        source
            .lines()
            .filter(l -> l.contains("min-[850px]:table-cell"))
            .filter(l -> !l.contains("print:table-cell"))
            .count();

    assertThat(screenOnly)
        .as("인쇄 폭(794px)이 850px 아래라 print:table-cell 이 없으면 인쇄에서 열이 사라진다")
        .isZero();
  }

  @Test
  void narrowerTablesKeepTheirOwnBreakpoint() throws IOException {
    String source = Files.readString(YIELD_ANALYTICS, StandardCharsets.UTF_8);
    int firstRanking = source.indexOf(SORTABLE_TABLE);
    String before = source.substring(0, firstRanking);

    long yearly = before.lines().filter(l -> l.contains("md:table-cell")).count();

    assertThat(yearly)
        .as("10열인 연도별 표는 헤더 접기 뒤 768px 부터 들어간다 - 랭킹 표와 같은 분기점으로 묶으면 괜히 열이 줄어든다")
        .isGreaterThan(0);
  }
}
