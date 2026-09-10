package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 주식 화면의 알림 상자({@code .alert})는 보조기술에 전달되는 역할을 가진다 - 오류·경고는 {@code role="alert"}, 성공·안내는 {@code
 * role="status"}.
 *
 * <p>실측 2026-09-10(소스 스캔): .alert 12개 중 role 이 있는 것은 오류 화면 3개뿐이었다. POST 뒤 결과 배너(성공/오류), 시뮬레이터 안내,
 * 관리 경고가 role 없이 그려져 화면 읽기 프로그램은 결과가 나온 줄 몰랐다.
 */
class AlertRoleTest {

  private static final Path JTE = Path.of("src/main/jte/stock");
  private static final Pattern ALERT =
      Pattern.compile("<div\\b[^>]*class=\"[^\"]*\\balert\\b[^\"]*\"[^>]*>", Pattern.DOTALL);

  @Test
  void 모든_알림_상자는_alert_또는_status_역할을_가진다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int alerts = 0;
    try (Stream<Path> walk = Files.walk(JTE)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        Matcher m = ALERT.matcher(Files.readString(p, StandardCharsets.UTF_8));
        while (m.find()) {
          alerts++;
          String tag = m.group().replaceAll("\\s+", " ");
          boolean ok = tag.contains("role=\"alert\"") || tag.contains("role=\"status\"");
          boolean errorish = tag.contains("alert-error") || tag.contains("alert-warning");
          if (!ok)
            offenders.add(
                JTE.relativize(p) + ": role 없음: " + tag.substring(0, Math.min(tag.length(), 90)));
          else if (errorish && !tag.contains("role=\"alert\""))
            offenders.add(
                JTE.relativize(p)
                    + ": 오류/경고인데 alert 가 아님: "
                    + tag.substring(0, Math.min(tag.length(), 90)));
        }
      }
    }
    assertThat(alerts).as("알림 상자를 하나도 못 찾았다").isGreaterThanOrEqualTo(12);
    assertThat(offenders).as("보조기술에 전달되지 않는 알림 상자").isEmpty();
  }
}
