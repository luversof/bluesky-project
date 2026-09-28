package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 수익률에서 빠진 배당이 있으면 그 수익률 칸이 근거를 달아야 한다.
 *
 * <p>세 수익률 열은 모두 분자로 {@code netAmountWithPrincipalCost}(기준일 원금이 있는 배당의 세후 합) 를 쓴다. 그래서 화면의 배당금(세후)
 * 와 원금을 그대로 나눈 값과 다르다.
 *
 * <p>실측 2026-09-11: 근거 툴팁이 {@code hidden xl:table-cell} 인 열 10 칸에만 있었고, 어느 폭에서나 보이는 '기간 평균투입원금 수익률'
 * 열에는 없었다 &mdash; 2020 년은 배당 5 건 중 2 건만 분자에 들어가는데 건수 열은 5 건이라고만 적는다.
 */
class DividendYieldBasisNoteTest {

  private static final Path YIELD =
      Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte");

  private String read() throws IOException {
    return Files.readString(YIELD, StandardCharsets.UTF_8);
  }

  /**
   * {@code title=} 로 근거를 단 칸 중 {@code sr-only} 가 없는 칸 수.
   *
   * <p>실측 2026-09-12: 이 칸들은 <b>일부러</b> title 만 단다 - 같은 줄의 항상 보이는 칸이 같은 문장을 이미 sr-only 로 읽어 주기
   * 때문이다(근거가 둘 이상 붙은 11 줄 전부에서 문장이 같았다). 수가 늘면 중복 낭독이고, 줄면 그 줄의 근거가 어디에도 없다는 뜻이라 양쪽 모두 막는다.
   */
  private int countCellsWithTitleButNoScreenReaderText(String template) {
    int found = 0;
    int at = template.indexOf("<td");
    while (at >= 0) {
      int tagEnd = template.indexOf(">", at);
      int cellEnd = template.indexOf("</td>", at);
      if (tagEnd < 0 || cellEnd < 0) {
        break;
      }
      String tag = template.substring(at, tagEnd);
      String body = template.substring(tagEnd, cellEnd);
      boolean hasReasonTitle =
          tag.contains("title=" + '"' + "${yieldBasisTitle.apply(")
              || tag.contains("title=" + '"' + "${basisNoneOrNull.apply(")
              || tag.contains("dailyBasisNoneTitle");
      if (hasReasonTitle && !body.contains("sr-only")) {
        found++;
      }
      at = template.indexOf("<td", tagEnd);
    }
    return found;
  }

  private int count(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }

  @Test
  void 보이는_수익률_열에도_근거가_붙는다() throws IOException {
    String template = read();

    // 연도별·종목별·계좌별 세 표의 본문과 합계 = 6 칸.
    // 2026-09-12 부터 같은 칸에 data-amount-basis 가 먼저 붙는다(금액 가리기가 이 문구도 가려야 해서다).
    // 2026-09-28 부터 항상 보이는 수익률 칸은 연 수익률이다(예전 기본 수익률 칸은 "열 더 보기" 로 갔고 근거는 그대로 달고 간다).
    assertThat(count(template, "data-annualized-cell data-amount-basis="))
        .as("항상 보이는 연 수익률 본문 칸 중 근거가 붙은 수")
        .isEqualTo(3);
    assertThat(count(template, "data-annualized-total data-amount-basis="))
        .as("항상 보이는 연 수익률 합계 칸 중 근거가 붙은 수")
        .isEqualTo(3);
    assertThat(count(template, "data-annualized-cell>") + count(template, "data-annualized-total>"))
        .as("근거 없이 남은 연 수익률 칸이 있으면 안 된다")
        .isZero();
    assertThat(
            count(
                template,
                "text-right text-dividend font-medium yield-extra-col"
                    + '"'
                    + " data-amount-basis="))
        .as("열 더 보기로 간 예전 기본 수익률 칸도 근거를 그대로 단다")
        .isEqualTo(6);
  }

  @Test
  void 숨는_열의_근거는_그대로_둔다() throws IOException {
    String template = read();

    // 툴팁이 붙은 칸 수는 title= 자리의 호출만 센다. 2026-09-11 에 보이는 6 칸이 sr-only 도 함께
    // 달면서 호출 자체는 28 번이 됐는데, 그건 칸이 늘어난 게 아니라 같은 근거를 두 번 쓴 것이다.
    // 2026-09-12: '기준일 평균원금 수익률' 세 칸은 기준일 원금이 아예 없을 때 다른 까닭을 쓴다
    // (그 전까지 그 칸의 "-" 에는 아무 말도 없었다 - 실측 4 칸). 그래서 툴팁 자리의 식이 두 갈래다.
    int plainTitles = count(template, "title=" + '"' + "${yieldBasisTitle.apply(");
    // 갈래식은 '기준일 원금이 없으면 그 까닭, 아니면 기존 근거' 한 덩어리인데 title 과 data-amount-basis 두 자리에
    // 똑같이 쓰인다 - 칸 수를 세려면 title= 로 시작하는 것만 세야 한다(그냥 세면 칸마다 두 번 잡힌다).
    int forkedTitles =
        count(
            template,
            "title="
                + '"'
                + "${basisNoneOrNull.apply(row.averagePrincipalCost()) != null ? basisNoneTitle :"
                + " yieldBasisTitle.apply(");
    // 2026-09-15: '기준일 평균시가 수익률' 4 칸이 이 집계에서 빠졌다 — 그 칸은 이제
    // 분자만 말하던 공용 문구 대신 제 분모까지 담은 marketBasisTitle 을 쓴다(아래 따로 본다).
    // 2026-09-28: 보이는 연 수익률 6 칸(세 표 본문 · 합계)이 더해졌다 - 예전 기본 수익률 6 칸은 열 더 보기로 가면서 근거를 그대로 달고 갔다.
    assertThat(plainTitles + forkedTitles)
        .as("예전 숨는 열 6 칸 + 예전 기본 수익률 6 칸 + 연 수익률 6 칸")
        .isEqualTo(18);
    assertThat(forkedTitles).as("기준일 원금이 없을 때 까닭이 갈리는 칸").isEqualTo(3);
    // 금액 칸('배당 기준일 평균원금') 의 "-" 에도 같은 까닭이 붙는다 - 세 표 각각 하나씩.
    assertThat(
            count(
                template,
                "title=" + '"' + "${basisNoneOrNull.apply(row.averagePrincipalCost())}" + '"'))
        .as("기준일 평균원금 금액 칸의 까닭")
        .isEqualTo(3);
    // 금액 가리기가 이 문구까지 가리도록 같은 값을 data-amount-basis 로도 싣는다 - 칸마다 하나.
    assertThat(
            count(template, "data-amount-basis=" + '"' + "${yieldBasisTitle.apply(")
                + count(template, "data-amount-basis=" + '"' + "${basisNoneOrNull.apply("))
        .as("가리기 대상 표식도 칸마다 하나(연 수익률 6 칸 포함)")
        .isEqualTo(18);
    assertThat(count(template, "yieldBasisTitle.apply("))
        // 2026-09-28: 툴팁 · 가리기 표식이 연 수익률 6 칸만큼 늘고(18 · 18), sr-only 는 여전히 보이는 6 칸(이제 연 수익률)에만 있다.
        .as("툴팁 18 + 가리기 표식 18 + 보이는 6 칸의 sr-only(조건 + 본문) 12")
        .isEqualTo(48);
    // 2026-09-12: 숨는 열에도 sr-only 를 달아 봤다가 되돌렸다. 실측(배당 화면, 근거가 둘 이상 붙은 11 줄):
    // 한 줄 안의 근거 문장이 <b>모두 같았다</b>(문장이 갈리는 줄 0). 숨는 열에 또 달면 같은 문장을 줄마다
    // 두세 번 읽게 되므로, 근거는 '항상 보이는 칸' 한 곳에만 싣는다.
    assertThat(countCellsWithTitleButNoScreenReaderText(template))
        // 2026-09-28: 예전 기본 수익률 6 칸도 열 더 보기(숨는 열)로 가며 sr-only 를 떼었다 - 6 + 6.
        // 같은 날 평균 투입원금 3 칸도 열 더 보기로 갔다(까닭은 보이는 연 수익률 칸이 말한다) - + 3.
        .as("숨는 열은 title 만 - 늘어나면 같은 문장을 두 번 읽는다")
        .isEqualTo(15);
  }

  @Test
  void 걸러진_배당이_없으면_툴팁을_붙이지_않는다() throws IOException {
    String template = read();

    assertThat(template)
        .as("제외액이 0 이면 null 을 돌려줘야 title= 이 통째로 빠진다")
        .contains("if (excludedNet.compareTo(BigDecimal.ZERO) <= 0)");
    assertThat(template).contains("stock.dividend.yield.basis.excluded");
  }

  /**
   * 기준일 평균시가 수익률은 <b>분모가 열로 안 나온다</b>.
   *
   * <p>형제 둘은 분모가 제 열로 있어 읽는 사람이 검산할 수 있다 &mdash; 기간 일평균 투입원금·배당 기준일 평균원금. 시가 수익률만 분자만 밝히고 분모는 어디에도
   * 없어, "왜 이 수익률만 높은가"(= 평균시가가 평균원금보다 작아서)를 화면에서 답할 수 없었다 &mdash; 실측 2026-09-15: 종목 16 칸 + 계좌 6 칸.
   *
   * <p>값은 이미 모델에 있었다({@code averagePrincipalMarketValue} &mdash; 푸터가 합계를 내려고 쓰고 있었다). 이 문구는 같은 행의
   * 다른 근거와 <b>내용이 다르므로</b>(분모가 다르다) sr-only 로도 내보낸다 &mdash; 중복 낭독을 막는 위의 규칙과 어긋나지 않는다.
   */
  @Test
  void 시가_수익률은_제_분모를_스스로_말한다() throws IOException {
    String template = read();

    // 종목·계좌 두 표의 본문과 합계 = 4 칸. 연도별 표엔 이 열이 없다.
    assertThat(count(template, "title=" + '\"' + "${marketBasisTitle.apply("))
        .as("시가 수익률 칸")
        .isEqualTo(4);
    // 금액 가리기가 이 문구까지 가리도록 같은 값을 data-amount-basis 로도 싪는다.
    assertThat(count(template, "data-amount-basis=" + '\"' + "${marketBasisTitle.apply("))
        .as("가리기 표식")
        .isEqualTo(4);
    // 툴팁 4 + 가리기 4 + sr-only(조건 + 본문) 8
    assertThat(count(template, "marketBasisTitle.apply(")).as("총 호출").isEqualTo(16);

    // 분모는 반드시 averagePrincipalMarketValue 여야 한다 - totalNetAmount 를 넘기면
    // 그건 분모가 아니라 세후 배당 합이다(예전 인자).
    assertThat(template)
        .as("분모는 평균시가")
        .contains(
            "marketBasisTitle.apply(row.netAmountWithPrincipalMarket(),"
                + " row.averagePrincipalMarketValue())");
    assertThat(template)
        .doesNotContain(
            "marketBasisTitle.apply(row.netAmountWithPrincipalMarket(),"
                + " row.totalNetAmount())");

    // 부분 문자열로 보면 몸통 이름을 바꿔도 통과한다
    // (실측: 키를 stock.dividend.yield.market.basis.unused 로 바꿼 변이가 그대로 지나갔다).
    // 값이 실린 줄로 본다 - 키 다음에 = 가 오는지까지.
    for (String rel :
        new String[] {
          "src/main/resources/uiMessage.properties", "src/main/resources/uiMessage_ko.properties"
        }) {
      String props = Files.readString(Path.of(rel), StandardCharsets.UTF_8);
      boolean defined = false;
      for (String line : props.split(String.valueOf((char) 10))) {
        String trimmed = line.trim().replace("\r", "");
        if (!trimmed.startsWith("stock.dividend.yield.market.basis")) {
          continue;
        }
        String rest = trimmed.substring("stock.dividend.yield.market.basis".length()).trim();
        if (rest.startsWith("=") && !rest.substring(1).trim().isEmpty()) {
          defined = true;
        }
      }
      assertThat(defined).as(rel + " 에 값이 실린 키가 있어야 한다").isTrue();
    }
  }
}
