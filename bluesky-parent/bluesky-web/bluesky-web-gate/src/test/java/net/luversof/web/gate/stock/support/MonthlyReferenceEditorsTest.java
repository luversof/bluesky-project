package net.luversof.web.gate.stock.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 월배당 기준 데이터는 공용 값이라 고칠 사람을 설정으로 정한다(2026-10-27 감사 - 로그인한 누구나 남의 화면 기준값을 지우고 출처 주소를 심을 수 있었다). */
class MonthlyReferenceEditorsTest {

  private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
  private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

  @Test
  void 목록이_있으면_그_사용자만_고친다() {
    MonthlyReferenceEditors editors = new MonthlyReferenceEditors(" " + OWNER + " , ");

    assertThat(editors.canEdit(OWNER)).isTrue();
    assertThat(editors.canEdit(OTHER)).isFalse();
    assertThat(editors.canEdit(null)).isFalse();
    assertThatCode(() -> editors.requireEditor(OWNER)).doesNotThrowAnyException();
    assertThatThrownBy(() -> editors.requireEditor(OTHER))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
  }

  @Test
  void 목록이_비면_예전처럼_모두_허용한다() {
    assertThat(new MonthlyReferenceEditors("").canEdit(OTHER)).isTrue();
    assertThat(new MonthlyReferenceEditors(null).canEdit(OTHER)).isTrue();
  }

  /** 기준 데이터를 바꾸는 경로(POST · PUT /dividend/monthly-reference/...)는 모두 고칠 사람을 확인한다 - 새 경로가 빠지지 않게. */
  @Test
  void 기준_데이터를_바꾸는_경로는_모두_확인한다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockDividendViewController.java"),
            StandardCharsets.UTF_8);
    Matcher mapping =
        Pattern.compile(
                "@(?:Post|Put)Mapping\\((?:[^)]*?value\\s*=\\s*)?\"(/dividend/monthly-reference[^\"]*)\"")
            .matcher(source);
    List<String> unguarded = new ArrayList<>();
    int writes = 0;
    while (mapping.find()) {
      writes++;
      int bodyStart = source.indexOf('{', source.indexOf(") {", mapping.end()));
      int bodyEnd = source.indexOf("\n  }\n", bodyStart);
      if (bodyEnd < 0) {
        bodyEnd = source.indexOf("\r\n  }\r\n", bodyStart);
      }
      if (!source
          .substring(bodyStart, bodyEnd)
          .contains("monthlyReferenceEditors.requireEditor(")) {
        unguarded.add(mapping.group(1));
      }
    }
    assertThat(writes).as("쓰기 경로를 못 찾았다 - 검사가 헛돈다").isGreaterThanOrEqualTo(11);
    assertThat(unguarded).as("고칠 사람을 확인하지 않는 쓰기 경로").isEmpty();
  }
}
