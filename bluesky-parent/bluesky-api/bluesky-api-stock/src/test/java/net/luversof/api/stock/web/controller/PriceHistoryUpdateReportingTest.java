package net.luversof.api.stock.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 가격 갱신 경로가 실패를 조용히 삼키지 않는지 소스로 확인한다.
 *
 * <p>이 경로는 KIS API 를 실제로 부르므로 단위 테스트로 돌릴 수 없다. 대신 "실패가 호출자에게 전달되는 형태인지" 를 고정한다 &mdash; 이게 무너지면 관리
 * 화면이 다시 늘 성공으로 보인다.
 */
class PriceHistoryUpdateReportingTest {

  private static final Path SERVICE =
      Path.of("src/main/java/net/luversof/api/stock/service/kis/KisStockPriceUpdateService.java");
  private static final Path CONTROLLER =
      Path.of("src/main/java/net/luversof/api/stock/web/controller/StockAdminController.java");

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌거나 사라졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 갱신_작업은_결과를_돌려준다() throws IOException {
    assertThat(read(SERVICE))
        .as("updatePriceHistory 가 void 로 돌아가면 실패 건수가 사라진다")
        .contains("public PriceHistoryUpdateResult updatePriceHistory(UUID userId)");
    assertThat(read(CONTROLLER))
        .as("엔드포인트가 결과를 돌려주지 않으면 호출자는 실패를 알 수 없다")
        .contains("PriceHistoryUpdateResult priceHistoryUpdate(");
  }

  @Test
  void 종목별_조회는_성공여부를_알린다() throws IOException {
    String service = read(SERVICE);
    assertThat(service)
        .as("fetchAndSavePriceHistory 가 void 면 실패를 셀 수 없다")
        .contains("private boolean fetchAndSavePriceHistory(");
    assertThat(service)
        .as("fetchRangesInBlocks 가 void 면 구간 실패가 사라진다")
        .contains("private boolean fetchRangesInBlocks(");
    assertThat(service).as("인증 설정 실패는 그 실행의 모든 종목 실패이므로 성공으로 넘기면 안 된다").contains("return false;");
  }

  @Test
  void 실행이_끝나면_결과를_한_줄로_남긴다() throws IOException {
    assertThat(read(SERVICE))
        .as("흩어진 종목별 경고만 남기면 몇 개가 실패했는지 로그를 뒤져야 한다")
        .contains("price history update finished");
  }
}
