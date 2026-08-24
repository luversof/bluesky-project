package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 타임존 문자열을 ZoneId 로 바꾸는 규칙을 고정한다.
 *
 * <p>이 값은 화면의 날짜를 정한다. 잘못된 문자열에 {@code ZoneId.of} 를 그대로 부르면 {@code ZoneRulesException} 이 나는데, 템플릿
 * 렌더 중이면 500 이고 컨트롤러면 공통 처리기가 본문 없는 200 으로 바꿔 htmx 가 빈 내용을 갈아끼운다 &mdash; 화면이 조용히 빈다 (실측:
 * 자산성장/배당내역/매매내역).
 *
 * <p>컨트롤러와 템플릿이 <b>같은 규칙</b>을 써야 한다는 것도 함께 고정한다. 예전에는 같은 코드가 두 벌이었는데, 이 세션에서 한 화면의 달력과 표가 서로 다른 존을
 * 쓰는 결함이 여러 번 나왔다.
 */
class StockZoneUtilTest {

  @Test
  void 비어_있으면_서버_기본_존이다() {
    assertThat(StockZoneUtil.resolve(null)).isEqualTo(ZoneId.systemDefault());
    assertThat(StockZoneUtil.resolve("")).isEqualTo(ZoneId.systemDefault());
    assertThat(StockZoneUtil.resolve("   ")).isEqualTo(ZoneId.systemDefault());
  }

  @Test
  void 정상_타임존은_그대로_쓴다() {
    assertThat(StockZoneUtil.resolve("Asia/Seoul")).isEqualTo(ZoneId.of("Asia/Seoul"));
    assertThat(StockZoneUtil.resolve("America/New_York")).isEqualTo(ZoneId.of("America/New_York"));
    assertThat(StockZoneUtil.resolve("UTC")).isEqualTo(ZoneId.of("UTC"));
  }

  /** 알 수 없는 값에서 예외가 나가면 화면이 조용히 빈다. 기본 존으로 떨어져야 한다. */
  @Test
  void 알_수_없는_값은_예외_대신_기본_존이다() {
    for (String bad : new String[] {"Mars/Olympus", "not a zone", "Asia/Seoul; DROP", "+99:00"}) {
      assertThat(StockZoneUtil.resolve(bad))
          .as(bad + " 에서 예외가 나가면 화면이 조용히 빈다")
          .isEqualTo(ZoneId.systemDefault());
    }
  }

  /**
   * 바꾸는 규칙이 한 곳에만 있는지.
   *
   * <p>컨트롤러의 {@code resolveZoneIdOrDefault} 는 이 유틸에 위임해야 한다. 두 벌이 되면 컨트롤러와 템플릿이 다른 존을 쓸 수 있다.
   */
  @Test
  void 바꾸는_규칙은_한_곳에만_있다() throws IOException {
    Path root = Path.of("src/main/java/net/luversof/web/gate/stock");
    int copies = 0;
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        copies += countOccurrences(source, "ZoneId.of(timeZone)");
      }
    }
    assertThat(copies).as("ZoneId.of(timeZone) 호출은 StockZoneUtil.resolve 에만 있어야 한다").isEqualTo(1);
  }

  /** 정규식 없이 부분 문자열 수를 센다(빌드 도구가 이스케이프를 먹는 일이 반복돼 단순하게 둔다). */
  private int countOccurrences(String source, String needle) {
    int found = 0;
    int at = source.indexOf(needle);
    while (at >= 0) {
      found++;
      at = source.indexOf(needle, at + needle.length());
    }
    return found;
  }
}
