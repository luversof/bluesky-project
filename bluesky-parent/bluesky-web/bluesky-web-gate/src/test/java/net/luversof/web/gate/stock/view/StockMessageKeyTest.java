package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 주식 화면이 쓰는 메시지 키가 두 로케일에 모두 실제 문구로 존재하는지 본다.
 *
 * <p>왜 필요한가: {@code MessageUtil.getMessage(code)} 는 키를 못 찾으면 예외를 던지지 않고 <b>빈 문자열</b>을 돌려준다(실측: 없는 키
 * 조회 결과가 {@code []}). 그래서 키를 빠뜨리면 그 자리에 라벨이 그냥 사라진 채 화면이 정상처럼 그려진다 — 로그도, 오류 화면도 없다. 주식 화면은 로그인 뒤에
 * 있어 이 증상이 눈에 띄기까지 오래 걸린다.
 *
 * <p>현재 상태(실측): 주식 화면이 쓰는 키 605 개, 공용 조각 21 개 모두 {@code uiMessage.properties} 와 {@code
 * uiMessage_ko.properties} 에 비어 있지 않은 값으로 존재하고, en/ko 값이 같은(번역이 빠진) 키도 없다.
 */
class StockMessageKeyTest {

  private static final Path RESOURCES = Path.of("src/main/resources");

  private static final List<Path> SOURCE_ROOTS =
      List.of(
          Path.of("src/main/jte/stock"),
          Path.of("src/main/java/net/luversof/web/gate/stock"),
          Path.of("src/main/jte/_components"),
          Path.of("src/main/jte/_layout"));

  /**
   * 메시지 키를 꺼내는 두 가지 표기를 모두 본다.
   *
   * <p>{@code getMessage("...")} 만 보던 시절에는 컨트롤러가 쓰는 {@code msg("...")} 헬퍼가 검사 밖에 있었다(실측: 그 표기로만 쓰이는
   * 키가 9 개). 그 키에 오타가 나면 화면에 키 문자열이 그대로 나가는데 아무도 잡지 못한다.
   */
  private static final Pattern MESSAGE_CALL =
      Pattern.compile("(?:getMessage|(?<![\\w.])msg)\\(\\s*\"([^\"]+)\"");

  private Map<String, String> properties(String fileName) throws IOException {
    Map<String, String> values = new LinkedHashMap<>();
    for (String raw :
        Files.readString(RESOURCES.resolve(fileName), StandardCharsets.UTF_8).split("\n")) {
      String line = raw.strip();
      if (line.isEmpty() || line.startsWith("#") || line.startsWith("!") || !line.contains("=")) {
        continue;
      }
      int split = line.indexOf('=');
      values.put(line.substring(0, split).strip(), line.substring(split + 1).strip());
    }
    return values;
  }

  private TreeSet<String> usedKeys() throws IOException {
    TreeSet<String> keys = new TreeSet<>();
    for (Path root : SOURCE_ROOTS) {
      if (!Files.isDirectory(root)) {
        continue;
      }
      try (Stream<Path> files = Files.walk(root)) {
        for (Path file :
            files
                .filter(
                    path -> path.toString().endsWith(".jte") || path.toString().endsWith(".java"))
                .toList()) {
          Matcher matcher = MESSAGE_CALL.matcher(Files.readString(file, StandardCharsets.UTF_8));
          while (matcher.find()) {
            String key = matcher.group(1);
            // 접두사에 값을 이어 붙여 쓰는 형태는 그 자체로는 키가 아니다.
            if (!key.endsWith(".")) {
              keys.add(key);
            }
          }
        }
      }
    }
    return keys;
  }

  @Test
  void 쓰는_메시지_키는_두_로케일에_문구가_있다() throws IOException {
    Map<String, String> english = properties("uiMessage.properties");
    Map<String, String> korean = properties("uiMessage_ko.properties");
    TreeSet<String> used = usedKeys();

    // 스캔이 조용히 0건이 되면 검사가 무력해지므로 하한을 둔다(현재 600여 개).
    assertThat(used).as("주식·공용 화면이 쓰는 메시지 키").hasSizeGreaterThan(300);

    List<String> problems = new ArrayList<>();
    for (String key : used) {
      String en = english.get(key);
      String ko = korean.get(key);
      if (en == null) {
        problems.add(key + " -> uiMessage.properties 에 없음");
      } else if (en.isBlank()) {
        problems.add(key + " -> uiMessage.properties 값이 비어 있음");
      }
      if (ko == null) {
        problems.add(key + " -> uiMessage_ko.properties 에 없음");
      } else if (ko.isBlank()) {
        problems.add(key + " -> uiMessage_ko.properties 값이 비어 있음");
      }
    }

    assertThat(problems).as("빠진 키는 화면에서 라벨이 사라질 뿐 오류가 나지 않으므로 빌드에서 잡는다").isEmpty();
  }

  /** 접두어에 변수를 붙여 만드는 키. 리터럴 스캔에 잡히지 않으므로 따로 본다. */
  private static final String LEDGER_RULE_PREFIX = "stock.admin.ledger.rule.";

  /**
   * 조립되는 메시지 키는 위 검사에 잡히지 않는다.
   *
   * <p>{@code adminActions.jte} 는 원장 점검 결과를 {@code getMessage("stock.admin.ledger.rule." +
   * finding.code())} 로 그린다. 코드가 서버에서 오므로 <b>리터럴 키가 소스에 없다</b> &mdash; 키를 빠뜨려도 위 검사는 통과하고, 화면에는 규칙
   * 이름 자리에 빈 칸이 남는다(MessageUtil 은 없는 키에 빈 문자열을 돌려준다).
   *
   * <p>규칙의 출처는 api-stock 의 {@code LedgerIntegrityService} 다. 게이트는 별도 저장소라 그 소스를 읽지 않는다(단독 빌드가 깨진다).
   * 대신 두 번들의 키 집합이 같고 값이 비어 있지 않은지, 그리고 개수가 줄지 않았는지를 본다 &mdash; 실측 2026-08-23 기준 규칙 24 개가 모두 양쪽에
   * 있다. 규칙을 추가하면 여기 하한도 함께 올린다.
   *
   * <p>하한은 실제 규칙 수와 맞춰 둔다. 예전에는 13 이었는데 그 사이 규칙이 24 개로 늘어, 규칙 11 개가 통째로 사라져도 통과하는 상태였다.
   */
  @Test
  void 원장_점검_규칙_이름은_두_로케일에_모두_있다() throws IOException {
    Map<String, String> english = properties("uiMessage.properties");
    Map<String, String> korean = properties("uiMessage_ko.properties");

    TreeSet<String> englishRules = new TreeSet<>();
    TreeSet<String> koreanRules = new TreeSet<>();
    english.keySet().stream()
        .filter(k -> k.startsWith(LEDGER_RULE_PREFIX))
        .forEach(englishRules::add);
    korean.keySet().stream()
        .filter(k -> k.startsWith(LEDGER_RULE_PREFIX))
        .forEach(koreanRules::add);

    assertThat(englishRules)
        .as("api-stock 의 LedgerIntegrityService 가 정의한 규칙 수(24)보다 적으면 어떤 규칙은 이름 없이 그려진다")
        .hasSizeGreaterThanOrEqualTo(24);
    assertThat(koreanRules).as("두 번들의 규칙 키 집합이 다르다").isEqualTo(englishRules);

    List<String> blank = new ArrayList<>();
    for (String key : englishRules) {
      if (english.get(key).isBlank()) {
        blank.add(key + " -> uiMessage.properties 값이 비어 있음");
      }
      if (korean.get(key).isBlank()) {
        blank.add(key + " -> uiMessage_ko.properties 값이 비어 있음");
      }
    }
    assertThat(blank).isEmpty();
  }

  /** 조립 키가 이 한 곳뿐이라는 전제. 늘어나면 위 검사만으로는 부족해진다. */
  @Test
  void 조립되는_메시지_키는_원장_점검_한_곳뿐이다() throws IOException {
    List<String> prefixes = new ArrayList<>();
    for (Path root : SOURCE_ROOTS) {
      if (!Files.isDirectory(root)) {
        continue;
      }
      try (Stream<Path> files = Files.walk(root)) {
        for (Path file :
            files
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".jte") || p.toString().endsWith(".java"))
                .toList()) {
          Matcher matcher =
              Pattern.compile("(?:getMessage|(?<![\\w.])msg)\\(\\s*\"([^\"]*\\.)\"\\s*\\+")
                  .matcher(Files.readString(file, StandardCharsets.UTF_8));
          while (matcher.find()) {
            prefixes.add(matcher.group(1));
          }
        }
      }
    }
    assertThat(prefixes)
        .as("접두어 + 변수로 만드는 키가 새로 생겼다. 그 접두어도 두 로케일 검사를 붙일 것")
        .containsOnly(LEDGER_RULE_PREFIX);
  }
}
