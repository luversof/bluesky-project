package net.luversof.web.gate.config;

import org.apache.catalina.core.StandardHost;
import org.apache.catalina.valves.ErrorReportValve;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 잘못된 요청에 톰캣 기본 오류 페이지가 나가지 않게 한다.
 *
 * <p>실측: {@code /stock/htmx/summary?from=<script>alert(1)</script>} 처럼 URL 에 RFC 위반 문자가 섞이면 응답(400)
 * 본문에 예외 메시지와 자바 스택트레이스 10줄, 그리고 "Apache Tomcat/11.0.22" 버전까지 그대로 실려 나갔다. 이 오류는 요청 라인 파싱
 * 단계(Http11InputBuffer)에서 나기 때문에 스프링의 예외 처리기나 {@code server.error.*} 프로퍼티로는 막을 수 없고, 호스트 파이프라인의
 * ErrorReportValve 를 직접 조여야 한다.
 *
 * <p>StandardHost 는 파이프라인에 ErrorReportValve 가 이미 있으면 기본 밸브를 추가하지 않는다. 그래서 여기서 미리 하나를 넣어둔다.
 */
@Configuration
public class TomcatErrorReportConfig {

  @Bean
  WebServerFactoryCustomizer<TomcatServletWebServerFactory> hideTomcatErrorReport() {
    return factory ->
        factory.addContextCustomizers(
            context -> {
              if (context.getParent() instanceof StandardHost host) {
                ErrorReportValve valve = new ErrorReportValve();
                valve.setShowReport(false);
                valve.setShowServerInfo(false);
                host.getPipeline().addValve(valve);
              }
            });
  }
}
