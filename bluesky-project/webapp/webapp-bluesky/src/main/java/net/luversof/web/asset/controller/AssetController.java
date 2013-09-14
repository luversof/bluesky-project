package net.luversof.web.asset.controller;

import java.text.MessageFormat;

import lombok.extern.slf4j.Slf4j;
import net.luversof.asset.domain.Asset;
import net.luversof.asset.service.AssetService;
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
@RequestMapping("/user/{userId}/asset")
public class AssetController {
	
	@Autowired
	private AssetService assetService;

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(value = { "" })
	public String list(@PathVariable long userId, @RequestParam(defaultValue = "1") int page, Authentication authentication, ModelMap modelMap) {
		log.debug("modelMap : {}", modelMap);
		modelMap.addAttribute(assetService.findByUsername(authentication.getName()));
		return "/asset/list";
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(method = RequestMethod.POST)
	public String save(@PathVariable long userId, Asset asset, Authentication authentication) {
		asset.setUsername(authentication.getName());
		log.debug("save asset : {}", asset);
		if (asset.getAssetGroup() != null && asset.getAssetGroup().getId() != 0) {
//			asset.setAssetGroup(assetGroupService.findOne(asset.getAssetGroup().getId()));
		}
		assetService.save(asset);
		return MessageFormat.format("redirect:/user/{0}/asset", userId);
	}

	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(value = "/{assetId}", method = RequestMethod.PUT)
	public String modify(@PathVariable long userId, Asset asset, Authentication authentication) {
		Asset targetAsset = assetService.findOne(asset.getId());
		
		if (!authentication.getName().equals(targetAsset.getUsername())) {
			throw new BlueskyException("username is not owner");
		}
		targetAsset.setName(asset.getName());
		targetAsset.setAmount(asset.getAmount());
		targetAsset.setEnable(asset.isEnable());
		
		assetService.save(targetAsset);
		return MessageFormat.format("redirect:/user/{0}/asset", userId);
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(value = "/{assetId}", method = RequestMethod.DELETE)
	public String delete(@PathVariable long userId, @PathVariable long assetId, Authentication authentication, ModelMap modelMap) {
		Asset targetAsset = assetService.findOne(assetId);
		if (!authentication.getName().equals(targetAsset.getUsername())) {
			throw new BlueskyException("username is not owner");
		}
		log.debug("id : {}", assetId);
		assetService.delete(assetId);
		return "redirect:/blog";
	}
}
