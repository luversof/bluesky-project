package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.frontend.TsSource;

/**
 * 표 머리글 고정은 <b>세 가지가 맞아야</b> 먹는다(사용자 요청 2026-09-22: 스크롤을 올리면 열 이름이 올라가 버린다).
 *
 * <ol>
 *   <li><b>가로 스크롤 상자를 걷어낸다.</b> {@code .overflow-x-auto} 는 CSS 규칙상 {@code overflow-y} 도 {@code
 *       auto} 가 되어 그 상자가 sticky 의 기준이 되는데, 상자는 세로로 안 구르므로 머리글이 그냥 밀려난다(실측 2026-09-22: computed
 *       {@code position=sticky} 인데 스크롤 900 뒤 {@code top=-343}).
 *   <li><b>글자 줄바꿈을 풀어 준다.</b> 안 그러면 표가 자리에 안 들어가 문서에 가로 스크롤이 생긴다(실측 1440px: 표 1169 &rarr; 1134 =
 *       들어갈 자리와 같아짐).
 *   <li><b>sticky 는 {@code th} 에 건다.</b> {@code tr} 에 걸면 붙지 않는다.
 * </ol>
 *
 * <p>상자에 높이를 줘서 붙이는 길은 <b>쓰지 않는다</b> &mdash; 표 안에 세로 스크롤이 생겨 창 스크롤과 둘이 된다(사용자 지적 2026-09-22).
 */
class TableHeadStickyTest {

  private static final Path ETF = Path.of("src/main/jte/stock/monthlyEtf.jte");

  private static final Path CSS = Path.of("src/main/frontend/main.css");

  /** 긴 목록 셋 - 활동 314 행 · 매매 상세 258 행 · 배당 상세 211 행(실측 2026-09-22). */
  private static final Path ACTIVITY =
      Path.of("src/main/jte/stock/htmx/fragments/activityList.jte");

  private static final Path TRADE_DETAIL =
      Path.of("src/main/jte/stock/htmx/fragments/trade/tradeDetailList.jte");

  /** 머리글이 화면 밖으로 나가던 표 셋(2026-09-23). 볼 수 있는 높이 929px 에 견준 표 높이를 적어 둔다. */
  private static final Path ACCOUNT_DETAIL =
      Path.of("src/main/jte/stock/htmx/accountDetailContent.jte");

  private static final Path ITEM_DETAIL =
      Path.of("src/main/jte/stock/htmx/stockItemDetailContent.jte");

  private static final Path TRADE_REALIZED =
      Path.of("src/main/jte/stock/htmx/fragments/trade/tradeRealizedSections.jte");

  private static final Path DIVIDEND_DETAIL =
      Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendTable.jte");

  private static final Path BUILT_CSS = Path.of("src/main/resources/static/main.css");

  private static final Path COMMON_TS = Path.of("src/main/frontend/src/common.ts");

  private static final Path BUILT_JS = Path.of("src/main/resources/static/js/common.js");

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌거나 사라졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  /** {@code @media (min-width: 90rem)}(글꼴 100% 에서 1440px) 블록 안쪽만 떠서 돌려준다. */
  private String stickyMedia(String css) {
    int media = css.indexOf("@media (min-width: 90rem) {");
    if (media < 0) {
      return "";
    }

    int depth = 0;
    for (int index = media; index < css.length(); index++) {
      char c = css.charAt(index);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          return css.substring(media, index);
        }
      }
    }
    return css.substring(media);
  }

  /**
   * {@code @media (min-width: 80rem)} 블록 중 그 선택자를 품은 것만 떠서 돌려준다.
   *
   * <p>글꼴이 커지면 표도 넓어지므로 px 이 아니라 rem 으로 끓는다 &mdash; 80rem 은 기본 글꼴에서 1280px 이다. 비슷한 블록이 여럿이다 &mdash;
   * 제목 · 검색 줄 고정(.table-filter-sticky)이 먼저 나온다. 첫 블록만 뜨면 빈 문자열이 나와 가드가 아무 것도 못 본다(2026-09-22 실수).
   */
  private String wideMedia(String css, String selector) {
    int from = 0;
    while (true) {
      int media = css.indexOf("@media (min-width: 80rem) {", from);
      if (media < 0) {
        return "";
      }

      int depth = 0;
      int end = css.length();
      for (int index = media; index < css.length(); index++) {
        char c = css.charAt(index);
        if (c == 123) {
          depth++;
        } else if (c == 125) {
          depth--;
          if (depth == 0) {
            end = index;
            break;
          }
        }
      }
      String block = css.substring(media, end);
      if (block.contains(selector + " {")) {
        return block;
      }
      from = end + 1;
    }
  }

  /** 미디어 블록 안에서 선택자 하나의 규칙 본문만 떠서 돌려준다. */
  private String rule(String block, String selector) {
    int at = block.indexOf(selector + " {");
    if (at < 0) {
      return "";
    }

    int close = block.indexOf(125, at);
    return close < 0 ? block.substring(at) : block.substring(at, close);
  }

  @Test
  void 세_가지가_한_덩이로_들어가_있다() throws IOException {
    assertThat(read(ETF))
        .as("표에 고정 클래스가 없으면 아무 일도 일어나지 않는다")
        .contains("class=\"overflow-x-auto table-head-sticky\"");

    String block = stickyMedia(read(CSS));
    assertThat(block).as("1440px 미디어 쿼리가 없다").isNotEmpty();
    assertThat(block).as("가로 스크롤 상자를 안 걷어내면 sticky 가 그 상자에 갇힌다").contains("overflow-x: visible");
    // ":where(th, td)" 에도 같은 줄이 있어, 그냥 찾으면 표 규칙을 nowrap 으로 바꿔도 통과한다.
    assertThat(rule(block, ".table-head-sticky > table"))
        .as("줄바꿈을 안 풀면 표가 자리에 안 들어가 문서에 가로 스크롤이 생긴다")
        .contains("white-space: normal")
        .doesNotContain("nowrap");
    assertThat(block)
        .as("sticky 는 tr 이 아니라 th 에 걸어야 한다")
        .contains("> thead > tr > th {")
        .contains("position: sticky");
  }

  @Test
  void 한국어는_낱말_안에서_자르지_않는다() throws IOException {
    String block = stickyMedia(read(CSS));

    assertThat(block).as("종목명이 낱말 가운데서 끊긴다").contains("word-break: keep-all");
    assertThat(block).as("더 못 줄일 때만 끊게 해야 자리 안에 들어온다").contains("overflow-wrap: anywhere");
  }

  /**
   * 숫자는 자릿수 가운데서 끊지 않는다(2026-09-30). anywhere 만 두면 열이 는 뒤 1440px 에서 숫자 58 곳이 "31.6 / 6%" 처럼 갈렸다.
   * 그리고 px 이 아니라 rem 으로 끊는다 - 1440px 로 두면 글꼴 200% 에서도 상자가 걷혀 창이 가로로 굴렀다(문서 1483 / 창 1440).
   */
  @Test
  void 숫자_칸은_자릿수_가운데서_끊지_않고_큰_글꼴에서는_상자가_돌아온다() throws IOException {
    String css = read(CSS);
    String block = stickyMedia(css);

    assertThat(rule(block, ".table-head-sticky > table td.text-right *"))
        .as("오른쪽 정렬 숫자 칸은 anywhere 를 푼다")
        .contains("overflow-wrap: normal");
    assertThat(rule(block, ".table-head-sticky > table td[data-keep-line]"))
        .as("지급 시기 두 글자가 세로로 흐르지 않게 - nowrap 은 영어 Month-end 가 열을 민다")
        .contains("overflow-wrap: normal");
    assertThat(read(ETF)).contains("<td class=\"align-top\" data-keep-line>${windowLabel}</td>");
    assertThat(css)
        .as("px 로 끊으면 글꼴을 키워도 상자가 걷힌 채라 창이 가로로 구른다")
        .doesNotContain("@media (min-width: 1440px) {");
  }

  @Test
  void 머리글은_고정_줄_아래에_놓인다() throws IOException {
    // 고정 줄(제목·검색·기간) 높이는 글월 길이 · 로케일 · 폭에 따라 달라져 CSS 로는 알 수 없다
    // (실측 2026-09-22: 1440px 216px · 1024px 336px · 390px 515px). 그래서 스크립트가 알려 준다.
    assertThat(stickyMedia(read(CSS)))
        .as("상단바 높이만 쓰면 머리글이 고정 줄에 깔린다")
        .contains("var(--table-head-sticky-top");
    // 이름은 주석에도 있다 - 실제로 넣는 줄을 본다.
    assertThat(TsSource.n(read(COMMON_TS)))
        .as("변수를 아무도 안 채우면 기본값(상단바 높이)만 쓰여 겹친다")
        .contains(TsSource.n("setProperty(\"--table-head-sticky-top\", top + \"px\")"))
        .contains(TsSource.n(".table-filter-sticky"));
    assertThat(TsSource.n(read(COMMON_TS)))
        .as("붙지 않는 폭에서는 그 줄이 자리를 차지하지 않는다 - position 으로 가려야 한다")
        .contains(TsSource.n("getComputedStyle(holder).position"));
  }

  @Test
  void 표_안에_세로_스크롤을_만들지_않는다() throws IOException {
    // 창 스크롤과 표 스크롤이 둘 다 생기면 굴릴 때마다 어느 쪽이 움직일지 모른다(사용자 지적).
    assertThat(read(ETF)).doesNotContain("table-pin-panel");
    assertThat(read(CSS)).as("쓰이지 않는 판 클래스가 남아 있다").doesNotContain("table-pin-panel");
    assertThat(stickyMedia(read(CSS))).as("높이 상한을 주면 표가 제 스크롤을 갖는다").doesNotContain("max-height");
  }

  /**
   * 긴 목록 셋(활동 · 매매 상세 · 배당 상세)도 같은 방법으로 붙인다 &mdash; 사용자 결정 2026-09-22: 1280px 이상.
   *
   * <p>이 표들은 {@code table-pin-rows pin-top-nav} 로 고정을 <b>약속만</b> 하고 있었다. 실측 2026-09-22: 세 표 모두 머리글이
   * 본문과 똑같이 700px 떠내려갔다 &mdash; 가로 스크롤 상자가 sticky 를 가두고 있었기 때문이다.
   */
  @Test
  void 긴_목록_셋에도_고정_상자가_붙어_있다() throws IOException {
    assertThat(read(ACTIVITY))
        .as("활동 목록 314 행 - 열 이름 없이는 어느 칸이 무엇인지 모른다")
        .contains("class=\"overflow-x-auto table-head-sticky-xl\"");
    assertThat(read(TRADE_DETAIL))
        .as("매매 상세 258 행")
        .contains("overflow-x-auto table-head-sticky-xl");
    assertThat(read(DIVIDEND_DETAIL))
        .as("배당 상세 211 행")
        .contains("overflow-x-auto table-head-sticky-xl");
  }

  /** 1280px 규칙도 월배당 ETF 와 같은 두 가지(상자 걷기 · th 에 sticky)를 해야 한다. */
  @Test
  void 긴_목록은_넓을_때만_붙고_글꼴이_커지면_꺼진다() throws IOException {
    String block = wideMedia(read(CSS), ".table-head-sticky-xl");
    assertThat(block)
        .as("80rem(기본 글꼴 1280px) 미디어 쿼리가 없다 - 글꼴 200% 에서 꾼지려면 rem 이어야 한다")
        .isNotEmpty();
    assertThat(rule(block, ".table-head-sticky-xl"))
        .as("가로 스크롤 상자를 안 걷어내면 sticky 가 그 상자에 갇힌다")
        .contains("overflow-x: visible")
        .contains("overflow-y: visible");
    assertThat(rule(block, ".table-head-sticky-xl > table > thead > tr > th"))
        .as("sticky 는 tr 이 아니라 th 에 걸어야 한다")
        .contains("position: sticky")
        .as("상단 바 뒤에 멈추면 붙어도 안 보인다 - table-pin-rows 의 top: 0 이 그랬다")
        .contains("top: var(--table-head-sticky-top")
        .as("머리글 아래로 본문 행이 비치면 읽힐 수 없다")
        .contains("background-color: var(--color-base-100)");
    assertThat(rule(block, ".table-head-sticky-xl"))
        .as("높이 상한을 주면 표가 제 스크롤을 갖는다(창 스크롤과 둘)")
        .doesNotContain("max-height");
  }

  /**
   * 상세 화면과 실현손익 표도 창보다 길다(사용자 결정 2026-09-23).
   *
   * <p>실측: 계좌 상세 매매 118 행 4018px · 배당 67 행 2262px · 매매 종목별 실현손익 37 행 1390px 인데 한 화면에서 볼 수 있는 높이는
   * 929px 이다. 앞의 둘은 {@code table-pin-rows} 로 고정을 약속만 하고 있었다. 창 안에 들어오는 짧은 표(5 ~ 18 행)는 일부러 안 붙인다 -
   * 고쳐도 얻는 것이 없다.
   */
  @Test
  void 창보다_긴_상세_표에도_고정_상자가_붙어_있다() throws IOException {
    String account = read(ACCOUNT_DETAIL);

    assertThat(boxOf(account, "stock.page.trade.title"))
        .as("계좌 상세 매매 118 행 4018px - 굴리면 열 이름이 사라진다")
        .contains("table-head-sticky-xl");
    assertThat(boxOf(account, "stock.page.dividend.title"))
        .as("계좌 상세 배당 67 행 2262px")
        .contains("table-head-sticky-xl");
    assertThat(boxOf(account, "stock.account.detail.holdings.title"))
        .as("보유 종목 표는 짧아 안 붙였다 - 범위를 넘기면 고칠 자리만 늘어난다")
        .doesNotContain("table-head-sticky-xl");
    assertThat(read(TRADE_REALIZED))
        .as("종목별 실현손익 37 행 1390px - 여기는 카드 컬포넌트가 상자 노릇을 한다")
        .contains("tableCard(cssClass = \"overflow-x-auto table-head-sticky-xl\"");
  }

  /**
   * 종목 상세도 계좌 상세와 같은 표라 같은 상자를 쓴다(두 화면이 같은 답을 내야 한다).
   *
   * <p>실측 2026-09-23(전체 기간, 볼 수 있는 높이 929px): 매매가 가장 많은 종목 매매 33 행 1147px · 배당 36 행 1230px, 배당이 가장
   * 많은 종목 매매 29 행 1012px · 배당 49 행 1663px 인데 넷 다 머리글이 떠내려갔다. 전에는 탐침이 첫 거래 종목(5 행)만 재서 "지금 데이터로는 필요
   * 없다" 로 잘못 넘겼다.
   */
  @Test
  void 종목_상세의_긴_표도_계좌_상세와_같이_붙는다() throws IOException {
    String item = read(ITEM_DETAIL);

    assertThat(boxOf(item, "stock.page.trade.title"))
        .as("종목 상세 매매 33 행 1147px")
        .contains("<div class=\"overflow-x-auto table-head-sticky-xl\">");
    assertThat(boxOf(item, "stock.page.dividend.title"))
        .as("종목 상세 배당 49 행 1663px")
        .contains("<div class=\"overflow-x-auto table-head-sticky-xl\">");
    assertThat(boxOf(item, "stock.item.detail.account.holdings.title"))
        .as("계좌별 보유 표는 계좌 수만큼(5 행)이라 짧다 - 계좌 상세의 보유 표처럼 안 붙인다")
        .doesNotContain("table-head-sticky-xl");
  }

  /**
   * 표 이름(메시지 키) 앞쪽을 떠서 그 표를 감싼 상자를 본다.
   *
   * <p>줄바꿈 · 들여쓰기에 묶인 긴 단언은 서식이 조금만 바뀌어도 깨진다(2026-09-23 실수).
   */
  private String boxOf(String template, String messageKey) {
    // 같은 키가 섹션 제목(h2)에도 쓰인다 - 표 이름(aria-label)으로 쓰인 자리를 골라야 한다.
    int at = template.indexOf(messageKey);
    while (at >= 0) {
      String before = template.substring(Math.max(0, at - 120), at);
      if (before.contains("aria-label")) {
        return template.substring(Math.max(0, at - 400), at);
      }
      at = template.indexOf(messageKey, at + 1);
    }
    throw new AssertionError(messageKey + " 를 표 이름으로 쓰는 자리를 못 찾았다");
  }

  /** 창 안에 들어오는 짧은 표에는 붙이지 않았다 - 범위를 넘겨 고치면 고칠 자리만 늘어난다. */
  @Test
  void 짧은_표에는_안_붙였다() throws IOException {
    assertThat(read(TRADE_REALIZED))
        .as("계좌별 실현손익은 6 행 319px 로 한 화면에 들어온다")
        .contains("tableCard(cssClass = \"overflow-x-auto\", body");
  }

  /**
   * 붙은 머리글 아래 본문 칸에 포커스가 가면 필터 줄 · 머리 줄 밑으로 숨지 않아야 한다(WCAG 2.4.11).
   *
   * <p>실측 2026-09-23(1440px, 월배당 ETF): Shift+Tab 으로 되짚은 종목 링크 21 개가 필터 줄(216px) · 머리 줄 뒤에 전부 가렸다.
   * scroll-padding-top 은 상단바 + 구역 막대만 비운다. 모자란 만큼(필터 줄 + 머리 줄)을 본문 칸의 scroll-margin-top 으로 더한다. 머리
   * 줄 높이는 폭 · 글꼴로 바뀌어 스크립트가 표 상자마다 알린다.
   */
  @Test
  void 붙은_머리글_아래로_포커스가_숨지_않는다() throws IOException {
    String css = read(CSS);
    assertThat(rule(stickyMedia(css), ".table-head-sticky > table > tbody *"))
        .as("월배당 ETF 표(1440px 부터 붙음)")
        .contains(
            "scroll-margin-top: calc(var(--table-head-sticky-top, var(--site-header-height, 4.4375rem)) + var(--sticky-thead-height, 0px) + 0.5rem - var(--sticky-top-stack));");
    assertThat(
            rule(
                wideMedia(css, ".table-head-sticky-xl > table > tbody *"),
                ".table-head-sticky-xl > table > tbody *"))
        .as("긴 목록 · 상세 표(80rem 부터 붙음) - 붙지 않는 좁은 폭에서 여백을 더하면 쓸데없이 더 굴린다")
        .contains(
            "scroll-margin-top: calc(var(--table-head-sticky-top, var(--site-header-height, 4.4375rem)) + var(--sticky-thead-height, 0px) + 0.5rem - var(--sticky-top-stack));");
    assertThat(css.substring(0, css.indexOf("@media (min-width: 90rem) {")))
        .as("미디어 쿼리 밖에 두면 머리글이 안 붙는 폭에서도 여백이 생긴다")
        .doesNotContain("tbody * {");

    // 붙은 필터 줄 아래 형제(보기 단추 등)도 그 줄 밑으로 숨었다(같은 날 4 개) - 필터 줄이 붙는 1280px 블록 안에 둔다.
    int filterMedia = css.indexOf("@media (min-width: 1280px) {");
    String filterBlock = css.substring(filterMedia, css.indexOf("@media", filterMedia + 1));
    assertThat(filterBlock).as("필터 줄이 붙는 블록").contains(".table-filter-sticky {");
    assertThat(
            rule(
                filterBlock,
                ".table-filter-sticky ~ :not(.table-head-sticky, .table-head-sticky-xl) *"))
        .as("표 상자는 빼고(본문은 머리 줄까지 더한 여백을 따로 받는다) 형제 안 요소에 필터 줄 높이만큼")
        .contains(
            "scroll-margin-top: calc(var(--table-head-sticky-top, var(--site-header-height, 4.4375rem)) + 0.5rem - var(--sticky-top-stack));");
    assertThat(read(BUILT_CSS))
        .as("npm run build")
        .contains(
            ".table-filter-sticky~:not(.table-head-sticky,.table-head-sticky-xl) *{scroll-margin-top:calc(var(--table-head-sticky-top,var(--site-header-height,4.4375rem)) + .5rem - var(--sticky-top-stack))}");

    String ts = read(COMMON_TS);
    String sync =
        ts.substring(
            ts.indexOf("function syncStickyStackTop(): void {"),
            ts.indexOf("function syncStickyTheadHeights(): void {"));
    assertThat(sync).as("고정 줄 높이를 다시 잴 때 머리 줄 높이도 다시 잰다").contains("syncStickyTheadHeights();");
    assertThat(TsSource.n(ts))
        .contains(TsSource.n("box.style.setProperty(\"--sticky-thead-height\", height + \"px\");"))
        .contains(
            TsSource.n(
                "document.querySelectorAll(\":is(.table-head-sticky, .table-head-sticky-xl) > table > thead\").forEach((head) => observer.observe(head));"));

    assertThat(read(BUILT_CSS))
        .as("npm run build")
        .contains(
            ".table-head-sticky>table>tbody *{scroll-margin-top:calc(var(--table-head-sticky-top,var(--site-header-height,4.4375rem)) + var(--sticky-thead-height,0px) + .5rem - var(--sticky-top-stack))}")
        .contains(
            ".table-head-sticky-xl>table>tbody *{scroll-margin-top:calc(var(--table-head-sticky-top,var(--site-header-height,4.4375rem)) + var(--sticky-thead-height,0px) + .5rem - var(--sticky-top-stack))}");
    assertThat(read(BUILT_JS)).as("npm run build").contains("--sticky-thead-height");
  }

  @Test
  void 배포본에도_들어_있다() throws IOException {
    // 메이븐은 프론트엔드를 빌드하지 않는다 - 커밋된 산출물이 배포본이다.
    // contains 는 이름 접두만 봐도 통과한다(...stickyX) - 규칙·문자열 자체를 본다.
    assertThat(read(BUILT_CSS))
        .as("npm run build 를 안 돌리면 화면에는 아무 변화가 없다")
        .contains(".table-head-sticky{")
        .contains(".table-head-sticky>table")
        .contains(".table-head-sticky-xl{")
        .contains(".table-head-sticky-xl>table>thead>tr>th{");
    assertThat(read(BUILT_JS))
        .as("스크립트를 안 빌드하면 고정 줄 높이가 안 들어와 머리글이 겹친다")
        .contains("\"--table-head-sticky-top\",top");
  }

  /**
   * 1440px 월배당 ETF 표가 상자를 36px 넘었다(2026-09-30): 숫자 · 날짜를 한 덩어리로 묶고 점수 열을 더한 뒤 열 최소 폭 합이 1,170px. 날짜
   * 바로 뒤 "부터" 가 붙어 "2026-06-23부터"(118px)가 한 덩어리였다 - 날짜 뒤에 wbr. 그리고 "지급 시기" 머리칸이 두 글자 칸 폭에 눌려 한 글자씩
   * 세로로 흘렀다 - 한국어에서만 빈칸에서 끊는다(영어 "Payout timing" 은 같은 규칙이면 27px 넘침).
   */
  @Test
  void 월배당_ETF_표는_1440px_상자_안에_들어간다() throws IOException {
    String block = stickyMedia(read(CSS));
    assertThat(block)
        .contains(":lang(ko) .table-head-sticky > table th[data-keep-words]")
        .contains("overflow-wrap: normal;");
    assertThat(read(ETF))
        .contains(
            "<th scope=\"col\" data-keep-words class=\"align-bottom\" aria-sort=\"${\"payout-window\"");
  }
}
