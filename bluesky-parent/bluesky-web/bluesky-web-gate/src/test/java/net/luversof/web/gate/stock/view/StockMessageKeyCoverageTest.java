package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 주식 화면이 부르는 메시지 키가 실제로 정의돼 있는지 본다.
 *
 * <p>없는 키는 <b>예외를 내지 않는다</b> &mdash; {@code MessageUtil.getMessage(code)} 는 기본값으로 빈 문자열을 넘기므로 그 자리가
 * 조용히 빈칸이 된다. 화면은 멀쩡해 보이고 라벨만 사라지므로 눈으로 보지 않으면 알 수 없다.
 *
 * <p>실제로 그런 일이 있었다 &mdash; 원장 점검 규칙 3 종을 api-stock 에 넣고 게이트 문구를 잊어, 관리 화면에 설명 없이 "(12)" 같은 숫자만 뜨는
 * 상태였다(2026-08-23). 그쪽은 키를 코드로 조립하므로 여기서 잡히지 않아 별도 검사(불변식 31)가 본다. 이 검사는 소스에 그대로 적힌 키를 맡는다.
 *
 * <p>실측 기준선(2026-08-23): 주식 소스 131 개에서 리터럴 키 616 개, 누락 0.
 */
class StockMessageKeyCoverageTest {

  private static final List<Path> SOURCE_ROOTS =
      List.of(Path.of("src/main/jte/stock"), Path.of("src/main/java/net/luversof/web/gate/stock"));

  /** {@code getMessage("...")} 와 컨트롤러의 {@code msg("...")}. */
  private static final Pattern MESSAGE_CALL =
      Pattern.compile("(?:getMessage|\\bmsg)\\(\\s*\"([^\"]+)\"");

  private Properties load(String fileName) throws IOException {
    Properties properties = new Properties();
    try (Reader reader =
        Files.newBufferedReader(Path.of("src/main/resources", fileName), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  private Set<String> usedKeys() throws IOException {
    Set<String> keys = new LinkedHashSet<>();
    for (Path root : SOURCE_ROOTS) {
      assertThat(root).as("소스 경로가 없다 - 검사가 무력해진다").exists();
      try (Stream<Path> files = Files.walk(root)) {
        for (Path file :
            files
                .filter(Files::isRegularFile)
                .filter(f -> f.toString().endsWith(".jte") || f.toString().endsWith(".java"))
                .toList()) {
          Matcher matcher = MESSAGE_CALL.matcher(Files.readString(file, StandardCharsets.UTF_8));
          while (matcher.find()) {
            String key = matcher.group(1);
            // 코드로 이어붙이는 접두사("stock.admin.ledger.rule.")는 그 자체로는 키가 아니다.
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
  void 주식_화면이_부르는_메시지_키는_모두_정의돼_있다() throws IOException {
    Set<String> keys = usedKeys();
    // 정규식이 낡아 조용히 0 건이 되면 검사가 무력해진다(현재 616 개).
    assertThat(keys).as("메시지 키를 찾지 못했다 - 추출식이 낡았다").hasSizeGreaterThan(500);

    Properties english = load("uiMessage.properties");
    Properties korean = load("uiMessage_ko.properties");

    List<String> missing = new ArrayList<>();
    for (String key : keys) {
      if (!english.containsKey(key)) {
        missing.add(key + " (en)");
      }
      if (!korean.containsKey(key)) {
        missing.add(key + " (ko)");
      }
    }

    assertThat(missing).as("없는 키는 예외가 아니라 빈칸으로 렌더된다 - 라벨이 사라져도 화면은 멀쩡해 보인다").isEmpty();
  }

  /** 두 언어 파일의 주식 키 집합이 같은지. 한쪽에만 있으면 그 언어에서만 빈칸이 된다. */
  @Test
  void 두_언어의_주식_키_집합이_같다() throws IOException {
    Properties english = load("uiMessage.properties");
    Properties korean = load("uiMessage_ko.properties");

    Set<String> englishStock = new LinkedHashSet<>();
    english.stringPropertyNames().stream()
        .filter(k -> k.startsWith("stock."))
        .forEach(englishStock::add);
    Set<String> koreanStock = new LinkedHashSet<>();
    korean.stringPropertyNames().stream()
        .filter(k -> k.startsWith("stock."))
        .forEach(koreanStock::add);

    assertThat(englishStock).as("주식 키를 찾지 못했다").hasSizeGreaterThan(500);

    Set<String> onlyEnglish = new LinkedHashSet<>(englishStock);
    onlyEnglish.removeAll(koreanStock);
    Set<String> onlyKorean = new LinkedHashSet<>(koreanStock);
    onlyKorean.removeAll(englishStock);

    assertThat(onlyEnglish).as("영문에만 있는 주식 키").isEmpty();
    assertThat(onlyKorean).as("한글에만 있는 주식 키").isEmpty();
  }
}
