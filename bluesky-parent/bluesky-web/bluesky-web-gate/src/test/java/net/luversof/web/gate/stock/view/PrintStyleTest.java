package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 인쇄에서 화면 전용 장치(고정 막대·가로 스크롤)가 종이를 망치지 않는다.
 *
 * <p>실측 2026-09-10(qa/print-audit.cjs·print-clip.cjs, A4 794px, print 미디어): 인쇄용 규칙이 하나도 없어 고정
 * 상단바(64px)와 하단 독(56px)이 그대로 찍히고 독이 본문 마지막 줄을 덮었다. 가로 스크롤 표는 종이에서 잘렸다 - 자산 현황 '계좌별 현황'
 * 760→842px(82px 손실), 매매 '상세 목록' 762→936px(174px 손실). 동작은 프로브가 재고, 여기서는 규칙이 지워지지 않았는지만 지킨다.
 */
class PrintStyleTest {

  private static String css() throws IOException {
    return Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8);
  }

  private static String printBlock() throws IOException {
    String css = css();
    int start = css.indexOf("@media print {");
    assertThat(start).as("인쇄 규칙이 없다").isGreaterThan(0);
    int end = css.indexOf("\n}", css.indexOf("break-inside: avoid;", start));
    assertThat(end).as("인쇄 규칙 블록의 끝을 찾지 못했다").isGreaterThan(start);
    return css.substring(start, end);
  }

  @Test
  void 고정_막대는_인쇄에서_빠진다() throws IOException {
    String block = printBlock();
    assertThat(block)
        .contains("header.navbar")
        .contains("nav.dock")
        .contains("display: none !important;");
  }

  /**
   * 화면에만 쓰는 조각은 종이에 찍히지 않는다.
   *
   * <p>실측 2026-09-10(qa/print-new-parts.cjs): 구역 막대가 3화면에, 행 선택 체크박스가 배당 14·매매 8·자산 현황 14개 찍혔고 토스트
   * 층(고정)이 화면마다 1개 남아 있었다. 누를 수 없는 막대와 빈 네모는 종이에서 쓸모가 없다.
   */
  @Test
  void 구역_막대와_선택_체크박스와_토스트는_인쇄에서_숨긴다() throws IOException {
    String block = printBlock();
    assertThat(block).as("구역 막대를 인쇄에서 숨기지 않는다").contains("[data-page-section-nav],");
    assertThat(block).as("행 선택 체크박스를 인쇄에서 숨기지 않는다").contains(".row-select-box,");
    assertThat(block).as("토스트 층을 인쇄에서 숨기지 않는다").contains("#app-toast,");
  }

  @Test
  void 가로_스크롤_표는_인쇄에서_펼쳐_잘리지_않는다() throws IOException {
    String block = printBlock();
    assertThat(block).contains(".overflow-x-auto").contains("overflow: visible !important;");
    assertThat(block).contains("width: 100% !important;");
    assertThat(block).contains("white-space: normal !important;");
  }

  @Test
  void 스티키와_쪽_나눔을_인쇄에_맞춘다() throws IOException {
    String block = printBlock();
    assertThat(block).contains("position: static !important;");
    assertThat(block).contains("break-inside: avoid;");
  }

  @Test
  void 인쇄_규칙은_화면_표시를_건드리지_않는다() throws IOException {
    String css = css();
    int printStart = css.indexOf("@media print {");
    String beforePrint = css.substring(0, printStart);
    assertThat(beforePrint)
        .as("화면용 독은 고정으로 남아 있어야 한다")
        .contains(".dock { position: fixed; left: 0; right: 0; bottom: 0;");
  }

  /**
   * 실측 2026-09-10(qa/print-dark.cjs): 채워진 버튼의 흰 글자가 배경 없는 인쇄에서 흰 종이에 사라졌다(라이트·다크 모두 대비 1.00, 5곳).
   */
  @Test
  void 채워진_버튼은_인쇄에서_읽히는_색으로_찍는다() throws IOException {
    String block = printBlock();
    assertThat(block).contains("color: var(--color-base-content) !important;");
    // 선택된 탭은 유틸 레이어의 !text-primary-content 를 이겨야 하므로 규칙이 따로 있다(같은 레이어·더 높은 특이성).
    int tabMarker = css().indexOf("선택된 탭은");
    assertThat(tabMarker).as("선택된 탭 인쇄 규칙 표식이 없다").isGreaterThan(0);
    String tabBlock =
        css().substring(tabMarker, css().indexOf("}", css().indexOf("box-shadow", tabMarker)));
    assertThat(tabBlock)
        .as("실측: 배당 '실수령 배당' 탭이 흰 글자라 종이에서 사라졌다(대비 1.00)")
        .contains("@layer utilities")
        .contains(".tab.tab-active")
        .contains("color: var(--color-base-content) !important;");
    assertThat(block).contains("background-color: transparent !important;");
  }

  /**
   * 실측 2026-09-10(qa/print-hidden-panels.cjs): 탭은 종이에서 누를 수 없어 선택 안 된 패널이 통째로 빠졌다 - 자산 성장 4패널 중 3개,
   * 표 21행 중 12행이 인쇄에서 사라졌다. 종이에는 모든 패널을 펼치고 탭 막대는 숨긴다.
   */
  @Test
  void 탭_패널은_인쇄에서_모두_펼친다() throws IOException {
    String block = printBlock();
    assertThat(block).contains("[data-panel-group]").contains("display: block !important;");
    assertThat(block).contains("[data-panel-tab-group]");
  }

  /** 펼쳐 놓으면 표가 나란히 찍히므로 각 패널에 자기 이름이 있어야 한다. */
  @Test
  void 배당_기간별_패널에는_각각_제목이_있다() throws IOException {
    String fragment =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendPeriodBreakdown.jte"),
            StandardCharsets.UTF_8);
    assertThat(fragment).contains("<h3 class=\"mb-2 text-sm font-bold\">${yearTabLabel}</h3>");
    assertThat(fragment).contains("<h3 class=\"mb-2 text-sm font-bold\">${monthTabLabel}</h3>");
  }
}
