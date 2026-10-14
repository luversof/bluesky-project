package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 시드 파일이 바뀌면 다음 조회에서 다시 읽는다(10-14: 단위 시험 재기동에 살아남은 수집 자식 프로세스가 파일을 새로 썼는데 동기 서비스를 거치지 않아 시뮬 화면 스냅샷
 * 표시 · 목록 순서가 옛 판에 머물렀다).
 */
class PoeMetaPopularityReloadTest {

  private static String seed(String snapshot, String skill) {
    return "{\"snapshots\":{\"allflame\":\""
        + snapshot
        + "\"},\"archetypes\":[{\"ascendancy\":\"Hierophant\",\"mainSkill\":\""
        + skill
        + "\",\"sample\":5}]}";
  }

  @Test
  void 파일이_바뀌면_다시_읽는다(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("ninja").resolve("ninja-archetypes.json");
    Files.createDirectories(file.getParent());
    Files.writeString(file, seed("1-old", "Arc"));
    Files.setLastModifiedTime(file, FileTime.from(Instant.parse("2026-10-13T20:54:00Z")));
    PoeMetaPopularityService svc = new PoeMetaPopularityService(dir.toString());
    assertThat(svc.order("", List.of()).snapshot()).isEqualTo("1-old");

    Files.writeString(file, seed("2-new", "Spark"));
    Files.setLastModifiedTime(file, FileTime.from(Instant.parse("2026-10-13T23:26:51Z")));
    PoeMetaPopularityService.MetaOrder o = svc.order("", List.of());
    assertThat(o.snapshot()).isEqualTo("2-new");
    assertThat(o.skills()).contains("Spark").doesNotContain("Arc");
  }

  @Test
  void 같은_시각이면_다시_읽지_않는다(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("ninja").resolve("ninja-archetypes.json");
    Files.createDirectories(file.getParent());
    FileTime t = FileTime.from(Instant.parse("2026-10-13T20:54:00Z"));
    Files.writeString(file, seed("1-old", "Arc"));
    Files.setLastModifiedTime(file, t);
    PoeMetaPopularityService svc = new PoeMetaPopularityService(dir.toString());
    Files.writeString(file, seed("2-new", "Spark"));
    Files.setLastModifiedTime(file, t); // 시각이 같으면 그대로(매 조회 파싱하지 않는다)
    assertThat(svc.order("", List.of()).snapshot()).isEqualTo("1-old");
  }
}
