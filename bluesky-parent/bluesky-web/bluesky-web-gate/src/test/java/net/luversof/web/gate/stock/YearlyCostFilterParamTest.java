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
    char q = (char) 34;
    assertThat(source)
        .as("계좌 필터를 연도별 세금·비용 호출에 실어야 한다")
        .contains("yearlyCostParams.add(" + q + "accountIdList" + q + ", id.toString());");
    assertThat(source)
        .as("종목 필터도 같이 실어야 한다 - 한쪽만 실으면 표 안에서 범위가 갈린다")
        .contains("yearlyCostParams.add(" + q + "stockItemIdList" + q + ", id.toString());");
  }
}
