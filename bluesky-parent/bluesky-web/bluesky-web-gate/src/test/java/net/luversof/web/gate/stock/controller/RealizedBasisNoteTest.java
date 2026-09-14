package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * 실현손익 두 표는 어떤 기준의 값인지 적어야 한다.
 *
 * <p>두 표의 열은 매도원가 · 매도 금액 · 실현손익 · 수익률이라 빼서 맞춰 보게 되는데, {@code 매도금액 - 매도원가} 는 실현손익이 되지 않는다. 실측
 * 2026-09-11(전체 기간): 종목별 37 행 <b>전부</b> 차이가 그 종목의 거래세와 정확히 같았고, 합은 1,932,821 로 화면의 거래세와 일치했다 (수수료
 * 100,671 은 빠지지 않는다). 예: 삼성전자 242,775,200 − 103,705,921 = 139,069,279 인데 실현손익은 138,569,333, 차이
 * 499,946 = 그 종목의 거래세.
 *
 * <p>표에 거래세 열이 없어 화면에서 검산할 수 없으므로 기준을 한 줄로 적는다. 값은 그대로 둔다 &mdash; 증권사 기록값을 쓰는 것은 이 화면의 규약이고, 다른
 * 화면(연도별 매매)은 이미 거래세 열을 따로 보여 준다.
 */
class RealizedBasisNoteTest {

  private static final String TEMPLATE =
      "src/main/jte/stock/htmx/fragments/trade/tradeRealizedSections.jte";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  private Properties messages(String path) throws IOException {
    Properties props = new Properties();
    try (var reader = Files.newBufferedReader(Path.of(path), StandardCharsets.UTF_8)) {
      props.load(reader);
    }
    return props;
  }

  private int count(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }

  @Test
  void 두_절_모두_기준을_적는다() throws IOException {
    String template = read(TEMPLATE);

    assertThat(template).contains("MessageUtil.getMessage(\"stock.realized.basis.note\")");
    assertThat(count(template, "${realizedBasisNote}")).as("계좌별·종목별 두 표에 각각 있어야 한다").isEqualTo(2);
  }

  @Test
  void 두_말이_모두_있고_거래세를_짚는다() throws IOException {
    Properties ko = messages("src/main/resources/uiMessage_ko.properties");
    Properties en = messages("src/main/resources/uiMessage.properties");

    assertThat(ko.getProperty("stock.realized.basis.note"))
        .as("무엇이 빠졌는지(거래세) 와 무엇이 안 빠졌는지(수수료) 를 모두 말해야 한다")
        .contains("거래세")
        .contains("수수료");
    assertThat(en.getProperty("stock.realized.basis.note"))
        .contains("transaction tax")
        .contains("Fees");
  }
}
