package net.luversof.web.battlenet.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.battlenet.d3.service.D3Service;
import net.luversof.security.core.userdetails.BlueskyUser;

@RestController
@RequestMapping("/battleNet/d3")
public class BattleNetController {
	
	@Autowired
	private D3Service d3Service;
	
	@PreAuthorize("hasRole('ROLE_USER_BATTLENET')")
	@RequestMapping("/my/profile")
	public Object myProfile (Authentication authentication, @RequestParam(defaultValue = "ko_KR") String locale, ModelMap modelMap) {
		BlueskyUser blueskyUser = (BlueskyUser) authentication.getPrincipal();
		return d3Service.getCareerProfile(blueskyUser.getUsername(), locale);
	}
	
	@RequestMapping("/profile/{battleTag}")
	public Object getCareerProfile(@PathVariable String battleTag, @RequestParam(defaultValue = "ko_KR") String locale) {
		return d3Service.getCareerProfile(battleTag, locale);
	}
	
	@RequestMapping("/profile/{battleTag}/hero/{heroId}")
	public Object getHeroProfile(@PathVariable String battleTag, @PathVariable int heroId, @RequestParam(defaultValue = "ko_KR") String locale) {
		return d3Service.getHeroProfile(battleTag, heroId, locale);
	}
	
	@RequestMapping("/data/item/{itemData}")
	public Object getItemData(@PathVariable String itemData, @RequestParam(defaultValue = "ko_KR") String locale) {
		return d3Service.getItemData(itemData, locale);
	}

}
