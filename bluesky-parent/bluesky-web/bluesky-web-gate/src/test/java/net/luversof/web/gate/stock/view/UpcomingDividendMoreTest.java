package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * '다가올 배당' 카드는 종목을 <b>다섯 줄까지만</b> 보여 주는데, 그 위의 합계는 이 창의 <b>모든</b> 종목을 더한 값이다.
 *
 * <p>몇이 더 있는지 밝히지 않으면 합계가 안 맞는 것처럼 읽힌다. 같은 화면의 배분 막대는 이미 "기타 N개" 를 적고, 최근 활동 카드는 "전체보기 →" 를 둔다 - 이
 * 카드만 아무 말이 없었다.
 *
 * <p>실측 2026-09-13: 지금은 이 창에 4 종목뿐이라 합계 901,689 = 네 줄의 합(542,612 + 246,428 + 82,849 + 29,800)으로 딱
 * 맞는다. 다섯을 넘는 순간부터 어긋난다.
 *
 * <p>고정하는 것은 셋이다. (1) 컨트롤러가 가려진 수를 센다. (2) 0 이면 아무것도 그리지 않는다. (3) 문구가 두 로케일에 있다.
 */
class UpcomingDividendMoreTest {

  private static final String CONTROLLER =
      "src/main/java/net/luversof/web/gate/stock/controller/StockSummaryHtmxController.java";
  private static final String FRAGMENT = "src/main/jte/stock/htmx/fragments/upcomingDividends.jte";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  /**
   * 보여 준 다섯을 뺀 나머지다 - 전체 수를 그대로 적으면 다섯 줄이 두 번 세어진다.
   *
   * <p>{@code model.addAttribute(} 는 단언에서 뺀다 - 그 자리가 포매터의 줄바꿈 지점이라, 2026-09-13 외부 재포맷으로 한 줄이 되자 내용은
   * 그대로인데 가드만 깨졌다.
   */
  @Test
  void 가려진_수는_전체에서_보여_준_만큼_뺀_값이다() throws IOException {
    assertThat(read(CONTROLLER))
        .contains("\"upcomingDividendHiddenCount\", Math.max(0, windowRows.size() - 5))");
  }

  @Test
  void 가려진_것이_없으면_그리지_않는다() throws IOException {
    assertThat(read(FRAGMENT)).contains("@if(upcomingDividendHiddenCount > 0)");
  }

  @Test
  void 카드가_그_수를_적는다() throws IOException {
    String jte = read(FRAGMENT);

    assertThat(jte).contains("@param int upcomingDividendHiddenCount");
    assertThat(jte).contains("data-upcoming-more=\"${upcomingDividendHiddenCount}\"");
    assertThat(jte).contains("StockViewSupport.msg(\"stock.summary.upcoming.dividend.more\"");
  }

  /** 문구는 "합계는 모두 더한 값" 이라는 사실까지 말해야 한다 - 그래야 합계와 목록의 차이가 설명된다. */
  @Test
  void 문구가_두_로케일에_있고_합계를_설명한다() throws IOException {
    String ko =
        Files.readString(
            Path.of("src/main/resources/uiMessage_ko.properties"), StandardCharsets.UTF_8);
    String en =
        Files.readString(
            Path.of("src/main/resources/uiMessage.properties"), StandardCharsets.UTF_8);

    assertThat(valueOf(ko)).as("한글 값").isNotBlank();
    assertThat(valueOf(en).toLowerCase()).as("영문 값").contains("total");
  }

  /** 그 키의 값 한 줄만 떼어 낸다 - 파일 전체로 보면 다른 메시지에 걸린다. */
  private String valueOf(String properties) {
    int at = properties.indexOf("stock.summary.upcoming.dividend.more");
    if (at < 0) {
      return "";
    }
    return properties.substring(properties.indexOf("=", at) + 1, properties.indexOf((char) 10, at));
  }

  /**
   * 합계는 평균과 최근을 함께 낸다(2026-10-13 사용자 지적). 배당 내역의 다가올 배당은 같은 날을 최근으로만 더해 두 화면 금액이 달랐다 - 10월 17일분
   * 997,459 vs 1,601,445. 최근 계산은 배당 내역과 같은 함수를 쓰고, 둘 다 종목마다 원 단위로 반올림한 뒤 더한다(두 화면이 원 단위까지 같게).
   */
  @Test
  void 합계는_평균과_최근을_배당_내역과_같은_계산으로_낸다() throws IOException {
    String controller = read(CONTROLLER);
    assertThat(controller).contains("MonthlyDividendCalculator .latestMonthlyDividend(row)");
    assertThat(controller).contains("\"windowLatestTotal\", windowLatestTotal");
    assertThat(controller)
        .contains(
            "BigDecimal.valueOf( net.luversof.web.gate.stock.util.StockFormatUtil.displayWon( row.expectedMonthlyDividend()))");

    String jte = read(FRAGMENT);
    assertThat(jte).contains("@param BigDecimal windowLatestTotal");
    assertThat(jte).contains("data-window-latest-total");
  }

  /**
   * 최근이 우선이다(2026-10-13 사용자 선택 "최근을 우선으로"). 큰 숫자 · 라벨은 최근, 평균은 아래 작게, 목록도 최근 금액이 큰 순 · 최근 금액 - 배당
   * 내역 다가올 배당의 종목 줄과 같은 값 · 같은 순서.
   */
  @Test
  void 최근이_앞이고_목록도_최근_기준이다() throws IOException {
    assertThat(read(CONTROLLER)).contains("::latestMonthlyDividend) .reversed())");

    String jte = read(FRAGMENT);
    assertThat(jte).contains("StockViewSupport.msg(\"stock.summary.upcoming.window.latest\"");
    assertThat(jte).contains("data-window-average-total");
    assertThat(jte).contains("StockFormatUtil.displayWon(rowLatest)");
    assertThat(jte).doesNotContain("StockFormatUtil.displayWon(row.expectedMonthlyDividend())");
    assertThat(jte.indexOf("data-window-latest-total"))
        .isLessThan(jte.indexOf("data-window-average-total"));
  }
}
