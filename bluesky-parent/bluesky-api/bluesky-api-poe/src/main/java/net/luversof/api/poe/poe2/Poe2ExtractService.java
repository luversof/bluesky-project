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

  public record Status(
      boolean running, String startedAt, String finishedAt, Integer exitCode, List<String> log) {}

  private final Path extractDir;
  private final Poe2DataService dataService;
  private final AtomicBoolean running = new AtomicBoolean();
  private final Deque<String> log = new ArrayDeque<>();
  private volatile String startedAt;
  private volatile String finishedAt;
  private volatile Integer exitCode;

  public Poe2ExtractService(
      @Value("${poe2.extract-dir:tools/poe2-extract}") String extractDir,
      Poe2DataService dataService) {
    this.extractDir = Path.of(extractDir);
    this.dataService = dataService;
  }

  public synchronized Status status() {
    return new Status(running.get(), startedAt, finishedAt, exitCode, List.copyOf(log));
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
