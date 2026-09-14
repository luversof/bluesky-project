package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.support.RequestZoneUtil;

/**
 * 모르는 {@code timeZone} 값은 거절한다 &mdash; {@code granularity} / {@code breakdown} 과 같은 규칙.
 *
 * <p>실측 2026-09-11(같은 요청에 값만 바꿔, 전체 기간 일별 시계열): {@code timeZone=Not/AZone} 은 <b>200</b> 이고 응답이 타임존을
 * 주지 않은 것과 완전히 같았다(1,652,366 자, md5 동일). 반면 {@code timeZone=UTC} 는 1,652,594 자로 다르고 첫 점의 경계가 {@code
 * 2008-12-31T15:00Z} 에서 {@code 2008-12-31T00:00Z} 로 옮겨간다 &mdash; 즉 오타는 조용히 버려졌다.
 *
 * <p>이 서비스는 UTC 로 뜬 컨테이너에서 돈다. 거기서는 그 폴백이 KST 오전 거래를 전날로 집계해 차트와 연도 경계를 하루씩 민다.
 */
class TimeZoneParamValidationTest {

  @Test
  void 모르는_존은_400_으로_끊는다() {
    assertThatThrownBy(() -> RequestZoneUtil.parse("Not/AZone", ZoneId.of("Asia/Seoul")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unsupported timeZone");

    var request = new TradeProfitRequest();
    request.setTimeZone("Asia/Seuol");
    assertThatThrownBy(request::resolveZoneId)
        .as("한 글자 바뀐 오타야말로 조용히 지나가면 안 된다")
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void 값이_없으면_종전_기본값_그대로다() {
    assertThat(RequestZoneUtil.parse(null, ZoneId.of("Asia/Seoul")))
        .isEqualTo(ZoneId.of("Asia/Seoul"));
    assertThat(RequestZoneUtil.parse("  ", ZoneId.of("Asia/Seoul")))
        .isEqualTo(ZoneId.of("Asia/Seoul"));
    // 연간 비용은 널을 받아 서비스가 한국 기준을 쓴다 - 널 폴백도 그대로 유지한다.
    assertThat(RequestZoneUtil.parse(null, null)).isNull();
    assertThat(new TradeProfitRequest().resolveZoneId()).isEqualTo(ZoneId.systemDefault());
  }

  @Test
  void 아는_존은_통과한다() {
    for (String value :
        new String[] {"Asia/Seoul", "UTC", " Asia/Seoul ", "America/New_York", "+09:00"}) {
      assertThat(RequestZoneUtil.parse(value, ZoneId.of("Asia/Seoul")))
          .as(value + " 는 통과해야 한다")
          .isEqualTo(ZoneId.of(value.trim()));
    }
  }
}
