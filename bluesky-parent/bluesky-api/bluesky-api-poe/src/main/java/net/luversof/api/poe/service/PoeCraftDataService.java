package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * 제작 엔진 데이터(10-15 C335 — 사용자 요청 "craftofexile 같은 제작 시뮬레이터 · 에뮬레이터").
 * tools/poe-extract/parse-mods-full.mjs 가 만든 {@code craft-mods.json}(풀마다 옵션 id → 그 풀의 가중치, 옵션마다 계열
 * · 접두/접미 · 아이템 레벨 · 옵션 그룹 · 옵션 태그)과 parse-essences.mjs 의 {@code essences.json}(보장 옵션 id · 그룹).
 *
 * <p>mods.json 의 등급 가중치는 그 등급이 처음 나온 풀의 값이라(옵션 254개는 풀마다 가중치가 다르다 — 힘 500/1000/333) 제작 확률에는 이 파일의
 * 풀별 가중치를 쓴다.
 */
@Service
public class PoeCraftDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeCraftDataService.class);

  /**
   * 옵션 한 줄 — f = 계열 키(mods.json families), g = prefix|suffix, l = 아이템 레벨 요구, grp = 옵션 그룹(같은 그룹은 한
   * 아이템에 하나), t = 옵션 태그(화석 · 수확 가중치용), ess = 에센스 전용.
   */
  public record CraftMod(
      String f, String g, int l, List<Integer> grp, List<String> t, Boolean ess) {}

  private record CraftData(
      String patch, Map<String, CraftMod> mods, Map<String, Map<String, Integer>> pools) {}

  /** 에센스 한 줄(essences.json) — modId · groups · modLevel · slug 는 10-15 C335 에 추가. */
  public record Essence(
      String family,
      String name,
      String nameKo,
      String icon,
      int tier,
      int ilvlMax,
      String gen,
      String modName,
      String modId,
      List<Integer> groups,
      Integer modLevel,
      String slug,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin) {}

  private record EssenceData(String patch, Map<String, List<Essence>> classes) {}

  private final Path craftFile;
  private final Path essenceFile;
  private volatile CraftData data = new CraftData("", Map.of(), Map.of());
  private volatile Map<String, List<Essence>> essences = Map.of();
  // 풀 → 옵션 id → 그 풀 안 같은 계열에서의 등급(1 = 아이템 레벨 요구가 가장 높은 것)
  private volatile Map<String, Map<String, Integer>> tierRank = Map.of();

  public PoeCraftDataService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.craftFile = Path.of(dataDir, "craft-mods.json");
    this.essenceFile = Path.of(dataDir, "essences.json");
    reload();
  }

  public synchronized void reload() {
    JsonMapper json = JsonMapper.builder().build();
    CraftData loaded = new CraftData("", Map.of(), Map.of());
    if (Files.exists(craftFile)) {
      try (InputStream in = Files.newInputStream(craftFile)) {
        loaded = json.readValue(in, CraftData.class);
        logger.info(
            "PoE 제작 데이터 로드: {} (옵션 {}개, 풀 {}개)",
            craftFile,
            loaded.mods().size(),
            loaded.pools().size());
      } catch (Exception e) {
        logger.warn("PoE 제작 데이터 로드 실패: {}", craftFile, e);
      }
    } else {
      logger.warn("PoE 제작 데이터 없음: {} — parse-mods-full.mjs 실행 필요", craftFile);
    }
    Map<String, List<Essence>> ess = Map.of();
    if (Files.exists(essenceFile)) {
      try (InputStream in = Files.newInputStream(essenceFile)) {
        EssenceData e = json.readValue(in, EssenceData.class);
        ess = e.classes() == null ? Map.of() : e.classes();
      } catch (Exception e) {
        logger.warn("PoE 에센스 로드 실패(제작): {}", essenceFile, e);
      }
    }
    this.data = loaded;
    this.essences = ess;
    this.tierRank = ranks(loaded);
  }

  private static Map<String, Map<String, Integer>> ranks(CraftData d) {
    Map<String, Map<String, Integer>> out = new HashMap<>();
    for (Map.Entry<String, Map<String, Integer>> pool : d.pools().entrySet()) {
      Map<String, List<String>> byFamily = new HashMap<>();
      for (String id : pool.getValue().keySet()) {
        CraftMod m = d.mods().get(id);
        if (m != null) {
          byFamily.computeIfAbsent(m.g() + "|" + m.f(), k -> new ArrayList<>()).add(id);
        }
      }
      Map<String, Integer> rank = new HashMap<>();
      for (List<String> ids : byFamily.values()) {
        ids.sort(Comparator.comparingInt((String id) -> d.mods().get(id).l()).reversed());
        for (int i = 0; i < ids.size(); i++) {
          rank.put(ids.get(i), i + 1);
        }
      }
      out.put(pool.getKey(), rank);
    }
    return out;
  }

  public boolean hasData() {
    return !data.mods().isEmpty();
  }

  public String patch() {
    return data.patch();
  }

  public CraftMod mod(String id) {
    return data.mods().get(id);
  }

  /** 풀(클래스|변형|영향력) → 옵션 id → 가중치. 없으면 빈 맵. */
  public Map<String, Integer> pool(String poolKey) {
    return data.pools().getOrDefault(poolKey, Map.of());
  }

  public boolean hasPool(String poolKey) {
    return data.pools().containsKey(poolKey);
  }

  /** 그 풀 안 같은 계열에서의 등급(1 = 최상위). 모르면 null. */
  public Integer tierRank(String poolKey, String modId) {
    return tierRank.getOrDefault(poolKey, Map.of()).get(modId);
  }

  public List<Essence> essences(String itemClass) {
    return essences.getOrDefault(itemClass, List.of());
  }

  /** 클래스 · 에센스 슬러그 → 에센스(그 클래스에 보장 옵션이 있는 것만). 없으면 null. */
  public Essence essence(String itemClass, String slug) {
    if (slug == null) {
      return null;
    }
    return essences(itemClass).stream().filter(e -> slug.equals(e.slug())).findFirst().orElse(null);
  }
}
