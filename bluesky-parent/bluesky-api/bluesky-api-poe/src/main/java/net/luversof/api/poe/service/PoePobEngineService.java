package net.luversof.api.poe.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import tools.jackson.databind.json.JsonMapper;

/**
 * Path of Building 커뮤니티 계산 엔진을 헤드리스(LuaJIT)로 구동해 빌드 스탯을 실제 재계산한다.
 *
 * <p>실행 = {@code luajit tools/poe-pob/calc.lua <build.xml>} (cwd 는 PoB 소스의 src). PoB 소스는 파생 산출물이라
 * git 밖({@code ~/.poe-gamedata/work/pob-src})에 클론해 둔다. 러너는 계산 결과를 {@code @@POB_RESULT@@{json}} 한 줄로
 * 출력한다. 회당 약 1.5초라 코드 sha-256 키 LRU 캐시를 둔다.
 */
@Service
public class PoePobEngineService {

  private static final Logger logger = LoggerFactory.getLogger(PoePobEngineService.class);

  private static final String RESULT_MARKER = "@@POB_RESULT@@";
  private static final String ERROR_MARKER = "@@POB_ERROR@@";
  private static final int CACHE_SIZE = 64;

  /** 표시 순서. uiMessage 의 poe.build.stat.<소문자 키> 와 짝을 이룬다. */
  private static final List<String> STAT_KEYS =
      List.of(
          "CombinedDPS",
          "TotalDPS",
          "FullDPS",
          "AverageDamage",
          "Speed",
          "Life",
          "EnergyShield",
          "Mana",
          "Armour",
          "Evasion",
          "TotalEHP",
          "ManaReservedPercent",
          "ManaUnreserved",
          "FireResist",
          "ColdResist",
          "LightningResist",
          "ChaosResist",
          "CritChance");

  public record EngineResult(List<PoeBuild.PlayerStat> stats, long durationMs) {}

  private static final String READY_MARKER = "@@POB_READY@@";
  private static final String REQUEST_END = "@@END@@";

  private final Path sourceDir;
  private final Path runnerScript;
  private final Path workerScript;
  private final String luajitPath;
  private final int poolSize;
  private final long evalTimeoutMs;
  private final int workerRecycleAfter;

  private final Map<String, EngineResult> cache =
      new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, EngineResult> eldest) {
          return size() > CACHE_SIZE;
        }
      };

  // 상주 워커 풀 — HeadlessWrapper 를 한 번만 로드해두고 재사용(프로세스/데이터 로드 반복 제거로 크게 빨라짐)
  private final BlockingQueue<Worker> idleWorkers = new LinkedBlockingQueue<>();
  private final AtomicInteger workerCount = new AtomicInteger();
  private final ScheduledExecutorService watchdog =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "pob-worker-watchdog");
            t.setDaemon(true);
            return t;
          });
  private volatile boolean shuttingDown;

  public PoePobEngineService(
      @Value("${poe.pob.src-dir:${user.home}/.poe-gamedata/work/pob-src}") String sourceDir,
      @Value("${poe.pob.runner:tools/poe-pob/calc.lua}") String runnerScript,
      @Value("${poe.pob.worker:tools/poe-pob/worker.lua}") String workerScript,
      @Value("${poe.pob.workers:0}") int poolSize,
      @Value("${poe.pob.eval-timeout-ms:120000}") long evalTimeoutMs,
      @Value("${poe.pob.worker-recycle-after:400}") int workerRecycleAfter,
      @Value("${poe.pob.luajit-path:}") String luajitPath) {
    this.sourceDir = Path.of(sourceDir);
    this.runnerScript = Path.of(runnerScript).toAbsolutePath();
    this.workerScript = Path.of(workerScript).toAbsolutePath();
    this.poolSize = autoPoolSize(poolSize);
    this.evalTimeoutMs = evalTimeoutMs;
    this.workerRecycleAfter = workerRecycleAfter;
    this.luajitPath = resolveLuajit(luajitPath);
  }

  /** 상주 luajit 워커 상한 — 워커당 ~1.6GB 라 코어 + RAM 에서 자동 산정(다른 PC 이식성). 최소 예약: OS/JVM 용 코어. */
  private static final int WORKER_CAP = 12; // 수확체감 + TREE_ROUND_CANDIDATES 정렬 상한

  private static final int WORKER_RESERVE_CORES = 4; // OS/JVM/메인스레드 여유
  private static final double WORKER_RAM_BUDGET_GB = 3.0; // 워커당 RAM 예산(~1.6GB 실사용 + 여유)

  /** 설정값(>0)이 있으면 그대로, 없으면(≤0) 감지한 코어/총RAM 기준 자동 산정. */
  static int autoPoolSize(int configured) {
    if (configured > 0) {
      return configured;
    }
    int cores = Runtime.getRuntime().availableProcessors();
    int byCores = Math.max(2, cores - WORKER_RESERVE_CORES);
    long totalGb = totalPhysicalMemoryGb();
    int byRam = (int) Math.max(2, Math.floor(totalGb / WORKER_RAM_BUDGET_GB));
    int chosen = Math.min(WORKER_CAP, Math.min(byCores, byRam));
    logger.info(
        "PoE 워커 풀 자동 산정: {} (코어 {} → {}, 총RAM {}GB → {}, 상한 {})",
        chosen,
        cores,
        byCores,
        totalGb,
        byRam,
        WORKER_CAP);
    return chosen;
  }

  /** 총 물리 RAM(GB). 조회 불가 시 보수적으로 8. */
  private static long totalPhysicalMemoryGb() {
    try {
      if (java.lang.management.ManagementFactory.getOperatingSystemMXBean()
          instanceof com.sun.management.OperatingSystemMXBean os) {
        return os.getTotalMemorySize() / (1024L * 1024 * 1024);
      }
    } catch (Throwable ignore) {
      // com.sun 미지원 JVM 등 — 기본값 사용
    }
    return 8;
  }

  /** 현재 워커 풀 크기(= 최적화기 executor 병렬성 기준값) */
  public int poolSize() {
    return poolSize;
  }

  private boolean useWorkerPool() {
    return Files.exists(workerScript);
  }

  private String resolveLuajit(String configured) {
    if (configured != null && !configured.isBlank()) {
      return configured;
    }
    Path wingetInstall =
        Path.of(
            System.getProperty("user.home"),
            "AppData",
            "Local",
            "Programs",
            "LuaJIT",
            "bin",
            "luajit.exe");
    return Files.exists(wingetInstall) ? wingetInstall.toString() : "luajit";
  }

  /** 이 서버에서 엔진 실행이 가능한지 (PoB 소스 + 러너 존재 여부) */
  public boolean isAvailable() {
    return Files.exists(sourceDir.resolve("src").resolve("HeadlessWrapper.lua"))
        && Files.exists(runnerScript);
  }

  /**
   * 빌드 XML 을 PoB 엔진으로 재계산한다. 같은 XML 은 캐시에서 즉시 반환.
   *
   * @throws IllegalStateException 엔진 실행 실패/시간 초과/결과 누락
   */
  public EngineResult recalculate(String buildXml) {
    String cacheKey = sha256(buildXml);
    synchronized (this) {
      EngineResult cached = cache.get(cacheKey);
      if (cached != null) {
        return cached;
      }
      EngineResult result = run(buildXml);
      cache.put(cacheKey, result);
      return result;
    }
  }

  /**
   * 배치 평가용 — 캐시/락 없이 계산해 숫자 스탯 맵을 그대로 돌려준다. luajit 프로세스는 서로 독립이라 여러 스레드에서 병렬 호출해도 된다.
   *
   * @throws IllegalStateException 엔진 실행 실패/시간 초과/결과 누락
   */
  public Map<String, Double> calculateValues(String buildXml) {
    return runRaw(buildXml);
  }

  private EngineResult run(String buildXml) {
    long startedAt = System.currentTimeMillis();
    return new EngineResult(toStats(runRaw(buildXml)), System.currentTimeMillis() - startedAt);
  }

  /** 워커 풀 우선(빠름), 없으면 프로세스-per-eval 폴백. 워커 장애 시 한 번 재시도. */
  private Map<String, Double> runRaw(String buildXml) {
    if (!useWorkerPool()) {
      return runRawOnce(buildXml);
    }
    IllegalStateException last = null;
    for (int attempt = 0; attempt < 2; attempt++) {
      Worker worker = acquireWorker();
      if (worker == null) {
        // 워커를 만들 수 없으면 폴백
        return runRawOnce(buildXml);
      }
      try {
        Map<String, Double> values = worker.eval(buildXml);
        // 정상 처리 → 반납(누적 사용 많으면 재활용해 Lua 메모리 증가 방지)
        if (worker.uses.incrementAndGet() >= workerRecycleAfter) {
          worker.kill();
        } else {
          idleWorkers.offer(worker);
        }
        return values;
      } catch (BuildError e) {
        // 빌드만 잘못됨 — 워커는 정상이라 반납, 예외는 상위로(최적화기가 -1 처리)
        idleWorkers.offer(worker);
        throw new IllegalStateException("PoB 엔진 오류: " + e.getMessage());
      } catch (WorkerFailure e) {
        worker.kill(); // 죽은 워커는 버리고 재시도
        last = new IllegalStateException("PoB 워커 실패: " + e.getMessage(), e);
      }
    }
    throw last != null ? last : new IllegalStateException("PoB 워커 평가 실패");
  }

  private Map<String, Double> runRawOnce(String buildXml) {
    Path xmlFile = null;
    try {
      xmlFile = Files.createTempFile("pob-build", ".xml");
      Files.writeString(xmlFile, buildXml, StandardCharsets.UTF_8);

      ProcessBuilder processBuilder =
          new ProcessBuilder(luajitPath, runnerScript.toString(), xmlFile.toString());
      processBuilder.directory(sourceDir.resolve("src").toFile());
      processBuilder.redirectErrorStream(true);
      Process process = processBuilder.start();
      // PoB 는 시작 오류 시 io.read 로 stdin 을 기다린다 — 먼저 닫아 프롬프트 대기(무한 멈춤)를 차단
      process.getOutputStream().close();

      // stdout 은 별도 스레드로 읽고, 본 스레드는 시간 제한을 강제한다 (멈춘 프로세스는 강제 종료).
      // readLine 을 본 스레드에서 하면 멈춘 프로세스에 무한 블록되어 배치/잡 전체가 멈춘다.
      List<String> outputLines =
          java.util.Collections.synchronizedList(new java.util.ArrayList<>());
      Thread outputReader =
          new Thread(
              () -> {
                try (BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                    outputLines.add(line);
                  }
                } catch (Exception ignored) {
                  // 강제 종료 시 스트림 예외는 무시
                }
              },
              "pob-engine-output");
      outputReader.setDaemon(true);
      outputReader.start();

      if (!process.waitFor(120, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        outputReader.join(5000);
        throw new IllegalStateException("PoB 엔진 시간 초과 (120초) — 프로세스 강제 종료");
      }
      outputReader.join(10000);

      String resultJson = null;
      String errorMessage = null;
      for (String line : List.copyOf(outputLines)) {
        if (line.startsWith(RESULT_MARKER)) {
          resultJson = line.substring(RESULT_MARKER.length());
        } else if (line.startsWith(ERROR_MARKER)) {
          errorMessage = line.substring(ERROR_MARKER.length());
        } else {
          logger.debug("PoB engine: {}", line);
        }
      }
      if (errorMessage != null) {
        throw new IllegalStateException("PoB 엔진 오류: " + errorMessage);
      }
      if (resultJson == null) {
        throw new IllegalStateException("PoB 엔진 결과 누락 (exit " + process.exitValue() + ")");
      }
      return parseValues(resultJson);
    } catch (IllegalStateException e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("PoB 엔진 실행 중단", e);
    } catch (Exception e) {
      throw new IllegalStateException("PoB 엔진 실행 실패", e);
    } finally {
      if (xmlFile != null) {
        try {
          Files.deleteIfExists(xmlFile);
        } catch (Exception ignored) {
          // 임시 파일 삭제 실패는 무시
        }
      }
    }
  }

  // ── 상주 워커 풀 ─────────────────────────────────────────

  /** 빌드 자체가 잘못됨(워커는 정상) — 최적화기는 이걸 -1 로 처리해 자연 탈락 */
  private static final class BuildError extends Exception {
    BuildError(String message) {
      super(message);
    }
  }

  /** 워커 프로세스가 죽음/멈춤(재시도 대상) */
  private static final class WorkerFailure extends Exception {
    WorkerFailure(String message) {
      super(message);
    }

    WorkerFailure(String message, Throwable cause) {
      super(message, cause);
    }
  }

  private final class Worker {
    final Process process;
    final BufferedWriter stdin;
    final BufferedReader stdout;
    final AtomicInteger uses = new AtomicInteger();
    volatile boolean timedOut;

    Worker(Process process, BufferedWriter stdin, BufferedReader stdout) {
      this.process = process;
      this.stdin = stdin;
      this.stdout = stdout;
    }

    Map<String, Double> eval(String buildXml) throws BuildError, WorkerFailure {
      timedOut = false;
      ScheduledFuture<?> killTask =
          watchdog.schedule(
              () -> {
                timedOut = true;
                process.destroyForcibly();
              },
              evalTimeoutMs,
              TimeUnit.MILLISECONDS);
      try {
        stdin.write(buildXml);
        stdin.write("\n");
        stdin.write(REQUEST_END);
        stdin.write("\n");
        stdin.flush();
        String line;
        while ((line = stdout.readLine()) != null) {
          if (line.startsWith(RESULT_MARKER)) {
            return parseValues(line.substring(RESULT_MARKER.length()));
          }
          if (line.startsWith(ERROR_MARKER)) {
            throw new BuildError(line.substring(ERROR_MARKER.length()));
          }
          logger.debug("PoB worker: {}", line);
        }
        throw new WorkerFailure(timedOut ? "시간 초과" : "스트림 종료(EOF)");
      } catch (BuildError | WorkerFailure e) {
        throw e;
      } catch (Exception e) {
        throw new WorkerFailure("IO 오류: " + e.getMessage(), e);
      } finally {
        killTask.cancel(false);
      }
    }

    void kill() {
      workerCount.decrementAndGet();
      try {
        process.destroyForcibly();
      } catch (Exception ignored) {
        // 무시
      }
    }
  }

  /** 유휴 워커를 얻거나, 풀 미달이면 새로 만든다. 풀이 꽉 차면 반납될 때까지 대기. 실패하면 null. */
  private Worker acquireWorker() {
    Worker worker = idleWorkers.poll();
    if (worker != null) {
      return worker;
    }
    if (!shuttingDown && workerCount.get() < poolSize) {
      synchronized (this) {
        if (!shuttingDown && workerCount.get() < poolSize) {
          Worker created = startWorker();
          if (created != null) {
            workerCount.incrementAndGet();
            return created;
          }
        }
      }
    }
    try {
      return idleWorkers.poll(evalTimeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  private Worker startWorker() {
    Process process = null;
    try {
      ProcessBuilder pb = new ProcessBuilder(luajitPath, workerScript.toString());
      pb.directory(sourceDir.resolve("src").toFile());
      pb.redirectErrorStream(true);
      process = pb.start();
      Process handle = process;
      BufferedWriter in =
          new BufferedWriter(
              new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
      BufferedReader out =
          new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
      // HeadlessWrapper 로드 대기 (READY 마커) — 최대 60초, 그 뒤 강제 종료
      ScheduledFuture<?> killTask =
          watchdog.schedule(handle::destroyForcibly, 60, TimeUnit.SECONDS);
      try {
        String line;
        while ((line = out.readLine()) != null) {
          if (line.startsWith(READY_MARKER)) {
            return new Worker(handle, in, out);
          }
          logger.debug("PoB worker(start): {}", line);
        }
      } finally {
        killTask.cancel(false);
      }
      logger.warn("PoB 워커 기동 실패 (READY 없음)");
      process.destroyForcibly();
      return null;
    } catch (Exception e) {
      logger.warn("PoB 워커 기동 오류", e);
      if (process != null) {
        process.destroyForcibly();
      }
      return null;
    }
  }

  @PreDestroy
  public void shutdownWorkers() {
    shuttingDown = true;
    watchdog.shutdownNow();
    Worker worker;
    while ((worker = idleWorkers.poll()) != null) {
      try {
        worker.process.destroyForcibly();
      } catch (Exception ignored) {
        // 무시
      }
    }
  }

  private Map<String, Double> parseValues(String json) {
    JsonMapper jsonMapper = JsonMapper.builder().build();
    @SuppressWarnings("unchecked")
    Map<String, Object> raw = jsonMapper.readValue(json, Map.class);
    Map<String, Double> values = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : raw.entrySet()) {
      if (entry.getValue() instanceof Number number) {
        values.put(entry.getKey(), number.doubleValue());
      }
    }
    return values;
  }

  /** 숫자 스탯 맵 → 표시용 PlayerStat 목록 (STAT_KEYS 순서/포맷) — 최적화 결과 표시에도 재사용 */
  public List<PoeBuild.PlayerStat> formatStats(Map<String, Double> values) {
    return toStats(values);
  }

  private List<PoeBuild.PlayerStat> toStats(Map<String, Double> values) {
    List<PoeBuild.PlayerStat> stats = new ArrayList<>();
    for (String key : STAT_KEYS) {
      Double value = values.get(key);
      if (value == null) {
        continue;
      }
      if ("FullDPS".equals(key) && value == 0) {
        continue; // 풀 DPS 미설정 빌드는 0 이라 표시하지 않는다
      }
      stats.add(new PoeBuild.PlayerStat(key.toLowerCase(Locale.ROOT), format(value)));
    }
    return stats;
  }

  private String format(double value) {
    if (Math.abs(value) >= 100) {
      return String.format(Locale.ROOT, "%,.0f", value);
    }
    return String.format(Locale.ROOT, "%.1f", value).replaceAll("\\.0$", "");
  }

  private String sha256(String value) {
    try {
      var digest = java.security.MessageDigest.getInstance("SHA-256");
      return java.util.HexFormat.of()
          .formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
