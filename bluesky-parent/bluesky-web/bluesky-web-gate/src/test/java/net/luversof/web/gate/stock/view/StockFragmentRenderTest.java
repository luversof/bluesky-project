package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

/**
 * 주식 화면 조각이 "값이 하나도 없는 상태"에서 그려지는지 본다.
 *
 * <p>이 프로젝트의 주식 화면은 전부 로그인 뒤에 있어 HTTP 로는 자동 검증이 불가능했다. 선컴파일된 JTE 는 스프링 없이도 직접 렌더링할 수 있으므로(실측), 그
 * 경로로 화면 단위 회귀를 잡는다.
 *
 * <p>검사 대상은 <b>모든 {@code @param} 에 기본값이 있는 조각</b>이다. 기본값을 선언했다는 것은 "값이 없어도 그릴 수 있다"는 뜻이므로, 그리다 터지면
 * 그 자체가 결함이다. 기본값 없는 파라미터가 하나라도 있는 조각은 컨트롤러가 반드시 채워 넣는 것이라 대상에서 자동으로 빠진다 — 손으로 관리하는 예외 목록이 없어 조각이
 * 늘어나도 이 파일을 고칠 필요가 없다.
 *
 * <p>왜 필요한가: 계좌를 아직 만들지 않은 사용자에게도 이 조각들이 그대로 나간다. api-stock 이 그런 사용자에게 400 대신 빈 결과를 주도록 바뀌었으므로(실측:
 * calculateProfit 등 5 개가 400 → 200 빈 결과), 빈 상태 렌더링이 곧 신규 사용자의 첫 화면이다.
 *
 * <p>수치 아티팩트도 함께 본다. 분모가 0 인 나눗셈이 화면에 {@code NaN} 이나 {@code Infinity} 로 새는 일이 흔한데, 빈 상태가 바로 그 조건이다.
 */
class StockFragmentRenderTest {

  private static final Path TEMPLATE_ROOT = Path.of("src/main/jte");

  private static final Pattern PARAM_LINE = Pattern.compile("(?m)^@param\s+(.+)$");

  /** 값이 없을 때 화면으로 새면 안 되는 표기. */
  private static final Pattern ARTIFACT = Pattern.compile("NaN|Infinity|undefined|null%");

  private List<String> stockTemplates() throws IOException {
    List<String> names = new ArrayList<>();
    try (Stream<Path> files = Files.walk(TEMPLATE_ROOT.resolve("stock"))) {
      files
          .filter(path -> path.toString().endsWith(".jte"))
          .forEach(path -> names.add(TEMPLATE_ROOT.relativize(path).toString().replace('\\', '/')));
    }
    names.sort(String::compareTo);
    return names;
  }

  /** 모든 파라미터가 기본값을 가진 조각인지. 파라미터가 아예 없는 조각도 대상이다. */
  private boolean rendersWithoutParams(String templateName) throws IOException {
    String source = Files.readString(TEMPLATE_ROOT.resolve(templateName), StandardCharsets.UTF_8);
    Matcher matcher = PARAM_LINE.matcher(source);
    while (matcher.find()) {
      if (!matcher.group(1).contains("=")) {
        return false;
      }
    }
    return true;
  }

  @Test
  void 값이_없어도_되는_조각은_빈_상태에서_그려진다() throws IOException {
    TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);

    List<String> targets = new ArrayList<>();
    for (String name : stockTemplates()) {
      if (rendersWithoutParams(name)) {
        targets.add(name);
      }
    }
    // 파싱이 조용히 0건이 되면 검사가 무력해지므로 하한을 둔다(주식 조각 52개 중 현재 27개가 대상).
    assertThat(targets).as("검사 대상 조각").hasSizeGreaterThan(25);

    List<String> failures = new ArrayList<>();
    for (String name : targets) {
      String html;
      try {
        StringOutput output = new StringOutput();
        engine.render(name, new HashMap<String, Object>(), output);
        html = output.toString();
      } catch (Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
          cause = cause.getCause();
        }
        failures.add(name + " -> " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        continue;
      }
      Matcher artifact = ARTIFACT.matcher(html);
      if (artifact.find()) {
        int from = Math.max(0, artifact.start() - 40);
        failures.add(
            name
                + " -> 수치 아티팩트: ..."
                + html.substring(from, Math.min(html.length(), artifact.end() + 20))
                    .replaceAll("\s+", " ")
                + "...");
      }
    }

    assertThat(failures)
        .as(
            "모든 @param 에 기본값이 있는 조각은 값이 하나도 없어도 그려져야 한다. 터진다면 기본값 선언과 실제 코드가"
                + " 어긋난 것이고, 계좌를 아직 만들지 않은 사용자에게 그대로 오류로 나간다")
        .isEmpty();
  }
}
