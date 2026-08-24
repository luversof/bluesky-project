package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 요청 객체의 {@code userId} 를 세션 사용자로 덮어쓰는지 본다.
 *
 * <p>이 화면들은 {@code TradeProfitRequest} 같은 객체를 <b>URL 에서 바인딩</b>한다. 그래서 사용자가 주소창에 {@code
 * ?userId=남의id} 를 붙이면 그 값이 그대로 들어온다. 컨트롤러가 세션 사용자로 덮어쓰지 않으면 그대로 백엔드에 나가고, 백엔드는 받은 userId 를 믿는다(실측:
 * 서로 다른 userId 로 부르면 각자의 데이터가 나온다 - 인가는 게이트에만 있다).
 *
 * <p>중복 파라미터도 막지 못한다 &mdash; 실측 2026-08-23: {@code ?userId=A&userId=B} 는 <b>앞의 값</b>이 쓰인다. 즉 사용자가
 * 먼저 쓴 값이 이긴다.
 *
 * <p>지금은 모든 컨트롤러가 덮어쓴다. 값이 아니라 <b>덮어쓴다는 사실</b>을 고정한다 &mdash; 화면 하나만 빠뜨려도 그 화면이 남의 데이터를 부른다.
 */
class RequestUserScopingTest {

  private static final Path CONTROLLER_DIR =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller");

  private static final Path REQUEST_DIR =
      Path.of("src/main/java/net/luversof/web/gate/stock/dto/request");

  /** userId 를 담는 요청 DTO 이름. */
  private Set<String> userScopedRequests() throws IOException {
    Set<String> names = new LinkedHashSet<>();
    try (Stream<Path> files = Files.list(REQUEST_DIR)) {
      for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        if (source.contains("UUID userId") || source.contains("setUserId(")) {
          names.add(file.getFileName().toString().replace(".java", ""));
        }
      }
    }
    return names;
  }

  @Test
  void 사용자_범위_요청은_세션_사용자로_덮어쓴다() throws IOException {
    Set<String> requests = userScopedRequests();
    // DTO 를 못 찾으면 검사가 무력해진다(현재 4개: TradeProfitRequest / TradeSearchRequest /
    // DividendRequest / MonthlyDividendSnapshotUpsertRequest).
    assertThat(requests).as("userId 를 담는 요청 DTO 를 찾지 못했다").hasSizeGreaterThanOrEqualTo(4);

    List<String> gaps = new ArrayList<>();
    int checked = 0;
    try (Stream<Path> files = Files.list(CONTROLLER_DIR)) {
      for (Path file : files.filter(f -> f.toString().endsWith(".java")).sorted().toList()) {
        String name = file.getFileName().toString();
        String source = Files.readString(file, StandardCharsets.UTF_8);
        boolean handlesRequests = source.contains("Mapping");
        boolean usesUserScopedRequest = requests.stream().anyMatch(source::contains);
        if (!handlesRequests || !usesUserScopedRequest) {
          // 매핑이 없는 공용 상위 클래스(StockBaseHtmxController)는 이미 채워진 요청을 받아 쓴다.
          continue;
        }
        checked++;
        if (!source.contains("setUserId(userId)")) {
          gaps.add(name);
        }
      }
    }

    assertThat(checked).as("검사한 컨트롤러가 없다").isGreaterThanOrEqualTo(6);
    assertThat(gaps).as("바인딩된 userId 를 세션 사용자로 덮어쓰지 않으면 주소창으로 남의 데이터를 부를 수 있다").isEmpty();
  }
}
