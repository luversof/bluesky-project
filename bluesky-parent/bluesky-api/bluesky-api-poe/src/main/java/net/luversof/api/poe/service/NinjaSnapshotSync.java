package net.luversof.api.poe.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * poe.ninja 스냅샷 동기 — PoE1·PoE2 공용 실행기(빈 아님, 각 동기 서비스가 하나씩 가진다).
 *
 * <p>poe.ninja 빌드 데이터는 버전이 붙은 스냅샷(예: 2137-20260930-36820)이고 하루에도 여러 번 새 버전이 나온다. 매 요청마다 조회하는 대신
 * <b>버전만 싸게 확인</b>(수집 스크립트를 NINJA_CHECK=1 로 — 리그·스냅샷 해석을 수집과 같은 코드로)하고, 저장본의 버전과 다를 때만 전체 수집을 돌린다.
 * 버전이 곧 내용의 주소라 이 저장본은 "틀린 값을 내는 캐시"가 되지 않는다 — 신선도는 확인 주기만큼만 늦다.
 *
 * <p>엔진을 쓰는 단계(벤치·출발점 — 캐릭터 상세 레이트리밋과 엔진 재계산으로 수십 분)는 따로, 마지막으로 돌린 스냅샷을 상태 파일에 적어 두고 하루 한 번만.
 */
public final class NinjaSnapshotSync {

  private static final Logger logger = LoggerFactory.getLogger(NinjaSnapshotSync.class);
  private static final String MARKER = "@@NINJA_VERSION@@";

  /** 현재 poe.ninja 빌드 리그와 스냅샷 버전. */
  public record Version(String league, String snapshot) {}

  /** 마지막 동기 결과 — 화면·로그용. */
  public record Status(
      String name,
      boolean running,
      String checkedAt,
      String storedSnapshot,
      String currentSnapshot,
      String updatedAt,
      String engineSnapshot,
      String engineAt,
      String error) {}

  /** 저장본에서 (리그에 맞는) 스냅샷 버전을 꺼낸다 — 파일 형식이 PoE1·PoE2 가 다르다. */
  public interface StoredVersion {
    String read(JsonNode root, String league);
  }

  private final String name;
  private final Path dir;
  private final String fetchScript;
  private final Path storedFile;
  private final StoredVersion storedVersion;
  private final Path stateFile;
  private final JsonMapper json = JsonMapper.builder().build();
  private final AtomicBoolean running = new AtomicBoolean();
  private volatile String checkedAt;
  private volatile String currentSnapshot;
  private volatile String league;
  private volatile String updatedAt;
  private volatile String error;

  public NinjaSnapshotSync(
      String name,
      Path dir,
      String fetchScript,
      Path storedFile,
      StoredVersion storedVersion,
      Path stateFile) {
    this.name = name;
    this.dir = dir.toAbsolutePath();
    this.fetchScript = fetchScript;
    this.storedFile = storedFile;
    this.storedVersion = storedVersion;
    this.stateFile = stateFile;
  }

  /** 스크립트가 서버 로컬에 있는가(k8s 파드엔 없다 — 그땐 동기하지 않는다). */
  public boolean isAvailable() {
    return Files.isRegularFile(dir.resolve(fetchScript));
  }

  public boolean isRunning() {
    return running.get();
  }

  public Status status() {
    JsonNode state = readState();
    return new Status(
        name,
        running.get(),
        checkedAt,
        storedSnapshot(currentLeagueOrNull()),
        currentSnapshot,
        updatedAt,
        state == null ? null : text(state, "engineSnapshot"),
        state == null ? null : text(state, "engineAt"),
        error);
  }

  /** 버전 확인 → 저장본과 다르면 전체 수집. 새로 받았으면 true(호출자가 서비스들에 다시 읽게 한다). 이미 돌고 있거나 스크립트가 없으면 false. */
  public boolean syncIfChanged(Duration timeout) {
    if (!isAvailable() || !running.compareAndSet(false, true)) {
      return false;
    }
    try {
      Version v = checkVersion();
      checkedAt = now();
      currentSnapshot = v.snapshot();
      league = v.league();
      String stored = storedSnapshot(v.league());
      if (v.snapshot().equals(stored)) {
        error = null;
        return false;
      }
      logger.info("[{}] poe.ninja 새 스냅샷 {} (저장본 {}) — 수집 시작", name, v.snapshot(), stored);
      run(List.of(fetchScript), Map.of(), timeout);
      String after = storedSnapshot(v.league());
      if (after == null) {
        throw new IllegalStateException("수집 후 저장본에서 스냅샷을 읽지 못함: " + storedFile);
      }
      updatedAt = now();
      error = null;
      logger.info("[{}] poe.ninja 스냅샷 동기 완료: {}", name, after);
      return true;
    } catch (Exception e) {
      error = e.getMessage();
      logger.warn("[{}] poe.ninja 스냅샷 동기 실패: {}", name, e.toString());
      return false;
    } finally {
      running.set(false);
    }
  }

  /**
   * 엔진 단계(벤치·출발점) — 저장본 스냅샷이 마지막 엔진 실행 때와 다를 때만 차례로 돈다. 모두 성공하면 그 스냅샷을 상태 파일에 적고 true. 스크립트들은 이 API
   * 의 엔진 엔드포인트(https://localhost:40135)를 부른다.
   */
  public boolean runEngineStepsIfStale(List<List<String>> steps, Duration timeoutEach) {
    if (!isAvailable() || !running.compareAndSet(false, true)) {
      return false;
    }
    try {
      String stored = storedSnapshot(currentLeagueOrNull());
      if (stored == null) {
        return false;
      }
      JsonNode state = readState();
      if (state != null && stored.equals(text(state, "engineSnapshot"))) {
        return false;
      }
      for (List<String> step : steps) {
        logger.info("[{}] 엔진 단계 시작: {} (스냅샷 {})", name, step, stored);
        run(step, Map.of(), timeoutEach);
      }
      ObjectNode next = json.createObjectNode();
      next.put("engineSnapshot", stored);
      next.put("engineAt", now());
      Files.writeString(stateFile, json.writeValueAsString(next), StandardCharsets.UTF_8);
      error = null;
      logger.info("[{}] 엔진 단계 완료 (스냅샷 {})", name, stored);
      return true;
    } catch (Exception e) {
      error = e.getMessage();
      logger.warn("[{}] 엔진 단계 실패: {}", name, e.toString());
      return false;
    } finally {
      running.set(false);
    }
  }

  private Version checkVersion() throws Exception {
    List<String> out = run(List.of(fetchScript), Map.of("NINJA_CHECK", "1"), Duration.ofMinutes(2));
    for (String line : out) {
      int i = line.indexOf(MARKER);
      if (i >= 0) {
        JsonNode v = json.readTree(line.substring(i + MARKER.length()).trim());
        String league = text(v, "league");
        String snapshot = text(v, "snapshot");
        if (league != null && snapshot != null) {
          return new Version(league, snapshot);
        }
      }
    }
    throw new IllegalStateException("버전 확인 응답 없음: " + String.join(" / ", tail(out, 3)));
  }

  /** 저장본의 스냅샷 — 없거나 못 읽으면 null. league 가 null 이면 형식이 허용하는 기본값. */
  private String storedSnapshot(String league) {
    if (!Files.isRegularFile(storedFile)) {
      return null;
    }
    try {
      return storedVersion.read(
          json.readTree(Files.readString(storedFile, StandardCharsets.UTF_8)), league);
    } catch (Exception e) {
      logger.warn("[{}] 저장본 읽기 실패 {}: {}", name, storedFile, e.toString());
      return null;
    }
  }

  /** 마지막 버전 확인에서 본 리그 — 확인 전이면 null(저장본 형식의 기본값을 쓴다). */
  private String currentLeagueOrNull() {
    return league;
  }

  private JsonNode readState() {
    if (!Files.isRegularFile(stateFile)) {
      return null;
    }
    try {
      return json.readTree(Files.readString(stateFile, StandardCharsets.UTF_8));
    } catch (Exception e) {
      return null;
    }
  }

  /** node 스크립트 실행 — 출력 줄을 돌려준다. 종료 코드가 0 이 아니거나 시간 초과면 예외(마지막 몇 줄 포함). */
  private List<String> run(List<String> scriptAndArgs, Map<String, String> env, Duration timeout)
      throws Exception {
    List<String> cmd = new ArrayList<>();
    cmd.add("node");
    cmd.addAll(scriptAndArgs);
    ProcessBuilder pb = new ProcessBuilder(cmd).directory(dir.toFile()).redirectErrorStream(true);
    pb.environment().putAll(env);
    Process process = pb.start();
    process.getOutputStream().close();
    Deque<String> lines = new ArrayDeque<>();
    Thread reader =
        new Thread(
            () -> {
              try (BufferedReader r =
                  new BufferedReader(
                      new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                  synchronized (lines) {
                    lines.addLast(line);
                    if (lines.size() > 400) {
                      lines.removeFirst();
                    }
                  }
                }
              } catch (Exception ignored) {
                // 프로세스가 끝나며 스트림이 닫힌다
              }
            },
            "ninja-sync-out");
    reader.setDaemon(true);
    reader.start();
    if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
      process.destroyForcibly();
      throw new IllegalStateException(scriptAndArgs + " 시간 초과(" + timeout + ")");
    }
    reader.join(5000);
    List<String> out;
    synchronized (lines) {
      out = List.copyOf(lines);
    }
    if (process.exitValue() != 0) {
      throw new IllegalStateException(
          scriptAndArgs
              + " 실패(exit "
              + process.exitValue()
              + "): "
              + String.join(" / ", tail(out, 5)));
    }
    return out;
  }

  private static List<String> tail(List<String> lines, int n) {
    return lines.subList(Math.max(0, lines.size() - n), lines.size());
  }

  private static String text(JsonNode node, String key) {
    JsonNode v = node.get(key);
    return v == null || v.isNull() ? null : v.asString();
  }

  private static String now() {
    return OffsetDateTime.now().withNano(0).toString();
  }
}
