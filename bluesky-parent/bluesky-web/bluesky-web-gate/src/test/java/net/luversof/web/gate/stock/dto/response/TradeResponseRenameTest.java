package net.luversof.web.gate.stock.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.constant.TradeType;

/**
 * TradeResponse.withStockItemName - 종목 이름만 바꾸고 나머지 성분은 그대로 옮긴다.
 *
 * <p>2026-10-22 까지 매매 목록 · 자산 성장이 성분 12 개를 하나씩 옮겨 적었다. 같은 형의 성분(BigDecimal 다섯 · UUID 셋)은 자리를 바꿔 적어도
 * 컴파일된다 - 그래서 값이 모두 다른 응답을 만들어 성분마다 비교한다(성분이 늘어나면 검사도 저절로 늘어난다).
 */
class TradeResponseRenameTest {

  @Test
  void 이름_말고는_모든_성분이_그대로다() throws ReflectiveOperationException {
    TradeResponse source =
        new TradeResponse(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
            "api 이름",
            TradeType.SELL,
            7,
            new BigDecimal("101"),
            new BigDecimal("102"),
            new BigDecimal("103"),
            new BigDecimal("104"),
            new BigDecimal("105"),
            Instant.parse("2026-10-22T00:00:00Z"));
    TradeResponse renamed = source.withStockItemName("게이트 이름");

    assertThat(renamed.stockItemName()).isEqualTo("게이트 이름");
    int compared = 0;
    for (RecordComponent component : TradeResponse.class.getRecordComponents()) {
      if (component.getName().equals("stockItemName")) {
        continue;
      }
      Object before = component.getAccessor().invoke(source);
      assertThat(component.getAccessor().invoke(renamed)).as(component.getName()).isEqualTo(before);
      compared++;
    }
    assertThat(compared).isEqualTo(TradeResponse.class.getRecordComponents().length - 1);
  }

  @Test
  void 매매_목록과_자산_성장이_쓴다() throws IOException {
    for (String controller :
        List.of("StockTradeHtmxController.java", "StockAssetGrowthHtmxController.java")) {
      String source =
          Files.readString(
              Path.of("src/main/java/net/luversof/web/gate/stock/controller", controller),
              StandardCharsets.UTF_8);
      assertThat(source)
          .as(controller)
          .contains(".withStockItemName(")
          .doesNotContain("new TradeResponse(");
    }
  }
}
