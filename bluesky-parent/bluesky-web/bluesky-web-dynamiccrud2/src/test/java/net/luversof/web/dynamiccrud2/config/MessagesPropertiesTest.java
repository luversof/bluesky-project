package net.luversof.web.dynamiccrud2.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 다국어 메시지 파일 가드. 한글이 편집기 코드페이지(ISO-8859-1 · MS949)로 저장되거나 병합으로 깨져 들어오면 화면이 통째로 깨지므로 UTF-8 로 읽히는지 ·
 * 한글이 있는지 · 깨진 글자(U+FFFD)가 없는지, ko 와 en 의 키가 같은지를 본다. 그리고 Msg 가 언어에 따라 고르는지.
 */
class MessagesPropertiesTest {

  @AfterEach
  void reset() {
    LocaleContextHolder.resetLocaleContext();
  }

  private static String readUtf8(String name) throws IOException {
    try (InputStream in = MessagesPropertiesTest.class.getResourceAsStream("/" + name)) {
      assertThat(in).as(name).isNotNull();
      var bytes = in.readAllBytes();
      try {
        return StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString();
      } catch (CharacterCodingException e) {
        throw new AssertionError(name + " 이 UTF-8 이 아닙니다(편집기 인코딩 확인)", e);
      }
    }
  }

  private static Properties load(String text) throws IOException {
    var properties = new Properties();
    properties.load(new StringReader(text));
    return properties;
  }

  /**
   * 파일은 ASCII 만(한글 · 기호는 \\uXXXX, 사내 메시지 파일 관례 - DMT2 와 같게). STS · Eclipse 는 .properties 를
   * ISO-8859-1 로 열어 날 UTF-8 한글이 깨져 보인다. 새 문구를 한글 그대로 넣으면 여기서 막힌다 - native2ascii 로 바꿔 넣을 것.
   */
  @Test
  void asciiOnlyAndSameKeys() throws IOException {
    for (var name : new String[] {"messages.properties", "messages_en.properties"}) {
      var text = readUtf8(name);
      var lineNo = 0;
      for (var line : text.split("\\r?\\n", -1)) {
        lineNo++;
        assertThat(line.chars().allMatch(c -> c < 128))
            .as("%s:%d 에 ASCII 가 아닌 글자(한글은 \\uXXXX 로): %s", name, lineNo, line)
            .isTrue();
      }
    }
    var ko = load(readUtf8("messages.properties"));
    var en = load(readUtf8("messages_en.properties"));
    assertThat(
            ko.values().stream().anyMatch(v -> Pattern.compile("[가-힣]").matcher((String) v).find()))
        .as("한국어 문구")
        .isTrue();
    assertThat(ko.values()).noneMatch(v -> ((String) v).contains("\uFFFD"));
    assertThat(ko.stringPropertyNames()).isEqualTo(en.stringPropertyNames());
  }

  @Test
  void msgFollowsLocale() {
    // 요청 밖은 한국어
    assertThat(Msg.t("common.save")).isEqualTo("저장");
    LocaleContextHolder.setLocale(Locale.US);
    assertThat(Msg.t("common.save")).isEqualTo("Save");
    assertThat(Msg.t("admin.table.title", "Table", "t")).isEqualTo("Table fields: t");
    // 지원하지 않는 언어는 한국어(서버 기본 로케일로 새지 않는다)
    LocaleContextHolder.setLocale(Locale.JAPAN);
    assertThat(Msg.t("common.save")).isEqualTo("저장");
  }

  @Test
  void localeRedirectStaysOnSite() {
    assertThat(LocaleController.backTo("https://dc2.bluesky.local/admin/menu/1?x=1"))
        .isEqualTo("/admin/menu/1?x=1");
    assertThat(LocaleController.backTo(null)).isEqualTo("/");
    assertThat(LocaleController.backTo("https://evil.example//other")).isEqualTo("/");
  }
}
