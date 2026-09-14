package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.support.StockZoneParamException;

/**
 * 타임존 문자열을 ZoneId 로 바꾸는 규칙을 고정한다.
 *
 * <p>이 값은 화면의 날짜를 정한다. 잘못된 문자열에 {@code ZoneId.of} 를 그대로 부르면 {@code ZoneRulesException} 이 나는데, 템플릿
 * 렌더 중이면 500 이고 컨트롤러면 공통 처리기가 본문 없는 200 으로 바꿔 htmx 가 빈 내용을 갈아끼운다 &mdash; 화면이 조용히 빈다 (실측:
 * 자산성장/배당내역/매매내역).
 *
 * <p>컨트롤러와 템플릿이 <b>같은 규칙</b>을 써야 한다는 것도 함께 고정한다. 예전에는 같은 코드가 두 벌이었는데, 이 세션에서 한 화면의 달력과 표가 서로 다른 존을
 * 쓰는 결함이 여러 번 나왔다.
 */
class StockZoneUtilTest {

  @Test
  void 비어_있으면_서버_기본_존이다() {
    assertThat(StockZoneUtil.resolve(null)).isEqualTo(ZoneId.systemDefault());
    assertThat(StockZoneUtil.resolve("")).isEqualTo(ZoneId.systemDefault());
    assertThat(StockZoneUtil.resolve("   ")).isEqualTo(ZoneId.systemDefault());
  }

  @Test
  void 정상_타임존은_그대로_쓴다() {
    assertThat(StockZoneUtil.resolve("Asia/Seoul")).isEqualTo(ZoneId.of("Asia/Seoul"));
    assertThat(StockZoneUtil.resolve("America/New_York")).isEqualTo(ZoneId.of("America/New_York"));
    assertThat(StockZoneUtil.resolve("UTC")).isEqualTo(ZoneId.of("UTC"));
  }

  /**
   * 알 수 없는 값은 이름을 들고 끊는다.
   *
   * <p>2026-09-11 까지 여기서 기본 존으로 떨어뜨렸다. 그때의 근거는 "예외가 나가면 화면이 조용히 빈다" 였는데, 그건 공통 처리기가 이 예외를 몰라서 본문 없는
   * 200 을 내던 시절의 이야기다. 지금은 {@code StockHtmxErrorResolver} 가 이름을 붙여 오류 조각을 그린다.
   *
   * <p>조용한 폴백이 남긴 실제 손해(실측 {@code ?timeZone=Not/AZone} 으로 다섯 화면): 자산성장만 "입력한 값(기간·필터 등)을 확인해 주세요" 를
   * 띄우고 나머지 넷은 <b>아무 말 없이</b> 그려졌다 &mdash; 주소에 적은 존이 아니라 서버 존으로 계산한 값이다. 존은 일자 경계를 옮긴다.
   */
  @Test
  void 알_수_없는_값은_이름을_들고_끊는다() {
    for (String bad : new String[] {"Mars/Olympus", "not a zone", "Asia/Seoul; DROP", "+99:00"}) {
      assertThatThrownBy(() -> StockZoneUtil.resolve(bad))
          .as(bad + " 는 조용히 서버 존으로 바뀌면 안 된다")
          .isInstanceOf(StockZoneParamException.class);
    }
    assertThat(
            catchThrowableOfType(
                    () -> StockZoneUtil.resolve("zone", "Mars/Olympus"),
                    StockZoneParamException.class)
                .getName())
        .as("어느 파라미터였는지 들고 다녀야 한다")
        .isEqualTo("zone");
  }

  /**
   * 끊더라도 화면이 조용히 비면 안 된다 - 옛 규칙의 근거였던 성질이다.
   *
   * <p>공통 처리기가 이 예외를 4xx 로 알아보고 이름 있는 문구를 골라야 한다. 하나라도 빠지면 htmx 가 본문 없는 200 을 갈아끼워 화면이 빈다.
   */
  @Test
  void 끊은_뒤에는_이름_있는_문구가_나간다() throws IOException {
    String resolver =
        Files.readString(
            Path.of("src/main/java/net/luversof/web/gate/stock/config/StockHtmxErrorResolver.java"),
            StandardCharsets.UTF_8);
    assertThat(countOccurrences(resolver, "StockZoneParamException"))
        .as("4xx 판정 한 번 + 문구 선택 한 번")
        .isEqualTo(2);
    assertThat(resolver).contains("stock.error.badrequest.timezone.desc");
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(
              Files.readString(
                  Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8))
          .as(bundle)
          .contains("stock.error.badrequest.timezone.desc");
    }
  }

  /**
   * 바꾸는 규칙이 한 곳에만 있는지.
   *
   * <p>컨트롤러의 {@code resolveZoneIdOrDefault} 는 이 유틸에 위임해야 한다. 두 벌이 되면 컨트롤러와 템플릿이 다른 존을 쓸 수 있다.
   */
  @Test
  void 바꾸는_규칙은_한_곳에만_있다() throws IOException {
    Path root = Path.of("src/main/java/net/luversof/web/gate/stock");
    int copies = 0;
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        copies += countOccurrences(source, "ZoneId.of(timeZone");
      }
    }
    assertThat(copies).as("ZoneId.of(timeZone) 호출은 StockZoneUtil.resolve 에만 있어야 한다").isEqualTo(1);
  }

  /** 정규식 없이 부분 문자열 수를 센다(빌드 도구가 이스케이프를 먹는 일이 반복돼 단순하게 둔다). */
  private int countOccurrences(String source, String needle) {
    int found = 0;
    int at = source.indexOf(needle);
    while (at >= 0) {
      found++;
      at = source.indexOf(needle, at + needle.length());
    }
    return found;
  }
}
