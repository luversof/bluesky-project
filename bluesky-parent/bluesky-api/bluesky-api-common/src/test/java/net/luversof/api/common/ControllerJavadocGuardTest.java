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
 * bluesky-api-* 컨트롤러 Javadoc 이 API 문서 규칙을 지키는지 본다. springdoc 이 therapi 로 Javadoc 을 읽고
 * ApiCommonSpringDocAutoConfiguration 이 규칙대로 다듬는다.
 *
 * <ul>
 *   <li>엔드포인트: 첫 줄이 summary - "대상 + 동작" 짧은 이름(예: "게시판 생성", "게시판 목록 조회"), 마침표 없이 {@value
 *       #MAX_SUMMARY} 자 이하. 권한 · 오류 · 부수 효과는 그 아래 {@code <p>} 문단(description)에 쓴다.
 *   <li>컨트롤러 클래스: 첫 줄이 태그 이름(예: "게시판"), 마침표 없이. 나머지 문단이 태그 설명이다.
 * </ul>
 *
 * <p>2026-11-25: "게시판 설정을 고친다." 같은 문장이 summary 로 그대로 보여 목록이 읽기 어려웠다.
 *
 * <p>소스를 직접 읽는 이유: 컴파일된 클래스에는 Javadoc 이 없다. 이 모듈의 작업 디렉터리에서 형제 모듈을 읽는다.
 */
class ControllerJavadocGuardTest {

  static final int MAX_SUMMARY = 40;

  private static final Path API_ROOT = Path.of("..");

  private static final Pattern MAPPING =
      Pattern.compile("^\\s*@(Get|Post|Put|Delete|Patch)Mapping\\b");

  private static final Pattern METHOD_NAME = Pattern.compile("(\\w+)\\s*\\(");

  /** 규칙을 어긴 곳: "모듈 클래스[#메서드]: 사유". */
  static Set<String> violations() throws IOException {
    Set<String> found = new TreeSet<>();
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
            int controllerLine = indexOf(lines, "@RestController");
            if (controllerLine < 0) {
              continue;
            }
            String where =
                module.getFileName() + " " + file.getFileName().toString().replace(".java", "");
            check(found, where, firstLine(lines, controllerLine), "태그 이름");
            for (int i = 0; i < lines.size(); i++) {
              if (MAPPING.matcher(lines.get(i)).find()) {
                check(found, where + "#" + methodName(lines, i), firstLine(lines, i), "summary");
              }
            }
          }
        }
      }
    }
    return found;
  }

  private static void check(Set<String> found, String where, String first, String what) {
    if (first == null) {
      found.add(where + ": " + what + "가 될 Javadoc 첫 줄이 없다");
    } else if (first.endsWith(".")) {
      found.add(where + ": " + what + " 는 문장이 아니라 짧은 이름으로(마침표 없이) - " + first);
    } else if (first.length() > MAX_SUMMARY) {
      found.add(where + ": " + what + " 가 " + MAX_SUMMARY + " 자를 넘는다 - " + first);
    }
  }

  private static int indexOf(List<String> lines, String text) {
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).contains(text)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * 대상 줄 위의 다른 어노테이션 · 빈 줄을 건너뛰고 바로 위 Javadoc 의 첫 줄(태그 앞 설명). Javadoc 이 없거나 태그({@code @param})로
   * 시작하면 null.
   */
  static String firstLine(List<String> lines, int target) {
    int j = target - 1;
    while (j >= 0) {
      String s = lines.get(j).strip();
      if (s.isEmpty() || s.startsWith("@") || s.startsWith(")") || s.startsWith("\"")) {
        j--;
        continue;
      }
      if (!s.endsWith("*/")) {
        return null;
      }
      int start = j;
      while (start >= 0 && !lines.get(start).contains("/**")) {
        start--;
      }
      for (int k = Math.max(start, 0); k <= j; k++) {
        String text =
            lines
                .get(k)
                .replace("/**", "")
                .replace("*/", "")
                .strip()
                .replaceFirst("^\\*", "")
                .strip();
        if (!text.isEmpty()) {
          return text.startsWith("@") ? null : text;
        }
      }
      return null;
    }
    return null;
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
  void 컨트롤러_Javadoc_은_문서_규칙을_지킨다() throws IOException {
    assertThat(violations()).isEmpty();
  }

  @Test
  void 규칙_판정() {
    List<String> sentence = List.of("  /** 게시판을 만든다. */", "  @PostMapping");
    List<String> name =
        List.of("  /**", "   * 게시판 생성", "   *", "   * <p>설명.", "   */", "  @PostMapping");
    List<String> tagsOnly = List.of("  /**", "   * @param a 값", "   */", "  @GetMapping");

    Set<String> found = new TreeSet<>();
    check(found, "sentence", firstLine(sentence, 1), "summary");
    check(found, "name", firstLine(name, 5), "summary");
    check(found, "tagsOnly", firstLine(tagsOnly, 3), "summary");

    assertThat(found)
        .hasSize(2)
        .anyMatch(v -> v.startsWith("sentence"))
        .anyMatch(v -> v.startsWith("tagsOnly"));
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
