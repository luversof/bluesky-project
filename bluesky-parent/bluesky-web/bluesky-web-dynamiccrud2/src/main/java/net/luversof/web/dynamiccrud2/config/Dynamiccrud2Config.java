package net.luversof.web.dynamiccrud2.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 모듈 공통 설정. 테스트는 bluesky-test 의 TestApplication 으로 뜨므로 Application 이 아닌 여기서 등록한다. */
@Configuration
@EnableConfigurationProperties(Dynamiccrud2Properties.class)
public class Dynamiccrud2Config {}
