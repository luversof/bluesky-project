package net.luversof.api.poe.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/** PoE 일반(베이스) 아이템 검색 서비스. 데이터는 {@code ~/.poe-gamedata/base-items.json}(파생 산출물, git 미관리)에서 로드한다. */
@Service
public class PoeBaseItemDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeBaseItemDataService.class);

  private record PoeBaseItemData(String patch, List<PoeBaseItem> items) {}

  /**
   * 그룹 칩 UI용 — 한 아이템 클래스(key=id, ko=한국어, slot=고유 카테고리와 공유하는 정규 슬롯 토큰). slot 은 탭 전환(일반↔고유) 시 필터 유지를
   * 위한 공통 키 — 예: One/Two Hand Sword 모두 slot="sword".
   */
  public record ClassEntry(String key, String ko, String slot) {}

  /** 아이템 클래스 그룹(key=그룹 id, 라벨은 게이트가 메시지로 해석) */
  public record ClassGroup(String key, List<ClassEntry> classes) {}

  /** 큐레이션: PoB 아이템 필터 분류 (그룹 → 소속 itemClass 표시 순서). 데이터에 없는 클래스는 자동 제외. */
  private static final List<Map.Entry<String, List<String>>> CLASS_GROUPS =
      List.of(
          Map.entry(
              "oneHand",
              List.of(
                  "Claw",
                  "Dagger",
                  "Rune Dagger",
                  "Wand",
                  "One Hand Sword",
                  "Thrusting One Hand Sword",
                  "One Hand Axe",
                  "One Hand Mace",
                  "Sceptre")),
          Map.entry(
              "twoHand",
              List.of(
                  "Bow", "Staff", "Warstaff", "Two Hand Sword", "Two Hand Axe", "Two Hand Mace")),
          Map.entry("offhand", List.of("Quiver", "Shield")),
          Map.entry("armour", List.of("Helmet", "Body Armour", "Gloves", "Boots")),
          Map.entry("jewellery", List.of("Amulet", "Ring", "Belt")),
          Map.entry("flask", List.of("LifeFlask", "ManaFlask", "HybridFlask", "UtilityFlask")),
          Map.entry("jewel", List.of("Jewel", "AbyssJewel")));

  /** itemClass → 정규 슬롯 토큰(고유 category 와 공유). 탭 전환 시 필터 유지용. 미지정 클래스는 자기 자신을 슬롯으로. */
  private static final Map<String, String> CLASS_SLOT =
      Map.ofEntries(
          Map.entry("Claw", "claw"),
          Map.entry("Dagger", "dagger"),
          Map.entry("Rune Dagger", "dagger"),
          Map.entry("Wand", "wand"),
          Map.entry("One Hand Sword", "sword"),
          Map.entry("Thrusting One Hand Sword", "sword"),
          Map.entry("Two Hand Sword", "sword"),
          Map.entry("One Hand Axe", "axe"),
          Map.entry("Two Hand Axe", "axe"),
          Map.entry("One Hand Mace", "mace"),
          Map.entry("Two Hand Mace", "mace"),
          Map.entry("Sceptre", "sceptre"),
          Map.entry("Bow", "bow"),
          Map.entry("Staff", "staff"),
          Map.entry("Warstaff", "staff"),
          Map.entry("Quiver", "quiver"),
          Map.entry("Shield", "shield"),
          Map.entry("Helmet", "helmet"),
          Map.entry("Body Armour", "body"),
          Map.entry("Gloves", "gloves"),
          Map.entry("Boots", "boots"),
          Map.entry("Amulet", "amulet"),
          Map.entry("Ring", "ring"),
          Map.entry("Belt", "belt"),
          Map.entry("LifeFlask", "flask"),
          Map.entry("ManaFlask", "flask"),
          Map.entry("HybridFlask", "flask"),
          Map.entry("UtilityFlask", "flask"),
          Map.entry("Jewel", "jewel"),
          Map.entry("AbyssJewel", "jewel"));

  private final Path dataFile;
  private volatile PoeBaseItemData data;

  public PoeBaseItemDataService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "base-items.json");
    reload();
  }

  /** 데이터 파일을 다시 읽는다 (추출 파이프라인 완료 후 재시작 없이 반영). */
  public synchronized void reload() {
    PoeBaseItemData loaded = new PoeBaseItemData("", List.of());
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        loaded = jsonMapper.readValue(inputStream, PoeBaseItemData.class);
        logger.info("PoE 일반 아이템 데이터 로드: {} ({}개)", dataFile, loaded.items().size());
      } catch (IOException e) {
        logger.warn("PoE 일반 아이템 데이터 로드 실패: {}", dataFile, e);
      }
    } else {
      logger.warn("PoE 일반 아이템 데이터 없음: {} — tools/poe-extract 파이프라인 실행 필요", dataFile);
    }
    this.data = loaded;
  }

  public int totalCount() {
    return data.items().size();
  }

  /** 아이템 클래스 id → 한국어 라벨 (표시 순서 유지) */
  public Map<String, String> itemClasses() {
    Map<String, String> classes = new LinkedHashMap<>();
    for (PoeBaseItem item : data.items()) {
      classes.putIfAbsent(
          item.itemClass(), item.itemClassKo() != null ? item.itemClassKo() : item.itemClass());
    }
    return classes;
  }

  /** 아이템 클래스를 PoB식 그룹으로 묶어 반환(데이터에 존재하는 클래스만, 미분류는 other 그룹). */
  public List<ClassGroup> itemClassGroups() {
    Map<String, String> classKo = itemClasses();
    return groupsFor(classKo.keySet(), classKo::get);
  }

  /**
   * 주어진 itemClass 집합을 PoB식 그룹으로 묶는다(고유 아이템도 재사용 — baseType 조인으로 얻은 세부 클래스를 같은 분류/슬롯으로 그룹핑).
   *
   * @param present 표시할 itemClass 들
   * @param koLookup itemClass → 한국어 라벨 (고유는 tincture/fishing 등 베이스에 없는 클래스 폴백 포함)
   */
  public List<ClassGroup> groupsFor(
      java.util.Collection<String> present, java.util.function.Function<String, String> koLookup) {
    Set<String> presentSet = new LinkedHashSet<>(present);
    List<ClassGroup> groups = new ArrayList<>();
    Set<String> placed = new LinkedHashSet<>();
    for (Map.Entry<String, List<String>> group : CLASS_GROUPS) {
      List<ClassEntry> entries = new ArrayList<>();
      for (String cls : group.getValue()) {
        if (presentSet.contains(cls)) {
          entries.add(new ClassEntry(cls, koLookup.apply(cls), CLASS_SLOT.getOrDefault(cls, cls)));
          placed.add(cls);
        }
      }
      if (!entries.isEmpty()) {
        groups.add(new ClassGroup(group.getKey(), entries));
      }
    }
    List<ClassEntry> others = new ArrayList<>();
    for (String cls : presentSet) {
      if (!placed.contains(cls)) {
        others.add(new ClassEntry(cls, koLookup.apply(cls), CLASS_SLOT.getOrDefault(cls, cls)));
      }
    }
    if (!others.isEmpty()) {
      groups.add(new ClassGroup("other", others));
    }
    return groups;
  }

  /** 베이스 이름(소문자) → itemClass (고유 baseType 조인용). */
  public Map<String, String> baseNameToClass() {
    Map<String, String> map = new LinkedHashMap<>();
    for (PoeBaseItem item : data.items()) {
      map.putIfAbsent(item.name().toLowerCase(Locale.ROOT), item.itemClass());
    }
    return map;
  }

  public Optional<PoeBaseItem> findBySlug(String slug) {
    return data.items().stream().filter(item -> item.slug().equals(slug)).findFirst();
  }

  /** 영문 이름 정확 일치 (대소문자 무시) — PoB 임포트 매칭용 */
  public Optional<PoeBaseItem> findByName(String name) {
    return data.items().stream().filter(item -> item.name().equalsIgnoreCase(name)).findFirst();
  }

  /** query: 이름 부분 일치(한/영), itemClass: all 또는 클래스 id */
  public List<PoeBaseItem> search(String query, String itemClass) {
    String normalizedQuery =
        query != null && !query.isBlank() ? query.trim().toLowerCase(Locale.ROOT) : null;

    return data.items().stream()
        .filter(
            item ->
                normalizedQuery == null
                    || item.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || (item.nameKo() != null && item.nameKo().contains(normalizedQuery)))
        .filter(
            item ->
                itemClass == null || "all".equals(itemClass) || itemClass.equals(item.itemClass()))
        .toList();
  }
}
