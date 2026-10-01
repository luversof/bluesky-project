package net.luversof.web.gate;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 메시지 파일이 잘못된 코드페이지로 읽혔다가 다시 저장되면 글자가 조용히 깨진다(발견 2026-09-30).
 *
 * <p>2026-09-29 19:44 병합(2c282857)으로 들어온 uiMessage.properties 에서 14 줄이 깨져 있었다: "한국어" 가 {@code i??e}
 * 로 시작하는 글자로, 가운뎃점이 "A" + 가운뎃점으로, 줄임표가 "a" + C1 제어 문자 + "|" 로. UTF-8 바이트를 latin-1 로 읽고 다시 적은 모양이다.
 * 컴파일도 시험도 모두 통과해서 화면(영문 로케일 · 로케일 선택)에만 드러난다.
 *
 * <p>잡는 모양: C1 제어 문자(U+0080~U+009F), UTF-8 을 latin-1 로 읽을 때 생기는 짝(Â · â · Ã 뒤 latin-1 기호), 대체
 * 문자(U+FFFD), 물음표 둘("??" - 한글 세 바이트가 물음표로 바뀐 자국). 모두 지금 메시지에는 한 번도 안 쓰인다(실측 0 건).
 */
class MessagePropertiesMojibakeTest {

  private static final Path RESOURCES = Path.of("src/main/resources");

  private static final char BACKSLASH = (char) 92;

  /** 이스케이프(백슬래시 u 네 자리)를 글자로 푼다. 백슬래시는 문자 코드로 적는다 - 소스에 적으면 컴파일러가 먼저 풀어 버린다. */
  static String unescape(String value) {
    StringBuilder builder = new StringBuilder(value.length());
    for (int index = 0; index < value.length(); index++) {
      char c = value.charAt(index);
      if (c == BACKSLASH && index + 6 <= value.length() && value.charAt(index + 1) == 'u') {
        String hex = value.substring(index + 2, index + 6);
        if (hex.chars().allMatch(ch -> Character.digit(ch, 16) >= 0)) {
          builder.append((char) Integer.parseInt(hex, 16));
          index += 5;
          continue;
        }
      }
      builder.append(c);
    }
    return builder.toString();
  }

  /** 깨진 자국이 있으면 그 까닭을, 없으면 null. */
  static String mojibakeReason(String text) {
    for (int index = 0; index < text.length(); index++) {
      char c = text.charAt(index);
      if (c >= 0x80 && c <= 0x9F) {
        return "C1 제어 문자 U+" + Integer.toHexString(c);
      }
      if (c == 0xFFFD) {
        return "대체 문자";
      }
      if ((c == 0xC2 || c == 0xC3 || c == 0xE2) && index + 1 < text.length()) {
        char next = text.charAt(index + 1);
        if (next >= 0x80 && next <= 0xBF) {
          return "UTF-8 을 latin-1 로 읽은 짝 U+" + Integer.toHexString(c);
        }
      }
      if (c == '?' && index + 1 < text.length() && text.charAt(index + 1) == '?') {
        return "물음표 둘";
      }
    }
    // "A" + 가운뎃점: 가운뎃점(U+00B7) 앞 C2 가 latin-1 로 읽혀 A 로 옮겨 적힌 자국. 낱말 사이에 홀로 선 A 만 본다 -
    // "ISA·연금저축" 처럼 낱말 끝 A 에 붙은 가운뎃점은 멀쩡하다(ko 662 줄).
    if (text.contains(" A" + (char) 0xB7 + " ")) {
      return "A + 가운뎃점";
    }
    return null;
  }

  @Test
  void 판정은_깨진_모양을_잡고_멀쩡한_글자는_둔다() {
    // 실제로 들어왔던 깨진 값 셋(백슬래시 이스케이프 그대로)
    String slash = String.valueOf(BACKSLASH);
    assertThat(
            mojibakeReason(
                unescape("i??e" + slash + "u03BC" + slash + "u00ADi?" + slash + "u00B4")))
        .isNotNull();
    assertThat(mojibakeReason(unescape("{0} nodes A" + slash + "u00B7 {1}s"))).isNotNull();
    assertThat(mojibakeReason("ISA" + (char) 0xB7 + "연금저축")).as("낱말 끝 A 는 멀쩡하다").isNull();
    assertThat(mojibakeReason(unescape("Select skillsa" + slash + "u0080|"))).isNotNull();
    assertThat(mojibakeReason("Asset trend " + (char) 0xE2 + (char) 0x80 + "? total")).isNotNull();
    // 멀쩡한 값
    assertThat(mojibakeReason(unescape(slash + "ud55c" + slash + "uad6d" + slash + "uc5b4")))
        .isNull();
    assertThat(mojibakeReason("연배당 7.20% " + (char) 0x2212 + " 분배금 감소 0.80% = 6.40점")).isNull();
    assertThat(mojibakeReason("Select skills" + (char) 0x2026)).isNull();
  }

  /** 메시지 파일 전부(uiMessage · gateMessage, 모든 로케일)를 줄마다 이스케이프를 풀어 본다. */
  @Test
  void 메시지_파일에_깨진_글자가_없다() throws IOException {
    List<String> broken = new ArrayList<>();
    List<Path> files;
    try (Stream<Path> stream = Files.list(RESOURCES)) {
      files =
          stream
              .filter(path -> path.getFileName().toString().endsWith(".properties"))
              .filter(path -> path.getFileName().toString().contains("Message"))
              .toList();
    }
    assertThat(files).as("메시지 파일을 못 찾았다 - 경로가 바뀌었다").hasSizeGreaterThanOrEqualTo(4);

    int lines = 0;
    for (Path file : files) {
      List<String> all = Files.readAllLines(file, StandardCharsets.UTF_8);
      for (int number = 0; number < all.size(); number++) {
        String line = all.get(number).strip();
        if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
          continue;
        }
        lines++;
        String text = unescape(line);
        String reason = mojibakeReason(text);

        if (reason != null) {
          broken.add(file.getFileName() + ":" + (number + 1) + " " + reason + " - " + line);
        }
      }
    }
    assertThat(lines).as("읽은 줄이 너무 적다 - 판정이 헛돈다").isGreaterThan(1000);
    assertThat(broken).as("잘못된 코드페이지로 저장된 자국").isEmpty();
  }
}
