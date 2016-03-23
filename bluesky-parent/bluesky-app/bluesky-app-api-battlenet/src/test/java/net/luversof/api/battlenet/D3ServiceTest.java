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
	
	private String profile = "파란하늘#3794";
	private String locale = "ko_KR";
	
	@Test
	public void getCareerProfileTest() {
		log.debug("getCareerProfileTest result : {}", d3Service.getCareerProfile("파란하늘#3794", "ko_KR"));
	}
	
	@Test
	public void getHeroProfileTest() {
		log.debug("getHeroProfileTest result : {}", d3Service.getHeroProfile(profile, 40533, locale));
	}
	
	@Test
	public void getItemDataTest() {
		log.debug("getItemDataTest result : {}", d3Service.getItemData("Unique_Helm_002_p1", locale));
	}
}
