package net.luversof.api.stock;

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
 * <p>이 모듈에는 실행될 수 없는 테스트가 있다. {@code mvn test} 는 매번 <b>오류 10 건</b>을 내고 끝나며, 그 숫자는 "원래 그런 것" 으로
 * 넘겨진다. 새로 생긴 오류가 그 틈에 섞여도 알아채기 어렵다. 그래서 목록을 못박는다 &mdash; 여기 없는 클래스가 컨텍스트를 띄우려 하면 이 검사가 깨진다.
 *
 * <p><b>왜 실행될 수 없나</b>(실측 2026-08-24, 원인을 여기 적어 둔다):
 *
 * <ul>
 *   <li>실패 지점은 컨텍스트 로드다 &mdash; {@code jdbcDialect} 빈이 만들어지며 커넥션을 요구한다.
 *   <li>끝 원인은 {@code IllegalStateException: Cannot determine target DataSource for lookup key
 *       [null]} 이다. 데이터소스는 {@code RoutingDataSource} 이고 룩업 키는 {@code
 *       RoutingDataSourceContextHolder} 에서 온다.
 *   <li>그 홀더를 채우는 것은 <b>서블릿 필터</b>({@code RoutingDataSourceContextHolderFilter})와 AOP 다. 테스트는
 *       {@code spring.main.web-application-type=none} 으로 뜨므로 필터가 돌지 않고, 키는 끝까지 null 이다.
 *   <li>따라서 <b>프로필만 준다고 해결되지 않는다</b>. 실측: {@code -DargLine="-Dspring.profiles.active=localdev"} 로
 *       config server 까지 붙여도 같은 예외가 그대로 났다.
 * </ul>
 *
 * <p><b>그리고 프로필을 주는 것은 위험하다</b> &mdash; 이 클래스들은 검증이 아니라 실사용자 데이터를 건드리는 개발용 도구다. 실측 사고(2026-08-22):
 * 프로필을 주고 {@code AccountTest} 를 돌리자 계좌 7 -&gt; 0, 거래 250 -&gt; 0, 배당 193 -&gt; 0 으로 지워졌다. 그 쪽은
 * {@code DestructiveTestGuardTest} 가 {@code @Disabled} 를 강제해 막는다. 이 검사는 <b>목록이 늘어나는 것</b>을 막는다.
 */
class ContextDependentTestInventoryTest {

  private static final Path TEST_ROOT = Path.of("src/test/java/net/luversof/api/stock");

  /**
   * 컨텍스트를 띄우는 것으로 알려진 클래스. 실측 2026-08-24: 이 여섯 개가 오류 10 건을 낸다 ({@code AccountTest} 는 모든 메서드가
   * {@code @Disabled} 라 0 건).
   */
  private static final List<String> KNOWN =
      List.of(
          "AccountTest.java",
          "DividendTest.java",
          "KisApiExampleTest.java",
          "StockItemTest.java",
          "TradeProfitTest.java",
          "TradeTest.java");

  private static final String SELF = "ContextDependentTestInventoryTest.java";

  /**
   * 컨텍스트를 띄우는 표식.
   *
   * <p>문자열로만 본다. 주석에 이 말이 들어 있어도 잡힌다(실측으로 확인) &mdash; 놓치는 것보다 낫다고 보고 그대로 둔다. 잡히면 사람이 목록에 등록하거나 표현을
   * 바꾸면 된다.
   */
  private static final List<String> CONTEXT_MARKERS =
      List.of("implements GeneralTest", "@SpringBootTest", "@DataJdbcTest", "@WebMvcTest");

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
            "컨텍스트를 띄우는 테스트가 늘었다. 이 모듈에서 그런 테스트는 DB(라우팅 키)를 얻지 못해 오류로 끝나므로,"
                + " 상시 오류가 그만큼 늘고 새 오류가 그 틈에 묻힌다. 정말 필요한지 확인하고 목록에 등록할 것")
        .containsExactlyInAnyOrderElementsOf(KNOWN);
  }

  /** 검사가 실제로 훑는지. 표식을 하나도 못 찾으면 위 검사는 빈 목록끼리 비교하게 된다. */
  @Test
  void 검사가_실제로_테스트_파일을_훑는다() throws IOException {
    long scanned;
    try (Stream<Path> files = Files.walk(TEST_ROOT)) {
      scanned = files.filter(p -> p.toString().endsWith(".java")).count();
    }
    // 실측 2026-08-24: 이 패키지 아래 9 개.
    assertThat(scanned).as("테스트 파일을 하나도 찾지 못했다").isGreaterThanOrEqualTo(8);
    assertThat(contextTests()).as("컨텍스트 테스트를 하나도 찾지 못했다 - 표식이 낡았다").isNotEmpty();
  }
}
