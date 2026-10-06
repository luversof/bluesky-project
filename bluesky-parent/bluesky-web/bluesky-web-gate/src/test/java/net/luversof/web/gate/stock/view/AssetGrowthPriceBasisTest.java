package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 자산 성장의 "평가 기준 YYYY-MM-DD 종가" 안내가 실제로 <b>뜨는지</b> 고정한다.
 *
 * <p>템플릿에는 이미 있었지만 값이 늘 {@code null} 이라 다섯 기간 &middot; 필터 조합 어디에서도 나오지 않았다. 원인은 기준일을 <b>기간을 건</b>
 * 손익 응답에서 찾은 것 &mdash; 실측 2026-09-11(같은 엔드포인트): 기간을 주면 {@code currentPrice} 와 {@code
 * evaluationAmount} 가 0 이고 {@code currentPriceDate} 는 10 행 모두 null 인데, 기간 없이 부르면 43 행 전부 날짜가 있고 보유
 * 종목의 마지막 종가는 2026-09-09 다.
 *
 * <p>그래서 기준일 전용으로 <b>기간 없는</b> 호출을 병렬로 하나 더 던진다(실측 28~39ms).
 */
class AssetGrowthPriceBasisTest {

  private static final Path CONTROLLER =
      Path.of(
          "src/main/java/net/luversof/web/gate/stock/controller/StockAssetGrowthHtmxController.java");
  private static final Path TEMPLATE = Path.of("src/main/jte/stock/htmx/asset-growth.jte");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 기준일은_기간_없는_호출에서_얻는다() throws IOException {
    String controller = read(CONTROLLER);

    int at = controller.indexOf("priceBasisRequest");
    assertThat(at).as("기준일 전용 요청이 있어야 한다").isGreaterThan(0);
    String block = controller.substring(at, controller.indexOf("priceBasisParams", at));
    assertThat(block)
        .as("기간을 실으면 currentPriceDate 가 전부 null 이라 안내가 사라진다 - 명시로 떨어낸다")
        .contains("setStartDate(null)")
        .contains("setEndDate(null)");
    assertThat(block)
        .as("화면 필터는 그대로 따라가야 한다")
        .contains("setAccountIdList")
        .contains("setStockItemIdList");
  }

  @Test
  void 기준일을_모델에_싣고_화면이_쓴다() throws IOException {
    String controller = read(CONTROLLER);
    String template = read(TEMPLATE);

    // 인자가 옛 목록(stockRealizedList)으로 돌아가면 다시 늘 null 이 된다 - 호출의 인자까지 못박는다.
    String marker = "priceBasisDateWithFallback(";
    int call = controller.indexOf(marker);
    assertThat(call).isGreaterThan(0);
    assertThat(controller.substring(call + marker.length()).strip())
        .startsWith("priceBasisHoldings");
    assertThat(template).contains("@param java.time.LocalDate priceBasisDate");
    // 문구 틀은 StockPriceBasisUtil.basisMessage 가 고른다(종가 / 장중 시세, 2026-10-02).
    assertThat(template).contains("StockPriceBasisUtil.basisMessage(priceBasisIntradayTime)");
  }
}
