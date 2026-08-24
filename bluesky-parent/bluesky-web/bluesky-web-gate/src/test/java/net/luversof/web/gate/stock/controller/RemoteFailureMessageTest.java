package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.luversof.boot.exception.BlueskyErrorMessage;
import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.exception.ErrorMessage;

/**
 * 백엔드가 알려준 실패 사유를 화면이 버리지 않는지 본다.
 *
 * <p>api-stock 호출이 실패하면 bluesky-boot 의 {@code BlueskyClientResponseErrorHandler} 가 응답 본문을 {@link
 * BlueskyException} 으로 바꿔 던지고, 그 안에는 백엔드가 "사용자에게 보여도 된다"고 표시한 실제 사유가 들어 있다. 그런데 화면은 그걸 버리고 "입력값을
 * 확인해 주세요" 같은 문구로 덮고 있었다 &mdash; 원인을 아는데도 모른다고 말하는 셈이고, 게다가 예외를 로그로도 남기지 않아 나중에 확인할 방법이 없었다(월배당 참고
 * 화면의 저장/삭제/일괄저장 등 8 곳).
 */
class RemoteFailureMessageTest {

  private static final Path CONTROLLER =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java");

  private BlueskyErrorMessage message(String text, boolean displayable) {
    BlueskyErrorMessage errorMessage = new BlueskyErrorMessage();
    errorMessage.setMessage(text);
    errorMessage.setDisplayableMessage(displayable);
    return errorMessage;
  }

  @Test
  void 보여도_되는_사유는_그대로_쓴다() {
    BlueskyException ex = new BlueskyException(message("이미 등록된 종목입니다.", true));

    assertThat(StockViewController.remoteDisplayableMessage(ex)).isEqualTo("이미 등록된 종목입니다.");
  }

  /** 내부용 메시지(예외 클래스명 등)를 그대로 보여주면 안 된다. */
  @Test
  void 내부용_메시지는_쓰지_않는다() {
    BlueskyException ex = new BlueskyException(message("NullPointerException", false));

    assertThat(StockViewController.remoteDisplayableMessage(ex)).isNull();
  }

  @Test
  void 빈_메시지는_쓰지_않는다() {
    assertThat(
            StockViewController.remoteDisplayableMessage(new BlueskyException(message("", true))))
        .isNull();
    assertThat(
            StockViewController.remoteDisplayableMessage(
                new BlueskyException(message("   ", true))))
        .isNull();
    assertThat(
            StockViewController.remoteDisplayableMessage(new BlueskyException(message(null, true))))
        .isNull();
  }

  /** 검증 오류는 필드마다 하나씩 목록으로 온다. 목록만 채워진 경우도 읽어야 한다. */
  @Test
  void 목록으로_온_사유도_읽는다() {
    BlueskyException ex =
        new BlueskyException(
            List.<ErrorMessage>of(message("internal", false), message("수량은 1 이상이어야 합니다.", true)));

    assertThat(StockViewController.remoteDisplayableMessage(ex)).isEqualTo("수량은 1 이상이어야 합니다.");
  }

  @Test
  void 백엔드_예외가_아니면_null_이다() {
    assertThat(StockViewController.remoteDisplayableMessage(new IllegalStateException("boom")))
        .isNull();
    assertThat(StockViewController.remoteDisplayableMessage(null)).isNull();
  }

  /** 예외를 삼키면 실패 원인을 나중에 확인할 방법이 없다. */
  @Test
  void 모든_광범위_catch_는_예외를_로그로_남긴다() throws IOException {
    String[] lines = Files.readString(CONTROLLER, StandardCharsets.UTF_8).split("\\R", -1);
    // 파서가 조용히 0건이 되면 검사가 무력해지므로 하한을 둔다(현재 13곳).
    int broadCatches = 0;
    for (int i = 0; i < lines.length; i++) {
      // 일부러 무시하는 catch(변수명 ignore/ignored)는 대상이 아니다. 이 코드베이스의 관례이고,
      // 잘못된 타임존 문자열처럼 사용자가 아무 값이나 넣을 수 있는 자리라 로그를 남기면 소음만 된다.
      if (!lines[i].contains("catch (Exception ex)")
          && !lines[i].contains("catch (RuntimeException ex)")
          && !lines[i].contains("catch (RuntimeException e)")) {
        continue;
      }
      broadCatches++;
      String following =
          String.join(
              "\n", java.util.Arrays.asList(lines).subList(i + 1, Math.min(lines.length, i + 4)));
      assertThat(following).as((i + 1) + "행의 catch 가 예외를 로그로 남기지 않는다").contains("log.");
    }
    assertThat(broadCatches).as("catch 블록을 하나도 찾지 못했다").isGreaterThan(8);
  }

  /** 원인을 모르는 실패를 입력 탓으로 돌리지 않는다. */
  @Test
  void 원인을_모르는_실패를_입력_탓으로_돌리지_않는다() throws IOException {
    // 주석까지 훑으면 "예전에는 이렇게 적었다"는 설명에 스스로 걸린다(실제로 처음에 그렇게 실패했다).
    // 코드 줄만 본다.
    String code =
        Files.readString(CONTROLLER, StandardCharsets.UTF_8)
            .lines()
            .map(String::strip)
            .filter(
                line -> !line.startsWith("//") && !line.startsWith("*") && !line.startsWith("/*"))
            .collect(java.util.stream.Collectors.joining("\\n"));
    assertThat(code)
        .as("지역 검증(IllegalArgumentException)이 이미 걸러낸 뒤라 이 경로는 정의상 입력 문제가 아니다")
        .doesNotContain("입력값을 확인해 주세요");
  }
}
