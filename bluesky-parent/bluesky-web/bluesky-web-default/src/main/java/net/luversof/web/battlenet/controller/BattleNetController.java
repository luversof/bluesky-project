package net.luversof.web.battlenet.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import net.luversof.api.battlenet.d3.service.D3Service;
import net.luversof.security.core.userdetails.BlueskyUser;

@Controller
@RequestMapping("battleNet/d3")
public class BattleNetController {
	
	@Autowired
	private D3Service d3Service;
	
	@Value("${api.battleNet.apikey}")
	private String apikey;
	
	@PreAuthorize("hasRole('ROLE_BATTLENETUSER')")
	@RequestMapping("/index")
	public void index() {
	}
	
	
	@PreAuthorize("hasRole('ROLE_BATTLENETUSER')")
	@RequestMapping("/my/profile")
	@ResponseBody
	public Object myProfile (Authentication authentication, @RequestParam(defaultValue = "ko_KR") String locale, ModelMap modelMap) {
		BlueskyUser blueskyUser = (BlueskyUser) authentication.getPrincipal();
		return d3Service.getCareerProfile(blueskyUser.getUsername(), locale, apikey);
	}
	
	@RequestMapping("/profile/{profile}")
	public Object getCareerProfile(@PathVariable String profile, @RequestParam(defaultValue = "ko_KR") String locale) {
		return d3Service.getCareerProfile(profile, locale, apikey);
	}
	
	@RequestMapping("/profile/{profile}/hero/{heroId}")
	public Object getHeroProfile(@PathVariable String profile, @PathVariable int heroId, @RequestParam(defaultValue = "ko_KR") String locale) {
		return d3Service.getHeroProfile(profile, heroId, locale, apikey);
	}
	
	@RequestMapping("/data/item/{itemId}")
	public Object getItemData(@PathVariable String itemId, @RequestParam(defaultValue = "ko_KR") String locale) {
		return d3Service.getItemData(itemId, locale, apikey);
	}

}
