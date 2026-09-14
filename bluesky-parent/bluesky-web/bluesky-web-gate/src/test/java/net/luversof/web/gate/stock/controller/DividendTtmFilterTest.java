package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 배당 월별 차트의 "최근 12개월 합"(TTM) 선도 계좌·종목 필터를 따라야 한다.
 *
 * <p>이 선은 표시 기간 <b>밖</b>의 달까지 필요해서 날짜로 자르지 않은 원장으로 만든다. 그런데 계좌·종목 필터까지 무시하고 있었다 &mdash; 막대는 필터를
 * 따르는데 선만 전체 포트폴리오를 그렸다.
 *
 * <p>실측 2026-09-10: 삼성SDI 로 걸면 막대 합 298,640 인데 TTM 선은 11,427,786 (올바른 값 131,130) 이었고 61 개 점이 전부
 * 틀렸다. 에스디바이오센서는 20,364 자리에 2,678,912 &mdash; 전체 원장의 2022-04 TTM 값과 정확히 같았다. 필터가 없을 때는 78 개 점이 모두
 * 맞아서 눈에 띄지 않았다.
 */
class DividendTtmFilterTest {

  private static final Path CONTROLLER =
      Path.of(
          "src/main/java/net/luversof/web/gate/stock/controller/StockDividendHtmxController.java");

  @Test
  void ttm은_필터를_거친_원장으로_만든다() throws IOException {
    List<String> lines = Files.readAllLines(CONTROLLER, StandardCharsets.UTF_8);

    int at = -1;
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).contains("StockDividendTtmUtil.byMonth(")) {
        at = i;
        break;
      }
    }
    assertThat(at).as("TTM 생성 지점을 찾지 못했다").isGreaterThan(0);

    String argument = lines.get(at + 1).strip();

    assertThat(argument)
        .as("allDividends 를 그대로 넘기면 막대만 필터를 따르고 선은 전체 포트폴리오를 그린다")
        .doesNotStartWith("allDividends");
    assertThat(argument).startsWith("ttmSource");
  }

  @Test
  void ttm_원장은_계좌와_종목_필터를_모두_건다() throws IOException {
    String source = Files.readString(CONTROLLER, StandardCharsets.UTF_8);

    int at = source.indexOf("List<DividendResponse> ttmSource =");
    assertThat(at).as("ttmSource 를 만들지 않는다").isGreaterThan(0);

    String block = source.substring(at, source.indexOf(".toList();", at));

    assertThat(block).contains("matchesFilter(effectiveAccountIdList, d.accountId())");
    assertThat(block).contains("matchesFilter(effectiveStockItemIdList, d.stockItemId())");
  }

  @Test
  void ttm은_표시_기간으로_자르지_않는다() throws IOException {
    String source = Files.readString(CONTROLLER, StandardCharsets.UTF_8);

    int at = source.indexOf("List<DividendResponse> ttmSource =");
    String block = source.substring(at, source.indexOf(".toList();", at));

    assertThat(block)
        .as("선은 표시 기간 밖의 달까지 필요하다 - 날짜로 자르면 12개월 창이 잘린다")
        .doesNotContain("inPayDateRange")
        .doesNotContain("startInstant");
  }
}
