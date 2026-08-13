package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

/**
 * PoE 고유 아이템 검색 서비스. 데이터는 {@code ~/.poe-gamedata/unique-items.json}(파생 산출물, git 미관리)에서 부팅 시 1회
 * 로드한다. 갱신 = tools/poe-extract 의 parse-uniques.mjs 재실행 + 재시작.
 *
 * <p>고유 데이터의 {@code category} 는 거친 분류(예: "sword")지만, 각 고유의 {@code baseType} 을 베이스 아이템에 조인하면 세부
 * itemClass(한손 검/양손 검)를 얻는다. UI 그룹/필터는 이 세부 클래스를 쓰고(일반 아이템 탭과 동일 분류), 최적화기는 기존 category 를 그대로 쓴다.
 */
@Service
public class PoeUniqueDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeUniqueDataService.class);

  private record PoeUniqueData(String patch, List<PoeUniqueItem> items) {}

  /** baseType 조인 실패 시 category → 대표 itemClass 폴백(단일 클래스는 확정, 무기 애매하면 1H 기본). */
  private static final Map<String, String> FALLBACK_CLASS =
      Map.ofEntries(
          Map.entry("claw", "Claw"),
          Map.entry("dagger", "Dagger"),
          Map.entry("wand", "Wand"),
          Map.entry("sword", "One Hand Sword"),
          Map.entry("axe", "One Hand Axe"),
          Map.entry("mace", "One Hand Mace"),
          Map.entry("bow", "Bow"),
          Map.entry("staff", "Staff"),
          Map.entry("quiver", "Quiver"),
          Map.entry("shield", "Shield"),
          Map.entry("helmet", "Helmet"),
          Map.entry("body", "Body Armour"),
          Map.entry("gloves", "Gloves"),
          Map.entry("boots", "Boots"),
          Map.entry("amulet", "Amulet"),
          Map.entry("ring", "Ring"),
          Map.entry("belt", "Belt"),
          Map.entry("flask", "UtilityFlask"),
          Map.entry("jewel", "Jewel"),
          Map.entry("tincture", "Tincture"),
          Map.entry("fishing", "Fishing Rod"));

  /** 베이스에 없는 고유 전용 클래스의 한국어 라벨(팅크/낚싯대). 나머지는 base itemClasses() 에서 온다. */
  private static final Map<String, String> EXTRA_KO =
      Map.of("Tincture", "팅크", "Fishing Rod", "낚싯대");

  private final Path dataFile;
  private final PoeBaseItemDataService poeBaseItemDataService;
  private volatile PoeUniqueData data;
  private volatile Map<String, String> classBySlug = Map.of(); // 고유 slug → 세부 itemClass

  public PoeUniqueDataService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir,
      PoeBaseItemDataService poeBaseItemDataService) {
    this.poeBaseItemDataService = poeBaseItemDataService;
    this.dataFile = Path.of(dataDir, "unique-items.json");
    reload();
  }

  /** 데이터 파일을 다시 읽는다 (추출 파이프라인 완료 후 재시작 없이 반영). 베이스가 먼저 로드돼 있어야 세부 클래스 조인이 된다. */
  public synchronized void reload() {
    PoeUniqueData loaded = new PoeUniqueData("", List.of());
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        loaded = jsonMapper.readValue(inputStream, PoeUniqueData.class);
        logger.info("PoE 고유 아이템 데이터 로드: {} ({}개)", dataFile, loaded.items().size());
      } catch (Exception e) {
        logger.warn("PoE 고유 아이템 데이터 로드 실패: {}", dataFile, e);
      }
    } else {
      logger.warn("PoE 고유 아이템 데이터 없음: {} — tools/poe-extract 파이프라인 실행 필요", dataFile);
    }
    this.data = loaded;
    this.classBySlug = deriveClasses(loaded);
  }

  /** 각 고유의 세부 itemClass 를 baseType→베이스 조인으로 계산(실패 시 category 폴백). */
  private Map<String, String> deriveClasses(PoeUniqueData loaded) {
    Map<String, String> nameToClass = poeBaseItemDataService.baseNameToClass();
    Map<String, String> derived = new LinkedHashMap<>();
    for (PoeUniqueItem item : loaded.items()) {
      String cls =
          item.baseType() != null
              ? nameToClass.get(item.baseType().toLowerCase(Locale.ROOT))
              : null;
      if (cls == null) {
        cls = FALLBACK_CLASS.getOrDefault(item.category(), item.category());
      }
      derived.put(item.slug(), cls);
    }
    return derived;
  }

  /** 이 고유의 세부 itemClass(한손 검/양손 검 등). */
  public String itemClassOf(PoeUniqueItem item) {
    return classBySlug.getOrDefault(item.slug(), item.category());
  }

  /**
   * baseType→베이스 조인으로 요구 능력치(힘/민첩/지능)와 아이콘 키(베이스 slug)를 채워 반환. 목록/상세에서 제목 아래 요구조건과 아이콘을 보여주기 위함.
   * 고유의 로컬 "요구 40% 증가" 모드는 반영하지 않음(베이스값 표기).
   */
  private PoeUniqueItem withBase(PoeUniqueItem it) {
    PoeBaseItem base =
        it.baseType() == null
            ? null
            : poeBaseItemDataService.findByName(it.baseType()).orElse(null);
    return new PoeUniqueItem(
        it.name(),
        it.nameKo(),
        it.slug(),
        it.baseType(),
        it.baseTypeKo(),
        it.category(),
        it.requiredLevel(),
        it.league(),
        it.radius(),
        it.implicits(),
        it.implicitsKo(),
        it.explicits(),
        it.explicitsKo(),
        it.variants(),
        it.defaultVariant(),
        base != null ? base.reqStr() : null,
        base != null ? base.reqDex() : null,
        base != null ? base.reqInt() : null,
        base != null ? base.slug() : null);
  }

  public int totalCount() {
    return data.items().size();
  }

  public List<String> categories() {
    return data.items().stream().map(PoeUniqueItem::category).distinct().sorted().toList();
  }

  /**
   * 고유 아이템을 세부 itemClass 기준으로 PoB식 그룹(일반 아이템과 동일 분류)으로 묶어 반환. 한국어 라벨은 베이스 itemClass 라벨(+팅크/낚싯대 폴백).
   */
  public List<PoeBaseItemDataService.ClassGroup> categoryGroups() {
    Set<String> present = new LinkedHashSet<>(classBySlug.values());
    Map<String, String> baseKo = poeBaseItemDataService.itemClasses();
    return poeBaseItemDataService.groupsFor(
        present, cls -> baseKo.getOrDefault(cls, EXTRA_KO.getOrDefault(cls, cls)));
  }

  public Optional<PoeUniqueItem> findBySlug(String slug) {
    return data.items().stream()
        .filter(item -> item.slug().equals(slug))
        .findFirst()
        .map(this::withBase);
  }

  /** 영문 이름 정확 일치 (대소문자 무시) — PoB 임포트 매칭용 */
  public Optional<PoeUniqueItem> findByName(String name) {
    return data.items().stream().filter(item -> item.name().equalsIgnoreCase(name)).findFirst();
  }

  /**
   * @param query 이름/베이스 부분 일치(한/영)
   * @param category all|null 또는 거친 카테고리(최적화기용)
   * @param itemClass all|null 또는 세부 itemClass(UI 필터용) — baseType 조인 결과 기준
   */
  public List<PoeUniqueItem> search(String query, String category, String itemClass) {
    String normalizedQuery =
        query != null && !query.isBlank() ? query.trim().toLowerCase(Locale.ROOT) : null;
    String categoryFilter =
        category != null && !category.isBlank() && !"all".equals(category) ? category : null;
    String classFilter =
        itemClass != null && !itemClass.isBlank() && !"all".equals(itemClass) ? itemClass : null;

    return data.items().stream()
        .filter(
            item ->
                normalizedQuery == null
                    || item.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || (item.nameKo() != null && item.nameKo().contains(normalizedQuery))
                    || item.baseType().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || (item.baseTypeKo() != null && item.baseTypeKo().contains(normalizedQuery)))
        .filter(item -> categoryFilter == null || categoryFilter.equals(item.category()))
        .filter(item -> classFilter == null || classFilter.equals(itemClassOf(item)))
        .map(this::withBase)
        .toList();
  }
}
