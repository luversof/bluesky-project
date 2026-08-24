package net.luversof.web.gate.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * 브라우저 로직 검증(node 테스트)을 메이븐 빌드 안에서 돌린다.
 *
 * <p>이 저장소의 브라우저 쪽 계산은 {@code src/main/frontend/test} 의 node 테스트가 지킨다 &mdash; 인출 시뮬레이터의 지속 개월 계산,
 * 배당 수익률 선택 합계, 차트 문구, 날짜 범위 프리셋 같은 것들이다. 그런데 그 테스트는 {@code npm run test:js} 로만 돌고, <b>메이븐은 프론트엔드를
 * 전혀 건드리지 않는다</b>(pom 에 npm/node 연동이 없다).
 *
 * <p>실측 2026-08-24: 일부러 실패하는 node 테스트를 하나 넣고 재 본 결과 &mdash;
 *
 * <pre>
 *   npm run test:js  -&gt; tests 112 · pass 111 · fail 1
 *   mvn test         -&gt; 372 · 실패 0   (기준선 그대로, 알아채지 못함)
 * </pre>
 *
 * <p>즉 브라우저 로직이 깨져도 빌드는 초록이었다. 여기서 같은 테스트를 실행해 그 결과를 빌드에 반영한다.
 *
 * <p>node 가 없는 환경에서는 <b>건너뛴다</b>({@code Assumptions}). 지금과 같아질 뿐 나빠지지 않고, 건너뛴 사실이 테스트 보고서에 남는다.
 */
class NodeTestSuiteTest {

  private static final Path FRONTEND = Path.of("src/main/frontend");

  private static final Path TEST_DIR = FRONTEND.resolve("test");

  private static final long TIMEOUT_SECONDS = 180;

  private List<String> testFiles() throws IOException {
    try (Stream<Path> files = Files.list(TEST_DIR)) {
      return files
          .filter(path -> path.getFileName().toString().endsWith(".test.mjs"))
          .sorted()
          .map(path -> TEST_DIR.relativize(path).toString())
          .map(name -> "test/" + name)
          .toList();
    }
  }

  /** node 를 실행할 수 있는지. 없으면 이 검사는 건너뛴다. */
  private boolean nodeAvailable() {
    try {
      Process process = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
      return process.waitFor(20, TimeUnit.SECONDS) && process.exitValue() == 0;
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return false;
    }
  }

  @Test
  void 브라우저_로직_테스트가_통과한다() throws IOException, InterruptedException {
    Assumptions.assumeTrue(Files.isDirectory(TEST_DIR), "프론트엔드 테스트 디렉토리가 없다");
    Assumptions.assumeTrue(nodeAvailable(), "node 를 실행할 수 없어 건너뛴다");

    List<String> files = testFiles();
    // 스캔이 조용히 0 건이 되면 node 가 아무것도 안 돌리고 성공으로 끝난다.
    assertThat(files).as("node 테스트 파일을 하나도 찾지 못했다 - 검사가 무력해진다").hasSizeGreaterThanOrEqualTo(15);

    List<String> command = new ArrayList<>(List.of("node", "--test"));
    command.addAll(files);

    Process process =
        new ProcessBuilder(command).directory(FRONTEND.toFile()).redirectErrorStream(true).start();
    String output;
    try (var stream = process.getInputStream()) {
      output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
    boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    if (!finished) {
      process.destroyForcibly();
    }
    assertThat(finished).as("node 테스트가 %d 초 안에 끝나지 않았다", TIMEOUT_SECONDS).isTrue();

    // 실패했을 때 무엇이 깨졌는지 바로 보이도록 실패한 줄만 추린다.
    List<String> failing =
        output.lines().filter(line -> line.startsWith("not ok") || line.contains("✖")).toList();
    assertThat(process.exitValue())
        .as(
            "브라우저 로직 테스트가 깨졌다 (%s):%n%s",
            String.join(", ", files.size() > 5 ? files.subList(0, 5) : files),
            failing.isEmpty() ? output : String.join("\n", failing))
        .isZero();
  }
}
