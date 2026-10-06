package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.luversof.web.gate.poe.config.PoeDataVersion;

/** /poe-data/ 캐시버스터(10-03 C38) — 파일 수정 시각이 붙고, 밖 경로 · 없는 파일 · 다른 주소는 빈 문자열. */
class PoeDataVersionTest {

  @TempDir Path dir;

  @Test
  void 파일_수정_시각이_붙는다() throws Exception {
    Path tree = Files.createDirectories(dir.resolve("poe2")).resolve("passive-tree.json");
    Files.writeString(tree, "{}");
    Files.setLastModifiedTime(tree, FileTime.fromMillis(1_700_000_000_000L));
    PoeDataVersion v = new PoeDataVersion(dir.toString());
    assertThat(PoeDataVersion.q("/poe-data/poe2/passive-tree.json")).isEqualTo("?v=1700000000000");
    assertThat(PoeDataVersion.q("/poe-data/poe2/passive-tree.json?x=1"))
        .isEqualTo("&v=1700000000000");
    assertThat(v).isNotNull();
  }

  @Test
  void 없는_파일_밖_경로_다른_주소는_빈_문자열() {
    new PoeDataVersion(dir.toString());
    assertThat(PoeDataVersion.q("/poe-data/nope.json")).isEmpty();
    assertThat(PoeDataVersion.q("/poe-data/../secret.txt")).isEmpty();
    assertThat(PoeDataVersion.q("/poe-assets/a.png")).isEmpty();
    assertThat(PoeDataVersion.q(null)).isEmpty();
  }
}
