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

  /**
   * 워커가 자신의 Lua 상태를 더 믿을 수 없다고 판단해 스스로 종료할 때 보내는 표식. 빌드 문제가 아니므로 새 워커로 재시도한다(실측: 같은 XML 이 새 워커에선
   * 성공, 여러 빌드를 실은 워커에선 스펙 임포트가 조용히 실패해 클래스가 사이온으로 떨어졌다).
   */
  private static final String FATAL_MARKER = "@@POB_FATAL@@";

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
          // 속성 — 장비 요구치(힘/민첩/지능) 충족 여부를 화면에서 바로 볼 수 있게 표시
          "Str",
          "Dex",
          "Int",
          "AccuracyHitChance",
          "Mana",
          "Armour",
          "Evasion",
          "TotalEHP",
          // 순생명재생 — 자가연소(정의의 화염류)에서만 계산됨(TotalBuildDegen!=0). 음수면 제 불에 타 죽는 지속불가 빌드.
          "NetLifeRegen",
          // 표시용 생명재생 — PoB 의 이름은 LifeRegenRecovery 다(BuildDisplayStats.lua 122행 label "Life
          // Regen").
          //   NetLifeRegen 은 자가연소(RF) 판정용이라 그 외 빌드에선 안 나와, 비교표가 "실빌드 989 / 내 —" 로 비어 있었다.
          "LifeRegenRecovery",
          // 흡혈은 **순재생에 안 들어간다** — PoB 는 NetLifeRegen = LifeRegenRecovery - totalLifeDegen 만
          //   계산하고(CalcDefence 3469) 흡혈은 별도 스탯이다. 그래서 재생이 음수여도 흡혈로 버티는
          //   근접 빌드는 정상인데, 이 값이 없으면 "재생 음수 = 결함"으로 오판한다(2026-08-29 조사).
          "LifeLeechGainRate",
          "LifeLeechGainPerHit",
          "ManaReservedPercent",
          "ManaUnreserved",
          "FireResist",
          "ColdResist",
          "LightningResist",
          "ChaosResist",
          // 캡 진단 — Missing=캡까지 미달분(>0 이면 총량 부족), OverCap=캡 초과 낭비분. 0 이면 toStats 가 생략.
          "MissingFireResist",
          "MissingColdResist",
          "MissingLightningResist",
          "FireResistOverCap",
          "ColdResistOverCap",
          "LightningResistOverCap",
          // 방어 레이어(현 패치 핵심) — 값 0 이면 toStats 가 생략(빌드에 없으면 미표시)
          "SpellSuppressionChance",
          "BlockChance",
          "SpellBlockChance",
          // 비교표에서 우리 값만 "—" 로 비던 항목들 — 벤치는 값을 갖고 있어 격차가 안 보였다.
          //   이름은 PoB BuildDisplayStats.lua 에서 확인(추측 아님): 181행 EffectiveMovementSpeedMod,
          //   201행 LootRarity, 145행 Ward, 169행 SpellDodgeChance.
          "EffectiveMovementSpeedMod",
          "LootRarity",
          "Ward",
          "SpellDodgeChance",
          "CritChance",
          "CritMultiplier");

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

  /**
   * 프로세스-per-eval 경로의 시간 예산. 상주 워커와 달리 매번 PoB 데이터를 다시 올리고(무궁무진 주얼 LUT 51MB 포함) 계산하므로 같은 120초를 쓰면
   * 무거운 실빌드(미니언 등)가 로드 시간에 치여 타임아웃된다(실측 3건).
   */
  private final long onceTimeoutMs;

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

  /** 워커 풀 세대 — {@link #reset()} 로 증가. 이전 세대 워커는 반납 시 폐기된다. */
  private final AtomicInteger generation = new AtomicInteger();

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
      @Value("${poe.pob.once-timeout-ms:300000}") long onceTimeoutMs,
      @Value("${poe.pob.luajit-path:}") String luajitPath) {
    this.sourceDir = Path.of(sourceDir);
    this.runnerScript = Path.of(runnerScript).toAbsolutePath();
    this.workerScript = Path.of(workerScript).toAbsolutePath();
    this.poolSize = autoPoolSize(poolSize);
    this.evalTimeoutMs = evalTimeoutMs;
    this.workerRecycleAfter = workerRecycleAfter;
    this.onceTimeoutMs = onceTimeoutMs;
    this.luajitPath = resolveLuajit(luajitPath);
  }

  /**
   * 클론된 PoB 엔진 버전(changelog.txt 첫 줄 {@code VERSION[2.66.2][날짜]}). 시뮬 계산은 이 엔진이 하므로 게임 데이터 패치와 별개로
   * "엔진이 현재 패치 트리인지" 확인용(이번 3.28→3.29 정합 이슈의 가시화). 없으면 null.
   */
  public String pobVersion() {
    try {
      String head = Files.readString(sourceDir.resolve("changelog.txt")).stripLeading();
      java.util.regex.Matcher m =
          java.util.regex.Pattern.compile("^VERSION\\[([^\\]]+)\\]").matcher(head);
      if (m.find()) {
        return m.group(1);
      }
    } catch (Exception ignored) {
      // changelog 없거나 못 읽으면 버전 미표기(엔진 미클론 등)
    }
    return null;
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
      // 값이 하나도 없는 결과는 캐시하지 않는다 — 엔진 쪽 일시 장애를 sha256 키로 박제하면
      // 재기동 전까지 같은 빌드가 계속 빈 결과를 받는다(실측: 갱신 직후 전건 stats 0).
      if (!result.stats().isEmpty()) {
        cache.put(cacheKey, result);
      }
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

  /**
   * 재계산 1건 — <b>상주 워커를 쓰지 않고</b> 프로세스-per-eval 로 돈다.
   *
   * <p>이 경로에는 남의 빌드(poe.ninja 실빌드 export, 사용자가 붙여넣은 PoB 코드)가 들어온다. 우리 최적화기가 만드는 빌드와 달리 무궁한 주얼·클러스터가
   * 잔뜩 붙어 무겁고, 이런 빌드를 여러 개 실은 워커는 PoB 내부 상태가 무너져 <b>다음 빌드의 스펙 임포트가 조용히 실패</b>한다(실측: 아키타입 18건 중 4건이
   * 새 워커 재시도 3회로도 못 넘김. 같은 XML 이 갓 띄운 워커에선 항상 성공). 어차피 회당 20~30초라 워커 재시도(≈워커 재기동 3회)보다 빠르고, 오염이
   * 최적화기 풀로 번지지도 않는다.
   */
  private EngineResult run(String buildXml) {
    long startedAt = System.currentTimeMillis();
    return new EngineResult(toStats(runRawOnce(buildXml)), System.currentTimeMillis() - startedAt);
  }

  /** 워커 풀 우선(빠름), 없으면 프로세스-per-eval 폴백. 워커 장애 시 한 번 재시도. */
  private Map<String, Double> runRaw(String buildXml) {
    if (!useWorkerPool()) {
      return runRawOnce(buildXml);
    }
    IllegalStateException last = null;
    // 3회 — 오염된 워커를 버리고 새 워커로 다시 붙는 경로라, 2회면 재시도가 또 다른 오염 워커를 잡아 실패한다(실측 4건).
    for (int attempt = 0; attempt < 3; attempt++) {
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
          release(worker);
        }
        return values;
      } catch (BuildError e) {
        // 빌드만 잘못됨 — 워커는 정상이라 반납, 예외는 상위로(최적화기가 -1 처리)
        release(worker);
        // ⚠ 환경이 깨져도 **여기로** 나오는 경우가 있다. TreeData/<버전> 이 없으면 PoB 로더가 뜨지 못해
        //    `calc.lua:69: attempt to call global 'loadBuildFromXML' (a nil value)` 같은 빌드 오류로 보인다
        //    (2026-08-12 재현). 그 문구만으로는 원인을 절대 알 수 없으므로 환경 점검을 함께 붙인다.
        //    점검은 캐시되어 평가마다 파일시스템을 두드리지 않는다.
        String buildDiag = environmentDiagnosis(buildXml);
        throw new IllegalStateException(
            "PoB 엔진 오류: "
                + e.getMessage()
                + (buildDiag.isEmpty() ? "" : " · " + buildDiag)
                + luaSyntaxHint(e.getMessage()));
      } catch (WorkerFailure e) {
        worker.kill(); // 죽은 워커는 버리고 재시도
        last = new IllegalStateException("PoB 워커 실패: " + e.getMessage(), e);
      }
    }
    if (last != null) {
      // 재시도까지 소진된 실패는 대개 **환경**(엔진 소스/타임리스 .bin/트리 버전) 문제다.
      // 증상만 던지면 다른 PC 에서 원인 추적에 시간을 쓰게 되므로 여기서 스스로 점검해 붙인다.
      String diag = environmentDiagnosis(buildXml);
      if (!diag.isEmpty()) {
        return throwWith(last, diag);
      }
    }
    throw last != null ? last : new IllegalStateException("PoB 워커 평가 실패");
  }

  private static Map<String, Double> throwWith(IllegalStateException base, String diag) {
    throw new IllegalStateException(base.getMessage() + " · " + diag, base);
  }

  /**
   * 실패가 반복될 때의 환경 점검 — "스펙 임포트 실패(클래스 N → 0)" 처럼 <b>매 요청이 같은 방식으로</b> 깨질 때 사람이 확인하던 것들을 자동으로 본다.
   * 원인을 못 찾으면 빈 문자열(=추측을 지어내지 않는다).
   *
   * <p>점검 항목은 실제로 이 증상을 낸 적이 있는 것들이다:
   *
   * <ul>
   *   <li>타임리스 주얼 .bin 이 없거나 원본(.zip)보다 오래됨 — LUT 인덱스가 어긋나 스펙 임포트가 중단된다
   *   <li>요청한 트리 버전의 PoB TreeData 디렉터리가 없음 — 로드가 기본 클래스(사이온)로 떨어진다
   * </ul>
   */
  /** 추출 파이프라인(timeless-bin.mjs)이 .bin 으로 푸는 주얼 — 이 목록 밖의 압축 파일은 검사 대상이 아니다. */
  private static final java.util.Set<String> TIMELESS_JEWELS =
      java.util.Set.of(
          "BrutalRestraint",
          "LethalPride",
          "MilitantFaith",
          "ElegantHubris",
          "HeroicTragedy",
          "GloriousVanity");

  /**
   * 점검 결과 캐시. BuildError 는 후보 평가마다 날 수 있어(최적화 한 판에 수천 번) 매번 파일시스템을 훑으면 그 자체가 부담이다. 환경은 잡 도중에 바뀌지
   * 않으므로 트리 버전별로 짧게 캐시한다.
   */
  private final Map<String, String> diagCache = new java.util.concurrent.ConcurrentHashMap<>();

  private volatile long diagCachedAt;

  private static final long DIAG_TTL_MS = 60_000;

  /**
   * Lua 파싱 실패면 조치를 붙인다.
   *
   * <p>상류 PoB 는 복합 대입(<code>x += 1</code>)을 지원하는 자체 LuaJIT 포크로 돌지만 우리는 표준 LuaJIT 을 쓴다. 상류가 그 문법을
   * 들여오면 해당 파일을 로드하는 순간 엔진이 통째로 죽는다 — 실측(2026-08-31, 타 PC): {@code PLoadModule() error loading
   * 'Modules/Main.lua': Modules/Main.lua:342: '=' expected near '+'} 로 **모든 빌드**가 실패했다. 원문만으로는 무엇을
   * 해야 하는지 알 수 없어 사용자가 원인을 못 찾았다. 조치(데이터 갱신 1회 → patch-pob 가 되돌림)를 메시지에 함께 준다.
   */
  private static String luaSyntaxHint(String message) {
    if (message == null) {
      return "";
    }
    boolean luaParseError =
        message.contains("PLoadModule() error")
            || message.contains("expected near")
            || message.contains("unexpected symbol near");
    return luaParseError
        ? " · 조치: PoB 소스에 표준 LuaJIT 이 못 읽는 문법이 있습니다(상류가 복합 대입 등을 들여온 경우)."
            + " 데이터 갱신을 한 번 실행하면 소스 갱신·패치가 다시 돌며 자동으로 되돌립니다."
        : "";
  }

  private String environmentDiagnosis(String buildXml) {
    java.util.regex.Matcher tv =
        java.util.regex.Pattern.compile("treeVersion=\"([^\"]+)\"").matcher(buildXml);
    String key = tv.find() ? tv.group(1) : "";
    long now = System.currentTimeMillis();
    if (now - diagCachedAt > DIAG_TTL_MS) {
      diagCache.clear();
      diagCachedAt = now;
    }
    return diagCache.computeIfAbsent(key, k -> environmentDiagnosisUncached(buildXml));
  }

  private String environmentDiagnosisUncached(String buildXml) {
    List<String> problems = new java.util.ArrayList<>();
    try {
      // ⚠ 데이터는 저장소 루트가 아니라 <b>src/</b> 아래에 있다(엔진 실행도 src 를 작업 디렉터리로 쓴다).
      //    루트 기준으로 찾으면 타임리스 점검은 폴더가 없어 통째로 건너뛰고, 트리 점검은 엔진이 멀쩡해도
      //    항상 "트리 데이터 없음"을 붙인다 — 진단이 없느니만 못한 거짓 안내가 된다.
      Path src = sourceDir.resolve("src");
      Path timeless = src.resolve("Data").resolve("TimelessJewelData");
      if (Files.isDirectory(timeless)) {
        // 원본은 통짜 .zip 이거나 분할(.zip.part0…)이다 — 찬란한 허영심이 분할 형태라
        // .zip 만 보면 **과거 실제로 stale 사고를 낸 주얼이 검사에서 빠진다**. 둘 다 원본으로 취급한다.
        try (java.util.stream.Stream<Path> files = Files.list(timeless)) {
          Map<String, Long> newestSource = new java.util.HashMap<>();
          for (Path f : files.toList()) {
            String name = f.getFileName().toString();
            java.util.regex.Matcher zm =
                java.util.regex.Pattern.compile("^(.+)\\.zip(?:\\.part\\d+)?$").matcher(name);
            // ⚠ 폴더의 압축 파일을 전부 요구하면 안 된다 — Abyss* 처럼 **추출 대상이 아닌 것**이 섞여 있어
            //    "AbyssAmanamu.bin 없음" 같은 거짓 진단이 쏟아진다(추출기 verify-engine.mjs 에서 실제로 겪음).
            if (!zm.matches() || !TIMELESS_JEWELS.contains(zm.group(1))) {
              continue;
            }
            long at = Files.getLastModifiedTime(f).toMillis();
            newestSource.merge(zm.group(1), at, Math::max);
          }
          for (Map.Entry<String, Long> e : newestSource.entrySet()) {
            Path bin = timeless.resolve(e.getKey() + ".bin");
            if (!Files.exists(bin)) {
              problems.add("타임리스 " + e.getKey() + ".bin 없음");
            } else if (Files.getLastModifiedTime(bin).toMillis() < e.getValue()) {
              problems.add("타임리스 " + e.getKey() + ".bin 이 원본보다 오래됨(stale)");
            }
          }
        }
      }
      java.util.regex.Matcher m =
          java.util.regex.Pattern.compile("treeVersion=\"([^\"]+)\"").matcher(buildXml);
      if (m.find() && !Files.isDirectory(src.resolve("TreeData").resolve(m.group(1)))) {
        problems.add("PoB 소스에 트리 " + m.group(1) + " 데이터 없음(엔진 갱신 필요)");
      }
    } catch (Exception e) {
      logger.debug("환경 점검 실패", e);
      return "";
    }
    if (problems.isEmpty()) {
      return "";
    }
    return "환경 점검: " + String.join(", ", problems) + " — 추출 파이프라인을 한 번 돌린 뒤 엔진을 재설정하세요";
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

      if (!process.waitFor(onceTimeoutMs, TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        outputReader.join(5000);
        throw new IllegalStateException(
            "PoB 엔진 시간 초과 (" + (onceTimeoutMs / 1000) + "초) — 프로세스 강제 종료");
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
      // ⚠ 환경이 깨지면 **이 경로로** 나온다. 상주 워커를 못 띄우면 여기(프로세스-per-eval)로 폴백하는데,
      //    TreeData/<버전> 이 없을 때가 정확히 그 경우다(2026-08-12 재현: 트리 폴더를 치우자 워커 대신
      //    이쪽으로 빠져 `calc.lua:69: attempt to call global 'loadBuildFromXML' (a nil value)` 만 남았다).
      //    그 문구로는 원인을 알 수 없으니 환경 점검을 붙인다.
      if (errorMessage != null) {
        String diag = environmentDiagnosis(buildXml);
        throw new IllegalStateException(
            "PoB 엔진 오류: "
                + errorMessage
                + (diag.isEmpty() ? "" : " · " + diag)
                + luaSyntaxHint(errorMessage));
      }
      if (resultJson == null) {
        String diag = environmentDiagnosis(buildXml);
        throw new IllegalStateException(
            "PoB 엔진 결과 누락 (exit "
                + process.exitValue()
                + ")"
                + (diag.isEmpty() ? "" : " · " + diag));
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

    /** 생성 시점의 풀 세대 — 데이터 갱신으로 세대가 바뀌면 반납 시 폐기한다. */
    final int gen = generation.get();

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
          if (line.startsWith(FATAL_MARKER)) {
            // 워커가 스스로 종료를 택한 상태 — 빌드가 아니라 워커가 망가진 것이라 새 워커로 재시도한다
            throw new WorkerFailure(line.substring(FATAL_MARKER.length()));
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

  /** 워커 반납 — 데이터 갱신으로 세대가 바뀐 워커는 낡은 게임 데이터를 물고 있으므로 폐기한다. */
  private void release(Worker worker) {
    if (worker.gen != generation.get()) {
      worker.kill();
      return;
    }
    idleWorkers.offer(worker);
  }

  /**
   * 데이터 갱신 후 엔진 상태를 비운다. 상주 워커는 기동 시 PoB 소스/트리 데이터를 메모리에 올려두므로, 갱신으로 그 파일들이 바뀌면 낡은 상태로 계산해 <b>예외 없이
   * 빈 결과</b>를 돌려준다(실제로 재계산이 stats 0 을 반환했다). 결과 캐시도 갱신 전 값이라 함께 비운다.
   */
  public void reset() {
    generation.incrementAndGet();
    synchronized (this) {
      cache.clear();
    }
    int killed = 0;
    Worker worker;
    while ((worker = idleWorkers.poll()) != null) {
      worker.kill();
      killed++;
    }
    logger.info("PoB 엔진 리셋 — 결과 캐시 비움, 유휴 워커 {}개 종료(다음 평가부터 새 워커)", killed);
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
      // 방어 레이어는 빌드에 없으면(0) 표시 생략 — 스탯시트 잡음 방지
      if (value == 0
          && ("SpellSuppressionChance".equals(key)
              || "BlockChance".equals(key)
              || "SpellBlockChance".equals(key))) {
        continue;
      }
      // 치명타 배율은 PoB 처럼 배수(x1.5) 로 표기. 비치명타 빌드(<=1)는 의미 없어 생략.
      if ("CritMultiplier".equals(key)) {
        if (value <= 1) {
          continue;
        }
        stats.add(new PoeBuild.PlayerStat(key.toLowerCase(Locale.ROOT), "x" + format(value)));
        continue;
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
