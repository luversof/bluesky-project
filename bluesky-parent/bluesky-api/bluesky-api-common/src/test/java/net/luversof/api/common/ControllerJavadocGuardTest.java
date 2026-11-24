package net.luversof.api.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * bluesky-api-* 컨트롤러의 엔드포인트는 Javadoc 이 있어야 한다 - springdoc 이 therapi 로 그 Javadoc 을 API 설명으로 쓴다
 * (2026-11-24, 설명 · 예시 규칙은 blog 의 BlogController · BlogControllerDocs).
 *
 * <p>소스를 직접 읽는 이유: 컴파일된 클래스에는 Javadoc 이 없다. 이 모듈의 작업 디렉터리에서 형제 모듈을 읽는다.
 */
class ControllerJavadocGuardTest {

  private static final Path API_ROOT = Path.of("..");

  private static final Pattern MAPPING =
      Pattern.compile("^\\s*@(Get|Post|Put|Delete|Patch)Mapping\\b");

  private static final Pattern METHOD_NAME = Pattern.compile("(\\w+)\\s*\\(");

  static Set<String> endpointsWithoutJavadoc() throws IOException {
    Set<String> missing = new TreeSet<>();
    try (Stream<Path> modules = Files.list(API_ROOT)) {
      for (Path module :
          modules.filter(p -> p.getFileName().toString().startsWith("bluesky-api-")).toList()) {
        Path src = module.resolve("src/main/java");
        if (!Files.isDirectory(src)) {
          continue;
        }
        try (Stream<Path> files = Files.walk(src)) {
          for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.stream().noneMatch(l -> l.contains("@RestController"))) {
              continue;
            }
            String className = file.getFileName().toString().replace(".java", "");
            for (int i = 0; i < lines.size(); i++) {
              if (MAPPING.matcher(lines.get(i)).find() && !hasJavadocAbove(lines, i)) {
                missing.add(module.getFileName() + " " + className + "#" + methodName(lines, i));
              }
            }
          }
        }
      }
    }
    return missing;
  }

  /** 매핑 위의 다른 어노테이션 · 빈 줄을 건너뛰고 바로 위가 Javadoc 끝({@code *}{@code /})인지 본다. */
  private static boolean hasJavadocAbove(List<String> lines, int mappingLine) {
    int j = mappingLine - 1;
    while (j >= 0) {
      String s = lines.get(j).strip();
      if (s.isEmpty() || s.startsWith("@") || s.startsWith(")") || s.startsWith("\"")) {
        j--;
        continue;
      }
      return s.endsWith("*/");
    }
    return false;
  }

  /** 매핑 아래에서 처음 나오는 메서드 이름(어노테이션 인자는 걷어 낸다). */
  private static String methodName(List<String> lines, int mappingLine) {
    StringBuilder text = new StringBuilder();
    for (int k = mappingLine + 1; k < Math.min(lines.size(), mappingLine + 8); k++) {
      String s = lines.get(k).strip();
      if (s.startsWith("@")) {
        continue;
      }
      text.append(' ').append(s);
      Matcher m = METHOD_NAME.matcher(text);
      if (m.find()) {
        return m.group(1);
      }
    }
    return "?";
  }

  @Test
  void 엔드포인트는_Javadoc_이_있어야_한다() throws IOException {
    assertThat(endpointsWithoutJavadoc()).as("엔드포인트에 Javadoc 을 쓸 것(API 설명이 된다)").isEmpty();
  }

  @Test
  void 형제_모듈의_컨트롤러를_실제로_읽는다() throws IOException {
    try (Stream<Path> files = Files.walk(API_ROOT.resolve("bluesky-api-blog/src/main/java"))) {
      assertThat(files.filter(p -> p.toString().endsWith("Controller.java")).count())
          .as("blog 컨트롤러가 보이지 않으면 작업 디렉터리가 바뀐 것")
          .isPositive();
    }
  }
}
