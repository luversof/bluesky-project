package net.luversof.web.gate.stock.view;

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
 * 여러 칸을 덮는 머리글은 {@code scope="colgroup"} 이다.
 *
 * <p>{@code scope="col"} 은 자기 칸 하나만 가리킨다. 그래서 {@code colspan} 이 붙은 묶음 머리글에 col 을 쓰면 낭독기가 그 이름을 첫
 * 열에만 붙이고 나머지 열은 묶음 없이 읽는다.
 *
 * <p>실측 2026-09-12(10 화면 · 표 31 개): 이 경우가 시뮬레이터 월배당 표에만 있었고 셋이었다 &mdash; 저장된 기준값 · 입력값 · 수익률 (각
 * {@code colspan="3"}).
 */
class ColumnGroupScopeTest {

  @Test
  void colspan_머리글은_colgroup_을_쓴다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int colgroups = 0;
    try (Stream<Path> files = Files.walk(Path.of("src/main/jte"))) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        int at = source.indexOf("<th ");
        while (at >= 0) {
          int end = source.indexOf(">", at);
          if (end < 0) {
            break;
          }
          String tag = source.substring(at, end);
          boolean spans =
              tag.contains("colspan=") && !tag.contains("colspan=" + (char) 34 + "1" + (char) 34);
          if (spans && tag.contains("scope=" + (char) 34 + "col" + (char) 34)) {
            offenders.add(
                file.getFileName()
                    + ": "
                    + tag.replaceAll("\s+", " ").substring(0, Math.min(70, tag.length())));
          }
          if (tag.contains("scope=" + (char) 34 + "colgroup" + (char) 34)) {
            colgroups++;
          }
          at = source.indexOf("<th ", end);
        }
      }
    }
    assertThat(colgroups).as("2026-09-12 기준 시뮬레이터 월배당 표의 묶음 머리글 셋").isGreaterThanOrEqualTo(3);
    assertThat(offenders).as("여러 칸을 덮는데 scope=col 인 머리글").isEmpty();
  }
}
