package net.luversof.client.user.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

/**
 * Shared Redis-backed Spring Session configuration for bluesky-web-* modules. Uses Redis
 * host/profile settings provided via properties.
 */
@AutoConfiguration
public class ClientUserCommonSessionConfig {

  @Configuration(proxyBeanMethods = false)
  @Profile("k8sdev")
  @PropertySource("classpath:clientUser.properties")
  static class K8sDevClientUserConfig {}
}
