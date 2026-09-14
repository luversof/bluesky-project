package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 지표 카드 보조줄의 금액도 금액 숨김에 걸려야 한다.
 *
 * <p>{@code statCard} 의 {@code sub} 는 통째로 글자라 흐려지지 않는다 - 컴포넌트가 그래서 {@code subBefore} + {@code
 * subAmount}({@code amount-value} 로 감싼다) + {@code subAfter} 를 따로 받는다.
 *
 * <p>실측 2026-09-12(금액 숨김 켜고 10 화면, 금액 텍스트 540 개): 계좌 상세 실현손익 카드의 "이 계좌 기준 +2,063,739" <b>한 곳만</b>
 * 흐려지지 않았다. 같은 문구를 쓰는 매매 화면의 계좌별 표 3 줄은 정상이었다.
 *
 * <p>자리표시자 위치가 로케일마다 다르다 &mdash; 한국어 "이 계좌 기준 {0}"(뒤), 영어 "{0} on this account's own basis"(앞).
 */
class StockMessageSplitUtilTest {

  @Test
  void 자리표시자_앞뒤를_가른다() {
    assertThat(StockMessageSplitUtil.beforePlaceholder("이 계좌 기준 {0}")).isEqualTo("이 계좌 기준 ");
    assertThat(StockMessageSplitUtil.afterPlaceholder("이 계좌 기준 {0}")).isEmpty();
    assertThat(StockMessageSplitUtil.beforePlaceholder("{0} on this account's own basis"))
        .isEmpty();
    assertThat(StockMessageSplitUtil.afterPlaceholder("{0} on this account's own basis"))
        .isEqualTo(" on this account's own basis");
  }

  @Test
  void 자리표시자가_없거나_널이면_안전하다() {
    assertThat(StockMessageSplitUtil.beforePlaceholder("자리 없음")).isEqualTo("자리 없음");
    assertThat(StockMessageSplitUtil.afterPlaceholder("자리 없음")).isEmpty();
    assertThat(StockMessageSplitUtil.beforePlaceholder(null)).isEmpty();
    assertThat(StockMessageSplitUtil.afterPlaceholder(null)).isEmpty();
  }

  /** 계좌 상세가 실제로 이 규칙을 거쳐야 한다 - sub 로 되돌아가면 다시 샌다. */
  @Test
  void 계좌_상세는_금액을_따로_넘긴다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/accountDetailContent.jte"), StandardCharsets.UTF_8);
    String flat = flatten(template);
    assertThat(flat).contains("subAmount = basisGapNotable");
    assertThat(flat).contains("StockMessageSplitUtil.beforePlaceholder(basisGapPattern)");
    assertThat(flat).contains("StockMessageSplitUtil.afterPlaceholder(basisGapPattern)");
    assertThat(flat)
        .as("금액을 sub 로 통째로 넘기면 흐려지지 않는다")
        .doesNotContain("sub = RealizedBasisGap.isNotable(");
  }

  /** 두 로케일 모두 자리표시자를 가져야 쪼개기가 뜻을 가진다. */
  @Test
  void 두_번들의_문구에_자리표시자가_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8);
      int at = text.indexOf("stock.trade.realized.basis.gap ");
      assertThat(at).as(bundle).isGreaterThan(-1);
      int end = text.indexOf(10, at);
      assertThat(text.substring(at, end < 0 ? text.length() : end)).as(bundle).contains("{0}");
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
