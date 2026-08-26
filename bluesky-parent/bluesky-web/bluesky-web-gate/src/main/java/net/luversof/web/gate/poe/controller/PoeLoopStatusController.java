package net.luversof.web.gate.poe.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import net.luversof.web.gate.poe.httpexchange.PoeOptimizeClient;

/**
 * 자율 개선 루프 현황 화면 — 사람이 스크립트 위치를 외워 실행하지 않아도 진행이 보이게.
 *
 * <p>보여줄 것은 저장된 문구가 아니라 <b>지금 상태</b>다: 최적화 잡은 API 에 직접 묻고, 배터리 진행은 결과 파일 줄 수로 세며, 사이클이 남긴 한 줄은
 * {@code STATUS.md} 에서 읽는다. 화면은 htmx 로 5초마다 스스로 갱신한다.
 */
@Controller
public class PoeLoopStatusController {

  private static final DateTimeFormatter STAMP =
      DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

  private final PoeOptimizeClient poeOptimizeClient;
  private final Path qaDir;

  public PoeLoopStatusController(
      PoeOptimizeClient poeOptimizeClient,
      @Value("${poe.qa-dir:${user.home}/.bluesky-qa}") String qaDir) {
    this.poeOptimizeClient = poeOptimizeClient;
    this.qaDir = Path.of(qaDir);
  }

  /** 루프 현황 — 한 화면. */
  @GetMapping(value = "/poe/loop", produces = MediaType.TEXT_HTML_VALUE)
  public String loop(Model model) {
    fill(model);
    return "poe/loop";
  }

  /** 자동 갱신용 조각 — 같은 데이터를 패널만 다시 그린다. */
  @GetMapping(value = "/poe/htmx/loop", produces = MediaType.TEXT_HTML_VALUE)
  public String loopFragment(Model model) {
    fill(model);
    return "poe/htmx/loopPanel";
  }

  private void fill(Model model) {
    model.addAttribute("stamp", STAMP.format(Instant.now()));
    model.addAttribute("currentTask", readLines(qaDir.resolve("STATUS.md"), 4));

    // 최적화 잡 — API 가 진실. 안 뜨면 "확인 불가"로 정직하게 표시한다(빈 값으로 속이지 않는다).
    try {
      var st = poeOptimizeClient.status();
      model.addAttribute("jobRunning", st.running());
      model.addAttribute("jobStatus", st.status());
      model.addAttribute("jobPhase", st.phase());
      model.addAttribute("jobEval", st.evalCount());
      model.addAttribute("jobLog", tail(st.logLines(), 8));
      model.addAttribute("jobKnown", true);
    } catch (RuntimeException e) {
      model.addAttribute("jobKnown", false);
    }

    List<String> results = readLines(qaDir.resolve("retry-results.txt"), Integer.MAX_VALUE);
    List<String> list = readLines(qaDir.resolve("retry-list.txt"), Integer.MAX_VALUE);
    model.addAttribute("batteryDone", results.size());
    model.addAttribute("batteryTotal", list.size());
    model.addAttribute("batteryFail", results.stream().filter(l -> l.contains(" FAIL ")).count());
    model.addAttribute("batteryTail", tail(results, 6));

    List<String> backlog = readLines(qaDir.resolve("poe-backlog.md"), Integer.MAX_VALUE);
    model.addAttribute(
        "queue", backlog.stream().filter(l -> l.startsWith("- [ ]")).limit(4).toList());
    model.addAttribute("doneCount", backlog.stream().filter(l -> l.startsWith("- [x]")).count());
    model.addAttribute(
        "recentDone", backlog.stream().filter(l -> l.startsWith("- [x]")).limit(3).toList());
  }

  private List<String> readLines(Path file, int limit) {
    try {
      if (!Files.exists(file)) {
        return List.of();
      }
      List<String> all = Files.readAllLines(file, StandardCharsets.UTF_8);
      return limit >= all.size() ? all : new ArrayList<>(all.subList(0, limit));
    } catch (IOException e) {
      return List.of();
    }
  }

  private static List<String> tail(List<String> lines, int n) {
    if (lines == null || lines.isEmpty()) {
      return List.of();
    }
    return lines.subList(Math.max(0, lines.size() - n), lines.size());
  }
}
