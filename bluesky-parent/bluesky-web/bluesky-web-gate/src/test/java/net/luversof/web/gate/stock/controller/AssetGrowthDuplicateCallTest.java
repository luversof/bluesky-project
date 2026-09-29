package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 자산 성장 조각은 조건이 같은 종목별 손익을 두 번 부르지 않는다.
 *
 * <p>실측 2026-09-23: 기간이 없으면(전체) 평가 기준일용 요청과 종목별 실현 손익 요청이 똑같아 {@code
 * calculateProfit?groupBy=STOCKITEM} 을 두 번 던졌다(각 18ms). 조건이 같으면 앞의 것을 같이 쓴다 - 두 쪽 다 읽기만 한다. 기간이 있으면
 * 두 요청이 다르므로 따로 던진다. 소스는 공백을 전부 지우고 본다(서식 정리로 줄이 접힌다).
 */
class AssetGrowthDuplicateCallTest {

  @Test
  void 조건이_같으면_종목별_손익을_한_번만_부른다() throws IOException {
    String source =
        Files.readString(
                Path.of(
                    "src/main/java/net/luversof/web/gate/stock/controller/StockAssetGrowthHtmxController.java"),
                StandardCharsets.UTF_8)
            .replaceAll("\\s+", "");

    assertThat(source)
        .as("같은지는 요청 파라미터 전체로 가린다 - 기간 키 하나만 보면 다른 필터가 달라도 같다고 본다")
        .contains("booleansameAsPriceBasis=stockRealizedParams.equals(priceBasisParams);")
        .as("같으면 앞의 요청을 같이 쓴다")
        .contains(
            "varstockRealizedFuture=emptySelection?null:sameAsPriceBasis?priceBasisFuture:java.util.concurrent.CompletableFuture.supplyAsync(()->tradeProfitClient.calculateProfit(stockRealizedParams),stockRemoteCallExecutor);");
  }
}
