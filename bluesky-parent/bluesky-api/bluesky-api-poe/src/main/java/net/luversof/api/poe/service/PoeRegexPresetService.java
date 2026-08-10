package net.luversof.api.poe.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * 지도 정규식 프리셋 저장소 — /poe/regex 페이지에서 만든 검색 조건(선택 모드·임계값·옵션·정규식)을 이름을 붙여 저장하고 다시 불러와 편집한다. 최적화
 * 이력(sim/history)과 같은 파일 방식: {@code dataDir/regex-presets/<id>.json}, id = 최초 저장 시각 epochMs(수정 시에도
 * 유지).
 */
@Service
public class PoeRegexPresetService {

  private static final Logger logger = LoggerFactory.getLogger(PoeRegexPresetService.class);
  private static final int PRESET_LIMIT = 200; // 폭주 방지 상한 — 초과 시 저장 거부

  /** 프리셋 한 건. data 는 화면 상태 전체(선택 키·임계값·옵션)를 클라이언트 정의 그대로 보관. */
  public record RegexPreset(
      long id, String name, long updatedMs, String regex, Map<String, Object> data) {}

  /** 목록용 요약 */
  public record RegexPresetEntry(long id, String name, long updatedMs, String regex) {}

  private final Path presetDir;

  public PoeRegexPresetService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.presetDir = Path.of(dataDir, "regex-presets");
  }

  public List<RegexPresetEntry> list() {
    if (!Files.exists(presetDir)) {
      return List.of();
    }
    JsonMapper jsonMapper = JsonMapper.builder().build();
    try (var stream = Files.list(presetDir)) {
      return stream
          .filter(p -> p.getFileName().toString().endsWith(".json"))
          .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
          .limit(PRESET_LIMIT)
          .map(
              p -> {
                try {
                  RegexPreset preset = jsonMapper.readValue(Files.readString(p), RegexPreset.class);
                  return new RegexPresetEntry(
                      preset.id(), preset.name(), preset.updatedMs(), preset.regex());
                } catch (Exception e) {
                  return null; // 깨진 파일 한 건이 목록 전체를 막지 않게
                }
              })
          .filter(java.util.Objects::nonNull)
          .toList();
    } catch (Exception e) {
      logger.warn("PoE 정규식 프리셋 목록 조회 실패", e);
      return List.of();
    }
  }

  /** 한 건 전체 조회 — 없거나 깨졌으면 null. */
  public RegexPreset get(long id) {
    Path file = presetDir.resolve(id + ".json");
    if (!Files.exists(file)) {
      return null;
    }
    try {
      return JsonMapper.builder().build().readValue(Files.readString(file), RegexPreset.class);
    } catch (Exception e) {
      logger.warn("PoE 정규식 프리셋 로드 실패: {}", id, e);
      return null;
    }
  }

  /** 저장(id 있으면 같은 파일 덮어쓰기 = 편집, 없으면 신규). 저장된 프리셋 반환, 실패 시 null. */
  public synchronized RegexPreset save(
      Long id, String name, String regex, Map<String, Object> data) {
    if (name == null || name.isBlank()) {
      return null;
    }
    long presetId = id != null && id > 0 ? id : System.currentTimeMillis();
    RegexPreset preset =
        new RegexPreset(
            presetId,
            name.trim(),
            System.currentTimeMillis(),
            regex == null ? "" : regex,
            data == null ? Map.of() : data);
    try {
      Files.createDirectories(presetDir);
      boolean isNew = !Files.exists(presetDir.resolve(presetId + ".json"));
      if (isNew && list().size() >= PRESET_LIMIT) {
        logger.warn("PoE 정규식 프리셋 상한({}) 도달 — 저장 거부", PRESET_LIMIT);
        return null;
      }
      Files.writeString(
          presetDir.resolve(presetId + ".json"),
          JsonMapper.builder().build().writeValueAsString(preset),
          StandardCharsets.UTF_8);
      return preset;
    } catch (Exception e) {
      logger.warn("PoE 정규식 프리셋 저장 실패: {}", name, e);
      return null;
    }
  }

  public boolean delete(long id) {
    try {
      return Files.deleteIfExists(presetDir.resolve(id + ".json"));
    } catch (Exception e) {
      logger.warn("PoE 정규식 프리셋 삭제 실패: {}", id, e);
      return false;
    }
  }
}
