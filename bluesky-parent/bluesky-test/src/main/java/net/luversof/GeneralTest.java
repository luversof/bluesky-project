package net.luversof;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * 실환경에 붙는 통합 테스트의 공통 진입점.
 *
 * <p><b>이 인터페이스를 구현한 테스트는 오프라인 빌드에서 컨텍스트를 못 띄운다.</b> 실측 2026-08-23 기준 bluesky-api-stock 10 건,
 * bluesky-web-gate 9 건이 그래서 error 로 남는다. 회귀가 아니라 환경 의존이므로, 테스트 수를 셀 때 이 건수는 기준선으로 둔다.
 *
 * <p>원인은 {@code spring.config.import=configserver:} 가 <b>없어서</b>다(있어서가 아니다). 클래스패스에 config client 가
 * 있으면 {@code ConfigDataMissingEnvironmentPostProcessor} 가 그 import 를 요구하며 기동을 거부한다 &mdash; 실제 메시지도
 * {@code spring.config.import missing configserver:} 다.
 *
 * <p>두 가지를 실제로 시도해 봤고 둘 다 해결이 아니었다. 같은 길을 다시 돌지 않도록 적어 둔다.
 *
 * <ul>
 *   <li>{@code spring.cloud.config.enabled=false} 를 테스트 프로퍼티로 준다 &rarr; 기동은 더 진행되지만 {@code Cannot
 *       determine target DataSource for lookup key [null]} 로 끝난다. 접속 정보 자체가 config server 에서 오기
 *       때문이다.
 *   <li>{@code localdev} 프로파일을 준다 &rarr; import 는 채워지지만 {@code Failed to obtain JDBC Connection} 으로
 *       끝난다.
 * </ul>
 *
 * <p>즉 이 테스트들은 config server 와 접속 가능한 DB 가 함께 있어야 돈다. 게다가 실사용자 데이터를 바꾸는 메서드는 이미
 * {@code @Disabled("실사용자 데이터를 바꾼다 - 필요할 때만 손으로 실행")} 로 막혀 있다. 지금처럼 컨텍스트에서 일찍 멈추는 편이 오히려 안전하다.
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.NONE)
public interface GeneralTest {}
