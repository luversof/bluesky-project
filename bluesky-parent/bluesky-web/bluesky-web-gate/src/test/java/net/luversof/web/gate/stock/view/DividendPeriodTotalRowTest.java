package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 합계 줄은 {@code tfoot} 에 둔다.
 *
 * <p>실측 2026-09-10: 배당 화면의 표 여섯 개 중 '기간별 집계' 연도별·월별 둘만 합계를 {@code tbody} 안에 두고 있었다. 나머지 넷(연도별 배당
 * 수익률 · 종목별/계좌별 랭킹 · 상세 목록)은 {@code tfoot} 을 쓴다. 그 탓에 tbody 행이 연도 8(데이터는 7) · 월 38(데이터는 37)로 세졌고,
 * 보조기술은 합계를 데이터 행으로 읽는다.
 */
class DividendPeriodTotalRowTest {

  private static final Path BREAKDOWN =
      Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendPeriodBreakdown.jte");

  @Test
  void 합계는_tfoot_에_있다() throws IOException {
    List<String> lines = Files.readAllLines(BREAKDOWN, StandardCharsets.UTF_8);

    int tfootOpen = 0;
    int tfootClose = 0;
    boolean inFoot = false;
    int totalsInFoot = 0;
    int totalsOutsideFoot = 0;
    for (String raw : lines) {
      String line = raw.strip();
      if (line.startsWith("<tfoot")) {
        tfootOpen++;
        inFoot = true;
      }
      if (line.contains("${totalLabel}")) {
        if (inFoot) {
          totalsInFoot++;
        } else {
          totalsOutsideFoot++;
        }
      }
      if (line.startsWith("</tfoot>")) {
        tfootClose++;
        inFoot = false;
      }
    }

    assertThat(tfootOpen).as("연도별·월별 두 표 모두 tfoot 이 있어야 한다").isEqualTo(2);
    assertThat(tfootClose).isEqualTo(tfootOpen);
    assertThat(totalsInFoot).as("합계 줄 두 개가 모두 tfoot 안에 있어야 한다").isEqualTo(2);
    assertThat(totalsOutsideFoot).as("tbody 에 남은 합계 줄이 있으면 데이터 행으로 읽힌다").isZero();
  }
}
