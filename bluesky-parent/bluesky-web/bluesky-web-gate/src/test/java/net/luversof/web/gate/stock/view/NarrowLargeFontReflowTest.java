package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 좁은 화면 + 큰 글꼴(320px, 브라우저 기본 글꼴 32px)에서 페이지가 가로로 밀리지 않는다.
 *
 * <p>실측 2026-09-17: 도넛 범례(DonutLegendVisibleTest) 말고도 다섯 자리가 페이지를 밀었다 &mdash; 대시보드 카드 머리(아이콘 칩 +
 * 화살표, 36px) · 관리 갱신 단추("(Dividends)" 한 단어, 33px) · 보유 스냅샷 날짜 칸(자산 성장 14px, 계좌 상세 같은 폼) · 월배당
 * "시뮬레이터 반영" 단추(3px) · 지속가능성 시나리오 카드의 '현재 편집' 배지(41px). 모두 "한 줄에 밀어붙임" 이라 자리마다 줄을 바꾸거나 줄어들 수 있게 했다.
 *
 * <p>큰 글꼴은 브라우저 설정으로 흉내 내야 한다 &mdash; 문서에 {@code html{font-size:200%}} 를 넣으면 화면 폭 기준(rem 미디어 쿼리)이
 * 16px 그대로라 640 · 1024px 에서 실제로는 안 나오는 3 열 · 사이드바 배치가 켜진 채 넘침이 잡혔다(설정 방식으로는 0px).
 *
 * <p>줄을 바꾸게 한 단추는 한글 단어를 지킨다(break-keep) &mdash; 안 주면 "시뮬레 / 이터 반 / 영" 으로 끊겼다. 소스는 공백을 누르고 본다.
 */
class NarrowLargeFontReflowTest {

  private static String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  private static String squash(String text) {
    return text.replaceAll("\\s+", " ");
  }

  private static int count(String haystack, String needle) {
    int n = 0;
    for (int at = haystack.indexOf(needle);
        at >= 0;
        at = haystack.indexOf(needle, at + needle.length())) {
      n++;
    }
    return n;
  }

  /** 카드 안쪽이 5rem 보다 좁으면 장식 칩 · 화살표를 뺀다. 기본 규칙 뒤에 있어야 이긴다(앞에 뒀더니 칩이 남았다). */
  @Test
  void 좁은_카드는_칩과_화살표를_뺀다() throws IOException {
    String css = squash(read("src/main/frontend/main.css").replaceAll("(?s)/\\*.*?\\*/", ""));
    int rule =
        css.indexOf(
            "@container (max-width: 5rem) { .stat-chip, .stat-card-go { display: none; } }");
    assertThat(rule).as("원본에 칩 · 화살표를 빼는 컨테이너 규칙이 없다").isGreaterThan(0);
    assertThat(rule)
        .as("칩 기본 규칙(display: inline-flex) 뒤에 있어야 같은 우선순위에서 이긴다")
        .isGreaterThan(css.indexOf(".stat-chip {"));

    String built = read("src/main/resources/static/main.css");
    int builtRule =
        built.indexOf("@container (max-width:5rem){.stat-chip,.stat-card-go{display:none}}");
    assertThat(builtRule).as("산출물에 규칙이 없다 - npm run build").isGreaterThan(0);
    assertThat(builtRule).isGreaterThan(built.indexOf(".stat-chip{"));
  }

  /** 도넛 카드 제목 줄은 탭을 아래로 내릴 수 있다 - 안 그러면 제목이 한 글자씩 세로로 쌓였다. */
  @Test
  void 도넛_카드_제목_줄이_줄을_바꾼다() throws IOException {
    assertThat(squash(read("src/main/jte/stock/htmx/fragments/components/doughnutShell.jte")))
        .contains("<div class=\"flex flex-wrap items-center justify-between gap-2 mb-2\">");
  }

  /** 관리 갱신 단추 넷은 좁은 폭에서 줄을 바꾸되 한글 단어는 지키고, 한 단어가 단추보다 넓을 때만 그 안에서 끊는다. */
  @Test
  void 관리_단추는_좁은_폭에서_단어를_지키며_줄을_바꾼다() throws IOException {
    String admin = read("src/main/jte/stock/htmx/fragments/adminActions.jte");
    int wrapping = count(admin, "max-sm:whitespace-normal");
    assertThat(wrapping).as("줄을 바꾸는 단추를 못 찾았다 - 검사가 무력하다").isGreaterThanOrEqualTo(4);
    assertThat(count(admin, "max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere"))
        .as("줄을 바꾸는 단추마다 단어 지키기 + 넘칠 때만 끊기")
        .isEqualTo(wrapping);

    String built = read("src/main/resources/static/main.css");
    assertThat(built)
        .contains(".max-sm\\:break-keep{word-break:keep-all}")
        .contains(".max-sm\\:wrap-anywhere{overflow-wrap:anywhere}");
  }

  /** 보유 스냅샷 날짜 칸(자산 성장 · 계좌 상세)은 카드 안쪽보다 넓어지지 않는다. */
  @Test
  void 스냅샷_날짜_칸이_줄어들_수_있다() throws IOException {
    for (String path :
        List.of(
            "src/main/jte/stock/htmx/asset-growth.jte",
            "src/main/jte/stock/htmx/accountDetailContent.jte")) {
      String jte = squash(read(path));
      int label = jte.indexOf("stock.holdings.snapshot.date.label");
      assertThat(label).as(path).isGreaterThan(0);
      String form = jte.substring(jte.lastIndexOf("<form", label), jte.indexOf("</form>", label));
      assertThat(form)
          .as(path + ": 라벨과 날짜 칸이 폼보다 넓어지면 페이지를 민다")
          .contains("<label class=\"form-control min-w-0 max-w-full\">")
          .contains(
              "<input type=\"date\" name=\"date\" required class=\"input input-sm input-bordered max-w-full\">");
    }
  }

  @Test
  void 월배당_반영_단추는_단어를_지키며_줄을_바꾼다() throws IOException {
    assertThat(squash(read("src/main/jte/stock/fragments/monthlyDividendSimulator.jte")))
        .contains(
            "class=\"btn btn-primary w-full max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere\"");
  }

  /**
   * 월배당 ETF 의 "지금 눈여겨볼 종목" 카드 · "이번 적립만 보기" 단추(2026-09-22 · 23 에 넣음).
   *
   * <p>실측 2026-09-23(320px + 글꼴 32px): 문서가 47px 넘쳤다. 상자 넷(화면 · 카드 본문 · 추천 상자 · 추천 카드)의 안쪽 여백이 겹쳐 카드
   * 자리가 92px 뿐인데 카드는 253px 이 필요했고, 단추는 한 줄 고정이라 262px 이었다. 좁은 폭에서 여백을 줄이고 줄을 바꾸게 해 0px.
   */
  @Test
  void 월배당_ETF_추천_카드와_이번_적립_단추가_좁은_폭에서_줄어든다() throws IOException {
    String etf = squash(read("src/main/jte/stock/monthlyEtf.jte"));

    assertThat(etf)
        .contains(
            "<div class=\"rounded-box border border-base-300 bg-base-200/40 p-2 sm:p-4 space-y-3\" data-etf-picks>")
        .contains("<div class=\"rounded-box bg-base-100 p-2 sm:p-3 space-y-1\" data-etf-pick ")
        .contains(
            "<div class=\"flex flex-wrap items-baseline gap-2\"> <span class=\"badge badge-sm badge-neutral\" data-pick-rank>")
        .contains(
            "<div class=\"flex flex-wrap items-baseline gap-1\"> <span class=\"text-lg font-semibold tabular-nums\" data-pick-score>")
        .contains(
            "\"btn-primary\" : \"btn-outline\"} max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere\" aria-current=\"${\"contribution\".equals(monthlyEtfView) ? \"page\" : null}\" data-contribution-view-link=\"contribution\"")
        .contains(
            "\"btn-outline\" : \"btn-primary\"} max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere\" aria-current=\"${\"contribution\".equals(monthlyEtfView) ? null : \"page\"}\" data-contribution-view-link=\"all\"");
  }

  /** 시나리오 카드 머리는 배지를 아래로 내릴 수 있다. 브라우저가 읽는 것은 빌드한 스크립트다. */
  @Test
  void 시나리오_카드_머리가_줄을_바꾼다() throws IOException {
    String head = "<div class=\"flex flex-wrap items-start justify-between gap-3\">";
    assertThat(read("src/main/frontend/src/stock/stockSimulator.ts")).contains(head);
    assertThat(read("src/main/resources/static/js/stock/stockSimulator.js"))
        .as("메이븐은 프론트엔드를 빌드하지 않는다 - npm run build 산출물이 배포본이다")
        .contains(head);
  }
}
