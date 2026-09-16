package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 복리 표의 '원금/수익 비율' 이 빌 때 그 까닭이 낭독기에 닿아야 한다.
 *
 * <p>연말 자산이 0 이하인 해는 나눌 것이 없어 비율이 성립하지 않는다. 그때 대시만 두면 낭독기에는 하이픈 하나로만 가고, 이 표는 한 행에 다른 까닭이 없어 그 칸이
 * 스스로 말해야 한다({@link DashMeaningInRowTest} 의 '한 행에 한 번' 규약과 같은 자리 &mdash; 여기서는 그 한 번이 이 칸이다).
 *
 * <p>실측 2026-09-15(경계 입력 12 종): 이율 -100% 와 원금 0 두 경우에서 여섯 칸이 까닭 없는 대시였다.
 *
 * <p>라벨은 앱 루트의 {@code data-ratio-none} 으로 내려온다 &mdash; 이 파일은 그 세 자리(메시지 · JTE · 스크립트)가 서로 이어져 있는지
 * 본다. 하나라도 끊기면 화면은 영어 기본값이나 빈 글자를 내보낸다.
 */
class CompoundRatioNoneReasonTest {

  private static final char QUOTE = '"';
  private static final String KEY = "stock.simulator.compound.table.ratio.none";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  /** 키가 값까지 실려 정의됐는지. 이름만 찾으면 {@code KEY + ".unused"} 같은 변이가 통과한다. */
  private boolean defines(String props, String key) {
    for (String line : props.split(String.valueOf((char) 10))) {
      String trimmed = line.trim().replace("\r", "");
      if (!trimmed.startsWith(key)) {
        continue;
      }
      String rest = trimmed.substring(key.length()).trim();
      if (rest.startsWith("=") && !rest.substring(1).trim().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  @Test
  void 문구가_양쪽_로케일에_정의돼_있다() throws IOException {
    for (String rel :
        new String[] {
          "src/main/resources/uiMessage.properties", "src/main/resources/uiMessage_ko.properties"
        }) {
      assertThat(defines(read(rel), KEY)).as(rel).isTrue();
    }

    // 한국어 파일에 새로 넣는 값은 \\uXXXX 여야 한다 - 날글자는 편집기 코드페이지에서 깨진다.
    for (String line :
        read("src/main/resources/uiMessage_ko.properties").split(String.valueOf((char) 10))) {
      if (line.startsWith(KEY)) {
        for (int i = 0; i < line.length(); i++) {
          assertThat((int) line.charAt(i)).as(line).isLessThan(128);
        }
      }
    }
  }

  @Test
  void JTE_가_그_문구를_앱_루트에_내려_준다() throws IOException {
    String template = read("src/main/jte/stock/fragments/compoundSimulator.jte");

    assertThat(template).as("메시지를 읽어").contains(KEY);
    assertThat(template)
        .as("앱 루트의 data-ratio-none 으로 내려 준다")
        .contains("data-ratio-none=" + QUOTE + "${ratioNoneLabel}" + QUOTE);
  }

  @Test
  void 스크립트가_대시에_까닭을_붙인다() throws IOException {
    // 브라우저가 도는 것은 빌드 산출물이다 - 원본이 아니라 그쪽을 본다.
    String built = read("src/main/resources/static/js/stock/compoundSimulator.js");

    assertThat(built).as("루트에서 문구를 읽는다").contains("ratioNone");

    // ⚠ 낱말이 번들 어딘가에 있기만 해도 통과하면 안 된다 - 실측 2026-09-15: "aria-hidden"
    // "sr-only" 만 찾는 단언은 '예전처럼 대시만 넣는' 변이를 그대로 통과시켰다(두 낱말이 죽은
    // 코드에도 남아 있었다). 대시를 **칸에 바로** 넣는 줄이 없어야 한다는 쪽으로 본다.
    assertThat(built)
        .as("까닭 없는 대시를 칸에 바로 넣는 줄이 없어야 한다")
        .doesNotContain("td.textContent=" + QUOTE + "-" + QUOTE);
    // 보이는 대시는 숨긴 표시로 두고, 낭독되는 글을 따로 붙인다.
    assertThat(built)
        .as("대시는 보조기술에서 감춘 표시로 넣는다")
        .contains("mark.textContent=" + QUOTE + "-" + QUOTE);
    assertThat(built).as("까닭은 sr-only 로 읽힌다").contains("sr-only");
  }
}
