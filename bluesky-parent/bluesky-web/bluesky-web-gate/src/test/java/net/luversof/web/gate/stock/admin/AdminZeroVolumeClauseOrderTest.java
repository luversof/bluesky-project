package net.luversof.web.gate.stock.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * "그중 종가가 바뀐 행" 은 받는 말 바로 뒤에 있어야 한다.
 *
 * <p>이 문장의 "그중" 은 <b>거래량 0 행</b>(실측 2026-09-12: 57,586 행 중 1,343 행)을 받는다. 그런데 2026-09-12 까지 이 문장이
 * 중복 행 문장 뒤에 놓여, 화면에서는 이렇게 읽혔다:
 *
 * <pre>
 * 시세 행 57,586건 중 거래량 0 이 1,343건 (2.33%)
 * 직전 거래일과 같은 행 9종목 중 1건(종가만) · 거래량까지 같은 것 0건
 * · 그중 종가가 바뀐 행 1건 - 거래가 없으면 종가는 바뀔 수 없다
 * </pre>
 *
 * <p>두 "1건" 이 우연히 같은 숫자라 뒤 문장이 앞 문장의 1 건을 받는 것처럼 읽힌다. 실제로는 앞은 마지막 시세일(2026-09-09) 기준이고 뒤는 쌍방울
 * 2025-05-08 로 서로 무관한 행이다.
 */
class AdminZeroVolumeClauseOrderTest {

  @Test
  void 그중_문장은_거래량0_문장_바로_뒤에_온다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/adminActions.jte"), StandardCharsets.UTF_8);
    int zeroVolumeLine = template.indexOf("dataStatus.priceHistoryZeroVolumeRatioPercent()");
    int changedClause =
        template.indexOf("dataStatus.priceHistoryZeroVolumeChangedCloseCount() > 0");
    int duplicateRows = template.indexOf("stock.admin.price.duplicate.rows");
    assertThat(zeroVolumeLine).as("거래량 0 문장이 있어야 한다").isGreaterThan(-1);
    assertThat(changedClause).as("그중 문장이 있어야 한다").isGreaterThan(-1);
    assertThat(duplicateRows).as("중복 행 문장이 있어야 한다").isGreaterThan(-1);
    assertThat(changedClause).as("그중 문장은 받는 말(거래량 0) 뒤에 와야 한다").isGreaterThan(zeroVolumeLine);
    assertThat(changedClause)
        .as("그중 문장이 중복 행 문장 뒤로 밀리면 그 1건을 받는 것처럼 읽힌다")
        .isLessThan(duplicateRows);
  }

  /** 예시 행(쌍방울 …)도 그 문장에 붙어 있어야 한다 - 떨어지면 어느 문장의 예시인지 알 수 없다. */
  @Test
  void 예시_행도_같이_따라간다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/adminActions.jte"), StandardCharsets.UTF_8);
    int changedClause =
        template.indexOf("dataStatus.priceHistoryZeroVolumeChangedCloseCount() > 0");
    int changedRows =
        template.indexOf("dataStatus.priceHistoryZeroVolumeChangedCloseRows() != null");
    int duplicateRows = template.indexOf("stock.admin.price.duplicate.rows");
    assertThat(changedRows).isGreaterThan(changedClause).isLessThan(duplicateRows);
  }
}
