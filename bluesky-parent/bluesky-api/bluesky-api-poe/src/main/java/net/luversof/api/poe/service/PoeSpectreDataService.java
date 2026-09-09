package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * 망령(Spectre) 목록 — tools/poe-extract/parse-spectres.mjs 가 만든 {@code ~/.poe-gamedata/spectres.json}.
 *
 * <p>발단(2026-09-09): 유령 소환 축이 <b>망령을 아예 소환하지 않고</b> 있었다. PoB 는 빌드 XML 의 {@code <Build>} 자식 {@code
 * <Spectre id="Metadata/..."/>} 로만 spectreList 를 채우는데(Build.lua:987-991) 우리가 그 섹션을 안 써서
 * spectreList=0 → minionList=0 → 미니언 미생성이었다(엔진 직접 진단). 그 축의 값 6,035,036 은 전부 Hextoad 보조젬이 만드는
 * Bursting Toad 에서 나온 것이었다.
 */
@Service
public class PoeSpectreDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeSpectreDataService.class);

  /**
   * 망령 한 종.
   *
   * @param id PoB 가 쓰는 메타데이터 경로(빌드 XML 의 Spectre id · 젬의 skillMinion 에 그대로 들어간다)
   * @param damage 몬스터 피해 계수(1.0 기준) — 후보 정렬에 쓴다
   */
  public record Spectre(
      String id,
      String name,
      List<String> tags,
      List<String> skills,
      Double damage,
      Double life,
      Double attackTime) {}

  private record SpectreData(int count, List<Spectre> spectres) {}

  private final Path dataFile;
  private volatile List<Spectre> spectres = List.of();
  private volatile Map<String, Spectre> byId = Map.of();

  public PoeSpectreDataService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "spectres.json");
    reload();
  }

  public synchronized void reload() {
    List<Spectre> loaded = List.of();
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        SpectreData data = jsonMapper.readValue(inputStream, SpectreData.class);
        loaded = data.spectres() == null ? List.of() : List.copyOf(data.spectres());
        logger.info("PoE 망령 목록 로드: {} ({}종)", dataFile, loaded.size());
      } catch (Exception e) {
        logger.warn("PoE 망령 목록 로드 실패: {}", dataFile, e);
      }
    } else {
      logger.warn("PoE 망령 목록 없음: {} — parse-spectres.mjs 실행 필요", dataFile);
    }
    this.spectres = loaded;
    this.byId =
        loaded.stream()
            .collect(
                java.util.stream.Collectors.toUnmodifiableMap(Spectre::id, s -> s, (a, b) -> a));
  }

  public List<Spectre> all() {
    return spectres;
  }

  public Spectre byId(String id) {
    return id == null ? null : byId.get(id);
  }

  /**
   * 후보 정렬 — 피해 계수 내림차순, 동률이면 id 순(결정성). 스킬이 근접 기본공격뿐인 망령은 뺀다(피해 기여가 없다).
   *
   * <p>⚠ 순서를 실행마다 흔들면 최적화 결과가 통째로 갈린다(표준 무기 SALT 사고와 같은 계열) — 반드시 전순서를 못 박는다.
   */
  public List<Spectre> ranked(int limit) {
    return spectres.stream()
        .filter(s -> s.skills() != null && s.skills().size() > 1)
        .filter(s -> s.damage() != null)
        .sorted(
            java.util.Comparator.comparingDouble((Spectre s) -> -s.damage())
                .thenComparing(Spectre::id))
        .limit(Math.max(0, limit))
        .toList();
  }
}
