package net.luversof.api.stock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Google API를 호출하기 위한 설정 관리 */
@ConfigurationProperties(prefix = "google.api")
public class GoogleApiProperties {}
