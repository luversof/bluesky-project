package net.luversof.api.poe.service;

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
 * 시뮬레이터 1단계 — 젬 DPS 랭킹 배치.
 *
 * <p>모든 액티브 젬을 동일한 템플릿 빌드(레벨 90 사이온, 트리·지원 젬 없음, 태그별 표준 무기 지급)에 하나씩 끼워 PoB 엔진으로 병렬 평가하고, DPS 내림차순
 * 랭킹을 {@code ~/.poe-gamedata/sim/gem-ranking.json} 에 저장한다. 진행 상태는 관리 탭 추출 파이프라인과 같은 폴링 패턴으로 노출한다.
 * 결과는 조건이 통제된 상대 비교값이다 (오라/커스 등 비피해 스킬은 자연히 하위).
 */
@Service
public class PoeSimService {

  private static final Logger logger = LoggerFactory.getLogger(PoeSimService.class);
  private static final int LOG_LIMIT = 200;

  public enum Status {
    IDLE,
    SUCCESS,
    FAILED
  }

  private record PoeGemRankingData(String patch, List<PoeGemRank> ranking) {}

  private final PoeGemDataService poeGemDataService;
  private final PoePobEngineService poePobEngineService;
  private final Path rankingFile;
  private final String treeVersion;
  private final int parallelism;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicInteger completedCount = new AtomicInteger();
  private volatile int totalCount;
  private volatile Status lastStatus = Status.IDLE;
  private final Deque<String> logLines = new ArrayDeque<>();

  private volatile PoeGemRankingData data = new PoeGemRankingData("", List.of());

  public PoeSimService(
      PoeGemDataService poeGemDataService,
      PoePobEngineService poePobEngineService,
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir,
      @Value("${poe.sim.tree-version:3_28}") String treeVersion,
      @Value("${poe.sim.parallelism:6}") int parallelism) {
    this.poeGemDataService = poeGemDataService;
    this.poePobEngineService = poePobEngineService;
    this.rankingFile = Path.of(dataDir, "sim", "gem-ranking.json");
    this.treeVersion = treeVersion;
    this.parallelism = parallelism;
    reload();
  }

  /** 랭킹 파일을 다시 읽는다 (배치 완료 후 재시작 없이 반영). */
  public synchronized void reload() {
    PoeGemRankingData loaded = new PoeGemRankingData("", List.of());
    if (Files.exists(rankingFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(rankingFile)) {
        loaded = jsonMapper.readValue(inputStream, PoeGemRankingData.class);
        logger.info("PoE 젬 랭킹 로드: {} ({}개)", rankingFile, loaded.ranking().size());
      } catch (Exception e) {
        logger.warn("PoE 젬 랭킹 로드 실패: {}", rankingFile, e);
      }
    }
    this.data = loaded;
  }

  public boolean isAvailable() {
    return poePobEngineService.isAvailable() && poeGemDataService.hasData();
  }

  public boolean isRunning() {
    return running.get();
  }

  public Status lastStatus() {
    return lastStatus;
  }

  public int progressDone() {
    return completedCount.get();
  }

  public int progressTotal() {
    return totalCount;
  }

  public String rankingPatch() {
    return data.patch();
  }

  public List<PoeGemRank> ranking() {
    return data.ranking();
  }

  public synchronized List<String> logTail() {
    return List.copyOf(logLines);
  }

  private synchronized void log(String line) {
    logLines.addLast(line);
    while (logLines.size() > LOG_LIMIT) {
      logLines.removeFirst();
    }
  }

  /** 랭킹 배치를 시작한다 (이미 실행 중이면 무시). */
  public void start() {
    if (!isAvailable() || !running.compareAndSet(false, true)) {
      return;
    }
    synchronized (this) {
      logLines.clear();
    }
    completedCount.set(0);
    Thread thread = new Thread(this::runBatch, "poe-sim-ranking");
    thread.setDaemon(true);
    thread.start();
  }

  private void runBatch() {
    long startedAt = System.currentTimeMillis();
    try {
      java.util.Set<String> seenNames = new java.util.HashSet<>();
      List<PoeGem> activeGems =
          poeGemDataService.search(null, "active", "all", null).stream()
              .filter(gem -> !gem.levels().isEmpty())
              // dat 에 같은 이름의 젬이 중복 수록된 경우(변형 등) 첫 항목만 평가
              .filter(gem -> seenNames.add(gem.name()))
              .toList();
      totalCount = activeGems.size();
      log("액티브 젬 " + totalCount + "개 평가 시작 (병렬 " + parallelism + ")");

      List<PoeGemRank> ranking = new ArrayList<>();
      ExecutorService executor = Executors.newFixedThreadPool(parallelism);
      try {
        List<java.util.concurrent.Future<PoeGemRank>> futures = new ArrayList<>();
        for (PoeGem gem : activeGems) {
          futures.add(executor.submit(() -> evaluate(gem)));
        }
        for (java.util.concurrent.Future<PoeGemRank> future : futures) {
          PoeGemRank rank = future.get();
          if (rank != null) {
            ranking.add(rank);
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

      ranking.sort(Comparator.comparingDouble(PoeGemRank::dps).reversed());
      Files.createDirectories(rankingFile.getParent());
      JsonMapper jsonMapper = JsonMapper.builder().build();
      Files.writeString(
          rankingFile,
          jsonMapper.writeValueAsString(new PoeGemRankingData(poeGemDataService.patch(), ranking)),
          StandardCharsets.UTF_8);
      reload();
      log(
          "완료: "
              + ranking.size()
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
      logger.warn("PoE 젬 랭킹 배치 실패", e);
    } finally {
      running.set(false);
    }
  }

  /** 젬 하나 평가 — 실패해도 배치 전체는 계속한다 */
  private PoeGemRank evaluate(PoeGem gem) {
    try {
      String weapon = weaponFor(gem);
      Map<String, Double> values = poePobEngineService.calculateValues(templateXml(gem, weapon));
      return new PoeGemRank(
          gem.slug(),
          gem.name(),
          gem.nameKo(),
          gem.color(),
          gem.tagsKo(),
          weapon,
          values.getOrDefault("CombinedDPS", 0d),
          values.getOrDefault("AverageDamage", 0d),
          values.getOrDefault("Speed", 0d));
    } catch (Exception e) {
      log("평가 실패: " + gem.name() + " — " + e.getMessage());
      return null;
    }
  }

  /** 공격 젬은 무기가 없으면 맨손이라 태그 기준 표준 무기를 지급한다 */
  private String weaponFor(PoeGem gem) {
    List<String> tags = gem.tags() != null ? gem.tags() : List.of();
    if (tags.contains("Bow")) {
      return "Thicket Bow";
    }
    if (tags.contains("Attack")) {
      return "Vaal Axe";
    }
    return null;
  }

  /** 템플릿 빌드 XML — 모든 젬에 동일 조건 (레벨 90 사이온, 젬 20/20, 트리 없음) */
  private String templateXml(PoeGem gem, String weapon) {
    StringBuilder items = new StringBuilder("<Items activeItemSet=\"1\">");
    if (weapon != null) {
      items
          .append("<Item id=\"1\">\nRarity: RARE\nSim Weapon\n")
          .append(weapon)
          // 맨몸 레벨 90 은 명중률이 바닥이라 공격 젬이 일괄 불리해진다 — 명중 보정 포함
          .append(
              "\nItem Level: 84\nImplicits: 0\nAdds 60 to 120 Physical Damage\n+2000 to Accuracy"
                  + " Rating\n</Item>")
          .append("<ItemSet id=\"1\"><Slot name=\"Weapon 1\" itemId=\"1\"/></ItemSet>");
    }
    items.append("</Items>");
    return "<PathOfBuilding>"
        + "<Build level=\"90\" targetVersion=\"3_0\" className=\"Scion\" ascendClassName=\"None\""
        + " mainSocketGroup=\"1\"/>"
        + "<Skills activeSkillSet=\"1\"><SkillSet id=\"1\">"
        + "<Skill mainActiveSkill=\"1\" enabled=\"true\" slot=\"Body Armour\">"
        + "<Gem nameSpec=\""
        + gem.name()
        + "\" level=\"20\" quality=\"20\" enabled=\"true\"/>"
        + "</Skill></SkillSet></Skills>"
        + "<Tree activeSpec=\"1\"><Spec treeVersion=\""
        + treeVersion
        + "\" classId=\"0\" ascendClassId=\"0\" nodes=\"\"/></Tree>"
        + items
        + "</PathOfBuilding>";
  }
}
