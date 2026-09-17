package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 합계 줄의 첫 칸은 그 줄의 머리글이어야 한다({@code th scope="row"}).
 *
 * <p>표 안에서 숫자만 골라 듣는 보조기술은 열 머리글은 읽어 주지만, 첫 칸이 {@code td} 면 그 줄이 무엇인지("합계") 전하지 못한다. 실측 2026-09-11:
 * 배당 '기간별 집계' 두 표만 {@code th scope="row"} 를 쓰고(본문 7·37 행 전부 + 합계 줄), 합계 줄이 있는 나머지 표 여덟 개는 본문·합계 모두
 * 행 머리글이 0 이었다.
 *
 * <p>여기서는 <b>합계 줄</b> 만 맞춘다. 본문 행까지 바꾸는 것은 표마다 첫 칸의 성격이 달라(날짜·종목·계좌) 따로 볼 일이다.
 */
class TotalRowHeaderTest {

  private static final Path FRAGMENTS = Path.of("src/main/jte/stock/htmx/fragments");
  private static final Pattern TFOOT = Pattern.compile("<tfoot[^>]*>(.*?)</tfoot>", Pattern.DOTALL);

  private List<Path> templatesWithTotalRow() throws IOException {
    try (Stream<Path> walk = Files.walk(FRAGMENTS)) {
      List<Path> found = new ArrayList<>();
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        if (Files.readString(p, StandardCharsets.UTF_8).contains("<tfoot")) found.add(p);
      }
      return found;
    }
  }

  private int count(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }

  @Test
  void 합계_줄이_있는_표는_모두_행_머리글을_가진다() throws IOException {
    List<Path> templates = templatesWithTotalRow();
    assertThat(templates).as("합계 줄이 있는 조각을 하나도 찾지 못했다").isNotEmpty();

    List<String> offenders = new ArrayList<>();
    int checked = 0;
    for (Path p : templates) {
      Matcher m = TFOOT.matcher(Files.readString(p, StandardCharsets.UTF_8));
      while (m.find()) {
        checked++;
        String block = m.group(1);
        int firstTd = block.indexOf("<td");
        int firstTh = block.indexOf("<th");
        boolean headerFirst = firstTh >= 0 && (firstTd < 0 || firstTh < firstTd);
        if (!headerFirst || !block.substring(firstTh).startsWith("<th scope=\"row\"")) {
          offenders.add(p.getFileName().toString());
        }
      }
    }

    assertThat(checked).as("검사한 합계 줄 수").isGreaterThanOrEqualTo(14);
    assertThat(offenders).as("합계 줄 첫 칸이 th scope=\"row\" 가 아닌 조각").isEmpty();
  }

  /**
   * 집계 표(한 행 = 한 대상)는 본문 행도 첫 칸이 머리글이어야 한다.
   *
   * <p>실측 2026-09-11: 여기 적힌 표들은 첫 칸이 계좌명·종목명·기간이라 그 줄을 가리키는 이름이다. 거래·배당 목록(첫 칸이 날짜)은 한 칸이 줄을 가리키지
   * 못하므로 제외한다 &mdash; 날짜에 {@code scope="row"} 를 달면 같은 날짜가 여러 줄을 가리켜 오히려 틀린 말이 된다.
   *
   * <p>숫자는 그 조각이 가진 행 머리글 수다(본문 + 합계). 표가 늘거나 줄면 여기도 함께 고쳐, 새 표가 조용히 머리글 없이 들어오지 않게 한다.
   */
  @Test
  void 집계_표는_본문_행도_머리글을_가진다() throws IOException {
    String[][] targets = {
      {"trade/tradeRealizedSections.jte", "4"},
      {"trade/tradePeriodBreakdown.jte", "2"},
      {"stockContributionTable.jte", "3"},
      {"yearlyCostSummary.jte", "2"},
      {"assetGrowthYearlySummary.jte", "2"},
      {"periodBreakdownTable.jte", "2"},
      {"dividend/dividendYieldAnalytics.jte", "6"},
      {"dividend/dividendSummaryCards.jte", "1"},
      // 종목 표의 "보유 계좌 보기" 상세(2026-09-17)도 계좌 칸을 행 머리글로 둔다: 5 -> 6.
      {"assetStatus.jte", "6"},
      {"dividend/dividendPeriodBreakdown.jte", "4"},
    };

    for (String[] t : targets) {
      String template = Files.readString(FRAGMENTS.resolve(t[0]), StandardCharsets.UTF_8);
      assertThat(count(template, "<th scope=\"row\""))
          .as(t[0] + " 의 행 머리글 수")
          .isEqualTo(Integer.parseInt(t[1]));
    }
  }

  /** 거래·배당 목록은 첫 칸이 날짜라 행 머리글을 달지 않는다(같은 날짜가 여러 줄을 가리킨다). */
  @Test
  void 날짜로_시작하는_목록은_그대로_둔다() throws IOException {
    for (String name : new String[] {"trade/tradeDetailList.jte", "dividend/dividendTable.jte"}) {
      String template = Files.readString(FRAGMENTS.resolve(name), StandardCharsets.UTF_8);
      int body = template.indexOf("<tbody>");
      String block = template.substring(body, template.indexOf("</tbody>", body));
      assertThat(block).as(name + " 의 목록 행에 행 머리글을 달면 안 된다").doesNotContain("<th scope=\"row\"");
    }
  }
}
