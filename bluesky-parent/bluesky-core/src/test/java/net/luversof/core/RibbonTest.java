package net.luversof.core;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.ribbon.RibbonClientSpecification;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.web.client.RestTemplate;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ServerList;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

@Slf4j
public class RibbonTest extends GeneralTest {

	@Autowired
	private ServerList<?> ribbonServerList;
	 
	
	@Test
	public void test() {
		log.debug("serverList : {}", ribbonServerList);
	}
}
