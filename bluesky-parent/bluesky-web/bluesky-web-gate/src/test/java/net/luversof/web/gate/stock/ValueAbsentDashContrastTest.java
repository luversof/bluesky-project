package net.luversof.web.gate.stock;

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
 * "값 없음"을 뜻하는 '-' 는 뜻을 담은 표시이므로 읽혀야 한다.
 *
 * <p>2026-09-10 까지 {@code text-base-content/30} 이었는데 대비가 라이트 2.00 · 다크 2.46 으로 AA(4.5) 에 한참 못 미쳤다.
 * 실측: 매매 19곳 · 자산 현황 6곳이고 <b>25곳 모두 셀 안에 이 표시 하나뿐</b>이라, 안 보이면 빈 칸으로 읽힌다 &mdash; 그런데 amountCell 의
 * 규칙은 "0 원과 '그 해엔 아무 일도 없었다' 는 다르게 읽힌다" 이므로 빈 칸으로 보이면 그 구분이 사라진다.
 *
 * <p>{@code /60} 으로 올려 라이트 4.94 · 다크 6.00. 같은 파일의 장식용 구분자({@code ·})는 뜻이 없으므로 그대로 둔다.
 *
 * <p>axe 는 이 결함을 잡지 못했다(다크 10화면 검사 위반 0). 대비는 캔버스로 색을 해석하는 프로브로 따로 재야 한다 &mdash; Tailwind v4 계산값이
 * {@code oklab()} 이라 정규식 파서는 조용히 건너뛴다.
 */
class ValueAbsentDashContrastTest {

  private static final Path JTE_ROOT = Path.of("src/main/jte");

  private static final String FAINT_DASH = "text-base-content/30\">-<";

  private static List<Path> jteFiles() throws IOException {
    try (Stream<Path> paths = Files.walk(JTE_ROOT)) {
      List<Path> out = new ArrayList<>();
      paths
          .filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().endsWith(".jte"))
          .forEach(out::add);
      return out;
    }
  }

  @Test
  void valueAbsentDashIsReadable() throws IOException {
    List<String> offenders = new ArrayList<>();
    for (Path p : jteFiles()) {
      String path = p.toString().replace(java.io.File.separatorChar, '/');
      if (!path.contains("/stock/") && !path.contains("/ui/")) {
        continue;
      }
      String source = Files.readString(p, StandardCharsets.UTF_8);
      if (source.contains(FAINT_DASH)) {
        offenders.add(p.toString());
      }
    }

    assertThat(offenders).as("'-'(값 없음)이 /30 이면 대비 2.00 으로 빈 칸처럼 보인다 - /60 이어야 AA 를 넘는다").isEmpty();
  }

  @Test
  void theDashIsStillThere() throws IOException {
    long dashes = 0;
    for (Path p : jteFiles()) {
      String source = Files.readString(p, StandardCharsets.UTF_8);
      // 대시 칸에 까닭(title)을 달면서 class 와 ">" 사이에 속성이 들어갔다 - 클래스 바로 뒤만 세면 놓친다.
      // 세려는 것은 "그 표시가 아직 있는가" 이므로 대시 span 자체를 센다.
      String marker = ">-</span>";
      for (int at = source.indexOf(marker); at >= 0; at = source.indexOf(marker, at + 1)) {
        dashes++;
      }
    }

    assertThat(dashes)
        .as("표시 자체가 사라지면 0 원과 '그 해엔 아무 일도 없었다' 를 구분할 수 없다")
        .isGreaterThanOrEqualTo(30);
  }
}
