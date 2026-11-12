package net.luversof.api.poe.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/** PoE1 최적화의 최근 결과 이력(sim/history/&lt;epochMs&gt;.json) - 저장 · 목록 · 조회 · 삭제. */
@Service
public class PoeOptimizeHistoryService {

  private static final Logger logger = LoggerFactory.getLogger(PoeOptimizeHistoryService.class);

  /** 이력 상한(최신 N 건만 남긴다). PoE2(Poe2SimService.HISTORY_LIMIT)와 같다. */
  private static final int HISTORY_LIMIT = 30;

  private final Path historyDir;

  public PoeOptimizeHistoryService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata/poe1}") String dataDir) {
    this(Path.of(dataDir, "sim", "history"));
  }

  PoeOptimizeHistoryService(Path historyDir) {
    this.historyDir = historyDir;
  }

  /** 최근 결과 이력 한 건의 목록용 요약(전체 결과 JSON 은 result(id) 로 별도 조회). id = 저장 시각 epochMs. */
  public record OptimizeHistoryEntry(
      long id,
      String gemName,
      String gemNameKo,
      String className,
      String classNameKo,
      String ascendancy,
      String ascendancyKo,
      String objective,
      String scenario,
      String scenarioKo,
      boolean combatBuffs,
      String finalValue,
      int evalCount,
      long durationMs) {}

  /** 완료 결과를 sim/history/&lt;epochMs&gt;.json 으로 남기고 상한(HISTORY_LIMIT)을 넘는 오래된 건을 정리한다. */
  void save(String resultJson) {
    try {
      Files.createDirectories(historyDir);
      Files.writeString(
          historyDir.resolve(System.currentTimeMillis() + ".json"),
          resultJson,
          StandardCharsets.UTF_8);
      prune();
    } catch (Exception e) {
      logger.warn("PoE 최적화 이력 저장 실패", e);
    }
  }

  /** epochMs 파일명은 13자리 고정폭이라 문자열 내림차순 = 시각 내림차순 — 상위 N 건만 남기고 삭제. */
  private void prune() throws java.io.IOException {
    try (var stream = Files.list(historyDir)) {
      var files =
          stream
              .filter(p -> p.getFileName().toString().endsWith(".json"))
              .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
              .toList();
      for (int i = HISTORY_LIMIT; i < files.size(); i++) {
        try {
          Files.deleteIfExists(files.get(i));
        } catch (java.io.IOException ignore) {
          // 개별 삭제 실패는 무시 — 다음 실행에서 다시 정리된다
        }
      }
    }
  }

  /** 최근 결과 목록(최신순, 최대 HISTORY_LIMIT). 파일이 없으면 빈 목록. */
  public List<OptimizeHistoryEntry> list() {
    if (!Files.exists(historyDir)) {
      return List.of();
    }
    JsonMapper jsonMapper = JsonMapper.builder().build();
    try (var stream = Files.list(historyDir)) {
      return stream
          .filter(p -> p.getFileName().toString().endsWith(".json"))
          .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
          .limit(HISTORY_LIMIT)
          .map(p -> {
            try {
              long id = Long.parseLong(p.getFileName().toString().replace(".json", ""));
              PoeOptimizeResult r =
                  jsonMapper.readValue(Files.readString(p), PoeOptimizeResult.class);
              return new OptimizeHistoryEntry(
                  id,
                  r.gemName(),
                  r.gemNameKo(),
                  r.className(),
                  r.classNameKo(),
                  r.ascendancy(),
                  r.ascendancyKo(),
                  r.objective(),
                  r.scenario(),
                  r.scenarioKo(),
                  r.combatBuffs(),
                  r.finalValue(),
                  r.evalCount(),
                  r.durationMs());
            } catch (Exception e) {
              return null; // 깨진 파일 한 건이 목록 전체를 막지 않게
            }
          })
          .filter(java.util.Objects::nonNull)
          .toList();
    } catch (java.io.IOException e) {
      logger.warn("PoE 최적화 이력 조회 실패", e);
      return List.of();
    }
  }

  /** 이력 결과 한 건 전체 조회(id = 저장 시각 epochMs) — 없거나 깨졌으면 null. */
  public PoeOptimizeResult result(long id) {
    Path file = historyDir.resolve(id + ".json");
    if (!Files.exists(file)) {
      return null;
    }
    try {
      return JsonMapper.builder()
          .build()
          .readValue(Files.readString(file), PoeOptimizeResult.class);
    } catch (Exception e) {
      logger.warn("PoE 최적화 이력 로드 실패: {}", id, e);
      return null;
    }
  }

  /** 최근 결과 이력 한 건 삭제(사용자 요청). id = 저장 시각 epochMs. 삭제했으면 true. */
  public boolean delete(long id) {
    Path file = historyDir.resolve(id + ".json");
    // 디렉토리 이탈 방지 — 정규화 경로가 historyDir 하위인지 확인
    if (!file.normalize().startsWith(historyDir.normalize())) {
      return false;
    }
    try {
      return Files.deleteIfExists(file);
    } catch (Exception e) {
      logger.warn("PoE 최적화 이력 삭제 실패: {}", id, e);
      return false;
    }
  }
}
