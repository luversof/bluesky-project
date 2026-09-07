package net.luversof.web.gate;

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
 * 스프링 컨텍스트를 띄우는 테스트가 <b>알려진 것뿐</b>인지 본다.
 *
 * <p>이 모듈에는 실행될 수 없는 테스트가 있다. {@code mvn test} 는 매번 <b>오류 9 건</b>을 내고 끝나며, 그 숫자는 "원래 그런 것" 으로 넘겨진다.
 * 실제로 이 세션에서만 컨트롤러를 쪼개다 새 실패가 여러 번 났는데, 그때마다 "기존 9 건 말고 새 것이 있나" 를 눈으로 가려내야 했다. 목록을 못박아 두면 새 것이 바로
 * 드러난다.
 *
 * <p><b>왜 실행될 수 없나</b>(실측 2026-09-07):
 *
 * <ul>
 *   <li>실패 지점은 컨텍스트 로드다 &mdash; {@code ConfigDataMissingEnvironmentPostProcessor$ImportException:
 *       spring.config.import missing configserver:}
 *   <li>이 모듈의 {@code application.properties} 는 {@code configserver:} 를 {@code localdev|opdev} 와
 *       {@code k8sdev} 프로파일 <b>안에만</b> 둔다. 테스트는 프로파일 없이 뜨므로 그 import 가 없다.
 *   <li>{@code bluesky-test} 모듈의 {@code application.properties} 에는 그 import 가 있지만, 이 모듈 자신의 것이
 *       클래스패스에서 앞서 가려 버린다.
 * </ul>
 *
 * <p><b>그러면 프로파일을 주면 되지 않나</b> &mdash; 안 된다. 이미 두 번 시도해 기록해 두었다({@code net.luversof.GeneralTest} 의
 * 주석). {@code spring.cloud.config.enabled=false} 는 {@code Cannot determine target DataSource} 로,
 * {@code localdev} 는 {@code Failed to obtain JDBC Connection} 으로 끝난다. 접속 정보 자체가 config server 에서 오기
 * 때문이다.
 *
 * <p><b>게다가 프로파일을 주는 것은 위험하다</b> &mdash; 자매 모듈에서 실제 사고가 있었다(2026-08-22, bluesky-api-stock): 프로파일을
 * 주고 통합 테스트를 돌리자 계좌 7 &rarr; 0, 거래 250 &rarr; 0, 배당 193 &rarr; 0 으로 지워졌다.
 *
 * <p>그래서 이 검사는 "돌게 만들기" 가 아니라 <b>목록이 늘어나는 것</b>을 막는다. 자매 모듈의 {@code
 * ContextDependentTestInventoryTest} 와 같은 장치다.
 */
class ContextDependentTestInventoryTest {

  private static final Path TEST_ROOT = Path.of("src/test/java/net/luversof/web/gate");

  /** 컨텍스트를 띄우는 것으로 알려진 클래스. 실측 2026-09-07: 이 다섯 개가 오류 9 건을 낸다. */
  private static final List<String> KNOWN =
      List.of(
          "BasicTest.java",
          "BlogArticleTest.java",
          "BoardArticleTest.java",
          "BoardTest.java",
          "BookkeepingClientTest.java");

  private static final String SELF = "ContextDependentTestInventoryTest.java";

  /**
   * 컨텍스트를 띄우는 표식.
   *
   * <p>문자열로만 본다. 주석에 이 말이 들어 있어도 잡힌다 &mdash; 놓치는 것보다 낫다고 보고 그대로 둔다. 잡히면 사람이 목록에 등록하거나 표현을 바꾸면 된다.
   */
  private static final List<String> CONTEXT_MARKERS =
      List.of(
          "implements GeneralTest",
          "implements GeneralWebTest",
          "@SpringBootTest",
          "@DataJdbcTest",
          "@WebMvcTest");

  private List<String> contextTests() throws IOException {
    List<String> found = new ArrayList<>();
    try (Stream<Path> files = Files.walk(TEST_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
        // 이 파일 자신은 표식 문자열을 목록으로 들고 있어 스스로를 잡는다.
        if (file.getFileName().toString().equals(SELF)) {
          continue;
        }
        String source = Files.readString(file, StandardCharsets.UTF_8);
        if (CONTEXT_MARKERS.stream().anyMatch(source::contains)) {
          found.add(file.getFileName().toString());
        }
      }
    }
    return found;
  }

  @Test
  void 컨텍스트를_띄우는_테스트는_알려진_것뿐이다() throws IOException {
    assertThat(contextTests())
        .as(
            "컨텍스트를 띄우는 테스트가 늘었다. 이 모듈에서 그런 테스트는 config server 에 닿지 못해 오류로 끝나므로,"
                + " 상시 오류가 그만큼 늘고 새 오류가 그 틈에 묻힌다. 정말 필요한지 확인하고 목록에 등록할 것")
        .containsExactlyInAnyOrderElementsOf(KNOWN);
  }

  /** 검사가 실제로 훑는지. 표식을 하나도 못 찾으면 위 검사는 빈 목록끼리 비교하게 된다. */
  @Test
  void 검사가_실제로_소스를_훑는다() throws IOException {
    assertThat(contextTests()).as("표식을 하나도 찾지 못했다 - 검사가 무력해진다").isNotEmpty();
  }
}
