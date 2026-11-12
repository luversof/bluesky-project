package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 이력은 최신 30 건만 남기고, 지운 건은 다시 조회되지 않는다(PoeOptimizeService 에서 떼어 낸 뒤 동작 유지 확인). */
class PoeOptimizeHistoryServiceTest {

  @TempDir Path dir;

  private long count() throws IOException {
    try (Stream<Path> files = Files.list(dir)) {
      return files.count();
    }
  }

  @Test
  void 저장하면_최신_30건만_남는다() throws IOException {
    for (long id = 1_000_000_000_000L; id < 1_000_000_000_031L; id++) {
      Files.writeString(dir.resolve(id + ".json"), "{}");
    }
    new PoeOptimizeHistoryService(dir).save("{}");

    assertThat(count()).isEqualTo(30);
    assertThat(dir.resolve("1000000000000.json")).doesNotExist();
  }

  @Test
  void 지운_이력은_다시_조회되지_않는다() throws IOException {
    Files.writeString(dir.resolve("1000000000000.json"), "{}");
    var service = new PoeOptimizeHistoryService(dir);

    assertThat(service.delete(1_000_000_000_000L)).isTrue();
    assertThat(service.result(1_000_000_000_000L)).isNull();
    assertThat(service.delete(1_000_000_000_000L)).isFalse();
  }
}
