package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 실현손익 두 표의 수익률 "-" 에는 까닭이 붙는다.
 *
 * <p>수익률의 분모는 매도원가다. 판 것이 없으면 분모가 없어 칸이 비는데, 실측 2026-09-15(매매 화면에 매도 0 건인 태그 '커버드콜' 을 걸어 재현):
 * 계좌별·종목별 두 표의 <b>본문 줄과 합계 줄 넷 모두</b> {@code <td class="text-right font-mono opacity-40">-</td>} 였다
 * &mdash; title 도 sr-only 도 aria-label 도 없어 마우스로도 낭독기로도 까닭을 알 수 없었다.
 *
 * <p>필터를 걸지 않으면 모든 계좌·종목에 매도가 있어 이 자리가 드러나지 않는다. 그래서 대시를 훑은 지난 검사들이 이 넷을 지나쳤다({@link
 * DashMeaningInRowTest} 의 9,175 칸 훑기 포함).
 *
 * <p>{@code opacity-40} 은 {@code _components/ui/amountCell} 이 2026-09-10 에 걷어낸 대비 미달 표기와 같은
 * 계열이다(라이트 2.00 · AA 4.5 미달). 같은 {@code text-base-content/60} 으로 맞춘다.
 */
class RealizedRateNoneReasonTest {

  private static final char QUOTE = '"';
  private static final Path TEMPLATE =
      Path.of("src/main/jte/stock/htmx/fragments/trade/tradeRealizedSections.jte");

  private String template() throws IOException {
    return Files.readString(TEMPLATE, StandardCharsets.UTF_8);
  }

  private static int countOf(String source, String marker) {
    int n = 0;
    for (int at = source.indexOf(marker); at >= 0; at = source.indexOf(marker, at + 1)) {
      n++;
    }
    return n;
  }

  /** 빌드가 ${} 안 공백을 지우므로 원본 서식 그대로 비교하면 변이를 통과시킨다. */
  private static String squeeze(String source) {
    return source.replaceAll("\\s+", " ");
  }

  @Test
  void 까닭_없는_대시가_남아_있지_않다() throws IOException {
    String squeezed = squeeze(template());

    // 낱말이 있는지로 보면 안 된다 - 고친 자리가 그 낱말을 이미 갖고 있어 변이를 통과시킨다.
    // 하면 안 되는 모양이 없는지로 본다.
    assertThat(squeezed)
        .as("까닭 없는 대시 칸이 남아 있다")
        .doesNotContain("font-mono opacity-40" + QUOTE + ">-</td>");
    assertThat(squeezed)
        .as("선택 합산 수익률이 까닭 없는 대시다")
        .doesNotContain("data-trade-realized-sel-rate>-</div>");
    assertThat(squeezed)
        .as("인라인 script 가 까닭 없이 대시로 되돌린다")
        .doesNotContain("rateEl.textContent = '-';");
  }

  @Test
  void 까닭은_마우스와_낭독기_모두에_닿는다() throws IOException {
    String template = template();

    // 계좌별 본문·합계 + 종목별 본문·합계 = 넷.
    assertThat(countOf(template, "title=" + QUOTE + "${rateNoneLabel}" + QUOTE + ">-</span>"))
        .as("표의 빈 수익률 칸에 단 마우스 까닭")
        .isEqualTo(4);
    // 표 넷 + 선택 합산 둘.
    assertThat(
            countOf(
                template, "<span class=" + QUOTE + "sr-only" + QUOTE + ">${rateNoneLabel}</span>"))
        .as("낭독기에 닿는 까닭 - title 만으로는 안 된다")
        .isEqualTo(6);
    assertThat(template).contains("stock.realized.rate.none");
  }

  /** 값이 생기면 까닭 툴팁은 사라져야 한다 - 안 지우면 수익률 위에 엉뚱한 설명이 남는다. */
  @Test
  void 값이_생기면_까닭_툴팁을_지운다() throws IOException {
    assertThat(squeeze(template()))
        .as("대시에서 값으로 바뀔 때 title 을 안 지운다")
        .contains("rateEl.removeAttribute('title');");
  }

  /** 대비: opacity-40 은 amountCell 이 이미 걷어낸 표기다(라이트 2.00 으로 AA 미달). */
  @Test
  void 대시의_대비는_다른_표와_같다() throws IOException {
    String template = template();

    assertThat(template).as("대비 미달 표기가 남아 있다").doesNotContain("font-mono opacity-40");
    assertThat(countOf(template, "<span class=" + QUOTE + "text-base-content/60" + QUOTE))
        .as("amountCell 과 같은 대비로 적은 칸")
        .isGreaterThanOrEqualTo(4);
  }

  @Test
  void 두_언어에_문구가_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8);
      // 키 + " = " 로 본다 - 부분 문자열로 보면 다른 키의 앞부분에 걸려 자명하게 통과할 수 있다.
      assertThat(text).as(bundle).contains("stock.realized.rate.none = ");
    }
  }
}
