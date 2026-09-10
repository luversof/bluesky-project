package net.luversof.api.stock.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;

/**
 * JSONB 변환기는 PostgreSQL dialect 의 저장소 타입과 함께 등록된다.
 *
 * <p>실측 2026-09-09: {@code new JdbcCustomConversions(List)} 로 만들면 PGobject 가 저장소 타입이 아니라 기동마다
 * CustomConversions WARN 2줄(reading/writing converter although it doesn't convert from/to a
 * store-supported type)이 났다. 변환 자체는 됐지만 등록 계약이 어긋난 상태였다. 컨텍스트 없이 빈 메서드만 호출한다.
 */
class StockJdbcCustomConversionsTest {

  private final JdbcCustomConversions conversions =
      new StockDataJdbcConfig().stockJdbcCustomConversions();

  @Test
  void PGobject_는_저장소_단순_타입이다() {
    assertThat(conversions.isSimpleType(PGobject.class))
        .as("dialect 단순 타입 없이 등록됨 - 기동 WARN 재발")
        .isTrue();
  }

  @Test
  void Map_과_PGobject_사이_읽기_쓰기_변환기가_모두_등록된다() {
    assertThat(conversions.hasCustomReadTarget(PGobject.class, Map.class)).isTrue();
    assertThat(conversions.hasCustomWriteTarget(Map.class, PGobject.class)).isTrue();
  }
}
