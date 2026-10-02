package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 원화 기호는 수에 붙이고(₩1,234), 부호는 기호 앞에 둔다(-₩1,234).
 *
 * <p>실측 2026-10-01: 대부분(템플릿 48 곳 · 차트)은 붙여 쓰는데 활동 내역 · 배당 요약 카드 · 배당 수익률 분석 세 파일 24 곳만 "₩
 * 1,815,470,942" 로 띄어 같은 화면 묶음 안에서 표기가 갈렸다. 그리고 두 곳은 손실의 부호가 기호 뒤였다 - 활동 캘린더 하루 합계 "₩ -347,060", 보유
 * 스냅샷 평가손익 "₩-1,234"(나머지 화면은 "-₩1,234").
 */
class CurrencySymbolPlacementTest {

  private static final Path JTE = Path.of("src/main/jte/stock");

  @Test
  void 주식_템플릿에는_띄어_쓴_원화_기호가_없다() throws IOException {
    List<String> hits = new ArrayList<>();
    try (Stream<Path> files = Files.walk(JTE)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String[] lines = Files.readString(file, StandardCharsets.UTF_8).split("\n");
        for (int i = 0; i < lines.length; i++) {
          if (lines[i].contains("₩ ${") || lines[i].contains("₩ \" +")) {
            hits.add(file.getFileName() + ":" + (i + 1));
          }
        }
      }
    }
    assertThat(hits).as("₩ 와 수 사이 빈칸").isEmpty();
  }

  @Test
  void 손실의_부호는_원화_기호_앞이다() throws IOException {
    String snapshot =
        Files.readString(JTE.resolve("htmx/holdings-snapshot.jte"), StandardCharsets.UTF_8);
    assertThat(snapshot)
        .as("보유 스냅샷 평가손익: 음수면 기호 앞에 -, 수는 절댓값")
        .contains("StockFormatUtil.displayWon(item.unrealizedProfit()) < 0 ? \"-\" : \"\"}₩${")
        .contains("Math.abs(StockFormatUtil.displayWon(item.unrealizedProfit()))");

    String activity =
        Files.readString(JTE.resolve("htmx/fragments/activityList.jte"), StandardCharsets.UTF_8);
    assertThat(activity)
        .as("활동 캘린더 하루 합계")
        .contains("${dayNetAmount.signum() < 0 ? \"-\" : \"\"}₩${df.format(dayNetAmount.abs())}")
        .doesNotContain("₩${df.format(dayNetAmount)}");
  }
}
