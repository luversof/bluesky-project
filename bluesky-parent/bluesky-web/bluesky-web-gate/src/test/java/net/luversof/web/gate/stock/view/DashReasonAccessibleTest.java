package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 대시의 까닭은 <b>보조기술에도</b> 닿아야 한다.
 *
 * <p>이 앱은 이미 같은 실수를 겪었다 &mdash; 2026-09-10 실측: 값·근거가 {@code title} 에만 있어 주식 6 화면 22 건이 마우스
 * 전용이었다({@code _components/ui/srExact} 가 그때 생겼다). 2026-09-11 에 대시마다 까닭을 달면서 같은 함정에 다시 빠져 <b>397
 * 칸</b>이 title 전용이 됐다(배당 297 · 자산 성장 60 · 매매 17 · 종목 상세 17 · 자산 현황 6).
 *
 * <p>칸 자체에 다는 곳은 {@code aria-label}(role=cell 이라 이름이 된다), 안쪽 span 으로 찍는 곳은 sr-only 를 덧댄다.
 */
class DashReasonAccessibleTest {

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  private int count(String source, String marker) {
    int n = 0;
    for (int at = source.indexOf(marker); at >= 0; at = source.indexOf(marker, at + 1)) {
      n++;
    }
    return n;
  }

  @Test
  void 칸에_단_까닭은_이름으로도_나간다() throws IOException {
    for (String rel :
        List.of(
            "src/main/jte/_components/ui/amountCell.jte",
            "src/main/jte/stock/htmx/fragments/dividend/dividendTable.jte",
            "src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte",
            "src/main/jte/stock/htmx/stockItemDetailContent.jte")) {
      String template = read(rel);
      assertThat(template).as(rel).contains("aria-label=");
    }
  }

  @Test
  void 수익률_분자_설명도_보조기술에_닿는다() throws IOException {
    String analytics =
        read("src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte");

    // 실측 2026-09-11: 이 설명이 title 에만 있어 34 칸이 마우스 전용이었다.
    // 같은 행의 숨은 열에도 같은 문구가 있으므로 <b>항상 보이는 칸</b>에만 단다 - 둘 다 달면 두 번 읽힌다.
    int srOnly = count(analytics, "sr-only" + (char) 34 + ">${yieldBasisTitle");
    assertThat(srOnly).as("항상 보이는 수익률 칸 여섯 곳").isEqualTo(6);
    assertThat(analytics)
        .as("값이 없으면 붙이지 않는다")
        .contains("!= null)<span class=" + (char) 34 + "sr-only");
  }

  @Test
  void 대시가_아닌_까닭도_보조기술에_닿는다() throws IOException {
    // 실측 2026-09-11(대시 말고 title 만 있는 것들): 매매 "이 계좌 기준" 3 곳과 배당 "N개월 평균" 1 곳이 마우스 전용이었다.
    assertThat(read("src/main/jte/stock/htmx/fragments/trade/tradeRealizedSections.jte"))
        .contains(
            "sr-only"
                + (char) 34
                + ">${MessageUtil.getMessage("
                + (char) 34
                + "stock.trade.realized.basis.gap.title");
    String script = read("src/main/frontend/src/stock/dividendHistory.ts");
    assertThat(script).contains("srSpan.className = " + (char) 34 + "sr-only" + (char) 34);
    assertThat(script).contains("descEl.appendChild(srSpan)");
    assertThat(read("src/main/resources/static/js/stock/dividendHistory.js")).contains("sr-only");
  }

  /**
   * 배당 목록의 수익률 "-" 는 까닭이 툴팁에만 있었다.
   *
   * <p>실측 2026-09-11: 전체 기간 목록 473 행 중 5 행이 기준일 원금을 못 되짚어 "-" 로 남는데, 그 까닭이 마우스 전용이었다. 행당 1KB 예산
   * (DividendTableCompactOutputTest) 때문에 미뤄뒀던 자리다 - 다섯 행뿐이라 조각은 397,532 → 398,212 자(+0.17%) 로 그쳤다.
   */
  @Test
  void 기준을_못_되짚은_수익률도_보조기술에_닿는다() throws IOException {
    String table = read("src/main/jte/stock/htmx/fragments/dividend/dividendTable.jte");

    assertThat(count(table, "title=" + (char) 34 + "${basisMissingTitle.apply(item)}"))
        .as("까닭 툴팁이 붙은 칸")
        .isEqualTo(1);
    assertThat(table)
        .as("같은 까닭이 sr-only 로도 나가야 한다")
        .contains(
            "@if(basisMissingTitle.apply(item) != null)<span class="
                + (char) 34
                + "sr-only"
                + (char) 34
                + ">${basisMissingTitle.apply(item)}</span>@endif");
  }

  /**
   * 숫자만 적힌 링크는 어디로 가는지 이름에 담아야 한다.
   *
   * <p>실측 2026-09-11(시뮬레이터 월배당 탭, 6곳): 링크 글자는 "지급 이력 기준 82.79%" 뿐이고 목적지 설명(관리 &gt; 월배당 기준 데이터에서 이
   * 종목의 저장값을 갱신합니다)은 {@code title} 에만 있었다. 화면을 못 보는 사용자는 백분율이 링크라는 것만 듣는다.
   *
   * <p>이 자리는 앞선 회차에 "링크 title 은 보조 설명이라 손실 없음" 으로 분류했던 곳이다 &mdash; 내용을 실제로 읽어 보니 설명이 아니라
   * <b>목적지</b>였다. 분류를 바로잡는다.
   */
  @Test
  void 숫자만_적힌_링크는_목적지를_이름에_담는다() throws IOException {
    String template = read("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");

    assertThat(count(template, "stock.simulator.monthly.table.cell.taxable.ratio.reference.link"))
        .as("title 한 번 + sr-only 한 번")
        .isEqualTo(2);
    assertThat(template)
        .as("링크 안쪽에 붙어야 이름의 일부가 된다")
        .contains(
            "<span class="
                + (char) 34
                + "sr-only"
                + (char) 34
                + ">${MessageUtil.getMessage("
                + (char) 34
                + "stock.simulator.monthly.table.cell.taxable.ratio.reference.link");
  }

  @Test
  void 안쪽_span_으로_찍는_곳은_sr_only_를_덧댄다() throws IOException {
    for (String rel :
        List.of(
            "src/main/jte/stock/htmx/fragments/trade/tradePeriodBreakdown.jte",
            "src/main/jte/stock/htmx/fragments/stockContributionTable.jte",
            "src/main/jte/stock/htmx/fragments/assetStatus.jte")) {
      String template = read(rel);
      int titles =
          count(template, "title=\"${zeroAmountTitle}\"") + count(template, "none\")}\">-</span>");
      int srOnly =
          count(template, "sr-only\">${zeroAmountTitle}")
              + count(template, "sr-only\">${MessageUtil.getMessage(\"stock.profit.realized.none")
              + count(
                  template, "sr-only\">${MessageUtil.getMessage(\"stock.dividend.received.none");
      assertThat(srOnly).as(rel + " 의 대시 까닭 수").isGreaterThanOrEqualTo(titles > 0 ? 1 : 0);
      assertThat(template).as(rel).contains("sr-only");
    }
  }
}
