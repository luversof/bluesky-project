package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 배당 기간별 집계 표도 세금 0 을 형제 표와 같게 적는다.
 *
 * <p>배당 이력 · 수익률 표와 매매 두 표는 이미 "-" 와 까닭을 단다. 이 표만 숫자 0 이었다.
 *
 * <p>실측 2026-09-13: 전체 기간으로 보면 월 버킷 37 개 중 0 인 달은 1 개(2025-07)뿐이라 눈에 잘 안 띈다. 그러나 이 표는 <b>필터를
 * 따른다</b> - 절세계좌(연금저축1 · ISA · 연금저축2)는 세금이 한 푼도 없고 배당 202 건 중 <b>110 건</b>이 그 계좌들이라, 그 계좌만 걸면 <b>월
 * 버킷 20 개가 모두 0</b> 이 된다. 합계 줄도 마찬가지다.
 *
 * <p>고치는 자리는 넷이다 - 연도 행 · 연도 합계 · 월 행 · 월 합계.
 */
class DividendBreakdownZeroTaxTest {

  private static final String TEMPLATE =
      "src/main/jte/stock/htmx/fragments/dividend/dividendPeriodBreakdown.jte";
  private static final String KEY = "stock.dividend.zero.amount.title";
  private static final String DETAIL_TABLE =
      "src/main/jte/stock/htmx/fragments/dividend/dividendTable.jte";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  /** 네 자리 모두 0 을 먼저 판단한다 - null 도 0 으로 본다. */
  @Test
  void 네_자리_모두_0_을_판단한다() throws IOException {
    String src = read(TEMPLATE);

    for (String accessor : List.of("row", "yearlyTotal", "monthlyTotal")) {
      assertThat(src)
          .as(accessor)
          .contains("@if(" + accessor + ".tax() == null || " + accessor + ".tax().signum() == 0)");
    }
    assertThat(src.split("\\.tax\\(\\).signum\\(\\) == 0\\)", -1).length - 1)
        .as("판단하는 자리 수")
        .isEqualTo(4);
  }

  /** 0 이면 까닭을 단 "-", 아니면 금액 그대로. */
  @Test
  void 세금_0_이면_대시_아니면_금액() throws IOException {
    String src = read(TEMPLATE);

    assertThat(src).contains("MessageUtil.getMessage(\"" + KEY + "\")");
    assertThat(src).contains("@else${decimalFormat.format(StockFormatUtil.displayWon(row.tax()))}");
    assertThat(src)
        .contains("@else${decimalFormat.format(StockFormatUtil.displayWon(yearlyTotal.tax()))}");
    assertThat(src)
        .contains("@else${decimalFormat.format(StockFormatUtil.displayWon(monthlyTotal.tax()))}");
  }

  /** 숫자를 그대로 찍던 옛 형태가 남아 있으면 안 된다. */
  @Test
  void 숫자를_그대로_찍던_옛_형태가_없다() throws IOException {
    assertThat(read(TEMPLATE))
        .doesNotContain(
            "<td class=\"text-right font-mono amount-value\">"
                + "${decimalFormat.format(StockFormatUtil.displayWon(row.tax()))}</td>");
  }

  /** 자리표시 대시는 모두 가려져 있다 - 안 가리면 "-0원" 으로 읽힌다. */
  @Test
  void 자리표시_대시가_모두_가려져_있다() throws IOException {
    String src = read(TEMPLATE);
    int at = src.indexOf(">-</span>");
    int seen = 0;
    while (at >= 0) {
      String before = src.substring(Math.max(0, at - 160), at);

      assertThat(before).as("가려지지 않은 자리표시 대시: " + before).contains("aria-hidden=\"true\"");
      seen++;
      at = src.indexOf(">-</span>", at + 1);
    }
    assertThat(seen).as("자리표시 대시 수").isEqualTo(4);
  }

  /** 까닭은 sr-only 로도 나가야 한다 - title 만 두면 마우스 전용이 된다. */
  @Test
  void 까닭이_보조기술에도_닿는다() throws IOException {
    String src = read(TEMPLATE);

    assertThat(src.split("<span class=\"sr-only\">\\$\\{zeroAmountTitle\\}</span>", -1).length - 1)
        .as("sr-only 까닭 수")
        .isEqualTo(4);
  }

  /** 형제 표와 같은 키를 쓴다 - 따로 만들면 다시 갈린다. */
  @Test
  void 형제_표와_같은_키를_쓴다() throws IOException {
    assertThat(read(DETAIL_TABLE)).as("배당 이력 표").contains(KEY);
  }

  /**
   * 배당 이력 표의 <b>합계 줄</b>도 같은 규칙을 쓴다.
   *
   * <p>본문 행은 2026-09-08 에 고쳤는데 합계 줄 두 칸(세금 · 과세 금액)이 남아 있었다 - 실측 2026-09-13: 절세계좌만 걸면 그 합계가 0 이라
   * "0" 이 그대로 보였다.
   */
  @Test
  void 배당_이력_합계_줄도_대시를_쓴다() throws IOException {
    String src = read(DETAIL_TABLE);

    assertThat(src).contains("@if(totalTax == null || totalTax.signum() == 0)");
    assertThat(src).contains("@if(totalTaxableAmount == null || totalTaxableAmount.signum() == 0)");
    assertThat(src).doesNotContain("${totalTax != null ? decimalFormat.format(totalTax) : \"0\"}");
    assertThat(src)
        .doesNotContain(
            "${totalTaxableAmount != null ? decimalFormat.format(totalTaxableAmount) : \"0\"}");
  }
}
