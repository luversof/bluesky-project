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
 * 드롭다운 메뉴 상자({@code .dropdown-content})는 그 자체로 탭 정지가 되지 않는다.
 *
 * <p>실측 2026-09-10: 네비바 로케일·프로필, 상세 '다른 종목/계좌' 메뉴 상자가 {@code tabindex="0"} 이라 Tab 순회에 이름 없는 빈
 * 정지(ul/div)가 화면마다 3개 끼어 있었다. 항목(버튼/링크)이 포커스를 받으면 {@code :focus-within} 으로 열린 채 유지되므로 상자의 tabindex
 * 는 필요 없다.
 */
class DropdownContentTabStopTest {

  private static final List<Path> ROOTS =
      List.of(Path.of("src/main/jte/stock"), Path.of("src/main/jte/_components/body/navbar"));
  private static final Pattern CONTENT =
      Pattern.compile(
          "<(?:ul|div|nav)\\b[^>]*class=\"[^\"]*dropdown-content[^\"]*\"[^>]*>", Pattern.DOTALL);

  @Test
  void 메뉴_상자에는_tabindex_가_없다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int boxes = 0;
    for (Path root : ROOTS) {
      try (Stream<Path> walk = Files.walk(root)) {
        for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
          Matcher m = CONTENT.matcher(Files.readString(p, StandardCharsets.UTF_8));
          while (m.find()) {
            boxes++;
            String tag = m.group().replaceAll("\\s+", " ");
            if (tag.contains("tabindex="))
              offenders.add(
                  root.relativize(p) + ": " + tag.substring(0, Math.min(tag.length(), 90)));
          }
        }
      }
    }
    assertThat(boxes).as("메뉴 상자를 하나도 못 찾았다").isGreaterThanOrEqualTo(3);
    assertThat(offenders).as("빈 탭 정지를 만드는 메뉴 상자").isEmpty();
  }
}
