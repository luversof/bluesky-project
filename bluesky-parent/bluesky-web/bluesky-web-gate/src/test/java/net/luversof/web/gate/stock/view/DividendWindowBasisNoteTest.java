package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 월중 / 월말 / 기타 로 가르는 근거를 화면이 말해야 한다.
 *
 * <p>이 셋은 종목에 단 태그(월중배당 &middot; 월말배당)로 갈리고, 태그가 없으면 기타다. 그런데 2026-09-12 까지 세 항목에는 {@code title} 도
 * {@code aria-label} 도 {@code data-tip} 도 없었고, 화면 어디에도 근거가 없었다 &mdash; 실측: 본문 전체에서 "태그" 라는 말은 필터
 * 드롭다운("태그: 전체") 한 곳뿐이었다.
 *
 * <p>실측 2026-09-12('올해'): 월중 5,807,436원 56건 &middot; 월말 15,203,050원 57건 &middot; <b>기타 6,152,014원
 * 3건</b>. 기타가 전체의 22.6% 인데 그것이 무엇인지 알 방법이 없었다.
 */
class DividendWindowBasisNoteTest {

  @Test
  void 구분_근거를_설명한다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendSummaryCards.jte"),
            StandardCharsets.UTF_8);
    String flat = flatten(template);
    assertThat(flat).as("설명 자리가 있어야 한다").contains("data-window-basis");
    assertThat(flat)
        .as("마우스와 보조기술 양쪽에 닿아야 한다")
        .contains("aria-label=\"${windowBasisLabel}\"")
        .contains("data-tip=\"${windowBasisLabel}\"");
    assertThat(flat).as("키보드로도 닿아야 한다").contains("role=\"note\" tabindex=\"0\"");
    assertThat(template).contains("stock.dividend.summary.window.basis");
  }

  @Test
  void 문구는_두_번들에_다_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(
              Files.readString(
                  Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8))
          .as(bundle)
          .contains("stock.dividend.summary.window.basis");
    }
  }

  private String flatten(String source) {
    StringBuilder out = new StringBuilder();
    boolean lastWasSpace = false;
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (Character.isWhitespace(c)) {
        if (!lastWasSpace) {
          out.append(' ');
        }
        lastWasSpace = true;
      } else {
        out.append(c);
        lastWasSpace = false;
      }
    }
    return out.toString();
  }
}
