package net.luversof.api.poe.poe2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * PoE2 데이터 추출 — tools/poe2-extract/run-all2.mjs 를 백그라운드로 돌리고(패치 확인 → 테이블 → 파서), 끝나면 {@link
 * Poe2DataService#reload()} 로 바로 반영한다. 한 번에 하나만.
 */
@Service
public class Poe2ExtractService {

  private static final Logger logger = LoggerFactory.getLogger(Poe2ExtractService.class);
  private static final int LOG_LINES = 200;

  /**
   * 추출 상태 — PoE1 관리 화면과 같은 항목을 보이려고 실행 가능 여부 · ImageMagick · 결과(SUCCESS/FAILED/IDLE)를 함께 준다(10-12
   * C261).
   */
  public record Status(
      boolean running,
      String startedAt,
      String finishedAt,
      Integer exitCode,
      List<String> log,
      boolean available,
      boolean imageMagickInstalled,
      // PoB-PoE2 계산 엔진(LuaJIT + 소스) — PoE1 관리 화면 "PoB 엔진" 줄 짝
      boolean engineAvailable,
      String status) {}

  /**
   * 데이터 버전 — 현재 로드된 데이터 · 추출 설정 · 패치 서버의 최신(PoE1 VersionInfo 짝, 10-12 C261). 한국 IP 는 패치 서버가 카카오 CDN
   * 번호 (0.5.5.4.2)를 주므로 비교는 GGG 식으로 옮긴 번호(4.5.5.4)로 하고 카카오 번호는 따로 싣는다.
   */
  public record VersionInfo(
      String dataPatch,
      String configPatch,
      String latestPatch,
      boolean upToDate,
      String configCdnVersion,
      String latestCdnVersion) {}

  private static final java.util.regex.Pattern PATCH_PATTERN =
      java.util.regex.Pattern.compile("\"patch\"\\s*:\\s*\"([^\"]+)\"");
  private static final java.util.regex.Pattern CDN_VERSION_PATTERN =
      java.util.regex.Pattern.compile("\"cdnVersion\"\\s*:\\s*\"([^\"]+)\"");
  private static final java.util.regex.Pattern GGG_URL =
      java.util.regex.Pattern.compile("/patch/(4\\.\\d+\\.\\d+\\.\\d+)/");
  private static final java.util.regex.Pattern KAKAO_URL =
      java.util.regex.Pattern.compile(
          "kakaogames\\.com/[^/]+/patch/(0\\.\\d+\\.\\d+\\.\\d+(?:\\.\\d+)?)/");
  private static final long LATEST_TTL_MS = 10 * 60 * 1000L;
  private volatile String[] cachedLatest;
  private volatile long cachedLatestAt;

  private final Path extractDir;
  private final Poe2DataService dataService;
  private final AtomicBoolean running = new AtomicBoolean();
  private final Deque<String> log = new ArrayDeque<>();
  private volatile String startedAt;
  private volatile String finishedAt;
  private volatile Integer exitCode;

  private final net.luversof.api.poe.service.PoeExtractService poeExtractService;
  private final Poe2PobEngineService engine;

  public Poe2ExtractService(
      @Value("${poe2.extract-dir:tools/poe2-extract}") String extractDir,
      Poe2DataService dataService,
      net.luversof.api.poe.service.PoeExtractService poeExtractService,
      Poe2PobEngineService engine) {
    this.extractDir = Path.of(extractDir);
    this.dataService = dataService;
    this.poeExtractService = poeExtractService;
    this.engine = engine;
  }

  public synchronized Status status() {
    String result = exitCode == null ? "IDLE" : exitCode == 0 ? "SUCCESS" : "FAILED";
    return new Status(
        running.get(),
        startedAt,
        finishedAt,
        exitCode,
        List.copyOf(log),
        isAvailable(),
        poeExtractService.isImageMagickInstalled(),
        engine.available(),
        result);
  }

  /** 이 서버에서 추출 스크립트를 돌릴 수 있는가(PoE1 isAvailable 짝). */
  public boolean isAvailable() {
    return java.nio.file.Files.exists(extractDir.resolve("run-all2.mjs"));
  }

  /** 데이터 버전 — 현재(gems.json) · 설정(config.json) · 최신(패치 서버). */
  public VersionInfo version() {
    String data = dataService.meta().patch();
    String config = configValue(PATCH_PATTERN);
    String[] latest = latest();
    String latestPatch = latest == null ? null : latest[0];
    return new VersionInfo(
        data,
        config,
        latestPatch,
        latestPatch != null && latestPatch.equals(data),
        configValue(CDN_VERSION_PATTERN),
        latest == null ? null : latest[1]);
  }

  private String configValue(java.util.regex.Pattern pattern) {
    try {
      java.util.regex.Matcher m =
          pattern.matcher(java.nio.file.Files.readString(extractDir.resolve("config.json")));
      return m.find() ? m.group(1) : null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 패치 서버(patch.pathofexile2.com:13060)에 [1,6] 을 보내 받은 CDN 주소에서 최신 번호 — run-all2.mjs patchServer 와
   * 같은 규칙. 반환 {GGG 식 번호, 카카오 번호 또는 null}, 실패면 직전 캐시(없으면 null). 10분 캐시.
   */
  private String[] latest() {
    long now = System.currentTimeMillis();
    String[] cached = cachedLatest;
    if (cached != null && now - cachedLatestAt < LATEST_TTL_MS) {
      return cached;
    }
    try (java.net.Socket socket = new java.net.Socket()) {
      socket.connect(new java.net.InetSocketAddress("patch.pathofexile2.com", 13060), 5000);
      socket.setSoTimeout(5000);
      socket.getOutputStream().write(new byte[] {1, 6});
      socket.getOutputStream().flush();
      byte[] buf = new byte[1024];
      int total = 0;
      java.io.InputStream in = socket.getInputStream();
      // 응답은 280바이트 남짓(주소 두 번) — 끝(EOF)이나 주소 두 개가 다 올 때까지 읽는다(앞부분만 끊으면 주소가 잘린다)
      while (total < buf.length) {
        int n;
        try {
          n = in.read(buf, total, buf.length - total);
        } catch (java.net.SocketTimeoutException e) {
          break;
        }
        if (n < 0) {
          break;
        }
        total += n;
        if (total >= 200) {
          break;
        }
      }
      for (int off = 0; off <= 1; off++) {
        String text = new String(buf, off, Math.max(0, total - off), StandardCharsets.UTF_16LE);
        java.util.regex.Matcher g = GGG_URL.matcher(text);
        if (g.find()) {
          return remember(new String[] {g.group(1), null}, now);
        }
        java.util.regex.Matcher k = KAKAO_URL.matcher(text);
        if (k.find()) {
          String[] parts = k.group(1).split("\\.");
          String ggg = "4." + parts[1] + "." + parts[2] + "." + parts[3];
          return remember(new String[] {ggg, k.group(1)}, now);
        }
      }
    } catch (Exception e) {
      logger.debug("PoE2 패치 서버 조회 실패", e);
    }
    return cached;
  }

  private String[] remember(String[] value, long now) {
    cachedLatest = value;
    cachedLatestAt = now;
    return value;
  }

  public boolean isRunning() {
    return running.get();
  }

  /** 추출 시작 — 이미 돌고 있으면 false. */
  public boolean start() {
    if (!running.compareAndSet(false, true)) {
      return false;
    }
    synchronized (this) {
      log.clear();
      startedAt = OffsetDateTime.now().withNano(0).toString();
      finishedAt = null;
      exitCode = null;
    }
    Thread thread =
        new Thread(
            () -> {
              int code = -1;
              try {
                ProcessBuilder pb = new ProcessBuilder("node", "run-all2.mjs");
                pb.directory(extractDir.toFile());
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.getOutputStream().close();
                try (BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                    append(line);
                  }
                }
                code = process.waitFor();
                if (code == 0) {
                  dataService.reload();
                  append("[poe2] 데이터 다시 읽음");
                }
              } catch (Exception e) {
                logger.warn("PoE2 추출 실패", e);
                append("추출 실패: " + e.getMessage());
              } finally {
                synchronized (this) {
                  exitCode = code;
                  finishedAt = OffsetDateTime.now().withNano(0).toString();
                }
                running.set(false);
              }
            },
            "poe2-extract");
    thread.setDaemon(true);
    thread.start();
    return true;
  }

  private synchronized void append(String line) {
    log.addLast(line);
    while (log.size() > LOG_LINES) {
      log.removeFirst();
    }
  }
}
