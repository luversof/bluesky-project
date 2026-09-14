package net.luversof.web.gate.stock.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 관리 화면의 "데이터 최신 시점" 은 시세가 <b>몇 종목까지</b> 최신인지도 말해야 한다.
 *
 * <p>실측 2026-09-12(86 종목 전수, api-stock 종목별 priceHistory 조회): 마지막 시세 일자는 2026-09-09 인데 그 날짜에 행이 있는
 * 종목은 <b>9 개</b>뿐이었다. 70 종목이 2026-04-01/03 에 멈춰 있고(쌍방울 2025-11-27 · 제일바이오 2020-03-03), 4 종목은 시세 이력이
 * 아예 없다. 시세 갱신이 보유 종목 위주로 도는 결과라 데이터가 틀린 것은 아니다.
 *
 * <p>그런데 화면은 "종목 갱신: 86건" 과 "종목 히스토리 갱신: 2026-09-09" 를 나란히 적어, 86 종목이 그 날까지 최신인 것으로 읽혔다. 이 화면의 구역
 * 이름이 "데이터 최신 시점" 인 만큼 없는 최신성을 믿게 두면 안 된다.
 */
class AdminPriceHistoryCoverageTest {

  @Test
  void 종목_신선도를_화면에_적는다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/adminActions.jte"), StandardCharsets.UTF_8);
    String flat = flatten(template);
    assertThat(flat)
        .as("종목 수 · 최신일 행이 있는 종목 수 · 이력 없는 종목 수를 모두 실어야 한다")
        .contains("stock.admin.price.history.coverage")
        .contains("dataStatus.priceHistoryLastDateItemCount()")
        .contains("dataStatus.priceHistoryNoHistoryItemCount()");
  }

  @Test
  void 게이트_응답_모델이_두_값을_받는다() throws IOException {
    String dto =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/dto/response/DataStatusResponse.java"),
            StandardCharsets.UTF_8);
    assertThat(dto)
        .contains("long priceHistoryLastDateItemCount,")
        .contains("long priceHistoryNoHistoryItemCount,");
  }

  @Test
  void 문구는_두_로케일에_다_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(
              Files.readString(
                  Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8))
          .as(bundle)
          .contains("stock.admin.price.history.coverage");
    }
  }

  private String flatten(String source) {
    StringBuilder out = new StringBuilder();
    boolean lastWasSpace = false;
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (Character.isWhitespace(c)) {
        if (!lastWasSpace) {
          out.append(' ');
        }
        lastWasSpace = true;
      } else {
        out.append(c);
        lastWasSpace = false;
      }
    }
    return out.toString();
  }
}
