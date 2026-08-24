package net.luversof.api.stock.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

import io.github.luversof.boot.connectioninfo.ConnectionInfoUtil;

/**
 * 주식 데이터소스의 커넥션 풀을 조정한다.
 *
 * <p>이 데이터소스는 bluesky-boot 의 connection-info 로더가 만든다. 로더는 {@code HikariConfig} 에 url/계정만 채우고 나머지는
 * 기본값(최대 10, 대기 30초)으로 두며, 풀 설정을 프로퍼티로 노출하지 않는다(실측: {@code
 * spring.datasource.hikari.maximum-pool-size} 를 줘도 {@code maximumPoolSize=10} 그대로). 그래서 이미 만들어진 풀을
 * 여기서 조정한다.
 *
 * <p>왜 필요한가(실측):
 *
 * <ul>
 *   <li>재기동 직후 첫 트래픽이 동시 16건이면 <b>16건 모두 30초 뒤 실패</b>했다({@code Connection is not available …
 *       total=10/10, active=10, waiting=16}). 그 시점 스레드 덤프에서 16개 전부 {@code getConnection} 대기였고 커넥션을
 *       쥔 스레드는 없었다.
 *   <li>콜드 요청은 워밍업된 요청보다 10배 가까이 느리다(같은 조회가 p50 521ms vs 61ms). 그동안 커넥션을 물고 있으므로 동시 요청이 풀 상한에 닿으면
 *       대기가 30초 한도를 넘긴다.
 *   <li>상한을 올리자 같은 콜드 16 동시가 <b>실패 0</b>(p50 521ms)로 바뀌었다. 상한을 넘는 32 동시에서는 다시 일부 실패했지만, 대기시간을 줄여
 *       30초 정지 대신 5.3초 만에 실패로 끝났다.
 * </ul>
 *
 * <p>화면 하나가 api-stock 를 5개까지 병렬 호출하므로 동시 사용자 3~4명이면 기존 상한(10)에 바로 닿는다. 상한 20 은 그 부하를 여유 있게 받으면서 파드당
 * DB 커넥션을 과하게 잡지 않는 선이다. 대기시간 5초는 "30초 정지" 대신 빠르게 실패시켜 호출자(게이트)가 오류 조각을 그리도록 한다.
 */
@Configuration
public class StockDataSourcePoolConfig implements InitializingBean {

  private static final Logger log = LoggerFactory.getLogger(StockDataSourcePoolConfig.class);

  private static final int MAXIMUM_POOL_SIZE = 20;

  /**
   * 상시 유지할 유휴 커넥션 수.
   *
   * <p>Hikari 는 기본적으로 최대치만큼 유휴 커넥션을 붙잡는다. 실측: DB 는 {@code max_connections=100} 인데 이미 80 개가 쓰이고 있고
   * 그중 stock 이 30 개(전부 idle)였다. 상한만 올리면 상시 점유가 그만큼 늘어 다른 서비스의 여유를 깎는다. 평소에는 적게 물고 있다가 몰릴 때만 늘리도록 최소
   * 유휴를 따로 낮춘다.
   */
  private static final int MINIMUM_IDLE = 5;

  private static final long CONNECTION_TIMEOUT_MS = 5_000L;

  /** 늘어난 커넥션을 되돌리는 시간. 기본 10 분은 몰린 뒤 오래도록 상한만큼 물고 있게 만든다. DB 여유가 20 개뿐이라 1 분이면 충분히 회수한다. */
  private static final long IDLE_TIMEOUT_MS = 60_000L;

  @Override
  public void afterPropertiesSet() {
    DataSource dataSource = ConnectionInfoUtil.getConnection("stock_postgresql");
    if (dataSource instanceof HikariDataSource hikari) {
      hikari.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
      hikari.setMinimumIdle(MINIMUM_IDLE);
      hikari.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
      hikari.setIdleTimeout(IDLE_TIMEOUT_MS);
      log.info(
          "stock connection pool tuned: maximumPoolSize={}, minimumIdle={}, connectionTimeout={}ms,"
              + " idleTimeout={}ms",
          hikari.getMaximumPoolSize(),
          hikari.getMinimumIdle(),
          hikari.getConnectionTimeout(),
          hikari.getIdleTimeout());
    } else {
      log.warn(
          "stock datasource is not a HikariDataSource ({}), pool tuning skipped",
          dataSource == null ? "null" : dataSource.getClass().getName());
    }
  }
}
