package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 자리표시 대시("-")의 뜻은 <b>그 행 어딘가에서</b> 낭독기에 닿아야 한다.
 *
 * <p>규약은 <b>한 행에 한 번</b>이다 &mdash; 같은 까닭을 여러 칸에 달면 낭독기가 두 번 읽고, 행당 1KB 예산({@code
 * DividendTableCompactOutputTest})도 넘어간다. 그래서 까닭은 "항상 보이는 칸" 한 곳에만 단다({@link
 * DashReasonAccessibleTest} 에 적힌 결정). 이 가드는 그 결정의 <b>빈 구멍</b>을 막는다: 행에 까닭이 <b>한 곳도</b> 없는 자리.
 *
 * <p>실측 2026-09-15(14 화면 9,175 칸을 훑음): 행 단위로 보면 5 칸이 남았다.
 *
 * <ul>
 *   <li>자산 성장 &gt; 연도별 성과 &gt; 원금 변동 1 칸 &mdash; 2016 년 행. 그 해엔 원금 움직임이 0 인데 행 어디에도 그 말이 없었다.
 *   <li>배당 &gt; 상세 목록 &gt; 합계 줄 4 칸 &mdash; 합계가 성립하지 않는 열인데 낭독기는 하이픈 넷만 들었다.
 * </ul>
 *
 * <p>칸 단위로 세면 21 칸이 나오는데 그중 16 칸은 <b>멀쩡한</b> 자리다(까닭이 옆 칸에 있다). 세는 단위를 행으로 잡아야 한다.
 */
class DashMeaningInRowTest {

  private static final char QUOTE = '"';

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

  /** 까닭 없이 홀로 선 대시. 이 표기가 남아 있으면 그 칸은 낭독기에 하이픈 하나로만 간다. */
  private int bareDashSpans(String template) {
    return count(template, "<span class=" + QUOTE + "text-base-content/60" + QUOTE + ">-</span>");
  }

  @Test
  void 연도별_성과의_빈_칸은_까닭을_함께_낸다() throws IOException {
    String template = read("src/main/jte/stock/htmx/fragments/assetGrowthYearlySummary.jte");

    assertThat(bareDashSpans(template)).as("까닭 없는 대시").isZero();

    // 이 표는 한 행에 다른 까닭이 없다 - 그러니 빈 칸마다 제 뜻을 말해야 한다.
    int withReason = count(template, "aria-hidden=" + QUOTE + "true" + QUOTE + " title=${");
    int spoken = count(template, "<span class=" + QUOTE + "sr-only" + QUOTE + ">${");
    assertThat(spoken).as("눈에 보이는 대시마다 낭독되는 짝").isGreaterThanOrEqualTo(withReason);
    assertThat(spoken).as("본문 네 칸 + 합계 세 칸").isGreaterThanOrEqualTo(7);

    // 원금 변동의 "-" 는 두 뜻이다 - 기록 없음과 0 원. 하나로 뭉뚱그리면 거짓말이 된다.
    assertThat(template)
        .as("null 과 0 을 갈라서 말한다")
        .contains("principal == null ? noValueTitle : zeroAmountTitle");
  }

  @Test
  void 배당_합계줄의_빈_칸은_합계가_없다고_말한다() throws IOException {
    String template = read("src/main/jte/stock/htmx/fragments/dividend/dividendTable.jte");

    // 합계 줄의 네 칸. 칸 자체에 다는 곳은 aria-label 이라야 이름이 된다.
    assertThat(count(template, "aria-label=" + QUOTE + "${noTotalTitle}" + QUOTE))
        .as("합계가 없는 열 넷")
        .isEqualTo(4);
    assertThat(template).contains("common.total.none");

    // 본문 줄의 "-" 는 건드리지 않는다 - 그 까닭은 같은 행의 수익률 칸이 이미 말한다(1KB 예산 결정).
    assertThat(count(template, "title=" + QUOTE + "${basisMissingTitle.apply(item)}" + QUOTE))
        .as("본문의 까닭은 여전히 한 곳")
        .isEqualTo(1);
  }

  @Test
  void 두_문구는_양쪽_로케일에_다_있다() throws IOException {
    for (String rel :
        new String[] {
          "src/main/resources/uiMessage.properties", "src/main/resources/uiMessage_ko.properties"
        }) {
      String props = read(rel);
      assertThat(props).as(rel).contains("common.value.none");
      assertThat(props).as(rel).contains("common.total.none");
    }

    // 한국어 파일에 새로 넣는 값은 \\uXXXX 여야 한다 - 날글자를 넣으면 편집기 코드페이지에서 깨진다.
    String ko = read("src/main/resources/uiMessage_ko.properties");
    for (String line : ko.split(String.valueOf((char) 10))) {
      if (line.startsWith("common.value.none") || line.startsWith("common.total.none")) {
        for (int i = 0; i < line.length(); i++) {
          assertThat((int) line.charAt(i)).as(line).isLessThan(128);
        }
      }
    }
  }
}
