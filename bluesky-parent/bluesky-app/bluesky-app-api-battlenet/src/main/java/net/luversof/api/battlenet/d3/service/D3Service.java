package net.luversof.api.battlenet.d3.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class D3Service {
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Value("${api.battleNet.apiHost}")
	private String battleNetApiHost;
	
	public Object getCareerProfile(String profile, String locale, String apikey) {
		return restTemplate.getForObject(battleNetApiHost + "/d3/profile/{profile}/?locale={locale}&apikey={apikey}", Object.class, profile, locale, apikey);
	}
	
	public Object getHeroProfile(String profile, int heroId, String locale, String apikey) {
		return restTemplate.getForObject(battleNetApiHost + "/d3/profile/{profile}/hero/{heroId}?locale={locale}&apikey={apikey}", Object.class, profile, heroId, locale, apikey);
	}
	
	public Object getItemData(String itemId, String locale, String apikey) {
		return restTemplate.getForObject(battleNetApiHost + "/d3/data/item/{itemId}?locale={locale}&apikey={apikey}", Object.class, itemId, locale, apikey);
	}
	
	public Object getFollowerData(String locale, String apikey) {
		return restTemplate.getForObject(battleNetApiHost + "/d3/data/follower/templar?locale={locale}&apikey={apikey}", Object.class, locale, apikey);
	}
}
