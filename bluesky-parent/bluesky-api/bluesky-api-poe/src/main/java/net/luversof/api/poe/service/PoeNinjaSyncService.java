package net.luversof.api.poe.service;

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
import tools.jackson.databind.JsonNode;

/**
 * PoE1 poe.ninja 스냅샷 동기 — 시뮬레이터 선택지 순서·미리보기(PoeMetaPopularityService)와 최적화기 시드가 쓰는
 * ninja-archetypes.json 을 poe.ninja 새 버전이 나올 때마다 다시 받는다(10-01 사용자 결정: 요청마다 조회하지 않고 버전 동기 스냅샷).
 *
 * <p>① {@code poe.ninja-sync.interval}(기본 30분)마다 버전 확인 → 바뀌었으면 fetch-ninja-builds.mjs → 서비스들 다시 읽기.
 * ② {@code poe.ninja-sync.engine-cron}(기본 매일 05:00)에 스냅샷이 엔진 단계 때와 다르면 calibrate-archetypes(엔진 벤치)
 * → fetch-ninja-seeds(실빌드 출발점). 데이터 추출(run-all)이 돌고 있으면 건너뛴다(같은 파일을 쓴다). 스크립트가 없는 환경(k8s)에선 아무것도 안
 * 한다.
 *
 * <p>전역 @EnableScheduling 없이 자체 스케줄러를 쓴다 — 의존 라이브러리의 @Scheduled 를 덩달아 켜지 않게.
 */
@Service
public class PoeNinjaSyncService {

  private static final Logger logger = LoggerFactory.getLogger(PoeNinjaSyncService.class);

  private final NinjaSnapshotSync sync;
  private final boolean enabled;
  private final Duration interval;
  private final String engineCron;
  private final PoeExtractService extract;
  private final PoeMetaPopularityService popularity;
  private final PoeOptimizeService optimize;
  private final PoeDataLoadStamp stamp;
  private ThreadPoolTaskScheduler scheduler;

  public PoeNinjaSyncService(
      @Value("${poe.extract-dir:tools/poe-extract}") String extractDir,
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir,
      @Value("${poe.ninja-sync.enabled:true}") boolean enabled,
      @Value("${poe.ninja-sync.interval:PT30M}") Duration interval,
      @Value("${poe.ninja-sync.engine-cron:0 0 5 * * *}") String engineCron,
      PoeExtractService extract,
      PoeMetaPopularityService popularity,
      PoeOptimizeService optimize,
      PoeDataLoadStamp stamp) {
    Path ninjaDir = Path.of(dataDir, "ninja");
    this.sync =
        new NinjaSnapshotSync(
            "poe1",
            Path.of(extractDir),
            "fetch-ninja-builds.mjs",
            ninjaDir.resolve("ninja-archetypes.json"),
            PoeNinjaSyncService::storedSnapshot,
            ninjaDir.resolve("ninja-sync-state.json"));
    this.enabled = enabled;
    this.interval = interval;
    this.engineCron = engineCron;
    this.extract = extract;
    this.popularity = popularity;
    this.optimize = optimize;
    this.stamp = stamp;
  }

  /** ninja-archetypes.json 의 snapshots = {리그: 버전}. 리그를 모르면(확인 전) 첫 항목. */
  static String storedSnapshot(JsonNode root, String league) {
    JsonNode snaps = root.path("snapshots");
    if (league != null) {
      JsonNode v = snaps.get(league);
      return v == null || v.isNull() ? null : v.asString();
    }
    for (JsonNode v : snaps) {
      return v.asString();
    }
    return null;
  }

  @PostConstruct
  void start() {
    if (!enabled || !sync.isAvailable()) {
      logger.info("PoE1 poe.ninja 스냅샷 동기 꺼짐 (enabled={}, 스크립트={})", enabled, sync.isAvailable());
      return;
    }
    scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("poe1-ninja-sync-");
    scheduler.setDaemon(true);
    scheduler.initialize();
    scheduler.scheduleWithFixedDelay(
        this::syncBuilds, java.time.Instant.now().plus(Duration.ofMinutes(2)), interval);
    scheduler.schedule(this::syncEngine, new CronTrigger(engineCron));
    logger.info("PoE1 poe.ninja 스냅샷 동기 시작 (확인 {}, 엔진 단계 '{}')", interval, engineCron);
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

  /** 버전 확인 → 바뀌었으면 수집 → 순서·시드 다시 읽기. */
  public boolean syncBuilds() {
    if (extract.isRunning()) {
      return false;
    }
    boolean updated = sync.syncIfChanged(Duration.ofMinutes(30));
    if (updated) {
      popularity.reload();
      optimize.reloadNinja();
      stamp.markReloaded();
    }
    return updated;
  }

  /** 엔진 단계 — 벤치(calibrate-archetypes) → 출발점(fetch-ninja-seeds). */
  public boolean syncEngine() {
    if (extract.isRunning()) {
      return false;
    }
    boolean ran =
        sync.runEngineStepsIfStale(
            List.of(List.of("calibrate-archetypes.mjs"), List.of("fetch-ninja-seeds.mjs")),
            Duration.ofHours(2));
    if (ran) {
      optimize.reloadNinja();
      stamp.markReloaded();
    }
    return ran;
  }
}
