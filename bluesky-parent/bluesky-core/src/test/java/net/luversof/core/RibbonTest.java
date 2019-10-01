package net.luversof.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.client.RestTemplate;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

@Slf4j
public class RibbonTest extends GeneralTest {

//	@Autowired(required = false)
//	private ServerList ribbonServerList;
	 
	@Autowired
	private LoadBalancerClient loadBalancerClient;
	
	@Autowired
	private DiscoveryClient discoveryClient;
	
	@Autowired
	private RestTemplate restTemplate;

	
	@Test
	@SneakyThrows
	public void test() {
		
		log.debug("discoveryClient serviceList : {}", discoveryClient.getServices());
		ServiceInstance instance = loadBalancerClient.choose("bluesky-cloud-config-server");
		log.debug("loadBalancerClient choose : {}", instance);
//		URI storesUri = URI.create(String.format("http://%s:%s", instance.getHost(), instance.getPort()));
//		log.debug("result : {}", storesUri);
	}
	
	@Test
	public void test2() {
		//log.debug("restTemplate result : {}", restTemplate.getForObject("http://bluesky-web-default/_check/discoveryClientServiceList", Object.class ));
		log.debug("restTemplate result : {}", restTemplate.getForObject("http://bluesky-cloud-config-server/bluesky-project.yml", String.class ));
	}
}
