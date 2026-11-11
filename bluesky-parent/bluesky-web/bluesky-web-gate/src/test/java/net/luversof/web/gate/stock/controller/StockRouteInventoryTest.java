package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.StockControllerSources;

/**
 * 주식 화면이 여는 <b>주소 목록</b>을 고정한다.
 *
 * <p>컨트롤러를 쪼갤 때 메서드는 옮겨졌는데 <b>매핑 애너테이션이 따라가지 않는</b> 일이 실제로 있었다 &mdash; 실측 2026-09-07: 월배당 프로필 순서
 * 변경({@code PUT /stock/dividend/monthly-reference/profile/order})이 {@code @PutMapping} 을 잃고 옮겨져,
 * 컴파일도 되고 검사도 다 통과하는데 그 주소만 404 가 될 뻔했다. 여러 줄짜리 애너테이션이 잘려 나간 것이었다.
 *
 * <p>주소는 화면과 자바스크립트가 문자열로 부르는 계약이라, 코드가 어느 클래스에 있든 <b>있어야 할 주소가 다 있는지</b>만 본다.
 */
class StockRouteInventoryTest {

  /** 화면·폼·자바스크립트가 실제로 부르는 주소. 사라지면 그 기능이 통째로 죽는다. */
  private static final List<String> REQUIRED_ROUTES =
      List.of(
          "/item",
          "/account",
          "/dividend",
          "/dividend/monthly-reference/profile",
          "/dividend/monthly-reference/profile/delete",
          "/dividend/monthly-reference/profile/order",
          "/dividend/monthly-reference/payout",
          "/dividend/monthly-reference/payout/delete",
          "/dividend/monthly-reference/payout/import",
          "/dividend/monthly-reference/payout/import/source",
          "/dividend/monthly-reference/payout/import/source/bulk",
          "/trade",
          "/asset-growth",
          "/analytics",
          "/dashboard",
          "/activity",
          "/simulator",
          "/admin");

  /**
   * 매핑 애너테이션에 적힌 주소만 센다.
   *
   * <p>본문의 {@code redirect:} 문자열은 세지 않는다 &mdash; 그것까지 세면 매핑을 잃어도 리다이렉트 문자열이 남아 검사가 통과한다.
   */
  private static List<String> mappedRoutes(String source) {
    List<String> found = new java.util.ArrayList<>();
    Matcher annotation =
        Pattern.compile(
                "@(?:Get|Post|Put|Delete|Request)Mapping\\s*\\((?:[^)]*?)\"([^\"]+)\"",
                Pattern.DOTALL)
            .matcher(source);
    while (annotation.find()) {
      found.add(annotation.group(1));
    }
    return found;
  }

  @Test
  void 있어야_할_주소가_모두_매핑돼_있다() {
    List<String> mapped = mappedRoutes(StockControllerSources.all());

    assertThat(mapped).as("매핑을 하나도 찾지 못했다 - 검사가 무력해진다").hasSizeGreaterThan(10);
    assertThat(mapped)
        .as("메서드는 옮겼는데 매핑 애너테이션이 따라가지 않으면 그 주소만 조용히 404 가 된다")
        .containsAll(REQUIRED_ROUTES);
  }

  /**
   * 같은 주소가 두 곳에 매핑되면 기동할 때 충돌한다.
   *
   * <p>클래스 단위 {@code @RequestMapping} 접두어를 붙인 <b>전체 경로</b>로 본다 &mdash; 접두어를 빼고 보면 {@code
   * /stock/dashboard} 와 {@code /stock/htmx/dashboard} 가 같은 것으로 읽힌다.
   */
  @Test
  void 같은_주소를_두_번_매핑하지_않는다() {
    List<String> full = new java.util.ArrayList<>();
    StockControllerSources.byFile()
        .forEach(
            (name, source) -> {
              Matcher prefix =
                  Pattern.compile("@RequestMapping\\s*\\((?:[^)]*?)\"([^\"]+)\"", Pattern.DOTALL)
                      .matcher(source);
              String base = prefix.find() ? prefix.group(1) : "";
              Matcher method =
                  Pattern.compile(
                          "@(?:Get|Post|Put|Delete)Mapping\\s*\\((?:[^)]*?)\"([^\"]+)\"",
                          Pattern.DOTALL)
                      .matcher(source);
              while (method.find()) {
                full.add(base + method.group(1));
              }
            });

    List<String> duplicated =
        full.stream()
            .filter(route -> java.util.Collections.frequency(full, route) > 1)
            .distinct()
            .toList();

    assertThat(full).as("매핑을 하나도 찾지 못했다 - 검사가 무력해진다").hasSizeGreaterThan(10);
    assertThat(duplicated).as("같은 주소가 두 곳에 매핑돼 있다").isEmpty();
  }
}
