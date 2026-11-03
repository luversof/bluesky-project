package net.luversof.api.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.DefaultCategory;

/**
 * 메시지 파일이 코드가 찾는 키를 다 갖는지.
 *
 * <p>2026-11-03 실측: 기본 문구 키 {@code BlueskyException} 이 없으면 공통 예외 처리(ProblemDetailUtil)가
 * NoSuchMessageException 으로 실패해 400 · 403 이 모두 500 이 됐다. 단위 시험은 이 경로를 타지 않아 로컬 기동에서야 드러났다.
 */
class MessagesTest {

  @Test
  void bundleHasFallbackErrorCodesAndDefaults() throws IOException {
    var properties = new Properties();
    try (InputStream in = getClass().getResourceAsStream("/bookkeepingMessage_ko.properties")) {
      properties.load(in);
    }
    assertThat(properties).containsKey("BlueskyException");
    assertThat(
            Arrays.stream(BookkeepingErrorCode.values())
                .map(c -> "BlueskyException.BookkeepingErrorCode." + c.name())
                .filter(k -> !properties.containsKey(k)))
        .isEmpty();
    assertThat(
            Arrays.stream(DefaultCategory.values())
                .map(DefaultCategory::messageCode)
                .filter(k -> !properties.containsKey(k)))
        .isEmpty();
    assertThat(properties).containsKey("bookkeeping.default.asset.CASH");
  }
}
