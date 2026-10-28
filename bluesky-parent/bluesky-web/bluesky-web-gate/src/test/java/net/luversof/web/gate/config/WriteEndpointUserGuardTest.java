package net.luversof.web.gate.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 블로그 · 게시판 · 가계부 컨트롤러는 사용자 id 를 {@code UserUtil.requireUserId()}(없으면 401)로만 얻는다(2026-10-28 감사).
 *
 * <p>{@code @BlueskyPreAuthorize} 는 메서드 보안이 꺼져 있어 아무것도 막지 않는다(주식 쪽은 {@code
 * StockEndpointAuthGuardTest} 가 같은 사실을 지킨다). {@code getUserId()} 는 비로그인이면 null 이라, 쓰기 경로에서 쓰면 NPE
 * 500 이 나거나 null 사용자로 API 까지 갔다.
 */
class WriteEndpointUserGuardTest {

  private static final Path GATE = Path.of("src/main/java/net/luversof/web/gate");

  private static final List<Path> CONTROLLER_DIRS =
      List.of(
          GATE.resolve("blog/controller"),
          GATE.resolve("board/controller"),
          GATE.resolve("bookkeeping/controller"));

  /** null 을 직접 다루는 화면(작성자 여부 표시)과 개발 점검용. */
  private static final Set<String> ALLOWED = Set.of("BoardViewController.java");

  @Test
  void 쓰기_컨트롤러는_getUserId_대신_requireUserId_를_쓴다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int scanned = 0;
    for (Path dir : CONTROLLER_DIRS) {
      try (Stream<Path> files = Files.list(dir)) {
        for (Path file : files.filter(p -> p.toString().endsWith("Controller.java")).toList()) {
          String name = file.getFileName().toString();
          if (ALLOWED.contains(name) || name.endsWith("DevCheckController.java")) {
            continue;
          }
          scanned++;
          if (Files.readString(file, StandardCharsets.UTF_8).contains("UserUtil.getUserId()")) {
            offenders.add(name);
          }
        }
      }
    }
    assertThat(scanned).as("본 컨트롤러가 없으면 이 검사는 아무것도 지키지 않는다").isGreaterThanOrEqualTo(6);
    assertThat(offenders).as("UserUtil.requireUserId() 로 바꿀 것").isEmpty();
  }
}
