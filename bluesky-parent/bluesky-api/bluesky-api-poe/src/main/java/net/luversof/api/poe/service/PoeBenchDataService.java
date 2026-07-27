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
 * 장인 작업대(벤치크래프트) 모드 — tools/poe-extract/parse-bench.mjs 가 만든 {@code ~/.poe-gamedata/bench.json}.
 * "작업대에서 이 클래스에 붙일 수 있는 모드 + 제작 비용"(poedb/craftofexile 의 Crafting Bench 섹션식)을 클래스별로 제공한다.
 */
@Service
public class PoeBenchDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeBenchDataService.class);

  /** 제작 비용 한 항목 — 화폐 이름(en/ko) × 수량. */
  public record BenchCost(String name, String nameKo, String icon, int count) {}

  /** 벤치 모드 한 항목 — gen(접두/접미), tier(같은 계열 내 순번), 모드 줄(min/max 범위), 비용. */
  public record BenchEntry(
      String gen,
      int tier,
      int reqLevel,
      String modName,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin,
      List<BenchCost> cost) {}

  private record BenchData(String patch, Map<String, List<BenchEntry>> classes) {}

  private final Path dataFile;
  private volatile BenchData data = new BenchData("", Map.of());

  public PoeBenchDataService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "bench.json");
    reload();
  }

  public synchronized void reload() {
    BenchData loaded = new BenchData("", Map.of());
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        loaded = jsonMapper.readValue(inputStream, BenchData.class);
        logger.info("PoE 벤치크래프트 로드: {} (클래스 {}개)", dataFile, loaded.classes().size());
      } catch (Exception e) {
        logger.warn("PoE 벤치크래프트 로드 실패: {}", dataFile, e);
      }
    } else {
      logger.warn("PoE 벤치크래프트 없음: {} — parse-bench.mjs 실행 필요", dataFile);
    }
    this.data = loaded;
  }

  /** 이 아이템 클래스에 붙일 수 있는 벤치 모드 목록(접두→접미, 계열·티어 정렬). 대상 아니면 null. */
  public List<BenchEntry> forItemClass(String itemClass) {
    return data.classes().get(itemClass);
  }
}
