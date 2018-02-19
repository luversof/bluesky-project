package net.luversof.web;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.netflix.discovery.EurekaClient;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

@Slf4j
public class DiscoveryClientTest extends GeneralTest {

	
	@Autowired
	private EurekaClient eurekaClient;
	
	@Test
	public void test() {
		log.debug("result : {}", eurekaClient.getAllKnownRegions());	
	}
}
