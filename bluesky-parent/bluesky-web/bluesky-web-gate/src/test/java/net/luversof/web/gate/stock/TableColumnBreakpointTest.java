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

  /**
   * 2026-09-28 사용자 요청("다양한 수익률과 투입원금이 잘 눈에 안 들어온다")으로 랭킹 표의 예전 열(세전 · 세금 · 과세 · 기간 일평균 투입원금 · 기준일
   * 지표 8 열)은 폭에 따라 펴는 대신 "열 더 보기" 를 켜야 나온다. 그 열들이 사라지면 값을 어디서도 볼 수 없으므로 여전히 지킨다 - 열 표시가 남아 있고, 표마다
   * 토글이 있고, 토글과 인쇄가 그 열을 펴는 규칙이 빌드 산출물에 있어야 한다.
   */
  @Test
  void rankingTablesStillHaveTheWideOnlyColumns() throws IOException {
    String source = Files.readString(YIELD_ANALYTICS, StandardCharsets.UTF_8);
    int firstRanking = source.indexOf(SORTABLE_TABLE);

    long extra =
        linesFrom(source, firstRanking).stream().filter(l -> l.contains("yield-extra-col")).count();
    assertThat(extra).as("랭킹 표 두 개의 머리 · 본문 · 합계 x 8 열").isGreaterThanOrEqualTo(40);
    assertThat(source.split("data-yield-extra-toggle>", -1)).as("표 세 개에 토글 하나씩").hasSize(3 + 1);
    assertThat(source.split("data-yield-extra-scope", -1)).as("토글이 켜는 범위 세 개").hasSize(3 + 1);

    String built =
        Files.readString(Path.of("src/main/resources/static/main.css"), StandardCharsets.UTF_8);
    assertThat(built)
        .contains(
            "[data-yield-extra-scope]:has([data-yield-extra-toggle]:checked) .yield-extra-col{display:table-cell}")
        .contains(".yield-extra-col{display:none}");
    // 2026-09-30: 인쇄도 화면과 같다. "인쇄에서는 모든 열" 이던 때 16 열이 1,162px 로 지면(750px)을 넘어 오른쪽 열이 잘렸다 -
    // 켜는 규칙(:has 토글) 하나만 남아야 한다.
    assertThat(
            built.split(java.util.regex.Pattern.quote(".yield-extra-col{display:table-cell}"), -1))
        .as("추가 열을 켜는 규칙은 토글 하나뿐(인쇄에서 늘 켜면 표가 지면 밖으로 잘린다)")
        .hasSize(2);
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

    // 2026-09-28 부터 연도별 표도 예전 열은 "열 더 보기" 로 편다(분기점 대신). 열 표시가 남아 있는지만 지킨다.
    long yearly = before.lines().filter(l -> l.contains("yield-extra-col")).count();

    assertThat(yearly).as("연도별 표의 예전 열(머리 · 본문 · 합계)").isGreaterThanOrEqualTo(15);
  }
}
