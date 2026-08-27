package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 같은 조각을 <b>두 경로로</b> 그리는 화면에서, 페이지 쪽 호출이 조각의 파라미터를 빠뜨리지 않는지 본다.
 *
 * <p>조각 하나가 컨트롤러의 뷰로도 반환되고 페이지 템플릿에서 {@code @template.…(…)} 로도 불릴 때, <b>배선이 두 벌</b>이 된다. 컨트롤러 쪽은
 * 모델을 통째로 넘기므로 값을 늘려도 저절로 따라가지만, 페이지 쪽은 파라미터를 하나씩 적기 때문에 <b>새로 늘린 값을 적는 것을 잊으면</b> JTE 가 조용히 기본값(대개
 * null)으로 채운다.
 *
 * <p>실제로 그랬다 &mdash; 2026-08-27: 기간 요약에 손익률·고점·저점을 넣고 {@code /asset-growth/period-return} 조각 경로만
 * 배선했다. 그 경로는 테스트가 모두 통과했는데, 화면이 실제로 쓰는 것은 {@code asset-growth.jte} 의 호출이었다. 새 값 다섯 개만 "계산 불가" 로
 * 나갔고, <b>api-stock 이 값을 못 낸 것과 화면에서 구분되지 않아</b> 원인을 백엔드에서 찾게 됐다.
 *
 * <p>모든 조각에 이 규칙을 걸면 소음이 크다 &mdash; 공용 UI 부품({@code statCard} · {@code card} · {@code emptyState})은
 * 꾸밈용 선택 파라미터를 생략하는 것이 정상이다. 그래서 <b>컨트롤러가 뷰로도 반환하는</b> 조각, 즉 배선이 두 벌인 것만 본다.
 */
class FragmentParameterCoverageTest {

  private static final Path JTE_ROOT = Path.of("src/main/jte");
  private static final Path JAVA_ROOT = Path.of("src/main/java");

  private static final Pattern PARAM =
      Pattern.compile("^@param\\s+(?:.+?\\s)?(\\w+)\\s*(?:=|$)", Pattern.MULTILINE);
  private static final Pattern CALL = Pattern.compile("@template\\.([\\w.]+)\\s*\\(");
  private static final Pattern NAMED_ARG = Pattern.compile("(\\w+)\\s*=");
  private static final Pattern VIEW_NAME = Pattern.compile("\"((?:[\\w-]+/)+[\\w-]+)\"");

  /**
   * 아직 남겨 둔 자리(꾸밈용 선택 파라미터). <b>늘어나면 실패</b>한다.
   *
   * <p>PoE 의 {@code modClass} 는 표시 토글이라 생략해도 화면 뜻이 바뀌지 않는다. 값을 나르는 조각이 아니므로 지금은 그대로 둔다.
   */
  private static final Set<String> ALLOWED = Set.of("poe/htmx/modClass");

  private record Gap(Path caller, String target, List<String> missing) {}

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private List<Path> filesUnder(Path root, String suffix) throws IOException {
    try (Stream<Path> walk = Files.walk(root)) {
      return walk.filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().endsWith(suffix))
          .toList();
    }
  }

  /** 컨트롤러가 뷰 이름 문자열로 직접 반환하는 조각. */
  private Set<String> controllerRenderedViews() throws IOException {
    Set<String> views = new LinkedHashSet<>();
    for (Path java : filesUnder(JAVA_ROOT, ".java")) {
      Matcher matcher = VIEW_NAME.matcher(read(java));
      while (matcher.find()) {
        String candidate = matcher.group(1);
        if (Files.exists(JTE_ROOT.resolve(candidate + ".jte"))) {
          views.add(candidate);
        }
      }
    }
    return views;
  }

  /** 여는 괄호부터 짝이 맞는 닫는 괄호까지. */
  private String argumentList(String source, int openParenIndex) {
    int depth = 0;
    for (int i = openParenIndex; i < source.length(); i++) {
      char c = source.charAt(i);
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
        if (depth == 0) {
          return source.substring(openParenIndex, i + 1);
        }
      }
    }
    return "";
  }

  @Test
  void 두_경로로_그리는_조각은_호출부가_파라미터를_다_넘긴다() throws IOException {
    Set<String> dualWired = controllerRenderedViews();
    assertThat(dualWired)
        .as("컨트롤러가 반환하는 조각을 하나도 찾지 못했다 - 검사가 무력해진다")
        .contains("stock/htmx/fragments/assetGrowthPeriodReturnSummary");

    List<Gap> gaps = new ArrayList<>();
    int checked = 0;
    for (Path caller : filesUnder(JTE_ROOT, ".jte")) {
      String source = read(caller);
      Matcher call = CALL.matcher(source);
      while (call.find()) {
        String target = call.group(1).replace('.', '/');
        if (!dualWired.contains(target) || ALLOWED.contains(target)) {
          continue;
        }
        String args = argumentList(source, call.end() - 1);
        Set<String> passed = new LinkedHashSet<>();
        Matcher named = NAMED_ARG.matcher(args);
        while (named.find()) {
          passed.add(named.group(1));
        }
        if (passed.isEmpty()) {
          continue;
        }
        checked++;
        List<String> missing = new ArrayList<>();
        Matcher param = PARAM.matcher(read(JTE_ROOT.resolve(target + ".jte")));
        while (param.find()) {
          if (!passed.contains(param.group(1))) {
            missing.add(param.group(1));
          }
        }
        if (!missing.isEmpty()) {
          gaps.add(new Gap(caller, target, missing));
        }
      }
    }

    assertThat(checked).as("검사 대상 호출을 하나도 찾지 못했다").isPositive();
    assertThat(gaps)
        .as("페이지가 조각에 넘기지 않은 파라미터가 있다. 화면에는 '계산 불가'(또는 빈 값)로 나가고, 백엔드가 값을 못 낸 것과 구분되지 않는다")
        .isEmpty();
  }
}
