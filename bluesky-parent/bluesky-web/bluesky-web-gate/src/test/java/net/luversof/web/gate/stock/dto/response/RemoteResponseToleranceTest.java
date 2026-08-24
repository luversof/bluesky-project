package net.luversof.web.gate.stock.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * api-stock 이 응답에 필드를 <b>더해도</b> 게이트가 깨지지 않는지 본다.
 *
 * <p>두 저장소가 따로 배포되므로 api-stock 이 먼저 새 필드를 내보내는 상황이 정상적으로 생긴다. 실제로 이 세션에서만 두 번 있었다 &mdash; 배당 메타에서
 * 필드를 빼고(2026-08-23), 데이터 현황에 거래량 0·종가변경 행 목록을 더했다. 받는 쪽이 모르는 필드에 실패하면 그 화면이 통째로 오류가 된다.
 *
 * <p>응답 DTO 19 개 중 10 개만 {@code @JsonIgnoreProperties(ignoreUnknown = true)} 를 달고 있었다(실측
 * 2026-08-23). 어느 것이 달렸는지가 아니라 <b>전부</b> 달려 있어야 규칙이 된다.
 */
class RemoteResponseToleranceTest {

  private static final Path RESPONSE_DIR =
      Path.of("src/main/java/net/luversof/web/gate/stock/dto/response");

  @Test
  void 원격_응답_DTO는_모르는_필드를_무시한다() throws IOException {
    List<String> missing = new ArrayList<>();
    int scanned = 0;
    try (Stream<Path> files = Files.list(RESPONSE_DIR)) {
      for (Path file : files.filter(f -> f.toString().endsWith(".java")).sorted().toList()) {
        scanned++;
        String source = Files.readString(file, StandardCharsets.UTF_8);
        // import 줄에도 같은 낱말이 있으므로 애너테이션 '사용'을 봐야 한다.
        // (처음엔 낱말만 찾다가, 애너테이션만 지우는 변이를 놓쳐 검사가 공허했다.)
        if (!source.contains("@JsonIgnoreProperties(ignoreUnknown = true)")) {
          missing.add(file.getFileName().toString());
        }
      }
    }

    // 디렉터리를 못 읽어 0 건이 되면 검사가 무력해진다(현재 19 개).
    assertThat(scanned).as("응답 DTO 를 하나도 읽지 못했다").isGreaterThan(15);
    assertThat(missing)
        .as(
            "api-stock 이 필드를 더하면 이 DTO 를 쓰는 화면이 통째로 오류가 된다."
                + " @JsonIgnoreProperties(ignoreUnknown = true) 를 붙일 것")
        .isEmpty();
  }
}
