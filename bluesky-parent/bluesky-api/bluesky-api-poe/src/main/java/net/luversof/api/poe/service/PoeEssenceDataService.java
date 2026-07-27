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
 * 에센스 제작 정보 — tools/poe-extract/parse-essences.mjs 가 만든 {@code ~/.poe-gamedata/essences.json}. "이
 * 에센스를 쓰면 이 아이템 클래스에 어떤 모드가 보장되는가"(poedb/craftofexile 의 에센스 섹션식)를 클래스별로 제공한다.
 */
@Service
public class PoeEssenceDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeEssenceDataService.class);

  /** 에센스 한 항목 — family(계열 키), 이름(en/ko), tier(클수록 상위), 보장 모드 줄(min/max 범위). */
  public record EssenceEntry(
      String family,
      String name,
      String nameKo,
      String icon,
      int tier,
      int ilvlMax,
      String gen,
      String modName,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin) {}

  private record EssenceData(String patch, Map<String, List<EssenceEntry>> classes) {}

  private final Path dataFile;
  private volatile EssenceData data = new EssenceData("", Map.of());

  public PoeEssenceDataService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "essences.json");
    reload();
  }

  public synchronized void reload() {
    EssenceData loaded = new EssenceData("", Map.of());
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        loaded = jsonMapper.readValue(inputStream, EssenceData.class);
        logger.info("PoE 에센스 로드: {} (클래스 {}개)", dataFile, loaded.classes().size());
      } catch (Exception e) {
        logger.warn("PoE 에센스 로드 실패: {}", dataFile, e);
      }
    } else {
      logger.warn("PoE 에센스 없음: {} — parse-essences.mjs 실행 필요", dataFile);
    }
    this.data = loaded;
  }

  /** 이 아이템 클래스에 부여 가능한 에센스 목록(계열별 묶음, 티어 내림차순). 대상 아니면 null. */
  public List<EssenceEntry> forItemClass(String itemClass) {
    return data.classes().get(itemClass);
  }
}
