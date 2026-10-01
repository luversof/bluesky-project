package net.luversof.api.poe.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * poe.ninja 실빌드 출발점 — 아키타입(전직|메인스킬)의 대표 실빌드(우리 엔진·표준 가정으로 재계산, 여러 명 중 DPS·EHP 중앙값에 가장 가까운 사람)의 정규화
 * PoB 코드. 산출: tools/poe-extract/fetch-ninja-seeds.mjs → {@code
 * ~/.poe-gamedata/ninja/ninja-start-builds.json}.
 *
 * <p>왜: 최적화기(빈 빌드에서 탐욕 선택)는 실빌드 생존력의 원천인 조합(타임리스·Split Personality 같은 주얼, 블록 체계)에 도달하지 못해 balanced
 * 결과 EHP 가 실빌드의 0.17~0.55x 였다(2026-09-30 실측). 시뮬레이터 결과 옆에 "실빌드에서 출발한 빌드"를 함께 보여 주고, 빌드 화면에서 업그레이드
 * 가이드로 다듬게 한다.
 *
 * <p>파일이 바뀌면(데이터 갱신) 다음 조회에서 다시 읽는다 — 재시작 불필요.
 */
@Service
public class PoeRealStartService {

  private static final Logger logger = LoggerFactory.getLogger(PoeRealStartService.class);

  /** 실빌드 출발점 한 건. name 은 poe.ninja 공개 캐릭터 이름(출처 표시용). */
  public record RealStart(
      String ascendancy,
      String mainSkill,
      String name,
      Integer level,
      Double dps,
      Double ehp,
      Double life,
      Double energyShield,
      Double netRegen,
      int sample,
      Double medianDps,
      Double medianEhp,
      String league,
      String code) {}

  private final Path file;
  private volatile FileTime loadedAt;
  private volatile Map<String, RealStart> byKey = Map.of();

  public PoeRealStartService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.file = Path.of(dataDir, "ninja", "ninja-start-builds.json");
  }

  /** (전직, 스킬) 정확 일치 우선, 전직이 비었거나 없으면 그 스킬의 표본이 가장 많은 아키타입. 없으면 null. */
  public RealStart find(String ascendancy, String skill) {
    if (skill == null || skill.isBlank()) {
      return null;
    }
    Map<String, RealStart> map = current();
    if (ascendancy != null && !ascendancy.isBlank()) {
      RealStart exact = map.get(ascendancy + "|" + skill);
      if (exact != null) {
        return exact;
      }
    }
    RealStart best = null;
    for (RealStart r : map.values()) {
      if (skill.equals(r.mainSkill()) && (best == null || r.sample() > best.sample())) {
        best = r;
      }
    }
    return best;
  }

  private Map<String, RealStart> current() {
    try {
      if (!Files.exists(file)) {
        return Map.of();
      }
      FileTime mtime = Files.getLastModifiedTime(file);
      if (!mtime.equals(loadedAt)) {
        synchronized (this) {
          if (!mtime.equals(loadedAt)) {
            byKey = load();
            loadedAt = mtime;
            logger.info("poe.ninja 실빌드 출발점 로드: {} 아키타입 ({})", byKey.size(), file);
          }
        }
      }
    } catch (Exception e) {
      logger.warn("poe.ninja 실빌드 출발점 로드 실패: {}", file, e);
    }
    return byKey;
  }

  private Map<String, RealStart> load() throws java.io.IOException {
    JsonNode root = JsonMapper.builder().build().readTree(Files.readString(file));
    Map<String, RealStart> map = new HashMap<>();
    for (Map.Entry<String, JsonNode> e : root.properties()) {
      String[] parts = e.getKey().split("\\|", 2);
      JsonNode v = e.getValue();
      if (parts.length < 2 || !v.path("code").isString()) {
        continue;
      }
      map.put(
          e.getKey(),
          new RealStart(
              parts[0],
              parts[1],
              v.path("name").asText(""),
              v.path("level").isNumber() ? v.path("level").asInt() : null,
              num(v, "dps"),
              num(v, "ehp"),
              num(v, "life"),
              num(v, "es"),
              num(v, "netRegen"),
              v.path("n").asInt(0),
              num(v, "medianDps"),
              num(v, "medianEhp"),
              v.path("league").asText(""),
              v.path("code").asText()));
    }
    return Map.copyOf(map);
  }

  private static Double num(JsonNode v, String key) {
    return v.path(key).isNumber() ? v.path(key).asDouble() : null;
  }
}
