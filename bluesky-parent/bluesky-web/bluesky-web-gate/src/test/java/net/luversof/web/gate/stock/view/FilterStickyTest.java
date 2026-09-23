package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 월배당 ETF 목록의 검색 · 기간 줄을 헤더 아래에 붙인다(사용자 요청 2026-09-22).
 *
 * <p>목록이 21 행이 되면서 좁히려면 맨 위까지 되돌아가야 했다. 그래서 그 줄만 붙인다.
 *
 * <p><b>표 안쪽에는 스크롤을 만들지 않는다.</b> 머리글을 붙이려면 표를 감싼 상자에 높이를 줘야 하는데, 그러면 창 스크롤과 표 스크롤이 둘 다 생겨 굴릴 때마다 어느
 * 쪽이 움직일지 모른다(사용자 지적 2026-09-22 &mdash; 그 방식은 되돌렸다).
 *
 * <p><b>1280px 부터만 붙인다.</b> 실측 2026-09-22(높이 900px · 행 높이 101px): 붙는 줄 높이가 768~1180px 에서는 256px 라
 * 보이는 줄이 8 &rarr; 5 로 줄지만, 1280px 부터는 필터가 한 줄(5 열)로 접혀 144px &rarr; 8 &rarr; 6 이다.
 */
class FilterStickyTest {

  private static final Path ETF = Path.of("src/main/jte/stock/monthlyEtf.jte");

  private static final Path CSS = Path.of("src/main/frontend/main.css");

  private static final Path BUILT_CSS = Path.of("src/main/resources/static/main.css");

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌거나 사라졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 검색과_기간_줄이_한_상자에_묶여_있다() throws IOException {
    String template = read(ETF);

    assertThat(template)
        .as("붙일 상자가 없으면 card-body 를 통째로 붙이게 되고, 그건 1,745px 을 덮는다(실측)")
        .contains("class=\"table-filter-sticky space-y-3\"");

    // 순서만 보면 상자가 기간 줄 앞에서 닫혀도 통과한다(변이가 그렇게 살아남았다).
    // 상자가 어디서 닫히는지 세어 둘이 정말 그 안에 있는지 본다.
    String inside = stickyBlock(template);
    assertThat(inside).as("검색 폼이 상자 안에 없다").contains("action=\"/stock/monthly-etf\"");
    assertThat(inside).as("기간 줄이 상자 안에 없다").contains("role=\"group\"");
    // 사용자 요청 2026-09-22: "sticky 가 테이붔 제목도 유지해주었으면 해" - 어느 표를 보는 중인지가 사라진다.
    assertThat(inside).as("표 제목이 상자 안에 없다").contains("class=\"card-title text-lg\"");
  }

  /** table-filter-sticky 상자가 여는 지점부터 그것이 닫힐 때까지의 속을 돌려준다. */
  private static String stickyBlock(String template) {
    int open = template.indexOf("<div class=\"table-filter-sticky");
    if (open < 0) {
      return "";
    }

    int depth = 0;
    int index = open;
    while (index < template.length()) {
      int nextOpen = template.indexOf("<div", index);
      int nextClose = template.indexOf("</div>", index);
      if (nextClose < 0) {
        break;
      }
      if (nextOpen >= 0 && nextOpen < nextClose) {
        depth++;
        index = nextOpen + 4;
        continue;
      }
      depth--;
      if (depth <= 0) {
        return template.substring(open, nextClose);
      }
      index = nextClose + 6;
    }
    return template.substring(open);
  }

  @Test
  void 표에는_높이를_주지_않는다() throws IOException {
    // 표 안 스크롤과 창 스크롤이 둘이 되면 굴릴 때마다 어느 쪽이 움직일지 모른다(사용자 지적).
    String template = read(ETF);
    String css = read(CSS);

    assertThat(template).as("표 상자에 판 높이를 주면 스크롤이 둘이 된다").doesNotContain("table-pin-panel");
    assertThat(css).as("쓰이지 않는 판 클래스가 남아 있다").doesNotContain("table-pin-panel");
    // 상자에 높이를 안 줌만 본다 - 넓은 화면에서 가로 스크롤을 걷는 것은 머리글 고정이 하는
    // 일이다(TableHeadStickyTest 가 그쪽을 본다).
    assertThat(template).as("표 상자는 가로 스크롤만 맡는다").contains("<div class=\"overflow-x-auto");
  }

  @Test
  void 좁은_화면에서는_붙이지_않는다() throws IOException {
    String css = read(CSS);
    int at = css.indexOf(".table-filter-sticky");
    assertThat(at).as(".table-filter-sticky 규칙이 없다").isGreaterThanOrEqualTo(0);

    // 규칙이 미디어 쿼리 안에 있어야 한다 - 밖에 있으면 390px 에서 375px 를 덮는다.
    String before = css.substring(0, at);
    int media = before.lastIndexOf("@media");
    assertThat(media).as("미디어 쿼리 밖에 있다").isGreaterThanOrEqualTo(0);
    assertThat(before.substring(media))
        .as("1280px 아래에서는 필터가 2 열로 접혀 256px 를 덮는다(보이는 줄 8 -> 5)")
        .contains("min-width: 1280px");
  }

  @Test
  void 배포본_css_에도_들어_있다() throws IOException {
    // 메이븐은 프론트엔드를 빌드하지 않는다 - 커밋된 산출물이 배포본이다.
    assertThat(read(BUILT_CSS))
        .as("npm run build 를 안 돌리면 화면에는 아무 변화가 없다")
        .contains(".table-filter-sticky{");
    assertThat(read(BUILT_CSS))
        .as("배포본의 경계도 1280px 이어야 한다")
        .contains("min-width:1280px){.table-filter-sticky");
  }
}
