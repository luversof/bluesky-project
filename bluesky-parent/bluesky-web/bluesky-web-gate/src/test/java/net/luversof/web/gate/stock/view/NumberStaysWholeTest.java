package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 보통 글꼴 · 넓은 화면에서 금액이 갈리던 두 자리(자율 조사 2026-09-30, 주식 화면 전수 숫자 30,982 개).
 *
 * <ul>
 *   <li>표 안 금액의 부호: 유니코드 줄 나눔 규칙상 "-" 뒤 "₩" 앞이 줄을 바꿔도 되는 자리라 시뮬 지속가능성 표 15 칸이 1024~1920px 모두에서 "-"
 *       만 윗줄에 남았다. 2026-09-17 결정("표 안 금액은 안 끊기")은 자릿수 사이만 막았다.
 *   <li>복리 결과 카드: 열 수를 화면 폭(xl:grid-cols-4)으로 정해 1280px(입력 폼 옆 616px 자리)에서 카드 안이 92px -
 *       "₩70,122,/240".
 * </ul>
 */
class NumberStaysWholeTest {

  private static String squash(String value) {
    StringBuilder builder = new StringBuilder(value.length());
    for (int index = 0; index < value.length(); index++) {
      char c = value.charAt(index);
      if (c != 32 && c != 9 && c != 10 && c != 13) {
        builder.append(c);
      }
    }
    return builder.toString();
  }

  @Test
  void 가로_스크롤_상자_안_표의_금액은_부호까지_한_줄이다() throws IOException {
    String css =
        squash(Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8));

    assertThat(css)
        .contains(
            squash(
                ".overflow-x-auto:not(.table-head-sticky, .table-head-sticky-xl) > table .amount-value { white-space: nowrap; }"));
  }

  @Test
  void 금액과_한글_단위_사이에서_줄을_바꾸지_않는다() throws IOException {
    String css =
        squash(Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8));

    assertThat(css)
        .as(
            "대시보드 1280px 안내 문장이 \"2,343,989 / 원\" 으로 갈렸다 - keep-all 로 빈칸에서 바꾸고, 모자라면 anywhere 가 받는다")
        .contains(squash(".amount-value { overflow-wrap: anywhere; word-break: keep-all; }"));
  }

  /** 매매 요약 카드(자산 성장 · 매매 화면): 1280px · 글꼴 200% 에서 sm:grid-cols-3 가 세 칸으로 눌러 금액이 갈렸다. */
  @Test
  void 매매_요약_카드는_자리_폭으로_세_칸을_정한다() throws IOException {
    for (String path :
        java.util.List.of(
            "src/main/jte/stock/htmx/tradeHistory.jte",
            "src/main/jte/stock/htmx/fragments/trade/tradeSummaryCards.jte")) {
      assertThat(squash(Files.readString(Path.of(path), StandardCharsets.UTF_8)))
          .as(path)
          .contains(
              squash(
                  "<div class=\"@container mb-4\"><div class=\"grid grid-cols-1 @3xl:grid-cols-3 gap-3\">"))
          .doesNotContain(squash("sm:grid-cols-3 gap-3 mb-4"));
    }
  }

  /**
   * 입력 칸 라벨 줄(라벨 + 금액 미리보기)은 자리가 모자라면 금액을 다음 줄로 내린다(2026-09-30): 영어 "Recurring Contribution" 옆에서
   * "20,000,/000" 으로 갈렸다(1280 · 1440px). 같은 모양의 지속가능성 입력 줄도 함께.
   */
  @Test
  void 입력_라벨_줄은_금액을_다음_줄로_내린다() throws IOException {
    for (String path :
        java.util.List.of(
            "src/main/jte/stock/fragments/compoundSimulator.jte",
            "src/main/jte/stock/simulator.jte")) {
      String source = Files.readString(Path.of(path), StandardCharsets.UTF_8);
      assertThat(source).as(path).doesNotContain("<div class=\"label justify-between gap-3\">");
      assertThat(source)
          .as(path)
          .contains("<div class=\"label flex-wrap justify-between gap-x-3 gap-y-0\">");
    }
  }

  @Test
  void 복리_결과_카드는_자리_폭으로_열을_정한다() throws IOException {
    String template =
        squash(
            Files.readString(
                Path.of("src/main/jte/stock/fragments/compoundSimulator.jte"),
                StandardCharsets.UTF_8));

    assertThat(template)
        .contains(squash("<div class=\"space-y-6 @container\">"))
        .contains(squash("<div class=\"grid gap-4 @lg:grid-cols-2 @4xl:grid-cols-4\">"))
        .as("화면 폭 기준 네 칸은 폼 옆 좁은 자리에서도 네 칸으로 나눈다")
        .doesNotContain(squash("md:grid-cols-2 xl:grid-cols-4"));
  }

  /**
   * 배당 표 칸의 금액과 둘째 줄("연 6.10% · 보유 260일")이 태그 사이 빈칸 없이 붙어, 글자로 뽑으면 영어에서 "28,642,5956.10%" 처럼 두 수가
   * 이어졌다(2026-09-30 en-locale-values-vs-ko 48 건 - 영어 둘째 줄은 숫자로 시작한다). 화면은 두 줄이라 멀쩡해도 복사 · 검색 · 도구가
   * 읽는 글자는 이어진다 - 빈칸을 둔다.
   */
  @Test
  void 금액과_둘째_줄은_글자로도_떨어져_있다() throws IOException {
    String source =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte"),
            StandardCharsets.UTF_8);
    assertThat(source)
        .doesNotContain("</div><div class=\"text-[11px] whitespace-nowrap")
        .doesNotContain("</span><div class=\"text-[11px] whitespace-nowrap")
        .doesNotContain("</div><div class=\"text-[11px] font-normal whitespace-nowrap")
        .contains("</div> <div class=\"text-[11px] whitespace-nowrap\">");
  }
}
