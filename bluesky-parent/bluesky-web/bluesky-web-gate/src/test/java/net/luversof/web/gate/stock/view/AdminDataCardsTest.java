package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 관리 화면 갱신 버튼은 그 데이터의 최신 시점 · 마지막 실행과 한 카드에 있다(사용자 요청 2026-10-28).
 *
 * <p>예전엔 버튼 줄 · "데이터 최신 시점" 줄 · "마지막 실행" 줄이 따로였고 줄마다 순서도 달랐다(버튼은 종목 · 매매 · 배당 · 시세, 최신 시점은 매매 · 배당
 * · 시세 · 종목) - 눈으로 짝을 맞춰야 했다.
 */
class AdminDataCardsTest {

  private static final Path ADMIN = Path.of("src/main/jte/stock/htmx/fragments/adminActions.jte");

  @Test
  void 카드마다_버튼과_최신_시점과_마지막_실행이_같은_데이터다() throws IOException {
    String source = Files.readString(ADMIN, StandardCharsets.UTF_8);
    for (String action : List.of("stock-items", "trades", "dividends", "price-histories")) {
      String marker = "data-admin-card=\"" + action + "\"";
      int start = source.indexOf(marker);
      assertThat(start).as(action + " 카드가 없다").isGreaterThan(-1);
      int next = source.indexOf("data-admin-card=\"", start + marker.length());
      String card = source.substring(start, next < 0 ? source.length() : next);
      assertThat(card)
          .as(action + " 카드")
          .contains("data-admin-action=\"" + action + "\"")
          .contains("data-admin-coverage=\"" + action + "\"")
          .contains("data-admin-last-run=\"" + action + "\"");
    }
    assertThat(source.split("data-admin-last-run=\"", -1))
        .as("카드 밖에 따로 도는 마지막 실행 줄이 없다")
        .hasSize(5);
  }
}
