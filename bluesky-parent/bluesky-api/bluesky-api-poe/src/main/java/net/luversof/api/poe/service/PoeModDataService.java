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
 * 전체 크래프팅 모드 풀 — tools/poe-extract/parse-mods-full.mjs 가 만든 {@code ~/.poe-gamedata/mods.json}.
 * poedb Modifiers 페이지식으로 아이템 클래스별 접두/접미 패밀리 + 티어 사다리(ilvl·수치범위·스폰웨이트)를 제공한다. 최적화기용 큐레이션({@link
 * PoeModPoolDataService})과는 별개의 "표시/탐색용" 전체 데이터.
 */
@Service
public class PoeModDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeModDataService.class);

  /** 모드 티어 한 줄 — 최대롤(en/ko) + 최소롤(enMin/koMin) 로 수치 범위 표기. */
  public record ModTier(
      String id,
      String name,
      String nameKo,
      int ilvl,
      int weight,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin) {}

  /** 패밀리 = 같은 그룹의 티어 사다리. gen = prefix|suffix. */
  public record ModFamily(String gen, Boolean essence, List<ModTier> tiers) {}

  /** 속성 변형 하나(방어구 str/dex/int 등) — 변형마다 붙는 모드가 다르다. */
  public record ModVariant(
      String key, String name, String nameKo, int prefixCount, int suffixCount) {}

  /** 영향력 하나(쉐이퍼/엘더/정복자 4종) — extraCount = 영향력 전용으로 더 붙는 패밀리 수. */
  public record ModInfluence(String key, String name, String nameKo, int extraCount) {}

  /** 아이템 클래스 하나 — 변형·영향력 목록(없으면 빈 목록) + 기본 풀. */
  public record ModItemClass(
      String itemClass,
      String name,
      String nameKo,
      List<ModVariant> variants,
      List<ModInfluence> influences,
      List<String> prefixes,
      List<String> suffixes) {}

  /** 풀 하나(클래스|변형) — 접두/접미 패밀리 키. */
  private record Pool(List<String> prefixes, List<String> suffixes) {}

  private record ModData(
      String patch,
      List<ModItemClass> itemClasses,
      Map<String, Pool> pools,
      Map<String, ModFamily> families) {}

  /** 한 (아이템 클래스 × 변형)의 완전한 모드 풀 — 패밀리 키를 실제 패밀리로 해석해 접두/접미로 나눠 담는다. */
  public record ClassMods(
      String itemClass,
      String name,
      String nameKo,
      String variant,
      List<ModVariant> variants,
      String influence,
      List<ModInfluence> influences,
      List<NamedFamily> prefixes,
      List<NamedFamily> suffixes) {}

  public record NamedFamily(String key, ModFamily family) {}

  private final Path dataFile;
  private volatile ModData data = new ModData("", List.of(), Map.of(), Map.of());

  public PoeModDataService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "mods.json");
    reload();
  }

  public synchronized void reload() {
    ModData loaded = new ModData("", List.of(), Map.of(), Map.of());
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        loaded = jsonMapper.readValue(inputStream, ModData.class);
        logger.info(
            "PoE 전체 모드 로드: {} (클래스 {}개, 패밀리 {}개)",
            dataFile,
            loaded.itemClasses().size(),
            loaded.families().size());
      } catch (Exception e) {
        logger.warn("PoE 전체 모드 로드 실패: {}", dataFile, e);
      }
    } else {
      logger.warn("PoE 전체 모드 없음: {} — parse-mods-full.mjs 실행 필요", dataFile);
    }
    this.data = loaded;
  }

  public boolean hasData() {
    return !data.itemClasses().isEmpty();
  }

  public String patch() {
    return data.patch();
  }

  /**
   * 이 (아이템 클래스 × 속성 변형)에 해당 패밀리(게임 모드 Id 패턴)가 실제로 붙을 수 있는지 — 시뮬레이터가 베이스에 맞지 않는 모드를 얹지 않도록 게임 데이터로
   * 판정한다. 풀 자체가 없으면(데이터 미로드/미지원 클래스) 판정을 보류하고 true.
   */
  public boolean canSpawn(String itemClass, String variant, String familyPattern) {
    if (familyPattern == null || data.pools().isEmpty()) {
      return true;
    }
    // 전체 풀이 모르는 패밀리면 **판정 보류**(true). 큐레이션 패턴이 낡아 실제 키와 어긋난 경우
    // (예: IncreasedAccuracy → 게임은 IncreasedAccuracyNew) 이걸 차단으로 처리하면 멀쩡한 모드가
    // 통째로 사라져 결과가 조용히 나빠진다(실측: DPS −2.8%).
    if (!data.families().containsKey(familyPattern)) {
      return true;
    }
    Pool pool = data.pools().get(itemClass + "|" + (variant == null ? "" : variant) + "|");
    if (pool == null) {
      return true; // 이 클래스/변형 풀을 모르면 막지 않는다(무기 등 변형 없는 슬롯 포함)
    }
    return pool.prefixes().contains(familyPattern) || pool.suffixes().contains(familyPattern);
  }

  /** 표시할 아이템 클래스 목록(패밀리 키 없이 이름·개수만 필요할 때). */
  public List<ModItemClass> itemClasses() {
    return data.itemClasses();
  }

  /**
   * (아이템 클래스 × 속성 변형)의 접두/접미 패밀리를 실제 티어와 함께 해석해 돌려준다. 없으면 null.
   *
   * @param variant 방어구 속성 변형 키(str_armour 등). 빈 값/무효면 그 클래스의 첫 변형(없으면 기본 풀).
   */
  public ClassMods forItemClass(String itemClass, String variant) {
    return forItemClass(itemClass, variant, "");
  }

  /**
   * @param influence 영향력 키(shaper/elder/crusader/eyrie/basilisk/adjudicator). 빈 값이면 영향력 없음.
   */
  public ClassMods forItemClass(String itemClass, String variant, String influence) {
    ModItemClass cls =
        data.itemClasses().stream()
            .filter(c -> c.itemClass().equals(itemClass))
            .findFirst()
            .orElse(null);
    if (cls == null) {
      return null;
    }
    // 변형이 있는 클래스는 유효한 변형을 골라야 한다 — 미지정/무효면 첫 변형으로 폴백(합집합 표시 방지)
    String resolvedVariant = "";
    if (!cls.variants().isEmpty()) {
      boolean valid =
          variant != null && cls.variants().stream().anyMatch(v -> v.key().equals(variant));
      resolvedVariant = valid ? variant : cls.variants().get(0).key();
    }
    String resolvedInfluence =
        influence != null && cls.influences().stream().anyMatch(i -> i.key().equals(influence))
            ? influence
            : "";
    Pool pool = data.pools().get(itemClass + "|" + resolvedVariant + "|" + resolvedInfluence);
    List<String> prefixKeys = pool != null ? pool.prefixes() : List.of();
    List<String> suffixKeys = pool != null ? pool.suffixes() : List.of();
    return new ClassMods(
        cls.itemClass(),
        cls.name(),
        cls.nameKo(),
        resolvedVariant,
        cls.variants(),
        resolvedInfluence,
        cls.influences(),
        resolve(prefixKeys),
        resolve(suffixKeys));
  }

  private List<NamedFamily> resolve(List<String> keys) {
    return keys.stream()
        .map(
            k -> {
              ModFamily f = data.families().get(k);
              return f == null ? null : new NamedFamily(k, f);
            })
        .filter(java.util.Objects::nonNull)
        .toList();
  }
}
