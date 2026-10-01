package net.luversof.api.poe.poe2;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.luversof.api.poe.service.NinjaSnapshotSync;
import tools.jackson.databind.JsonNode;

/**
 * PoE2 poe.ninja 스냅샷 동기 — 시뮬레이터(선택지 순서·미리보기·실행 후보)가 쓰는 ninja-builds2.json·ninja-archetypes2.json 을
 * poe.ninja 새 버전이 나올 때마다 다시 받는다. PoE1(PoeNinjaSyncService)과 같은 규칙: {@code
 * poe2.ninja-sync.interval}(기본 30분)마다 버전 확인 → fetch-ninja-builds2.mjs, {@code
 * poe2.ninja-sync.engine-cron}(기본 매일 05:30)에 실빌드 출발점(fetch-ninja-start2.mjs). 파일은 Poe2NinjaService
 * 가 수정 시각으로 다시 읽는다. 데이터 추출(run-all2)이 돌고 있으면 건너뛴다.
 */
@Service
public class Poe2NinjaSyncService {

  private static final Logger logger = LoggerFactory.getLogger(Poe2NinjaSyncService.class);

  private final NinjaSnapshotSync sync;
  private final boolean enabled;
  private final Duration interval;
  private final String engineCron;
  private final Poe2ExtractService extract;
  private ThreadPoolTaskScheduler scheduler;

  public Poe2NinjaSyncService(
      @Value("${poe2.extract-dir:tools/poe2-extract}") String extractDir,
      @Value("${poe2.data-dir:${user.home}/.poe-gamedata/poe2}") String dataDir,
      @Value("${poe2.ninja-sync.enabled:true}") boolean enabled,
      @Value("${poe2.ninja-sync.interval:PT30M}") Duration interval,
      @Value("${poe2.ninja-sync.engine-cron:0 30 5 * * *}") String engineCron,
      Poe2ExtractService extract) {
    Path ninjaDir = Path.of(dataDir, "ninja");
    this.sync =
        new NinjaSnapshotSync(
            "poe2",
            Path.of(extractDir),
            "fetch-ninja-builds2.mjs",
            ninjaDir.resolve("ninja-archetypes2.json"),
            Poe2NinjaSyncService::storedSnapshot,
            ninjaDir.resolve("ninja-sync-state2.json"));
    this.enabled = enabled;
    this.interval = interval;
    this.engineCron = engineCron;
    this.extract = extract;
  }

  /** ninja-archetypes2.json 은 리그 하나 — {league, snapshot}. 리그가 바뀌었으면 다른 스냅샷으로 본다. */
  static String storedSnapshot(JsonNode root, String league) {
    JsonNode snap = root.get("snapshot");
    if (snap == null || snap.isNull()) {
      return null;
    }
    if (league != null && !league.equals(root.path("league").asString(""))) {
      return null;
    }
    return snap.asString();
  }

  @PostConstruct
  void start() {
    if (!enabled || !sync.isAvailable()) {
      logger.info("PoE2 poe.ninja 스냅샷 동기 꺼짐 (enabled={}, 스크립트={})", enabled, sync.isAvailable());
      return;
    }
    scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("poe2-ninja-sync-");
    scheduler.setDaemon(true);
    scheduler.initialize();
    scheduler.scheduleWithFixedDelay(
        this::syncBuilds, java.time.Instant.now().plus(Duration.ofMinutes(3)), interval);
    scheduler.schedule(this::syncEngine, new CronTrigger(engineCron));
    logger.info("PoE2 poe.ninja 스냅샷 동기 시작 (확인 {}, 엔진 단계 '{}')", interval, engineCron);
  }

  @PreDestroy
  void stop() {
    if (scheduler != null) {
      scheduler.shutdown();
    }
  }

  public NinjaSnapshotSync.Status status() {
    return sync.status();
  }

  public boolean syncBuilds() {
    if (extract.isRunning()) {
      return false;
    }
    return sync.syncIfChanged(Duration.ofMinutes(30));
  }

  public boolean syncEngine() {
    if (extract.isRunning()) {
      return false;
    }
    return sync.runEngineStepsIfStale(
        List.of(List.of("fetch-ninja-start2.mjs")), Duration.ofHours(2));
  }
}
