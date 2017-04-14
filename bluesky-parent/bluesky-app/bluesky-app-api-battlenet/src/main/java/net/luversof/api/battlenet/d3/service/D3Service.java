package net.luversof.api.battlenet.d3.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class D3Service {
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Value("${api.battleNet.apikey}")
	private String apikey;
	
	@Value("${api.battleNet.apiHost}")
	private String battleNetApiHost;
	
	public Object getCareerProfile(String battleTag, String locale) {
		return restTemplate.getForObject(battleNetApiHost + "/d3/profile/{battleTag}/?locale={locale}&apikey={apikey}", Object.class, battleTag, locale, apikey);
	}
	
	public Object getHeroProfile(String battleTag, int heroId, String locale) {
		return restTemplate.getForObject(battleNetApiHost + "/d3/profile/{battleTag}/hero/{heroId}?locale={locale}&apikey={apikey}", Object.class, battleTag, heroId, locale, apikey);
	}
	
	public Object getItemData(String itemData, String locale) {
		return restTemplate.getForObject(battleNetApiHost + "/d3/data/item/{itemData}?locale={locale}&apikey={apikey}", Object.class, itemData, locale, apikey);
	}
	
	public Object getFollowerData(String locale) {
		return restTemplate.getForObject(battleNetApiHost + "/d3/data/follower/templar?locale={locale}&apikey={apikey}", Object.class, locale, apikey);
	}
}
