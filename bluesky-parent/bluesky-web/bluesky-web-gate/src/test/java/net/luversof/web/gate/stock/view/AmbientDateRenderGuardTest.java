package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 서버가 내려준 시각을 브라우저 로컬 타임존으로 그리지 않는지 본다.
 *
 * <p>시계열 지점의 {@code timestamp} 는 <b>요청 타임존의 자정</b>을 가리키는 instant 다(KST 2026-01-01 → {@code
 * 2025-12-31T15:00:00Z}). 이걸 타임존 인자 없이 그리면 브라우저 로컬을 따라간다 &mdash; 실측: KST 밖에서는 라벨이 하루씩 앞으로 밀리고, 연도
 * 경계에서는 2026-01-01 이 2025-12-31 로 찍혀 해가 바뀌었다.
 *
 * <p>그래서 <b>날짜/시각</b> 서식은 반드시 타임존을 명시한다. 숫자 서식({@code Number.toLocaleString()}, {@code
 * Intl.NumberFormat()})은 대상이 아니다 &mdash; 이 앱이 지원하는 ko/en 은 자릿수 구분이 같고, 차트 헬퍼는 이미 {@code
 * data-locale}(페이지가 심는 앱 로케일)을 우선한다.
 */
class AmbientDateRenderGuardTest {

  private static final List<Path> CLIENT_ROOTS =
      List.of(Path.of("src/main/frontend/src"), Path.of("src/main/jte/stock"));

  /** 인자 없는 날짜/시각 서식. 로케일과 <b>타임존</b>을 동시에 브라우저에 맡긴다. */
  private static final Pattern BARE_DATE_FORMAT =
      Pattern.compile("\\.toLocale(?:Date|Time)String\\(\\s*\\)");

  private List<String> offenders() throws IOException {
    List<String> found = new ArrayList<>();
    for (Path root : CLIENT_ROOTS) {
      if (!Files.isDirectory(root)) {
        continue;
      }
      try (Stream<Path> files = Files.walk(root)) {
        for (Path file :
            files
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".ts") || p.toString().endsWith(".jte"))
                .filter(p -> !p.toString().contains("node_modules"))
                .toList()) {
          String source = Files.readString(file, StandardCharsets.UTF_8);
          Matcher matcher = BARE_DATE_FORMAT.matcher(source);
          while (matcher.find()) {
            long line =
                source.substring(0, matcher.start()).chars().filter(c -> c == '\n').count() + 1;
            found.add(file.getFileName() + ":" + line);
          }
        }
      }
    }
    return found;
  }

  @Test
  void 날짜_서식은_타임존을_명시한다() throws IOException {
    assertThat(offenders())
        .as("인자 없이 그리면 브라우저 로컬 타임존을 따라 KST 밖에서 하루 밀린다." + " 서버가 집계에 쓴 timeZone 을 넘길 것")
        .isEmpty();
  }

  /** 검사가 실제로 파일을 훑고 있는지. 대상이 0 개면 이 가드는 아무것도 보지 않는다. */
  @Test
  void 검사_대상_파일이_존재한다() throws IOException {
    long count = 0;
    for (Path root : CLIENT_ROOTS) {
      try (Stream<Path> files = Files.walk(root)) {
        count +=
            files
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".ts") || p.toString().endsWith(".jte"))
                .filter(p -> !p.toString().contains("node_modules"))
                .count();
      }
    }
    assertThat(count).as("클라이언트 파일을 하나도 못 찾았다").isGreaterThan(20);
  }

  /**
   * 요청 타임존을 받는 화면이 날짜를 서버 존으로 찍지 않는지 본다.
   *
   * <p>위 검사가 보는 것은 <b>브라우저</b> 존이고, 이건 <b>서버</b> 존이다. 한 화면이 요청 타임존을 받아 놓고 일부만 그 존으로 그리면 같은 응답 안에서
   * 날짜가 갈린다 &mdash; 실측으로 다섯 화면이 그랬다. 활동 목록은 달력을 요청 존으로 나누면서 표의 날짜 칸은 {@code ZoneId.systemDefault()}
   * 로 찍었고, 계좌/종목 상세는 차트·필터에 {@code filterTimeZone} 을 넘기면서 매매·배당 표의 날짜만 서버 존이었으며, 매매 목록과 배당 이력은 기간
   * 표시·필터에 {@code zone} 을 넘기면서 정작 표 조각에는 넘기지 않았다.
   *
   * <p>저장된 매매/배당 시각은 전부 UTC 자정이라(실측 443건) 서버보다 서쪽 존에서는 전부 하루가 어긋난다.
   *
   * <p><b>조각까지 따라간다.</b> 처음 쓴 이 검사는 템플릿이 {@code @param String timeZone} 을 직접 선언한 경우만 봤다. 그런데 표 조각들은
   * 타임존을 선언하지 않은 채 부모가 넘겨주기만 기다리는 구조라, 부모가 존을 갖고도 안 넘긴 두 건(tradeDetailList, dividendTable)을 그대로
   * 통과시켰다. 그래서 {@code @template.} 포함 관계를 타고 올라가 조상 중 하나라도 요청 타임존을 가지면 대상으로 본다.
   *
   * <p>요청 타임존이 화면 전체에 없는 경우(recentActivities, tradeHistory, adminActions)는 비교할 대상이 없어 제외된다.
   */
  @Test
  void 요청_타임존을_받는_화면은_서버_존으로_날짜를_찍지_않는다() throws IOException {
    Map<String, String> sources = stockTemplates();
    List<String> offenders =
        sources.entrySet().stream()
            .filter(entry -> entry.getValue().contains("systemDefault"))
            .filter(entry -> hasRequestZone(entry.getKey(), sources, new HashSet<>()))
            .map(Map.Entry::getKey)
            .sorted()
            .toList();

    assertThat(offenders).as("요청 타임존이 있는 화면이다. 날짜는 그 존으로 찍고, 조각이면 부모에서 zone 을 넘겨받을 것").isEmpty();
  }

  /** 위 검사가 대상을 실제로 고르고 있는지. 대상이 0 개면 아무것도 보지 않는 셈이다. */
  @Test
  void 요청_타임존이_닿는_화면이_실제로_있다() throws IOException {
    Map<String, String> sources = stockTemplates();
    long count =
        sources.keySet().stream()
            .filter(name -> hasRequestZone(name, sources, new HashSet<>()))
            .count();
    assertThat(count).as("요청 타임존이 닿는 템플릿을 하나도 못 찾았다").isGreaterThanOrEqualTo(5);
  }

  /** 포함 관계를 타고 올라가 이 템플릿이 요청 타임존에 닿는지 본다. */
  private boolean hasRequestZone(String name, Map<String, String> sources, Set<String> seen) {
    if (!seen.add(name)) {
      return false;
    }
    String source = sources.get(name);
    if (source == null) {
      return false;
    }
    if (REQUEST_ZONE_PARAM.matcher(source).find()) {
      return true;
    }
    return sources.entrySet().stream()
        .filter(entry -> entry.getValue().contains("." + name + "("))
        .anyMatch(entry -> hasRequestZone(entry.getKey(), sources, seen));
  }

  /** 확장자를 뗀 템플릿 이름 -> 본문. */
  private Map<String, String> stockTemplates() throws IOException {
    Map<String, String> sources = new HashMap<>();
    try (Stream<Path> files = Files.walk(Path.of("src/main/jte/stock"))) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String name = file.getFileName().toString().replace(".jte", "");
        sources.put(name, Files.readString(file, StandardCharsets.UTF_8));
      }
    }
    return sources;
  }

  private static final Pattern REQUEST_ZONE_PARAM =
      Pattern.compile("@param\\s+String\\s+\\w*[tT]imeZone");
}
