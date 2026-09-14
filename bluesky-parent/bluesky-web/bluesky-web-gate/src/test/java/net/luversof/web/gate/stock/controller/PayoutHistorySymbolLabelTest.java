package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 월배당 기준 데이터의 '저장된 지급 이력' 표는 어느 종목의 이력인지 제목에 적어야 한다.
 *
 * <p>실측 2026-09-11: 이 표는 종목에 따라 29~58 행이라 위쪽 종목 선택기에서 한참 멀어지는데, 제목은 "저장된 지급 이력", 설명은 "선택한 종목의 최근
 * 월배당 지급 이력을 확인합니다." 뿐이고 <b>종목명도 코드도 어디에도 없었다</b>. 스크롤한 뒤에는 무엇을 보고 있는지 알 수 없다.
 *
 * <p>바로 위 등록 폼이 이미 쓰는 라벨({@code payoutSymbolLabel} = "476800 · KODEX 한국부동산리츠인프라")을 그대로 붙인다.
 */
class PayoutHistorySymbolLabelTest {

  private static final String TEMPLATE =
      "src/main/jte/stock/fragments/monthlyDividendReference.jte";

  private String read() throws IOException {
    return Files.readString(Path.of(TEMPLATE), StandardCharsets.UTF_8);
  }

  @Test
  void 이력_표_제목이_종목을_밝힌다() throws IOException {
    String template = read();

    int title = template.indexOf("stock.page.dividend.monthly.reference.payout.list.title");
    assertThat(title).as("이력 표 제목을 찾지 못했다").isGreaterThan(0);

    String heading = template.substring(title, template.indexOf("</h2>", title));
    assertThat(heading).as("제목이 선택된 종목을 말해야 한다").contains("payoutSymbolLabel");
    assertThat(heading).contains("data-payout-history-symbol");
  }

  @Test
  void 종목이_없으면_빈_배지를_그리지_않는다() throws IOException {
    String template = read();

    int title = template.indexOf("stock.page.dividend.monthly.reference.payout.list.title");
    String heading = template.substring(title, template.indexOf("</h2>", title));

    assertThat(heading)
        .as("선택 전에는 빈 배지가 남으면 안 된다")
        .contains("payoutSymbolLabel != null && !payoutSymbolLabel.isBlank()");
  }

  @Test
  void 라벨은_코드와_이름을_함께_쓴다() throws IOException {
    String template = read();

    assertThat(template)
        .as("등록 폼이 쓰는 라벨과 같은 규칙이어야 한다")
        .contains("payoutSymbolLabelTmp = stockItem.symbol() + \" · \" + stockItem.name();");
  }
}
