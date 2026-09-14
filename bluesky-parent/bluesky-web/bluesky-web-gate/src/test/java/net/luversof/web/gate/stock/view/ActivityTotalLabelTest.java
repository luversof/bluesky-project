package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 한 화면에서 "전체" 가 두 뜻으로 쓰이지 않는다.
 *
 * <p>활동 화면에는 기간 프리셋 <b>"전체"</b>(= 모든 기간)가 있다. 그 옆에서 카드가 <b>"전체 활동"</b> 이라고 적었는데, 그 값은 기간을 따라간다(실측
 * 2026-09-12: 전체 460건 → 2025 년 113건). 여기서의 "전체" 는 기간이 아니라 <b>매매 + 배당을 합친 것</b>이다(460 = 258 + 202,
 * 113 = 57 + 56). 영어는 이미 "Total Activity" 로 합계를 말한다.
 *
 * <p>값은 맞지만 같은 화면에서 같은 낱말이 두 뜻으로 쓰여 읽는 사람이 기간으로 오해할 수 있다 &mdash; 한국어도 합계 뜻으로 맞춘다. 실측: 매매·배당 화면에서는
 * "전체" 가 기간 프리셋 하나로만 쓰여 이런 충돌이 없다.
 */
class ActivityTotalLabelTest {

  @Test
  void 합계_카드는_기간_낱말을_쓰지_않는다() throws IOException {
    String ko =
        Files.readString(
            Path.of("src/main/resources/uiMessage_ko.properties"), StandardCharsets.ISO_8859_1);
    String value = valueOf(ko, "stock.activity.card.total.activity");
    char bs = (char) 92;
    String jeonche = bs + "uC804" + bs + "uCCB4";
    assertThat(value).as("기간 프리셋과 같은 낱말('전체')을 쓰면 기간으로 읽힌다").doesNotContain(jeonche);
    assertThat(value).as("합계 뜻을 밝혀야 한다").contains(bs + "uD569" + bs + "uACC4");
  }

  /** 영어는 이미 합계를 말한다 - 되돌아가지 않게 함께 묶는다. */
  @Test
  void 영어는_합계를_말한다() throws IOException {
    String en =
        Files.readString(
            Path.of("src/main/resources/uiMessage.properties"), StandardCharsets.ISO_8859_1);
    assertThat(valueOf(en, "stock.activity.card.total.activity")).containsIgnoringCase("total");
  }

  private static String valueOf(String bundle, String key) {
    for (String line : bundle.split(String.valueOf((char) 10))) {
      String row = line.trim();
      if (row.startsWith(key) && row.contains("=")) {
        return row.substring(row.indexOf('=') + 1).trim();
      }
    }
    throw new IllegalStateException("키를 찾지 못했다: " + key);
  }
}
