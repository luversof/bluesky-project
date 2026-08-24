package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * JSON 으로 오류를 돌려주는 자리가 클라이언트가 읽는 키를 쓰는지 본다.
 *
 * <p>브라우저 쪽 {@code handleApiError} 는 응답 본문의 <b>최상위</b> {@code isDisplayableMessage} 와 {@code
 * message} 를 읽는다. 그래야 서버가 보낸 사유가 화면에 그대로 나온다.
 *
 * <p>bluesky-boot 의 표준 오류 본문은 {@code {status, title, result:{message, displayableMessage}}} 로 키 이름도
 * 위치도 다르다(실측: api-stock 의 400 응답). 그 형태로 나가면 클라이언트는 사유를 찾지 못하고 화면 기본 문구로 덮는다 &mdash; 세션이 끊겨 401 이
 * 났는데도 "표시 순서를 저장하지 못했습니다" 만 반복해 보이게 된다.
 *
 * <p>그래서 이 컨트롤러들이 최상위 형태를 <b>직접</b> 만드는 것이 계약이다. 실측 2026-08-23: 순서 저장 엔드포인트의 401 / 400 / 500 세 갈래가
 * 모두 그렇게 돼 있다(그 셋이 전부다).
 */
class JsonErrorBodyContractTest {

  private static final Path CONTROLLER_DIR =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller");

  /** {@code Map.of(...)} 한 번의 인자 전체를 괄호 짝을 맞춰 잘라 낸다. */
  private String argumentsOf(String source, int mapAt) {
    int open = source.indexOf('(', mapAt);
    int depth = 1;
    int at = open + 1;
    while (at < source.length() && depth > 0) {
      char c = source.charAt(at++);
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
      }
    }
    return source.substring(open + 1, Math.max(open + 1, at - 1));
  }

  private record MessageBody(int offset, boolean hasDisplayableFlag) {}

  /**
   * {@code message} 를 담은 {@code Map.of(...)} 본문을 모두 찾는다.
   *
   * <p>{@code Map.of("message"} 로 찾으면 여러 줄로 쓴 본문을 놓친다 &mdash; 실제로 그렇게 세다가 4 곳으로 잘못 짚었다(진짜는 3 곳).
   * 그래서 {@code Map.of(} 마다 괄호 짝을 맞춰 인자를 통째로 읽고 그 안을 본다.
   */
  private List<MessageBody> messageBodies(String source) {
    List<MessageBody> found = new ArrayList<>();
    int at = 0;
    while (true) {
      int start = source.indexOf("Map.of(", at);
      if (start < 0) {
        return found;
      }
      String arguments = argumentsOf(source, start);
      if (arguments.contains("\"message\"")) {
        found.add(new MessageBody(start, arguments.contains("isDisplayableMessage")));
      }
      at = start + 1;
    }
  }

  private int lineOf(String source, int offset) {
    return source.substring(0, offset).split("\n", -1).length;
  }

  @Test
  void 메시지를_담은_JSON_오류는_표시_가능_표시를_함께_보낸다() throws IOException {
    List<String> offenders = new ArrayList<>();
    try (var files = Files.list(CONTROLLER_DIR)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        for (MessageBody body : messageBodies(source)) {
          if (!body.hasDisplayableFlag()) {
            offenders.add(file.getFileName() + ":" + lineOf(source, body.offset()));
          }
        }
      }
    }

    assertThat(offenders)
        .as(
            "isDisplayableMessage 가 없으면 브라우저가 사유를 찾지 못하고 화면 기본 문구로 덮는다."
                + " Map.of(\"message\", ..., \"isDisplayableMessage\", true) 형태로 보낼 것")
        .isEmpty();
  }

  /** 검사가 실제로 훑고 있는지. 하나도 못 찾으면 위 검사는 늘 통과한다. */
  @Test
  void 검사가_실제로_오류_본문을_훑는다() throws IOException {
    int found = 0;
    try (var files = Files.list(CONTROLLER_DIR)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        found += messageBodies(Files.readString(file, StandardCharsets.UTF_8)).size();
      }
    }
    // 실측 2026-08-23: 3 곳 - 순서 저장 엔드포인트의 401 / 400 / 500.
    assertThat(found).as("JSON 오류 본문을 하나도 찾지 못했다").isGreaterThanOrEqualTo(3);
  }
}
