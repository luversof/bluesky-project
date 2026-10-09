package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 최적화 결과 보조젬 툴팁 레벨(10-05 C155) — 이름 꼬리 "(Lv16)"(속성 요구치로 낮춘 계산 레벨)를 젬 상세 주소로 넘긴다. 꼬리가 없으면 빈 값(상세 기본
 * 20레벨 = 계산). 결과 조각이 보조젬 · 추가 스킬 링크 두 곳에서 이 값을 붙이는지도 본다.
 */
class PoeTextGemLevelParamTest {

  @Test
  void parsesLoweredLevelSuffix() {
    assertThat(PoeText.gemLevelParam("Added Fire Damage Support (Lv16)")).isEqualTo("&level=16");
    assertThat(PoeText.gemLevelParam("화염 피해 추가 보조 (Lv7)")).isEqualTo("&level=7");
  }

  @Test
  void noSuffixMeansDefaultLevel() {
    assertThat(PoeText.gemLevelParam("Added Fire Damage Support")).isEmpty();
    assertThat(PoeText.gemLevelParam(null)).isEmpty();
    // 꼬리가 끝이 아니면(다른 괄호) 무시
    assertThat(PoeText.gemLevelParam("Impresence (Lv16) Fire")).isEmpty();
  }

  @Test
  void resultFragmentPassesLevelForSupportsAndLinkGems() throws Exception {
    String jte = Files.readString(Path.of("src/main/jte/poe/htmx/simOptimizeResult.jte"));
    assertThat(jte)
        .contains("PoeText.gemLevelParam(support.name())")
        .contains("PoeText.gemLevelParam(linkGem.name())");
  }
}
