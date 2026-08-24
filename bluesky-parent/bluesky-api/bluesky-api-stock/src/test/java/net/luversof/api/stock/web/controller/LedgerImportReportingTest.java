package net.luversof.api.stock.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 원장 가져오기 경로가 버려진 행을 조용히 삼키지 않는지 소스로 확인한다.
 *
 * <p>이 경로는 구글 시트를 실제로 부르고 원장을 통째로 다시 넣으므로 단위 테스트로 돌릴 수 없다. 대신 "결과가 호출자에게 전달되는 형태인지" 를 고정한다.
 */
class LedgerImportReportingTest {

  private static final Path SERVICE =
      Path.of("src/main/java/net/luversof/api/stock/service/StockAdminService.java");
  private static final Path CONTROLLER =
      Path.of("src/main/java/net/luversof/api/stock/web/controller/StockAdminController.java");

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌거나 사라졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 거래와_배당_가져오기는_결과를_돌려준다() throws IOException {
    String service = read(SERVICE);
    assertThat(service)
        .as("tradeBulkInsert 가 void 로 돌아가면 버려진 행이 흔적 없이 사라진다")
        .contains("public LedgerImportResult tradeBulkInsert(UUID userId)");
    assertThat(service)
        .as("dividendBulkInsert 도 같은 이유로 결과를 돌려줘야 한다")
        .contains("public LedgerImportResult dividendBulkInsert(UUID userId)");

    String controller = read(CONTROLLER);
    assertThat(controller).contains("LedgerImportResult tradeBulkInsert(");
    assertThat(controller).contains("LedgerImportResult dividendBulkInsert(");
  }

  @Test
  void 못_찾은_종목명을_모은다() throws IOException {
    assertThat(read(SERVICE))
        .as("종목을 못 찾아 버린 이름을 모으지 않으면 원인을 알 수 없다")
        .contains("unknownStockNames");
  }

  @Test
  void 버려진_행이_있으면_한_줄로_남긴다() throws IOException {
    assertThat(read(SERVICE)).as("DEBUG 한 줄로는 운영에서 아무 흔적도 남지 않는다").contains("import dropped rows");
  }
}
