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
 * 320px + 텍스트 간격(WCAG 1.4.12: 줄간격 1.5·자간 0.12em·어간 0.16em)에서도 문서가 가로로 넘치지 않는다.
 *
 * <p>실측 2026-09-10(qa/text-spacing.cjs·text-spacing-offenders.cjs, en 320px): 자산 성장 안내 문구의 {@code
 * whitespace-nowrap}(+26px), 시뮬레이터 도구 배지(+4px), 관리 > 월배당 기준 출처 가져오기 버튼(+64px, 간격 없이도 +1px), 종목 상세
 * 지표 카드 2열의 13자리 금액(+15px)이 문서 폭을 넘겼다. 나머지 9화면은 두 로케일 모두 0.
 */
class TextSpacingReflowTest {

  private static final Path JTE = Path.of("src/main/jte");

  private static String read(String rel) throws IOException {
    return Files.readString(JTE.resolve(rel), StandardCharsets.UTF_8);
  }

  private static String tagContaining(String html, String marker) {
    Matcher m =
        Pattern.compile("<[a-z]+\\b[^>]*>(?=[^<]*" + Pattern.quote(marker) + ")").matcher(html);
    assertThat(m.find()).as("표식을 감싸는 태그를 찾지 못했다: " + marker).isTrue();
    return m.group();
  }

  @Test
  void 자산_성장_날짜_클릭_안내는_줄바꿈을_허용한다() throws IOException {
    String tag = tagContaining(read("stock/htmx/asset-growth.jte"), "${clickDateHoldingsLabel}");
    assertThat(tag).as("한 문장을 nowrap 으로 묶으면 320px 에서 문서가 넘친다").doesNotContain("whitespace-nowrap");
  }

  @Test
  void 시뮬레이터_도구_배지는_줄바꿈을_허용한다() throws IOException {
    String tag = tagContaining(read("stock/simulator.jte"), "${toolTitle}");
    assertThat(tag).contains("badge").contains("whitespace-normal").contains("h-auto");
  }

  @Test
  void 출처_가져오기_버튼은_줄바꿈을_허용한다() throws IOException {
    String html = read("stock/fragments/monthlyDividendReference.jte");
    int form = html.indexOf("/payout/import/source\"");
    assertThat(form).as("출처 가져오기 폼을 찾지 못했다").isGreaterThan(0);
    Matcher m = Pattern.compile("<button\\b[^>]*type=\"submit\"[^>]*>").matcher(html);
    assertThat(m.find(form)).isTrue();
    assertThat(m.group()).contains("w-full").contains("whitespace-normal").contains("h-auto");
  }

  @Test
  void 종목_상세_지표_카드는_아주_좁은_폭에서_한_열이다() throws IOException {
    String html = read("stock/htmx/stockItemDetailContent.jte");
    Matcher m = Pattern.compile("<div class=\"grid [^\"]*lg:grid-cols-4[^\"]*\">").matcher(html);
    assertThat(m.find()).as("지표 카드 격자를 찾지 못했다").isTrue();
    assertThat(m.group())
        .as("2열 고정이면 13자리 금액(font-mono text-lg)이 320px 에서 카드 밖으로 나간다")
        .contains("grid-cols-1")
        .contains("min-[360px]:grid-cols-2");
  }

  // 2차 실측: 안내문 nowrap 을 풀어도 +26px 가 남았다 - 기간 수익 요약의 text-4xl 금액(-₩159,605,442, 13자)이 288px 로 262px
  // 칸을 넘겼다.
  @Test
  void 기간_수익_요약_금액은_아주_좁은_폭에서_한_단계_작다() throws IOException {
    Matcher m =
        Pattern.compile("<span\\b[^>]*font-bold leading-none amount-value[^>]*>")
            .matcher(read("stock/htmx/fragments/assetGrowthPeriodReturnSummary.jte"));
    assertThat(m.find()).as("기간 수익 요약 금액을 찾지 못했다").isTrue();
    String tag = m.group();
    assertThat(tag)
        .contains("text-3xl")
        .contains("min-[360px]:text-4xl")
        .doesNotContain("\"text-4xl");
  }

  // 3차 실측(qa/ag-join.cjs): 시작/종료 date 입력의 고유 폭(125px, 간격 덧입히면 141px)이 1열 필터 트랙을 253~285px 로 키워 자산성장
  // 필터(details 안 214px)가 18px 남았다. 트랙은 minmax(0,1fr) 로 묶고 360px 미만에선 두 입력을 세로로 잇는다.
  @Test
  void 필터_격자는_좁은_폭에서_날짜_입력을_세로로_잇는다() throws IOException {
    String css = Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8);
    assertThat(css)
        .contains(
            ".stock-filter-grid { display: grid; gap: 0.75rem; grid-template-columns: minmax(0, 1fr); }");
    int narrow = css.indexOf("@media (width < 360px)");
    assertThat(narrow).as("360px 미만 규칙이 없다").isGreaterThan(0);
    String block =
        css.substring(
            narrow,
            css.indexOf("}", css.indexOf(".stock-filter-grid .join > *:last-child", narrow)));
    assertThat(block)
        .contains(".stock-filter-grid .join { flex-direction: column; }")
        .contains(".stock-filter-grid .join > .join-item { width: 100%; }");
  }
}
