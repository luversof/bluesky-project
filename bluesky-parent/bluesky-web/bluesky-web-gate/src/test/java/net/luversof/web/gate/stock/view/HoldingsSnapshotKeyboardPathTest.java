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
 * 보유 스냅샷은 마우스 없이도 열린다.
 *
 * <p>실측 2026-09-09: {@code /stock/htmx/holdings-snapshot} 은 자산성장·계좌상세 두 화면 모두 캔버스 점/마커 클릭으로만
 * 불렸다(Chart.js onClick, 캔버스 좌표 판정). 키보드·보조기술은 이 기능에 닿을 수 없었다(WCAG 2.1.1). 같은 조각을 날짜 입력 + 조회
 * 버튼(hx-get)으로 부르는 폼이 두 화면에 있어야 한다. 화면별 문맥(자산성장은 timeZone, 계좌상세는 accountId)은 숨은 입력으로 함께 보낸다.
 */
class HoldingsSnapshotKeyboardPathTest {

  private static final Path JTE = Path.of("src/main/jte");
  private static final Pattern FORM =
      Pattern.compile(
          "<form\\b[^>]*hx-get=\"/stock/htmx/holdings-snapshot\"[^>]*>(.*?)</form>",
          Pattern.DOTALL);

  private static String formOf(String relative) throws IOException {
    String src = Files.readString(JTE.resolve(relative), StandardCharsets.UTF_8);
    Matcher m = FORM.matcher(src);
    assertThat(m.find()).as(relative + " 에 보유 스냅샷 날짜 폼이 없다 - 차트 클릭(마우스)만으로 열린다").isTrue();
    String form = m.group();
    assertThat(form).contains("hx-target=\"#holdings-snapshot-container\"");
    assertThat(form).contains("type=\"date\"").contains("name=\"date\"").contains("required");
    assertThat(form).contains("type=\"submit\"");
    assertThat(form).contains("stock.holdings.snapshot.date.label");
    assertThat(src).contains("<div id=\"holdings-snapshot-container\">");
    return form;
  }

  @Test
  void 자산성장_화면은_타임존과_함께_날짜로_스냅샷을_연다() throws IOException {
    assertThat(formOf("stock/htmx/asset-growth.jte")).contains("name=\"timeZone\"");
  }

  @Test
  void 계좌상세_화면은_계좌id와_함께_날짜로_스냅샷을_연다() throws IOException {
    assertThat(formOf("stock/htmx/accountDetailContent.jte")).contains("name=\"accountId\"");
  }
}
