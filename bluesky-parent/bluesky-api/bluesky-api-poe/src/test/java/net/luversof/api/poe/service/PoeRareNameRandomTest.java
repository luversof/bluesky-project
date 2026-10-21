package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** 종류별 무작위 레어 이름(10-20 C488) — 실제 낱말표(rare-name-words.json classWords)로. 없으면 건너뛴다. */
class PoeRareNameRandomTest {

  @Test
  void 장갑_이름은_장갑_낱말로_끝나고_한국어로_옮겨진다() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "rare-name-words.json")), "낱말표 없음");
    PoeRareNameService names = new PoeRareNameService(dir);
    Assumptions.assumeTrue(
        names.randomName("Gloves", new SplittableRandom(1)) != null, "옛 낱말표(classWords 없음)");
    // 게임 Words 표 SpawnWeight_Tags 'gloves' 낱말(장갑 접미 17개 중 일부)
    List<String> glove =
        List.of("Grip", "Grasp", "Fist", "Mitts", "Touch", "Hand", "Paw", "Talons");
    Set<String> seen = new HashSet<>();
    SplittableRandom rng = new SplittableRandom(7);
    boolean sawGloveWord = false;
    for (int i = 0; i < 200; i++) {
      String[] n = names.randomName("Gloves", rng);
      assertThat(n[0]).matches("\\S+ \\S+");
      assertThat(n[1]).isNotBlank().isNotEqualTo(n[0]);
      sawGloveWord |= glove.contains(n[0].substring(n[0].indexOf(' ') + 1));
      // 장갑 이름에 반지 · 무기 전용 낱말은 없다(자가검사: 'Bane' 은 무기 낱말)
      assertThat(n[0]).doesNotEndWith(" Bane");
      seen.add(n[0]);
    }
    assertThat(sawGloveWord).isTrue();
    assertThat(seen).hasSizeGreaterThan(50);
    assertThat(names.randomName("NoSuchClass", rng)).isNull();
  }
}
