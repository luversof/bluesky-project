//package net.luversof.core.config;
//
//import org.springframework.cloud.netflix.ribbon.RibbonClients;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import com.netflix.client.config.IClientConfig;
//import com.netflix.loadbalancer.BestAvailableRule;
//import com.netflix.loadbalancer.ConfigurationBasedServerList;
//import com.netflix.loadbalancer.IPing;
//import com.netflix.loadbalancer.IRule;
//import com.netflix.loadbalancer.PingUrl;
//import com.netflix.loadbalancer.Server;
//import com.netflix.loadbalancer.ServerList;
//import com.netflix.loadbalancer.ServerListSubsetFilter;
//
//@RibbonClients(defaultConfiguration = DefaultRibbonConfig.class)
//public class RibbonDefaultConfig {
//
//	public static class BazServiceList extends ConfigurationBasedServerList {
//		public BazServiceList(IClientConfig config) {
//			super.initWithNiwsConfig(config);
//		}
//	}
//}
//
//@Configuration
//class DefaultRibbonConfig {
//
//	@Bean
//	public IRule ribbonRule() {
//		return new BestAvailableRule();
//	}
//
//	@Bean
//	public IPing ribbonPing() {
//		return new PingUrl();
//	}
//
//	@Bean
//	public ServerList<Server> ribbonServerList(IClientConfig config) {
//		return new RibbonDefaultConfig.BazServiceList(config);
//	}
//
//	@Bean
//	public ServerListSubsetFilter serverListFilter() {
//		ServerListSubsetFilter filter = new ServerListSubsetFilter();
//		return filter;
//	}
//
//}