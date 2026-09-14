package net.luversof.web.gate.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * JSON 오류 본문(ProblemDetail)에 스캐폴딩 자리표시자를 내보내지 않는다.
 *
 * <p>실측 2026-09-11: {@code GET /stock/api/timeSeries?userId=x} 가 {@code
 * {"detail":"MethodArgumentNotValid 에러 ","title":"MethodArgumentNotValid 에러 제목"}} 을 돌려줬다. 영어 번들은 더
 * 심해서 {@code MethodArgumentNotValidException=en ex} 였고, 다른 줄에는 오타도 있었다("오류 젬목", "nane은 null 이
 * 아니어야", "error occured").
 *
 * <p>이 문구는 게이트의 모든 JSON 오류가 쓰는 것이라 주식 API 도 그대로 내보낸다.
 */
class ProblemDetailMessageTest {

  private static final List<String> KEYS =
      List.of(
          "problemDetail.org.springframework.web.bind.MethodArgumentNotValidException",
          "problemDetail.title.org.springframework.web.bind.MethodArgumentNotValidException",
          "problemDetail.org.springframework.validation.BindException",
          "problemDetail.title.org.springframework.validation.BindException",
          "problemDetail.org.springframework.web.method.annotation.ModelAttributeMethodProcessor",
          "problemDetail.title.org.springframework.web.method.annotation.ModelAttributeMethodProcessor");

  private Properties load(String name) throws IOException {
    Properties properties = new Properties();
    try (var reader =
        Files.newBufferedReader(
            Path.of("src/main/resources").resolve(name), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  @Test
  void 예외_클래스_이름을_사용자에게_보여_주지_않는다() throws IOException {
    for (String name :
        List.of(
            "gateMessage.properties", "gateMessage_ko.properties", "gateMessage_en.properties")) {
      Properties bundle = load(name);
      for (String key : KEYS) {
        String value = bundle.getProperty(key);
        if (value == null) {
          continue;
        }
        assertThat(value)
            .as(name + " / " + key)
            .doesNotContain("MethodArgumentNotValid")
            .doesNotContain("BindException")
            .doesNotContain("ModelAttributeMethodProcessor")
            .doesNotContain("error title")
            .doesNotContain("제목");
        assertThat(value.trim()).as(name + " / " + key + " 는 비어 있으면 안 된다").isNotEmpty();
      }
    }
  }

  @Test
  void 영어_번들에_자리표시자가_없다() throws IOException {
    Properties en = load("gateMessage_en.properties");

    assertThat(en.getProperty("MethodArgumentNotValidException"))
        .as("예전 값은 " + '"' + "en ex" + '"' + " 였다")
        .isEqualTo("The request contains invalid values.");
    for (String key : KEYS) {
      assertThat(en.getProperty(key)).as("영어 번들에 " + key).isNotNull();
    }
  }

  @Test
  void 오타를_남기지_않는다() throws IOException {
    for (String name :
        List.of(
            "gateMessage.properties", "gateMessage_ko.properties", "gateMessage_en.properties")) {
      String raw =
          Files.readString(Path.of("src/main/resources").resolve(name), StandardCharsets.UTF_8);
      assertThat(raw)
          .as(name)
          .doesNotContain("occured")
          .doesNotContain("nane")
          .doesNotContain("en ex");
    }
  }
}
