package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * 레어 아이템 크래프팅용 모드 풀 — tools/poe-extract/parse-mods.mjs 가 만든 {@code ~/.poe-gamedata/mod-pool.json}.
 * 패밀리별로 슬롯 적용 범위 + 키워드 + 티어 사다리(best-first, 각 티어의 최대 롤 문장 en/ko)를 담는다. 시뮬레이터의 레어 생성/티어 비교에 쓴다.
 */
@Service
public class PoeModPoolDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeModPoolDataService.class);

  /** 모드 티어 하나 — 최대 롤 기준 문장 (여러 스탯이면 여러 줄) */
  public record ModTier(int level, List<String> en, List<String> ko) {}

  /**
   * @param gen prefix | suffix
   * @param slots 적용 슬롯 카테고리 (body/helmet/.../weaponAttack/weaponSpell)
   * @param tiers best-first (index 0 = 최상위 티어)
   */
  public record ModFamily(
      String key, String gen, List<String> slots, List<String> keywords, List<ModTier> tiers) {}

  private record ModPoolData(String patch, List<ModFamily> families) {}

  private final Path dataFile;
  private volatile ModPoolData data = new ModPoolData("", List.of());

  public PoeModPoolDataService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "mod-pool.json");
    reload();
  }

  public synchronized void reload() {
    ModPoolData loaded = new ModPoolData("", List.of());
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        loaded = jsonMapper.readValue(inputStream, ModPoolData.class);
        logger.info("PoE 모드 풀 로드: {} (패밀리 {}개)", dataFile, loaded.families().size());
      } catch (Exception e) {
        logger.warn("PoE 모드 풀 로드 실패: {}", dataFile, e);
      }
    } else {
      logger.warn("PoE 모드 풀 없음: {} — parse-mods.mjs 실행 필요", dataFile);
    }
    this.data = loaded;
  }

  public boolean hasData() {
    return !data.families().isEmpty();
  }

  /** 해당 슬롯 카테고리에 붙는 패밀리들 */
  public List<ModFamily> familiesForSlot(String slotCategory) {
    return data.families().stream().filter(f -> f.slots().contains(slotCategory)).toList();
  }

  /** 전체 패밀리 (번역 사전 구축 등) */
  public List<ModFamily> families() {
    return data.families();
  }

  /** 베이스 아이템 클래스 → 모드 풀 슬롯 카테고리 (일반 아이템 티어표용). 매핑 없으면 null. */
  public String slotForItemClass(String itemClass) {
    if (itemClass == null) {
      return null;
    }
    return switch (itemClass) {
      case "Body Armour" -> "body";
      case "Helmet" -> "helmet";
      case "Gloves" -> "gloves";
      case "Boots" -> "boots";
      case "Shield" -> "shield";
      case "Ring" -> "ring";
      case "Amulet" -> "amulet";
      case "Belt" -> "belt";
      case "Quiver" -> "quiver";
      case "Wand", "Sceptre" -> "weaponSpell";
      default ->
          (itemClass.contains("Sword")
                  || itemClass.contains("Axe")
                  || itemClass.contains("Mace")
                  || itemClass.contains("Claw")
                  || itemClass.contains("Dagger")
                  || itemClass.contains("Bow")
                  || itemClass.contains("Staff"))
              ? "weaponAttack"
              : null;
    };
  }

  /** 베이스 아이템 클래스에 붙을 수 있는 패밀리들 (매핑 없으면 빈 목록) */
  public List<ModFamily> familiesForItemClass(String itemClass) {
    String slot = slotForItemClass(itemClass);
    return slot == null ? List.of() : familiesForSlot(slot);
  }
}
