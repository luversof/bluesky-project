package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 자산 현황 표는 종이에서도 열을 잃지 않는다.
 *
 * <p>실측 2026-09-11(816px, print, 전체 기간): 주식 화면을 통틀어 인쇄에서 빠지는 열이 자산 현황에만 33 칸 남아 있었다 &mdash; '계좌별
 * 현황' 과 펼친 '계좌 보유 종목 상세' 의 평균 단가 · 현재가 · 비중, '종목별 현황' 의 수량 · 현재가 · 합산 손익.
 *
 * <p>'종목별 현황' 은 11 열을 다 살리면 열 최소 폭 합이 824px 로 지면(782px)을 42px 넘는다. 셀 글자를 0.70rem 으로 내리면 722px 가 되어
 * 들어간다 (패딩만 3px 로 줄이면 802px 로 모자랐다). '계좌별 현황' 은 다 살려도 679px 라 그대로 둔다.
 */
class AssetStatusPrintColumnTest {

  private static final Path ASSET_STATUS =
      Path.of("src/main/jte/stock/htmx/fragments/assetStatus.jte");
  private static final Path MAIN_CSS = Path.of("src/main/frontend/main.css");
  private static final Path BUILT_CSS = Path.of("src/main/resources/static/main.css");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 폭에_따라_숨는_열은_인쇄에서_되살아난다() throws IOException {
    String template = read(ASSET_STATUS);

    Pattern hiddenCell = Pattern.compile("hidden (sm|md|lg|xl):table-cell(?! print:table-cell)");
    Matcher matcher = hiddenCell.matcher(template);
    int leftBehind = 0;
    while (matcher.find()) {
      leftBehind++;
    }
    assertThat(leftBehind).as("인쇄에서 되살리지 않은 열 칸 수").isZero();
    assertThat(template).as("되살린 칸이 실제로 있어야 한다").contains("print:table-cell");
  }

  @Test
  void 넓은_표만_한_단계_더_조인다() throws IOException {
    String template = read(ASSET_STATUS);
    String css = read(MAIN_CSS);

    assertThat(template)
        .as("종목별 현황(11열)에만 붙인다")
        .contains(
            "table table-sm w-full print-dense"
                + '"'
                + " data-sortable-table data-asset-status-stock-table");
    assertThat(template)
        .as("계좌별 현황은 다 살려도 679px 라 그대로 둔다")
        .doesNotContain(
            "print-dense" + '"' + " data-sortable-table data-asset-status-account-table");
    assertThat(css).contains("table.print-dense :is(th, td)");
    assertThat(css).as("셀 글자를 0.70rem 으로").contains("font-size: 0.70rem");
  }

  /**
   * 접어 둔 계좌 보유 상세는 종이에서 펼친다.
   *
   * <p>종이에는 누를 버튼이 없어서 접힌 채로 찍히면 그 내용이 통째로 사라진다 &mdash; 실측 2026-09-11(816px, print, 전체 기간): 자산
   * 현황에서만 표 5 개 · 행 23 개가 그렇게 빠졌다(다른 다섯 화면은 0). 이 상세는 계좌 x 종목 단위라 인쇄되는 '계좌별 현황' · '종목별 현황' 어느 쪽으로도
   * 되살릴 수 없다.
   */
  @Test
  void 접힌_계좌_상세는_종이에서_펼친다() throws IOException {
    String template = read(ASSET_STATUS);
    String css = read(MAIN_CSS);

    assertThat(template).as("펼칠 줄을 찾을 표식").contains("data-account-detail-row");
    assertThat(css).contains("tr[data-account-detail-row]");
    assertThat(css).as("종이에서 뜻이 없는 버튼은 감춘다").contains("[data-account-detail-toggle]");
  }

  @Test
  void 빌드된_css_에도_들어가_있다() throws IOException {
    String built = read(BUILT_CSS);
    assertThat(built).as("메이븐은 프론트엔드를 빌드하지 않는다 - npm run build 산출물이 배포본이다").contains("print-dense");
    assertThat(built).contains("data-account-detail-row");
  }
}
