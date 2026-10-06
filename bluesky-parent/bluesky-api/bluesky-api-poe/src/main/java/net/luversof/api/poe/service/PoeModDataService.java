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
      List<String> koMin,
      // en(최대롤) 줄별 리마인더 Id(인게임 회색 부연, mods.json reminderText 사전 키 — 10-04 C131). 없으면 null
      List<List<String>> rem) {}

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

  /** 풀 하나(클래스|변형) — 접두/접미 패밀리 키(+영향력 없음 풀엔 부패 임플리싯 키). */
  private record Pool(
      List<String> prefixes,
      List<String> suffixes,
      List<String> corrupted,
      List<String> enchants) {}

  private record ModData(
      String patch,
      List<ModItemClass> itemClasses,
      Map<String, Pool> pools,
      Map<String, ModFamily> families,
      Map<String, PoeReminderText> reminderText,
      // 풀 밖 옵션의 영 · 한 쌍(플라스크 · 제작대 · 엘드리치 …) — 빌드 화면 번역 사전 보강(10-04 C135)
      List<ExtraPair> extra,
      // 옵션 이름 영 → 한 전부(접두 · 접미) — 마법 아이템 이름(10-04 C146)
      Map<String, String> affixNames) {}

  /** 풀 밖 옵션 한 개의 영 · 한 문장(최대 · 최소롤). */
  public record ExtraPair(
      List<String> en,
      List<String> ko,
      List<String> enMin,
      List<String> koMin,
      // en 줄별 리마인더 Id(10-04 C141). 없으면 null
      List<List<String>> rem) {}

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
      List<NamedFamily> suffixes,
      List<NamedFamily> corrupted,
      List<NamedFamily> enchants) {}

  public record NamedFamily(String key, ModFamily family) {}

  /**
   * 전체 모드 풀의 모든 패밀리 — 번역 사전 구축용.
   *
   * <p>큐레이션 풀(mod-pool.json)은 시뮬 후보로 쓰는 것만 담고 있어, 플라스크·주얼 등 거기 없는 모드는 임포트한 빌드에서 영문으로 남았다. 전체
   * 풀(mods.json)에는 ko 가 함께 있으므로 사전의 빈 자리를 채우는 데 쓴다.
   */
  public java.util.Collection<ModFamily> allFamilies() {
    return data.families().values();
  }

  private final Path dataFile;
  private volatile ModData data =
      new ModData("", List.of(), Map.of(), Map.of(), Map.of(), List.of(), Map.of());

  /**
   * 한 풀에서 티어 사다리 하나 — 게임의 ModTypeKey 하나. 티어 Id 는 상위 먼저.
   *
   * @param key 사람이 읽는 이름(구성 티어의 패밀리 키 중 가장 많은 것)
   * @param gen prefix | suffix
   * @param families 게임 ModFamily 값 — 둘이 겹치면 한 아이템에 같이 못 붙는다
   */
  public record PoolGroup(
      String key, String gen, int modType, List<Integer> families, List<String> tiers) {}

  /** 한 풀 안에서 태그 서명이 같은 베이스들과 그 베이스들에 붙는 사다리 — 같은 클래스라도 베이스마다 붙는 모드가 다르다(소환수 피해는 뼈 반지·황혼 반지에만). */
  public record PoolSig(List<String> bases, List<PoolGroup> groups) {}

  /** mod-pool-tiers.json — (클래스|변형|) 풀 → 서명별 베이스·사다리. */
  private record PoolTierData(String patch, Map<String, List<PoolSig>> pools) {}

  private final Path tiersFile;
  private volatile Map<String, List<PoolSig>> poolTiers = Map.of();

  /** 티어 Id → 티어(모든 패밀리). 풀 묶음이 가리키는 티어의 문장을 찾을 때 쓴다. */
  private volatile Map<String, ModTier> tierIndex = Map.of();

  public PoeModDataService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "mods.json");
    this.tiersFile = Path.of(dataDir, "mod-pool-tiers.json");
    reload();
  }

  public synchronized void reload() {
    ModData loaded = new ModData("", List.of(), Map.of(), Map.of(), Map.of(), List.of(), Map.of());
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
    this.affixNameKo = null; // 다시 읽으면 옵션 이름 색인도 새로(C145)
    Map<String, ModTier> index = new java.util.HashMap<>();
    for (ModFamily family : loaded.families().values()) {
      for (ModTier tier : family.tiers()) {
        index.putIfAbsent(tier.id(), tier);
      }
    }
    this.tierIndex = index;
    Map<String, List<PoolSig>> tiers = Map.of();
    if (Files.exists(tiersFile)) {
      try (InputStream inputStream = Files.newInputStream(tiersFile)) {
        PoolTierData t = JsonMapper.builder().build().readValue(inputStream, PoolTierData.class);
        tiers = t.pools() == null ? Map.of() : t.pools();
        logger.info("PoE 풀별 티어 로드: {} (풀 {}개)", tiersFile, tiers.size());
      } catch (Exception e) {
        logger.warn("PoE 풀별 티어 로드 실패: {}", tiersFile, e);
      }
    } else {
      logger.warn("PoE 풀별 티어 없음: {} — parse-mods-full.mjs 실행 필요", tiersFile);
    }
    this.poolTiers = tiers;
  }

  /**
   * 이 (아이템 클래스 × 속성 변형) 풀의 티어 사다리들 — 풀을 모르면 빈 목록.
   *
   * <p>families 의 티어 사다리는 같은 그룹을 <b>모든 클래스에서 합친 것</b>이라(생명력 +175~189 는 갑옷 전용인데 장화 패밀리에도 들어 있다) "이
   * 베이스에서 몇 티어"를 말하려면 이 목록을 써야 한다. 사다리는 게임 ModTypeKey 로 묶여 있어, Id 이름 규칙으로 쪼개진 최상위 티어(억제
   * ChanceToSuppressSpells5__)도 제자리에 들어 있다.
   */
  public List<PoolGroup> poolGroups(String itemClass, String variant, String baseName) {
    List<PoolSig> sigs = poolTiers.get(itemClass + "|" + (variant == null ? "" : variant) + "|");
    if (sigs == null || sigs.isEmpty()) {
      return List.of();
    }
    // 그 베이스의 서명 — 합집합을 쓰면 뼈 반지 전용 소환수 피해가 산호 반지에도 붙은 것으로 나온다(2026-09-29 가이드 레어 목표에서 실제로 그랬다)
    for (PoolSig sig : sigs) {
      if (sig.bases() != null && sig.bases().contains(baseName)) {
        return sig.groups() == null ? List.of() : sig.groups();
      }
    }
    return List.of(); // 모르는 베이스면 추측하지 않는다(레어 목표를 건너뛴다)
  }

  /** 티어 Id 로 티어(문장 포함)를 찾는다. 없으면 null. */
  public ModTier tier(String tierId) {
    return tierIndex.get(tierId);
  }

  public boolean hasData() {
    return !data.itemClasses().isEmpty();
  }

  private volatile Map<String, String> affixNameKo;

  /**
   * 옵션 이름(접두 "Vivid" · 접미 "of the Whelpling") → 한국어("선명한" · "- 새끼용"), 없으면 null — 마법 아이템 이름(10-04
   * C145).
   */
  public String affixNameKo(String en) {
    Map<String, String> m = affixNameKo;
    if (m == null) {
      Map<String, String> built = new java.util.HashMap<>();
      for (ModFamily f : data.families().values()) {
        for (ModTier t : f.tiers()) {
          if (t.name() != null && t.nameKo() != null && !t.nameKo().isBlank()) {
            built.putIfAbsent(t.name(), t.nameKo());
          }
        }
      }
      // 티어 이름에 없는 것(제작대 접미 "of Craft" 등)은 옵션 이름 전부 표에서(C146)
      if (data.affixNames() != null) {
        data.affixNames().forEach(built::putIfAbsent);
      }
      m = Map.copyOf(built);
      affixNameKo = m;
    }
    return en == null ? null : m.get(en);
  }

  /** 풀 밖 옵션 쌍(10-04 C135). 옛 데이터면 빈 목록. */
  public List<ExtraPair> extraPairs() {
    return data.extra() == null ? List.of() : data.extra();
  }

  /** 리마인더 Id → 영 · 한 문구(10-04 C131). 옛 데이터면 빈 맵. */
  public Map<String, PoeReminderText> reminderText() {
    return data.reminderText() == null ? Map.of() : data.reminderText();
  }

  public String patch() {
    return data.patch();
  }

  /**
   * 이 (아이템 클래스 × 속성 변형)에 해당 패밀리(게임 모드 Id 패턴)가 실제로 붙을 수 있는지 — 시뮬레이터가 베이스에 맞지 않는 모드를 얹지 않도록 게임 데이터로
   * 판정한다. 풀 자체가 없으면(데이터 미로드/미지원 클래스) 판정을 보류하고 true.
   */
  public boolean canSpawn(String itemClass, String variant, String familyPattern) {
    return canSpawn(itemClass, variant, familyPattern, "");
  }

  /**
   * @param influence 영향력 키(elder/shaper/…). 영향력 전용 패밀리(엘더 헬멧 소켓 지원 등)는 무영향력 풀에 없어 그 영향력 풀로 판정해야 한다.
   */
  public boolean canSpawn(
      String itemClass, String variant, String familyPattern, String influence) {
    if (familyPattern == null || data.pools().isEmpty()) {
      return true;
    }
    // 전체 풀이 모르는 패밀리면 **판정 보류**(true). 큐레이션 패턴이 낡아 실제 키와 어긋난 경우
    // (예: IncreasedAccuracy → 게임은 IncreasedAccuracyNew) 이걸 차단으로 처리하면 멀쩡한 모드가
    // 통째로 사라져 결과가 조용히 나빠진다(실측: DPS −2.8%).
    if (!data.families().containsKey(familyPattern)) {
      return true;
    }
    Pool pool =
        data.pools()
            .get(
                itemClass
                    + "|"
                    + (variant == null ? "" : variant)
                    + "|"
                    + (influence == null ? "" : influence));
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
    // 부패 임플리싯은 영향력과 무관 — 추출기가 영향력 없음("") 풀에만 담아두므로 항상 거기서 읽는다
    Pool basePool = data.pools().get(itemClass + "|" + resolvedVariant + "|");
    List<String> corruptedKeys =
        basePool != null && basePool.corrupted() != null ? basePool.corrupted() : List.of();
    List<String> enchantKeys =
        basePool != null && basePool.enchants() != null ? basePool.enchants() : List.of();
    return new ClassMods(
        cls.itemClass(),
        cls.name(),
        cls.nameKo(),
        resolvedVariant,
        cls.variants(),
        resolvedInfluence,
        cls.influences(),
        resolve(prefixKeys),
        resolve(suffixKeys),
        resolve(corruptedKeys),
        resolve(enchantKeys));
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
