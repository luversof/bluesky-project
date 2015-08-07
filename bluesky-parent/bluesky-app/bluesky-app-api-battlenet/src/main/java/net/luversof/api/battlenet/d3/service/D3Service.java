package net.luversof.api.battlenet.d3.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class D3Service {
	
	@Autowired
	private RestTemplate restTemplate;
	
	public Object getCareerProfile(String profile, String locale, String apikey) {
		return restTemplate.getForObject("https://kr.api.battle.net/d3/profile/{profile}/?locale={locale}&apikey={apikey}", Object.class, profile, locale, apikey);
	}
}
