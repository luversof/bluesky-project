package net.luversof.web.bookkeeping.controller;

import lombok.extern.slf4j.Slf4j;
import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.service.AssetGroupService;
import net.luversof.core.exception.BlueskyException;
import net.luversof.web.AuthorizeRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/user/{userId}/assetGroup")
public class AssetGroupController {
	
	@Autowired
	private AssetGroupService assetGroupService;

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(method = RequestMethod.GET, value = "")
	public String list(@PathVariable long userId, @RequestParam(defaultValue = "1") int page, Authentication authentication, ModelMap modelMap) {
		log.debug("modelMap : {}", modelMap);
		modelMap.addAttribute(assetGroupService.findByUsername(authentication.getName()));
		return "/bookkeeping/assetGroup/list";
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(method = RequestMethod.POST)
	public void save(@PathVariable long userId, AssetGroup assetGroup, Authentication authentication, ModelMap modelMap) {
		assetGroup.setUsername(authentication.getName());
		log.debug("save assetGroup : {}", assetGroup);
		modelMap.addAttribute(assetGroupService.save(assetGroup));
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(value = "/{assetGroupId}", method = RequestMethod.PUT)
	public void modify(@PathVariable long userId, AssetGroup assetGroup, Authentication authentication, ModelMap modelMap) {
		AssetGroup targetAssetGroup = assetGroupService.findOne(assetGroup.getId());
		if (!authentication.getName().equals(targetAssetGroup.getUsername())) {
			throw new BlueskyException("username is not owner");
		}
		targetAssetGroup.setName(assetGroup.getName());
		modelMap.addAttribute(assetGroupService.save(targetAssetGroup));
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(value = "/{assetGroupId}", method = RequestMethod.DELETE)
	public void delete(@PathVariable long userId, @PathVariable long assetGroupId, Authentication authentication, ModelMap modelMap) {
		AssetGroup targetAssetGroup = assetGroupService.findOne(assetGroupId);
		if (!authentication.getName().equals(targetAssetGroup.getUsername())) {
			throw new BlueskyException("username is not owner");
		}
		log.debug("id : {}", assetGroupId);
		assetGroupService.delete(assetGroupId);
	}
	
}
