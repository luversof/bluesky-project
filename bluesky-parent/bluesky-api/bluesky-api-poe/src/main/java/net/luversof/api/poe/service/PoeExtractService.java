package net.luversof.api.poe.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * PoE 데이터 추출 파이프라인(tools/poe-extract/run-all.mjs)을 웹에서 실행한다.
 *
 * <p>Node 와 파이프라인 스크립트가 서버 로컬에 있어야 동작한다(로컬 개발 환경 전용 — k8s 파드에는 없으므로 버튼이 비활성 안내로 표시됨). 완료 시 데이터
 * 서비스들을 재로드해 재시작 없이 반영한다.
 */
@Service
public class PoeExtractService {

  private static final Logger logger = LoggerFactory.getLogger(PoeExtractService.class);
  private static final int MAX_LOG_LINES = 300;

  /** 최신 패치 버전 공개 소스(poe-tool-dev, 게임 업데이트마다 갱신) */
  private static final String LATEST_PATCH_URL =
      "https://raw.githubusercontent.com/poe-tool-dev/latest-patch-version/main/latest.txt";

  private static final long LATEST_TTL_MS = 10 * 60 * 1000L;
  private static final Pattern PATCH_PATTERN = Pattern.compile("\"patch\"\\s*:\\s*\"([^\"]*)\"");
  private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)+");

  public enum Status {
    IDLE,
    RUNNING,
    SUCCESS,
    FAILED
  }

  private final Path extractDir;
  private final PoeGemDataService poeGemDataService;
  private final PoeUniqueDataService poeUniqueDataService;
  private final PoeBaseItemDataService poeBaseItemDataService;
  private final PoeTreeGraphService poeTreeGraphService;
  private final PoeModPoolDataService poeModPoolDataService;
  private final PoeModDataService poeModDataService;
  private final PoeEldritchDataService poeEldritchDataService;
  private final PoeEssenceDataService poeEssenceDataService;
  private final PoeBenchDataService poeBenchDataService;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final Deque<String> logLines = new ArrayDeque<>();
  private volatile Status lastStatus = Status.IDLE;

  private volatile String cachedLatest;
  private volatile long cachedLatestAt;

  public PoeExtractService(
      @Value("${poe.extract-dir:tools/poe-extract}") String extractDir,
      PoeGemDataService poeGemDataService,
      PoeUniqueDataService poeUniqueDataService,
      PoeBaseItemDataService poeBaseItemDataService,
      PoeTreeGraphService poeTreeGraphService,
      PoeModPoolDataService poeModPoolDataService,
      PoeModDataService poeModDataService,
      PoeEldritchDataService poeEldritchDataService,
      PoeEssenceDataService poeEssenceDataService,
      PoeBenchDataService poeBenchDataService) {
    this.extractDir = Path.of(extractDir).toAbsolutePath();
    this.poeGemDataService = poeGemDataService;
    this.poeUniqueDataService = poeUniqueDataService;
    this.poeBaseItemDataService = poeBaseItemDataService;
    this.poeTreeGraphService = poeTreeGraphService;
    this.poeModPoolDataService = poeModPoolDataService;
    this.poeModDataService = poeModDataService;
    this.poeEldritchDataService = poeEldritchDataService;
    this.poeEssenceDataService = poeEssenceDataService;
    this.poeBenchDataService = poeBenchDataService;
  }

  /** 파이프라인 스크립트가 서버 로컬에 존재하는가 (k8s 파드에서는 false) */
  public boolean isAvailable() {
    return Files.exists(extractDir.resolve("run-all.mjs"));
  }

  /** 아이콘 변환(DDS→PNG)에 필요한 ImageMagick 설치 여부 — 없으면 관리 화면에 설치 안내를 띄운다 */
  public boolean isImageMagickInstalled() {
    try (var programFiles = Files.list(Path.of("C:/Program Files"))) {
      if (programFiles.anyMatch(dir -> dir.getFileName().toString().startsWith("ImageMagick"))) {
        return true;
      }
    } catch (Exception e) {
      // 비 Windows 등 — PATH 검사로 넘어간다
    }
    String pathValue =
        System.getenv().entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase("PATH"))
            .map(java.util.Map.Entry::getValue)
            .findFirst()
            .orElse("");
    for (String pathEntry : pathValue.split(java.io.File.pathSeparator)) {
      if (!pathEntry.isBlank()
          && (Files.exists(Path.of(pathEntry, "magick.exe"))
              || Files.exists(Path.of(pathEntry, "magick")))) {
        return true;
      }
    }
    return false;
  }

  /** 현재 로드된 데이터의 패치 버전(skill-gems.json 기준) */
  public String dataPatch() {
    return poeGemDataService.patch();
  }

  /** 추출 설정(config.json)에 지정된 패치 버전 — 없으면 null(파드 등 스크립트 부재 환경) */
  public String configPatch() {
    try {
      Matcher matcher = PATCH_PATTERN.matcher(Files.readString(extractDir.resolve("config.json")));
      if (matcher.find()) {
        return matcher.group(1);
      }
    } catch (Exception e) {
      logger.debug("config.json 패치 읽기 실패", e);
    }
    return null;
  }

  /** 최신 패치 버전(공개 소스) — 10분 캐시, 조회 실패 시 직전 캐시(없으면 null) */
  public String latestPatch() {
    long now = System.currentTimeMillis();
    String cached = cachedLatest;
    if (cached != null && now - cachedLatestAt < LATEST_TTL_MS) {
      return cached;
    }
    try {
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(LATEST_PATCH_URL))
              .timeout(Duration.ofSeconds(8))
              .GET()
              .build();
      HttpResponse<String> response =
          client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() == 200) {
        String version = response.body().trim();
        if (VERSION_PATTERN.matcher(version).matches()) {
          cachedLatest = version;
          cachedLatestAt = now;
          return version;
        }
      }
    } catch (Exception e) {
      logger.debug("최신 패치 버전 조회 실패", e);
    }
    return cached;
  }

  /** config.json 의 patch 필드만 교체(다른 서식/키 순서 보존). 성공 시 새 버전, 실패 시 null */
  private String writeConfigPatch(String version) {
    try {
      Path configFile = extractDir.resolve("config.json");
      String content = Files.readString(configFile);
      Matcher matcher = PATCH_PATTERN.matcher(content);
      if (!matcher.find()) {
        return null;
      }
      String updated =
          new StringBuilder(content).replace(matcher.start(1), matcher.end(1), version).toString();
      Files.writeString(configFile, updated);
      return version;
    } catch (Exception e) {
      logger.warn("config.json 패치 갱신 실패", e);
      return null;
    }
  }

  public boolean isRunning() {
    return running.get();
  }

  public Status lastStatus() {
    return lastStatus;
  }

  public List<String> logTail() {
    synchronized (logLines) {
      return new ArrayList<>(logLines);
    }
  }

  /**
   * @param toLatest true 면 실행 직전 config.json 의 patch 를 최신 버전으로 자동 교체(원클릭 갱신)
   * @return 시작했으면 true, 이미 실행 중이면 false
   */
  public boolean start(boolean toLatest) {
    if (!isAvailable() || !running.compareAndSet(false, true)) {
      return false;
    }
    synchronized (logLines) {
      logLines.clear();
    }
    lastStatus = Status.RUNNING;

    Thread worker =
        new Thread(
            () -> {
              try {
                if (toLatest) {
                  String latest = latestPatch();
                  String current = configPatch();
                  if (latest != null && !latest.equals(current)) {
                    String written = writeConfigPatch(latest);
                    appendLog(
                        written != null
                            ? "패치 버전 갱신: " + current + " → " + latest
                            : "패치 버전 자동 갱신 실패 — config.json 유지(" + current + ")");
                  } else if (latest == null) {
                    appendLog("최신 버전 조회 실패 — 현재 config.json 버전으로 진행(" + current + ")");
                  }
                }
                appendLog("파이프라인 시작: " + extractDir);
                Process process =
                    new ProcessBuilder("node", "run-all.mjs")
                        .directory(extractDir.toFile())
                        .redirectErrorStream(true)
                        .start();
                try (BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                    appendLog(line);
                  }
                }
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                  appendLog("데이터 재로드 중...");
                  poeGemDataService.reload();
                  // 베이스를 먼저 재로드해야 고유의 세부 itemClass 조인이 최신 데이터로 계산된다
                  poeBaseItemDataService.reload();
                  poeUniqueDataService.reload();
                  poeTreeGraphService.reload();
                  poeModPoolDataService.reload();
                  // 표시/탐색용 데이터도 함께 — 빠지면 원클릭 갱신 후 옛 풀이 그대로 보인다
                  poeModDataService.reload();
                  poeEldritchDataService.reload();
                  poeEssenceDataService.reload();
                  poeBenchDataService.reload();
                  appendLog("완료 — 화면을 새로고침하면 반영됩니다.");
                  lastStatus = Status.SUCCESS;
                } else {
                  appendLog("실패 (exit " + exitCode + ")");
                  lastStatus = Status.FAILED;
                }
              } catch (Exception e) {
                logger.warn("PoE 추출 파이프라인 실행 실패", e);
                appendLog("오류: " + e.getMessage());
                lastStatus = Status.FAILED;
              } finally {
                running.set(false);
              }
            },
            "poe-extract");
    worker.setDaemon(true);
    worker.start();
    return true;
  }

  private void appendLog(String line) {
    synchronized (logLines) {
      logLines.addLast(line);
      while (logLines.size() > MAX_LOG_LINES) {
        logLines.removeFirst();
      }
    }
  }
}
