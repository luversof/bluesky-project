package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 평가액을 머리에 띄우는 화면이 "어느 날 종가 기준인지"를 밝히는지 본다.
 *
 * <p>이 앱은 시세를 자동으로 모으지 않는다 - {@code @Scheduled} 도 k8s CronJob 도 없고 관리 화면에서 사람이 눌러야 수집된다. 그래서 평가액이
 * 며칠 전 종가로 계산되는 일이 실제로 일어난다(실측 2026-08-22 토요일: 마지막 수집이 2026-08-20 이라 거래일인 08-21 금요일이 빠진 채 총자산 총자산이
 * 표시됐다).
 *
 * <p>자산 현황·포트폴리오·종목 상세·계좌 상세에는 안내가 있었지만 <b>요약(대시보드) 화면만 빠져 있었다</b> - 사용자가 가장 먼저 보는 큰 숫자가 정작 기준일을
 * 밝히지 않았다. 화면이 늘 때 같은 누락이 반복되지 않도록 목록으로 고정한다.
 */
class PriceBasisDisclosureTest {

  private static final String KEY = "stock.asset.status.price.basis";

  /** 평가액(또는 총자산)을 표시하므로 기준일을 밝혀야 하는 템플릿. */
  private static final List<Path> TEMPLATES =
      List.of(
          Path.of("src/main/jte/stock/htmx/fragments/summary.jte"),
          Path.of("src/main/jte/stock/htmx/fragments/assetStatus.jte"),
          Path.of("src/main/jte/stock/htmx/fragments/tabsPortfolio.jte"),
          Path.of("src/main/jte/stock/stockItemDetail.jte"),
          Path.of("src/main/jte/stock/accountDetail.jte"));

  /** 그 템플릿에 값을 넘겨야 하는 컨트롤러. */
  private static final List<Path> CONTROLLERS =
      List.of(
          Path.of(
              "src/main/java/net/luversof/web/gate/stock/controller/StockSummaryHtmxController.java"),
          Path.of(
              "src/main/java/net/luversof/web/gate/stock/controller/StockPortfolioHtmxController.java"),
          Path.of("src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java"));

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 평가액을_보여주는_템플릿은_기준일을_받아_표시한다() throws IOException {
    for (Path template : TEMPLATES) {
      String source = read(template);
      assertThat(source)
          .as(template + " 이 기준일 파라미터를 받지 않는다. 컨트롤러가 넘겨도 쓰이지 않는다")
          .contains("@param java.time.LocalDate priceBasisDate");
      assertThat(source)
          .as(template + " 이 기준일을 화면에 그리지 않는다")
          .contains(KEY)
          .contains("priceBasisDate.toString()");
      assertThat(source)
          .as(template + " 이 기준일이 없을 때도 안내를 그린다. 근거가 없으면 감춰야 한다")
          .contains("priceBasisDate != null");
    }
  }

  @Test
  void 컨트롤러가_기준일을_넘긴다() throws IOException {
    for (Path controller : CONTROLLERS) {
      assertThat(read(controller))
          .as(controller + " 이 priceBasisDate 를 모델에 넣지 않는다")
          .contains("\"priceBasisDate\"");
    }
  }

  /**
   * 기준일 계산이 화면마다 따로 적히면 한 곳만 고치고 나머지를 잊는다.
   *
   * <p>실제로 같은 스트림 계산이 컨트롤러 세 곳에 각각 있었다. 요약 화면을 넷째로 붙이면서 공용 유틸로 모았으므로, 다시 흩어지지 않게 막는다.
   */
  @Test
  void 기준일_계산은_한_곳에만_있다() throws IOException {
    for (Path controller : CONTROLLERS) {
      String source = read(controller);
      assertThat(source)
          .as(controller + " 이 기준일을 직접 계산한다. StockPriceBasisUtil 을 쓸 것")
          .doesNotContain("TradeProfit::currentPriceDate");
      assertThat(source).contains("StockPriceBasisUtil.latestPriceBasisDate(");
    }
  }

  @Test
  void 안내_문구_키가_두_번들에_있다() throws IOException {
    for (String bundle : List.of("uiMessage.properties", "uiMessage_ko.properties")) {
      assertThat(read(Path.of("src/main/resources", bundle)))
          .as(bundle + " 에 " + KEY + " 이 없다")
          .contains(KEY);
    }
  }
}
