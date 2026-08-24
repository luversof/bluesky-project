package net.luversof.api.stock.service;

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
 * 같은 값을 담은 상수가 한 클래스 안에 두 번 선언되지 않는지 본다.
 *
 * <p>왜 필요한가(실측 2026-08-23): {@link LedgerIntegrityService} 에 원천징수 세율 {@code 0.154} 가 {@code
 * WITHHOLDING_RATE} 와 {@code DIVIDEND_WITHHOLDING_RATE} 두 이름으로 있었고, 세 곳에서 나뉘어 쓰이고 있었다. 세법이 바뀌면 한쪽만
 * 고쳐지고 나머지는 조용히 옛 값을 쓴다 &mdash; 어느 화면이 틀렸는지는 숫자만 봐서는 알 수 없다.
 *
 * <p>같은 부류를 이 저장소에서 두 번 만났다. 같은 최신-종가 SQL 이 두 벌 있었고 그중 하나가 죽은 사본이었다({@code
 * ClosePriceQueryVolumeGuardTest} 참고).
 */
class DuplicateConstantGuardTest {

  private static final Path SERVICE_DIR = Path.of("src/main/java/net/luversof/api/stock/service");

  private record Constant(String name, String value) {}

  /** {@code private static final <타입> 이름 = 리터럴;} 형태만 본다. */
  private List<Constant> constantsOf(String source) {
    List<Constant> constants = new ArrayList<>();
    String marker = "static final ";
    for (int at = source.indexOf(marker); at >= 0; at = source.indexOf(marker, at + 1)) {
      int end = source.indexOf(';', at);
      if (end < 0) {
        break;
      }
      String line = source.substring(at + marker.length(), end);
      int assign = line.indexOf('=');
      if (assign < 0) {
        continue;
      }
      String left = line.substring(0, assign).trim();
      String right = line.substring(assign + 1).trim().replaceAll("\s+", " ");
      int space = left.lastIndexOf(' ');
      if (space < 0) {
        continue;
      }
      constants.add(new Constant(left.substring(space + 1), right));
    }
    return constants;
  }

  @Test
  void 같은_값의_상수가_한_클래스에_두_번_선언되지_않는다() throws IOException {
    Map<String, List<String>> duplicates = new LinkedHashMap<>();
    int scanned = 0;
    try (Stream<Path> files = Files.walk(SERVICE_DIR)) {
      for (Path file :
          files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        Map<String, List<String>> byValue = new LinkedHashMap<>();
        for (Constant constant : constantsOf(source)) {
          scanned++;
          // 문자열/불리언 상수는 같은 값이 여러 이름으로 있는 것이 자연스럽다(코드명, 라벨 등).
          if (constant.value().startsWith("\"") || constant.value().matches("(true|false)")) {
            continue;
          }
          byValue.computeIfAbsent(constant.value(), key -> new ArrayList<>()).add(constant.name());
        }
        byValue.forEach(
            (value, names) -> {
              if (names.size() > 1) {
                duplicates.put(file.getFileName() + " " + value, names);
              }
            });
      }
    }

    // 파서가 조용히 0건이 되면 검사가 무력해진다.
    // 실측 2026-08-23 기준 service 패키지의 static final 선언은 19 개다. 파서가 조용히 0 건이 되는 것만 막으면 되므로
    // 하한은 넉넉히 둔다.
    assertThat(scanned).as("상수를 하나도 찾지 못했다").isGreaterThanOrEqualTo(15);
    assertThat(duplicates).as("같은 값의 상수가 한 클래스에 여러 이름으로 있다. 값이 바뀌면 한쪽만 고쳐진다").isEmpty();
  }
}
