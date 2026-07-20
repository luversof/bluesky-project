package net.luversof.api.poe.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * PoE 스킬젬 데이터 검색 서비스.
 *
 * <p>데이터는 tools/poe-extract 오프라인 파이프라인이 생성하는 파생 산출물이라 git 으로 관리하지 않고, 기본적으로 {@code
 * ~/.poe-gamedata/skill-gems.json} 에서 부팅 시 1회 로드한다(poe.data-dir 프로퍼티로 변경 가능). 파일이 없으면 부팅은 정상 진행되고
 * 화면에 안내가 표시된다. 시즌 갱신 = 파이프라인 재실행 + 서버 재시작.
 */
@Service
public class PoeGemDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeGemDataService.class);

  private record PoeGemData(String patch, List<PoeGem> gems) {}

  /** 그룹 칩 UI용 — 한 태그(key=영문, ko=한국어) */
  public record TagEntry(String key, String ko) {}

  /** 태그 그룹(key=그룹 id, 라벨은 게이트가 메시지로 해석) */
  public record TagGroup(String key, List<TagEntry> tags) {}

  /** 큐레이션: 젬 태그 분류 (그룹 → 소속 태그 표시 순서). 데이터에 없는 태그는 자동 제외, 미분류는 other 그룹. */
  private static final List<Map.Entry<String, List<String>>> TAG_GROUPS =
      List.of(
          Map.entry(
              "type",
              List.of(
                  "Attack",
                  "Spell",
                  "Minion",
                  "Aura",
                  "Curse",
                  "Hex",
                  "Herald",
                  "Warcry",
                  "Brand",
                  "Totem",
                  "Trap",
                  "Mine",
                  "Golem",
                  "Blessing",
                  "Mark",
                  "Guard",
                  "Stance")),
          Map.entry("damage", List.of("Physical", "Fire", "Cold", "Lightning", "Chaos")),
          Map.entry(
              "delivery",
              List.of(
                  "Projectile",
                  "Melee",
                  "AoE",
                  "Nova",
                  "Strike",
                  "Slam",
                  "Chaining",
                  "Bow",
                  "Channelling",
                  "Movement",
                  "Travel",
                  "Blink",
                  "Orb",
                  "Link",
                  "Retaliation")),
          Map.entry(
              "trait",
              List.of(
                  "Duration",
                  "Trigger",
                  "Critical",
                  "Support",
                  "Exceptional",
                  "Vaal",
                  "Arcane",
                  "Prismatic")));

  private final Path dataFile;
  private volatile PoeGemData data;

  public PoeGemDataService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "skill-gems.json");
    reload();
  }

  /** 데이터 파일을 다시 읽는다 (추출 파이프라인 완료 후 재시작 없이 반영). */
  public synchronized void reload() {
    PoeGemData loaded = new PoeGemData("", List.of());
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        loaded = jsonMapper.readValue(inputStream, PoeGemData.class);
        logger.info(
            "PoE 스킬젬 데이터 로드: {} ({}개, patch {})", dataFile, loaded.gems().size(), loaded.patch());
      } catch (IOException e) {
        logger.warn("PoE 스킬젬 데이터 로드 실패: {}", dataFile, e);
      }
    } else {
      logger.warn("PoE 스킬젬 데이터 없음: {} — tools/poe-extract 파이프라인 실행 필요", dataFile);
    }
    this.data = loaded;
  }

  public boolean hasData() {
    return !data.gems().isEmpty();
  }

  public String patch() {
    return data.patch();
  }

  public int totalCount() {
    return data.gems().size();
  }

  public Optional<PoeGem> findBySlug(String slug) {
    return data.gems().stream().filter(gem -> gem.slug().equals(slug)).findFirst();
  }

  /** 영문 이름 정확 일치 (대소문자 무시) — PoB 임포트 매칭용 */
  public Optional<PoeGem> findByName(String name) {
    return data.gems().stream().filter(gem -> gem.name().equalsIgnoreCase(name)).findFirst();
  }

  /**
   * @param query 이름 부분 일치 (한/영, 대소문자 무시)
   * @param type all | active | support
   * @param color all | red | green | blue | white
   */
  public List<PoeGem> search(String query, String type, String color, String tag) {
    String normalizedQuery =
        query != null && !query.isBlank() ? query.trim().toLowerCase(Locale.ROOT) : null;
    String tagFilter = tag != null && !tag.isBlank() && !"all".equals(tag) ? tag : null;

    return data.gems().stream()
        .filter(
            gem ->
                normalizedQuery == null
                    || gem.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || (gem.nameKo() != null && gem.nameKo().contains(normalizedQuery)))
        .filter(
            gem ->
                type == null
                    || "all".equals(type)
                    || ("support".equals(type) ? gem.isSupport() : !gem.isSupport()))
        .filter(gem -> color == null || "all".equals(color) || color.equals(gem.color()))
        .filter(gem -> tagFilter == null || (gem.tags() != null && gem.tags().contains(tagFilter)))
        .toList();
  }

  /** 태그를 큐레이션 그룹으로 묶어 반환(데이터에 존재하는 태그만, 미분류는 other 그룹). */
  public List<TagGroup> tagGroups() {
    // 태그 영문 → 한국어 (첫 등장 기준) + 존재 태그 집합
    Map<String, String> tagKo = new LinkedHashMap<>();
    for (PoeGem gem : data.gems()) {
      if (gem.tags() == null) {
        continue;
      }
      for (int i = 0; i < gem.tags().size(); i++) {
        String key = gem.tags().get(i);
        String ko = gem.tagsKo() != null && i < gem.tagsKo().size() ? gem.tagsKo().get(i) : key;
        tagKo.putIfAbsent(key, ko);
      }
    }

    List<TagGroup> groups = new ArrayList<>();
    java.util.Set<String> placed = new java.util.LinkedHashSet<>();
    for (Map.Entry<String, List<String>> group : TAG_GROUPS) {
      List<TagEntry> entries = new ArrayList<>();
      for (String key : group.getValue()) {
        String ko = tagKo.get(key);
        if (ko != null) {
          entries.add(new TagEntry(key, ko));
          placed.add(key);
        }
      }
      if (!entries.isEmpty()) {
        groups.add(new TagGroup(group.getKey(), entries));
      }
    }
    List<TagEntry> others = new ArrayList<>();
    for (Map.Entry<String, String> entry : tagKo.entrySet()) {
      if (!placed.contains(entry.getKey())) {
        others.add(new TagEntry(entry.getKey(), entry.getValue()));
      }
    }
    if (!others.isEmpty()) {
      groups.add(new TagGroup("other", others));
    }
    return groups;
  }
}
