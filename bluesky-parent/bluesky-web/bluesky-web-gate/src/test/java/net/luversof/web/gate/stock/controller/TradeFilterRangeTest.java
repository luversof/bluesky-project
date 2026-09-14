package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 필터 목록("기간에 등장한 계좌·종목")은 목록과 같은 기간에서 나와야 한다.
 *
 * <p>실측 2026-09-10: 매매 경로가 {@code effectiveRange} 보정 <b>전</b>의 {@code startDate/endDate} 를 {@code
 * findFilterIds} 에 넘기고 있었다.
 *
 * <ul>
 *   <li>역순 입력(2026-09-10 ~ 2026-08-10): 목록은 23 행으로 정상인데 필터가 비었다 &mdash; 계좌 6 -&gt; 1, 종목 8 -&gt; 1
 *       (남은 1 은 "전체" 옵션뿐).
 *   <li>날짜 없이 프리셋만(rangeMode=1): 같은 23 행 목록인데 필터에 전체 기간 기준 종목 44 개가 실렸다(정상 8).
 * </ul>
 *
 * <p>같은 파일의 활동 경로는 이미 보정값을 쓰고 있었다. 두 호출이 어긋나 있던 것이 단서였다.
 */
class TradeFilterRangeTest {

  private static final Path SOURCE =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller/StockTradeHtmxController.java");

  @Test
  void 필터_구간은_보정된_기간에서_나온다() throws IOException {
    List<String> lines = Files.readAllLines(SOURCE, StandardCharsets.UTF_8);

    int found = 0;
    for (int i = 0; i < lines.size(); i++) {
      if (!lines.get(i).contains("findFilterIds(")) {
        continue;
      }
      found++;
      String window = String.join(" ", lines.subList(Math.max(0, i - 12), i));

      assertThat(window)
          .as("findFilterIds 앞에서 availStart/availEnd 를 정하지 않았다 (호출 " + found + ")")
          .contains("final Instant availStart =")
          .contains("final Instant availEnd =");
      assertThat(window)
          .as("보정 전 파라미터를 넘기면 목록과 필터가 다른 기간을 본다 (호출 " + found + ")")
          .doesNotContain("final Instant availStart = startDate;")
          .doesNotContain("final Instant availEnd = endDate;");
    }

    assertThat(found).as("findFilterIds 호출을 찾지 못했다").isEqualTo(2);
  }
}
