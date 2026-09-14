package net.luversof.web.gate.stock.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.support.StockDateParamException;

/**
 * 주소의 날짜 값을 못 읽으면 <b>어느 값인지</b> 말해 준다.
 *
 * <p>실측 2026-09-11(매매 이력 조각): {@code size=abc} 와 {@code page=abc} 는 "주소의 size 값을 읽지 못했습니다" 로 정확히 알려
 * 주는데, 같은 조각의 {@code from=notadate} 만 <b>"불러오지 못했습니다 · 잠시 후 다시 시도해 주세요"</b> 와 재시도 버튼이었다 &mdash; 서버
 * 장애처럼 보이지만 다시 시도해도 같은 결과다(from/to 를 String 으로 받아 컨트롤러가 직접 읽기 때문에 형 변환 예외가 아니었다).
 */
class StockDateParamMessageTest {

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  @Test
  void 날짜_파라미터_예외는_사용자_잘못으로_센다() {
    assertThat(StockHtmxErrorResolver.isClientError(new StockDateParamException("from", null)))
        .as("서버 장애로 세면 재시도 안내가 나가고 로그도 ERROR 로 남는다")
        .isTrue();
  }

  @Test
  void 리졸버가_이름과_날짜_문구를_쓴다() throws IOException {
    String resolver =
        read("src/main/java/net/luversof/web/gate/stock/config/StockHtmxErrorResolver.java");

    assertThat(resolver).contains("StockDateParamException dateParam");
    assertThat(resolver).contains("dateParam.getName()");
    assertThat(resolver).contains("stock.error.badrequest.day.desc");
  }

  @Test
  void 매매이력_조각이_그_예외를_던진다() throws IOException {
    String controller =
        read(
            "src/main/java/net/luversof/web/gate/stock/controller/StockAssetGrowthHtmxController.java");

    assertThat(controller).contains("parseDayParam(" + (char) 34 + "from" + (char) 34 + ", from)");
    assertThat(controller).contains("parseDayParam(" + (char) 34 + "to" + (char) 34 + ", to)");
    assertThat(controller)
        .as("그냥 LocalDate.parse 로 되돌리면 다시 서버 장애처럼 보인다")
        .doesNotContain("LocalDate.parse(from)");
    assertThat(controller).doesNotContain("LocalDate.parse(to)");
  }

  @Test
  void 두_번들_모두_문구를_가진다() throws IOException {
    for (String name : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(read("src/main/resources/" + name)).contains("stock.error.badrequest.day.desc");
    }
  }
}
