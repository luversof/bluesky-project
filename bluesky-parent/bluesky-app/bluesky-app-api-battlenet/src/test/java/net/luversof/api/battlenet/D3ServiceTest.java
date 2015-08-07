package net.luversof.api.battlenet;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.battlenet.d3.service.D3Service;

@Slf4j
public class D3ServiceTest extends GeneralTest {

	@Autowired
	private D3Service d3Service;
	
	@Test
	public void getCareerProfileTest() {
		log.debug("result : {}", d3Service.getCareerProfile("파란하늘#3794", "ko_KR", "cntvyqjqspexfebzd42qte5faa4jgyaz"));
	}
}
