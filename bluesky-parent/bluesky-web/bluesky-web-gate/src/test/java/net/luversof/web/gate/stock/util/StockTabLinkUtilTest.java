package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 같은 페이지 탭 링크는 탭만 바꾼다.
 *
 * <p>실측 2026-09-12: 배당 화면의 두 탭은 {@code /stock/dividend?tab=history} 처럼 탭만 적고 있었다. {@code
 * ?rangeMode=all&accountIdList=...} 로 들어와 한 계좌만 보던 화면에서 탭을 누르면 상세 목록이 <b>40 건에서 202 건(전체)</b> 으로 늘고
 * 계좌 선택도 비었다 - 필터 표시는 그대로라 화면만 봐서는 알 수 없다.
 */
class StockTabLinkUtilTest {

  @Test
  void 조건은_그대로_두고_탭만_바꾼다() {
    assertThat(
            StockTabLinkUtil.tabHref(
                "/stock/dividend",
                "rangeMode=all&accountIdList=A1&accountIdList=A2&locale=ko_KR&tab=history",
                "calendar"))
        .isEqualTo(
            "/stock/dividend?rangeMode=all&accountIdList=A1&accountIdList=A2&locale=ko_KR&tab=calendar");
  }

  @Test
  void 같은_이름이_여러_번_오는_값의_순서를_지킨다() {
    assertThat(
            StockTabLinkUtil.tabHref(
                "/stock/dividend", "accountIdList=B&accountIdList=A", "history"))
        .as("다시 묶으면 순서가 바뀔 수 있다")
        .isEqualTo("/stock/dividend?accountIdList=B&accountIdList=A&tab=history");
  }

  @Test
  void 인코딩된_값을_다시_인코딩하지_않는다() {
    assertThat(StockTabLinkUtil.tabHref("/stock/dividend", "timeZone=Asia%2FSeoul", "calendar"))
        .isEqualTo("/stock/dividend?timeZone=Asia%2FSeoul&tab=calendar");
  }

  @Test
  void 옛_탭_값은_남기지_않는다() {
    assertThat(StockTabLinkUtil.tabHref("/stock/dividend", "tab=calendar&x=1", "history"))
        .isEqualTo("/stock/dividend?x=1&tab=history");
    assertThat(StockTabLinkUtil.tabHref("/stock/dividend", "tab&x=1", "history"))
        .as("값 없는 tab 도 옛 탭이다")
        .isEqualTo("/stock/dividend?x=1&tab=history");
  }

  @Test
  void 질의가_없으면_탭만_붙인다() {
    assertThat(StockTabLinkUtil.tabHref("/stock/dividend", null, "history"))
        .isEqualTo("/stock/dividend?tab=history");
    assertThat(StockTabLinkUtil.tabHref("/stock/dividend", "", "calendar"))
        .isEqualTo("/stock/dividend?tab=calendar");
  }

  /**
   * 시뮬레이터 탭도 같은 규칙을 쓴다.
   *
   * <p>실측 2026-09-12: 월배당 표를 종목코드로 정렬(sort=symbol&direction=asc)한 뒤 다른 탭에 갔다가 돌아오면 주소가 {@code
   * /stock/simulator?tab=monthly-dividend} 로 돌아가 정렬이 풀리고 기본 순서가 됐다.
   */
  @Test
  void 시뮬레이터_탭도_조건을_들고_간다() throws IOException {
    String jte =
        Files.readString(Path.of("src/main/jte/stock/simulator.jte"), StandardCharsets.UTF_8);
    char q = (char) 34;
    for (String attr :
        new String[] {
          "simulatorTabHrefSustainability",
          "simulatorTabHrefMonthlyDividend",
          "simulatorTabHrefCompound"
        }) {
      assertThat(jte).as(attr + " 를 써야 한다").contains("href=" + q + "${" + attr + "}" + q);
    }
    assertThat(jte)
        .as("고정 주소가 남아 있으면 그 탭만 조건을 잃는다")
        .doesNotContain("href=" + q + "/stock/simulator?tab=");

    String controller =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java"),
            StandardCharsets.UTF_8);
    assertThat(controller).contains("StockTabLinkUtil.tabHref(");
    assertThat(controller).contains("request.getQueryString()");
  }

  /** 템플릿이 이 주소를 실제로 쓰는지. 안 쓰면 유틸만 맞고 화면은 그대로다. */
  @Test
  void 템플릿이_고정_주소를_쓰지_않는다() throws IOException {
    String jte =
        Files.readString(Path.of("src/main/jte/stock/dividend.jte"), StandardCharsets.UTF_8);
    char q = (char) 34;
    assertThat(jte).doesNotContain("href=" + q + "/stock/dividend?tab=");
    assertThat(jte).contains("dividendHistoryTabHref");
    assertThat(jte).contains("dividendCalendarTabHref");
  }
}
