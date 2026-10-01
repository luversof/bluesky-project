package net.luversof.api.poe.poe2;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * PoE2 시뮬레이터 "젬 DPS 랭킹" 탭 — PoE1 PoeSimService 와 같은 배치(10-01 사용자 요청: PoE1 에 맞춰 탭 추가).
 *
 * <p>모든 액티브 스킬 젬을 같은 기준 캐릭터(레벨 90 워리어, 전직·트리·보조젬 없음, 젬 20/20)에 하나씩 끼우고, 젬의 무기
 * 요구(weaponRequirements)에 맞춘 레어 무기(Adds 60 to 120 Physical Damage · +2000 Accuracy — PoE1 과 같은 표준
 * 무기)를 쥐여 PoB-PoE2 엔진으로 병렬 평가한다. DPS 내림차순 랭킹을 {@code ~/.poe-gamedata/poe2/sim/gem-ranking2.json} 에
 * 저장한다. 절대값이 아니라 조건을 통제한 상대 비교값이다(엔진 기본 적 저항이 걸려 있어 수치가 낮게 보인다 — 모든 젬이 같은 조건). 시뮬레이션·다듬기와 같은 잠금을 써
 * 엔진 작업은 한 번에 하나만.
 */
@Service
public class Poe2SimRankingService {

  private static final Logger logger = LoggerFactory.getLogger(Poe2SimRankingService.class);
  private static final int LOG_LIMIT = 200;
  private static final int PARALLELISM = 4;

  /** 무기 요구 → 기준 무기 베이스(각 종류 최상위 드롭 레벨). 요구가 없으면(주문·소환수 등) 무기 없이. */
  private static final Map<String, String> BASES =
      Map.of(
          "Bow", "Fanatic Bow",
          "Crossbow", "Siege Crossbow",
          "Spear", "Grand Spear",
          "Quarterstaff", "Aegis Quarterstaff",
          "Talisman", "Maji Talisman",
          "One Hand Mace", "Fortified Hammer",
          "Two Hand Mace", "Ruination Maul");

  private static final String SHIELD_BASE = "Blacksteel Crest Shield";

  public enum Status {
    IDLE,
    SUCCESS,
    FAILED
  }

  /** 랭킹 한 건 — PoE1 PoeGemRank 와 같은 필드. weapon = 쥐여 준 기준 무기 베이스(없으면 null). */
  public record GemRank(
      String slug,
      String name,
      String nameKo,
      String color,
      List<String> tagsKo,
      String weapon,
      double dps,
      double averageDamage,
      double speed) {}

  public record RankingData(String patch, List<GemRank> ranking) {}

  public record RankingStatus(
      boolean available,
      boolean running,
      String status,
      int progressDone,
      int progressTotal,
      List<String> logLines) {}

  private final Poe2DataService data;
  private final Poe2PobEngineService engine;
  private final Poe2RefineService refine;
  private final Path rankingFile;
  private final JsonMapper json = JsonMapper.builder().build();

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicInteger completedCount = new AtomicInteger();
  private volatile int totalCount;
  private volatile Status lastStatus = Status.IDLE;
  private final Deque<String> logLines = new ArrayDeque<>();
  private volatile RankingData ranking = new RankingData("", List.of());

  public Poe2SimRankingService(
      Poe2DataService data,
      Poe2PobEngineService engine,
      Poe2RefineService refine,
      @Value("${poe2.data-dir:${user.home}/.poe-gamedata/poe2}") String dataDir) {
    this.data = data;
    this.engine = engine;
    this.refine = refine;
    this.rankingFile = Path.of(dataDir, "sim", "gem-ranking2.json");
    reload();
  }

  public synchronized void reload() {
    RankingData loaded = new RankingData("", List.of());
    if (Files.exists(rankingFile)) {
      try (InputStream in = Files.newInputStream(rankingFile)) {
        loaded = json.readValue(in, RankingData.class);
        logger.info("PoE2 젬 랭킹 로드: {} ({}개)", rankingFile, loaded.ranking().size());
      } catch (Exception e) {
        logger.warn("PoE2 젬 랭킹 로드 실패: {}", rankingFile, e);
      }
    }
    this.ranking = loaded;
  }

  public RankingData ranking() {
    return ranking;
  }

  public RankingStatus status() {
    List<String> lines;
    synchronized (this) {
      lines = List.copyOf(logLines);
    }
    return new RankingStatus(
        engine.available(),
        running.get(),
        lastStatus.name(),
        completedCount.get(),
        totalCount,
        lines);
  }

  private synchronized void log(String line) {
    logLines.addLast(line);
    while (logLines.size() > LOG_LIMIT) {
      logLines.removeFirst();
    }
  }

  /** 배치 시작 — 엔진이 없거나 이미(랭킹·시뮬·다듬기가) 돌고 있으면 false. */
  public boolean start() {
    if (!engine.available() || !running.compareAndSet(false, true)) {
      return false;
    }
    if (!refine.tryLock()) {
      running.set(false);
      return false;
    }
    synchronized (this) {
      logLines.clear();
    }
    completedCount.set(0);
    Thread thread =
        new Thread(
            () -> {
              try {
                runBatch();
              } finally {
                refine.unlock();
                running.set(false);
              }
            },
            "poe2-sim-ranking");
    thread.setDaemon(true);
    thread.start();
    return true;
  }

  private void runBatch() {
    long startedAt = System.currentTimeMillis();
    try {
      java.util.Set<String> seen = new java.util.HashSet<>();
      List<Poe2.Gem> gems =
          data.searchGems(null, "skill", null, null).stream()
              .map(g -> data.gemByName(g.name()).orElse(g)) // searchGems 는 레벨 표를 뺀다 — 원본으로
              .filter(g -> g.levels() != null && !g.levels().isEmpty())
              .filter(g -> seen.add(g.name()))
              .toList();
      totalCount = gems.size();
      log("액티브 스킬 젬 " + totalCount + "개 평가 시작 (병렬 " + PARALLELISM + ")");
      List<GemRank> result = new ArrayList<>();
      ExecutorService executor = Executors.newFixedThreadPool(PARALLELISM);
      try {
        List<java.util.concurrent.Future<GemRank>> futures = new ArrayList<>();
        for (Poe2.Gem gem : gems) {
          futures.add(executor.submit(() -> evaluate(gem)));
        }
        for (java.util.concurrent.Future<GemRank> f : futures) {
          GemRank r = f.get();
          if (r != null) {
            result.add(r);
          }
          int done = completedCount.incrementAndGet();
          if (done % 25 == 0) {
            log(done + "/" + totalCount + " 완료");
          }
        }
      } finally {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
      }
      result.sort(Comparator.comparingDouble(GemRank::dps).reversed());
      Files.createDirectories(rankingFile.getParent());
      Files.writeString(
          rankingFile,
          json.writeValueAsString(new RankingData(data.patch(), result)),
          StandardCharsets.UTF_8);
      reload();
      log(
          "완료: "
              + result.size()
              + "개, "
              + (System.currentTimeMillis() - startedAt) / 1000
              + "초 → "
              + rankingFile);
      lastStatus = Status.SUCCESS;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      lastStatus = Status.FAILED;
      log("중단됨: " + e.getMessage());
    } catch (Exception e) {
      lastStatus = Status.FAILED;
      log("실패: " + e);
      logger.warn("PoE2 젬 랭킹 배치 실패", e);
    }
  }

  /** 젬 하나 평가 — 실패해도 배치 전체는 계속한다(null). */
  private GemRank evaluate(Poe2.Gem gem) {
    try {
      String weapon = weaponFor(gem.weaponRequirements());
      Poe2PobEngineService.Result r = engine.calc(templateXml(gem, weapon));
      if (r.error() != null) {
        log("실패 " + gem.name() + ": " + r.error());
        return null;
      }
      Map<String, Double> v = r.values();
      return new GemRank(
          gem.slug(),
          gem.name(),
          gem.nameKo(),
          gem.color(),
          gem.tagsKo(),
          weapon,
          v.getOrDefault("CombinedDPS", 0d),
          v.getOrDefault("AverageDamage", 0d),
          v.getOrDefault("Speed", 0d));
    } catch (RuntimeException e) {
      log("실패 " + gem.name() + ": " + e.getMessage());
      return null;
    }
  }

  /**
   * 무기 요구 → 기준 무기. "Bow" 처럼 종류가 적혀 있으면 그 종류, 방패 요구면 메이스+방패(SHIELD), 맨손만이면 없음, "Any Martial Weapon"
   * 류는 한손 메이스(PoE1 의 "공격 = 표준 근접 무기"와 같은 취지).
   */
  static String weaponFor(String req) {
    if (req == null || req.isBlank()) {
      return null;
    }
    List<String> parts = List.of(req.split(",\\s*"));
    for (Map.Entry<String, String> e : BASES.entrySet()) {
      if (parts.contains(e.getKey())) {
        return e.getValue();
      }
    }
    if (req.contains("Shield")) {
      return SHIELD_BASE;
    }
    if (req.contains("Unarmed") && !req.contains("Martial")) {
      return null;
    }
    return BASES.get("One Hand Mace");
  }

  /** 기준 캐릭터 XML — tools 시제품(scratchpad rank2-proto)으로 무기 부가 피해·젬 레벨이 실제 반영되는 것을 확인한 형식. */
  static String templateXml(Poe2.Gem gem, String weapon) {
    StringBuilder items = new StringBuilder();
    StringBuilder slots = new StringBuilder();
    String standard =
        "Item Level: 82\nQuality: 20\nImplicits: 0\nAdds 60 to 120 Physical Damage\n+2000 to Accuracy Rating\n";
    if (SHIELD_BASE.equals(weapon)) {
      // 방패 요구 스킬 — 한손 메이스(표준 무기) + 방패
      items
          .append("<Item id=\"1\">\nRarity: RARE\nRanking Weapon\n")
          .append(BASES.get("One Hand Mace"))
          .append('\n')
          .append(standard)
          .append("</Item>\n");
      items
          .append("<Item id=\"2\">\nRarity: RARE\nRanking Shield\n")
          .append(SHIELD_BASE)
          .append("\nItem Level: 82\nImplicits: 0\n</Item>\n");
      slots.append("<Slot itemId=\"1\" name=\"Weapon 1\"/><Slot itemId=\"2\" name=\"Weapon 2\"/>");
    } else if (weapon != null) {
      items
          .append("<Item id=\"1\">\nRarity: RARE\nRanking Weapon\n")
          .append(weapon)
          .append('\n')
          .append(standard)
          .append("</Item>\n");
      slots.append("<Slot itemId=\"1\" name=\"Weapon 1\"/>");
    }
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<PathOfBuilding2>\n"
        + "<Build level=\"90\" targetVersion=\"0_1\" className=\"Warrior\" ascendClassName=\"None\""
        + " mainSocketGroup=\"1\" characterLevelAutoMode=\"false\"/>\n"
        + "<Skills activeSkillSet=\"1\" defaultGemQuality=\"0\" defaultGemLevel=\"normalMaximum\">"
        + "<SkillSet id=\"1\"><Skill mainActiveSkillCalcs=\"1\" includeInFullDPS=\"false\" label=\"\""
        + " enabled=\"true\" slot=\"Body Armour\" mainActiveSkill=\"1\">"
        + "<Gem level=\"20\" quality=\"20\" nameSpec=\""
        + xmlAttr(gem.name())
        + "\" gemId=\""
        + xmlAttr(gem.id())
        + "\" enabled=\"true\" count=\"1\" enableGlobal1=\"true\" enableGlobal2=\"false\"/>"
        + "</Skill></SkillSet></Skills>\n"
        + "<Tree activeSpec=\"1\"><Spec ascendClassId=\"0\" treeVersion=\"0_5\" classId=\"6\" nodes=\"\"/></Tree>\n"
        + "<Items activeItemSet=\"1\" useSecondWeaponSet=\"false\">\n"
        + items
        + "<ItemSet useSecondWeaponSet=\"false\" id=\"1\">"
        + slots
        + "</ItemSet>\n</Items>\n</PathOfBuilding2>\n";
  }

  private static String xmlAttr(String s) {
    return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;");
  }
}
