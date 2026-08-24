package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 서버가 "오늘"을 정할 때 주변 타임존(JVM 기본값)에 기대는 자리를 붙잡아 둔다.
 *
 * <p>{@code LocalDate.now()} 와 {@code ZoneId.systemDefault()} 는 JVM 기본 타임존을 따른다. 이 앱의 날짜 경계는 전부 한국
 * 시장 기준이므로, JVM 존이 KST 가 아니면 "오늘 / 이번달 / 다음 지급일"이 통째로 최대 하루 밀린다.
 *
 * <p>실측 2026-08-23: 배포 차트가 그 값을 맞춰 준다 &mdash; {@code bluesky-web-gate} 와 {@code bluesky-api-stock}
 * 의 values.yaml 에 {@code TZ: Asia/Seoul} 이 있다(같은 subchart 의 다른 12 개 앱에는 없다). 즉 <b>지금은 맞지만, 그 정확성이
 * 다른 저장소의 환경변수 한 줄에 달려 있다</b>. 그 줄이 빠져도 앱은 조용히 뜨고 날짜만 어긋난다.
 *
 * <p>런타임 쪽은 {@code check-gate-deployment.py} 가 실제로 도는 프로세스의 {@code user.timezone} 을 확인한다. 여기서는 소스
 * 쪽을 본다 &mdash; 새 자리가 늘어나면 그때 알아채도록.
 *
 * <p>api-stock 은 {@code MARKET_ZONE_ID = Asia/Seoul} 상수를 쓰므로 이 의존이 없다. 게이트도 같은 방향으로 옮길 수 있지만, 요청
 * 타임존을 보내지 않는 비 KST 사용자의 날짜가 바뀌므로 별도 결정이 필요하다.
 */
class AmbientZoneDependencyTest {

  private static final List<Path> ROOTS =
      List.of(Path.of("src/main/java/net/luversof/web/gate/stock"), Path.of("src/main/jte/stock"));

  private static final List<String> AMBIENT =
      List.of("LocalDate.now()", "LocalDateTime.now()", "ZoneId.systemDefault()");

  /**
   * 현재 확인된 자리 수. 실측 2026-08-23 기준.
   *
   * <p>줄어드는 것은 환영이지만(존을 명시했다는 뜻) 늘어나면 알아야 한다.
   */
  private static final int KNOWN_AMBIENT_SITES = 19;

  private Map<String, Integer> countByFile() throws IOException {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (Path root : ROOTS) {
      if (!Files.isDirectory(root)) {
        continue;
      }
      try (Stream<Path> files = Files.walk(root)) {
        for (Path file :
            files
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".jte"))
                .toList()) {
          String source = Files.readString(file, StandardCharsets.UTF_8);
          int hits = 0;
          for (String token : AMBIENT) {
            for (int at = source.indexOf(token); at >= 0; at = source.indexOf(token, at + 1)) {
              hits++;
            }
          }
          if (hits > 0) {
            counts.put(file.getFileName().toString(), hits);
          }
        }
      }
    }
    return counts;
  }

  @Test
  void 주변_타임존에_기대는_자리가_늘지_않는다() throws IOException {
    Map<String, Integer> counts = countByFile();
    int total = counts.values().stream().mapToInt(Integer::intValue).sum();

    // 스캔이 조용히 0건이 되면 검사가 무력해진다.
    assertThat(counts).as("파일을 하나도 읽지 못했다").isNotEmpty();

    List<String> detail = new ArrayList<>();
    counts.forEach((file, hits) -> detail.add(file + " x" + hits));
    assertThat(total)
        .as(
            "서버가 '오늘'을 JVM 기본 타임존으로 정하는 자리가 늘었다: "
                + String.join(", ", detail)
                + ". 한국 시장 일자가 필요하면 ZoneId.of(\"Asia/Seoul\") 을 명시하거나 요청 타임존을 받을 것."
                + " 이 값이 맞는 것은 배포 차트의 TZ=Asia/Seoul 덕분이고, 그 줄이 빠지면 조용히 어긋난다")
        .isLessThanOrEqualTo(KNOWN_AMBIENT_SITES);
  }
}
