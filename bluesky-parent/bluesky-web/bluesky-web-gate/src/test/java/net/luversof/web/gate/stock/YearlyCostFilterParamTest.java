package net.luversof.web.gate.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 자산 성장 화면의 계좌·종목 필터는 연도별 세금·비용에도 걸려야 한다.
 *
 * <p>실측 2026-09-12: 한 계좌로 좁힌 화면과 안 좁힌 화면에서 이 표가 14 행 · 합계 +225,630,135 원까지 바이트 단위로 같았다. 같은 화면의 종목별
 * 기여 · 연도별 성과 · 매매 내역은 모두 좁혀졌으므로, 한 표만 다른 범위를 말하고 있었던 셈이다. 원인은 게이트가 이 호출에만 필터를 안 실었고 api-stock 쪽에도
 * 받을 자리가 없었던 것이다.
 */
class YearlyCostFilterParamTest {

  @Test
  void 게이트가_계좌와_종목을_모두_싣는다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockAssetGrowthHtmxController.java"),
            StandardCharsets.UTF_8);
    // 2026-10-22 부터 두 필터는 addFilterIds 한 곳에서 싣는다(아래 동작 시험이 그 내용을 본다).
    assertThat(source)
        .as("연도별 세금·비용 호출에 화면 필터를 실어야 한다")
        .contains("addFilterIds(yearlyCostParams, request);");
  }

  /** 계좌와 종목을 둘 다 싣는다 - 한쪽만 실으면 표 안에서 범위가 갈린다. 빈 id 는 뺀다. */
  @Test
  void 필터_싣기는_계좌와_종목_둘_다() {
    var request = new net.luversof.web.gate.stock.dto.request.TradeProfitRequest();
    java.util.UUID account = java.util.UUID.randomUUID();
    java.util.UUID stock = java.util.UUID.randomUUID();
    request.setAccountIdList(java.util.Arrays.asList(account, null));
    request.setStockItemIdList(java.util.List.of(stock));
    var params = new org.springframework.util.LinkedMultiValueMap<String, String>();

    net.luversof.web.gate.stock.controller.StockAssetGrowthHtmxControllerAccess.addFilterIds(
        params, request);

    assertThat(params.get("accountIdList")).containsExactly(account.toString());
    assertThat(params.get("stockItemIdList")).containsExactly(stock.toString());
  }
}
