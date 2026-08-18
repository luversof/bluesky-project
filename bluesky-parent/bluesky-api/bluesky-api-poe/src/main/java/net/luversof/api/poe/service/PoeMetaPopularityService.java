package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 실빌드 사용 빈도 — 시뮬 폼의 <b>목록 정렬 근거</b>.
 *
 * <p>사람은 "치프틴이 많이 쓰는 스킬"을 먼저 보고 싶어 한다. 알파벳순 목록은 그걸 못 준다. 그래서 poe.ninja 아키타입 시드(전직×메인스킬 표본 수)와
 * 패싯(아이템 사용 빈도)을 읽어 <b>서로의 선택에 따라</b> 순서를 낸다.
 *
 * <ul>
 *   <li>전직 고르면 → 그 전직이 많이 쓰는 스킬 순
 *   <li>스킬 고르면 → 그 스킬을 많이 쓰는 전직 순
 *   <li>둘 다 → 그 조합이 많이 쓰는 고유템 순
 * </ul>
 *
 * <p>⚠ 패싯은 <b>아키타입당 상위 12개로 잘려</b> 있다(실측). 그래서 "목록에 없음 = 안 쓰임"이 아니라 "상위권 아님"일 뿐이다 — 정렬 가중치로만 쓰고,
 * 목록에서 빼지는 않는다.
 */
@Service
public class PoeMetaPopularityService {

  private static final Logger logger = LoggerFactory.getLogger(PoeMetaPopularityService.class);

  private final Path seedFile;

  /** 전직 → (스킬 영문명 → 표본 수) */
  private volatile Map<String, Map<String, Integer>> skillsByAscendancy = Map.of();

  /** 스킬 영문명 → (전직 → 표본 수) */
  private volatile Map<String, Map<String, Integer>> ascendanciesBySkill = Map.of();

  /** "전직|스킬" → (아이템 이름 → 사용 수). 전직만/스킬만인 키도 함께 담아 부분 선택에도 답한다. */
  private volatile Map<String, Map<String, Integer>> itemsByKey = Map.of();

  public PoeMetaPopularityService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.seedFile = Path.of(dataDir, "ninja", "ninja-archetypes.json");
    reload();
  }

  public synchronized void reload() {
    Map<String, Map<String, Integer>> bySkillOfAsc = new LinkedHashMap<>();
    Map<String, Map<String, Integer>> byAscOfSkill = new LinkedHashMap<>();
    Map<String, Map<String, Integer>> items = new LinkedHashMap<>();
    if (!Files.exists(seedFile)) {
      logger.warn("poe.ninja 아키타입 시드 없음: {} — 목록은 기본 순서로 나갑니다", seedFile);
      return;
    }
    try (InputStream in = Files.newInputStream(seedFile)) {
      JsonNode root = JsonMapper.builder().build().readTree(in);
      JsonNode arr = root.get("archetypes");
      if (arr == null || !arr.isArray()) {
        return;
      }
      for (JsonNode a : arr) {
        String asc = a.path("ascendancy").asText("");
        String skill = a.path("mainSkill").asText("");
        int sample = a.path("sample").asInt(0);
        if (asc.isEmpty() || skill.isEmpty() || sample <= 0) {
          continue;
        }
        bySkillOfAsc
            .computeIfAbsent(asc, k -> new LinkedHashMap<>())
            .merge(skill, sample, Integer::sum);
        byAscOfSkill
            .computeIfAbsent(skill, k -> new LinkedHashMap<>())
            .merge(asc, sample, Integer::sum);
        JsonNode facetItems = a.path("facets").path("groups").path("items");
        if (facetItems.isArray()) {
          for (String key : List.of(asc + "|" + skill, asc + "|", "|" + skill, "|")) {
            Map<String, Integer> bucket = items.computeIfAbsent(key, k -> new LinkedHashMap<>());
            for (JsonNode it : facetItems) {
              String name = it.path("name").asText("");
              int count = it.path("count").asInt(0);
              if (!name.isEmpty() && count > 0) {
                bucket.merge(name, count, Integer::sum);
              }
            }
          }
        }
      }
      this.skillsByAscendancy = bySkillOfAsc;
      this.ascendanciesBySkill = byAscOfSkill;
      this.itemsByKey = items;
      logger.info(
          "PoE 실빌드 인기도 로드: 전직 {} · 스킬 {} · 아이템 키 {}",
          bySkillOfAsc.size(),
          byAscOfSkill.size(),
          items.size());
    } catch (Exception e) {
      logger.warn("poe.ninja 아키타입 시드 읽기 실패: {}", seedFile, e);
    }
  }

  public boolean hasData() {
    return !skillsByAscendancy.isEmpty();
  }

  private static List<String> ordered(Map<String, Integer> counts) {
    if (counts == null || counts.isEmpty()) {
      return List.of();
    }
    List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
    entries.sort(
        Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
            .reversed()
            .thenComparing(Map.Entry::getKey));
    return entries.stream().map(Map.Entry::getKey).toList();
  }

  /** 이 전직이 많이 쓰는 스킬(영문명) 순. 전직 미지정이면 전체 합산 순. */
  public List<String> skillOrder(String ascendancy) {
    if (ascendancy == null || ascendancy.isBlank()) {
      Map<String, Integer> all = new LinkedHashMap<>();
      for (Map<String, Integer> m : skillsByAscendancy.values()) {
        m.forEach((k, v) -> all.merge(k, v, Integer::sum));
      }
      return ordered(all);
    }
    return ordered(skillsByAscendancy.get(ascendancy));
  }

  /** 이 스킬들을 많이 쓰는 전직 순. 스킬 미지정이면 전체 합산 순. */
  public List<String> ascendancyOrder(List<String> skillNames) {
    Map<String, Integer> merged = new LinkedHashMap<>();
    if (skillNames == null || skillNames.isEmpty()) {
      for (Map<String, Integer> m : ascendanciesBySkill.values()) {
        m.forEach((k, v) -> merged.merge(k, v, Integer::sum));
      }
    } else {
      for (String skill : skillNames) {
        Map<String, Integer> m = ascendanciesBySkill.get(skill);
        if (m != null) {
          m.forEach((k, v) -> merged.merge(k, v, Integer::sum));
        }
      }
    }
    return ordered(merged);
  }

  /** 이 조합이 많이 쓰는 아이템(영문 이름) 순 — 유니크 목록 정렬용. */
  public List<String> itemOrder(String ascendancy, List<String> skillNames) {
    Map<String, Integer> merged = new LinkedHashMap<>();
    String asc = ascendancy == null ? "" : ascendancy;
    List<String> skills = skillNames == null || skillNames.isEmpty() ? List.of("") : skillNames;
    for (String skill : skills) {
      Map<String, Integer> m = itemsByKey.get(asc + "|" + skill);
      if (m == null && !asc.isEmpty()) {
        m = itemsByKey.get(asc + "|");
      }
      if (m == null && !skill.isEmpty()) {
        m = itemsByKey.get("|" + skill);
      }
      if (m == null) {
        m = itemsByKey.get("|");
      }
      if (m != null) {
        m.forEach((k, v) -> merged.merge(k, v, Integer::sum));
      }
    }
    return ordered(merged);
  }

  /** 정렬 결과 묶음 — 폼이 한 번에 받아 세 목록을 재배치한다. */
  public record MetaOrder(List<String> skills, List<String> ascendancies, List<String> items) {}

  public MetaOrder order(String ascendancy, List<String> skillNames) {
    return new MetaOrder(
        skillOrder(ascendancy), ascendancyOrder(skillNames), itemOrder(ascendancy, skillNames));
  }

  /** 영문 이름 비교용 키(대소문자·공백 무시) — 젬/유니크 이름 대조에 쓴다. */
  public static String key(String name) {
    return name == null ? "" : name.toLowerCase(Locale.ROOT).replace(" ", "");
  }
}
