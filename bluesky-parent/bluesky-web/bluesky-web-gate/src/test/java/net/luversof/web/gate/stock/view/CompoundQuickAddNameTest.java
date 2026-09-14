package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 복리 시뮬레이터의 금액 증감 버튼은 어느 칸을 건드리는지 이름으로 말한다.
 *
 * <p>실측 2026-09-12(접근성 트리):
 *
 * <pre>
 *   spinbutton "초기 원금 0 +100만 +1,000만 0"   ← 감싼 label 이 미리보기와 버튼 글자까지 삼켰다
 *   button "+100만"  button "+1,000만"  button "0"     ← 여섯 버튼이 세 이름을 두 번씩 나눠 가졌다
 *   spinbutton "정기 납입액 123,456 +100만 +1,000만 0"
 * </pre>
 *
 * <p>눈으로는 바로 위 칸을 보면 알지만, 이름만 듣는 쪽은 "+100만" 이 초기 원금인지 정기 납입액인지 알 수 없다 &mdash; 대시보드의 같은 이름 카드 두 장,
 * 자산 현황의 같은 이름 표 다섯 개와 같은 부류다.
 *
 * <p>"0" 버튼은 더하는 것이 아니라 <b>0 으로 되돌린다</b>({@code compoundSimulator.ts}: {@code delta === 0 ? 0 :
 * …}). 그래서 문구도 따로 둔다.
 */
class CompoundQuickAddNameTest {

  private static final Path TEMPLATE =
      Path.of("src/main/jte/stock/fragments/compoundSimulator.jte");

  @Test
  void 증감_버튼은_칸_이름을_달고_나온다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(
            countOf(template, "aria-label=\"${java.text.MessageFormat.format(quickAddAriaPattern,"))
        .as("더하기 버튼 네 개(칸 둘 × 금액 둘)")
        .isEqualTo(4);
    assertThat(
            countOf(
                template, "aria-label=\"${java.text.MessageFormat.format(quickResetAriaPattern,"))
        .as("0 으로 되돌리는 버튼 두 개")
        .isEqualTo(2);
    // 이름이 붙지 않은 증감 버튼이 남아 있으면 안 된다.
    assertThat(countOf(template, "data-amount-add-target=")).as("증감 버튼 여섯 개 모두").isEqualTo(6);
    assertThat(countOf(template, "aria-label=\"${java.text.MessageFormat.format(quick"))
        .as("여섯 개 모두 이름을 가진다")
        .isEqualTo(6);
  }

  /** 감싼 {@code label} 이 미리보기·버튼 글자를 이름에 섞지 않도록 칸에 이름을 직접 준다. */
  @Test
  void 금액_칸은_제_이름을_따로_가진다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template).contains("id=\"stockCompoundInitial\"");
    assertThat(template)
        .as("초기 원금 칸의 이름")
        .contains(
            "value=\"0\" class=\"input input-bordered w-full\" aria-label=\"${initialLabel}\"");
    assertThat(template)
        .as("정기 납입액 칸의 이름")
        .contains(
            "value=\"20000000\" class=\"input input-bordered w-full\" aria-label=\"${contributionLabel}\"");
  }

  @Test
  void 문구는_두_언어에_다_있고_자리를_가진다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8);
      String add = lineOf(text, "stock.simulator.compound.quickadd.add.aria");
      String reset = lineOf(text, "stock.simulator.compound.quickadd.reset.aria");
      assertThat(add).as(bundle + " 더하기 문구").isNotNull().contains("{0}").contains("{1}");
      assertThat(reset).as(bundle + " 되돌리기 문구").isNotNull().contains("{0}");
    }
  }

  private String lineOf(String text, String key) {
    for (String line : text.split(String.valueOf((char) 10))) {
      if (line.startsWith(key)) {
        return line;
      }
    }
    return null;
  }

  private int countOf(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }
}
