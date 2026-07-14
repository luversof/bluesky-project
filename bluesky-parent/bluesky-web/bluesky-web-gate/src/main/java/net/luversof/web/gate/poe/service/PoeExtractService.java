package net.luversof.web.gate.poe.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final Deque<String> logLines = new ArrayDeque<>();
  private volatile Status lastStatus = Status.IDLE;

  public PoeExtractService(
      @Value("${poe.extract-dir:tools/poe-extract}") String extractDir,
      PoeGemDataService poeGemDataService,
      PoeUniqueDataService poeUniqueDataService,
      PoeBaseItemDataService poeBaseItemDataService) {
    this.extractDir = Path.of(extractDir).toAbsolutePath();
    this.poeGemDataService = poeGemDataService;
    this.poeUniqueDataService = poeUniqueDataService;
    this.poeBaseItemDataService = poeBaseItemDataService;
  }

  /** 파이프라인 스크립트가 서버 로컬에 존재하는가 (k8s 파드에서는 false) */
  public boolean isAvailable() {
    return Files.exists(extractDir.resolve("run-all.mjs"));
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
   * @return 시작했으면 true, 이미 실행 중이면 false
   */
  public boolean start() {
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
                  poeUniqueDataService.reload();
                  poeBaseItemDataService.reload();
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
